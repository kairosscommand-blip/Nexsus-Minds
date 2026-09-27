package com.nexuscraft.nexusminds;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.EnumSet;

/**
 * Lightweight pack coordination: when two or more same-kind mobs share a target, whichever one
 * is closest takes point (falls through to vanilla's own melee-approach goal, untouched), and the
 * rest peel off to a lateral point beside the target instead of queuing up single-file behind the
 * leader. The "who's closest" scan is throttled to once every
 * {@link MindsConfig#packScanIntervalTicks} rather than every tick, and the result cached between
 * scans -- full neighbor scans on every tick for every mob would be exactly the kind of
 * always-be-scanning cost this plugin family avoids everywhere else.
 */
final class PackFlankGoal implements Goal<Mob> {

    private static final double MELEE_RANGE = 2.2;
    private static final double SCAN_RADIUS = 16.0;
    private static final double SPEED = 1.2;

    private final GoalKey<Mob> key;
    private final Mob mob;
    private final MindsConfig config;

    private long lastScanAtMillis;
    private boolean isFlankerThisBout;

    PackFlankGoal(GoalKey<Mob> key, Mob mob, MindsConfig config) {
        this.key = key;
        this.mob = mob;
        this.config = config;
    }

    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }

    @Override
    public boolean shouldActivate() {
        if (!config.packTacticsEnabled) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        Location mobLocation = mob.getLocation();
        if (target == null || mobLocation == null || mobLocation.getWorld() == null) {
            return false;
        }
        Location targetLocation = target.getLocation();
        if (targetLocation == null) {
            return false;
        }
        if (mobLocation.distanceSquared(targetLocation) <= MELEE_RANGE * MELEE_RANGE) {
            // Already in melee range -- step aside and let vanilla's own attack goal take over.
            return false;
        }

        long now = System.currentTimeMillis();
        long scanIntervalMillis = config.packScanIntervalTicks * 50L;
        if (now - lastScanAtMillis >= scanIntervalMillis) {
            isFlankerThisBout = !isClosestAllySharingTarget(mobLocation, targetLocation, target);
            lastScanAtMillis = now;
        }
        return isFlankerThisBout;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        Location mobLocation = mob.getLocation();
        if (target == null || mobLocation == null) {
            return;
        }
        Location targetLocation = target.getLocation();
        if (targetLocation == null || targetLocation.getWorld() == null) {
            return;
        }

        double dx = targetLocation.getX() - mobLocation.getX();
        double dz = targetLocation.getZ() - mobLocation.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.001) {
            return;
        }
        double dirX = dx / length;
        double dirZ = dz / length;
        // Perpendicular to the mob-to-target line, so the flanker approaches from the side.
        double perpX = -dirZ;
        double perpZ = dirX;

        Location flankPoint = new Location(targetLocation.getWorld(),
                targetLocation.getX() + perpX * config.packFlankOffsetBlocks,
                targetLocation.getY(),
                targetLocation.getZ() + perpZ * config.packFlankOffsetBlocks);

        SafeMovement.moveToward(mob, flankPoint, SPEED, config);
    }

    /** True if this mob is the closest same-kind ally (targeting the same entity) to the target,
     *  or if no such ally is found nearby -- "same kind" is judged by concrete Java class rather
     *  than {@code Entity#getType()}, since this stub tree's {@code Entity#getType()} doesn't
     *  reflect a mob's actual subtype (see {@code SpawnListener}'s own note on the same issue). */
    private boolean isClosestAllySharingTarget(Location mobLocation, Location targetLocation, LivingEntity target) {
        double myDistanceSquared = mobLocation.distanceSquared(targetLocation);
        for (Entity nearby : mobLocation.getWorld().getNearbyEntities(mobLocation, SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS)) {
            if (nearby == mob || nearby.getClass() != mob.getClass() || !(nearby instanceof Mob)) {
                continue;
            }
            Mob ally = (Mob) nearby;
            if (ally.getTarget() != target) {
                continue;
            }
            Location allyLocation = ally.getLocation();
            if (allyLocation == null) {
                continue;
            }
            if (allyLocation.distanceSquared(targetLocation) < myDistanceSquared) {
                return false;
            }
        }
        return true;
    }
}
