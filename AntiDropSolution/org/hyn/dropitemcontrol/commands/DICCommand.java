package org.hyn.dropitemcontrol.commands;

import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hyn.dropitemcontrol.Dropitemcontrol;
import org.hyn.dropitemcontrol.managers.ItemLockManager;
import org.hyn.dropitemcontrol.managers.PluginStateManager;
import org.hyn.dropitemcontrol.utils.MessageUtils;

public class DICCommand implements CommandExecutor {
   private final Dropitemcontrol plugin;
   private final ItemLockManager lockManager;
   private final MessageUtils messageUtils;
   private final PluginStateManager stateManager;
   private final LockItemCommand lockItemCommand;
   private final UnlockItemCommand unlockItemCommand;
   private final LockedItemsCommand lockedItemsCommand;
   private final ToggleDICCommand toggleDICCommand;
   private final LanguageCommand languageCommand;

   public DICCommand(Dropitemcontrol plugin, ItemLockManager lockManager, MessageUtils messageUtils, PluginStateManager stateManager, LockItemCommand lockItemCommand, UnlockItemCommand unlockItemCommand, LockedItemsCommand lockedItemsCommand, ToggleDICCommand toggleDICCommand) {
      this.plugin = plugin;
      this.lockManager = lockManager;
      this.messageUtils = messageUtils;
      this.stateManager = stateManager;
      this.lockItemCommand = lockItemCommand;
      this.unlockItemCommand = unlockItemCommand;
      this.lockedItemsCommand = lockedItemsCommand;
      this.toggleDICCommand = toggleDICCommand;
      this.languageCommand = new LanguageCommand(plugin, plugin.getLanguageManager(), messageUtils);
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length == 0) {
         this.showHelp(sender);
         return true;
      } else {
         String subCommand = args[0].toLowerCase();
         String[] subArgs = new String[args.length - 1];
         if (args.length > 1) {
            System.arraycopy(args, 1, subArgs, 0, args.length - 1);
         }

         byte var8 = -1;
         switch(subCommand.hashCode()) {
         case -1613589672:
            if (subCommand.equals("language")) {
               var8 = 7;
            }
            break;
         case -868304044:
            if (subCommand.equals("toggle")) {
               var8 = 4;
            }
            break;
         case -840442044:
            if (subCommand.equals("unlock")) {
               var8 = 1;
            }
            break;
         case 3551:
            if (subCommand.equals("on")) {
               var8 = 5;
            }
            break;
         case 109935:
            if (subCommand.equals("off")) {
               var8 = 6;
            }
            break;
         case 3198785:
            if (subCommand.equals("help")) {
               var8 = 9;
            }
            break;
         case 3314158:
            if (subCommand.equals("lang")) {
               var8 = 8;
            }
            break;
         case 3322014:
            if (subCommand.equals("list")) {
               var8 = 2;
            }
            break;
         case 3327275:
            if (subCommand.equals("lock")) {
               var8 = 0;
            }
            break;
         case 100526016:
            if (subCommand.equals("items")) {
               var8 = 3;
            }
         }

         switch(var8) {
         case 0:
            if (sender.hasPermission("dropitemcontrol.lock")) {
               return this.lockItemCommand.onCommand(sender, command, label, subArgs);
            }

            if (sender instanceof Player) {
               this.messageUtils.sendMessage((Player)sender, "no-permission");
            } else {
               sender.sendMessage(String.valueOf(ChatColor.RED) + "Bạn không có quyền sử dụng lệnh này.");
            }
            break;
         case 1:
            if (sender.hasPermission("dropitemcontrol.unlock")) {
               return this.unlockItemCommand.onCommand(sender, command, label, subArgs);
            }

            if (sender instanceof Player) {
               this.messageUtils.sendMessage((Player)sender, "no-permission");
            } else {
               sender.sendMessage(String.valueOf(ChatColor.RED) + "Bạn không có quyền sử dụng lệnh này.");
            }
            break;
         case 2:
         case 3:
            if (sender.hasPermission("dropitemcontrol.list")) {
               return this.lockedItemsCommand.onCommand(sender, command, label, subArgs);
            }

            if (sender instanceof Player) {
               this.messageUtils.sendMessage((Player)sender, "no-permission");
            } else {
               sender.sendMessage(String.valueOf(ChatColor.RED) + "Bạn không có quyền sử dụng lệnh này.");
            }
            break;
         case 4:
         case 5:
         case 6:
            if (sender.hasPermission("dropitemcontrol.admin")) {
               String var10001;
               if (subCommand.equals("on") && !this.stateManager.isEnabled()) {
                  this.stateManager.enablePlugin();
                  if (sender instanceof Player) {
                     var10001 = this.messageUtils.getPrefix();
                     sender.sendMessage(var10001 + String.valueOf(ChatColor.GREEN) + "DropItemControl đã được BẬT!");
                  } else {
                     sender.sendMessage(String.valueOf(ChatColor.GREEN) + "DropItemControl đã được BẬT!");
                  }

                  return true;
               }

               int itemsUnlocked;
               if (subCommand.equals("off") && this.stateManager.isEnabled()) {
                  itemsUnlocked = this.removeAllLockLores();
                  this.stateManager.disablePlugin();
                  if (sender instanceof Player) {
                     var10001 = this.messageUtils.getPrefix();
                     sender.sendMessage(var10001 + String.valueOf(ChatColor.RED) + "DropItemControl đã được TẮT!");
                     if (itemsUnlocked > 0) {
                        var10001 = this.messageUtils.getPrefix();
                        sender.sendMessage(var10001 + String.valueOf(ChatColor.YELLOW) + "Đã xóa lore khóa của " + String.valueOf(ChatColor.WHITE) + itemsUnlocked + String.valueOf(ChatColor.YELLOW) + " item.");
                     }
                  } else {
                     sender.sendMessage(String.valueOf(ChatColor.RED) + "DropItemControl đã được TẮT!");
                     if (itemsUnlocked > 0) {
                        var10001 = String.valueOf(ChatColor.YELLOW);
                        sender.sendMessage(var10001 + "Đã xóa lore khóa của " + String.valueOf(ChatColor.WHITE) + itemsUnlocked + String.valueOf(ChatColor.YELLOW) + " item.");
                     }
                  }

                  return true;
               }

               if (subCommand.equals("toggle")) {
                  if (!this.stateManager.isEnabled()) {
                     return this.toggleDICCommand.onCommand(sender, command, label, subArgs);
                  }

                  itemsUnlocked = this.removeAllLockLores();
                  boolean newState = this.toggleDICCommand.onCommand(sender, command, label, subArgs);
                  if (sender instanceof Player && itemsUnlocked > 0) {
                     var10001 = this.messageUtils.getPrefix();
                     sender.sendMessage(var10001 + String.valueOf(ChatColor.YELLOW) + "Đã xóa lore khóa của " + String.valueOf(ChatColor.WHITE) + itemsUnlocked + String.valueOf(ChatColor.YELLOW) + " item.");
                  } else if (itemsUnlocked > 0) {
                     var10001 = String.valueOf(ChatColor.YELLOW);
                     sender.sendMessage(var10001 + "Đã xóa lore khóa của " + String.valueOf(ChatColor.WHITE) + itemsUnlocked + String.valueOf(ChatColor.YELLOW) + " item.");
                  }

                  return newState;
               }

               if (sender instanceof Player) {
                  var10001 = this.messageUtils.getPrefix();
                  sender.sendMessage(var10001 + String.valueOf(ChatColor.YELLOW) + "DropItemControl đã " + (this.stateManager.isEnabled() ? "được BẬT" : "bị TẮT") + " rồi!");
               } else {
                  var10001 = String.valueOf(ChatColor.YELLOW);
                  sender.sendMessage(var10001 + "DropItemControl đã " + (this.stateManager.isEnabled() ? "được BẬT" : "bị TẮT") + " rồi!");
               }

               return true;
            }

            if (sender instanceof Player) {
               this.messageUtils.sendMessage((Player)sender, "no-permission");
            } else {
               sender.sendMessage(String.valueOf(ChatColor.RED) + "Bạn không có quyền sử dụng lệnh này.");
            }
            break;
         case 7:
         case 8:
            if (sender.hasPermission("dropitemcontrol.language")) {
               return this.languageCommand.onCommand(sender, command, label, subArgs);
            }

            if (sender instanceof Player) {
               this.messageUtils.sendMessage((Player)sender, "no-permission");
            } else {
               sender.sendMessage(String.valueOf(ChatColor.RED) + "Bạn không có quyền sử dụng lệnh này.");
            }
            break;
         case 9:
            this.showHelp(sender);
            break;
         default:
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Lệnh không hợp lệ. Sử dụng /dic help để xem danh sách lệnh.");
         }

         return true;
      }
   }

   private int removeAllLockLores() {
      int count = 0;
      String lockLore = ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("settings.lock-lore", "&c&lĐã khóa - Không thể vứt"));
      Iterator var3 = Bukkit.getOnlinePlayers().iterator();

      while(var3.hasNext()) {
         Player player = (Player)var3.next();
         ItemStack[] var5 = player.getInventory().getContents();
         int var6 = var5.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            ItemStack item = var5[var7];
            if (item != null && item.hasItemMeta()) {
               ItemMeta meta = item.getItemMeta();
               if (meta != null && meta.hasLore()) {
                  List<String> lore = meta.getLore();
                  if (lore != null && lore.contains(lockLore)) {
                     lore.remove(lockLore);
                     meta.setLore(lore.isEmpty() ? null : lore);
                     item.setItemMeta(meta);
                     ++count;
                  }
               }
            }
         }
      }

      return count;
   }

   private void showHelp(CommandSender sender) {
      String var10001 = String.valueOf(ChatColor.GOLD);
      sender.sendMessage(var10001 + "===== " + String.valueOf(ChatColor.YELLOW) + "DropItemControl Commands" + String.valueOf(ChatColor.GOLD) + " =====");
      if (sender.hasPermission("dropitemcontrol.lock")) {
         var10001 = String.valueOf(ChatColor.YELLOW);
         sender.sendMessage(var10001 + "/dic lock [type|all]" + String.valueOf(ChatColor.WHITE) + " - Khóa item đang cầm trên tay");
         var10001 = String.valueOf(ChatColor.YELLOW);
         sender.sendMessage(var10001 + "/dic lock type" + String.valueOf(ChatColor.WHITE) + " - Khóa tất cả item cùng loại");
         var10001 = String.valueOf(ChatColor.YELLOW);
         sender.sendMessage(var10001 + "/dic lock all" + String.valueOf(ChatColor.WHITE) + " - Khóa tất cả item trong túi đồ");
      }

      if (sender.hasPermission("dropitemcontrol.unlock")) {
         var10001 = String.valueOf(ChatColor.YELLOW);
         sender.sendMessage(var10001 + "/dic unlock [type|all]" + String.valueOf(ChatColor.WHITE) + " - Mở khóa item đang cầm trên tay");
         var10001 = String.valueOf(ChatColor.YELLOW);
         sender.sendMessage(var10001 + "/dic unlock type" + String.valueOf(ChatColor.WHITE) + " - Mở khóa tất cả item cùng loại");
         var10001 = String.valueOf(ChatColor.YELLOW);
         sender.sendMessage(var10001 + "/dic unlock all" + String.valueOf(ChatColor.WHITE) + " - Mở khóa tất cả item trong túi đồ");
      }

      if (sender.hasPermission("dropitemcontrol.list")) {
         var10001 = String.valueOf(ChatColor.YELLOW);
         sender.sendMessage(var10001 + "/dic list" + String.valueOf(ChatColor.WHITE) + " - Xem danh sách các item đã khóa");
      }

      if (sender.hasPermission("dropitemcontrol.language")) {
         var10001 = String.valueOf(ChatColor.YELLOW);
         sender.sendMessage(var10001 + "/dic language [lang]" + String.valueOf(ChatColor.WHITE) + " - Đổi ngôn ngữ (vi/en)");
      }

      if (sender.hasPermission("dropitemcontrol.admin")) {
         var10001 = String.valueOf(ChatColor.YELLOW);
         sender.sendMessage(var10001 + "/dic toggle" + String.valueOf(ChatColor.WHITE) + " - Bật/tắt plugin");
         var10001 = String.valueOf(ChatColor.YELLOW);
         sender.sendMessage(var10001 + "/dic on" + String.valueOf(ChatColor.WHITE) + " - Bật plugin");
         var10001 = String.valueOf(ChatColor.YELLOW);
         sender.sendMessage(var10001 + "/dic off" + String.valueOf(ChatColor.WHITE) + " - Tắt plugin");
      }

      var10001 = String.valueOf(ChatColor.YELLOW);
      sender.sendMessage(var10001 + "/dic help" + String.valueOf(ChatColor.WHITE) + " - Hiển thị trợ giúp này");
      sender.sendMessage(String.valueOf(ChatColor.GOLD) + "====================================");
   }
}
