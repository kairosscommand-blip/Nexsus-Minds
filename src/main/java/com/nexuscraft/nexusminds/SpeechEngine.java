package com.nexuscraft.nexusminds;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

/**
 * Ties the dialogue bank, the garble transformer, the floating-text renderer, and the talk
 * cooldowns together into the three moments this plugin ever puts words on a mob: a hostile mob
 * garbling a line above its head, a villager speaking a profession-flavored line to whoever
 * right-clicked it, and a pillager doing the same with its own flat line pool.
 */
final class SpeechEngine {

    private final MindsConfig config;
    private final DialogueBank dialogue;
    private final GarbleTransformer garbler;
    private final TextDisplayManager displays;
    private final TalkCooldowns cooldowns;

    SpeechEngine(MindsConfig config, DialogueBank dialogue, GarbleTransformer garbler,
                 TextDisplayManager displays, TalkCooldowns cooldowns) {
        this.config = config;
        this.dialogue = dialogue;
        this.garbler = garbler;
        this.displays = displays;
        this.cooldowns = cooldowns;
    }

    void hostileSpeak(Mob mob, String situation) {
        if (!config.hostileSpeechEnabled) {
            return;
        }
        String template = dialogue.randomHostileTemplate(situation);
        if (template == null) {
            return;
        }
        displays.speak(mob, garbler.garble(template), config.hostileSpeechDisplaySeconds);
    }

    void villagerSpeak(Player player, Villager villager) {
        if (!config.villagerPillagerSpeechEnabled || !cooldowns.isReady(villager, player, config.villagerPillagerCooldownSeconds)) {
            return;
        }
        String line = dialogue.randomVillagerLine(villager.getProfession());
        if (line == null) {
            return;
        }
        player.sendMessage("§e" + professionLabel(villager.getProfession()) + "§7: §f" + line);
    }

    void pillagerSpeak(Player player, Pillager pillager) {
        if (!config.villagerPillagerSpeechEnabled || !cooldowns.isReady(pillager, player, config.villagerPillagerCooldownSeconds)) {
            return;
        }
        String line = dialogue.randomPillagerLine();
        if (line == null) {
            return;
        }
        player.sendMessage("§cPillager§7: §f" + line);
    }

    private String professionLabel(Villager.Profession profession) {
        String name = profession.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
