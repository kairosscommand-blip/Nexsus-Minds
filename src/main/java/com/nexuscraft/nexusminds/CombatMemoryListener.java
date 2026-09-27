package com.nexuscraft.nexusminds;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * The moment a managed mob's grudge is actually formed: a player hitting it both starts (or
 * refreshes) that mob's grudge against them and flags it to pulse {@link CallForBackupGoal} on
 * its next tick. Deliberately does not speak here -- the "aggro" line belongs to
 * {@link GrudgeChaseGoal}'s first tick so the mob doesn't say the same line twice for one moment.
 */
final class CombatMemoryListener implements Listener {

    private final MindsConfig config;
    private final MindMemory memory;
    private final PendingCalls pendingCalls;

    CombatMemoryListener(MindsConfig config, MindMemory memory, PendingCalls pendingCalls) {
        this.config = config;
        this.memory = memory;
        this.pendingCalls = pendingCalls;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!config.enabled) {
            return;
        }
        Entity damaged = event.getEntity();
        Entity damager = event.getDamager();
        if (!(damaged instanceof Mob mob) || !(damager instanceof Player player)) {
            return;
        }

        long expiresAt = System.currentTimeMillis() + config.grudgeDurationSeconds * 1000L;
        memory.setGrudge(mob, player, expiresAt);
        pendingCalls.mark(mob);
    }
}
