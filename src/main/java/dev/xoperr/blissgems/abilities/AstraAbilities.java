/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.entity.Display$Billboard
 *  org.bukkit.entity.Display$Brightness
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.ItemDisplay
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scheduler.BukkitTask
 *  org.bukkit.util.RayTraceResult
 *  org.bukkit.util.Transformation
 *  org.bukkit.util.Vector
 *  org.joml.Matrix3f
 *  org.joml.Quaternionf
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.ParticleUtils;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class AstraAbilities
implements GemAbilityHandler {
    private final BlissGems plugin;
    private final Map<UUID, Long> dashFallImmuneUntil = new HashMap<UUID, Long>();
    private final Set<UUID> projectingPlayers = new HashSet<UUID>();
    private final Map<UUID, BukkitTask> projectionTasks = new HashMap<UUID, BukkitTask>();
    private final Map<UUID, Location> projectionOrigins = new HashMap<UUID, Location>();
    private final Map<UUID, GameMode> projectionPreviousGameModes = new HashMap<UUID, GameMode>();
    private final Set<UUID> voidActivePlayers = new HashSet<UUID>();
    private final Map<UUID, BukkitTask> voidTasks = new HashMap<UUID, BukkitTask>();
    private final Map<UUID, Integer> consecutiveDaggerHits = new HashMap<UUID, Integer>();
    private final Map<UUID, DaggerVolley> daggerVolleys = new HashMap<UUID, DaggerVolley>();
    private static final int DAGGER_MODEL_CMD = 12350;

    public AstraAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    public void onRightClick(Player player, int tier) {
        if (tier == 2 && player.isSneaking()) {
            this.astralProjection(player);
        } else {
            this.astralDaggers(player);
        }
    }

    @Override
    public void onPrimary(Player player, int tier) {
        this.astralDaggers(player);
    }

    @Override
    public void onSecondary(Player player, int tier) {
        this.astralProjection(player);
    }

    @Override
    public void onTertiary(Player player, int tier) {
        this.activateDimensionalDrift(player);
    }

    @Override
    public void onQuaternary(Player player, int tier) {
        this.activateDimensionalVoid(player);
    }

    public void activateDimensionalDrift(Player player) {
        if (!this.requireTier2(player)) {
            return;
        }
        this.dimensionalDrift(player);
    }

    public void activateDimensionalVoid(Player player) {
        if (!this.requireTier2(player)) {
            return;
        }
        this.dimensionalVoid(player);
    }

    private boolean requireTier2(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return false;
        }
        return true;
    }

    private void announceActivation(Player player, String abilityName) {
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", abilityName);
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    public boolean isProjecting(Player player) {
        return this.projectingPlayers.contains(player.getUniqueId());
    }

    public Location getProjectionOrigin(Player player) {
        return this.projectionOrigins.get(player.getUniqueId());
    }

    public boolean hasDashFallImmunity(Player player) {
        Long until = this.dashFallImmuneUntil.get(player.getUniqueId());
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until) {
            this.dashFallImmuneUntil.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public boolean isVoidActive(Player player) {
        return this.voidActivePlayers.contains(player.getUniqueId());
    }

    public void astralDaggers(final Player player) {
        final String abilityKey = "astra-daggers";
        final UUID id = player.getUniqueId();
        DaggerVolley volley = this.daggerVolleys.get(id);
        if (volley != null) {
            this.launchNextDagger(player, volley);
            return;
        }
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        volley = new DaggerVolley();
        this.daggerVolleys.put(id, volley);
        for (int i = 0; i < 5; ++i) {
            ItemDisplay dg = this.spawnDaggerDisplay(player);
            volley.slot.put(dg, i);
            volley.pending.addLast(dg);
        }
        this.positionHoveringDaggers(player, volley);
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getEyeLocation(), 30, 0.5, 0.5, 0.5, 0.03);
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 1.4f);
        final int hoverTimeoutTicks = this.plugin.getConfig().getInt("abilities.astra-daggers.hover-timeout-ticks", 600);
        final DaggerVolley tracked = volley;
        tracked.task = new BukkitRunnable(){

            public void run() {
                DaggerVolley v = AstraAbilities.this.daggerVolleys.get(id);
                if (v == null || v != tracked) {
                    this.cancel();
                    return;
                }
                if (!player.isOnline() || player.isDead()) {
                    AstraAbilities.this.discardVolley(player, v);
                    this.cancel();
                    return;
                }
                AstraAbilities.this.positionHoveringDaggers(player, v);
                if (++v.idleTicks >= hoverTimeoutTicks) {
                    AstraAbilities.this.discardVolley(player, v);
                    AstraAbilities.this.daggerVolleys.remove(id);
                    AstraAbilities.this.plugin.getAbilityManager().useAbility(player, abilityKey);
                    this.cancel();
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 1L, 1L);
        this.announceActivation(player, "Astral Daggers");
    }

    private ItemStack buildDaggerItem() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(Integer.valueOf(this.plugin.getConfig().getInt("abilities.astra-daggers.model-data", 12350)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemDisplay spawnDaggerDisplay(Player player) {
        Location at = player.getEyeLocation();
        at.setYaw(0.0f);
        at.setPitch(0.0f);
        return (ItemDisplay)player.getWorld().spawn(at, ItemDisplay.class, d -> {
            d.setItemStack(this.buildDaggerItem());
            d.setBillboard(Display.Billboard.FIXED);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setInterpolationDuration(3);
            d.setTeleportDuration(3);
            d.setPersistent(false);
        });
    }

    private void positionHoveringDaggers(Player player, DaggerVolley volley) {
        Location eye = player.getEyeLocation();
        World world = eye.getWorld();
        Vector look = eye.getDirection().normalize();
        Vector right = look.clone().crossProduct(new Vector(0, 1, 0));
        if (right.lengthSquared() < 1.0E-6) {
            right = new Vector(1, 0, 0);
        }
        right.normalize();
        double d = this.plugin.getConfig().getDouble("abilities.astra-daggers.hover-distance", 1.6);
        Vector fwd = look.clone().multiply(d);
        Vector rgt = right.clone().multiply(d);
        Vector down = new Vector(0.0, -0.2, 0.0);
        Vector face = look.clone().rotateAroundY(Math.PI);
        for (ItemDisplay dagger : volley.pending) {
            if (dagger == null || dagger.isDead()) continue;
            int slot = volley.slot.getOrDefault(dagger, 0);
            Vector off = switch (slot) {
                case 1 -> rgt.clone().multiply(-1);
                case 2 -> rgt.clone();
                case 3 -> fwd.clone().subtract(rgt);
                case 4 -> fwd.clone().add(rgt);
                default -> fwd.clone();
            };
            off.add(down);
            Location hover = new Location(world, eye.getX() + off.getX(), eye.getY() + off.getY(), eye.getZ() + off.getZ(), 0.0f, 0.0f);
            this.orientDagger(dagger, face);
            dagger.teleport(hover);
        }
    }

    private void launchNextDagger(Player player, DaggerVolley volley) {
        ItemDisplay dagger = volley.pending.pollFirst();
        volley.idleTicks = 0;
        if (dagger != null) {
            volley.slot.remove(dagger);
            if (!dagger.isDead()) {
                this.flyDagger(player, dagger);
            }
        }
        if (volley.pending.isEmpty()) {
            if (volley.task != null) {
                volley.task.cancel();
            }
            this.daggerVolleys.remove(player.getUniqueId());
            this.plugin.getAbilityManager().useAbility(player, "astra-daggers");
        }
    }

    private void flyDagger(final Player player, final ItemDisplay dagger) {
        final Vector dir = player.getEyeLocation().getDirection().normalize();
        final double speed = this.plugin.getConfig().getDouble("abilities.astra-daggers.speed", 1.1);
        final double range = this.plugin.getConfig().getInt("abilities.astra-daggers.range", 30);
        final double damage = this.plugin.getConfigManager().getAbilityDamage("astra-daggers");
        final Vector step = dir.clone().multiply(speed);
        dagger.setTeleportDuration(0);
        this.orientDagger(dagger, dir.clone().rotateAroundY(Math.PI));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.6f);
        new BukkitRunnable(){
            final Location loc;
            final Particle.DustOptions trailDust;
            double travelled;
            {
                this.loc = AstraAbilities.zeroRot(player.getEyeLocation().add(dir.clone().multiply(1.2)));
                this.trailDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 0.9f);
                this.travelled = 0.0;
            }

            public void run() {
                if (dagger.isDead() || !dagger.isValid()) {
                    this.cancel();
                    return;
                }
                if (this.travelled >= range) {
                    this.impact(this.loc, false);
                    dagger.remove();
                    this.cancel();
                    return;
                }
                this.loc.add(step);
                this.travelled += speed;
                if (this.loc.getBlock().getType().isSolid()) {
                    this.impact(this.loc, false);
                    dagger.remove();
                    this.cancel();
                    return;
                }
                for (Entity entity : this.loc.getWorld().getNearbyEntities(this.loc, 0.6, 0.6, 0.6)) {
                    Player tp;
                    if (!(entity instanceof LivingEntity)) continue;
                    LivingEntity target = (LivingEntity)entity;
                    if (entity == player || entity instanceof Player && (!player.canSee(tp = (Player)entity) || AstraAbilities.this.plugin.getTrustedPlayersManager().isTrusted(player, tp))) continue;
                    double armorPiercing = AstraAbilities.this.plugin.getConfig().getDouble("abilities.astra-daggers.armor-piercing", 0.0);
                    if (armorPiercing > 0.0 && target instanceof Player) {
                        double reducedDamage = damage * (1.0 - armorPiercing);
                        double pierceDamage = damage * armorPiercing;
                        target.damage(reducedDamage, (Entity)player);
                        ((Player)target).setHealth(Math.max(0.0, ((Player)target).getHealth() - pierceDamage));
                    } else {
                        target.damage(damage, (Entity)player);
                    }
                    int glowingTicks = AstraAbilities.this.plugin.getConfig().getInt("abilities.astra-daggers.glowing-duration-ticks", 60);
                    if (glowingTicks > 0) {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowingTicks, 0, false, false));
                    }
                    AstraAbilities.this.onDaggerHit(player, target);
                    this.impact(target.getLocation().add(0.0, 1.0, 0.0), true);
                    dagger.remove();
                    this.cancel();
                    return;
                }
                dagger.teleport(this.loc);
                World world = this.loc.getWorld();
                world.spawnParticle(Particle.DUST, this.loc, 3, 0.05, 0.05, 0.05, 0.0, (Object)this.trailDust, true);
                world.spawnParticle(Particle.REVERSE_PORTAL, this.loc, 2, 0.03, 0.03, 0.03, 0.01);
            }

            private void impact(Location at, boolean hit) {
                Particle.DustOptions purple = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.3f);
                at.getWorld().spawnParticle(Particle.DUST, at, hit ? 60 : 20, 0.5, 0.5, 0.5, 0.0, (Object)purple, true);
                at.getWorld().spawnParticle(Particle.REVERSE_PORTAL, at, hit ? 40 : 15, 0.4, 0.4, 0.4, 0.02);
                if (hit) {
                    at.getWorld().spawnParticle(Particle.ENCHANTED_HIT, at, 30, 0.5, 0.5, 0.5);
                    at.getWorld().spawnParticle(Particle.WITCH, at, 20, 0.4, 0.4, 0.4);
                } else {
                    AstraAbilities.this.consecutiveDaggerHits.put(player.getUniqueId(), 0);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    private void onDaggerHit(Player player, LivingEntity target) {
        if (target instanceof Player && this.plugin.getAchievementManager() != null) {
            UUID uid = player.getUniqueId();
            int hits = this.consecutiveDaggerHits.getOrDefault(uid, 0) + 1;
            this.consecutiveDaggerHits.put(uid, hits);
            this.plugin.getAchievementManager().setProgress(player, Achievement.PIERCING_PRECISION, hits);
        }
    }

    private static Location zeroRot(Location l) {
        l.setYaw(0.0f);
        l.setPitch(0.0f);
        return l;
    }

    private void orientDagger(ItemDisplay dagger, Vector dir) {
        Vector3f yAxis;
        Vector3f xAxis;
        float scale = (float)this.plugin.getConfig().getDouble("abilities.astra-daggers.model-scale", 1.4);
        double yawOffset = Math.toRadians(this.plugin.getConfig().getDouble("abilities.astra-daggers.model-yaw-offset", 180.0));
        Vector d = dir.clone();
        if (yawOffset != 0.0) {
            d.rotateAroundY(yawOffset);
        }
        if (d.lengthSquared() < 1.0E-6) {
            d = new Vector(0, 0, 1);
        }
        if ((xAxis = new Vector3f(0.0f, 1.0f, 0.0f).cross((Vector3fc)(yAxis = new Vector3f((float)d.getX(), (float)d.getY(), (float)d.getZ()).normalize()))).lengthSquared() < 1.0E-6f) {
            xAxis.set(1.0f, 0.0f, 0.0f);
        } else {
            xAxis.normalize();
        }
        Vector3f zAxis = new Vector3f((Vector3fc)xAxis).cross((Vector3fc)yAxis).normalize();
        Quaternionf rot = new Matrix3f((Vector3fc)xAxis, (Vector3fc)yAxis, (Vector3fc)zAxis).getNormalizedRotation(new Quaternionf());
        dagger.setInterpolationDelay(0);
        dagger.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), rot, new Vector3f(scale, scale, scale), new Quaternionf()));
    }

    private void discardVolley(Player player, DaggerVolley volley) {
        for (ItemDisplay dagger : volley.pending) {
            if (dagger == null || dagger.isDead()) continue;
            dagger.remove();
        }
        volley.pending.clear();
        if (volley.task != null) {
            volley.task.cancel();
        }
        this.daggerVolleys.remove(player.getUniqueId());
    }

    public void astralProjection(final Player player) {
        String abilityKey = "astra-projection";
        if (this.isProjecting(player)) {
            this.endProjection(player);
            return;
        }
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        final Location origin = player.getLocation().clone();
        int durationSeconds = this.plugin.getConfig().getInt("abilities.durations.astra-projection", 10);
        final int duration = durationSeconds * 20;
        final double maxRadius = this.plugin.getConfig().getDouble("abilities.astra-projection.radius", 128.0);
        this.projectionOrigins.put(uuid, origin);
        this.projectionPreviousGameModes.put(uuid, player.getGameMode());
        this.projectingPlayers.add(uuid);
        Particle.DustOptions purpleDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.0f);
        origin.getWorld().spawnParticle(Particle.DUST, origin.clone().add(0.0, 1.0, 0.0), 60, 0.4, 1.0, 0.4, 0.0, (Object)purpleDust, true);
        origin.getWorld().spawnParticle(Particle.REVERSE_PORTAL, origin.clone().add(0.0, 1.0, 0.0), 40, 0.4, 1.0, 0.4);
        origin.getWorld().spawnParticle(Particle.SOUL, origin.clone().add(0.0, 1.0, 0.0), 20, 0.3, 0.5, 0.3, 0.02);
        player.playSound(origin, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.5f);
        player.playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.3f);
        player.setGameMode(GameMode.SPECTATOR);
        BukkitTask projTask = new BukkitRunnable(){
            int ticksElapsed = 0;

            public void run() {
                int ticksRemaining;
                Location current;
                if (!player.isOnline() || player.isDead() || !AstraAbilities.this.isProjecting(player)) {
                    AstraAbilities.this.endProjection(player);
                    this.cancel();
                    return;
                }
                ++this.ticksElapsed;
                if (this.ticksElapsed % 4 == 0 && (!(current = player.getLocation()).getWorld().equals((Object)origin.getWorld()) || current.distance(origin) > maxRadius)) {
                    player.teleport(origin);
                    player.sendMessage("\u00a7d\u00a7oYou cannot travel that far from your body!");
                }
                if (this.ticksElapsed % 20 == 0) {
                    Particle.DustOptions trailDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 0.7f);
                    origin.getWorld().spawnParticle(Particle.DUST, origin.clone().add(0.0, 1.0, 0.0), 8, 0.3, 0.8, 0.3, 0.0, (Object)trailDust, true);
                    origin.getWorld().spawnParticle(Particle.SOUL, origin.clone().add(0.0, 1.2, 0.0), 3, 0.2, 0.3, 0.2, 0.01);
                }
                if ((ticksRemaining = duration - this.ticksElapsed) == 60) {
                    player.sendMessage("\u00a7d\u00a7o3 seconds remaining...");
                }
                if (this.ticksElapsed >= duration) {
                    AstraAbilities.this.endProjection(player);
                    this.cancel();
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        this.projectionTasks.put(uuid, projTask);
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, durationSeconds);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Astral Projection");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        player.sendMessage("\u00a7d\u00a7oYour soul leaves your body... (" + durationSeconds + "s)");
    }

    public void endProjection(Player player) {
        UUID uuid = player.getUniqueId();
        if (!this.projectingPlayers.contains(uuid)) {
            return;
        }
        this.projectingPlayers.remove(uuid);
        BukkitTask task = this.projectionTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        this.plugin.getAbilityManager().endAbilityDuration(player, "astra-projection");
        Location origin = this.projectionOrigins.remove(uuid);
        GameMode previousMode = this.projectionPreviousGameModes.remove(uuid);
        if (player.isOnline()) {
            if (origin != null) {
                player.teleport(origin);
            }
            player.setGameMode(previousMode != null ? previousMode : GameMode.SURVIVAL);
            Location loc = player.getLocation();
            Particle.DustOptions purpleDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.0f);
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0.0, 1.0, 0.0), 60, 0.4, 1.0, 0.4, 0.0, (Object)purpleDust, true);
            loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0.0, 1.0, 0.0), 40, 0.4, 1.0, 0.4);
            loc.getWorld().spawnParticle(Particle.SOUL, loc.clone().add(0.0, 1.0, 0.0), 15, 0.3, 0.5, 0.3, 0.02);
            player.playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.8f);
            player.sendMessage("\u00a7d\u00a7oYour soul returns to your body.");
        }
    }

    public void dimensionalDrift(final Player player) {
        String abilityKey = "astra-drift";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        double power = this.plugin.getConfig().getDouble("abilities.astra-drift.power", 2.8);
        double lift = this.plugin.getConfig().getDouble("abilities.astra-drift.lift", 0.35);
        Vector velocity = player.getLocation().getDirection();
        velocity.setY(0);
        if (velocity.lengthSquared() > 1.0E-6) {
            velocity.normalize();
        }
        velocity.multiply(power);
        velocity.setY(lift * power);
        player.setVelocity(velocity);
        int invisSeconds = this.plugin.getConfig().getInt("abilities.astra-drift.invisibility", 3);
        int fallImmuneSeconds = this.plugin.getConfig().getInt("abilities.astra-drift.fall-immunity", 6);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, invisSeconds * 20, 0, false, false));
        this.dashFallImmuneUntil.put(player.getUniqueId(), System.currentTimeMillis() + (long)fallImmuneSeconds * 1000L);
        Location loc = player.getLocation();
        Particle.DustOptions purpleDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.3f);
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0.0, 1.0, 0.0), 50, 0.5, 0.8, 0.5, 0.0, (Object)purpleDust, true);
        loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0.0, 1.0, 0.0), 45, 0.4, 0.6, 0.4, 0.05);
        loc.getWorld().spawnParticle(Particle.WITCH, loc, 15, 0.4, 0.4, 0.4, 0.0);
        player.playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.4f);
        player.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.6f);
        new BukkitRunnable(){
            int ticks = 0;

            public void run() {
                if (this.ticks++ >= 12 || !player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }
                Location p = player.getLocation().add(0.0, 0.4, 0.0);
                Particle.DustOptions trail = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 0.9f);
                p.getWorld().spawnParticle(Particle.DUST, p, 4, 0.2, 0.2, 0.2, 0.0, (Object)trail, true);
                p.getWorld().spawnParticle(Particle.END_ROD, p, 2, 0.1, 0.1, 0.1, 0.01);
            }
        }.runTaskTimer((Plugin)this.plugin, 1L, 1L);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Dimensional Drift");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        player.sendMessage("\u00a7d\u00a7oYou blink through the dimensional rift!");
    }

    public void dimensionalVoid(final Player player) {
        String abilityKey = "astra-void";
        if (this.isVoidActive(player)) {
            player.sendMessage("\u00a7c\u00a7oDimensional Void is already active!");
            return;
        }
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        Location center = player.getLocation().clone();
        final double radius = this.plugin.getConfig().getDouble("abilities.astra-void.radius", 10.0);
        int durationSeconds = this.plugin.getConfig().getInt("abilities.durations.astra-void", 8);
        final int duration = durationSeconds * 20;
        this.voidActivePlayers.add(uuid);
        ParticleUtils.drawDome(center, ParticleUtils.ASTRA_PURPLE, 1.2f, radius);
        this.drawVoidSphere(player.getLocation(), Math.min(radius, 2.8), 0.0);
        player.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.5f);
        player.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof Player) || entity == player) continue;
            Player target = (Player)entity;
            if (this.plugin.getTrustedPlayersManager().isTrusted(player, target)) continue;
            target.sendMessage("\u00a74\u00a7l\u00a7oYour gem abilities have been nullified!");
            target.playSound(target.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.3f);
        }
        BukkitTask voidTask = new BukkitRunnable(){
            int ticksElapsed = 0;

            public void run() {
                Location playerLoc;
                if (!player.isOnline() || player.isDead() || this.ticksElapsed >= duration) {
                    AstraAbilities.this.endVoid(player);
                    this.cancel();
                    return;
                }
                ++this.ticksElapsed;
                if (this.ticksElapsed % 20 == 0) {
                    playerLoc = player.getLocation();
                    for (Entity entity : playerLoc.getWorld().getNearbyEntities(playerLoc, radius, radius, radius)) {
                        if (!(entity instanceof Player) || entity == player) continue;
                        Player target = (Player)entity;
                        if (AstraAbilities.this.plugin.getTrustedPlayersManager().isTrusted(player, target)) continue;
                        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 30, 1, false, true));
                        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 30, 0, false, true));
                    }
                }
                if (this.ticksElapsed % 4 == 0) {
                    AstraAbilities.this.drawVoidSphere(player.getLocation(), Math.min(radius, 2.8), (double)this.ticksElapsed * 0.12);
                }
                if (this.ticksElapsed % 10 == 0) {
                    playerLoc = player.getLocation();
                    Particle.DustOptions voidDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.0f);
                    for (int i = 0; i < 32; ++i) {
                        double angle = (double)i / 32.0 * 2.0 * Math.PI;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        playerLoc.getWorld().spawnParticle(Particle.DUST, playerLoc.clone().add(x, 0.3, z), 2, 0.1, 0.1, 0.1, 0.0, (Object)voidDust, true);
                    }
                }
                if (this.ticksElapsed % 40 == 0) {
                    player.getLocation().getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.3f);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        this.voidTasks.put(uuid, voidTask);
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, durationSeconds);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Dimensional Void");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        player.sendMessage("\u00a7d\u00a7oDimensional Void active for " + durationSeconds + "s. Enemy abilities nullified in " + (int)radius + " block radius.");
    }

    private void drawVoidSphere(Location center, double sphereRadius, double phase) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        Particle.DustOptions purple = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.1f);
        Location mid = center.clone().add(0.0, sphereRadius * 0.9, 0.0);
        int rings = 10;
        for (int r = 1; r < rings; ++r) {
            double theta = Math.PI * (double)r / (double)rings;
            double ringRadius = sphereRadius * Math.sin(theta);
            double y = sphereRadius * Math.cos(theta);
            int pts = Math.max(10, (int)(ringRadius * 13.0));
            for (int i = 0; i < pts; ++i) {
                double a = (double)i / (double)pts * 2.0 * Math.PI;
                double x = Math.cos(a) * ringRadius;
                double z = Math.sin(a) * ringRadius;
                world.spawnParticle(Particle.DUST, mid.clone().add(x, y, z), 1, 0.0, 0.0, 0.0, 0.0, (Object)purple, true);
            }
        }
        int swirls = 3;
        int spiralPts = 64;
        double turns = 2.0;
        for (int s = 0; s < swirls; ++s) {
            double startAngle = phase + (double)s * (Math.PI * 2 / (double)swirls);
            for (int i = 0; i < spiralPts; ++i) {
                double t = (double)i / (double)(spiralPts - 1);
                double y = (t - 0.5) * 2.0 * sphereRadius * 0.95;
                double rr = Math.sqrt(Math.max(0.0, sphereRadius * sphereRadius - y * y));
                double a = t * turns * 2.0 * Math.PI + startAngle;
                double x = Math.cos(a) * rr;
                double z = Math.sin(a) * rr;
                world.spawnParticle(Particle.END_ROD, mid.clone().add(x, y, z), 0, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private void endVoid(Player player) {
        UUID uuid = player.getUniqueId();
        if (!this.voidActivePlayers.contains(uuid)) {
            return;
        }
        this.voidActivePlayers.remove(uuid);
        BukkitTask task = this.voidTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        this.plugin.getAbilityManager().endAbilityDuration(player, "astra-void");
        if (player.isOnline()) {
            player.sendMessage("\u00a7d\u00a7oDimensional Void faded.");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.5f);
        }
    }

    public boolean isAbilitySuppressed(Player target) {
        for (UUID voidUserId : this.voidActivePlayers) {
            Player voidUser = Bukkit.getPlayer((UUID)voidUserId);
            if (voidUser == null || !voidUser.isOnline() || !voidUser.getWorld().equals((Object)target.getWorld())) continue;
            double radius = this.plugin.getConfig().getDouble("abilities.astra-void.radius", 10.0);
            if (!(voidUser.getLocation().distance(target.getLocation()) <= radius) || this.plugin.getTrustedPlayersManager().isTrusted(voidUser, target)) continue;
            return true;
        }
        return false;
    }

    private LivingEntity getTargetEntity(Player player, int range) {
        RayTraceResult result = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), (double)range, entity -> entity instanceof LivingEntity && entity != player);
        return result != null ? (LivingEntity)result.getHitEntity() : null;
    }

    private String getDirectionArrow(Location from, Location to) {
        double relative;
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        double angle = Math.atan2(direction.getZ(), direction.getX());
        double yaw = Math.toRadians(from.getYaw());
        for (relative = angle - yaw; relative > Math.PI; relative -= Math.PI * 2) {
        }
        while (relative < -Math.PI) {
            relative += Math.PI * 2;
        }
        if (relative > -0.7853981633974483 && relative <= 0.7853981633974483) {
            return "\u2192";
        }
        if (relative > 0.7853981633974483 && relative <= 2.356194490192345) {
            return "\u2193";
        }
        if (relative > -2.356194490192345 && relative <= -0.7853981633974483) {
            return "\u2191";
        }
        return "\u2190";
    }

    @Override
    public void cleanup(Player player) {
        if (this.isProjecting(player)) {
            this.endProjection(player);
        }
        if (this.isVoidActive(player)) {
            this.endVoid(player);
        }
        this.dashFallImmuneUntil.remove(player.getUniqueId());
        DaggerVolley volley = this.daggerVolleys.get(player.getUniqueId());
        if (volley != null) {
            this.discardVolley(player, volley);
        }
    }

    private static final class DaggerVolley {
        final Deque<ItemDisplay> pending = new ArrayDeque<ItemDisplay>();
        final Map<ItemDisplay, Integer> slot = new HashMap<ItemDisplay, Integer>();
        BukkitTask task;
        int idleTicks;

        private DaggerVolley() {
        }
    }
}

