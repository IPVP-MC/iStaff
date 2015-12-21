package com.gmail.xd.zwander.toolbox.tools;

import com.connorl.istaff.IStaff;
import com.gmail.xd.zwander.menu.Menu;
import com.gmail.xd.zwander.menu.Menu.CloseAction;
import com.gmail.xd.zwander.menu.Menu.ItemAction;
import com.gmail.xd.zwander.toolbox.BasicZTool;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class InvInspectZTool extends BasicZTool {

    private static ItemStack item;

    static {
        item = new ItemStack(Material.BLAZE_ROD, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Inspect Player Inventory");
        meta.setLore(Arrays.asList(ChatColor.GOLD + "Right click on a player to view their inventory.", ChatColor.GOLD + "Crouch + right click on a player to view their armor."));
        item.setItemMeta(meta);
    }

    public InvInspectZTool() {
        super(item);
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!humanEntity.hasPermission("istaff.true")) {
            humanEntity.sendMessage(ChatColor.RED + "You do not have permission to use that item.");
            humanEntity.getInventory().remove(getItem());
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
    }

    @Override
    public void onPlayerPickup(PlayerPickupItemEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onPlayerItemBreak(PlayerItemBreakEvent event) {
    }

    @Override
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("istaff.true")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use that item.");
            player.getInventory().remove(getItem());
            event.setCancelled(true);
            return;
        }

        Entity clicked = event.getRightClicked();
        if (clicked instanceof Player) {
            if (player.hasPermission("istaff.editinv")) {
                if (player.isSneaking()) {
                    Player clickedPlayer = (Player) clicked;
                    String title = clickedPlayer.getName() + "'s Armor";
                    if (title.length() > 32) title = "Armor";

                    final Menu menu = new Menu(title, 1, ((Player) clicked).getInventory().getArmorContents());
                    menu.setGlobalAction(new ItemAction() {
                        @Override
                        public void run(Player player, final Inventory inv, ItemStack item, int slot, InventoryAction action, InventoryClickEvent event) {
                            Bukkit.getServer().getScheduler().runTaskLater(IStaff.getPlugin(), new Runnable() {
                                @Override
                                public void run() {
                                    clickedPlayer.getInventory().setArmorContents(Arrays.copyOf(inv.getContents(), 4));
                                    clickedPlayer.updateInventory();
                                }
                            }, 1L);
                        }
                    });

                    menu.setCloseAction(new CloseAction() {
                        @Override
                        public boolean run(HumanEntity humanEntity, Inventory inv) {
                            return true;
                        }
                    });

                    menu.showMenu(player);
                } else {
                    player.openInventory(((Player) clicked).getInventory());
                }
            } else {
                if (player.isSneaking()) {
                    final Player clickedPlayer = (Player) clicked;
                    String title = clickedPlayer.getName() + "'s Armor";
                    if (title.length() > 32) title = "Armor";

                    final Menu menu = new Menu(title, 1, ((Player) clicked).getInventory().getArmorContents());
                    menu.setGlobalAction(new ItemAction() {
                        @Override
                        public void run(Player player, Inventory inventory, ItemStack item, int slot, InventoryAction action, InventoryClickEvent event) {
                            event.setCancelled(true);
                        }
                    });

                    menu.setCloseAction(new CloseAction() {
                        @Override
                        public boolean run(HumanEntity humanEntity, Inventory inventory) {
                            return true;
                        }
                    });

                    menu.showMenu(player);
                } else {
                    final Player clickedPlayer = (Player) clicked;
                    String title = clickedPlayer.getName() + "'s Inventory";
                    if (title.length() > 32) title = "Inventory";

                    final Menu menu = new Menu(title, 4, ((Player) clicked).getInventory().getContents());
                    menu.setGlobalAction(new ItemAction() {
                        @Override
                        public void run(Player player, Inventory inventory, ItemStack item, int slot, InventoryAction action, InventoryClickEvent event) {
                            event.setCancelled(true);
                        }
                    });

                    menu.setCloseAction(new CloseAction() {
                        @Override
                        public boolean run(HumanEntity humanEntity, Inventory invventory) {
                            return true;
                        }
                    });

                    menu.showMenu(player);
                }
            }
        }
    }
}
