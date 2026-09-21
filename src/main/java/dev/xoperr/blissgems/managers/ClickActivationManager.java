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
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class ClickActivationManager {
    private final BlissGems plugin;
    private final Map<UUID, Boolean> clickActivationCache = new HashMap<UUID, Boolean>();
    private final File playerDataFolder;

    public ClickActivationManager(BlissGems plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!this.playerDataFolder.exists()) {
            this.playerDataFolder.mkdirs();
        }
    }

    public boolean isClickActivationEnabled(Player player) {
        UUID uuid = player.getUniqueId();
        Boolean cached = this.clickActivationCache.get(uuid);
        if (cached != null) {
            return cached;
        }
        boolean enabled = this.loadClickActivation(player);
        this.clickActivationCache.put(uuid, enabled);
        return enabled;
    }

    public boolean toggleClickActivation(Player player) {
        boolean newState = !this.isClickActivationEnabled(player);
        this.setClickActivation(player, newState);
        return newState;
    }

    public void setClickActivation(Player player, boolean enabled) {
        UUID uuid = player.getUniqueId();
        this.clickActivationCache.put(uuid, enabled);
        this.saveClickActivation(player, enabled);
    }

    private boolean loadClickActivation(Player player) {
        File playerFile = this.getPlayerFile(player);
        if (!playerFile.exists()) {
            return true;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration((File)playerFile);
        return config.getBoolean("click-activation-enabled", true);
    }

    private void saveClickActivation(Player player, boolean enabled) {
        File playerFile = this.getPlayerFile(player);
        YamlConfiguration config = YamlConfiguration.loadConfiguration((File)playerFile);
        config.set("click-activation-enabled", (Object)enabled);
        try {
            config.save(playerFile);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save click activation for " + player.getName() + ": " + e.getMessage());
        }
    }

    private File getPlayerFile(Player player) {
        return new File(this.playerDataFolder, String.valueOf(player.getUniqueId()) + ".yml");
    }

    public void clearCache(Player player) {
        this.clickActivationCache.remove(player.getUniqueId());
    }
}

