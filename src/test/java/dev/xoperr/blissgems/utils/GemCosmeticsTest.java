package dev.xoperr.blissgems.utils;

import static org.junit.jupiter.api.Assertions.*;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class GemCosmeticsTest {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    /** The gem text is the Oraxen design (cosmetics.yml); every gem must have it, without a fixed level line. */
    @Test
    void everyGemHasItsOraxenText() throws Exception {
        var url = GemCosmeticsTest.class.getClassLoader().getResource("cosmetics.yml");
        assertNotNull(url, "cosmetics.yml must ship in the jar - without it gems fall back to the plain legacy text");
        var cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
            new java.io.InputStreamReader(url.openStream(), java.nio.charset.StandardCharsets.UTF_8));
        for (String gem : new String[]{"astra", "fire", "flux", "life", "puff", "speed", "strength", "wealth"}) {
            for (int tier = 1; tier <= 2; tier++) {
                String id = gem + "_gem_t" + tier;
                assertNotNull(cfg.getString("items." + id + ".name"), id + " has no name in cosmetics.yml");
                var lore = cfg.getStringList("items." + id + ".lore");
                assertFalse(lore.isEmpty(), id + " has no lore in cosmetics.yml");
                assertFalse(String.join("\n", lore).contains("Pristine"),
                    id + ": the level line is added live by the plugin, not written in cosmetics.yml");
            }
        }
    }

    @Test
    void levelLineShowsTheActualLevel() {
        assertEquals(" (Cracked)", PLAIN.serialize(CustomItemManager.energyLine(3)));
        assertEquals(" (Pristine)", PLAIN.serialize(CustomItemManager.energyLine(5)));
        assertEquals(" (Pristine +5)", PLAIN.serialize(CustomItemManager.energyLine(10)));
        assertEquals(" (Broken)", PLAIN.serialize(CustomItemManager.energyLine(0)));
    }
}
