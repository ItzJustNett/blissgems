package dev.xoperr.blissgems.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Achievement;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

/**
 * Mirrors the plugin's achievements as real advancements (a "Bliss Gems" tab in the advancements
 * screen, the vanilla toast and the chat announcement), registered at runtime through Paper - no
 * datapack or extra plugin needed. Progress itself still lives in {@link AchievementManager}.
 */
public final class BlissAdvancements {
    private static final String ROOT = "root";
    private static final Map<Achievement, String[]> LOOK = Map.ofEntries(
        // icon, frame
        Map.entry(Achievement.SHATTERED, new String[]{"minecraft:redstone", "task"}),
        Map.entry(Achievement.ZIP_AWAY, new String[]{"minecraft:copper_block", "challenge"}),
        Map.entry(Achievement.PIERCING_PRECISION, new String[]{"minecraft:amethyst_shard", "goal"}),
        Map.entry(Achievement.SAVED_BY_THE_DICE, new String[]{"minecraft:ender_eye", "task"}),
        Map.entry(Achievement.OVERFLOWING, new String[]{"minecraft:experience_bottle", "goal"}),
        Map.entry(Achievement.ALMOST_FELL_FROM_GRACE, new String[]{"minecraft:feather", "challenge"}),
        Map.entry(Achievement.GOOD_AS_NEW, new String[]{"minecraft:beacon", "goal"}),
        Map.entry(Achievement.REAWAKENING, new String[]{"minecraft:enchanted_book", "goal"}),
        Map.entry(Achievement.ITS_RABBIT_SEASON, new String[]{"minecraft:rabbit_foot", "challenge"}),
        Map.entry(Achievement.TIME_FOR_A_CHANGE, new String[]{"minecraft:emerald", "task"}),
        Map.entry(Achievement.THE_NEXT_LEVEL, new String[]{"minecraft:nether_star", "task"}),
        Map.entry(Achievement.OVERCHARGED, new String[]{"minecraft:lightning_rod", "goal"}),
        Map.entry(Achievement.BOUNDARY_BREAK, new String[]{"minecraft:enchanting_table", "challenge"}));

    private final BlissGems plugin;
    private boolean loaded;

    public BlissAdvancements(BlissGems plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return this.loaded;
    }

    @SuppressWarnings("deprecation")
    public void register() {
        if (!this.plugin.getConfig().getBoolean("advancements.enabled", true)) return;
        try {
            this.load(ROOT, this.root());
            for (Achievement a : Achievement.values()) this.load(id(a), this.child(a));
            this.loaded = true;
        } catch (Throwable t) {
            this.plugin.getLogger().warning("Advancements: could not register the Bliss Gems tab (" + t.getMessage() + "); achievements stay chat-only.");
        }
    }

    @SuppressWarnings("deprecation")
    private void load(String id, JsonObject json) {
        NamespacedKey key = new NamespacedKey(this.plugin, id);
        if (Bukkit.getAdvancement(key) != null) Bukkit.getUnsafe().removeAdvancement(key);
        try {
            Bukkit.getUnsafe().loadAdvancement(key, json.toString());
        } catch (RuntimeException modernFailed) {
            // Before 1.21.5 the background was a full texture path
            JsonObject display = json.getAsJsonObject("display");
            if (!display.has("background")) throw modernFailed;
            display.addProperty("background", "minecraft:textures/gui/advancements/backgrounds/adventure.png");
            Bukkit.getUnsafe().loadAdvancement(key, json.toString());
        }
    }

    private JsonObject root() {
        JsonObject json = this.base("minecraft:nautilus_shell", "task", "Bliss Gems", "Hold a gem. Everything else follows.", false);
        json.getAsJsonObject("display").addProperty("background", "minecraft:gui/advancements/backgrounds/adventure");
        return json;
    }

    private JsonObject child(Achievement a) {
        String[] look = LOOK.getOrDefault(a, new String[]{"minecraft:nautilus_shell", "task"});
        JsonObject json = this.base(look[0], look[1], a.getDisplayName(), a.getDescription(), true);
        json.addProperty("parent", this.plugin.getName().toLowerCase() + ":" + ROOT);
        return json;
    }

    private JsonObject base(String icon, String frame, String title, String description, boolean announce) {
        JsonObject display = new JsonObject();
        JsonObject iconJson = new JsonObject();
        iconJson.addProperty("id", icon);
        display.add("icon", iconJson);
        JsonObject titleJson = new JsonObject();
        titleJson.addProperty("text", title);
        titleJson.addProperty("color", "gold");
        display.add("title", titleJson);
        JsonObject descJson = new JsonObject();
        descJson.addProperty("text", description);
        display.add("description", descJson);
        display.addProperty("frame", frame);
        display.addProperty("show_toast", announce);
        display.addProperty("announce_to_chat", announce);
        display.addProperty("hidden", false);
        JsonObject criteria = new JsonObject();
        JsonObject impossible = new JsonObject();
        impossible.addProperty("trigger", "minecraft:impossible");
        criteria.add("unlock", impossible);
        JsonArray req = new JsonArray();
        JsonArray inner = new JsonArray();
        inner.add("unlock");
        req.add(inner);
        JsonObject json = new JsonObject();
        json.add("display", display);
        json.add("criteria", criteria);
        json.add("requirements", req);
        return json;
    }

    private static String id(Achievement a) {
        return a.name().toLowerCase();
    }

    /** The toast and the chat line for a fresh unlock. */
    public void award(Player player, Achievement achievement) {
        if (!this.loaded) return;
        this.grant(player, ROOT);
        this.grant(player, id(achievement));
    }

    /** On join: show the tab, and quietly mark achievements that were unlocked before advancements existed. */
    public void sync(Player player) {
        if (!this.loaded) return;
        this.grant(player, ROOT);
        World world = player.getWorld();
        Boolean announce = world.getGameRuleValue(GameRule.ANNOUNCE_ADVANCEMENTS);
        try {
            if (Boolean.TRUE.equals(announce)) world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            for (Achievement a : this.plugin.getAchievementManager().getUnlocked(player)) this.grant(player, id(a));
        } finally {
            if (Boolean.TRUE.equals(announce)) world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, true);
        }
    }

    private void grant(Player player, String id) {
        Advancement adv = Bukkit.getAdvancement(new NamespacedKey(this.plugin, id));
        if (adv == null) return;
        AdvancementProgress progress = player.getAdvancementProgress(adv);
        if (!progress.isDone()) progress.awardCriteria("unlock");
    }
}
