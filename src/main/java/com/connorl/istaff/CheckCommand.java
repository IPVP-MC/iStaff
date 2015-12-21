package com.connorl.istaff;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class CheckCommand implements CommandExecutor {

    private final IStaff plugin;

    public CheckCommand(IStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, final String[] args) {
        if (args.length == 0) {
            return false;
        }

        sender.sendMessage(ChatColor.GREEN + "Loading stats...");
        new BukkitRunnable() {
            @Override
            public void run() {
                sender.sendMessage(ChatColor.GOLD + "Showing reports stats for player " + ChatColor.AQUA + args[0]);
                sender.sendMessage(ChatColor.GOLD + "Total resolved: " + ChatColor.AQUA + ISDataBaseManager.getTotalResolvedReports(args[0]));
                sender.sendMessage(ChatColor.GOLD + "Month resolved: " + ChatColor.AQUA + ISDataBaseManager.getMonthResolvedReports(args[0]));
                sender.sendMessage(ChatColor.GOLD + "Week resolved: " + ChatColor.AQUA + ISDataBaseManager.getWeekResolvedReports(args[0]));
            }
        }.runTaskAsynchronously(plugin);
        return true;
    }
}
