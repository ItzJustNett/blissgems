package dev.xoperr.blissgems.core.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Manager for WorldGuard region integration (soft dependency).
 * Checks if players are in regions where gems should be disabled.
 */
public class RegionManager {

    private final Plugin plugin;
    private final boolean worldGuardEnabled;
    private final boolean regionCheckEnabled;

    public RegionManager(Plugin plugin) {
        this.plugin = plugin;
        this.worldGuardEnabled = checkWorldGuard();
        this.regionCheckEnabled = plugin.getConfig().getBoolean("worldguard.enabled", true);

        if (worldGuardEnabled) {
            plugin.getLogger().info("WorldGuard detected - region protection enabled");
        } else {
            plugin.getLogger().info("WorldGuard not found - region protection disabled");
        }
    }

    /**
     * Check if WorldGuard is installed and available
     */
    private boolean checkWorldGuard() {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            return plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Check if gems are disabled at a player's location
     */
    public boolean areGemsDisabled(Player player) {
        return areGemsDisabled(player.getLocation());
    }

    /**
     * Check if gems are disabled at a location
     */
    public boolean areGemsDisabled(Location location) {
        if (!worldGuardEnabled || !regionCheckEnabled) {
            return false;
        }

        try {
            // Get WorldGuard region manager
            com.sk89q.worldguard.protection.managers.RegionManager regionManager =
                WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(location.getWorld()));

            if (regionManager == null) {
                return false;
            }

            // Get regions at location
            ApplicableRegionSet regions = regionManager.getApplicableRegions(
                BukkitAdapter.asBlockVector(location)
            );

            // Check for gem-disabled flag
            return checkRegionFlags(regions);

        } catch (Exception e) {
            plugin.getLogger().warning("Error checking WorldGuard regions: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if any region in the set has gems disabled
     */
    private boolean checkRegionFlags(ApplicableRegionSet regions) {
        String mode = plugin.getConfig().getString("worldguard.mode", "blacklist");
        List<String> regionList = plugin.getConfig().getStringList("worldguard.regions");

        for (ProtectedRegion region : regions) {
            String regionId = region.getId();

            // Whitelist mode: gems disabled UNLESS in whitelist
            if (mode.equalsIgnoreCase("whitelist")) {
                if (!regionList.contains(regionId)) {
                    return true; // Not in whitelist = disabled
                }
            }
            // Blacklist mode: gems disabled ONLY in blacklist
            else if (mode.equalsIgnoreCase("blacklist")) {
                if (regionList.contains(regionId)) {
                    return true; // In blacklist = disabled
                }
            }
        }

        return false; // No matching region = enabled
    }

    /**
     * Get disabled message for player
     */
    public String getDisabledMessage() {
        return plugin.getConfig().getString("worldguard.disabled-message",
            "&cGem abilities are disabled in this region!");
    }

    /**
     * Check if WorldGuard is enabled
     */
    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }
}
