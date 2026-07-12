package org.hyn.dropitemcontrol.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.hyn.dropitemcontrol.Dropitemcontrol;
import org.hyn.dropitemcontrol.managers.ItemLockManager;
import org.hyn.dropitemcontrol.managers.PluginStateManager;
import org.hyn.dropitemcontrol.utils.MessageUtils;

public class ItemDropListener implements Listener {
   private final Dropitemcontrol plugin;
   private final ItemLockManager lockManager;
   private final MessageUtils messageUtils;
   private final PluginStateManager stateManager;

   public ItemDropListener(Dropitemcontrol plugin, ItemLockManager lockManager, MessageUtils messageUtils, PluginStateManager stateManager) {
      this.plugin = plugin;
      this.lockManager = lockManager;
      this.messageUtils = messageUtils;
      this.stateManager = stateManager;
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onItemDrop(PlayerDropItemEvent event) {
      if (this.stateManager.isEnabled()) {
         Player player = event.getPlayer();
         ItemStack droppedItem = event.getItemDrop().getItemStack();
         boolean isLocked = this.lockManager.isItemLocked(droppedItem);
         if (!isLocked && this.plugin.getConfig().getBoolean("settings.lock-by-item-type", true)) {
            isLocked = this.lockManager.isItemTypeLocked(player, droppedItem);
         }

         if (isLocked) {
            event.setCancelled(true);
            this.messageUtils.sendMessage(player, "cannot-drop");
            if (this.plugin.getConfig().getBoolean("settings.show-unlock-reminder", true)) {
               this.messageUtils.sendMessage(player, "unlock-reminder");
            }
         }

      }
   }
}
