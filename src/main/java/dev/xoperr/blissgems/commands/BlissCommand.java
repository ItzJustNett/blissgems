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
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.api.GemRegistry;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.EnergyState;
import dev.xoperr.blissgems.utils.GemType;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.commands.StatsCommand;
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
import org.bukkit.attribute.AttributeInstance;
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
                this.plugin.getEnhancedGuiManager().openMainMenu((Player) sender);
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
            case "ability": {
                this.handleAbilityBindingsList(sender, args);
                break;
            }
            case "set_ability": {
                this.handleSetAbility(sender, args);
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

        // Resolve gem: try built-in GemType first, then addon gems via registry
        String gemIdArg = args[2].toLowerCase();
        GemType gemType = null;
        String resolvedGemId = null;
        String resolvedDisplayName = null;

        for (GemType type : GemType.values()) {
            if (type.getId().equalsIgnoreCase(gemIdArg) || type.getDisplayName().equalsIgnoreCase(gemIdArg)) {
                gemType = type;
                resolvedGemId = type.getId();
                resolvedDisplayName = type.getDisplayName();
                break;
            }
        }

        // If not a built-in gem, check addon gems via registry
        if (gemType == null) {
            GemRegistry registry = this.plugin.getGemRegistry();
            if (registry != null) {
                dev.xoperr.blissgems.api.GemDefinition def = registry.getGem(gemIdArg);
                if (def != null) {
                    resolvedGemId = def.getId();
                    resolvedDisplayName = def.getDisplayName();
                }
            }
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

        // Remove old gem(s) from inventory first (built-in AND addon)
        for (int i = 0; i < target.getInventory().getSize(); i++) {
            ItemStack item = target.getInventory().getItem(i);
            if (item != null) {
                String itemId = CustomItemManager.getIdByItem(item);
                if (this.plugin.getGemManager().isAnyGem(itemId)) {
                    target.getInventory().setItem(i, null);
                }
            }
        }

        if (this.plugin.getGemManager().giveGem(target, resolvedGemId, tier)) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("gem-given", "player", target.getName(), "gem", resolvedDisplayName, "tier", tier));
        } else {
            sender.sendMessage("\u00a7cFailed to give gem!");
        }
    }

    private void handleReroll(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blissgems.admin")) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission", new Object[0]));
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

        // Get list of enabled gems from config
        List<GemType> enabledGems = new ArrayList<>();
        for (GemType type : GemType.values()) {
            if (this.plugin.getConfig().getBoolean("gems.enabled." + type.getId(), true)) {
                enabledGems.add(type);
            }
        }

        if (enabledGems.isEmpty()) {
            sender.sendMessage("\u00a7cNo gems are enabled in the config!");
            return;
        }

        // Select random gem
        GemType randomGem = enabledGems.get(new java.util.Random().nextInt(enabledGems.size()));

        // Get tier (default to 1)
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

        // Remove old gem(s) from inventory first (built-in AND addon)
        for (int i = 0; i < target.getInventory().getSize(); i++) {
            ItemStack item = target.getInventory().getItem(i);
            if (item != null) {
                String itemId = CustomItemManager.getIdByItem(item);
                if (this.plugin.getGemManager().isAnyGem(itemId)) {
                    target.getInventory().setItem(i, null);
                }
            }
        }
        // Explicitly clear an offhand gem too — the loop's slot range doesn't reliably
        // cover the offhand, and gems normally live there, so a stale gem left in the
        // offhand would keep driving abilities (e.g. F) after the reroll.
        ItemStack offGem = target.getInventory().getItemInOffHand();
        if (offGem != null && this.plugin.getGemManager().isAnyGem(CustomItemManager.getIdByItem(offGem))) {
            target.getInventory().setItemInOffHand(null);
        }

        // Notify command sender that ritual is starting
        sender.sendMessage("\u00a7d\u00a7lInitiating gem ritual for " + target.getName() + "...");
        target.sendMessage("\u00a7d\u00a7l\u00a7nGEM REROLL RITUAL");
        target.sendMessage("\u00a77\u00a7oThe ancient powers are choosing your fate...");

        // Start the ritual animation
        final int finalTier = tier;
        this.plugin.getGemRitualManager().performGemRitual(target, randomGem, false, finalTier);

        // Give the gem after a short delay (let ritual build up).
        // Place it directly in the offhand so gem resolution (F, passives) picks up the
        // new gem, not a stale one left elsewhere.
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            if (this.plugin.getGemManager().giveGemToOffhand(target, randomGem, finalTier)) {
                String msg = this.plugin.getConfigManager().getFormattedMessage("gem-rerolled", "player", target.getName(), "gem", randomGem.getDisplayName(), "tier", finalTier);
                if (msg != null && !msg.isEmpty()) {
                    sender.sendMessage(msg);
                } else {
                    sender.sendMessage("\u00a7aRerolled " + target.getName() + "'s gem to " + randomGem.getColor() + randomGem.getDisplayName() + " \u00a7a(Tier " + finalTier + ")!");
                }

                // Notify the target player
                String targetMsg = this.plugin.getConfigManager().getFormattedMessage("gem-rerolled-received", "gem", randomGem.getDisplayName(), "tier", finalTier);
                if (targetMsg != null && !targetMsg.isEmpty()) {
                    target.sendMessage(targetMsg);
                } else {
                    target.sendMessage("\u00a7d\u00a7l\u00bb \u00a7fYour gem has been chosen: " + randomGem.getColor() + "\u00a7l" + randomGem.getDisplayName() + " \u00a7f(Tier " + finalTier + ")\u00a7d\u00a7l \u00ab");
                }
            } else {
                sender.sendMessage("\u00a7cFailed to reroll gem!");
            }
        }, 20L); // 1 second delay
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

    /** Consumables that are non-droppable (anti-dupe) and so can only change hands via /bliss transfer. */
    private static final java.util.Set<String> TRANSFERABLE_ITEMS =
        java.util.Set.of("gem_trader", "gem_upgrader", "energy_bottle", "repair_kit");

    /**
     * /bliss transfer <player> <item> [amount]
     * Player-to-player handoff for the non-droppable BlissGems consumables \u2014 replaces the
     * drop-to-trade path we removed to close the drop-and-swap dupe.
     */
    private void handleTransfer(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can transfer items!");
            return;
        }
        Player player = (Player) sender;
        if (args.length < 3) {
            player.sendMessage("\u00a7cUsage: /bliss transfer <player> <item> [amount]");
            player.sendMessage("\u00a77Transferable: gem_trader, gem_upgrader, energy_bottle, repair_kit");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
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
            } catch (NumberFormatException e) {
                player.sendMessage("\u00a7cInvalid amount!");
                return;
            }
            if (amount < 1 || amount > 64) {
                player.sendMessage("\u00a7cAmount must be between 1 and 64!");
                return;
            }
        }

        // Confirm the sender actually has enough
        int have = countCustomItem(player, itemId);
        if (have < amount) {
            player.sendMessage("\u00a7cYou only have \u00a7f" + have + "\u00a7c of that item.");
            return;
        }

        // Remove from sender, then deliver to target; refund any that didn't fit.
        removeCustomItem(player, itemId, amount);
        ItemStack give = CustomItemManager.getItemById(itemId);
        if (give == null) {
            player.sendMessage("\u00a7cInvalid item.");
            return;
        }
        give.setAmount(amount);
        java.util.Map<Integer, ItemStack> leftover = target.getInventory().addItem(give);
        int notDelivered = 0;
        for (ItemStack left : leftover.values()) {
            if (left != null) notDelivered += left.getAmount();
        }
        if (notDelivered > 0) {
            // Refund the undelivered portion to the sender (a slot just freed up).
            ItemStack refund = CustomItemManager.getItemById(itemId);
            refund.setAmount(notDelivered);
            player.getInventory().addItem(refund);
        }
        int delivered = amount - notDelivered;
        if (delivered <= 0) {
            player.sendMessage("\u00a7c" + target.getName() + "'s inventory is full \u2014 nothing was transferred.");
            return;
        }

        String itemName = give.getItemMeta() != null ? give.getItemMeta().getDisplayName() : itemId;
        player.sendMessage("\u00a7aTransferred \u00a7f" + delivered + "x " + itemName + " \u00a7ato " + target.getName() + "!");
        target.sendMessage("\u00a7a" + player.getName() + " \u00a7atransferred you \u00a7f" + delivered + "x " + itemName + "\u00a7a!");
        if (notDelivered > 0) {
            player.sendMessage("\u00a7e" + notDelivered + " couldn't fit and were returned to you.");
        }
    }

    /** Count how many of a custom item (by id) the player holds across their whole inventory. */
    private int countCustomItem(Player player, String itemId) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (itemId.equals(CustomItemManager.getIdByItem(item))) {
                total += item.getAmount();
            }
        }
        return total;
    }

    /** Remove exactly {@code amount} of a custom item (by id) from the player's inventory. */
    private void removeCustomItem(Player player, String itemId, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;
            if (!itemId.equals(CustomItemManager.getIdByItem(item))) continue;
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

            this.plugin.getConfigManager().sendFormattedMessage(player, "energy-info-header");
            this.plugin.getConfigManager().sendFormattedMessage(player, "energy-info-line1",
                "energyBar", energyBar, "energy", energy);
            this.plugin.getConfigManager().sendFormattedMessage(player, "energy-info-line2",
                "state", state.getDisplayName());
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
            this.plugin.getConfigManager().sendFormattedMessage(player, "not-enough-energy");
            return;
        }
        this.plugin.getEnergyManager().removeEnergy(player, 1);
        ItemStack bottle = CustomItemManager.getItemById((String)"energy_bottle");
        if (bottle != null) {
            player.getInventory().addItem(new ItemStack[]{bottle});
            this.plugin.getConfigManager().sendFormattedMessage(player, "energy-withdrawn");
        } else {
            this.plugin.getConfigManager().sendFormattedMessage(player, "energy-bottle-failed");
        }
    }

    private void handleWhoOwns(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        if (!sender.hasPermission("blissgems.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return;
        }
        org.bukkit.inventory.ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() == org.bukkit.Material.AIR) {
            player.sendMessage("§cHold an item in your main hand.");
            return;
        }
        java.util.UUID owner = CustomItemManager.getOwner(held);
        if (owner == null) {
            player.sendMessage("§eThat item has no ownership stamp.");
            return;
        }
        org.bukkit.OfflinePlayer op = this.plugin.getServer().getOfflinePlayer(owner);
        String name = op.getName() != null ? op.getName() : "unknown";
        player.sendMessage("§d§lOwner: §f" + name + " §7(" + owner + ")");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }
        Player player = (Player)sender;
        if (!this.plugin.getGemManager().hasActiveGem(player)) {
            this.plugin.getConfigManager().sendFormattedMessage(player, "no-active-gem");
            return;
        }
        GemType gemType = this.plugin.getGemManager().getGemType(player);
        int tier = this.plugin.getGemManager().getGemTier(player);
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        EnergyState state = this.plugin.getEnergyManager().getEnergyState(player);
        this.plugin.getConfigManager().sendFormattedMessage(player, "gem-info", "gem", gemType.getDisplayName(), "tier", tier, "energy", energy, "state", state.getDisplayName());
    }

    private void handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blissgems.admin")) {
            this.plugin.getConfigManager().sendFormattedMessage(sender, "no-permission");
            return;
        }
        this.plugin.getConfigManager().reload();
        this.plugin.getConfigManager().sendFormattedMessage(sender, "config-reloaded");
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
            this.plugin.getConfigManager().sendFormattedMessage(player, "requires-wealth-gem-pockets");
            return;
        }

        // Check tier
        int tier = this.plugin.getGemManager().getGemTier(player);
        if (tier < 2) {
            this.plugin.getConfigManager().sendFormattedMessage(player, "requires-wealth-t2-pockets");
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
            this.plugin.getConfigManager().sendFormattedMessage(player, "click-activation-enabled");
        } else {
            this.plugin.getConfigManager().sendFormattedMessage(player, "click-activation-disabled");
        }
    }

    /**
     * Find the gem item ID from the player's main or offhand.
     * Returns null if no gem is found in either hand.
     */
    private String findGemInHand(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        GemRegistry registry = this.plugin.getGemRegistry();

        String oraxenId = CustomItemManager.getIdByItem(mainHand);
        if (oraxenId != null && (GemType.isGem(oraxenId) ||
                (registry != null && registry.isRegisteredGem(oraxenId)))) {
            return oraxenId;
        }
        oraxenId = CustomItemManager.getIdByItem(offHand);
        if (oraxenId != null && (GemType.isGem(oraxenId) ||
                (registry != null && registry.isRegisteredGem(oraxenId)))) {
            return oraxenId;
        }
        return null;
    }

    /**
     * True (and messages the player) if their gem is currently locked by Auratus's Gem Lock.
     * Used to block every ability activation path while locked.
     */
    private boolean blockedByGemLock(Player player) {
        dev.xoperr.blissgems.managers.GemLockManager mgr = this.plugin.getGemLockManager();
        if (mgr != null && mgr.isLocked(player)) {
            int left = mgr.getRemainingSeconds(player.getUniqueId());
            player.sendMessage("\u00a76" + dev.xoperr.blissgems.managers.GemLockManager.LOCK_GLYPH
                + " \u00a7c\u00a7oYour gem is locked! \u00a77(" + left + "s)");
            return true;
        }
        return false;
    }

    private void handleAbilityMain(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }
        Player player = (Player)sender;
        if (blockedByGemLock(player)) return;

        String oraxenId = findGemInHand(player);
        if (oraxenId == null) {
            this.plugin.getConfigManager().sendFormattedMessage(player, "must-hold-gem");
            return;
        }

        // Check energy
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        if (energy <= 0) {
            this.plugin.getConfigManager().sendFormattedMessage(player, "no-energy");
            return;
        }

        GemRegistry registry = this.plugin.getGemRegistry();
        int tier = registry != null ? registry.tierFromItemId(oraxenId) : (oraxenId.endsWith("_gem_t2") ? 2 : 1);

        // Try built-in gem first
        GemType gemType = GemType.fromOraxenId(oraxenId);
        if (gemType != null) {
            switch (gemType) {
                case ASTRA: this.plugin.getAstraAbilities().astralDaggers(player); break;
                case FIRE: this.plugin.getFireAbilities().chargedFireball(player); break;
                case FLUX: this.plugin.getFluxAbilities().fluxBeam(player); break;
                case LIFE: this.plugin.getLifeAbilities().heartDrainer(player); break;
                case PUFF: this.plugin.getPuffAbilities().dash(player); break;
                case SPEED: this.plugin.getSpeedAbilities().onRightClick(player, tier); break;
                case STRENGTH: this.plugin.getStrengthAbilities().chadStrength(player); break;
                case WEALTH: this.plugin.getWealthAbilities().unfortunate(player); break;
            }
            return;
        }

        // Addon gem - route through registry
        if (registry != null) {
            String gemId = registry.gemIdFromItemId(oraxenId);
            GemAbilityHandler handler = gemId != null ? registry.getAbilityHandler(gemId) : null;
            if (handler != null) {
                handler.onPrimary(player, tier);
            }
        }
    }

    private void handleAbilitySecondary(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }
        Player player = (Player)sender;
        if (blockedByGemLock(player)) return;

        String oraxenId = findGemInHand(player);
        if (oraxenId == null) {
            this.plugin.getConfigManager().sendFormattedMessage(player, "must-hold-gem");
            return;
        }

        GemRegistry registry = this.plugin.getGemRegistry();
        int tier = registry != null ? registry.tierFromItemId(oraxenId) : (oraxenId.endsWith("_gem_t2") ? 2 : 1);

        if (tier < 2) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("requires-tier2");
            if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
            return;
        }

        int energy = this.plugin.getEnergyManager().getEnergy(player);
        if (energy <= 0) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy", new Object[0]);
            if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
            return;
        }

        GemType gemType = GemType.fromOraxenId(oraxenId);
        if (gemType != null) {
            switch (gemType) {
                case ASTRA: this.plugin.getAstraAbilities().astralProjection(player); break;
                case FIRE: this.plugin.getFireAbilities().cozyCampfire(player); break;
                case FLUX: this.plugin.getFluxAbilities().ground(player); break;
                case LIFE: this.plugin.getLifeAbilities().circleOfLife(player); break;
                case PUFF: this.plugin.getPuffAbilities().breezyBash(player); break;
                case SPEED: this.plugin.getSpeedAbilities().speedStorm(player); break;
                case STRENGTH: this.plugin.getStrengthAbilities().frailer(player); break;
                case WEALTH: this.plugin.getWealthAbilities().richRush(player); break;
            }
            return;
        }

        // Addon gem
        if (registry != null) {
            String gemId = registry.gemIdFromItemId(oraxenId);
            GemAbilityHandler handler = gemId != null ? registry.getAbilityHandler(gemId) : null;
            if (handler != null) handler.onSecondary(player, tier);
        }
    }

    public void triggerPrimary(Player player) {
        handleAbilityMain(player, new String[0]);
    }

    public void triggerSecondary(Player player) {
        handleAbilitySecondary(player, new String[0]);
    }

    public void triggerTertiary(Player player) {
        handleAbilityTertiary(player, new String[0]);
    }

    public void triggerQuaternary(Player player) {
        handleAbilityQuaternary(player, new String[0]);
    }

    /**
     * Dispatch an ability based on the configured slot. Returns false if slot is null
     * (caller should treat that as "input is unbound").
     */
    public boolean triggerSlot(Player player, dev.xoperr.blissgems.utils.AbilitySlot slot) {
        if (slot == null) return false;
        switch (slot) {
            case PRIMARY: triggerPrimary(player); return true;
            case SECONDARY: triggerSecondary(player); return true;
            case TERTIARY: triggerTertiary(player); return true;
            case QUATERNARY: triggerQuaternary(player); return true;
        }
        return false;
    }

    private void handleAbilityTertiary(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }
        Player player = (Player) sender;
        if (blockedByGemLock(player)) return;

        String oraxenId = findGemInHand(player);
        if (oraxenId == null) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("must-hold-gem");
            if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
            return;
        }

        GemRegistry registry = this.plugin.getGemRegistry();
        int tier = registry != null ? registry.tierFromItemId(oraxenId) : (oraxenId.endsWith("_gem_t2") ? 2 : 1);
        if (tier < 2) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("requires-tier2");
            if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
            return;
        }

        int energy = this.plugin.getEnergyManager().getEnergy(player);
        if (energy <= 0) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy", new Object[0]);
            if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
            return;
        }

        GemType gemType = GemType.fromOraxenId(oraxenId);
        if (gemType != null) {
            switch (gemType) {
                case FIRE: this.plugin.getFireAbilities().crisp(player); break;
                case ASTRA: this.plugin.getAstraAbilities().activateDimensionalDrift(player); break;
                case FLUX: this.plugin.getFluxAbilities().flashbang(player); break;
                case LIFE: this.plugin.getLifeAbilities().vitalityVortex(player); break;
                case PUFF: this.plugin.getPuffAbilities().groupBreezyBash(player); break;
                case STRENGTH: this.plugin.getStrengthAbilities().shadowStalker(player); break;
                case SPEED: this.plugin.getSpeedAbilities().activateTerminalVelocity(player); break;
                case WEALTH: this.plugin.getWealthAbilities().itemLock(player); break;
                default: player.sendMessage("\u00a7c\u00a7oNo tertiary ability for your gem type!"); break;
            }
            return;
        }

        // Addon gem
        if (registry != null) {
            String gemId = registry.gemIdFromItemId(oraxenId);
            // Auratus's tertiary (F) is otherwise unused — hang the Gem Lock ability on it.
            if ("auratus".equals(gemId)) {
                this.plugin.getGemLockManager().castGemLock(player);
                return;
            }
            GemAbilityHandler handler = gemId != null ? registry.getAbilityHandler(gemId) : null;
            if (handler != null) handler.onTertiary(player, tier);
        }
    }

    private void handleAbilityQuaternary(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }
        Player player = (Player) sender;
        if (blockedByGemLock(player)) return;

        String oraxenId = findGemInHand(player);
        if (oraxenId == null) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("must-hold-gem");
            if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
            return;
        }

        GemRegistry registry = this.plugin.getGemRegistry();
        int tier = registry != null ? registry.tierFromItemId(oraxenId) : (oraxenId.endsWith("_gem_t2") ? 2 : 1);
        if (tier < 2) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("requires-tier2");
            if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
            return;
        }

        int energy = this.plugin.getEnergyManager().getEnergy(player);
        if (energy <= 0) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy", new Object[0]);
            if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
            return;
        }

        GemType gemType = GemType.fromOraxenId(oraxenId);
        if (gemType != null) {
            switch (gemType) {
                case FIRE: this.plugin.getFireAbilities().meteorShower(player); break;
                case ASTRA: this.plugin.getAstraAbilities().activateDimensionalVoid(player); break;
                case FLUX: this.plugin.getFluxAbilities().kineticBurst(player); break;
                case LIFE: this.plugin.getLifeAbilities().heartLock(player); break;
                case PUFF: this.plugin.getPuffAbilities().updraft(player); break;
                case STRENGTH: this.plugin.getStrengthAbilities().nullify(player); break;
                case WEALTH: this.plugin.getWealthAbilities().amplification(player); break;
                default: player.sendMessage("\u00a7c\u00a7oNo quaternary ability for your gem type!"); break;
            }
            return;
        }

        // Addon gem
        if (registry != null) {
            String gemId = registry.gemIdFromItemId(oraxenId);
            GemAbilityHandler handler = gemId != null ? registry.getAbilityHandler(gemId) : null;
            if (handler != null) handler.onQuaternary(player, tier);
        }
    }

    private void handleAbilityBindingsList(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        Player player = (Player) sender;
        dev.xoperr.blissgems.managers.AbilityBindingManager mgr = this.plugin.getAbilityBindingManager();
        if (mgr == null) {
            player.sendMessage("§cBinding system unavailable.");
            return;
        }

        java.util.EnumMap<dev.xoperr.blissgems.utils.AbilityBinding, dev.xoperr.blissgems.utils.AbilitySlot> map = mgr.getAll(player);

        player.sendMessage("§d§l⚡ Your Ability Bindings");
        for (dev.xoperr.blissgems.utils.AbilitySlot slot : dev.xoperr.blissgems.utils.AbilitySlot.values()) {
            dev.xoperr.blissgems.utils.AbilityBinding boundInput = null;
            for (java.util.Map.Entry<dev.xoperr.blissgems.utils.AbilityBinding, dev.xoperr.blissgems.utils.AbilitySlot> e : map.entrySet()) {
                if (e.getValue() == slot) { boundInput = e.getKey(); break; }
            }
            String inputLabel = boundInput != null ? "§f" + boundInput.getDisplayName() : "§8unbound";
            player.sendMessage("§7" + slot.getDisplayName() + " §8→ " + inputLabel);
        }

        player.sendMessage("");
        player.sendMessage("§d§l⚡ Available Inputs");
        for (dev.xoperr.blissgems.utils.AbilityBinding b : dev.xoperr.blissgems.utils.AbilityBinding.values()) {
            dev.xoperr.blissgems.utils.AbilitySlot s = map.get(b);
            String suffix = s != null ? " §8(§7" + s.getDisplayName() + "§8)" : "";
            player.sendMessage("§f• §7" + b.getId() + " §8- §f" + b.getDisplayName() + suffix);
        }
        player.sendMessage("");
        player.sendMessage("§7Change with §f/bliss set_ability <slot> <input>");
        player.sendMessage("§7Unbind a slot with §f/bliss set_ability <slot> none");
        player.sendMessage("§7Reset defaults with §f/bliss set_ability reset");
    }

    private void handleSetAbility(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }
        Player player = (Player) sender;
        dev.xoperr.blissgems.managers.AbilityBindingManager mgr = this.plugin.getAbilityBindingManager();
        if (mgr == null) {
            player.sendMessage("§cBinding system unavailable.");
            return;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
            mgr.resetToDefaults(player);
            player.sendMessage("§aAbility bindings reset to defaults.");
            return;
        }

        if (args.length < 3) {
            player.sendMessage("§cUsage: §f/bliss set_ability <slot> <input>");
            player.sendMessage("§7Slots: §fprimary, secondary, tertiary, quaternary");
            player.sendMessage("§7Inputs: §fright_click, shift_right_click, left_click, shift_left_click, swap_hand, shift_swap_hand, none");
            player.sendMessage("§7See current bindings with §f/bliss ability");
            return;
        }

        dev.xoperr.blissgems.utils.AbilitySlot slot = dev.xoperr.blissgems.utils.AbilitySlot.fromId(args[1]);
        if (slot == null) {
            player.sendMessage("§cUnknown slot: §f" + args[1]
                + "§c. Valid: primary, secondary, tertiary, quaternary.");
            return;
        }

        String inputId = args[2];
        if (inputId.equalsIgnoreCase("none") || inputId.equalsIgnoreCase("unbind")) {
            java.util.EnumMap<dev.xoperr.blissgems.utils.AbilityBinding, dev.xoperr.blissgems.utils.AbilitySlot> map = mgr.getAll(player);
            for (java.util.Map.Entry<dev.xoperr.blissgems.utils.AbilityBinding, dev.xoperr.blissgems.utils.AbilitySlot> e : map.entrySet()) {
                if (e.getValue() == slot) {
                    mgr.unbind(player, e.getKey());
                }
            }
            player.sendMessage("§aUnbound §l" + slot.getDisplayName() + "§a.");
            return;
        }

        dev.xoperr.blissgems.utils.AbilityBinding input = dev.xoperr.blissgems.utils.AbilityBinding.fromId(inputId);
        if (input == null) {
            player.sendMessage("§cUnknown input: §f" + inputId
                + "§c. See §f/bliss ability§c for the list.");
            return;
        }

        mgr.setBinding(player, input, slot);
        player.sendMessage("§aBound §f" + input.getDisplayName()
            + "§a → §l" + slot.getDisplayName() + "§a.");
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

    private void handleOraxen(CommandSender sender, String[] args) {
        if (!sender.hasPermission("blissgems.admin")) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-permission", new Object[0]));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /bliss oraxen <true/false>");
            sender.sendMessage("§7Auto-replace legacy gems with Oraxen items on join: "
                + (dev.xoperr.blissgems.utils.OraxenGemFixer.isFixOnJoinEnabled(this.plugin) ? "§aEnabled" : "§cDisabled"));
            return;
        }

        String value = args[1].toLowerCase();
        boolean enable;

        if (value.equals("true") || value.equals("on") || value.equals("yes") || value.equals("1")) {
            enable = true;
        } else if (value.equals("false") || value.equals("off") || value.equals("no") || value.equals("0")) {
            enable = false;
        } else {
            sender.sendMessage("§cInvalid value! Use true or false.");
            return;
        }

        dev.xoperr.blissgems.utils.OraxenGemFixer.setFixOnJoinEnabled(this.plugin, enable);
        if (enable) {
            sender.sendMessage("§aLegacy gems will now be replaced with Oraxen items when players join!");
        } else {
            sender.sendMessage("§cAutomatic gem replacement on join has been disabled!");
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

    private void handleStats(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }

        Player player = (Player) sender;
        StatsCommand statsCommand = new StatsCommand(this.plugin);
        statsCommand.execute(player, args.length > 1 ? java.util.Arrays.copyOfRange(args, 1, args.length) : new String[]{});
    }

    private void handleReleaseSouls(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }
        Player player = (Player) sender;

        // Check if player has Astra gem
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
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }
        Player player = (Player) sender;

        // Check if player has Astra gem
        if (!this.plugin.getGemManager().hasGemType(player, GemType.ASTRA)) {
            player.sendMessage("\u00a7c\u00a7oOnly Astra gem holders can view captured souls!");
            return;
        }

        var souls = this.plugin.getSoulManager().getCapturedSouls(player);
        int count = souls.size();
        int max = 2;

        player.sendMessage("\u00a7d\u00a7lCaptured Souls (" + count + "/" + max + "):");
        if (count == 0) {
            player.sendMessage("\u00a77  No souls captured. Sneak + hit a mob to capture it.");
        } else {
            for (int i = 0; i < souls.size(); i++) {
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
            // Reset attack cooldown by briefly maximising attack speed
            AttributeInstance attackSpeed = online.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED);
            if (attackSpeed != null) {
                double original = attackSpeed.getBaseValue();
                attackSpeed.setBaseValue(1024);
                attackSpeed.setBaseValue(original);
            }
            count++;
        }

        sender.sendMessage("§a§lAttack cooldown reset for " + count + " online player(s)!");

        // Broadcast to all players
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage("§e§oAttack cooldowns have been normalized by an admin.");
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
            this.plugin.getConfigManager().sendFormattedMessage(sender, "smp-already-started");
            return;
        }

        // Set SMP as started
        this.plugin.getConfigManager().setSmpStarted(true);

        // Notify sender
        this.plugin.getConfigManager().sendFormattedMessage(sender, "smp-started");

        // Give gems to all online players who haven't received one yet
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!hasReceivedFirstGem(online)) {
                GemType randomGem = getRandomEnabledGem();
                if (randomGem != null) {
                    final GemType finalGem = randomGem;
                    final Player target = online;

                    // Welcome messages
                    target.sendMessage("");
                    target.sendMessage("\u00a7d\u00a7l\u00a7m                                                  ");
                    target.sendMessage("\u00a7d\u00a7lWELCOME TO BLISSGEMS!");
                    target.sendMessage("");
                    target.sendMessage("\u00a77\u00a7oThe ancient gem ritual begins...");
                    target.sendMessage("\u00a77\u00a7oYour destiny is being forged...");
                    target.sendMessage("\u00a7d\u00a7l\u00a7m                                                  ");
                    target.sendMessage("");

                    // Start the ritual animation
                    this.plugin.getGemRitualManager().performGemRitual(target, finalGem, true, 1);

                    // Give the gem after a short delay (let ritual build up)
                    this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                        if (target.isOnline() && this.plugin.getGemManager().giveGem(target, finalGem, 1)) {
                            markFirstGemReceived(target);

                            String welcomeMsg = this.plugin.getConfigManager().getFormattedMessage("first-gem-received",
                                "gem", finalGem.getDisplayName());
                            if (welcomeMsg != null && !welcomeMsg.isEmpty()) {
                                target.sendMessage(welcomeMsg);
                            } else {
                                target.sendMessage("");
                                target.sendMessage("\u00a7d\u00a7l\u00bb \u00a7fYour gem has been chosen: " + finalGem.getColor() + "\u00a7l" + finalGem.getDisplayName() + "\u00a7d\u00a7l \u00ab");
                                target.sendMessage("");
                            }

                            this.plugin.getLogger().info("SMP Start: Gave " + target.getName() + " their first gem: " + finalGem.getDisplayName());
                        }
                    }, 20L);
                }
            }
        }
    }

    private boolean hasReceivedFirstGem(Player player) {
        java.io.File dataFolder = new java.io.File(this.plugin.getDataFolder(), "playerdata");
        java.io.File file = new java.io.File(dataFolder, player.getUniqueId() + ".yml");
        if (!file.exists()) {
            return false;
        }
        org.bukkit.configuration.file.FileConfiguration data = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        return data.getBoolean("received-first-gem", false);
    }

    private void markFirstGemReceived(Player player) {
        java.io.File dataFolder = new java.io.File(this.plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        java.io.File file = new java.io.File(dataFolder, player.getUniqueId() + ".yml");
        org.bukkit.configuration.file.FileConfiguration data;
        if (file.exists()) {
            data = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        } else {
            data = new org.bukkit.configuration.file.YamlConfiguration();
        }
        data.set("received-first-gem", true);
        if (!data.contains("energy")) {
            data.set("energy", this.plugin.getConfigManager().getStartingEnergy());
        }
        try {
            data.save(file);
        } catch (java.io.IOException e) {
            this.plugin.getLogger().warning("Failed to save first gem status for " + player.getName() + ": " + e.getMessage());
        }
    }

    private GemType getRandomEnabledGem() {
        java.util.ArrayList<GemType> enabledGems = new java.util.ArrayList<>();
        for (GemType type : GemType.values()) {
            if (this.plugin.getConfigManager().isGemEnabled(type)) {
                enabledGems.add(type);
            }
        }
        if (enabledGems.isEmpty()) {
            return null;
        }
        return enabledGems.get(new java.util.Random().nextInt(enabledGems.size()));
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
                count++;
            }
            sender.sendMessage("\u00a7a\u00a7lCleared all ability cooldowns for " + count + " online player(s)!");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(this.plugin.getConfigManager().getFormattedMessage("player-not-found", new Object[0]));
            return;
        }

        this.plugin.getAbilityManager().clearCooldowns(target);
        sender.sendMessage("\u00a7aCleared all ability cooldowns for \u00a7l" + target.getName() + "\u00a7a!");
    }

    private void handleAchievements(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cOnly players can use this command!");
            return;
        }
        Player player = (Player) sender;

        java.util.Set<Achievement> unlocked = this.plugin.getAchievementManager().getUnlocked(player);
        int total = Achievement.values().length;
        int unlockedCount = unlocked.size();

        player.sendMessage("\u00a76\u00a7l\u2b50 Achievements (" + unlockedCount + "/" + total + ")");
        player.sendMessage("");

        for (Achievement achievement : Achievement.values()) {
            boolean isUnlocked = unlocked.contains(achievement);
            int progress = this.plugin.getAchievementManager().getProgress(player, achievement);
            int target = achievement.getTargetProgress();

            if (isUnlocked) {
                player.sendMessage("\u00a7a\u2714 \u00a7e" + achievement.getDisplayName() + " \u00a77- " + achievement.getDescription());
            } else if (target > 1 && progress > 0) {
                player.sendMessage("\u00a78\u2718 \u00a77" + achievement.getDisplayName() + " \u00a78- " + achievement.getDescription() + " \u00a7e(" + progress + "/" + target + ")");
            } else {
                player.sendMessage("\u00a78\u2718 \u00a77" + achievement.getDisplayName() + " \u00a78- " + achievement.getDescription());
            }
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
        sender.sendMessage("\u00a77/bliss reload \u00a78- Reload config");
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> completions = new ArrayList<String>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("give", "reroll", "giveitem", "transfer", "energy", "withdraw", "info", "pockets", "amplify", "autosmelt", "reload", "toggle_click", "ability:main", "ability:secondary", "ability:tertiary", "ability:quaternary", "trust", "untrust", "trusted", "stats", "achievements", "bannable", "oraxen", "souls", "release", "normalise", "normalize", "smp", "clearcds"));
        } else if (args.length == 2) {
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
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                List<String> gemIds = Arrays.stream(GemType.values()).map(GemType::getId).collect(Collectors.toList());
                GemRegistry registry = this.plugin.getGemRegistry();
                if (registry != null) {
                    for (dev.xoperr.blissgems.api.GemDefinition def : registry.getAllGems()) {
                        if (!gemIds.contains(def.getId())) {
                            gemIds.add(def.getId());
                        }
                    }
                }
                return gemIds;
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
            if (args[0].equalsIgnoreCase("transfer")) {
                return new ArrayList<>(TRANSFERABLE_ITEMS);
            }
            if (args[0].equalsIgnoreCase("energy")) {
                return Arrays.asList("set", "add", "remove");
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
        }
        return completions.stream().filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
    }
}

