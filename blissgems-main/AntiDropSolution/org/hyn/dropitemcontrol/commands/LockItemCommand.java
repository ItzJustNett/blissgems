package org.hyn.dropitemcontrol.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.hyn.dropitemcontrol.Dropitemcontrol;
import org.hyn.dropitemcontrol.managers.ItemLockManager;
import org.hyn.dropitemcontrol.utils.MessageUtils;

public class LockItemCommand implements CommandExecutor {
   private final Dropitemcontrol plugin;
   private final ItemLockManager lockManager;
   private final MessageUtils messageUtils;

   public LockItemCommand(Dropitemcontrol plugin, ItemLockManager lockManager, MessageUtils messageUtils) {
      this.plugin = plugin;
      this.lockManager = lockManager;
      this.messageUtils = messageUtils;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage(String.valueOf(ChatColor.RED) + "Lệnh này chỉ có thể được sử dụng bởi người chơi.");
         return true;
      } else {
         Player player = (Player)sender;
         if (!player.hasPermission("dropitemcontrol.lock")) {
            this.messageUtils.sendMessage(player, "no-permission");
            return true;
         } else {
            boolean lockType = false;
            boolean lockAll = false;
            if (args.length > 0) {
               if (args[0].equalsIgnoreCase("type")) {
                  lockType = true;
               } else if (args[0].equalsIgnoreCase("all")) {
                  lockAll = true;
               }
            }

            if (lockAll) {
               int lockedCount = this.lockAllItems(player);
               if (lockedCount > 0) {
                  this.messageUtils.sendMessage(player, "all-items-locked", String.valueOf(lockedCount));
               } else {
                  this.messageUtils.sendMessage(player, "no-items-to-lock");
               }

               return true;
            } else {
               ItemStack item = player.getInventory().getItemInMainHand();
               if (item != null && item.getType() != Material.AIR) {
                  int lockedCount = this.lockManager.getLockedItemCount(player);
                  int maxLocked = this.lockManager.getPlayerItemLimit(player);
                  if (lockedCount >= maxLocked) {
                     this.messageUtils.sendMessage(player, "item-lock-limit-reached", String.valueOf(maxLocked));
                     return true;
                  } else if (this.lockManager.isItemLocked(item)) {
                     this.messageUtils.sendMessage(player, "item-already-locked");
                     return true;
                  } else {
                     String itemName = item.getType().toString().toLowerCase().replace("_", " ");
                     if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                        itemName = item.getItemMeta().getDisplayName();
                     }

                     if (lockType && this.plugin.getConfig().getBoolean("settings.lock-by-item-type", true)) {
                        Material itemType = item.getType();
                        int lockedItems = this.lockManager.lockAllItemsOfType(player, itemType);
                        if (lockedItems > 0) {
                           this.messageUtils.sendMessage(player, "item-type-locked", itemName);
                        } else {
                           this.messageUtils.sendMessage(player, "item-already-locked");
                        }
                     } else if (this.lockManager.lockItem(player, item)) {
                        this.messageUtils.sendMessage(player, "item-locked", itemName);
                     } else {
                        this.messageUtils.sendMessage(player, "item-lock-failed");
                     }

                     return true;
                  }
               } else {
                  this.messageUtils.sendMessage(player, "no-item-in-hand");
                  return true;
               }
            }
         }
      }
   }

   private int lockAllItems(Player player) {
      int count = 0;
      int maxLocked = this.lockManager.getPlayerItemLimit(player);
      int lockedCount = this.lockManager.getLockedItemCount(player);
      if (lockedCount >= maxLocked) {
         this.messageUtils.sendMessage(player, "item-lock-limit-reached", String.valueOf(maxLocked));
         return 0;
      } else {
         ItemStack mainHand = player.getInventory().getItemInMainHand();
         if (mainHand != null && mainHand.getType() != Material.AIR && !this.lockManager.isItemLocked(mainHand)) {
            if (this.lockManager.lockItem(player, mainHand)) {
               ++count;
            }

            lockedCount = this.lockManager.getLockedItemCount(player);
            if (lockedCount >= maxLocked) {
               return count;
            }
         }

         ItemStack[] var6 = player.getInventory().getContents();
         int var7 = var6.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            ItemStack item = var6[var8];
            if (item != null && item.getType() != Material.AIR && !this.lockManager.isItemLocked(item)) {
               if (this.lockManager.lockItem(player, item)) {
                  ++count;
               }

               lockedCount = this.lockManager.getLockedItemCount(player);
               if (lockedCount >= maxLocked) {
                  break;
               }
            }
         }

         return count;
      }
   }
}
