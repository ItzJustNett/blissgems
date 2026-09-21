/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.commands.FixedHeartsCommand;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import dev.xoperr.blissgems.utils.OraxenGemFixer;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class PlayerJoinListener
implements Listener {
    private final BlissGems plugin;
    private final Random random;

    public PlayerJoinListener(BlissGems plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (this.plugin.getGoldGemManager() != null) {
            this.plugin.getGoldGemManager().load(player.getUniqueId());
            this.plugin.getGoldGemManager().deliverPendingGems(player);
        }
        if (OraxenGemFixer.isFixOnJoinEnabled(this.plugin)) {
            this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                int fixed;
                if (player.isOnline() && (fixed = OraxenGemFixer.fixInventory(this.plugin, player)) > 0) {
                    this.plugin.getLogger().info("Replaced " + fixed + " legacy gem item(s) for " + player.getName());
                }
            }, 20L);
        }
        if (!this.hasReceivedFirstGem(player) && this.plugin.getGemManager().findGemInInventory(player) == null) {
            if (!this.plugin.getConfigManager().isSmpStarted()) {
                this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                    if (player.isOnline()) {
                        this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "smp-not-started", new Object[0]);
                    }
                }, 40L);
            } else {
                String randomGem = this.getRandomEnabledGem();
                if (randomGem != null) {
                    String finalGem = randomGem;
                    this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                        if (player.isOnline()) {
                            player.sendMessage("");
                            player.sendMessage("\u00a7d\u00a7l\u00a7m                                                  ");
                            player.sendMessage("\u00a7d\u00a7lWELCOME TO BLISSGEMS!");
                            player.sendMessage("");
                            player.sendMessage("\u00a77\u00a7oThe ancient gem ritual begins...");
                            player.sendMessage("\u00a77\u00a7oYour destiny is being forged...");
                            player.sendMessage("\u00a7d\u00a7l\u00a7m                                                  ");
                            player.sendMessage("");
                            this.plugin.getGemRitualManager().performGemRitual(player, finalGem, true, 1);
                            this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                                if (this.plugin.getGemManager().giveGem(player, finalGem, 1)) {
                                    this.markFirstGemReceived(player);
                                    String gemName = this.plugin.getGemManager().getGemDisplayName(finalGem);
                                    String gemColor = this.plugin.getGemManager().getGemColorCode(finalGem);
                                    String welcomeMsg = this.plugin.getConfigManager().getFormattedMessage("first-gem-received", "gem", gemName);
                                    if (welcomeMsg != null && !welcomeMsg.isEmpty()) {
                                        player.sendMessage(welcomeMsg);
                                    } else {
                                        player.sendMessage("");
                                        player.sendMessage("\u00a7d\u00a7l\u00bb \u00a7fYour gem has been chosen: " + gemColor + "\u00a7l" + gemName + "\u00a7d\u00a7l \u00ab");
                                        player.sendMessage("");
                                    }
                                    this.plugin.getLogger().info("Gave " + player.getName() + " their first gem: " + gemName);
                                }
                            }, 20L);
                        }
                    }, 40L);
                }
            }
        }
        if (!this.hasBeenGemLockChecked(player)) {
            String itemId;
            boolean fixed = false;
            for (int i = 0; i < player.getInventory().getSize(); ++i) {
                String itemId2;
                ItemStack item = player.getInventory().getItem(i);
                if (item == null || (itemId2 = CustomItemManager.getIdByItem(item)) == null || !GemType.isGem(itemId2) || !CustomItemManager.markAsUndroppable(item)) continue;
                fixed = true;
            }
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (offHand != null && (itemId = CustomItemManager.getIdByItem(offHand)) != null && GemType.isGem(itemId) && CustomItemManager.markAsUndroppable(offHand)) {
                fixed = true;
            }
            this.markGemLockChecked(player);
            if (fixed) {
                this.plugin.getLogger().info("Fixed undroppable tags on gems for " + player.getName());
            }
        }
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (this.plugin.getConfigManager().isFixedHeartsEnabled()) {
                FixedHeartsCommand.applyTenHearts(this.plugin, player);
                return;
            }
            if (this.plugin.getLifeAbilities() != null) {
                this.plugin.getLifeAbilities().cleanup(player);
            }
            if (this.plugin.getSoulManager() != null) {
                this.plugin.getSoulManager().cleanup(player);
            }
        }, 20L);
        this.plugin.getGemManager().updateActiveGem(player);
        this.plugin.getAbilityManager().loadCooldowns(player.getUniqueId());
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (player.isOnline()) {
                this.plugin.getPluginMessagingManager().sendGemData(player);
            }
        }, 20L);
        if (!this.hasSeenBindingTip(player)) {
            this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                player.sendMessage("");
                player.sendMessage("\u00a7d\u00a7l\u26a1 Tip: \u00a7fYou can customize how your gem abilities are triggered!");
                player.sendMessage("\u00a77Run \u00a7f/bliss ability \u00a77to see your current bindings.");
                player.sendMessage("\u00a77Change them with \u00a7f/bliss set_ability <slot> <input> \u00a77(e.g. \u00a7f/bliss set_ability primary left_click\u00a77).");
                player.sendMessage("");
                this.markBindingTipSeen(player);
            }, 100L);
        }
    }

    private boolean hasSeenBindingTip(Player player) {
        File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
        File file = new File(dataFolder, String.valueOf(player.getUniqueId()) + ".yml");
        if (!file.exists()) {
            return false;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration((File)file);
        return data.getBoolean("seen-binding-tip", false);
    }

    private void markBindingTipSeen(Player player) {
        File file;
        File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        YamlConfiguration data = (file = new File(dataFolder, String.valueOf(player.getUniqueId()) + ".yml")).exists() ? YamlConfiguration.loadConfiguration((File)file) : new YamlConfiguration();
        data.set("seen-binding-tip", (Object)true);
        try {
            data.save(file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to mark binding tip seen for " + player.getName() + ": " + e.getMessage());
        }
    }

    private boolean hasReceivedFirstGem(Player player) {
        File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
        File file = new File(dataFolder, String.valueOf(player.getUniqueId()) + ".yml");
        if (!file.exists()) {
            return false;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration((File)file);
        return data.getBoolean("received-first-gem", false);
    }

    private void markFirstGemReceived(Player player) {
        File file;
        File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        YamlConfiguration data = (file = new File(dataFolder, String.valueOf(player.getUniqueId()) + ".yml")).exists() ? YamlConfiguration.loadConfiguration((File)file) : new YamlConfiguration();
        data.set("received-first-gem", (Object)true);
        if (!data.contains("energy")) {
            data.set("energy", (Object)this.plugin.getConfigManager().getStartingEnergy());
        }
        try {
            data.save(file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save first gem status for " + player.getName() + ": " + e.getMessage());
        }
    }

    private boolean hasBeenGemLockChecked(Player player) {
        File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
        File file = new File(dataFolder, String.valueOf(player.getUniqueId()) + ".yml");
        if (!file.exists()) {
            return false;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration((File)file);
        return data.getBoolean("gems-lock-checked", false);
    }

    private void markGemLockChecked(Player player) {
        File file;
        File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        YamlConfiguration data = (file = new File(dataFolder, String.valueOf(player.getUniqueId()) + ".yml")).exists() ? YamlConfiguration.loadConfiguration((File)file) : new YamlConfiguration();
        data.set("gems-lock-checked", (Object)true);
        try {
            data.save(file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save gem-lock-checked for " + player.getName() + ": " + e.getMessage());
        }
    }

    private String getRandomEnabledGem() {
        List<String> enabledGems = this.plugin.getGemManager().getAvailableGemIds();
        if (enabledGems.isEmpty()) {
            return null;
        }
        return enabledGems.get(this.random.nextInt(enabledGems.size()));
    }
}

