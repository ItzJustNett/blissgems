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
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.ArrayList;
import java.util.List;

public class FluxAbilities {
    private final BlissGems plugin;

    public FluxAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    public void onRightClick(Player player, int tier) {
        if (tier == 2 && player.isSneaking()) {
            this.chainLightning(player);
        } else {
            this.ground(player);
        }
    }

    public void ground(Player player) {
        String abilityKey = "flux-ground";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        int duration = this.plugin.getConfigManager().getAbilityDuration("flux-ground-freeze");
        for (Entity entity : player.getNearbyEntities(8.0, 8.0, 8.0)) {
            if (!(entity instanceof LivingEntity)) continue;
            LivingEntity target = (LivingEntity)entity;
            if (entity instanceof Player) continue;
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration * 20, 255, false, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration * 20, 255, false, true));
            target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5);
        }
        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 50, 2.0, 2.0, 2.0);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Ground"));
    }

    public void shockingArrows(Player player) {
        player.sendMessage("\u00a7b\u00a7lShocking Arrows \u00a77(Passive - shoots electric arrows)");
    }

    public void chainLightning(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "flux-chain-lightning";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        double damage = this.plugin.getConfig().getDouble("abilities.damage.flux-chain-lightning", 6.0);
        double chainRadius = this.plugin.getConfig().getDouble("abilities.flux-chain-lightning.chain-radius", 5.0);
        int maxChains = this.plugin.getConfig().getInt("abilities.flux-chain-lightning.max-chains", 5);

        // Find initial target
        LivingEntity target = this.getTargetEntity(player, 15);
        if (target == null) {
            player.sendMessage("\u00a7cNo target found!");
            return;
        }

        List<LivingEntity> hitEntities = new ArrayList<>();
        LivingEntity currentTarget = target;
        hitEntities.add(currentTarget);

        // Initial hit
        currentTarget.damage(damage, (Entity)player);
        currentTarget.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, currentTarget.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5);

        // Draw line from player to first target
        drawLightningLine(player.getEyeLocation(), currentTarget.getLocation().add(0, 1, 0));

        // Chain to nearby entities
        for (int i = 0; i < maxChains - 1; i++) {
            LivingEntity nextTarget = null;
            double closestDistance = chainRadius;

            for (Entity entity : currentTarget.getNearbyEntities(chainRadius, chainRadius, chainRadius)) {
                if (!(entity instanceof LivingEntity)) continue;
                if (entity instanceof Player) continue;
                if (hitEntities.contains(entity)) continue;

                double distance = entity.getLocation().distance(currentTarget.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    nextTarget = (LivingEntity) entity;
                }
            }

            if (nextTarget == null) break;

            // Draw lightning line between targets
            drawLightningLine(currentTarget.getLocation().add(0, 1, 0), nextTarget.getLocation().add(0, 1, 0));

            // Damage next target (reduced damage for chains)
            double chainDamage = damage * 0.7;
            nextTarget.damage(chainDamage, (Entity)player);
            nextTarget.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, nextTarget.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3);

            hitEntities.add(nextTarget);
            currentTarget = nextTarget;
        }

        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.5f);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Chain Lightning"));
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

