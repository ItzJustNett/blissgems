package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemDefinition;
import dev.xoperr.blissgems.api.GemRarity;
import dev.xoperr.blissgems.api.event.GemRollEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * The single place a random gem is rolled. Picks a rarity by weight, then a gem of that rarity
 * uniformly, then maybe mutates it, then lets addons change the result via {@link GemRollEvent}.
 */
public class GemRollManager {
    private final BlissGems plugin;

    public GemRollManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    /**
     * Rolls a gem for the player. {@code tier} is the tier the gem will be given at; mutations
     * that do not reach that tier are skipped. Returns null when nothing can be rolled or a
     * listener cancelled the roll.
     */
    public String roll(Player player, GemRollEvent.Reason reason, int tier, Collection<String> exclude) {
        List<String> pool = new ArrayList<String>(this.plugin.getGemManager().getAvailableGemIds());
        if (exclude != null) {
            pool.removeAll(exclude);
        }
        String base = this.pickWeighted(pool);
        if (base == null) {
            return null;
        }
        String mutation = this.maybeMutate(base, tier);
        String rolled = mutation != null ? mutation : base;
        GemRollEvent event = new GemRollEvent(player, reason, rolled, tier, mutation != null);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getGemId() == null) {
            return null;
        }
        if (this.isMutation(event.getGemId())) {
            this.announceMutation(player, event.getGemId());
        }
        return event.getGemId();
    }

    public GemRarity getRarity(String gemId) {
        GemRarity override = GemRarity.fromString(this.plugin.getConfig().getString("gems.rarity." + gemId));
        if (override != null) {
            return override;
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        GemDefinition def = registry != null ? registry.getGem(gemId) : null;
        return def != null ? def.getRarity() : GemRarity.COMMON;
    }

    private String pickWeighted(List<String> pool) {
        if (pool.isEmpty()) {
            return null;
        }
        EnumMap<GemRarity, List<String>> buckets = new EnumMap<GemRarity, List<String>>(GemRarity.class);
        for (String id : pool) {
            buckets.computeIfAbsent(this.getRarity(id), r -> new ArrayList<String>()).add(id);
        }
        FileConfiguration config = this.plugin.getConfig();
        double total = 0.0;
        EnumMap<GemRarity, Double> weights = new EnumMap<GemRarity, Double>(GemRarity.class);
        for (Map.Entry<GemRarity, List<String>> e : buckets.entrySet()) {
            double w = Math.max(0.0, config.getDouble("gems.rarity-weights." + e.getKey().name().toLowerCase(), GemRollManager.defaultWeight(e.getKey())));
            weights.put(e.getKey(), w);
            total += w;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (total <= 0.0) {
            return pool.get(random.nextInt(pool.size()));
        }
        double r = random.nextDouble(total);
        for (Map.Entry<GemRarity, Double> e : weights.entrySet()) {
            r -= e.getValue();
            if (r >= 0.0) continue;
            List<String> bucket = buckets.get(e.getKey());
            return bucket.get(random.nextInt(bucket.size()));
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private String maybeMutate(String baseId, int tier) {
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        if (registry == null) {
            return null;
        }
        List<String> excluded = this.plugin.getConfig().getStringList("gems.exclude-from-random");
        ArrayList<String> mutations = new ArrayList<String>();
        for (GemDefinition def : registry.getMutationsOf(baseId)) {
            if (def.getMaxTier() < tier || excluded.contains(def.getId())) continue;
            mutations.add(def.getId());
        }
        if (mutations.isEmpty()) {
            return null;
        }
        FileConfiguration config = this.plugin.getConfig();
        double chance = config.getDouble("gems.mutation-chance-per-gem." + baseId, config.getDouble("gems.mutation-chance", 0.05));
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() >= chance) {
            return null;
        }
        return mutations.get(random.nextInt(mutations.size()));
    }

    private boolean isMutation(String gemId) {
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        GemDefinition def = registry != null ? registry.getGem(gemId) : null;
        return def != null && def.isMutation();
    }

    private void announceMutation(Player player, String gemId) {
        GemManager gems = this.plugin.getGemManager();
        String name = gems.getGemColorCode(gemId) + "§l" + gems.getGemDisplayName(gemId);
        player.sendTitle("§b§lMUTATION!", "§7Your gem mutated into " + name, 10, 60, 20);
        player.sendMessage("§b§l» MUTATION! §fYour gem mutated into " + name + "§f!");
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.8f, 1.4f);
    }

    private static double defaultWeight(GemRarity rarity) {
        return switch (rarity) {
            case COMMON -> 100.0;
            case RARE -> 40.0;
            case MYTHIC -> 8.0;
            case LEGENDARY -> 2.0;
        };
    }
}
