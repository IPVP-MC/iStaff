package org.ipvp.istaff.toolbox;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
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
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class ZToolBox implements Listener {

    /**
     * Create a ZToolBox listener instance and register it
     *
     * @param plugin Plugin to register on
     */
    public ZToolBox(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Represents a tool
     *
     * @author zwander
     */
    public interface ZTool {

        void onInventoryClick(InventoryClickEvent event);

        void onPlayerInteract(PlayerInteractEvent event);

        void onPlayerInteractEntity(PlayerInteractEntityEvent event);

        void onPlayerDropItem(PlayerDropItemEvent event);

        void onPlayerItemConsume(PlayerItemConsumeEvent event);

        void onPlayerItemHeld(PlayerItemHeldEvent event);

        void onPlayerPickup(PlayerPickupItemEvent event);

        void onPlayerItemBreak(PlayerItemBreakEvent event);

        ItemStack getItem();
    }

    private static Map<String, ZTool> tools = new HashMap<>();

    /**
     * Add a tool to the tool box. This automatically causes it to receive events.
     * <br>
     * Tools of the same
     *
     * @param tool the tool
     */
    public static void setTool(ZTool tool) {
        ItemStack stack = tool.getItem();
        if (stack == null) {
            throw new IllegalArgumentException("Tool cannot be null");
        }

        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            tools.put(stack.getItemMeta().getDisplayName(), tool);
            return;
        }

        ItemMeta meta = Bukkit.getServer().getItemFactory().getItemMeta(tool.getItem().getType());
        meta.setDisplayName(stack.getType().toString());
        stack.setItemMeta(meta);
        tools.put(meta.getDisplayName(), tool);
    }

    /**
     * Removes the given tool from the tool box. This means it will no longer receive events.
     *
     * @param tool Tool to remove
     */
    public static void removeTool(ZTool tool) {
        tools.remove(tool.getItem().getItemMeta().getDisplayName());
    }

    /**
     * Removes the tool given by the argument from the tool box.
     *
     * @param tool Name of tool to remove
     */
    public void removeTool(String tool) {
        tools.remove(tool);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        
    }

    @EventHandler
    public void onEvent(InventoryClickEvent event) {
        ItemStack stack = event.getCurrentItem();
        if (stack == null) {
            return;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        String name = meta.getDisplayName();
        if (name == null) {
            return;
        }

        ZTool tool = tools.get(name);
        if (tool == null) {
            return;
        }

        tool.onInventoryClick(event);
    }

    @EventHandler
    public void onEvent(PlayerInteractEvent event) {
        ItemStack stack = event.getItem();
        if (stack == null) {
            return;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        String name = meta.getDisplayName();
        if (name == null) {
            return;
        }

        ZTool tool = tools.get(name);
        if (tool == null) {
            return;
        }

        tool.onPlayerInteract(event);
    }

    @EventHandler
    public void onEvent(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        if (stack == null) {
            return;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        String name = meta.getDisplayName();
        if (name == null) {
            return;
        }

        ZTool tool = tools.get(name);
        if (tool == null) {
            return;
        }

        tool.onPlayerDropItem(event);
    }

    @EventHandler
    public void onEvent(PlayerItemConsumeEvent event) {
        ItemMeta meta = event.getItem().getItemMeta();
        if (meta == null) {
            return;
        }

        String name = meta.getDisplayName();
        if (name == null) {
            return;
        }

        ZTool tool = tools.get(name);
        if (tool == null) {
            return;
        }

        tool.onPlayerItemConsume(event);
    }

    @EventHandler
    public void onEvent(PlayerItemHeldEvent event) {
        ItemStack stack = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (stack == null) {
            return;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        String name = meta.getDisplayName();
        if (name == null) {
            return;
        }

        ZTool tool = tools.get(name);
        if (tool == null) {
            return;
        }

        tool.onPlayerItemHeld(event);
    }

    @EventHandler
    public void onEvent(PlayerPickupItemEvent event) {
        ItemStack stack = event.getItem().getItemStack();
        if (stack == null) {
            return;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        String name = meta.getDisplayName();
        if (name == null) {
            return;
        }

        ZTool tool = tools.get(name);
        if (tool == null) {
            return;
        }

        tool.onPlayerPickup(event);
    }

    @EventHandler
    public void onEvent(PlayerItemBreakEvent event) {
        ItemStack stack = event.getBrokenItem();
        if (stack == null) {
            return;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        String name = meta.getDisplayName();
        if (name == null) {
            return;
        }

        ZTool tool = tools.get(name);
        if (tool == null) {
            return;
        }

        tool.onPlayerItemBreak(event);
    }

    @EventHandler
    public void onEvent(PlayerInteractEntityEvent event) {
        ItemStack stack = event.getPlayer().getItemInHand();
        if (stack == null) {
            return;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        String name = meta.getDisplayName();
        if (name == null) {
            return;
        }

        ZTool tool = tools.get(name);
        if (tool == null) {
            return;
        }

        tool.onPlayerInteractEntity(event);
    }
}
