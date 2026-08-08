/*
 * Astra Gem Abilities — dimensional stealth and ranged daggers
 *
 * Tier 1:
 *   - Astral Daggers (Primary): Conjure 5 phantom daggers in a cross formation, each press launches one
 *   - Soul Capture passive + Soul Absorption (temporary max hearts on kill, handled by SoulManager)
 *
 * Tier 2 (all Tier 1 abilities plus):
 *   - Astral Daggers (Primary, no shift): Same as T1
 *   - Astral Projection (Shift): Enter spectator mode to scout, bounded to 8 chunk radius, returns to origin
 *   - Dimensional Drift (Tertiary): Forward dash with brief invisibility and fall-damage immunity
 *   - Dimensional Void (Quaternary): Nullify enemy gem abilities in radius
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.ParticleUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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
import org.joml.Matrix3f;
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
        if (!requireTier2(player)) {
            return;
        }
        dimensionalDrift(player);
    }

    /**
     * Activates Dimensional Void (called from command /bliss ability:tertiary or special keybind)
     */
    public void activateDimensionalVoid(Player player) {
        if (!requireTier2(player)) {
            return;
        }
        dimensionalVoid(player);
    }

    /** Warns and returns false when the player is not on a Tier 2 gem. */
    private boolean requireTier2(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return false;
        }
        return true;
    }

    /** Sends the configured "ability activated" line, when one is configured. */
    private void announceActivation(Player player, String abilityName) {
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", abilityName);
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

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

        // Conjure 5 daggers in the cross formation; slot 0 is dead ahead and launches first.
        // Position + facing are recomputed from the live look each tick (so they always sit
        // ahead and point forward in first person), and Display interpolation smooths it.
        for (int i = 0; i < 5; i++) {
            ItemDisplay dg = spawnDaggerDisplay(player);
            volley.slot.put(dg, i);
            volley.pending.addLast(dg);
        }
        positionHoveringDaggers(player, volley);

        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getEyeLocation(), 30, 0.5, 0.5, 0.5, 0.03);
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 1.4f);

        // Hover loop — keeps the formation softly tracking the head. NO auto-fire: each press
        // launches the next dagger. A long safety window discards leftovers so a cast can't
        // hover forever (and then arms the cooldown).
        final int hoverTimeoutTicks = this.plugin.getConfig().getInt("abilities.astra-daggers.hover-timeout-ticks", 600);
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
                if (++v.idleTicks >= hoverTimeoutTicks) {
                    discardVolley(player, v);
                    daggerVolleys.remove(id);
                    plugin.getAbilityManager().useAbility(player, abilityKey);
                    this.cancel();
                }
            }
        }.runTaskTimer(this.plugin, 1L, 1L);

        // The cast only conjures the daggers — all 5 hover in formation and each press
        // launches the next, starting with the one dead ahead.
        announceActivation(player, "Astral Daggers");
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
        // Spawn with zero entity rotation so orientDagger's transformation is world-space
        // (a FIXED-billboard display's final facing = entity yaw/pitch ∘ transformation).
        Location at = player.getEyeLocation();
        at.setYaw(0f);
        at.setPitch(0f);
        return player.getWorld().spawn(at, ItemDisplay.class, d -> {
            d.setItemStack(buildDaggerItem());
            d.setBillboard(Display.Billboard.FIXED);
            d.setBrightness(new Display.Brightness(15, 15));
            // Smooth the head-follow: glide position over a few ticks and ease rotation,
            // so the array trails the head softly instead of snapping every tick.
            d.setInterpolationDuration(3);
            d.setTeleportDuration(3);
            d.setPersistent(false);
        });
    }

    /**
     * Keep the still-pending daggers arrayed around the head, always sitting ahead and
     * pointing forward (first-person: they read as "always in front, always forward").
     * Position and facing are recomputed from the live look each tick; Display teleport +
     * transformation interpolation smooth the motion so it follows the head softly rather
     * than snapping. Teleports use zero yaw/pitch so the world-space transform is preserved.
     */
    private void positionHoveringDaggers(Player player, DaggerVolley volley) {
        Location eye = player.getEyeLocation();
        var world = eye.getWorld();
        Vector look = eye.getDirection().normalize();
        Vector right = look.clone().crossProduct(new Vector(0, 1, 0));
        if (right.lengthSquared() < 1.0E-6) {
            right = new Vector(1, 0, 0);
        }
        right.normalize();

        double d = this.plugin.getConfig().getDouble("abilities.astra-daggers.hover-distance", 1.6);
        Vector fwd = look.clone().multiply(d);
        Vector rgt = right.clone().multiply(d);
        Vector down = new Vector(0, -0.2, 0);
        // Blades hover facing the opposite way (cocked back), flipped 180° from their flight.
        Vector face = look.clone().rotateAroundY(Math.PI);

        for (ItemDisplay dagger : volley.pending) {
            if (dagger == null || dagger.isDead()) {
                continue;
            }
            int slot = volley.slot.getOrDefault(dagger, 0);
            Vector off = switch (slot) {
                case 1 -> rgt.clone().multiply(-1);          // left
                case 2 -> rgt.clone();                        // right
                case 3 -> fwd.clone().subtract(rgt);          // front-left corner
                case 4 -> fwd.clone().add(rgt);               // front-right corner
                default -> fwd.clone();                       // slot 0 — dead ahead
            };
            off.add(down);
            Location hover = new Location(world,
                    eye.getX() + off.getX(), eye.getY() + off.getY(), eye.getZ() + off.getZ(), 0f, 0f);
            orientDagger(dagger, face);
            dagger.teleport(hover);
        }
    }

    private void launchNextDagger(Player player, DaggerVolley volley) {
        ItemDisplay dagger = volley.pending.pollFirst();
        volley.idleTicks = 0;
        if (dagger != null) {
            volley.slot.remove(dagger);
            if (!dagger.isDead()) {
                flyDagger(player, dagger);
            }
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
        final Vector step = dir.clone().multiply(speed);
        // Flight must be crisp — drop the hover glide so the projectile doesn't lag its path.
        dagger.setTeleportDuration(0);
        orientDagger(dagger, dir);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.6f);

        new BukkitRunnable() {
            final Location loc = zeroRot(player.getEyeLocation().add(dir.clone().multiply(1.2)));
            final Particle.DustOptions trailDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 0.9f);
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

                loc.add(step);
                travelled += speed;

                if (loc.getBlock().getType().isSolid()) {
                    impact(loc, false);
                    dagger.remove();
                    this.cancel();
                    return;
                }

                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 0.6, 0.6, 0.6)) {
                    if (!(entity instanceof LivingEntity target) || entity == player) {
                        continue;
                    }
                    if (entity instanceof Player tp
                            && (!player.canSee(tp) || plugin.getTrustedPlayersManager().isTrusted(player, tp))) {
                        continue;
                    }
                    target.damage(damage, (Entity) player);
                    onDaggerHit(player, target);
                    impact(target.getLocation().add(0, 1, 0), true);
                    dagger.remove();
                    this.cancel();
                    return;
                }

                dagger.teleport(loc);
                var world = loc.getWorld();
                world.spawnParticle(Particle.DUST, loc, 3, 0.05, 0.05, 0.05, 0.0, trailDust, true);
                world.spawnParticle(Particle.REVERSE_PORTAL, loc, 2, 0.03, 0.03, 0.03, 0.01);
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

    /** Strip yaw/pitch off a location so a FIXED display's world-space transformation is preserved on teleport. */
    private static Location zeroRot(Location l) {
        l.setYaw(0f);
        l.setPitch(0f);
        return l;
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
        // Build a stable orientation: model +Y aligns to the aim, and roll is pinned to
        // world-up so the blade never spins about its own axis as the head turns.
        Vector3f yAxis = new Vector3f((float) d.getX(), (float) d.getY(), (float) d.getZ()).normalize();
        Vector3f xAxis = new Vector3f(0f, 1f, 0f).cross(yAxis);
        if (xAxis.lengthSquared() < 1.0E-6f) {
            xAxis.set(1f, 0f, 0f);   // aim is vertical — pick any stable right axis
        } else {
            xAxis.normalize();
        }
        Vector3f zAxis = new Vector3f(yAxis).cross(xAxis).normalize();
        Quaternionf rot = new Matrix3f(xAxis, yAxis, zAxis).getNormalizedRotation(new Quaternionf());
        // Start easing toward the new facing immediately (paired with the spawn-set
        // interpolation-duration), so head turns rotate the blades smoothly.
        dagger.setInterpolationDelay(0);
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
        // Formation slot (0 ahead, 1 left, 2 right, 3 front-left, 4 front-right). The
        // hover position/orientation are recomputed from the live look each tick, but
        // Display interpolation smooths the motion so they follow the head softly
        // instead of snapping tick-to-tick.
        final Map<ItemDisplay, Integer> slot = new HashMap<>();
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

        // Massive activation visual — dome of Astra purple + the void bubble snapping in.
        ParticleUtils.drawDome(center, ParticleUtils.ASTRA_PURPLE, 1.2f, radius);
        drawVoidSphere(player.getLocation(), Math.min(radius, 2.8), 0);
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

                // Purple void bubble hugging the caster + white swirls — rendered often so it
                // reads as a solid, glowing sphere rather than a flickering shell.
                if (ticksElapsed % 4 == 0) {
                    drawVoidSphere(player.getLocation(), Math.min(radius, 2.8), ticksElapsed * 0.12);
                }

                // Ground circle marking the true nullify radius, every 10 ticks — violet only.
                if (ticksElapsed % 10 == 0) {
                    Location playerLoc = player.getLocation();
                    Particle.DustOptions voidDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.0f);
                    for (int i = 0; i < 32; i++) {
                        double angle = (i / 32.0) * 2 * Math.PI;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        playerLoc.getWorld().spawnParticle(Particle.DUST,
                            playerLoc.clone().add(x, 0.3, z), 2, 0.1, 0.1, 0.1, 0.0, voidDust, true);
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

    /**
     * Purple Astra "void bubble" — a spherical shell of enchant-purple dust with a
     * couple of bright white swirl lines coiling around it. {@code phase} rotates the
     * swirls each call so the bubble looks alive. Radius is kept small (body-sized)
     * so it reads as an aura around the caster, not a giant mesh.
     */
    private void drawVoidSphere(Location center, double sphereRadius, double phase) {
        var world = center.getWorld();
        if (world == null) {
            return;
        }
        Particle.DustOptions purple = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.1f);
        // Centre the bubble on the caster's body rather than their feet.
        Location mid = center.clone().add(0, sphereRadius * 0.9, 0);

        // Dense purple shell: stacked latitude rings.
        int rings = 10;
        for (int r = 1; r < rings; r++) {
            double theta = Math.PI * r / rings;
            double ringRadius = sphereRadius * Math.sin(theta);
            double y = sphereRadius * Math.cos(theta);
            int pts = Math.max(10, (int) (ringRadius * 13));
            for (int i = 0; i < pts; i++) {
                double a = (i / (double) pts) * 2 * Math.PI;
                double x = Math.cos(a) * ringRadius;
                double z = Math.sin(a) * ringRadius;
                world.spawnParticle(Particle.DUST, mid.clone().add(x, y, z), 1, 0, 0, 0, 0.0, purple, true);
            }
        }

        // Bright-white SWIRLS (not a cage): a few pole-to-pole spirals that coil around the
        // ball, so the white reads as curling streaks. END_ROD glows like the reference.
        int swirls = 3;
        int spiralPts = 64;
        double turns = 2.0;
        for (int s = 0; s < swirls; s++) {
            double startAngle = phase + s * (2 * Math.PI / swirls);
            for (int i = 0; i < spiralPts; i++) {
                double t = i / (double) (spiralPts - 1);          // 0..1 bottom→top
                double y = (t - 0.5) * 2.0 * sphereRadius * 0.95; // near pole to pole
                double rr = Math.sqrt(Math.max(0.0, sphereRadius * sphereRadius - y * y));
                double a = t * turns * 2 * Math.PI + startAngle;  // wrap around while rising
                double x = Math.cos(a) * rr;
                double z = Math.sin(a) * rr;
                world.spawnParticle(Particle.END_ROD, mid.clone().add(x, y, z), 0, 0, 0, 0, 0.0);
            }
        }
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
