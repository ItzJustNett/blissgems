/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class TrustedPlayersManager {
    private final BlissGems plugin;
    private final Map<UUID, Set<UUID>> trustedPlayersCache = new HashMap<UUID, Set<UUID>>();
    private final File playerDataFolder;

    public TrustedPlayersManager(BlissGems plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!this.playerDataFolder.exists()) {
            this.playerDataFolder.mkdirs();
        }
    }

    public boolean isTrusted(Player player, Player target) {
        Set<UUID> trustedSet;
        if (player.getUniqueId().equals(target.getUniqueId())) {
            return true;
        }
        UUID playerUuid = player.getUniqueId();
        if (!this.trustedPlayersCache.containsKey(playerUuid)) {
            this.loadTrustedPlayers(player);
        }
        return (trustedSet = this.trustedPlayersCache.get(playerUuid)) != null && trustedSet.contains(target.getUniqueId());
    }

    public void addTrustedPlayer(Player player, Player target) {
        Set<UUID> trustedSet;
        UUID playerUuid = player.getUniqueId();
        if (!this.trustedPlayersCache.containsKey(playerUuid)) {
            this.loadTrustedPlayers(player);
        }
        if ((trustedSet = this.trustedPlayersCache.get(playerUuid)) == null) {
            trustedSet = new HashSet<UUID>();
            this.trustedPlayersCache.put(playerUuid, trustedSet);
        }
        trustedSet.add(target.getUniqueId());
        this.saveTrustedPlayers(player);
    }

    public boolean removeTrustedPlayer(Player player, Player target) {
        Set<UUID> trustedSet;
        UUID playerUuid = player.getUniqueId();
        if (!this.trustedPlayersCache.containsKey(playerUuid)) {
            this.loadTrustedPlayers(player);
        }
        if ((trustedSet = this.trustedPlayersCache.get(playerUuid)) == null) {
            return false;
        }
        boolean removed = trustedSet.remove(target.getUniqueId());
        if (removed) {
            this.saveTrustedPlayers(player);
        }
        return removed;
    }

    public Set<UUID> getTrustedPlayers(Player player) {
        Set<UUID> trustedSet;
        UUID playerUuid = player.getUniqueId();
        if (!this.trustedPlayersCache.containsKey(playerUuid)) {
            this.loadTrustedPlayers(player);
        }
        return (trustedSet = this.trustedPlayersCache.get(playerUuid)) != null ? new HashSet<UUID>(trustedSet) : new HashSet();
    }

    private void loadTrustedPlayers(Player player) {
        UUID playerUuid = player.getUniqueId();
        File playerFile = this.getPlayerFile(player);
        HashSet<UUID> trustedSet = new HashSet<UUID>();
        if (playerFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration((File)playerFile);
            List<String> trustedList = config.getStringList("trusted-players");
            for (String uuidString : trustedList) {
                try {
                    trustedSet.add(UUID.fromString(uuidString));
                }
                catch (IllegalArgumentException e) {
                    this.plugin.getLogger().warning("Invalid UUID in trusted players for " + player.getName() + ": " + uuidString);
                }
            }
        }
        this.trustedPlayersCache.put(playerUuid, trustedSet);
    }

    private void saveTrustedPlayers(Player player) {
        UUID playerUuid = player.getUniqueId();
        Set<UUID> trustedSet = this.trustedPlayersCache.get(playerUuid);
        if (trustedSet == null) {
            trustedSet = new HashSet<UUID>();
        }
        File playerFile = this.getPlayerFile(player);
        YamlConfiguration config = YamlConfiguration.loadConfiguration((File)playerFile);
        List trustedList = trustedSet.stream().map(UUID::toString).collect(Collectors.toList());
        config.set("trusted-players", trustedList);
        try {
            config.save(playerFile);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save trusted players for " + player.getName() + ": " + e.getMessage());
        }
    }

    private File getPlayerFile(Player player) {
        return new File(this.playerDataFolder, String.valueOf(player.getUniqueId()) + ".yml");
    }

    public void clearCache(Player player) {
        this.trustedPlayersCache.remove(player.getUniqueId());
    }
}

