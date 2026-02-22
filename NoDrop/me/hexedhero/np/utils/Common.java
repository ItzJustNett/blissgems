package me.hexedhero.np.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Common {
   public static void tell(CommandSender sender, String message) {
      sender.sendMessage(colorize(message));
   }

   public static String colorize(String message) {
      return ChatColor.translateAlternateColorCodes('&', message);
   }
}
