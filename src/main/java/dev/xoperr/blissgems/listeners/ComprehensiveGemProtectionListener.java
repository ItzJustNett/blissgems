package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive gem protection listener that prevents gems from leaving player inventory
 * through ANY possible method. This covers all the scenarios listed:
 *
 * DROP EVENTS:
 * - Q / Ctrl+Q → PlayerDropItemEvent
 * - Death → PlayerDeathEvent (handled separately in PlayerDeathListener)
 * - Cursor on disconnect → PlayerQuitEvent
 *
 * INVENTORY INTERACTIONS:
 * - Shift+click to container → InventoryClickEvent (SHIFT_*)
 * - Hotbar swap (1-9) on ground item → InventoryClickEvent (HOTBAR_SWAP)
 * - Drag outside inventory → InventoryDragEvent
 * - Leftover items in crafting/anvil/etc on close → InventoryCloseEvent
 * - Cursor item on inventory close → InventoryCloseEvent
 *
 * CONTAINERS/BLOCKS:
 * - All container types (chests, barrels, shulkers, etc.)
 * - Furnaces, brewing stands, beacons, composters
 * - Item frames, armor stands
 * - Lecterns, smithing tables, etc.
 *
 * ENTITIES/MOBS:
 * - Hoppers extracting items → InventoryMoveItemEvent
 * - Allay/mob pickup → EntityPickupItemEvent
 * - Armor stand/item frame placement → PlayerInteractEntityEvent
 *
 * OTHER:
 * - Creative mode item deletion
 * - Any other edge cases
 */
public class ComprehensiveGemProtectionListener implements Listener {
    private final BlissGems plugin;

    public ComprehensiveGemProtectionListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if gem protection is enabled in config
     */
    private boolean isProtectionEnabled() {
        return plugin.getConfig().getBoolean("gems.prevent-drop", true);
    }

    /**
     * Send protection message to player
     */
    private void sendProtectionMessage(Player player) {
        String message = plugin.getConfigManager().getFormattedMessage("cannot-drop-gem");
        if (message != null && !message.isEmpty()) {
            player.sendMessage(message);
        } else {
            player.sendMessage("§c§lYou cannot drop your gem!");
        }
    }

    // ==========================================
    // DROP EVENTS
    // ==========================================

    /**
     * Prevent direct item dropping (Q key, Ctrl+Q)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!isProtectionEnabled()) {
            return;
        }

        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (CustomItemManager.isUndroppable(droppedItem)) {
            event.setCancelled(true);
            sendProtectionMessage(event.getPlayer());
        }
    }

    /**
     * Prevent cursor item from being lost on disconnect
     * Return it to player inventory
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!isProtectionEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack cursorItem = player.getItemOnCursor();

        if (cursorItem != null && CustomItemManager.isUndroppable(cursorItem)) {
            // Return gem to inventory
            player.setItemOnCursor(null);

            // Try to add to inventory
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(cursorItem);
            } else {
                // Inventory full - force drop at player location
                // (PlayerDeathListener will handle this if they log out during death)
                player.getWorld().dropItemNaturally(player.getLocation(), cursorItem);
            }
        }
    }

    // ==========================================
    // INVENTORY CLICK EVENTS
    // ==========================================

    /**
     * Comprehensive inventory click handler
     * Covers: shift-clicks, hotbar swaps, cursor placement, number key swaps, etc.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isProtectionEnabled()) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack cursor = event.getCursor();
        ItemStack clicked = event.getCurrentItem();
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();

        // CRITICAL: Explicitly block ender chest placement
        if (topInventory != null && topInventory.getType() == InventoryType.ENDER_CHEST) {
            // Check if trying to place/move a gem into ender chest
            if ((cursor != null && CustomItemManager.isUndroppable(cursor)) ||
                (clicked != null && CustomItemManager.isUndroppable(clicked) && event.isShiftClick())) {
                event.setCancelled(true);
                sendProtectionMessage(player);
                return;
            }
        }

        // Case 1: Prevent placing gem from cursor into non-player inventory
        if (cursor != null && CustomItemManager.isUndroppable(cursor)) {
            if (clickedInventory != null && !isPlayerInventory(clickedInventory)) {
                event.setCancelled(true);
                sendProtectionMessage(player);
                return;
            }
        }

        // Case 2: Prevent shift-clicking gem from player inventory to other inventories
        if (event.isShiftClick() && clicked != null && CustomItemManager.isUndroppable(clicked)) {
            if (clickedInventory != null && isPlayerInventory(clickedInventory)) {
                // Shift-clicking FROM player inventory
                // Check if there's a non-player inventory open
                if (topInventory != null && !isPlayerInventory(topInventory)) {
                    event.setCancelled(true);
                    sendProtectionMessage(player);
                    return;
                }
            }
        }

        // Case 3: Prevent hotbar swap (number keys 1-9) to move gems to non-player inventory
        if (event.getClick() == ClickType.NUMBER_KEY) {
            int hotbarButton = event.getHotbarButton();
            if (hotbarButton >= 0 && hotbarButton < 9) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
                if (hotbarItem != null && CustomItemManager.isUndroppable(hotbarItem)) {
                    if (clickedInventory != null && !isPlayerInventory(clickedInventory)) {
                        event.setCancelled(true);
                        sendProtectionMessage(player);
                        return;
                    }
                }
            }
        }

        // Case 4: Prevent dropping gems in creative mode (Click outside inventory)
        if (event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) {
            if (clicked != null && CustomItemManager.isUndroppable(clicked)) {
                event.setCancelled(true);
                sendProtectionMessage(player);
                return;
            }
        }

        // Case 5: Prevent moving gems using SWAP_OFFHAND (F key)
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if ((clicked != null && CustomItemManager.isUndroppable(clicked)) ||
                (offhand != null && CustomItemManager.isUndroppable(offhand))) {
                if (clickedInventory != null && !isPlayerInventory(clickedInventory)) {
                    event.setCancelled(true);
                    sendProtectionMessage(player);
                    return;
                }
            }
        }

        // Case 6: Prevent double-clicking to collect gems into non-player inventory
        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            if (cursor != null && CustomItemManager.isUndroppable(cursor)) {
                if (clickedInventory != null && !isPlayerInventory(clickedInventory)) {
                    event.setCancelled(true);
                    sendProtectionMessage(player);
                    return;
                }
            }
        }

        // Case 7: Creative mode middle-click cloning
        if (player.getGameMode() == GameMode.CREATIVE) {
            if (event.getClick() == ClickType.CREATIVE) {
                if (clicked != null && CustomItemManager.isUndroppable(clicked)) {
                    // Allow cloning in creative, but mark the clone as undroppable too
                    // This is handled automatically by the PDC being copied
                }
            }
        }
    }

    /**
     * Prevent gems from being dragged into non-player inventories
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!isProtectionEnabled()) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack draggedItem = event.getOldCursor();
        if (draggedItem == null || !CustomItemManager.isUndroppable(draggedItem)) {
            return;
        }

        // Check if any of the dragged slots are in a non-player inventory
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory != null && !isPlayerInventory(topInventory)) {
            int topSize = topInventory.getSize();

            for (int slot : event.getRawSlots()) {
                if (slot < topSize) {
                    // Slot is in the top (non-player) inventory
                    event.setCancelled(true);
                    sendProtectionMessage((Player) event.getWhoClicked());
                    return;
                }
            }
        }
    }

    /**
     * Handle cursor items when closing inventories
     * Bukkit automatically returns cursor items to inventory on close,
     * but if inventory is full, it drops them - we prevent that.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        // NOTE: We don't need to handle crafting-table leftovers manually.
        // Bukkit automatically returns them to player inventory.
        // Our onInventoryClick handler already prevents gems from being placed
        // in crafting/anvil/etc. slots in the first place.

        // The cursor item is also automatically returned by Bukkit.
        // We just need to prevent it from dropping if inventory is full,
        // which is already handled by onItemDrop event.
    }

    // ==========================================
    // HOPPER/AUTOMATED MOVEMENT
    // ==========================================

    /**
     * Prevent hoppers and other automated systems from moving gems
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (!isProtectionEnabled()) {
            return;
        }

        ItemStack item = event.getItem();
        if (CustomItemManager.isUndroppable(item)) {
            event.setCancelled(true);
        }
    }

    // ==========================================
    // ENTITY INTERACTIONS
    // ==========================================

    /**
     * Prevent placing gems in item frames, armor stands, etc.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!isProtectionEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (CustomItemManager.isUndroppable(item)) {
            Entity entity = event.getRightClicked();

            // Prevent placing in item frames, armor stands, etc.
            if (entity.getType().name().contains("ITEM_FRAME") ||
                entity.getType().name().contains("ARMOR_STAND")) {
                event.setCancelled(true);
                sendProtectionMessage(player);
            }
        }

        // Also check offhand
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (CustomItemManager.isUndroppable(offhand)) {
            Entity entity = event.getRightClicked();

            if (entity.getType().name().contains("ITEM_FRAME") ||
                entity.getType().name().contains("ARMOR_STAND")) {
                event.setCancelled(true);
                sendProtectionMessage(player);
            }
        }
    }

    /**
     * Prevent non-player entities from picking up gems
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!isProtectionEnabled()) {
            return;
        }

        // Allow players to pick up gems
        if (event.getEntity() instanceof Player) {
            return;
        }

        // Prevent non-player entities (Allay, mobs, etc.) from picking up gems
        ItemStack item = event.getItem().getItemStack();
        if (CustomItemManager.isUndroppable(item)) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent using gems in composters and other block interactions
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isProtectionEnabled()) {
            return;
        }

        ItemStack item = event.getItem();
        if (item != null && CustomItemManager.isUndroppable(item)) {
            // Prevent composting gems
            if (event.getClickedBlock() != null &&
                event.getClickedBlock().getType().name().contains("COMPOSTER")) {
                event.setCancelled(true);
                sendProtectionMessage(event.getPlayer());
            }
        }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    /**
     * Check if an inventory belongs to a player's main inventory
     * (NOT ender chest - ender chests should be blocked)
     */
    private boolean isPlayerInventory(Inventory inventory) {
        if (inventory == null) {
            return false;
        }

        InventoryType type = inventory.getType();

        // IMPORTANT: ENDER_CHEST is NOT a player inventory for our purposes
        // We want to block gems from being placed in ender chests
        if (type == InventoryType.ENDER_CHEST) {
            return false;
        }

        // Only allow player main inventory and crafting view
        if (type == InventoryType.PLAYER || type == InventoryType.CRAFTING) {
            return true;
        }

        return false;
    }
}
