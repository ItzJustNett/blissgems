package dev.xoperr.blissgems.core.api.text;

import dev.xoperr.blissgems.core.managers.TextManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Public API for displaying custom text to players.
 * Supports action bar messages, titles, and subtitles.
 *
 * This API provides the framework for showing text when gems are held.
 * The actual gem-specific text should be implemented in the gems module.
 *
 * Usage example:
 * <pre>
 * // Show action bar text
 * InventoryTextAPI.sendActionBar(player, "§6⚡ Speed Gem Active");
 *
 * // Show title
 * InventoryTextAPI.sendTitle(player, "§cFire Gem", "§7Right-click to shoot fireball", 10, 70, 20);
 *
 * // Clear displays
 * InventoryTextAPI.clearActionBar(player);
 * </pre>
 */
public class InventoryTextAPI {

    private static TextManager manager;

    /**
     * Initialize the API with a TextManager instance.
     * This is called internally by XoperrCore on plugin enable.
     *
     * @param textManager The manager instance
     */
    public static void initialize(TextManager textManager) {
        manager = textManager;
    }

    /**
     * Send an action bar message to a player.
     * This appears above the hotbar.
     *
     * @param player The player to send the message to
     * @param message The message (supports color codes with §)
     */
    public static void sendActionBar(Player player, String message) {
        checkInitialized();
        manager.sendActionBar(player, message);
    }

    /**
     * Send an action bar message using Adventure Component.
     *
     * @param player The player to send the message to
     * @param component The message component
     */
    public static void sendActionBar(Player player, Component component) {
        checkInitialized();
        manager.sendActionBar(player, component);
    }

    /**
     * Clear the action bar for a player.
     *
     * @param player The player to clear the action bar for
     */
    public static void clearActionBar(Player player) {
        checkInitialized();
        manager.clearActionBar(player);
    }

    /**
     * Send a title to a player.
     *
     * @param player The player to send the title to
     * @param title The main title text
     * @param subtitle The subtitle text (can be null)
     * @param fadeIn Fade in duration in ticks
     * @param stay Stay duration in ticks
     * @param fadeOut Fade out duration in ticks
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        checkInitialized();
        manager.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
    }

    /**
     * Send a title to a player using Adventure Components.
     *
     * @param player The player to send the title to
     * @param title The main title component
     * @param subtitle The subtitle component (can be null)
     * @param fadeIn Fade in duration in ticks
     * @param stay Stay duration in ticks
     * @param fadeOut Fade out duration in ticks
     */
    public static void sendTitle(Player player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        checkInitialized();
        manager.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
    }

    /**
     * Clear the title for a player.
     *
     * @param player The player to clear the title for
     */
    public static void clearTitle(Player player) {
        checkInitialized();
        manager.clearTitle(player);
    }

    /**
     * Register a custom text provider.
     * This allows external plugins to hook into the text system.
     *
     * @param provider The text provider to register
     */
    public static void registerTextProvider(InventoryTextProvider provider) {
        checkInitialized();
        manager.registerTextProvider(provider);
    }

    /**
     * Unregister a text provider.
     *
     * @param provider The text provider to unregister
     */
    public static void unregisterTextProvider(InventoryTextProvider provider) {
        checkInitialized();
        manager.unregisterTextProvider(provider);
    }

    private static void checkInitialized() {
        if (manager == null) {
            throw new IllegalStateException("InventoryTextAPI has not been initialized! Ensure XoperrCore is loaded.");
        }
    }
}
