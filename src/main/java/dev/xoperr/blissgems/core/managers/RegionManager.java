/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sk89q.worldedit.bukkit.BukkitAdapter
 *  com.sk89q.worldguard.WorldGuard
 *  com.sk89q.worldguard.protection.ApplicableRegionSet
 *  com.sk89q.worldguard.protection.managers.RegionManager
 *  com.sk89q.worldguard.protection.regions.ProtectedRegion
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package dev.xoperr.blissgems.core.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class RegionManager {
    private final Plugin plugin;
    private final boolean worldGuardEnabled;
    private final boolean regionCheckEnabled;

    public RegionManager(Plugin plugin) {
        this.plugin = plugin;
        this.worldGuardEnabled = this.checkWorldGuard();
        this.regionCheckEnabled = plugin.getConfig().getBoolean("worldguard.enabled", true);
        if (this.worldGuardEnabled) {
            plugin.getLogger().info("WorldGuard detected - region protection enabled");
        } else {
            plugin.getLogger().info("WorldGuard not found - region protection disabled");
        }
    }

    private boolean checkWorldGuard() {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            return this.plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean areGemsDisabled(Player player) {
        return this.areGemsDisabled(player.getLocation());
    }

    public boolean areGemsDisabled(Location location) {
        if (!this.worldGuardEnabled || !this.regionCheckEnabled) {
            return false;
        }
        try {
            com.sk89q.worldguard.protection.managers.RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt((World)location.getWorld()));
            if (regionManager == null) {
                return false;
            }
            ApplicableRegionSet regions = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector((Location)location));
            return this.checkRegionFlags(regions);
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("Error checking WorldGuard regions: " + e.getMessage());
            return false;
        }
    }

    private boolean checkRegionFlags(ApplicableRegionSet regions) {
        String mode = this.plugin.getConfig().getString("worldguard.mode", "blacklist");
        List regionList = this.plugin.getConfig().getStringList("worldguard.regions");
        for (ProtectedRegion region : regions) {
            String regionId = region.getId();
            if (!(mode.equalsIgnoreCase("whitelist") ? !regionList.contains(regionId) : mode.equalsIgnoreCase("blacklist") && regionList.contains(regionId))) continue;
            return true;
        }
        return false;
    }

    public String getDisabledMessage() {
        return this.plugin.getConfig().getString("worldguard.disabled-message", "&cGem abilities are disabled in this region!");
    }

    public boolean isWorldGuardEnabled() {
        return this.worldGuardEnabled;
    }
}

