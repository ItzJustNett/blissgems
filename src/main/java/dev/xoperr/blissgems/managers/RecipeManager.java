/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.Recipe
 *  org.bukkit.inventory.RecipeChoice
 *  org.bukkit.inventory.RecipeChoice$ExactChoice
 *  org.bukkit.inventory.ShapedRecipe
 *  org.bukkit.plugin.Plugin
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class RecipeManager {
    private static final String CUSTOM_PREFIX = "blissgems:";
    private final BlissGems plugin;
    private final List<NamespacedKey> registeredRecipes = new ArrayList<NamespacedKey>();

    public RecipeManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        if (!this.plugin.getConfig().getBoolean("crafting.enabled", true)) {
            this.plugin.getLogger().info("Crafting recipes are disabled in config");
            return;
        }
        FileConfiguration recipes = this.loadRecipeFile();
        if (!recipes.getBoolean("enabled", true)) {
            this.plugin.getLogger().info("Crafting recipes are disabled in recipes.yml");
            return;
        }
        ConfigurationSection section = recipes.getConfigurationSection("recipes");
        if (section == null) {
            this.plugin.getLogger().warning("recipes.yml has no 'recipes' section - no crafts registered");
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection recipe = section.getConfigurationSection(key);
            if (recipe == null || !recipe.getBoolean("enabled", true) || "gold_gem".equals(key) && !this.plugin.getConfig().getBoolean("gold.summon.recipe-enabled", true)) continue;
            try {
                this.register(key, recipe);
            }
            catch (RuntimeException e) {
                this.plugin.getLogger().warning("Recipe '" + key + "' could not be registered: " + e.getMessage());
            }
        }
        this.plugin.getLogger().info("Registered " + this.registeredRecipes.size() + " custom crafting recipes");
    }

    private FileConfiguration loadRecipeFile() {
        File file = new File(this.plugin.getDataFolder(), "recipes.yml");
        if (!file.exists()) {
            this.plugin.saveResource("recipes.yml", false);
        }
        return YamlConfiguration.loadConfiguration((File)file);
    }

    private void register(String key, ConfigurationSection recipe) {
        String resultId = recipe.getString("result");
        if (resultId == null) {
            this.plugin.getLogger().warning("Recipe '" + key + "' has no result - skipped");
            return;
        }
        ItemStack result = CustomItemManager.getItemById(resultId);
        if (result == null) {
            this.plugin.getLogger().warning("Recipe '" + key + "' produces unknown item '" + resultId + "' - skipped");
            return;
        }
        result.setAmount(Math.max(1, recipe.getInt("amount", 1)));
        List<String> shape = recipe.getStringList("shape");
        if (shape.isEmpty() || shape.size() > 3) {
            this.plugin.getLogger().warning("Recipe '" + key + "' needs a shape of 1-3 rows - skipped");
            return;
        }
        NamespacedKey namespacedKey = new NamespacedKey((Plugin)this.plugin, key);
        ShapedRecipe shaped = new ShapedRecipe(namespacedKey, result);
        try {
            shaped.shape(shape.toArray(new String[0]));
        }
        catch (IllegalArgumentException e) {
            this.plugin.getLogger().warning("Recipe '" + key + "' has an invalid shape: " + e.getMessage());
            return;
        }
        ConfigurationSection ingredients = recipe.getConfigurationSection("ingredients");
        if (ingredients == null) {
            this.plugin.getLogger().warning("Recipe '" + key + "' has no ingredients - skipped");
            return;
        }
        HashSet<Character> unfilled = new HashSet<Character>();
        for (String row : shape) {
            for (char slot : row.toCharArray()) {
                if (slot == ' ') continue;
                unfilled.add(Character.valueOf(slot));
            }
        }
        for (String symbol : ingredients.getKeys(false)) {
            if (symbol.length() != 1) {
                this.plugin.getLogger().warning("Recipe '" + key + "': ingredient key '" + symbol + "' must be a single character - skipped");
                return;
            }
            if (!unfilled.remove(Character.valueOf(symbol.charAt(0)))) {
                this.plugin.getLogger().warning("Recipe '" + key + "': ingredient '" + symbol + "' does not appear in the shape - ignored");
                continue;
            }
            if (this.setIngredient(key, shaped, symbol.charAt(0), ingredients.getString(symbol))) continue;
            return;
        }
        if (!unfilled.isEmpty()) {
            this.plugin.getLogger().warning("Recipe '" + key + "': the shape uses " + String.valueOf(unfilled) + " with no matching ingredient - skipped");
            return;
        }
        try {
            this.plugin.getServer().addRecipe((Recipe)shaped);
        }
        catch (IllegalStateException e) {
            this.plugin.getLogger().warning("Recipe '" + key + "' is already registered - skipped");
            return;
        }
        catch (IllegalArgumentException e) {
            this.plugin.getLogger().warning("Recipe '" + key + "' was rejected by the server: " + e.getMessage());
            return;
        }
        this.registeredRecipes.add(namespacedKey);
    }

    private boolean setIngredient(String key, ShapedRecipe recipe, char symbol, String value) {
        if (value == null || value.isBlank()) {
            this.plugin.getLogger().warning("Recipe '" + key + "': ingredient '" + symbol + "' has no value");
            return false;
        }
        if (value.toLowerCase().startsWith(CUSTOM_PREFIX)) {
            String itemId = value.substring(CUSTOM_PREFIX.length());
            ItemStack custom = CustomItemManager.getItemById(itemId);
            if (custom == null) {
                this.plugin.getLogger().warning("Recipe '" + key + "': unknown BlissGems item '" + itemId + "'");
                return false;
            }
            recipe.setIngredient(symbol, (RecipeChoice)new RecipeChoice.ExactChoice(custom));
            return true;
        }
        Material material = Material.matchMaterial((String)value.toUpperCase());
        if (material == null) {
            this.plugin.getLogger().warning("Recipe '" + key + "': unknown material '" + value + "'");
            return false;
        }
        recipe.setIngredient(symbol, material);
        return true;
    }

    public void unregisterRecipes() {
        for (NamespacedKey key : this.registeredRecipes) {
            this.plugin.getServer().removeRecipe(key);
        }
        this.registeredRecipes.clear();
    }
}

