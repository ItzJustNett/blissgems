package dev.xoperr.blissgems.villagerevent;

import dev.xoperr.blissgems.BlissGems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Phase 1: above the spawn pedestal three egg-shaped auras (one per village colour) trace
 * themselves for 13 s, then four stars spiral in while the core charges under lightning,
 * it bursts at tick 460 and drops the "Disc of the Wanderer", and settles in smoke.
 */
public final class DiscSummonRitual {
    private static final int ARM_START = 260;
    private static final int BURST = 460;
    private static final int SETTLE_END = 550;
    private static final Color ARM_COLOR = Color.fromRGB(245, 245, 255);
    private static final Particle.DustOptions PINK = new Particle.DustOptions(Color.fromRGB(255, 105, 180), 1.2f);
    private static final Particle.DustOptions MAGENTA = new Particle.DustOptions(Color.fromRGB(218, 112, 214), 1.2f);
    private static final Particle.DustOptions CORE = new Particle.DustOptions(Color.fromRGB(255, 205, 90), 1.2f);
    private static final Particle.DustOptions EMBER = new Particle.DustOptions(Color.fromRGB(220, 60, 30), 1.4f);
    private static final Particle.DustOptions STAR_CORE = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);
    private static long busyUntil;

    private final BlissGems plugin;
    private final VillagerEventItems items;
    private final Random random = new Random();
    private final Map<Integer, List<double[]>> eggArcs = new HashMap<>(); // per egg: list of {headAngle, circleIndex}
    private final Map<Integer, List<Circle>> eggCircles = new HashMap<>();
    private double starAngle;

    public DiscSummonRitual(BlissGems plugin, VillagerEventItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    public static boolean isRunning() {
        return Bukkit.getCurrentTick() < busyUntil;
    }

    /** Returns false when there is no spawn pedestal to run it on. */
    public boolean summon(Location pedestalMain) {
        if (pedestalMain == null || pedestalMain.getWorld() == null || isRunning()) {
            return false;
        }
        busyUntil = Bukkit.getCurrentTick() + SETTLE_END + 1;
        World world = pedestalMain.getWorld();
        Location center = pedestalMain.clone().add(0.5, 1.0, 0.5);
        new BukkitRunnable() {
            int t;

            @Override
            public void run() {
                DiscSummonRitual self = DiscSummonRitual.this;
                if (this.t < BURST) {
                    self.eggs(world, center, this.t);
                    if (this.t >= ARM_START) {
                        self.groundStars(world, center, this.t);
                        self.charge(world, center, pedestalMain, this.t);
                    }
                }
                if (this.t == BURST) {
                    self.burst(world, center, self.items.villageDisc());
                } else if (this.t > BURST && this.t <= SETTLE_END) {
                    self.settle(world, center, this.t - BURST);
                }
                if (this.t >= SETTLE_END) this.cancel(); else this.t++;
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
        return true;
    }

    private void eggs(World w, Location c, int t) {
        for (int i = 0; i < 3; i++) {
            double a = i * (Math.PI * 2.0 / 3.0) + Math.PI / 6.0;
            double x = c.getX() + 3.8 * Math.cos(a);
            double y = c.getY() + 1.8;
            double z = c.getZ() + 3.8 * Math.sin(a);
            this.traceEggAura(w, i + 1, x, y, z);
            if (t % 14 == 0) this.sideStreaks(w, x, y, z, i + 1);
        }
        if (t % 8 == 0) w.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.1f);
    }

    private void sideStreaks(World w, double x, double y, double z, int id) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int n = r.nextInt(3);
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double d = 0.5 + r.nextDouble() * 2.0;
            Location start = new Location(w, x + Math.cos(a) * d, y - 0.2 + r.nextDouble() * 2.6, z + Math.sin(a) * d);
            Vector vel = new Vector(r.nextDouble() - 0.5, 0.25 + r.nextDouble() * 0.55, r.nextDouble() - 0.5).normalize().multiply(0.3);
            Vector axis = new Vector(r.nextDouble() - 0.5, r.nextDouble() - 0.5, r.nextDouble() - 0.5).normalize();
            double spin = (0.025 + r.nextDouble() * 0.045) * (r.nextBoolean() ? 1 : -1);
            this.animateStreak(w, start, vel, VillagerEventItems.auraDust(id, 1.8f), axis, spin);
        }
    }

    private void animateStreak(World w, Location start, Vector vel, Particle.DustOptions dust, Vector axis, double spin) {
        new BukkitRunnable() {
            final Location pos = start.clone();
            int age;

            @Override
            public void run() {
                vel.rotateAroundAxis(axis, spin);
                vel.setY(vel.getY() + 0.005);
                for (int i = 0; i < 2; i++) {
                    this.pos.add(vel.clone().multiply(0.5));
                    w.spawnParticle(Particle.DUST, this.pos, 1, 0, 0, 0, 0, dust, true);
                }
                if (++this.age > 90) this.cancel();
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
    }

    /** Draws a moving 99-degree arc around a random tilted ellipse; a new ellipse starts when the last finishes a lap. */
    private void traceEggAura(World w, int id, double x, double y, double z) {
        List<double[]> heads = this.eggArcs.computeIfAbsent(id, k -> new ArrayList<>());
        List<Circle> circles = this.eggCircles.computeIfAbsent(id, k -> new ArrayList<>());
        if (heads.isEmpty() || heads.get(heads.size() - 1)[0] >= Math.PI * 2 && heads.size() < 2) {
            heads.add(new double[]{0.0});
            circles.add(this.makeRandomCircle(2.4));
        }
        Iterator<double[]> hi = heads.iterator();
        Iterator<Circle> ci = circles.iterator();
        while (hi.hasNext()) {
            double[] head = hi.next();
            Circle circle = ci.next();
            head[0] += 0.06;
            double from = Math.max(0.0, head[0] - 1.7278759594743864);
            double to = Math.min(Math.PI * 2, head[0]);
            if (from < to) drawArc(w, x, y, z, circle, from, to, 18.0, VillagerEventItems.auraDust(id, 1.9f), VillagerEventItems.auraDustEdge(id, 0.12f), true);
            if (head[0] - 1.7278759594743864 >= Math.PI * 2) {
                hi.remove();
                ci.remove();
            }
        }
    }

    static void drawArc(World w, double x, double y, double z, Circle c, double from, double to, double perRad,
                        Particle.DustOptions core, Particle.DustOptions edge, boolean force) {
        double span = to - from;
        int n = Math.max(2, (int) Math.ceil(span * perRad));
        for (int i = 0; i < n; i++) {
            double a = from + (n == 1 ? 0.0 : (double) i / (n - 1)) * span;
            double cos = Math.cos(a), sin = Math.sin(a);
            for (int layer = -1; layer <= 1; layer++) {
                double ru = c.ru + layer * 0.08, rv = c.rv + layer * 0.08;
                w.spawnParticle(Particle.DUST,
                    x + cos * ru * c.u.getX() + sin * rv * c.v.getX(),
                    y + cos * ru * c.u.getY() + sin * rv * c.v.getY(),
                    z + cos * ru * c.u.getZ() + sin * rv * c.v.getZ(),
                    1, 0, 0, 0, 0, layer == 0 ? core : edge, force);
            }
        }
    }

    Circle makeRandomCircle(double radius) {
        double theta = this.random.nextDouble() * 2.0 * Math.PI;
        double phi = Math.acos(2.0 * this.random.nextDouble() - 1.0);
        double nx = Math.sin(phi) * Math.cos(theta);
        Vector normal = new Vector(nx, Math.sin(phi) * Math.sin(theta), Math.cos(phi));
        Vector ref = Math.abs(nx) < 0.9 ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
        Vector u = normal.clone().crossProduct(ref).normalize();
        Vector v = normal.clone().crossProduct(u).normalize();
        return new Circle(u, v, radius * (0.75 + this.random.nextDouble() * 0.5), radius * (0.75 + this.random.nextDouble() * 0.5));
    }

    private void groundStars(World w, Location c, int t) {
        double orbit = Math.max(0.06, 0.9 * Math.pow(1.0 - Math.min(1.0, (t - ARM_START) / 200.0), 0.5));
        this.starAngle += Math.min(0.126, 0.063 * Math.pow(0.9 / orbit, 1.5));
        double y = c.getY() + 0.6;
        double speed = Math.max(0.0, 2.8 - orbit) / 11.0;
        for (int i = 0; i < 4; i++) {
            double a = this.starAngle + i * (Math.PI / 2);
            double x = c.getX() + orbit * Math.cos(a);
            double z = c.getZ() + orbit * Math.sin(a);
            w.spawnParticle(Particle.END_ROD, x, y, z, 0, Math.cos(a) * speed, 0.031818181818181815, Math.sin(a) * speed, 1.0, null, true);
            w.spawnParticle(Particle.END_ROD, x, y, z, 1, 0.03, 0.03, 0.03, 0.0);
            w.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, STAR_CORE, true);
        }
    }

    private void charge(World w, Location c, Location lightningAt, int t) {
        double k = (t - ARM_START) / 200.0;
        if (t % 2 == 0) {
            for (int i = 0; i < 32; i++) {
                double a = Math.PI * 2 * i / 32 + t * 0.05;
                w.spawnParticle(Particle.FLAME, c.getX() + 4.6 * Math.cos(a), c.getY() + 0.1, c.getZ() + 4.6 * Math.sin(a), 1, 0, 0.01, 0, 0);
            }
        }
        sphere(w, c.getX(), c.getY() + 0.4, c.getZ(), 0.15 + k * 0.35, CORE, 3, 5);
        if ((t - ARM_START) % 26 == 0) this.cosmeticLightning(lightningAt);
        if (t % 12 == 0) w.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.8f + (float) k * 0.9f);
    }

    private void burst(World w, Location c, ItemStack disc) {
        double x = c.getX(), y = c.getY() + 0.4, z = c.getZ();
        w.spawnParticle(Particle.EXPLOSION_EMITTER, x, y, z, 1);
        w.spawnParticle(Particle.EXPLOSION, x, y, z, 8, 0.6, 0.4, 0.6, 0);
        w.spawnParticle(Particle.FLAME, x, y, z, 90, 0.3, 0.3, 0.3, 0.25);
        w.spawnParticle(Particle.LAVA, x, y, z, 30, 0.4, 0.3, 0.4, 0);
        w.spawnParticle(Particle.DUST, x, y, z, 70, 1.3, 0.6, 1.3, 0, EMBER, true);
        Particle.DustOptions arm = new Particle.DustOptions(ARM_COLOR, 0.12f);
        for (int i = 0; i < 4; i++) {
            for (double s = 0.0; s < 1.4; s += 0.08) {
                double a = this.starAngle + i * (Math.PI / 2) + s * 6.0;
                double r = 0.3 + s * 2.2;
                w.spawnParticle(Particle.DUST, c.getX() + r * Math.cos(a), c.getY() + 0.1, c.getZ() + r * Math.sin(a), 1, 0, 0, 0, 0, arm, true);
            }
        }
        w.strikeLightningEffect(c);
        w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.8f);
        w.playSound(c, Sound.BLOCK_BEACON_POWER_SELECT, 1.2f, 1.2f);
        w.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.4f);
        Item item = w.dropItem(c.clone().add(0, 0.2, 0), disc);
        item.setVelocity(new Vector(0, 0.25, 0));
        item.setPickupDelay(40);
        item.setWillAge(false);
        item.customName(Component.text("Disc of the Wanderer").decoration(TextDecoration.ITALIC, false));
        item.setCustomNameVisible(true);
    }

    private void settle(World w, Location c, int t) {
        w.spawnParticle(Particle.LARGE_SMOKE, c.getX(), c.getY() + 0.3, c.getZ(), 2, 0.15, 0.3, 0.15, 0.02);
        if (t % 2 == 0) {
            w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, c.getX(), c.getY() + 0.2, c.getZ(), 1, 0.1, 0.2, 0.1, 0.01);
            double a = this.random.nextDouble() * 2.0 * Math.PI;
            double r = 2.6 * Math.sqrt(this.random.nextDouble());
            w.spawnParticle(Particle.LAVA, c.getX() + r * Math.cos(a), c.getY() + 1.5, c.getZ() + r * Math.sin(a), 1, 0, 0, 0, 0);
        }
        if (t < 32 && t % 3 == 0) {
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                w.spawnParticle(Particle.CLOUD, c.getX() + 2.2 * Math.cos(a), c.getY() + 0.1, c.getZ() + 2.2 * Math.sin(a), 1, 0.05, 0, 0.05, 0);
            }
        }
        if (t % 3 == 0) {
            w.spawnParticle(Particle.DUST, c.getX() + (this.random.nextDouble() - 0.5) * 0.6, c.getY() + 1.5 + t / 6.0,
                c.getZ() + (this.random.nextDouble() - 0.5) * 0.6, 1, 0, 0, 0, 0, t % 2 == 0 ? PINK : MAGENTA, true);
        }
    }

    private void cosmeticLightning(Location at) {
        if (at == null || at.getWorld() == null) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            int dx = 7 + this.random.nextInt(9);
            int dz = 7 + this.random.nextInt(9);
            at.getWorld().strikeLightningEffect(at.clone().add(this.random.nextBoolean() ? dx : -dx, 0, this.random.nextBoolean() ? dz : -dz));
        }
    }

    private static void sphere(World w, double x, double y, double z, double r, Particle.DustOptions dust, int rings, int perRing) {
        for (int i = 0; i <= rings; i++) {
            double phi = Math.PI * i / rings;
            double rr = r * Math.sin(phi);
            for (int j = 0; j < perRing; j++) {
                double a = Math.PI * 2 * j / perRing;
                w.spawnParticle(Particle.DUST, x + rr * Math.cos(a), y + r * Math.cos(phi), z + rr * Math.sin(a), 1, 0, 0, 0, 0, dust, true);
            }
        }
    }

    /** A tilted ellipse: two in-plane basis vectors and their radii. */
    static final class Circle {
        final Vector u;
        final Vector v;
        final double ru;
        final double rv;

        Circle(Vector u, Vector v, double ru, double rv) {
            this.u = u;
            this.v = v;
            this.ru = ru;
            this.rv = rv;
        }
    }
}
