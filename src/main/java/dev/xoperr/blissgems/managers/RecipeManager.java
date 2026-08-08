package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages custom crafting recipes for BlissGems items
 * Based on Bliss SMP Season 3 recipes
 */
public class RecipeManager {
    private final BlissGems plugin;
    private final List<NamespacedKey> registeredRecipes = new ArrayList<>();

    public RecipeManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        if (!plugin.getConfig().getBoolean("crafting.enabled", true)) {
            plugin.getLogger().info("Crafting recipes are disabled in config");
            return;
        }

        registerGemFragmentRecipe();
        registerGemTraderRecipe();
        registerRepairKitRecipe();
        registerReviveBeaconRecipe();
        registerRestorationBookRecipe();
        registerPrismaticEdgeRecipe();
        registerGoldGemRecipe();
        registerUpgraderRecipe();

        plugin.getLogger().info("Registered " + registeredRecipes.size() + " custom crafting recipes");
    }

    /**
     * Builds the custom result item for a recipe, logging the standard warning when the
     * item definition is missing. Returns null when the recipe must be skipped.
     */
    private ItemStack requireItem(String itemId, String warningLabel) {
        ItemStack item = CustomItemManager.getItemById(itemId);
        if (item == null) {
            plugin.getLogger().warning("Could not create " + warningLabel + " - recipe not registered");
        }
        return item;
    }

    private ShapedRecipe newRecipe(String keyName, ItemStack result, String... shape) {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, keyName), result);
        recipe.shape(shape);
        return recipe;
    }

    private void register(ShapedRecipe recipe) {
        plugin.getServer().addRecipe(recipe);
        registeredRecipes.add(recipe.getKey());
    }

    /**
     * Gem Fragment Recipe (ingredient for other recipes)
     * Pattern:
     *   D A D
     *   E I E
     *   D A D
     * D = Diamond, A = Amethyst Cluster, E = Emerald, I = Iron Block
     *
     * Creates a custom gem fragment item (prismarine shard with custom PDC)
     */
    private void registerGemFragmentRecipe() {
        ItemStack gemFragment = requireItem("gem_fragment", "gem_fragment item");
        if (gemFragment == null) return;

        // Set amount to 4 (balanced crafting output)
        gemFragment.setAmount(4);

        ShapedRecipe recipe = newRecipe("gem_fragment", gemFragment, "DAD", "EIE", "DAD");
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('A', Material.AMETHYST_CLUSTER);
        recipe.setIngredient('E', Material.EMERALD);
        recipe.setIngredient('I', Material.IRON_BLOCK);

        register(recipe);
    }

    /**
     * Gem Trader Recipe
     * Pattern:
     *   B D B
     *   D S D
     *   B D B
     * B = Diamond Block, D = Dragon's Breath, S = Sculk Catalyst
     */
    private void registerGemTraderRecipe() {
        ItemStack gemTrader = requireItem("gem_trader", "gem_trader item");
        if (gemTrader == null) return;

        ShapedRecipe recipe = newRecipe("gem_trader", gemTrader, "BDB", "DSD", "BDB");
        recipe.setIngredient('B', Material.DIAMOND_BLOCK);
        recipe.setIngredient('D', Material.DRAGON_BREATH);
        recipe.setIngredient('S', Material.SCULK_CATALYST);

        register(recipe);
    }

    /**
     * Repair Kit Recipe
     * Pattern:
     *   F A F
     *   N T N
     *   F A F
     * F = Gem Fragment (Prismarine Shard), A = Anvil, N = Netherite Ingot, T = Netherite Upgrade Template
     *
     * Note: Uses regular prismarine shards as "Gem Fragments"
     * Players should craft gem fragments first, which produces prismarine shards with custom data
     */
    private void registerRepairKitRecipe() {
        ItemStack repairKit = requireItem("repair_kit", "repair_kit item");
        if (repairKit == null) return;

        ShapedRecipe recipe = newRecipe("repair_kit", repairKit, "FAF", "NTN", "FAF");
        recipe.setIngredient('F', Material.PRISMARINE_SHARD); // Prismarine Shard = Gem Fragment
        recipe.setIngredient('A', Material.ANVIL);
        recipe.setIngredient('N', Material.NETHERITE_INGOT);
        recipe.setIngredient('T', Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);

        register(recipe);
    }

    /**
     * Revive Beacon Recipe
     * Pattern:
     *   E T E
     *   T B T
     *   E T E
     * E = Echo Shard, T = Totem of Undying, B = Beacon
     */
    private void registerReviveBeaconRecipe() {
        ItemStack reviveBeacon = requireItem("revive_beacon", "revive_beacon item");
        if (reviveBeacon == null) return;

        ShapedRecipe recipe = newRecipe("revive_beacon", reviveBeacon, "ETE", "TBT", "ETE");
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
        recipe.setIngredient('B', Material.BEACON);

        register(recipe);
    }

    /**
     * Restoration Book Recipe
     * Pattern:
     *   F T F
     *   E N E
     *   F B F
     * F = Gem Fragment (Prismarine Shard), T = Totem of Undying, E = Echo Shard,
     * N = Nether Star, B = Book
     */
    private void registerRestorationBookRecipe() {
        ItemStack restorationBook = requireItem("restoration_book", "restoration_book item");
        if (restorationBook == null) return;

        ShapedRecipe recipe = newRecipe("restoration_book", restorationBook, "FTF", "ENE", "FBF");
        recipe.setIngredient('F', Material.PRISMARINE_SHARD); // Prismarine Shard = Gem Fragment
        recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('N', Material.NETHER_STAR);
        recipe.setIngredient('B', Material.BOOK);

        register(recipe);
    }

    /**
     * Prismatic Edge Recipe
     * Pattern:
     *   . F .
     *   . S .
     *   E N E
     * F = Gem Fragment (Prismarine Shard), S = Netherite Sword, E = Echo Shard, N = Nether Star
     */
    private void registerPrismaticEdgeRecipe() {
        ItemStack prismaticEdge = CustomItemManager.getItemById("prismatic_edge");
        if (prismaticEdge == null) {
            plugin.getLogger().warning("Could not create prismatic_edge item - recipe not registered");
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, "prismatic_edge");
        ShapedRecipe recipe = new ShapedRecipe(key, prismaticEdge);

        recipe.shape(" F ", " S ", "ENE");
        recipe.setIngredient('F', Material.PRISMARINE_SHARD); // Prismarine Shard = Gem Fragment
        recipe.setIngredient('S', Material.NETHERITE_SWORD);
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('N', Material.NETHER_STAR);

        plugin.getServer().addRecipe(recipe);
        registeredRecipes.add(key);
    }

    /**
     * Gold Gem summon
     * Pattern:
     *   W W W
     *   W C W
     *   W   W
     * W = Wire Fragment, C = Fragment Core - seven fragments bound by the core.
     *
     * Unlike the other recipes this one matches on the exact custom items rather than their
     * base materials: a gem this powerful must not fall out of seven plain lightning rods.
     */
    private void registerGoldGemRecipe() {
        if (!plugin.getConfig().getBoolean("gold.summon.recipe-enabled", true)) {
            return;
        }
        ItemStack goldGem = CustomItemManager.getItemById("gold_gem_t1");
        ItemStack wire = CustomItemManager.getItemById("wire_fragment");
        ItemStack core = CustomItemManager.getItemById("fragment_core");
        if (goldGem == null || wire == null || core == null) {
            plugin.getLogger().warning("Could not create Gold Gem components - summon recipe not registered");
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, "gold_gem");
        ShapedRecipe recipe = new ShapedRecipe(key, goldGem);

        recipe.shape("WWW", "WCW", "W W");
        recipe.setIngredient('W', new org.bukkit.inventory.RecipeChoice.ExactChoice(wire));
        recipe.setIngredient('C', new org.bukkit.inventory.RecipeChoice.ExactChoice(core));

        plugin.getServer().addRecipe(recipe);
        registeredRecipes.add(key);
    }

    /**
     * Universal Gem Upgrader Recipe (works for ALL gem types)
     * Pattern:
     *   B B B
     *   B S B
     *   B B B
     * B = Diamond Block, S = Nether Star
     */
    private void registerUpgraderRecipe() {
        ItemStack upgrader = CustomItemManager.getItemById("gem_upgrader");
        if (upgrader == null) {
            plugin.getLogger().warning("Could not create gem_upgrader - recipe not registered");
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, "gem_upgrader");
        ShapedRecipe recipe = new ShapedRecipe(key, upgrader);

        recipe.shape("BBB", "BSB", "BBB");
        recipe.setIngredient('B', Material.DIAMOND_BLOCK);
        recipe.setIngredient('S', Material.NETHER_STAR);

        plugin.getServer().addRecipe(recipe);
        registeredRecipes.add(key);
    }

    /**
     * Unregisters all recipes (called on plugin disable)
     */
    public void unregisterRecipes() {
        for (NamespacedKey key : registeredRecipes) {
            plugin.getServer().removeRecipe(key);
        }
        registeredRecipes.clear();
    }
}
