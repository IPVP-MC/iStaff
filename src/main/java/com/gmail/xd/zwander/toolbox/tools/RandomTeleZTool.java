package com.gmail.xd.zwander.toolbox.tools;

import com.gmail.xd.zwander.toolbox.BasicZTool;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RandomTeleZTool extends BasicZTool {

    private static ItemStack item;

    static {
        item = new ItemStack(Material.EYE_OF_ENDER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Random Teleport");
        meta.setLore(Collections.singletonList(ChatColor.GOLD + "Teleport to a random player on this server."));
        item.setItemMeta(meta);
    }

    public RandomTeleZTool() {
        super(item);
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!humanEntity.hasPermission("istaff.true")) {
            humanEntity.sendMessage(ChatColor.RED + "You do not have permission to use that item.");
            humanEntity.getInventory().remove(this.getItem());
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("istaff.true")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use that item.");
            player.getInventory().remove(getItem());
        } else {
            List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
            players.remove(event.getPlayer());
            Player target = players.isEmpty() ? null : players.get(RANDOM.nextInt(players.size()));
            if (target != null) {
                player.teleport(target);
                player.sendMessage(ChatColor.YELLOW + "You have been teleported to " + ChatColor.GOLD + target.getName() + ChatColor.YELLOW + ".");
            } else {
                player.sendMessage(ChatColor.YELLOW + "No one to teleport to!");
            }
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
        event.setCancelled(true);
    }
}
