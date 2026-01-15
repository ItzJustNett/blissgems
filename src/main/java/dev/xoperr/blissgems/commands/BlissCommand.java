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
 *  org.bukkit.inventory.ItemStack
 */
package dev.xoperr.blissgems.commands;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.EnergyState;
import dev.xoperr.blissgems.utils.GemType;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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
            // Show GUI if player, otherwise show help
            if (sender instanceof Player) {
                this.plugin.getBlissGuiManager().openMainMenu((Player) sender);
            } else {
                this.sendHelp(sender);
            }
            return true;
        }
        switch (subcommand = args[0].toLowerCase()) {
            case "give": {
                this.handleGive(sender, args);
                break;
            }
            case "giveitem": {
                this.handleGiveItem(sender, args);
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
            case "autosmelt": {
                this.handleAutoSmelt(sender, args);
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

    private void handleGiveItem(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blissgems.admin")) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission", new Object[0]));
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
            return;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found", new Object[0]));
            return;
        }
        String itemId = args[2].toLowerCase();

        // Get amount (default to 1)
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

        // Validate and create item
        ItemStack item = CustomItemManager.getItemById(itemId);
        if (item == null) {
            sender.sendMessage("\u00a7cInvalid item ID: " + itemId);
            sender.sendMessage("\u00a77Available items: energy_bottle, repair_kit, gem_trader, gem_fragment, gem_upgrader");
            return;
        }

        // Set amount and give to player
        item.setAmount(amount);
        target.getInventory().addItem(new ItemStack[]{item});

        // Success message
        String itemName = item.getItemMeta() != null ? item.getItemMeta().getDisplayName() : itemId;
        sender.sendMessage("\u00a7aGave " + amount + "x " + itemName + " \u00a7ato " + target.getName() + "!");
        target.sendMessage("\u00a7aYou received " + amount + "x " + itemName + "\u00a7a!");
    }

    private void handleEnergy(CommandSender sender, String[] args) {
        // If no arguments, show own energy (any player can use)
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("\u00a7cOnly players can check their energy!");
                return;
            }
            Player player = (Player) sender;
            int energy = this.plugin.getEnergyManager().getEnergy(player);
            EnergyState state = EnergyState.fromEnergy(energy);
            String energyBar = this.getEnergyBar(energy);

            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("energy-info-header"));
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("energy-info-line1",
                "energyBar", energyBar, "energy", energy));
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("energy-info-line2",
                "state", state.getDisplayName()));
            return;
        }

        // Admin-only functionality for modifying other players' energy
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
        for (int i = 1; i <= 10; i++) {
            if (i <= energy) {
                bar.append("\u00a7a\u2588"); // Green filled block
            } else {
                bar.append("\u00a77\u2588"); // Gray filled block
            }
        }
        return bar.toString();
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
        ItemStack bottle = CustomItemManager.getItemById((String)"energy_bottle");
        if (bottle != null) {
            player.getInventory().addItem(new ItemStack[]{bottle});
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("energy-withdrawn", new Object[0]));
        } else {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("energy-bottle-failed"));
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

    private void handlePockets(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }
        Player player = (Player)sender;

        // Check if player has a Wealth gem
        GemType gemType = this.plugin.getGemManager().getGemType(player);
        if (gemType != GemType.WEALTH) {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("requires-wealth-gem-pockets"));
            return;
        }

        // Check tier
        int tier = this.plugin.getGemManager().getGemTier(player);
        if (tier < 2) {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("requires-wealth-t2-pockets"));
            return;
        }

        // Open pockets inventory
        this.plugin.getWealthAbilities().pockets(player);
    }

    private void handleAmplify(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }
        Player player = (Player)sender;

        // Check if player has a Wealth gem
        GemType gemType = this.plugin.getGemManager().getGemType(player);
        if (gemType != GemType.WEALTH) {
            player.sendMessage("\u00a7cYou need a Wealth gem to use Amplification!");
            return;
        }

        // Check tier
        int tier = this.plugin.getGemManager().getGemTier(player);
        if (tier < 2) {
            player.sendMessage("\u00a7cAmplification requires Tier 2 Wealth gem!");
            return;
        }

        // Use amplification ability
        this.plugin.getWealthAbilities().amplification(player);
    }

    private void handleToggleClick(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }
        Player player = (Player)sender;

        boolean newState = this.plugin.getClickActivationManager().toggleClickActivation(player);

        if (newState) {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("click-activation-enabled"));
        } else {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("click-activation-disabled"));
        }
    }

    private void handleAbilityMain(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }
        Player player = (Player)sender;

        // Check if player has a gem in hand
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        String oraxenId = CustomItemManager.getIdByItem(mainHand);
        if (oraxenId == null || !GemType.isGem(oraxenId)) {
            oraxenId = CustomItemManager.getIdByItem(offHand);
            if (oraxenId == null || !GemType.isGem(oraxenId)) {
                player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("must-hold-gem"));
                return;
            }
        }

        // Check energy
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        if (energy <= 0) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }

        GemType gemType = GemType.fromOraxenId(oraxenId);
        if (gemType == null) {
            return;
        }

        int tier = oraxenId.endsWith("_gem_t2") ? 2 : 1;

        // Trigger primary ability (non-sneaking behavior)
        switch (gemType) {
            case ASTRA:
                this.plugin.getAstraAbilities().astralDaggers(player);
                break;
            case FIRE:
                this.plugin.getFireAbilities().chargedFireball(player);
                break;
            case FLUX:
                this.plugin.getFluxAbilities().ground(player);
                break;
            case LIFE:
                this.plugin.getLifeAbilities().heartDrainer(player);
                break;
            case PUFF:
                this.plugin.getPuffAbilities().dash(player);
                break;
            case SPEED:
                this.plugin.getSpeedAbilities().onRightClick(player, tier);
                break;
            case STRENGTH:
                this.plugin.getStrengthAbilities().bloodthorns(player);
                break;
            case WEALTH:
                this.plugin.getWealthAbilities().durabilityChip(player);
                break;
        }
    }

    private void handleAbilitySecondary(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }
        Player player = (Player)sender;

        // Check if player has a gem in hand
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        String oraxenId = CustomItemManager.getIdByItem(mainHand);
        if (oraxenId == null || !GemType.isGem(oraxenId)) {
            oraxenId = CustomItemManager.getIdByItem(offHand);
            if (oraxenId == null || !GemType.isGem(oraxenId)) {
                player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("must-hold-gem"));
                return;
            }
        }

        int tier = oraxenId.endsWith("_gem_t2") ? 2 : 1;

        // Check tier requirement for secondary abilities
        if (tier < 2) {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("requires-tier2"));
            return;
        }

        // Check energy
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        if (energy <= 0) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }

        GemType gemType = GemType.fromOraxenId(oraxenId);
        if (gemType == null) {
            return;
        }

        // Trigger secondary ability (sneaking behavior for Tier 2)
        switch (gemType) {
            case ASTRA:
                this.plugin.getAstraAbilities().astralProjection(player);
                break;
            case FIRE:
                this.plugin.getFireAbilities().cozyCampfire(player);
                break;
            case FLUX:
                this.plugin.getFluxAbilities().ground(player);
                break;
            case LIFE:
                this.plugin.getLifeAbilities().circleOfLife(player);
                break;
            case PUFF:
                this.plugin.getPuffAbilities().breezyBash(player);
                break;
            case SPEED:
                this.plugin.getSpeedAbilities().speedStorm(player);
                break;
            case STRENGTH:
                this.plugin.getStrengthAbilities().chadStrength(player);
                break;
            case WEALTH:
                this.plugin.getWealthAbilities().richRush(player);
                break;
        }
    }

    private void handleTrust(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("\u00a7cUsage: /bliss trust <player>");
            return;
        }

        Player player = (Player)sender;
        Player target = Bukkit.getPlayer(args[1]);

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
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("\u00a7cUsage: /bliss untrust <player>");
            return;
        }

        Player player = (Player)sender;
        Player target = Bukkit.getPlayer(args[1]);

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
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }

        Player player = (Player)sender;
        java.util.Set<UUID> trusted = this.plugin.getTrustedPlayersManager().getTrustedPlayers(player);

        if (trusted.isEmpty()) {
            player.sendMessage("\u00a77You have no trusted players. Use \u00a7b/bliss trust <player>\u00a77 to add someone.");
            return;
        }

        player.sendMessage("\u00a75\u00a7lTrusted Players (" + trusted.size() + "):");
        for (UUID uuid : trusted) {
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
            String status = offlinePlayer.isOnline() ? "\u00a7a[Online]" : "\u00a77[Offline]";
            player.sendMessage("\u00a78 - \u00a7b" + name + " " + status);
        }
    }

    private void handleBannable(CommandSender sender, String[] args) {
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
        boolean enable;

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
            String msg = this.plugin.getConfigManager().getFormattedMessage("ban-enabled");
            if (msg != null && !msg.isEmpty()) {
                sender.sendMessage(msg);
            } else {
                sender.sendMessage("\u00a7aBan-on-zero-energy has been enabled!");
            }
        } else {
            String msg = this.plugin.getConfigManager().getFormattedMessage("ban-disabled");
            if (msg != null && !msg.isEmpty()) {
                sender.sendMessage(msg);
            } else {
                sender.sendMessage("\u00a7cBan-on-zero-energy has been disabled!");
            }
        }
    }

    private void handleAutoSmelt(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("blissgems.autosmelt")) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission", new Object[0]));
            return;
        }

        // Check if player has Wealth gem Tier 2
        GemType playerGem = this.plugin.getGemManager().getGemType(player);
        int tier = this.plugin.getGemManager().getGemTier(player);

        if (playerGem != GemType.WEALTH || tier < 2) {
            player.sendMessage("§c§lYou need Wealth Gem Tier 2 to use auto-smelt!");
            return;
        }

        // Toggle auto-smelt for this player
        boolean currentState = this.plugin.getWealthAbilities().isAutoSmeltEnabled(player);
        this.plugin.getWealthAbilities().setAutoSmelt(player, !currentState);

        if (!currentState) {
            player.sendMessage("§a§lAuto-Smelt enabled! Ores will now be automatically smelted when mined.");
        } else {
            player.sendMessage("§c§lAuto-Smelt disabled!");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("\u00a75\u00a7lBlissGems Commands:");
        sender.sendMessage("\u00a77/bliss give <player> <gem_type> [tier] \u00a78- Give a gem");
        sender.sendMessage("\u00a77/bliss giveitem <player> <item_id> [amount] \u00a78- Give special items");
        sender.sendMessage("\u00a77/bliss energy <player> <set/add/remove> <amount> \u00a78- Manage energy");
        sender.sendMessage("\u00a77/bliss withdraw \u00a78- Extract energy into bottle");
        sender.sendMessage("\u00a77/bliss info \u00a78- Show your gem info");
        sender.sendMessage("\u00a77/bliss pockets \u00a78- Open personal inventory (Wealth T2)");
        sender.sendMessage("\u00a77/bliss amplify \u00a78- Amplify potion effects (Wealth T2)");
        sender.sendMessage("\u00a77/bliss autosmelt \u00a78- Toggle auto-smelting (Wealth T2)");
        sender.sendMessage("\u00a77/bliss toggle_click \u00a78- Toggle click activation on/off");
        sender.sendMessage("\u00a77/bliss ability:main \u00a78- Trigger primary ability");
        sender.sendMessage("\u00a77/bliss ability:secondary \u00a78- Trigger secondary ability (T2)");
        sender.sendMessage("\u00a77/bliss trust <player> \u00a78- Trust player (prevent friendly fire)");
        sender.sendMessage("\u00a77/bliss untrust <player> \u00a78- Untrust player");
        sender.sendMessage("\u00a77/bliss trusted \u00a78- List trusted players");
        sender.sendMessage("\u00a77/bliss bannable <true/false> \u00a78- Toggle ban on 0 energy (Admin)");
        sender.sendMessage("\u00a77/bliss reload \u00a78- Reload config");
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> completions = new ArrayList<String>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("give", "giveitem", "energy", "withdraw", "info", "pockets", "amplify", "autosmelt", "reload", "toggle_click", "ability:main", "ability:secondary", "trust", "untrust", "trusted", "bannable"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("giveitem") || args[0].equalsIgnoreCase("energy") || args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("bannable")) {
                return Arrays.asList("true", "false");
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                return Arrays.stream(GemType.values()).map(GemType::getId).collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("giveitem")) {
                // Provide tab completion for special items
                List<String> items = new ArrayList<>();
                items.add("energy_bottle");
                items.add("repair_kit");
                items.add("gem_trader");
                items.add("gem_fragment");
                items.add("gem_upgrader"); // Universal upgrader
                return items;
            }
            if (args[0].equalsIgnoreCase("energy")) {
                return Arrays.asList("set", "add", "remove");
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("give")) {
                return Arrays.asList("1", "2");
            }
            if (args[0].equalsIgnoreCase("giveitem")) {
                return Arrays.asList("1", "8", "16", "32", "64");
            }
        }
        return completions.stream().filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
    }
}

