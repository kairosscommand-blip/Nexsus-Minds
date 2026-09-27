package com.nexuscraft.nexusminds;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A one-shot "pulse" goal: it does nothing until {@link CombatMemoryListener} flags this mob via
 * {@link PendingCalls} the instant a player hurts it, then on its single following tick it calls
 * nearby same-kind allies in to help, copying its own grudge onto them, and clears the flag so it
 * goes dormant again until the next hit. "Same kind" is judged by concrete Java class rather than
 * {@code Entity#getType()} -- documented stub limitation, see {@code SpawnListener}'s own note.
 */
final class CallForBackupGoal implements Goal<Mob> {

    private final GoalKey<Mob> key;
    private final Mob mob;
    private final MindMemory memory;
    private final SpeechEngine speech;
    private final PendingCalls pendingCalls;
    private final MindsConfig config;

    CallForBackupGoal(GoalKey<Mob> key, Mob mob, MindMemory memory, SpeechEngine speech,
                       PendingCalls pendingCalls, MindsConfig config) {
        this.key = key;
        this.mob = mob;
        this.memory = memory;
        this.speech = speech;
        this.pendingCalls = pendingCalls;
        this.config = config;
    }

    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET);
    }

    @Override
    public boolean shouldActivate() {
        return pendingCalls.isPending(mob);
    }

    @Override
    public void tick() {
        pendingCalls.consume(mob);

        UUID attackerUuid = memory.grudgeTarget(mob);
        if (attackerUuid == null) {
            return;
        }
        Player attacker = Bukkit.getPlayer(attackerUuid);
        if (attacker == null || !attacker.isOnline()) {
            return;
        }

        Location center = mob.getLocation();
        if (center == null || center.getWorld() == null) {
            return;
        }

        double radius = config.backupCallRadius;
        long expiresAt = System.currentTimeMillis() + config.grudgeDurationSeconds * 1000L;

        int called = 0;
        for (Entity nearby : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (called >= config.backupCallMaxAllies) {
                break;
            }
            if (nearby == mob || nearby.getClass() != mob.getClass() || !(nearby instanceof Mob)) {
                continue;
            }
            Mob ally = (Mob) nearby;
            if (ally.getTarget() != null) {
                continue;
            }
            ally.setTarget(attacker);
            memory.setGrudge(ally, attacker, expiresAt);
            called++;
        }

        if (called > 0) {
            speech.hostileSpeak(mob, "backup-call");
        }
    }
}
