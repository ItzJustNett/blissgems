package dev.xoperr.blissgems.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.ConfigManager;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the real ConfigManager against the shipped config.yml, changes values, and checks the
 * plugin sees the new values - i.e. that the numbers really come from the config.
 */
class ConfigManagerTest {
    @TempDir
    Path dataFolder;

    private YamlConfiguration yaml;
    private BlissGems plugin;
    private ConfigManager manager;

    @BeforeEach
    void setUp() {
        this.yaml = YamlConfiguration.loadConfiguration(ConfigIndex.CONFIG.toFile());
        this.plugin = mock(BlissGems.class);
        when(this.plugin.getConfig()).thenReturn(this.yaml);
        // no config.yml in the data folder: the auto-repair step has nothing to do
        when(this.plugin.getDataFolder()).thenReturn(this.dataFolder.toFile());
        this.manager = new ConfigManager(this.plugin);
    }

    private List<String> keysUnder(String section) {
        ConfigurationSection s = this.yaml.getConfigurationSection(section);
        return s == null ? List.of() : new ArrayList<>(s.getKeys(false));
    }

    @TestFactory
    Stream<DynamicTest> abilityCooldownsFollowConfig() {
        return this.keysUnder("abilities.cooldowns").stream().map(key -> DynamicTest.dynamicTest(key, () -> {
            this.yaml.set("abilities.cooldowns." + key, 4321);
            assertEquals(4321, this.manager.getAbilityCooldown(key), "abilities.cooldowns." + key);
        }));
    }

    @TestFactory
    Stream<DynamicTest> abilityDurationsFollowConfig() {
        return this.keysUnder("abilities.durations").stream().map(key -> DynamicTest.dynamicTest(key, () -> {
            this.yaml.set("abilities.durations." + key, 321);
            assertEquals(321, this.manager.getAbilityDuration(key), "abilities.durations." + key);
        }));
    }

    @TestFactory
    Stream<DynamicTest> abilityDamageFollowsConfig() {
        return this.keysUnder("abilities.damage").stream().map(key -> DynamicTest.dynamicTest(key, () -> {
            this.yaml.set("abilities.damage." + key, 43.21);
            assertEquals(43.21, this.manager.getAbilityDamage(key), 1e-9, "abilities.damage." + key);
        }));
    }

    private static final Pattern TIER_KEY = Pattern.compile("passives\\.([a-z]+)\\.tier([12])\\.([a-z0-9-]+)");

    /**
     * Every passives.<gem>.tier<N>.<setting> that ConfigManager claims to read (its source names the
     * setting) must change what one of ConfigManager's tier getters returns for that tier.
     */
    @TestFactory
    Stream<DynamicTest> tierPassivesFollowConfig() {
        String source = ConfigIndex.get().source("utils/ConfigManager.java");
        List<DynamicTest> tests = new ArrayList<>();
        for (String key : ConfigIndex.get().leaves.keySet()) {
            Matcher m = TIER_KEY.matcher(key);
            if (!m.matches() || !source.contains("\"." + m.group(3) + "\"")) continue;
            int tier = Integer.parseInt(m.group(2));
            tests.add(DynamicTest.dynamicTest(key, () -> {
                Object original = this.yaml.get(key);
                Object sentinel = original instanceof Boolean b ? !b : original instanceof Integer ? 77 : 7.77;
                List<Object> before = tierResults(tier);
                this.yaml.set(key, sentinel);
                List<Object> after = tierResults(tier);
                boolean seen = false;
                for (int i = 0; i < after.size(); i++) {
                    if (!Objects.equals(before.get(i), after.get(i)) && same(after.get(i), sentinel)) seen = true;
                }
                assertTrue(seen, key + " was changed to " + sentinel + " but no ConfigManager tier getter returned it");
            }));
        }
        return tests.stream();
    }

    private static boolean same(Object value, Object sentinel) {
        if (value instanceof Number n && sentinel instanceof Number s) return Math.abs(n.doubleValue() - s.doubleValue()) < 1e-9;
        return Objects.equals(value, sentinel);
    }

    private List<Object> tierResults(int tier) throws Exception {
        List<Object> out = new ArrayList<>();
        for (Method method : ConfigManager.class.getMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || method.getParameterCount() != 1 || method.getParameterTypes()[0] != int.class) continue;
            if (!method.getName().startsWith("get") && !method.getName().startsWith("is")) continue;
            out.add(method.invoke(this.manager, tier));
        }
        return out;
    }

    @Test
    void reloadPicksUpAnEditedConfig() {
        YamlConfiguration edited = YamlConfiguration.loadConfiguration(ConfigIndex.CONFIG.toFile());
        edited.set("abilities.cooldowns.fire-fireball", 999);
        edited.set("abilities.damage.astra-daggers", 1.5);
        when(this.plugin.getConfig()).thenReturn(edited);
        this.manager.reload();
        assertEquals(999, this.manager.getAbilityCooldown("fire-fireball"));
        assertEquals(1.5, this.manager.getAbilityDamage("astra-daggers"), 1e-9);
    }

    @Test
    void dataFolderIsUntouched() {
        assertTrue(!new File(this.dataFolder.toFile(), "config.yml").exists());
    }
}
