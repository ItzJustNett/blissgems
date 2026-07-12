package org.hyn.dropitemcontrol.tabcompleters;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class ToggleDICTabCompleter implements TabCompleter {
   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      return new ArrayList();
   }
}
