package dev.xoperr.blissgems.commands;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.OraxenGemFixer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /fixgems [player|all] - replaces legacy BlissGems items with their
 * Oraxen-built equivalents in the target's inventory.
 */
public class FixGemsCommand implements CommandExecutor, TabCompleter {
    private final BlissGems plugin;

    public FixGemsCommand(BlissGems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("blissgems.admin") && !sender.hasPermission("blissgems.fixgems")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
            int players = 0;
            int fixed = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                int f = OraxenGemFixer.fixInventory(this.plugin, online);
                if (f > 0) {
                    players++;
                    fixed += f;
                }
            }
            sender.sendMessage("§aReplaced §e" + fixed + "§a legacy gem item(s) across §e" + players + "§a player(s).");
            return true;
        }

        Player target;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cUsage: /fixgems <player|all>");
                return true;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer §e" + args[0] + "§c not found or offline.");
                return true;
            }
        }

        int fixed = OraxenGemFixer.fixInventory(this.plugin, target);
        if (fixed > 0) {
            sender.sendMessage("§aReplaced §e" + fixed + "§a legacy gem item(s) for §e" + target.getName() + "§a.");
        } else {
            sender.sendMessage("§7No legacy gem items found for §e" + target.getName() + "§7.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("all");
            options.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            return options.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
