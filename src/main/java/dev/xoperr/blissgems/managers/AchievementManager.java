package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Achievement;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AchievementManager {
    private final BlissGems plugin;
    private final File dataFolder;
    // playerUUID -> (achievement -> progress)
    private final Map<UUID, Map<Achievement, Integer>> progressCache = new HashMap<>();
    // playerUUID -> set of unlocked achievements
    private final Map<UUID, Set<Achievement>> unlockedCache = new HashMap<>();

    public AchievementManager(BlissGems plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "achievements");
        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs();
        }
    }

    public boolean isUnlocked(Player player, Achievement achievement) {
        return getUnlocked(player).contains(achievement);
    }

    public Set<Achievement> getUnlocked(Player player) {
        UUID uuid = player.getUniqueId();
        if (!unlockedCache.containsKey(uuid)) {
            loadPlayerData(player);
        }
        return unlockedCache.getOrDefault(uuid, new HashSet<>());
    }

    public int getProgress(Player player, Achievement achievement) {
        UUID uuid = player.getUniqueId();
        if (!progressCache.containsKey(uuid)) {
            loadPlayerData(player);
        }
        return progressCache.getOrDefault(uuid, new HashMap<>()).getOrDefault(achievement, 0);
    }

    /**
     * Add cumulative progress to an achievement. Auto-unlocks when target is reached.
     */
    public void addProgress(Player player, Achievement achievement, int amount) {
        if (isUnlocked(player, achievement)) return;

        UUID uuid = player.getUniqueId();
        Map<Achievement, Integer> playerProgress = progressCache.computeIfAbsent(uuid, k -> new HashMap<>());
        int current = playerProgress.getOrDefault(achievement, 0);
        int newProgress = current + amount;
        playerProgress.put(achievement, newProgress);

        if (newProgress >= achievement.getTargetProgress()) {
            unlock(player, achievement);
        }
    }

    /**
     * Set progress directly (useful for consecutive counters that can reset).
     */
    public void setProgress(Player player, Achievement achievement, int amount) {
        if (isUnlocked(player, achievement)) return;

        UUID uuid = player.getUniqueId();
        Map<Achievement, Integer> playerProgress = progressCache.computeIfAbsent(uuid, k -> new HashMap<>());
        playerProgress.put(achievement, amount);

        if (amount >= achievement.getTargetProgress()) {
            unlock(player, achievement);
        }
    }

    /**
     * Directly unlock an achievement (for one-shot triggers).
     */
    public void unlock(Player player, Achievement achievement) {
        if (isUnlocked(player, achievement)) return;

        UUID uuid = player.getUniqueId();
        Set<Achievement> unlocked = unlockedCache.computeIfAbsent(uuid, k -> new HashSet<>());
        unlocked.add(achievement);

        // Set progress to target for display purposes
        Map<Achievement, Integer> playerProgress = progressCache.computeIfAbsent(uuid, k -> new HashMap<>());
        playerProgress.put(achievement, achievement.getTargetProgress());

        // Notification
        player.sendMessage("\u00a76\u00a7l\u2b50 ACHIEVEMENT UNLOCKED! \u00a7e" + achievement.getDisplayName());
        player.sendMessage("\u00a77\u00a7o" + achievement.getDescription());
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0.1);

        savePlayerData(uuid);
    }

    private void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        File file = new File(dataFolder, uuid + ".yml");

        Set<Achievement> unlocked = new HashSet<>();
        Map<Achievement, Integer> progress = new HashMap<>();

        if (file.exists()) {
            FileConfiguration data = YamlConfiguration.loadConfiguration(file);

            for (Achievement achievement : Achievement.values()) {
                String key = achievement.name().toLowerCase();
                progress.put(achievement, data.getInt("progress." + key, 0));
                if (data.getBoolean("unlocked." + key, false)) {
                    unlocked.add(achievement);
                }
            }
        }

        unlockedCache.put(uuid, unlocked);
        progressCache.put(uuid, progress);
    }

    public void savePlayerData(UUID uuid) {
        File file = new File(dataFolder, uuid + ".yml");
        FileConfiguration data = new YamlConfiguration();

        Map<Achievement, Integer> progress = progressCache.getOrDefault(uuid, new HashMap<>());
        Set<Achievement> unlocked = unlockedCache.getOrDefault(uuid, new HashSet<>());

        for (Achievement achievement : Achievement.values()) {
            String key = achievement.name().toLowerCase();
            data.set("progress." + key, progress.getOrDefault(achievement, 0));
            data.set("unlocked." + key, unlocked.contains(achievement));
        }

        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save achievements for " + uuid + ": " + e.getMessage());
        }
    }

    public void saveAll() {
        for (UUID uuid : unlockedCache.keySet()) {
            savePlayerData(uuid);
        }
    }

    public void clearCache(UUID uuid) {
        progressCache.remove(uuid);
        unlockedCache.remove(uuid);
    }
}
