package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.managers.GoldGemManager;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Keeps the Gold Gem to a single copy per server.
 *
 * With {@code gold.summon.once-per-server} on, the summon craft works exactly once. After
 * that the recipe still exists but refuses to produce anything - the gem that is out there is
 * the only one there will ever be, so losing it means hunting down whoever has it rather than
 * crafting a replacement.
 */
public class GoldSummonListener implements Listener {

    private final BlissGems plugin;

    public GoldSummonListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    private boolean isBlocked() {
        GoldGemManager gold = this.plugin.getGoldGemManager();
        return gold != null
            && this.plugin.getConfig().getBoolean("gold.summon.once-per-server", true)
            && gold.isSummoned();
    }

    private boolean isGoldGem(ItemStack item) {
        return item != null && GoldGemManager.GOLD_ITEM_ID.equals(CustomItemManager.getIdByItem(item));
    }

    /** Blank the result in the grid, so a spent summon reads as impossible rather than broken. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepare(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null || !this.isGoldGem(event.getInventory().getResult())) {
            return;
        }
        if (this.isBlocked()) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!this.isGoldGem(event.getRecipe().getResult())) {
            return;
        }
        if (this.isBlocked()) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                player.sendMessage(this.plugin.getConfigManager().getMessage("gold-already-summoned"));
            }
            return;
        }
        if (event.getWhoClicked() instanceof Player player) {
            this.plugin.getGoldGemManager().markSummoned(player);
            String announcement = this.plugin.getConfigManager().getMessage("gold-summoned");
            if (announcement != null && !announcement.isEmpty()) {
                this.plugin.getServer().broadcastMessage(announcement.replace("{player}", player.getName()));
            }
        }
    }
}
