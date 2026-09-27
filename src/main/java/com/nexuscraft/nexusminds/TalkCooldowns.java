package com.nexuscraft.nexusminds;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-(mob, player) pair cooldown for villager/pillager dialogue, so right-clicking the same
 * villager repeatedly doesn't spam a fresh line every time. Deliberately in-memory only, not
 * persisted -- a server restart is a perfectly reasonable moment for a villager to have "new"
 * things to say.
 */
final class TalkCooldowns {

    private final Map<String, Long> lastSpokenAtMillis = new HashMap<>();

    boolean isReady(Mob mob, Player player, int cooldownSeconds) {
        String key = mob.getUniqueId() + ":" + player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastSpokenAtMillis.get(key);
        if (last != null && now - last < cooldownSeconds * 1000L) {
            return false;
        }
        lastSpokenAtMillis.put(key, now);
        return true;
    }
}
