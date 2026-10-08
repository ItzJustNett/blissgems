package dev.xoperr.blissgems.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/**
 * Gems are nautilus shells (Bedrock lets players hold those off hand). Every gem model number the
 * plugin can put on one - base, the Pristine variants (+20/+30/+40), and the Gold Gem - must be in
 * the resource pack's nautilus_shell.json, or that gem shows up as a plain nautilus shell.
 */
class GemModelsTest {
    private static final Path SOURCE = Path.of("src/main/java/dev/xoperr/blissgems/utils/CustomItemManager.java");
    private static final Path PACK_ITEM = Path.of("resourcepack/assets/minecraft/items/nautilus_shell.json");

    @Test
    void gemsAreNautilusShells() throws Exception {
        assertEquals(Material.NAUTILUS_SHELL, CustomItemManager.GEM_MATERIAL);
        String src = Files.readString(SOURCE);
        Matcher m = Pattern.compile("registerItem\\(\"([a-z]+_gem_t[12])\", ([A-Za-z_.]+),").matcher(src);
        int gems = 0;
        while (m.find()) {
            gems++;
            assertEquals("GEM_MATERIAL", m.group(2), m.group(1) + " is not registered as GEM_MATERIAL");
        }
        assertEquals(17, gems, "expected 16 gems (8 x T1/T2) + the Gold Gem");
    }

    @Test
    void packHasEveryGemModel() throws Exception {
        assertTrue(Files.exists(PACK_ITEM), PACK_ITEM + " is missing");
        TreeSet<Integer> have = new TreeSet<>();
        for (JsonElement e : JsonParser.parseString(Files.readString(PACK_ITEM)).getAsJsonObject()
                .getAsJsonObject("model").getAsJsonArray("entries")) {
            have.add(e.getAsJsonObject().get("threshold").getAsInt());
        }
        List<Integer> missing = new ArrayList<>();
        for (int tier = 1; tier <= 2; tier++) {
            for (int gem = 1; gem <= 8; gem++) {
                int base = tier * 1000 + gem;
                for (int offset : new int[]{0, 20, 30, 40}) {
                    if (!have.contains(base + offset)) missing.add(base + offset);
                }
            }
        }
        if (!have.contains(1009)) missing.add(1009);
        assertTrue(missing.isEmpty(), "nautilus_shell.json has no model for custom_model_data " + missing);
    }
}
