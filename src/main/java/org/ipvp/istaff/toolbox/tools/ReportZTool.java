package org.ipvp.istaff.toolbox.tools;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ipvp.istaff.IStaff;
import org.ipvp.istaff.toolbox.BasicZTool;

import java.util.Collections;

public class ReportZTool extends BasicZTool {

    private static ItemStack item;

    static {
        item = new ItemStack(Material.CHEST, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Reports");
        meta.setLore(Collections.singletonList(ChatColor.GOLD + "Click to open reports."));
        item.setItemMeta(meta);
    }

    public ReportZTool() {
        super(item);
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!humanEntity.hasPermission("istaff.true")) {
            humanEntity.sendMessage(ChatColor.RED + "You do not have permission to use that item.");
            event.getInventory().remove(getItem());
            event.setCancelled(true);
            return;
        }

        if (humanEntity instanceof Player) {
            Player player = (Player) humanEntity;
            IStaff.getPlugin().getIStaffDataBaseManager().serveSavedMenu(player);
        } else {
            humanEntity.sendMessage("Only players can execute that command!");
        }

        event.setCancelled(true);
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("istaff.true")) {
            IStaff.getPlugin().getIStaffDataBaseManager().serveSavedMenu(player);
        } else {
            player.sendMessage(ChatColor.RED + "You do not have permission to use that item.");
            player.getInventory().remove(this.getItem());
        }

        event.setUseItemInHand(Event.Result.DENY);
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
    }
}
