package dev.xoperr.blissgems.core.managers;

import org.bukkit.plugin.Plugin;
import dev.xoperr.blissgems.core.api.text.InventoryTextProvider;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Manager class handling custom text display to players.
 * Supports action bars, titles, and custom text providers.
 */
public class TextManager {

    private final Plugin plugin;
    private final List<InventoryTextProvider> textProviders;
    private BukkitTask updateTask;

    public TextManager(Plugin plugin) {
        this.plugin = plugin;
        this.textProviders = new ArrayList<>();

        // Start the update task for continuous action bar updates
        startUpdateTask();
    }

    /**
     * Send an action bar message to a player.
     */
    public void sendActionBar(Player player, String message) {
        if (player == null || message == null) {
            return;
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    /**
     * Clear the action bar for a player.
     */
    public void clearActionBar(Player player) {
        if (player == null) {
            return;
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
    }

    /**
     * Send a title to a player.
     */
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null || title == null) {
            return;
        }

        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    /**
     * Clear the title for a player.
     */
    public void clearTitle(Player player) {
        if (player == null) {
            return;
        }

        player.resetTitle();
    }

    /**
     * Register a custom text provider.
     */
    public void registerTextProvider(InventoryTextProvider provider) {
        if (provider == null) {
            return;
        }

        textProviders.add(provider);
        // Sort by priority (highest first)
        textProviders.sort(Comparator.comparingInt(InventoryTextProvider::getPriority).reversed());
    }

    /**
     * Unregister a text provider.
     */
    public void unregisterTextProvider(InventoryTextProvider provider) {
        textProviders.remove(provider);
    }

    /**
     * Start the update task for continuous action bar updates.
     */
    private void startUpdateTask() {
        updateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateAllPlayers, 0L, 5L);
    }

    /**
     * Update action bar text for all online players based on registered providers.
     */
    private void updateAllPlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    /**
     * Update action bar text for a specific player.
     */
    private void updatePlayer(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        // Check all registered providers
        for (InventoryTextProvider provider : textProviders) {
            if (!provider.shouldUpdate(player, mainHand, offHand)) {
                continue;
            }

            String text = provider.getActionBarText(player, mainHand, offHand);
            if (text != null) {
                sendActionBar(player, text);
                return; // First provider wins
            }
        }
    }

    /**
     * Cleanup method called on plugin disable.
     */
    public void cleanup() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        textProviders.clear();
    }
}
