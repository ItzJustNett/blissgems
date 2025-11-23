/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitTask
 */
package fun.obriy.blissgems.abilities;

import fun.obriy.blissgems.BlissGems;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class LifeAbilities {
    private final BlissGems plugin;

    public LifeAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    public void onRightClick(Player player, int tier) {
        if (tier == 2 && player.isSneaking()) {
            this.circleOfLife(player);
        } else {
            this.heartDrainer(player);
        }
    }

    public void heartDrainer(Player player) {
        String abilityKey = "life-drainer";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        LivingEntity target = this.getTargetEntity(player, 15);
        if (target == null) {
            player.sendMessage("\u00a7cNo target found!");
            return;
        }
        int duration = this.plugin.getConfigManager().getAbilityDuration("life-drainer");
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration * 20, 1, false, true));
        target.getWorld().spawnParticle(Particle.SCULK_SOUL, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 1.5f);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Heart Drainer"));
    }

    public void circleOfLife(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "life-circle-of-life";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        int duration = this.plugin.getConfigManager().getAbilityDuration("life-circle");
        int durationTicks = duration * 20;

        // Store the circle location
        final org.bukkit.Location circleLocation = player.getLocation().clone();
        final double radius = 8.0;

        final int[] ticksElapsed = {0};
        BukkitTask circleTask = this.plugin.getServer().getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            if (!player.isOnline() || ticksElapsed[0] >= durationTicks) {
                return;
            }

            // Check if owner is in the circle - give Regeneration 3
            if (player.getLocation().distance(circleLocation) <= radius) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 2, true, true));
            }

            // Apply Wither to all other entities in the circle
            for (Entity entity : circleLocation.getWorld().getNearbyEntities(circleLocation, radius, radius, radius)) {
                if (entity == player) continue;
                if (!(entity instanceof LivingEntity)) continue;

                LivingEntity target = (LivingEntity) entity;
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 1, false, true));
            }

            // Spawn particles at circle location
            player.getWorld().spawnParticle(Particle.HEART, circleLocation.clone().add(0, 1, 0), 10, 4.0, 0.5, 4.0);
            player.getWorld().spawnParticle(Particle.SCULK_SOUL, circleLocation.clone().add(0, 0.5, 0), 15, 4.0, 0.2, 4.0);

            ticksElapsed[0] += 20;
        }, 0L, 20L);

        // Cancel task after duration
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, circleTask::cancel, durationTicks);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Circle of Life"));
    }

    private LivingEntity getTargetEntity(Player player, int range) {
        return player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), (double)range, entity -> entity instanceof LivingEntity && entity != player) != null ? (LivingEntity)player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), (double)range, entity -> entity instanceof LivingEntity && entity != player).getHitEntity() : null;
    }
}

