/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.NamespacedKey
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package dev.xoperr.blissgems.core.managers;

import dev.xoperr.blissgems.core.api.enchant.EnchantmentRule;
import dev.xoperr.blissgems.core.api.protection.GemProtectionAPI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class AutoEnchantManager {
    private static final EquipmentSlot[] PLAYER_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND};
    private final Plugin plugin;
    private final Map<String, EnchantmentRule> rules;
    private BukkitTask updateTask;
    private boolean enabled;

    public AutoEnchantManager(Plugin plugin) {
        this.plugin = plugin;
        this.rules = new HashMap<String, EnchantmentRule>();
        this.enabled = true;
        this.startUpdateTask();
    }

    public void registerRule(EnchantmentRule rule) {
        if (rule == null || rule.getGemId() == null) {
            return;
        }
        this.rules.put(rule.getGemId(), rule);
    }

    public void unregisterRule(String gemId) {
        this.rules.remove(gemId);
    }

    public EnchantmentRule getRule(String gemId) {
        return this.rules.get(gemId);
    }

    public boolean hasRule(String gemId) {
        return this.rules.containsKey(gemId);
    }

    public void updatePlayer(Player player) {
        if (!this.enabled || player == null) {
            return;
        }
        Set<String> gemsInInventory = this.findGemsInInventory(player);
        for (EquipmentSlot slot : PLAYER_SLOTS) {
            this.updateEquipmentSlot(player, slot, gemsInInventory);
        }
    }

    private Set<String> findGemsInInventory(Player player) {
        HashSet<String> gemIds = new HashSet<String>();
        for (ItemStack item : player.getInventory().getContents()) {
            String gemId;
            if (item == null || !GemProtectionAPI.isGem(item) || (gemId = GemProtectionAPI.getGemId(item)) == null || !this.rules.containsKey(gemId)) continue;
            gemIds.add(gemId);
        }
        return gemIds;
    }

    private void updateEquipmentSlot(Player player, EquipmentSlot slot, Set<String> gemsInInventory) {
        ItemStack item = player.getInventory().getItem(slot);
        if (item == null || item.getType().isAir()) {
            return;
        }
        HashMap<Enchantment, Integer> enchantsToApply = new HashMap<Enchantment, Integer>();
        for (String gemId : gemsInInventory) {
            EnchantmentRule rule = this.rules.get(gemId);
            if (rule == null || !rule.hasEnchantsForSlot(slot)) continue;
            for (Map.Entry<Enchantment, Integer> entry : rule.getEnchantsForSlot(slot).entrySet()) {
                enchantsToApply.merge(entry.getKey(), entry.getValue(), Math::max);
            }
        }
        this.removeOutdatedAutoEnchants(item, enchantsToApply);
        this.applyAutoEnchants(item, enchantsToApply);
    }

    private void removeOutdatedAutoEnchants(ItemStack item, Map<Enchantment, Integer> newEnchants) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        for (Enchantment enchant : new HashSet<>(meta.getEnchants().keySet())) {
            NamespacedKey enchantKey = this.autoEnchantKey(enchant);
            if (!pdc.has(enchantKey, PersistentDataType.BYTE) || newEnchants.containsKey(enchant)) continue;
            meta.removeEnchant(enchant);
            pdc.remove(enchantKey);
        }
        item.setItemMeta(meta);
    }

    private NamespacedKey autoEnchantKey(Enchantment enchant) {
        return new NamespacedKey(this.plugin, "auto_" + enchant.getKey().getKey());
    }

    private void applyAutoEnchants(ItemStack item, Map<Enchantment, Integer> enchantsToApply) {
        if (enchantsToApply.isEmpty()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean modified = false;
        for (Map.Entry<Enchantment, Integer> entry : enchantsToApply.entrySet()) {
            Enchantment enchant = entry.getKey();
            int level = entry.getValue();
            int currentLevel = meta.getEnchantLevel(enchant);
            NamespacedKey enchantKey = this.autoEnchantKey(enchant);
            boolean isAutoEnchant = pdc.has(enchantKey, PersistentDataType.BYTE);
            if (currentLevel == 0) {
                meta.addEnchant(enchant, level, true);
                pdc.set(enchantKey, PersistentDataType.BYTE, (byte)1);
                modified = true;
                continue;
            }
            if (!isAutoEnchant || currentLevel == level) continue;
            meta.removeEnchant(enchant);
            meta.addEnchant(enchant, level, true);
            modified = true;
        }
        if (modified) {
            item.setItemMeta(meta);
        }
    }

    public void clearPlayerEnchantments(Player player) {
        for (EquipmentSlot slot : PLAYER_SLOTS) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null) continue;
            this.removeAllAutoEnchants(item);
        }
    }

    private void removeAllAutoEnchants(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean modified = false;
        for (Enchantment enchant : new HashSet<>(meta.getEnchants().keySet())) {
            NamespacedKey enchantKey = this.autoEnchantKey(enchant);
            if (!pdc.has(enchantKey, PersistentDataType.BYTE)) continue;
            meta.removeEnchant(enchant);
            pdc.remove(enchantKey);
            modified = true;
        }
        if (modified) {
            item.setItemMeta(meta);
        }
    }

    private void startUpdateTask() {
        this.updateTask = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
            if (this.enabled) {
                for (Player player : this.plugin.getServer().getOnlinePlayers()) {
                    this.updatePlayer(player);
                }
            }
        }, 20L, 10L);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            for (Player player : this.plugin.getServer().getOnlinePlayers()) {
                this.clearPlayerEnchantments(player);
            }
        }
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void cleanup() {
        if (this.updateTask != null) {
            this.updateTask.cancel();
        }
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            this.clearPlayerEnchantments(player);
        }
        this.rules.clear();
    }
}

