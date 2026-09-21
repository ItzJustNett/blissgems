/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.inventory.EquipmentSlot
 */
package dev.xoperr.blissgems.core.api.enchant;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;

public class EnchantmentRule {
    private final String gemId;
    private final Map<EquipmentSlot, Map<Enchantment, Integer>> enchantments;
    private boolean overrideExisting;
    private int priority;

    public EnchantmentRule(String gemId) {
        this.gemId = gemId;
        this.enchantments = new EnumMap<EquipmentSlot, Map<Enchantment, Integer>>(EquipmentSlot.class);
        this.overrideExisting = false;
        this.priority = 0;
    }

    public EnchantmentRule addEnchantment(EquipmentSlot slot, Enchantment enchantment, int level) {
        this.enchantments.computeIfAbsent(slot, k -> new HashMap()).put(enchantment, level);
        return this;
    }

    public EnchantmentRule addArmorEnchantment(Enchantment enchantment, int level) {
        this.addEnchantment(EquipmentSlot.HEAD, enchantment, level);
        this.addEnchantment(EquipmentSlot.CHEST, enchantment, level);
        this.addEnchantment(EquipmentSlot.LEGS, enchantment, level);
        this.addEnchantment(EquipmentSlot.FEET, enchantment, level);
        return this;
    }

    public EnchantmentRule addWeaponEnchantment(Enchantment enchantment, int level) {
        this.addEnchantment(EquipmentSlot.HAND, enchantment, level);
        return this;
    }

    public EnchantmentRule setOverrideExisting(boolean override) {
        this.overrideExisting = override;
        return this;
    }

    public EnchantmentRule setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public String getGemId() {
        return this.gemId;
    }

    public Map<EquipmentSlot, Map<Enchantment, Integer>> getEnchantments() {
        return Collections.unmodifiableMap(this.enchantments);
    }

    public Map<Enchantment, Integer> getEnchantsForSlot(EquipmentSlot slot) {
        return this.enchantments.getOrDefault(slot, Collections.emptyMap());
    }

    public boolean shouldOverrideExisting() {
        return this.overrideExisting;
    }

    public int getPriority() {
        return this.priority;
    }

    public boolean hasEnchantsForSlot(EquipmentSlot slot) {
        return this.enchantments.containsKey(slot) && !this.enchantments.get(slot).isEmpty();
    }
}

