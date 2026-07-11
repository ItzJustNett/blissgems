package dev.xoperr.blissgems.utils;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Replaces legacy (pre-Oraxen) BlissGems items in player inventories with their
 * Oraxen-built equivalents, so every player ends up with the pack cosmetics.
 * Used by /fixgems and, when oraxen-items.fix-on-join is enabled, on every join.
 */
public final class OraxenGemFixer {

    public static final String FIX_ON_JOIN_PATH = "oraxen-items.fix-on-join";

    private OraxenGemFixer() {
    }

    public static boolean isFixOnJoinEnabled(BlissGems plugin) {
        return plugin.getConfig().getBoolean(FIX_ON_JOIN_PATH, true);
    }

    public static void setFixOnJoinEnabled(BlissGems plugin, boolean enabled) {
        plugin.getConfig().set(FIX_ON_JOIN_PATH, enabled);
        plugin.saveConfig();
    }

    /**
     * Scans the player's whole inventory (hotbar, main, armor, offhand) and swaps
     * legacy BlissGems items in place.
     *
     * @return how many stacks were replaced
     */
    public static int fixInventory(BlissGems plugin, Player player) {
        PlayerInventory inv = player.getInventory();
        int fixed = 0;
        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack replacement = rebuildIfLegacy(plugin, player, inv.getItem(slot));
            if (replacement != null) {
                inv.setItem(slot, replacement);
                fixed++;
            }
        }
        return fixed;
    }

    /**
     * Returns the Oraxen-built replacement for a legacy BlissGems stack,
     * or null if the stack is fine as-is (not ours, already Oraxen, or
     * Oraxen can't build it).
     */
    private static ItemStack rebuildIfLegacy(BlissGems plugin, Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String id = CustomItemManager.getIdByItem(item);
        if (id == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || CustomItemManager.isOraxenBacked(meta)) {
            return null;
        }
        int energy = GemType.isGem(id) ? plugin.getEnergyManager().getEnergy(player) : -1;
        ItemStack fresh = CustomItemManager.getItemById(id, energy);
        if (fresh == null) {
            return null;
        }
        ItemMeta freshMeta = fresh.getItemMeta();
        if (freshMeta == null || !CustomItemManager.isOraxenBacked(freshMeta)) {
            return null; // Oraxen unavailable or item not configured there - keep the old stack
        }
        fresh.setAmount(item.getAmount());
        return fresh;
    }
}
