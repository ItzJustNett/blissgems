package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class StatsManager {
    private final BlissGems plugin;
    private final Map<UUID, PlayerStats> playerStats = new HashMap<>();
    private final File statsFolder;

    public StatsManager(BlissGems plugin) {
        this.plugin = plugin;
        this.statsFolder = new File(plugin.getDataFolder(), "playerstats");
        if (!statsFolder.exists()) {
            statsFolder.mkdirs();
        }
        loadAllStats();
    }

    public static class PlayerStats {
        public int kills = 0;
        public int deaths = 0;
        public long timePlayedMs = 0;
        public long lastGemSwitchTime = System.currentTimeMillis();
    }

    public void recordKill(Player killer, Player victim) {
        PlayerStats stats = playerStats.computeIfAbsent(killer.getUniqueId(), k -> new PlayerStats());
        stats.kills++;
        saveStats(killer.getUniqueId());
    }

    public void recordDeath(Player victim) {
        PlayerStats stats = playerStats.computeIfAbsent(victim.getUniqueId(), k -> new PlayerStats());
        stats.deaths++;
        saveStats(victim.getUniqueId());
    }

    public void recordGemSwitch(Player player) {
        PlayerStats stats = playerStats.computeIfAbsent(player.getUniqueId(), k -> new PlayerStats());
        long now = System.currentTimeMillis();
        stats.timePlayedMs += (now - stats.lastGemSwitchTime);
        stats.lastGemSwitchTime = now;
        saveStats(player.getUniqueId());
    }

    public int getKills(Player player) {
        return playerStats.getOrDefault(player.getUniqueId(), new PlayerStats()).kills;
    }

    public int getDeaths(Player player) {
        return playerStats.getOrDefault(player.getUniqueId(), new PlayerStats()).deaths;
    }

    public long getTimePlayed(Player player) {
        PlayerStats stats = playerStats.getOrDefault(player.getUniqueId(), new PlayerStats());
        long total = stats.timePlayedMs;
        // Add current session time
        if (stats.lastGemSwitchTime > 0) {
            total += (System.currentTimeMillis() - stats.lastGemSwitchTime);
        }
        return total;
    }

    public List<Map.Entry<UUID, Integer>> getTopKillers(int limit) {
        return playerStats.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().kills, a.getValue().kills))
                .limit(limit)
                .map(e -> (Map.Entry<UUID, Integer>) new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().kills))
                .toList();
    }

    public Map<String, Integer> getGemUsageStats() {
        Map<String, Integer> usage = new HashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String gemType = plugin.getGemManager().getGemType(player) != null ?
                    plugin.getGemManager().getGemType(player).getDisplayName() : "None";
            usage.put(gemType, usage.getOrDefault(gemType, 0) + 1);
        }
        return usage;
    }

    private void saveStats(UUID playerUUID) {
        // Simple save - can be expanded to YAML later
    }

    private void loadAllStats() {
        // Load all stats from disk on startup
    }

    public void cleanup(UUID playerId) {
        recordGemSwitch(Bukkit.getPlayer(playerId));
    }
}
