package dev.xoperr.blissgems.lore;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.managers.GemManager;
import dev.xoperr.blissgems.utils.GemType;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * A Binding Vow (Nemvevo's): a player holds a gem's Tier 2 powers with no gem at all. The plugin
 * treats them as carrying that gem whenever no real gem is in their hands, so abilities fire
 * from an empty hand with the usual inputs, passives apply, and the action bar shows the gem in
 * the vow's own colour. {@code /bliss vow <player> <gem|off>}.
 * The EMP is a vow of its own ({@code /bliss vow <player> emp}): no gem, just the EMP.
 */
public final class BindingVows {
    private final BlissGems plugin;
    private final File file;
    private static final String EMP_SECTION = "emp-vows";
    private final Map<UUID, GemType> vows = new HashMap<>();
    private final Set<UUID> empVows = new HashSet<>();

    public BindingVows(BlissGems plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "binding_vows.yml");
        YamlConfiguration y = YamlConfiguration.loadConfiguration(this.file);
        for (String raw : y.getStringList(EMP_SECTION)) this.empVows.add(UUID.fromString(raw));
        for (String key : y.getKeys(false)) {
            if (key.equals(EMP_SECTION)) continue;
            GemType type = GemManager.builtInType(y.getString(key));
            if (type != null) this.vows.put(UUID.fromString(key), type);
        }
    }

    /** The gem a player is vowed to, or null. */
    public GemType vow(Player player) {
        return player == null ? null : this.vows.get(player.getUniqueId());
    }

    /** The player holds the EMP Binding Vow. */
    public boolean hasEmpVow(Player player) {
        return player != null && this.empVows.contains(player.getUniqueId());
    }

    /** The item id the vow stands in for (always Tier 2), or null. */
    public String vowItemId(Player player) {
        GemType type = this.vow(player);
        return type == null ? null : GemType.buildOraxenId(type, 2);
    }

    /** Vow powers fire from an empty main hand, so the player's items keep their normal clicks. */
    public boolean firesFrom(Player player, ItemStack mainHand) {
        return this.vow(player) != null && (mainHand == null || mainHand.getType().isAir());
    }

    public void command(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§7/bliss vow <player> <gem|quakitus|emp|off>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return;
        }
        if (args[2].equalsIgnoreCase("quakitus")) {
            this.bindFireGem(sender, target);
            return;
        }
        if (args[2].equalsIgnoreCase("emp")) {
            this.empVows.add(target.getUniqueId());
            this.save();
            sender.sendMessage("§a" + target.getName() + " now holds the EMP.");
            created(target);
            target.sendMessage("§5§l✦ Binding Vow §d- you hold an EMP (/bliss emp, or sneak + F with both hands empty).");
            return;
        }
        if (args[2].equalsIgnoreCase("off")) {
            this.vows.remove(target.getUniqueId());
            this.empVows.remove(target.getUniqueId());
            this.save();
            this.plugin.getGemManager().updateActiveGem(target);
            sender.sendMessage("§7" + target.getName() + "'s Binding Vow is broken.");
            target.sendMessage("§5§oYour Binding Vow is broken.");
            return;
        }
        GemType type = GemManager.builtInType(args[2]);
        if (type == null) {
            sender.sendMessage("§cUnknown gem: " + args[2]);
            return;
        }
        this.vows.put(target.getUniqueId(), type);
        this.save();
        this.plugin.getGemManager().updateActiveGem(target);
        sender.sendMessage("§a" + target.getName() + " is bound to the " + type.getDisplayName() + " gem's powers.");
        created(target);
        target.sendMessage(type.getColor() + "§l✦ Binding Vow §7- the " + type.getDisplayName() + " gem's powers are yours, no gem needed. Use them with an empty hand (F works as a right click in the air).");
    }

    /** Quakitus's vow: the player's gem becomes the Binding Vow Fire Gem (given if they have none). */
    private void bindFireGem(CommandSender sender, Player target) {
        ItemStack vowGem = CustomItemManager.createFireVowGem();
        ItemStack current = this.plugin.getGemManager().findGemInInventory(target);
        PlayerInventory inv = target.getInventory();
        if (current != null && current.equals(inv.getItemInOffHand())) {
            inv.setItemInOffHand(vowGem);
        } else {
            int slot = current == null ? -1 : inv.first(current);
            if (slot >= 0) {
                inv.setItem(slot, vowGem);
            } else if (inv.getItemInOffHand().getType().isAir()) {
                inv.setItemInOffHand(vowGem);
            } else {
                inv.addItem(vowGem).values().forEach(left -> target.getWorld().dropItem(target.getLocation(), left));
            }
        }
        this.plugin.getGemManager().updateActiveGem(target);
        sender.sendMessage("§a" + target.getName() + " now holds the Binding Vow Fire Gem.");
        created(target);
        target.sendMessage("§5§l✦ Binding Vow §d- your Fire Gem keeps only its fireball and Fire Resistance.");
    }

    private static void created(Player target) {
        target.sendTitle("", "§fBINDING VOW §aCREATED", 10, 70, 20);
        target.playSound(target.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 1.2f);
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, GemType> e : this.vows.entrySet()) y.set(e.getKey().toString(), e.getValue().getId());
        y.set(EMP_SECTION, this.empVows.stream().map(UUID::toString).toList());
        try {
            y.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("binding_vows.yml: " + e.getMessage());
        }
    }
}
