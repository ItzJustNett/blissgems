/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 */
package dev.xoperr.blissgems.core.managers;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

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

    public boolean markAsGem(ItemStack item) {
        ItemMeta meta;
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        if (this.isGem(item)) {
            return false;
        }
        ItemMeta itemMeta = meta = item.hasItemMeta() ? item.getItemMeta() : this.plugin.getServer().getItemFactory().getItemMeta(item.getType());
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(this.gemKey, PersistentDataType.BYTE, (byte)1);
        item.setItemMeta(meta);
        return true;
    }

    public boolean markAsGem(ItemStack item, String gemId, int tier) {
        ItemMeta meta;
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        ItemMeta itemMeta = meta = item.hasItemMeta() ? item.getItemMeta() : this.plugin.getServer().getItemFactory().getItemMeta(item.getType());
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(this.gemKey, PersistentDataType.BYTE, (byte)1);
        if (gemId != null) {
            container.set(this.gemIdKey, PersistentDataType.STRING, gemId);
        }
        container.set(this.gemTierKey, PersistentDataType.INTEGER, tier);
        item.setItemMeta(meta);
        return true;
    }

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
        if (!container.has(this.gemKey, PersistentDataType.BYTE)) {
            return false;
        }
        container.remove(this.gemKey);
        container.remove(this.gemIdKey);
        container.remove(this.gemTierKey);
        item.setItemMeta(meta);
        return true;
    }

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
        return container.has(this.gemKey, PersistentDataType.BYTE) && (Byte)container.get(this.gemKey, PersistentDataType.BYTE) == 1;
    }

    public String getGemId(ItemStack item) {
        if (!this.isGem(item)) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return (String)container.get(this.gemIdKey, PersistentDataType.STRING);
    }

    public int getGemTier(ItemStack item) {
        if (!this.isGem(item)) {
            return 0;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Integer tier = (Integer)container.get(this.gemTierKey, PersistentDataType.INTEGER);
        return tier != null ? tier : 0;
    }
}

