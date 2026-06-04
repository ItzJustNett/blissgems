package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.AbilityBinding;
import dev.xoperr.blissgems.utils.AbilitySlot;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player mapping from input gesture (AbilityBinding) to ability slot (AbilitySlot).
 * One-to-one model: each input maps to at most one slot, each slot to at most one input.
 * Setting an input that's already mapped reassigns it (the prior slot becomes unbound).
 * Setting a slot that already has an input frees the prior input.
 *
 * Persisted per-player to playerdata/<uuid>.yml under ability-bindings.<input>: <slot>.
 */
public class AbilityBindingManager {
    private final BlissGems plugin;
    private final Map<UUID, EnumMap<AbilityBinding, AbilitySlot>> cache = new ConcurrentHashMap<>();
    private static final EnumMap<AbilityBinding, AbilitySlot> DEFAULTS = new EnumMap<>(AbilityBinding.class);

    static {
        DEFAULTS.put(AbilityBinding.RIGHT_CLICK, AbilitySlot.PRIMARY);
        DEFAULTS.put(AbilityBinding.SHIFT_RIGHT_CLICK, AbilitySlot.SECONDARY);
        DEFAULTS.put(AbilityBinding.SWAP_HAND, AbilitySlot.TERTIARY);
        DEFAULTS.put(AbilityBinding.SHIFT_SWAP_HAND, AbilitySlot.QUATERNARY);
        // LEFT_CLICK and SHIFT_LEFT_CLICK left unbound by default
    }

    public AbilityBindingManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns the slot bound to the given input for this player,
     * or null if that input is unbound.
     */
    public AbilitySlot getSlot(Player player, AbilityBinding input) {
        return getOrLoad(player.getUniqueId()).get(input);
    }

    /**
     * Returns a snapshot copy of the player's full binding map.
     */
    public EnumMap<AbilityBinding, AbilitySlot> getAll(Player player) {
        return new EnumMap<>(getOrLoad(player.getUniqueId()));
    }

    /**
     * Assigns input → slot for this player. Reassigns if either was already bound elsewhere.
     */
    public void setBinding(Player player, AbilityBinding input, AbilitySlot slot) {
        EnumMap<AbilityBinding, AbilitySlot> map = getOrLoad(player.getUniqueId());
        // Free any other input currently on this slot (1-to-1)
        map.entrySet().removeIf(e -> e.getValue() == slot && e.getKey() != input);
        map.put(input, slot);
        save(player.getUniqueId(), map);
    }

    public void unbind(Player player, AbilityBinding input) {
        EnumMap<AbilityBinding, AbilitySlot> map = getOrLoad(player.getUniqueId());
        map.remove(input);
        save(player.getUniqueId(), map);
    }

    public void resetToDefaults(Player player) {
        EnumMap<AbilityBinding, AbilitySlot> map = new EnumMap<>(DEFAULTS);
        cache.put(player.getUniqueId(), map);
        save(player.getUniqueId(), map);
    }

    public void clearCache(UUID id) {
        cache.remove(id);
    }

    // ---- internals ----

    private EnumMap<AbilityBinding, AbilitySlot> getOrLoad(UUID id) {
        EnumMap<AbilityBinding, AbilitySlot> map = cache.get(id);
        if (map != null) return map;
        map = load(id);
        cache.put(id, map);
        return map;
    }

    private EnumMap<AbilityBinding, AbilitySlot> load(UUID id) {
        File f = playerFile(id);
        if (!f.exists()) {
            return new EnumMap<>(DEFAULTS);
        }
        FileConfiguration data = YamlConfiguration.loadConfiguration(f);
        if (!data.contains("ability-bindings")) {
            return new EnumMap<>(DEFAULTS);
        }
        EnumMap<AbilityBinding, AbilitySlot> map = new EnumMap<>(AbilityBinding.class);
        Map<String, Object> raw = data.getConfigurationSection("ability-bindings").getValues(false);
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            AbilityBinding b = AbilityBinding.fromId(e.getKey());
            AbilitySlot s = AbilitySlot.fromId(String.valueOf(e.getValue()));
            if (b != null && s != null) map.put(b, s);
        }
        return map;
    }

    private void save(UUID id, EnumMap<AbilityBinding, AbilitySlot> map) {
        File f = playerFile(id);
        FileConfiguration data = f.exists()
            ? YamlConfiguration.loadConfiguration(f)
            : new YamlConfiguration();
        // Wipe section and rewrite
        data.set("ability-bindings", null);
        Map<String, String> out = new HashMap<>();
        for (Map.Entry<AbilityBinding, AbilitySlot> e : map.entrySet()) {
            out.put(e.getKey().getId(), e.getValue().getId());
        }
        data.createSection("ability-bindings", out);
        try {
            data.save(f);
        } catch (IOException e) {
            plugin.getLogger().warning("[AbilityBindings] Failed to save for " + id + ": " + e.getMessage());
        }
    }

    private File playerFile(UUID id) {
        File folder = new File(plugin.getDataFolder(), "playerdata");
        if (!folder.exists()) folder.mkdirs();
        return new File(folder, id + ".yml");
    }
}
