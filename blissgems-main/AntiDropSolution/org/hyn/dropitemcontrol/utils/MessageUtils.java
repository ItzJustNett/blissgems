package org.hyn.dropitemcontrol.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.hyn.dropitemcontrol.Dropitemcontrol;
import org.hyn.dropitemcontrol.managers.LanguageManager;

public class MessageUtils {
   private final Dropitemcontrol plugin;
   private final FileConfiguration config;
   private final LanguageManager languageManager;

   public MessageUtils(Dropitemcontrol plugin, LanguageManager languageManager) {
      this.plugin = plugin;
      this.config = plugin.getConfig();
      this.languageManager = languageManager;
   }

   public String getPrefix() {
      return ChatColor.translateAlternateColorCodes('&', this.config.getString("messages.prefix", "&8[&bDropItemControl&8] &7"));
   }

   public String getMessage(Player player, String path, Object... replacements) {
      FileConfiguration langConfig = player != null ? this.languageManager.getLanguageConfig(player) : this.languageManager.getDefaultLanguageConfig();
      String message = langConfig.getString("messages." + path);
      if (message == null) {
         message = this.config.getString("messages." + path, "Message not found: " + path);
      }

      message = ChatColor.translateAlternateColorCodes('&', message);
      if (replacements != null && replacements.length > 0) {
         if (replacements.length == 1) {
            String value = String.valueOf(replacements[0]);
            message = message.replace("%item%", value);
            message = message.replace("%limit%", value);
            message = message.replace("(%limit%)", "(" + value + ")");
            message = message.replace("%language%", value);
         } else {
            for(int i = 0; i < replacements.length; i += 2) {
               if (i + 1 < replacements.length) {
                  String placeholder = String.valueOf(replacements[i]);
                  String replacement = String.valueOf(replacements[i + 1]);
                  message = message.replace(placeholder, replacement);
               }
            }
         }
      }

      String var10000 = this.getPrefix();
      return var10000 + message;
   }

   public String getSetting(Player player, String path, String defaultValue) {
      FileConfiguration langConfig = player != null ? this.languageManager.getLanguageConfig(player) : this.languageManager.getDefaultLanguageConfig();
      String value = langConfig.getString("settings." + path);
      if (value == null) {
         value = this.config.getString("settings." + path, defaultValue);
      }

      return ChatColor.translateAlternateColorCodes('&', value);
   }

   public String formatMessage(String path, Object... replacements) {
      return this.getMessage((Player)null, path, replacements);
   }

   public void sendMessage(Player player, String path, Object... replacements) {
      player.sendMessage(this.getMessage(player, path, replacements));
   }
}
