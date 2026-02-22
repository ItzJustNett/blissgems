package org.hyn.dropitemcontrol.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.hyn.dropitemcontrol.Dropitemcontrol;
import org.hyn.dropitemcontrol.managers.ItemLockManager;
import org.hyn.dropitemcontrol.utils.MessageUtils;

public class LockedItemsCommand implements CommandExecutor {
   private final Dropitemcontrol plugin;
   private final ItemLockManager lockManager;
   private final MessageUtils messageUtils;

   public LockedItemsCommand(Dropitemcontrol plugin, ItemLockManager lockManager, MessageUtils messageUtils) {
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
         if (!player.hasPermission("dropitemcontrol.list")) {
            this.messageUtils.sendMessage(player, "no-permission");
            return true;
         } else {
            int lockedCount = this.lockManager.getLockedItemCount(player);
            int maxLocked = this.lockManager.getPlayerItemLimit(player);
            int defaultLimit = this.lockManager.getDefaultMaxLockedItems();
            String var10001 = String.valueOf(ChatColor.GOLD);
            player.sendMessage(var10001 + "===== " + String.valueOf(ChatColor.YELLOW) + "Item đã khóa" + String.valueOf(ChatColor.GOLD) + " =====");
            var10001 = String.valueOf(ChatColor.YELLOW);
            player.sendMessage(var10001 + "Số lượng item đã khóa: " + String.valueOf(ChatColor.WHITE) + lockedCount + "/" + maxLocked);
            if (maxLocked != defaultLimit) {
               var10001 = String.valueOf(ChatColor.YELLOW);
               player.sendMessage(var10001 + "Giới hạn của bạn: " + String.valueOf(ChatColor.WHITE) + maxLocked + String.valueOf(ChatColor.YELLOW) + " (Mặc định: " + String.valueOf(ChatColor.WHITE) + defaultLimit + String.valueOf(ChatColor.YELLOW) + ")");
            }

            var10001 = String.valueOf(ChatColor.YELLOW);
            player.sendMessage(var10001 + "Sử dụng " + String.valueOf(ChatColor.WHITE) + "/dic lock" + String.valueOf(ChatColor.YELLOW) + " để khóa item trên tay.");
            var10001 = String.valueOf(ChatColor.YELLOW);
            player.sendMessage(var10001 + "Sử dụng " + String.valueOf(ChatColor.WHITE) + "/dic unlock" + String.valueOf(ChatColor.YELLOW) + " để mở khóa item trên tay.");
            player.sendMessage(String.valueOf(ChatColor.GOLD) + "====================================");
            return true;
         }
      }
   }
}
