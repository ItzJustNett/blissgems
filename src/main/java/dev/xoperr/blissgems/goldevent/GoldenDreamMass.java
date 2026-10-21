package dev.xoperr.blissgems.goldevent;

import dev.xoperr.blissgems.BlissGems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * The golden mass: an orb of gold motes that grows over 10 s, then for 20 s pulls everything within
 * 40 blocks in - mobs and items are eaten, terrain is peeled off as glowing block displays, a well is
 * dug beneath it - and every player who touches it closes their eyes and falls into the dream.
 */
final class GoldenDreamMass {
    static final int SILENCE_TICKS = 200;
    static final int PULL_TICKS = 400;
    private static final double ORB_START_R = 0.3;
    private static final double ORB_R = 2.94;
    private static final int MOTES = 200;
    private static final double PULL_REACH = 40.0;
    private static final int PLAYER_PULL_DELAY = 120;
    private static final int BLOCKS_PER_TICK = 120;
    private static final int BLOCK_TRIES = 400;
    private static final int FLY_CAP = 600;
    private static final double WELL_R = 7.0;
    private static final int WELL_DEPTH = 64;
    private static final int WELL_PER_TICK = 200;
    private static final int EYE_TICKS = 70;
    private static final int BREAK_SOUNDS_PER_TICK = 12;
    static final Color HOLDER_GOLD = Color.fromRGB(255, 205, 90);

    private final BlissGems plugin;
    private final GoldenDreamRitual owner;
    private final Location centre;
    private final UUID trigger;
    private final boolean terrain;
    private final Vector[] orbitU = new Vector[MOTES];
    private final Vector[] orbitV = new Vector[MOTES];
    private final double[] motePhase = new double[MOTES];
    private final double[] moteSpeed = new double[MOTES];
    private final double[] moteR = new double[MOTES];
    private final List<Flying> flying = new ArrayList<>();
    private final Map<Long, int[]> peeled = new HashMap<>();
    private final Map<UUID, Prev> clocks = new HashMap<>();
    private final Set<UUID> veiled = new HashSet<>();
    private final Map<UUID, Integer> eyeK = new HashMap<>();
    private final List<Location> shield = new ArrayList<>();
    private final Map<Long, Integer> wellRoof = new HashMap<>();
    private BukkitTask task;
    private int crumbled;
    private int breakSoundTick = -1;
    private int breakSounds;

    GoldenDreamMass(BlissGems plugin, GoldenDreamRitual owner, Location at, UUID trigger, long triggerOffset, boolean triggerRelative) {
        this.plugin = plugin;
        // the ritual leaves the trigger's sky on the dome; remember their real time for afterwards
        this.clocks.put(trigger, new Prev(triggerOffset, triggerRelative));
        this.owner = owner;
        this.centre = at.clone().add(0, 4.0, 0);
        this.trigger = trigger;
        this.terrain = plugin.getConfig().getBoolean("golden-dream.mass-destroys-terrain", true);
    }

    private static double orbRadius(int t) {
        if (t >= SILENCE_TICKS) return ORB_R * (1.0 + 0.03 * Math.sin(t * 0.15));
        double f = t / (double) SILENCE_TICKS;
        return ORB_START_R + (ORB_R - ORB_START_R) * f * f * (3.0 - 2.0 * f);
    }

    void schedule(long delay) {
        Bukkit.getScheduler().runTaskLater(this.plugin, this::start, delay);
    }

    private void start() {
        this.owner.entryStarted(this.trigger);
        if (!this.centre.isWorldLoaded()) {
            this.owner.finish(this.trigger);
            return;
        }
        this.owner.register(this);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < MOTES; i++) {
            Vector n = randomUnit(r);
            Vector u = n.clone().crossProduct(new Vector(0, 1, 0));
            if (u.lengthSquared() < 1.0E-6) u = new Vector(1, 0, 0);
            u.normalize();
            this.orbitU[i] = u;
            this.orbitV[i] = n.clone().crossProduct(u).normalize();
            this.motePhase[i] = r.nextDouble(0.0, Math.PI * 2);
            this.moteSpeed[i] = r.nextDouble(0.15, 0.35) * (r.nextBoolean() ? 1 : -1);
            this.moteR[i] = 0.15 + 0.85 * Math.cbrt(r.nextDouble());
        }
        this.plugin.getLogger().info("Golden Dream: the mass forms at " + this.centre.getWorld().getName() + " "
            + Math.round(this.centre.getX()) + "," + Math.round(this.centre.getY()) + "," + Math.round(this.centre.getZ()));
        this.task = new BukkitRunnable() {
            int t;

            @Override
            public void run() {
                GoldenDreamMass m = GoldenDreamMass.this;
                if (!m.centre.isWorldLoaded()) {
                    m.end(false);
                    this.cancel();
                    return;
                }
                World w = m.centre.getWorld();
                double radius = orbRadius(this.t);
                m.drawMass(w, this.t, radius);
                if (this.t >= SILENCE_TICKS) {
                    int pt = this.t - SILENCE_TICKS;
                    m.pullEntities(w, radius, pt);
                    m.shield.clear();
                    if (pt < PLAYER_PULL_DELAY) {
                        for (Player p : w.getPlayers()) if (!m.eyeK.containsKey(p.getUniqueId())) m.shield.add(p.getLocation());
                    }
                    if (m.terrain) {
                        m.digWell(w);
                        m.ripBlocks(w);
                    }
                    if (this.t % 2 == 0) m.flyBlocks(w, radius);
                    m.tickVeils();
                }
                m.tickEyes();
                if (this.t >= SILENCE_TICKS + PULL_TICKS) {
                    m.end(true);
                    this.cancel();
                    return;
                }
                this.t++;
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
    }

    private void drawMass(World w, int t, double radius) {
        double f = Math.min(1.0, radius / ORB_R);
        double spread = 0.25 * f;
        int motes = 60 + (int) Math.round(140.0 * f * f);
        Particle.DustOptions dust = new Particle.DustOptions(HOLDER_GOLD, 1.0f + 0.8f * (float) f);
        for (int i = 0; i < motes; i++) {
            double a = this.motePhase[i] + t * this.moteSpeed[i];
            double rr = radius * this.moteR[i];
            Vector u = this.orbitU[i], v = this.orbitV[i];
            w.spawnParticle(Particle.DUST,
                this.centre.getX() + (u.getX() * Math.cos(a) + v.getX() * Math.sin(a)) * rr,
                this.centre.getY() + (u.getY() * Math.cos(a) + v.getY() * Math.sin(a)) * rr,
                this.centre.getZ() + (u.getZ() * Math.cos(a) + v.getZ() * Math.sin(a)) * rr,
                1 + (int) Math.round(f), spread, spread, spread, 0.0, dust, true);
        }
        if (t % 4 == 0) {
            Vector d = randomUnit(ThreadLocalRandom.current());
            double far = radius + 3.0;
            w.spawnParticle(Particle.END_ROD, this.centre.clone().add(d.clone().multiply(far)), 0, -d.getX(), -d.getY(), -d.getZ(), far / 11.0, null, true);
        }
    }

    private void pullEntities(World w, double radius, int pt) {
        double contact = radius + 0.5;
        for (Entity e : w.getNearbyEntities(this.centre, PULL_REACH, PULL_REACH, PULL_REACH)) {
            if (e instanceof Display || e instanceof Interaction || e instanceof Marker || e instanceof Hanging || e instanceof ArmorStand || e.hasMetadata("NPC")) continue;
            Location at = e.getLocation();
            double d2 = at.distanceSquared(this.centre);
            if (e instanceof Player p) {
                if (p.getGameMode() == GameMode.SPECTATOR || this.eyeK.containsKey(p.getUniqueId())) continue;
                this.clocks.putIfAbsent(p.getUniqueId(), new Prev(p.getPlayerTimeOffset(), p.isPlayerTimeRelative()));
                if (d2 < contact * contact) {
                    this.veiled.remove(p.getUniqueId());
                    this.eyeK.put(p.getUniqueId(), 0);
                    continue;
                }
                this.veiled.add(p.getUniqueId());
                if (pt < PLAYER_PULL_DELAY) continue;
            } else if (d2 < radius * radius) {
                this.eat(w, at);
                e.remove();
                continue;
            }
            Vector to = this.centre.toVector().subtract(at.toVector());
            double len = to.length();
            if (len >= 0.001 && len <= PULL_REACH) e.setVelocity(to.multiply((0.15 + 0.4 * (1.0 - Math.min(1.0, len / PULL_REACH))) / len));
        }
    }

    private void eat(World w, Location at) {
        w.spawnParticle(Particle.DUST, at, 8, 0.3, 0.3, 0.3, 0.0, new Particle.DustOptions(HOLDER_GOLD, 1.8f), true);
    }

    private void ripBlocks(World w) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int taken = 0;
        for (int i = 0; i < BLOCK_TRIES && taken < BLOCKS_PER_TICK; i++) {
            double a = r.nextDouble(0.0, Math.PI * 2);
            double d = PULL_REACH * Math.sqrt(r.nextDouble());
            int x = this.centre.getBlockX() + (int) Math.round(Math.cos(a) * d);
            int z = this.centre.getBlockZ() + (int) Math.round(Math.sin(a) * d);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            Block b = w.getHighestBlockAt(x, z, HeightMap.WORLD_SURFACE);
            if (!pullable(b) || this.shielded(b)) continue;
            int[] peel = this.peeled.computeIfAbsent(key(x, z), k -> new int[]{b.getY(), 3 + r.nextInt(3)});
            if (b.getY() > peel[0] - peel[1]) {
                taken++;
                this.take(w, b);
            }
        }
    }

    private void digWell(World w) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int top = this.centre.getBlockY();
        for (int i = 0; i < WELL_PER_TICK; i++) {
            double a = r.nextDouble(0.0, Math.PI * 2);
            double d = WELL_R * Math.sqrt(r.nextDouble());
            int x = this.centre.getBlockX() + (int) Math.round(Math.cos(a) * d);
            int z = this.centre.getBlockZ() + (int) Math.round(Math.sin(a) * d);
            if (!w.isChunkLoaded(x >> 4, z >> 4)) continue;
            long k = key(x, z);
            Integer roof = this.wellRoof.get(k);
            int from = roof == null ? top : Math.min(top, roof + 3);
            for (int y = from; y >= Math.max(w.getMinHeight(), top - WELL_DEPTH); y--) {
                Block b = w.getBlockAt(x, y, z);
                if (b.getType().isAir()) continue;
                if (pullable(b) && !this.shielded(b)) {
                    this.wellRoof.put(k, y);
                    this.take(w, b);
                }
                break;
            }
        }
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private void take(World w, Block b) {
        BlockData data = b.getBlockData();
        if (this.flying.size() < FLY_CAP) {
            BlockDisplay d = w.spawn(b.getLocation(), BlockDisplay.class, bd -> {
                bd.setBlock(data);
                bd.setPersistent(false);
                bd.setTeleportDuration(2);
                bd.setBrightness(new Display.Brightness(15, 15));
                bd.setGlowing(true);
                bd.setGlowColorOverride(HOLDER_GOLD);
            });
            for (UUID id : this.eyeK.keySet()) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) p.hideEntity(this.plugin, d);
            }
            this.flying.add(new Flying(d, 0.12));
        } else if (++this.crumbled % 3 == 0) {
            w.spawnParticle(Particle.BLOCK, b.getLocation().add(0.5, 0.5, 0.5), 6, 0.3, 0.3, 0.3, 0.0, data, true);
        }
        int now = Bukkit.getCurrentTick();
        if (now != this.breakSoundTick) {
            this.breakSoundTick = now;
            this.breakSounds = 0;
        }
        if (this.breakSounds++ < BREAK_SOUNDS_PER_TICK) {
            w.playSound(b.getLocation(), data.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 1.0f, 0.8f);
        }
        b.setBlockData(Material.AIR.createBlockData(), true);
    }

    private boolean shielded(Block b) {
        for (Location at : this.shield) {
            double dx = b.getX() + 0.5 - at.getX(), dz = b.getZ() + 0.5 - at.getZ();
            if (dx * dx + dz * dz <= 4.0 && b.getY() <= at.getBlockY() + 1 && b.getY() >= at.getBlockY() - 6) return true;
        }
        return false;
    }

    private static boolean pullable(Block b) {
        Material m = b.getType();
        return !m.isAir() && !b.isLiquid() && m.getHardness() >= 0.0f && !(b.getState() instanceof TileState);
    }

    private void flyBlocks(World w, double radius) {
        Vector target = this.centre.toVector().subtract(new Vector(0.5, 0.5, 0.5));
        for (Iterator<Flying> it = this.flying.iterator(); it.hasNext(); ) {
            Flying f = it.next();
            if (!f.d.isValid()) {
                it.remove();
                continue;
            }
            Location at = f.d.getLocation();
            Vector to = target.clone().subtract(at.toVector());
            double len = to.length();
            if (len < radius) {
                this.eat(w, at.clone().add(0.5, 0.5, 0.5));
                f.d.remove();
                it.remove();
                continue;
            }
            at.add(to.multiply(Math.min(f.speed * 2.0, len) / len));
            f.d.teleport(at);
            f.speed = Math.min(3.0, f.speed * 1.06 * 1.06);
        }
    }

    /** Players within reach see a frozen golden-hour sky (and golden fog on Bedrock). */
    private void tickVeils() {
        for (Iterator<UUID> it = this.veiled.iterator(); it.hasNext(); ) {
            UUID id = it.next();
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline() && p.getWorld() == this.centre.getWorld() && p.getLocation().distanceSquared(this.centre) <= PULL_REACH * PULL_REACH) {
                DreamSky.golden(p);
                continue;
            }
            Prev prev = this.clocks.remove(id);
            if (p != null && prev != null) DreamSky.restore(p, prev.offset, prev.relative);
            it.remove();
        }
    }

    /** Touched players freeze, go blind (eyes closed) for 70 ticks, then enter the dream. */
    private void tickEyes() {
        for (Iterator<Map.Entry<UUID, Integer>> it = this.eyeK.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Integer> e = it.next();
            Player p = Bukkit.getPlayer(e.getKey());
            if (p == null || !p.isOnline() || p.isDead()) {
                Prev prev = this.clocks.remove(e.getKey());
                if (p != null) {
                    p.setGravity(true);
                    DreamSky.openEyes(p);
                    if (prev != null) DreamSky.restore(p, prev.offset, prev.relative);
                }
                it.remove();
                continue;
            }
            int k = e.getValue();
            if (k == 0) {
                p.setGravity(false);
                DreamSky.closeEyes(p, EYE_TICKS + 40);
                for (Flying f : this.flying) if (f.d.isValid()) p.hideEntity(this.plugin, f.d);
            }
            p.setVelocity(new Vector());
            if (k >= EYE_TICKS) {
                p.setGravity(true);
                Prev prev = this.clocks.remove(e.getKey());
                it.remove();
                this.owner.world().enter(p, prev != null ? prev.offset : 0L, prev == null || prev.relative, e.getKey().equals(this.trigger));
            } else {
                e.setValue(k + 1);
            }
        }
    }

    private void end(boolean finished) {
        if (this.task != null) this.task.cancel();
        this.task = null;
        for (Flying f : this.flying) if (f.d.isValid()) f.d.remove();
        this.flying.clear();
        this.peeled.clear();
        this.wellRoof.clear();
        for (Map.Entry<UUID, Integer> e : new ArrayList<>(this.eyeK.entrySet())) {
            Player p = Bukkit.getPlayer(e.getKey());
            Prev prev = this.clocks.remove(e.getKey());
            if (p == null || !p.isOnline()) continue;
            p.setGravity(true);
            if (finished && !p.isDead()) {
                this.owner.world().enter(p, prev != null ? prev.offset : 0L, prev == null || prev.relative, e.getKey().equals(this.trigger));
            } else {
                DreamSky.openEyes(p);
                if (prev != null) DreamSky.restore(p, prev.offset, prev.relative);
            }
        }
        this.eyeK.clear();
        for (Map.Entry<UUID, Prev> e : this.clocks.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null && p.isOnline()) DreamSky.restore(p, e.getValue().offset, e.getValue().relative);
        }
        this.clocks.clear();
        this.veiled.clear();
        this.owner.unregister(this);
        this.owner.finish(this.trigger);
    }

    void abortNow() {
        this.end(false);
    }

    private static Vector randomUnit(ThreadLocalRandom r) {
        double y = r.nextDouble(-1.0, 1.0);
        double a = r.nextDouble(0.0, Math.PI * 2);
        double s = Math.sqrt(1.0 - y * y);
        return new Vector(s * Math.cos(a), y, s * Math.sin(a));
    }

    private static final class Flying {
        final BlockDisplay d;
        double speed;

        Flying(BlockDisplay d, double speed) {
            this.d = d;
            this.speed = speed;
        }
    }

    private record Prev(long offset, boolean relative) {
    }
}
