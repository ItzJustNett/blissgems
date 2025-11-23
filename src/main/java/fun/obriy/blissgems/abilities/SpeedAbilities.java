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
        for (Entity entity : player.getNearbyEntities(10.0, 10.0, 10.0)) {
            if (!(entity instanceof LivingEntity)) continue;
            LivingEntity target = (LivingEntity)entity;
            if (entity instanceof Player && player.equals((Object)entity)) continue;
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 3, false, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 200, 2, false, true));
            target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0.0, 1.0, 0.0), 20, 0.5, 0.5, 0.5);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_HURT, 1.0f, 0.5f);
        player.spawnParticle(Particle.CLOUD, player.getLocation().add(0.0, 1.0, 0.0), 50, 3.0, 3.0, 3.0);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Sloth's Sedative"));
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
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 5, false, true));
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.5f);
        player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 100, 1.0, 1.0, 1.0);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Speed Storm"));
    }
}

