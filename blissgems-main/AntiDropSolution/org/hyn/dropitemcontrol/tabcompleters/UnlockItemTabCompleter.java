package org.hyn.dropitemcontrol.tabcompleters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.hyn.dropitemcontrol.Dropitemcontrol;

public class UnlockItemTabCompleter implements TabCompleter {
   private final Dropitemcontrol plugin;
   private final List<String> options;

   public UnlockItemTabCompleter(Dropitemcontrol plugin) {
      this.plugin = plugin;
      this.options = Arrays.asList("type", "all");
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (!(sender instanceof Player)) {
         return new ArrayList();
      } else if (!sender.hasPermission("dropitemcontrol.unlock")) {
         return new ArrayList();
      } else if (args.length == 1) {
         String input = args[0].toLowerCase();
         return (List)this.options.stream().filter((option) -> {
            return option.toLowerCase().startsWith(input);
         }).collect(Collectors.toList());
      } else {
         return new ArrayList();
      }
   }
}
