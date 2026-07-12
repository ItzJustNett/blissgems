package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

/**
 * Handles Wealth gem villager trading discounts.
 * Players with Wealth gems get discounted prices on villager trades.
 */
public class VillagerTradeListener implements Listener {
    private final BlissGems plugin;

    public VillagerTradeListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        // Only apply discount if the recipe is being acquired by a player
        if (!(event.getEntity().getRecipeCount() > 0)) {
            return;
        }

        // Get the player who triggered this trade (if any)
        Player player = findNearbyPlayerWithWealth(event.getEntity().getLocation());
        if (player == null) {
            return;
        }

        // Check if player has Wealth gem
        if (!plugin.getGemManager().hasGemType(player, GemType.WEALTH)) {
            return;
        }

        // Get tier and discount percentage
        int tier = plugin.getGemManager().getGemTier(player);
        double discount = plugin.getConfigManager().getVillagerDiscount(tier);

        // Apply discount to the new recipe
        MerchantRecipe originalRecipe = event.getRecipe();
        MerchantRecipe discountedRecipe = applyDiscount(originalRecipe, discount);

        event.setRecipe(discountedRecipe);
    }

    /**
     * Find a nearby player with a Wealth gem (within 10 blocks)
     */
    private Player findNearbyPlayerWithWealth(org.bukkit.Location location) {
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distance(location) <= 10.0) {
                if (plugin.getGemManager().hasGemType(player, GemType.WEALTH)) {
                    return player;
                }
            }
        }
        return null;
    }

    /**
     * Apply discount to a merchant recipe by reducing ingredient amounts
     */
    private MerchantRecipe applyDiscount(MerchantRecipe original, double discount) {
        // Create new recipe with same result
        MerchantRecipe discounted = new MerchantRecipe(
            original.getResult(),
            original.getUses(),
            original.getMaxUses(),
            original.hasExperienceReward(),
            original.getVillagerExperience(),
            original.getPriceMultiplier()
        );

        // Apply discount to ingredients
        for (ItemStack ingredient : original.getIngredients()) {
            if (ingredient != null) {
                ItemStack discountedIngredient = ingredient.clone();
                int originalAmount = ingredient.getAmount();
                int discountedAmount = Math.max(1, (int)(originalAmount * (1.0 - discount)));
                discountedIngredient.setAmount(discountedAmount);
                discounted.addIngredient(discountedIngredient);
            }
        }

        return discounted;
    }
}
