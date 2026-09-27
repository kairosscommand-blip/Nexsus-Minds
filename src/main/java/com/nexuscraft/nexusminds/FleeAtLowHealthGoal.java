package com.nexuscraft.nexusminds;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.util.EnumSet;

/**
 * Personality-dependent panic response: once a mob's health fraction drops at or below its
 * personality's flee threshold (an ALPHA never flees; a COWARD flees earliest, see
 * {@link MindsConfig#fleeFractionFor}), it disengages and retreats away from whatever it's
 * currently targeting instead of trading hits to the death like vanilla's melee goal would.
 */
final class FleeAtLowHealthGoal implements Goal<Mob> {

    private static final double RETREAT_DISTANCE = 8.0;
    private static final double SPEED = 1.3;

    private final GoalKey<Mob> key;
    private final Mob mob;
    private final MindMemory memory;
    private final SpeechEngine speech;
    private final MindsConfig config;

    private boolean spokenThisBout;

    FleeAtLowHealthGoal(GoalKey<Mob> key, Mob mob, MindMemory memory, SpeechEngine speech, MindsConfig config) {
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
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return false;
        }
        double fraction = mob.getHealth() / 20.0;
        return fraction <= config.fleeFractionFor(memory.personalityOf(mob));
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
        LivingEntity target = mob.getTarget();
        Location mobLocation = mob.getLocation();
        if (target == null || mobLocation == null) {
            return;
        }
        Location threatLocation = target.getLocation();
        if (threatLocation == null || threatLocation.getWorld() == null) {
            return;
        }

        double dx = mobLocation.getX() - threatLocation.getX();
        double dz = mobLocation.getZ() - threatLocation.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.001) {
            // Directly on top of the threat -- pick an arbitrary retreat direction.
            dx = 1;
            dz = 0;
            length = 1;
        }

        Location retreat = new Location(mobLocation.getWorld(),
                mobLocation.getX() + (dx / length) * RETREAT_DISTANCE,
                mobLocation.getY(),
                mobLocation.getZ() + (dz / length) * RETREAT_DISTANCE);

        if (!spokenThisBout) {
            speech.hostileSpeak(mob, "panic");
            spokenThisBout = true;
        }

        SafeMovement.moveToward(mob, retreat, SPEED, config);
    }
}
