package com.nexuscraft.nexusminds;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.UUID;

/**
 * The persistent-memory half of the mob AI overhaul: a mob that has been hurt by a specific
 * player keeps pursuing that player on sight later, well beyond vanilla's own short-lived aggro
 * range, until its grudge (see {@link MindMemory#grudgeTarget}) expires. Yields to
 * {@link FleeAtLowHealthGoal} whenever this mob's health is already below its flee threshold --
 * a cornered, badly hurt mob runs, it doesn't press a losing fight just because it recognizes you.
 */
final class GrudgeChaseGoal implements Goal<Mob> {

    private static final double SPEED = 1.15;

    private final GoalKey<Mob> key;
    private final Mob mob;
    private final MindMemory memory;
    private final SpeechEngine speech;
    private final MindsConfig config;

    private boolean spokenThisBout;

    GrudgeChaseGoal(GoalKey<Mob> key, Mob mob, MindMemory memory, SpeechEngine speech, MindsConfig config) {
        this.key = key;
        this.mob = mob;
        this.memory = memory;
        this.speech = speech;
        this.config = config;
    }

    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.TARGET);
    }

    @Override
    public boolean shouldActivate() {
        double fraction = mob.getHealth() / 20.0;
        if (fraction <= config.fleeFractionFor(memory.personalityOf(mob))) {
            return false;
        }
        return resolveGrudgeTarget() != null;
    }

    @Override
    public void start() {
        spokenThisBout = false;
    }

    @Override
    public void stop() {
        spokenThisBout = false;
    }

    @Override
    public void tick() {
        Player target = resolveGrudgeTarget();
        if (target == null) {
            return;
        }
        mob.setTarget(target);
        if (!spokenThisBout) {
            speech.hostileSpeak(mob, "aggro");
            spokenThisBout = true;
        }
        SafeMovement.moveToward(mob, target.getLocation(), SPEED, config);
    }

    /** Null unless the grudge is still live, the player is online, in the same world, and within
     *  {@link MindsConfig#grudgeDetectionRadius} of the mob -- a grudge doesn't grant a mob
     *  omniscience, only a longer memory than vanilla's transient aggro. */
    private Player resolveGrudgeTarget() {
        UUID targetUuid = memory.grudgeTarget(mob);
        if (targetUuid == null) {
            return null;
        }
        Player player = Bukkit.getPlayer(targetUuid);
        if (player == null || !player.isOnline()) {
            return null;
        }
        Location mobLocation = mob.getLocation();
        Location playerLocation = player.getLocation();
        if (mobLocation == null || playerLocation == null || mobLocation.getWorld() == null || playerLocation.getWorld() == null) {
            return null;
        }
        if (!mobLocation.getWorld().getName().equals(playerLocation.getWorld().getName())) {
            return null;
        }
        double radius = config.grudgeDetectionRadius;
        if (mobLocation.distanceSquared(playerLocation) > radius * radius) {
            return null;
        }
        return player;
    }
}
