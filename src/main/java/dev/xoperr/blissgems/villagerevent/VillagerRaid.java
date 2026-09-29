package dev.xoperr.blissgems.villagerevent;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.pedestal.PedestalManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Raider;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * Phase 3, "The Last Raid": 8 waves of pillagers, vindicators, ravagers, witches, zombies and
 * zombified piglins land around the compass under a held thunderstorm. Raiders hunt the three
 * village villagers first, then players. From wave 2 some zombies build or dig toward their
 * target, from wave 4 some web the nearest player; every block they touch reverts after 30 s.
 * Clearing wave 8 wins; losing all three villagers fails it.
 */
public final class VillagerRaid implements Listener {
    public static final String RAID_TAG = "bliss_last_raid";
    private static final int WAVES = 8;
    private static final String TAG_WEB = "bliss_raid_web";
    private static final String TAG_BUILD = "bliss_raid_build";
    private static final String TAG_BREAK = "bliss_raid_break";
    private static final String TITLE = "ᴛʜᴇ ʟᴀꜱᴛ ʀᴀɪᴅ";
    private static VillagerRaid current;

    private final BlissGems plugin;
    private final VillagerEventManager manager;
    private final VillagerEventState state;
    private final Location center;
    private final BossBar bar;
    private final Set<UUID> alive = new HashSet<>();
    private final List<Edit> edits = new ArrayList<>();
    private final List<BukkitTask> tasks = new ArrayList<>();
    private int wave;
    private int waveSize;
    private int pendingSpawns;
    private boolean betweenWaves;
    private boolean finished;
    private long refillStart = -1L;
    private boolean stormHeld;
    private boolean prevStorm;
    private boolean prevThunder;
    private int prevWeather;

    public static VillagerRaid current() {
        return current;
    }

    public VillagerRaid(BlissGems plugin, VillagerEventManager manager, Location center) {
        this.plugin = plugin;
        this.manager = manager;
        this.state = manager.state();
        this.center = center.clone();
        this.bar = BossBar.bossBar(barTitle(), 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private static Component barTitle() {
        Component out = Component.empty();
        int n = TITLE.length();
        for (int i = 0; i < n; i++) {
            double t = n == 1 ? 0.0 : (double) i / (n - 1);
            double k = 1.0 - Math.abs(t * 2.0 - 1.0);
            out = out.append(Component.text(TITLE.charAt(i)).color(TextColor.color((int) Math.round(105 + 109 * k), (int) Math.round(8 + 26 * k), (int) Math.round(16 + 18 * k))));
        }
        return out.decoration(TextDecoration.BOLD, true);
    }

    public void start() {
        current = this;
        for (Player p : Bukkit.getOnlinePlayers()) p.showBossBar(this.bar);
        this.nextWave();
        this.bellToll();
        this.startTasks();
    }

    /** Picks a raid back up after a restart, adopting raiders still tagged in the world. */
    public void resume(int wave, boolean between) {
        if (wave < 1) {
            this.start();
            return;
        }
        current = this;
        this.wave = wave;
        this.betweenWaves = between;
        this.waveSize = composition(Math.min(wave, WAVES)).size();
        World w = this.center.getWorld();
        if (w != null) {
            for (int cx = -2; cx <= 2; cx++) {
                for (int cz = -2; cz <= 2; cz++) w.getChunkAt((this.center.getBlockX() >> 4) + cx, (this.center.getBlockZ() >> 4) + cz);
            }
            for (Entity e : w.getEntities()) {
                if (!e.isDead() && e.isValid() && e.getScoreboardTags().contains(RAID_TAG)) this.alive.add(e.getUniqueId());
            }
        }
        for (Player p : Bukkit.getOnlinePlayers()) p.showBossBar(this.bar);
        this.bar.progress((float) this.barProgress());
        if (this.betweenWaves) {
            this.refillStart = Bukkit.getCurrentTick();
            Bukkit.getScheduler().runTaskLater(this.plugin, this::nextWave, 100L);
        }
        this.startTasks();
    }

    private void startTasks() {
        this.tasks.add(Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 20L, 20L));
        this.tasks.add(Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (!this.finished) this.bar.progress((float) this.barProgress());
        }, 0L, 2L));
        this.tasks.add(Bukkit.getScheduler().runTaskTimer(this.plugin, this::ambientBolt, 35L, 35L));
        this.tasks.add(Bukkit.getScheduler().runTaskTimer(this.plugin, this::raiderBehaviour, 15L, 15L));
        this.stormOn();
    }

    private void tick() {
        if (this.finished) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) p.showBossBar(this.bar);
        this.alive.removeIf(id -> {
            Entity e = Bukkit.getEntity(id);
            return e != null && (e.isDead() || !e.isValid());
        });
        this.witchesDrink();
        if (this.betweenWaves || this.pendingSpawns > 0 || !this.alive.isEmpty()) {
            return;
        }
        if (this.wave >= WAVES) {
            this.victory();
            return;
        }
        this.betweenWaves = true;
        this.state.setRaidBetween(true);
        this.state.save();
        this.refillStart = Bukkit.getCurrentTick();
        this.bellToll();
        Bukkit.getScheduler().runTaskLater(this.plugin, this::nextWave, 100L);
    }

    private void nextWave() {
        if (this.finished) {
            return;
        }
        this.wave++;
        this.betweenWaves = false;
        this.refillStart = -1L;
        this.state.setRaidWave(this.wave);
        this.state.setRaidBetween(false);
        this.state.save();
        List<EntityType> mobs = composition(this.wave);
        this.waveSize = mobs.size();
        this.bar.progress(1.0f);
        Collections.shuffle(mobs);
        this.pendingSpawns = mobs.size();
        long delay = 0L;
        for (int i = 0; i < mobs.size(); ) {
            int group = Math.min(1 + ThreadLocalRandom.current().nextInt(2), mobs.size() - i);
            List<EntityType> batch = new ArrayList<>(mobs.subList(i, i + group));
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                if (this.finished) return;
                for (EntityType type : batch) this.spawnOne(type);
                this.pendingSpawns -= batch.size();
            }, delay);
            i += group;
            delay += 8L;
        }
        this.playAll(Sound.EVENT_RAID_HORN, 1.0f);
    }

    private void victory() {
        this.finished = true;
        this.cleanup();
        this.announce("&a&l✦ THE VILLAGE STANDS ✦ &7— the last raid is broken.");
        this.playAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f);
        this.playAll(Sound.ENTITY_PLAYER_LEVELUP, 1.2f);
        crownTopDamager(this.state);
        this.state.endEvent();
    }

    public void stop(boolean removeRaiders) {
        this.finished = true;
        if (removeRaiders) {
            for (UUID id : this.alive) {
                Entity e = Bukkit.getEntity(id);
                if (e != null) e.remove();
            }
            sweepRaiders();
        }
        this.cleanup();
    }

    public static int sweepRaiders() {
        int n = 0;
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (e.getScoreboardTags().contains(RAID_TAG)) {
                    e.remove();
                    n++;
                }
            }
        }
        return n;
    }

    private void cleanup() {
        HandlerList.unregisterAll(this);
        for (BukkitTask t : this.tasks) t.cancel();
        this.tasks.clear();
        this.restoreEdits();
        this.stormOff();
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(this.bar);
        if (current == this) current = null;
    }

    /** Whoever dealt the most damage to raiders is announced as the raid's Honored One. */
    public static void crownTopDamager(VillagerEventState state) {
        UUID top = state.topRaidDamager();
        if (top == null) {
            return;
        }
        Player online = Bukkit.getPlayer(top);
        String name = online != null ? online.getName() : Bukkit.getOfflinePlayer(top).getName();
        if (name == null) name = "Someone";
        Bukkit.broadcastMessage(PedestalManager.color("&6✦ &e" + name + " &6dealt the most damage in the Last Raid and rises as the &l&eHonored One&6!"));
    }

    // ---- waves ----

    private static List<EntityType> composition(int wave) {
        List<EntityType> list = new ArrayList<>();
        int[][] table = {
            {8, 3, 2, 2, 0, 0}, {10, 4, 2, 3, 3, 2}, {10, 5, 3, 4, 4, 3}, {12, 6, 3, 5, 5, 3},
            {12, 7, 4, 6, 5, 4}, {14, 8, 4, 7, 6, 4}, {14, 10, 5, 8, 7, 5}, {16, 11, 6, 9, 8, 5}};
        EntityType[] types = {EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.RAVAGER, EntityType.WITCH, EntityType.ZOMBIE, EntityType.ZOMBIFIED_PIGLIN};
        int[] row = table[Math.max(1, Math.min(WAVES, wave)) - 1];
        for (int i = 0; i < types.length; i++) add(list, types[i], row[i]);
        return list;
    }

    private static void add(List<EntityType> list, EntityType type, int base) {
        double scale = 1.0;
        org.bukkit.plugin.Plugin p = Bukkit.getPluginManager().getPlugin("BlissGems");
        if (p != null) scale = Math.max(0.05, Math.min(3.0, p.getConfig().getDouble("villager-event.raid-mob-scale", 1.0)));
        int count = (int) Math.round(base * 2 * scale);
        if (base > 0) count = Math.max(1, count);
        for (int i = 0; i < count; i++) list.add(type);
    }

    private void spawnOne(EntityType type) {
        World w = this.center.getWorld();
        if (w == null) {
            return;
        }
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double a = r.nextDouble() * Math.PI * 2.0;
        double d = Math.sqrt(64.0 + r.nextDouble() * 1536.0); // 8..40 blocks, even spread by area
        double x = this.center.getX() + Math.cos(a) * d;
        double z = this.center.getZ() + Math.sin(a) * d;
        Location at = new Location(w, x + 0.5, w.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1, z + 0.5);
        Entity e = w.spawnEntity(at, type);
        if (!e.isValid()) {
            return;
        }
        if (e instanceof Raider raider) raider.setCanJoinRaid(false);
        if (e instanceof LivingEntity le) {
            le.setRemoveWhenFarAway(false);
            buffRaider(le, this.wave);
        }
        if (e instanceof PigZombie pz) pz.setAngry(true);
        this.assignBehaviour(e);
        if (e instanceof Zombie zombie && !(e instanceof PigZombie)) {
            EntityEquipment eq = zombie.getEquipment();
            if (eq != null) {
                eq.setItemInMainHand(new ItemStack(e.getScoreboardTags().contains(TAG_BREAK) ? Material.DIAMOND_PICKAXE : Material.IRON_AXE));
                eq.setItemInMainHandDropChance(0.0f);
            }
        }
        Player near = this.nearestPlayer();
        if (e instanceof Mob mob && near != null) mob.setTarget(near);
        this.alive.add(e.getUniqueId());
        e.addScoreboardTag(RAID_TAG);
        this.arrivalStrike(w, at);
    }

    @SuppressWarnings("deprecation")
    private static void buffRaider(LivingEntity e, int wave) {
        AttributeInstance hp = e.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (hp != null) {
            hp.setBaseValue(hp.getBaseValue() * (1.3 + 0.06 * (wave - 1)));
            e.setHealth(hp.getValue());
        }
        e.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1000000, 0, true, false, false));
        if (wave >= 3) e.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 1000000, 0, true, false, false));
    }

    private void assignBehaviour(Entity e) {
        if (!(e instanceof Zombie)) {
            return;
        }
        List<String> options = new ArrayList<>();
        if (this.wave >= 2) {
            options.add(TAG_BUILD);
            options.add(TAG_BREAK);
        }
        if (this.wave >= 4) options.add(TAG_WEB);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        if (!options.isEmpty() && r.nextDouble() < Math.min(0.75, 0.35 + 0.07 * (this.wave - 2))) {
            e.addScoreboardTag(options.get(r.nextInt(options.size())));
        }
    }

    private void witchesDrink() {
        for (UUID id : this.alive) {
            if (Bukkit.getEntity(id) instanceof Witch witch && witch.getFireTicks() > 0 && !witch.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
                witch.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 400, 0, true, true, true));
                witch.getWorld().playSound(witch.getLocation(), Sound.ENTITY_WITCH_DRINK, 1.0f, 1.0f);
            }
        }
    }

    private void arrivalStrike(World w, Location at) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int ring = 0; ring < 3; ring++) {
            double radius = 1.7 - ring * 0.4;
            for (int i = 0; i < 18; i++) {
                double a = r.nextDouble() * Math.PI * 2.0 + Math.PI * 2 * i / 18.0;
                w.spawnParticle(Particle.END_ROD, at.getX() + Math.cos(a) * radius, at.getY() + 0.1, at.getZ() + Math.sin(a) * radius, 0, 0, 1, 0, 0.45, null, true);
            }
        }
        for (int i = 0; i < 2; i++) DrawnBolt.draw(this.plugin, w, DrawnBolt.path(w, at, 2.5, 12.0, r), 7);
        w.playSound(at, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.2f, 1.0f);
    }

    // ---- raider AI ----

    private void raiderBehaviour() {
        if (this.finished) {
            return;
        }
        long now = Bukkit.getCurrentTick();
        this.edits.removeIf(edit -> {
            if (now < edit.until) return false;
            undo(edit);
            return true;
        });
        for (UUID id : new ArrayList<>(this.alive)) {
            if (!(Bukkit.getEntity(id) instanceof Mob mob) || mob.isDead() || !mob.isValid()) continue;
            this.retarget(mob);
            Set<String> tags = mob.getScoreboardTags();
            if (tags.contains(TAG_WEB)) this.webPlayer(mob);
            else if (tags.contains(TAG_BUILD)) this.buildToward(mob);
            else if (tags.contains(TAG_BREAK)) this.digToward(mob);
        }
    }

    private void retarget(Mob mob) {
        LivingEntity target = this.nearestVillager(mob);
        if (target == null) target = this.nearestPlayerTo(mob);
        if (target == null) {
            return;
        }
        LivingEntity cur = mob.getTarget();
        if (cur == target && cur.isValid() && !cur.isDead()) {
            return;
        }
        mob.setTarget(target);
    }

    private LivingEntity nearestVillager(Mob mob) {
        Villager best = null;
        double bestD = Double.MAX_VALUE;
        for (int id = 1; id <= 3; id++) {
            Villager v = this.state.getVillager(id);
            if (v == null || v.isDead() || !v.isValid() || !v.getWorld().equals(mob.getWorld())) continue;
            double d = v.getLocation().distanceSquared(mob.getLocation());
            if (d < bestD) {
                bestD = d;
                best = v;
            }
        }
        return best;
    }

    private Player nearestPlayerTo(Mob mob) {
        Player best = null;
        double bestD = Double.MAX_VALUE;
        for (Player p : mob.getWorld().getPlayers()) {
            if (p.isDead() || p.getGameMode() == GameMode.SPECTATOR) continue;
            double d = p.getLocation().distanceSquared(mob.getLocation());
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    private Player nearestPlayer() {
        Player best = null;
        double bestD = Double.MAX_VALUE;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(this.center.getWorld())) continue;
            double d = p.getLocation().distanceSquared(this.center);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    private void webPlayer(Mob mob) {
        Player best = null;
        double bestD = 144.0;
        for (Player p : mob.getWorld().getPlayers()) {
            double d = p.getLocation().distanceSquared(mob.getLocation());
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        if (best == null) {
            return;
        }
        this.edit(best.getLocation().getBlock(), Material.COBWEB);
        mob.getWorld().playSound(best.getLocation(), Sound.BLOCK_WOOL_PLACE, 1.2f, 0.8f);
    }

    private void digToward(Mob mob) {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        Vector dir = target.getLocation().toVector().subtract(mob.getLocation().toVector()).setY(0);
        if (dir.lengthSquared() < 1.0E-6) {
            return;
        }
        Location ahead = mob.getLocation().add(dir.normalize());
        Block head = ahead.clone().add(0, 1, 0).getBlock();
        Block feet = ahead.getBlock();
        Block hit = this.breakable(head) ? head : this.breakable(feet) ? feet : null;
        if (hit == null) {
            return;
        }
        mob.getWorld().playEffect(hit.getLocation(), Effect.STEP_SOUND, hit.getType());
        mob.getWorld().playSound(hit.getLocation(), Sound.BLOCK_STONE_BREAK, 1.4f, 0.8f);
        mob.swingMainHand();
        this.edit(hit, Material.AIR);
    }

    private void buildToward(Mob mob) {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        Vector dir = target.getLocation().toVector().subtract(mob.getLocation().toVector()).setY(0);
        if (dir.lengthSquared() < 1.0E-6) {
            return;
        }
        Location ahead = mob.getLocation().add(dir.normalize());
        Block below = ahead.clone().add(0, -1, 0).getBlock();
        Block place;
        if (below.getType().isAir() && ahead.getBlock().getType().isAir()) {
            place = below; // bridge a gap
        } else if (target.getLocation().getY() > mob.getLocation().getY() + 1.5 && mob.getLocation().getBlock().getType().isAir()) {
            place = mob.getLocation().getBlock(); // pillar up
        } else {
            return;
        }
        mob.swingMainHand();
        mob.getWorld().playSound(place.getLocation(), Sound.BLOCK_STONE_PLACE, 1.4f, 0.9f);
        this.edit(place, Material.COBBLESTONE);
    }

    private void edit(Block block, Material to) {
        if (block.getType() == to || this.protectedBlock(block)) {
            return;
        }
        Location loc = block.getLocation();
        for (Edit e : this.edits) {
            if (e.loc.equals(loc)) {
                e.placed = to;
                block.setType(to, false);
                return;
            }
        }
        this.edits.add(new Edit(loc, block.getBlockData(), to, Bukkit.getCurrentTick() + 600L));
        block.setType(to, false);
    }

    private static void undo(Edit e) {
        Block b = e.loc.getBlock();
        if (b.getType() == e.placed) b.setBlockData(e.was, false);
    }

    private boolean breakable(Block b) {
        return !b.getType().isAir() && b.getType().isSolid() && !this.protectedBlock(b);
    }

    private boolean protectedBlock(Block b) {
        Material m = b.getType();
        if (m == Material.BEDROCK || m == Material.OBSIDIAN || m == Material.BEACON || m == Material.END_PORTAL_FRAME) return true;
        if (b.getState() instanceof InventoryHolder) return true;
        if (this.manager.pedestalProtects(b.getLocation())) return true;
        return this.center.getWorld() != null && b.getWorld().equals(this.center.getWorld()) && b.getLocation().distanceSquared(this.center) < 16.0;
    }

    private void restoreEdits() {
        for (Edit e : this.edits) undo(e);
        this.edits.clear();
    }

    // ---- events ----

    @EventHandler(ignoreCancelled = true)
    public void onRaiderDamaged(EntityDamageByEntityEvent event) {
        if (this.finished || !event.getEntity().getScoreboardTags().contains(RAID_TAG)) {
            return;
        }
        Player dealer = event.getDamager() instanceof Player p ? p
            : event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p2 ? p2 : null;
        if (dealer != null) this.state.addRaidDamage(dealer.getUniqueId(), event.getFinalDamage());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onRaiderFriendlyFire(EntityDamageByEntityEvent event) {
        if (this.finished || !isRaider(event.getEntity())) {
            return;
        }
        Entity source = event.getDamager();
        if (source instanceof Projectile proj) {
            ProjectileSource shooter = proj.getShooter();
            if (shooter instanceof Entity se) source = se;
        }
        if (isRaider(source)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onRaiderTarget(EntityTargetEvent event) {
        if (!this.finished && isRaider(event.getEntity()) && isRaider(event.getTarget())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onRaiderTransform(EntityTransformEvent event) {
        if (!this.finished && isRaider(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler
    public void onRaiderRemoved(EntityRemoveEvent event) {
        if (!this.finished && event.getCause() != EntityRemoveEvent.Cause.UNLOAD) this.alive.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onWeatherLightning(LightningStrikeEvent event) {
        if (!this.finished && event.getCause() == LightningStrikeEvent.Cause.WEATHER && this.center.getWorld() != null && event.getWorld().equals(this.center.getWorld())) {
            event.setCancelled(true);
        }
    }

    private static boolean isRaider(Entity e) {
        return e != null && e.getScoreboardTags().contains(RAID_TAG);
    }

    // ---- atmosphere ----

    private void ambientBolt() {
        World w = this.center.getWorld();
        if (this.finished || w == null) {
            return;
        }
        ThreadLocalRandom r = ThreadLocalRandom.current();
        DrawnBolt.draw(this.plugin, w, DrawnBolt.path(w, this.center, 20.0, 22.0, r), 7);
        if (r.nextDouble() < 0.3) w.playSound(this.center, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.2f, 1.0f);
    }

    private void stormOn() {
        World w = this.center.getWorld();
        if (w == null) {
            return;
        }
        this.prevStorm = w.hasStorm();
        this.prevThunder = w.isThundering();
        this.prevWeather = w.getWeatherDuration();
        w.setStorm(true);
        w.setThundering(true);
        w.setWeatherDuration(24000);
        w.setThunderDuration(24000);
        this.stormHeld = true;
    }

    private void stormOff() {
        if (!this.stormHeld) {
            return;
        }
        this.stormHeld = false;
        World w = this.center.getWorld();
        if (w != null) {
            w.setStorm(this.prevStorm);
            w.setThundering(this.prevThunder);
            w.setWeatherDuration(Math.max(0, this.prevWeather));
        }
    }

    private double barProgress() {
        if (this.betweenWaves && this.refillStart >= 0) {
            return Math.max(0.0, Math.min(1.0, (Bukkit.getCurrentTick() - this.refillStart) / 100.0));
        }
        if (this.waveSize <= 0) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, (this.alive.size() + Math.max(0, this.pendingSpawns)) / (double) this.waveSize));
    }

    private void bellToll() {
        for (int i = 0; i < 4; i++) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.playAll(Sound.BLOCK_BELL_USE, 0.5f), i * 12L);
        }
    }

    private void announce(String msg) {
        Bukkit.broadcastMessage(PedestalManager.color(msg));
    }

    private void playAll(Sound sound, float pitch) {
        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p.getLocation(), sound, 1.0f, pitch);
    }

    private static final class Edit {
        final Location loc;
        final BlockData was;
        final long until;
        Material placed;

        Edit(Location loc, BlockData was, Material placed, long until) {
            this.loc = loc;
            this.was = was;
            this.placed = placed;
            this.until = until;
        }
    }
}
