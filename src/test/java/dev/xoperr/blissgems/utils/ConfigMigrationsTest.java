package dev.xoperr.blissgems.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** Changed defaults reach existing configs once, without touching values an admin set. */
class ConfigMigrationsTest {
    private static YamlConfiguration shipped() {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
            ConfigMigrationsTest.class.getClassLoader().getResourceAsStream("config.yml"), StandardCharsets.UTF_8));
    }

    @Test
    void oldDefaultsMoveToTheNewOnesCustomValuesStay() {
        YamlConfiguration old = new YamlConfiguration();
        old.set("abilities.cooldowns.flux-beam", 240);          // old default -> 150
        old.set("abilities.damage.blur", 2.5);                   // old default -> 3.5
        old.set("abilities.cooldowns.speed-storm", 100);         // admin's own value -> stays
        List<String> changed = ConfigMigrations.apply(old);
        assertEquals(150, old.get("abilities.cooldowns.flux-beam"), "an int stays an int");
        assertEquals(3.5, old.getDouble("abilities.damage.blur"));
        assertEquals(100, old.getInt("abilities.cooldowns.speed-storm"));
        assertEquals(List.of("abilities.cooldowns.flux-beam", "abilities.damage.blur"), changed);
        assertTrue(old.getStringList(ConfigMigrations.KEY).contains("2026-10-buff-fire-flux-speed"));
        // runs only once: going back to the old value by hand is respected
        old.set("abilities.cooldowns.flux-beam", 240);
        assertTrue(ConfigMigrations.apply(old).isEmpty());
        assertEquals(240, old.getInt("abilities.cooldowns.flux-beam"));
    }

    @Test
    void shippedConfigAlreadyHasEveryNewDefault() {
        YamlConfiguration cfg = shipped();
        for (List<ConfigMigrations.Change> changes : ConfigMigrations.ALL.values()) {
            for (ConfigMigrations.Change c : changes) {
                assertEquals(c.newDefault(), cfg.getDouble(c.path()), 1e-9, c.path() + " in config.yml");
            }
        }
        // and must not say "already applied", or auto-repair would copy that into old configs
        assertTrue(cfg.getStringList(ConfigMigrations.KEY).isEmpty(), "config-migrations must ship empty");
    }
}
