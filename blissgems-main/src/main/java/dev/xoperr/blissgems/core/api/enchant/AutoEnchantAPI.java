package dev.xoperr.blissgems.core.api.enchant;

import dev.xoperr.blissgems.core.managers.AutoEnchantManager;
import org.bukkit.entity.Player;

/**
 * Public API for automatic enchantment application based on gems in inventory.
 * When a player has a gem in their inventory, enchantments are automatically
 * applied to their equipped gear according to registered rules.
 *
 * Usage example:
 * <pre>
 * // Register a rule for the Fire gem
 * EnchantmentRule fireRule = new EnchantmentRule("fire")
 *     .addArmorEnchantment(Enchantment.FIRE_PROTECTION, 4)
 *     .addWeaponEnchantment(Enchantment.FIRE_ASPECT, 2);
 * AutoEnchantAPI.registerRule(fireRule);
 *
 * // Register a rule for the Speed gem
 * EnchantmentRule speedRule = new EnchantmentRule("speed")
 *     .addEnchantment(EquipmentSlot.FEET, Enchantment.SWIFT_SNEAK, 3)
 *     .addEnchantment(EquipmentSlot.FEET, Enchantment.DEPTH_STRIDER, 3);
 * AutoEnchantAPI.registerRule(speedRule);
 *
 * // Force update a player's enchantments
 * AutoEnchantAPI.updatePlayer(player);
 * </pre>
 */
public class AutoEnchantAPI {

    private static AutoEnchantManager manager;

    /**
     * Initialize the API with an AutoEnchantManager instance.
     * This is called internally by XoperrCore on plugin enable.
     *
     * @param autoEnchantManager The manager instance
     */
    public static void initialize(AutoEnchantManager autoEnchantManager) {
        manager = autoEnchantManager;
    }

    /**
     * Register an enchantment rule.
     * When a player has a gem matching the rule's gemId, the enchantments
     * will be automatically applied to their equipped gear.
     *
     * @param rule The enchantment rule to register
     */
    public static void registerRule(EnchantmentRule rule) {
        checkInitialized();
        manager.registerRule(rule);
    }

    /**
     * Unregister an enchantment rule by gem ID.
     *
     * @param gemId The gem ID to unregister
     */
    public static void unregisterRule(String gemId) {
        checkInitialized();
        manager.unregisterRule(gemId);
    }

    /**
     * Get a registered rule by gem ID.
     *
     * @param gemId The gem ID
     * @return The enchantment rule, or null if not found
     */
    public static EnchantmentRule getRule(String gemId) {
        checkInitialized();
        return manager.getRule(gemId);
    }

    /**
     * Check if a rule is registered for a gem ID.
     *
     * @param gemId The gem ID to check
     * @return True if a rule exists for this gem
     */
    public static boolean hasRule(String gemId) {
        checkInitialized();
        return manager.hasRule(gemId);
    }

    /**
     * Force update a player's equipment enchantments.
     * This checks their inventory for gems and applies/removes enchantments accordingly.
     *
     * @param player The player to update
     */
    public static void updatePlayer(Player player) {
        checkInitialized();
        manager.updatePlayer(player);
    }

    /**
     * Remove all auto-applied enchantments from a player.
     * This does not remove player-owned enchantments.
     *
     * @param player The player to clear enchantments from
     */
    public static void clearPlayerEnchantments(Player player) {
        checkInitialized();
        manager.clearPlayerEnchantments(player);
    }

    /**
     * Enable or disable auto-enchant system globally.
     *
     * @param enabled True to enable, false to disable
     */
    public static void setEnabled(boolean enabled) {
        checkInitialized();
        manager.setEnabled(enabled);
    }

    /**
     * Check if auto-enchant system is enabled.
     *
     * @return True if enabled
     */
    public static boolean isEnabled() {
        checkInitialized();
        return manager.isEnabled();
    }

    private static void checkInitialized() {
        if (manager == null) {
            throw new IllegalStateException("AutoEnchantAPI has not been initialized! Ensure XoperrCore is loaded.");
        }
    }
}
