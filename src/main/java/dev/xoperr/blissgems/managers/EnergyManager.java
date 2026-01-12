/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.EnergyState;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class EnergyManager {
    private final BlissGems plugin;
    private final File dataFolder;
    private final Map<UUID, Integer> energyCache;

    public EnergyManager(BlissGems plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.energyCache = new HashMap<UUID, Integer>();
        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs();
        }
    }

    public int getEnergy(Player player) {
        return this.energyCache.computeIfAbsent(player.getUniqueId(), uuid -> {
            FileConfiguration data = this.loadPlayerData(player);
            return data.getInt("energy", this.plugin.getConfigManager().getStartingEnergy());
        });
    }

    public void setEnergy(Player player, int energy) {
        int maxEnergy = this.plugin.getConfigManager().getMaxEnergy();
        energy = Math.max(0, Math.min(maxEnergy, energy));
        this.energyCache.put(player.getUniqueId(), energy);
        this.savePlayerEnergy(player, energy);
        // Update gem textures to reflect new energy state
        this.plugin.getGemManager().updateGemTextures(player);
    }

    public void addEnergy(Player player, int amount) {
        this.setEnergy(player, this.getEnergy(player) + amount);
    }

    public void removeEnergy(Player player, int amount) {
        this.setEnergy(player, this.getEnergy(player) - amount);
    }

    public EnergyState getEnergyState(Player player) {
        return EnergyState.fromEnergy(this.getEnergy(player));
    }

    public boolean arePassivesActive(Player player) {
        return this.getEnergyState(player).passivesActive();
    }

    public boolean canUseAbilities(Player player) {
        return this.getEnergyState(player).abilitiesUsable();
    }

    private FileConfiguration loadPlayerData(Player player) {
        File file = new File(this.dataFolder, String.valueOf(player.getUniqueId()) + ".yml");
        if (!file.exists()) {
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration((File)file);
    }

    private void savePlayerEnergy(Player player, int energy) {
        File file = new File(this.dataFolder, String.valueOf(player.getUniqueId()) + ".yml");
        FileConfiguration data = this.loadPlayerData(player);
        data.set("energy", (Object)energy);
        try {
            data.save(file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save player data for " + player.getName() + ": " + e.getMessage());
        }
    }

    public void saveAll() {
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (!this.energyCache.containsKey(player.getUniqueId())) continue;
            this.savePlayerEnergy(player, this.energyCache.get(player.getUniqueId()));
        }
    }

    public void clearCache(UUID uuid) {
        this.energyCache.remove(uuid);
    }
}

