package dev.xoperr.blissgems.core.api.enchant;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;

import java.util.*;

/**
 * Defines which enchantments should be automatically applied to equipment
 * when a player has a specific gem in their inventory.
 *
 * Example:
 * <pre>
 * EnchantmentRule rule = new EnchantmentRule("speed")
 *     .addEnchantment(EquipmentSlot.FEET, Enchantment.SWIFT_SNEAK, 3)
 *     .addEnchantment(EquipmentSlot.LEGS, Enchantment.PROTECTION, 2);
 * </pre>
 */
public class EnchantmentRule {

    private final String gemId;
    private final Map<EquipmentSlot, Map<Enchantment, Integer>> enchantments;
    private boolean overrideExisting;
    private int priority;

    /**
     * Create a new enchantment rule for a specific gem type.
     *
     * @param gemId The gem ID (e.g., "fire", "speed", "strength")
     */
    public EnchantmentRule(String gemId) {
        this.gemId = gemId;
        this.enchantments = new EnumMap<>(EquipmentSlot.class);
        this.overrideExisting = false;
        this.priority = 0;
    }

    /**
     * Add an enchantment to a specific equipment slot.
     *
     * @param slot The equipment slot (HEAD, CHEST, LEGS, FEET, HAND, OFF_HAND)
     * @param enchantment The enchantment to apply
     * @param level The enchantment level
     * @return This rule for chaining
     */
    public EnchantmentRule addEnchantment(EquipmentSlot slot, Enchantment enchantment, int level) {
        enchantments.computeIfAbsent(slot, k -> new HashMap<>()).put(enchantment, level);
        return this;
    }

    /**
     * Add an enchantment to all armor slots.
     *
     * @param enchantment The enchantment to apply
     * @param level The enchantment level
     * @return This rule for chaining
     */
    public EnchantmentRule addArmorEnchantment(Enchantment enchantment, int level) {
        addEnchantment(EquipmentSlot.HEAD, enchantment, level);
        addEnchantment(EquipmentSlot.CHEST, enchantment, level);
        addEnchantment(EquipmentSlot.LEGS, enchantment, level);
        addEnchantment(EquipmentSlot.FEET, enchantment, level);
        return this;
    }

    /**
     * Add an enchantment to the main hand.
     *
     * @param enchantment The enchantment to apply
     * @param level The enchantment level
     * @return This rule for chaining
     */
    public EnchantmentRule addWeaponEnchantment(Enchantment enchantment, int level) {
        addEnchantment(EquipmentSlot.HAND, enchantment, level);
        return this;
    }

    /**
     * Set whether this rule should override existing enchantments.
     * Default is false (won't override).
     *
     * @param override True to override existing enchantments
     * @return This rule for chaining
     */
    public EnchantmentRule setOverrideExisting(boolean override) {
        this.overrideExisting = override;
        return this;
    }

    /**
     * Set the priority of this rule.
     * Higher priority rules are applied first when multiple gems provide
     * the same enchantment.
     *
     * @param priority The priority (default is 0)
     * @return This rule for chaining
     */
    public EnchantmentRule setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    // Getters

    public String getGemId() {
        return gemId;
    }

    public Map<EquipmentSlot, Map<Enchantment, Integer>> getEnchantments() {
        return Collections.unmodifiableMap(enchantments);
    }

    public Map<Enchantment, Integer> getEnchantsForSlot(EquipmentSlot slot) {
        return enchantments.getOrDefault(slot, Collections.emptyMap());
    }

    public boolean shouldOverrideExisting() {
        return overrideExisting;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * Check if this rule has any enchantments defined for a specific slot.
     */
    public boolean hasEnchantsForSlot(EquipmentSlot slot) {
        return enchantments.containsKey(slot) && !enchantments.get(slot).isEmpty();
    }
}
