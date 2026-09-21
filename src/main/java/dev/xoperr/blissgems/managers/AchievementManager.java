/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Achievement;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class AchievementManager {
    private final BlissGems plugin;
    private final File dataFolder;
    private final Map<UUID, Map<Achievement, Integer>> progressCache = new HashMap<UUID, Map<Achievement, Integer>>();
    private final Map<UUID, Set<Achievement>> unlockedCache = new HashMap<UUID, Set<Achievement>>();

    public AchievementManager(BlissGems plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "achievements");
        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs();
        }
    }

    public boolean isUnlocked(Player player, Achievement achievement) {
        return this.getUnlocked(player).contains((Object)achievement);
    }

    public Set<Achievement> getUnlocked(Player player) {
        UUID uuid = player.getUniqueId();
        Set<Achievement> unlocked = this.unlockedCache.get(uuid);
        if (unlocked == null) {
            this.loadPlayerData(player);
            unlocked = this.unlockedCache.get(uuid);
        }
        return unlocked;
    }

    public int getProgress(Player player, Achievement achievement) {
        UUID uuid = player.getUniqueId();
        Map<Achievement, Integer> progress = this.progressCache.get(uuid);
        if (progress == null) {
            this.loadPlayerData(player);
            progress = this.progressCache.get(uuid);
        }
        return progress.getOrDefault((Object)achievement, 0);
    }

    public void addProgress(Player player, Achievement achievement, int amount) {
        if (this.isUnlocked(player, achievement)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        Map<Achievement, Integer> playerProgress = this.progressCache.computeIfAbsent(uuid, k -> new HashMap<>());
        int newProgress = playerProgress.merge(achievement, amount, Integer::sum);
        if (newProgress >= achievement.getTargetProgress()) {
            this.unlock(player, achievement);
        }
    }

    public void setProgress(Player player, Achievement achievement, int amount) {
        if (this.isUnlocked(player, achievement)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        Map<Achievement, Integer> playerProgress = this.progressCache.computeIfAbsent(uuid, k -> new HashMap<>());
        playerProgress.put(achievement, amount);
        if (amount >= achievement.getTargetProgress()) {
            this.unlock(player, achievement);
        }
    }

    public void unlock(Player player, Achievement achievement) {
        if (this.isUnlocked(player, achievement)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        Set unlocked = this.unlockedCache.computeIfAbsent(uuid, k -> new HashSet());
        unlocked.add(achievement);
        Map playerProgress = this.progressCache.computeIfAbsent(uuid, k -> new HashMap());
        playerProgress.put(achievement, achievement.getTargetProgress());
        player.sendMessage("\u00a76\u00a7l\u2b50 ACHIEVEMENT UNLOCKED! \u00a7e" + achievement.getDisplayName());
        player.sendMessage("\u00a77\u00a7o" + achievement.getDescription());
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0.0, 1.0, 0.0), 40, 0.5, 0.5, 0.5, 0.1);
        this.savePlayerData(uuid);
    }

    private void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        File file = new File(this.dataFolder, String.valueOf(uuid) + ".yml");
        HashSet<Achievement> unlocked = new HashSet<Achievement>();
        HashMap<Achievement, Integer> progress = new HashMap<Achievement, Integer>();
        if (file.exists()) {
            YamlConfiguration data = YamlConfiguration.loadConfiguration((File)file);
            for (Achievement achievement : Achievement.values()) {
                String key = achievement.name().toLowerCase();
                progress.put(achievement, data.getInt("progress." + key, 0));
                if (!data.getBoolean("unlocked." + key, false)) continue;
                unlocked.add(achievement);
            }
        }
        this.unlockedCache.put(uuid, unlocked);
        this.progressCache.put(uuid, progress);
    }

    public void savePlayerData(UUID uuid) {
        File file = new File(this.dataFolder, String.valueOf(uuid) + ".yml");
        YamlConfiguration data = new YamlConfiguration();
        Map progress = this.progressCache.getOrDefault(uuid, Collections.emptyMap());
        Set unlocked = this.unlockedCache.getOrDefault(uuid, Collections.emptySet());
        for (Achievement achievement : Achievement.values()) {
            String key = achievement.name().toLowerCase();
            data.set("progress." + key, (Object)progress.getOrDefault((Object)achievement, 0));
            data.set("unlocked." + key, (Object)unlocked.contains((Object)achievement));
        }
        try {
            data.save(file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save achievements for " + String.valueOf(uuid) + ": " + e.getMessage());
        }
    }

    public void saveAll() {
        for (UUID uuid : this.unlockedCache.keySet()) {
            this.savePlayerData(uuid);
        }
    }

    public void clearCache(UUID uuid) {
        this.progressCache.remove(uuid);
        this.unlockedCache.remove(uuid);
    }
}

