package dev.xoperr.blissgems.goldevent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.WeatherType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Dream "looks" built only from things every client renders: per-player time of day and weather
 * (Java and Bedrock), blindness for closed eyes, and on Bedrock (via Geyser, optional) the client's
 * built-in fog presets and camera fades. The pocket world's golden sky itself comes from the
 * bliss_skies datapack (see {@link SkyDatapack}). No resource pack needed.
 */
public final class DreamSky {
    private static long goldenTime = 12600L;
    private static long memoryTime = 12900L;
    private static long stormTime = 18000L;
    /**
     * Shaderpack signals: the BlissGems shaderpack (Iris/OptiFine) reads the client's day number
     * (worldDay). On these days it draws GoldenDome / GloomHaze; on any normal day it changes nothing.
     * Without the shaderpack the only difference is the moon phase.
     */
    public static final long DOME_DAY = 80036L;
    public static final long GLOOM_DAY = 80037L;
    public static final long STORM_DAY = 80038L;
    public static final long GLITCH_DAY = 80039L;
    private static boolean signals = true;

    private static long at(long day, long timeOfDay) {
        return signals ? day * 24000L + timeOfDay : timeOfDay;
    }
    private static String pocketFog = "minecraft:fog_mesa";
    private static String goldenFog = "minecraft:fog_mesa";
    private static String memoryFog = "minecraft:fog_basalt_deltas";
    private static String stormFog = "minecraft:fog_the_end";

    private static final Map<UUID, String> FOG_NOW = new HashMap<>();

    private DreamSky() {
    }

    /** One Bedrock fog at a time; nothing is re-sent while it's unchanged. */
    private static void setFog(Player p, String fogId) {
        String now = FOG_NOW.get(p.getUniqueId());
        if (fogId != null && fogId.equals(now)) return;
        if (now != null) BedrockBridge.clearFog(p, null);
        if (fogId == null || fogId.isBlank()) {
            FOG_NOW.remove(p.getUniqueId());
            return;
        }
        FOG_NOW.put(p.getUniqueId(), fogId);
        BedrockBridge.fog(p, fogId);
    }

    public static void load(Plugin plugin) {
        ConfigurationSection c = plugin.getConfig().getConfigurationSection("golden-dream.sky");
        if (c == null) return;
        goldenTime = c.getLong("golden-hour-time", goldenTime);
        signals = c.getBoolean("shader-signals", true);
        memoryTime = c.getLong("memory-time", memoryTime);
        stormTime = c.getLong("storm-time", stormTime);
        pocketFog = c.getString("bedrock-fog.pocket", pocketFog);
        goldenFog = c.getString("bedrock-fog.golden", goldenFog);
        memoryFog = c.getString("bedrock-fog.memory", memoryFog);
        stormFog = c.getString("bedrock-fog.storm", stormFog);
    }

    /** Near the ritual / the golden mass: a frozen golden-hour sky. */
    public static void golden(Player p) {
        p.setPlayerTime(at(DOME_DAY, goldenTime), false);
        setFog(p, goldenFog);
    }

    /** Sky glitch (the shaderpack's glitch mode): the moment the fragments go off. */
    public static void glitch(Player p) {
        if (signals) p.setPlayerTime(GLITCH_DAY * 24000L + 6000L, false);
    }

    /** GoldenDome fading in: f 0..1 (the shaderpack reads the time of day 0..1000 as the fade). */
    public static void domeFade(Player p, double f) {
        long tod = Math.round(Math.max(0.0, Math.min(1.0, f)) * 1000.0);
        p.setPlayerTime(at(DOME_DAY, tod), false);
        setFog(p, goldenFog);
    }

    /** The pocket walkway: GoldenDome shader (its gold colour on Java also comes from the datapack). */
    public static void pocket(Player p) {
        p.setPlayerTime(at(DOME_DAY, 6000L), false);
        p.setPlayerWeather(WeatherType.CLEAR);
        setFog(p, pocketFog);
    }

    /** The memory world: an endless dusk, or a night storm while the great thunder hangs. */
    public static void memory(Player p, boolean storm) {
        if (storm) {
            p.setPlayerTime(at(STORM_DAY, stormTime), false);
            p.setPlayerWeather(WeatherType.DOWNFALL);
            setFog(p, stormFog);
        } else {
            p.setPlayerTime(at(GLOOM_DAY, memoryTime), false);
            p.setPlayerWeather(WeatherType.CLEAR);
            setFog(p, memoryFog);
        }
    }

    /** Back to the real sky. */
    public static void clear(Player p) {
        p.resetPlayerWeather();
        setFog(p, null);
    }

    /** Real time of day and real sky again. */
    public static void reset(Player p) {
        p.resetPlayerTime();
        clear(p);
    }

    public static void restore(Player p, long offset, boolean relative) {
        p.setPlayerTime(offset, relative);
        clear(p);
    }

    /** Eyes closing: blindness on both editions, plus a black camera fade on Bedrock. */
    public static void closeEyes(Player p, int ticks) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ticks, 0, false, false, false));
        BedrockBridge.fade(p, 0, 0, 0, 0.5f, Math.max(0f, ticks / 20f - 1.0f), 0.5f);
    }

    /** Forget a player who left (their client drops the fog anyway). */
    public static void forget(UUID id) {
        FOG_NOW.remove(id);
    }

    public static void openEyes(Player p) {
        p.removePotionEffect(PotionEffectType.BLINDNESS);
    }
}
