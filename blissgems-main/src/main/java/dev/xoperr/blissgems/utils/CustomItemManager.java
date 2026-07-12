package dev.xoperr.blissgems.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
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
            "§7- Soul Capture & Soul Healing",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l🔪 ASTRAL DAGGERS",
            "§7Fire 3 phantom daggers that deal damage",
            "",
            "§8Upgrade to Tier 2 for Astral Projection,",
            "§8Dimensional Drift & Void!"
        ));
        registerItem("fire_gem_t1", Material.ECHO_SHARD, 1002, "§d§lFIRE GEM", List.of(
            "§f§lMANIPULATE FIRE",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Fire Resistance",
            "§7- Flame & Fire Aspect on weapons",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l🔥 CHARGED FIREBALL",
            "§7Hold to charge, release to fire",
            "§7Stand on obsidian to prevent charge decay",
            "",
            "§8Upgrade to Tier 2 for Campfire,",
            "§8Crisp & Meteor Shower!"
        ));
        registerItem("flux_gem_t1", Material.ECHO_SHARD, 1003, "§d§lFLUX GEM", List.of(
            "§f§lWITH GREAT POWER COMES GREAT RESPONSIBILITY",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Immune to Weakness, Slowness & Hunger",
            "§7- Shocking Chance (stun on arrow hits)",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Conduction (teleport to copper blocks)",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l✠ FLUX BEAM",
            "§7Charge and fire a powerful beam",
            "§7Charged beam deals massive armor damage",
            "",
            "§8Upgrade to Tier 2 for Ground & more!"
        ));
        registerItem("life_gem_t1", Material.ECHO_SHARD, 1004, "§d§lLIFE GEM", List.of(
            "§f§lCONTROL THE BALANCE OF LIFE",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Wither immunity",
            "§7- Continuous healing",
            "§7- Unbreaking on tools",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l💘 HEART DRAINER",
            "§7Siphon health from your enemies",
            "",
            "§8Upgrade to Tier 2 for Life Circle,",
            "§8Vitality Vortex & Heart Lock!"
        ));
        registerItem("puff_gem_t1", Material.ECHO_SHARD, 1005, "§d§lPUFF GEM", List.of(
            "§f§lBE THE BIGGEST BIRD",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- No fall damage",
            "§7- Power & Punch on bows",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Double Jump",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l☁ DASH",
            "§7Dashes in the direction you're looking",
            "§7Deals damage if passing through enemies",
            "",
            "§8Upgrade to Tier 2 for Breezy Bash!"
        ));
        registerItem("speed_gem_t1", Material.ECHO_SHARD, 1006, "§d§lSPEED GEM", List.of(
            "§f§lWATCH THE WORLD TURN INTO A BLUR",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Speed I & Dolphin's Grace",
            "§7- Efficiency on tools",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l⚡ BLUR",
            "§7Summons successive lightning strikes",
            "§7dealing damage and knockback",
            "",
            "§8Upgrade to Tier 2 for Speed Storm",
            "§8& Terminal Velocity!"
        ));
        registerItem("strength_gem_t1", Material.ECHO_SHARD, 1007, "§d§lSTRENGTH GEM", List.of(
            "§f§lHAVE THE STRENGTH OF AN ARMY",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Strength I",
            "§7- Sharpness II on weapons",
            "§7- Bloodthorns (more damage at low HP)",
            "",
            "§8No active abilities at Tier 1",
            "§8Upgrade to Tier 2 for Nullify, Frailer",
            "§8& Shadow Stalker!"
        ));
        registerItem("wealth_gem_t1", Material.ECHO_SHARD, 1008, "§d§lWEALTH GEM", List.of(
            "§f§lFUEL AN EMPIRE",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Luck & Hero of the Village",
            "§7- Mending, Fortune & Looting on tools",
            "§7- Durability Chip (extra armor damage)",
            "",
            "§8No active abilities at Tier 1",
            "§8Upgrade to Tier 2 for Pockets,",
            "§8Unfortunate & more!"
        ));

        // Gems - Tier 2 (using ECHO_SHARD for BlissGems pack)
        registerItem("astra_gem_t2", Material.ECHO_SHARD, 2001, "§d§lASTRA GEM", List.of(
            "§f§lMANAGE THE TIDES OF THE COSMOS",
            "§a§o(Pristine)",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Soul Capture & Soul Healing",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l🔪 ASTRAL DAGGERS",
            "§7Fire 3 phantom daggers that deal damage",
            "",
            "§b§l👻 ASTRAL PROJECTION",
            "§7Scout in spectator mode",
            "§7Sub-abilities: §dSpook §7& §dTag",
            "",
            "§b§l🌀 DIMENSIONAL DRIFT",
            "§7Ride an invisible horse while invisible",
            "",
            "§b§l🕳 DIMENSIONAL VOID",
            "§7Nullify enemy gem abilities in radius"
        ));
        registerItem("fire_gem_t2", Material.ECHO_SHARD, 2002, "§d§lFIRE GEM", List.of(
            "§f§lMANIPULATE FIRE",
            "§a§o(Pristine)",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Fire Resistance",
            "§7- Flame & Fire Aspect on weapons",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l🔥 CHARGED FIREBALL",
            "§7Hold to charge, release to fire",
            "§7Stand on obsidian to prevent charge decay",
            "",
            "§b§l🥾 COZY CAMPFIRE",
            "§7Spawns a campfire granting allies Regen IV",
            "",
            "§b§l🧊 CRISP",
            "§7Evaporate water, replace blocks with nether",
            "",
            "§b§l🧨 METEOR SHOWER",
            "§7Rain fire on a target area"
        ));
        registerItem("flux_gem_t2", Material.ECHO_SHARD, 2003, "§d§lFLUX GEM", List.of(
            "§f§lWITH GREAT POWER COMES GREAT RESPONSIBILITY",
            "§a§o(Pristine)",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Immune to Weakness, Slowness & Hunger",
            "§7- Shocking Chance (stun on arrow hits)",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Conduction (teleport to copper blocks)",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l✠ FLUX BEAM",
            "§7Chargeable beam dealing massive armor damage",
            "",
            "§b§l🌀 GROUND",
            "§7Freeze enemies in place",
            "",
            "§b§l💥 FLASHBANG",
            "§7Blindness and Nausea to enemies in radius",
            "",
            "§b§l⚡ KINETIC BURST",
            "§7Radial knockback with sonic boom"
        ));
        registerItem("life_gem_t2", Material.ECHO_SHARD, 2004, "§d§lLIFE GEM", List.of(
            "§f§lCONTROL THE BALANCE OF LIFE",
            "§a§o(Pristine)",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Wither immunity",
            "§7- Continuous healing",
            "§7- Unbreaking on tools",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l💘 HEART DRAINER",
            "§7Siphon health from your enemies",
            "",
            "§b§l✨ CIRCLE OF LIFE",
            "§7Zone that decreases enemy max HP",
            "§7and increases yours and allies' HP",
            "",
            "§b§l💫 VITALITY VORTEX",
            "§7Grants effects based on your surroundings",
            "",
            "§b§l🔒 HEART LOCK",
            "§7Cap enemy max HP at their current HP"
        ));
        registerItem("puff_gem_t2", Material.ECHO_SHARD, 2005, "§d§lPUFF GEM", List.of(
            "§f§lBE THE BIGGEST BIRD",
            "§a§o(Pristine)",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- No fall damage",
            "§7- Power & Punch on bows",
            "",
            "§b🌟 §b§lABILITY",
            "§7- Double Jump",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l☁ DASH",
            "§7Dashes in the direction you're looking",
            "§7Deals damage if passing through enemies",
            "",
            "§b§l⏫ BREEZY BASH",
            "§7Launch an enemy skyward then slam them",
            "",
            "§b§l🌪 GROUP BREEZY BASH",
            "§7Send all nearby enemies flying away"
        ));
        registerItem("speed_gem_t2", Material.ECHO_SHARD, 2006, "§d§lSPEED GEM", List.of(
            "§f§lWATCH THE WORLD TURN INTO A BLUR",
            "§a§o(Pristine)",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Speed I & Dolphin's Grace",
            "§7- Efficiency on tools",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l⚡ BLUR",
            "§7Summons successive lightning strikes",
            "§7dealing damage and knockback",
            "",
            "§b§l🌩 SPEED STORM",
            "§7Freezes enemies while granting allies",
            "§7Speed and Haste",
            "",
            "§b§l💨 TERMINAL VELOCITY",
            "§7Speed III + Haste II for a short duration"
        ));
        registerItem("strength_gem_t2", Material.ECHO_SHARD, 2007, "§d§lSTRENGTH GEM", List.of(
            "§f§lHAVE THE STRENGTH OF AN ARMY",
            "§a§o(Pristine)",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Strength I",
            "§7- Sharpness V on weapons",
            "§7- Bloodthorns (more damage at low HP)",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l🚫 NULLIFY",
            "§7Strip all potion effects from a target",
            "",
            "§b§l⚔ FRAILER",
            "§7Clear target's potions, apply Weakness I",
            "§7(20s) and Wither I (40s)",
            "",
            "§b§l🔍 SHADOW STALKER",
            "§7Consume a player head or owned item",
            "§7to track a player's location"
        ));
        registerItem("wealth_gem_t2", Material.ECHO_SHARD, 2008, "§d§lWEALTH GEM", List.of(
            "§f§lFUEL AN EMPIRE",
            "§a§o(Pristine)",
            "",
            "§a🌟 §a§lPASSIVES",
            "§7- Luck & Hero of the Village",
            "§7- Mending, Fortune & Looting on tools",
            "§7- Durability Chip & Armor Mend",
            "",
            "§d🌟 §d§lPOWERS",
            "§b§l💸 UNFORTUNATE",
            "§7Chance to disable enemy actions",
            "",
            "§b§l🎒 POCKETS",
            "§79 extra inventory slots (/bliss pockets)",
            "",
            "§b§l🔒 ITEM LOCK",
            "§7Lock an enemy's held item temporarily",
            "",
            "§b§l✨ AMPLIFICATION",
            "§7Boost all enchantments for 45s",
            "",
            "§b§l🍀 RICH RUSH",
            "§7Increased drop rates for ~3 min"
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
        // Use same key name as DropItemControl for compatibility
        UNDROPPABLE_KEY = new NamespacedKey(plugin, "locked_item");
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

        // Mark gems as locked/undroppable using DropItemControl's PDC key format
        // This prevents gems from being dropped (checked by GemDropListener against config)
        if (GemType.isGem(id)) {
            meta.getPersistentDataContainer().set(UNDROPPABLE_KEY, PersistentDataType.BYTE, (byte) 1);
        }

        // Set display name
        meta.setDisplayName(data.displayName);

        // Set lore and, for Pristine+ gems, append the state badge + enchant glint
        if (GemType.isGem(id)) {
            applyPristinePlusVisuals(meta, data.lore, energy);
        } else if (data.lore != null && !data.lore.isEmpty()) {
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
        applyPristinePlusVisuals(meta, data.lore, energy);
        item.setItemMeta(meta);
    }

    /**
     * Rebuild the gem's lore from its base lore and add a Pristine+ badge line
     * plus an enchantment glint when the energy level is 6 or higher.
     * Glint/badge are removed when the energy drops back below that threshold.
     */
    private static void applyPristinePlusVisuals(ItemMeta meta, List<String> baseLore, int energy) {
        EnergyState state = EnergyState.fromEnergy(energy);
        boolean enhanced = state.isEnhanced();

        List<String> lore = new ArrayList<>();
        if (baseLore != null) {
            lore.addAll(baseLore);
        }
        if (enhanced) {
            if (!lore.isEmpty()) {
                lore.add("");
            }
            lore.add("§5✮ " + state.getDisplayName());
        }
        meta.setLore(lore.isEmpty() ? null : lore);

        if (enhanced) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.removeEnchant(Enchantment.UNBREAKING);
        }
    }

    /**
     * Check if an ItemStack is a custom item
     */
    public static boolean isCustomItem(ItemStack item) {
        return getIdByItem(item) != null;
    }

    /**
     * Mark an existing item as undroppable by adding the locked_item PDC tag.
     * Returns true if the tag was added (item was missing it), false if already tagged or invalid.
     */
    public static boolean markAsUndroppable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (container.has(UNDROPPABLE_KEY, PersistentDataType.BYTE) &&
            container.get(UNDROPPABLE_KEY, PersistentDataType.BYTE) == 1) {
            return false; // Already tagged
        }

        container.set(UNDROPPABLE_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return true;
    }

    /**
     * Check if an item is locked/undroppable (PDC flag)
     * Uses EXACT same logic as DropItemControl's isItemLocked() method
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

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(UNDROPPABLE_KEY, PersistentDataType.BYTE) &&
               container.get(UNDROPPABLE_KEY, PersistentDataType.BYTE) == 1;
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
