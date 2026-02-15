/*
 * Fire Gem Abilities
 * - Charged Fireball (Tier 1+2): 15 second charge with visual particles
 * - Campfire (Tier 2): Places campfire block that burns enemies and heals caster
 * - Crisp (Tier 2): Evaporates all water in range and replaces surrounding blocks with nether blocks
 * - Meteor Shower (Tier 2): Rains fire on a target area
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.ParticleUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class FireAbilities {
    private final BlissGems plugin;
    private final Map<UUID, Integer> chargingPlayers = new HashMap<>();
    private final Map<UUID, BukkitTask> chargingTasks = new HashMap<>();
    private final Map<UUID, Location> activeCampfires = new HashMap<>();
    private final Map<UUID, BukkitTask> campfireTasks = new HashMap<>();

    // Crisp state
    private final Set<UUID> crispActivePlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> crispTasks = new HashMap<>();

    // Meteor Shower state
    private final Set<UUID> meteorShowersActive = new HashSet<>();
    private final Map<UUID, BukkitTask> meteorTasks = new HashMap<>();

    private static final int MAX_CHARGE = 100;
    private static final int CHARGE_DURATION_TICKS = 300; // 15 seconds

    // Nether block palette for Crisp
    private static final Material[] NETHER_BLOCKS = {
        Material.NETHERRACK,
        Material.NETHER_BRICKS,
        Material.NETHER_BRICK_FENCE,
        Material.MAGMA_BLOCK,
        Material.SOUL_SAND
    };

    private final Random random = new Random();

    public FireAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    public boolean isCharging(Player player) {
        return chargingPlayers.containsKey(player.getUniqueId());
    }

    public int getCharge(Player player) {
        return chargingPlayers.getOrDefault(player.getUniqueId(), 0);
    }

    public boolean isCrispActive(Player player) {
        return crispActivePlayers.contains(player.getUniqueId());
    }

    public void onRightClick(Player player, int tier) {
        if (tier == 2 && player.isSneaking()) {
            this.cozyCampfire(player);
        } else {
            this.chargedFireball(player);
        }
    }

    public void chargedFireball(Player player) {
        String abilityKey = "fire-fireball";

        // If already charging, fire the shot
        if (isCharging(player)) {
            fireChargedShot(player);
            return;
        }

        // Check cooldown before starting charge
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        // Start charging
        UUID uuid = player.getUniqueId();
        chargingPlayers.put(uuid, 0);

        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.0f, 0.5f);
        player.sendMessage("\u00a76\u00a7oCharging fireball... Right-click again to fire!");

        // Charging task - increases charge over 15 seconds
        BukkitTask task = new BukkitRunnable() {
            int ticksElapsed = 0;
            boolean maxChargeNotified = false;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !chargingPlayers.containsKey(uuid)) {
                    cancelCharging(player);
                    this.cancel();
                    return;
                }

                ticksElapsed++;

                // Calculate charge based on elapsed time (15 seconds = 300 ticks for 100%)
                int newCharge = Math.min((ticksElapsed * MAX_CHARGE) / CHARGE_DURATION_TICKS, MAX_CHARGE);
                chargingPlayers.put(uuid, newCharge);

                // Show charge bar in action bar
                showChargeBar(player, newCharge);

                // Visual particles AROUND the player - NOT blocking view
                Location playerLoc = player.getLocation(); // Ground level, not head level!

                // Create a ring of flame particles around the player at WAIST/FEET height
                double ringRadius = 1.4; // Further from player
                for (int i = 0; i < 8; i++) {
                    double angle = (i / 8.0) * 2 * Math.PI + (ticksElapsed * 0.1);
                    double x = Math.cos(angle) * ringRadius;
                    double z = Math.sin(angle) * ringRadius;

                    // Ground level ring - FEET
                    player.getWorld().spawnParticle(Particle.FLAME,
                        playerLoc.clone().add(x, 0.2, z),
                        3, 0.1, 0.1, 0.1, 0.01);

                    // Waist height ring - NOT at head!
                    player.getWorld().spawnParticle(Particle.FLAME,
                        playerLoc.clone().add(x * 0.8, 0.8, z * 0.8),
                        2, 0.05, 0.05, 0.05, 0.01);
                }

                // Spiral of particles going upward - BEHIND player, not in front
                double spiralAngle = (ticksElapsed * 0.3) % (2 * Math.PI);
                double spiralRadius = 1.0; // Further from center
                for (int h = 0; h < 3; h++) {
                    double heightOffset = h * 0.4; // Lower heights
                    double x = Math.cos(spiralAngle + h) * spiralRadius;
                    double z = Math.sin(spiralAngle + h) * spiralRadius;
                    player.getWorld().spawnParticle(Particle.FLAME,
                        playerLoc.clone().add(x, heightOffset, z),
                        1, 0.05, 0.05, 0.05, 0);
                }

                // Additional particles that increase with charge - AROUND player, NOT at head
                if (ticksElapsed % 5 == 0) {
                    int particleCount = 5 + (newCharge / 15);

                    // Flame particles AROUND player at waist level - NOT at head!
                    player.getWorld().spawnParticle(Particle.FLAME,
                        playerLoc.clone().add(0, 0.8, 0), // Waist level
                        particleCount * 2, 1.2, 0.5, 1.2, 0.02); // Spread horizontally, not vertically

                    // Fire gem bright orange dust particles (RGB 255, 119, 0) - LOW
                    Particle.DustOptions orangeDust = new Particle.DustOptions(ParticleUtils.FIRE_ORANGE, 1.5f);
                    player.getWorld().spawnParticle(Particle.DUST,
                        playerLoc.clone().add(0, 0.5, 0), // Lower than waist
                        particleCount, 1.5, 0.4, 1.5, 0.0, orangeDust, true); // Wide spread, low vertical

                    // Small fire particles on the ground around player - CIRCLE
                    for (int i = 0; i < 8; i++) {
                        double angle = (i / 8.0) * 2 * Math.PI;
                        double x = Math.cos(angle) * 1.8;
                        double z = Math.sin(angle) * 1.8;
                        player.getWorld().spawnParticle(Particle.LAVA,
                            player.getLocation().add(x, 0.1, z),
                            2, 0.1, 0.1, 0.1, 0);
                    }
                }

                // Sound feedback at milestones
                if (newCharge == 25 || newCharge == 50 || newCharge == 75) {
                    player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.0f + (newCharge / 100.0f));
                }

                // Max charge reached - notify only once
                if (newCharge >= MAX_CHARGE && !maxChargeNotified) {
                    player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.5f);
                    player.sendMessage("\u00a76\u00a7lFully charged! \u00a7eRight-click to fire!");
                    maxChargeNotified = true;
                }

                // Keep showing max charge particles - IMPRESSIVE but NOT blocking view
                if (newCharge >= MAX_CHARGE && ticksElapsed % 3 == 0) {
                    // Large flame burst AROUND player at LOW height - NOT at head!
                    player.getWorld().spawnParticle(Particle.FLAME,
                        playerLoc.clone().add(0, 0.6, 0), // LOW, at torso
                        25, 1.8, 0.4, 1.8, 0.08); // Wide horizontal spread, minimal vertical

                    // Soul fire flames for extra effect - GROUND level
                    player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        playerLoc.clone().add(0, 0.3, 0), // Very low
                        15, 1.5, 0.3, 1.5, 0.05); // Wide spread

                    // Lava particles on ground - BIG circle
                    player.getWorld().spawnParticle(Particle.LAVA,
                        player.getLocation().add(0, 0.1, 0),
                        12, 2.0, 0.1, 2.0, 0); // Even wider ground spread
                }
            }
        }.runTaskTimer(this.plugin, 0L, 1L);

        chargingTasks.put(uuid, task);

        // Auto-fire after 15 seconds (when fully charged)
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (isCharging(player)) {
                fireChargedShot(player);
            }
        }, CHARGE_DURATION_TICKS + 20L); // Extra 1 second grace period
    }

    private void showChargeBar(Player player, int charge) {
        int bars = charge / 5; // 20 bars total for 100 charge
        StringBuilder bar = new StringBuilder("\u00a76Fireball: \u00a7c");

        for (int i = 0; i < 20; i++) {
            if (i < bars) {
                bar.append("\u2588"); // Full block
            } else {
                bar.append("\u00a78\u2588"); // Dark gray block
            }
        }

        bar.append(" \u00a7e").append(charge).append("%");

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar.toString()));
    }

    private void fireChargedShot(Player player) {
        UUID uuid = player.getUniqueId();
        int charge = chargingPlayers.getOrDefault(uuid, 0);

        // Cancel charging task
        BukkitTask task = chargingTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        chargingPlayers.remove(uuid);

        if (charge < 10) {
            player.sendMessage("\u00a7c\u00a7oNot enough charge!");
            return;
        }

        // Calculate damage and yield based on charge
        double baseDamage = this.plugin.getConfig().getDouble("abilities.damage.fire-fireball", 8.0);
        double damageMultiplier = charge / 100.0;
        float yield = 1.0f + (charge / 50.0f); // 1.0 to 3.0

        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        Fireball fireball = (Fireball)player.getWorld().spawn(eyeLoc.clone().add(direction.clone().multiply(1.5)), Fireball.class);
        fireball.setShooter((ProjectileSource)player);
        fireball.setVelocity(direction.multiply(1.5 + (charge / 100.0))); // Speed based on charge
        fireball.setYield(yield);
        fireball.setIsIncendiary(true);

        // Visual feedback based on charge
        int particles = 20 + (charge / 2);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f + (charge / 200.0f));
        player.spawnParticle(Particle.FLAME, eyeLoc, particles, 0.5, 0.5, 0.5, 0.1);

        this.plugin.getAbilityManager().useAbility(player, "fire-fireball");
        player.sendMessage("\u00a76\u00a7oFired at " + charge + "% power!");
    }

    public void cancelCharging(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = chargingTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        chargingPlayers.remove(uuid);
    }

    public void cozyCampfire(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }

        String abilityKey = "fire-campfire";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        UUID uuid = player.getUniqueId();

        // Remove existing campfire if player has one
        if (activeCampfires.containsKey(uuid)) {
            removeCampfire(player);
        }

        // Get the block at player's feet
        Block targetBlock = player.getLocation().getBlock();

        // Find suitable location for campfire
        if (targetBlock.getType() != Material.AIR) {
            targetBlock = targetBlock.getRelative(0, 1, 0);
        }

        // Check if we can place a block there
        if (targetBlock.getType() != Material.AIR && targetBlock.getType() != Material.CAVE_AIR) {
            player.sendMessage("\u00a7c\u00a7oCannot place campfire here!");
            return;
        }

        // Place the campfire block
        Material previousMaterial = targetBlock.getType();
        targetBlock.setType(Material.CAMPFIRE);
        Location campfireLocation = targetBlock.getLocation().clone();

        // Store campfire location
        activeCampfires.put(uuid, campfireLocation);

        // Get config values
        double radius = this.plugin.getConfig().getDouble("abilities.fire-campfire.radius", 5.0);
        double damage = this.plugin.getConfig().getDouble("abilities.damage.fire-campfire", 2.0);
        int burnDuration = this.plugin.getConfig().getInt("abilities.fire-campfire.burn-duration", 3);
        int duration = this.plugin.getConfig().getInt("abilities.durations.fire-campfire", 60) * 20; // Convert to ticks

        // Play placement sound
        player.playSound(campfireLocation, Sound.BLOCK_CAMPFIRE_CRACKLE, 1.0f, 1.0f);
        player.sendMessage("\u00a76\u00a7oPlaced Campfire! Heals you and burns enemies for 1 minute.");

        // Create campfire effect task
        BukkitTask campfireTask = new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                // Check if campfire still exists
                Block currentBlock = campfireLocation.getBlock();
                if (currentBlock.getType() != Material.CAMPFIRE) {
                    // Campfire was broken
                    activeCampfires.remove(uuid);
                    campfireTasks.remove(uuid);
                    player.sendMessage("\u00a76\u00a7oCampfire was destroyed!");
                    this.cancel();
                    return;
                }

                if (ticksElapsed >= duration) {
                    // Duration expired - remove campfire
                    removeCampfireBlock(campfireLocation);
                    activeCampfires.remove(uuid);
                    campfireTasks.remove(uuid);
                    player.sendMessage("\u00a76\u00a7oCampfire expired!");
                    this.cancel();
                    return;
                }

                // Every second (20 ticks), apply effects
                if (ticksElapsed % 20 == 0) {
                    // Give Regeneration 4 to trusted players and the caster if in range
                    for (Entity entity : campfireLocation.getWorld().getNearbyEntities(campfireLocation, radius, radius, radius)) {
                        if (!(entity instanceof Player)) continue;
                        Player nearby = (Player) entity;

                        if (nearby.equals(player) || plugin.getTrustedPlayersManager().isTrusted(player, nearby)) {
                            // Heal caster and trusted allies with Regeneration IV
                            nearby.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 3)); // Regen IV for 2 seconds
                        }
                    }

                    // Damage and burn enemies in radius
                    for (Entity entity : campfireLocation.getWorld().getNearbyEntities(campfireLocation, radius, radius, radius)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity target = (LivingEntity) entity;

                            // Don't damage trusted players (friendly fire prevention)
                            if (entity instanceof Player) {
                                Player targetPlayer = (Player) entity;
                                if (plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                                    continue; // Skip trusted players
                                }
                            }

                            // Deal damage
                            target.damage(damage, player);

                            // Set on fire
                            target.setFireTicks(burnDuration * 20);

                            // Fire particles on damaged entity
                            target.getWorld().spawnParticle(Particle.FLAME,
                                target.getLocation().add(0, 1, 0),
                                10, 0.3, 0.5, 0.3, 0.02);
                        }
                    }

                    // Play crackling sound periodically
                    campfireLocation.getWorld().playSound(campfireLocation, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.5f, 1.0f);
                }

                // MASSIVE VISIBLE CIRCLE showing campfire radius with Fire gem orange color
                if (ticksElapsed % 5 == 0) { // Every 5 ticks for more visibility
                    // DENSE fire ring to show radius - MANY MORE PARTICLES
                    int circlePoints = 48; // Much denser circle
                    Particle.DustOptions orangeDust = new Particle.DustOptions(ParticleUtils.FIRE_ORANGE, 1.0f);
                    for (int i = 0; i < circlePoints; i++) {
                        double angle = (i / (double) circlePoints) * 2 * Math.PI;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;

                        // Ground level - Fire orange dust particles
                        campfireLocation.getWorld().spawnParticle(Particle.DUST,
                            campfireLocation.clone().add(x, 0.3, z),
                            3, 0.1, 0.1, 0.1, 0.0, orangeDust, true);

                        // Mid level - FLAME particles
                        campfireLocation.getWorld().spawnParticle(Particle.FLAME,
                            campfireLocation.clone().add(x, 0.8, z),
                            2, 0.1, 0.1, 0.1, 0.01);

                        // Higher level - LAVA particles for visibility
                        if (i % 4 == 0) { // Every 4th point
                            campfireLocation.getWorld().spawnParticle(Particle.LAVA,
                                campfireLocation.clone().add(x, 1.2, z),
                                1, 0, 0, 0, 0);
                        }
                    }

                    // Extra center particles for visibility
                    campfireLocation.getWorld().spawnParticle(Particle.FLAME,
                        campfireLocation.clone().add(0.5, 1.0, 0.5),
                        15, 0.3, 0.5, 0.3, 0.02);
                    campfireLocation.getWorld().spawnParticle(Particle.LAVA,
                        campfireLocation.clone().add(0.5, 0.5, 0.5),
                        8, 0.5, 0.2, 0.5, 0);
                }

                ticksElapsed++;
            }
        }.runTaskTimer(this.plugin, 0L, 1L);

        campfireTasks.put(uuid, campfireTask);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);
    }

    private void removeCampfireBlock(Location location) {
        Block block = location.getBlock();
        if (block.getType() == Material.CAMPFIRE) {
            block.setType(Material.AIR);
            location.getWorld().playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
        }
    }

    public void removeCampfire(Player player) {
        UUID uuid = player.getUniqueId();

        // Cancel task
        BukkitTask task = campfireTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        // Remove block
        Location loc = activeCampfires.remove(uuid);
        if (loc != null) {
            removeCampfireBlock(loc);
        }
    }

    // ========================================================================
    // CRISP — Tier 2 Tertiary
    // Evaporates all water in range and replaces surrounding blocks with nether blocks
    // ========================================================================

    public void crisp(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }

        String abilityKey = "fire-crisp";

        if (isCrispActive(player)) {
            player.sendMessage("\u00a7c\u00a7oCrisp is already active!");
            return;
        }

        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Location center = player.getLocation().clone();
        int radius = this.plugin.getConfig().getInt("abilities.fire-crisp.radius", 10);
        int durationSeconds = this.plugin.getConfig().getInt("abilities.durations.fire-crisp", 15);
        int duration = durationSeconds * 20; // ticks

        crispActivePlayers.add(uuid);

        // Activation effects
        player.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 1.5f, 0.5f);
        player.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.7f);
        player.sendMessage("\u00a76\u00a7lCrisp! \u00a7eEvaporating water and scorching the earth for " + durationSeconds + "s!");

        // --- Initial pass: replace water and surrounding blocks ---
        evaporateAndScorch(center, radius);

        // Activation visual — expanding ring of fire
        for (int i = 0; i < 36; i++) {
            double angle = (i / 36.0) * 2 * Math.PI;
            for (double r = 0; r <= radius; r += 0.5) {
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;
                center.getWorld().spawnParticle(Particle.FLAME,
                    center.clone().add(x, 0.3, z), 1, 0.05, 0.1, 0.05, 0.02);
            }
        }
        Particle.DustOptions orangeDust = new Particle.DustOptions(ParticleUtils.FIRE_ORANGE, 1.5f);
        for (int i = 0; i < 48; i++) {
            double angle = (i / 48.0) * 2 * Math.PI;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            center.getWorld().spawnParticle(Particle.DUST,
                center.clone().add(x, 0.5, z), 5, 0.1, 0.3, 0.1, 0.0, orangeDust, true);
        }

        // Ongoing task — keep evaporating water placed after activation
        BukkitTask crispTask = new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticksElapsed >= duration) {
                    endCrisp(player);
                    this.cancel();
                    return;
                }

                ticksElapsed++;

                // Every 10 ticks (0.5s), sweep for new water blocks placed in the area
                if (ticksElapsed % 10 == 0) {
                    Location playerLoc = player.getLocation();
                    evaporateWater(playerLoc, radius);

                    // Ambient effects: small lava/flame particles on scorched ground
                    if (ticksElapsed % 20 == 0) {
                        for (int i = 0; i < 8; i++) {
                            double angle = random.nextDouble() * 2 * Math.PI;
                            double r = random.nextDouble() * radius;
                            double x = Math.cos(angle) * r;
                            double z = Math.sin(angle) * r;
                            playerLoc.getWorld().spawnParticle(Particle.LAVA,
                                playerLoc.clone().add(x, 0.2, z), 1, 0.1, 0.1, 0.1, 0);
                            playerLoc.getWorld().spawnParticle(Particle.FLAME,
                                playerLoc.clone().add(x, 0.3, z), 1, 0.1, 0.1, 0.1, 0.01);
                        }
                        playerLoc.getWorld().playSound(playerLoc, Sound.BLOCK_FIRE_AMBIENT, 0.4f, 0.8f);
                    }
                }
            }
        }.runTaskTimer(this.plugin, 0L, 1L);

        crispTasks.put(uuid, crispTask);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
    }

    /**
     * Evaporates all water blocks in radius and replaces surrounding solid blocks with nether blocks.
     */
    private void evaporateAndScorch(Location center, int radius) {
        evaporateWater(center, radius);

        // Replace surface-level solid blocks around center with nether blocks
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) continue; // Circle check
                Location loc = center.clone().add(x, 0, z);
                // Find the surface block
                Block surface = loc.getWorld().getHighestBlockAt(loc);
                if (surface.getType().isSolid() && !surface.getType().name().startsWith("NETHER") &&
                        surface.getType() != Material.MAGMA_BLOCK && surface.getType() != Material.SOUL_SAND) {
                    // Replace with random nether block
                    Material netherMat = NETHER_BLOCKS[random.nextInt(NETHER_BLOCKS.length)];
                    surface.setType(netherMat);
                }
            }
        }
    }

    /**
     * Evaporates (removes) all water source and flowing water blocks in the area.
     */
    private void evaporateWater(Location center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radius * radius) continue;
                    Block block = center.clone().add(x, y, z).getBlock();
                    if (block.getType() == Material.WATER || block.getType() == Material.BUBBLE_COLUMN ||
                            block.getType() == Material.KELP || block.getType() == Material.KELP_PLANT ||
                            block.getType() == Material.SEAGRASS || block.getType() == Material.TALL_SEAGRASS) {
                        // Spawn steam effect before removing
                        center.getWorld().spawnParticle(Particle.CLOUD,
                            block.getLocation().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.02);
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    private void endCrisp(Player player) {
        UUID uuid = player.getUniqueId();
        crispActivePlayers.remove(uuid);

        BukkitTask task = crispTasks.remove(uuid);
        if (task != null) task.cancel();

        if (player.isOnline()) {
            player.sendMessage("\u00a76\u00a7oCrisp faded.");
        }
    }

    // ========================================================================
    // METEOR SHOWER — Tier 2 Quaternary
    // Rains fire on a target area
    // ========================================================================

    public void meteorShower(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }

        String abilityKey = "fire-meteor-shower";

        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        UUID uuid = player.getUniqueId();

        // Target: where the player is looking (up to 50 blocks, landing on ground)
        Location target = getGroundTarget(player, 50);
        if (target == null) {
            player.sendMessage("\u00a7c\u00a7oNo valid target area found!");
            return;
        }

        int durationSeconds = this.plugin.getConfig().getInt("abilities.durations.fire-meteor-shower", 8);
        int duration = durationSeconds * 20;
        double aoeRadius = this.plugin.getConfig().getDouble("abilities.fire-meteor-shower.radius", 8.0);
        double damage = this.plugin.getConfig().getDouble("abilities.damage.fire-meteor-shower", 5.0);
        int meteorInterval = this.plugin.getConfig().getInt("abilities.fire-meteor-shower.interval-ticks", 10);

        meteorShowersActive.add(uuid);

        // Announcement
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.3f);
        player.sendMessage("\u00a76\u00a7lMeteor Shower! \u00a7eFire rains down for " + durationSeconds + "s!");

        // Show target radius visually
        Particle.DustOptions orangeDust = new Particle.DustOptions(ParticleUtils.FIRE_ORANGE, 1.2f);
        for (int i = 0; i < 48; i++) {
            double angle = (i / 48.0) * 2 * Math.PI;
            double x = Math.cos(angle) * aoeRadius;
            double z = Math.sin(angle) * aoeRadius;
            target.getWorld().spawnParticle(Particle.DUST,
                target.clone().add(x, 0.5, z), 3, 0.1, 0.2, 0.1, 0.0, orangeDust, true);
        }

        final Location finalTarget = target.clone();

        BukkitTask meteorTask = new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticksElapsed >= duration) {
                    meteorShowersActive.remove(uuid);
                    meteorTasks.remove(uuid);
                    if (player.isOnline()) {
                        player.sendMessage("\u00a76\u00a7oMeteor Shower ended.");
                    }
                    this.cancel();
                    return;
                }

                ticksElapsed++;

                // Spawn a meteor every meteorInterval ticks
                if (ticksElapsed % meteorInterval == 0) {
                    // Random position within AoE radius
                    double angle = random.nextDouble() * 2 * Math.PI;
                    double r = Math.sqrt(random.nextDouble()) * aoeRadius; // sqrt for uniform distribution
                    double mx = Math.cos(angle) * r;
                    double mz = Math.sin(angle) * r;

                    // Spawn high above the target
                    int spawnHeight = 20 + random.nextInt(10);
                    Location spawnLoc = finalTarget.clone().add(mx, spawnHeight, mz);
                    Location impactLoc = finalTarget.clone().add(mx, 0, mz);

                    // Find actual ground level at impact
                    Block groundBlock = finalTarget.getWorld().getHighestBlockAt(impactLoc);
                    impactLoc.setY(groundBlock.getY() + 0.5);

                    // Create meteor trail — particles falling down
                    Vector trajectory = impactLoc.toVector().subtract(spawnLoc.toVector()).normalize();

                    // Draw particle trail from spawn to impact
                    plugin.getServer().getScheduler().runTaskLater((Plugin) plugin, () -> {
                        if (!player.isOnline()) return;

                        // Particle trail
                        double totalDist = spawnLoc.distance(impactLoc);
                        for (double d = 0; d < totalDist; d += 0.8) {
                            Location trailLoc = spawnLoc.clone().add(trajectory.clone().multiply(d));
                            finalTarget.getWorld().spawnParticle(Particle.FLAME,
                                trailLoc, 3, 0.1, 0.1, 0.1, 0.03);
                            finalTarget.getWorld().spawnParticle(Particle.LAVA,
                                trailLoc, 1, 0.05, 0.05, 0.05, 0);
                        }

                        // Impact explosion visuals
                        finalTarget.getWorld().spawnParticle(Particle.FLAME,
                            impactLoc, 40, 1.0, 0.5, 1.0, 0.1);
                        finalTarget.getWorld().spawnParticle(Particle.LAVA,
                            impactLoc, 15, 0.5, 0.2, 0.5, 0);
                        finalTarget.getWorld().spawnParticle(Particle.EXPLOSION,
                            impactLoc, 3, 0.3, 0.1, 0.3, 0.0);
                        finalTarget.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                            impactLoc, 10, 0.5, 0.3, 0.5, 0.05);

                        // Impact sound
                        finalTarget.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.5f);
                        finalTarget.getWorld().playSound(impactLoc, Sound.BLOCK_LAVA_POP, 1.0f, 0.8f);

                        // Damage entities in small impact radius
                        double impactRadius = 2.5;
                        for (Entity entity : finalTarget.getWorld().getNearbyEntities(impactLoc, impactRadius, impactRadius, impactRadius)) {
                            if (!(entity instanceof LivingEntity)) continue;
                            if (entity == player) continue;

                            LivingEntity livingTarget = (LivingEntity) entity;

                            // Skip trusted players
                            if (entity instanceof Player) {
                                Player targetPlayer = (Player) entity;
                                if (plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                                    continue;
                                }
                            }

                            livingTarget.damage(damage, player);
                            livingTarget.setFireTicks(60); // 3 seconds on fire

                            livingTarget.getWorld().spawnParticle(Particle.FLAME,
                                livingTarget.getLocation().add(0, 1, 0),
                                15, 0.4, 0.6, 0.4, 0.05);
                        }

                        // Set ground blocks on fire if exposed to sky
                        Block impactBlock = impactLoc.getBlock();
                        if (impactBlock.getType() == Material.AIR || impactBlock.getType() == Material.CAVE_AIR) {
                            Block below = impactBlock.getRelative(0, -1, 0);
                            if (below.getType().isSolid()) {
                                impactBlock.setType(Material.FIRE);
                            }
                        }

                    }, 0L);
                }

                // Ambient "incoming" warning — ring of particles showing AoE boundary every 20 ticks
                if (ticksElapsed % 20 == 0) {
                    Particle.DustOptions warningDust = new Particle.DustOptions(ParticleUtils.FIRE_ORANGE, 0.8f);
                    for (int i = 0; i < 24; i++) {
                        double angle = (i / 24.0) * 2 * Math.PI;
                        double x = Math.cos(angle) * aoeRadius;
                        double z = Math.sin(angle) * aoeRadius;
                        finalTarget.getWorld().spawnParticle(Particle.DUST,
                            finalTarget.clone().add(x, 0.3, z), 2, 0.1, 0.1, 0.1, 0.0, warningDust, true);
                    }
                }
            }
        }.runTaskTimer(this.plugin, 0L, 1L);

        meteorTasks.put(uuid, meteorTask);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
    }

    /**
     * Finds the ground location where the player is looking, up to maxDistance blocks away.
     */
    private Location getGroundTarget(Player player, int maxDistance) {
        var result = player.getWorld().rayTraceBlocks(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            maxDistance
        );

        if (result != null && result.getHitBlock() != null) {
            return result.getHitBlock().getLocation().add(0.5, 1, 0.5);
        }

        // Fallback: project forward and find ground
        Location projected = player.getEyeLocation().add(
            player.getEyeLocation().getDirection().multiply(20)
        );
        Block highest = player.getWorld().getHighestBlockAt(projected);
        return highest.getLocation().add(0.5, 1, 0.5);
    }

    // Clean up when player leaves
    public void cleanup(Player player) {
        cancelCharging(player);
        removeCampfire(player);
        endCrisp(player);

        UUID uuid = player.getUniqueId();
        meteorShowersActive.remove(uuid);
        BukkitTask meteorTask = meteorTasks.remove(uuid);
        if (meteorTask != null) meteorTask.cancel();
    }
}
