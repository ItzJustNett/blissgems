package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages per-player click activation toggle for gem abilities
 * When disabled, players must use commands to activate abilities
 */
public class ClickActivationManager {
    private final BlissGems plugin;
    private final Map<UUID, Boolean> clickActivationCache = new HashMap<>();
    private final File playerDataFolder;

    public ClickActivationManager(BlissGems plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");

        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }

    /**
     * Check if click activation is enabled for a player
     * @param player The player to check
     * @return true if click activation is enabled (default), false if disabled
     */
    public boolean isClickActivationEnabled(Player player) {
        UUID uuid = player.getUniqueId();

        // Check cache first
        if (clickActivationCache.containsKey(uuid)) {
            return clickActivationCache.get(uuid);
        }

        // Load from file
        boolean enabled = loadClickActivation(player);
        clickActivationCache.put(uuid, enabled);
        return enabled;
    }

    /**
     * Toggle click activation for a player
     * @param player The player
     * @return The new state (true = enabled, false = disabled)
     */
    public boolean toggleClickActivation(Player player) {
        boolean currentState = isClickActivationEnabled(player);
        boolean newState = !currentState;

        setClickActivation(player, newState);
        return newState;
    }

    /**
     * Set click activation state for a player
     * @param player The player
     * @param enabled true to enable, false to disable
     */
    public void setClickActivation(Player player, boolean enabled) {
        UUID uuid = player.getUniqueId();
        clickActivationCache.put(uuid, enabled);
        saveClickActivation(player, enabled);
    }

    /**
     * Load click activation state from player data file
     * @param player The player
     * @return true if enabled (default), false if disabled
     */
    private boolean loadClickActivation(Player player) {
        File playerFile = getPlayerFile(player);

        if (!playerFile.exists()) {
            return true; // Default: enabled
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        return config.getBoolean("click-activation-enabled", true);
    }

    /**
     * Save click activation state to player data file
     * @param player The player
     * @param enabled The state to save
     */
    private void saveClickActivation(Player player, boolean enabled) {
        File playerFile = getPlayerFile(player);
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        config.set("click-activation-enabled", enabled);

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save click activation for " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Get the player data file
     */
    private File getPlayerFile(Player player) {
        return new File(playerDataFolder, player.getUniqueId() + ".yml");
    }

    /**
     * Clear cache for a player (call on logout)
     */
    public void clearCache(Player player) {
        clickActivationCache.remove(player.getUniqueId());
    }
}
