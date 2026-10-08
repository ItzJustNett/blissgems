package dev.xoperr.blissgems.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/** Chat messages: old & codes still work, MiniMessage gradients render to hex colours. */
class MessageRenderTest {
    private static String render(String raw) throws Exception {
        Method m = dev.xoperr.blissgems.utils.ConfigManager.class.getDeclaredMethod("renderMessage", String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, raw);
    }

    @Test
    void ampersandCodesStillWork() throws Exception {
        assertEquals("§cNo!", render("&cNo!"));
    }

    @Test
    void gradientsBecomeHexColours() throws Exception {
        String out = render("<gradient:#FF0000:#0000FF>Hello</gradient> {player}");
        assertTrue(out.contains("§x"), "a gradient should come out as hex colours: " + out);
        assertTrue(out.endsWith(" {player}") || out.contains("{player}"), "placeholders survive: " + out);
        assertFalse(out.contains("<gradient"), "no raw tags in chat: " + out);
    }

    @Test
    void mixedCodesAndTags() throws Exception {
        String out = render("<gradient:#FFAA00:#FFDD55>Harvested</gradient> &7({count}/8)");
        assertTrue(out.contains("§7({count}/8)"), out);
    }
}
