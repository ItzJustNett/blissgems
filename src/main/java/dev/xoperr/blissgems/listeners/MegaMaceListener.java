package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;

/**
 * The three triad maces (Heavy, Tempered, Overcharged) anywhere in a crafting grid, and nothing
 * else, forge the Mega Mace. It keeps the best level of every enchantment the three carried.
 * The registered recipe takes any three maces (exact ingredients would reject enchanted or worn
 * ones); this listener only lets it through for the triad. A real recipe is needed: without one
 * the server hands the ingredients back when the result is taken.
 * mega-mace.max-crafts caps how many are ever forged; the count lives in mega_mace.yml.
 */
public class MegaMaceListener implements Listener {
    private static final Set<String> TRIAD = Set.of("mace_1", "mace_limited", "mace_3");
    private final BlissGems plugin;
    private final NamespacedKey key;
    private final File file;
    private int crafted;

    public MegaMaceListener(BlissGems plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "mega_mace");
        this.file = new File(plugin.getDataFolder(), "mega_mace.yml");
        this.crafted = YamlConfiguration.loadConfiguration(this.file).getInt("crafted", 0);
        ItemStack mega = CustomItemManager.getItemById("mega_mace");
        if (mega != null && Bukkit.getRecipe(this.key) == null) {
            ShapelessRecipe recipe = new ShapelessRecipe(this.key, mega);
            recipe.addIngredient(3, Material.MACE);
            Bukkit.addRecipe(recipe);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepare(PrepareItemCraftEvent event) {
        if (!(event.getRecipe() instanceof Keyed keyed) || !this.key.equals(keyed.getKey())) return;
        CraftingInventory grid = event.getInventory();
        grid.setResult(null);
        if (!this.plugin.getConfig().getBoolean("mega-mace.enabled", true)) return;
        Set<String> found = new HashSet<>();
        Map<Enchantment, Integer> best = new HashMap<>();
        for (ItemStack item : grid.getMatrix()) {
            if (item == null || item.getType().isAir()) continue;
            String id = CustomItemManager.getIdByItem(item);
            if (id == null || !TRIAD.contains(id) || !found.add(id)) return;
            for (Map.Entry<Enchantment, Integer> e : item.getEnchantments().entrySet()) best.merge(e.getKey(), e.getValue(), Math::max);
        }
        if (!found.equals(TRIAD)) return;
        if (this.limitReached()) {
            event.getViewers().forEach(viewer -> viewer.sendMessage("\u00a7cNo more Mega Maces can be forged on this server."));
            return;
        }
        ItemStack mega = CustomItemManager.getItemById("mega_mace");
        if (mega == null) return;
        mega.addUnsafeEnchantments(best);
        grid.setResult(mega);
    }

    private boolean limitReached() {
        int max = this.plugin.getConfig().getInt("mega-mace.max-crafts", -1);
        return max >= 0 && this.crafted >= max;
    }

    private boolean isMegaCraft(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();
        return event.getRecipe() instanceof Keyed keyed && this.key.equals(keyed.getKey()) && result != null && !result.getType().isAir();
    }

    /** Two grids prepared at once must not both get past the cap. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!this.isMegaCraft(event)) return;
        if (this.limitReached()) {
            event.setCancelled(true);
            event.getInventory().setResult(null);
        } else if (event.isShiftClick() && event.getWhoClicked().getInventory().firstEmpty() == -1) {
            // a shift-click into a full inventory forges nothing, so it must not be counted either
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCrafted(CraftItemEvent event) {
        if (!this.isMegaCraft(event)) return;
        this.crafted++;
        YamlConfiguration data = new YamlConfiguration();
        data.set("crafted", this.crafted);
        try {
            data.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Could not save mega_mace.yml: " + e.getMessage());
        }
    }
}
