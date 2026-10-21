package dev.xoperr.blissgems.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Indexes the shipped config.yml and the plugin's Java source so the tests can check that every
 * setting an admin can edit is actually read by the code (and vice versa). Built once per run.
 */
final class ConfigIndex {
    static final Path ROOT = Paths.get("").toAbsolutePath();
    static final Path CONFIG = ROOT.resolve("src/main/resources/config.yml");
    static final Path SOURCES = ROOT.resolve("src/main/java");

    private static ConfigIndex instance;

    final YamlConfiguration config;
    /** Every non-section key in config.yml, in file order, with its value. */
    final Map<String, Object> leaves = new LinkedHashMap<>();
    /** file name -> source text */
    final Map<String, String> files = new LinkedHashMap<>();
    /** Every string literal in the source. */
    final Set<String> literals = new LinkedHashSet<>();
    /** Config sections the code iterates (getConfigurationSection / isConfigurationSection). */
    final Set<String> sectionsRead = new LinkedHashSet<>();
    /** getConfig().getX("literal.path", default) calls. */
    final List<Read> reads = new ArrayList<>();

    record Read(String method, String path, String defaultExpr, String where) {
    }

    private static final Pattern LITERAL = Pattern.compile("\"((?:[^\"\\\\\\n]|\\\\.)*)\"");
    private static final Pattern SECTION = Pattern.compile("(?:getConfigurationSection|isConfigurationSection)\\(\\s*\"([^\"]+)\"\\s*\\)");
    private static final String GETTERS = "getInt|getDouble|getBoolean|getString|getLong|getStringList|getList|isConfigurationSection|getConfigurationSection|contains|isSet";

    static synchronized ConfigIndex get() {
        if (instance == null) instance = new ConfigIndex();
        return instance;
    }

    private ConfigIndex() {
        this.config = YamlConfiguration.loadConfiguration(CONFIG.toFile());
        for (String key : this.config.getKeys(true)) {
            if (!this.config.isConfigurationSection(key)) this.leaves.put(key, this.config.get(key));
        }
        try (Stream<Path> walk = Files.walk(SOURCES)) {
            for (Path p : (Iterable<Path>) walk.filter(f -> f.toString().endsWith(".java"))::iterator) {
                String text = Files.readString(p, StandardCharsets.UTF_8);
                String name = SOURCES.relativize(p).toString().replace('\\', '/');
                this.files.put(name, text);
                Matcher lit = LITERAL.matcher(text);
                while (lit.find()) this.literals.add(lit.group(1));
                Matcher sec = SECTION.matcher(text);
                while (sec.find()) this.sectionsRead.add(sec.group(1));
                // ConfigManager keeps the plugin config in a field named "config"
                String receiver = name.endsWith("/ConfigManager.java") ? "(?:getConfig\\(\\)|this\\.config)" : "getConfig\\(\\)";
                Matcher m = Pattern.compile(receiver + "\\.(" + GETTERS + ")\\(\\s*\"([^\"]+)\"\\s*(?:,\\s*([^()]*?|[^()]*\\([^()]*\\)[^()]*?))?\\)").matcher(text);
                while (m.find()) {
                    int line = 1;
                    for (int i = 0; i < m.start(); i++) if (text.charAt(i) == '\n') line++;
                    this.reads.add(new Read(m.group(1), m.group(2), m.group(3) == null ? "" : m.group(3).trim(), name + ":" + line));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Whether the code reads this config key. A key counts as read when its full path appears as a
     * literal, when it is assembled from a literal prefix and a literal suffix
     * ("abilities.cooldowns." + "fire-fireball", "passives.astra.tier" + n + ".phase-chance",
     * "passives.extra-effects." + gem + ".t" + n), or when the code iterates a section above it.
     */
    boolean isRead(String path) {
        if (this.literals.contains(path)) return true;
        for (String prefix : this.literals) {
            if (prefix.length() < 3 || !path.startsWith(prefix) || prefix.equals(path)) continue;
            String rest = path.substring(prefix.length());
            String restNoDigits = rest.replaceAll("[0-9]+$", "");
            for (String suffix : this.literals) {
                if (suffix.length() < 2) continue;
                if (rest.endsWith(suffix)) return true;
                if (!restNoDigits.equals(rest) && restNoDigits.endsWith(suffix)) return true;
            }
        }
        String[] segments = path.split("\\.");
        StringBuilder parent = new StringBuilder();
        for (int i = 0; i < segments.length - 1; i++) {
            if (i > 0) parent.append('.');
            parent.append(segments[i]);
            if (this.sectionsRead.contains(parent.toString())) return true;
        }
        return false;
    }

    /** The key exists in config.yml, as a value or a section. */
    boolean exists(String path) {
        return this.config.contains(path);
    }

    String source(String fileSuffix) {
        for (Map.Entry<String, String> e : this.files.entrySet()) if (e.getKey().endsWith(fileSuffix)) return e.getValue();
        throw new IllegalArgumentException("no source file " + fileSuffix);
    }
}
