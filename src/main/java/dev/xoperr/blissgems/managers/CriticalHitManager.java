/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.entity.Player
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.ParticleUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class CriticalHitManager {
    private final BlissGems plugin;
    private final Map<UUID, Integer> hitCounts = new HashMap<UUID, Integer>();

    public CriticalHitManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    public boolean registerHit(Player player) {
        int threshold;
        UUID uuid = player.getUniqueId();
        int count = this.hitCounts.merge(uuid, 1, Integer::sum);
        if (count >= (threshold = this.plugin.getConfig().getInt("abilities.strength-chad.hit-threshold", 4))) {
            this.hitCounts.put(uuid, 0);
            Particle.DustOptions redDust = new Particle.DustOptions(ParticleUtils.STRENGTH_RED, 2.0f);
            player.spawnParticle(Particle.CRIT, player.getLocation().add(0.0, 1.0, 0.0), 50, 0.5, 0.5, 0.5, 0.3);
            player.spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.1);
            player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 20, 0.5, 0.5, 0.5, 0.0, (Object)redDust, true);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.7f);
            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 2.0f);
            return true;
        }
        if (count == threshold - 1) {
            player.spawnParticle(Particle.CRIT, player.getLocation().add(0.0, 1.0, 0.0), 15, 0.3, 0.5, 0.3, 0.1);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.8f);
        }
        return false;
    }

    public int getHitCount(Player player) {
        return this.hitCounts.getOrDefault(player.getUniqueId(), 0);
    }

    public void resetHitCount(Player player) {
        this.hitCounts.remove(player.getUniqueId());
    }

    public void clearHitData(UUID playerId) {
        this.hitCounts.remove(playerId);
    }
}

