/*
 * Astra Gem Abilities — dimensional stealth and ranged daggers
 *
 * Tier 1:
 *   - Astral Daggers (Primary): Fire 3 phantom daggers that deal damage
 *   - Soul Capture passive + Soul Healing (handled by SoulManager)
 *
 * Tier 2 (all Tier 1 abilities plus):
 *   - Astral Daggers (Primary, no shift): Same as T1
 *   - Astral Projection (Shift): Short spectator mode, stay where you end up, ghost trail at origin
 *     - Sub-abilities during projection: Spook (scare nearby players) and Tag (mark a player)
 *   - Dimensional Drift (Double-shift / secondary command): Invisible horse + player invisibility
 *   - Dimensional Void (Sneak + Swap / tertiary): Nullify enemy gem abilities in radius
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
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

import java.util.*;

public class AstraAbilities {
    private final BlissGems plugin;

    // Astral Projection state
    private final Map<UUID, Location> projectionOrigins = new HashMap<>();
    private final Map<UUID, GameMode> previousGameModes = new HashMap<>();
    private final Map<UUID, BukkitTask> projectionTasks = new HashMap<>();

    // Dimensional Drift state
    private final Map<UUID, Horse> driftHorses = new HashMap<>();
    private final Map<UUID, BukkitTask> driftTasks = new HashMap<>();
    private final Set<UUID> driftingPlayers = new HashSet<>();

    // Dimensional Void state
    private final Set<UUID> voidActivePlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> voidTasks = new HashMap<>();

    // Tagged players (Astral Projection sub-ability)
    private final Map<UUID, UUID> taggedPlayers = new HashMap<>(); // tagger -> tagged
    private final Map<UUID, BukkitTask> tagTasks = new HashMap<>();

    public AstraAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    // ========================================================================
    // Right-click routing
    // ========================================================================

    public void onRightClick(Player player, int tier) {
        if (tier == 2) {
            if (player.isSneaking()) {
                // If in projection, use sub-abilities
                if (isInProjection(player)) {
                    // Shift right-click during projection = Spook
                    spook(player);
                } else {
                    this.astralProjection(player);
                }
            } else {
                // If in projection, Tag on non-shift right-click
                if (isInProjection(player)) {
                    tag(player);
                } else {
                    this.astralDaggers(player);
                }
            }
        } else {
            this.astralDaggers(player);
        }
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

    public boolean isInProjection(Player player) {
        return projectionOrigins.containsKey(player.getUniqueId());
    }

    public boolean isDrifting(Player player) {
        return driftingPlayers.contains(player.getUniqueId());
    }

    public boolean isVoidActive(Player player) {
        return voidActivePlayers.contains(player.getUniqueId());
    }

    // ========================================================================
    // 1. ASTRAL DAGGERS — Tier 1+2 Primary
    // ========================================================================

    public void astralDaggers(Player player) {
        String abilityKey = "astra-daggers";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();

        for (int i = 0; i < 3; ++i) {
            int index = i;
            this.plugin.getServer().getScheduler().runTaskLater((Plugin) this.plugin, () -> {
                Location start = eyeLoc.clone().add(direction.clone().multiply(1));
                Vector spread = direction.clone().rotateAroundAxis(new Vector(0, 1, 0), Math.toRadians(index * 15 - 15));

                for (int j = 0; j < 30; ++j) {
                    Location current = start.clone().add(spread.clone().multiply((double) j * 0.5));

                    // Astra deep purple dust particles (RGB 106, 11, 184)
                    Particle.DustOptions purpleDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.0f);
                    player.getWorld().spawnParticle(Particle.DUST, current, 3, 0.1, 0.1, 0.1, 0.0, purpleDust, true);
                    player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, current, 2, 0.05, 0.05, 0.05, 0.01);
                    player.getWorld().spawnParticle(Particle.WITCH, current, 1, 0.0, 0.0, 0.0, 0.0);

                    for (Entity entity : current.getWorld().getNearbyEntities(current, 0.5, 0.5, 0.5)) {
                        if (!(entity instanceof LivingEntity)) continue;
                        LivingEntity target = (LivingEntity) entity;
                        if (entity == player) continue;

                        // Skip invisible/trusted players
                        if (entity instanceof Player) {
                            Player targetPlayer = (Player) entity;
                            if (!player.canSee(targetPlayer) || plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                                continue;
                            }
                        }

                        double damage = this.plugin.getConfigManager().getAbilityDamage("astra-daggers");
                        target.damage(damage, (Entity) player);

                        // MASSIVE deep purple particle burst on hit
                        Particle.DustOptions purpleDustHit = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.5f);
                        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 80, 0.9, 0.9, 0.9, 0.0, purpleDustHit, true);
                        target.getWorld().spawnParticle(Particle.REVERSE_PORTAL, target.getLocation().add(0.0, 1.0, 0.0), 60, 0.7, 0.7, 0.7);
                        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0.0, 1.0, 0.0), 40, 0.7, 0.7, 0.7);
                        target.getWorld().spawnParticle(Particle.WITCH, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5);
                        target.getWorld().spawnParticle(Particle.END_ROD, target.getLocation().add(0.0, 1.0, 0.0), 25, 0.5, 0.5, 0.5);
                        return;
                    }
                    if (current.getBlock().getType().isSolid()) break;
                }
            }, (long) i * 5L);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.5f);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);

        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Astral Daggers");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    // ========================================================================
    // 2. ASTRAL PROJECTION — Tier 2 Secondary (Shift)
    //    Short spectator, ghost trail at origin, stay where you end up
    // ========================================================================

    public void astralProjection(Player player) {
        String abilityKey = "astra-projection";

        // If already in projection, end it early
        if (isInProjection(player)) {
            endProjection(player);
            return;
        }

        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        // Store original location and gamemode
        Location origin = player.getLocation().clone();
        GameMode previousMode = player.getGameMode();

        projectionOrigins.put(player.getUniqueId(), origin);
        previousGameModes.put(player.getUniqueId(), previousMode);

        // Set to spectator mode
        player.setGameMode(GameMode.SPECTATOR);

        // Deep purple projection particles at departure
        Particle.DustOptions purpleDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.0f);
        player.getWorld().spawnParticle(Particle.DUST, origin, 100, 0.8, 1.5, 0.8, 0.0, purpleDust, true);
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, origin, 80, 0.7, 1.2, 0.7);
        player.getWorld().spawnParticle(Particle.PORTAL, origin, 50, 0.5, 1.0, 0.5);
        player.getWorld().spawnParticle(Particle.WITCH, origin, 30, 0.5, 1.0, 0.5);
        player.playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
        player.playSound(origin, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.2f);

        // Config values
        double maxRadius = this.plugin.getConfig().getDouble("abilities.astra-projection.radius", 150.0);
        int durationSeconds = this.plugin.getConfig().getInt("abilities.durations.astra-projection", 5);
        int duration = durationSeconds * 20; // Convert to ticks

        // Start monitoring task — ghost trail at origin + radius/duration enforcement
        BukkitTask task = new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !isInProjection(player)) {
                    if (player.isDead() && isInProjection(player)) {
                        endProjection(player);
                    }
                    this.cancel();
                    return;
                }

                ticksElapsed++;

                // Ghost trail at origin every 10 ticks (0.5s)
                if (ticksElapsed % 10 == 0) {
                    Particle.DustOptions ghostDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.2f);
                    origin.getWorld().spawnParticle(Particle.DUST, origin.clone().add(0, 1, 0), 15, 0.4, 0.8, 0.4, 0.0, ghostDust, true);
                    origin.getWorld().spawnParticle(Particle.SOUL, origin.clone().add(0, 1.5, 0), 5, 0.3, 0.5, 0.3, 0.02);
                    origin.getWorld().spawnParticle(Particle.REVERSE_PORTAL, origin.clone().add(0, 0.5, 0), 8, 0.3, 0.6, 0.3, 0.01);
                }

                // Particle trail following projection player
                if (ticksElapsed % 5 == 0) {
                    Location projLoc = player.getLocation();
                    Particle.DustOptions trailDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 0.7f);
                    projLoc.getWorld().spawnParticle(Particle.DUST, projLoc, 5, 0.2, 0.2, 0.2, 0.0, trailDust, true);
                    projLoc.getWorld().spawnParticle(Particle.END_ROD, projLoc, 2, 0.1, 0.1, 0.1, 0.01);
                }

                // Check distance from origin
                double distance = player.getLocation().distance(origin);
                if (distance > maxRadius) {
                    Vector direction = player.getLocation().toVector().subtract(origin.toVector()).normalize();
                    Location edgeLocation = origin.clone().add(direction.multiply(maxRadius - 1));
                    edgeLocation.setYaw(player.getLocation().getYaw());
                    edgeLocation.setPitch(player.getLocation().getPitch());
                    player.teleport(edgeLocation);
                    player.sendMessage(plugin.getConfigManager().getFormattedMessage("projection-boundary", new Object[0]));
                }

                // Check duration
                if (ticksElapsed >= duration) {
                    endProjection(player);
                    this.cancel();
                }
            }
        }.runTaskTimer(this.plugin, 0L, 1L);

        projectionTasks.put(player.getUniqueId(), task);

        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Astral Projection");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        player.sendMessage("\u00a7d\u00a7oYou have " + durationSeconds + "s. Right-click to Tag, Shift+Right-click to Spook.");
    }

    public void endProjection(Player player) {
        if (!isInProjection(player)) {
            return;
        }

        Location origin = projectionOrigins.remove(player.getUniqueId());
        GameMode previousMode = previousGameModes.remove(player.getUniqueId());

        // Cancel monitoring task
        BukkitTask task = projectionTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }

        // Stay where you are (no teleport back) — arrival particles
        Location arrivalLoc = player.getLocation().clone();
        Particle.DustOptions purpleDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.0f);
        arrivalLoc.getWorld().spawnParticle(Particle.DUST, arrivalLoc, 80, 0.8, 1.5, 0.8, 0.0, purpleDust, true);
        arrivalLoc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, arrivalLoc, 60, 0.7, 1.2, 0.7);
        arrivalLoc.getWorld().spawnParticle(Particle.END_ROD, arrivalLoc, 30, 0.5, 1.0, 0.5);
        arrivalLoc.getWorld().spawnParticle(Particle.SOUL, arrivalLoc, 20, 0.5, 1.0, 0.5);
        player.playSound(arrivalLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.playSound(arrivalLoc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.8f);

        // Restore gamemode
        if (previousMode != null) {
            player.setGameMode(previousMode);
        } else {
            player.setGameMode(GameMode.SURVIVAL);
        }

        // Set cooldown when projection ends
        this.plugin.getAbilityManager().useAbility(player, "astra-projection");

        String msg = this.plugin.getConfigManager().getFormattedMessage("projection-ended", new Object[0]);
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    // ========================================================================
    // 2a. SPOOK — Sub-ability during Astral Projection
    //     Scares nearby players (Blindness + Nausea + spooky effects)
    // ========================================================================

    private void spook(Player player) {
        if (!isInProjection(player)) return;

        String abilityKey = "astra-spook";
        if (this.plugin.getAbilityManager().isOnCooldown(player, abilityKey)) {
            return;
        }

        Location loc = player.getLocation();
        double radius = this.plugin.getConfig().getDouble("abilities.astra-spook.radius", 8.0);
        int duration = this.plugin.getConfig().getInt("abilities.durations.astra-spook", 3) * 20;

        int spookCount = 0;
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (!(entity instanceof Player)) continue;
            Player target = (Player) entity;
            if (target.equals(player)) continue;
            if (plugin.getTrustedPlayersManager().isTrusted(player, target)) continue;

            // Apply blindness + nausea
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 0, false, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, duration, 0, false, true));

            // Spooky particles on target
            target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.05);
            target.getWorld().spawnParticle(Particle.REVERSE_PORTAL, target.getLocation().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0.02);
            target.playSound(target.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.5f);
            target.playSound(target.getLocation(), Sound.ENTITY_GHAST_SCREAM, 0.6f, 0.7f);
            target.sendMessage("\u00a74\u00a7l\u00a7oSomething is watching you...");
            spookCount++;
        }

        // AoE visual
        Particle.DustOptions ghostDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.5f);
        for (int i = 0; i < 32; i++) {
            double angle = (i / 32.0) * 2 * Math.PI;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0.5, z), 3, 0.1, 0.1, 0.1, 0.0, ghostDust, true);
            loc.getWorld().spawnParticle(Particle.SOUL, loc.clone().add(x, 1, z), 1, 0.1, 0.1, 0.1, 0.01);
        }

        player.playSound(loc, Sound.ENTITY_WARDEN_ROAR, 0.7f, 1.5f);
        this.plugin.getAbilityManager().setCooldown(player, abilityKey,
            this.plugin.getConfig().getInt("abilities.cooldowns.astra-spook", 10));

        player.sendMessage("\u00a7d\u00a7oSpooked " + spookCount + " player(s)!");
    }

    // ========================================================================
    // 2b. TAG — Sub-ability during Astral Projection
    //     Marks a player, giving the Astra user a compass-like indicator
    // ========================================================================

    private void tag(Player player) {
        if (!isInProjection(player)) return;

        String abilityKey = "astra-tag";
        if (this.plugin.getAbilityManager().isOnCooldown(player, abilityKey)) {
            return;
        }

        // Raycast to find target
        LivingEntity target = getTargetEntity(player, 20);
        if (target == null || !(target instanceof Player)) {
            player.sendMessage("\u00a7c\u00a7oNo player target found! Aim at a player.");
            return;
        }

        Player targetPlayer = (Player) target;
        if (plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
            player.sendMessage("\u00a7c\u00a7oYou cannot tag a trusted player!");
            return;
        }

        UUID taggerId = player.getUniqueId();
        UUID taggedId = targetPlayer.getUniqueId();

        // Cancel existing tag task
        BukkitTask oldTask = tagTasks.remove(taggerId);
        if (oldTask != null) oldTask.cancel();

        taggedPlayers.put(taggerId, taggedId);

        int tagDuration = this.plugin.getConfig().getInt("abilities.durations.astra-tag", 30) * 20;

        // Tag visual on target
        Particle.DustOptions purpleDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.2f);
        targetPlayer.getWorld().spawnParticle(Particle.DUST, targetPlayer.getLocation().add(0, 2.2, 0), 20, 0.2, 0.2, 0.2, 0.0, purpleDust, true);
        targetPlayer.getWorld().spawnParticle(Particle.END_ROD, targetPlayer.getLocation().add(0, 2.5, 0), 10, 0.1, 0.1, 0.1, 0.01);
        targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 0.5f);
        targetPlayer.sendMessage("\u00a7d\u00a7oYou have been tagged by an astral presence...");

        // Start tracking task — shows directional indicator in action bar
        BukkitTask tagTask = new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                ticksElapsed++;

                if (!player.isOnline() || !targetPlayer.isOnline() || ticksElapsed >= tagDuration) {
                    taggedPlayers.remove(taggerId);
                    tagTasks.remove(taggerId);
                    if (player.isOnline()) {
                        player.sendMessage("\u00a7d\u00a7oTag on " + targetPlayer.getName() + " expired.");
                    }
                    this.cancel();
                    return;
                }

                // Show directional indicator every 20 ticks (1 second)
                if (ticksElapsed % 20 == 0 && player.isOnline() && targetPlayer.isOnline()) {
                    if (player.getWorld().equals(targetPlayer.getWorld())) {
                        double distance = player.getLocation().distance(targetPlayer.getLocation());
                        String direction = getDirectionArrow(player.getLocation(), targetPlayer.getLocation());
                        net.md_5.bungee.api.chat.TextComponent tagMsg = new net.md_5.bungee.api.chat.TextComponent(
                            "\u00a7d\u00a7lTAG \u00a77" + direction + " \u00a7d" + targetPlayer.getName() + " \u00a77" + (int) distance + "m"
                        );
                        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, tagMsg);

                        // Glowing particles on tagged player visible to tagger
                        Particle.DustOptions tagDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 0.8f);
                        targetPlayer.getWorld().spawnParticle(Particle.DUST, targetPlayer.getLocation().add(0, 2.2, 0), 5, 0.15, 0.15, 0.15, 0.0, tagDust, true);
                    }
                }
            }
        }.runTaskTimer(this.plugin, 0L, 1L);

        tagTasks.put(taggerId, tagTask);
        this.plugin.getAbilityManager().setCooldown(player, abilityKey,
            this.plugin.getConfig().getInt("abilities.cooldowns.astra-tag", 15));

        player.sendMessage("\u00a7d\u00a7oTagged " + targetPlayer.getName() + "! Tracking for " +
            (tagDuration / 20) + " seconds.");
    }

    // ========================================================================
    // 3. DIMENSIONAL DRIFT — Tier 2
    //    Summons invisible horse, player becomes invisible
    // ========================================================================

    public void dimensionalDrift(Player player) {
        String abilityKey = "astra-drift";

        // Toggle off if already drifting
        if (isDrifting(player)) {
            endDrift(player);
            return;
        }

        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Location loc = player.getLocation();
        int durationSeconds = this.plugin.getConfig().getInt("abilities.durations.astra-drift", 15);
        int duration = durationSeconds * 20;

        // Spawn invisible horse
        Horse horse = player.getWorld().spawn(loc, Horse.class, h -> {
            h.setTamed(true);
            h.setOwner(player);
            h.setAdult();
            h.setInvisible(true);
            h.setSilent(true);
            h.setInvulnerable(true);
            h.setAI(false); // Player controls it
            h.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
            h.getInventory().setSaddle(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SADDLE));
        });

        // Mount player on horse
        horse.addPassenger(player);
        driftHorses.put(uuid, horse);
        driftingPlayers.add(uuid);

        // Make player invisible
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration + 20, 0, false, false));

        // Departure effects
        Particle.DustOptions purpleDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.3f);
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 60, 0.6, 1.0, 0.6, 0.0, purpleDust, true);
        loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0.02);
        loc.getWorld().spawnParticle(Particle.WITCH, loc, 20, 0.5, 0.5, 0.5, 0.0);
        player.playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.5f);
        player.playSound(loc, Sound.ENTITY_HORSE_SADDLE, 0.5f, 2.0f);

        // Monitor task
        BukkitTask driftTask = new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !isDrifting(player)) {
                    endDrift(player);
                    this.cancel();
                    return;
                }

                ticksElapsed++;

                // Subtle particle trail (visible to others to hint at presence)
                if (ticksElapsed % 8 == 0) {
                    Location pLoc = player.getLocation();
                    Particle.DustOptions trailDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 0.5f);
                    pLoc.getWorld().spawnParticle(Particle.DUST, pLoc.clone().add(0, 0.3, 0), 2, 0.3, 0.1, 0.3, 0.0, trailDust, true);
                    pLoc.getWorld().spawnParticle(Particle.END_ROD, pLoc.clone().add(0, 0.5, 0), 1, 0.2, 0.1, 0.2, 0.005);
                }

                // Duration check
                if (ticksElapsed >= duration) {
                    endDrift(player);
                    this.cancel();
                }
            }
        }.runTaskTimer(this.plugin, 0L, 1L);

        driftTasks.put(uuid, driftTask);

        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Dimensional Drift");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        player.sendMessage("\u00a7d\u00a7oYou fade into the dimensional rift... (" + durationSeconds + "s)");
    }

    public void endDrift(Player player) {
        UUID uuid = player.getUniqueId();
        if (!driftingPlayers.contains(uuid)) return;

        driftingPlayers.remove(uuid);

        // Cancel task
        BukkitTask task = driftTasks.remove(uuid);
        if (task != null) task.cancel();

        // Remove horse
        Horse horse = driftHorses.remove(uuid);
        if (horse != null) {
            horse.removePassenger(player);
            horse.remove();
        }

        // Remove invisibility
        player.removePotionEffect(PotionEffectType.INVISIBILITY);

        // Arrival effects
        Location loc = player.getLocation();
        Particle.DustOptions purpleDust = new Particle.DustOptions(ParticleUtils.ASTRA_PURPLE, 1.3f);
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 60, 0.6, 1.0, 0.6, 0.0, purpleDust, true);
        loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0.02);
        player.playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.8f);

        // Set cooldown on end
        this.plugin.getAbilityManager().useAbility(player, "astra-drift");

        player.sendMessage("\u00a7d\u00a7oDimensional Drift ended.");
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
        if (isInProjection(player)) {
            endProjection(player);
        }
        if (isDrifting(player)) {
            endDrift(player);
        }
        if (isVoidActive(player)) {
            endVoid(player);
        }

        // Cleanup tags
        UUID uuid = player.getUniqueId();
        BukkitTask tagTask = tagTasks.remove(uuid);
        if (tagTask != null) tagTask.cancel();
        taggedPlayers.remove(uuid);
    }
}
