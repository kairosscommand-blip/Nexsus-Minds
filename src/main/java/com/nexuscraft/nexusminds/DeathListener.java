package com.nexuscraft.nexusminds;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/** A managed mob's dying "speech" -- its last garbled line, same {@link SpeechEngine} plumbing
 *  as every other hostile line, just triggered by death instead of combat state. */
final class DeathListener implements Listener {

    private final MindsConfig config;
    private final SpeechEngine speech;

    DeathListener(MindsConfig config, SpeechEngine speech) {
        this.config = config;
        this.speech = speech;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!config.enabled) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Mob mob)) {
            return;
        }
        speech.hostileSpeak(mob, "death");
    }
}
