package com.nexuscraft.nexusminds;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/** Parses config.yml's behavior toggles and tuning knobs. Same "config drives defaults, code
 *  never hardcodes them" split this whole Nexus family uses (compare NexusHydro's HydroConfig).
 *  Field defaults are pre-seeded in the constructor so the plugin behaves reasonably even before
 *  {@link #load} ever runs against a real config.yml. */
final class MindsConfig {

    private final Plugin plugin;
    private final Random random = new Random();

    boolean enabled = true;

    boolean applyToZombie = true;
    boolean applyToSkeleton = true;
    boolean applyToPillager = true;

    final Map<Personality, Double> personalityWeights = new EnumMap<>(Personality.class);
    private final Map<Personality, Double> fleeHealthFraction = new EnumMap<>(Personality.class);

    double grudgeDetectionRadius = 24;
    long grudgeDurationSeconds = 1800;

    double backupCallRadius = 16;
    int backupCallMaxAllies = 4;

    boolean packTacticsEnabled = true;
    int packScanIntervalTicks = 20;
    double packFlankOffsetBlocks = 4;

    boolean hazardAvoidanceEnabled = true;

    boolean hostileSpeechEnabled = true;
    int hostileSpeechDisplaySeconds = 3;

    boolean villagerPillagerSpeechEnabled = true;
    int villagerPillagerCooldownSeconds = 15;

    MindsConfig(Plugin plugin) {
        this.plugin = plugin;

        personalityWeights.put(Personality.COWARD, 0.20);
        personalityWeights.put(Personality.BALANCED, 0.50);
        personalityWeights.put(Personality.AGGRESSIVE, 0.25);
        personalityWeights.put(Personality.ALPHA, 0.05);

        fleeHealthFraction.put(Personality.COWARD, 0.60);
        fleeHealthFraction.put(Personality.BALANCED, 0.35);
        fleeHealthFraction.put(Personality.AGGRESSIVE, 0.15);
        fleeHealthFraction.put(Personality.ALPHA, 0.0);
    }

    void load(Logger log) {
        FileConfiguration c = plugin.getConfig();

        enabled = c.getBoolean("enabled", true);

        ConfigurationSection appliesTo = c.getConfigurationSection("applies-to");
        if (appliesTo != null) {
            applyToZombie = appliesTo.getBoolean("zombie", applyToZombie);
            applyToSkeleton = appliesTo.getBoolean("skeleton", applyToSkeleton);
            applyToPillager = appliesTo.getBoolean("pillager", applyToPillager);
        }

        ConfigurationSection personality = c.getConfigurationSection("personality");
        if (personality != null) {
            loadPersonalityMap(personality.getConfigurationSection("weights"), personalityWeights, log, "personality.weights");
            loadPersonalityMap(personality.getConfigurationSection("flee-health-fraction"), fleeHealthFraction, log, "personality.flee-health-fraction");
        }

        ConfigurationSection grudge = c.getConfigurationSection("grudge");
        if (grudge != null) {
            grudgeDetectionRadius = Math.max(0, grudge.getDouble("detection-radius", grudgeDetectionRadius));
            grudgeDurationSeconds = Math.max(1, grudge.getLong("duration-seconds", grudgeDurationSeconds));
        }

        ConfigurationSection backupCall = c.getConfigurationSection("backup-call");
        if (backupCall != null) {
            backupCallRadius = Math.max(0, backupCall.getDouble("radius", backupCallRadius));
            backupCallMaxAllies = Math.max(0, backupCall.getInt("max-allies-called", backupCallMaxAllies));
        }

        ConfigurationSection packTactics = c.getConfigurationSection("pack-tactics");
        if (packTactics != null) {
            packTacticsEnabled = packTactics.getBoolean("enabled", packTacticsEnabled);
            packScanIntervalTicks = Math.max(1, packTactics.getInt("scan-interval-ticks", packScanIntervalTicks));
            packFlankOffsetBlocks = Math.max(0, packTactics.getDouble("flank-offset-blocks", packFlankOffsetBlocks));
        }

        ConfigurationSection hazardAvoidance = c.getConfigurationSection("hazard-avoidance");
        if (hazardAvoidance != null) {
            hazardAvoidanceEnabled = hazardAvoidance.getBoolean("enabled", hazardAvoidanceEnabled);
        }

        ConfigurationSection speech = c.getConfigurationSection("speech");
        if (speech != null) {
            ConfigurationSection hostile = speech.getConfigurationSection("hostile");
            if (hostile != null) {
                hostileSpeechEnabled = hostile.getBoolean("enabled", hostileSpeechEnabled);
                hostileSpeechDisplaySeconds = Math.max(1, hostile.getInt("display-seconds", hostileSpeechDisplaySeconds));
            }
            ConfigurationSection villagerPillager = speech.getConfigurationSection("villager-pillager");
            if (villagerPillager != null) {
                villagerPillagerSpeechEnabled = villagerPillager.getBoolean("enabled", villagerPillagerSpeechEnabled);
                villagerPillagerCooldownSeconds = Math.max(0, villagerPillager.getInt("cooldown-seconds", villagerPillagerCooldownSeconds));
            }
        }
    }

    /** How low (as a fraction of max health, 0.0-1.0) this personality lets a mob's health drop
     *  before it disengages and flees -- an ALPHA's 0.0 means it never flees at all. */
    double fleeFractionFor(Personality personality) {
        Double value = fleeHealthFraction.get(personality);
        return value != null ? value : 0.35;
    }

    /** A weighted random personality roll, used exactly once per mob at spawn. Falls back to
     *  BALANCED if the configured weights are missing or all zero, rather than throwing. */
    Personality rollPersonality() {
        double total = 0;
        for (double weight : personalityWeights.values()) {
            total += Math.max(0, weight);
        }
        if (total <= 0) {
            return Personality.BALANCED;
        }
        double roll = random.nextDouble() * total;
        double cumulative = 0;
        for (Map.Entry<Personality, Double> entry : personalityWeights.entrySet()) {
            cumulative += Math.max(0, entry.getValue());
            if (roll < cumulative) {
                return entry.getKey();
            }
        }
        return Personality.BALANCED;
    }

    private void loadPersonalityMap(ConfigurationSection section, Map<Personality, Double> target, Logger log, String pathForWarnings) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            Personality personality = matchPersonality(key);
            if (personality == null) {
                log.warning("NexusMinds: unknown personality '" + key + "' in " + pathForWarnings + ", skipping");
                continue;
            }
            target.put(personality, section.getDouble(key, target.getOrDefault(personality, 0.0)));
        }
    }

    private Personality matchPersonality(String name) {
        try {
            return Personality.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
