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
 *  org.bukkit.util.Vector
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PuffAbilities {
    private final BlissGems plugin;
    private final Set<UUID> fallDamageImmune = new HashSet<>();

    public PuffAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    public boolean hasFallDamageImmunity(Player player) {
        return fallDamageImmune.contains(player.getUniqueId());
    }

    public void removeFallDamageImmunity(Player player) {
        fallDamageImmune.remove(player.getUniqueId());
    }

    public void onRightClick(Player player, int tier) {
        if (tier == 2 && player.isSneaking()) {
            this.breezyBash(player);
        } else {
            this.dash(player);
        }
    }

    public void dash(Player player) {
        String abilityKey = "puff-dash";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        Vector direction = player.getLocation().getDirection();
        direction.setY(0.3);
        direction.multiply(2.5);
        player.setVelocity(direction);
        int tier = this.plugin.getGemManager().getGemTier(player);
        if (tier >= 2) {
            this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                for (Entity entity : player.getNearbyEntities(2.0, 2.0, 2.0)) {
                    if (!(entity instanceof LivingEntity)) continue;
                    LivingEntity target = (LivingEntity)entity;
                    if (entity instanceof Player) continue;
                    double damage = this.plugin.getConfigManager().getAbilityDamage("puff-dash");
                    target.damage(damage, (Entity)player);
                }
            }, 5L);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.5f);
        player.spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Dash");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    public void breezyBash(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "puff-breezy-bash";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        Vector velocity = player.getVelocity();
        velocity.setY(2.0);
        player.setVelocity(velocity);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1.0f, 1.0f);
        player.spawnParticle(Particle.CLOUD, player.getLocation(), 50, 1.0, 1.0, 1.0, 0.2);

        // Add temporary fall damage immunity
        UUID uuid = player.getUniqueId();
        fallDamageImmune.add(uuid);

        // Remove immunity after 10 seconds
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            fallDamageImmune.remove(uuid);
        }, 200L);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Breezy Bash");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }
}

