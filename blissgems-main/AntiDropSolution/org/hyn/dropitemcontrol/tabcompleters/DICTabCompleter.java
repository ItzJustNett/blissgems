package org.hyn.dropitemcontrol.tabcompleters;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.hyn.dropitemcontrol.Dropitemcontrol;

public class DICTabCompleter implements TabCompleter {
   private final Dropitemcontrol plugin;
   private final LockItemTabCompleter lockItemTabCompleter;
   private final UnlockItemTabCompleter unlockItemTabCompleter;
   private final LanguageTabCompleter languageTabCompleter;

   public DICTabCompleter(Dropitemcontrol plugin, LockItemTabCompleter lockItemTabCompleter, UnlockItemTabCompleter unlockItemTabCompleter) {
      this.plugin = plugin;
      this.lockItemTabCompleter = lockItemTabCompleter;
      this.unlockItemTabCompleter = unlockItemTabCompleter;
      this.languageTabCompleter = new LanguageTabCompleter(plugin.getLanguageManager());
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> completions = new ArrayList();
      if (args.length == 1) {
         List<String> subCommands = new ArrayList();
         if (sender.hasPermission("dropitemcontrol.lock")) {
            subCommands.add("lock");
         }

         if (sender.hasPermission("dropitemcontrol.unlock")) {
            subCommands.add("unlock");
         }

         if (sender.hasPermission("dropitemcontrol.list")) {
            subCommands.add("list");
            subCommands.add("items");
         }

         if (sender.hasPermission("dropitemcontrol.language")) {
            subCommands.add("language");
            subCommands.add("lang");
         }

         if (sender.hasPermission("dropitemcontrol.admin")) {
            subCommands.add("toggle");
            subCommands.add("on");
            subCommands.add("off");
         }

         subCommands.add("help");
         return (List)subCommands.stream().filter((subCommandx) -> {
            return subCommandx.toLowerCase().startsWith(args[0].toLowerCase());
         }).collect(Collectors.toList());
      } else {
         if (args.length > 1) {
            String subCommand = args[0].toLowerCase();
            String[] subArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subArgs, 0, args.length - 1);
            byte var9 = -1;
            switch(subCommand.hashCode()) {
            case -1613589672:
               if (subCommand.equals("language")) {
                  var9 = 2;
               }
               break;
            case -840442044:
               if (subCommand.equals("unlock")) {
                  var9 = 1;
               }
               break;
            case 3314158:
               if (subCommand.equals("lang")) {
                  var9 = 3;
               }
               break;
            case 3327275:
               if (subCommand.equals("lock")) {
                  var9 = 0;
               }
            }

            switch(var9) {
            case 0:
               if (sender.hasPermission("dropitemcontrol.lock")) {
                  return this.lockItemTabCompleter.onTabComplete(sender, command, alias, subArgs);
               }
               break;
            case 1:
               if (sender.hasPermission("dropitemcontrol.unlock")) {
                  return this.unlockItemTabCompleter.onTabComplete(sender, command, alias, subArgs);
               }
               break;
            case 2:
            case 3:
               if (sender.hasPermission("dropitemcontrol.language")) {
                  return this.languageTabCompleter.onTabComplete(sender, command, alias, subArgs);
               }
            }
         }

         return completions;
      }
   }
}
