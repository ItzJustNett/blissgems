/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryType
 *  org.bukkit.inventory.ItemStack
 */
package dev.xoperr.blissgems.core.listeners;

import dev.xoperr.blissgems.core.managers.ProtectionManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class InventoryInteractListener
implements Listener {
    private final ProtectionManager protectionManager;
    private final Set<InventoryType> restrictedInventories;
    private final Set<InventoryType> specialToolInventories;

    public InventoryInteractListener(ProtectionManager protectionManager) {
        this.protectionManager = protectionManager;
        this.restrictedInventories = new HashSet<InventoryType>(Arrays.asList(InventoryType.CHEST, InventoryType.ENDER_CHEST, InventoryType.DISPENSER, InventoryType.DROPPER, InventoryType.HOPPER, InventoryType.FURNACE, InventoryType.BREWING, InventoryType.BARREL, InventoryType.SHULKER_BOX, InventoryType.BLAST_FURNACE, InventoryType.SMOKER, InventoryType.LECTERN, InventoryType.STONECUTTER, InventoryType.COMPOSTER, InventoryType.CHISELED_BOOKSHELF));
        this.specialToolInventories = new HashSet<InventoryType>(Arrays.asList(InventoryType.GRINDSTONE, InventoryType.ENCHANTING, InventoryType.ANVIL, InventoryType.SMITHING, InventoryType.MERCHANT, InventoryType.LOOM, InventoryType.CARTOGRAPHY));
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        if (event.getClickedInventory() != null && event.getClickedInventory().getType() != InventoryType.PLAYER && cursorItem != null && !cursorItem.getType().isAir() && this.protectionManager.isGem(cursorItem) && (this.restrictedInventories.contains(event.getClickedInventory().getType()) || this.specialToolInventories.contains(event.getClickedInventory().getType()))) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() != null && event.isShiftClick() && event.getClickedInventory().getType() == InventoryType.PLAYER && clickedItem != null && !clickedItem.getType().isAir() && event.getView().getTopInventory().getType() != InventoryType.CRAFTING && this.protectionManager.isGem(clickedItem) && (this.restrictedInventories.contains(event.getView().getTopInventory().getType()) || this.specialToolInventories.contains(event.getView().getTopInventory().getType()))) {
            event.setCancelled(true);
        }
    }
}

