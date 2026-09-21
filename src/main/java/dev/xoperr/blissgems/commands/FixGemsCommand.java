/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 */
package dev.xoperr.blissgems.commands;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.OraxenGemFixer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class FixGemsCommand
implements CommandExecutor,
TabCompleter {
    private final BlissGems plugin;

    public FixGemsCommand(BlissGems plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (!sender.hasPermission("blissgems.admin") && !sender.hasPermission("blissgems.fixgems")) {
            sender.sendMessage("\u00a7cYou don't have permission to use this command.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
            int players = 0;
            int fixed = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                int f = OraxenGemFixer.fixInventory(this.plugin, online);
                if (f <= 0) continue;
                ++players;
                fixed += f;
            }
            sender.sendMessage("\u00a7aReplaced \u00a7e" + fixed + "\u00a7a legacy gem item(s) across \u00a7e" + players + "\u00a7a player(s).");
            return true;
        }
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("\u00a7cUsage: /fixgems <player|all>");
                return true;
            }
            target = (Player)sender;
        } else {
            target = Bukkit.getPlayer((String)args[0]);
            if (target == null) {
                sender.sendMessage("\u00a7cPlayer \u00a7e" + args[0] + "\u00a7c not found or offline.");
                return true;
            }
        }
        int fixed = OraxenGemFixer.fixInventory(this.plugin, target);
        if (fixed > 0) {
            sender.sendMessage("\u00a7aReplaced \u00a7e" + fixed + "\u00a7a legacy gem item(s) for \u00a7e" + target.getName() + "\u00a7a.");
        } else {
            sender.sendMessage("\u00a77No legacy gem items found for \u00a7e" + target.getName() + "\u00a77.");
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            ArrayList<String> options = new ArrayList<String>();
            options.add("all");
            options.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            return options.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return new ArrayList<String>();
    }
}

