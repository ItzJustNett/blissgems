/*
 * Astra Gem Abilities — dimensional stealth and ranged daggers
 *
 * Tier 1:
 *   - Astral Daggers (Primary): Fire 3 phantom daggers that deal damage
 *   - Soul Capture passive + Soul Absorption (temporary max hearts on kill, handled by SoulManager)
 *
 * Tier 2 (all Tier 1 abilities plus):
 *   - Astral Daggers (Primary, no shift): Same as T1
 *   - Astral Projection (Shift): Enter spectator mode to scout, bounded to 8 chunk radius, returns to origin
 *   - Dimensional Drift (Double-shift / secondary command): Invisible horse + player invisibility
 *   - Dimensional Void (Sneak + Swap / tertiary): Nullify enemy gem abilities in radius
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.ParticleUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public class AstraAbilities implements GemAbilityHandler {
    private final BlissGems plugin;

    // Dimensional Drift (Momentum Dash) state — brief fall-damage immunity window per player
    private final Map<UUID, Long> dashFallImmuneUntil = new HashMap<>();

    // Astral Projection (spectator mode) state
    private final Set<UUID> projectingPlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> projectionTasks = new HashMap<>();
    private final Map<UUID, Location> projectionOrigins = new HashMap<>();
    private final Map<UUID, GameMode> projectionPreviousGameModes = new HashMap<>();

    // Dimensional Void state
    private final Set<UUID> voidActivePlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> voidTasks = new HashMap<>();

    // Achievement: Piercing Precision — consecutive dagger hits on players
    private final Map<UUID, Integer> consecutiveDaggerHits = new HashMap<>();

    // Astral Daggers — hovering daggers waiting to be launched, per player (manual per-press)
    private final Map<UUID, DaggerVolley> daggerVolleys = new HashMap<>();
    private static final int DAGGER_MODEL_CMD = 12350; // blissgems:custom/astra_dagger on ECHO_SHARD

    public AstraAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    // ========================================================================
    // Right-click routing
    // ========================================================================

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

    /**
     * Activates Dimensional Drift (called from command /bliss ability:secondary when not sneaking,
     * or can be triggered via a keybind)
     */
    public void activateDimensionalDrift(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        dimensionalDrift(player);
    }

    /**
     * Activates Dimensional Void (called from command /bliss ability:tertiary or special keybind)
     */
    public void activateDimensionalVoid(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        dimensionalVoid(player);
    }

    // ========================================================================
    // State checkers
    // ========================================================================


    public boolean isProjecting(Player player) {
        return projectingPlayers.contains(player.getUniqueId());
    }

    public Location getProjectionOrigin(Player player) {
        return projectionOrigins.get(player.getUniqueId());
    }

    public boolean hasDashFallImmunity(Player player) {
        Long until = dashFallImmuneUntil.get(player.getUniqueId());
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until) {
            dashFallImmuneUntil.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public boolean isVoidActive(Player player) {
        return voidActivePlayers.contains(player.getUniqueId());
    }

    // ========================================================================
    // 1. ASTRAL DAGGERS — Tier 1+2 Primary
    // ========================================================================

    public void astralDaggers(Player player) {
        String abilityKey = "astra-daggers";
        UUID id = player.getUniqueId();

        // If daggers are already conjured and hovering, this press launches the next one.
        DaggerVolley volley = daggerVolleys.get(id);
        if (volley != null) {
            launchNextDagger(player, volley);
            return;
        }

        // Fresh cast — respect cooldown.
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        volley = new DaggerVolley();
        daggerVolleys.put(id, volley);

        // Conjure 3 hovering dagger models fanned out in front of the player.
        for (int i = 0; i < 3; i++) {
            volley.pending.addLast(spawnDaggerDisplay(player));
        }
        positionHoveringDaggers(player, volley);

        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getEyeLocation(), 30, 0.5, 0.5, 0.5, 0.03);
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 1.4f);

        // Hover + auto-fire loop: keeps pending daggers arrayed in front, and auto-launches
        // any left unfired within the idle window.
        final int autoFireTicks = this.plugin.getConfig().getInt("abilities.astra-daggers.auto-fire-ticks", 80);
        final DaggerVolley tracked = volley;
        tracked.task = new BukkitRunnable() {
            @Override
            public void run() {
                DaggerVolley v = daggerVolleys.get(id);
                if (v == null || v != tracked) {
                    this.cancel();
                    return;
                }
                if (!player.isOnline() || player.isDead()) {
                    discardVolley(player, v);
                    this.cancel();
                    return;
                }
                positionHoveringDaggers(player, v);
                if (++v.idleTicks >= autoFireTicks) {
                    while (!v.pending.isEmpty()) {
                        launchNextDagger(player, v);
                    }
                }
            }
        }.runTaskTimer(this.plugin, 1L, 1L);

        // Per the design, the initial cast also launches the first dagger.
        launchNextDagger(player, volley);

        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Astral Daggers");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    /** Build the dagger item (ECHO_SHARD + the astra_dagger custom model). */
    private ItemStack buildDaggerItem() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(this.plugin.getConfig().getInt("abilities.astra-daggers.model-data", DAGGER_MODEL_CMD));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemDisplay spawnDaggerDisplay(Player player) {
        return player.getWorld().spawn(player.getEyeLocation(), ItemDisplay.class, d -> {
            d.setItemStack(buildDaggerItem());
            d.setBillboard(Display.Billboard.FIXED);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setInterpolationDuration(2);
            d.setPersistent(false);
        });
    }

    /** Keep the still-pending daggers hovering in a fan in front of the player, aiming where they look. */
    private void positionHoveringDaggers(Player player, DaggerVolley volley) {
        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection().normalize();
        Vector right = look.clone().crossProduct(new Vector(0, 1, 0));
        if (right.lengthSquared() < 1.0E-6) {
            right = new Vector(1, 0, 0);
        }
        right.normalize();
        int n = volley.pending.size();
        int idx = 0;
        for (ItemDisplay dagger : volley.pending) {
            if (dagger != null && !dagger.isDead()) {
                double offset = (idx - (n - 1) / 2.0) * 0.6;
                Location hover = eye.clone()
                        .add(look.clone().multiply(1.6))
                        .add(right.clone().multiply(offset))
                        .add(0, -0.2, 0);
                Vector flatLook = look.clone();
                flatLook.setY(0);
                if (flatLook.lengthSquared() < 1.0E-6) {
                    flatLook = new Vector(0, 0, 1);
                }
                flatLook.normalize();
                orientDagger(dagger, flatLook);
                dagger.teleport(hover);
            }
            idx++;
        }
    }

    private void launchNextDagger(Player player, DaggerVolley volley) {
        ItemDisplay dagger = volley.pending.pollFirst();
        volley.idleTicks = 0;
        if (dagger != null && !dagger.isDead()) {
            flyDagger(player, dagger);
        }
        if (volley.pending.isEmpty()) {
            if (volley.task != null) {
                volley.task.cancel();
            }
            daggerVolleys.remove(player.getUniqueId());
            this.plugin.getAbilityManager().useAbility(player, "astra-daggers");
        }
    }

    /** Send a single dagger flying straight in the player's aim direction, damaging the first enemy hit. */
    private void flyDagger(Player player, ItemDisplay dagger) {
        final Vector dir = player.getEyeLocation().getDirection().normalize();
        final double speed = this.plugin.getConfig().getDouble("abilities.astra-daggers.speed", 1.1);
        final double range = this.plugin.getConfig().getInt("abilities.astra-daggers.range", 30);
        final double damage = this.plugin.getConfigManager().getAbilityDamage("astra-daggers");
        orientDagger(dagger, dir);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.6f);

        new BukkitRunnable() {
            final Location loc = player.getEyeLocation().add(dir.clone().multiply(1.2));
            double travelled = 0;

            @Override
            public void run() {
                if (dagger.isDead() || !dagger.isValid()) {
                    this.cancel();
                    return;
                }
                if (travelled >= range) {
                    impact(loc, false);
                    dagger.remove();
                    this.cancel();
                    return;
                }

                loc.add(dir.clone().multiply(speed));
                travelled += speed;

                if (loc.getBlock().getType().isSolid()) {
                    impact(loc, false);
                    dagger.remove();
                    this.cancel();
                    return;
                }

                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 0.6, 0.6, 0.6)) {
                    if (!(entity instanceof LivingEntity) || entity == player) {
                        continue;
                    }
                    LivingEntity target = (LivingEntity) entity;
                    if (entity instanceof Player) {
                        Player tp = (Player) entity;
                        if (!player.canSee(tp) || plugin.getTrustedPlayersManager().isTrusted(player, tp)) {
                            continue;
                        }
                    }
                    target.damage(damage, (Entity) player);
                    onDaggerHit(player, target);
                    impact(target.getLocation().add(0, 1, 0), true);
                    dagger.remove();
                    this.cancel();
                    return;
                }

                dagger.teleport(loc);
                Particle.DustOptions purple = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 0.9f);
                loc.getWorld().spawnParticle(Particle.DUST, loc, 3, 0.05, 0.05, 0.05, 0.0, purple, true);
                loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 2, 0.03, 0.03, 0.03, 0.01);
            }

            private void impact(Location at, boolean hit) {
                Particle.DustOptions purple = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.3f);
                at.getWorld().spawnParticle(Particle.DUST, at, hit ? 60 : 20, 0.5, 0.5, 0.5, 0.0, purple, true);
                at.getWorld().spawnParticle(Particle.REVERSE_PORTAL, at, hit ? 40 : 15, 0.4, 0.4, 0.4, 0.02);
                if (hit) {
                    at.getWorld().spawnParticle(Particle.ENCHANTED_HIT, at, 30, 0.5, 0.5, 0.5);
                    at.getWorld().spawnParticle(Particle.WITCH, at, 20, 0.4, 0.4, 0.4);
                } else {
                    // Dagger missed — reset the Piercing Precision streak.
                    consecutiveDaggerHits.put(player.getUniqueId(), 0);
                }
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
    }

    private void onDaggerHit(Player player, LivingEntity target) {
        if (target instanceof Player && this.plugin.getAchievementManager() != null) {
            UUID uid = player.getUniqueId();
            int hits = consecutiveDaggerHits.getOrDefault(uid, 0) + 1;
            consecutiveDaggerHits.put(uid, hits);
            this.plugin.getAchievementManager().setProgress(player, Achievement.PIERCING_PRECISION, hits);
        }
    }

    /** Align the dagger model's +Y axis to the given direction (best-effort; tweak model-scale in config). */
    private void orientDagger(ItemDisplay dagger, Vector dir) {
        float scale = (float) this.plugin.getConfig().getDouble("abilities.astra-daggers.model-scale", 1.4);
        // Correct for how the model is authored: rotate the facing horizontally by this
        // offset (default 180 so the blade points away from the player, not at them).
        double yawOffset = Math.toRadians(this.plugin.getConfig().getDouble("abilities.astra-daggers.model-yaw-offset", 180.0));
        Vector d = dir.clone();
        if (yawOffset != 0.0) {
            d.rotateAroundY(yawOffset);
        }
        if (d.lengthSquared() < 1.0E-6) {
            d = new Vector(0, 0, 1);
        }
        Quaternionf rot = new Quaternionf().rotationTo(
                new Vector3f(0f, 1f, 0f),
                new Vector3f((float) d.getX(), (float) d.getY(), (float) d.getZ()).normalize());
        dagger.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                rot,
                new Vector3f(scale, scale, scale),
                new Quaternionf()));
    }

    /** Remove all still-hovering daggers without launching (player left, gem swapped, etc.). */
    private void discardVolley(Player player, DaggerVolley volley) {
        for (ItemDisplay dagger : volley.pending) {
            if (dagger != null && !dagger.isDead()) {
                dagger.remove();
            }
        }
        volley.pending.clear();
        if (volley.task != null) {
            volley.task.cancel();
        }
        daggerVolleys.remove(player.getUniqueId());
    }

    /** Per-player state for a set of conjured daggers waiting to be launched. */
    private static final class DaggerVolley {
        final Deque<ItemDisplay> pending = new ArrayDeque<>();
        BukkitTask task;
        int idleTicks;
    }

    // ========================================================================
    // 2. ASTRAL PROJECTION — Tier 2 Secondary (Shift)
    //    Short 5-block blink in the facing direction. Refuses to fire if any
    //    block along the path is not passable at feet or head level.
    // ========================================================================

    public void astralProjection(Player player) {
        String abilityKey = "astra-projection";

        // Toggle off if already projecting
        if (isProjecting(player)) {
            endProjection(player);
            return;
        }

        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Location origin = player.getLocation().clone();
        int durationSeconds = this.plugin.getConfig().getInt("abilities.durations.astra-projection", 10);
        int duration = durationSeconds * 20;
        double maxRadius = this.plugin.getConfig().getDouble("abilities.astra-projection.radius", 128.0);

        // Save state
        projectionOrigins.put(uuid, origin);
        projectionPreviousGameModes.put(uuid, player.getGameMode());
        projectingPlayers.add(uuid);

        // Departure effects
        Particle.DustOptions purpleDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.0f);
        origin.getWorld().spawnParticle(Particle.DUST, origin.clone().add(0, 1, 0), 60, 0.4, 1.0, 0.4, 0.0, purpleDust, true);
        origin.getWorld().spawnParticle(Particle.REVERSE_PORTAL, origin.clone().add(0, 1, 0), 40, 0.4, 1.0, 0.4);
        origin.getWorld().spawnParticle(Particle.SOUL, origin.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.02);
        player.playSound(origin, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.5f);
        player.playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.3f);

        // Switch to spectator mode
        player.setGameMode(GameMode.SPECTATOR);

        // Monitor task — enforce radius boundary and duration
        BukkitTask projTask = new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !isProjecting(player)) {
                    endProjection(player);
                    this.cancel();
                    return;
                }

                ticksElapsed++;

                // Enforce radius boundary — pull back if too far
                if (ticksElapsed % 4 == 0) {
                    Location current = player.getLocation();
                    if (!current.getWorld().equals(origin.getWorld()) ||
                        current.distance(origin) > maxRadius) {
                        // Teleport back to the edge of the allowed radius toward origin
                        player.teleport(origin);
                        player.sendMessage("\u00a7d\u00a7oYou cannot travel that far from your body!");
                    }
                }

                // Subtle particle trail at origin (ghost body effect)
                if (ticksElapsed % 20 == 0) {
                    Particle.DustOptions trailDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 0.7f);
                    origin.getWorld().spawnParticle(Particle.DUST, origin.clone().add(0, 1, 0), 8, 0.3, 0.8, 0.3, 0.0, trailDust, true);
                    origin.getWorld().spawnParticle(Particle.SOUL, origin.clone().add(0, 1.2, 0), 3, 0.2, 0.3, 0.2, 0.01);
                }

                // Duration countdown warning
                int ticksRemaining = duration - ticksElapsed;
                if (ticksRemaining == 60) { // 3 seconds left
                    player.sendMessage("\u00a7d\u00a7o3 seconds remaining...");
                }

                // Duration check
                if (ticksElapsed >= duration) {
                    endProjection(player);
                    this.cancel();
                }
            }
        }.runTaskTimer(this.plugin, 0L, 1L);

        projectionTasks.put(uuid, projTask);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);

        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Astral Projection");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        player.sendMessage("\u00a7d\u00a7oYour soul leaves your body... (" + durationSeconds + "s)");
    }

    public void endProjection(Player player) {
        UUID uuid = player.getUniqueId();
        if (!projectingPlayers.contains(uuid)) return;

        projectingPlayers.remove(uuid);

        // Cancel task
        BukkitTask task = projectionTasks.remove(uuid);
        if (task != null) task.cancel();

        // Restore location and game mode
        Location origin = projectionOrigins.remove(uuid);
        GameMode previousMode = projectionPreviousGameModes.remove(uuid);

        if (player.isOnline()) {
            if (origin != null) {
                player.teleport(origin);
            }
            player.setGameMode(previousMode != null ? previousMode : GameMode.SURVIVAL);

            // Arrival effects
            Location loc = player.getLocation();
            Particle.DustOptions purpleDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.0f);
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 60, 0.4, 1.0, 0.4, 0.0, purpleDust, true);
            loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 1, 0), 40, 0.4, 1.0, 0.4);
            loc.getWorld().spawnParticle(Particle.SOUL, loc.clone().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.02);
            player.playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.8f);

            player.sendMessage("\u00a7d\u00a7oYour soul returns to your body.");
        }
    }



    // ========================================================================
    // 2b. TAG — Sub-ability during Astral Projection
    //     Marks a player, giving the Astra user a compass-like indicator
    // ========================================================================


    // ========================================================================
    // 3. DIMENSIONAL DRIFT — Tier 2 (Momentum Dash)
    //    Flings the player forward in a fixed low arc, with brief invisibility
    //    and fall-damage immunity. Repositioning/escape tool; deals no damage.
    // ========================================================================

    public void dimensionalDrift(Player player) {
        String abilityKey = "astra-drift";

        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        // Fixed forward arc (Puff-style): a horizontal fling with a small fixed lift,
        // independent of look pitch.
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

        // Brief invisibility + fall-damage immunity window
        int invisSeconds = this.plugin.getConfig().getInt("abilities.astra-drift.invisibility", 3);
        int fallImmuneSeconds = this.plugin.getConfig().getInt("abilities.astra-drift.fall-immunity", 6);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, invisSeconds * 20, 0, false, false));
        dashFallImmuneUntil.put(player.getUniqueId(), System.currentTimeMillis() + (long) fallImmuneSeconds * 1000L);

        // Departure burst
        Location loc = player.getLocation();
        Particle.DustOptions purpleDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.3f);
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 50, 0.5, 0.8, 0.5, 0.0, purpleDust, true);
        loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 1, 0), 45, 0.4, 0.6, 0.4, 0.05);
        loc.getWorld().spawnParticle(Particle.WITCH, loc, 15, 0.4, 0.4, 0.4, 0.0);
        player.playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.4f);
        player.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.6f);

        // Purple rift trail during the fling
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks++ >= 12 || !player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }
                Location p = player.getLocation().add(0, 0.4, 0);
                Particle.DustOptions trail = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 0.9f);
                p.getWorld().spawnParticle(Particle.DUST, p, 4, 0.2, 0.2, 0.2, 0.0, trail, true);
                p.getWorld().spawnParticle(Particle.END_ROD, p, 2, 0.1, 0.1, 0.1, 0.01);
            }
        }.runTaskTimer(this.plugin, 1L, 1L);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Dimensional Drift");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        player.sendMessage("§d§oYou blink through the dimensional rift!");
    }

    // ========================================================================
    // 4. DIMENSIONAL VOID — Tier 2
    //    Nullifies all enemy gem abilities within a radius for a duration
    // ========================================================================

    public void dimensionalVoid(Player player) {
        String abilityKey = "astra-void";

        if (isVoidActive(player)) {
            player.sendMessage("\u00a7c\u00a7oDimensional Void is already active!");
            return;
        }

        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Location center = player.getLocation().clone();
        double radius = this.plugin.getConfig().getDouble("abilities.astra-void.radius", 10.0);
        int durationSeconds = this.plugin.getConfig().getInt("abilities.durations.astra-void", 8);
        int duration = durationSeconds * 20;

        voidActivePlayers.add(uuid);

        // Massive activation visual — dome of Astra purple
        ParticleUtils.drawDome(center, ParticleUtils.ASTRA_PURPLE, 1.2f, radius);
        player.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.5f);
        player.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);

        // Broadcast suppression message to affected players
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof Player && entity != player) {
                Player target = (Player) entity;
                if (!plugin.getTrustedPlayersManager().isTrusted(player, target)) {
                    target.sendMessage("\u00a74\u00a7l\u00a7oYour gem abilities have been nullified!");
                    target.playSound(target.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.3f);
                }
            }
        }

        // Void field task — suppresses enemy abilities inside radius
        BukkitTask voidTask = new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticksElapsed >= duration) {
                    endVoid(player);
                    this.cancel();
                    return;
                }

                ticksElapsed++;

                // Apply Mining Fatigue + Weakness to enemies inside radius (suppresses gem use)
                if (ticksElapsed % 20 == 0) {
                    Location playerLoc = player.getLocation();
                    for (Entity entity : playerLoc.getWorld().getNearbyEntities(playerLoc, radius, radius, radius)) {
                        if (entity instanceof Player && entity != player) {
                            Player target = (Player) entity;
                            if (!plugin.getTrustedPlayersManager().isTrusted(player, target)) {
                                // Disable their abilities by setting a temporary flag via cooldown
                                // Apply debuffs as visual indicator
                                target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 30, 1, false, true));
                                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 30, 0, false, true));
                            }
                        }
                    }
                }

                // Visual dome every 10 ticks
                if (ticksElapsed % 10 == 0) {
                    Location playerLoc = player.getLocation();
                    Particle.DustOptions voidDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.0f);
                    // Ground circle
                    for (int i = 0; i < 32; i++) {
                        double angle = (i / 32.0) * 2 * Math.PI;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        playerLoc.getWorld().spawnParticle(Particle.DUST,
                            playerLoc.clone().add(x, 0.3, z), 2, 0.1, 0.1, 0.1, 0.0, voidDust, true);
                        playerLoc.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                            playerLoc.clone().add(x, 0.8, z), 1, 0.1, 0.1, 0.1, 0.01);
                    }
                    // Vertical pillars at cardinal points
                    for (int dir = 0; dir < 4; dir++) {
                        double angle = dir * Math.PI / 2;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        for (double y = 0; y < 3; y += 0.5) {
                            playerLoc.getWorld().spawnParticle(Particle.SOUL,
                                playerLoc.clone().add(x, y, z), 1, 0.05, 0.1, 0.05, 0.01);
                        }
                    }
                }

                // Ambient sound
                if (ticksElapsed % 40 == 0) {
                    player.getLocation().getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.3f);
                }
            }
        }.runTaskTimer(this.plugin, 0L, 1L);

        voidTasks.put(uuid, voidTask);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);

        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Dimensional Void");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        player.sendMessage("\u00a7d\u00a7oDimensional Void active for " + durationSeconds + "s. Enemy abilities nullified in " + (int) radius + " block radius.");
    }

    private void endVoid(Player player) {
        UUID uuid = player.getUniqueId();
        voidActivePlayers.remove(uuid);

        BukkitTask task = voidTasks.remove(uuid);
        if (task != null) task.cancel();

        if (player.isOnline()) {
            player.sendMessage("\u00a7d\u00a7oDimensional Void faded.");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.5f);
        }
    }

    /**
     * Check if a player's abilities are suppressed by any active Dimensional Void
     * Called from AbilityManager or GemInteractListener to block enemy ability usage
     */
    public boolean isAbilitySuppressed(Player target) {
        for (UUID voidUserId : voidActivePlayers) {
            Player voidUser = Bukkit.getPlayer(voidUserId);
            if (voidUser == null || !voidUser.isOnline()) continue;
            if (!voidUser.getWorld().equals(target.getWorld())) continue;

            double radius = this.plugin.getConfig().getDouble("abilities.astra-void.radius", 10.0);
            if (voidUser.getLocation().distance(target.getLocation()) <= radius) {
                // Check if target is not trusted by the void user
                if (!plugin.getTrustedPlayersManager().isTrusted(voidUser, target)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ========================================================================
    // Utility methods
    // ========================================================================

    private LivingEntity getTargetEntity(Player player, int range) {
        var result = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            (double) range,
            entity -> entity instanceof LivingEntity && entity != player
        );
        return result != null ? (LivingEntity) result.getHitEntity() : null;
    }

    private String getDirectionArrow(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        double angle = Math.atan2(direction.getZ(), direction.getX());
        double yaw = Math.toRadians(from.getYaw());

        double relative = angle - yaw;
        // Normalize to -PI to PI
        while (relative > Math.PI) relative -= 2 * Math.PI;
        while (relative < -Math.PI) relative += 2 * Math.PI;

        if (relative > -Math.PI / 4 && relative <= Math.PI / 4) return "\u2192"; // right →
        if (relative > Math.PI / 4 && relative <= 3 * Math.PI / 4) return "\u2193"; // behind ↓
        if (relative > -3 * Math.PI / 4 && relative <= -Math.PI / 4) return "\u2191"; // ahead ↑
        return "\u2190"; // left ←
    }

    /**
     * Cleanup when player leaves
     */
    public void cleanup(Player player) {
        if (isProjecting(player)) {
            endProjection(player);
        }
        if (isVoidActive(player)) {
            endVoid(player);
        }
        dashFallImmuneUntil.remove(player.getUniqueId());
        DaggerVolley volley = daggerVolleys.get(player.getUniqueId());
        if (volley != null) {
            discardVolley(player, volley);
        }
    }
}
