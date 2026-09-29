package dev.xoperr.blissgems.villagerevent;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.pedestal.PedestalManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * The Villager event, start to finish:
 * <ol>
 *   <li>Phase 1 - the disc summon ritual over the spawn pedestal drops the Disc of the Wanderer,
 *       which, played in a jukebox, whispers where the lost village (the compass) is.</li>
 *   <li>Phase 2 - "Lost Souls" intro; three villagers hide around the world. Kill one to get its
 *       soul, bring the soul to the compass to return the villager.</li>
 *   <li>Phase 3 - once all three are home, the Last Raid: 8 waves at the compass. Win and the
 *       top damage dealer is crowned; lose all three villagers and the village falls.</li>
 * </ol>
 */
public final class VillagerEventManager {
    private static final String[] LOST_SOULS = {
        "we are the last of our kind..", "three souls who yet remain", "find us", "before all is truly lost",
        "we beg of you", "rise against our captors", "and restore what once was", "do not falter",
        "do not be swayed by", "the shimmer of emeralds", "or our super duper", "really...",
        "crazy good trades..", "You", "Hold the hope of our people", "bring us home", "and rekindle",
        "the spirit of the villages"};
    private static final Particle.DustOptions RED_DUST = new Particle.DustOptions(Color.fromRGB(190, 20, 20), 2.0f);

    private final BlissGems plugin;
    private final VillagerEventState state;
    private final VillagerEventItems items;
    private final VillagerEventVisuals visuals;
    private BukkitTask pendingHuntStart;
    private boolean raidScheduled;

    public VillagerEventManager(BlissGems plugin) {
        this.plugin = plugin;
        this.state = new VillagerEventState(plugin);
        this.state.load();
        this.items = new VillagerEventItems(plugin);
        this.visuals = new VillagerEventVisuals(plugin, this.state, this.items);
        this.visuals.start();
        if (this.state.isFinalActive() && this.state.compassLoc() != null) {
            // resume the raid once worlds and entities are loaded
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (this.state.isFinalActive() && VillagerRaid.current() == null) {
                    new VillagerRaid(plugin, this, this.state.compassLoc()).resume(this.state.raidWave(), this.state.raidBetween());
                }
            }, 100L);
        }
    }

    public VillagerEventState state() {
        return this.state;
    }

    public VillagerEventItems items() {
        return this.items;
    }

    boolean pedestalProtects(Location loc) {
        PedestalManager p = this.plugin.getPedestalManager();
        return p != null && p.state().isPedestalBlock(loc);
    }

    // ---- phase 1 ----

    /** False when there is no spawn pedestal to hold the ritual on. */
    public boolean phase1() {
        PedestalManager p = this.plugin.getPedestalManager();
        return p != null && new DiscSummonRitual(this.plugin, this.items).summon(p.state().mainLoc());
    }

    // ---- phase 2 ----

    public void phase2() {
        if (VillagerRaid.current() != null) VillagerRaid.current().stop(true);
        this.state.setRaidWave(0);
        this.state.setRaidBetween(false);
        this.state.setVillagersReturned(0);
        this.state.clearReturnedVillagers();
        this.state.setFinalActive(false);
        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        this.playLostSoulsIntro();
    }

    /** The "Lost Souls:" lines type themselves out in titles over ~28 s, then the hunt begins. */
    private void playLostSoulsIntro() {
        if (this.plugin.getConfig().getBoolean("villager-event.lost-souls-audio", true)) {
            for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p, "bliss:lost_souls", SoundCategory.MASTER, 1000000.0f, 1.0f);
        }
        int totalChars = 0;
        for (String line : LOST_SOULS) totalChars += line.length();
        if (totalChars == 0) totalChars = 1;
        long typed = 0L;
        boolean first = true;
        Component header = VillagerEventItems.text("&6&lLost Souls:");
        for (String line : LOST_SOULS) {
            int len = Math.max(1, line.length());
            long lineStart = 568L * typed / totalChars;
            long lineEnd = 568L * (typed + len) / totalChars;
            long span = Math.max(len, lineEnd - lineStart);
            for (int c = 1; c <= len; c++) {
                final String shown = line.substring(0, Math.min(c, line.length()));
                final boolean fadeIn = first;
                first = false;
                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    Title title = Title.title(header, VillagerEventItems.text("&f" + shown),
                        Title.Times.times(Duration.ofMillis(fadeIn ? 400 : 0), Duration.ofMillis(6000), Duration.ZERO));
                    for (Player p : Bukkit.getOnlinePlayers()) p.showTitle(title);
                }, lineStart + span * (c - 1) / len);
            }
            typed += len;
        }
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(PedestalManager.color("&e(!)Find the lost souls."));
                p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_PLACE, 1.0f, 1.0f);
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.0f);
            }
        }, 578L);
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
        }, 580L);
        if (this.pendingHuntStart != null) this.pendingHuntStart.cancel();
        this.pendingHuntStart = Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            this.pendingHuntStart = null;
            this.state.setActive(true);
            this.state.save();
        }, 586L);
    }

    // ---- phase 3 ----

    public void phase3() {
        if (!this.state.isEventRunning()) this.state.setActive(true);
        this.beginRaid();
    }

    public void beginRaid() {
        if (!this.state.isEventRunning()) {
            return;
        }
        if (this.pendingHuntStart != null) {
            this.pendingHuntStart.cancel();
            this.pendingHuntStart = null;
        }
        this.state.setActive(false);
        this.state.setFinalActive(true);
        this.state.save();
        this.raidOpeningShot();
        if (VillagerRaid.current() != null || this.raidScheduled) {
            return;
        }
        Location at = this.state.compassLoc();
        if (at == null || at.getWorld() == null) {
            at = Bukkit.getOnlinePlayers().stream().findFirst().map(Player::getLocation).orElse(null);
        }
        if (at == null) {
            this.plugin.getLogger().warning("Villager raid: no compass location and no players online — raid not spawned.");
            return;
        }
        this.raidScheduled = true;
        final Location center = at;
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            this.raidScheduled = false;
            if (this.state.isFinalActive() && VillagerRaid.current() == null) new VillagerRaid(this.plugin, this, center).start();
        }, 118L);
    }

    public void failRaid() {
        if (!this.state.isFinalActive()) {
            return;
        }
        VillagerRaid raid = VillagerRaid.current();
        if (raid != null) raid.stop(true);
        this.state.setFinalActive(false);
        this.state.setActive(true);
        this.state.setRaidWave(0);
        this.state.setRaidBetween(false);
        this.state.setVillagersReturned(0);
        this.state.clearReturnedVillagers();
        this.state.save();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(PedestalManager.color("&4&l✖ THE VILLAGE HAS FALLEN &7— the last raid is lost."));
            p.sendMessage(PedestalManager.color("&e(!)Return all three souls to the compass to raise the village again."));
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.6f);
        }
    }

    /** Sonic boom at the compass, seven bolts, a swirling red swarm that collapses and bursts. */
    private void raidOpeningShot() {
        Location compass = this.state.compassLoc();
        if (compass == null || compass.getWorld() == null) {
            return;
        }
        World w = compass.getWorld();
        Location c = compass.clone().add(0, 2, 0);
        w.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 50.0f, 1.6f);
        w.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 20.0f, 1.0f);
        w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 20.0f, 0.75f);
        for (int i = 0; i < 72; i++) {
            double a = Math.PI * 2 * i / 72.0;
            w.spawnParticle(Particle.END_ROD, c, 0, Math.cos(a), 0, Math.sin(a), 2.4, null, true);
        }
        ThreadLocalRandom r = ThreadLocalRandom.current();
        List<List<Location>> bolts = new ArrayList<>();
        for (int i = 0; i < 7; i++) bolts.add(DrawnBolt.path(w, c, 18.0, 14.0, r));
        int[] progress = new int[7];
        double[] ang = new double[90], rad = new double[90], ys = new double[90];
        for (int i = 0; i < 90; i++) {
            ang[i] = r.nextDouble() * Math.PI * 2.0;
            rad[i] = 16.0 * (0.75 + r.nextDouble() * 0.25);
            ys[i] = c.getY() + (r.nextDouble() - 0.35) * 8.0;
        }
        new BukkitRunnable() {
            int t;

            @Override
            public void run() {
                for (int b = 0; b < 7; b++) {
                    List<Location> bolt = bolts.get(b);
                    if (this.t < b * 5 || progress[b] >= bolt.size()) continue;
                    int end = Math.min(bolt.size(), progress[b] + 7);
                    for (int i = progress[b]; i < end; i++) w.spawnParticle(Particle.WAX_OFF, bolt.get(i), 1, 0, 0, 0, 0);
                    if (progress[b] == 0) w.playSound(bolt.get(0), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 1.4f);
                    progress[b] = end;
                }
                if (this.t >= 50 && this.t < 86) {
                    double k = (this.t - 50) / 36.0;
                    for (int i = 0; i < 90; i++) {
                        double rr = rad[i] * (1.0 - k);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, c.getX() + Math.cos(ang[i]) * rr, ys[i] + (c.getY() - ys[i]) * k, c.getZ() + Math.sin(ang[i]) * rr, 1, 0, 0, 0, 0);
                        ang[i] += 0.05;
                    }
                    if (this.t == 50) w.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 6.0f, 0.6f);
                }
                if (this.t >= 90 && this.t < 100) {
                    double spread = 14.0 * (this.t - 90 + 1) / 10.0;
                    w.spawnParticle(Particle.DUST, c, 70, spread * 0.6, spread * 0.4, spread * 0.6, 0, RED_DUST, true);
                    if (this.t == 90) w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 8.0f, 1.4f);
                }
                if (this.t == 102) {
                    for (int i = 0; i < 64; i++) {
                        double a = Math.PI * 2 * i / 64.0;
                        w.spawnParticle(Particle.FLAME, c, 0, Math.cos(a), 0.05, Math.sin(a), 1.2, null, true);
                    }
                    w.playSound(c, Sound.ITEM_FIRECHARGE_USE, 8.0f, 0.7f);
                }
                if (++this.t > 118) this.cancel();
            }
        }.runTaskTimer(this.plugin, 1L, 1L);
    }

    // ---- end ----

    /** Returns false when nothing is running. */
    public boolean end() {
        boolean raid = this.state.isFinalActive();
        boolean pending = this.pendingHuntStart != null;
        if (!raid && !this.state.isActive() && !pending) {
            return false;
        }
        if (pending) {
            this.pendingHuntStart.cancel();
            this.pendingHuntStart = null;
        }
        if (raid || this.state.topRaidDamager() != null) VillagerRaid.crownTopDamager(this.state);
        if (VillagerRaid.current() != null) VillagerRaid.current().stop(true);
        this.state.endEvent();
        VillagerRaid.sweepRaiders();
        return true;
    }

    public void shutdown() {
        this.visuals.stop();
        if (VillagerRaid.current() != null) VillagerRaid.current().stop(false);
        this.state.save();
    }
}
