/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Sound
 *  org.bukkit.attribute.AttributeInstance
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 */
package dev.xoperr.blissgems.commands;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.api.GemDefinition;
import dev.xoperr.blissgems.api.GemRegistry;
import dev.xoperr.blissgems.commands.StatsCommand;
import dev.xoperr.blissgems.managers.AbilityBindingManager;
import dev.xoperr.blissgems.managers.GemLockManager;
import dev.xoperr.blissgems.managers.GemRegistryImpl;
import dev.xoperr.blissgems.managers.GoldGemManager;
import dev.xoperr.blissgems.managers.MaceVillagerManager;
import dev.xoperr.blissgems.managers.SoulManager;
import dev.xoperr.blissgems.utils.AbilityBinding;
import dev.xoperr.blissgems.utils.AbilitySlot;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.Attributes;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.EnergyState;
import dev.xoperr.blissgems.utils.GemType;
import dev.xoperr.blissgems.utils.OraxenGemFixer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class BlissCommand
implements CommandExecutor,
TabCompleter {
    private final BlissGems plugin;
    private static final Set<String> TRANSFERABLE_ITEMS = Set.of("gem_trader", "gem_upgrader", "energy_bottle", "repair_kit");

    public BlissCommand(BlissGems plugin) {
        this.plugin = plugin;
    }

    private boolean requirePlayer(CommandSender sender) {
        if (sender instanceof Player) {
            return true;
        }
        sender.sendMessage("\u00a7cOnly players can use this command!");
        return false;
    }

    private boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission("blissgems.admin")) {
            return true;
        }
        sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission", new Object[0]));
        return false;
    }

    private void sendConfigMessageIfPresent(Player player, String key) {
        String msg = this.plugin.getConfigManager().getFormattedMessage(key, new Object[0]);
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    private int gemTierOf(GemRegistry registry, String oraxenId) {
        return registry != null ? registry.tierFromItemId(oraxenId) : (oraxenId.endsWith("_gem_t2") ? 2 : 1);
    }

    private static Boolean parseToggle(String value) {
        switch (value.toLowerCase()) {
            case "true": 
            case "on": 
            case "yes": 
            case "1": {
                return Boolean.TRUE;
            }
            case "false": 
            case "off": 
            case "no": 
            case "0": {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    private void clearGemsFromInventory(Player target) {
        PlayerInventory inv = target.getInventory();
        for (int i = 0; i < inv.getSize(); ++i) {
            ItemStack item = inv.getItem(i);
            if (item == null || !this.plugin.getGemManager().isAnyGem(CustomItemManager.getIdByItem(item))) continue;
            inv.setItem(i, null);
        }
    }

    private static List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                this.plugin.getEnhancedGuiManager().openMainMenu((Player)sender);
            } else {
                this.sendHelp(sender);
            }
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "give": {
                this.handleGive(sender, args);
                break;
            }
            case "reroll": {
                this.handleReroll(sender, args);
                break;
            }
            case "giveitem": {
                this.handleGiveItem(sender, args);
                break;
            }
            case "transfer": {
                this.handleTransfer(sender, args);
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
            case "whoowns": {
                this.handleWhoOwns(sender, args);
                break;
            }
            case "reload": {
                this.handleReload(sender, args);
                break;
            }
            case "pockets": {
                this.handlePockets(sender, args);
                break;
            }
            case "amplify": {
                this.handleAmplify(sender, args);
                break;
            }
            case "toggle_click": {
                this.handleToggleClick(sender, args);
                break;
            }
            case "ability:main": {
                this.handleAbilityMain(sender, args);
                break;
            }
            case "ability:secondary": {
                this.handleAbilitySecondary(sender, args);
                break;
            }
            case "ability:tertiary": {
                this.handleAbilityTertiary(sender, args);
                break;
            }
            case "ability:quaternary": {
                this.handleAbilityQuaternary(sender, args);
                break;
            }
            case "ability:quinary": {
                this.handleExtraSlot(sender, AbilitySlot.QUINARY);
                break;
            }
            case "ability:senary": {
                this.handleExtraSlot(sender, AbilitySlot.SENARY);
                break;
            }
            case "trust": {
                this.handleTrust(sender, args);
                break;
            }
            case "untrust": {
                this.handleUntrust(sender, args);
                break;
            }
            case "trusted": {
                this.handleTrustedList(sender, args);
                break;
            }
            case "bannable": {
                this.handleBannable(sender, args);
                break;
            }
            case "oraxen": {
                this.handleOraxen(sender, args);
                break;
            }
            case "autosmelt": {
                this.handleAutoSmelt(sender, args);
                break;
            }
            case "conduction": {
                this.handleConduction(sender, args);
                break;
            }
            case "charge": {
                this.handleCharge(sender, args);
                break;
            }
            case "setwatts": {
                this.handleSetWatts(sender, args);
                break;
            }
            case "getwatts": {
                this.handleGetWatts(sender, args);
                break;
            }
            case "stats": {
                this.handleStats(sender, args);
                break;
            }
            case "release": {
                this.handleReleaseSouls(sender, args);
                break;
            }
            case "souls": {
                this.handleSoulsInfo(sender, args);
                break;
            }
            case "achievements": {
                this.handleAchievements(sender, args);
                break;
            }
            case "normalise": 
            case "normalize": {
                this.handleNormalise(sender, args);
                break;
            }
            case "smp": {
                this.handleSmp(sender, args);
                break;
            }
            case "clearcds": {
                this.handleClearCooldowns(sender, args);
                break;
            }
            case "nocdtoggle": {
                this.handleNoCooldownToggle(sender, args);
                break;
            }
            case "ability": {
                if (args.length > 1) {
                    String sub = args[1].toLowerCase();
                    switch (sub) {
                        case "main":
                        case "primary": {
                            this.handleAbilityMain(sender, args);
                            return true;
                        }
                        case "secondary": {
                            this.handleAbilitySecondary(sender, args);
                            return true;
                        }
                        case "tertiary": {
                            this.handleAbilityTertiary(sender, args);
                            return true;
                        }
                        case "quaternary": {
                            this.handleAbilityQuaternary(sender, args);
                            return true;
                        }
                        case "quinary": {
                            this.handleExtraSlot(sender, AbilitySlot.QUINARY);
                            return true;
                        }
                        case "senary": {
                            this.handleExtraSlot(sender, AbilitySlot.SENARY);
                            return true;
                        }
                        default:
                            break;
                    }
                }
                this.handleAbilityBindingsList(sender, args);
                break;
            }
            case "primary":
            case "main": {
                this.handleAbilityMain(sender, args);
                break;
            }
            case "secondary": {
                this.handleAbilitySecondary(sender, args);
                break;
            }
            case "tertiary": {
                this.handleAbilityTertiary(sender, args);
                break;
            }
            case "quaternary": {
                this.handleAbilityQuaternary(sender, args);
                break;
            }
            case "quinary": {
                this.handleExtraSlot(sender, AbilitySlot.QUINARY);
                break;
            }
            case "senary": {
                this.handleExtraSlot(sender, AbilitySlot.SENARY);
                break;
            }
            case "viewer":
            case "gemviewer":
            case "gui":
            case "menu": {
                if (sender instanceof Player) {
                    this.plugin.getEnhancedGuiManager().openMainMenu((Player)sender);
                } else {
                    sender.sendMessage("\u00a7cOnly players can open the gem viewer!");
                }
                break;
            }
            case "set_ability": {
                this.handleSetAbility(sender, args);
                break;
            }
            case "goldgem": {
                this.handleGoldGem(sender, args);
                break;
            }
            case "goldcycle": {
                this.handleGoldCycle(sender);
                break;
            }
            case "goldarmor": 
            case "goldarmour": {
                this.handleGoldArmorToggle(sender);
                break;
            }
            case "enchantlimit": {
                this.handleEnchantLimit(sender, args);
                break;
            }
            case "spawnvillager": {
                this.handleSpawnVillager(sender, args);
                break;
            }
            case "news": {
                new NewsCommand(this.plugin).onCommand(sender, command, label, args);
                break;
            }
            default: {
                this.sendHelp(sender);
            }
        }
        return true;
    }

    private void handleEnchantLimit(CommandSender sender, String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(org.bukkit.ChatColor.GOLD + "=== Enchantment Limits ===");
            if (this.plugin.getEnchantLimiterManager() != null) {
                for (java.util.Map.Entry<String, Integer> e : this.plugin.getEnchantLimiterManager().getAllLimits().entrySet()) {
                    sender.sendMessage(org.bukkit.ChatColor.YELLOW + e.getKey() + ": " + org.bukkit.ChatColor.WHITE + e.getValue());
                }
            }
            sender.sendMessage(org.bukkit.ChatColor.GRAY + "Usage: /bliss enchantlimit <enchantment> <maxLevel>");
            return;
        }
        String enchant = args[1].toLowerCase();
        int maxLevel;
        try {
            maxLevel = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "Invalid number for maxLevel: " + args[2]);
            return;
        }
        if (this.plugin.getEnchantLimiterManager() != null) {
            this.plugin.getEnchantLimiterManager().setLimit(enchant, maxLevel);
            sender.sendMessage(org.bukkit.ChatColor.GREEN + "Set enchantment limit for " + org.bukkit.ChatColor.YELLOW + enchant + org.bukkit.ChatColor.GREEN + " to " + org.bukkit.ChatColor.YELLOW + maxLevel);
        }
    }

    private void handleSpawnVillager(CommandSender sender, String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "Only players can run this command.");
            return;
        }
        Player player = (Player) sender;
        if (args.length < 2) {
            player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /bliss spawnvillager <mace1|mace2|mace3|energy>");
            return;
        }
        String typeKey = args[1].toLowerCase();
        String type;
        if (typeKey.contains("1") || typeKey.equalsIgnoreCase("mace1")) {
            type = MaceVillagerManager.TYPE_MACE_1;
        } else if (typeKey.contains("2") || typeKey.equalsIgnoreCase("mace2")) {
            type = MaceVillagerManager.TYPE_MACE_2;
        } else if (typeKey.contains("3") || typeKey.equalsIgnoreCase("mace3")) {
            type = MaceVillagerManager.TYPE_MACE_3;
        } else if (typeKey.contains("energy") || typeKey.equalsIgnoreCase("energygames")) {
            type = MaceVillagerManager.TYPE_ENERGY;
        } else {
            player.sendMessage(org.bukkit.ChatColor.RED + "Unknown villager type. Options: mace1, mace2, mace3, energy");
            return;
        }
        if (this.plugin.getMaceVillagerManager() != null) {
            this.plugin.getMaceVillagerManager().spawnCustomVillager(player.getLocation(), type);
            player.sendMessage(org.bukkit.ChatColor.GREEN + "Spawned custom merchant: " + org.bukkit.ChatColor.YELLOW + type);
        }
    }

    private void handleGive(CommandSender sender, String[] args) {
        GemRegistryImpl tierRegistry;
        GemDefinition tierDef;
        GemDefinition def;
        GemRegistryImpl registry;
        if (!this.requireAdmin(sender)) {
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
        String gemIdArg = args[2].toLowerCase();
        String resolvedGemId = null;
        String resolvedDisplayName = null;
        for (GemType type : GemType.values()) {
            if (!type.getId().equalsIgnoreCase(gemIdArg) && !type.getDisplayName().equalsIgnoreCase(gemIdArg)) continue;
            resolvedGemId = type.getId();
            resolvedDisplayName = type.getDisplayName();
            break;
        }
        if (resolvedGemId == null && (registry = this.plugin.getGemRegistry()) != null && (def = registry.getGem(gemIdArg)) != null) {
            resolvedGemId = def.getId();
            resolvedDisplayName = def.getDisplayName();
        }
        if (resolvedGemId == null) {
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
        GemDefinition gemDefinition = tierDef = (tierRegistry = this.plugin.getGemRegistry()) != null ? tierRegistry.getGem(resolvedGemId) : null;
        if (tierDef != null && tier > tierDef.getMaxTier()) {
            tier = tierDef.getMaxTier();
            sender.sendMessage("\u00a7e" + resolvedDisplayName + " has no Tier 2 - giving Tier " + tier + " instead.");
            if ("gold".equals(resolvedGemId)) {
                sender.sendMessage("\u00a77The Gold Gem's secondary abilities come from the tier of the soul it channels: \u00a7f/bliss goldgem fill <player> <soul> 2\u00a77.");
            }
        }
        this.clearGemsFromInventory(target);
        if (this.plugin.getGemManager().giveGem(target, resolvedGemId, tier)) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("gem-given", "player", target.getName(), "gem", resolvedDisplayName, "tier", tier));
        } else {
            sender.sendMessage("\u00a7cFailed to give gem!");
        }
    }

    private void handleGoldGem(CommandSender sender, String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            this.sendGoldGemUsage(sender);
            return;
        }
        Player target = Bukkit.getPlayer((String)args[2]);
        if (target == null) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found", new Object[0]));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "fill": {
                this.handleGoldGemFill(sender, target, args);
                break;
            }
            case "remove": {
                this.handleGoldGemRemove(sender, target, args);
                break;
            }
            case "clear": {
                int removed = this.plugin.getGoldGemManager().clearSouls(target);
                sender.sendMessage(removed > 0 ? "\u00a7aEmptied " + target.getName() + "'s Gold Gem (" + removed + " soul(s) removed)." : "\u00a7e" + target.getName() + "'s Gold Gem is already dormant.");
                break;
            }
            case "list": {
                this.handleGoldGemList(sender, target);
                break;
            }
            default: {
                this.sendGoldGemUsage(sender);
            }
        }
    }

    private void sendGoldGemUsage(CommandSender sender) {
        sender.sendMessage("\u00a7cUsage:");
        sender.sendMessage("\u00a77/bliss goldgem fill <player> <soulType> <tier>");
        sender.sendMessage("\u00a77/bliss goldgem remove <player> <soulType>");
        sender.sendMessage("\u00a77/bliss goldgem clear <player>");
        sender.sendMessage("\u00a77/bliss goldgem list <player>");
    }

    private void handleGoldCycle(CommandSender sender) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        GoldGemManager gold = this.plugin.getGoldGemManager();
        if (gold == null || !gold.holdsGoldGem(player)) {
            player.sendMessage("\u00a7cYou are not carrying the Gold Gem.");
            return;
        }
        String next = gold.cycleActive(player);
        if (next == null) {
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-no-soul"));
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.4f);
        player.sendMessage(this.plugin.getConfigManager().getMessage("gold-soul-selected").replace("{gem}", this.plugin.getGemManager().getGemDisplayName(next)));
    }

    private void handleGoldArmorToggle(CommandSender sender) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        GoldGemManager gold = this.plugin.getGoldGemManager();
        if (gold == null || !gold.holdsGoldGem(player)) {
            player.sendMessage("\u00a7cYou are not carrying the Gold Gem.");
            return;
        }
        boolean enabled = gold.toggleTrims(player);
        player.sendMessage(enabled ? "\u00a76Gold armour trims \u00a7aenabled\u00a76." : "\u00a77Gold armour trims \u00a7cdisabled\u00a77. Your original trims are restored.");
    }

    private String resolveGemIdArg(CommandSender sender, String arg) {
        GemDefinition def;
        String gemIdArg = arg.toLowerCase();
        for (GemType type : GemType.values()) {
            if (!type.getId().equalsIgnoreCase(gemIdArg) && !type.getDisplayName().equalsIgnoreCase(gemIdArg)) continue;
            return type.getId();
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        if (registry != null && (def = registry.getGem(gemIdArg)) != null) {
            return def.getId();
        }
        sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("invalid-gem-type", new Object[0]));
        return null;
    }

    private void handleGoldGemFill(CommandSender sender, Player target, String[] args) {
        int tier;
        if (args.length < 5) {
            sender.sendMessage("\u00a7cUsage: /bliss goldgem fill <player> <soulType> <tier>");
            return;
        }
        String resolvedGemId = this.resolveGemIdArg(sender, args[3]);
        if (resolvedGemId == null) {
            return;
        }
        try {
            tier = Integer.parseInt(args[4]);
        }
        catch (NumberFormatException e) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("invalid-tier", new Object[0]));
            return;
        }
        if (tier < 1 || tier > 2) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("invalid-tier", new Object[0]));
            return;
        }
        this.plugin.getGoldGemManager().fillSoul(target, resolvedGemId, tier);
        sender.sendMessage("\u00a7aFilled " + target.getName() + "'s Gold Gem with a Tier " + tier + " " + this.plugin.getGemManager().getGemDisplayName(resolvedGemId) + " \u00a7asoul.");
    }

    private void handleGoldGemRemove(CommandSender sender, Player target, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("\u00a7cUsage: /bliss goldgem remove <player> <soulType>");
            return;
        }
        String resolvedGemId = this.resolveGemIdArg(sender, args[3]);
        if (resolvedGemId == null) {
            return;
        }
        String display = this.plugin.getGemManager().getGemDisplayName(resolvedGemId);
        if (this.plugin.getGoldGemManager().removeSoul(target, resolvedGemId)) {
            sender.sendMessage("\u00a7aRemoved the " + display + " \u00a7asoul from " + target.getName() + "'s Gold Gem.");
        } else {
            sender.sendMessage("\u00a7c" + target.getName() + "'s Gold Gem has no " + display + " \u00a7csoul.");
        }
    }

    private void handleGoldGemList(CommandSender sender, Player target) {
        Map<String, GoldGemManager.Harvest> souls = this.plugin.getGoldGemManager().getHarvested(target.getUniqueId());
        if (souls.isEmpty()) {
            sender.sendMessage("\u00a7e" + target.getName() + "'s Gold Gem is dormant.");
            return;
        }
        String active = this.plugin.getGoldGemManager().getActive(target.getUniqueId());
        sender.sendMessage("\u00a76\u00a7l" + target.getName() + "'s harvested souls \u00a78(" + souls.size() + "/8)");
        for (Map.Entry<String, GoldGemManager.Harvest> soul : souls.entrySet()) {
            sender.sendMessage("\u00a77- " + this.plugin.getGemManager().getGemDisplayName(soul.getKey()) + " \u00a78(T" + soul.getValue().tier() + ")" + (soul.getKey().equals(active) ? " \u00a76\u00a7l[active]" : ""));
        }
    }

    private void handleReroll(CommandSender sender, String[] args) {
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00a7cUsage: /bliss reroll <player> [tier]");
            return;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found", new Object[0]));
            return;
        }
        String randomGem = this.getRandomEnabledGem();
        if (randomGem == null) {
            sender.sendMessage("\u00a7cNo gems are enabled in the config!");
            return;
        }
        int tier = 1;
        if (args.length >= 3) {
            try {
                tier = Integer.parseInt(args[2]);
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
        this.clearGemsFromInventory(target);
        ItemStack offGem = target.getInventory().getItemInOffHand();
        if (offGem != null && this.plugin.getGemManager().isAnyGem(CustomItemManager.getIdByItem(offGem))) {
            target.getInventory().setItemInOffHand(null);
        }
        sender.sendMessage("\u00a7d\u00a7lInitiating gem ritual for " + target.getName() + "...");
        target.sendMessage("\u00a7d\u00a7l\u00a7nGEM REROLL RITUAL");
        target.sendMessage("\u00a77\u00a7oThe ancient powers are choosing your fate...");
        int finalTier = tier;
        this.plugin.getGemRitualManager().performGemRitual(target, randomGem, false, finalTier);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (this.plugin.getGemManager().giveGemToOffhand(target, randomGem, finalTier)) {
                String gemName = this.plugin.getGemManager().getGemDisplayName(randomGem);
                String gemColor = this.plugin.getGemManager().getGemColorCode(randomGem);
                String msg = this.plugin.getConfigManager().getFormattedMessage("gem-rerolled", "player", target.getName(), "gem", gemName, "tier", finalTier);
                if (msg != null && !msg.isEmpty()) {
                    sender.sendMessage(msg);
                } else {
                    sender.sendMessage("\u00a7aRerolled " + target.getName() + "'s gem to " + gemColor + gemName + " \u00a7a(Tier " + finalTier + ")!");
                }
                String targetMsg = this.plugin.getConfigManager().getFormattedMessage("gem-rerolled-received", "gem", gemName, "tier", finalTier);
                if (targetMsg != null && !targetMsg.isEmpty()) {
                    target.sendMessage(targetMsg);
                } else {
                    target.sendMessage("\u00a7d\u00a7l\u00bb \u00a7fYour gem has been chosen: " + gemColor + "\u00a7l" + gemName + " \u00a7f(Tier " + finalTier + ")\u00a7d\u00a7l \u00ab");
                }
            } else {
                sender.sendMessage("\u00a7cFailed to reroll gem!");
            }
        }, 20L);
    }

    private void handleGiveItem(CommandSender sender, String[] args) {
        ItemStack item;
        if (!this.requireAdmin(sender)) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("\u00a7cUsage: /bliss giveitem <player> <item_id> [amount]");
            sender.sendMessage("\u00a77Available items:");
            sender.sendMessage("\u00a7b  - energy_bottle");
            sender.sendMessage("\u00a7b  - repair_kit");
            sender.sendMessage("\u00a7b  - gem_trader");
            sender.sendMessage("\u00a7b  - gem_fragment");
            sender.sendMessage("\u00a7b  - gem_upgrader \u00a77(universal - works for all gems)");
            sender.sendMessage("\u00a7b  - restoration_book \u00a77(revives a Broken gem)");
            sender.sendMessage("\u00a7b  - prismatic_edge \u00a77(legendary sword)");
            return;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found", new Object[0]));
            return;
        }
        String itemId = args[2].toLowerCase();
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount < 1 || amount > 64) {
                    sender.sendMessage("\u00a7cAmount must be between 1 and 64!");
                    return;
                }
            }
            catch (NumberFormatException e) {
                sender.sendMessage("\u00a7cInvalid amount!");
                return;
            }
        }
        if ((item = CustomItemManager.getItemById(itemId)) == null) {
            sender.sendMessage("\u00a7cInvalid item ID: " + itemId);
            sender.sendMessage("\u00a77Available items: energy_bottle, repair_kit, gem_trader, gem_fragment, gem_upgrader, restoration_book, prismatic_edge");
            return;
        }
        item.setAmount(amount);
        target.getInventory().addItem(new ItemStack[]{item});
        ItemMeta itemMeta = item.getItemMeta();
        String itemName = itemMeta != null ? itemMeta.getDisplayName() : itemId;
        sender.sendMessage("\u00a7aGave " + amount + "x " + itemName + " \u00a7ato " + target.getName() + "!");
        target.sendMessage("\u00a7aYou received " + amount + "x " + itemName + "\u00a7a!");
    }

    private void handleTransfer(CommandSender sender, String[] args) {
        int delivered;
        int have;
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can transfer items!");
            return;
        }
        Player player = (Player)sender;
        if (args.length < 3) {
            player.sendMessage("\u00a7cUsage: /bliss transfer <player> <item> [amount]");
            player.sendMessage("\u00a77Transferable: gem_trader, gem_upgrader, energy_bottle, repair_kit");
            return;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found", new Object[0]));
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("\u00a7cYou can't transfer items to yourself!");
            return;
        }
        String itemId = args[2].toLowerCase();
        if (!TRANSFERABLE_ITEMS.contains(itemId)) {
            player.sendMessage("\u00a7cYou can only transfer: \u00a7fgem_trader, gem_upgrader, energy_bottle, repair_kit");
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            }
            catch (NumberFormatException e) {
                player.sendMessage("\u00a7cInvalid amount!");
                return;
            }
            if (amount < 1 || amount > 64) {
                player.sendMessage("\u00a7cAmount must be between 1 and 64!");
                return;
            }
        }
        if ((have = this.countCustomItem(player, itemId)) < amount) {
            player.sendMessage("\u00a7cYou only have \u00a7f" + have + "\u00a7c of that item.");
            return;
        }
        this.removeCustomItem(player, itemId, amount);
        ItemStack give = CustomItemManager.getItemById(itemId);
        if (give == null) {
            player.sendMessage("\u00a7cInvalid item.");
            return;
        }
        give.setAmount(amount);
        HashMap<Integer, ItemStack> leftover = target.getInventory().addItem(new ItemStack[]{give});
        int notDelivered = 0;
        for (ItemStack left : leftover.values()) {
            if (left == null) continue;
            notDelivered += left.getAmount();
        }
        if (notDelivered > 0) {
            ItemStack refund = CustomItemManager.getItemById(itemId);
            refund.setAmount(notDelivered);
            player.getInventory().addItem(new ItemStack[]{refund});
        }
        if ((delivered = amount - notDelivered) <= 0) {
            player.sendMessage("\u00a7c" + target.getName() + "'s inventory is full, nothing was transferred.");
            return;
        }
        String itemName = give.getItemMeta() != null ? give.getItemMeta().getDisplayName() : itemId;
        player.sendMessage("\u00a7aTransferred \u00a7f" + delivered + "x " + itemName + " \u00a7ato " + target.getName() + "!");
        target.sendMessage("\u00a7a" + player.getName() + " \u00a7atransferred you \u00a7f" + delivered + "x " + itemName + "\u00a7a!");
        if (notDelivered > 0) {
            player.sendMessage("\u00a7e" + notDelivered + " couldn't fit and were returned to you.");
        }
    }

    private int countCustomItem(Player player, String itemId) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !itemId.equals(CustomItemManager.getIdByItem(item))) continue;
            total += item.getAmount();
        }
        return total;
    }

    private void removeCustomItem(Player player, String itemId, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; ++i) {
            ItemStack item = contents[i];
            if (item == null || !itemId.equals(CustomItemManager.getIdByItem(item))) continue;
            int take = Math.min(remaining, item.getAmount());
            int left = item.getAmount() - take;
            if (left <= 0) {
                player.getInventory().setItem(i, null);
            } else {
                item.setAmount(left);
            }
            remaining -= take;
        }
    }

    private void handleEnergy(CommandSender sender, String[] args) {
        int amount;
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("\u00a7cOnly players can check their energy!");
                return;
            }
            Player player = (Player)sender;
            int energy = this.plugin.getEnergyManager().getEnergy(player);
            EnergyState state = EnergyState.fromEnergy(energy);
            String energyBar = this.getEnergyBar(energy);
            this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "energy-info-header", new Object[0]);
            this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "energy-info-line1", "energyBar", energyBar, "energy", energy);
            this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "energy-info-line2", "state", state.getDisplayName());
            return;
        }
        if (!this.requireAdmin(sender)) {
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
                int currentEnergy = this.plugin.getEnergyManager().getEnergy(target);
                if (currentEnergy <= 0) {
                    sender.sendMessage("\u00a7c" + target.getName() + " already has 0 energy!");
                    break;
                }
                this.plugin.getEnergyManager().removeEnergy(target, amount);
                sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("energy-removed", "player", target.getName(), "amount", amount));
                break;
            }
            default: {
                sender.sendMessage("\u00a7cUsage: /bliss energy <player> <set/add/remove> <amount>");
            }
        }
    }

    private String getEnergyBar(int energy) {
        StringBuilder bar = new StringBuilder();
        for (int i = 1; i <= 10; ++i) {
            if (i <= energy) {
                bar.append("\u00a7a\u2588");
                continue;
            }
            bar.append("\u00a77\u2588");
        }
        return bar.toString();
    }

    private void handleWithdraw(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        int currentEnergy = this.plugin.getEnergyManager().getEnergy(player);
        if (currentEnergy <= 1) {
            this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "not-enough-energy", new Object[0]);
            return;
        }
        this.plugin.getEnergyManager().removeEnergy(player, 1);
        ItemStack bottle = CustomItemManager.getItemById("energy_bottle");
        if (bottle != null) {
            player.getInventory().addItem(new ItemStack[]{bottle});
            this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "energy-withdrawn", new Object[0]);
        } else {
            this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "energy-bottle-failed", new Object[0]);
        }
    }

    private void handleWhoOwns(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }
        Player player = (Player)sender;
        if (!sender.hasPermission("blissgems.admin")) {
            sender.sendMessage("\u00a7cYou don't have permission to use this command!");
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() == Material.AIR) {
            player.sendMessage("\u00a7cHold an item in your main hand.");
            return;
        }
        UUID owner = CustomItemManager.getOwner(held);
        if (owner == null) {
            player.sendMessage("\u00a7eThat item has no ownership stamp.");
            return;
        }
        OfflinePlayer op = this.plugin.getServer().getOfflinePlayer(owner);
        String name = op.getName() != null ? op.getName() : "unknown";
        player.sendMessage("\u00a7d\u00a7lOwner: \u00a7f" + name + " \u00a77(" + String.valueOf(owner) + ")");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (!this.plugin.getGemManager().hasActiveGem(player)) {
            this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "no-active-gem", new Object[0]);
            return;
        }
        GemType gemType = this.plugin.getGemManager().getGemType(player);
        int tier = this.plugin.getGemManager().getGemTier(player);
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        EnergyState state = this.plugin.getEnergyManager().getEnergyState(player);
        this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "gem-info", "gem", gemType.getDisplayName(), "tier", tier, "energy", energy, "state", state.getDisplayName());
    }

    private void handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blissgems.admin")) {
            this.plugin.getConfigManager().sendFormattedMessage(sender, "no-permission", new Object[0]);
            return;
        }
        this.plugin.getConfigManager().reload();
        this.plugin.getConfigManager().sendFormattedMessage(sender, "config-reloaded", new Object[0]);
    }

    private void handlePockets(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (!this.plugin.getGemManager().hasGemType(player, GemType.WEALTH)) {
            this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "requires-wealth-gem-pockets", new Object[0]);
            return;
        }
        int tier = this.plugin.getGemManager().getGemTier(player, GemType.WEALTH);
        if (tier < 2) {
            this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "requires-wealth-t2-pockets", new Object[0]);
            return;
        }
        this.plugin.getWealthAbilities().pockets(player);
    }

    private void handleAmplify(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (!this.plugin.getGemManager().hasGemType(player, GemType.WEALTH)) {
            player.sendMessage("\u00a7cYou need a Wealth gem to use Amplification!");
            return;
        }
        int tier = this.plugin.getGemManager().getGemTier(player, GemType.WEALTH);
        if (tier < 2) {
            player.sendMessage("\u00a7cAmplification requires Tier 2 Wealth gem!");
            return;
        }
        this.plugin.getWealthAbilities().amplification(player);
    }

    private void handleToggleClick(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        boolean newState = this.plugin.getClickActivationManager().toggleClickActivation(player);
        if (newState) {
            this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "click-activation-enabled", new Object[0]);
        } else {
            this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "click-activation-disabled", new Object[0]);
        }
    }

    private String findGemInHand(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        String oraxenId = CustomItemManager.getIdByItem(mainHand);
        if (oraxenId != null && (GemType.isGem(oraxenId) || registry != null && registry.isRegisteredGem(oraxenId))) {
            return oraxenId;
        }
        oraxenId = CustomItemManager.getIdByItem(offHand);
        if (oraxenId != null && (GemType.isGem(oraxenId) || registry != null && registry.isRegisteredGem(oraxenId))) {
            return oraxenId;
        }
        return null;
    }

    private boolean unlocksAllAtTier1(String oraxenId) {
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        if (registry == null) {
            return false;
        }
        String gemId = registry.gemIdFromItemId(oraxenId);
        GemDefinition def = gemId != null ? registry.getGem(gemId) : null;
        return def != null && def.getMaxTier() < 2;
    }

    private boolean blockedByGemLock(Player player) {
        GemLockManager mgr = this.plugin.getGemLockManager();
        if (mgr != null && mgr.isLocked(player)) {
            int left = mgr.getRemainingSeconds(player.getUniqueId());
            player.sendMessage("\u00a76\ua42c \u00a7c\u00a7oYour gem is locked! \u00a77(" + left + "s)");
            return true;
        }
        return false;
    }

    private void handleAbilityMain(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (this.blockedByGemLock(player)) {
            return;
        }
        String oraxenId = this.findGemInHand(player);
        if (oraxenId == null) {
            this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "must-hold-gem", new Object[0]);
            return;
        }
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        if (energy <= 0) {
            this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "no-energy", new Object[0]);
            return;
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        int tier = registry != null ? registry.tierFromItemId(oraxenId) : (oraxenId.endsWith("_gem_t2") ? 2 : 1);
        GemType gemType = GemType.fromOraxenId(oraxenId);
        if (gemType != null) {
            switch (gemType) {
                case ASTRA: {
                    this.plugin.getAstraAbilities().astralDaggers(player);
                    break;
                }
                case FIRE: {
                    this.plugin.getFireAbilities().chargedFireball(player);
                    break;
                }
                case FLUX: {
                    this.plugin.getFluxAbilities().fluxBeam(player);
                    break;
                }
                case LIFE: {
                    this.plugin.getLifeAbilities().heartDrainer(player);
                    break;
                }
                case PUFF: {
                    this.plugin.getPuffAbilities().dash(player);
                    break;
                }
                case SPEED: {
                    this.plugin.getSpeedAbilities().onRightClick(player, tier);
                    break;
                }
                case STRENGTH: {
                    this.plugin.getStrengthAbilities().chadStrength(player);
                    break;
                }
                case WEALTH: {
                    this.plugin.getWealthAbilities().unfortunate(player);
                }
            }
            return;
        }
        if (registry != null) {
            GemAbilityHandler handler;
            String gemId = registry.gemIdFromItemId(oraxenId);
            GemAbilityHandler gemAbilityHandler = handler = gemId != null ? registry.getAbilityHandler(gemId) : null;
            if (handler != null) {
                handler.onPrimary(player, tier);
            }
        }
    }

    private void handleAbilitySecondary(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (this.blockedByGemLock(player)) {
            return;
        }
        String oraxenId = this.findGemInHand(player);
        if (oraxenId == null) {
            this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "must-hold-gem", new Object[0]);
            return;
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        int tier = registry != null ? registry.tierFromItemId(oraxenId) : (oraxenId.endsWith("_gem_t2") ? 2 : 1);
        if (tier < 2 && !this.unlocksAllAtTier1(oraxenId)) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("requires-tier2", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        if (energy <= 0) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        GemType gemType = GemType.fromOraxenId(oraxenId);
        if (gemType != null) {
            switch (gemType) {
                case ASTRA: {
                    this.plugin.getAstraAbilities().astralProjection(player);
                    break;
                }
                case FIRE: {
                    this.plugin.getFireAbilities().cozyCampfire(player);
                    break;
                }
                case FLUX: {
                    this.plugin.getFluxAbilities().ground(player);
                    break;
                }
                case LIFE: {
                    this.plugin.getLifeAbilities().circleOfLife(player);
                    break;
                }
                case PUFF: {
                    this.plugin.getPuffAbilities().breezyBash(player);
                    break;
                }
                case SPEED: {
                    this.plugin.getSpeedAbilities().speedStorm(player);
                    break;
                }
                case STRENGTH: {
                    this.plugin.getStrengthAbilities().frailer(player);
                    break;
                }
                case WEALTH: {
                    this.plugin.getWealthAbilities().richRush(player);
                }
            }
            return;
        }
        if (registry != null) {
            GemAbilityHandler handler;
            String gemId = registry.gemIdFromItemId(oraxenId);
            GemAbilityHandler gemAbilityHandler = handler = gemId != null ? registry.getAbilityHandler(gemId) : null;
            if (handler != null) {
                handler.onSecondary(player, tier);
            }
        }
    }

    public void triggerPrimary(Player player) {
        this.handleAbilityMain((CommandSender)player, new String[0]);
    }

    public void triggerSecondary(Player player) {
        this.handleAbilitySecondary((CommandSender)player, new String[0]);
    }

    public void triggerTertiary(Player player) {
        this.handleAbilityTertiary((CommandSender)player, new String[0]);
    }

    public void triggerQuaternary(Player player) {
        this.handleAbilityQuaternary((CommandSender)player, new String[0]);
    }

    public void triggerQuinary(Player player) {
        this.handleExtraSlot((CommandSender)player, AbilitySlot.QUINARY);
    }

    public void triggerSenary(Player player) {
        this.handleExtraSlot((CommandSender)player, AbilitySlot.SENARY);
    }

    private void handleExtraSlot(CommandSender sender, AbilitySlot slot) {
        GemAbilityHandler handler;
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }
        Player player = (Player)sender;
        if (this.blockedByGemLock(player)) {
            return;
        }
        String oraxenId = this.findGemInHand(player);
        if (oraxenId == null) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("must-hold-gem", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        if (registry == null) {
            return;
        }
        int tier = registry.tierFromItemId(oraxenId);
        if (tier < 2 && !this.unlocksAllAtTier1(oraxenId)) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("requires-tier2", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        if (energy <= 0) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        String gemId = registry.gemIdFromItemId(oraxenId);
        GemAbilityHandler gemAbilityHandler = handler = gemId != null ? registry.getAbilityHandler(gemId) : null;
        if (handler == null) {
            return;
        }
        if (slot == AbilitySlot.QUINARY) {
            handler.onQuinary(player, tier);
        } else {
            handler.onSenary(player, tier);
        }
    }

    public boolean triggerSlot(Player player, AbilitySlot slot) {
        if (slot == null) {
            return false;
        }
        switch (slot) {
            case PRIMARY: {
                this.triggerPrimary(player);
                return true;
            }
            case SECONDARY: {
                this.triggerSecondary(player);
                return true;
            }
            case TERTIARY: {
                this.triggerTertiary(player);
                return true;
            }
            case QUATERNARY: {
                this.triggerQuaternary(player);
                return true;
            }
            case QUINARY: {
                this.triggerQuinary(player);
                return true;
            }
            case SENARY: {
                this.triggerSenary(player);
                return true;
            }
        }
        return false;
    }

    private void handleAbilityTertiary(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (this.blockedByGemLock(player)) {
            return;
        }
        String oraxenId = this.findGemInHand(player);
        if (oraxenId == null) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("must-hold-gem", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        int tier = registry != null ? registry.tierFromItemId(oraxenId) : (oraxenId.endsWith("_gem_t2") ? 2 : 1);
        if (tier < 2 && !this.unlocksAllAtTier1(oraxenId)) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("requires-tier2", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        if (energy <= 0) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        GemType gemType = GemType.fromOraxenId(oraxenId);
        if (gemType != null) {
            switch (gemType) {
                case FIRE: {
                    this.plugin.getFireAbilities().crisp(player);
                    break;
                }
                case ASTRA: {
                    this.plugin.getAstraAbilities().activateDimensionalDrift(player);
                    break;
                }
                case FLUX: {
                    this.plugin.getFluxAbilities().flashbang(player);
                    break;
                }
                case LIFE: {
                    this.plugin.getLifeAbilities().vitalityVortex(player);
                    break;
                }
                case PUFF: {
                    this.plugin.getPuffAbilities().groupBreezyBash(player);
                    break;
                }
                case STRENGTH: {
                    this.plugin.getStrengthAbilities().shadowStalker(player);
                    break;
                }
                case SPEED: {
                    this.plugin.getSpeedAbilities().activateTerminalVelocity(player);
                    break;
                }
                case WEALTH: {
                    this.plugin.getWealthAbilities().itemLock(player);
                    break;
                }
                default: {
                    player.sendMessage("\u00a7c\u00a7oNo tertiary ability for your gem type!");
                }
            }
            return;
        }
        if (registry != null) {
            GemAbilityHandler handler;
            String gemId = registry.gemIdFromItemId(oraxenId);
            if ("auratus".equals(gemId)) {
                this.plugin.getGemLockManager().castGemLock(player);
                return;
            }
            GemAbilityHandler gemAbilityHandler = handler = gemId != null ? registry.getAbilityHandler(gemId) : null;
            if (handler != null) {
                handler.onTertiary(player, tier);
            }
        }
    }

    private void handleAbilityQuaternary(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (this.blockedByGemLock(player)) {
            return;
        }
        String oraxenId = this.findGemInHand(player);
        if (oraxenId == null) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("must-hold-gem", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        int tier = registry != null ? registry.tierFromItemId(oraxenId) : (oraxenId.endsWith("_gem_t2") ? 2 : 1);
        if (tier < 2 && !this.unlocksAllAtTier1(oraxenId)) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("requires-tier2", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        if (energy <= 0) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        GemType gemType = GemType.fromOraxenId(oraxenId);
        if (gemType != null) {
            switch (gemType) {
                case FIRE: {
                    this.plugin.getFireAbilities().meteorShower(player);
                    break;
                }
                case ASTRA: {
                    this.plugin.getAstraAbilities().activateDimensionalVoid(player);
                    break;
                }
                case FLUX: {
                    this.plugin.getFluxAbilities().kineticBurst(player);
                    break;
                }
                case LIFE: {
                    this.plugin.getLifeAbilities().heartLock(player);
                    break;
                }
                case PUFF: {
                    this.plugin.getPuffAbilities().updraft(player);
                    break;
                }
                case SPEED: {
                    this.plugin.getSpeedAbilities().galeClouds(player);
                    break;
                }
                case STRENGTH: {
                    this.plugin.getStrengthAbilities().nullify(player);
                    break;
                }
                case WEALTH: {
                    this.plugin.getWealthAbilities().amplification(player);
                    break;
                }
                default: {
                    player.sendMessage("\u00a7c\u00a7oNo quaternary ability for your gem type!");
                }
            }
            return;
        }
        if (registry != null) {
            GemAbilityHandler handler;
            String gemId = registry.gemIdFromItemId(oraxenId);
            GemAbilityHandler gemAbilityHandler = handler = gemId != null ? registry.getAbilityHandler(gemId) : null;
            if (handler != null) {
                handler.onQuaternary(player, tier);
            }
        }
    }

    private void handleAbilityBindingsList(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        AbilityBindingManager mgr = this.plugin.getAbilityBindingManager();
        if (mgr == null) {
            player.sendMessage("\u00a7cBinding system unavailable.");
            return;
        }
        EnumMap<AbilityBinding, AbilitySlot> map = mgr.getAll(player);
        player.sendMessage("\u00a7d\u00a7l\u26a1 Your Ability Bindings");
        for (AbilitySlot abilitySlot : AbilitySlot.values()) {
            AbilityBinding boundInput = null;
            for (Map.Entry<AbilityBinding, AbilitySlot> e : map.entrySet()) {
                if (e.getValue() != abilitySlot) continue;
                boundInput = e.getKey();
                break;
            }
            String inputLabel = boundInput != null ? "\u00a7f" + boundInput.getDisplayName() : "\u00a78unbound";
            player.sendMessage("\u00a77" + abilitySlot.getDisplayName() + " \u00a78\u2192 " + inputLabel);
        }
        player.sendMessage("");
        player.sendMessage("\u00a7d\u00a7l\u26a1 Available Inputs");
        for (Enum enum_ : AbilityBinding.values()) {
            AbilitySlot s = map.get(enum_);
            String suffix = s != null ? " \u00a78(\u00a77" + s.getDisplayName() + "\u00a78)" : "";
            player.sendMessage("\u00a7f\u2022 \u00a77" + ((AbilityBinding)enum_).getId() + " \u00a78- \u00a7f" + ((AbilityBinding)enum_).getDisplayName() + suffix);
        }
        player.sendMessage("");
        player.sendMessage("\u00a77Change with \u00a7f/bliss set_ability <slot> <input>");
        player.sendMessage("\u00a77Unbind a slot with \u00a7f/bliss set_ability <slot> none");
        player.sendMessage("\u00a77Reset defaults with \u00a7f/bliss set_ability reset");
    }

    private void handleSetAbility(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        AbilityBindingManager mgr = this.plugin.getAbilityBindingManager();
        if (mgr == null) {
            player.sendMessage("\u00a7cBinding system unavailable.");
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
            mgr.resetToDefaults(player);
            player.sendMessage("\u00a7aAbility bindings reset to defaults.");
            return;
        }
        if (args.length < 3) {
            player.sendMessage("\u00a7cUsage: \u00a7f/bliss set_ability <slot> <input>");
            player.sendMessage("\u00a77Slots: \u00a7fprimary, secondary, tertiary, quaternary");
            player.sendMessage("\u00a77Inputs: \u00a7fright_click, shift_right_click, left_click, shift_left_click, swap_hand, shift_swap_hand, none");
            player.sendMessage("\u00a77See current bindings with \u00a7f/bliss ability");
            return;
        }
        AbilitySlot slot = AbilitySlot.fromId(args[1]);
        if (slot == null) {
            player.sendMessage("\u00a7cUnknown slot: \u00a7f" + args[1] + "\u00a7c. Valid: primary, secondary, tertiary, quaternary.");
            return;
        }
        String inputId = args[2];
        if (inputId.equalsIgnoreCase("none") || inputId.equalsIgnoreCase("unbind")) {
            EnumMap<AbilityBinding, AbilitySlot> map = mgr.getAll(player);
            for (Map.Entry<AbilityBinding, AbilitySlot> e : map.entrySet()) {
                if (e.getValue() != slot) continue;
                mgr.unbind(player, e.getKey());
            }
            player.sendMessage("\u00a7aUnbound \u00a7l" + slot.getDisplayName() + "\u00a7a.");
            return;
        }
        AbilityBinding input = AbilityBinding.fromId(inputId);
        if (input == null) {
            player.sendMessage("\u00a7cUnknown input: \u00a7f" + inputId + "\u00a7c. See \u00a7f/bliss ability\u00a7c for the list.");
            return;
        }
        mgr.setBinding(player, input, slot);
        player.sendMessage("\u00a7aBound \u00a7f" + input.getDisplayName() + "\u00a7a \u2192 \u00a7l" + slot.getDisplayName() + "\u00a7a.");
    }

    private void handleTrust(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00a7cUsage: /bliss trust <player>");
            return;
        }
        Player player = (Player)sender;
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found", new Object[0]));
            return;
        }
        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage("\u00a7cYou already trust yourself!");
            return;
        }
        this.plugin.getTrustedPlayersManager().addTrustedPlayer(player, target);
        player.sendMessage("\u00a7aYou now trust \u00a7l" + target.getName() + "\u00a7r\u00a7a! Your gem abilities will not harm them.");
        target.sendMessage("\u00a7a" + player.getName() + " \u00a7anow trusts you! Their gem abilities will not harm you.");
    }

    private void handleUntrust(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00a7cUsage: /bliss untrust <player>");
            return;
        }
        Player player = (Player)sender;
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found", new Object[0]));
            return;
        }
        boolean removed = this.plugin.getTrustedPlayersManager().removeTrustedPlayer(player, target);
        if (removed) {
            player.sendMessage("\u00a7cYou no longer trust \u00a7l" + target.getName() + "\u00a7r\u00a7c! Your gem abilities can now harm them.");
            target.sendMessage("\u00a7c" + player.getName() + " \u00a7cno longer trusts you! Their gem abilities can now harm you.");
        } else {
            player.sendMessage("\u00a7cYou were not trusting " + target.getName() + "!");
        }
    }

    private void handleTrustedList(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        Set<UUID> trusted = this.plugin.getTrustedPlayersManager().getTrustedPlayers(player);
        if (trusted.isEmpty()) {
            player.sendMessage("\u00a77You have no trusted players. Use \u00a7b/bliss trust <player>\u00a77 to add someone.");
            return;
        }
        player.sendMessage("\u00a75\u00a7lTrusted Players (" + trusted.size() + "):");
        for (UUID uuid : trusted) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)uuid);
            String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
            String status = offlinePlayer.isOnline() ? "\u00a7a[Online]" : "\u00a77[Offline]";
            player.sendMessage("\u00a78 - \u00a7b" + name + " " + status);
        }
    }

    private void handleBannable(CommandSender sender, String[] args) {
        boolean enable;
        if (!sender.hasPermission("blissgems.admin")) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission", new Object[0]));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00a7cUsage: /bliss bannable <true/false>");
            sender.sendMessage("\u00a77Current status: " + (this.plugin.getConfigManager().isBanOnZeroEnergyEnabled() ? "\u00a7aEnabled" : "\u00a7cDisabled"));
            return;
        }
        String value = args[1].toLowerCase();
        if (value.equals("true") || value.equals("on") || value.equals("yes") || value.equals("1")) {
            enable = true;
        } else if (value.equals("false") || value.equals("off") || value.equals("no") || value.equals("0")) {
            enable = false;
        } else {
            sender.sendMessage("\u00a7cInvalid value! Use true or false.");
            return;
        }
        this.plugin.getConfigManager().setBanOnZeroEnergy(enable);
        if (enable) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("ban-enabled", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                sender.sendMessage(msg);
            } else {
                sender.sendMessage("\u00a7aBan-on-zero-energy has been enabled!");
            }
        } else {
            String msg = this.plugin.getConfigManager().getFormattedMessage("ban-disabled", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                sender.sendMessage(msg);
            } else {
                sender.sendMessage("\u00a7cBan-on-zero-energy has been disabled!");
            }
        }
    }

    private void handleOraxen(CommandSender sender, String[] args) {
        boolean enable;
        if (!sender.hasPermission("blissgems.admin")) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission", new Object[0]));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00a7cUsage: /bliss oraxen <true/false>");
            sender.sendMessage("\u00a77Auto-replace legacy gems with Oraxen items on join: " + (OraxenGemFixer.isFixOnJoinEnabled(this.plugin) ? "\u00a7aEnabled" : "\u00a7cDisabled"));
            return;
        }
        String value = args[1].toLowerCase();
        if (value.equals("true") || value.equals("on") || value.equals("yes") || value.equals("1")) {
            enable = true;
        } else if (value.equals("false") || value.equals("off") || value.equals("no") || value.equals("0")) {
            enable = false;
        } else {
            sender.sendMessage("\u00a7cInvalid value! Use true or false.");
            return;
        }
        OraxenGemFixer.setFixOnJoinEnabled(this.plugin, enable);
        if (enable) {
            sender.sendMessage("\u00a7aLegacy gems will now be replaced with Oraxen items when players join!");
        } else {
            sender.sendMessage("\u00a7cAutomatic gem replacement on join has been disabled!");
        }
    }

    private void handleAutoSmelt(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (!player.hasPermission("blissgems.autosmelt")) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission", new Object[0]));
            return;
        }
        boolean hasFire = this.plugin.getGemManager().hasGemType(player, GemType.FIRE);
        boolean hasWealthT2 = this.plugin.getGemManager().hasGemType(player, GemType.WEALTH) && this.plugin.getGemManager().getGemTier(player, GemType.WEALTH) >= 2;
        if (!hasFire && !hasWealthT2) {
            player.sendMessage("\u00a7c\u00a7lYou need the Fire Gem or Wealth Gem (Tier 2) to use auto-smelt!");
            return;
        }
        boolean currentState = this.plugin.getWealthAbilities().isAutoSmeltEnabled(player);
        this.plugin.getWealthAbilities().setAutoSmelt(player, !currentState);
        if (!currentState) {
            player.sendMessage("\u00a7a\u00a7lAuto-Smelt enabled! Ores will now be automatically smelted when mined.");
        } else {
            player.sendMessage("\u00a7c\u00a7lAuto-Smelt disabled!");
        }
    }

    private void handleConduction(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (!this.plugin.getGemManager().hasGemType(player, GemType.FLUX)) {
            player.sendMessage("\u00a7c\u00a7lYou need the Flux Gem to use Conduction!");
            return;
        }
        this.plugin.getFluxAbilities().conduction(player);
    }

    private void handleCharge(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (!this.plugin.getGemManager().hasGemType(player, GemType.FLUX)) {
            player.sendMessage("\u00a7c\u00a7lYou need the Flux Gem to use the Charging Station!");
            return;
        }
        if (this.plugin.getFluxEnergyManager() != null) {
            this.plugin.getFluxEnergyManager().openChargingStation(player);
        }
    }

    private void handleSetWatts(CommandSender sender, String[] args) {
        double amount;
        if (!sender.hasPermission("blissgems.admin")) {
            sender.sendMessage("\u00a7cYou don't have permission to use this command!");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("\u00a7cUsage: /bliss setwatts <player> <amount>");
            return;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            sender.sendMessage("\u00a7cPlayer not found!");
            return;
        }
        try {
            amount = Double.parseDouble(args[2]);
        }
        catch (NumberFormatException e) {
            sender.sendMessage("\u00a7cInvalid number: " + args[2]);
            return;
        }
        if (this.plugin.getFluxEnergyManager() != null) {
            this.plugin.getFluxEnergyManager().setWatts(target.getUniqueId(), amount);
            sender.sendMessage("\u00a7aSet \u00a7e" + target.getName() + "\u00a7a's Flux Gem energy to \u00a7b" + String.format("%,.0f", amount) + " Watts\u00a7a.");
            target.sendMessage("\u00a7b\ud83d\udd2e \u00a7aAn administrator updated your Flux Gem energy to \u00a7b" + String.format("%,.0f", amount) + " Watts\u00a7a.");
        }
    }

    private void handleGetWatts(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 1) {
            if (!sender.hasPermission("blissgems.admin") && !sender.getName().equalsIgnoreCase(args[1])) {
                sender.sendMessage("\u00a7cYou don't have permission to view other players' energy!");
                return;
            }
            target = Bukkit.getPlayer((String)args[1]);
            if (target == null) {
                sender.sendMessage("\u00a7cPlayer not found!");
                return;
            }
        } else {
            if (!this.requirePlayer(sender)) {
                return;
            }
            target = (Player)sender;
        }
        if (this.plugin.getFluxEnergyManager() != null) {
            double watts = this.plugin.getFluxEnergyManager().getWatts(target.getUniqueId());
            int maxWatts = this.plugin.getFluxEnergyManager().getMaxWatts();
            double beam = this.plugin.getFluxEnergyManager().getBeamCharge(target.getUniqueId());
            double percent = watts / (double)maxWatts * 100.0;
            sender.sendMessage("\u00a7b\ud83d\udd2e \u00a76\u00a7lFlux Energy Status for \u00a7e" + target.getName() + "\u00a76:");
            sender.sendMessage("\u00a7f  Battery: \u00a7b" + String.format("%,.0f", watts) + " \u00a77/ \u00a7b" + String.format("%,d", maxWatts) + " W \u00a7a(" + String.format("%.2f%%", percent) + ")");
            sender.sendMessage("\u00a7f  Beam Charge: \u00a7e" + String.format("%.1f%%", beam));
            sender.sendMessage("\u00a7f  Charging State: " + (this.plugin.getFluxEnergyManager().isCharging(target) ? "\u00a7aCharging" : "\u00a7cIdle"));
        }
    }

    private void handleStats(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        StatsCommand statsCommand = new StatsCommand(this.plugin);
        statsCommand.execute(player, args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[]{});
    }

    private void handleReleaseSouls(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (!this.plugin.getGemManager().hasGemType(player, GemType.ASTRA)) {
            player.sendMessage("\u00a7c\u00a7oOnly Astra gem holders can release captured souls!");
            return;
        }
        if (!this.plugin.getEnergyManager().arePassivesActive(player)) {
            player.sendMessage("\u00a7c\u00a7oYour gem energy is too low to release souls!");
            return;
        }
        this.plugin.getSoulManager().releaseAllSouls(player);
    }

    private void handleSoulsInfo(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        if (!this.plugin.getGemManager().hasGemType(player, GemType.ASTRA)) {
            player.sendMessage("\u00a7c\u00a7oOnly Astra gem holders can view captured souls!");
            return;
        }
        List<SoulManager.CapturedMob> souls = this.plugin.getSoulManager().getCapturedSouls(player);
        int count = souls.size();
        int max = 2;
        player.sendMessage("\u00a7d\u00a7lCaptured Souls (" + count + "/" + max + "):");
        if (count == 0) {
            player.sendMessage("\u00a77  No souls captured. Sneak + hit a mob to capture it.");
        } else {
            for (int i = 0; i < souls.size(); ++i) {
                player.sendMessage("\u00a7d  " + (i + 1) + ". \u00a7f" + souls.get(i).getDisplayName());
            }
            player.sendMessage("\u00a77  Use \u00a7d/bliss release\u00a77 to release all captured souls.");
        }
    }

    private void handleNormalise(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blissgems.admin")) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission", new Object[0]));
            return;
        }
        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            AttributeInstance attackSpeed = online.getAttribute(Attributes.attackSpeed());
            if (attackSpeed != null) {
                double original = attackSpeed.getBaseValue();
                attackSpeed.setBaseValue(1024.0);
                attackSpeed.setBaseValue(original);
            }
            ++count;
        }
        sender.sendMessage("\u00a7a\u00a7lAttack cooldown reset for " + count + " online player(s)!");
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage("\u00a7e\u00a7oAttack cooldowns have been normalized by an admin.");
        }
    }

    private void handleSmp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blissgems.admin")) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission", new Object[0]));
            return;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("start")) {
            sender.sendMessage("\u00a7cUsage: /bliss smp start");
            return;
        }
        if (this.plugin.getConfigManager().isSmpStarted()) {
            this.plugin.getConfigManager().sendFormattedMessage(sender, "smp-already-started", new Object[0]);
            return;
        }
        this.plugin.getConfigManager().setSmpStarted(true);
        this.plugin.getConfigManager().sendFormattedMessage(sender, "smp-started", new Object[0]);
        for (Player online : Bukkit.getOnlinePlayers()) {
            String randomGem;
            if (this.hasReceivedFirstGem(online) || (randomGem = this.getRandomEnabledGem()) == null) continue;
            String finalGem = randomGem;
            Player target = online;
            target.sendMessage("");
            target.sendMessage("\u00a7d\u00a7l\u00a7m                                                  ");
            target.sendMessage("\u00a7d\u00a7lWELCOME TO BLISSGEMS!");
            target.sendMessage("");
            target.sendMessage("\u00a77\u00a7oThe ancient gem ritual begins...");
            target.sendMessage("\u00a77\u00a7oYour destiny is being forged...");
            target.sendMessage("\u00a7d\u00a7l\u00a7m                                                  ");
            target.sendMessage("");
            this.plugin.getGemRitualManager().performGemRitual(target, finalGem, true, 1);
            this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                if (target.isOnline() && this.plugin.getGemManager().giveGem(target, finalGem, 1)) {
                    this.markFirstGemReceived(target);
                    String gemName = this.plugin.getGemManager().getGemDisplayName(finalGem);
                    String welcomeMsg = this.plugin.getConfigManager().getFormattedMessage("first-gem-received", "gem", gemName);
                    if (welcomeMsg != null && !welcomeMsg.isEmpty()) {
                        target.sendMessage(welcomeMsg);
                    } else {
                        target.sendMessage("");
                        target.sendMessage("\u00a7d\u00a7l\u00bb \u00a7fYour gem has been chosen: " + this.plugin.getGemManager().getGemColorCode(finalGem) + "\u00a7l" + gemName + "\u00a7d\u00a7l \u00ab");
                        target.sendMessage("");
                    }
                    this.plugin.getLogger().info("SMP Start: Gave " + target.getName() + " their first gem: " + gemName);
                }
            }, 20L);
        }
    }

    private boolean hasReceivedFirstGem(Player player) {
        File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
        File file = new File(dataFolder, String.valueOf(player.getUniqueId()) + ".yml");
        if (!file.exists()) {
            return false;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration((File)file);
        return data.getBoolean("received-first-gem", false);
    }

    private void markFirstGemReceived(Player player) {
        File file;
        File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        YamlConfiguration data = (file = new File(dataFolder, String.valueOf(player.getUniqueId()) + ".yml")).exists() ? YamlConfiguration.loadConfiguration((File)file) : new YamlConfiguration();
        data.set("received-first-gem", (Object)true);
        if (!data.contains("energy")) {
            data.set("energy", (Object)this.plugin.getConfigManager().getStartingEnergy());
        }
        try {
            data.save(file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save first gem status for " + player.getName() + ": " + e.getMessage());
        }
    }

    private String getRandomEnabledGem() {
        List<String> enabledGems = this.plugin.getGemManager().getAvailableGemIds();
        if (enabledGems.isEmpty()) {
            return null;
        }
        return enabledGems.get(new Random().nextInt(enabledGems.size()));
    }

    private void handleClearCooldowns(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blissgems.admin")) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission", new Object[0]));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00a7cUsage: /bliss clearcds <player|all>");
            return;
        }
        if (args[1].equalsIgnoreCase("all")) {
            int count = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                this.plugin.getAbilityManager().clearCooldowns(online);
                ++count;
            }
            sender.sendMessage("\u00a7a\u00a7lCleared all ability cooldowns for " + count + " online player(s)!");
            return;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found", new Object[0]));
            return;
        }
        this.plugin.getAbilityManager().clearCooldowns(target);
        sender.sendMessage("\u00a7aCleared all ability cooldowns for \u00a7l" + target.getName() + "\u00a7a!");
    }

    private void handleNoCooldownToggle(CommandSender sender, String[] args) {
        Player target;
        if (!sender.hasPermission("blissgems.admin")) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission", new Object[0]));
            return;
        }
        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("\u00a7cUsage: /bliss nocdtoggle <player>");
                return;
            }
            target = (Player)sender;
        } else {
            target = Bukkit.getPlayer((String)args[1]);
            if (target == null) {
                sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found", new Object[0]));
                return;
            }
        }
        boolean enabled = this.plugin.getAbilityManager().toggleNoCooldown(target);
        if (enabled) {
            sender.sendMessage("\u00a7a\u00a7lNo-cooldown \u00a7aENABLED for \u00a7l" + target.getName() + "\u00a7a!");
            if (target != sender) {
                target.sendMessage("\u00a7a\u00a7oYour ability cooldowns have been disabled.");
            }
        } else {
            sender.sendMessage("\u00a7c\u00a7lNo-cooldown \u00a7cDISABLED for \u00a7l" + target.getName() + "\u00a7c!");
            if (target != sender) {
                target.sendMessage("\u00a7c\u00a7oYour ability cooldowns are back to normal.");
            }
        }
    }

    private void handleAchievements(CommandSender sender, String[] args) {
        if (!this.requirePlayer(sender)) {
            return;
        }
        Player player = (Player)sender;
        Set<Achievement> unlocked = this.plugin.getAchievementManager().getUnlocked(player);
        int total = Achievement.values().length;
        int unlockedCount = unlocked.size();
        player.sendMessage("\u00a76\u00a7l\u2b50 Achievements (" + unlockedCount + "/" + total + ")");
        player.sendMessage("");
        for (Achievement achievement : Achievement.values()) {
            boolean isUnlocked = unlocked.contains((Object)achievement);
            int progress = this.plugin.getAchievementManager().getProgress(player, achievement);
            int target = achievement.getTargetProgress();
            if (isUnlocked) {
                player.sendMessage("\u00a7a\u2714 \u00a7e" + achievement.getDisplayName() + " \u00a77- " + achievement.getDescription());
                continue;
            }
            if (target > 1 && progress > 0) {
                player.sendMessage("\u00a78\u2718 \u00a77" + achievement.getDisplayName() + " \u00a78- " + achievement.getDescription() + " \u00a7e(" + progress + "/" + target + ")");
                continue;
            }
            player.sendMessage("\u00a78\u2718 \u00a77" + achievement.getDisplayName() + " \u00a78- " + achievement.getDescription());
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("\u00a75\u00a7lBlissGems Commands:");
        sender.sendMessage("\u00a77/bliss give <player> <gem_type> [tier] \u00a78- Give a gem");
        sender.sendMessage("\u00a77/bliss reroll <player> [tier] \u00a78- Give a random gem");
        sender.sendMessage("\u00a77/bliss giveitem <player> <item_id> [amount] \u00a78- Give special items");
        sender.sendMessage("\u00a77/bliss energy <player> <set/add/remove> <amount> \u00a78- Manage energy");
        sender.sendMessage("\u00a77/bliss withdraw \u00a78- Extract energy into bottle");
        sender.sendMessage("\u00a77/bliss info \u00a78- Show your gem info");
        sender.sendMessage("\u00a77/bliss pockets \u00a78- Open personal inventory (Wealth T2)");
        sender.sendMessage("\u00a77/bliss amplify \u00a78- Amplify potion effects (Wealth T2)");
        sender.sendMessage("\u00a77/bliss autosmelt \u00a78- Toggle auto-smelting (Wealth T2)");
        sender.sendMessage("\u00a77/bliss conduction \u00a78- Teleport to nearest copper block (Flux)");
        sender.sendMessage("\u00a77/bliss toggle_click \u00a78- Toggle click activation on/off");
        sender.sendMessage("\u00a77/bliss ability:main \u00a78- Trigger primary ability");
        sender.sendMessage("\u00a77/bliss ability:secondary \u00a78- Trigger secondary ability (T2)");
        sender.sendMessage("\u00a77/bliss trust <player> \u00a78- Trust player (prevent friendly fire)");
        sender.sendMessage("\u00a77/bliss untrust <player> \u00a78- Untrust player");
        sender.sendMessage("\u00a77/bliss trusted \u00a78- List trusted players");
        sender.sendMessage("\u00a77/bliss souls \u00a78- View captured souls (Astra)");
        sender.sendMessage("\u00a77/bliss release \u00a78- Release captured souls (Astra)");
        sender.sendMessage("\u00a77/bliss achievements \u00a78- View your achievements");
        sender.sendMessage("\u00a77/bliss stats [top|me|gems] \u00a78- View server stats");
        sender.sendMessage("\u00a77/bliss bannable <true/false> \u00a78- Toggle ban on 0 energy (Admin)");
        sender.sendMessage("\u00a77/bliss smp start \u00a78- Start the SMP and distribute gems (Admin)");
        sender.sendMessage("\u00a77/bliss normalise \u00a78- Reset attack cooldowns for all players (Admin)");
        sender.sendMessage("\u00a77/bliss clearcds <player|all> \u00a78- Clear ability cooldowns (Admin)");
        sender.sendMessage("\u00a77/bliss nocdtoggle <player> \u00a78- Toggle no ability cooldowns (Admin)");
        sender.sendMessage("\u00a77/bliss goldgem fill <player> <soulType> <tier> \u00a78- Add a harvested soul (Admin)");
        sender.sendMessage("\u00a77/bliss goldgem remove <player> <soulType> \u00a78- Take a soul back out (Admin)");
        sender.sendMessage("\u00a77/bliss goldgem clear <player> \u00a78- Empty a Gold Gem (Admin)");
        sender.sendMessage("\u00a77/bliss goldgem list <player> \u00a78- List harvested souls (Admin)");
        sender.sendMessage("\u00a77/bliss ability \u00a78- Show your ability keybinds");
        sender.sendMessage("\u00a77/bliss set_ability <slot> <input> \u00a78- Rebind an ability input");
        sender.sendMessage("\u00a77/bliss reload \u00a78- Reload config");
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> completions = new ArrayList<String>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("give", "reroll", "giveitem", "transfer", "energy", "withdraw", "info", "pockets", "amplify", "autosmelt", "conduction", "charge", "setwatts", "getwatts", "reload", "toggle_click", "ability:main", "ability:secondary", "ability:tertiary", "ability:quaternary", "primary", "secondary", "tertiary", "quaternary", "quinary", "senary", "viewer", "gemviewer", "gui", "trust", "untrust", "trusted", "stats", "achievements", "bannable", "oraxen", "souls", "release", "normalise", "normalize", "smp", "clearcds", "nocdtoggle", "goldgem", "goldcycle", "goldarmor", "ability", "set_ability", "enchantlimit", "spawnvillager", "news"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("ability")) {
                return Arrays.asList("main", "secondary", "tertiary", "quaternary", "quinary", "senary", "reset");
            }
            if (args[0].equalsIgnoreCase("spawnvillager")) {
                return Arrays.asList("mace1", "mace2", "mace3", "energy");
            }
            if (args[0].equalsIgnoreCase("enchantlimit")) {
                return Arrays.asList("density", "breach", "wind_burst", "sharpness", "protection", "unbreaking", "mending");
            }
            if (args[0].equalsIgnoreCase("nocdtoggle") || args[0].equalsIgnoreCase("setwatts") || args[0].equalsIgnoreCase("getwatts")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("clearcds")) {
                List<String> targets = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                targets.add(0, "all");
                return targets;
            }
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("reroll") || args[0].equalsIgnoreCase("giveitem") || args[0].equalsIgnoreCase("transfer") || args[0].equalsIgnoreCase("energy") || args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("stats")) {
                return Arrays.asList("top", "me", "gems");
            }
            if (args[0].equalsIgnoreCase("bannable") || args[0].equalsIgnoreCase("oraxen")) {
                return Arrays.asList("true", "false");
            }
            if (args[0].equalsIgnoreCase("smp")) {
                return Arrays.asList("start");
            }
            if (args[0].equalsIgnoreCase("goldgem")) {
                return Arrays.asList("fill", "remove", "clear", "list");
            }
            if (args[0].equalsIgnoreCase("set_ability")) {
                List<String> slots = Arrays.stream(AbilitySlot.values()).map(AbilitySlot::getId).collect(Collectors.toList());
                slots.add("reset");
                return slots;
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setwatts")) {
                return Arrays.asList("100000", "500000", "1000000", "2000000");
            }
            if (args[0].equalsIgnoreCase("goldgem")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("give")) {
                List<String> gemIds = Arrays.stream(GemType.values()).map(GemType::getId).collect(Collectors.toList());
                GemRegistryImpl registry = this.plugin.getGemRegistry();
                if (registry != null) {
                    for (GemDefinition def : registry.getAllGems()) {
                        if (gemIds.contains(def.getId())) continue;
                        gemIds.add(def.getId());
                    }
                }
                return gemIds;
            }
            if (args[0].equalsIgnoreCase("giveitem")) {
                ArrayList<String> items = new ArrayList<String>();
                items.add("energy_bottle");
                items.add("repair_kit");
                items.add("gem_trader");
                items.add("gem_fragment");
                items.add("gem_upgrader");
                items.add("restoration_book");
                items.add("prismatic_edge");
                return items;
            }
            if (args[0].equalsIgnoreCase("transfer")) {
                return new ArrayList<String>(TRANSFERABLE_ITEMS);
            }
            if (args[0].equalsIgnoreCase("energy")) {
                return Arrays.asList("set", "add", "remove");
            }
            if (args[0].equalsIgnoreCase("set_ability")) {
                List<String> inputs = Arrays.stream(AbilityBinding.values()).map(AbilityBinding::getId).collect(Collectors.toList());
                inputs.add("none");
                return inputs;
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("reroll")) {
                return Arrays.asList("1", "2");
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("give")) {
                return Arrays.asList("1", "2");
            }
            if (args[0].equalsIgnoreCase("giveitem") || args[0].equalsIgnoreCase("transfer")) {
                return Arrays.asList("1", "8", "16", "32", "64");
            }
            if (args[0].equalsIgnoreCase("goldgem") && (args[1].equalsIgnoreCase("fill") || args[1].equalsIgnoreCase("remove"))) {
                return Arrays.stream(GemType.values()).map(GemType::getId).collect(Collectors.toList());
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("goldgem") && args[1].equalsIgnoreCase("fill")) {
            return Arrays.asList("1", "2");
        }
        return completions.stream().filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
    }
}

