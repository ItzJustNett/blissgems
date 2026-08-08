package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Registers BlissGems' crafting recipes from recipes.yml.
 *
 * Every recipe — shape, ingredients, output amount, whether it exists at all — is data.
 * Server owners retune crafts by editing that file; nothing here is hard-coded beyond the
 * defaults shipped in the jar, which are written out on first run.
 */
public class RecipeManager {

    /** Prefix marking an ingredient as a BlissGems custom item rather than a material. */
    private static final String CUSTOM_PREFIX = "blissgems:";

    private final BlissGems plugin;
    private final List<NamespacedKey> registeredRecipes = new ArrayList<>();

    public RecipeManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers every enabled recipe in recipes.yml.
     *
     * The legacy {@code crafting.enabled} switch in config.yml still turns the whole system
     * off, so servers that had crafting disabled stay that way after the upgrade.
     */
    public void registerRecipes() {
        if (!plugin.getConfig().getBoolean("crafting.enabled", true)) {
            plugin.getLogger().info("Crafting recipes are disabled in config");
            return;
        }
        FileConfiguration recipes = loadRecipeFile();
        if (!recipes.getBoolean("enabled", true)) {
            plugin.getLogger().info("Crafting recipes are disabled in recipes.yml");
            return;
        }
        ConfigurationSection section = recipes.getConfigurationSection("recipes");
        if (section == null) {
            plugin.getLogger().warning("recipes.yml has no 'recipes' section - no crafts registered");
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection recipe = section.getConfigurationSection(key);
            if (recipe == null || !recipe.getBoolean("enabled", true)) {
                continue;
            }
            // Predates recipes.yml, and servers that turned the Gold Gem craft off there
            // must stay turned off after the upgrade.
            if ("gold_gem".equals(key) && !plugin.getConfig().getBoolean("gold.summon.recipe-enabled", true)) {
                continue;
            }
            register(key, recipe);
        }
        plugin.getLogger().info("Registered " + registeredRecipes.size() + " custom crafting recipes");
    }

    /** Load recipes.yml, writing the packaged defaults out the first time. */
    private FileConfiguration loadRecipeFile() {
        File file = new File(plugin.getDataFolder(), "recipes.yml");
        if (!file.exists()) {
            plugin.saveResource("recipes.yml", false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void register(String key, ConfigurationSection recipe) {
        String resultId = recipe.getString("result");
        if (resultId == null) {
            plugin.getLogger().warning("Recipe '" + key + "' has no result - skipped");
            return;
        }
        ItemStack result = CustomItemManager.getItemById(resultId);
        if (result == null) {
            plugin.getLogger().warning("Recipe '" + key + "' produces unknown item '" + resultId + "' - skipped");
            return;
        }
        result.setAmount(Math.max(1, recipe.getInt("amount", 1)));

        List<String> shape = recipe.getStringList("shape");
        if (shape.isEmpty() || shape.size() > 3) {
            plugin.getLogger().warning("Recipe '" + key + "' needs a shape of 1-3 rows - skipped");
            return;
        }

        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        ShapedRecipe shaped = new ShapedRecipe(namespacedKey, result);
        try {
            shaped.shape(shape.toArray(new String[0]));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Recipe '" + key + "' has an invalid shape: " + e.getMessage());
            return;
        }

        ConfigurationSection ingredients = recipe.getConfigurationSection("ingredients");
        if (ingredients == null) {
            plugin.getLogger().warning("Recipe '" + key + "' has no ingredients - skipped");
            return;
        }
        for (String symbol : ingredients.getKeys(false)) {
            if (symbol.length() != 1) {
                plugin.getLogger().warning("Recipe '" + key + "': ingredient key '" + symbol
                    + "' must be a single character - skipped");
                return;
            }
            if (!setIngredient(key, shaped, symbol.charAt(0), ingredients.getString(symbol))) {
                return;
            }
        }

        try {
            plugin.getServer().addRecipe(shaped);
        } catch (IllegalStateException e) {
            // A recipe key that survived a reload is already registered; leave the live one.
            plugin.getLogger().warning("Recipe '" + key + "' is already registered - skipped");
            return;
        }
        registeredRecipes.add(namespacedKey);
    }

    /**
     * Resolve one ingredient onto the recipe. Returns false (having logged why) if the
     * ingredient can't be resolved, so the caller abandons the whole recipe rather than
     * registering a craft that is missing a slot.
     */
    private boolean setIngredient(String key, ShapedRecipe recipe, char symbol, String value) {
        if (value == null || value.isBlank()) {
            plugin.getLogger().warning("Recipe '" + key + "': ingredient '" + symbol + "' has no value");
            return false;
        }
        if (value.toLowerCase().startsWith(CUSTOM_PREFIX)) {
            String itemId = value.substring(CUSTOM_PREFIX.length());
            ItemStack custom = CustomItemManager.getItemById(itemId);
            if (custom == null) {
                plugin.getLogger().warning("Recipe '" + key + "': unknown BlissGems item '" + itemId + "'");
                return false;
            }
            recipe.setIngredient(symbol, new RecipeChoice.ExactChoice(custom));
            return true;
        }
        Material material = Material.matchMaterial(value.toUpperCase());
        if (material == null) {
            plugin.getLogger().warning("Recipe '" + key + "': unknown material '" + value + "'");
            return false;
        }
        recipe.setIngredient(symbol, material);
        return true;
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
