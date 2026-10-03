package dev.xoperr.blissgems.goldevent;

import dev.xoperr.blissgems.BlissGems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Entry to the Golden Dream: throw all seven numbered Wire Fragments into one pile (within 2.5
 * blocks of each other). They rise and spiral inward for 10 s inside a golden braid while the
 * shower plays under a golden-hour sky, the eyes close, and 10 s later the golden mass forms above the pile.
 */
public final class GoldenDreamRitual implements Listener {
    private static final int NEEDED = 7;
    private static final double PILE_R = 2.5;
    private static final double SCAN_R = 6.0;
    private static final long SETTLE_TICKS = 12L;
    private static final int SPIRAL_TICKS = 200;
    private static final double SPIRAL_SPEED = 0.1;
    private static final double BRAID_R = 4.0;
    private static final double BRAID_AMP = 0.7;
    private static final int BRAID_WAVES = 4;
    private static final double BRAID_STEP = 0.09;
    private static final Particle[] SHOWER_MIX = {Particle.SOUL, Particle.END_ROD, Particle.SCULK_CHARGE};
    private static final String SHOWER_SOUND = "bliss:goldgem_dream_shower";
    private static final double SOUND_RANGE_SQ = 64.0 * 64.0;
    private static final int[] CLIP_BEATS = {6, 20, 37, 58, 79, 82, 98, 137, 158, 178};
    private static final boolean[] CLIP_BIG = {true, true, true, false, false, false, true, true, true, false};

    private final BlissGems plugin;
    private final GoldenDream dream;
    private final FragmentCoreRitual fx;
    private final Set<UUID> running = new HashSet<>();
    private final Set<GoldenDreamMass> masses = new HashSet<>();
    private final Map<UUID, Runnable> entryAborts = new HashMap<>();

    GoldenDreamRitual(BlissGems plugin, GoldenDream dream, FragmentCoreRitual fx) {
        this.plugin = plugin;
        this.dream = dream;
        this.fx = fx;
    }

    GoldenDreamWorld world() {
        return this.dream.world();
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!RitualItems.isWireFragment(event.getItemDrop().getItemStack())) return;
        Player p = event.getPlayer();
        Item dropped = event.getItemDrop();
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.judgePile(p, dropped.getLocation()), SETTLE_TICKS);
    }

    private void judgePile(Player p, Location at) {
        if (p == null || at == null || at.getWorld() == null || this.running.contains(p.getUniqueId())) return;
        if (GoldenDreamWorld.isDreamWorld(at.getWorld())) return;
        List<Item> pile = this.collect(p, at);
        if (pile.size() < NEEDED) return;
        if (!this.dream.world().memoryAvailable()) {
            p.sendMessage(dev.xoperr.blissgems.pedestal.PedestalManager.color("&6The fragments stay silent \u2014 the dream has no memory to show yet."));
            if (p.hasPermission("blissgems.admin")) {
                p.sendMessage(dev.xoperr.blissgems.pedestal.PedestalManager.color("&7Put the map's world folder at &f/" + GoldenDreamWorld.IMPORT_FOLDER + " &7(next to the server jar), or set &fgolden-dream.memory-world: generate&7."));
            }
            return;
        }
        Location c = centre(pile);
        for (Item i : pile) if (i.getLocation().distanceSquared(c) > PILE_R * PILE_R) return;
        this.begin(p, c, pile);
    }

    /** One item per fragment number, thrown by this player, within 6 blocks. */
    private List<Item> collect(Player p, Location at) {
        Map<Integer, Item> byNumber = new HashMap<>();
        for (Entity e : at.getWorld().getNearbyEntities(at, SCAN_R, SCAN_R, SCAN_R)) {
            if (!(e instanceof Item item) || item.isDead() || !p.getUniqueId().equals(item.getThrower())) continue;
            if (!RitualItems.isWireFragment(item.getItemStack())) continue;
            int n = RitualItems.wireFragmentNumber(item.getItemStack());
            if (n >= 1 && n <= NEEDED) byNumber.putIfAbsent(n, item);
        }
        return byNumber.size() == NEEDED ? new ArrayList<>(byNumber.values()) : List.of();
    }

    private static Location centre(List<Item> items) {
        double x = 0, y = 0, z = 0;
        for (Item i : items) {
            Location l = i.getLocation();
            x += l.getX();
            y += l.getY();
            z += l.getZ();
        }
        int n = items.size();
        return new Location(items.get(0).getWorld(), x / n, y / n, z / n);
    }

    private void begin(Player player, Location centre, List<Item> pile) {
        UUID id = player.getUniqueId();
        this.running.add(id);
        List<ItemStack> frags = new ArrayList<>();
        pile.stream().sorted(Comparator.comparingInt(i -> RitualItems.wireFragmentNumber(i.getItemStack()))).forEach(i -> {
            ItemStack one = i.getItemStack().clone();
            one.setAmount(1);
            frags.add(one);
        });
        for (Item i : pile) if (!i.isDead()) i.remove();
        World w = centre.getWorld();
        w.playSound(centre, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 2.0f, 0.7f);
        this.plugin.getLogger().info("Golden Dream: entry ritual triggered by " + player.getName() + " at " + w.getName() + " "
            + Math.round(centre.getX()) + "," + Math.round(centre.getY()) + "," + Math.round(centre.getZ()));
        Location low = centre.clone().add(0, 1.0, 0);
        Location high = centre.clone().add(0, 2.0, 0);
        long prevOffset = player.getPlayerTimeOffset();
        boolean prevRel = player.isPlayerTimeRelative();
        this.entryAborts.put(id, () -> {
            for (ItemStack f : frags) w.dropItem(centre, f);
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) DreamSky.restore(p, prevOffset, prevRel);
        });
        new BukkitRunnable() {
            final List<ItemDisplay> shown = new ArrayList<>();
            int t;
            int timer;
            int beat;
            int mix;

            @Override
            public void run() {
                GoldenDreamRitual self = GoldenDreamRitual.this;
                Player p = Bukkit.getPlayer(id);
                if (p == null || !p.isOnline() || p.isDead()) {
                    stopShower();
                    for (ItemStack f : frags) w.dropItem(centre, f);
                    this.release(p, true);
                    this.cancel();
                    return;
                }
                if (this.t >= SPIRAL_TICKS) {
                    if (this.t == SPIRAL_TICKS) stopShower();
                    if (this.cover(p, this.t - SPIRAL_TICKS)) {
                        this.release(p, false);
                        this.cancel();
                        new GoldenDreamMass(self.plugin, self, high.clone(), id).schedule(200L);
                    }
                    this.t++;
                    return;
                }
                DreamSky.golden(p);
                if (this.t % 2 == 0) drawBraid(w, high);
                if (self.fx != null && --this.timer <= 0) {
                    this.timer = 4;
                    self.fx.flash(w, low, 1, 0, 0, 0);
                    this.beat++;
                    int rings = this.beat % 2 == 0 ? 1 : 2;
                    for (int i = 0; i < rings; i++) {
                        Particle part = SHOWER_MIX[++this.mix % SHOWER_MIX.length];
                        double f = 1.0 + Math.pow(ThreadLocalRandom.current().nextDouble(), 2) * 1.2;
                        int base = part == Particle.SCULK_CHARGE ? 45 : 30;
                        self.fx.angledBurst(w, low, 45.0, (int) (base * (0.6 + 0.4 * f)), 0.55 * f, part);
                    }
                }
                if (this.t == 0) {
                    for (Player near : w.getPlayers()) {
                        if (near.getLocation().distanceSquared(low) < SOUND_RANGE_SQ) near.playSound(low, SHOWER_SOUND, SoundCategory.MASTER, 1.4f, 1.0f);
                    }
                    if (self.fx != null) for (ItemStack f : frags) this.shown.add(self.fx.spawnDisplay(high, f, 0.7f));
                }
                for (int b = 0; b < CLIP_BEATS.length; b++) {
                    if (CLIP_BEATS[b] != this.t) continue;
                    boolean big = CLIP_BIG[b];
                    double speed = big ? 2.4 : 1.44;
                    int points = big ? 40 : 28;
                    w.spawnParticle(Particle.END_ROD, low, big ? 26 : 14, 0.4, 0.4, 0.4, 0.06);
                    for (int i = 0; i < points; i++) {
                        double a = Math.PI * 2 * i / points;
                        w.spawnParticle(Particle.POOF, low.getX(), low.getY(), low.getZ(), 0, Math.cos(a), 0.0, Math.sin(a), speed, null, true);
                    }
                    break;
                }
                double r = BRAID_R * (1.0 - this.t / (double) SPIRAL_TICKS);
                for (int i = 0; i < this.shown.size(); i++) {
                    ItemDisplay d = this.shown.get(i);
                    if (!d.isValid()) continue;
                    double a = i * (Math.PI * 2 / this.shown.size()) + this.t * SPIRAL_SPEED;
                    d.teleport(new Location(w, high.getX() + Math.cos(a) * r, high.getY(), high.getZ() + Math.sin(a) * r));
                }
                this.t++;
            }

            private void stopShower() {
                for (Player near : w.getPlayers()) {
                    if (near.getLocation().distanceSquared(low) < SOUND_RANGE_SQ) near.stopSound(SHOWER_SOUND, SoundCategory.MASTER);
                }
            }

            /** The eyes close for 32 ticks (blindness / Bedrock fade). True when done. */
            private boolean cover(Player p, int k) {
                if (k == 0) {
                    for (ItemDisplay d : this.shown) if (d.isValid()) d.remove();
                    this.shown.clear();
                    DreamSky.closeEyes(p, 32);
                }
                return k >= 32;
            }

            private void release(Player p, boolean aborted) {
                if (aborted) GoldenDreamRitual.this.entryAborts.remove(id);
                for (ItemDisplay d : this.shown) if (d.isValid()) d.remove();
                this.shown.clear();
                if (p != null && p.isOnline()) DreamSky.restore(p, prevOffset, prevRel);
                if (aborted) GoldenDreamRitual.this.finish(id);
            }
        }.runTaskTimer(this.plugin, 1L, 1L);
    }

    private static void drawBraid(World w, Location c) {
        int steps = (int) Math.ceil(Math.PI * 2 * Math.sqrt(BRAID_R * BRAID_R + (BRAID_AMP * BRAID_WAVES) * (BRAID_AMP * BRAID_WAVES)) / BRAID_STEP);
        Particle.DustOptions dust = new Particle.DustOptions(GoldenDreamMass.HOLDER_GOLD, 0.5f);
        for (int strand = 0; strand < 2; strand++) {
            for (int i = 0; i < steps; i++) {
                double a = i * (Math.PI * 2 / steps);
                w.spawnParticle(Particle.DUST, c.getX() + Math.cos(a) * BRAID_R, c.getY() + BRAID_AMP * Math.sin(BRAID_WAVES * a + strand * Math.PI),
                    c.getZ() + Math.sin(a) * BRAID_R, 1, 0, 0, 0, 0, dust, true);
            }
        }
    }

    void finish(UUID id) {
        if (id != null) this.running.remove(id);
    }

    void register(GoldenDreamMass m) {
        this.masses.add(m);
    }

    void unregister(GoldenDreamMass m) {
        this.masses.remove(m);
    }

    void entryStarted(UUID id) {
        this.entryAborts.remove(id);
    }

    public boolean isRunning(Player p) {
        return p != null && this.running.contains(p.getUniqueId());
    }

    /** Drops pending fragments back and collapses any mass (on shutdown). */
    public void abortAll() {
        for (Runnable r : new ArrayList<>(this.entryAborts.values())) {
            try {
                r.run();
            } catch (Throwable ignored) {
            }
        }
        this.entryAborts.clear();
        for (GoldenDreamMass m : new ArrayList<>(this.masses)) {
            try {
                m.abortNow();
            } catch (Throwable ignored) {
            }
        }
        this.masses.clear();
        this.running.clear();
    }
}
