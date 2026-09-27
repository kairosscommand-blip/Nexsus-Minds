package com.nexuscraft.nexusminds;

import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;

/**
 * Renders a hostile mob's garbled "speech" as a real, floating {@link TextDisplay} entity a
 * couple of blocks above it -- a real, current Paper Display-family entity type, spawned via
 * {@link org.bukkit.World#spawn(Location, Class)}. Deliberately a spatial, shared effect: anyone
 * standing near the mob sees the same text, the same way anyone would hear a monster growl.
 * That's the opposite of NexusArrival's private, per-player illusions (see that plugin's
 * ArrivalVision), and on purpose -- a monster taunting you or dying is meant to be a moment other
 * nearby players witness too, not something only you perceive. It is deliberately NOT sent to
 * global chat either, which would read as spam to players nowhere near the mob.
 */
final class TextDisplayManager {

    private static final double HEIGHT_ABOVE_MOB = 2.4;

    private final Plugin plugin;

    TextDisplayManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /** {@code TextDisplay#setText(String)} still works on real Paper but is flagged deprecated
     *  in favor of an Adventure {@code Component}-based setter. Left as the plain-String call on
     *  purpose rather than guessed at a switch to the newer overload: this method's exact real
     *  signature wasn't independently confirmed, and a wrong guess would trade a harmless
     *  deprecation warning for an actual compile failure -- the worse outcome of the two. Revisit
     *  if a future real build flags this as an error rather than a warning. */
    @SuppressWarnings("deprecation")
    void speak(Mob mob, String text, int displaySeconds) {
        Location mobLocation = mob.getLocation();
        if (mobLocation == null || mobLocation.getWorld() == null) {
            return;
        }
        Location above = mobLocation.clone().add(0, HEIGHT_ABOVE_MOB, 0);
        TextDisplay display = above.getWorld().spawn(above, TextDisplay.class);
        display.setText(text);

        long delayTicks = Math.max(1, displaySeconds) * 20L;
        plugin.getServer().getScheduler().runTaskLater(plugin, display::remove, delayTicks);
    }
}
