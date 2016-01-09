package org.ipvp.istaff.data;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Represents the previous inventory of player.
 */
public class FauxPlayerInventory {

    public final Inventory inventory;
    public final ItemStack[] armor;

    public FauxPlayerInventory(Inventory inventory, ItemStack[] armor) {
        this.inventory = inventory;
        this.armor = armor;
    }

    public FauxPlayerInventory(PlayerInventory inventory) {
        Inventory reportInventory = Bukkit.createInventory(null, inventory.getSize(), inventory.getTitle());
        reportInventory.setContents(inventory.getContents());

        this.inventory = reportInventory;
        this.armor = inventory.getArmorContents();
    }
}
