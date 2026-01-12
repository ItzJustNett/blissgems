package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages trusted player lists to prevent friendly fire from gem abilities
 * Players can trust others to prevent negative gem effects from affecting them
 */
public class TrustedPlayersManager {
    private final BlissGems plugin;
    private final Map<UUID, Set<UUID>> trustedPlayersCache = new HashMap<>();
    private final File playerDataFolder;

    public TrustedPlayersManager(BlissGems plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");

        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }

    /**
     * Check if a player trusts another player
     * @param player The player who owns the gem
     * @param target The target player who might be affected
     * @return true if target is trusted (should NOT be harmed), false otherwise
     */
    public boolean isTrusted(Player player, Player target) {
        // Players always trust themselves
        if (player.getUniqueId().equals(target.getUniqueId())) {
            return true;
        }

        UUID playerUuid = player.getUniqueId();

        // Check cache
        if (!trustedPlayersCache.containsKey(playerUuid)) {
            loadTrustedPlayers(player);
        }

        Set<UUID> trustedSet = trustedPlayersCache.get(playerUuid);
        return trustedSet != null && trustedSet.contains(target.getUniqueId());
    }

    /**
     * Add a player to the trusted list
     * @param player The player who is trusting
     * @param target The player to trust
     */
    public void addTrustedPlayer(Player player, Player target) {
        UUID playerUuid = player.getUniqueId();

        if (!trustedPlayersCache.containsKey(playerUuid)) {
            loadTrustedPlayers(player);
        }

        Set<UUID> trustedSet = trustedPlayersCache.get(playerUuid);
        if (trustedSet == null) {
            trustedSet = new HashSet<>();
            trustedPlayersCache.put(playerUuid, trustedSet);
        }

        trustedSet.add(target.getUniqueId());
        saveTrustedPlayers(player);
    }

    /**
     * Remove a player from the trusted list
     * @param player The player who is un-trusting
     * @param target The player to un-trust
     * @return true if player was in list and removed, false otherwise
     */
    public boolean removeTrustedPlayer(Player player, Player target) {
        UUID playerUuid = player.getUniqueId();

        if (!trustedPlayersCache.containsKey(playerUuid)) {
            loadTrustedPlayers(player);
        }

        Set<UUID> trustedSet = trustedPlayersCache.get(playerUuid);
        if (trustedSet == null) {
            return false;
        }

        boolean removed = trustedSet.remove(target.getUniqueId());
        if (removed) {
            saveTrustedPlayers(player);
        }

        return removed;
    }

    /**
     * Get all trusted players for a player
     * @param player The player
     * @return Set of trusted player UUIDs
     */
    public Set<UUID> getTrustedPlayers(Player player) {
        UUID playerUuid = player.getUniqueId();

        if (!trustedPlayersCache.containsKey(playerUuid)) {
            loadTrustedPlayers(player);
        }

        Set<UUID> trustedSet = trustedPlayersCache.get(playerUuid);
        return trustedSet != null ? new HashSet<>(trustedSet) : new HashSet<>();
    }

    /**
     * Load trusted players from player data file
     * @param player The player
     */
    private void loadTrustedPlayers(Player player) {
        UUID playerUuid = player.getUniqueId();
        File playerFile = getPlayerFile(player);

        Set<UUID> trustedSet = new HashSet<>();

        if (playerFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            List<String> trustedList = config.getStringList("trusted-players");

            for (String uuidString : trustedList) {
                try {
                    trustedSet.add(UUID.fromString(uuidString));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in trusted players for " + player.getName() + ": " + uuidString);
                }
            }
        }

        trustedPlayersCache.put(playerUuid, trustedSet);
    }

    /**
     * Save trusted players to player data file
     * @param player The player
     */
    private void saveTrustedPlayers(Player player) {
        UUID playerUuid = player.getUniqueId();
        Set<UUID> trustedSet = trustedPlayersCache.get(playerUuid);

        if (trustedSet == null) {
            trustedSet = new HashSet<>();
        }

        File playerFile = getPlayerFile(player);
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        // Convert UUIDs to strings
        List<String> trustedList = trustedSet.stream()
                .map(UUID::toString)
                .collect(java.util.stream.Collectors.toList());

        config.set("trusted-players", trustedList);

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save trusted players for " + player.getName() + ": " + e.getMessage());
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
        trustedPlayersCache.remove(player.getUniqueId());
    }
}
