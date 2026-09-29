package dev.xoperr.blissgems.goldevent;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.pedestal.PedestalManager;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/**
 * Bot "memories": Citizens player-NPCs in diamond gear that roam around where they were
 * spawned, wearing a pool name and an optional skin. Killing one purges it and drops loot.
 * Citizens is an optional dependency and is only ever touched through reflection, so this
 * class works (as "unavailable") on servers without it.
 */
public final class MemoryNpcs implements Listener {
    private final BlissGems plugin;
    private final MemoryRoster roster;
    private final File file;
    private final YamlConfiguration cfg;
    private final boolean citizens;
    private final Map<String, Mem> live = new HashMap<>();
    private final Map<UUID, Mem> byEntity = new HashMap<>();
    private final List<ItemStack> loot = new ArrayList<>();
    private BukkitTask task;

    public MemoryNpcs(BlissGems plugin, MemoryRoster roster) {
        this.plugin = plugin;
        this.roster = roster;
        this.file = new File(plugin.getDataFolder(), "memory_npcs.yml");
        this.cfg = YamlConfiguration.loadConfiguration(this.file);
        this.citizens = Bukkit.getPluginManager().getPlugin("Citizens") != null && classExists("net.citizensnpcs.api.CitizensAPI");
        this.loadLoot();
        ConfigurationSection spawned = this.cfg.getConfigurationSection("spawned");
        if (spawned != null) for (String k : spawned.getKeys(false)) roster.claimSpecificName(spawned.getString(k + ".name"));
    }

    public boolean available() {
        return this.citizens;
    }

    /** Spawns one bot memory at the location; returns its name, or null when no name is free / it failed. */
    public String spawn(Location at, String skin) {
        if (!this.citizens || at == null || at.getWorld() == null) return null;
        String name = this.roster.claimFreeName();
        if (name == null) return null;
        Mem m = new Mem(UUID.randomUUID().toString().substring(0, 8), name, skin, at.clone());
        if (!this.spawnEntity(m)) {
            this.roster.releaseName(name);
            return null;
        }
        this.live.put(m.id, m);
        this.persist(m);
        this.start();
        return name;
    }

    /** Re-creates the bots saved in memory_npcs.yml (after a restart). */
    public void reload() {
        if (!this.citizens) return;
        ConfigurationSection spawned = this.cfg.getConfigurationSection("spawned");
        if (spawned == null) return;
        for (String id : new ArrayList<>(spawned.getKeys(false))) {
            String p = "spawned." + id + ".";
            World w = this.cfg.getString(p + "world") == null ? null : Bukkit.getWorld(this.cfg.getString(p + "world"));
            String name = this.cfg.getString(p + "name");
            if (w == null || name == null) continue;
            this.roster.claimSpecificName(name);
            Mem m = new Mem(id, name, this.cfg.getString(p + "skin"), new Location(w, this.cfg.getDouble(p + "x"), this.cfg.getDouble(p + "y"),
                this.cfg.getDouble(p + "z"), (float) this.cfg.getDouble(p + "yaw"), (float) this.cfg.getDouble(p + "pitch")));
            if (this.spawnEntity(m)) this.live.put(id, m);
        }
        if (!this.live.isEmpty()) this.start();
    }

    public int clearAll() {
        int n = 0;
        for (Mem m : new ArrayList<>(this.live.values())) {
            this.destroy(m);
            n++;
        }
        this.live.clear();
        this.cfg.set("spawned", null);
        this.save();
        return n;
    }

    public int count() {
        return this.live.size();
    }

    public List<String> liveNames() {
        List<String> out = new ArrayList<>();
        for (Mem m : this.live.values()) out.add(m.name);
        return out;
    }

    public List<Location> liveLocations() {
        List<Location> out = new ArrayList<>();
        for (Mem m : this.live.values()) {
            Entity e = this.entity(m);
            if (e != null) out.add(e.getLocation());
        }
        return out;
    }

    private Mem byName(String name) {
        for (Mem m : this.live.values()) if (m.name.equalsIgnoreCase(name)) return m;
        return null;
    }

    public boolean rename(String oldName, String newName) {
        Mem m = this.byName(oldName);
        if (m == null || !this.roster.isNameFree(newName)) return false;
        this.roster.releaseName(m.name);
        this.roster.claimSpecificName(newName);
        m.name = newName;
        try {
            call(m.npc, "setName", newName);
            this.respawnInPlace(m);
        } catch (Throwable t) {
            this.plugin.getLogger().warning("NPC memory rename: " + t.getMessage());
        }
        this.persist(m);
        return true;
    }

    public boolean reskin(String name, String skin) {
        Mem m = this.byName(name);
        if (m == null) return false;
        m.skin = skin;
        try {
            Object trait = call(m.npc, "getOrAddTrait", Class.forName("net.citizensnpcs.trait.SkinTrait"));
            if (skin == null || skin.isBlank()) {
                call(trait, "clearTexture");
                this.respawnInPlace(m);
            } else {
                call(trait, "setSkinName", skin);
            }
        } catch (Throwable t) {
            this.plugin.getLogger().warning("NPC memory skin: " + t.getMessage());
        }
        this.persist(m);
        return true;
    }

    // ---- Citizens (reflection) ----

    private boolean spawnEntity(Mem m) {
        try {
            Object registry = Class.forName("net.citizensnpcs.api.CitizensAPI").getMethod("getNPCRegistry").invoke(null);
            Object npc = call(registry, "createNPC", EntityType.PLAYER, m.name);
            if (m.skin != null && !m.skin.isBlank()) {
                call(call(npc, "getOrAddTrait", Class.forName("net.citizensnpcs.trait.SkinTrait")), "setSkinName", m.skin);
            }
            Object data = call(npc, "data");
            Class<?> meta = Class.forName("net.citizensnpcs.api.npc.NPC$Metadata");
            call(data, "setPersistent", enumConst(meta, "SHOULD_SAVE"), false);
            call(data, "setPersistent", enumConst(meta, "DEFAULT_PROTECTED"), false);
            call(data, "setPersistent", enumConst(meta, "RESPAWN_DELAY"), -1);
            m.npc = npc;
            call(npc, "spawn", m.home);
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.gear(m), 5L);
            return true;
        } catch (Throwable t) {
            this.plugin.getLogger().warning("NPC memory spawn failed: " + t);
            return false;
        }
    }

    private void gear(Mem m) {
        try {
            Entity e = this.entity(m);
            if (e == null) return;
            this.byEntity.put(e.getUniqueId(), m);
            Object equipment = call(m.npc, "getOrAddTrait", Class.forName("net.citizensnpcs.api.trait.trait.Equipment"));
            Class<?> slot = Class.forName("net.citizensnpcs.api.trait.trait.Equipment$EquipmentSlot");
            call(equipment, "set", enumConst(slot, "HELMET"), new ItemStack(Material.DIAMOND_HELMET));
            call(equipment, "set", enumConst(slot, "CHESTPLATE"), new ItemStack(Material.DIAMOND_CHESTPLATE));
            call(equipment, "set", enumConst(slot, "LEGGINGS"), new ItemStack(Material.DIAMOND_LEGGINGS));
            call(equipment, "set", enumConst(slot, "BOOTS"), new ItemStack(Material.DIAMOND_BOOTS));
            call(equipment, "set", enumConst(slot, "HAND"), new ItemStack(Material.DIAMOND_SWORD));
            m.geared = true;
        } catch (Throwable t) {
            this.plugin.getLogger().warning("NPC memory gear failed: " + t);
        }
    }

    private Entity entity(Mem m) {
        if (m.npc == null) return null;
        try {
            if (!(Boolean) call(m.npc, "isSpawned")) return null;
            return (Entity) call(m.npc, "getEntity");
        } catch (Throwable t) {
            return null;
        }
    }

    private void respawnInPlace(Mem m) throws Exception {
        Entity e = this.entity(m);
        if (e == null) return;
        Location at = e.getLocation();
        call(m.npc, "despawn");
        call(m.npc, "spawn", at);
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.gear(m), 5L);
    }

    private void destroy(Mem m) {
        m.destroyedByUs = true;
        Entity e = this.entity(m);
        if (e != null) this.byEntity.remove(e.getUniqueId());
        try {
            if (m.npc != null) call(m.npc, "destroy");
        } catch (Throwable ignored) {
        }
        this.roster.releaseName(m.name);
    }

    /** Sprinting bots wander 6-14 blocks around their home every few seconds. */
    private void start() {
        if (this.task == null) this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tickAll, 10L, 10L);
    }

    private void tickAll() {
        long now = System.currentTimeMillis();
        for (Mem m : new ArrayList<>(this.live.values())) {
            try {
                this.tick(m, now);
            } catch (Throwable t) {
                if (!m.warned) {
                    m.warned = true;
                    this.plugin.getLogger().warning("NPC memory " + m.name + " tick failed: " + t);
                }
            }
        }
    }

    private void tick(Mem m, long now) throws Exception {
        Entity e = this.entity(m);
        if (e == null || !m.geared) return;
        if (e instanceof Player bot) bot.setSprinting(true);
        Object nav = call(m.npc, "getNavigator");
        if (now < m.nextWander && (Boolean) call(nav, "isNavigating")) return;
        m.nextWander = now + 3000L + ThreadLocalRandom.current().nextInt(3000);
        World w = m.home.getWorld();
        if (w == null) return;
        double a = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
        double d = 6.0 + ThreadLocalRandom.current().nextDouble() * 8.0;
        Location target = m.home.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
        if (!w.isChunkLoaded(target.getBlockX() >> 4, target.getBlockZ() >> 4)) target = m.home.clone();
        target.setY(w.getHighestBlockYAt(target) + 1);
        call(nav, "setTarget", target);
        call(call(nav, "getLocalParameters"), "speedModifier", 1.3f);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBotDeath(PlayerDeathEvent event) {
        Mem m = this.byEntity.remove(event.getEntity().getUniqueId());
        if (m == null || m.destroyedByUs) return;
        event.deathMessage(null);
        event.getDrops().clear();
        Location at = event.getEntity().getLocation();
        if (at.getY() < at.getWorld().getMinHeight()) at = m.home;
        String msg = PedestalManager.color("&e" + m.name + " has been purged.");
        for (Player p : Bukkit.getOnlinePlayers()) if (GoldenDreamWorld.isDreamWorld(p.getWorld()) == GoldenDreamWorld.isDreamWorld(at.getWorld())) p.sendMessage(msg);
        at.getWorld().strikeLightningEffect(at);
        at.getWorld().playSound(at, Sound.ENTITY_WITHER_DEATH, SoundCategory.MASTER, 3.0f, 0.5f);
        for (ItemStack item : this.loot) at.getWorld().dropItemNaturally(at, item.clone());
        m.destroyedByUs = true;
        this.live.remove(m.id);
        this.roster.releaseName(m.name);
        this.cfg.set("spawned." + m.id, null);
        this.save();
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            try {
                call(m.npc, "destroy");
            } catch (Throwable ignored) {
            }
        }, 1L);
    }

    public void shutdown() {
        for (Mem m : new ArrayList<>(this.live.values())) {
            m.destroyedByUs = true;
            try {
                if (m.npc != null) call(m.npc, "destroy");
            } catch (Throwable ignored) {
            }
        }
        this.live.clear();
        this.byEntity.clear();
        if (this.task != null) this.task.cancel();
        this.task = null;
    }

    private void persist(Mem m) {
        String p = "spawned." + m.id + ".";
        this.cfg.set(p + "name", m.name);
        this.cfg.set(p + "skin", m.skin);
        this.cfg.set(p + "world", m.home.getWorld().getName());
        this.cfg.set(p + "x", m.home.getX());
        this.cfg.set(p + "y", m.home.getY());
        this.cfg.set(p + "z", m.home.getZ());
        this.cfg.set(p + "yaw", m.home.getYaw());
        this.cfg.set(p + "pitch", m.home.getPitch());
        this.save();
    }

    private void loadLoot() {
        if (!this.cfg.contains("loot")) {
            this.cfg.set("loot", List.of("DIAMOND:1", "EMERALD:2", "GOLDEN_APPLE:1", "IRON_INGOT:3"));
            this.save();
        }
        for (String s : this.cfg.getStringList("loot")) {
            String[] parts = s.split(":");
            Material mat = Material.matchMaterial(parts[0].trim());
            if (mat == null) {
                this.plugin.getLogger().warning("memory_npcs.yml loot: unknown material '" + parts[0] + "'");
                continue;
            }
            int amount = 1;
            try {
                if (parts.length > 1) amount = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
            }
            this.loot.add(new ItemStack(mat, Math.max(1, Math.min(mat.getMaxStackSize(), amount))));
        }
    }

    private void save() {
        try {
            this.cfg.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("memory_npcs.yml: " + e.getMessage());
        }
    }

    // ---- reflection helpers ----

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumConst(Class<?> type, String name) {
        return Enum.valueOf((Class) type, name);
    }

    /** Calls the public method with that name whose parameters accept the given arguments. */
    static Object call(Object target, String name, Object... args) throws Exception {
        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
            Class<?>[] types = m.getParameterTypes();
            boolean ok = true;
            for (int i = 0; i < types.length && ok; i++) ok = args[i] == null || box(types[i]).isInstance(args[i]);
            if (!ok) continue;
            m.setAccessible(true);
            return m.invoke(target, args);
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + name + "/" + args.length);
    }

    private static Class<?> box(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == boolean.class) return Boolean.class;
        if (c == float.class) return Float.class;
        if (c == double.class) return Double.class;
        if (c == long.class) return Long.class;
        return c;
    }

    private static final class Mem {
        final String id;
        String name;
        String skin;
        final Location home;
        Object npc;
        boolean geared;
        boolean warned;
        boolean destroyedByUs;
        long nextWander;

        Mem(String id, String name, String skin, Location home) {
            this.id = id;
            this.name = name;
            this.skin = skin;
            this.home = home;
        }
    }
}
