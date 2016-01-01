package com.gmail.xd.zwander.toolbox.tools;

import com.gmail.xd.zwander.toolbox.BasicZTool;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
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

import java.util.Collections;

public class FreezeZTool extends BasicZTool {

    private static ItemStack item = new ItemStack(Material.ICE, 1);

    static {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Freeze Player");
        meta.setLore(Collections.singletonList(ChatColor.GOLD + "Right click on a player to freeze/unfreeze them."));
        item.setItemMeta(meta);
    }

    public FreezeZTool() {
        super(item);
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (humanEntity instanceof Player) {
            event.setCancelled(true);
            Player player = (Player) humanEntity;
            if (!player.hasPermission("istaff.true")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use that item.");
                player.getInventory().remove(getItem());
            }
        }
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
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
        Player player = event.getPlayer();
        if (!player.hasPermission("istaff.true")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use that item.");
            player.getInventory().remove(getItem());
            event.setCancelled(true);
            return;
        }

        Entity rightClicked = event.getRightClicked();
        if (rightClicked instanceof Player) {
            Player clickedPlayer = (Player) rightClicked;
            Bukkit.dispatchCommand(player, "halt " + clickedPlayer.getName());
        }
    }
}
