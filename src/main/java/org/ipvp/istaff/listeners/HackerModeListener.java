package org.ipvp.istaff.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.ipvp.istaff.IStaff;
import org.ipvp.istaff.data.FauxPlayerInventory;
import org.ipvp.istaff.data.PlayerHackerMode;
import org.ipvp.istaff.data.TeleportRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class HackerModeListener implements Listener {

    public static Map<UUID, Map.Entry<Long, TeleportRequest>> teleportRequests = new HashMap<>();

    public static final Map<UUID, PlayerHackerMode> hackerModers = new HashMap<>();
    private final IStaff plugin;

    public HackerModeListener(IStaff plugin) {
        this.plugin = plugin;
    }

    public static final ExecutorService executor = Executors.newCachedThreadPool();

    private static final class JoinRunnable implements Runnable {

        private final HackerModeListener hackerModeListener;
        private final Player player;

        public JoinRunnable(HackerModeListener hackerModeListener, Player player) {
            this.hackerModeListener = hackerModeListener;
            this.player = player;
        }

        @Override
        public void run() {
            Future<PlayerHackerMode> future = executor.submit(new Callable<PlayerHackerMode>() {
                @Override
                public PlayerHackerMode call() throws Exception {
                    return IStaff.getPlugin().getIStaffDataBaseManager().getHackerMode(player.getUniqueId());
                }
            });

            PlayerHackerMode hackerMode;
            try {
                hackerMode = future.get();
            } catch (InterruptedException | ExecutionException ignored) {
                hackerMode = new PlayerHackerMode(player.getUniqueId(), false);
            }

            hackerModeListener.hackerModers.put(player.getUniqueId(), hackerMode);

            // If the player was called for a teleportation.
            Map.Entry<Long, TeleportRequest> entry = teleportRequests.remove(player.getUniqueId());
            if (entry != null) {
                TeleportRequest teleportRequest = entry.getValue();
                if (teleportRequest != null) {
                    IStaff.getPlugin().getHackerModeHandler().teleport(player, teleportRequest);
                }
            }

            IStaff.getPlugin().getHackerModeHandler().updatePlayerHackerMode(player, false, hackerMode.isHackerMode());

            // Remove PVP timer on HCF.
            player.performCommand("pvp enable");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (plugin.getBungeeMessager().getServerName() == null) {
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    plugin.getBungeeMessager().sendPluginMessage("GetServer", null);
                }
            }, 30L);
        }

        final Player player = event.getPlayer();
        if (player.hasPermission("istaff.delay")) {
            player.sendMessage(net.md_5.bungee.api.ChatColor.YELLOW + "Delaying iStaff setup to allow other plugins to modify your inventory...");
            Bukkit.getScheduler().runTaskLater(plugin, new JoinRunnable(this, player), 30L);
        } else if (player.hasPermission("istaff.true")) {
            new JoinRunnable(this, player).run();
        } else {
            // If the player has no staff permissions, just hide anyone else from staff mode.
            for (Player target : Bukkit.getOnlinePlayers()) {
                PlayerHackerMode hackerMode = hackerModers.get(target.getUniqueId());
                if (hackerMode.isHackerMode()) {
                    player.hidePlayer(target);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onItemPickup(PlayerPickupItemEvent event) {
        if (hackerModers.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (hackerModers.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        if (hackerModers.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerKick(PlayerKickEvent event) {
        hackerModers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        hackerModers.remove(event.getPlayer().getUniqueId());

        final String playerName = event.getPlayer().getName();
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getIStaffDataBaseManager().resolveAllReports(playerName);
            }
        }.runTaskAsynchronously(IStaff.getPlugin());

        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        if (inventory.contains(plugin.getHackerModeHandler().hackerTool.getItem())) {
            FauxPlayerInventory fauxPlayerInventory = plugin.getHackerModeHandler().getInventories().get(player.getUniqueId());
            if (fauxPlayerInventory != null) {
                inventory.setArmorContents(fauxPlayerInventory.armor);
                inventory.setContents(fauxPlayerInventory.inventory.getContents());
                player.updateInventory();
            } else {
                player.sendMessage(net.md_5.bungee.api.ChatColor.RED + "No local copy of your inventory to restore. " +
                        "Perhaps the server crashed? Will be restored when you rejoin.");
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (hackerModers.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot drop items whilst in staff mode.");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            if (hackerModers.containsKey(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            if (attacker != null && hackerModers.containsKey(attacker.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        HumanEntity entity = event.getEntity();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            if (hackerModers.containsKey(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
}
