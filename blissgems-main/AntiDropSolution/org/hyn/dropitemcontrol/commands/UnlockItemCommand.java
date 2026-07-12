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

public class UnlockItemCommand implements CommandExecutor {
   private final Dropitemcontrol plugin;
   private final ItemLockManager lockManager;
   private final MessageUtils messageUtils;

   public UnlockItemCommand(Dropitemcontrol plugin, ItemLockManager lockManager, MessageUtils messageUtils) {
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
         if (!player.hasPermission("dropitemcontrol.unlock")) {
            this.messageUtils.sendMessage(player, "no-permission");
            return true;
         } else {
            boolean unlockType = false;
            boolean unlockAll = false;
            if (args.length > 0) {
               if (args[0].equalsIgnoreCase("type")) {
                  unlockType = true;
               } else if (args[0].equalsIgnoreCase("all")) {
                  unlockAll = true;
               }
            }

            if (unlockAll) {
               int unlockedCount = this.unlockAllItems(player);
               if (unlockedCount > 0) {
                  this.messageUtils.sendMessage(player, "all-items-unlocked", String.valueOf(unlockedCount));
               } else {
                  this.messageUtils.sendMessage(player, "no-items-to-unlock");
               }

               return true;
            } else {
               ItemStack item = player.getInventory().getItemInMainHand();
               if (item != null && item.getType() != Material.AIR) {
                  if (!this.lockManager.isItemLocked(item)) {
                     this.messageUtils.sendMessage(player, "item-not-locked");
                     return true;
                  } else {
                     String itemName = item.getType().toString().toLowerCase().replace("_", " ");
                     if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                        itemName = item.getItemMeta().getDisplayName();
                     }

                     if (unlockType && this.plugin.getConfig().getBoolean("settings.lock-by-item-type", true)) {
                        Material itemType = item.getType();
                        int unlockedItems = this.lockManager.unlockAllItemsOfType(player, itemType);
                        if (unlockedItems > 0) {
                           this.messageUtils.sendMessage(player, "item-type-unlocked", itemName);
                        } else {
                           this.messageUtils.sendMessage(player, "item-not-locked");
                        }
                     } else if (this.lockManager.unlockItem(player, item)) {
                        this.messageUtils.sendMessage(player, "item-unlocked", itemName);
                     } else {
                        this.messageUtils.sendMessage(player, "item-unlock-failed");
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

   private int unlockAllItems(Player player) {
      int count = 0;
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      if (mainHand != null && mainHand.getType() != Material.AIR && this.lockManager.isItemLocked(mainHand) && this.lockManager.unlockItem(player, mainHand)) {
         ++count;
      }

      ItemStack[] var4 = player.getInventory().getContents();
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         ItemStack item = var4[var6];
         if (item != null && item.getType() != Material.AIR && this.lockManager.isItemLocked(item) && this.lockManager.unlockItem(player, item)) {
            ++count;
         }
      }

      return count;
   }
}
