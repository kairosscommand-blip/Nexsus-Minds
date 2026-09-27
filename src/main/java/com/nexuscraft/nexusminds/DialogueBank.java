package com.nexuscraft.nexusminds;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
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
    /** Deliberately a plain HashMap, not an EnumMap -- real Bukkit's {@code Villager.Profession}
     *  is NOT actually a Java {@code enum} (it implements {@code OldEnum} for datapack-driven
     *  extensibility, same as several other former-enum registry types in current Paper API), so
     *  {@code EnumMap<Villager.Profession, ...>} fails to compile against the real server jar
     *  even though it type-checks fine against this project's own simplified sandbox stub, which
     *  still models Profession as a plain enum. Caught by an actual `mvn` build against real
     *  paper-api; see CHANGES.md. */
    private final Map<Villager.Profession, List<String>> villagerLines = new HashMap<>();
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
                // Villager.Profession.valueOf(String) still exists but is deprecated and marked
                // for removal on real Bukkit -- Registry lookup by NamespacedKey is the current,
                // non-deprecated way to resolve a registry-backed constant like this one (same
                // reasoning as Sound/Enchantment elsewhere in this plugin family).
                Villager.Profession profession = Registry.VILLAGER_PROFESSION.get(NamespacedKey.minecraft(key.toLowerCase()));
                if (profession == null) {
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
