package dev.xoperr.blissgems.goldevent;

import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.villagerevent.VillagerEventItems;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Items of the Gold Gem event. Materials and custom model data match Blood's resource pack:
 * wire fragments are COPPER_INGOT 7001-7007, the core NETHER_STAR 7000 (ghost 7001), and the
 * ritual props (reveal, voronoi stages, shards, orb...) are PAPER 71xx.
 */
public final class RitualItems {
    public static final Material WIRE_FRAGMENT_MATERIAL = Material.COPPER_INGOT;
    public static final Material FRAGMENT_CORE_MATERIAL = Material.NETHER_STAR;
    public static final Material PROP_MATERIAL = Material.PAPER;
    public static final NamespacedKey KEY_WIRE_FRAGMENT = NamespacedKey.fromString("bliss:wire_fragment");
    public static final NamespacedKey KEY_FRAGMENT_CORE = NamespacedKey.fromString("bliss:fragment_core");

    private RitualItems() {
    }

    /** Wire Fragment 1..7; each is a different piece and all seven are needed for the dream. */
    public static ItemStack wireFragment(int number) {
        int n = Math.max(1, Math.min(7, number));
        ItemStack item = new ItemStack(WIRE_FRAGMENT_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KEY_WIRE_FRAGMENT, PersistentDataType.INTEGER, n);
        meta.setCustomModelData(7000 + n);
        meta.displayName(VillagerEventItems.text("<##F6C500>&lᴡɪʀᴇ ғʀᴀɢᴍᴇɴᴛ &7(" + n + "/7)"));
        meta.lore(List.of(VillagerEventItems.text("<##FFD773>A fragment with unspoken potential")));
        meta.setMaxStackSize(1);
        item.setItemMeta(meta);
        return item;
    }

    /** 1..7, or -1 when the item is not a numbered Wire Fragment. */
    public static int wireFragmentNumber(ItemStack item) {
        if (item == null || item.getType() != WIRE_FRAGMENT_MATERIAL || !item.hasItemMeta()) return -1;
        Integer n = item.getItemMeta().getPersistentDataContainer().get(KEY_WIRE_FRAGMENT, PersistentDataType.INTEGER);
        return n == null ? -1 : n;
    }

    public static boolean isWireFragment(ItemStack item) {
        return wireFragmentNumber(item) > 0 || (item != null && "wire_fragment".equals(CustomItemManager.getIdByItem(item)));
    }

    public static ItemStack fragmentCore() {
        ItemStack item = new ItemStack(FRAGMENT_CORE_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KEY_FRAGMENT_CORE, PersistentDataType.BYTE, (byte) 1);
        meta.setCustomModelData(7000);
        meta.displayName(VillagerEventItems.text("<##FFD773>&lꜰʀᴀɢᴍᴇɴᴛ ᴄᴏʀᴇ"));
        meta.lore(List.of(VillagerEventItems.text("<##C2A878>A centerpiece of divine masterpiece")));
        meta.setMaxStackSize(1);
        item.setItemMeta(meta);
        return item;
    }

    /** The new core, or the older "fragment_core" custom item from before the event existed. */
    public static boolean isFragmentCore(ItemStack item) {
        if (item == null) return false;
        if (item.getType() == FRAGMENT_CORE_MATERIAL && item.hasItemMeta()) {
            Byte b = item.getItemMeta().getPersistentDataContainer().get(KEY_FRAGMENT_CORE, PersistentDataType.BYTE);
            if (b != null && b == 1) return true;
        }
        return "fragment_core".equals(CustomItemManager.getIdByItem(item));
    }

    public static ItemStack fragmentCoreGhost() {
        return prop(FRAGMENT_CORE_MATERIAL, 7001);
    }

    public static ItemStack goldGemReveal() {
        return prop(PROP_MATERIAL, 7101);
    }

    public static ItemStack goldGemEmpty() {
        return prop(PROP_MATERIAL, 7120);
    }

    /** Gold gem filling up as it absorbs the fragments, stage 0..6. */
    public static ItemStack goldGemVoronoi(int stage) {
        return prop(PROP_MATERIAL, 7110 + Math.max(0, Math.min(6, stage)));
    }

    /** gemBit 0..7 (astra, fire, flux, life, puff, speed, strength, wealth), variant 1..3. */
    public static ItemStack gemShard(int gemBit, int variant) {
        return prop(PROP_MATERIAL, 7130 + Math.max(0, Math.min(7, gemBit)) * 3 + Math.max(0, Math.min(2, variant - 1)));
    }

    public static ItemStack gemBroken(int gemBit) {
        return prop(PROP_MATERIAL, 7160 + Math.max(0, Math.min(7, gemBit)));
    }

    public static ItemStack goldOrb() {
        return prop(PROP_MATERIAL, 7180);
    }

    private static ItemStack prop(Material material, int model) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(model);
        meta.setMaxStackSize(1);
        item.setItemMeta(meta);
        return item;
    }
}
