/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 */
package dev.xoperr.blissgems.utils;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemCosmetics;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class OraxenGemFixer {
    public static final String FIX_ON_JOIN_PATH = "oraxen-items.fix-on-join";

    private OraxenGemFixer() {
    }

    public static boolean isFixOnJoinEnabled(BlissGems plugin) {
        return plugin.getConfig().getBoolean(FIX_ON_JOIN_PATH, true);
    }

    public static void setFixOnJoinEnabled(BlissGems plugin, boolean enabled) {
        plugin.getConfig().set(FIX_ON_JOIN_PATH, (Object)enabled);
        plugin.saveConfig();
    }

    public static int fixInventory(BlissGems plugin, Player player) {
        PlayerInventory inv = player.getInventory();
        int fixed = 0;
        for (int slot = 0; slot < inv.getSize(); ++slot) {
            ItemStack replacement = OraxenGemFixer.rebuildIfLegacy(plugin, player, inv.getItem(slot));
            if (replacement == null) continue;
            inv.setItem(slot, replacement);
            ++fixed;
        }
        return fixed;
    }

    private static ItemStack rebuildIfLegacy(BlissGems plugin, Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String id = CustomItemManager.getIdByItem(item);
        if (id == null) {
            return null;
        }
        if (!GemCosmetics.has(id)) {
            return null;
        }
        int energy = GemType.isGem(id) ? plugin.getEnergyManager().getEnergy(player) : -1;
        ItemStack fresh = CustomItemManager.getItemById(id, energy);
        if (fresh == null) {
            return null;
        }
        fresh.setAmount(item.getAmount());
        return fresh;
    }
}

