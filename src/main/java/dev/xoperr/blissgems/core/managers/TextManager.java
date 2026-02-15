package dev.xoperr.blissgems.core.managers;

import org.bukkit.plugin.Plugin;
import dev.xoperr.blissgems.core.api.text.InventoryTextProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
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

        Component component = LegacyComponentSerializer.legacySection().deserialize(message);
        player.sendActionBar(component);
    }

    /**
     * Send an action bar message using Adventure Component.
     */
    public void sendActionBar(Player player, Component component) {
        if (player == null || component == null) {
            return;
        }

        player.sendActionBar(component);
    }

    /**
     * Clear the action bar for a player.
     */
    public void clearActionBar(Player player) {
        if (player == null) {
            return;
        }

        player.sendActionBar(Component.empty());
    }

    /**
     * Send a title to a player.
     */
    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null || title == null) {
            return;
        }

        Component titleComponent = LegacyComponentSerializer.legacySection().deserialize(title);
        Component subtitleComponent = subtitle != null ?
                LegacyComponentSerializer.legacySection().deserialize(subtitle) : Component.empty();

        sendTitle(player, titleComponent, subtitleComponent, fadeIn, stay, fadeOut);
    }

    /**
     * Send a title to a player using Adventure Components.
     */
    public void sendTitle(Player player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null || title == null) {
            return;
        }

        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L)
        );

        Title titleObj = Title.title(
                title,
                subtitle != null ? subtitle : Component.empty(),
                times
        );

        player.showTitle(titleObj);
    }

    /**
     * Clear the title for a player.
     */
    public void clearTitle(Player player) {
        if (player == null) {
            return;
        }

        player.clearTitle();
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
