/*
 * Fire Gem Abilities
 * - Charged Fireball (Tier 1+2): 15 second charge with visual particles
 * - Campfire (Tier 2): Places campfire block that burns enemies and heals caster
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
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
import java.util.Map;
import java.util.UUID;

public class FireAbilities {
    private final BlissGems plugin;
    private final Map<UUID, Integer> chargingPlayers = new HashMap<>();
    private final Map<UUID, BukkitTask> chargingTasks = new HashMap<>();
    private final Map<UUID, Location> activeCampfires = new HashMap<>();
    private final Map<UUID, BukkitTask> campfireTasks = new HashMap<>();
    private static final int MAX_CHARGE = 100;
    private static final int CHARGE_DURATION_TICKS = 300; // 15 seconds

    public FireAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    public boolean isCharging(Player player) {
        return chargingPlayers.containsKey(player.getUniqueId());
    }

    public int getCharge(Player player) {
        return chargingPlayers.getOrDefault(player.getUniqueId(), 0);
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

                    // Orange dust particles around player in a larger radius - LOW
                    player.getWorld().spawnParticle(Particle.DUST,
                        playerLoc.clone().add(0, 0.5, 0), // Lower than waist
                        particleCount, 1.5, 0.4, 1.5, // Wide spread, low vertical
                        new Particle.DustOptions(org.bukkit.Color.ORANGE, 1.5f));

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
                    // Give Regeneration 2 to the caster if in range
                    if (player.isOnline() && player.getLocation().distance(campfireLocation) <= radius) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1)); // Regen 2 for 2 seconds
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

                // MASSIVE VISIBLE CIRCLE showing campfire radius
                if (ticksElapsed % 5 == 0) { // Every 5 ticks for more visibility
                    // DENSE fire ring to show radius - MANY MORE PARTICLES
                    int circlePoints = 48; // Much denser circle
                    for (int i = 0; i < circlePoints; i++) {
                        double angle = (i / (double) circlePoints) * 2 * Math.PI;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;

                        // Ground level - FLAME particles
                        campfireLocation.getWorld().spawnParticle(Particle.FLAME,
                            campfireLocation.clone().add(x, 0.3, z),
                            3, 0.1, 0.1, 0.1, 0.01);

                        // Mid level - SOUL FIRE particles
                        campfireLocation.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
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

    // Clean up when player leaves
    public void cleanup(Player player) {
        cancelCharging(player);
        removeCampfire(player);
    }
}
