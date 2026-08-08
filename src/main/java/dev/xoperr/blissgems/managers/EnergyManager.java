package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Achievement;
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
        this.energyCache = new HashMap<>();
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
        this.plugin.getGemManager().updateGemTextures(player);

        if (this.plugin.getAchievementManager() != null) {
            if (energy == 0) {
                this.plugin.getAchievementManager().unlock(player, Achievement.SHATTERED);
            }
            if (energy == maxEnergy) {
                this.plugin.getAchievementManager().unlock(player, Achievement.OVERFLOWING);
            }
        }
    }

    public void addEnergy(Player player, int amount) {
        this.setEnergy(player, this.getEnergy(player) + amount);
    }

    public void removeEnergy(Player player, int amount) {
        int current = this.getEnergy(player);
        if (current <= 0) {
            return;
        }
        this.setEnergy(player, current - amount);
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

    private File playerFile(Player player) {
        return new File(this.dataFolder, player.getUniqueId() + ".yml");
    }

    private FileConfiguration loadPlayerData(Player player) {
        File file = this.playerFile(player);
        if (!file.exists()) {
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void savePlayerEnergy(Player player, int energy) {
        File file = this.playerFile(player);
        FileConfiguration data = this.loadPlayerData(player);
        data.set("energy", energy);
        try {
            data.save(file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save player data for " + player.getName() + ": " + e.getMessage());
        }
    }

    public void saveAll() {
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            Integer cached = this.energyCache.get(player.getUniqueId());
            if (cached == null) continue;
            this.savePlayerEnergy(player, cached);
        }
    }

    public void clearCache(UUID uuid) {
        this.energyCache.remove(uuid);
    }
}

