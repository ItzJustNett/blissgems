package dev.xoperr.blissgems.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom item manager to replace Oraxen functionality
 * Uses PDC (Persistent Data Container) and Custom Model Data
 */
public class CustomItemManager {
    private static final Map<String, CustomItemData> ITEM_REGISTRY = new HashMap<>();
    private static NamespacedKey ITEM_ID_KEY;
    private static NamespacedKey UNDROPPABLE_KEY;

    // Register all custom items
    static {
        // Gems - Tier 1 (using ECHO_SHARD for BlissGems pack)
        registerItem("astra_gem_t1", Material.ECHO_SHARD, 1001, "§d§lASTRA GEM", List.of(
            "§f§lMANAGE THE TIDES OF THE COSMOS",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- 15% of incoming attacks will pass through you",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Dimensional Drift",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l🔪 DAGGERS",
            "§7Shoot 5 phantom daggers",
            "§7Each dagger hit deals 3 hearts of damage",
            "§7Hitting a player disables their gem for 10s",
            "",
            "§8Upgrade to Tier 2 for Astral Projection!"
        ));
        registerItem("fire_gem_t1", Material.ECHO_SHARD, 1002, "§d§lFIRE GEM", List.of(
            "§f§lMANIPULATE FIRE",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Fire Resistance",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Crisp",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l🧨 METEOR SHOWER",
            "§7Spawns a fireball that charges up passively",
            "§7When on fire blocks charges 2x as fast",
            "",
            "§8Upgrade to Tier 2 for Cozy Campfire!"
        ));
        registerItem("flux_gem_t1", Material.ECHO_SHARD, 1003, "§d§lFLUX GEM", List.of(
            "§f§lWITH GREAT POWER COMES GREAT RESPONSIBILITY",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Immune to weakness, slowness & hunger",
            "§7- Chance to stun when using arrows",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Conduction",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l✠ BEAM",
            "§7Shoot a powerful beam that does 2.5 hearts",
            "§7and bypasses totems",
            "",
            "§8Upgrade to Tier 2 for Ground ability!"
        ));
        registerItem("life_gem_t1", Material.ECHO_SHARD, 1004, "§d§lLIFE GEM", List.of(
            "§f§lCONTROL THE BALANCE OF LIFE",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Immune to withering",
            "§7- Heal half a heart every 5 seconds",
            "§7- +2 more hearts from gaps",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Rapid Healing",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l💘 HEART DRAINER",
            "§7Removes 4 hearts (20s)",
            "",
            "§8Upgrade to Tier 2 for Life Circle!"
        ));
        registerItem("puff_gem_t1", Material.ECHO_SHARD, 1005, "§d§lPUFF GEM", List.of(
            "§f§lBE THE BIGGEST BIRD",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- No fall damage",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Double Jump",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l☁ DASH",
            "§7Dashes you in the direction you are looking",
            "§7and deals 2 hearts of damage",
            "§7Regardless the protection",
            "",
            "§8Upgrade to Tier 2 for Breezy Bash!"
        ));
        registerItem("speed_gem_t1", Material.ECHO_SHARD, 1006, "§d§lSPEED GEM", List.of(
            "§f§lWATCH THE WORLD TURN INTO A BLUR",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Speed 2",
            "§7- Dolphin's Grace",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Terminal Velocity",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l🎯 SLOTHS SEDATIVE",
            "§7Slowness 2, mining fatigue 3",
            "§7Clears speed effects, 0.5x slower crits, 30s",
            "",
            "§8Upgrade to Tier 2 for Speed Storm!"
        ));
        registerItem("strength_gem_t1", Material.ECHO_SHARD, 1007, "§d§lSTRENGTH GEM", List.of(
            "§f§lHAVE THE STRENGTH OF A ARMY",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Strength 2",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Player Tracker",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l⚔ FRAILER",
            "§7Clears potion effects",
            "§7Gives weakness 1 for 20 seconds",
            "§7And gives withering, 40s",
            "",
            "§8Upgrade to Tier 2 for Chad Strength!"
        ));
        registerItem("wealth_gem_t1", Material.ECHO_SHARD, 1008, "§d§lWEALTH GEM", List.of(
            "§f§lFUEL A EMPIRE",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Luck 2",
            "§7- Hero of the Village 2",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Portable Chest",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l💸 UNFORTUNATE",
            "§7Cancels 40% of your enemies' actions",
            "",
            "§8Upgrade to Tier 2 for Amplification!"
        ));

        // Gems - Tier 2 (using ECHO_SHARD for BlissGems pack)
        registerItem("astra_gem_t2", Material.ECHO_SHARD, 2001, "§d§lASTRA GEM", List.of(
            "§f§lMANAGE THE TIDES OF THE COSMOS",
            "§a§o(Pristine)",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- 15% of incoming attacks will pass through you",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Dimensional Drift",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l🔪 DAGGERS",
            "§7Shoot 5 phantom daggers",
            "§7Each dagger hit deals 3 hearts of damage",
            "§7Hitting a player disables their gem for 10s",
            "",
            "§b§l👻 ASTRAL PROJECTION",
            "§7Transform into a projection and explore",
            "§7in spectator mode"
        ));
        registerItem("fire_gem_t2", Material.ECHO_SHARD, 2002, "§d§lFIRE GEM", List.of(
            "§f§lMANIPULATE FIRE",
            "§a§o(Pristine)",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Fire Resistance",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Crisp",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l🧨 METEOR SHOWER",
            "§7Spawns a fireball that charges up passively",
            "§7When on fire blocks charges 2x as fast",
            "",
            "§b§l🥾 COZY CAMPFIRE",
            "§7Spawns a campfire that heals 2 hearts",
            "§7and hunger / sec"
        ));
        registerItem("flux_gem_t2", Material.ECHO_SHARD, 2003, "§d§lFLUX GEM", List.of(
            "§f§lWITH GREAT POWER COMES GREAT RESPONSIBILITY",
            "§a§o(Pristine)",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Immune to weakness, slowness & hunger",
            "§7- Chance to stun when using arrows",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Conduction",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l✠ BEAM",
            "§7Shoot a powerful beam that does 2.5 hearts",
            "§7and bypasses totems",
            "",
            "§b§l🌀 GROUND",
            "§7Freezes enemies for 3 seconds"
        ));
        registerItem("life_gem_t2", Material.ECHO_SHARD, 2004, "§d§lLIFE GEM", List.of(
            "§f§lCONTROL THE BALANCE OF LIFE",
            "§a§o(Pristine)",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Immune to withering",
            "§7- Heal half a heart every 5 seconds",
            "§7- +2 more hearts from gaps",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Rapid Healing",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l💘 HEART DRAINER",
            "§7Removes 4 hearts (20s)",
            "§7Removes 2 hearts (N4, 1m)",
            "",
            "§b§l✨ LIFE CIRCLE",
            "§7Summons a 3 block wide zone",
            "§7Gives you +5 hearts and +5 hearts for nearby players"
        ));
        registerItem("puff_gem_t2", Material.ECHO_SHARD, 2005, "§d§lPUFF GEM", List.of(
            "§f§lBE THE BIGGEST BIRD",
            "§a§o(Pristine)",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- No fall damage",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Double Jump",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l☁ DASH",
            "§7Dashes you in the direction you are looking",
            "§7and deals 2 hearts of damage",
            "§7Regardless the protection",
            "",
            "§b§l⏫ BREEZY BASH",
            "§7Lifts players 3 blocks and smashes them",
            "§7At high velocity"
        ));
        registerItem("speed_gem_t2", Material.ECHO_SHARD, 2006, "§d§lSPEED GEM", List.of(
            "§f§lWATCH THE WORLD TURN INTO A BLUR",
            "§a§o(Pristine)",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Speed 2",
            "§7- Dolphin's Grace",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Terminal Velocity",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l🎯 SLOTHS SEDATIVE",
            "§7Slowness 2, mining fatigue 3",
            "§7Clears speed effects, 0.5x slower crits, 30s, R4",
            "§7Slowness 4, mining fatigue 3",
            "§7Clears speed effects, 0.5x slower crits, 40s, R4",
            "",
            "§b§l🌩 SPEED STORM",
            "§7Spawns a Thunder Storm with lightning strikes",
            "§7Gives trusted 1.5x faster crits and speed 3"
        ));
        registerItem("strength_gem_t2", Material.ECHO_SHARD, 2007, "§d§lSTRENGTH GEM", List.of(
            "§f§lHAVE THE STRENGTH OF A ARMY",
            "§a§o(Pristine)",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Strength 2",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Player Tracker",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l⚔ FRAILER",
            "§7Clears potion effects",
            "§7Gives weakness 1 for 20 seconds",
            "§7And gives withering, 40s, R5",
            "",
            "§b§l🤺 CHAD STRENGTH R4 40s",
            "§7Every 3 crits charges a hit that deals 2x the dmg",
            "§7Every 8 crits charges a hit that deals 2x the dmg"
        ));
        registerItem("wealth_gem_t2", Material.ECHO_SHARD, 2008, "§d§lWEALTH GEM", List.of(
            "§f§lFUEL A EMPIRE",
            "§a§o(Pristine)",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Luck 2",
            "§7- Hero of the Village 2",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Portable Chest",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l💸 UNFORTUNATE",
            "§7Cancels 40% of your enemies' actions",
            "",
            "§b§l🍀 AMPLIFICATION",
            "§7Can lock for 30 seconds"
        ));

        // Universal Upgrader (works for all gem types)
        registerItem("gem_upgrader", Material.ENCHANTED_BOOK, 3001, "§6§l§nGem Upgrader", List.of(
            "§7Right Click to upgrade any Tier 1 gem to Tier 2",
            "",
            "§8Works for ALL gem types:",
            "§5Astra §8• §cFire §8• §bFlux §8• §dLife",
            "§fPuff §8• §aSpeed §8• §6Strength §8• §eWealth"
        ));

        // Special items (using BlissGems pack materials)
        registerItem("energy_bottle", Material.GHAST_TEAR, 4001, "§b§lEnergy Bottle");
        registerItem("gem_trader", Material.EMERALD, 4002, "§2§lGem Trader");
        registerItem("repair_kit", Material.BEACON, 4003, "§d§lRepair Kit");
        registerItem("gem_fragment", Material.PRISMARINE_SHARD, 4004, "§3§lGem Fragment");
        registerItem("revive_beacon", Material.BEACON, 4005, "§e§lRevive Beacon", java.util.Arrays.asList(
            "§7A powerful beacon that can revive players",
            "§7from the brink of death.",
            "",
            "§6Right-click to activate"
        ));
    }

    public static void initialize(JavaPlugin plugin) {
        ITEM_ID_KEY = new NamespacedKey(plugin, "item_id");
        UNDROPPABLE_KEY = new NamespacedKey(plugin, "undroppable");
    }

    private static void registerItem(String id, Material material, int customModelData, String displayName) {
        ITEM_REGISTRY.put(id, new CustomItemData(material, customModelData, displayName, null));
    }

    private static void registerItem(String id, Material material, int customModelData, String displayName, List<String> lore) {
        ITEM_REGISTRY.put(id, new CustomItemData(material, customModelData, displayName, lore));
    }

    /**
     * Public API for addon plugins to register custom items
     * @param id The custom item ID (e.g., "allforone_gem_t1")
     * @param material The base material
     * @param customModelData The custom model data value
     * @param displayName The display name with color codes
     * @param lore The item lore (can be null)
     */
    public static void registerAddonItem(String id, Material material, int customModelData, String displayName, List<String> lore) {
        if (ITEM_REGISTRY.containsKey(id)) {
            throw new IllegalArgumentException("Item ID '" + id + "' is already registered!");
        }
        ITEM_REGISTRY.put(id, new CustomItemData(material, customModelData, displayName, lore));
    }

    /**
     * Get the custom item ID from an ItemStack
     * Replaces: OraxenItems.getIdByItem(item)
     */
    public static String getIdByItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        // Check if item has our custom ID in PDC
        if (meta.getPersistentDataContainer().has(ITEM_ID_KEY, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(ITEM_ID_KEY, PersistentDataType.STRING);
        }

        return null;
    }

    /**
     * Create an ItemStack from a custom item ID
     * Replaces: OraxenItems.getItemById(id).build()
     */
    public static ItemStack getItemById(String id) {
        return getItemById(id, -1);
    }

    /**
     * Create an ItemStack from a custom item ID with energy-aware pristine texture
     * @param id The custom item ID
     * @param energy The energy level (0-10), or -1 to use default texture
     * @return The ItemStack with appropriate pristine texture based on energy
     */
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

        // Calculate custom model data based on energy for gems
        int customModelData = data.customModelData;
        if (GemType.isGem(id) && energy >= 0) {
            customModelData = getPristineModelData(data.customModelData, energy, id);
        }

        // Set custom model data for resource pack
        meta.setCustomModelData(customModelData);

        // Store the item ID in PDC
        meta.getPersistentDataContainer().set(ITEM_ID_KEY, PersistentDataType.STRING, id);

        // Mark gems as undroppable (like DropItemControl's locked_item flag)
        if (GemType.isGem(id)) {
            meta.getPersistentDataContainer().set(UNDROPPABLE_KEY, PersistentDataType.BYTE, (byte) 1);
        }

        // Set display name
        meta.setDisplayName(data.displayName);

        // Set lore if available
        if (data.lore != null && !data.lore.isEmpty()) {
            meta.setLore(data.lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Calculate pristine model data based on energy level
     * Energy 0-2: Base texture (most broken)
     * Energy 3-5: Pristine 2 (damaged) - +20
     * Energy 6-8: Pristine 3 (worn) - +30
     * Energy 9-10: Pristine 4 (pristine/least broken) - +40
     *
     * Note: Strength gem always uses base texture (no pristine system)
     */
    private static int getPristineModelData(int baseModelData, int energy, String itemId) {
        // Strength gem always uses base texture regardless of energy
        if (itemId != null && (itemId.equals("strength_gem_t1") || itemId.equals("strength_gem_t2"))) {
            return baseModelData;
        }

        int pristineOffset;
        if (energy <= 2) {
            pristineOffset = 0; // Base texture - most broken (no pristine1 textures exist)
        } else if (energy <= 5) {
            pristineOffset = 20; // Pristine 2
        } else if (energy <= 8) {
            pristineOffset = 30; // Pristine 3
        } else {
            pristineOffset = 40; // Pristine 4 - least broken
        }
        return baseModelData + pristineOffset;
    }

    /**
     * Update the custom model data of a gem item based on energy level
     * This allows gems to visually reflect their energy state
     */
    public static void updateGemTexture(ItemStack item, int energy) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        String id = getIdByItem(item);
        if (id == null || !GemType.isGem(id)) {
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

        int pristineModelData = getPristineModelData(data.customModelData, energy, id);
        meta.setCustomModelData(pristineModelData);
        item.setItemMeta(meta);
    }

    /**
     * Check if an ItemStack is a custom item
     */
    public static boolean isCustomItem(ItemStack item) {
        return getIdByItem(item) != null;
    }

    /**
     * Check if an item is marked as undroppable (PDC flag)
     * Uses the same approach as DropItemControl's isItemLocked
     */
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

        return meta.getPersistentDataContainer().has(UNDROPPABLE_KEY, PersistentDataType.BYTE) &&
               meta.getPersistentDataContainer().get(UNDROPPABLE_KEY, PersistentDataType.BYTE) == 1;
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
