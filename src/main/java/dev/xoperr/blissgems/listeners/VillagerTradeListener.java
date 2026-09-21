/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.VillagerAcquireTradeEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.MerchantRecipe
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

public class VillagerTradeListener
implements Listener {
    private final BlissGems plugin;

    public VillagerTradeListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        if (event.getEntity().getRecipeCount() <= 0) {
            return;
        }
        Player player = this.findNearbyPlayerWithWealth(event.getEntity().getLocation());
        if (player == null) {
            return;
        }
        if (!this.plugin.getGemManager().hasGemType(player, GemType.WEALTH)) {
            return;
        }
        int tier = this.plugin.getGemManager().getGemTier(player);
        double discount = this.plugin.getConfigManager().getVillagerDiscount(tier);
        MerchantRecipe originalRecipe = event.getRecipe();
        MerchantRecipe discountedRecipe = this.applyDiscount(originalRecipe, discount);
        event.setRecipe(discountedRecipe);
    }

    private Player findNearbyPlayerWithWealth(Location location) {
        for (Player player : location.getWorld().getPlayers()) {
            if (!(player.getLocation().distance(location) <= 10.0) || !this.plugin.getGemManager().hasGemType(player, GemType.WEALTH)) continue;
            return player;
        }
        return null;
    }

    private MerchantRecipe applyDiscount(MerchantRecipe original, double discount) {
        MerchantRecipe discounted = new MerchantRecipe(original.getResult(), original.getUses(), original.getMaxUses(), original.hasExperienceReward(), original.getVillagerExperience(), original.getPriceMultiplier());
        for (ItemStack ingredient : original.getIngredients()) {
            if (ingredient == null) continue;
            ItemStack discountedIngredient = ingredient.clone();
            int originalAmount = ingredient.getAmount();
            int discountedAmount = Math.max(1, (int)((double)originalAmount * (1.0 - discount)));
            discountedIngredient.setAmount(discountedAmount);
            discounted.addIngredient(discountedIngredient);
        }
        return discounted;
    }
}

