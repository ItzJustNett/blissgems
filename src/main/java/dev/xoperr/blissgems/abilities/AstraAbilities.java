/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.GameMode
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.util.Vector
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AstraAbilities {
    private final BlissGems plugin;
    private final Map<UUID, Location> projectionOrigins = new HashMap<>();
    private final Map<UUID, GameMode> previousGameModes = new HashMap<>();

    public AstraAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    public void onRightClick(Player player, int tier) {
        if (tier == 2) {
            // Tier 2: Alternate between daggers and projection (sneak for projection)
            if (player.isSneaking()) {
                this.astralProjection(player);
            } else {
                this.astralDaggers(player);
            }
        } else {
            this.astralDaggers(player);
        }
    }

    public boolean isInProjection(Player player) {
        return projectionOrigins.containsKey(player.getUniqueId());
    }

    public void astralDaggers(Player player) {
        String abilityKey = "astra-daggers";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        for (int i = 0; i < 3; ++i) {
            int index = i;
            this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                Location start = eyeLoc.clone().add(direction.clone().multiply(1));
                Vector spread = direction.clone().rotateAroundAxis(new Vector(0, 1, 0), Math.toRadians(index * 15 - 15));
                for (int j = 0; j < 30; ++j) {
                    Location current = start.clone().add(spread.clone().multiply((double)j * 0.5));
                    // Enhanced particle trail - MORE PARTICLES
                    player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, current, 3, 0.1, 0.1, 0.1, 0.0);
                    player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, current, 2, 0.05, 0.05, 0.05, 0.01);
                    player.getWorld().spawnParticle(Particle.WITCH, current, 1, 0.0, 0.0, 0.0, 0.0);
                    for (Entity entity : current.getWorld().getNearbyEntities(current, 0.5, 0.5, 0.5)) {
                        if (!(entity instanceof LivingEntity)) continue;
                        LivingEntity target = (LivingEntity)entity;
                        if (entity == player) continue;

                        // Skip players you can't see OR trusted players
                        if (entity instanceof Player) {
                            Player targetPlayer = (Player) entity;
                            if (!player.canSee(targetPlayer) || plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                                continue;
                            }
                        }

                        double damage = this.plugin.getConfigManager().getAbilityDamage("astra-daggers");
                        target.damage(damage, (Entity)player);
                        // MASSIVE particle burst on hit - DOUBLED PARTICLES!
                        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0.0, 1.0, 0.0), 80, 0.9, 0.9, 0.9);
                        target.getWorld().spawnParticle(Particle.REVERSE_PORTAL, target.getLocation().add(0.0, 1.0, 0.0), 60, 0.7, 0.7, 0.7);
                        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0.0, 1.0, 0.0), 40, 0.7, 0.7, 0.7);
                        target.getWorld().spawnParticle(Particle.WITCH, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5);
                        target.getWorld().spawnParticle(Particle.END_ROD, target.getLocation().add(0.0, 1.0, 0.0), 25, 0.5, 0.5, 0.5);
                        return;
                    }
                    if (current.getBlock().getType().isSolid()) break;
                }
            }, (long)i * 5L);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.5f);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);

        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Astral Daggers");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    public void astralProjection(Player player) {
        String abilityKey = "astra-projection";

        // Check if already in projection - allow toggling off without cooldown check
        if (isInProjection(player)) {
            endProjection(player);
            return;
        }

        // Only check cooldown when STARTING projection, not when ending it
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

        // Enhanced particle effects - MORE PARTICLES
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, origin, 100, 0.8, 1.5, 0.8);
        player.getWorld().spawnParticle(Particle.PORTAL, origin, 80, 0.7, 1.2, 0.7);
        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, origin, 50, 0.5, 1.0, 0.5);
        player.getWorld().spawnParticle(Particle.WITCH, origin, 30, 0.5, 1.0, 0.5);
        player.playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
        player.playSound(origin, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.2f);

        // Get config values
        double maxRadius = this.plugin.getConfig().getDouble("abilities.astra-projection.radius", 150.0);
        int duration = this.plugin.getConfig().getInt("abilities.durations.astra-projection", 10) * 20; // Convert to ticks

        // Start monitoring task
        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !isInProjection(player)) {
                    // Cancel projection if player dies
                    if (player.isDead() && isInProjection(player)) {
                        endProjection(player);
                    }
                    this.cancel();
                    return;
                }

                ticksElapsed++;

                // Check distance from origin
                double distance = player.getLocation().distance(origin);
                if (distance > maxRadius) {
                    // Teleport back to edge of radius
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

        // DON'T set cooldown here - set it when projection ends so player can cancel early
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Astral Projection");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    public void endProjection(Player player) {
        if (!isInProjection(player)) {
            return;
        }

        Location origin = projectionOrigins.remove(player.getUniqueId());
        GameMode previousMode = previousGameModes.remove(player.getUniqueId());

        if (origin != null) {
            player.teleport(origin);
            // Enhanced return particle effects
            player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, origin, 100, 0.8, 1.5, 0.8);
            player.getWorld().spawnParticle(Particle.PORTAL, origin, 80, 0.7, 1.2, 0.7);
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, origin, 50, 0.5, 1.0, 0.5);
            player.getWorld().spawnParticle(Particle.END_ROD, origin, 40, 0.5, 1.0, 0.5);
            player.getWorld().spawnParticle(Particle.SOUL, origin, 30, 0.5, 1.0, 0.5);
            player.playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.playSound(origin, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.8f);
        }

        if (previousMode != null) {
            player.setGameMode(previousMode);
        } else {
            player.setGameMode(GameMode.SURVIVAL);
        }

        // Set cooldown ONLY when projection ends (not when it starts)
        // This allows player to cancel early without being blocked by cooldown
        this.plugin.getAbilityManager().useAbility(player, "astra-projection");

        String msg = this.plugin.getConfigManager().getFormattedMessage("projection-ended", new Object[0]);
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }
}

