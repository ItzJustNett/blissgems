package dev.xoperr.blissgems.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

/** Auto-update picks the newest full release for Paper from modrinth.com/plugin/bliss-smp-plugin. */
class UpdateCheckerTest {
    private static JsonArray versions(String json) {
        return JsonParser.parseString(json).getAsJsonArray();
    }

    @Test
    void picksHighestReleaseNotNewestUpload() {
        JsonArray v = versions("["
            + "{\"version_number\":\"5.0.1\",\"version_type\":\"release\",\"loaders\":[\"paper\"]},"
            + "{\"version_number\":\"5.2.0\",\"version_type\":\"beta\",\"loaders\":[\"paper\"]},"
            + "{\"version_number\":\"5.1.6\",\"version_type\":\"release\",\"loaders\":[\"purpur\",\"paper\"]},"
            + "{\"version_number\":\"6.0.0\",\"version_type\":\"release\",\"loaders\":[\"fabric\"]},"
            + "{\"version_number\":\"5.0.0\",\"version_type\":\"release\",\"loaders\":[\"spigot\"]}]");
        assertEquals("5.1.6", UpdateChecker.pickLatest(v).get("version_number").getAsString());
    }

    @Test
    void versionOrder() {
        assertTrue(UpdateChecker.compareVersions("5.1.10", "5.1.9") > 0);
        assertTrue(UpdateChecker.compareVersions("5.1.6", "5.1.6") == 0);
        assertTrue(UpdateChecker.compareVersions("5.0.1", "5.1.0") < 0);
    }

    @Test
    void officialProjectIsTheDefault() {
        assertEquals("bliss-smp-plugin", UpdateChecker.DEFAULT_PROJECT);
    }
}
