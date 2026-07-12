package dev.xoperr.blissgems.core.api.region;

import dev.xoperr.blissgems.core.managers.RegionManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Public API for checking WorldGuard region restrictions on gems.
 *
 * Usage:
 * <pre>
 * if (RegionAPI.areGemsDisabled(player)) {
 *     player.sendMessage("Gems are disabled here!");
 *     return;
 * }
 * </pre>
 */
public class RegionAPI {

    private static RegionManager manager;

    /**
     * Initialize the API with a RegionManager instance.
     * This is called internally by BlissGems on plugin enable.
     *
     * @param regionManager The manager instance
     */
    public static void initialize(RegionManager regionManager) {
        manager = regionManager;
    }

    /**
     * Check if gems are disabled at a player's location
     *
     * @param player The player to check
     * @return true if gems are disabled at the player's location, false otherwise
     */
    public static boolean areGemsDisabled(Player player) {
        checkInitialized();
        return manager.areGemsDisabled(player);
    }

    /**
     * Check if gems are disabled at a location
     *
     * @param location The location to check
     * @return true if gems are disabled at the location, false otherwise
     */
    public static boolean areGemsDisabled(Location location) {
        checkInitialized();
        return manager.areGemsDisabled(location);
    }

    /**
     * Get the disabled message from config
     *
     * @return The message to show when gems are disabled
     */
    public static String getDisabledMessage() {
        checkInitialized();
        return manager.getDisabledMessage();
    }

    /**
     * Check if WorldGuard integration is enabled
     *
     * @return true if WorldGuard is installed and enabled, false otherwise
     */
    public static boolean isWorldGuardEnabled() {
        checkInitialized();
        return manager.isWorldGuardEnabled();
    }

    private static void checkInitialized() {
        if (manager == null) {
            throw new IllegalStateException("RegionAPI has not been initialized!");
        }
    }
}
