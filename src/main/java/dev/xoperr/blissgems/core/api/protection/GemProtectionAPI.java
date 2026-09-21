/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.inventory.ItemStack
 */
package dev.xoperr.blissgems.core.api.protection;

import dev.xoperr.blissgems.core.managers.ProtectionManager;
import org.bukkit.inventory.ItemStack;

public class GemProtectionAPI {
    private static ProtectionManager manager;

    public static void initialize(ProtectionManager protectionManager) {
        manager = protectionManager;
    }

    public static boolean markAsGem(ItemStack item) {
        GemProtectionAPI.checkInitialized();
        return manager.markAsGem(item);
    }

    public static boolean unmarkGem(ItemStack item) {
        GemProtectionAPI.checkInitialized();
        return manager.unmarkGem(item);
    }

    public static boolean isGem(ItemStack item) {
        GemProtectionAPI.checkInitialized();
        return manager.isGem(item);
    }

    public static boolean markAsGem(ItemStack item, String gemId, int tier) {
        GemProtectionAPI.checkInitialized();
        return manager.markAsGem(item, gemId, tier);
    }

    public static String getGemId(ItemStack item) {
        GemProtectionAPI.checkInitialized();
        return manager.getGemId(item);
    }

    public static int getGemTier(ItemStack item) {
        GemProtectionAPI.checkInitialized();
        return manager.getGemTier(item);
    }

    private static void checkInitialized() {
        if (manager == null) {
            throw new IllegalStateException("GemProtectionAPI has not been initialized! Ensure XoperrCore is loaded.");
        }
    }
}

