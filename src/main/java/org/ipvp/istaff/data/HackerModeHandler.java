package org.ipvp.istaff.data;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.ipvp.istaff.IStaff;
import org.ipvp.istaff.listeners.HackerModeListener;
import org.ipvp.istaff.toolbox.ZToolBox;
import org.ipvp.istaff.toolbox.ZToolBox.ZTool;
import org.ipvp.istaff.toolbox.tools.FreezeZTool;
import org.ipvp.istaff.toolbox.tools.HackerZTool;
import org.ipvp.istaff.toolbox.tools.InvInspectZTool;
import org.ipvp.istaff.toolbox.tools.RandomTeleZTool;
import org.ipvp.istaff.toolbox.tools.ReportZTool;
import org.ipvp.istaff.toolbox.tools.TeleporterZTool;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

public class HackerModeHandler {

    public ZTool hackerTool = new HackerZTool();
    private ZTool reportTool = new ReportZTool();
    private ZTool invInspTool = new InvInspectZTool();
    private ZTool freezeTool = new FreezeZTool();
    private ZTool randomTeleTool = new RandomTeleZTool();
    private ZTool teleTool = new TeleporterZTool();

    private final IStaff plugin;

    public HackerModeHandler(IStaff plugin) {
        this.plugin = plugin;

        ZToolBox.setTool(hackerTool);
        ZToolBox.setTool(reportTool);
        ZToolBox.setTool(invInspTool);
        ZToolBox.setTool(freezeTool);
        ZToolBox.setTool(randomTeleTool);
        ZToolBox.setTool(teleTool);
    }

    @Getter
    private final Map<UUID, FauxPlayerInventory> inventories = new HashMap<>();

    /**
     * Sets a player's Hacker Mode. This includes setting the inventory and hiding the player.
     * Runs async
     *
     * @param player player to set
     * @param on     whether to set the mode on or off. True=on, false=off;
     * @param force  whether or not to force the mode, disregarding inventory safety checks
     */
    public void setPlayerHackerMode(final Player player, final boolean on, final boolean force) {
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getIStaffDataBaseManager().setHackerMode(player.getUniqueId(), on);
                updatePlayerHackerMode(player, force, on);
            }
        }.runTaskAsynchronously(IStaff.getPlugin());
    }

    /**
     * Updates a player's hacker mode so it is consistent with the database, then teleports them to the given telerequest.
     * <br>
     * Runs async
     *
     * @param player player to update
     */
    public void teleport(final Player player, final TeleportRequest teleportRequest) {
        player.sendMessage(ChatColor.GREEN + "Preparing to teleport...");
        teleportRequest.teleport(player);
        // TODO:
        // player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "You are reviewing " +
        //        teleportRequest.targetName + " for " + teleportRequest.reason + ".");
    }

    /**
     * Restores pre hacker mode inventory to the given player on quit.
     */
    public void restoreAllPlayerInventories() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerInventory inventory = player.getInventory();
            if (inventory.contains(hackerTool.getItem())) {
                FauxPlayerInventory fauxPlayerInventory = inventories.get(player.getUniqueId());
                if (fauxPlayerInventory != null) {
                    inventory.setArmorContents(fauxPlayerInventory.armor);
                    inventory.setContents(fauxPlayerInventory.inventory.getContents());
                    player.updateInventory();
                } else {
                    player.sendMessage(ChatColor.RED +
                            "No local copy of your inventory to restore. " +
                            "Perhaps the server crashed? " +
                            "Will be restored when you rejoin.");
                }
            }
        }
    }

    public void forceClearSavedInventory(final Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                PlayerInventory inventory = player.getInventory();
                inventory.clear();
                inventory.setArmorContents(new ItemStack[]{null, null, null, null});
                try {
                    plugin.getIStaffDataBaseManager().savePlayerInventory(player);
                } catch (TimeoutException ignored) {
                }

                inventories.put(player.getUniqueId(), new FauxPlayerInventory(inventory));
            }
        }.runTaskAsynchronously(IStaff.getPlugin());
    }

    public void updatePlayerHackerMode(final Player player, boolean force, boolean on) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.equals(player) && !target.hasPermission("istaff.true")) {
                if (on) {
                    target.hidePlayer(player);
                } else {
                    target.showPlayer(player);
                }
            }
        }

        if (on) {
            player.setFoodLevel(20);
            player.setAllowFlight(true);
            player.sendMessage(ChatColor.YELLOW + "Staff Mode:" + ChatColor.GREEN + " Enabled");
        } else {
            if (player.getGameMode() != GameMode.CREATIVE) {
                player.setAllowFlight(false);
            }

            player.sendMessage(ChatColor.YELLOW + "Staff Mode: " + ChatColor.RED + "Disabled");
        }

        setInventory(player, on, force);
    }


    private void setInventory(Player player, boolean on, boolean force) {
        PlayerInventory inventory = player.getInventory();
        if (on) {
            if (inventory.contains(hackerTool.getItem())) {
                player.sendMessage(ChatColor.RED + "It seems like you are already in hacker mode...");
            } else {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            plugin.getIStaffDataBaseManager().savePlayerInventory(player);
                        } catch (TimeoutException ex) {
                            player.sendMessage(ChatColor.RED +
                                    "Your inventory could not be saved to the database. " +
                                    "To prevent inventory loss, it is recommended you exit hacker mode as soon as possible using /h");
                        }
                    }
                }.runTaskAsynchronously(plugin);

                inventories.put(player.getUniqueId(), new FauxPlayerInventory(player.getInventory()));
            }

            inventory.clear();
            inventory.setItem(0, teleTool.getItem());
            inventory.setItem(2, freezeTool.getItem());
            inventory.setItem(4, reportTool.getItem());
            inventory.setItem(6, randomTeleTool.getItem());
            inventory.setItem(8, invInspTool.getItem());
            inventory.setItem(9, hackerTool.getItem());
            inventory.setArmorContents(new ItemStack[]{null, null, null, null});
            player.updateInventory();
        } else {
            if (!inventory.contains(hackerTool.getItem())) {
                if (!force) {
                    return;
                }
            }

            FauxPlayerInventory fauxPlayerInventory = inventories.get(player.getUniqueId());
            if (fauxPlayerInventory == null) {
                player.sendMessage(ChatColor.RED + "No local copy of your inventory to restore. " +
                        "Perhaps the server crashed? Restoring from backup database.");

                Future<FauxPlayerInventory> future = HackerModeListener.executor.submit(new Callable<FauxPlayerInventory>() {
                    @Override
                    public FauxPlayerInventory call() throws Exception {
                        try {
                            if (IStaffDataBaseManager.DEBUG) {
                                return null;
                            }

                            return plugin.getIStaffDataBaseManager().loadPlayerInventory(player);
                        } catch (TimeoutException ex) {
                            player.sendMessage("Could not load inventory from database. " +
                                    "This is due to misconfiguration of the server. " +
                                    "Your inventory is still likely saved in the database. " +
                                    "To prevent loss of inventory, " +
                                    "DO NOT attempt to enter hacker mode on ANY server. " +
                                    "Contact the plugin developer to resolve.");

                            return null;
                        }
                    }
                });

                try {
                    fauxPlayerInventory = future.get();
                } catch (InterruptedException | ExecutionException ignored) {
                }
            }

            if (fauxPlayerInventory != null) {
                inventory.setArmorContents(fauxPlayerInventory.armor);
                inventory.setContents(fauxPlayerInventory.inventory.getContents());
                player.updateInventory();
            }
        }
    }
}
