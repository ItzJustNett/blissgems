/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.NamespacedKey
 *  org.bukkit.attribute.AttributeInstance
 *  org.bukkit.attribute.AttributeModifier
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 */
package dev.xoperr.blissgems.commands;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Attributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class FixHeartsCommand
implements CommandExecutor,
TabCompleter {
    private final BlissGems plugin;

    public FixHeartsCommand(BlissGems plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (!sender.hasPermission("blissgems.admin") && !sender.hasPermission("blissgems.fixhearts")) {
            sender.sendMessage("\u00a7cYou don't have permission to use this command.");
            return true;
        }
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("\u00a7cUsage: /fixhearts <player>");
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
        AttributeInstance attr = target.getAttribute(Attributes.maxHealth());
        if (attr == null) {
            sender.sendMessage("\u00a7cCould not access max-health attribute for " + target.getName() + ".");
            return true;
        }
        this.plugin.getLogger().info("[fixhearts] " + target.getName() + " baseValue=" + attr.getBaseValue() + " value=" + attr.getValue() + " health=" + target.getHealth());
        for (AttributeModifier m : attr.getModifiers()) {
            Object keyDesc = "n/a";
            try {
                NamespacedKey k = m.getKey();
                if (k != null) {
                    keyDesc = k.toString();
                }
            }
            catch (Throwable k) {
                // empty catch block
            }
            this.plugin.getLogger().info("[fixhearts]   modifier name=" + m.getName() + " key=" + (String)keyDesc + " uuid=" + String.valueOf(m.getUniqueId()) + " amount=" + m.getAmount() + " op=" + String.valueOf(m.getOperation()));
        }
        if (this.plugin.getLifeAbilities() != null) {
            this.plugin.getLifeAbilities().cleanup(target);
        }
        if (this.plugin.getSoulManager() != null) {
            this.plugin.getSoulManager().cleanup(target);
        }
        int removed = 0;
        ArrayList<AttributeModifier> toRemove = new ArrayList<>(attr.getModifiers());
        for (AttributeModifier m : toRemove) {
            attr.removeModifier(m);
            ++removed;
        }
        double defaultBase = attr.getDefaultValue();
        attr.setBaseValue(defaultBase);
        target.setHealth(attr.getValue());
        target.sendMessage("\u00a7d\u00a7l\u2764 \u00a7fYour hearts have been restored.");
        if (sender != target) {
            sender.sendMessage("\u00a7aFixed hearts for \u00a7l" + target.getName() + "\u00a7a (removed " + removed + " modifier(s)).");
        }
        this.plugin.getLogger().info("[fixhearts] " + sender.getName() + " reset hearts for " + target.getName() + " (removed " + removed + " modifier(s))");
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(partial)).collect(Collectors.toList());
        }
        return new ArrayList<String>();
    }
}

