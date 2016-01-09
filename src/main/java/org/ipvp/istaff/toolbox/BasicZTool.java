package org.ipvp.istaff.toolbox;

import org.bukkit.inventory.ItemStack;
import org.ipvp.istaff.toolbox.ZToolBox.ZTool;

import java.util.Random;

public abstract class BasicZTool implements ZTool {

    protected static final Random RANDOM = new Random();

    private final ItemStack stack;

    public BasicZTool(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public ItemStack getItem() {
        return stack;
    }
}
