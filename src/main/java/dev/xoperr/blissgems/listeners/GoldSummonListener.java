/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.CraftItemEvent
 *  org.bukkit.event.inventory.PrepareItemCraftEvent
 *  org.bukkit.inventory.ItemStack
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.managers.GoldGemManager;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

public class GoldSummonListener
implements Listener {
    private final BlissGems plugin;

    public GoldSummonListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    private boolean isBlocked() {
        GoldGemManager gold = this.plugin.getGoldGemManager();
        return gold != null && this.plugin.getConfig().getBoolean("gold.summon.once-per-server", true) && gold.isSummoned();
    }

    private boolean isGoldGem(ItemStack item) {
        return item != null && "gold_gem_t1".equals(CustomItemManager.getIdByItem(item));
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onPrepare(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null || !this.isGoldGem(event.getInventory().getResult())) {
            return;
        }
        if (this.isBlocked()) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onCraft(CraftItemEvent event) {
        if (!this.isGoldGem(event.getRecipe().getResult())) {
            return;
        }
        if (this.isBlocked()) {
            event.setCancelled(true);
            HumanEntity humanEntity = event.getWhoClicked();
            if (humanEntity instanceof Player) {
                Player player = (Player)humanEntity;
                player.sendMessage(this.plugin.getConfigManager().getMessage("gold-already-summoned"));
            }
            return;
        }
        HumanEntity humanEntity = event.getWhoClicked();
        if (humanEntity instanceof Player) {
            Player player = (Player)humanEntity;
            this.plugin.getGoldGemManager().markSummoned(player);
            String announcement = this.plugin.getConfigManager().getMessage("gold-summoned");
            if (announcement != null && !announcement.isEmpty()) {
                this.plugin.getServer().broadcastMessage(announcement.replace("{player}", player.getName()));
            }
        }
    }
}

