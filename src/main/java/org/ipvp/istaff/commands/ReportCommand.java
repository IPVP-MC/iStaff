package org.ipvp.istaff.commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.ipvp.istaff.IStaff;
import org.ipvp.istaff.listeners.BungeeMessager;

public class ReportCommand implements CommandExecutor {

    private IStaff plugin;

    private int maxChars = 16;
    private long inTime = 120000;
    private int maxCount = 1;

    public ReportCommand(IStaff instance) {
        this.plugin = instance;
        maxChars = plugin.getConfig().getInt("reports.max.characters");
        inTime = plugin.getConfig().getLong("reports.max.in_time_ms");
        maxCount = plugin.getConfig().getInt("reports.max.count");
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/report <username> <reason>");
            return true;
        }

        final Player reported = plugin.getServer().getPlayer(args[0]);

        if (reported == null) {
            sender.sendMessage("That player is not online.");
            return true;
        }

        if (!reported.getName().equalsIgnoreCase(args[0])) {
            sender.sendMessage("That player is not online.");
            return true;
        }

        if (sender.getName().equals(reported.getName())) {
            sender.sendMessage(ChatColor.RED + "You can't report yourself!");
            return true;
        }

        final StringBuilder builder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            builder.append(args[i]).append(" ");
        }

        if (builder.length() > maxChars) {
            sender.sendMessage(ChatColor.RED + "Report reason cant be longer than " + maxChars + " chars!");
            return true;
        }

        // Check to make sure the given player hasn't reported the offender recently
        // insert the report into the database
        sender.sendMessage(ChatColor.GREEN + "Reporting...");
        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getIStaffDataBaseManager().hasSentRecentReports(sender, maxCount, inTime)) {
                    //sender.sendMessage(ChatColor.RED + "You have already reported " + maxCount + " player(s) in the last " + (inTime / 1000) + " seconds.");
                    sender.sendMessage(ChatColor.RED + "You have already recently reported someone.");
                    return;
                }

                plugin.getIStaffDataBaseManager().addReport(reported, sender, builder.toString());
                plugin.getIStaffDataBaseManager().closeOldReports();
                sender.sendMessage(ChatColor.GREEN + "Your report has been submitted.");
                BungeeMessager.sendAdminMessage(ChatColor.RED + "" + ChatColor.BOLD + "A new report has come in.");
            }
        }.runTaskAsynchronously(IStaff.getPlugin());
        return true;
    }
}
