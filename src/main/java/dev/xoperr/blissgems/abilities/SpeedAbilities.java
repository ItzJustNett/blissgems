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
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpeedAbilities {
    private final BlissGems plugin;

    public SpeedAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    public void onRightClick(Player player, int tier) {
        if (tier == 2 && player.isSneaking()) {
            this.speedStorm(player);
        } else {
            this.slothSedative(player);
        }
    }

    public void slothSedative(Player player) {
        String abilityKey = "speed-sedative";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        int hitCount = 0;
        for (Entity entity : player.getNearbyEntities(10.0, 10.0, 10.0)) {
            if (!(entity instanceof LivingEntity)) continue;
            LivingEntity target = (LivingEntity)entity;
            if (entity instanceof Player && player.equals((Object)entity)) continue;

            // Skip trusted players (friendly fire prevention)
            if (entity instanceof Player) {
                Player targetPlayer = (Player) entity;
                if (this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                    continue;
                }
            }

            // Apply MASSIVE slowness debuff
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 3, false, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 200, 2, false, true));

            // MASSIVE particle effects on each target
            target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0.0, 1.0, 0.0), 40, 0.7, 0.7, 0.7);
            target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5);
            target.getWorld().spawnParticle(Particle.ASH, target.getLocation().add(0.0, 0.5, 0.0), 25, 0.5, 0.5, 0.5);
            hitCount++;
        }

        // Visual effects on caster
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_HURT, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 0.7f);
        player.spawnParticle(Particle.CLOUD, player.getLocation().add(0.0, 1.0, 0.0), 100, 3.5, 3.5, 3.5);
        player.spawnParticle(Particle.SMOKE, player.getLocation().add(0.0, 1.0, 0.0), 60, 3.0, 3.0, 3.0);

        // Circle showing AoE range
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * 2 * Math.PI;
            double x = Math.cos(angle) * 10.0;
            double z = Math.sin(angle) * 10.0;
            player.getWorld().spawnParticle(Particle.CLOUD,
                player.getLocation().add(x, 0.5, z),
                3, 0.1, 0.1, 0.1, 0);
        }

        this.plugin.getAbilityManager().useAbility(player, abilityKey);

        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Sloth's Sedative");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg + " §7(Slowed " + hitCount + " enemies!)");
        }
        player.sendMessage("§e§oSloth's Sedative slows all nearby enemies with Slowness IV!");
    }

    public void speedStorm(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "speed-storm";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        // Apply MASSIVE speed boost
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 5, false, true));

        // MASSIVE visual effects
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.8f);
        player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 200, 1.5, 1.5, 1.5);
        player.spawnParticle(Particle.GUST, player.getLocation().add(0.0, 1.0, 0.0), 50, 1.0, 1.0, 1.0);
        player.spawnParticle(Particle.END_ROD, player.getLocation().add(0.0, 1.0, 0.0), 40, 1.0, 1.0, 1.0);

        // Speed trail circles
        for (int i = 0; i < 16; i++) {
            double angle = (i / 16.0) * 2 * Math.PI;
            double x = Math.cos(angle) * 2.0;
            double z = Math.sin(angle) * 2.0;
            player.getWorld().spawnParticle(Particle.GUST,
                player.getLocation().add(x, 0.5, z),
                3, 0.1, 0.1, 0.1, 0.05);
        }

        this.plugin.getAbilityManager().useAbility(player, abilityKey);

        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Speed Storm");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        player.sendMessage("§a§oSpeed Storm grants you Speed VI for 10 seconds!");
    }
}

