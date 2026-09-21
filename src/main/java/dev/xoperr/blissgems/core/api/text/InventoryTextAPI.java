/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package dev.xoperr.blissgems.core.api.text;

import dev.xoperr.blissgems.core.api.text.InventoryTextProvider;
import dev.xoperr.blissgems.core.managers.TextManager;
import org.bukkit.entity.Player;

public class InventoryTextAPI {
    private static TextManager manager;

    public static void initialize(TextManager textManager) {
        manager = textManager;
    }

    public static void sendActionBar(Player player, String message) {
        InventoryTextAPI.checkInitialized();
        manager.sendActionBar(player, message);
    }

    public static void clearActionBar(Player player) {
        InventoryTextAPI.checkInitialized();
        manager.clearActionBar(player);
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        InventoryTextAPI.checkInitialized();
        manager.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
    }

    public static void clearTitle(Player player) {
        InventoryTextAPI.checkInitialized();
        manager.clearTitle(player);
    }

    public static void registerTextProvider(InventoryTextProvider provider) {
        InventoryTextAPI.checkInitialized();
        manager.registerTextProvider(provider);
    }

    public static void unregisterTextProvider(InventoryTextProvider provider) {
        InventoryTextAPI.checkInitialized();
        manager.unregisterTextProvider(provider);
    }

    private static void checkInitialized() {
        if (manager == null) {
            throw new IllegalStateException("InventoryTextAPI has not been initialized! Ensure XoperrCore is loaded.");
        }
    }
}

