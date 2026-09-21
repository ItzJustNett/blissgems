/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.inventory.ItemStack
 */
package dev.xoperr.blissgems.core.listeners;

import dev.xoperr.blissgems.core.managers.ProtectionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class ItemDropListener
implements Listener {
    private final ProtectionManager protectionManager;

    public ItemDropListener(ProtectionManager protectionManager) {
        this.protectionManager = protectionManager;
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (this.protectionManager.isGem(droppedItem)) {
            event.setCancelled(true);
        }
    }
}

