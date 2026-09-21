/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 */
package dev.xoperr.blissgems.core.api.region;

import dev.xoperr.blissgems.core.managers.RegionManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class RegionAPI {
    private static RegionManager manager;

    public static void initialize(RegionManager regionManager) {
        manager = regionManager;
    }

    public static boolean areGemsDisabled(Player player) {
        RegionAPI.checkInitialized();
        return manager.areGemsDisabled(player);
    }

    public static boolean areGemsDisabled(Location location) {
        RegionAPI.checkInitialized();
        return manager.areGemsDisabled(location);
    }

    public static String getDisabledMessage() {
        RegionAPI.checkInitialized();
        return manager.getDisabledMessage();
    }

    public static boolean isWorldGuardEnabled() {
        RegionAPI.checkInitialized();
        return manager.isWorldGuardEnabled();
    }

    private static void checkInitialized() {
        if (manager == null) {
            throw new IllegalStateException("RegionAPI has not been initialized!");
        }
    }
}

