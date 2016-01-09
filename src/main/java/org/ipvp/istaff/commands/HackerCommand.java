package org.ipvp.istaff.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.ipvp.istaff.IStaff;
import org.ipvp.istaff.data.PlayerHackerMode;
import org.ipvp.istaff.listeners.HackerModeListener;

public class HackerCommand implements CommandExecutor {

    private final IStaff plugin;

    public HackerCommand(IStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can execute that command!");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            PlayerHackerMode playerHackerMode = HackerModeListener.hackerModers.get(player.getUniqueId());
            boolean on = playerHackerMode.isHackerMode();

            playerHackerMode.hackerMode = !on;
            plugin.getHackerModeHandler().updatePlayerHackerMode(player, false, playerHackerMode.hackerMode);

            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getIStaffDataBaseManager().setHackerMode(player.getUniqueId(), playerHackerMode.hackerMode);
                }
            }.runTaskAsynchronously(plugin);
            return true;
        }

        if (args[0].equalsIgnoreCase("forceoff")) {
            plugin.getHackerModeHandler().setPlayerHackerMode(player, false, true);
        } else if (args[0].equalsIgnoreCase("forceclear")) {
            plugin.getHackerModeHandler().forceClearSavedInventory(player);
        }

        return true;
    }
}
