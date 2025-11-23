/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.th0rgal.oraxen.api.OraxenItems
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package fun.obriy.blissgems.commands;

import fun.obriy.blissgems.BlissGems;
import fun.obriy.blissgems.utils.EnergyState;
import fun.obriy.blissgems.utils.GemType;
import io.th0rgal.oraxen.api.OraxenItems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BlissCommand
implements CommandExecutor,
TabCompleter {
    private final BlissGems plugin;

    public BlissCommand(BlissGems plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subcommand;
        if (args.length == 0) {
            this.sendHelp(sender);
            return true;
        }
        switch (subcommand = args[0].toLowerCase()) {
            case "give": {
                this.handleGive(sender, args);
                break;
            }
            case "energy": {
                this.handleEnergy(sender, args);
                break;
            }
            case "withdraw": {
                this.handleWithdraw(sender, args);
                break;
            }
            case "info": {
                this.handleInfo(sender, args);
                break;
            }
            case "reload": {
                this.handleReload(sender, args);
                break;
            }
            default: {
                this.sendHelp(sender);
            }
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blissgems.admin")) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission", new Object[0]));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("\u00a7cUsage: /bliss give <player> <gem_type> [tier]");
            return;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found", new Object[0]));
            return;
        }
        GemType gemType = null;
        for (GemType type : GemType.values()) {
            if (!type.getId().equalsIgnoreCase(args[2]) && !type.getDisplayName().equalsIgnoreCase(args[2])) continue;
            gemType = type;
            break;
        }
        if (gemType == null) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("invalid-gem-type", new Object[0]));
            return;
        }
        int tier = 1;
        if (args.length >= 4) {
            try {
                tier = Integer.parseInt(args[3]);
                if (tier < 1 || tier > 2) {
                    sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("invalid-tier", new Object[0]));
                    return;
                }
            }
            catch (NumberFormatException e) {
                sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("invalid-tier", new Object[0]));
                return;
            }
        }
        if (this.plugin.getGemManager().giveGem(target, gemType, tier)) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("gem-given", "player", target.getName(), "gem", gemType.getDisplayName(), "tier", tier));
        } else {
            sender.sendMessage("\u00a7cFailed to give gem!");
        }
    }

    private void handleEnergy(CommandSender sender, String[] args) {
        int amount;
        if (!sender.hasPermission("blissgems.admin")) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission", new Object[0]));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage("\u00a7cUsage: /bliss energy <player> <set/add/remove> <amount>");
            return;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found", new Object[0]));
            return;
        }
        String action = args[2].toLowerCase();
        try {
            amount = Integer.parseInt(args[3]);
        }
        catch (NumberFormatException e) {
            sender.sendMessage("\u00a7cInvalid amount!");
            return;
        }
        switch (action) {
            case "set": {
                this.plugin.getEnergyManager().setEnergy(target, amount);
                sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("energy-set", "player", target.getName(), "amount", amount));
                break;
            }
            case "add": {
                this.plugin.getEnergyManager().addEnergy(target, amount);
                sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("energy-added", "player", target.getName(), "amount", amount));
                break;
            }
            case "remove": {
                this.plugin.getEnergyManager().removeEnergy(target, amount);
                sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("energy-removed", "player", target.getName(), "amount", amount));
                break;
            }
            default: {
                sender.sendMessage("\u00a7cUsage: /bliss energy <player> <set/add/remove> <amount>");
            }
        }
    }

    private void handleWithdraw(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }
        Player player = (Player)sender;
        int currentEnergy = this.plugin.getEnergyManager().getEnergy(player);
        if (currentEnergy <= 1) {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("not-enough-energy", new Object[0]));
            return;
        }
        this.plugin.getEnergyManager().removeEnergy(player, 1);
        ItemStack bottle = OraxenItems.getItemById((String)"energy_bottle").build();
        if (bottle != null) {
            player.getInventory().addItem(new ItemStack[]{bottle});
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("energy-withdrawn", new Object[0]));
        } else {
            player.sendMessage("\u00a7cFailed to create energy bottle!");
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }
        Player player = (Player)sender;
        if (!this.plugin.getGemManager().hasActiveGem(player)) {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-active-gem", new Object[0]));
            return;
        }
        GemType gemType = this.plugin.getGemManager().getGemType(player);
        int tier = this.plugin.getGemManager().getGemTier(player);
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        EnergyState state = this.plugin.getEnergyManager().getEnergyState(player);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("gem-info", "gem", gemType.getDisplayName(), "tier", tier, "energy", energy, "state", state.getDisplayName()));
    }

    private void handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blissgems.admin")) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission", new Object[0]));
            return;
        }
        this.plugin.getConfigManager().reload();
        sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("config-reloaded", new Object[0]));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("\u00a75\u00a7lBlissGems Commands:");
        sender.sendMessage("\u00a77/bliss give <player> <gem_type> [tier] \u00a78- Give a gem");
        sender.sendMessage("\u00a77/bliss energy <player> <set/add/remove> <amount> \u00a78- Manage energy");
        sender.sendMessage("\u00a77/bliss withdraw \u00a78- Extract energy into bottle");
        sender.sendMessage("\u00a77/bliss info \u00a78- Show your gem info");
        sender.sendMessage("\u00a77/bliss reload \u00a78- Reload config");
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> completions = new ArrayList<String>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("give", "energy", "withdraw", "info", "reload"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("energy")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                return Arrays.stream(GemType.values()).map(GemType::getId).collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("energy")) {
                return Arrays.asList("set", "add", "remove");
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            return Arrays.asList("1", "2");
        }
        return completions.stream().filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
    }
}

