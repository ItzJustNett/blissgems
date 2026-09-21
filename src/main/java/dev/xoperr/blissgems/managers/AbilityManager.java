/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scheduler.BukkitTask
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class AbilityManager {
    private final BlissGems plugin;
    private final Map<UUID, Map<String, Long>> cooldowns;
    private final Map<UUID, Map<String, Long>> activeAbilities = new HashMap<UUID, Map<String, Long>>();
    private final Map<UUID, Map<String, BukkitTask>> durationTasks = new HashMap<UUID, Map<String, BukkitTask>>();
    private final File cooldownDataFolder;
    private final Set<UUID> noCooldown = new HashSet<UUID>();

    public AbilityManager(BlissGems plugin) {
        this.plugin = plugin;
        this.cooldowns = new HashMap<UUID, Map<String, Long>>();
        this.cooldownDataFolder = new File(plugin.getDataFolder(), "cooldowns");
        if (!this.cooldownDataFolder.exists()) {
            this.cooldownDataFolder.mkdirs();
        }
    }

    public boolean toggleNoCooldown(Player player) {
        UUID uuid = player.getUniqueId();
        if (this.noCooldown.remove(uuid)) {
            return false;
        }
        this.noCooldown.add(uuid);
        this.clearCooldowns(player);
        return true;
    }

    public boolean hasNoCooldown(Player player) {
        return this.noCooldown.contains(player.getUniqueId());
    }

    public boolean isOnCooldown(Player player, String abilityKey) {
        if (this.noCooldown.contains(player.getUniqueId())) {
            return false;
        }
        if (this.isAbilityActive(player, abilityKey)) {
            return true;
        }
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
        if (this.noCooldown.contains(player.getUniqueId())) {
            return 0;
        }
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
        if (this.noCooldown.contains(player.getUniqueId())) {
            return;
        }
        Map playerCooldowns = this.cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap());
        playerCooldowns.put(abilityKey, System.currentTimeMillis() + (long)seconds * 1000L);
        this.saveCooldowns(player.getUniqueId());
    }

    public boolean isAbilityActive(Player player, String abilityKey) {
        if (player == null || this.noCooldown.contains(player.getUniqueId())) {
            return false;
        }
        Map<String, Long> active = this.activeAbilities.get(player.getUniqueId());
        if (active == null) {
            return false;
        }
        Long endTime = active.get(abilityKey);
        if (endTime == null) {
            return false;
        }
        if (System.currentTimeMillis() >= endTime) {
            this.endAbilityDuration(player, abilityKey);
            return false;
        }
        return true;
    }

    public int getRemainingActiveDuration(Player player, String abilityKey) {
        if (player == null || this.noCooldown.contains(player.getUniqueId())) {
            return 0;
        }
        Map<String, Long> active = this.activeAbilities.get(player.getUniqueId());
        if (active == null) {
            return 0;
        }
        Long endTime = active.get(abilityKey);
        if (endTime == null) {
            return 0;
        }
        long remaining = endTime - System.currentTimeMillis();
        return remaining > 0L ? (int)Math.ceil((double)remaining / 1000.0) : 0;
    }

    public boolean canUseAbility(Player player, String abilityKey) {
        if (this.plugin.getRegionManager() != null && this.plugin.getRegionManager().areGemsDisabled(player)) {
            String message = this.plugin.getRegionManager().getDisabledMessage();
            if (message != null && !message.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)message));
            }
            return false;
        }
        if (!this.plugin.getEnergyManager().canUseAbilities(player)) {
            this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "ability-no-energy", new Object[0]);
            return false;
        }
        if (!abilityKey.startsWith("astra-") && this.plugin.getAstraAbilities().isAbilitySuppressed(player)) {
            player.sendMessage("\u00a74\u00a7l\u00a7oYour gem abilities are nullified by a Dimensional Void!");
            return false;
        }
        if (this.isAbilityActive(player, abilityKey)) {
            player.sendMessage("\u00a7c\u00a7oThis ability is currently active!");
            return false;
        }
        return !this.isOnCooldown(player, abilityKey);
    }

    public void useAbility(Player player, String abilityKey) {
        int cooldown = this.plugin.getConfigManager().getAbilityCooldown(abilityKey);
        this.setCooldown(player, abilityKey, cooldown);
    }

    public void useAbilityWithDuration(final Player player, final String abilityKey, int durationSeconds) {
        if (player == null) {
            return;
        }
        if (this.noCooldown.contains(player.getUniqueId())) {
            return;
        }
        UUID uuid = player.getUniqueId();
        Map playerTasks = this.durationTasks.computeIfAbsent(uuid, k -> new HashMap());
        BukkitTask oldTask = (BukkitTask)playerTasks.remove(abilityKey);
        if (oldTask != null) {
            oldTask.cancel();
        }
        if (durationSeconds <= 0) {
            this.useAbility(player, abilityKey);
            return;
        }
        Map playerActive = this.activeAbilities.computeIfAbsent(uuid, k -> new HashMap());
        playerActive.put(abilityKey, System.currentTimeMillis() + (long)durationSeconds * 1000L);
        BukkitTask task = new BukkitRunnable(){

            public void run() {
                AbilityManager.this.endAbilityDuration(player, abilityKey);
            }
        }.runTaskLater((Plugin)this.plugin, (long)durationSeconds * 20L);
        playerTasks.put(abilityKey, task);
    }

    public void endAbilityDuration(Player player, String abilityKey) {
        Map<String, Long> playerActive;
        boolean wasActive;
        BukkitTask task;
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        Map<String, BukkitTask> playerTasks = this.durationTasks.get(uuid);
        if (playerTasks != null && (task = playerTasks.remove(abilityKey)) != null) {
            task.cancel();
        }
        boolean bl = wasActive = (playerActive = this.activeAbilities.get(uuid)) != null && playerActive.remove(abilityKey) != null;
        if (wasActive && !this.noCooldown.contains(uuid)) {
            this.useAbility(player, abilityKey);
        }
    }

    public void clearCooldowns(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, BukkitTask> tasks = this.durationTasks.remove(uuid);
        if (tasks != null) {
            for (BukkitTask task : tasks.values()) {
                if (task == null) continue;
                task.cancel();
            }
        }
        this.activeAbilities.remove(uuid);
        this.cooldowns.remove(uuid);
        File cooldownFile = new File(this.cooldownDataFolder, String.valueOf(player.getUniqueId()) + ".yml");
        if (cooldownFile.exists()) {
            cooldownFile.delete();
        }
    }

    public void clearCache(UUID uuid) {
        Map<String, BukkitTask> tasks = this.durationTasks.remove(uuid);
        if (tasks != null) {
            for (BukkitTask task : tasks.values()) {
                if (task == null) continue;
                task.cancel();
            }
        }
        this.activeAbilities.remove(uuid);
        this.cooldowns.remove(uuid);
    }

    public void loadCooldowns(UUID uuid) {
        File cooldownFile = new File(this.cooldownDataFolder, String.valueOf(uuid) + ".yml");
        if (!cooldownFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration((File)cooldownFile);
        HashMap<String, Long> playerCooldowns = new HashMap<String, Long>();
        Set<String> keys = config.getKeys(false);
        if (keys != null) {
            for (String key : keys) {
                long endTime = config.getLong(key, 0L);
                if (endTime <= System.currentTimeMillis()) continue;
                playerCooldowns.put(key, endTime);
            }
        }
        if (!playerCooldowns.isEmpty()) {
            this.cooldowns.put(uuid, playerCooldowns);
        }
    }

    private void saveCooldowns(UUID uuid) {
        Map<String, Long> playerCooldowns = this.cooldowns.get(uuid);
        if (playerCooldowns == null || playerCooldowns.isEmpty()) {
            return;
        }
        File cooldownFile = new File(this.cooldownDataFolder, String.valueOf(uuid) + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Long> entry : playerCooldowns.entrySet()) {
            if (entry.getValue() <= System.currentTimeMillis()) continue;
            config.set(entry.getKey(), (Object)entry.getValue());
        }
        try {
            config.save(cooldownFile);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save cooldowns for " + String.valueOf(uuid) + ": " + e.getMessage());
        }
    }

    public Map<String, Long> getActiveAbilitiesFor(UUID uuid) {
        Map<String, Long> active = this.activeAbilities.get(uuid);
        if (active == null) {
            return java.util.Collections.emptyMap();
        }
        return new HashMap<>(active);
    }

    public void saveAllCooldowns() {
        for (UUID uuid : this.cooldowns.keySet()) {
            this.saveCooldowns(uuid);
        }
    }
}

