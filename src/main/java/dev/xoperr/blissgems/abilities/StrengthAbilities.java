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

public class StrengthAbilities {
    private final BlissGems plugin;

    public StrengthAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    public void onRightClick(Player player, int tier) {
        if (tier == 2 && player.isSneaking()) {
            this.chadStrength(player);
        } else if (tier == 2) {
            // Tier 2 without sneak: Try frailerPower first (single target), fallback to bloodthorns
            LivingEntity target = this.getTargetEntity(player, 15);
            if (target != null) {
                this.frailerPower(player);
            } else {
                this.bloodthorns(player);
            }
        } else {
            // Tier 1: Only bloodthorns
            this.bloodthorns(player);
        }
    }

    public void bloodthorns(Player player) {
        String abilityKey = "strength-bloodthorns";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        double healthPercent = player.getHealth() / player.getMaxHealth();
        double baseDamage = this.plugin.getConfigManager().getAbilityDamage("strength-bloodthorns");
        double damage = baseDamage * healthPercent;

        int hitCount = 0;
        for (Entity entity : player.getNearbyEntities(5.0, 5.0, 5.0)) {
            if (!(entity instanceof LivingEntity)) continue;
            LivingEntity target = (LivingEntity)entity;

            // Skip trusted players (friendly fire prevention)
            if (entity instanceof Player) {
                Player targetPlayer = (Player) entity;
                if (this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                    continue;
                }
            }

            // FIXED: Now damages BOTH players AND mobs!
            target.damage(damage, (Entity)player);

            // MASSIVE particle effects on hit
            target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0.0, 1.0, 0.0), 20, 0.7, 0.7, 0.7);
            target.getWorld().spawnParticle(Particle.CRIMSON_SPORE, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5);
            target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0.0, 1.0, 0.0), 5, 0.5, 0.5, 0.5);
            hitCount++;
        }

        // Visual effects on caster - MASSIVE
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, 1.0f, 0.8f);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.2f);
        player.spawnParticle(Particle.CRIMSON_SPORE, player.getLocation().add(0.0, 1.0, 0.0), 100, 2.5, 2.5, 2.5);
        player.spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0.0, 1.0, 0.0), 20, 2.0, 0.5, 2.0);

        // Circle effect showing AoE range
        for (int i = 0; i < 16; i++) {
            double angle = (i / 16.0) * 2 * Math.PI;
            double x = Math.cos(angle) * 5.0;
            double z = Math.sin(angle) * 5.0;
            player.getWorld().spawnParticle(Particle.CRIMSON_SPORE,
                player.getLocation().add(x, 0.5, z),
                3, 0.1, 0.1, 0.1, 0);
        }

        this.plugin.getAbilityManager().useAbility(player, abilityKey);

        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Bloodthorns");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg + " §7(" + hitCount + " targets hit!)");
        }
    }

    public void frailerPower(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "strength-frailer";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        LivingEntity target = this.getTargetEntity(player, 15);
        if (target == null) {
            player.sendMessage("\u00a7cNo target found!");
            return;
        }
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 2, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1, false, true));
        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 0.8f);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Frailer Power"));
    }

    public void chadStrength(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "strength-chad";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 1, false, true));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f);
        player.spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation().add(0.0, 2.0, 0.0), 30, 0.5, 0.5, 0.5);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Chad Strength"));
    }

    private LivingEntity getTargetEntity(Player player, int range) {
        return player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), (double)range, entity -> entity instanceof LivingEntity && entity != player) != null ? (LivingEntity)player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), (double)range, entity -> entity instanceof LivingEntity && entity != player).getHitEntity() : null;
    }
}

