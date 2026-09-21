/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import java.io.File;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class StatsManager {
    private final BlissGems plugin;
    private final Map<UUID, PlayerStats> playerStats = new HashMap<UUID, PlayerStats>();
    private final File statsFolder;

    public StatsManager(BlissGems plugin) {
        this.plugin = plugin;
        this.statsFolder = new File(plugin.getDataFolder(), "playerstats");
        if (!this.statsFolder.exists()) {
            this.statsFolder.mkdirs();
        }
        this.loadAllStats();
    }

    public void recordKill(Player killer, Player victim) {
        PlayerStats stats = this.playerStats.computeIfAbsent(killer.getUniqueId(), k -> new PlayerStats());
        ++stats.kills;
        this.saveStats(killer.getUniqueId());
    }

    public void recordDeath(Player victim) {
        PlayerStats stats = this.playerStats.computeIfAbsent(victim.getUniqueId(), k -> new PlayerStats());
        ++stats.deaths;
        this.saveStats(victim.getUniqueId());
    }

    public void recordGemSwitch(Player player) {
        PlayerStats stats = this.playerStats.computeIfAbsent(player.getUniqueId(), k -> new PlayerStats());
        long now = System.currentTimeMillis();
        stats.timePlayedMs += now - stats.lastGemSwitchTime;
        stats.lastGemSwitchTime = now;
        this.saveStats(player.getUniqueId());
    }

    public int getKills(Player player) {
        return this.playerStats.getOrDefault((Object)player.getUniqueId(), (PlayerStats)new PlayerStats()).kills;
    }

    public int getDeaths(Player player) {
        return this.playerStats.getOrDefault((Object)player.getUniqueId(), (PlayerStats)new PlayerStats()).deaths;
    }

    public long getTimePlayed(Player player) {
        PlayerStats stats = this.playerStats.getOrDefault(player.getUniqueId(), new PlayerStats());
        long total = stats.timePlayedMs;
        if (stats.lastGemSwitchTime > 0L) {
            total += System.currentTimeMillis() - stats.lastGemSwitchTime;
        }
        return total;
    }

    public List<Map.Entry<UUID, Integer>> getTopKillers(int limit) {
        return this.playerStats.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue().kills, a.getValue().kills)).limit(limit).<Map.Entry<UUID, Integer>>map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().kills)).toList();
    }

    public Map<String, Integer> getGemUsageStats() {
        HashMap<String, Integer> usage = new HashMap<String, Integer>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String gemType = this.plugin.getGemManager().getGemType(player) != null ? this.plugin.getGemManager().getGemType(player).getDisplayName() : "None";
            usage.put(gemType, usage.getOrDefault(gemType, 0) + 1);
        }
        return usage;
    }

    private void saveStats(UUID playerUUID) {
    }

    private void loadAllStats() {
    }

    public void cleanup(UUID playerId) {
        this.recordGemSwitch(Bukkit.getPlayer((UUID)playerId));
    }

    public static class PlayerStats {
        public int kills = 0;
        public int deaths = 0;
        public long timePlayedMs = 0L;
        public long lastGemSwitchTime = System.currentTimeMillis();
    }
}

