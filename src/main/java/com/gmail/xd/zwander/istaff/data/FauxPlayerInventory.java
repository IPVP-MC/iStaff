package com.gmail.xd.zwander.istaff.data;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class FauxPlayerInventory {

    public final Inventory inventory;
    public final ItemStack[] armor;

    public FauxPlayerInventory(Inventory inventory, ItemStack[] armor) {
        this.inventory = inventory;
        this.armor = armor;
    }

    public FauxPlayerInventory(PlayerInventory inventory) {
        Inventory reportInventory = Bukkit.createInventory(null, 36, inventory.getTitle());
        reportInventory.setContents(inventory.getContents());

        this.inventory = reportInventory;
        this.armor = inventory.getArmorContents();
    }
}
