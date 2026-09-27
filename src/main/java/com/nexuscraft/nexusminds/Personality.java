package com.nexuscraft.nexusminds;

/**
 * A mob's permanent temperament, rolled once at spawn and then fixed for its whole life
 * (stored in its {@link org.bukkit.persistence.PersistentDataContainer}, see {@link MindMemory}).
 * Every personality-dependent behavior in this plugin (flee threshold, whether it ever calls
 * for backup, whether it takes point in a pack) reads off this single value.
 */
public enum Personality {
    COWARD,
    BALANCED,
    AGGRESSIVE,
    ALPHA,
}
