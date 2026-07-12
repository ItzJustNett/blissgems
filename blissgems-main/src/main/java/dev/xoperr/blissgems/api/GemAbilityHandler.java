package dev.xoperr.blissgems.api;

import org.bukkit.entity.Player;

/**
 * Contract for gem ability handling. Addon plugins implement this to define
 * what happens when a player activates each ability slot.
 */
public interface GemAbilityHandler {

    /**
     * Primary ability (right-click / ability:main command).
     */
    void onPrimary(Player player, int tier);

    /**
     * Secondary ability (shift+right-click / ability:secondary command).
     * Default no-op for gems that don't have a secondary.
     */
    default void onSecondary(Player player, int tier) {}

    /**
     * Tertiary ability (F key / ability:tertiary command).
     * Default no-op for gems that don't have a tertiary.
     */
    default void onTertiary(Player player, int tier) {}

    /**
     * Quaternary ability (Shift+F key / ability:quaternary command).
     * Default no-op for gems that don't have a quaternary.
     */
    default void onQuaternary(Player player, int tier) {}

    /**
     * Called when a player's active gem changes away from this gem type,
     * or the player disconnects. Use this to cancel running tasks, remove entities, etc.
     */
    default void cleanup(Player player) {}
}
