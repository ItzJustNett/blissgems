package dev.xoperr.blissgems.goldevent;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.pedestal.PedestalManager;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Summoning the Gold Gem from a Fragment Core.
 * <ul>
 *   <li>Carry the core: seven Wire Fragments circle your head.</li>
 *   <li>Hold it in your main hand: you're pushed back, a ghost core appears in front of you and
 *       the fragments ring it. Right-click to seal: the core floats into place.</li>
 *   <li>Finale (21 s): the core beats to a rhythm of flashes and bursts, spinning gold arms and
 *       eruptions while the fragments whirl ever faster; then the Gold Gem is revealed 8 blocks up
 *       with the fragments on seven tilted orbits.</li>
 *   <li>Absorption: lightning crackles and the gem swallows the fragments one by one.</li>
 *   <li>Orbit hold (10 s) and aftermath: pulses of light knock everyone near back, your own gem is
 *       drawn out of you, cracks and shatters into falling shards, and the Gold Gem - carrying your
 *       old gem as its first soul - descends into your hands.</li>
 * </ul>
 * If the server stops mid-ritual the core (and your gem) are given back on your next join.
 */
public final class FragmentCoreRitual implements Listener {
    private static final Color GOLD = Color.fromRGB(255, 176, 42);
    private static final Color HOLDER_GOLD = Color.fromRGB(255, 205, 90);
    private static final Particle.DustOptions GOLD_DUST_SMALL = new Particle.DustOptions(GOLD, 0.7f);
    private static final int[] CORE_BEATS = {6, 27, 52, 69, 91, 110, 131, 150, 171, 190, 228};
    private static final int[] ABSORB_TETHERS = {26, 22, 21, 20, 19, 19, 18};
    private static final int[] PULSE_BEATS = {49, 83, 122, 167, 214, 264, 308, 353, 402, 446, 494, 547, 595, 642, 691};
    private static final Particle[] AFTERMATH_MIX = {Particle.SOUL, Particle.END_ROD, Particle.SCULK_CHARGE};
    private static final float GEM_ART_ROLL = (float) Math.toRadians(-45.0);
    private static final double ORBIT_R = 5.2;
    // aftermath timeline (ticks), matching the original's pacing at 1.5x slow
    private static final int DRAW_AT = slow(248);
    private static final int DRAW_FLIGHT = slow(40);
    private static final int DRAW_BURST = DRAW_FLIGHT + slow(60);
    private static final int DRAW_COVER = DRAW_BURST + slow(12);
    private static final int DRAW_BROKEN = DRAW_COVER + 12;
    private static final int DRAW_COVER2 = DRAW_BROKEN + slow(70);
    private static final int DRAW_LASTBURST = DRAW_COVER2 + 34;
    private static final int DRAW_SHARDS = DRAW_LASTBURST + slow(8);
    private static final int DRAW_COVER3 = DRAW_SHARDS + 90;
    private static final int DRAW_HANDOFF = DRAW_COVER3 + 20;
    private static final int GOLD_DESCENT = slow(44);
    private static final int DRAW_END = DRAW_HANDOFF + GOLD_DESCENT;
    private static final String[] RITUAL_SOUNDS = {"bliss:goldgem_p1_core", "bliss:goldgem_p2_bursts", "bliss:goldgem_p3_orbit",
        "bliss:goldgem_p4_handoff", "bliss:goldgem_p5_wires", "bliss:goldgem_p6_climax", "bliss:goldgem_p7_aftermath"};

    private final BlissGems plugin;
    private final NamespacedKey displayKey;
    private final File owedFile;
    private final Map<UUID, State> states = new HashMap<>();
    private final Set<UUID> owedCore = new HashSet<>();
    private final Map<UUID, String> owedGem = new HashMap<>(); // player -> "gemId:tier"
    private BukkitTask task;
    private double rot;
    private double feetAng;

    private static int slow(int ticks) {
        return (int) Math.round(ticks * 1.5);
    }

    public FragmentCoreRitual(BlissGems plugin) {
        this.plugin = plugin;
        this.displayKey = new NamespacedKey(plugin, "fragcore_display");
        this.owedFile = new File(plugin.getDataFolder(), "ritual_owed.yml");
    }

    public void start() {
        this.sweepOrphans();
        this.owedLoad();
        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 2L, 2L);
    }

    public void stop() {
        if (this.task != null) this.task.cancel();
        for (State s : this.states.values()) this.clearDisplays(s);
        this.states.clear();
        this.owedSave();
    }

    // ---- items ----

    private static boolean isCore(ItemStack item) {
        return RitualItems.isFragmentCore(item);
    }

    private boolean hasCore(Player p) {
        // contains(Material) only compares item types, so players without a nether star cost almost nothing.
        if (!p.getInventory().contains(RitualItems.FRAGMENT_CORE_MATERIAL)) return false;
        for (ItemStack i : p.getInventory().getContents()) if (isCore(i)) return true;
        return false;
    }

    private boolean summonBlocked() {
        return this.plugin.getGoldGemManager() != null && this.plugin.getConfig().getBoolean("gold.summon.once-per-server", true)
            && this.plugin.getGoldGemManager().isSummoned();
    }

    // ---- events ----

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        State s = this.states.remove(event.getPlayer().getUniqueId());
        if (s != null) this.clearDisplays(s);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.payOwed(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Player p = event.getPlayer();
        if (!isCore(p.getInventory().getItemInMainHand())) {
            return;
        }
        State s = this.states.get(p.getUniqueId());
        if (s == null || !s.holding || s.sealed || s.center == null) {
            return;
        }
        event.setCancelled(true);
        if (this.summonBlocked()) {
            p.sendMessage(PedestalManager.color("&cThe Gold Gem has already been summoned on this server."));
            return;
        }
        s.sealed = true;
        s.holding = false;
        p.getInventory().setItemInMainHand(null);
        this.owedCore.add(p.getUniqueId());
        this.owedSave();
        ItemDisplay core = this.spawnDisplay(p.getEyeLocation(), RitualItems.fragmentCore(), 0.85f);
        core.setGlowing(true);
        core.setGlowColorOverride(GOLD);
        s.placedCore = core.getUniqueId();
        s.floatFrom = p.getEyeLocation();
        s.floatTick = 0;
        s.coreAxis = randomAxis();
        this.ritualSound(s.center, 0);
    }

    // ---- idle / holding ----

    private void tick() {
        this.rot += 0.18;
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            State s = this.states.get(id);
            if (s != null && s.sealed) {
                if (s.center != null && p.getWorld() == s.center.getWorld() && p.getLocation().distanceSquared(s.center) <= 100.0) {
                    this.maintainSealed(p, s);
                } else if (!s.finale) {
                    this.clearDisplays(s);
                    this.states.remove(id);
                    this.payOwed(p);
                }
                continue;
            }
            if (!this.hasCore(p)) {
                if (s != null) {
                    this.clearDisplays(s);
                    this.states.remove(id);
                }
                continue;
            }
            if (s == null) {
                s = new State();
                this.states.put(id, s);
                this.spawnFragments(p, s);
            }
            boolean inHand = isCore(p.getInventory().getItemInMainHand());
            if (inHand && !s.holding) {
                s.holding = true;
                s.center = p.getLocation().add(0, 1.9, 0);
                // the shove and burst only the first time the core is raised, not on every hotbar swap
                if (!s.announced) {
                    s.announced = true;
                    this.pushBack(p);
                    List<Player> audience = this.audience(p.getLocation());
                    this.pushFx(p, audience);
                    for (Player a : audience) a.playSound(a.getLocation(), "bliss:goldgem_core_hold", SoundCategory.MASTER, 1.0f, 1.0f);
                }
                this.ensureGhost(s);
            } else if (!inHand && s.holding) {
                s.holding = false;
                this.removeDisplay(s.ghost);
                s.ghost = null;
                s.center = null;
            }
            s.angle += 0.06;
            if (s.holding) this.updateRing(p, s); else this.updateHalo(p, s);
        }
    }

    private void updateHalo(Player p, State s) {
        World w = p.getWorld();
        this.feetSpiral(w, p.getLocation());
        Location chest = p.getLocation().add(0, 1, 0);
        this.radiateSparks(w, chest);
        for (int i = 0; i < 7; i++) {
            double a = s.angle + i * (Math.PI * 2 / 7);
            ItemDisplay d = this.display(s.frags.get(i));
            if (d == null) continue;
            d.teleport(p.getEyeLocation().add(Math.cos(a) * 0.72, 0.6, Math.sin(a) * 0.72));
            this.spinFragment(d, i);
        }
    }

    private void ensureGhost(State s) {
        if (s.center == null || this.display(s.ghost) != null) {
            return;
        }
        ItemDisplay ghost = this.spawnDisplay(s.center, RitualItems.fragmentCoreGhost(), 0.85f);
        ghost.setGlowing(true);
        ghost.setGlowColorOverride(Color.WHITE);
        ghost.setBrightness(new Display.Brightness(15, 15));
        s.ghost = ghost.getUniqueId();
    }

    /** The fragments ring the core in the vertical plane facing the player. */
    private void updateRing(Player p, State s) {
        if (s.center == null) {
            return;
        }
        if (!s.sealed) {
            if (s.holding) this.ensureGhost(s);
            this.radiateSparks(p.getWorld(), p.getLocation().add(0, 1, 0));
        }
        Vector toward = p.getEyeLocation().toVector().subtract(s.center.toVector()).setY(0);
        toward = toward.lengthSquared() < 1.0E-6 ? new Vector(1, 0, 0) : toward.normalize();
        Vector side = toward.clone().crossProduct(new Vector(0, 1, 0));
        side = side.lengthSquared() < 1.0E-6 ? new Vector(1, 0, 0) : side.normalize();
        Vector up = side.clone().crossProduct(toward).normalize();
        for (int i = 0; i < 7; i++) {
            double a = s.angle + i * (Math.PI * 2 / 7);
            ItemDisplay d = this.display(s.frags.get(i));
            if (d == null) continue;
            d.teleport(s.center.clone().add(side.clone().multiply(Math.cos(a) * 0.9)).add(up.clone().multiply(Math.sin(a) * 0.9)));
            this.spinFragment(d, i);
        }
    }

    private void maintainSealed(Player p, State s) {
        if (s.finale) {
            return;
        }
        s.angle += 0.04;
        this.updateRing(p, s);
        if (s.floatTick < 0 || s.placedCore == null) {
            return;
        }
        s.floatTick++;
        double k = Math.min(1.0, s.floatTick / 21.0);
        ItemDisplay core = this.display(s.placedCore);
        if (core != null) {
            core.teleport(lerp(s.floatFrom, s.center, k * k * (3.0 - 2.0 * k)));
            this.spinCore(core, s);
        }
        if (k < 1.0) {
            return;
        }
        s.floatTick = -1;
        this.removeDisplay(s.ghost);
        s.ghost = null;
        if (!s.climaxed) {
            s.climaxed = true;
            Vector toward = p.getEyeLocation().toVector().subtract(s.center.toVector()).setY(0);
            toward = toward.lengthSquared() < 1.0E-6 ? new Vector(0, 0, 1) : toward.normalize();
            Vector side = toward.clone().crossProduct(new Vector(0, 1, 0));
            side = side.lengthSquared() < 1.0E-6 ? new Vector(1, 0, 0) : side.normalize();
            this.beginFinale(p, s, side);
        }
    }

    // ---- finale ----

    private void beginFinale(Player player, State s, Vector side) {
        World w = player.getWorld();
        Location center = s.center.clone();
        Vector up = new Vector(0, 1, 0);
        UUID id = player.getUniqueId();
        s.finale = true;
        this.ritualSound(center, 1);
        Vector away = center.toVector().subtract(player.getEyeLocation().toVector()).setY(0);
        away = away.lengthSquared() < 1.0E-6 ? new Vector(0, 0, 1) : away.normalize();
        Location gemLoc = center.clone().add(away.getX() * 3.0, 8.0, away.getZ() * 3.0);
        gemLoc.setYaw(0);
        gemLoc.setPitch(0);
        this.screenFlash(player, 255, 244, 214, 4, 12);
        w.spawnParticle(Particle.END_ROD, center, 20, 0.25, 0.25, 0.25, 0.4);
        this.climax(w, center, side, up);
        float baseYaw = player.getLocation().getYaw();
        float basePitch = player.getLocation().getPitch();
        new BukkitRunnable() {
            int t;
            double orbit;
            double camTh;
            double bYaw = baseYaw;
            double bPitch = basePitch;
            Float lastYaw;
            Float lastPitch;
            double armAng;
            double orbAng;
            int whiteTimer;

            @Override
            public void run() {
                FragmentCoreRitual self = FragmentCoreRitual.this;
                Player p = Bukkit.getPlayer(id);
                State st = self.states.get(id);
                if (p == null || !p.isOnline() || st == null || !st.sealed) {
                    if (st != null) st.finale = false;
                    this.cancel();
                    return;
                }
                if (this.t <= 420) this.orbitCamera(p);
                int beat = indexOf(CORE_BEATS, this.t);
                if (beat >= 0) {
                    if (beat == CORE_BEATS.length - 1) {
                        self.screenFlash(p, 255, 244, 214, 6, 20);
                    } else {
                        self.flash(w, center, 6, 0.3, 0.3, 0.3);
                    }
                    self.angledBurst(w, center, 0.0, 78, 1.859, Particle.END_ROD);
                    ThreadLocalRandom r = ThreadLocalRandom.current();
                    int n = 2 + r.nextInt(2);
                    for (int i = 0; i < n; i++) {
                        double f = 1.0 + r.nextDouble() * r.nextDouble() * 1.2;
                        self.angledBurst(w, center, -1.0, (int) (30.0 * (0.6 + 0.4 * f)), 0.55 * f * 1.3, Particle.SOUL);
                    }
                }
                double k = Math.min(1.0, this.t / 240.0);
                this.orbit += 0.06 + 0.56 * k * k;
                self.rot += 0.2 + 1.3 * (this.t / 480.0);
                this.armAng += 0.063;
                if (this.t < 240) {
                    self.placeFragments(st, center, side, up, this.orbit);
                } else {
                    this.orbAng += 0.12;
                    self.placeOrbits(st, gemLoc, this.orbAng, this.t % 2 == 0, goldPulse(this.t));
                    if (this.t > 250) self.spinGoldGem(st, this.t);
                    double kk = Math.min(1.0, (this.t - 240) / 180.0);
                    if (--this.whiteTimer <= 0) {
                        self.flash(w, gemLoc, 1, 0, 0, 0);
                        this.whiteTimer = Math.max(3, (int) (22 - 19 * kk));
                    }
                }
                if (this.t == 19) self.screenFlash(p, 255, 244, 214, 4, 12);
                if (this.t >= 31 && this.t < 240) self.eruption(w, center);
                if (this.t >= 46 && this.t < 240) {
                    self.spinningArms(w, center, side, up, this.armAng);
                    if (this.t % 3 == 0) self.angledBurst(w, center, -1.0, 30, 0.715, Particle.FIREWORK);
                }
                if (this.t == 240) {
                    self.ritualSound(gemLoc, 2);
                    self.removeDisplay(st.placedCore);
                    st.placedCore = null;
                    ItemDisplay gem = self.spawnDisplay(gemLoc, CustomItemManager.getItemById("gold_gem_t1"), 0.01f);
                    gem.setBillboard(Display.Billboard.FIXED);
                    gem.setGlowing(true);
                    gem.setGlowColorOverride(GOLD);
                    gem.setBrightness(new Display.Brightness(15, 15));
                    st.goldGem = gem.getUniqueId();
                    self.popDisplay(gem, 1.54f, 8);
                    self.orientForOrbit(st);
                }
                if (this.t == 420) {
                    self.ritualSound(gemLoc, 3);
                    self.screenFlash(p, 255, 244, 214, 6, 20);
                    List<UUID> frags = new ArrayList<>(st.frags);
                    UUID gem = st.goldGem;
                    Bukkit.getScheduler().runTaskLater(self.plugin, () -> self.beginAbsorption(id, gemLoc.clone(), frags, gem), 20L);
                    this.cancel();
                    return;
                }
                this.t++;
            }

            /** Gently circles the holder's view around where they are looking, widening over time. */
            private void orbitCamera(Player p) {
                float yaw = p.getLocation().getYaw();
                float pitch = p.getLocation().getPitch();
                if (this.lastYaw != null) {
                    this.bYaw += Location.normalizeYaw(yaw - this.lastYaw);
                    this.bPitch += pitch - this.lastPitch;
                }
                double k = Math.min(1.0, this.t / 420.0);
                double amp = 7.0 + 11.2 * k * k;
                double limit = 89.0 - amp;
                this.bPitch = Math.max(-limit, Math.min(limit, this.bPitch));
                this.camTh += 0.04 + 0.18 * k;
                double ease = Math.min(1.0, this.t / 20.0);
                ease = ease * ease * (3.0 - 2.0 * ease);
                float ny = (float) (this.bYaw + Math.cos(this.camTh) * amp * ease);
                float np = (float) Math.max(-89.0, Math.min(89.0, this.bPitch + Math.sin(this.camTh) * amp * ease));
                p.setRotation(ny, np);
                this.lastYaw = ny;
                this.lastPitch = np;
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
    }

    // ---- absorption: the gem swallows the fragments ----

    private void beginAbsorption(UUID id, Location gemLoc, List<UUID> frags, UUID gemId) {
        this.ritualSound(gemLoc, 4);
        World w = gemLoc.getWorld();
        new BukkitRunnable() {
            int idx;
            int tick;
            int gtick;
            int tailHold;
            Location startPos;

            @Override
            public void run() {
                FragmentCoreRitual self = FragmentCoreRitual.this;
                Player p = Bukkit.getPlayer(id);
                State st = self.states.get(id);
                if (p == null || !p.isOnline() || st == null) {
                    this.cancel();
                    return;
                }
                this.gtick++;
                double spin = this.gtick * 0.045;
                self.spinGoldGem(st, this.gtick * 8);
                while (this.idx < frags.size() && self.display(frags.get(this.idx)) == null) {
                    this.idx++;
                    this.tick = 0;
                }
                boolean done = this.idx >= frags.size();
                boolean pre = this.gtick <= 53;
                for (int i = 0; i < 7 && i < frags.size(); i++) {
                    Vector[] basis = ringBasis(i);
                    if (this.gtick % 2 == 0) self.drawRing(w, gemLoc, basis[0], basis[1], ORBIT_R, goldPulse(this.gtick), 120);
                    if (done || (!pre && i <= this.idx)) continue;
                    double a = spin + i * (Math.PI * 2 / 7);
                    ItemDisplay d = self.display(frags.get(i));
                    if (d != null) d.teleport(ringPoint(gemLoc, basis, a));
                }
                if (this.gtick >= 9 && this.gtick < 37 && (this.gtick - 9) % 2 == 0) {
                    for (int i = 0; i < 3; i++) self.lightningBolt(w, gemLoc);
                }
                if (done) {
                    if (++this.tailHold >= 41) {
                        self.finishAbsorption(p, id, gemId);
                        this.cancel();
                    }
                    return;
                }
                if (pre) {
                    if (this.gtick == 22 || this.gtick == 27) self.gustRing(w, gemLoc, this.gtick * 0.06);
                    return;
                }
                ItemDisplay frag = self.display(frags.get(this.idx));
                if (this.tick == 0) this.startPos = frag.getLocation();
                int tether = ABSORB_TETHERS[Math.min(this.idx, ABSORB_TETHERS.length - 1)];
                if (this.tick < tether) {
                    self.tetherLine(w, this.startPos, gemLoc);
                } else {
                    if (this.tick == tether) {
                        self.angledBurst(w, gemLoc, 0.0, 30, 1.2, Particle.END_ROD);
                        self.scatter(w, gemLoc, 26);
                    }
                    double k = (this.tick - tether + 1) / 3.0;
                    if (k >= 1.0) {
                        frag.remove();
                        self.screenFlash(p, 255, 244, 214, 4, 12);
                        this.idx++;
                        this.tick = 0;
                        w.playSound(gemLoc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 3.0f, 0.8f + this.idx * 0.1f);
                        return;
                    }
                    Location at = lerp(this.startPos, gemLoc, k * k);
                    self.tetherLine(w, at, gemLoc);
                    frag.teleport(at);
                }
                this.tick++;
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
    }

    private void finishAbsorption(Player p, UUID id, UUID gemId) {
        State st = this.states.get(id);
        this.ritualSound(st != null && st.center != null ? st.center : p.getLocation(), 5);
        this.screenFlash(p, 255, 244, 214, 4, 12);
        ItemDisplay gem = this.display(gemId);
        Location at = gem != null ? gem.getLocation() : p.getLocation();
        this.beginOrbitHold(id, gemId, at.clone());
    }

    /** 10 s: the gem spins over seven glittering orbit rings with soul eruptions. */
    private void beginOrbitHold(UUID id, UUID gemId, Location at) {
        World w = at.getWorld();
        new BukkitRunnable() {
            int t;
            int spin;

            @Override
            public void run() {
                FragmentCoreRitual self = FragmentCoreRitual.this;
                Player p = Bukkit.getPlayer(id);
                ItemDisplay gem = self.display(gemId);
                if (p == null || !p.isOnline() || gem == null) {
                    self.abort(id, gemId);
                    this.cancel();
                    return;
                }
                this.spin++;
                self.setGemSpin(gem, this.spin);
                if (this.t % 2 == 0) {
                    for (int i = 0; i < 7; i++) {
                        Vector[] basis = ringBasis(i);
                        for (int j = 0; j < 120; j++) {
                            if (ThreadLocalRandom.current().nextInt(100) < 40) continue;
                            w.spawnParticle(Particle.DUST, ringPoint(at, basis, Math.PI * 2 * j / 120), 1, 0, 0, 0, 0, goldPulse(this.t), true);
                        }
                    }
                }
                if (this.t % 4 == 0) {
                    ThreadLocalRandom r = ThreadLocalRandom.current();
                    w.spawnParticle(Particle.END_ROD, ringPoint(at, ringBasis(r.nextInt(7)), r.nextDouble() * Math.PI * 2), 0, 0, 0, 0, 0, null, true);
                }
                if (this.t < 192) {
                    ThreadLocalRandom r = ThreadLocalRandom.current();
                    for (int i = 0; i < 5; i++) {
                        double a = r.nextDouble() * Math.PI * 2, y = r.nextDouble() * 2 - 1, h = Math.sqrt(Math.max(0, 1 - y * y));
                        Vector d = new Vector(h * Math.cos(a), y, h * Math.sin(a));
                        Location o = at.clone().add(d.clone().multiply(ORBIT_R));
                        w.spawnParticle(Particle.SOUL, o, 0, d.getX(), d.getY(), d.getZ(), 0.12 + r.nextDouble() * 0.75, null, true);
                    }
                }
                if (this.t == 146 || this.t == 166) self.angledBurst(w, at, 0.0, 48, 1.8, Particle.SOUL);
                if (++this.t >= 192) {
                    self.screenFlash(p, 255, 244, 214, 6, 20);
                    Bukkit.getScheduler().runTaskLater(self.plugin, () -> self.beginAftermath(id, gemId, at), 8L);
                    this.cancel();
                }
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
    }

    // ---- aftermath: your gem is drawn out, shatters, and the Gold Gem descends ----

    private void beginAftermath(UUID id, UUID gemId, Location at) {
        World w = at.getWorld();
        this.ritualSound(at, 6);
        new BukkitRunnable() {
            int t;
            int timer;
            int spin;
            int mix;
            int beat;
            int lastPulse = -999;
            boolean released;
            UUID draw;
            Location drawFrom;
            Location drawTo;
            Location goldFrom;
            Color tint;
            String sacrificed;
            final List<UUID> shards = new ArrayList<>();
            int endAt = DRAW_AT + DRAW_END + 1;

            @Override
            public void run() {
                FragmentCoreRitual self = FragmentCoreRitual.this;
                Player p = Bukkit.getPlayer(id);
                ItemDisplay gem = self.display(gemId);
                if (p == null || !p.isOnline() || gem == null || ++this.t >= this.endAt) {
                    self.removeDisplay(this.draw);
                    for (UUID s : this.shards) self.removeDisplay(s);
                    self.abort(id, gemId);
                    if (p != null) self.payOwed(p);
                    this.cancel();
                    return;
                }
                this.spin++;
                if (!this.released) {
                    self.setGemSpin(gem, this.spin);
                    if (indexOf(PULSE_BEATS, this.t) >= 0) {
                        this.lastPulse = this.t;
                        self.lightPulse(w, at);
                    }
                }
                if (this.t == DRAW_AT) this.startDraw(p);
                int d = this.draw == null ? Integer.MIN_VALUE : this.t - DRAW_AT;
                ItemDisplay drawn = self.display(this.draw);
                if (drawn != null) {
                    if (d <= DRAW_FLIGHT) {
                        double k = (double) d / DRAW_FLIGHT;
                        Location pos = arc(this.drawFrom, this.drawTo, k * k * (3.0 - 2.0 * k));
                        drawn.teleport(pos);
                        w.spawnParticle(Particle.DUST, pos, 2, 0.05, 0.05, 0.05, 0, new Particle.DustOptions(this.tint, 0.8f), true);
                    } else {
                        drawn.teleport(this.drawTo.clone().add(0, Math.sin(d * 0.08 / 1.5) * 0.08, 0));
                        if (d % 2 == 0) w.spawnParticle(Particle.DUST, this.drawTo, 6, 0.35, 0.35, 0.35, 0, new Particle.DustOptions(this.tint, 1.0f), true);
                    }
                    boolean shaking = d > DRAW_BROKEN && d < DRAW_COVER2;
                    ThreadLocalRandom r = ThreadLocalRandom.current();
                    float jx = shaking ? (float) ((r.nextDouble() - 0.5) * 0.16) : 0f;
                    float jy = shaking ? (float) ((r.nextDouble() - 0.5) * 0.16) : 0f;
                    float roll = shaking ? (float) ((r.nextDouble() - 0.5) * 0.3) : 0f;
                    drawn.setInterpolationDelay(0);
                    drawn.setInterpolationDuration(2);
                    drawn.setTransformation(new Transformation(new Vector3f(jx, jy, 0), new Quaternionf().rotateY(2.1f + this.spin * 0.07f),
                        new Vector3f(1, 1, 1), new Quaternionf().rotateZ(GEM_ART_ROLL + roll)));
                }
                if (d >= 0) {
                    if (d == DRAW_BURST || d == DRAW_LASTBURST) self.gustRing(w, this.drawTo, d * 0.06);
                    if (d == DRAW_COVER || d == DRAW_COVER2) self.screenFlash(p, this.tint.getRed(), this.tint.getGreen(), this.tint.getBlue(), 6, 20);
                    if (d == DRAW_BROKEN && drawn != null) {
                        w.playSound(this.drawTo, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.6f);
                        w.spawnParticle(Particle.CRIT, this.drawTo, 30, 0.2, 0.2, 0.2, 0.3);
                    }
                    if (d == DRAW_SHARDS && drawn != null) {
                        self.spawnShards(w, this.drawTo, drawn.getItemStack(), this.shards);
                        self.removeDisplay(this.draw);
                        w.playSound(this.drawTo, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 3.0f, 0.5f);
                    }
                    if (d > DRAW_SHARDS && d <= DRAW_COVER3) self.fallShards(w, this.shards, new Particle.DustOptions(this.tint, 0.8f));
                    if (d == DRAW_COVER3) {
                        self.screenFlash(p, 255, 244, 214, 6, 20);
                        for (UUID s : this.shards) self.removeDisplay(s);
                        this.shards.clear();
                    }
                    if (d == DRAW_HANDOFF) {
                        self.screenFlash(p, this.tint.getRed(), this.tint.getGreen(), this.tint.getBlue(), 0, 26);
                        this.released = true;
                        this.goldFrom = gem.getLocation().clone();
                    }
                    if (this.goldFrom != null && d > DRAW_HANDOFF && d < DRAW_END) {
                        double k = (double) (d - DRAW_HANDOFF) / GOLD_DESCENT;
                        gem.teleport(lerp(this.goldFrom, p.getLocation().add(0, 1, 0), k * k * (3.0 - 2.0 * k)));
                    }
                    if (d == DRAW_END) {
                        self.grantGoldGem(p, this.sacrificed);
                        self.lightPulse(w, at);
                        self.removeDisplay(gemId);
                        State st = self.states.remove(id);
                        if (st != null) self.clearDisplays(st);
                        this.cancel();
                        return;
                    }
                }
                if (!this.released && --this.timer <= 0) {
                    this.beat++;
                    int gap = slow(4);
                    if (this.t - this.lastPulse < 14) gap = Math.max(2, (int) Math.round(gap * 0.65));
                    this.timer = gap;
                    self.flash(w, at, 1, 0, 0, 0);
                    int rings = this.beat % 2 == 0 ? 1 : 2;
                    for (int i = 0; i < rings; i++) {
                        Particle part = AFTERMATH_MIX[++this.mix % AFTERMATH_MIX.length];
                        double f = 1.0 + Math.pow(ThreadLocalRandom.current().nextDouble(), 2) * 1.2;
                        int base = part == Particle.SCULK_CHARGE ? 45 : 30;
                        self.angledBurst(w, at, 45.0, (int) (base * (0.6 + 0.4 * f)), 0.55 * f, part);
                    }
                }
            }

            /** Lifts the holder's own gem out of their inventory toward the Gold Gem. */
            private void startDraw(Player p) {
                FragmentCoreRitual self = FragmentCoreRitual.this;
                this.drawFrom = p.getEyeLocation().subtract(0, 0.5, 0).add(p.getEyeLocation().getDirection().multiply(0.35));
                this.drawFrom.setYaw(0);
                this.drawFrom.setPitch(0);
                this.drawTo = at.clone().add(0, 2.6, 0);
                ItemStack own = self.plugin.getGemManager().findGemInInventory(p);
                String gemIdStr = self.plugin.getGemManager().getGemId(p);
                if (own == null || gemIdStr == null) {
                    // nothing to sacrifice: hand the Gold Gem over straight away
                    this.endAt = this.t + 1;
                    self.grantGoldGem(p, null);
                    self.removeDisplay(gemId);
                    State st = self.states.remove(p.getUniqueId());
                    if (st != null) self.clearDisplays(st);
                    return;
                }
                int tier = self.plugin.getGemManager().getGemTier(p);
                this.sacrificed = gemIdStr + ":" + tier;
                self.owedGem.put(p.getUniqueId(), this.sacrificed);
                self.owedSave();
                ItemStack shown = own.clone();
                own.setAmount(own.getAmount() - 1);
                self.plugin.getGemManager().updateActiveGem(p);
                ItemDisplay d = self.spawnDisplay(this.drawFrom, shown, 1.0f);
                d.setBillboard(Display.Billboard.FIXED);
                d.setBrightness(new Display.Brightness(15, 15));
                this.tint = self.plugin.getGemRitualManager() != null ? self.plugin.getGemRitualManager().getGemColor(gemIdStr) : GOLD;
                d.setGlowing(true);
                d.setGlowColorOverride(this.tint);
                this.draw = d.getUniqueId();
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
    }

    private void grantGoldGem(Player p, String sacrificed) {
        ItemStack gold = CustomItemManager.getItemById("gold_gem_t1");
        if (gold != null) {
            for (ItemStack left : p.getInventory().addItem(gold).values()) p.getWorld().dropItemNaturally(p.getLocation(), left);
        }
        if (this.plugin.getGoldGemManager() != null) {
            this.plugin.getGoldGemManager().holdsGoldGem(p);
            if (sacrificed != null) {
                String[] parts = sacrificed.split(":");
                this.plugin.getGoldGemManager().fillSoul(p, parts[0], parts.length > 1 ? Integer.parseInt(parts[1]) : 1);
            }
            this.plugin.getGoldGemManager().markSummoned(p);
        }
        this.owedCore.remove(p.getUniqueId());
        this.owedGem.remove(p.getUniqueId());
        this.owedSave();
        Bukkit.broadcastMessage(PedestalManager.color("<##FFD773>&l✦ &f" + p.getName() + " <##FFD773>has summoned the &lGold Gem&r<##FFD773>."));
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    private void abort(UUID id, UUID gemId) {
        this.removeDisplay(gemId);
        State st = this.states.remove(id);
        if (st != null) this.clearDisplays(st);
    }

    // ---- owed items survive a crash ----

    private void payOwed(Player p) {
        if (p == null || !p.isOnline()) {
            return;
        }
        UUID id = p.getUniqueId();
        State st = this.states.get(id);
        if (st != null && st.finale) {
            return;
        }
        boolean core = this.owedCore.remove(id);
        String gem = this.owedGem.remove(id);
        if (!core && gem == null) {
            return;
        }
        if (core) {
            ItemStack item = RitualItems.fragmentCore();
            if (item != null) for (ItemStack left : p.getInventory().addItem(item).values()) p.getWorld().dropItemNaturally(p.getLocation(), left);
        }
        if (gem != null) {
            String[] parts = gem.split(":");
            this.plugin.getGemManager().giveGem(p, parts[0], parts.length > 1 ? Integer.parseInt(parts[1]) : 1);
        }
        this.owedSave();
        p.sendMessage(PedestalManager.color("<##FFD773>A Gold Gem ritual was cut short &7- what you put in has been returned."));
    }

    private void owedLoad() {
        if (!this.owedFile.exists()) {
            return;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(this.owedFile);
        for (String s : y.getStringList("core")) this.owedCore.add(UUID.fromString(s));
        if (y.getConfigurationSection("gem") != null) {
            for (String k : y.getConfigurationSection("gem").getKeys(false)) this.owedGem.put(UUID.fromString(k), y.getString("gem." + k));
        }
    }

    private void owedSave() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("core", this.owedCore.stream().map(UUID::toString).toList());
        for (Map.Entry<UUID, String> e : this.owedGem.entrySet()) y.set("gem." + e.getKey(), e.getValue());
        try {
            y.save(this.owedFile);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Could not save ritual_owed.yml: " + e.getMessage());
        }
    }

    // ---- displays ----

    ItemDisplay spawnDisplay(Location at, ItemStack item, float scale) {
        return at.getWorld().spawn(at, ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setBillboard(Display.Billboard.CENTER);
            d.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(scale, scale, scale), new Quaternionf()));
            d.setTeleportDuration(3);
            d.setPersistent(false);
            d.getPersistentDataContainer().set(this.displayKey, PersistentDataType.BYTE, (byte) 1);
        });
    }

    ItemDisplay display(UUID id) {
        return id != null && Bukkit.getEntity(id) instanceof ItemDisplay d ? d : null;
    }

    void removeDisplay(UUID id) {
        ItemDisplay d = this.display(id);
        if (d != null) d.remove();
    }

    private void clearDisplays(State s) {
        for (UUID f : s.frags) this.removeDisplay(f);
        this.removeDisplay(s.ghost);
        this.removeDisplay(s.placedCore);
        this.removeDisplay(s.goldGem);
    }

    private void sweepOrphans() {
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (e.getPersistentDataContainer().has(this.displayKey, PersistentDataType.BYTE)) e.remove();
            }
        }
    }

    private void spawnFragments(Player p, State s) {
        for (int i = 0; i < 7; i++) {
            ItemDisplay d = this.spawnDisplay(p.getEyeLocation(), RitualItems.wireFragment(i + 1), 0.35f);
            d.setGlowing(true);
            d.setGlowColorOverride(GOLD);
            d.setBillboard(Display.Billboard.FIXED);
            d.setInterpolationDuration(2);
            s.frags.add(d.getUniqueId());
        }
    }

    private void spinFragment(ItemDisplay d, int i) {
        Vector3f axis = new Vector3f((float) Math.sin(i * 1.3 + 0.4), (float) Math.cos(i * 0.7 + 0.5), (float) Math.sin(i * 0.9 + 1.1));
        if (axis.lengthSquared() < 1.0E-4f) axis.set(0, 1, 0);
        axis.normalize();
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(2);
        d.setTransformation(new Transformation(new Vector3f(), new Quaternionf().rotateAxis((float) (this.rot * (1.0 + 0.25 * i) + i), axis),
            new Vector3f(0.35f, 0.35f, 0.35f), new Quaternionf()));
    }

    private void spinCore(ItemDisplay d, State s) {
        if (s.coreAxis == null) s.coreAxis = new Vector3f(0, 1, 0);
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(2);
        d.setTransformation(new Transformation(new Vector3f(), new Quaternionf().rotateAxis((float) this.rot, s.coreAxis),
            new Vector3f(0.85f, 0.85f, 0.85f), new Quaternionf()));
    }

    private void spinGoldGem(State s, int t) {
        ItemDisplay gem = this.display(s.goldGem);
        if (gem != null) this.setGemSpin(gem, t);
    }

    private void setGemSpin(ItemDisplay gem, int t) {
        gem.setInterpolationDelay(0);
        gem.setInterpolationDuration(2);
        gem.setTransformation(new Transformation(new Vector3f(), new Quaternionf().rotateY(t * 0.05f),
            new Vector3f(1.54f, 1.54f, 1.54f), new Quaternionf().rotateZ(GEM_ART_ROLL)));
    }

    private void popDisplay(ItemDisplay d, float scale, int ticks) {
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(ticks);
        d.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(scale, scale, scale), new Quaternionf().rotateZ(GEM_ART_ROLL)));
    }

    private void orientForOrbit(State s) {
        for (UUID f : s.frags) {
            ItemDisplay d = this.display(f);
            if (d == null) continue;
            d.setBillboard(Display.Billboard.CENTER);
            d.setInterpolationDelay(0);
            d.setInterpolationDuration(0);
            d.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(0.35f, 0.35f, 0.35f), new Quaternionf()));
            d.setTeleportDuration(2);
        }
    }

    private void placeFragments(State s, Location c, Vector u, Vector v, double angle) {
        for (int i = 0; i < 7 && i < s.frags.size(); i++) {
            double a = angle + i * (Math.PI * 2 / 7);
            ItemDisplay d = this.display(s.frags.get(i));
            if (d == null) continue;
            d.teleport(c.clone().add(u.clone().multiply(Math.cos(a) * 0.9).add(v.clone().multiply(Math.sin(a) * 0.9))));
            this.spinFragment(d, i);
        }
    }

    /** Seven orbits around the gem, each tilted a little more than the last. */
    private void placeOrbits(State s, Location c, double angle, boolean drawRings, Particle.DustOptions dust) {
        for (int i = 0; i < 7 && i < s.frags.size(); i++) {
            Vector[] basis = ringBasis(i);
            if (drawRings) this.drawRing(c.getWorld(), c, basis[0], basis[1], ORBIT_R, dust, 120);
            ItemDisplay d = this.display(s.frags.get(i));
            if (d != null) d.teleport(ringPoint(c, basis, angle + i * (Math.PI * 2 / 7)));
        }
    }

    private static Vector[] ringBasis(int i) {
        Quaternionf q = new Quaternionf().rotateY((float) (i * (Math.PI * 2 / 7))).rotateX((float) (Math.toRadians(20.0) + i * Math.toRadians(19.166666666666668)));
        Vector3f u = q.transform(new Vector3f(1, 0, 0));
        Vector3f v = q.transform(new Vector3f(0, 0, 1));
        return new Vector[]{new Vector(u.x, u.y, u.z), new Vector(v.x, v.y, v.z)};
    }

    private static Location ringPoint(Location c, Vector[] basis, double a) {
        double x = Math.cos(a) * ORBIT_R, z = Math.sin(a) * ORBIT_R;
        return c.clone().add(basis[0].getX() * x + basis[1].getX() * z, basis[0].getY() * x + basis[1].getY() * z, basis[0].getZ() * x + basis[1].getZ() * z);
    }

    // ---- shards ----

    private void spawnShards(World w, Location at, ItemStack look, List<UUID> out) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 8; i++) {
            Location l = at.clone().add((r.nextDouble() - 0.5) * 1.2, (r.nextDouble() - 0.5) * 0.8, (r.nextDouble() - 0.5) * 1.2);
            ItemDisplay d = this.spawnDisplay(l, look, 0.25f + (float) r.nextDouble() * 0.2f);
            d.setBillboard(Display.Billboard.FIXED);
            d.setTransformation(new Transformation(new Vector3f(), new Quaternionf().rotateXYZ((float) r.nextDouble() * 6, (float) r.nextDouble() * 6, (float) r.nextDouble() * 6),
                new Vector3f(0.3f, 0.3f, 0.3f), new Quaternionf()));
            out.add(d.getUniqueId());
        }
    }

    private void fallShards(World w, List<UUID> shards, Particle.DustOptions dust) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (UUID id : shards) {
            ItemDisplay d = this.display(id);
            if (d == null) continue;
            Location l = d.getLocation();
            if (l.getBlock().getRelative(0, -1, 0).getType().isSolid() && l.getY() - l.getBlockY() < 0.3) continue;
            Location n = l.clone().add((r.nextDouble() - 0.5) * 0.08, -0.12, (r.nextDouble() - 0.5) * 0.08);
            d.teleport(n);
            w.spawnParticle(Particle.DUST, n, 1, 0, 0, 0, 0, dust, true);
        }
    }

    // ---- effects ----

    private void pushBack(Player p) {
        Vector dir = p.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 1.0E-6) dir = new Vector(0, 0, 1);
        p.setVelocity(dir.normalize().multiply(-1.1).setY(0.32));
    }

    List<Player> audience(Location at) {
        List<Player> out = new ArrayList<>();
        if (at == null || at.getWorld() == null) return out;
        for (Player p : at.getWorld().getPlayers()) if (p.getLocation().distanceSquared(at) <= 6400.0) out.add(p);
        return out;
    }

    private void pushFx(Player p, List<Player> audience) {
        World w = p.getWorld();
        Location chest = p.getLocation().add(0, 1, 0);
        w.spawnParticle(Particle.SOUL, chest, 24, 0.3, 0.4, 0.3, 0.05, null, true);
        this.flash(w, chest, 1, 0, 0, 0);
        for (Player a : audience) {
            a.playSound(a.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 1.4f, 1.3f);
            a.playSound(a.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.MASTER, 1.2f, 1.1f);
        }
    }

    private void feetSpiral(World w, Location at) {
        this.feetAng += 0.22;
        for (int arm = 0; arm < 2; arm++) {
            for (int layer = 0; layer < 2; layer++) {
                double a = this.feetAng + arm * Math.PI;
                double c = Math.cos(a), s = Math.sin(a);
                w.spawnParticle(Particle.SCULK_CHARGE, at.clone().add(c * 0.1, 0.3, s * 0.1), 0, c, 0, s, 0.055 * (1.0 - 0.35 * layer), 0.0f, true);
            }
        }
    }

    private void radiateSparks(World w, Location at) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double a = r.nextDouble() * Math.PI * 2, y = r.nextDouble() * 2 - 1, h = Math.sqrt(Math.max(0, 1 - y * y));
        Vector d = new Vector(h * Math.cos(a), y, h * Math.sin(a));
        Location start = at.clone().add(d.clone().multiply(0.25));
        this.goldStreak(w, start, d, 1.0 + r.nextDouble() * 3.0);
    }

    private void goldStreak(World w, Location start, Vector dir, double len) {
        Particle.DustOptions dust = new Particle.DustOptions(HOLDER_GOLD, 0.6f);
        for (double s = 0; s <= len; s += 0.35) w.spawnParticle(Particle.DUST, start.clone().add(dir.clone().multiply(s)), 1, 0, 0, 0, 0, dust, true);
    }

    private void spinningArms(World w, Location c, Vector u, Vector v, double ang) {
        Particle.DustOptions dust = new Particle.DustOptions(HOLDER_GOLD, 1.0f);
        for (int arm = 0; arm < 4; arm++) {
            for (int dot = 0; dot < 5; dot++) {
                double f = dot / 4.0;
                double r = 0.9 + 3.52 * f;
                double a = ang + arm * (Math.PI / 2) - 0.85 * f;
                Vector dir = u.clone().multiply(Math.cos(a)).add(v.clone().multiply(Math.sin(a)));
                w.spawnParticle(Particle.DUST, c.clone().add(dir.multiply(r)), 1, 0, 0, 0, 0, dust, true);
            }
        }
    }

    private void eruption(World w, Location c) {
        w.spawnParticle(Particle.END_ROD, c, 4, 0.15, 0.2, 0.15, 0.7);
        w.spawnParticle(Particle.END_ROD, c, 2, 0.4, 0.5, 0.4, 0.25);
        w.spawnParticle(Particle.FIREWORK, c, 2, 0.3, 0.4, 0.3, 0.3);
    }

    /** A ring of particles flung outward in a plane; tilt &lt; 0 = any orientation, else within that many degrees of flat. */
    void angledBurst(World w, Location c, double tilt, int count, double speed, Particle particle) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Vector normal;
        if (tilt < 0) {
            normal = new Vector(r.nextDouble() * 2 - 1, r.nextDouble() * 2 - 1, r.nextDouble() * 2 - 1);
            if (normal.lengthSquared() < 1.0E-6) normal = new Vector(0, 1, 0);
        } else {
            double th = Math.toRadians(tilt) * r.nextDouble(), ph = r.nextDouble() * Math.PI * 2;
            normal = new Vector(Math.sin(th) * Math.cos(ph), Math.cos(th), Math.sin(th) * Math.sin(ph));
        }
        normal.normalize();
        Vector a = normal.clone().crossProduct(new Vector(0, 1, 0));
        if (a.lengthSquared() < 1.0E-6) a = new Vector(1, 0, 0);
        a.normalize();
        Vector b = normal.clone().crossProduct(a).normalize();
        double sp = particle == Particle.SOUL ? speed * 0.7 : speed;
        for (int i = 0; i < count; i++) {
            double th = (double) i / count * Math.PI * 2;
            double dx = a.getX() * Math.cos(th) + b.getX() * Math.sin(th);
            double dy = a.getY() * Math.cos(th) + b.getY() * Math.sin(th);
            double dz = a.getZ() * Math.cos(th) + b.getZ() * Math.sin(th);
            if (particle == Particle.SCULK_CHARGE) {
                for (int k = 0; k < 2; k++) w.spawnParticle(particle, c, 0, dx, dy, dz, sp, 0.0f, true);
            } else {
                w.spawnParticle(particle, c, 0, dx, dy, dz, sp, null, true);
            }
        }
    }

    /** A big circle then a four-pointed star launched outward in the finale's plane. */
    private void climax(World w, Location c, Vector u, Vector v) {
        for (int i = 0; i < 60; i++) {
            double a = i / 60.0 * Math.PI * 2;
            this.launch(w, c, u, v, Math.cos(a) * 5.0, Math.sin(a) * 5.0);
        }
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            double[] xs = new double[4], ys = new double[4];
            for (int i = 0; i < 4; i++) {
                double a = i * (Math.PI / 2) + Math.PI / 4;
                xs[i] = Math.cos(a) * 4.0;
                ys[i] = Math.sin(a) * 4.0;
            }
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) % 4;
                for (int s = 0; s <= 44; s++) {
                    double t = s / 44.0, m = 1 - t;
                    double b0 = m * m * m, b1 = 3 * m * m * t, b2 = 3 * m * t * t, b3 = t * t * t;
                    this.launch(w, c, u, v, b0 * xs[i] + b1 * xs[i] * 0.18 + b2 * xs[j] * 0.18 + b3 * xs[j], b0 * ys[i] + b1 * ys[i] * 0.18 + b2 * ys[j] * 0.18 + b3 * ys[j]);
                }
            }
        }, 12L);
    }

    private void launch(World w, Location c, Vector u, Vector v, double x, double y) {
        Vector dir = u.clone().multiply(x).add(v.clone().multiply(y));
        w.spawnParticle(Particle.END_ROD, c, 0, dir.getX(), dir.getY(), dir.getZ(), 0.11, null, true);
    }

    private void lightningBolt(World w, Location c) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double dist = 15.6 * Math.sqrt(r.nextDouble());
        double a = r.nextDouble() * Math.PI * 2;
        double bx = c.getX() + Math.cos(a) * dist, bz = c.getZ() + Math.sin(a) * dist;
        double by = c.getY() + 5.2 - 0.8 + (r.nextDouble() * 2 - 1) * 1.6;
        double ox = 0, oz = 0;
        for (int seg = 0; seg < 7; seg++) {
            double nx = (r.nextDouble() - 0.5) * 0.8, nz = (r.nextDouble() - 0.5) * 0.8;
            for (int i = 0; i < 8; i++) {
                double t = i / 8.0;
                w.spawnParticle(Particle.ELECTRIC_SPARK, bx + ox + (nx - ox) * t, by + seg * 0.9285714285714286 + t * 0.9285714285714286, bz + oz + (nz - oz) * t, 1, 0, 0, 0, 0, null, true);
            }
            ox = nx;
            oz = nz;
        }
    }

    private void tetherLine(World w, Location a, Location b) {
        Vector d = b.toVector().subtract(a.toVector());
        double len = d.length();
        if (len < 1.0E-6) return;
        int n = Math.max(2, (int) (len / 0.3));
        for (int i = 0; i <= n; i++) {
            double t = (double) i / n;
            w.spawnParticle(Particle.WAX_OFF, a.getX() + d.getX() * t, a.getY() + d.getY() * t, a.getZ() + d.getZ() * t, 1, 0, 0, 0, 0, null, true);
        }
    }

    private void drawRing(World w, Location c, Vector u, Vector v, double r, Particle.DustOptions dust, int points) {
        for (int i = 0; i < points; i++) {
            double a = (double) i / points * Math.PI * 2;
            double x = Math.cos(a) * r, z = Math.sin(a) * r;
            w.spawnParticle(Particle.DUST, c.clone().add(u.getX() * x + v.getX() * z, u.getY() * x + v.getY() * z, u.getZ() * x + v.getZ() * z), 1, 0, 0, 0, 0, dust, true);
        }
    }

    private void gustRing(World w, Location c, double phase) {
        for (int layer = 0; layer < 2; layer++) {
            for (int i = 0; i < 20; i++) {
                double a = phase + (double) i / 20 * Math.PI * 2;
                w.spawnParticle(Particle.CLOUD, c.clone().add(0, layer * 0.06, 0), 0, Math.cos(a), 0, Math.sin(a), layer == 0 ? 0.26 : 0.13, null, true);
            }
        }
    }

    /** A flash plus a shock of light that makes everyone within 12 blocks flinch (no damage). */
    private void lightPulse(World w, Location c) {
        this.flash(w, c, 3, 0.25, 0.25, 0.25);
        this.scatter(w, c, 120);
        for (Entity e : w.getNearbyEntities(c, 12, 12, 12)) {
            if (e instanceof LivingEntity le) le.playHurtAnimation(0.0f);
            if (e instanceof Player p) p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
    }

    private void scatter(World w, Location c, int count) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            double a = r.nextDouble() * Math.PI * 2, y = r.nextDouble() * 2 - 1, h = Math.sqrt(Math.max(0, 1 - y * y));
            double dx = h * Math.cos(a), dz = h * Math.sin(a);
            double speed = 0.55 + r.nextDouble() * 1.85;
            switch (r.nextInt(3)) {
                case 0 -> w.spawnParticle(Particle.END_ROD, c, 0, dx, y, dz, speed, null, true);
                case 1 -> w.spawnParticle(Particle.SOUL, c, 0, dx, y, dz, speed * 0.7, null, true);
                default -> w.spawnParticle(Particle.SCULK_CHARGE, c, 0, dx, y, dz, speed, 0.0f, true);
            }
        }
    }

    void flash(World w, Location c, int count, double ox, double oy, double oz) {
        if (Particle.FLASH.getDataType() == Color.class) {
            w.spawnParticle(Particle.FLASH, c, count, ox, oy, oz, 0.0, Color.WHITE, true);
        } else {
            w.spawnParticle(Particle.FLASH, c, count, ox, oy, oz, 0.0, null, true);
        }
    }

    /**
     * Full-screen colour flash for one player: a huge text display with a solid background held
     * just in front of their eyes, fading out, hidden from everyone else.
     */
    void screenFlash(Player p, int r, int g, int b, int hold, int total) {
        // Bedrock (via Geyser) can't see display entities: use its camera fade instead
        if (BedrockBridge.fade(p, r, g, b, 0f, hold / 20f, Math.max(0, total - hold) / 20f)) return;
        UUID id = p.getUniqueId();
        Location eye = p.getEyeLocation();
        TextDisplay td = p.getWorld().spawn(eye.add(eye.getDirection().multiply(0.45)), TextDisplay.class, d -> {
            d.setPersistent(false);
            d.text(Component.text("█".repeat(24)));
            d.setBillboard(Display.Billboard.CENTER);
            d.setBackgroundColor(Color.fromARGB(255, r, g, b));
            d.setSeeThrough(false);
            d.setShadowed(false);
            d.setDefaultBackground(false);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setTeleportDuration(1);
            d.setTransformation(new Transformation(new Vector3f(0, -12, 0), new Quaternionf(), new Vector3f(4, 4, 4), new Quaternionf()));
            d.getPersistentDataContainer().set(this.displayKey, PersistentDataType.BYTE, (byte) 1);
        });
        for (Player other : Bukkit.getOnlinePlayers()) if (!other.getUniqueId().equals(id)) other.hideEntity(this.plugin, td);
        new BukkitRunnable() {
            int k;

            @Override
            public void run() {
                Player pl = Bukkit.getPlayer(id);
                if (pl == null || !pl.isOnline() || td.isDead() || this.k > total) {
                    if (!td.isDead()) td.remove();
                    this.cancel();
                    return;
                }
                Location e = pl.getEyeLocation();
                td.teleport(e.add(e.getDirection().multiply(0.45)));
                int alpha = this.k < hold ? 255 : (int) (255.0 * (1.0 - (double) (this.k - hold) / Math.max(1, total - hold)));
                td.setBackgroundColor(Color.fromARGB(Math.max(0, alpha), r, g, b));
                this.k++;
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
    }

    private void ritualSound(Location at, int phase) {
        if (at == null || phase < 0 || phase >= RITUAL_SOUNDS.length) return;
        for (Player p : this.audience(at)) p.playSound(p.getLocation(), RITUAL_SOUNDS[phase], SoundCategory.MASTER, 1.0f, 1.0f);
    }

    private static Particle.DustOptions goldPulse(int t) {
        double k = 0.5 + 0.5 * Math.sin(t * 0.05);
        return new Particle.DustOptions(Color.fromRGB(Math.min(255, (int) (210 + 45 * k)), Math.min(255, (int) (150 + 78 * k)), Math.min(255, (int) (45 + 85 * k))), 0.8f);
    }

    private static Vector3f randomAxis() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Vector3f v = new Vector3f((float) (r.nextDouble() * 2 - 1), (float) (r.nextDouble() * 2 - 1), (float) (r.nextDouble() * 2 - 1));
        return v.lengthSquared() < 1.0E-4f ? new Vector3f(0, 1, 0) : v.normalize();
    }

    private static Location lerp(Location a, Location b, double t) {
        return new Location(a.getWorld(), a.getX() + (b.getX() - a.getX()) * t, a.getY() + (b.getY() - a.getY()) * t, a.getZ() + (b.getZ() - a.getZ()) * t);
    }

    /** Straight line from a to b lifted into an arc 3 blocks high at the middle. */
    private static Location arc(Location a, Location b, double t) {
        return lerp(a, b, t).add(0, Math.sin(t * Math.PI) * 3.0, 0);
    }

    private static int indexOf(int[] arr, int v) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == v) return i;
        return -1;
    }

    private static final class State {
        final List<UUID> frags = new ArrayList<>();
        UUID ghost;
        UUID placedCore;
        UUID goldGem;
        boolean holding;
        boolean announced;
        boolean sealed;
        boolean climaxed;
        boolean finale;
        Location center;
        double angle;
        Location floatFrom;
        int floatTick = -1;
        Vector3f coreAxis;
    }
}
