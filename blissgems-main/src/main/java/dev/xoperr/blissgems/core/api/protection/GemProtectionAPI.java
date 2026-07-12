package dev.xoperr.blissgems.core.api.protection;

import dev.xoperr.blissgems.core.managers.ProtectionManager;
import org.bukkit.inventory.ItemStack;

/**
 * Public API for protecting items (gems) from being dropped, stored, or modified.
 * Uses PersistentDataContainer to mark items as protected.
 *
 * Usage:
 * <pre>
 * ItemStack gem = new ItemStack(Material.DIAMOND);
 * GemProtectionAPI.markAsGem(gem);
 * boolean isGem = GemProtectionAPI.isGem(gem); // true
 * </pre>
 */
public class GemProtectionAPI {

    private static ProtectionManager manager;

    /**
     * Initialize the API with a ProtectionManager instance.
     * This is called internally by XoperrCore on plugin enable.
     *
     * @param protectionManager The manager instance
     */
    public static void initialize(ProtectionManager protectionManager) {
        manager = protectionManager;
    }

    /**
     * Mark an item as a gem, preventing it from being dropped or moved.
     *
     * @param item The item to protect
     * @return true if successfully marked, false if item is null or already marked
     */
    public static boolean markAsGem(ItemStack item) {
        checkInitialized();
        return manager.markAsGem(item);
    }

    /**
     * Remove gem protection from an item.
     *
     * @param item The item to unprotect
     * @return true if successfully unmarked, false if item is null or not protected
     */
    public static boolean unmarkGem(ItemStack item) {
        checkInitialized();
        return manager.unmarkGem(item);
    }

    /**
     * Check if an item is marked as a gem.
     *
     * @param item The item to check
     * @return true if the item is protected, false otherwise
     */
    public static boolean isGem(ItemStack item) {
        checkInitialized();
        return manager.isGem(item);
    }

    /**
     * Mark an item as a gem with additional custom data.
     * Useful for storing gem type, tier, or other metadata.
     *
     * @param item The item to protect
     * @param gemId Unique identifier for the gem type (e.g., "astra", "fire", "speed")
     * @param tier The gem tier (1 or 2)
     * @return true if successfully marked, false otherwise
     */
    public static boolean markAsGem(ItemStack item, String gemId, int tier) {
        checkInitialized();
        return manager.markAsGem(item, gemId, tier);
    }

    /**
     * Get the gem ID from a protected item.
     *
     * @param item The item to check
     * @return The gem ID (e.g., "astra", "fire"), or null if not a gem
     */
    public static String getGemId(ItemStack item) {
        checkInitialized();
        return manager.getGemId(item);
    }

    /**
     * Get the gem tier from a protected item.
     *
     * @param item The item to check
     * @return The gem tier (1 or 2), or 0 if not a gem
     */
    public static int getGemTier(ItemStack item) {
        checkInitialized();
        return manager.getGemTier(item);
    }

    private static void checkInitialized() {
        if (manager == null) {
            throw new IllegalStateException("GemProtectionAPI has not been initialized! Ensure XoperrCore is loaded.");
        }
    }
}
