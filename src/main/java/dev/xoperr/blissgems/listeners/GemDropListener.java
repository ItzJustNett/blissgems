/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryMoveItemEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.inventory.ItemStack
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class GemDropListener
implements Listener {
    private final BlissGems plugin;

    public GemDropListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        String itemId = CustomItemManager.getIdByItem(droppedItem);
        if ("flux_gem_t1".equals(itemId) || "flux_gem_t2".equals(itemId)) {
            event.setCancelled(true);
            if (this.plugin.getFluxEnergyManager() != null) {
                this.plugin.getFluxEnergyManager().openChargingStation(player);
            }
            return;
        }
        if (!this.plugin.getConfig().getBoolean("gems.prevent-drop", true)) {
            return;
        }
        boolean isLocked = CustomItemManager.isUndroppable(droppedItem);
        if (isLocked) {
            event.setCancelled(true);
            String message = this.plugin.getConfigManager().getFormattedMessage("cannot-drop-gem", new Object[0]);
            if (message != null && !message.isEmpty()) {
                player.sendMessage(message);
            } else {
                player.sendMessage("\u00a7c\u00a7lYou cannot drop your gem!");
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (CustomItemManager.isUndroppable(item)) {
            event.setCancelled(true);
        }
    }
}

