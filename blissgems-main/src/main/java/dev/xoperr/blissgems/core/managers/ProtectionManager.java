package dev.xoperr.blissgems.core.managers;

import org.bukkit.plugin.Plugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Manager class handling gem protection logic.
 * Uses PersistentDataContainer to store protection data on items.
 */
public class ProtectionManager {

    private final Plugin plugin;
    private final NamespacedKey gemKey;
    private final NamespacedKey gemIdKey;
    private final NamespacedKey gemTierKey;

    public ProtectionManager(Plugin plugin) {
        this.plugin = plugin;
        this.gemKey = new NamespacedKey(plugin, "is_gem");
        this.gemIdKey = new NamespacedKey(plugin, "gem_id");
        this.gemTierKey = new NamespacedKey(plugin, "gem_tier");
    }

    /**
     * Mark an item as a gem.
     */
    public boolean markAsGem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        if (isGem(item)) {
            return false; // Already marked
        }

        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : plugin.getServer().getItemFactory().getItemMeta(item.getType());
        if (meta == null) {
            return false;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(gemKey, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return true;
    }

    /**
     * Mark an item as a gem with additional metadata.
     */
    public boolean markAsGem(ItemStack item, String gemId, int tier) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : plugin.getServer().getItemFactory().getItemMeta(item.getType());
        if (meta == null) {
            return false;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(gemKey, PersistentDataType.BYTE, (byte) 1);

        if (gemId != null) {
            container.set(gemIdKey, PersistentDataType.STRING, gemId);
        }

        container.set(gemTierKey, PersistentDataType.INTEGER, tier);

        item.setItemMeta(meta);
        return true;
    }

    /**
     * Remove gem protection from an item.
     */
    public boolean unmarkGem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        if (!item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (!container.has(gemKey, PersistentDataType.BYTE)) {
            return false; // Not a gem
        }

        // Remove all gem-related keys
        container.remove(gemKey);
        container.remove(gemIdKey);
        container.remove(gemTierKey);

        item.setItemMeta(meta);
        return true;
    }

    /**
     * Check if an item is marked as a gem.
     */
    public boolean isGem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        if (!item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(gemKey, PersistentDataType.BYTE) &&
               container.get(gemKey, PersistentDataType.BYTE) == 1;
    }

    /**
     * Get the gem ID from an item.
     */
    public String getGemId(ItemStack item) {
        if (!isGem(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(gemIdKey, PersistentDataType.STRING);
    }

    /**
     * Get the gem tier from an item.
     */
    public int getGemTier(ItemStack item) {
        if (!isGem(item)) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        Integer tier = container.get(gemTierKey, PersistentDataType.INTEGER);
        return tier != null ? tier : 0;
    }
}
