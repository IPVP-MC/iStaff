package com.gmail.xd.zwander.toolbox;

import com.gmail.xd.zwander.toolbox.ZToolBox.ZTool;
import org.bukkit.inventory.ItemStack;

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
