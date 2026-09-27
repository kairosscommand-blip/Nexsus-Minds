package com.nexuscraft.nexusminds;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** {@code /nexusminds reload} -- admin-only, double-checked here as well as in plugin.yml's
 *  permission default, same belt-and-suspenders convention as every other Nexus command. */
final class MindsCommand implements CommandExecutor {

    private final Runnable reload;

    MindsCommand(Runnable reload) {
        this.reload = reload;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nexusminds.admin")) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reload.run();
            sender.sendMessage("§aNexusMinds reloaded.");
            return true;
        }
        sender.sendMessage("§7Usage: /nexusminds reload");
        return true;
    }
}
