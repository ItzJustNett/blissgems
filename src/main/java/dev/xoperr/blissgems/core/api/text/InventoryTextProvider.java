/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package dev.xoperr.blissgems.core.api.text;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface InventoryTextProvider {
    public String getActionBarText(Player var1, ItemStack var2, ItemStack var3);

    default public boolean shouldUpdate(Player player, ItemStack mainHand, ItemStack offHand) {
        return true;
    }

    default public int getPriority() {
        return 0;
    }
}

