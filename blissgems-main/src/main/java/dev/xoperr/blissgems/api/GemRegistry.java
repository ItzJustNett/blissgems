package dev.xoperr.blissgems.api;

import java.util.Collection;
import java.util.List;

/**
 * Central registry for all gem types (built-in and addon).
 * Both built-in gems and addon gems register through this interface.
 */
public interface GemRegistry {

    /**
     * Register a new gem definition. For addon gems this also registers
     * the custom items via CustomItemManager.
     * @throws IllegalArgumentException if the gem ID is already registered
     */
    void registerGem(GemDefinition definition);

    /**
     * Register ability handlers for a gem.
     */
    void registerAbilities(String gemId, GemAbilityHandler handler);

    /**
     * Register passive effect handlers for a gem.
     */
    void registerPassives(String gemId, GemPassiveHandler handler);

    /**
     * Register cooldown display entries for a gem.
     */
    void registerCooldowns(String gemId, List<CooldownEntry> entries);

    /**
     * Get the definition for a gem by its ID (e.g. "fire", "ice").
     */
    GemDefinition getGem(String gemId);

    /**
     * Get the ability handler for a gem.
     */
    GemAbilityHandler getAbilityHandler(String gemId);

    /**
     * Get the passive handler for a gem.
     */
    GemPassiveHandler getPassiveHandler(String gemId);

    /**
     * Get the cooldown display entries for a gem.
     */
    List<CooldownEntry> getCooldownEntries(String gemId);

    /**
     * Get all registered gem definitions.
     */
    Collection<GemDefinition> getAllGems();

    /**
     * Check if an item ID (e.g. "ice_gem_t1") belongs to any registered gem.
     */
    boolean isRegisteredGem(String itemId);

    /**
     * Extract the gem ID from an item ID.
     * E.g. "ice_gem_t1" -> "ice", "fire_gem_t2" -> "fire".
     * Returns null if the item ID doesn't match any registered gem.
     */
    String gemIdFromItemId(String itemId);

    /**
     * Extract the tier from an item ID.
     * E.g. "ice_gem_t2" -> 2, "fire_gem_t1" -> 1.
     * Returns 1 as default.
     */
    int tierFromItemId(String itemId);
}
