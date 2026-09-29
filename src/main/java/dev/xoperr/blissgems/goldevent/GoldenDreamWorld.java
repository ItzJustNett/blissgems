package dev.xoperr.blissgems.goldevent;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.EnergyState;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

/**
 * The Golden Dream's worlds.
 * <ul>
 *   <li><b>goldendream</b> - the pocket: an invisible barrier walkway in a golden void. Dreamers arrive
 *       stripped of everything, stand for a minute, then are walked along the golden sky for four more
 *       (movement locked) until they fall into the memory.</li>
 *   <li><b>goldenworld</b> (+ nether + end) - the memory, optionally a copy of an old map dropped into
 *       /goldendream_import. Dreamers enter it as intruders with a random gem at Pristine and hunt the
 *       memories.</li>
 * </ul>
 * /goldendream wake (or the great thunder's float) returns a dreamer to their real place, inventory
 * and gem; whoever started the dream wakes holding the Fragment Core.
 */
public final class GoldenDreamWorld implements Listener {
    static final String POCKET = "goldendream";
    static final String WORLD = "goldenworld";
    static final String NETHER = "goldenworldnether";
    static final String END = "goldenworldend";
    public static final String IMPORT_FOLDER = "goldendream_import";
    static final int WALK_DELAY_TICKS = 1200;
    static final int LEAVE_TICKS = 6000;
    static final double WALK_SPEED = 0.15;
    private static final Particle LIGHT_MOTE = particle("FIREFLY", Particle.WHITE_ASH);

    private final BlissGems plugin;
    private final GoldenDream dream;
    private final NamespacedKey locked;
    private final File dreamFile;
    private final YamlConfiguration data;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Visit> visits = new HashMap<>();
    private final Set<UUID> graded = new HashSet<>();
    private final Map<UUID, Long> gradedDay = new HashMap<>();
    private final Map<UUID, Long> cueNext = new HashMap<>();
    private World pocket;
    private World overworld;
    private World nether;
    private World end;
    private BukkitTask gradeTask;
    private int stormUntil = -1;

    public GoldenDreamWorld(BlissGems plugin, GoldenDream dream) {
        this.plugin = plugin;
        this.dream = dream;
        this.locked = new NamespacedKey(plugin, "dream_locked");
        this.dreamFile = new File(plugin.getDataFolder(), "golden_dream.yml");
        this.data = YamlConfiguration.loadConfiguration(this.dreamFile);
        File container = Bukkit.getWorldContainer();
        if (new File(container, POCKET).isDirectory()) this.pocket();
        if (new File(container, WORLD).isDirectory()) this.worldSet();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (this.pocket == null || p.getWorld() != this.pocket) continue;
                if (this.inDream(p.getUniqueId()) && !this.sessions.containsKey(p.getUniqueId())) {
                    if (this.intruded(p.getUniqueId())) {
                        Location at = this.arrival();
                        if (at != null) p.teleport(at);
                    } else {
                        this.enter(p, p.getPlayerTimeOffset(), p.isPlayerTimeRelative(), false);
                    }
                }
                Visit v = this.loadVisit(p.getUniqueId());
                if (v != null) this.arm(v);
            }
        }, 20L);
    }

    private static Particle particle(String name, Particle fallback) {
        try {
            return Particle.valueOf(name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public void storm(int ticks) {
        this.stormUntil = Math.max(this.stormUntil, Bukkit.getCurrentTick() + ticks);
    }

    // ---- records ----

    public boolean inDream(UUID id) {
        return this.data.contains(id.toString());
    }

    private boolean intruded(UUID id) {
        return this.data.getBoolean(id + ".intruded", false);
    }

    public boolean isIntruder(UUID id) {
        return id != null && this.intruded(id);
    }

    private void saveData() {
        try {
            this.data.save(this.dreamFile);
        } catch (IOException e) {
            this.plugin.getLogger().warning("golden_dream.yml: " + e.getMessage());
        }
    }

    /** Remembers everything the player had in the real world, once. */
    private void capture(Player p, boolean starter) {
        String k = p.getUniqueId().toString();
        if (this.data.contains(k)) return;
        this.data.set(k + ".inv", encode(p.getInventory().getContents()));
        this.data.set(k + ".ender", encode(p.getEnderChest().getContents()));
        this.data.set(k + ".lvl", p.getLevel());
        this.data.set(k + ".exp", p.getExp());
        this.data.set(k + ".loc", p.getLocation());
        this.data.set(k + ".intruded", false);
        this.data.set(k + ".starter", starter);
        this.data.set(k + ".realEnergy", this.plugin.getEnergyManager().getEnergy(p));
        this.data.set(k + ".realGem", this.plugin.getGemManager().getGemId(p));
        this.saveData();
    }

    static String encode(ItemStack[] items) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            out.writeInt(items.length);
            for (ItemStack i : items) out.writeObject(i);
            out.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    static ItemStack[] decode(String s) throws IOException, ClassNotFoundException {
        try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(s)))) {
            ItemStack[] items = new ItemStack[in.readInt()];
            for (int i = 0; i < items.length; i++) items[i] = (ItemStack) in.readObject();
            return items;
        }
    }

    // ---- worlds ----

    World pocket() {
        if (this.pocket != null) return this.pocket;
        this.pocket = Bukkit.getWorld(POCKET);
        if (this.pocket == null) {
            this.pocket = new WorldCreator(POCKET).environment(World.Environment.NORMAL).generator(new Walkway()).generateStructures(false).createWorld();
        }
        if (this.pocket == null) {
            this.plugin.getLogger().warning("Golden Dream: could not create world '" + POCKET + "'");
            return null;
        }
        this.pocket.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        this.pocket.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        this.pocket.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        this.pocket.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        this.pocket.setFullTime(6000L);
        this.pocket.setSpawnLocation(0, 65, 0);
        return this.pocket;
    }

    public boolean memorySetBuilt() {
        return this.overworld != null && this.nether != null && this.end != null;
    }

    World worldSet() {
        if (this.overworld == null) this.overworld = this.load(WORLD, World.Environment.NORMAL);
        if (this.nether == null) this.nether = this.load(NETHER, World.Environment.NETHER);
        if (this.end == null) this.end = this.load(END, World.Environment.THE_END);
        if (this.gradeTask == null && this.overworld != null) this.gradeTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::grade, 1L, 1L);
        return this.overworld;
    }

    private World load(String name, World.Environment env) {
        World w = Bukkit.getWorld(name);
        if (w != null) return w;
        w = new WorldCreator(name).environment(env).createWorld();
        if (w == null) this.plugin.getLogger().warning("Golden Dream: could not create world '" + name + "'");
        else this.plugin.getLogger().info("Golden Dream: world '" + name + "' ready");
        return w;
    }

    private boolean ours(World w) {
        return w != null && (w == this.overworld || w == this.nether || w == this.end);
    }

    public boolean isMemoryWorld(World w) {
        return this.ours(w);
    }

    public static boolean isPocketWorld(World w) {
        return w != null && w.getName().equals(POCKET);
    }

    public static boolean isDreamWorld(World w) {
        if (w == null) return false;
        String n = w.getName();
        return n.equals(POCKET) || n.equals(WORLD) || n.equals(NETHER) || n.equals(END);
    }

    public Location memoryArrival() {
        return this.arrival();
    }

    private Location arrival() {
        World w = this.worldSet();
        if (w == null) return null;
        Location s = w.getSpawnLocation();
        return new Location(w, s.getBlockX() + 0.5, w.getHighestBlockYAt(s.getBlockX(), s.getBlockZ()) + 1, s.getBlockZ() + 0.5);
    }

    public void forgetGrade(UUID id) {
        this.graded.remove(id);
        this.gradedDay.remove(id);
    }

    /** Keeps memory-world players under an endless dusk (a night storm while the thunder hangs) and sprinkles light motes. */
    private void grade() {
        if (Bukkit.getCurrentTick() % 20 == 0) this.proximityCue();
        boolean storming = Bukkit.getCurrentTick() < this.stormUntil;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!this.ours(p.getWorld())) {
                if (this.graded.remove(p.getUniqueId())) {
                    this.gradedDay.remove(p.getUniqueId());
                    DreamSky.reset(p);
                }
                continue;
            }
            long key = storming ? 1L : 0L;
            Long last = this.gradedDay.get(p.getUniqueId());
            if (this.graded.add(p.getUniqueId()) || last == null || last != key) {
                DreamSky.memory(p, storming);
                this.gradedDay.put(p.getUniqueId(), key);
            }
            int phase = Bukkit.getCurrentTick() % 5;
            p.getWorld().spawnParticle(LIGHT_MOTE, p.getLocation().add(0, 2, 0), phase == 1 || phase == 3 ? 3 : 4, 22.0, 9.0, 22.0, 0.0, null, true);
        }
    }

    /** Intruders within 12 blocks of a memory hear a whisper, at most every 20 s. */
    private void proximityCue() {
        if (!this.plugin.getConfig().getBoolean("golden-dream.memory-proximity-sound", true)) return;
        long now = System.currentTimeMillis();
        List<Location> memories = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!this.ours(p.getWorld()) || !this.intruded(p.getUniqueId()) || p.isDead()) continue;
            Long next = this.cueNext.get(p.getUniqueId());
            if (next != null && next > now) continue;
            if (memories == null) {
                memories = new ArrayList<>();
                for (UUID id : this.dream.roster().activeIds()) {
                    Player m = Bukkit.getPlayer(id);
                    if (m != null && !m.isDead()) memories.add(m.getLocation());
                }
                memories.addAll(this.dream.npcs().liveLocations());
            }
            Location at = p.getLocation();
            for (Location m : memories) {
                if (m.getWorld() == at.getWorld() && m.distanceSquared(at) <= 144.0) {
                    p.getWorld().playSound(at, "bliss:alternate", SoundCategory.MASTER, 1.0f, 1.0f);
                    this.cueNext.put(p.getUniqueId(), now + 20000L);
                    break;
                }
            }
        }
    }

    // ---- entering ----

    void enter(Player p, long prevOffset, boolean prevRelative, boolean starter) {
        World pocket = this.pocket();
        if (pocket == null) {
            DreamSky.restore(p, prevOffset, prevRelative);
            return;
        }
        if (this.intruded(p.getUniqueId())) {
            DreamSky.restore(p, prevOffset, prevRelative);
            Location at = this.arrival();
            if (at != null) {
                p.setVelocity(new Vector());
                p.teleport(at);
                p.setFallDistance(0);
            }
            return;
        }
        this.capture(p, starter);
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(null);
        this.plugin.getGemManager().updateActiveGem(p);
        this.worldSet();
        this.dropVisit(p.getUniqueId());
        Session old = this.sessions.remove(p.getUniqueId());
        if (old != null) {
            if (old.task != null) old.task.cancel();
            this.unlock(p, old);
        }
        Session s = new Session(p.getUniqueId(), prevOffset, prevRelative);
        this.sessions.put(s.pid, s);
        p.teleport(new Location(pocket, 0.5, 65.0, 0.5, -90.0f, 0.0f));
        p.setVelocity(new Vector());
        s.task = new BukkitRunnable() {
            @Override
            public void run() {
                GoldenDreamWorld self = GoldenDreamWorld.this;
                Player pl = Bukkit.getPlayer(s.pid);
                if (pl == null || !pl.isOnline() || pl.isDead() || pl.getWorld() != pocket) {
                    if (pl != null) {
                        self.unlock(pl, s);
                        if (!s.clockBack) pl.setPlayerTime(s.prevOffset, s.prevRel);
                    }
                    self.sessions.remove(s.pid);
                    this.cancel();
                    return;
                }
                if (s.t == 0) {
                    DreamSky.pocket(pl);
                    s.clockBack = true;
                } else if (s.t == 30) {
                    DreamSky.openEyes(pl);
                }
                pocket.spawnParticle(Particle.ASH, pl.getLocation().add(0, 1.5, 0), 4, 12.0, 7.2, 12.0, 0.0, null, true);
                keepOnWalkway(pl);
                if (s.auto && s.t == WALK_DELAY_TICKS) self.lock(pl, s);
                if (s.locked) pl.setVelocity(new Vector(WALK_SPEED, Math.min(0.0, pl.getVelocity().getY()), 0.0));
                if (s.t >= LEAVE_TICKS || s.skip) {
                    self.leave(pl, s);
                    this.cancel();
                    return;
                }
                s.t++;
            }
        }.runTaskTimer(this.plugin, 1L, 1L);
    }

    /** The walkway is 5 wide (z -2..2); anyone off it or below it is put back on top. */
    private static void keepOnWalkway(Player p) {
        Location at = p.getLocation();
        double z = Math.max(-1.7, Math.min(2.7, at.getZ()));
        if (Math.abs(at.getZ() - z) <= 1.0E-6 && at.getY() >= 64.98) return;
        at.setZ(z);
        at.setY(65.0);
        p.teleport(at);
        p.setVelocity(new Vector());
        p.setFallDistance(0);
    }

    public boolean skip(UUID id) {
        Session s = this.sessions.get(id);
        if (s == null) return false;
        s.skip = true;
        return true;
    }

    public boolean startWalk(UUID id) {
        Session s = this.sessions.get(id);
        Player p = s == null ? null : Bukkit.getPlayer(id);
        if (p == null) return false;
        s.auto = false;
        this.lock(p, s);
        return true;
    }

    public boolean stopWalk(UUID id) {
        Session s = this.sessions.get(id);
        Player p = s == null ? null : Bukkit.getPlayer(id);
        if (p == null) return false;
        s.auto = false;
        this.unlock(p, s);
        return true;
    }

    public List<UUID> walking() {
        return new ArrayList<>(this.sessions.keySet());
    }

    // ---- admin visits to the pocket ----

    public boolean isVisiting(UUID id) {
        return this.visits.containsKey(id);
    }

    public String pocketTp(Player p) {
        UUID id = p.getUniqueId();
        if (this.sessions.containsKey(id) || this.inDream(id)) return p.getName() + " is in the dream - the ritual owns them.";
        if (this.visits.containsKey(id)) return p.getName() + " is already in the pocket.";
        World pocket = this.pocket();
        if (pocket == null) return "The pocket world could not be created.";
        Visit v = new Visit(id, p.getLocation().clone());
        this.visits.put(id, v);
        this.data.set("pocketVisits." + id + ".back", v.back);
        this.saveData();
        p.setVelocity(new Vector());
        if (!p.teleport(new Location(pocket, 0.5, 65.0, 0.5, -90.0f, 0.0f))) {
            this.dropVisit(id);
            return p.getName() + " could not be moved right now.";
        }
        p.setFallDistance(0);
        this.arm(v);
        return null;
    }

    public boolean pocketKick(Player p) {
        Visit v = this.visits.containsKey(p.getUniqueId()) ? this.visits.get(p.getUniqueId()) : this.loadVisit(p.getUniqueId());
        if (v == null) return false;
        Location back = v.back != null && v.back.isWorldLoaded() ? v.back : Bukkit.getWorlds().get(0).getSpawnLocation();
        p.setVelocity(new Vector());
        if (!p.teleport(back)) return false;
        this.dropVisit(p.getUniqueId());
        p.setFallDistance(0);
        return true;
    }

    private void arm(Visit v) {
        World pocket = this.pocket();
        if (pocket == null) return;
        if (v.task != null) v.task.cancel();
        v.task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            Player p = Bukkit.getPlayer(v.pid);
            if (p == null || !p.isOnline()) {
                v.task.cancel();
                v.task = null;
                return;
            }
            if (p.getWorld() != pocket) {
                this.dropVisit(v.pid);
                return;
            }
            if (!p.isDead()) keepOnWalkway(p);
        }, 1L, 1L);
    }

    private Visit loadVisit(UUID id) {
        Visit v = this.visits.get(id);
        if (v != null) return v;
        if (!this.data.contains("pocketVisits." + id)) return null;
        v = new Visit(id, this.data.getLocation("pocketVisits." + id + ".back"));
        this.visits.put(id, v);
        return v;
    }

    private void dropVisit(UUID id) {
        Visit v = this.visits.remove(id);
        if (v != null && v.task != null) v.task.cancel();
        if (this.data.contains("pocketVisits." + id)) {
            this.data.set("pocketVisits." + id, null);
            this.saveData();
        }
    }

    // ---- into the memory ----

    private void leave(Player p, Session s) {
        this.unlock(p, s);
        p.setPlayerTime(s.prevOffset, s.prevRel);
        this.sessions.remove(s.pid);
        Location at = this.arrival();
        if (at == null) {
            this.plugin.getLogger().warning("Golden Dream: no world to send " + p.getName() + " to");
            return;
        }
        p.setVelocity(new Vector());
        p.teleport(at);
        p.setFallDistance(0);
        this.intruder(p);
    }

    /** First arrival in the memory: a clean slate and a random gem other than their real one, at Pristine. */
    private void intruder(Player p) {
        if (this.intruded(p.getUniqueId())) return;
        this.data.set(p.getUniqueId() + ".intruded", true);
        this.saveData();
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(null);
        p.getEnderChest().clear();
        p.setLevel(0);
        p.setExp(0);
        p.setTotalExperience(0);
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setAbsorptionAmount(0);
        p.setArrowsInBody(0);
        p.setFreezeTicks(0);
        p.setRemainingAir(p.getMaximumAir());
        @SuppressWarnings("deprecation") AttributeInstance hp = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        p.setHealth(hp != null ? hp.getValue() : 20.0);
        p.setFoodLevel(20);
        p.setSaturation(5.0f);
        p.setExhaustion(0);
        String real = this.data.getString(p.getUniqueId() + ".realGem");
        List<String> pool = new ArrayList<>(this.plugin.getGemManager().getAvailableGemIds());
        pool.remove("gold");
        if (pool.size() > 1 && real != null) pool.remove(real);
        if (!pool.isEmpty()) {
            String gem = pool.get(new Random().nextInt(pool.size()));
            this.plugin.getEnergyManager().setEnergy(p, EnergyState.PRISTINE.getMinEnergy());
            this.plugin.getGemManager().giveGem(p, gem, 1);
            this.plugin.getLogger().info("Golden Dream: " + p.getName() + " enters the memory as an intruder with " + gem);
        }
    }

    /** Leaves the dream for good: items, place, gem and energy restored; the starter gets the Fragment Core. */
    public boolean wake(Player p) {
        String k = p.getUniqueId().toString();
        if (!this.data.contains(k) || p.isDead()) return false;
        Session s = this.sessions.remove(p.getUniqueId());
        if (s != null) {
            if (s.task != null) s.task.cancel();
            this.unlock(p, s);
        }
        this.graded.remove(p.getUniqueId());
        DreamSky.reset(p);
        try {
            String inv = this.data.getString(k + ".inv");
            if (inv != null) p.getInventory().setContents(decode(inv));
            String ender = this.data.getString(k + ".ender");
            if (ender != null) p.getEnderChest().setContents(decode(ender));
            p.setLevel(this.data.getInt(k + ".lvl", 0));
            p.setExp((float) this.data.getDouble(k + ".exp", 0.0));
        } catch (Exception e) {
            this.plugin.getLogger().warning("Golden Dream: could not restore " + p.getName() + "'s items (" + e.getMessage() + ")");
        }
        if (this.data.contains(k + ".realEnergy")) this.plugin.getEnergyManager().setEnergy(p, this.data.getInt(k + ".realEnergy"));
        Location back = this.data.getLocation(k + ".loc");
        boolean starter = this.data.getBoolean(k + ".starter", false);
        this.data.set(k, null);
        this.saveData();
        if (back != null) {
            p.setVelocity(new Vector());
            p.teleport(back);
            p.setFallDistance(0);
        }
        this.plugin.getGemManager().updateActiveGem(p);
        if (starter) {
            for (ItemStack left : p.getInventory().addItem(RitualItems.fragmentCore()).values()) p.getWorld().dropItemNaturally(p.getLocation(), left);
        }
        this.plugin.getLogger().info("Golden Dream: " + p.getName() + " wakes from the dream" + (starter ? " holding the Fragment Core" : ""));
        return true;
    }

    // ---- walking lock ----

    @SuppressWarnings("deprecation")
    private void lock(Player p, Session s) {
        if (s.locked) return;
        AttributeInstance jump = p.getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
        s.prevWalk = p.getWalkSpeed();
        s.prevFlight = p.getAllowFlight();
        s.prevJump = jump != null ? jump.getBaseValue() : 0.42;
        p.setWalkSpeed(0);
        p.setFlying(false);
        p.setAllowFlight(false);
        if (jump != null) jump.setBaseValue(0);
        p.getPersistentDataContainer().set(this.locked, PersistentDataType.BYTE, (byte) 1);
        s.locked = true;
    }

    @SuppressWarnings("deprecation")
    private void unlock(Player p, Session s) {
        if (!s.locked) return;
        p.setWalkSpeed(s.prevWalk);
        p.setAllowFlight(s.prevFlight);
        AttributeInstance jump = p.getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
        if (jump != null) jump.setBaseValue(s.prevJump);
        p.getPersistentDataContainer().remove(this.locked);
        s.locked = false;
    }

    @SuppressWarnings("deprecation")
    private void unlockDefaults(Player p) {
        p.setWalkSpeed(0.2f);
        AttributeInstance jump = p.getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
        if (jump != null) jump.setBaseValue(jump.getDefaultValue());
        p.setAllowFlight(p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR);
        p.getPersistentDataContainer().remove(this.locked);
    }

    public void shutdown() {
        for (Session s : this.sessions.values()) {
            if (s.task != null) s.task.cancel();
            Player p = Bukkit.getPlayer(s.pid);
            if (p != null) {
                this.unlock(p, s);
                DreamSky.restore(p, s.prevOffset, s.prevRel);
            }
        }
        this.sessions.clear();
        for (Visit v : this.visits.values()) if (v.task != null) v.task.cancel();
        this.visits.clear();
        if (this.gradeTask != null) this.gradeTask.cancel();
        this.gradeTask = null;
        this.graded.clear();
        for (Player p : Bukkit.getOnlinePlayers()) if (p.getWorld() == this.pocket || this.ours(p.getWorld())) DreamSky.reset(p);
    }

    // ---- events ----

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        AttributeInstance jump = p.getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
        boolean frozen = p.getWalkSpeed() == 0.0f && jump != null && jump.getBaseValue() <= 0.0;
        if ((p.getPersistentDataContainer().has(this.locked, PersistentDataType.BYTE) || frozen) && !this.sessions.containsKey(p.getUniqueId())) {
            this.unlockDefaults(p);
            DreamSky.reset(p);
            this.plugin.getLogger().info("Golden Dream: undid a leftover movement lock on " + p.getName());
        }
        if (!p.hasGravity()) {
            p.setGravity(true);
            this.plugin.getLogger().info("Golden Dream: restored gravity for " + p.getName());
        }
        UUID id = p.getUniqueId();
        if (this.data.getBoolean(id + ".wakeOnJoin", false)) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                if (p.isOnline() && !p.isDead()) this.wake(p);
            }, 20L);
            return;
        }
        if (this.inDream(id)) {
            if (!this.sessions.containsKey(id)) Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                if (!p.isOnline() || !this.inDream(id) || this.sessions.containsKey(id)) return;
                if (!this.intruded(id)) {
                    this.enter(p, p.getPlayerTimeOffset(), p.isPlayerTimeRelative(), false);
                } else if (!this.ours(p.getWorld())) {
                    Location at = this.arrival();
                    if (at != null) {
                        p.teleport(at);
                        p.setFallDistance(0);
                    }
                }
            }, 20L);
            return;
        }
        if (this.data.contains("pocketVisits." + id)) {
            if (p.getWorld().getName().equals(POCKET)) {
                Visit v = this.loadVisit(id);
                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    if (p.isOnline() && v != null) this.arm(v);
                }, 20L);
            } else {
                this.dropVisit(id);
            }
        } else if (p.getWorld().getName().equals(POCKET)) {
            Location at = this.arrival();
            if (at != null) Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (p.isOnline()) {
                    p.teleport(at);
                    p.setFallDistance(0);
                }
            });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        DreamSky.forget(p.getUniqueId());
        this.graded.remove(p.getUniqueId());
        this.gradedDay.remove(p.getUniqueId());
        Session s = this.sessions.remove(p.getUniqueId());
        if (s != null) {
            if (s.task != null) s.task.cancel();
            this.unlock(p, s);
            p.setPlayerTime(s.prevOffset, s.prevRel);
        }
    }

    /** The memory's own nether and end connect to each other, never to the real worlds. */
    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        World from = event.getFrom().getWorld();
        if (!this.ours(from)) return;
        Location at = event.getFrom();
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            if (from == this.overworld && this.nether != null) {
                event.setTo(new Location(this.nether, at.getX() / 8.0, Math.min(Math.max(at.getY(), 10.0), 120.0), at.getZ() / 8.0, at.getYaw(), at.getPitch()));
            } else if (from == this.nether && this.overworld != null) {
                event.setTo(new Location(this.overworld, at.getX() * 8.0, at.getY(), at.getZ() * 8.0, at.getYaw(), at.getPitch()));
            } else {
                return;
            }
            event.setCanCreatePortal(true);
            event.setSearchRadius(128);
            event.setCreationRadius(16);
        } else if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            if (from == this.overworld && this.end != null) {
                platform(this.end);
                event.setTo(new Location(this.end, 100.5, 49.0, 0.5, 90.0f, 0.0f));
            } else if (from == this.end && this.overworld != null) {
                Location a = this.arrival();
                if (a != null) event.setTo(a);
            }
        }
    }

    private static void platform(World end) {
        for (int x = 98; x <= 102; x++) {
            for (int z = -2; z <= 2; z++) {
                end.getBlockAt(x, 48, z).setType(Material.OBSIDIAN, false);
                for (int y = 49; y <= 51; y++) end.getBlockAt(x, y, z).setType(Material.AIR, false);
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();
        World w = p.getWorld();
        if (this.pocket != null && w == this.pocket) {
            event.setRespawnLocation(new Location(this.pocket, 0.5, 65.0, 0.5, -90.0f, 0.0f));
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (p.isOnline() && !p.isDead() && this.inDream(p.getUniqueId()) && !this.sessions.containsKey(p.getUniqueId())) {
                    this.enter(p, p.getPlayerTimeOffset(), p.isPlayerTimeRelative(), false);
                }
            });
            return;
        }
        if (!this.ours(w)) return;
        if ((event.isBedSpawn() || event.isAnchorSpawn()) && event.getRespawnLocation().getWorld() != null && this.ours(event.getRespawnLocation().getWorld())) return;
        Location a = this.arrival();
        if (a != null) event.setRespawnLocation(a);
    }

    // ---- admin: reset and import ----

    private void evacuate() {
        Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        for (World w : new World[]{this.pocket, this.overworld, this.nether, this.end}) {
            if (w == null) continue;
            for (Player p : new ArrayList<>(w.getPlayers())) {
                Session s = this.sessions.remove(p.getUniqueId());
                if (s != null && s.task != null) s.task.cancel();
                if (p.getWalkSpeed() == 0.0f) this.unlockDefaults(p);
                this.graded.remove(p.getUniqueId());
                DreamSky.reset(p);
                p.teleport(spawn);
                p.setFallDistance(0);
            }
        }
    }

    /** Deletes and regenerates every dream world and drops every record; dreamers wake (offline ones on login). */
    public boolean resetAll(CommandSender sender) {
        for (Visit v : new ArrayList<>(this.visits.values())) {
            Player p = Bukkit.getPlayer(v.pid);
            if (p != null && p.isOnline() && !p.isDead()) this.pocketKick(p);
            else if (v.task != null) v.task.cancel();
        }
        this.visits.clear();
        this.evacuate();
        int woke = 0, later = 0;
        for (String key : new ArrayList<>(this.data.getKeys(false))) {
            if (key.equals("pocketVisits")) continue;
            Player p = null;
            try {
                p = Bukkit.getPlayer(UUID.fromString(key));
            } catch (IllegalArgumentException ignored) {
            }
            if (p != null && p.isOnline() && !p.isDead()) {
                if (this.wake(p)) woke++;
            } else {
                this.data.set(key + ".wakeOnJoin", true);
                later++;
            }
        }
        this.saveData();
        if (this.gradeTask != null) this.gradeTask.cancel();
        this.gradeTask = null;
        this.graded.clear();
        boolean ok = this.wipe(POCKET) & this.wipe(END) & this.wipe(NETHER) & this.wipe(WORLD);
        this.pocket = this.overworld = this.nether = this.end = null;
        this.pocket();
        this.worldSet();
        if (sender != null) {
            sender.sendMessage(dev.xoperr.blissgems.pedestal.PedestalManager.color(ok
                ? "&aGolden Dream reset — fresh worlds. Woke &f" + woke + "&a; &f" + later + "&a offline dreamer(s) will wake on their next login."
                : "&eGolden Dream reset, but a world folder was locked — restart to finish clearing it."));
        }
        return ok;
    }

    /** Copies /goldendream_import in as the memory overworld. */
    public boolean importOverworld(CommandSender sender) {
        File src = new File(Bukkit.getWorldContainer(), IMPORT_FOLDER);
        if (!src.isDirectory() || (!new File(src, "level.dat").isFile() && !new File(src, "region").isDirectory())) {
            if (sender != null) sender.sendMessage(dev.xoperr.blissgems.pedestal.PedestalManager.color("&cNo world found at &f" + IMPORT_FOLDER + "&c. Put a world folder there first."));
            return false;
        }
        this.evacuate();
        if (!this.wipe(WORLD)) {
            this.overworld = this.load(WORLD, World.Environment.NORMAL);
            if (sender != null) sender.sendMessage(dev.xoperr.blissgems.pedestal.PedestalManager.color("&eCould not clear the old goldenworld (locked) — restart, then import again."));
            return false;
        }
        this.overworld = null;
        File dst = new File(Bukkit.getWorldContainer(), WORLD);
        try {
            copyTree(src.toPath(), dst.toPath());
            new File(dst, "uid.dat").delete();
            new File(dst, "session.lock").delete();
        } catch (IOException e) {
            if (sender != null) sender.sendMessage(dev.xoperr.blissgems.pedestal.PedestalManager.color("&cImport copy failed: &f" + e.getMessage()));
            return false;
        }
        this.overworld = this.load(WORLD, World.Environment.NORMAL);
        if (this.overworld != null && this.gradeTask == null) this.gradeTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::grade, 1L, 1L);
        if (sender != null) sender.sendMessage(dev.xoperr.blissgems.pedestal.PedestalManager.color(this.overworld != null
            ? "&aImported &f" + IMPORT_FOLDER + "&a as the memory overworld." : "&cImport copied but the world would not load."));
        return this.overworld != null;
    }

    private boolean wipe(String name) {
        World w = Bukkit.getWorld(name);
        if (w != null && !Bukkit.unloadWorld(w, false)) {
            this.plugin.getLogger().warning("Golden Dream: could not unload world '" + name + "' (players still in it?)");
            return false;
        }
        File dir = new File(Bukkit.getWorldContainer(), name);
        if (!dir.exists()) return true;
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            this.plugin.getLogger().warning("Golden Dream: could not delete world '" + name + "': " + e.getMessage());
            return false;
        }
        return !dir.exists();
    }

    private static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> walk = Files.walk(from)) {
            for (Path src : (Iterable<Path>) walk::iterator) {
                Path dst = to.resolve(from.relativize(src).toString());
                if (Files.isDirectory(src)) Files.createDirectories(dst);
                else Files.copy(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    // ---- types ----

    /** Void world with a 5-wide invisible barrier walkway along x at y 60-64. */
    static final class Walkway extends ChunkGenerator {
        @Override
        public void generateSurface(WorldInfo info, Random random, int chunkX, int chunkZ, ChunkData data) {
            int baseZ = chunkZ << 4;
            for (int z = baseZ; z < baseZ + 16; z++) {
                if (z < -2 || z > 2) continue;
                for (int x = 0; x < 16; x++) for (int y = 60; y <= 64; y++) data.setBlock(x, y, z - baseZ, Material.BARRIER);
            }
        }

        @Override public boolean shouldGenerateNoise() { return false; }
        @Override public boolean shouldGenerateSurface() { return true; }
        @Override public boolean shouldGenerateCaves() { return false; }
        @Override public boolean shouldGenerateDecorations() { return false; }
        @Override public boolean shouldGenerateMobs() { return false; }
        @Override public boolean shouldGenerateStructures() { return false; }

        /** All the_void: the bliss_skies datapack paints that biome's sky gold. */
        @Override
        public BiomeProvider getDefaultBiomeProvider(WorldInfo info) {
            return new BiomeProvider() {
                @Override
                public Biome getBiome(WorldInfo w, int x, int y, int z) {
                    return Biome.THE_VOID;
                }

                @Override
                public List<Biome> getBiomes(WorldInfo w) {
                    return List.of(Biome.THE_VOID);
                }
            };
        }

        @Override
        public Location getFixedSpawnLocation(World world, Random random) {
            return new Location(world, 0.5, 65.0, 0.5);
        }
    }

    private static final class Session {
        final UUID pid;
        final long prevOffset;
        final boolean prevRel;
        float prevWalk = 0.2f;
        boolean prevFlight;
        double prevJump = 0.42;
        boolean locked;
        boolean clockBack;
        boolean skip;
        boolean auto = true;
        int t;
        BukkitTask task;

        Session(UUID pid, long prevOffset, boolean prevRel) {
            this.pid = pid;
            this.prevOffset = prevOffset;
            this.prevRel = prevRel;
        }
    }

    private static final class Visit {
        final UUID pid;
        final Location back;
        BukkitTask task;

        Visit(UUID pid, Location back) {
            this.pid = pid;
            this.back = back;
        }
    }
}
