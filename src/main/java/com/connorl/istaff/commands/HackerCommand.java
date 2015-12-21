package com.connorl.istaff.commands;

import com.connorl.istaff.Callback;
import com.connorl.istaff.ISDataBaseManager;
import com.connorl.istaff.IStaff;
import com.connorl.istaff.listeners.HackerModeListener;
import com.gmail.xd.zwander.istaff.data.FauxPlayerInventory;
import com.gmail.xd.zwander.istaff.data.PlayerHackerMode;
import com.gmail.xd.zwander.istaff.data.TeleportRequest;
import com.gmail.xd.zwander.toolbox.ZToolBox;
import com.gmail.xd.zwander.toolbox.ZToolBox.ZTool;
import com.gmail.xd.zwander.toolbox.tools.FreezeZTool;
import com.gmail.xd.zwander.toolbox.tools.HackerZTool;
import com.gmail.xd.zwander.toolbox.tools.InvInspectZTool;
import com.gmail.xd.zwander.toolbox.tools.RandomTeleZTool;
import com.gmail.xd.zwander.toolbox.tools.ReportZTool;
import com.gmail.xd.zwander.toolbox.tools.TeleporterZTool;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Created by Zwander on 24/11/2015.
 */
public class HackerCommand implements CommandExecutor {

    private static ZTool hackerTool = new HackerZTool();
    private static ZTool reportTool = new ReportZTool();
    private static ZTool invInspTool = new InvInspectZTool();
    private static ZTool freezeTool = new FreezeZTool();
    private static ZTool randomTeleTool = new RandomTeleZTool();
    private static ZTool teleTool = new TeleporterZTool();

    static {
        ZToolBox.setTool(hackerTool);
        ZToolBox.setTool(reportTool);
        ZToolBox.setTool(invInspTool);
        ZToolBox.setTool(freezeTool);
        ZToolBox.setTool(randomTeleTool);
        ZToolBox.setTool(teleTool);
    }

    private static Map<UUID, FauxPlayerInventory> inventories = new HashMap<>();

    public HackerCommand(IStaff instance) {
        Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(instance, new Runnable() {
            @Override
            public void run() {
                Iterator<Callback> iterator = sl.iterator();
                while (iterator.hasNext()) {
                    iterator.next().run();
                    iterator.remove();
                }
            }
        }, 1L, 1L);
    }

    /**
     * Toggles a player's Hacker Mode. Runs async.
     * <br>
     * Does not force mode if inventory safety checks fail.
     *
     * @param player player to toggle Hacker Mode on
     */
    public static void togglePlayerHackerMode(final Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                PlayerHackerMode playerHackerMode = ISDataBaseManager.getHackerMode(player);
                boolean on = playerHackerMode != null && playerHackerMode.hackerMode;
                setPlayerHackerModeSync(player, !on);
                updatePlayerHackerModeSync(player, false);
            }
        }.runTaskAsynchronously(IStaff.getPlugin());
    }

    /**
     * Sets a player's Hacker Mode. This includes setting the inventory and hiding the player.
     * Runs async
     *
     * @param player player to set
     * @param on     whether to set the mode on or off. True=on, false=off;
     * @param force  whether or not to force the mode, disregarding inventory safety checks
     */
    public static void setPlayerHackerMode(final Player player, final boolean on, final boolean force) {
        new BukkitRunnable() {
            @Override
            public void run() {
                setPlayerHackerModeSync(player, on);
                updatePlayerHackerModeSync(player, force);
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
    public static void updatePlayerHackerModeAndTeleport(final Player player, final TeleportRequest teleportRequest, final boolean force) {
        player.sendMessage(ChatColor.GREEN + "Preparing to teleport...");
        new BukkitRunnable() {
            @Override
            public void run() {
                updatePlayerHackerModeSync(player, force);
                sl.add(new Callback() {
                    @Override
                    public void run() {
                        teleportRequest.teleport(player);
                        //TODO:
                        //player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "You are reviewing " +
                        //        teleportRequest.targetName + " for " + teleportRequest.reason + ".");
                    }
                });
            }
        }.runTaskAsynchronously(IStaff.getPlugin());
    }

    /**
     * Updates a player's hacker mode so it is consistent with the database.
     * <br>
     * Runs async
     *
     * @param player player to update
     */
    public static void updatePlayerHackerMode(final Player player, final boolean force) {
        new BukkitRunnable() {
            @Override
            public void run() {
                updatePlayerHackerModeSync(player, force);
            }
        }.runTaskAsynchronously(IStaff.getPlugin());
    }

    /**
     * Restores pre hacker mode inventory to the given player on quit.
     *
     * @param event Quit event
     */
    public static void restorePlayerInventoryOnQuit(final PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        if (inventory.contains(hackerTool.getItem())) {
            FauxPlayerInventory fauxPlayerInventory = inventories.get(player.getUniqueId());
            if (fauxPlayerInventory == null) {
                player.sendMessage(ChatColor.RED + "No local copy of your inventory to restore. Perhaps the server crashed? Will be restored when you rejoin.");
            } else {
                inventory.setArmorContents(fauxPlayerInventory.armor);
                inventory.setContents(fauxPlayerInventory.inventory.getContents());
            }

            player.updateInventory();
        }
    }

    /**
     * Restores pre hacker mode inventory to the given player on quit.
     */
    public static void restoreAllPlayerInventories() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerInventory inventory = player.getInventory();
            if (inventory.contains(hackerTool.getItem())) {
                FauxPlayerInventory fauxPlayerInventory = inventories.get(player.getUniqueId());
                if (fauxPlayerInventory == null) {
                    player.sendMessage(ChatColor.RED + "No local copy of your inventory to restore. Perhaps the server crashed? Will be restored when you rejoin.");
                } else {
                    inventory.setArmorContents(fauxPlayerInventory.armor);
                    inventory.setContents(fauxPlayerInventory.inventory.getContents());
                }

                player.updateInventory();
            }
        }
    }

    public static void forceClearSavedInventory(final Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                PlayerInventory inventory = player.getInventory();
                inventory.clear();
                inventory.setArmorContents(new ItemStack[]{null, null, null, null});
                try {
                    ISDataBaseManager.savePlayerInventory(player);
                } catch (TimeoutException ignored) {
                }

                FauxPlayerInventory inv = new FauxPlayerInventory(inventory);
                inventories.put(player.getUniqueId(), inv);
            }
        }.runTaskAsynchronously(IStaff.getPlugin());
    }

    private static List<Callback> sl = new ArrayList<>();

    private static void updatePlayerHackerModeSync(final Player player, boolean force) {
        PlayerHackerMode playerHackerMode = ISDataBaseManager.getHackerMode(player);
        final boolean on = playerHackerMode != null && playerHackerMode.hackerMode;
        sl.add(new Callback() {
            @Override
            public void run() {
                ISDataBaseManager.setHackerMode(player, on);
                if (on) {
                    HackerModeListener.set(player);
                    player.setAllowFlight(true);
                    player.sendMessage(ChatColor.YELLOW + "Staff Mode:" + ChatColor.GREEN + " Enabled");
                } else {
                    HackerModeListener.unset(player);
                    if (player.getGameMode() == GameMode.ADVENTURE || player.getGameMode() == GameMode.SURVIVAL) {
                        player.setAllowFlight(false);
                    }

                    player.sendMessage(ChatColor.YELLOW + "Staff Mode: " + ChatColor.RED + "Disabled");
                }
            }
        });

        setPlayerInvSync(player, on, force);
    }

    private static void setPlayerHackerModeSync(Player player, boolean on) {
        if (player != null) {
            ISDataBaseManager.setHackerMode(player, on);
        }
    }

    private static void setPlayerInvSync(Player player, boolean on, boolean force) {
        if (player == null) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        if (on) {
            if (!inventory.contains(hackerTool.getItem())) {
                try {
                    ISDataBaseManager.savePlayerInventory(player);
                } catch (TimeoutException ex) {
                    player.sendMessage(ChatColor.RED + "Your inventory could not be saved to the database. " +
                            "To prevent inventory loss, it is recommended you exit hacker mode as soon as possible using /h");
                }

                FauxPlayerInventory fauxPlayerInventory = new FauxPlayerInventory(player.getInventory());
                inventories.put(player.getUniqueId(), fauxPlayerInventory);
            } else {
                player.sendMessage(ChatColor.RED + "It seems like you are already in hacker mode...");
                //player.sendMessage(ChatColor.RED+"Something has tried to set you to hacker mode. " +
                //        "As you already appear to be in hacker mode, we have not overwritten your saved inventory.");
            }

            inventory.clear();
            inventory.setItem(9, hackerTool.getItem());
            inventory.setItem(4, reportTool.getItem());
            inventory.setItem(8, invInspTool.getItem());
            inventory.setItem(0, teleTool.getItem());
            inventory.setItem(6, randomTeleTool.getItem());
            inventory.setItem(2, freezeTool.getItem());
            inventory.setArmorContents(new ItemStack[]{null, null, null, null});

        } else {
            if (!inventory.contains(hackerTool.getItem())) {
                if (!force) {
                    /*player.sendMessage(ChatColor.RED+"No \"Hacker Mode Enabled\" item found while trying to DISABLE hacker mode. This item is a safeguard against accidental inventory whiping. If this is a mistake, type /h forceoff.");
                    player.sendMessage(ChatColor.RED+"If you were trying to "+ChatColor.GREEN+"ENABLE"+ChatColor.RED+" hacker mode, please run /h again. If the error persists, contact the plugin developer.");*/
                    return;
                }
            }

            FauxPlayerInventory fauxPlayerInventory1 = inventories.get(player.getUniqueId());
            if (fauxPlayerInventory1 == null) {
                player.sendMessage(ChatColor.RED + "No local copy of your inventory to restore. Perhaps the server crashed? Restoring from backup database.");
                FauxPlayerInventory fauxPlayerInventory;
                try {
                    fauxPlayerInventory = ISDataBaseManager.loadPlayerInventory(player);
                    if (fauxPlayerInventory == null) {
                        return;
                    }

                    inventory.setArmorContents(fauxPlayerInventory.armor);
                    inventory.setContents(fauxPlayerInventory.inventory.getContents());
                } catch (TimeoutException ex) {
                    player.sendMessage("Could not load inventory from database. This is due to misconfiguration of the server. Your inventory is still likely saved in the database. To prevent loss of inventory, DO NOT attempt to enter hacker mode on ANY server. Contact the plugin developer to resolve.");
                }
            } else {
                inventory.setArmorContents(fauxPlayerInventory1.armor);
                inventory.setContents(fauxPlayerInventory1.inventory.getContents());
            }
        }

        player.updateInventory();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can execute that command!");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            togglePlayerHackerMode((Player) sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("forceoff")) {
            setPlayerHackerMode(player, false, true);
        } else if (args[0].equalsIgnoreCase("forceclear")) {
            forceClearSavedInventory(player);
        }

        return true;
    }
}
