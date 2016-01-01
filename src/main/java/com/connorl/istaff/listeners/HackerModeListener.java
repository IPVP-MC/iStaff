package com.connorl.istaff.listeners;

import com.connorl.istaff.ISDataBaseManager;
import com.connorl.istaff.IStaff;
import com.gmail.xd.zwander.istaff.data.PlayerHackerMode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class HackerModeListener implements Listener {

    public static void set(Player player) {
        player.setFoodLevel(20);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (!target.equals(player) && !target.hasPermission("istaff.true")) {
                        target.hidePlayer(player);
                    }
                }
            }
        }.runTask(IStaff.getPlugin());
    }

    public static void unset(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    if (!target.equals(player) && !target.hasPermission("istaff.true")) {
                        target.showPlayer(player);
                    }
                }
            }
        }.runTask(IStaff.getPlugin());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        PlayerHackerMode hackerMode = ISDataBaseManager.getHackerMode(player);
        if (hackerMode != null && hackerMode.isHackerMode()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot drop items whilst in staff mode.");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            PlayerHackerMode hackerMode = ISDataBaseManager.getHackerMode((Player) entity);
            if (hackerMode != null && hackerMode.isHackerMode()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            PlayerHackerMode hackerMode = ISDataBaseManager.getHackerMode((Player) entity);
            if (hackerMode != null && hackerMode.isHackerMode()) {
                event.setCancelled(true);
            }
        }
    }
}
