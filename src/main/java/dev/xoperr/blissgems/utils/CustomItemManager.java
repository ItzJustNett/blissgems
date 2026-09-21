/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package dev.xoperr.blissgems.utils;

import dev.xoperr.blissgems.utils.EnergyState;
import dev.xoperr.blissgems.utils.GemCosmetics;
import dev.xoperr.blissgems.utils.GemType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomItemManager {
    private static final Map<String, CustomItemData> ITEM_REGISTRY = new HashMap<String, CustomItemData>();
    private static NamespacedKey ITEM_ID_KEY;
    private static NamespacedKey UNDROPPABLE_KEY;
    private static NamespacedKey OWNER_KEY;
    private static NamespacedKey GOLD_INSTANCE_KEY;
    private static NamespacedKey DOUBLE_DURABILITY_KEY;
    private static NamespacedKey MYTHIC_KEY;
    private static NamespacedKey MACE_LIMITED_KEY;
    private static NamespacedKey VILLAGER_SOUL_KEY;
    private static NamespacedKey VILLAGER_DATA_KEY;

    public static void initialize(JavaPlugin plugin) {
        ITEM_ID_KEY = new NamespacedKey((Plugin)plugin, "item_id");
        UNDROPPABLE_KEY = new NamespacedKey((Plugin)plugin, "locked_item");
        OWNER_KEY = new NamespacedKey((Plugin)plugin, "item_owner");
        GOLD_INSTANCE_KEY = new NamespacedKey((Plugin)plugin, "gold_instance_id");
        DOUBLE_DURABILITY_KEY = new NamespacedKey((Plugin)plugin, "double_durability");
        MYTHIC_KEY = new NamespacedKey((Plugin)plugin, "special_mythic");
        MACE_LIMITED_KEY = new NamespacedKey((Plugin)plugin, "mace_limited");
        VILLAGER_SOUL_KEY = new NamespacedKey((Plugin)plugin, "villager_soul");
        VILLAGER_DATA_KEY = new NamespacedKey((Plugin)plugin, "villager_data");
        GemCosmetics.initialize(plugin);
    }

    public static UUID getGoldInstanceId(ItemStack item) {
        if (GOLD_INSTANCE_KEY == null || item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        String raw = (String)meta.getPersistentDataContainer().get(GOLD_INSTANCE_KEY, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static UUID ensureGoldInstanceId(ItemStack item) {
        UUID existing = CustomItemManager.getGoldInstanceId(item);
        if (existing != null) {
            return existing;
        }
        if (GOLD_INSTANCE_KEY == null || item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        UUID id = UUID.randomUUID();
        meta.getPersistentDataContainer().set(GOLD_INSTANCE_KEY, PersistentDataType.STRING, id.toString());
        item.setItemMeta(meta);
        return id;
    }

    public static boolean setOwner(ItemStack item, UUID owner) {
        if (OWNER_KEY == null || owner == null || item == null || item.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        if (meta.getPersistentDataContainer().has(OWNER_KEY, PersistentDataType.STRING)) {
            return false;
        }
        meta.getPersistentDataContainer().set(OWNER_KEY, PersistentDataType.STRING, owner.toString());
        item.setItemMeta(meta);
        return true;
    }

    public static UUID getOwner(ItemStack item) {
        if (OWNER_KEY == null || item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        String raw = (String)meta.getPersistentDataContainer().get(OWNER_KEY, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        }
        catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static boolean hasOwner(ItemStack item) {
        return CustomItemManager.getOwner(item) != null;
    }

    private static void registerItem(String id, Material material, int customModelData, String displayName) {
        ITEM_REGISTRY.put(id, new CustomItemData(material, customModelData, displayName, null));
    }

    private static void registerItem(String id, Material material, int customModelData, String displayName, List<String> lore) {
        ITEM_REGISTRY.put(id, new CustomItemData(material, customModelData, displayName, lore));
    }

    public static void registerAddonItem(String id, Material material, int customModelData, String displayName, List<String> lore) {
        if (ITEM_REGISTRY.containsKey(id)) {
            Bukkit.getLogger().warning("[BlissGems] Custom item id '" + id + "' already registered \u2014 skipping duplicate.");
            return;
        }
        ITEM_REGISTRY.put(id, new CustomItemData(material, customModelData, displayName, lore));
    }

    public static String getIdByItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        if (meta.getPersistentDataContainer().has(ITEM_ID_KEY, PersistentDataType.STRING)) {
            return (String)meta.getPersistentDataContainer().get(ITEM_ID_KEY, PersistentDataType.STRING);
        }
        return null;
    }

    public static ItemStack getItemById(String id) {
        return CustomItemManager.getItemById(id, -1);
    }

    public static ItemStack getItemById(String id, int energy) {
        CustomItemData data = ITEM_REGISTRY.get(id);
        if (data == null) {
            return null;
        }
        ItemStack item = new ItemStack(data.material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        int customModelData = data.customModelData;
        if (GemType.isGem(id) && energy >= 0) {
            customModelData = CustomItemManager.getPristineModelData(data.customModelData, energy, id);
        }
        meta.setCustomModelData(Integer.valueOf(customModelData));
        meta.getPersistentDataContainer().set(ITEM_ID_KEY, PersistentDataType.STRING, id);
        if (GemType.isGem(id)) {
            meta.getPersistentDataContainer().set(UNDROPPABLE_KEY, PersistentDataType.BYTE, (byte)1);
        }
        if (GemCosmetics.apply(meta, id)) {
            if (GemType.isGem(id)) {
                CustomItemManager.applyEnhancedGlint(meta, energy);
            }
        } else {
            meta.setDisplayName(data.displayName);
            if (GemType.isGem(id)) {
                CustomItemManager.applyPristinePlusVisuals(meta, data.lore, energy);
            } else if (data.lore != null && !data.lore.isEmpty()) {
                meta.setLore(data.lore);
            }
        }
        CustomItemManager.applySignatureEnchants(id, meta);
        item.setItemMeta(meta);
        if ("gold_gem_t1".equals(id)) {
            CustomItemManager.ensureGoldInstanceId(item);
        }
        return item;
    }

    private static void applySignatureEnchants(String id, ItemMeta meta) {
        if ("prismatic_edge".equals(id)) {
            meta.addEnchant(Enchantment.SHARPNESS, 7, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            if (MYTHIC_KEY != null) {
                meta.getPersistentDataContainer().set(MYTHIC_KEY, PersistentDataType.STRING, "prismatic_edge");
            }
        } else if ("netherite_helmet_mythic".equals(id)) {
            meta.addEnchant(Enchantment.PROTECTION, 3, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addEnchant(Enchantment.RESPIRATION, 3, true);
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            if (DOUBLE_DURABILITY_KEY != null) {
                meta.getPersistentDataContainer().set(DOUBLE_DURABILITY_KEY, PersistentDataType.BYTE, (byte)1);
            }
            if (MYTHIC_KEY != null) {
                meta.getPersistentDataContainer().set(MYTHIC_KEY, PersistentDataType.STRING, "netherite_helmet");
            }
        } else if ("netherite_chestplate_mythic".equals(id)) {
            meta.addEnchant(Enchantment.PROTECTION, 3, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            if (DOUBLE_DURABILITY_KEY != null) {
                meta.getPersistentDataContainer().set(DOUBLE_DURABILITY_KEY, PersistentDataType.BYTE, (byte)1);
            }
            if (MYTHIC_KEY != null) {
                meta.getPersistentDataContainer().set(MYTHIC_KEY, PersistentDataType.STRING, "netherite_chestplate");
            }
        } else if ("netherite_leggings_mythic".equals(id)) {
            meta.addEnchant(Enchantment.PROTECTION, 3, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            if (Enchantment.SWIFT_SNEAK != null) {
                meta.addEnchant(Enchantment.SWIFT_SNEAK, 3, true);
            }
            if (DOUBLE_DURABILITY_KEY != null) {
                meta.getPersistentDataContainer().set(DOUBLE_DURABILITY_KEY, PersistentDataType.BYTE, (byte)1);
            }
            if (MYTHIC_KEY != null) {
                meta.getPersistentDataContainer().set(MYTHIC_KEY, PersistentDataType.STRING, "netherite_leggings");
            }
        } else if ("netherite_boots_mythic".equals(id)) {
            meta.addEnchant(Enchantment.PROTECTION, 3, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addEnchant(Enchantment.FEATHER_FALLING, 4, true);
            meta.addEnchant(Enchantment.DEPTH_STRIDER, 3, true);
            if (Enchantment.SOUL_SPEED != null) {
                meta.addEnchant(Enchantment.SOUL_SPEED, 3, true);
            }
            if (DOUBLE_DURABILITY_KEY != null) {
                meta.getPersistentDataContainer().set(DOUBLE_DURABILITY_KEY, PersistentDataType.BYTE, (byte)1);
            }
            if (MYTHIC_KEY != null) {
                meta.getPersistentDataContainer().set(MYTHIC_KEY, PersistentDataType.STRING, "netherite_boots");
            }
        } else if ("mace_1".equals(id)) {
            if (MYTHIC_KEY != null) {
                meta.getPersistentDataContainer().set(MYTHIC_KEY, PersistentDataType.STRING, "mace_1");
            }
        } else if ("mace_limited".equals(id)) {
            if (MACE_LIMITED_KEY != null) {
                meta.getPersistentDataContainer().set(MACE_LIMITED_KEY, PersistentDataType.BYTE, (byte)1);
            }
            if (MYTHIC_KEY != null) {
                meta.getPersistentDataContainer().set(MYTHIC_KEY, PersistentDataType.STRING, "mace_limited");
            }
        } else if ("mace_3".equals(id)) {
            if (MYTHIC_KEY != null) {
                meta.getPersistentDataContainer().set(MYTHIC_KEY, PersistentDataType.STRING, "mace_3");
            }
        }
    }

    private static int getPristineModelData(int baseModelData, int energy, String itemId) {
        if (itemId != null && itemId.startsWith("gold_gem")) {
            return baseModelData;
        }
        int pristineOffset = energy <= 2 ? 0 : (energy <= 5 ? 20 : (energy <= 8 ? 30 : 40));
        return baseModelData + pristineOffset;
    }

    private static void applyEnhancedGlint(ItemMeta meta, int energy) {
        if (EnergyState.fromEnergy(energy).isEnhanced()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
        } else {
            meta.removeEnchant(Enchantment.UNBREAKING);
        }
    }

    public static void updateGemTexture(ItemStack item, int energy) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        String id = CustomItemManager.getIdByItem(item);
        if (id == null || !GemType.isGem(id)) {
            return;
        }
        if (id.startsWith("gold_gem")) {
            return;
        }
        CustomItemData data = ITEM_REGISTRY.get(id);
        if (data == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        int pristineModelData = CustomItemManager.getPristineModelData(data.customModelData, energy, id);
        meta.setCustomModelData(Integer.valueOf(pristineModelData));
        if (GemCosmetics.has(id)) {
            CustomItemManager.applyEnhancedGlint(meta, energy);
        } else {
            CustomItemManager.applyPristinePlusVisuals(meta, data.lore, energy);
        }
        item.setItemMeta(meta);
    }

    private static void applyPristinePlusVisuals(ItemMeta meta, List<String> baseLore, int energy) {
        EnergyState state = EnergyState.fromEnergy(energy);
        boolean enhanced = state.isEnhanced();
        ArrayList<String> lore = new ArrayList<>();
        if (baseLore != null) {
            lore.addAll(baseLore);
        }
        if (enhanced) {
            if (!lore.isEmpty()) {
                lore.add("");
            }
            lore.add("\u00a75\u272e " + state.getDisplayName());
        }
        meta.setLore(lore.isEmpty() ? null : lore);
        if (enhanced) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
        } else {
            meta.removeEnchant(Enchantment.UNBREAKING);
        }
    }

    public static boolean isCustomItem(ItemStack item) {
        return CustomItemManager.getIdByItem(item) != null;
    }

    public static boolean markAsUndroppable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (container.has(UNDROPPABLE_KEY, PersistentDataType.BYTE) && (Byte)container.get(UNDROPPABLE_KEY, PersistentDataType.BYTE) == 1) {
            return false;
        }
        container.set(UNDROPPABLE_KEY, PersistentDataType.BYTE, (byte)1);
        item.setItemMeta(meta);
        return true;
    }

    public static boolean isUndroppable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(UNDROPPABLE_KEY, PersistentDataType.BYTE) && (Byte)container.get(UNDROPPABLE_KEY, PersistentDataType.BYTE) == 1;
    }

    public static boolean isDoubleDurability(ItemStack item) {
        if (item == null || !item.hasItemMeta() || DOUBLE_DURABILITY_KEY == null) {
            return false;
        }
        Byte b = (Byte)item.getItemMeta().getPersistentDataContainer().get(DOUBLE_DURABILITY_KEY, PersistentDataType.BYTE);
        return b != null && b == 1;
    }

    public static boolean isLimitedMace(ItemStack item) {
        if (item == null || !item.hasItemMeta() || MACE_LIMITED_KEY == null) {
            return false;
        }
        Byte b = (Byte)item.getItemMeta().getPersistentDataContainer().get(MACE_LIMITED_KEY, PersistentDataType.BYTE);
        return b != null && b == 1;
    }

    public static boolean isMythic(ItemStack item) {
        if (item == null || !item.hasItemMeta() || MYTHIC_KEY == null) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(MYTHIC_KEY, PersistentDataType.STRING);
    }

    public static String getMythicId(ItemStack item) {
        if (item == null || !item.hasItemMeta() || MYTHIC_KEY == null) {
            return null;
        }
        return (String)item.getItemMeta().getPersistentDataContainer().get(MYTHIC_KEY, PersistentDataType.STRING);
    }

    public static NamespacedKey getVillagerSoulKey() {
        return VILLAGER_SOUL_KEY;
    }

    public static NamespacedKey getVillagerDataKey() {
        return VILLAGER_DATA_KEY;
    }

    public static NamespacedKey getDoubleDurabilityKey() {
        return DOUBLE_DURABILITY_KEY;
    }

    public static NamespacedKey getMythicKey() {
        return MYTHIC_KEY;
    }

    public static NamespacedKey getMaceLimitedKey() {
        return MACE_LIMITED_KEY;
    }

    static {
        CustomItemManager.registerItem("mace_1", Material.MACE, 6001, "\u00a7e\u00a7lHeavy Mace", List.of("\u00a77A perfectly balanced battle mace.", "", "\u00a7a\u2714 Fully Enchantable", "\u00a78Part of the original triad."));
        CustomItemManager.registerItem("mace_limited", Material.MACE, 6002, "\u00a7c\u00a7lTempered Mace", List.of("\u00a77A heavy, restricted battle mace.", "", "\u00a7c\u2716 Density max level II", "\u00a7c\u2716 Wind Burst & Breach Restricted", "\u00a76\u26a1 60s Combat Cooldown"));
        CustomItemManager.registerItem("mace_3", Material.MACE, 6003, "\u00a7b\u00a7lOvercharged Mace", List.of("\u00a77Surging with raw kinetic force.", "", "\u00a7b\u2726 High Impact Triad Weapon"));
        CustomItemManager.registerItem("netherite_helmet_mythic", Material.NETHERITE_HELMET, 7001, "\u00a76\u00a7lMythic Netherite Helmet", List.of("\u00a77Forged with ancient durability.", "", "\u00a7a\u2726 Protection III", "\u00a7a\u2726 Maxed Secondary Enchants", "\u00a7d\u2726 Double Durability (814 Max)"));
        CustomItemManager.registerItem("netherite_chestplate_mythic", Material.NETHERITE_CHESTPLATE, 7002, "\u00a76\u00a7lMythic Netherite Chestplate", List.of("\u00a77Forged with ancient durability.", "", "\u00a7a\u2726 Protection III", "\u00a7a\u2726 Maxed Secondary Enchants", "\u00a7d\u2726 Double Durability (1184 Max)"));
        CustomItemManager.registerItem("netherite_leggings_mythic", Material.NETHERITE_LEGGINGS, 7003, "\u00a76\u00a7lMythic Netherite Leggings", List.of("\u00a77Forged with ancient durability.", "", "\u00a7a\u2726 Protection III", "\u00a7a\u2726 Maxed Secondary Enchants", "\u00a7d\u2726 Double Durability (1110 Max)"));
        CustomItemManager.registerItem("netherite_boots_mythic", Material.NETHERITE_BOOTS, 7004, "\u00a76\u00a7lMythic Netherite Boots", List.of("\u00a77Forged with ancient durability.", "", "\u00a7a\u2726 Protection III", "\u00a7a\u2726 Maxed Secondary Enchants", "\u00a7d\u2726 Double Durability (962 Max)"));
        CustomItemManager.registerItem("player_tracker", Material.COMPASS, 8001, "\u00a7a\u00a7lPlayer Tracker", List.of("\u00a77Points towards the nearest player.", "", "\u00a7eTrade with the Energy Games Villager", "\u00a7efor raw Energy."));
        CustomItemManager.registerItem("villager_soul", Material.NETHER_STAR, 8002, "\u00a7d\u00a7lVillager Soul", List.of("\u00a77An essence encapsulating a merchant.", "", "\u00a7eShift + Right-Click block to release.", "\u00a78Preserves all trade history and limits."));
        CustomItemManager.registerItem("astra_gem_t1", Material.ECHO_SHARD, 1001, "\u00a7d\u00a7lASTRA GEM", List.of("\u00a7f\u00a7lMANAGE THE TIDES OF THE COSMOS", "", "\u00a7a\ud83c\udf1f \u00a7a\u00a7lPASSIVES", "\u00a77- Soul Capture & Soul Healing", "", "\u00a7d\ud83c\udf1f \u00a7d\u00a7lPOWERS", "\u00a7b\u00a7l\ud83d\udd2a ASTRAL DAGGERS", "\u00a77Conjure 3 phantom daggers, launch each one by one", "", "\u00a78Upgrade to Tier 2 for Astral Projection,", "\u00a78Dimensional Drift & Void!"));
        CustomItemManager.registerItem("fire_gem_t1", Material.ECHO_SHARD, 1002, "\u00a7d\u00a7lFIRE GEM", List.of("\u00a7f\u00a7lMANIPULATE FIRE", "", "\u00a7a\ud83c\udf1f \u00a7a\u00a7lPASSIVES", "\u00a77- Fire Resistance", "\u00a77- Flame & Fire Aspect on weapons", "", "\u00a7d\ud83c\udf1f \u00a7d\u00a7lPOWERS", "\u00a7b\u00a7l\ud83d\udd25 CHARGED FIREBALL", "\u00a77Hold to charge, release to fire", "\u00a77Stand on obsidian to prevent charge decay", "", "\u00a78Upgrade to Tier 2 for Campfire,", "\u00a78Crisp & Meteor Shower!"));
        CustomItemManager.registerItem("flux_gem_t1", Material.ECHO_SHARD, 1003, "\u00a7d\u00a7lFLUX GEM", List.of("\u00a7f\u00a7lWITH GREAT POWER COMES GREAT RESPONSIBILITY", "", "\u00a7a\ud83c\udf1f \u00a7a\u00a7lPASSIVES", "\u00a77- Immune to Weakness, Slowness & Hunger", "\u00a77- Shocking Chance (stun on arrow hits)", "", "\u00a7b\ud83c\udf1f \u00a7b\u00a7lABILITY", "\u00a77- Conduction (/bliss conduction \u2192 nearest copper block)", "", "\u00a7d\ud83c\udf1f \u00a7d\u00a7lPOWERS", "\u00a7b\u00a7l\u2720 FLUX BEAM", "\u00a77Charge and fire a powerful beam", "\u00a77Charged beam deals massive armor damage", "", "\u00a78Upgrade to Tier 2 for Ground & more!"));
        CustomItemManager.registerItem("life_gem_t1", Material.ECHO_SHARD, 1004, "\u00a7d\u00a7lLIFE GEM", List.of("\u00a7f\u00a7lCONTROL THE BALANCE OF LIFE", "", "\u00a7a\ud83c\udf1f \u00a7a\u00a7lPASSIVES", "\u00a77- Wither immunity", "\u00a77- Continuous healing", "\u00a77- Unbreaking on tools", "", "\u00a7d\ud83c\udf1f \u00a7d\u00a7lPOWERS", "\u00a7b\u00a7l\ud83d\udc98 HEART DRAINER", "\u00a77Siphon health from your enemies", "", "\u00a78Upgrade to Tier 2 for Life Circle,", "\u00a78Vitality Vortex & Heart Lock!"));
        CustomItemManager.registerItem("puff_gem_t1", Material.ECHO_SHARD, 1005, "\u00a7d\u00a7lPUFF GEM", List.of("\u00a7f\u00a7lBE THE BIGGEST BIRD", "", "\u00a7a\ud83c\udf1f \u00a7a\u00a7lPASSIVES", "\u00a77- No fall damage", "\u00a77- Power & Punch on bows", "", "\u00a7b\ud83c\udf1f \u00a7b\u00a7lABILITY", "\u00a77- Double Jump", "", "\u00a7d\ud83c\udf1f \u00a7d\u00a7lPOWERS", "\u00a7b\u00a7l\u2601 DASH", "\u00a77Dashes in the direction you're looking", "\u00a77Deals damage if passing through enemies", "", "\u00a78Upgrade to Tier 2 for Breezy Bash!"));
        CustomItemManager.registerItem("speed_gem_t1", Material.ECHO_SHARD, 1006, "\u00a7d\u00a7lSPEED GEM", List.of("\u00a7f\u00a7lWATCH THE WORLD TURN INTO A BLUR", "", "\u00a7a\ud83c\udf1f \u00a7a\u00a7lPASSIVES", "\u00a77- Speed I & Dolphin's Grace", "\u00a77- Efficiency on tools", "", "\u00a7d\ud83c\udf1f \u00a7d\u00a7lPOWERS", "\u00a7b\u00a7l\u26a1 BLUR", "\u00a77Summons successive lightning strikes", "\u00a77dealing damage and knockback", "", "\u00a78Upgrade to Tier 2 for Speed Storm", "\u00a78& Terminal Velocity!"));
        CustomItemManager.registerItem("strength_gem_t1", Material.ECHO_SHARD, 1007, "\u00a7d\u00a7lSTRENGTH GEM", List.of("\u00a7f\u00a7lHAVE THE STRENGTH OF AN ARMY", "", "\u00a7a\ud83c\udf1f \u00a7a\u00a7lPASSIVES", "\u00a77- Strength I", "\u00a77- Sharpness II on weapons", "\u00a77- Bloodthorns (more damage at low HP)", "", "\u00a78No active abilities at Tier 1", "\u00a78Upgrade to Tier 2 for Nullify, Frailer", "\u00a78& Shadow Stalker!"));
        CustomItemManager.registerItem("wealth_gem_t1", Material.ECHO_SHARD, 1008, "\u00a7d\u00a7lWEALTH GEM", List.of("\u00a7f\u00a7lFUEL AN EMPIRE", "", "\u00a7a\ud83c\udf1f \u00a7a\u00a7lPASSIVES", "\u00a77- Luck & Hero of the Village", "\u00a77- Mending, Fortune & Looting on tools", "\u00a77- Durability Chip (extra armor damage)", "", "\u00a78No active abilities at Tier 1", "\u00a78Upgrade to Tier 2 for Pockets,", "\u00a78Unfortunate & more!"));
        CustomItemManager.registerItem("gold_gem_t1", Material.PRISMARINE_CRYSTALS, 1009, "\u00a76\u00a7lGOLD GEM", List.of("\u00a7f\u00a7lWATCH THE LINES OF REALITY FRAY AS EIGHT SOULS BECOME ONE", "\u00a76(Dormant)", "", "\u00a76\ud83c\udf1f \u00a76\u00a7lPASSIVES", "\u00a77- \u00a7kunstable power", "\u00a77- \u00a7kharvested souls", "\u00a77- \u00a7ksoulbound vessel", "\u00a77- \u00a7kfraying lines", "", "\u00a76\ud83c\udf1f \u00a76\u00a7lABILITY", "\u00a77- \u00a7ksundering beam", "", "\u00a76\ud83c\udf1f \u00a76\u00a7lPOWERS", "\u00a77- \u00a7kchannelled soul", "\u00a77- \u00a7krepurposing", "", "\u00a77- \u00a7kawakening", "\u00a77- \u00a7keight as one"));
        CustomItemManager.registerItem("astra_gem_t2", Material.ECHO_SHARD, 2001, "\u00a7d\u00a7lASTRA GEM", List.of("\u00a7f\u00a7lMANAGE THE TIDES OF THE COSMOS", "\u00a7a\u00a7o(Pristine)", "", "\u00a7a\ud83c\udf1f \u00a7a\u00a7lPASSIVES", "\u00a77- Soul Capture & Soul Healing", "", "\u00a7d\ud83c\udf1f \u00a7d\u00a7lPOWERS", "\u00a7b\u00a7l\ud83d\udd2a ASTRAL DAGGERS", "\u00a77Conjure 3 phantom daggers, launch each one by one", "", "\u00a7b\u00a7l\ud83d\udc7b ASTRAL PROJECTION", "\u00a77Scout in spectator mode", "\u00a77Sub-abilities: \u00a7dSpook \u00a77& \u00a7dTag", "", "\u00a7b\u00a7l\ud83c\udf00 DIMENSIONAL DRIFT", "\u00a77Dash forward through the rift, briefly invisible", "", "\u00a7b\u00a7l\ud83d\udd73 DIMENSIONAL VOID", "\u00a77Nullify enemy gem abilities in radius"));
        CustomItemManager.registerItem("fire_gem_t2", Material.ECHO_SHARD, 2002, "\u00a7d\u00a7lFIRE GEM", List.of("\u00a7f\u00a7lMANIPULATE FIRE", "\u00a7a\u00a7o(Pristine)", "", "\u00a7a\ud83c\udf1f \u00a7a\u00a7lPASSIVES", "\u00a77- Fire Resistance", "\u00a77- Flame & Fire Aspect on weapons", "", "\u00a7d\ud83c\udf1f \u00a7d\u00a7lPOWERS", "\u00a7b\u00a7l\ud83d\udd25 CHARGED FIREBALL", "\u00a77Hold to charge, release to fire", "\u00a77Stand on obsidian to prevent charge decay", "", "\u00a7b\u00a7l\ud83e\udd7e COZY CAMPFIRE", "\u00a77Spawns a campfire granting allies Regen IV", "", "\u00a7b\u00a7l\ud83e\uddca CRISP", "\u00a77Evaporate water, replace blocks with nether", "", "\u00a7b\u00a7l\ud83e\udde8 METEOR SHOWER", "\u00a77Rain fire on a target area"));
        CustomItemManager.registerItem("flux_gem_t2", Material.ECHO_SHARD, 2003, "\u00a7d\u00a7lFLUX GEM", List.of("\u00a7f\u00a7lWITH GREAT POWER COMES GREAT RESPONSIBILITY", "\u00a7a\u00a7o(Pristine)", "", "\u00a7a\ud83c\udf1f \u00a7a\u00a7lPASSIVES", "\u00a77- Immune to Weakness, Slowness & Hunger", "\u00a77- Shocking Chance (stun on arrow hits)", "", "\u00a7b\ud83c\udf1f \u00a7b\u00a7lABILITY", "\u00a77- Conduction (/bliss conduction \u2192 nearest copper block)", "", "\u00a7d\ud83c\udf1f \u00a7d\u00a7lPOWERS", "\u00a7b\u00a7l\u2720 FLUX BEAM", "\u00a77Chargeable beam dealing massive armor damage", "", "\u00a7b\u00a7l\ud83c\udf00 GROUND", "\u00a77Freeze enemies in place", "", "\u00a7b\u00a7l\ud83d\udca5 FLASHBANG", "\u00a77Blindness and Nausea to enemies in radius", "", "\u00a7b\u00a7l\u26a1 KINETIC BURST", "\u00a77Radial knockback with sonic boom"));
        CustomItemManager.registerItem("life_gem_t2", Material.ECHO_SHARD, 2004, "\u00a7d\u00a7lLIFE GEM", List.of("\u00a7f\u00a7lCONTROL THE BALANCE OF LIFE", "\u00a7a\u00a7o(Pristine)", "", "\u00a7a\ud83c\udf1f \u00a7a\u00a7lPASSIVES", "\u00a77- Wither immunity", "\u00a77- Continuous healing", "\u00a77- Unbreaking on tools", "", "\u00a7d\ud83c\udf1f \u00a7d\u00a7lPOWERS", "\u00a7b\u00a7l\ud83d\udc98 HEART DRAINER", "\u00a77Siphon health from your enemies", "", "\u00a7b\u00a7l\u2728 CIRCLE OF LIFE", "\u00a77Zone that decreases enemy max HP", "\u00a77and increases yours and allies' HP", "", "\u00a7b\u00a7l\ud83d\udcab VITALITY VORTEX", "\u00a77Grants effects based on your surroundings", "", "\u00a7b\u00a7l\ud83d\udd12 HEART LOCK", "\u00a77Cap enemy max HP at their current HP"));
        CustomItemManager.registerItem("puff_gem_t2", Material.ECHO_SHARD, 2005, "\u00a7d\u00a7lPUFF GEM", List.of("\u00a7f\u00a7lBE THE BIGGEST BIRD", "\u00a7a\u00a7o(Pristine)", "", "\u00a7a\ud83c\udf1f \u00a7a\u00a7lPASSIVES", "\u00a77- No fall damage", "\u00a77- Power & Punch on bows", "", "\u00a7b\ud83c\udf1f \u00a7b\u00a7lABILITY", "\u00a77- Double Jump", "", "\u00a7d\ud83c\udf1f \u00a7d\u00a7lPOWERS", "\u00a7b\u00a7l\u2601 DASH", "\u00a77Dashes in the direction you're looking", "\u00a77Deals damage if passing through enemies", "", "\u00a7b\u00a7l\u23eb BREEZY BASH", "\u00a77Launch an enemy skyward then slam them", "", "\u00a7b\u00a7l\ud83c\udf2a GROUP BREEZY BASH", "\u00a77Send all nearby enemies flying away"));
        CustomItemManager.registerItem("speed_gem_t2", Material.ECHO_SHARD, 2006, "\u00a7d\u00a7lSPEED GEM", List.of("\u00a7f\u00a7lWATCH THE WORLD TURN INTO A BLUR", "\u00a7a\u00a7o(Pristine)", "", "\u00a7a\ud83c\udf1f \u00a7a\u00a7lPASSIVES", "\u00a77- Speed I & Dolphin's Grace", "\u00a77- Efficiency on tools", "", "\u00a7d\ud83c\udf1f \u00a7d\u00a7lPOWERS", "\u00a7b\u00a7l\u26a1 BLUR", "\u00a77Summons successive lightning strikes", "\u00a77dealing damage and knockback", "", "\u00a7b\u00a7l\ud83c\udf29 SPEED STORM", "\u00a77Freezes enemies while granting allies", "\u00a77Speed and Haste", "", "\u00a7b\u00a7l\ud83d\udca8 TERMINAL VELOCITY", "\u00a77Speed III + Haste II for a short duration"));
        CustomItemManager.registerItem("strength_gem_t2", Material.ECHO_SHARD, 2007, "\u00a7d\u00a7lSTRENGTH GEM", List.of("\u00a7f\u00a7lHAVE THE STRENGTH OF AN ARMY", "\u00a7a\u00a7o(Pristine)", "", "\u00a7a\ud83c\udf1f \u00a7a\u00a7lPASSIVES", "\u00a77- Strength I", "\u00a77- Sharpness V on weapons", "\u00a77- Bloodthorns (more damage at low HP)", "", "\u00a7d\ud83c\udf1f \u00a7d\u00a7lPOWERS", "\u00a7b\u00a7l\u2694 CHAD STRENGTH", "\u00a77Empower your next few hits with bonus damage", "", "\u00a7b\u00a7l\ud83d\udc94 FRAILER", "\u00a77Apply Weakness I (20s), Slowness & Wither I (40s)", "", "\u00a7b\u00a7l\ud83d\udd0d SHADOW STALKER", "\u00a77Consume a player head or an owned item", "\u00a77to track a player's location", "", "\u00a7b\u00a7l\ud83d\udeab NULLIFY", "\u00a77Temporarily strip a target's potion effects"));
        CustomItemManager.registerItem("wealth_gem_t2", Material.ECHO_SHARD, 2008, "\u00a7d\u00a7lWEALTH GEM", List.of("\u00a7f\u00a7lFUEL AN EMPIRE", "\u00a7a\u00a7o(Pristine)", "", "\u00a7a\ud83c\udf1f \u00a7a\u00a7lPASSIVES", "\u00a77- Luck & Hero of the Village", "\u00a77- Mending, Fortune & Looting on tools", "\u00a77- Durability Chip & Armor Mend", "", "\u00a7d\ud83c\udf1f \u00a7d\u00a7lPOWERS", "\u00a7b\u00a7l\ud83d\udcb8 UNFORTUNATE", "\u00a77Chance to disable enemy actions", "", "\u00a7b\u00a7l\ud83c\udf92 POCKETS", "\u00a779 extra inventory slots (/bliss pockets)", "", "\u00a7b\u00a7l\ud83d\udd12 ITEM LOCK", "\u00a77Lock an enemy's held item temporarily", "", "\u00a7b\u00a7l\u2728 AMPLIFICATION", "\u00a77Boost all enchantments for 45s", "", "\u00a7b\u00a7l\ud83c\udf40 RICH RUSH", "\u00a77Increased drop rates for ~3 min"));
        CustomItemManager.registerItem("gem_upgrader", Material.ENCHANTED_BOOK, 3001, "\u00a76\u00a7l\u00a7nGem Upgrader", List.of("\u00a77Right Click to upgrade any Tier 1 gem to Tier 2", "", "\u00a78Works for ALL gem types:", "\u00a75Astra \u00a78\u2022 \u00a7cFire \u00a78\u2022 \u00a7bFlux \u00a78\u2022 \u00a7dLife", "\u00a7fPuff \u00a78\u2022 \u00a7aSpeed \u00a78\u2022 \u00a76Strength \u00a78\u2022 \u00a7eWealth"));
        CustomItemManager.registerItem("prismatic_edge", Material.NETHERITE_SWORD, 5001, "\u00a7b\u00a7l\u00a7nPrismatic Edge", List.of("\u00a77A blade holding the light of every gem.", "", "\u00a7bSneak + Left Click \u00a77to fire a \u00a7dprismatic beam", "\u00a77that freezes whoever it strikes.", "", "\u00a77Land \u00a7e5 hits in a row \u00a77and every hit", "\u00a77after that crits - until you are struck."));
        CustomItemManager.registerItem("restoration_book", Material.ENCHANTED_BOOK, 3002, "\u00a75\u00a7l\u00a7nRestoration Book", List.of("\u00a77Right Click while your gem is \u00a7c\u00a7lBROKEN", "\u00a77to begin a \u00a75Restoration Ritual\u00a77.", "", "\u00a77Your gem is reforged at random and", "\u00a77returns at \u00a7bPristine\u00a77.", "", "\u00a78The whole server will know."));
        CustomItemManager.registerItem("energy_bottle", Material.GHAST_TEAR, 4001, "\u00a7b\u00a7lEnergy Bottle");
        CustomItemManager.registerItem("gem_trader", Material.EMERALD, 4002, "\u00a72\u00a7lGem Trader");
        CustomItemManager.registerItem("repair_kit", Material.BEACON, 4003, "\u00a7d\u00a7lRepair Kit");
        CustomItemManager.registerItem("gem_fragment", Material.PRISMARINE_SHARD, 4004, "\u00a73\u00a7lGem Fragment");
        CustomItemManager.registerItem("wire_fragment", Material.LIGHTNING_ROD, 4006, "\u00a76\u00a7lWire Fragment", List.of("\u00a77A strand torn from the lines of reality.", "", "\u00a78Seven of these and a Fragment Core", "\u00a78summon the Gold Gem."));
        CustomItemManager.registerItem("fragment_core", Material.NETHER_STAR, 4007, "\u00a76\u00a7lFragment Core", List.of("\u00a77What was left behind after the Golden Dream.", "", "\u00a78Binds seven Wire Fragments into one."));
        CustomItemManager.registerItem("revive_beacon", Material.BEACON, 4005, "\u00a7e\u00a7lRevive Beacon", Arrays.asList("\u00a77A powerful beacon that can revive players", "\u00a77from the brink of death.", "", "\u00a76Right-click to activate"));
    }

    private static class CustomItemData {
        final Material material;
        final int customModelData;
        final String displayName;
        final List<String> lore;

        CustomItemData(Material material, int customModelData, String displayName, List<String> lore) {
            this.material = material;
            this.customModelData = customModelData;
            this.displayName = displayName;
            this.lore = lore;
        }
    }
}

