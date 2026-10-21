package dev.xoperr.blissgems.villagerevent;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.pedestal.PedestalManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.scheduler.BukkitTask;

/**
 * While the soul hunt runs: the floating compass (three tilted rings, a spinning accretion
 * disk, grey haze and a cherry-blossom crown) over the village, coloured beams pointing from
 * it toward each villager/soul, drifting auras around them, keeping event villagers in place
 * and a "Beneath the earth" hint + lightning over villagers nobody has found yet.
 */
public final class VillagerEventVisuals {
    private static final Particle.DustOptions GRAY = new Particle.DustOptions(Color.fromRGB(150, 150, 150), 1.6f);
    private static final double COMPASS_HEIGHT = 5.0;

    private final BlissGems plugin;
    private final VillagerEventState state;
    private final DiscSummonRitual circles;
    private final Random random = new Random();
    private final List<BukkitTask> tasks = new ArrayList<>();
    private final Map<Integer, Location> auraLastPos = new HashMap<>();
    private final Map<Integer, List<double[]>> auraHeads = new HashMap<>();
    private final Map<Integer, List<DiscSummonRitual.Circle>> auraCircles = new HashMap<>();
    private final Map<Integer, Integer> auraRest = new HashMap<>();
    private double selfSpinDeg;
    private int blossomTick;
    private double diskAngle;
    private int trialStep;

    public VillagerEventVisuals(BlissGems plugin, VillagerEventState state, VillagerEventItems items) {
        this.plugin = plugin;
        this.state = state;
        this.circles = new DiscSummonRitual(plugin, items);
    }

    public void start() {
        this.tasks.add(Bukkit.getScheduler().runTaskTimer(this.plugin, this::drawCompass, 1L, 1L));
        this.tasks.add(Bukkit.getScheduler().runTaskTimer(this.plugin, this::drawTrackingBeams, 4L, 4L));
        this.tasks.add(Bukkit.getScheduler().runTaskTimer(this.plugin, this::drawAuras, 1L, 1L));
        this.tasks.add(Bukkit.getScheduler().runTaskTimer(this.plugin, this::lockVillagers, 10L, 10L));
        this.tasks.add(Bukkit.getScheduler().runTaskTimer(this.plugin, this::warnBeneath, 300L, 300L));
    }

    public void stop() {
        for (BukkitTask t : this.tasks) t.cancel();
        this.tasks.clear();
    }

    // ---- the compass ----

    private void drawCompass() {
        Location c = this.state.compassLoc();
        if (!this.state.isActive() || c == null || c.getWorld() == null) {
            return;
        }
        this.state.addRingRot(2.0);
        this.selfSpinDeg += 0.8;
        World w = c.getWorld();
        double x = c.getX(), y = c.getY() + COMPASS_HEIGHT, z = c.getZ();
        this.trialRing(w, c);
        double sin = Math.sin(Math.toRadians(this.selfSpinDeg));
        double cos = Math.cos(Math.toRadians(this.selfSpinDeg));
        this.ring(w, x, y, z, 3.5, 60, Math.toRadians(-45.0), this.state.ringRot(), sin, cos, Particle.WAX_ON);
        this.ring(w, x, y, z, 8.0, 72, Math.toRadians(20.0), -this.state.ringRot(), sin, cos, Particle.WAX_OFF);
        this.ring(w, x, y, z, 12.0, 150, Math.toRadians(10.0), -this.state.ringRot(), sin, cos, Particle.WAX_OFF);
        this.accretionDisk(w, x, y, z);
        for (int i = 0; i < 5; i++) {
            double a = this.random.nextDouble() * 2.0 * Math.PI;
            double r = 13.0 + this.random.nextDouble() * 4.0;
            w.spawnParticle(Particle.DUST, x + r * Math.cos(a), y + (this.random.nextDouble() - 0.5) * 3.0, z + r * Math.sin(a), 0,
                Math.cos(a) * 0.5, 1.0, Math.sin(a) * 0.5, 0.03, GRAY, false);
        }
        if (++this.blossomTick % 15 == 0) {
            for (int i = 0; i < 48; i++) {
                double a = Math.toRadians(this.state.ringRot()) + Math.PI * 2 * i / 48.0;
                w.spawnParticle(Particle.CHERRY_LEAVES, x + Math.cos(a) * 20.0, y + 11.0, z + Math.sin(a) * 20.0, 1, 0, 0, 0, 0, null, false);
            }
        }
    }

    /** A tilted ring spinning about its own vertical axis. */
    private void ring(World w, double x, double y, double z, double r, int points, double tilt, double rotDeg, double sin, double cos, Particle p) {
        for (int i = 1; i <= points; i++) {
            double a = Math.toRadians(i / (double) points * 360.0 + rotDeg);
            double px = r * Math.sin(a);
            double pz = r * Math.cos(a) * Math.cos(tilt);
            double py = -r * Math.cos(a) * Math.sin(tilt);
            w.spawnParticle(p, x + px * cos + pz * sin, y + py, z - px * sin + pz * cos, 1, 0, 0, 0, 0);
        }
    }

    private void accretionDisk(World w, double x, double y, double z) {
        this.diskAngle += 0.35;
        double tilt = Math.toRadians(25.0);
        double ts = Math.sin(tilt), tc = Math.cos(tilt);
        for (int arm = 0; arm < 4; arm++) {
            for (int i = 0; i < 11; i++) {
                double f = i / 10.0;
                double r = Math.sqrt(1.0 - f);
                double a = this.diskAngle + Math.PI * 2 * arm / 4.0 + 1.6 * f;
                double ca = Math.cos(a), sa = Math.sin(a);
                double vx = -ca + -sa * 0.45;
                double vz = -sa + ca * 0.45;
                double vy = vz * ts;
                double vzz = vz * tc;
                double len = Math.sqrt(vx * vx + vy * vy + vzz * vzz);
                if (len < 1.0E-6) continue;
                double side = r * sa;
                w.spawnParticle(Particle.SCRAPE, x + r * ca, y + side * ts, z + side * tc, 0, vx / len, vy / len, vzz / len, 0.12);
            }
        }
    }

    private void trialRing(World w, Location c) {
        this.trialStep++;
        for (int trail = 0; trail < 3; trail++) {
            double a = Math.PI * 2 * Math.floorMod(this.trialStep - trail, 36) / 36.0;
            for (double h = 0.1; h <= 0.7; h += 0.12) {
                w.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, new Location(w, c.getX() + Math.cos(a) * 3.0, c.getY() + h, c.getZ() + Math.sin(a) * 3.0), 1, 0.02, 0.02, 0.02, 0, null, true);
            }
        }
    }

    // ---- beams from the compass toward each villager / soul ----

    private void drawTrackingBeams() {
        Location compass = this.state.compassLoc();
        if (!this.state.isActive() || compass == null) {
            return;
        }
        for (int id = 1; id <= 3; id++) {
            Location target = this.state.villagerLoc(id);
            String holder = this.state.soulHolder(id);
            Player carrier = holder == null ? null : Bukkit.getPlayerExact(holder);
            if (carrier != null) {
                target = carrier.getLocation();
                this.state.setSoulLastPos(id, target);
            } else if (this.state.soulLastPos(id) != null) {
                target = this.state.soulLastPos(id);
            }
            if (target != null) this.drawBeam(id, compass, target);
        }
    }

    private void drawBeam(int id, Location from, Location to) {
        World tw = to.getWorld();
        World fw = from.getWorld();
        if (tw == null || fw == null) {
            return;
        }
        String name = tw.getName().toLowerCase();
        int dim = name.contains("nether") ? 1 : name.contains("end") ? 2 : 0;
        boolean otherWorld = !tw.equals(fw);
        if (otherWorld && dim == 0) {
            return;
        }
        double tx = to.getX(), tz = to.getZ();
        if (otherWorld && dim == 1) {
            tx *= 8.0;
            tz *= 8.0;
        }
        double dx = tx - from.getX(), dz = tz - from.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 1.0) {
            return;
        }
        double len = Math.max(4.0, Math.min(30.0, Math.floor(dist * 30.0 / 8000.0)));
        Particle.DustOptions dust = VillagerEventItems.beamDust(id);
        double y = from.getY() + COMPASS_HEIGHT;
        double px = from.getX(), pz = from.getZ();
        for (double s = 0.2; s <= len; s += 0.2) {
            px = from.getX() + dx / dist * s;
            pz = from.getZ() + dz / dist * s;
            fw.spawnParticle(Particle.DUST, px, y, pz, 1, 0, 0, 0, 0, dust, true);
        }
        if (dim == 1) {
            fw.spawnParticle(Particle.FLAME, px, y, pz, 1, 0, 0, 0, 0);
            fw.spawnParticle(Particle.FLAME, px, y + 0.4, pz, 1, 0, 0, 0, 0);
        } else if (dim == 2) {
            fw.spawnParticle(Particle.SCULK_SOUL, px, y, pz, 1, 0, 0, 0, 0);
            fw.spawnParticle(Particle.SCULK_SOUL, px, y + 0.4, pz, 1, 0, 0, 0, 0);
        } else {
            fw.spawnParticle(Particle.DUST, px, y, pz, 1, 0, 0, 0, 0, dust, true);
        }
    }

    // ---- auras around villagers / soul carriers ----

    private void drawAuras() {
        if (!this.state.isActive()) {
            return;
        }
        this.state.addAuraRot(12.0);
        for (int id = 1; id <= 3; id++) {
            Location at = this.locateAuraTarget(id);
            if (at == null) {
                this.auraLastPos.remove(id);
                continue;
            }
            Location last = this.auraLastPos.put(id, at.clone());
            boolean moved = last == null || !last.getWorld().equals(at.getWorld()) || last.distanceSquared(at) > 0.0025;
            if (!moved) this.spawnAuraHelix(id, at);
        }
    }

    private Location locateAuraTarget(int id) {
        if (this.state.isVillagerAlive(id)) {
            Villager v = this.state.getVillager(id);
            if (v != null && v.isValid() && !v.isDead()) return v.getLocation();
        }
        String holder = this.state.soulHolder(id);
        Player carrier = holder == null ? null : Bukkit.getPlayerExact(holder);
        if (carrier != null) return carrier.getLocation();
        return this.state.soulLastPos(id);
    }

    private void spawnAuraHelix(int id, Location at) {
        World w = at.getWorld();
        List<double[]> heads = this.auraHeads.computeIfAbsent(id, k -> new ArrayList<>());
        List<DiscSummonRitual.Circle> circles = this.auraCircles.computeIfAbsent(id, k -> new ArrayList<>());
        if (heads.isEmpty()) {
            int rest = this.auraRest.getOrDefault(id, 0);
            if (rest > 0) {
                this.auraRest.put(id, rest - 1);
                return;
            }
            heads.add(new double[]{0.0});
            circles.add(this.circles.makeRandomCircle(1.3));
        }
        Iterator<double[]> hi = heads.iterator();
        Iterator<DiscSummonRitual.Circle> ci = circles.iterator();
        while (hi.hasNext()) {
            double[] head = hi.next();
            DiscSummonRitual.Circle circle = ci.next();
            head[0] += 0.06;
            double from = Math.max(0.0, head[0] - 1.0995574287564276);
            double to = Math.min(Math.PI * 2, head[0]);
            if (from < to) {
                DiscSummonRitual.drawArc(w, at.getX(), at.getY() + 1.0, at.getZ(), circle, from, to, 5.0,
                    VillagerEventItems.auraDust(id, 2.0f), VillagerEventItems.auraDustEdge(id, 0.2f), false);
            }
            if (head[0] - 1.0995574287564276 >= Math.PI * 2) {
                hi.remove();
                ci.remove();
            }
        }
        if (heads.isEmpty()) this.auraRest.put(id, 20);
    }

    // ---- villagers stay where they were placed ----

    private void lockVillagers() {
        if (!this.state.isActive()) {
            return;
        }
        for (int id = 1; id <= 3; id++) {
            if (!this.state.isVillagerAlive(id)) continue;
            Villager v = this.state.getVillager(id);
            Location home = this.state.villagerLoc(id);
            if (v == null || home == null) continue;
            if (!v.getWorld().equals(home.getWorld())) {
                v.teleport(home);
                continue;
            }
            if (!v.isOnGround()) continue;
            Location at = v.getLocation();
            double dx = at.getX() - home.getX(), dz = at.getZ() - home.getZ();
            if (dx * dx + dz * dz > 4.0) {
                v.teleport(home);
            } else if (Math.abs(at.getY() - home.getY()) > 0.5) {
                this.state.setVillagerLoc(id, at);
                this.state.save();
            }
        }
    }

    private void warnBeneath() {
        if (!this.state.isActive()) {
            return;
        }
        String msg = PedestalManager.color("&e(!)Beneath the earth");
        for (int id = 1; id <= 3; id++) {
            if (this.state.wasVillagerEverKilled(id) || !this.state.isVillagerAlive(id)) continue;
            Location home = this.state.villagerLoc(id);
            if (home == null || home.getWorld() == null) continue;
            World w = home.getWorld();
            boolean anyone = false;
            for (Player p : w.getPlayers()) {
                double dx = p.getLocation().getX() - home.getX(), dz = p.getLocation().getZ() - home.getZ();
                if (dx * dx + dz * dz <= 100.0) {
                    p.sendMessage(msg);
                    anyone = true;
                }
            }
            if (anyone) w.strikeLightningEffect(new Location(w, home.getX(), w.getHighestBlockYAt(home.getBlockX(), home.getBlockZ()) + 1.0, home.getZ()));
        }
    }
}
