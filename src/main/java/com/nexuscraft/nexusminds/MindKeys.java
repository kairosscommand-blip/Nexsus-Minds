package com.nexuscraft.nexusminds;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * The fixed set of {@link NamespacedKey}s NexusMinds tags onto a mob's own
 * {@link org.bukkit.persistence.PersistentDataContainer}. Built once and shared -- every mob gets the
 * same keys, just different values, exactly like every other Nexus plugin's item/block PDC tagging.
 */
final class MindKeys {

    final NamespacedKey personality;
    final NamespacedKey grudgeTargetUuid;
    final NamespacedKey grudgeExpiresAtMillis;

    MindKeys(Plugin plugin) {
        this.personality = new NamespacedKey(plugin, "personality");
        this.grudgeTargetUuid = new NamespacedKey(plugin, "grudge-target-uuid");
        this.grudgeExpiresAtMillis = new NamespacedKey(plugin, "grudge-expires-at-millis");
    }
}
