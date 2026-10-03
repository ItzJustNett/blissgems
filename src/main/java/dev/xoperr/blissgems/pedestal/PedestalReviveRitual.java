package dev.xoperr.blissgems.pedestal;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.EnergyState;
import dev.xoperr.blissgems.utils.GemType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * The restoration ritual. After 5 deposits the Broken player stands on the beacon,
 * holds still for 3 s, then: eight gems rise from below, the player floats up 4 blocks
 * under lightning, the gems spin around them while their energy climbs back to Pristine
 * and their gem rolls faster and faster until it lands on a new one.
 */
public final class PedestalReviveRitual implements Listener {
    // gem ring layout around the focus point (x, z)
    private static final Object[][] RING = {
        {GemType.PUFF, 0, 6}, {GemType.WEALTH, -6, 0}, {GemType.FLUX, 6, 0}, {GemType.ASTRA, 0, -6},
        {GemType.STRENGTH, -4, 4}, {GemType.LIFE, 4, 4}, {GemType.SPEED, -4, -4}, {GemType.FIRE, 4, -4}
    };
    // angle each gem holds on the spinning circle
    private static final GemType[] CIRCLE = {GemType.STRENGTH, GemType.ASTRA, GemType.FLUX, GemType.WEALTH,
        GemType.PUFF, GemType.FIRE, GemType.SPEED, GemType.LIFE};

    private static World weatherWorld;
    private static boolean savedStorm;
    private static boolean savedThundering;
    private static int savedWeatherDuration;
    private static int savedThunderDuration;

    private final BlissGems plugin;
    private final PedestalManager manager;
    private final PedestalState state;
    private final Random random = new Random();
    private Map<GemType, ItemDisplay> activeGems;

    public PedestalReviveRitual(BlissGems plugin, PedestalManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.state = manager.state();
    }

    // ---- trigger: standing on the beacon after all deposits ----

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!this.state.revivalLocked(player.getUniqueId())) {
            this.tryStart(player);
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ())) {
            return;
        }
        Location held = from.clone();
        held.setYaw(to.getYaw());
        held.setPitch(to.getPitch());
        event.setTo(held);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLockedTeleport(PlayerTeleportEvent event) {
        if (!this.state.revivalLocked(event.getPlayer().getUniqueId())) {
            return;
        }
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL || cause == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            event.setCancelled(true);
        }
    }

    /** Undoes what an interrupted ritual left on a player who logged out mid-ritual. */
    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (this.state.revivalLocked(p.getUniqueId())) return;
        if (p.isInvulnerable() && (p.getGameMode() == org.bukkit.GameMode.SURVIVAL || p.getGameMode() == org.bukkit.GameMode.ADVENTURE)) {
            p.setInvulnerable(false);
            this.plugin.getLogger().info("Pedestal: cleared a stuck invulnerable flag on " + p.getName());
        }
        if (!p.hasGravity()) p.setGravity(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLockedDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p && this.state.revivalLocked(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    void tryStart(Player player) {
        if (this.state.pedestalBusy() || this.state.ritual() != PedestalState.Ritual.REVIVE
            || this.state.count() < PedestalState.DEPOSITS_REQUIRED || !player.getUniqueId().equals(this.state.revivingPlayer())) {
            return;
        }
        if (!onCenter(player)) {
            return;
        }
        this.state.setPedestalBusy(true);
        this.awaitStillness(player);
    }

    private boolean onCenter(Player player) {
        Location stand = this.state.standLoc();
        if (stand == null || !player.getWorld().equals(stand.getWorld())) {
            return false;
        }
        Location at = player.getLocation();
        double dx = at.getX() - stand.getX();
        double dz = at.getZ() - stand.getZ();
        return dx * dx + dz * dz <= 0.5625 && Math.abs(at.getY() - stand.getY()) <= 1.0;
    }

    private void awaitStillness(Player player) {
        UUID id = player.getUniqueId();
        new BukkitRunnable() {
            Location last = player.getLocation().clone();
            int still;
            int waited;

            @Override
            public void run() {
                PedestalReviveRitual self = PedestalReviveRitual.this;
                if (!player.isOnline() || player.isDead() || !self.state.pedestalBusy() || !self.onCenter(player) || ++this.waited > 600) {
                    this.cancel();
                    self.state.setPedestalBusy(false);
                    return;
                }
                Location at = player.getLocation();
                if (at.distanceSquared(this.last) > 0.0025) {
                    this.still = 0;
                    this.last = at.clone();
                } else if (++this.still >= 60) {
                    this.cancel();
                    self.engageLock(player, id);
                }
            }
        }.runTaskTimer(this.plugin, 1L, 1L);
    }

    private void engageLock(Player player, UUID id) {
        // damage is cancelled by onLockedDamage while locked; never set the persistent
        // invulnerable flag (it stuck forever when a player left mid-ritual)
        this.state.setRevivalLocked(id, true);
        this.beginAnimation(player, this.state.mainLoc().add(0.5, -3.0, 0.5));
    }

    // ---- animation ----

    private void beginAnimation(Player player, Location focus) {
        if (!player.isOnline() || player.isDead()) {
            this.cleanupAbort(player.getUniqueId(), true);
            return;
        }
        player.setGravity(false);
        Map<GemType, ItemDisplay> gems = this.spawnGemRing(focus);
        this.activeGems = gems;
        int levelTicks = this.smoothLevelHead(player);
        long delay = (levelTicks > 0 ? levelTicks + 8L : 1L) + 50L;
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (!player.isOnline()) {
                removeGems(gems);
                this.cleanupAbort(player.getUniqueId(), true);
            } else {
                this.raiseGemsToChest(player, gems, focus);
            }
        }, delay);
    }

    /** Turns the head to the nearest cardinal direction and level over 40 ticks. */
    private int smoothLevelHead(Player player) {
        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();
        float target = (float) (Math.ceil(yaw / 90.0 - 0.5) * 90.0);
        float delta = Location.normalizeYaw(target - yaw);
        if (Math.abs(delta) < 1.0f && Math.abs(pitch) < 1.0f) {
            return 0;
        }
        new BukkitRunnable() {
            int t;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }
                double e = ease(Math.min(1.0, ++this.t / 40.0));
                player.setRotation(yaw + (float) (delta * e), (float) (pitch * (1.0 - e)));
                if (this.t >= 40) this.cancel();
            }
        }.runTaskTimer(this.plugin, 1L, 1L);
        return 40;
    }

    private static double ease(double x) {
        return x < 0.5 ? 2.0 * x * x : 1.0 - Math.pow(-2.0 * x + 2.0, 2.0) / 2.0;
    }

    private Map<GemType, ItemDisplay> spawnGemRing(Location focus) {
        Map<GemType, ItemDisplay> gems = new EnumMap<>(GemType.class);
        for (Object[] g : RING) {
            GemType type = (GemType) g[0];
            ItemDisplay display = this.spawnGemDisplay(focus.clone().add((int) g[1], 1.5, (int) g[2]), type);
            if (display != null) gems.put(type, display);
        }
        return gems;
    }

    private ItemDisplay spawnGemDisplay(Location at, GemType type) {
        ItemStack item = CustomItemManager.getItemById(GemType.buildOraxenId(type, 1));
        if (item == null) {
            return null;
        }
        Color glow = this.plugin.getGemRitualManager() != null ? this.plugin.getGemRitualManager().getGemColor(type.getId()) : Color.WHITE;
        return at.getWorld().spawn(at, ItemDisplay.class, d -> {
            d.setPersistent(false);
            d.setItemStack(item);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.65f, 0.65f, 0.65f), new AxisAngle4f(0, 0, 1, 0)));
            d.setGlowing(true);
            d.setGlowColorOverride(glow);
            d.setTeleportDuration(1);
        });
    }

    private void raiseGemsToChest(Player player, Map<GemType, ItemDisplay> gems, Location focus) {
        double startY = focus.getY() + 1.5;
        double endY = player.getLocation().getY() + 0.3;
        new BukkitRunnable() {
            int t;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    removeGems(gems);
                    PedestalReviveRitual.this.cleanupAbort(player.getUniqueId(), true);
                    return;
                }
                double e = ease(Math.min(1.0, ++this.t / 60.0));
                setGemRingY(gems, startY + (endY - startY) * e);
                if (this.t >= 60) {
                    this.cancel();
                    new RitualTask(player, focus, gems).runTaskTimer(PedestalReviveRitual.this.plugin, 1L, 1L);
                }
            }
        }.runTaskTimer(this.plugin, 1L, 1L);
    }

    private static void setGemRingY(Map<GemType, ItemDisplay> gems, double y) {
        for (ItemDisplay d : gems.values()) {
            if (d == null || d.isDead()) continue;
            Location l = d.getLocation();
            l.setY(y);
            l.setYaw(0);
            l.setPitch(0);
            d.teleport(l);
        }
    }

    private static void removeGems(Map<GemType, ItemDisplay> gems) {
        if (gems == null) {
            return;
        }
        for (ItemDisplay d : gems.values()) {
            if (d != null && !d.isDead()) d.remove();
        }
        gems.clear();
    }

    private void pedestalLightning(Location center) {
        if (center == null) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            int a = 7 + this.random.nextInt(9);
            int b = 7 + this.random.nextInt(9);
            int c = 7 + this.random.nextInt(9);
            int d = 7 + this.random.nextInt(9);
            Location[] spots = {center.clone().add(a, 0, -b), center.clone().add(-c, 0, d), center.clone().add(c, 0, b), center.clone().add(-a, 0, -d)};
            center.getWorld().strikeLightning(spots[this.random.nextInt(4)]);
        }
    }

    // ---- gem rolling ----

    private List<String> rollPool() {
        List<String> pool = new ArrayList<>(this.plugin.getGemManager().getAvailableGemIds());
        if (pool.isEmpty()) {
            for (GemType t : GemType.values()) pool.add(t.getId());
        }
        return pool;
    }

    private void rollRandomGem(Player player) {
        String current = this.plugin.getGemManager().getGemId(player);
        List<String> pool = this.rollPool();
        if (pool.size() > 1 && current != null) pool.remove(current);
        String next = pool.get(this.random.nextInt(pool.size()));
        if (this.plugin.getGemManager().findGemInInventory(player) != null) {
            this.plugin.getGemManager().replaceGem(player, next);
        } else {
            this.plugin.getGemManager().giveGem(player, next, 1);
        }
        player.sendMessage(PedestalManager.color("🔮 &bYou have traded your current gem to " + this.plugin.getGemManager().getGemColorCode(next) + this.plugin.getGemManager().getGemDisplayName(next)));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.0f);
        GemType type = GemType.fromOraxenId(next + "_gem_t1");
        if (type != null) PedestalManager.applyGemGlow(player, type, 200);
    }

    // ---- weather ----

    public static void forceStormAndThunder(World world) {
        if (world == null || weatherWorld != null) {
            return;
        }
        weatherWorld = world;
        savedStorm = world.hasStorm();
        savedThundering = world.isThundering();
        savedWeatherDuration = world.getWeatherDuration();
        savedThunderDuration = world.getThunderDuration();
        world.setStorm(true);
        world.setThundering(true);
        world.setWeatherDuration(20000);
        world.setThunderDuration(20000);
    }

    public static void restoreWeather() {
        if (weatherWorld == null) {
            return;
        }
        try {
            weatherWorld.setStorm(savedStorm);
            weatherWorld.setThundering(savedThundering);
            weatherWorld.setWeatherDuration(Math.max(savedWeatherDuration, 0));
            weatherWorld.setThunderDuration(Math.max(savedThunderDuration, 0));
        } catch (Throwable ignored) {
        }
        weatherWorld = null;
    }

    // ---- cleanup ----

    public void shutdownRestore() {
        UUID id = this.state.revivingPlayer();
        if (id != null && this.state.pedestalBusy()) {
            this.cleanupAbort(id, false);
        }
    }

    private void cleanupAbort(UUID id, boolean resetRitual) {
        removeGems(this.activeGems);
        this.activeGems = null;
        this.state.setRevivalLocked(id, false);
        this.state.setPedestalBusy(false);
        restoreWeather();
        Player player = Bukkit.getPlayer(id);
        if (player != null) {
            player.setGravity(true);
            PedestalManager.clearGemGlow(player);
        }
        this.state.removeBeam();
        if (resetRitual) {
            this.state.resetRitual();
        }
    }

    // ---- main loop ----

    private final class RitualTask extends BukkitRunnable {
        private final Player player;
        private final Location focus;
        private final Map<GemType, ItemDisplay> gems;
        private int phase;
        private int liftTick;
        private int holdTick;
        private int spinTick;
        private double angle;
        private int nextRollTick = 285;
        private int energy;

        RitualTask(Player player, Location focus, Map<GemType, ItemDisplay> gems) {
            this.player = player;
            this.focus = focus.clone();
            this.gems = gems;
            this.energy = PedestalReviveRitual.this.plugin.getEnergyManager().getEnergy(player);
        }

        @Override
        public void run() {
            if (!this.player.isOnline() || this.player.isDead() || PedestalReviveRitual.this.state.ritual() != PedestalState.Ritual.REVIVE) {
                this.abort();
                return;
            }
            switch (this.phase) {
                case 0 -> this.lift();
                case 1 -> this.hold();
                case 2 -> this.spin();
                default -> this.finish();
            }
        }

        /** 200 ticks rising 0.02 blocks per tick, lightning every 2 s. */
        private void lift() {
            this.player.teleport(this.player.getLocation().add(0.0, 0.02, 0.0));
            setGemRingY(this.gems, this.player.getLocation().getY() + 0.3);
            this.liftTick++;
            if (this.liftTick % 40 == 0 && this.liftTick <= 160) {
                PedestalReviveRitual.this.pedestalLightning(PedestalReviveRitual.this.state.mainLoc());
            }
            if (this.liftTick >= 200) this.phase = 1;
        }

        private void hold() {
            setGemRingY(this.gems, this.player.getLocation().getY() + 0.3);
            if (++this.holdTick >= 30) this.phase = 2;
        }

        /**
         * 380 ticks of spinning. Radius 6 at 1 deg/tick until tick 229, then over 150 ticks
         * the ring tightens to 0.5 and speeds to 12 deg/tick. Energy climbs +1 every 10 ticks
         * from 230, and the gem re-rolls from tick 285 with gaps shrinking from 14 to 2 ticks.
         */
        private void spin() {
            double radius = 6.0;
            double speed = 1.0;
            if (this.spinTick >= 229) {
                double k = Math.min((this.spinTick - 229) / 150.0, 1.0);
                radius = 6.0 - 5.5 * k;
                speed = 1.0 + 11.0 * k;
            }
            this.angle += speed;
            Location main = PedestalReviveRitual.this.state.mainLoc();
            Location center = new Location(this.player.getWorld(), main.getX() + 0.5, this.player.getLocation().getY() + 0.3, main.getZ() + 0.5);
            for (int i = 0; i < CIRCLE.length; i++) {
                tpGem(this.gems.get(CIRCLE[i]), center, this.angle + i * 45.0, radius);
            }
            if (this.spinTick > 0 && this.spinTick % 50 == 0) {
                PedestalReviveRitual.this.pedestalLightning(main);
            }
            int next = this.spinTick + 1;
            if (next == 230 || next == 240 || next == 250 || next == 260 || next == 270) {
                this.energy = Math.min(EnergyState.PRISTINE.getMinEnergy(), this.energy + 1);
                PedestalReviveRitual.this.plugin.getEnergyManager().setEnergy(this.player, this.energy);
            }
            if (this.spinTick == this.nextRollTick && this.spinTick < 379) {
                PedestalReviveRitual.this.rollRandomGem(this.player);
                double left = Math.max(0.0, Math.min(1.0, (379 - this.spinTick) / 94.0));
                this.nextRollTick = this.spinTick + Math.max(2, (int) Math.round(2.0 + 12.0 * left));
            }
            if (++this.spinTick >= 380) this.phase = 3;
        }

        private static void tpGem(ItemDisplay d, Location center, double deg, double radius) {
            if (d == null || d.isDead()) {
                return;
            }
            double r = Math.toRadians(deg);
            Location l = center.clone().add(-radius * Math.sin(r), 0.0, radius * Math.cos(r));
            l.setYaw(0);
            l.setPitch(0);
            d.teleport(l);
        }

        private void finish() {
            this.cancel();
            PedestalReviveRitual self = PedestalReviveRitual.this;
            UUID id = this.player.getUniqueId();
            removeGems(this.gems);
            self.activeGems = null;
            String gemId = self.plugin.getGemManager().getGemId(this.player);
            Color color = gemId != null && self.plugin.getGemRitualManager() != null ? self.plugin.getGemRitualManager().getGemColor(gemId) : Color.WHITE;
            Location burst = this.player.getLocation().add(0.0, 1.0, 0.0);
            burst.getWorld().spawnParticle(Particle.DUST, burst, 350, 1.5, 1.5, 1.5, 0.0, new Particle.DustOptions(color, 3.0f), true);
            PedestalManager.clearGemGlow(this.player);
            restoreWeather();
            self.state.setRevivalLocked(id, false);
            self.state.setPedestalBusy(false);
            self.state.setRevivingPlayer(null);
            self.state.resetCount();
            this.player.setGravity(true);
            // blast everyone else away from the pedestal
            Location stand = self.state.standLoc();
            if (stand != null) {
                for (Entity e : stand.getWorld().getNearbyEntities(stand, 10.0, 10.0, 10.0)) {
                    if (e.getUniqueId().equals(id)) continue;
                    Vector push = e.getLocation().toVector().subtract(stand.toVector());
                    if (push.lengthSquared() < 1.0E-4) push = new Vector(0, 0, 1);
                    e.setVelocity(push.normalize().multiply(1.1).setY(0.75));
                }
            }
            String done = PedestalManager.color("&a" + this.player.getName() + " has succesfully completed their Restoration ritual!");
            for (Player online : Bukkit.getOnlinePlayers()) online.sendMessage(done);
            self.manager.unlock(this.player, Achievement.REAWAKENING);
            Location at = this.player.getLocation();
            this.player.playSound(at, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.0f);
            this.player.playSound(at, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.0f);
            this.player.playSound(at, Sound.BLOCK_BELL_RESONATE, 2.0f, 1.0f);
            this.player.playSound(at, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.0f);
            this.player.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
            this.player.playSound(at, Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.5f);
            this.player.playSound(at, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
            self.state.setRitual(PedestalState.Ritual.NONE);
            self.state.setActive(false);
            self.state.removeBeam();
            self.state.unbuildStructure();
            self.state.save();
        }

        private void abort() {
            this.cancel();
            removeGems(this.gems);
            PedestalReviveRitual.this.cleanupAbort(this.player.getUniqueId(), true);
        }
    }
}
