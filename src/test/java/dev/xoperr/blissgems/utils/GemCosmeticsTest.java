package dev.xoperr.blissgems.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class GemCosmeticsTest {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private static ConfigurationSection items() {
        var in = GemCosmeticsTest.class.getClassLoader().getResourceAsStream("cosmetics.yml");
        assertNotNull(in, "cosmetics.yml must ship in the jar, or gems fall back to the legacy text");
        return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)).getConfigurationSection("items");
    }

    @Test
    void everySoulGemHasParseableText() {
        ConfigurationSection items = items();
        MiniMessage mm = MiniMessage.miniMessage();
        for (GemType type : GemType.values()) {
            for (int tier = 1; tier <= 2; tier++) {
                String id = type.getId() + "_gem_t" + tier;
                ConfigurationSection sec = items.getConfigurationSection(id);
                assertNotNull(sec, "missing cosmetics for " + id);
                assertTrue(PLAIN.serialize(mm.deserialize(sec.getString("name"))).contains("GEM"), id);
                for (String line : sec.getStringList("lore")) {
                    String plain = PLAIN.serialize(mm.deserialize(line));
                    assertFalse(plain.contains("<") || plain.contains("§"), id + " has a broken line: " + line);
                    assertFalse(plain.contains("Pristine"), id + " hardcodes an energy level: " + line);
                }
            }
        }
    }

    @Test
    void energyLineShowsTheActualLevel() {
        assertEquals("Energy ◆◆◆◇◇◇◇◇◇◇ Cracked", PLAIN.serialize(CustomItemManager.energyLine(3)));
        assertEquals("Energy ◆◆◆◆◆◆◆◆◆◆ Pristine +5", PLAIN.serialize(CustomItemManager.energyLine(10)));
        assertEquals("Energy ◇◇◇◇◇◇◇◇◇◇ BROKEN", PLAIN.serialize(CustomItemManager.energyLine(0)));
    }
}
