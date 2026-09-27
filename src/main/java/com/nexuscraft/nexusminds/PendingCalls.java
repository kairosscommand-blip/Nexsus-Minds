package com.nexuscraft.nexusminds;

import org.bukkit.entity.Mob;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * A one-shot flag set by {@link CombatMemoryListener} the instant a managed mob is hurt by a
 * player, and consumed by {@link CallForBackupGoal} the next time that mob's goals tick. Identity
 * keyed (not equals/hashCode) since two distinct mob instances should never be conflated just
 * because a stub or future equals() override made them look equal.
 */
final class PendingCalls {

    private final Set<Mob> pending = Collections.newSetFromMap(new IdentityHashMap<>());

    void mark(Mob mob) {
        pending.add(mob);
    }

    boolean isPending(Mob mob) {
        return pending.contains(mob);
    }

    void consume(Mob mob) {
        pending.remove(mob);
    }
}
