package com.connorl.istaff.listeners;

import com.connorl.istaff.ISDataBaseManager;
import com.connorl.istaff.IStaff;
import com.connorl.istaff.commands.HackerCommand;
import com.gmail.xd.zwander.istaff.data.PlayerHackerMode;
import com.gmail.xd.zwander.istaff.data.TeleportRequest;
import com.gmail.xd.zwander.toolbox.tools.HackerZTool;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class ConnectionHandler implements Listener {

    public static Map<UUID, Entry<Long, TeleportRequest>> teleportRequests = new HashMap<>();

    public ConnectionHandler(Plugin plugin) {
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                Iterator<Entry<UUID, Entry<Long, TeleportRequest>>> iterator = teleportRequests.entrySet().iterator();
                long currentTime = System.currentTimeMillis();
                while (iterator.hasNext()) {
                    Entry<UUID, Entry<Long, TeleportRequest>> entry = iterator.next();
                    if (currentTime - entry.getValue().getKey() > 30000) {
                        iterator.remove();
                    }
                }
            }
        }, 60L, 60L);
    }

    public static void addTeleRequest(UUID player, TeleportRequest tr) {
        Bukkit.getLogger().info("Added telereq");
        teleportRequests.put(player, new AbstractMap.SimpleEntry<>(System.currentTimeMillis(), tr));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (BungeeMessager.getServerName() == null) {
            Bukkit.getScheduler().runTaskLater(IStaff.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    BungeeMessager.sendPluginMessage("GetServer", null);
                }
            }, 30L);
        }

        final Player player = event.getPlayer();
        if (player.hasPermission("istaff.delay")) {
            player.sendMessage(ChatColor.YELLOW + "Delaying iStaff setup to allow other plugins to modify your inventory...");
            Bukkit.getScheduler().runTaskLater(IStaff.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    Entry<Long, TeleportRequest> entry = teleportRequests.get(player.getUniqueId());
                    if (entry != null) {
                        TeleportRequest teleportRequest = entry.getValue();
                        if (teleportRequest != null) {
                            HackerCommand.updatePlayerHackerModeAndTeleport(player, teleportRequest, false);
                            player.performCommand("pvp enable");
                        } else {
                            HackerCommand.updatePlayerHackerMode(player, false);
                        }

                        teleportRequests.remove(player.getUniqueId());
                    } else {
                        HackerCommand.updatePlayerHackerMode(player, false);
                    }
                }
            }, 30L);
        } else if (player.hasPermission("istaff.true")) {
            Entry<Long, TeleportRequest> entry = teleportRequests.remove(player.getUniqueId());
            if (entry != null) {
                TeleportRequest teleportRequest = entry.getValue();
                if (teleportRequest != null) {
                    HackerCommand.updatePlayerHackerModeAndTeleport(player, teleportRequest, false);
                    player.performCommand("pvp enable");
                } else {
                    HackerCommand.updatePlayerHackerMode(player, false);
                }
            } else {
                HackerCommand.updatePlayerHackerMode(player, false);
            }
        } else {
            for (Player target : Bukkit.getOnlinePlayers()) {
                PlayerHackerMode hackerMode = ISDataBaseManager.getHackerMode(target);
                if (hackerMode != null && hackerMode.hackerMode) {
                    player.hidePlayer(target);
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final String playerName = event.getPlayer().getName();
        HackerCommand.restorePlayerInventoryOnQuit(event);
        new BukkitRunnable() {
            @Override
            public void run() {
                ISDataBaseManager.resolveAllReports(playerName);
            }
        }.runTaskAsynchronously(IStaff.getPlugin());
    }

    public static void forceTele(Player player) {
        Entry<Long, TeleportRequest> entry = teleportRequests.remove(player.getUniqueId());
        if (entry != null) {
            TeleportRequest teleportRequest = entry.getValue();
            if (teleportRequest != null) {
                teleportRequest.teleport(player);
            }
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        if (event.getPlayer().getInventory().contains(HackerZTool.getHackerItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (event.getPlayer().getInventory().contains(HackerZTool.getHackerItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.getPlayer().getInventory().contains(HackerZTool.getHackerItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player attacker = (Player) event.getDamager();
        if (attacker == null) {
            return;
        }

        if (attacker.getInventory().contains(HackerZTool.getHackerItem())) {
            event.setCancelled(true);
        }
    }
}
