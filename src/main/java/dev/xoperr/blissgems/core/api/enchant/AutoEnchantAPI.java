/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package dev.xoperr.blissgems.core.api.enchant;

import dev.xoperr.blissgems.core.api.enchant.EnchantmentRule;
import dev.xoperr.blissgems.core.managers.AutoEnchantManager;
import org.bukkit.entity.Player;

public class AutoEnchantAPI {
    private static AutoEnchantManager manager;

    public static void initialize(AutoEnchantManager autoEnchantManager) {
        manager = autoEnchantManager;
    }

    public static void registerRule(EnchantmentRule rule) {
        AutoEnchantAPI.checkInitialized();
        manager.registerRule(rule);
    }

    public static void unregisterRule(String gemId) {
        AutoEnchantAPI.checkInitialized();
        manager.unregisterRule(gemId);
    }

    public static EnchantmentRule getRule(String gemId) {
        AutoEnchantAPI.checkInitialized();
        return manager.getRule(gemId);
    }

    public static boolean hasRule(String gemId) {
        AutoEnchantAPI.checkInitialized();
        return manager.hasRule(gemId);
    }

    public static void updatePlayer(Player player) {
        AutoEnchantAPI.checkInitialized();
        manager.updatePlayer(player);
    }

    public static void clearPlayerEnchantments(Player player) {
        AutoEnchantAPI.checkInitialized();
        manager.clearPlayerEnchantments(player);
    }

    public static void setEnabled(boolean enabled) {
        AutoEnchantAPI.checkInitialized();
        manager.setEnabled(enabled);
    }

    public static boolean isEnabled() {
        AutoEnchantAPI.checkInitialized();
        return manager.isEnabled();
    }

    private static void checkInitialized() {
        if (manager == null) {
            throw new IllegalStateException("AutoEnchantAPI has not been initialized! Ensure XoperrCore is loaded.");
        }
    }
}

