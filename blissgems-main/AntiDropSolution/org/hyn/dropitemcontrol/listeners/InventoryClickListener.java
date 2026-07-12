package org.hyn.dropitemcontrol.listeners;

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
import org.hyn.dropitemcontrol.Dropitemcontrol;
import org.hyn.dropitemcontrol.managers.ItemLockManager;
import org.hyn.dropitemcontrol.managers.PluginStateManager;
import org.hyn.dropitemcontrol.utils.MessageUtils;

public class InventoryClickListener implements Listener {
   private final Dropitemcontrol plugin;
   private final ItemLockManager lockManager;
   private final MessageUtils messageUtils;
   private final PluginStateManager stateManager;
   private final Set<InventoryType> restrictedInventories;
   private final Set<InventoryType> specialToolInventories;

   public InventoryClickListener(Dropitemcontrol plugin, ItemLockManager lockManager, MessageUtils messageUtils, PluginStateManager stateManager) {
      this.plugin = plugin;
      this.lockManager = lockManager;
      this.messageUtils = messageUtils;
      this.stateManager = stateManager;
      this.restrictedInventories = new HashSet(Arrays.asList(InventoryType.CHEST, InventoryType.DISPENSER, InventoryType.DROPPER, InventoryType.HOPPER, InventoryType.FURNACE, InventoryType.BREWING, InventoryType.BARREL, InventoryType.SHULKER_BOX, InventoryType.BLAST_FURNACE, InventoryType.SMOKER, InventoryType.LECTERN, InventoryType.STONECUTTER));
      this.specialToolInventories = new HashSet(Arrays.asList(InventoryType.GRINDSTONE, InventoryType.ENCHANTING, InventoryType.ANVIL, InventoryType.SMITHING, InventoryType.MERCHANT));
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onInventoryClick(InventoryClickEvent event) {
      if (this.stateManager.isEnabled()) {
         if (event.getWhoClicked() instanceof Player) {
            Player player = (Player)event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();
            boolean isLocked;
            if (event.getClickedInventory() != null && event.getClickedInventory().getType() != InventoryType.PLAYER && cursorItem != null) {
               isLocked = this.lockManager.isItemLocked(cursorItem);
               if (!isLocked && this.plugin.getConfig().getBoolean("settings.lock-by-item-type", true)) {
                  isLocked = this.lockManager.isItemTypeLocked(player, cursorItem);
               }

               if (isLocked) {
                  event.setCancelled(true);
                  if (this.specialToolInventories.contains(event.getClickedInventory().getType())) {
                     this.messageUtils.sendMessage(player, "cannot-use-tool");
                  } else if (this.restrictedInventories.contains(event.getClickedInventory().getType())) {
                     this.messageUtils.sendMessage(player, "cannot-store");
                  }

                  if (this.plugin.getConfig().getBoolean("settings.show-unlock-reminder", true)) {
                     this.messageUtils.sendMessage(player, "unlock-reminder");
                  }

                  return;
               }
            }

            if (event.getClickedInventory() != null && (event.isShiftClick() || event.getAction().name().contains("HOTBAR")) && event.getClickedInventory().getType() == InventoryType.PLAYER && event.isShiftClick() && clickedItem != null && event.getView().getTopInventory().getType() != InventoryType.CRAFTING) {
               isLocked = this.lockManager.isItemLocked(clickedItem);
               if (!isLocked && this.plugin.getConfig().getBoolean("settings.lock-by-item-type", true)) {
                  isLocked = this.lockManager.isItemTypeLocked(player, clickedItem);
               }

               if (isLocked) {
                  event.setCancelled(true);
                  if (this.specialToolInventories.contains(event.getView().getTopInventory().getType())) {
                     this.messageUtils.sendMessage(player, "cannot-use-tool");
                  } else if (this.restrictedInventories.contains(event.getView().getTopInventory().getType())) {
                     this.messageUtils.sendMessage(player, "cannot-store");
                  }

                  if (this.plugin.getConfig().getBoolean("settings.show-unlock-reminder", true)) {
                     this.messageUtils.sendMessage(player, "unlock-reminder");
                  }
               }
            }

         }
      }
   }
}
