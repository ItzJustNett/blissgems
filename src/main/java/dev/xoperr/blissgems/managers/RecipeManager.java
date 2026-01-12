package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
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

    /**
     * Registers all crafting recipes
     */
    public void registerRecipes() {
        // Check if recipes are enabled in config
        if (!plugin.getConfig().getBoolean("crafting.enabled", true)) {
            plugin.getLogger().info("Crafting recipes are disabled in config");
            return;
        }

        // Register base items first
        registerGemFragmentRecipe();

        // Register special items
        registerGemTraderRecipe();
        registerRepairKitRecipe();
        registerReviveBeaconRecipe();

        // Register universal upgrader (works for all gem types)
        registerUpgraderRecipe();

        plugin.getLogger().info("Registered " + registeredRecipes.size() + " custom crafting recipes");
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
        ItemStack gemFragment = CustomItemManager.getItemById("gem_fragment");
        if (gemFragment == null) {
            plugin.getLogger().warning("Could not create gem_fragment item - recipe not registered");
            return;
        }

        // Set amount to 4 (balanced crafting output)
        gemFragment.setAmount(4);

        NamespacedKey key = new NamespacedKey(plugin, "gem_fragment");
        ShapedRecipe recipe = new ShapedRecipe(key, gemFragment);

        recipe.shape("DAD", "EIE", "DAD");
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('A', Material.AMETHYST_CLUSTER);
        recipe.setIngredient('E', Material.EMERALD);
        recipe.setIngredient('I', Material.IRON_BLOCK);

        plugin.getServer().addRecipe(recipe);
        registeredRecipes.add(key);
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
        ItemStack gemTrader = CustomItemManager.getItemById("gem_trader");
        if (gemTrader == null) {
            plugin.getLogger().warning("Could not create gem_trader item - recipe not registered");
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, "gem_trader");
        ShapedRecipe recipe = new ShapedRecipe(key, gemTrader);

        recipe.shape("BDB", "DSB", "BDB");
        recipe.setIngredient('B', Material.DIAMOND_BLOCK);
        recipe.setIngredient('D', Material.DRAGON_BREATH);
        recipe.setIngredient('S', Material.SCULK_CATALYST);

        plugin.getServer().addRecipe(recipe);
        registeredRecipes.add(key);
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
        ItemStack repairKit = CustomItemManager.getItemById("repair_kit");
        if (repairKit == null) {
            plugin.getLogger().warning("Could not create repair_kit item - recipe not registered");
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, "repair_kit");
        ShapedRecipe recipe = new ShapedRecipe(key, repairKit);

        recipe.shape("FAF", "NTN", "FAF");
        recipe.setIngredient('F', Material.PRISMARINE_SHARD); // Prismarine Shard = Gem Fragment
        recipe.setIngredient('A', Material.ANVIL);
        recipe.setIngredient('N', Material.NETHERITE_INGOT);
        recipe.setIngredient('T', Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);

        plugin.getServer().addRecipe(recipe);
        registeredRecipes.add(key);
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
        ItemStack reviveBeacon = CustomItemManager.getItemById("revive_beacon");
        if (reviveBeacon == null) {
            plugin.getLogger().warning("Could not create revive_beacon item - recipe not registered");
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, "revive_beacon");
        ShapedRecipe recipe = new ShapedRecipe(key, reviveBeacon);

        recipe.shape("ETE", "TBT", "ETE");
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
        recipe.setIngredient('B', Material.BEACON);

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
