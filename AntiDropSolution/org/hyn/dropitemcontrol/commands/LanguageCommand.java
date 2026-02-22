package org.hyn.dropitemcontrol.commands;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.hyn.dropitemcontrol.Dropitemcontrol;
import org.hyn.dropitemcontrol.managers.LanguageManager;
import org.hyn.dropitemcontrol.utils.MessageUtils;

public class LanguageCommand implements CommandExecutor {
   private final Dropitemcontrol plugin;
   private final LanguageManager languageManager;
   private final MessageUtils messageUtils;

   public LanguageCommand(Dropitemcontrol plugin, LanguageManager languageManager, MessageUtils messageUtils) {
      this.plugin = plugin;
      this.languageManager = languageManager;
      this.messageUtils = messageUtils;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage("§cThis command can only be used by players!");
         return true;
      } else {
         Player player = (Player)sender;
         if (!player.hasPermission("dropitemcontrol.language")) {
            this.messageUtils.sendMessage(player, "no-permission");
            return true;
         } else if (args.length == 0) {
            Map<String, String> languages = this.languageManager.getAvailableLanguages();
            String currentLang = this.languageManager.getPlayerLanguage(player);
            player.sendMessage("§8§m-----------------------------------------------------");
            player.sendMessage("§b§l DropItemControl - Available Languages");
            player.sendMessage("§8§m-----------------------------------------------------");
            Iterator var14 = languages.entrySet().iterator();

            while(var14.hasNext()) {
               Entry<String, String> entry = (Entry)var14.next();
               String langCode = (String)entry.getKey();
               String langName = (String)entry.getValue();
               if (langCode.equals(currentLang)) {
                  player.sendMessage("§a✓ §f" + langName + " §7(" + langCode + ") §b[Current]");
               } else {
                  player.sendMessage("§7• §f" + langName + " §7(" + langCode + ") §e[/dic language " + langCode + "]");
               }
            }

            player.sendMessage("§8§m-----------------------------------------------------");
            return true;
         } else {
            String langCode = args[0].toLowerCase();
            if (this.languageManager.setPlayerLanguage(player, langCode)) {
               Map<String, String> languages = this.languageManager.getAvailableLanguages();
               String langName = (String)languages.getOrDefault(langCode, langCode);
               this.messageUtils.sendMessage(player, "language-changed", langName);
            } else {
               player.sendMessage("§cInvalid language! Use /dic language to see available languages.");
            }

            return true;
         }
      }
   }
}
