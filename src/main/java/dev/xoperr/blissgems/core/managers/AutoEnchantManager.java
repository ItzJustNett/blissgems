package dev.xoperr.blissgems.core.managers;

import org.bukkit.plugin.Plugin;
import dev.xoperr.blissgems.core.api.enchant.EnchantmentRule;
import dev.xoperr.blissgems.core.api.protection.GemProtectionAPI;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manager class handling automatic enchantment application.
 * Applies enchantments to equipped gear when gems are in inventory.
 */
public class AutoEnchantManager {

    private final Plugin plugin;
    private final Map<String, EnchantmentRule> rules;
    private final NamespacedKey autoEnchantKey;
    private BukkitTask updateTask;
    private boolean enabled;

    public AutoEnchantManager(Plugin plugin) {
        this.plugin = plugin;
        this.rules = new HashMap<>();
        this.autoEnchantKey = new NamespacedKey(plugin, "auto_enchant");
        this.enabled = true;

        // Start periodic update task
        startUpdateTask();
    }

    /**
     * Register an enchantment rule.
     */
    public void registerRule(EnchantmentRule rule) {
        if (rule == null || rule.getGemId() == null) {
            return;
        }
        rules.put(rule.getGemId(), rule);
    }

    /**
     * Unregister a rule by gem ID.
     */
    public void unregisterRule(String gemId) {
        rules.remove(gemId);
    }

    /**
     * Get a rule by gem ID.
     */
    public EnchantmentRule getRule(String gemId) {
        return rules.get(gemId);
    }

    /**
     * Check if a rule exists.
     */
    public boolean hasRule(String gemId) {
        return rules.containsKey(gemId);
    }

    /**
     * Update a player's equipment enchantments based on gems in inventory.
     */
    public void updatePlayer(Player player) {
        if (!enabled || player == null) {
            return;
        }

        // Find all gems in player's inventory
        Set<String> gemsInInventory = findGemsInInventory(player);

        // Update each equipment slot
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            updateEquipmentSlot(player, slot, gemsInInventory);
        }
    }

    /**
     * Find all gem IDs in a player's inventory.
     */
    private Set<String> findGemsInInventory(Player player) {
        Set<String> gemIds = new HashSet<>();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && GemProtectionAPI.isGem(item)) {
                String gemId = GemProtectionAPI.getGemId(item);
                if (gemId != null && rules.containsKey(gemId)) {
                    gemIds.add(gemId);
                }
            }
        }

        return gemIds;
    }

    /**
     * Update enchantments for a specific equipment slot.
     */
    private void updateEquipmentSlot(Player player, EquipmentSlot slot, Set<String> gemsInInventory) {
        ItemStack item = player.getInventory().getItem(slot);

        if (item == null || item.getType().isAir()) {
            return;
        }

        // Collect all enchantments that should be applied
        Map<Enchantment, Integer> enchantsToApply = new HashMap<>();

        for (String gemId : gemsInInventory) {
            EnchantmentRule rule = rules.get(gemId);
            if (rule != null && rule.hasEnchantsForSlot(slot)) {
                Map<Enchantment, Integer> ruleEnchants = rule.getEnchantsForSlot(slot);
                for (Map.Entry<Enchantment, Integer> entry : ruleEnchants.entrySet()) {
                    Enchantment enchant = entry.getKey();
                    int level = entry.getValue();

                    // Take highest level if multiple gems provide same enchant
                    enchantsToApply.merge(enchant, level, Math::max);
                }
            }
        }

        // Remove auto-enchants that are no longer needed
        removeOutdatedAutoEnchants(item, enchantsToApply);

        // Apply new auto-enchants
        applyAutoEnchants(item, enchantsToApply);
    }

    /**
     * Remove auto-enchants that are no longer needed.
     */
    private void removeOutdatedAutoEnchants(ItemStack item, Map<Enchantment, Integer> newEnchants) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Map<Enchantment, Integer> currentEnchants = meta.getEnchants();

        for (Enchantment enchant : new HashSet<>(currentEnchants.keySet())) {
            // Check if this is an auto-enchant
            NamespacedKey enchantKey = new NamespacedKey(plugin, "auto_" + enchant.getKey().getKey());

            if (pdc.has(enchantKey, PersistentDataType.BYTE)) {
                // This is an auto-enchant
                if (!newEnchants.containsKey(enchant)) {
                    // Should be removed
                    meta.removeEnchant(enchant);
                    pdc.remove(enchantKey);
                }
            }
        }

        item.setItemMeta(meta);
    }

    /**
     * Apply auto-enchants to an item.
     */
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

            // Check if item already has this enchantment
            int currentLevel = meta.getEnchantLevel(enchant);
            NamespacedKey enchantKey = new NamespacedKey(plugin, "auto_" + enchant.getKey().getKey());

            boolean isAutoEnchant = pdc.has(enchantKey, PersistentDataType.BYTE);

            if (currentLevel == 0) {
                // No enchant exists, apply it
                meta.addEnchant(enchant, level, true);
                pdc.set(enchantKey, PersistentDataType.BYTE, (byte) 1);
                modified = true;
            } else if (isAutoEnchant && currentLevel != level) {
                // Update auto-enchant level
                meta.removeEnchant(enchant);
                meta.addEnchant(enchant, level, true);
                modified = true;
            }
            // If player has their own enchant, don't override it
        }

        if (modified) {
            item.setItemMeta(meta);
        }
    }

    /**
     * Clear all auto-enchantments from a player.
     */
    public void clearPlayerEnchantments(Player player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item != null) {
                removeAllAutoEnchants(item);
            }
        }
    }

    /**
     * Remove all auto-enchants from an item.
     */
    private void removeAllAutoEnchants(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Map<Enchantment, Integer> enchants = meta.getEnchants();
        boolean modified = false;

        for (Enchantment enchant : new HashSet<>(enchants.keySet())) {
            NamespacedKey enchantKey = new NamespacedKey(plugin, "auto_" + enchant.getKey().getKey());

            if (pdc.has(enchantKey, PersistentDataType.BYTE)) {
                meta.removeEnchant(enchant);
                pdc.remove(enchantKey);
                modified = true;
            }
        }

        if (modified) {
            item.setItemMeta(meta);
        }
    }

    /**
     * Start periodic update task.
     */
    private void startUpdateTask() {
        // Update all players every 10 ticks (0.5 seconds)
        updateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (enabled) {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    updatePlayer(player);
                }
            }
        }, 20L, 10L);
    }

    /**
     * Enable or disable the system.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        if (!enabled) {
            // Clear all auto-enchants when disabled
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                clearPlayerEnchantments(player);
            }
        }
    }

    /**
     * Check if enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Cleanup on plugin disable.
     */
    public void cleanup() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        // Clear all auto-enchants
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            clearPlayerEnchantments(player);
        }

        rules.clear();
    }
}
