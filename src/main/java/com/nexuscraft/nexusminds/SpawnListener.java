package com.nexuscraft.nexusminds;

import com.destroystokyo.paper.entity.ai.GoalKey;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;

/**
 * Fires once per real creature spawn (real Bukkit's {@code CreatureSpawnEvent} contract) --
 * exactly the right moment to roll a mob's permanent personality and install its custom Goals,
 * both done exactly once per mob for its whole life. The four {@link GoalKey}s are built once
 * here in the constructor and reused for every mob of that goal's kind: {@code MobGoals#addGoal}
 * already ties a specific goal instance to a specific mob instance, so the key only needs to
 * identify the goal's "kind," never the individual mob -- an earlier draft of this class built a
 * fresh key per mob (e.g. keyed off {@code mob.hashCode()}), which turned out to be both
 * unnecessary and a poor fit for {@code NamespacedKey}'s character rules.
 */
final class SpawnListener implements Listener {

    private final Plugin plugin;
    private final MindsConfig config;
    private final MindMemory memory;
    private final SpeechEngine speech;
    private final PendingCalls pendingCalls;

    private final GoalKey<Mob> fleeKey;
    private final GoalKey<Mob> grudgeKey;
    private final GoalKey<Mob> backupKey;
    private final GoalKey<Mob> flankKey;

    SpawnListener(Plugin plugin, MindsConfig config, MindMemory memory, SpeechEngine speech, PendingCalls pendingCalls) {
        this.plugin = plugin;
        this.config = config;
        this.memory = memory;
        this.speech = speech;
        this.pendingCalls = pendingCalls;

        this.fleeKey = GoalKey.of(Mob.class, new NamespacedKey(plugin, "flee"));
        this.grudgeKey = GoalKey.of(Mob.class, new NamespacedKey(plugin, "grudge"));
        this.backupKey = GoalKey.of(Mob.class, new NamespacedKey(plugin, "backup"));
        this.flankKey = GoalKey.of(Mob.class, new NamespacedKey(plugin, "flank"));
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (!config.enabled) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Mob mob)) {
            return;
        }
        if (!appliesTo(mob)) {
            return;
        }

        if (!memory.hasPersonality(mob)) {
            memory.setPersonality(mob, config.rollPersonality());
        }

        installGoals(mob);
    }

    private boolean appliesTo(Mob mob) {
        if (mob instanceof Zombie) {
            return config.applyToZombie;
        }
        if (mob instanceof Skeleton) {
            return config.applyToSkeleton;
        }
        if (mob instanceof Pillager) {
            return config.applyToPillager;
        }
        return false;
    }

    private void installGoals(Mob mob) {
        var mobGoals = plugin.getServer().getMobGoals();
        mobGoals.addGoal(mob, 0, new FleeAtLowHealthGoal(fleeKey, mob, memory, speech, config));
        mobGoals.addGoal(mob, 1, new GrudgeChaseGoal(grudgeKey, mob, memory, speech, config));
        mobGoals.addGoal(mob, 1, new CallForBackupGoal(backupKey, mob, memory, speech, pendingCalls, config));
        mobGoals.addGoal(mob, 2, new PackFlankGoal(flankKey, mob, config));
    }
}
