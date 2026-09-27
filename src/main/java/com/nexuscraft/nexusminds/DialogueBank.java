package com.nexuscraft.nexusminds;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * The "rich scripted/contextual" line bank the user chose over real generative-AI conversation --
 * every line a mob can ever say lives in config.yml, hand-written, no external calls, no network
 * dependency, same self-contained convention every other Nexus plugin follows. This class only
 * loads and randomly picks from those pools; it never generates text of its own.
 */
final class DialogueBank {

    private final Plugin plugin;
    private final Random random = new Random();

    private final Map<String, List<String>> hostileTemplates = new HashMap<>();
    private final Map<Villager.Profession, List<String>> villagerLines = new EnumMap<>(Villager.Profession.class);
    private List<String> pillagerLines = new ArrayList<>();

    DialogueBank(Plugin plugin) {
        this.plugin = plugin;
    }

    void load(Logger log) {
        hostileTemplates.clear();
        villagerLines.clear();
        pillagerLines = new ArrayList<>();

        ConfigurationSection dialogue = plugin.getConfig().getConfigurationSection("dialogue");
        if (dialogue == null) {
            return;
        }

        ConfigurationSection hostile = dialogue.getConfigurationSection("hostile-templates");
        if (hostile != null) {
            for (String situation : hostile.getKeys(false)) {
                List<String> lines = hostile.getStringList(situation);
                if (!lines.isEmpty()) {
                    hostileTemplates.put(situation, lines);
                }
            }
        }

        ConfigurationSection villager = dialogue.getConfigurationSection("villager");
        if (villager != null) {
            for (String key : villager.getKeys(false)) {
                Villager.Profession profession;
                try {
                    profession = Villager.Profession.valueOf(key.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warning("NexusMinds: unknown villager profession '" + key + "' in dialogue.villager, skipping");
                    continue;
                }
                List<String> lines = villager.getStringList(key);
                if (!lines.isEmpty()) {
                    villagerLines.put(profession, lines);
                }
            }
        }

        List<String> pillager = dialogue.getStringList("pillager");
        if (!pillager.isEmpty()) {
            pillagerLines = pillager;
        }
    }

    /** Null if this situation has no configured lines -- callers treat that as "say nothing". */
    String randomHostileTemplate(String situation) {
        return randomOf(hostileTemplates.get(situation));
    }

    /** Falls back to the {@code NONE} pool (config's generic small-talk lines) when the specific
     *  profession has no lines configured, so a villager never goes silent just because an admin
     *  didn't fill in every profession. */
    String randomVillagerLine(Villager.Profession profession) {
        List<String> lines = villagerLines.get(profession);
        if (lines == null || lines.isEmpty()) {
            lines = villagerLines.get(Villager.Profession.NONE);
        }
        return randomOf(lines);
    }

    String randomPillagerLine() {
        return randomOf(pillagerLines);
    }

    private String randomOf(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        return lines.get(random.nextInt(lines.size()));
    }
}
