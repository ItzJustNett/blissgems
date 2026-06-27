package dev.xoperr.blissgems.commands;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /fixedhearts everyone true|false
 *
 * Toggles the global "fixed hearts" feature. When enabled, every player is
 * forced back to exactly 10 hearts (vanilla 20.0 max health) when they join,
 * and all currently-online players are reset immediately. The setting persists
 * in config.yml so it survives restarts.
 */
public class FixedHeartsCommand implements CommandExecutor, TabCompleter {
    private final BlissGems plugin;

    public FixedHeartsCommand(BlissGems plugin) {
        this.plugin = plugin;
    }

    /**
     * Force a single player back to exactly 10 hearts (vanilla 20.0 max health),
     * stripping every max-health modifier and resetting the base value. Current
     * health is clamped down to the new max if needed (never healed up).
     *
     * @return the number of modifiers removed, or -1 if the attribute was unavailable.
     */
    public static int applyTenHearts(BlissGems plugin, Player target) {
        AttributeInstance attr = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) {
            return -1;
        }

        // Let plugin systems drop their own max-health tracking state first.
        if (plugin.getLifeAbilities() != null) {
            plugin.getLifeAbilities().cleanup(target);
        }
        if (plugin.getSoulManager() != null) {
            plugin.getSoulManager().cleanup(target);
        }

        // Strip every modifier (from any source) and reset base to vanilla 20.0.
        int removed = 0;
        for (AttributeModifier m : new ArrayList<>(attr.getModifiers())) {
            attr.removeModifier(m);
            removed++;
        }
        attr.setBaseValue(attr.getDefaultValue());

        // Clamp current health down to the new max so setHealth never exceeds it.
        if (target.getHealth() > attr.getValue()) {
            target.setHealth(attr.getValue());
        }
        return removed;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("blissgems.admin") && !sender.hasPermission("blissgems.fixedhearts")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("everyone")) {
            sender.sendMessage("§cUsage: §e/fixedhearts everyone <true|false>");
            sender.sendMessage("§7Currently: " + (this.plugin.getConfigManager().isFixedHeartsEnabled()
                ? "§aenabled" : "§cdisabled"));
            return true;
        }

        Boolean enabled = parseBoolean(args[1]);
        if (enabled == null) {
            sender.sendMessage("§cExpected §etrue §cor §efalse§c, got §e" + args[1] + "§c.");
            return true;
        }

        this.plugin.getConfigManager().setFixedHeartsEnabled(enabled);

        if (enabled) {
            int affected = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (applyTenHearts(this.plugin, online) >= 0) {
                    affected++;
                }
            }
            sender.sendMessage("§d§l❤ §aFixed hearts §lENABLED§a. Everyone is locked to 10 hearts on join "
                + "§7(reset " + affected + " online player(s)).");
            this.plugin.getLogger().info("[fixedhearts] " + sender.getName()
                + " ENABLED fixed hearts (reset " + affected + " online player(s))");
        } else {
            sender.sendMessage("§d§l❤ §eFixed hearts §lDISABLED§e. Players keep their current hearts on join.");
            this.plugin.getLogger().info("[fixedhearts] " + sender.getName() + " DISABLED fixed hearts");
        }
        return true;
    }

    private static Boolean parseBoolean(String s) {
        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("on") || s.equalsIgnoreCase("enable")
            || s.equalsIgnoreCase("enabled") || s.equalsIgnoreCase("yes")) {
            return Boolean.TRUE;
        }
        if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("off") || s.equalsIgnoreCase("disable")
            || s.equalsIgnoreCase("disabled") || s.equalsIgnoreCase("no")) {
            return Boolean.FALSE;
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("everyone").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("everyone")) {
            return Arrays.asList("true", "false").stream()
                .filter(s -> s.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
