package dev.xoperr.blissgems.config;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * "If I change it in config.yml, does it change in-game?" - checked statically against the source:
 * every key admins can edit must be read by the code, every key the code reads must be in
 * config.yml (or admins can't set it - and a typo'd path silently ignores theirs), and the
 * fallback the code uses must equal the shipped value (old configs missing a key behave the same).
 */
class ConfigKeysTest {
    /**
     * Messages that exist in config.yml but whose text is still written directly in the code.
     * Editing them does nothing yet. Wire one up (read it via ConfigManager.getMessage) and remove
     * it from this list - the test fails if this list goes stale, and if a new unused message
     * appears that is not listed here.
     */
    static final Set<String> KNOWN_UNWIRED_MESSAGES = new TreeSet<>(List.of(
        "messages.ability-cooldown",
        "messages.projection-boundary",
        "messages.projection-ended",
        "messages.projection-blocked",
        "messages.cannot-swap-gem-full-inventory",
        "messages.cannot-move-gem-to-container",
        "messages.cannot-use-gem-in-composter",
        "messages.cannot-trust-self",
        "messages.trusted-players-header",
        "messages.trusted-player-entry",
        "messages.phased-attack",
        "messages.enemy-launched",
        "messages.slam-down",
        "messages.double-jump",
        "messages.life-gem-golden-apple",
        "messages.player-grounded",
        "messages.pedestal-created",
        "messages.pedestal-created-nearby",
        "messages.pedestal-creation-failed",
        "messages.pedestal-activated",
        "messages.pedestal-exhausted",
        "messages.revive-beacon-activated",
        "messages.revive-beacon-range",
        "messages.revive-beacon-expired",
        "messages.soul-absorbed",
        "messages.cannot-capture-players",
        "messages.soul-capacity-full",
        "messages.entity-cannot-capture",
        "messages.soul-captured",
        "messages.no-souls-captured",
        "messages.souls-released",
        "messages.charged-attack",
        "messages.crit-progress",
        "messages.no-target-found",
        "messages.fireball-charging",
        "messages.fireball-fully-charged",
        "messages.fireball-fired",
        "messages.campfire-cannot-place",
        "messages.campfire-placed",
        "messages.campfire-destroyed",
        "messages.campfire-expired",
        "messages.flux-beam-charging",
        "messages.flux-beam-fully-charged",
        "messages.flux-beam-fired",
        "messages.armor-restored",
        "messages.unfortunate-afflicted",
        "messages.unfortunate-blocked",
        "messages.item-locked",
        "messages.item-locked-blocked",
        "messages.double-debris",
        "messages.no-gem-types-available",
        "messages.trade-failed",
        "messages.upgrade-failed"
    ));

    private final ConfigIndex index = ConfigIndex.get();

    @Test
    void everyConfigKeyIsReadByTheCode() {
        List<String> dead = new ArrayList<>();
        for (Map.Entry<String, Object> e : this.index.leaves.entrySet()) {
            String key = e.getKey();
            if (key.equals("config-version")) continue;
            if (KNOWN_UNWIRED_MESSAGES.contains(key)) continue;
            if (!this.index.isRead(key)) dead.add(key + " = " + e.getValue());
        }
        if (!dead.isEmpty()) {
            fail(dead.size() + " config key(s) are never read - changing them does nothing in-game:\n  " + String.join("\n  ", dead));
        }
    }

    @Test
    void unwiredMessageListIsCurrent() {
        List<String> stale = new ArrayList<>();
        for (String key : KNOWN_UNWIRED_MESSAGES) {
            if (!this.index.exists(key)) stale.add(key + " (no longer in config.yml)");
            else if (this.index.isRead(key)) stale.add(key + " (now read by the code - remove it from the list)");
        }
        if (!stale.isEmpty()) fail("KNOWN_UNWIRED_MESSAGES is out of date:\n  " + String.join("\n  ", stale));
    }

    @Test
    void everyKeyTheCodeReadsIsInConfig() {
        Set<String> missing = new LinkedHashSet<>();
        for (ConfigIndex.Read read : this.index.reads) {
            if (!this.index.exists(read.path())) missing.add(read.path() + "  <- " + read.where());
        }
        if (!missing.isEmpty()) {
            fail(missing.size() + " key(s) are read by the code but missing from config.yml (admins can't set them, or the path is a typo):\n  "
                + String.join("\n  ", missing));
        }
    }

    @Test
    void codeDefaultsMatchConfig() {
        List<String> mismatches = new ArrayList<>();
        for (ConfigIndex.Read read : this.index.reads) {
            if (read.defaultExpr().isEmpty() || !this.index.exists(read.path())) continue;
            Object shipped = this.index.config.get(read.path());
            String def = read.defaultExpr();
            Boolean same = sameValue(shipped, def);
            if (same != null && !same) {
                mismatches.add(read.path() + ": config.yml = " + shipped + ", code fallback = " + def + "  <- " + read.where());
            }
        }
        if (!mismatches.isEmpty()) {
            fail(mismatches.size() + " code fallback(s) disagree with config.yml (a config missing the key behaves differently):\n  "
                + String.join("\n  ", mismatches));
        }
    }

    private static final Pattern NUMBER = Pattern.compile("-?[0-9]+(?:\\.[0-9]+)?[LlFfDd]?");

    /** null when the default expression isn't a plain literal we can compare. */
    static Boolean sameValue(Object shipped, String def) {
        if (shipped instanceof Boolean b) {
            if (def.equals("true") || def.equals("false")) return b == Boolean.parseBoolean(def);
            return null;
        }
        if (shipped instanceof Number n) {
            if (!NUMBER.matcher(def).matches()) return null;
            return Math.abs(Double.parseDouble(def.replaceAll("[LlFfDd]$", "")) - n.doubleValue()) < 1e-9;
        }
        if (shipped instanceof String s) {
            if (def.length() >= 2 && def.startsWith("\"") && def.endsWith("\"") && !def.substring(1, def.length() - 1).contains("\"")) {
                return s.equals(def.substring(1, def.length() - 1));
            }
            return null;
        }
        return null;
    }

    private static final Pattern ABILITY_USE = Pattern.compile(
        "\\b(?:canUseAbility|useAbility|useAbilityWithDuration|isOnCooldown|getRemainingCooldown)\\([^,()]+,\\s*\"([^\"]+)\"");
    private static final Pattern ABILITY_KEY_VAR = Pattern.compile("String abilityKey = \"([^\"]+)\"");
    private static final Pattern COOLDOWN_GET = Pattern.compile("getAbilityCooldown\\(\"([^\"]+)\"");
    private static final Pattern DURATION_GET = Pattern.compile("getAbilityDuration\\(\"([^\"]+)\"");
    private static final Pattern DAMAGE_GET = Pattern.compile("getAbilityDamage\\(\"([^\"]+)\"");

    /**
     * Abilities look their numbers up by key under abilities.cooldowns / durations / damage, and
     * silently fall back to 10 s / 10 s / 4 HP when the key isn't there - a hardcoded value in disguise.
     */
    @Test
    void everyAbilityNumberIsConfigurable() {
        Set<String> missing = new TreeSet<>();
        for (Map.Entry<String, String> file : this.index.files.entrySet()) {
            String text = file.getValue();
            check(text, ABILITY_USE, "abilities.cooldowns.", file.getKey(), missing);
            check(text, ABILITY_KEY_VAR, "abilities.cooldowns.", file.getKey(), missing);
            check(text, COOLDOWN_GET, "abilities.cooldowns.", file.getKey(), missing);
            check(text, DURATION_GET, "abilities.durations.", file.getKey(), missing);
            check(text, DAMAGE_GET, "abilities.damage.", file.getKey(), missing);
        }
        if (!missing.isEmpty()) {
            fail(missing.size() + " ability number(s) fall back to a hardcoded default (no config entry):\n  " + String.join("\n  ", missing));
        }
    }

    private void check(String text, Pattern pattern, String section, String file, Set<String> missing) {
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            String path = section + m.group(1);
            if (!this.index.exists(path)) missing.add(path + "  <- " + file);
        }
    }
}
