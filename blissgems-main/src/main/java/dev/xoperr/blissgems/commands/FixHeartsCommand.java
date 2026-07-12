package dev.xoperr.blissgems.commands;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FixHeartsCommand implements CommandExecutor, TabCompleter {
    private final BlissGems plugin;

    public FixHeartsCommand(BlissGems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("blissgems.admin") && !sender.hasPermission("blissgems.fixhearts")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        Player target;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cUsage: /fixhearts <player>");
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

        AttributeInstance attr = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) {
            sender.sendMessage("§cCould not access max-health attribute for " + target.getName() + ".");
            return true;
        }

        // Diagnostic: dump every modifier on the player's max-health BEFORE any cleanup,
        // so we can see exactly what's attached and which filter (if any) is missing them.
        this.plugin.getLogger().info("[fixhearts] " + target.getName()
            + " baseValue=" + attr.getBaseValue()
            + " value=" + attr.getValue()
            + " health=" + target.getHealth());
        for (org.bukkit.attribute.AttributeModifier m : attr.getModifiers()) {
            String keyDesc = "n/a";
            try {
                org.bukkit.NamespacedKey k = m.getKey();
                if (k != null) keyDesc = k.toString();
            } catch (Throwable ignored) {}
            this.plugin.getLogger().info("[fixhearts]   modifier name=" + m.getName()
                + " key=" + keyDesc
                + " uuid=" + m.getUniqueId()
                + " amount=" + m.getAmount()
                + " op=" + m.getOperation());
        }

        // Run plugin-level cleanups so they can drop their tracking state too
        if (this.plugin.getLifeAbilities() != null) {
            this.plugin.getLifeAbilities().cleanup(target);
        }
        if (this.plugin.getSoulManager() != null) {
            this.plugin.getSoulManager().cleanup(target);
        }

        // Nuclear option: strip EVERY modifier on max-health. /fixhearts is admin-only
        // and the whole point is to force the player back to vanilla 10 hearts, regardless
        // of which plugin or vanilla source (health boost potion, etc.) is responsible.
        int removed = 0;
        List<org.bukkit.attribute.AttributeModifier> toRemove = new ArrayList<>(attr.getModifiers());
        for (org.bukkit.attribute.AttributeModifier m : toRemove) {
            attr.removeModifier(m);
            removed++;
        }

        // Force base value back to vanilla default (20.0 = 10 hearts)
        double defaultBase = attr.getDefaultValue();
        attr.setBaseValue(defaultBase);

        // Set current health to the new max so the player visibly snaps to 10 hearts
        target.setHealth(attr.getValue());

        target.sendMessage("§d§l❤ §fYour hearts have been restored.");
        if (sender != target) {
            sender.sendMessage("§aFixed hearts for §l" + target.getName()
                + "§a (removed " + removed + " modifier(s)).");
        }
        this.plugin.getLogger().info("[fixhearts] " + sender.getName() + " reset hearts for "
            + target.getName() + " (removed " + removed + " modifier(s))");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
