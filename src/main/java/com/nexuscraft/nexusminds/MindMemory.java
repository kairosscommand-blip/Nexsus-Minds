package com.nexuscraft.nexusminds;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Reads and writes a mob's permanent personality and its persistent grudge onto its own
 * {@link PersistentDataContainer} -- the same PDC-tagging convention this whole plugin family
 * already uses for items and blocks, applied here to a living entity. On a real server this
 * data travels with the mob's own saved NBT and survives a restart; nothing here is stored in a
 * separate file.
 */
final class MindMemory {

    private final MindKeys keys;

    MindMemory(MindKeys keys) {
        this.keys = keys;
    }

    boolean hasPersonality(Mob mob) {
        return mob.getPersistentDataContainer().has(keys.personality, PersistentDataType.STRING);
    }

    Personality personalityOf(Mob mob) {
        PersistentDataContainer pdc = mob.getPersistentDataContainer();
        String raw = pdc.get(keys.personality, PersistentDataType.STRING);
        if (raw == null) {
            return Personality.BALANCED;
        }
        try {
            return Personality.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return Personality.BALANCED;
        }
    }

    void setPersonality(Mob mob, Personality personality) {
        mob.getPersistentDataContainer().set(keys.personality, PersistentDataType.STRING, personality.name());
    }

    void setGrudge(Mob mob, Player attacker, long expiresAtMillis) {
        PersistentDataContainer pdc = mob.getPersistentDataContainer();
        pdc.set(keys.grudgeTargetUuid, PersistentDataType.STRING, attacker.getUniqueId().toString());
        pdc.set(keys.grudgeExpiresAtMillis, PersistentDataType.LONG, expiresAtMillis);
    }

    /**
     * The UUID of the player this mob still holds a grudge against, or {@code null} if it has
     * none, the grudge has expired, or the stored data is unreadable. Expiry is checked here so
     * every caller gets the same "is this grudge still live" answer without repeating the logic.
     */
    UUID grudgeTarget(Mob mob) {
        PersistentDataContainer pdc = mob.getPersistentDataContainer();
        String rawUuid = pdc.get(keys.grudgeTargetUuid, PersistentDataType.STRING);
        if (rawUuid == null) {
            return null;
        }
        Long expiresAt = pdc.get(keys.grudgeExpiresAtMillis, PersistentDataType.LONG);
        if (expiresAt == null || System.currentTimeMillis() >= expiresAt) {
            return null;
        }
        try {
            return UUID.fromString(rawUuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
