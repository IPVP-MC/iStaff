package com.gmail.xd.zwander.menu;
/**
 * CLASS FROM
 * http://bukkit.org/threads/easy-menu-maker-api-v2-1-with-interfaces.349435/
 * http://bukkit.org/members/567legodude.90909839/
 */

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Menu {

    public interface ItemAction {

        void run(Player player, Inventory inv, ItemStack item, int slot, InventoryAction action, InventoryClickEvent event);
    }

    public interface CloseAction {

        boolean run(HumanEntity humanEntity, Inventory inventory);
    }

    @Getter
    @Setter
    private String title = "";

    @Getter
    private int rows = 3;

    private Map<Integer, ItemStack> content = new HashMap<>();
    private Map<Integer, ItemAction> commands = new HashMap<>();
    private Inventory inventory;
    private ItemAction itemAction;

    @Getter
    @Setter
    private CloseAction closeAction;

    private boolean runEmpty = true;

    public static List<Menu> menus = new ArrayList<>();

    public Menu(String title, int rows, ItemStack[] contents) {
        this(title, rows);
        setContents(contents);
    }

    public Menu(String title, int rows) throws IndexOutOfBoundsException {
        if (rows < 1 || rows > 6) {
            throw new IndexOutOfBoundsException("Menu can only have between 1 and 6 rows.");
        }

        this.title = title;
        this.rows = rows;
        menus.add(this);
    }

    public static void initialize(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onInvClick(InventoryClickEvent event) {
                List<Menu> menusCopy = new ArrayList<>(menus);
                for (Menu menu : menusCopy) {
                    menu.onInventoryClick(event);
                }
            }
        }, plugin);

        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onClose(InventoryCloseEvent event) {
                Iterator<Menu> iterator = menus.iterator();
                while (iterator.hasNext()) {
                    Menu menu = iterator.next();
                    if (menu.onInventoryClose(event)) {
                        iterator.remove();
                    }
                }
            }
        }, plugin);
    }

    private boolean onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        HumanEntity player = event.getPlayer();
        return inventory.getName().equals(title) && inventory.equals(this.inventory) && closeAction != null && closeAction.run(player, inventory);
    }

    private void onInventoryClick(InventoryClickEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (humanEntity instanceof Player) {
            Player player = (Player) humanEntity;
            Inventory inventory = event.getInventory();
            ItemStack item = event.getCurrentItem();
            int slot = event.getRawSlot();
            InventoryAction action = event.getAction();

            if (item == null || item.getType() == Material.AIR) {
                if (!runEmpty) {
                    return;
                }
            }

            if (inventory.getName().equals(title) && inventory.equals(this.inventory)) {
                if (slot <= (rows * 9) - 1) {
                    if (hasAction(slot)) {
                        commands.get(slot).run(player, inventory, item, slot, action, event);
                    }

                    if (itemAction != null) {
                        itemAction.run(player, inventory, item, slot, action, event);
                    }
                }
            }
        }
    }

    @Deprecated
    public boolean hasAction(int slot) {
        return commands.containsKey(slot);
    }

    @Deprecated
    public void setAction(int slot, ItemAction action) {
        commands.put(slot, action);
    }

    public void setGlobalAction(ItemAction action) {
        this.itemAction = action;
    }

    public void removeGlobalAction() {
        this.itemAction = null;
    }

    @Deprecated
    public void removeAction(int slot) {
        commands.remove(slot);
    }

    public void runWhenEmpty(boolean state) {
        this.runEmpty = state;
    }

    public int nextOpenSlot() {
        int h = 0;
        for (Integer i : content.keySet()) {
            if (i > h) {
                h = i;
            }
        }

        for (int i = 0; i <= h; i++) {
            if (!content.containsKey(i)) {
                return i;
            }
        }

        return h + 1;
    }

    public void setContents(ItemStack[] contents) throws ArrayIndexOutOfBoundsException {
        if (contents.length > rows * 9) {
            throw new ArrayIndexOutOfBoundsException("setContents() : Contents are larger than inventory.");
        }

        content.clear();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                content.put(i, contents[i]);
            }
        }
    }

    public void addItem(ItemStack item) {
        if (nextOpenSlot() > (rows * 9) - 1) {
            return;
        }

        setItem(nextOpenSlot(), item);
    }

    public void setItem(int slot, ItemStack item) throws IndexOutOfBoundsException {
        if (slot < 0 || slot > (rows * 9) - 1) {
            throw new IndexOutOfBoundsException("setItem() : Slot is outside inventory.");
        }

        if (item == null || item.getType() == Material.AIR) {
            removeItem(slot);
            return;
        }

        content.put(slot, item);
    }

    public void fill(ItemStack item) {
        for (int i = 0; i < rows * 9; i++) {
            content.put(i, item);
        }
    }

    public void fillRange(int start, int end, ItemStack item) throws IndexOutOfBoundsException {
        if (end <= start) {
            throw new IndexOutOfBoundsException("fillRange() : Ending index must be less than starting index.");
        }

        if (start < 0 || start > (rows * 9) - 1) {
            throw new IndexOutOfBoundsException("fillRange() : Starting index is outside inventory.");
        }

        if (end < 0 || end > (rows * 9) - 1) {
            throw new IndexOutOfBoundsException("fillRange() : Ending index is outside inventory.");
        }

        for (int i = start; i <= end; i++) {
            content.put(i, item);
        }
    }

    public ItemStack removeItem(int slot) {
        return content.remove(slot);
    }

    public ItemStack getItem(int slot) {
        return content.get(slot);
    }

    public void build() {
        inventory = Bukkit.createInventory(null, rows * 9, this.title);
        inventory.clear();
        for (Map.Entry<Integer, ItemStack> entry : content.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue());
        }
    }

    public Inventory getMenu() {
        build();
        return inventory;
    }

    public void showMenu(Player player) {
        player.openInventory(getMenu());
    }

    public ItemStack[] getContents() {
        return getMenu().getContents();
    }

    public void unregister() {
        menus.remove(this);
    }

    public ItemStack createItem(Material material, int amount, String name, String lore, short durability) throws IndexOutOfBoundsException {
        if (amount < 1 || amount > 64) {
            throw new IndexOutOfBoundsException("Amount should be between 1 and 64.");
        }

        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (name != null && !name.isEmpty()) {
            meta.setDisplayName(name);
        }

        if (lore != null && !lore.isEmpty()) {
            String[] lines = lore.split(Pattern.quote("^$"));
            List<String> newlore = new ArrayList<>();
            Collections.addAll(newlore, lines);
            meta.setLore(newlore);
        }

        item.setDurability(durability);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createItem(Material material, int amount, String name, String lore, short durability, byte data) throws IndexOutOfBoundsException {
        if (amount < 1 || amount > 64) {
            throw new IndexOutOfBoundsException("Amount should be between 1 and 64.");
        }

        ItemStack item = new ItemStack(material, amount, data);
        ItemMeta meta = item.getItemMeta();

        if (name != null && !name.isEmpty()) {
            meta.setDisplayName(name);
        }

        if (lore != null && !lore.isEmpty()) {
            String[] lines = lore.split(Pattern.quote("^$"));
            List<String> newlore = new ArrayList<>();
            Collections.addAll(newlore, lines);
            meta.setLore(newlore);
        }

        item.setDurability(durability);
        item.setItemMeta(meta);
        return item;
    }
}