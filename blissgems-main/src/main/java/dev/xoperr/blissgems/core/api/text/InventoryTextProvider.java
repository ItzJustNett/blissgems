package dev.xoperr.blissgems.core.api.text;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Interface for providing custom text based on items held by the player.
 * Implement this in your gems module to show custom text when gems are held.
 *
 * Example implementation:
 * <pre>
 * public class GemTextProvider implements InventoryTextProvider {
 *     {@literal @}Override
 *     public String getActionBarText(Player player, ItemStack mainHand, ItemStack offHand) {
 *         if (GemProtectionAPI.isGem(mainHand)) {
 *             String gemId = GemProtectionAPI.getGemId(mainHand);
 *             return "§6⚡ " + gemId + " Gem Active";
 *         }
 *         return null;
 *     }
 *
 *     {@literal @}Override
 *     public boolean shouldUpdate(Player player, ItemStack mainHand, ItemStack offHand) {
 *         return GemProtectionAPI.isGem(mainHand) || GemProtectionAPI.isGem(offHand);
 *     }
 * }
 * </pre>
 */
public interface InventoryTextProvider {

    /**
     * Get the action bar text to display for the player.
     * Called every tick when the player is holding items.
     *
     * @param player The player
     * @param mainHand Item in the main hand (can be null)
     * @param offHand Item in the off hand (can be null)
     * @return The text to display, or null to display nothing
     */
    String getActionBarText(Player player, ItemStack mainHand, ItemStack offHand);

    /**
     * Check if the text should be updated for this player.
     * Return false to skip processing for this player (optimization).
     *
     * @param player The player
     * @param mainHand Item in the main hand (can be null)
     * @param offHand Item in the off hand (can be null)
     * @return true if text should be updated, false otherwise
     */
    default boolean shouldUpdate(Player player, ItemStack mainHand, ItemStack offHand) {
        return true;
    }

    /**
     * Get the priority of this provider.
     * Higher priority providers are checked first.
     * Default priority is 0.
     *
     * @return The priority
     */
    default int getPriority() {
        return 0;
    }
}
