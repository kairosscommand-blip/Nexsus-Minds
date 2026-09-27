package com.nexuscraft.nexusminds;

import org.bukkit.plugin.java.JavaPlugin;

/** Mob AI + speech overhaul -- see README.md for the full design and reasoning. Built on Paper's
 *  real {@code Goal}/{@code Pathfinder}/{@code MobGoals} API (a genuine, intended extension
 *  point, unlike redstone which hits a hard NMS wall this plugin family deliberately stays clear
 *  of) and a rich, config-driven, self-contained dialogue system -- no external AI/network calls,
 *  same standalone-plugin convention as every other Nexus plugin. */
public final class NexusMindsPlugin extends JavaPlugin {

    private MindsConfig config;
    private MindKeys keys;
    private MindMemory memory;
    private DialogueBank dialogue;
    private PendingCalls pendingCalls;
    private SpeechEngine speech;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        config = new MindsConfig(this);
        config.load(getLogger());

        keys = new MindKeys(this);
        memory = new MindMemory(keys);

        dialogue = new DialogueBank(this);
        dialogue.load(getLogger());

        pendingCalls = new PendingCalls();

        speech = new SpeechEngine(config, dialogue, new GarbleTransformer(),
                new TextDisplayManager(this), new TalkCooldowns());

        getServer().getPluginManager().registerEvents(
                new SpawnListener(this, config, memory, speech, pendingCalls), this);
        getServer().getPluginManager().registerEvents(
                new CombatMemoryListener(config, memory, pendingCalls), this);
        getServer().getPluginManager().registerEvents(
                new DeathListener(config, speech), this);
        getServer().getPluginManager().registerEvents(
                new VillagerTalkListener(config, speech), this);

        getCommand("nexusminds").setExecutor(new MindsCommand(this::reload));

        getLogger().info("NexusMinds enabled -- applies to zombie:" + config.applyToZombie
                + " skeleton:" + config.applyToSkeleton + " pillager:" + config.applyToPillager
                + ", pack tactics " + (config.packTacticsEnabled ? "on" : "off") + ".");
    }

    @Override
    public void onDisable() {
        getLogger().info("NexusMinds disabled.");
    }

    private void reload() {
        reloadConfig();
        config.load(getLogger());
        dialogue.load(getLogger());
    }
}
