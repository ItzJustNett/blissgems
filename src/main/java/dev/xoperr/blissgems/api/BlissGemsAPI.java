package dev.xoperr.blissgems.api;

import dev.xoperr.blissgems.managers.AbilityManager;
import dev.xoperr.blissgems.managers.EnergyManager;
import dev.xoperr.blissgems.managers.GemManager;
import dev.xoperr.blissgems.managers.TrustedPlayersManager;
import dev.xoperr.blissgems.utils.ConfigManager;
import org.bukkit.entity.Player;

/**
 * Main entry point for the BlissGems addon API.
 * Retrieve via Bukkit's ServicesManager:
 * <pre>
 *   BlissGemsAPI api = getServer().getServicesManager()
 *       .getRegistration(BlissGemsAPI.class).getProvider();
 * </pre>
 */
public interface BlissGemsAPI {

    /**
     * Get the gem registry for registering/querying gems.
     */
    GemRegistry getGemRegistry();

    /**
     * Get the ability manager for cooldown tracking.
     */
    AbilityManager getAbilityManager();

    /**
     * Get the energy manager for player energy operations.
     */
    EnergyManager getEnergyManager();

    /**
     * Get the config manager for reading configuration values.
     */
    ConfigManager getConfigManager();

    /**
     * Get the gem manager for active gem state.
     */
    GemManager getGemManager();

    /**
     * Get the trusted players manager.
     */
    TrustedPlayersManager getTrustedPlayersManager();

    /**
     * Check if a player currently has a specific gem (by gem ID).
     * Works for both built-in and addon gems.
     */
    boolean playerHasGem(Player player, String gemId);
}
