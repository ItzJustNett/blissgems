package dev.xoperr.blissgems.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Default values that changed between versions. config.yml auto-repair only adds missing keys, so
 * an existing server would keep the old defaults forever; each migration here runs once (its id is
 * remembered in {@code config-migrations}) and changes a value only while it still equals the old
 * default - anything an admin set themselves is left alone.
 */
public final class ConfigMigrations {
    public static final String KEY = "config-migrations";

    record Change(String path, double oldDefault, double newDefault) {}

    static final Map<String, List<Change>> ALL = new LinkedHashMap<>();

    static {
        // Fire, Flux and Speed were far behind the other gems
        ALL.put("2026-10-buff-fire-flux-speed", List.of(
            new Change("abilities.cooldowns.fire-fireball", 45.0, 35.0),
            new Change("abilities.damage.fire-campfire", 2.0, 3.0),
            new Change("abilities.fire-campfire.radius", 4.0, 5.0),
            new Change("abilities.fire-campfire.heal-amount", 0.4, 1.0),
            new Change("abilities.cooldowns.fire-campfire", 120.0, 90.0),
            new Change("abilities.cooldowns.fire-crisp", 90.0, 60.0),
            new Change("abilities.durations.fire-crisp", 15.0, 20.0),
            new Change("abilities.damage.fire-meteor-shower", 8.0, 10.0),
            new Change("abilities.cooldowns.fire-meteor-shower", 120.0, 90.0),
            new Change("abilities.durations.fire-meteor-shower", 8.0, 10.0),
            new Change("abilities.cooldowns.flux-beam", 240.0, 150.0),
            new Change("abilities.damage.flux-beam-base", 10.0, 14.0),
            new Change("abilities.durations.flux-ground-freeze", 3.0, 4.0),
            new Change("abilities.durations.flux-flashbang", 5.0, 6.0),
            new Change("abilities.flux-kinetic-burst.knockback", 2.0, 2.6),
            new Change("abilities.flux-kinetic-burst.radius", 5.0, 6.0),
            new Change("passives.flux.shocking-chance", 0.15, 0.25),
            new Change("passives.flux.tier2.shocking-arrow-damage", 3.0, 4.0),
            new Change("abilities.cooldowns.speed-storm", 135.0, 90.0),
            new Change("abilities.speed-storm.damage", 2.0, 3.0),
            new Change("abilities.durations.speed-storm", 10.0, 12.0),
            new Change("abilities.damage.blur", 2.5, 3.5),
            new Change("abilities.durations.terminal-velocity", 10.0, 15.0),
            new Change("abilities.cooldowns.speed-terminal", 60.0, 50.0),
            new Change("abilities.cooldowns.speed-gale-clouds", 60.0, 45.0)));
    }

    private ConfigMigrations() {
    }

    /** Applies every migration not applied yet; returns the paths whose value changed. */
    public static List<String> apply(ConfigurationSection cfg) {
        List<String> done = new ArrayList<>(cfg.getStringList(KEY));
        List<String> changed = new ArrayList<>();
        for (Map.Entry<String, List<Change>> m : ALL.entrySet()) {
            if (done.contains(m.getKey())) continue;
            for (Change c : m.getValue()) {
                if (!cfg.isSet(c.path()) || !(cfg.get(c.path()) instanceof Number n)) continue;
                if (Math.abs(n.doubleValue() - c.oldDefault()) > 1e-9) continue;
                boolean whole = n instanceof Integer || n instanceof Long;
                cfg.set(c.path(), whole ? (Object) (int) Math.round(c.newDefault()) : (Object) c.newDefault());
                changed.add(c.path());
            }
            done.add(m.getKey());
        }
        cfg.set(KEY, done);
        return changed;
    }
}
