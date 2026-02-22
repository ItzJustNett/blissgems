package org.hyn.dropitemcontrol.tabcompleters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.hyn.dropitemcontrol.managers.LanguageManager;

public class LanguageTabCompleter implements TabCompleter {
   private final LanguageManager languageManager;

   public LanguageTabCompleter(LanguageManager languageManager) {
      this.languageManager = languageManager;
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList();
      if (!(sender instanceof Player)) {
         return completions;
      } else {
         if (args.length == 1) {
            Map<String, String> languages = this.languageManager.getAvailableLanguages();
            Iterator var7 = languages.keySet().iterator();

            while(var7.hasNext()) {
               String langCode = (String)var7.next();
               if (langCode.toLowerCase().startsWith(args[0].toLowerCase())) {
                  completions.add(langCode);
               }
            }
         }

         return completions;
      }
   }
}
