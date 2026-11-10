package dev.xoperr.blissgems.lore;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Attributes;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

/**
 * The Assassins: players the Prophet sent to hunt the Honored One. An admin turns a player into
 * one with {@code /bliss assassin <player> <auratus|heretic>}: full enchanted netherite with Curse
 * of Vanishing, 20 hearts, a stocked ender chest and a Tier 2 Auratus or Heretic gem (needs the
 * BlissMythics addon). {@code /bliss assassin <player> off} takes the hearts away again.
 */
public final class Assassins {
    private final BlissGems plugin;
    private final NamespacedKey heartsKey;

    public Assassins(BlissGems plugin) {
        this.plugin = plugin;
        this.heartsKey = new NamespacedKey(plugin, "assassin-hearts");
    }

    public void handle(Player sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§7/bliss assassin <player> <auratus|heretic|off>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found.");
            return;
        }
        String gem = args[2].toLowerCase();
        if (gem.equals("off")) {
            this.setHearts(target, false);
            sender.sendMessage("§7" + target.getName() + " is no longer an Assassin.");
            return;
        }
        if (!gem.equals("auratus") && !gem.equals("heretic")) {
            sender.sendMessage("§cThe gem has to be auratus or heretic.");
            return;
        }
        if (this.plugin.getGemRegistry() == null || this.plugin.getGemRegistry().getGem(gem) == null) {
            sender.sendMessage("§cThe " + gem + " gem isn't registered - install the BlissMythics addon first.");
            return;
        }
        this.equip(target, gem);
        this.stockEnderChest(target);
        this.setHearts(target, true);
        this.plugin.getGemManager().giveGem(target, gem, 2);
        Bukkit.broadcastMessage("§4§l" + target.getName() + " §chas been sent by the Prophet. §7An Assassin walks among you.");
        sender.sendMessage("§a" + target.getName() + " is now an Assassin with the " + gem + " gem.");
    }

    private void equip(Player p, String gem) {
        PlayerInventory inv = p.getInventory();
        // Bliss: Auratus wears gold Flow/Bolt/Eye/Spire trims, Heretic redstone Silence all over
        boolean auratus = gem.equals("auratus");
        TrimMaterial material = auratus ? TrimMaterial.GOLD : TrimMaterial.REDSTONE;
        inv.setHelmet(this.trim(this.piece(Material.NETHERITE_HELMET, Enchantment.PROTECTION, 4), material, auratus ? TrimPattern.FLOW : TrimPattern.SILENCE));
        inv.setChestplate(this.trim(this.piece(Material.NETHERITE_CHESTPLATE, Enchantment.PROTECTION, 4), material, auratus ? TrimPattern.BOLT : TrimPattern.SILENCE));
        inv.setLeggings(this.trim(this.piece(Material.NETHERITE_LEGGINGS, Enchantment.PROTECTION, 4), material, auratus ? TrimPattern.EYE : TrimPattern.SILENCE));
        inv.setBoots(this.trim(this.piece(Material.NETHERITE_BOOTS, Enchantment.PROTECTION, 4), material, auratus ? TrimPattern.SPIRE : TrimPattern.SILENCE));
        inv.addItem(this.piece(Material.NETHERITE_SWORD, Enchantment.SHARPNESS, 5), this.piece(Material.NETHERITE_AXE, Enchantment.SHARPNESS, 5));
    }

    private ItemStack piece(Material material, Enchantment main, int level) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(main, level, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.setLore(List.of("§8Sent by the Prophet."));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack trim(ItemStack armor, TrimMaterial material, TrimPattern pattern) {
        if (armor.getItemMeta() instanceof ArmorMeta meta) {
            meta.setTrim(new ArmorTrim(material, pattern));
            armor.setItemMeta(meta);
        }
        return armor;
    }

    /** A personal stock: gapples, pearls, totems, potions' worth of supplies and blocks. */
    private void stockEnderChest(Player p) {
        Inventory chest = p.getEnderChest();
        chest.addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 8), new ItemStack(Material.GOLDEN_APPLE, 64),
            new ItemStack(Material.ENDER_PEARL, 16), new ItemStack(Material.ENDER_PEARL, 16), new ItemStack(Material.TOTEM_OF_UNDYING),
            new ItemStack(Material.TOTEM_OF_UNDYING), new ItemStack(Material.TOTEM_OF_UNDYING), new ItemStack(Material.WIND_CHARGE, 64),
            new ItemStack(Material.COBWEB, 64), new ItemStack(Material.EXPERIENCE_BOTTLE, 64), new ItemStack(Material.EXPERIENCE_BOTTLE, 64),
            new ItemStack(Material.COOKED_BEEF, 64), new ItemStack(Material.OBSIDIAN, 64), new ItemStack(Material.WATER_BUCKET));
    }

    /** +20 max health (20 hearts total). */
    private void setHearts(Player p, boolean on) {
        AttributeInstance attr = p.getAttribute(Attributes.maxHealth());
        if (attr == null) return;
        for (AttributeModifier m : new java.util.ArrayList<>(attr.getModifiers())) {
            if (this.heartsKey.equals(m.getKey())) attr.removeModifier(m);
        }
        if (on) {
            attr.addModifier(new AttributeModifier(this.heartsKey, 20.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
            p.setHealth(attr.getValue());
        } else if (p.getHealth() > attr.getValue()) {
            p.setHealth(attr.getValue());
        }
    }
}
