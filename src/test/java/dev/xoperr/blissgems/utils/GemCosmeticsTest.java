package dev.xoperr.blissgems.utils;

import static org.junit.jupiter.api.Assertions.*;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class GemCosmeticsTest {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @Test
    void gemsUseTheirOwnTextNotAnOverride() {
        assertNull(GemCosmeticsTest.class.getClassLoader().getResource("cosmetics.yml"),
                "cosmetics.yml would replace the hand-made gem text");
    }

    @Test
    void levelLineShowsTheActualLevel() {
        assertEquals("(Cracked)", PLAIN.serialize(CustomItemManager.energyLine(3)));
        assertEquals("(Pristine)", PLAIN.serialize(CustomItemManager.energyLine(5)));
        assertEquals("(Pristine +5)", PLAIN.serialize(CustomItemManager.energyLine(10)));
        assertEquals("(Broken)", PLAIN.serialize(CustomItemManager.energyLine(0)));
    }
}
