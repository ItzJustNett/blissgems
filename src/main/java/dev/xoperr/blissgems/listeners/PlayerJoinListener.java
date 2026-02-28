/*
 * PlayerJoinListener - Handles first-time gem distribution
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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

        // Check if player has received their first gem and SMP has started
        if (!hasReceivedFirstGem(player)) {
            if (!this.plugin.getConfigManager().isSmpStarted()) {
                // SMP hasn't started yet, notify the player
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                    if (player.isOnline()) {
                        this.plugin.getConfigManager().sendFormattedMessage(player, "smp-not-started");
                    }
                }, 40L);
            } else {
                // Give random gem
                GemType randomGem = getRandomEnabledGem();
                if (randomGem != null) {
                    // Delay to ensure player is fully loaded
                    final GemType finalGem = randomGem;
                    this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                        if (player.isOnline()) {
                            // Welcome messages
                            player.sendMessage("");
                            player.sendMessage("\u00a7d\u00a7l\u00a7m                                                  ");
                            player.sendMessage("\u00a7d\u00a7lWELCOME TO BLISSGEMS!");
                            player.sendMessage("");
                            player.sendMessage("\u00a77\u00a7oThe ancient gem ritual begins...");
                            player.sendMessage("\u00a77\u00a7oYour destiny is being forged...");
                            player.sendMessage("\u00a7d\u00a7l\u00a7m                                                  ");
                            player.sendMessage("");

                            // Start the ritual animation
                            this.plugin.getGemRitualManager().performGemRitual(player, finalGem, true);

                            // Give the gem after a short delay (let ritual build up)
                            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                                if (this.plugin.getGemManager().giveGem(player, finalGem, 1)) {
                                    // Mark that player has received their first gem
                                    markFirstGemReceived(player);

                                    // Send welcome message
                                    String welcomeMsg = this.plugin.getConfigManager().getFormattedMessage("first-gem-received",
                                        "gem", finalGem.getDisplayName());
                                    if (welcomeMsg != null && !welcomeMsg.isEmpty()) {
                                        player.sendMessage(welcomeMsg);
                                    } else {
                                        player.sendMessage("");
                                        player.sendMessage("\u00a7d\u00a7l\u00bb \u00a7fYour gem has been chosen: " + finalGem.getColor() + "\u00a7l" + finalGem.getDisplayName() + "\u00a7d\u00a7l \u00ab");
                                        player.sendMessage("");
                                    }

                                    this.plugin.getLogger().info("Gave " + player.getName() + " their first gem: " + finalGem.getDisplayName());
                                }
                            }, 20L); // 1 second delay
                        }
                    }, 40L); // 2 second delay after join
                }
            }
        }

        // One-time migration: ensure all existing gems have the undroppable PDC tag
        if (!hasBeenGemLockChecked(player)) {
            boolean fixed = false;
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item == null) continue;
                String itemId = CustomItemManager.getIdByItem(item);
                if (itemId != null && GemType.isGem(itemId)) {
                    if (CustomItemManager.markAsUndroppable(item)) {
                        fixed = true;
                    }
                }
            }
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (offHand != null) {
                String itemId = CustomItemManager.getIdByItem(offHand);
                if (itemId != null && GemType.isGem(itemId)) {
                    if (CustomItemManager.markAsUndroppable(offHand)) {
                        fixed = true;
                    }
                }
            }
            markGemLockChecked(player);
            if (fixed) {
                this.plugin.getLogger().info("Fixed undroppable tags on gems for " + player.getName());
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

    private boolean hasBeenGemLockChecked(Player player) {
        File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
        File file = new File(dataFolder, player.getUniqueId() + ".yml");

        if (!file.exists()) {
            return false;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        return data.getBoolean("gems-lock-checked", false);
    }

    private void markGemLockChecked(Player player) {
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

        data.set("gems-lock-checked", true);

        try {
            data.save(file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save gem-lock-checked for " + player.getName() + ": " + e.getMessage());
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
