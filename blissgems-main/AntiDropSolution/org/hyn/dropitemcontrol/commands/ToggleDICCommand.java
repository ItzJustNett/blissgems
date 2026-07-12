package org.hyn.dropitemcontrol.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.hyn.dropitemcontrol.Dropitemcontrol;
import org.hyn.dropitemcontrol.managers.PluginStateManager;
import org.hyn.dropitemcontrol.utils.MessageUtils;

public class ToggleDICCommand implements CommandExecutor {
   private final Dropitemcontrol plugin;
   private final PluginStateManager stateManager;
   private final MessageUtils messageUtils;

   public ToggleDICCommand(Dropitemcontrol plugin, PluginStateManager stateManager, MessageUtils messageUtils) {
      this.plugin = plugin;
      this.stateManager = stateManager;
      this.messageUtils = messageUtils;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("dropitemcontrol.admin")) {
         if (sender instanceof Player) {
            this.messageUtils.sendMessage((Player)sender, "no-permission");
         } else {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Bạn không có quyền sử dụng lệnh này.");
         }

         return true;
      } else {
         boolean newState = !this.stateManager.isEnabled();
         String var10001;
         if (newState) {
            this.stateManager.enablePlugin();
            if (sender instanceof Player) {
               var10001 = this.messageUtils.getPrefix();
               sender.sendMessage(var10001 + String.valueOf(ChatColor.GREEN) + "DropItemControl đã được BẬT!");
            } else {
               sender.sendMessage(String.valueOf(ChatColor.GREEN) + "DropItemControl đã được BẬT!");
            }
         } else {
            this.stateManager.disablePlugin();
            if (sender instanceof Player) {
               var10001 = this.messageUtils.getPrefix();
               sender.sendMessage(var10001 + String.valueOf(ChatColor.RED) + "DropItemControl đã được TẮT!");
            } else {
               sender.sendMessage(String.valueOf(ChatColor.RED) + "DropItemControl đã được TẮT!");
            }
         }

         return true;
      }
   }
}
