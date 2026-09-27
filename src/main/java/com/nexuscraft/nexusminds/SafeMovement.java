package com.nexuscraft.nexusminds;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;

/**
 * The single helper every movement-issuing Goal in this plugin routes through instead of calling
 * {@code mob.getPathfinder().moveTo(...)} directly. It checks the one block directly ahead of the
 * mob's intended direction of travel for lava and, if found, sidesteps to a perpendicular point
 * instead. That is honestly all it does -- this is NOT a full path-safety guarantee (the
 * pathfinder this stub tree (and real Paper's own {@code Pathfinder} interface) exposes has no
 * path-preview API, so there is no way to inspect the whole route a mob is about to walk) but it
 * meaningfully cuts down on mobs walking straight into lava while fleeing or repositioning, which
 * is the actual, modest goal here.
 */
final class SafeMovement {

    private SafeMovement() {
    }

    static void moveToward(Mob mob, Location destination, double speed, MindsConfig config) {
        Location from = mob.getLocation();
        if (from == null || from.getWorld() == null || destination.getWorld() == null) {
            return;
        }

        if (config.hazardAvoidanceEnabled) {
            double dx = destination.getX() - from.getX();
            double dz = destination.getZ() - from.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length > 0.001) {
                double stepX = dx / length;
                double stepZ = dz / length;
                Block ahead = from.getWorld().getBlockAt(
                        (int) Math.floor(from.getX() + stepX),
                        from.getBlockY(),
                        (int) Math.floor(from.getZ() + stepZ));
                if (ahead.getType() == Material.LAVA) {
                    // Sidestep perpendicular to the intended direction instead of walking into it.
                    double perpX = -stepZ;
                    double perpZ = stepX;
                    destination = new Location(from.getWorld(),
                            from.getX() + perpX * 3,
                            from.getY(),
                            from.getZ() + perpZ * 3);
                }
            }
        }

        mob.getPathfinder().moveTo(destination, speed);
    }
}
