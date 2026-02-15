/*
 * PlayerJoinListener - Handles first-time gem distribution
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class PlayerJoinListener implements Listener {
    private final BlissGems plugin;
    private final Random random;

    public PlayerJoinListener(BlissGems plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if player has received their first gem
        if (!hasReceivedFirstGem(player)) {
            // Give random gem
            GemType randomGem = getRandomEnabledGem();
            if (randomGem != null) {
                // Give the gem (tier 1)
                if (this.plugin.getGemManager().giveGem(player, randomGem, 1)) {
                    // Mark that player has received their first gem
                    markFirstGemReceived(player);

                    // Send welcome message
                    player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("first-gem-received",
                        "gem", randomGem.getDisplayName()));

                    this.plugin.getLogger().info("Gave " + player.getName() + " their first gem: " + randomGem.getDisplayName());
                }
            }
        }

        // Update active gem status
        this.plugin.getGemManager().updateActiveGem(player);

        // Load ability cooldowns from disk
        this.plugin.getAbilityManager().loadCooldowns(player.getUniqueId());

        // Send gem data to client mod (if installed)
        // Small delay to ensure player is fully loaded
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            if (player.isOnline()) {
                this.plugin.getPluginMessagingManager().sendGemData(player);
            }
        }, 20L); // 1 second delay
    }

    private boolean hasReceivedFirstGem(Player player) {
        File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
        File file = new File(dataFolder, player.getUniqueId() + ".yml");

        if (!file.exists()) {
            return false;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        return data.getBoolean("received-first-gem", false);
    }

    private void markFirstGemReceived(Player player) {
        File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File file = new File(dataFolder, player.getUniqueId() + ".yml");
        FileConfiguration data;

        if (file.exists()) {
            data = YamlConfiguration.loadConfiguration(file);
        } else {
            data = new YamlConfiguration();
        }

        data.set("received-first-gem", true);

        // Also set starting energy if not already set
        if (!data.contains("energy")) {
            data.set("energy", this.plugin.getConfigManager().getStartingEnergy());
        }

        try {
            data.save(file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save first gem status for " + player.getName() + ": " + e.getMessage());
        }
    }

    private GemType getRandomEnabledGem() {
        ArrayList<GemType> enabledGems = new ArrayList<>();

        for (GemType type : GemType.values()) {
            if (this.plugin.getConfigManager().isGemEnabled(type)) {
                enabledGems.add(type);
            }
        }

        if (enabledGems.isEmpty()) {
            return null;
        }

        return enabledGems.get(this.random.nextInt(enabledGems.size()));
    }
}
