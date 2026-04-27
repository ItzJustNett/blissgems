package dev.xoperr.blissgems.api;

import org.bukkit.entity.Player;

/**
 * Contract for gem passive effects. Called periodically by PassiveManager
 * for each online player holding the gem in their offhand.
 */
public interface GemPassiveHandler {

    /**
     * Apply passive effects to the player.
     * @param player The player holding this gem
     * @param tier The gem tier (1 or 2)
     */
    void applyPassives(Player player, int tier);
}
