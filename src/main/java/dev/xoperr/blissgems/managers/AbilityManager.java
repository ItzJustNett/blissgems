/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class AbilityManager {
    private final BlissGems plugin;
    private final Map<UUID, Map<String, Long>> cooldowns;
    private final File cooldownDataFolder;

    public AbilityManager(BlissGems plugin) {
        this.plugin = plugin;
        this.cooldowns = new HashMap<UUID, Map<String, Long>>();
        this.cooldownDataFolder = new File(plugin.getDataFolder(), "cooldowns");
        if (!this.cooldownDataFolder.exists()) {
            this.cooldownDataFolder.mkdirs();
        }
    }

    public boolean isOnCooldown(Player player, String abilityKey) {
        Map<String, Long> playerCooldowns = this.cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            return false;
        }
        Long endTime = playerCooldowns.get(abilityKey);
        if (endTime == null) {
            return false;
        }
        return System.currentTimeMillis() < endTime;
    }

    public int getRemainingCooldown(Player player, String abilityKey) {
        Map<String, Long> playerCooldowns = this.cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            return 0;
        }
        Long endTime = playerCooldowns.get(abilityKey);
        if (endTime == null) {
            return 0;
        }
        long remaining = endTime - System.currentTimeMillis();
        return remaining > 0L ? (int)Math.ceil((double)remaining / 1000.0) : 0;
    }

    public void setCooldown(Player player, String abilityKey, int seconds) {
        Map playerCooldowns = this.cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap());
        playerCooldowns.put(abilityKey, System.currentTimeMillis() + (long)seconds * 1000L);
        // Persist cooldown to disk
        saveCooldowns(player.getUniqueId());
    }

    public boolean canUseAbility(Player player, String abilityKey) {
        if (!this.plugin.getEnergyManager().canUseAbilities(player)) {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-no-energy", new Object[0]));
            return false;
        }
        // Check if abilities are suppressed by Dimensional Void (skip check for astra abilities — void user isn't suppressed)
        if (!abilityKey.startsWith("astra-") && this.plugin.getAstraAbilities().isAbilitySuppressed(player)) {
            player.sendMessage("\u00a74\u00a7l\u00a7oYour gem abilities are nullified by a Dimensional Void!");
            return false;
        }
        if (this.isOnCooldown(player, abilityKey)) {
            // Cooldown is displayed in action bar, no need for chat message
            return false;
        }
        return true;
    }

    public void useAbility(Player player, String abilityKey) {
        int cooldown = this.plugin.getConfigManager().getAbilityCooldown(abilityKey);
        this.setCooldown(player, abilityKey, cooldown);
    }

    public void clearCooldowns(Player player) {
        this.cooldowns.remove(player.getUniqueId());
        // Also clear from disk
        File cooldownFile = new File(cooldownDataFolder, player.getUniqueId() + ".yml");
        if (cooldownFile.exists()) {
            cooldownFile.delete();
        }
    }

    public void clearCache(UUID uuid) {
        this.cooldowns.remove(uuid);
    }

    /**
     * Load cooldowns from disk for a player (called on join)
     */
    public void loadCooldowns(UUID uuid) {
        File cooldownFile = new File(cooldownDataFolder, uuid + ".yml");
        if (!cooldownFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(cooldownFile);
        Map<String, Long> playerCooldowns = new HashMap<>();

        Set<String> keys = config.getKeys(false);
        if (keys != null) {
            for (String key : keys) {
                long endTime = config.getLong(key, 0L);
                // Only load cooldowns that haven't expired yet
                if (endTime > System.currentTimeMillis()) {
                    playerCooldowns.put(key, endTime);
                }
            }
        }

        if (!playerCooldowns.isEmpty()) {
            this.cooldowns.put(uuid, playerCooldowns);
        }
    }

    /**
     * Save cooldowns to disk for a player
     */
    private void saveCooldowns(UUID uuid) {
        Map<String, Long> playerCooldowns = this.cooldowns.get(uuid);
        if (playerCooldowns == null || playerCooldowns.isEmpty()) {
            return;
        }

        File cooldownFile = new File(cooldownDataFolder, uuid + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        // Save only non-expired cooldowns
        for (Map.Entry<String, Long> entry : playerCooldowns.entrySet()) {
            if (entry.getValue() > System.currentTimeMillis()) {
                config.set(entry.getKey(), entry.getValue());
            }
        }

        try {
            config.save(cooldownFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save cooldowns for " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Save all player cooldowns (called on plugin disable)
     */
    public void saveAllCooldowns() {
        for (UUID uuid : cooldowns.keySet()) {
            saveCooldowns(uuid);
        }
    }
}

