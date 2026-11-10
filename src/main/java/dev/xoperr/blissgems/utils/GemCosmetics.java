/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.minimessage.MiniMessage
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.java.JavaPlugin
 */
package dev.xoperr.blissgems.utils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class GemCosmetics {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Map<String, Component> NAMES = new HashMap<String, Component>();
    private static final Map<String, List<Component>> LORE = new HashMap<String, List<Component>>();
    /** Looks registered by addons; kept across cosmetics.yml reloads. */
    private static final Map<String, Component> ADDON_NAMES = new HashMap<String, Component>();
    private static final Map<String, List<Component>> ADDON_LORE = new HashMap<String, List<Component>>();

    private GemCosmetics() {
    }

    public static void initialize(JavaPlugin plugin) {
        NAMES.clear();
        LORE.clear();
        InputStream in = plugin.getResource("cosmetics.yml");
        if (in == null) {
            plugin.getLogger().warning("cosmetics.yml missing from jar; items fall back to legacy text.");
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(in, StandardCharsets.UTF_8));
        ConfigurationSection items = cfg.getConfigurationSection("items");
        if (items == null) {
            return;
        }
        for (String id : items.getKeys(false)) {
            ConfigurationSection sec = items.getConfigurationSection(id);
            if (sec == null) continue;
            try {
                List<String> lore;
                String name = sec.getString("name");
                if (name != null) {
                    NAMES.put(id, MM.deserialize(name));
                }
                if ((lore = sec.getStringList("lore")).isEmpty()) continue;
                ArrayList<Component> rendered = new ArrayList<Component>(lore.size());
                for (String line : lore) {
                    rendered.add(MM.deserialize(line));
                }
                LORE.put(id, rendered);
            }
            catch (RuntimeException ex) {
                plugin.getLogger().warning("Bad cosmetic for '" + id + "': " + ex.getMessage());
            }
        }
        plugin.getLogger().info("Loaded MiniMessage cosmetics for " + NAMES.size() + " items.");
    }

    /**
     * Lets an addon give its gems the same MiniMessage look as the built-in ones (smallcaps name,
     * passives / ability / powers sections). Gem items render it with the live energy line on top.
     */
    public static void register(String itemId, String miniName, List<String> miniLore) {
        if (miniName != null) {
            ADDON_NAMES.put(itemId, MM.deserialize(miniName));
        }
        if (miniLore != null && !miniLore.isEmpty()) {
            ArrayList<Component> rendered = new ArrayList<Component>(miniLore.size());
            for (String line : miniLore) {
                rendered.add(MM.deserialize(line));
            }
            ADDON_LORE.put(itemId, rendered);
        }
    }

    public static boolean has(String itemId) {
        return NAMES.containsKey(itemId) || LORE.containsKey(itemId) || ADDON_NAMES.containsKey(itemId) || ADDON_LORE.containsKey(itemId);
    }

    public static Component name(String itemId) {
        Component name = NAMES.get(itemId);
        return name != null ? name : ADDON_NAMES.get(itemId);
    }

    public static List<Component> lore(String itemId) {
        List<Component> lore = LORE.get(itemId);
        return lore != null ? lore : ADDON_LORE.get(itemId);
    }

    public static boolean apply(ItemMeta meta, String itemId) {
        List<Component> lore;
        boolean applied = false;
        Component name = GemCosmetics.name(itemId);
        if (name != null) {
            meta.displayName(name);
            applied = true;
        }
        if ((lore = GemCosmetics.lore(itemId)) != null) {
            meta.lore(lore);
            applied = true;
        }
        return applied;
    }
}

