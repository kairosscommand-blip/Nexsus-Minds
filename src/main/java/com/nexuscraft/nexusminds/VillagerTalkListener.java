package com.nexuscraft.nexusminds;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * The "rich scripted/contextual" dialogue the user asked for: right-clicking a villager or
 * pillager gets you a real, profession- (or pillager-) flavored line, sent to just you, gated by
 * {@link TalkCooldowns} so repeated right-clicks don't spam a fresh line every time.
 */
final class VillagerTalkListener implements Listener {

    private final MindsConfig config;
    private final SpeechEngine speech;

    VillagerTalkListener(MindsConfig config, SpeechEngine speech) {
        this.config = config;
        this.speech = speech;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!config.enabled) {
            return;
        }
        Entity clicked = event.getRightClicked();
        if (clicked instanceof Villager villager) {
            speech.villagerSpeak(event.getPlayer(), villager);
        } else if (clicked instanceof Pillager pillager) {
            speech.pillagerSpeak(event.getPlayer(), pillager);
        }
    }
}
