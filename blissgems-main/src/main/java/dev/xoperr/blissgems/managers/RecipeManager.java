package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        registerRecipe("gem_fragment", new String[]{"DAD", "EIE", "DAD"}, 
            Map.of('D', Material.DIAMOND, 'A', Material.AMETHYST_CLUSTER, 'E', Material.EMERALD, 'I', Material.IRON_BLOCK));

        // Register special items
        registerRecipe("gem_trader", new String[]{"BDB", "DSD", "BDB"}, 
            Map.of('B', Material.DIAMOND_BLOCK, 'D', Material.DRAGON_BREATH, 'S', Material.SCULK_CATALYST));
            
        registerRecipe("repair_kit", new String[]{"FAF", "NTN", "FAF"}, 
            Map.of('F', Material.PRISMARINE_SHARD, 'A', Material.ANVIL, 'N', Material.NETHERITE_INGOT, 'T', Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            
        registerRecipe("revive_beacon", new String[]{"ETE", "TBT", "ETE"}, 
            Map.of('E', Material.ECHO_SHARD, 'T', Material.TOTEM_OF_UNDYING, 'B', Material.BEACON));

        // Register universal upgrader (works for all gem types)
        registerRecipe("gem_upgrader", new String[]{"BBB", "BSB", "BBB"}, 
            Map.of('B', Material.DIAMOND_BLOCK, 'S', Material.NETHER_STAR));

        plugin.getLogger().info("Registered " + registeredRecipes.size() + " custom crafting recipes");
    }

    private void registerRecipe(String itemId, String[] defaultShape, Map<Character, Material> defaultIngredients) {
        ItemStack item = CustomItemManager.getItemById(itemId);
        if (item == null) {
            plugin.getLogger().warning("Could not create " + itemId + " item - recipe not registered");
            return;
        }

        if (itemId.equals("gem_fragment")) {
            item.setAmount(4);
        }

        NamespacedKey key = new NamespacedKey(plugin, itemId);
        ShapedRecipe recipe = new ShapedRecipe(key, item);

        String path = "crafting." + itemId;
        if (!plugin.getConfig().contains(path)) {
            recipe.shape(defaultShape[0], defaultShape[1], defaultShape[2]);
            for (Map.Entry<Character, Material> entry : defaultIngredients.entrySet()) {
                recipe.setIngredient(entry.getKey(), entry.getValue());
            }
        } else {
            List<String> shapeList = plugin.getConfig().getStringList(path + ".shape");
            if (shapeList.size() != 3) {
                plugin.getLogger().warning("Invalid shape for " + itemId + " in config.yml. Using default.");
                recipe.shape(defaultShape[0], defaultShape[1], defaultShape[2]);
            } else {
                recipe.shape(shapeList.get(0), shapeList.get(1), shapeList.get(2));
            }

            ConfigurationSection ingredients = plugin.getConfig().getConfigurationSection(path + ".ingredients");
            if (ingredients == null) {
                plugin.getLogger().warning("Missing ingredients for " + itemId + " in config.yml. Using default.");
                for (Map.Entry<Character, Material> entry : defaultIngredients.entrySet()) {
                    recipe.setIngredient(entry.getKey(), entry.getValue());
                }
            } else {
                for (String keyStr : ingredients.getKeys(false)) {
                    if (keyStr.length() != 1) continue;
                    char c = keyStr.charAt(0);
                    String matName = ingredients.getString(keyStr);
                    if (matName != null) {
                        try {
                            Material mat = Material.valueOf(matName.toUpperCase());
                            recipe.setIngredient(c, mat);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid material " + matName + " in recipe " + itemId);
                            return; // Fail safe
                        }
                    }
                }
            }
        }

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
