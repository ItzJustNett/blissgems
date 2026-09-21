/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.GameMode
 *  org.bukkit.Material
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityPickupItemEvent
 *  org.bukkit.event.entity.ItemDespawnEvent
 *  org.bukkit.event.entity.ItemMergeEvent
 *  org.bukkit.event.entity.ItemSpawnEvent
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.inventory.InventoryMoveItemEvent
 *  org.bukkit.event.inventory.InventoryType
 *  org.bukkit.event.player.PlayerChangedWorldEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerInteractEntityEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.ArrayList;
import java.util.Iterator;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class ComprehensiveGemProtectionListener
implements Listener {
    private final BlissGems plugin;

    public ComprehensiveGemProtectionListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    private boolean isProtectionEnabled() {
        return this.plugin.getConfig().getBoolean("gems.prevent-drop", true);
    }

    private void sendProtectionMessage(Player player) {
        String message = this.plugin.getConfigManager().getFormattedMessage("cannot-drop-gem", new Object[0]);
        if (message != null && !message.isEmpty()) {
            player.sendMessage(message);
        } else {
            player.sendMessage("\u00a7c\u00a7lYou cannot drop your gem!");
        }
    }

    static void resyncOffhand(BlissGems plugin, Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        ItemStack copy = offhand == null ? new ItemStack(Material.AIR) : offhand.clone();
        plugin.getServer().getScheduler().runTask((Plugin)plugin, () -> {
            player.sendEquipmentChange((LivingEntity)player, EquipmentSlot.OFF_HAND, copy);
            player.updateInventory();
        });
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!this.isProtectionEnabled()) {
            return;
        }
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (CustomItemManager.isUndroppable(droppedItem)) {
            event.setCancelled(true);
            this.sendProtectionMessage(event.getPlayer());
            Player p = event.getPlayer();
            this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> ((Player)p).updateInventory());
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (this.plugin.getGemLockManager() != null) {
            this.plugin.getGemLockManager().clear(event.getPlayer().getUniqueId());
        }
        if (!this.isProtectionEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack cursorItem = player.getItemOnCursor();
        if (cursorItem != null && CustomItemManager.isUndroppable(cursorItem)) {
            player.setItemOnCursor(null);
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(new ItemStack[]{cursorItem});
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), cursorItem);
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack hotbarItem;
        int hotbarButton;
        if (!this.isProtectionEnabled()) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getWhoClicked();
        ItemStack cursor = event.getCursor();
        ItemStack clicked = event.getCurrentItem();
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory != null && topInventory.getType() == InventoryType.ENDER_CHEST && (cursor != null && CustomItemManager.isUndroppable(cursor) || clicked != null && CustomItemManager.isUndroppable(clicked) && event.isShiftClick())) {
            event.setCancelled(true);
            this.sendProtectionMessage(player);
            return;
        }
        if (cursor != null && CustomItemManager.isUndroppable(cursor) && clickedInventory != null && !this.isPlayerInventory(clickedInventory)) {
            event.setCancelled(true);
            this.sendProtectionMessage(player);
            return;
        }
        if (event.isShiftClick() && clicked != null && CustomItemManager.isUndroppable(clicked) && clickedInventory != null && this.isPlayerInventory(clickedInventory) && topInventory != null && !this.isPlayerInventory(topInventory)) {
            event.setCancelled(true);
            this.sendProtectionMessage(player);
            return;
        }
        if (event.getClick() == ClickType.NUMBER_KEY && (hotbarButton = event.getHotbarButton()) >= 0 && hotbarButton < 9 && (hotbarItem = player.getInventory().getItem(hotbarButton)) != null && CustomItemManager.isUndroppable(hotbarItem) && clickedInventory != null && !this.isPlayerInventory(clickedInventory)) {
            event.setCancelled(true);
            this.sendProtectionMessage(player);
            return;
        }
        if ((event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) && clicked != null && CustomItemManager.isUndroppable(clicked)) {
            event.setCancelled(true);
            this.sendProtectionMessage(player);
            return;
        }
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (clicked != null && CustomItemManager.isUndroppable(clicked) || offhand != null && CustomItemManager.isUndroppable(offhand)) {
                event.setCancelled(true);
                ComprehensiveGemProtectionListener.resyncOffhand(this.plugin, player);
                return;
            }
        }
        if (event.getClick() == ClickType.DOUBLE_CLICK && cursor != null && CustomItemManager.isUndroppable(cursor) && clickedInventory != null && !this.isPlayerInventory(clickedInventory)) {
            event.setCancelled(true);
            this.sendProtectionMessage(player);
            return;
        }
        if (player.getGameMode() != GameMode.CREATIVE || event.getClick() != ClickType.CREATIVE || clicked == null || CustomItemManager.isUndroppable(clicked)) {
            // empty if block
        }
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!this.isProtectionEnabled()) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        ItemStack draggedItem = event.getOldCursor();
        if (draggedItem == null || !CustomItemManager.isUndroppable(draggedItem)) {
            return;
        }
        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory != null && !this.isPlayerInventory(topInventory)) {
            int topSize = topInventory.getSize();
            Iterator iterator = event.getRawSlots().iterator();
            while (iterator.hasNext()) {
                int slot = (Integer)iterator.next();
                if (slot >= topSize) continue;
                event.setCancelled(true);
                this.sendProtectionMessage((Player)event.getWhoClicked());
                return;
            }
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onInventoryClose(InventoryCloseEvent event) {
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (!this.isProtectionEnabled()) {
            return;
        }
        ItemStack item = event.getItem();
        if (CustomItemManager.isUndroppable(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity;
        ItemStack offhand;
        Entity entity2;
        if (!this.isProtectionEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (CustomItemManager.isUndroppable(item) && ((entity2 = event.getRightClicked()).getType().name().contains("ITEM_FRAME") || entity2.getType().name().contains("ARMOR_STAND"))) {
            event.setCancelled(true);
            this.sendProtectionMessage(player);
        }
        if (CustomItemManager.isUndroppable(offhand = player.getInventory().getItemInOffHand()) && ((entity = event.getRightClicked()).getType().name().contains("ITEM_FRAME") || entity.getType().name().contains("ARMOR_STAND"))) {
            event.setCancelled(true);
            this.sendProtectionMessage(player);
        }
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!this.isProtectionEnabled()) {
            return;
        }
        if (event.getEntity() instanceof Player) {
            return;
        }
        ItemStack item = event.getItem().getItemStack();
        if (CustomItemManager.isUndroppable(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!this.isProtectionEnabled()) {
            return;
        }
        ItemStack item = event.getItem();
        if (item != null && CustomItemManager.isUndroppable(item) && event.getClickedBlock() != null && this.isItemStashingBlock(event.getClickedBlock().getType().name())) {
            event.setCancelled(true);
            this.sendProtectionMessage(event.getPlayer());
        }
    }

    private boolean isItemStashingBlock(String blockName) {
        return blockName.contains("COMPOSTER") || blockName.contains("SHELF") || blockName.contains("DECORATED_POT");
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!this.isProtectionEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            this.enforceOneGemOnly(player);
        }, 1L);
    }

    private void enforceOneGemOnly(Player player) {
        if (!this.plugin.getConfigManager().isSingleGemOnly()) {
            return;
        }
        boolean foundFirst = false;
        ArrayList<Integer> duplicateSlots = new ArrayList<Integer>();
        for (int i = 0; i < player.getInventory().getSize(); ++i) {
            String itemId;
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || (itemId = CustomItemManager.getIdByItem(item)) == null || !this.plugin.getGemManager().isAnyGem(itemId)) continue;
            if (!foundFirst) {
                foundFirst = true;
                continue;
            }
            duplicateSlots.add(i);
        }
        if (!duplicateSlots.isEmpty()) {
            Iterator iterator = duplicateSlots.iterator();
            while (iterator.hasNext()) {
                int slot = (Integer)iterator.next();
                player.getInventory().setItem(slot, null);
            }
            this.plugin.getLogger().info("Removed " + duplicateSlots.size() + " duplicate gem(s) from " + player.getName() + " after world change.");
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item itemEntity = event.getEntity();
        ItemStack stack = itemEntity.getItemStack();
        if (stack == null || !this.isGemLike(stack)) {
            return;
        }
        CustomItemManager.markAsUndroppable(stack);
        itemEntity.setItemStack(stack);
        try {
            itemEntity.setUnlimitedLifetime(true);
        }
        catch (NoSuchMethodError ignored) {
            itemEntity.setTicksLived(1);
        }
        itemEntity.setInvulnerable(true);
        itemEntity.setPersistent(true);
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void onItemDespawn(ItemDespawnEvent event) {
        ItemStack stack = event.getEntity().getItemStack();
        if (this.isGemLike(stack)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void onItemMerge(ItemMergeEvent event) {
        if (this.isGemLike(event.getEntity().getItemStack()) || this.isGemLike(event.getTarget().getItemStack())) {
            event.setCancelled(true);
        }
    }

    private boolean isGemLike(ItemStack item) {
        if (item == null) {
            return false;
        }
        if (CustomItemManager.isUndroppable(item)) {
            return true;
        }
        String id = CustomItemManager.getIdByItem(item);
        return id != null && this.plugin.getGemManager().isAnyGem(id);
    }

    private boolean isPlayerInventory(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        InventoryType type = inventory.getType();
        if (type == InventoryType.ENDER_CHEST) {
            return false;
        }
        return type == InventoryType.PLAYER || type == InventoryType.CRAFTING;
    }
}

