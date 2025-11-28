/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
package fun.obriy.blissgems.abilities;

import fun.obriy.blissgems.BlissGems;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class FluxAbilities {
    private final BlissGems plugin;
    private static final Set<UUID> stunnedPlayers = new HashSet<>();

    public FluxAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    public static boolean isPlayerStunned(UUID playerId) {
        return stunnedPlayers.contains(playerId);
    }

    public void onRightClick(Player player, int tier) {
        if (tier == 2 && player.isSneaking()) {
            this.fluxRay(player);
        } else {
            this.ground(player);
        }
    }

    public void ground(Player player) {
        String abilityKey = "flux-ground";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        // Stun duration: 5 seconds (100 ticks)
        int stunDuration = 100;

        // Find target player
        LivingEntity target = this.getTargetEntity(player, 20);
        if (target == null || !(target instanceof Player)) {
            player.sendMessage("§cNo player target found!");
            return;
        }

        Player targetPlayer = (Player) target;

        // Apply stun effects
        targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, stunDuration, 255, false, true));
        targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, stunDuration, -10, false, true));
        targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, stunDuration, 255, false, true));
        targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, stunDuration, 255, false, true));

        // Add to stunned players list
        stunnedPlayers.add(targetPlayer.getUniqueId());

        // Remove from stunned list after 5 seconds
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            stunnedPlayers.remove(targetPlayer.getUniqueId());
        }, stunDuration);

        // Visual effects
        targetPlayer.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, targetPlayer.getLocation().add(0.0, 1.0, 0.0), 50, 0.5, 1.0, 0.5);
        targetPlayer.playSound(targetPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);

        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.2f);
        player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 30, 1.0, 1.0, 1.0);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Ground"));
        targetPlayer.sendMessage("§c§lYou have been stunned for 5 seconds!");
    }

    public void shockingArrows(Player player) {
        player.sendMessage("\u00a7b\u00a7lShocking Arrows \u00a77(Passive - shoots electric arrows)");
    }

    public void fluxRay(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("§c§oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "flux-ray";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        // Flux Ray configuration
        int rayRange = 20;
        int duration = this.plugin.getConfig().getInt("abilities.durations.flux-ray", 8); // 8 seconds default
        double damagePerTick = 0.5 / 20.0; // 0.5 hearts per second = 1 HP per second = 0.05 HP per tick

        Location start = player.getEyeLocation();

        // Play initial sound
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.5f);

        // Create a repeating task that runs every tick for the duration
        new BukkitRunnable() {
            int ticksElapsed = 0;
            final int maxTicks = duration * 20; // Convert seconds to ticks

            @Override
            public void run() {
                if (ticksElapsed >= maxTicks) {
                    this.cancel();
                    return;
                }

                // Get current eye location (updates if player moves)
                Location currentStart = player.getEyeLocation();

                // Find all entities in the ray path
                Set<LivingEntity> hitEntities = new HashSet<>();
                for (int i = 1; i <= rayRange; i++) {
                    Location point = currentStart.clone().add(currentStart.getDirection().multiply(i));

                    // Spawn particles along the ray
                    if (ticksElapsed % 2 == 0) { // Reduce particle spam
                        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 2, 0.1, 0.1, 0.1, 0.01);
                        player.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.05, 0.05, 0.05, 0);
                    }

                    // Check for entities at this point
                    for (Entity entity : player.getWorld().getNearbyEntities(point, 0.5, 0.5, 0.5)) {
                        if (entity instanceof LivingEntity && entity != player && !hitEntities.contains(entity)) {
                            hitEntities.add((LivingEntity) entity);
                        }
                    }
                }

                // Damage all hit entities
                for (LivingEntity target : hitEntities) {
                    target.damage(damagePerTick * 20, player); // Damage for this tick
                    if (ticksElapsed % 10 == 0) { // Show hit particle every 0.5 seconds
                        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3);
                    }
                }

                ticksElapsed++;
            }
        }.runTaskTimer(this.plugin, 0L, 1L);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Flux Ray"));
    }

    private void drawLightningLine(Location from, Location to) {
        double distance = from.distance(to);
        int particles = (int) (distance * 4);

        for (int i = 0; i <= particles; i++) {
            double ratio = (double) i / particles;
            double x = from.getX() + (to.getX() - from.getX()) * ratio;
            double y = from.getY() + (to.getY() - from.getY()) * ratio;
            double z = from.getZ() + (to.getZ() - from.getZ()) * ratio;

            from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, x, y, z, 1, 0.05, 0.05, 0.05, 0);
        }
    }

    private LivingEntity getTargetEntity(Player player, int range) {
        return player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), (double)range, entity -> entity instanceof LivingEntity && entity != player) != null ? (LivingEntity)player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), (double)range, entity -> entity instanceof LivingEntity && entity != player).getHitEntity() : null;
    }
}

