package dev.xoperr.blissgems.goldevent;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.villagerevent.VillagerEventItems;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Fake names and skins (Paper profile API, no ProtocolLib). Used for dream "memories":
 * a player can wear a pool name and someone else's skin. The real name is remembered so
 * commands like /msg keep finding them.
 */
public final class NickManager implements Listener {
    private final BlissGems plugin;
    private final GoldenDream dream;
    private final File file;
    private final YamlConfiguration cfg;
    private static boolean tableBroken;
    private static Field tableField;

    public NickManager(BlissGems plugin, GoldenDream dream) {
        this.plugin = plugin;
        this.dream = dream;
        this.file = new File(plugin.getDataFolder(), "nicks.yml");
        this.cfg = YamlConfiguration.loadConfiguration(this.file);
    }

    public void setName(Player p, String name) {
        this.cfg.set(p.getUniqueId() + ".name", name);
        this.save();
        this.apply(p);
    }

    public boolean setSkinFrom(Player target, Player source) {
        ProfileProperty tex = textures(source.getPlayerProfile());
        if (tex == null) return false;
        this.captureReal(target);
        String k = target.getUniqueId().toString();
        this.cfg.set(k + ".skin.value", tex.getValue());
        this.cfg.set(k + ".skin.signature", tex.getSignature());
        this.save();
        this.apply(target);
        return true;
    }

    public void clearName(Player p) {
        this.cfg.set(p.getUniqueId() + ".name", null);
        this.save();
        this.apply(p);
    }

    public void clearSkin(Player p) {
        this.cfg.set(p.getUniqueId() + ".skin", null);
        this.save();
        this.apply(p);
    }

    public void reset(Player p) {
        String k = p.getUniqueId().toString();
        boolean was = this.isNicked(p.getUniqueId());
        this.cfg.set(k + ".name", null);
        this.cfg.set(k + ".skin", null);
        this.save();
        if (was) this.apply(p);
    }

    /** Drops a nick without touching the (possibly offline or dead) player. */
    public void forget(UUID id) {
        this.cfg.set(id + ".name", null);
        this.cfg.set(id + ".skin", null);
        this.save();
    }

    public boolean isNicked(UUID id) {
        return this.cfg.getString(id + ".name") != null || this.cfg.contains(id + ".skin.value");
    }

    public void apply(Player p) {
        String k = p.getUniqueId().toString();
        this.captureReal(p);
        String real = this.cfg.getString(k + ".real.name", p.getName());
        String fake = this.cfg.getString(k + ".name");
        String profileName = fake == null ? real : sanitize(fake);
        if (profileName.isEmpty()) profileName = real;
        PlayerProfile profile = Bukkit.createProfileExact(p.getUniqueId(), profileName);
        String value = this.cfg.getString(k + ".skin.value", this.cfg.getString(k + ".real.value"));
        if (value != null) {
            profile.setProperty(new ProfileProperty("textures", value, this.cfg.getString(k + ".skin.signature", this.cfg.getString(k + ".real.signature"))));
        }
        p.setPlayerProfile(profile);
        Component shown = fake == null ? Component.text(real) : VillagerEventItems.text(fake);
        p.displayName(shown);
        p.playerListName(shown);
        reconcileNameTable(this::realName);
        this.dream.tabList().refresh(p, true);
    }

    public String realName(Player p) {
        return this.cfg.getString(p.getUniqueId() + ".real.name", p.getName());
    }

    private static String sanitize(String s) {
        String out = s.replaceAll("(?i)<#+[0-9a-f]{6}>", "").replaceAll("(?i)[&§].", "").replace(' ', '_').replaceAll("[^A-Za-z0-9_]", "");
        return out.length() > 16 ? out.substring(0, 16) : out;
    }

    private void captureReal(Player p) {
        String k = p.getUniqueId().toString();
        if (!this.cfg.contains(k + ".real.name")) {
            this.cfg.set(k + ".real.name", p.getName());
            this.save();
        }
        if (!this.cfg.contains(k + ".real.value")) {
            ProfileProperty tex = textures(p.getPlayerProfile());
            if (tex != null) {
                this.cfg.set(k + ".real.value", tex.getValue());
                this.cfg.set(k + ".real.signature", tex.getSignature());
                this.save();
            }
        }
    }

    private static ProfileProperty textures(PlayerProfile profile) {
        for (ProfileProperty prop : profile.getProperties()) if (prop.getName().equals("textures")) return prop;
        return null;
    }

    /**
     * Re-keys the server's by-name player lookup under real names, so /msg RealName still finds a
     * nicked player. Uses internals by reflection; if the server layout changes it silently stops.
     */
    @SuppressWarnings("unchecked")
    static void reconcileNameTable(Function<Player, String> realName) {
        if (tableBroken) return;
        try {
            Object handle = Bukkit.getServer().getClass().getMethod("getHandle").invoke(Bukkit.getServer());
            if (tableField == null) tableField = findField(handle.getClass(), "playersByName");
            Map<String, Object> table = (Map<String, Object>) tableField.get(handle);
            table.clear();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasMetadata("NPC")) continue;
                table.put(realName.apply(p).toLowerCase(Locale.ROOT), p.getClass().getMethod("getHandle").invoke(p));
            }
        } catch (Throwable t) {
            tableBroken = true;
        }
    }

    private static Field findField(Class<?> c, String name) throws NoSuchFieldException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldException(name);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        String k = p.getUniqueId().toString();
        boolean nicked = this.isNicked(p.getUniqueId());
        if ((nicked || this.cfg.contains(k + ".real.name")) && !p.getName().equals(this.cfg.getString(k + ".real.name"))) {
            this.cfg.set(k + ".real.name", p.getName());
            this.save();
        }
        if (nicked) Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (p.isOnline()) this.apply(p);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPostRespawn(PlayerPostRespawnEvent event) {
        reconcileNameTable(this::realName);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTask(this.plugin, () -> reconcileNameTable(this::realName));
    }

    private void save() {
        try {
            this.cfg.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("nicks.yml: " + e.getMessage());
        }
    }
}
