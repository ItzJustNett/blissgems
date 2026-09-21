package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.ParticleUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Manages hit counting for Strength Gem's Chad Strength passive.
 * Every Nth hit (configurable, default 4) deals bonus damage.
 */
public class CriticalHitManager {
    private final BlissGems plugin;
    private final Map<UUID, Integer> hitCounts = new HashMap<>();

    public CriticalHitManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers a melee hit from a Strength gem player.
     * Returns true if this is the Nth hit (bonus damage should trigger).
     */
    public boolean registerHit(Player player) {
        UUID uuid = player.getUniqueId();
        int count = hitCounts.merge(uuid, 1, Integer::sum);
        int threshold = plugin.getConfig().getInt("abilities.strength-chad.hit-threshold", 4);

        if (count >= threshold) {
            hitCounts.put(uuid, 0);

            // Big visual effect for Chad Strength trigger
            Particle.DustOptions redDust = new Particle.DustOptions(ParticleUtils.STRENGTH_RED, 2.0f);
            player.spawnParticle(Particle.CRIT,
                player.getLocation().add(0, 1, 0),
                50, 0.5, 0.5, 0.5, 0.3);
            player.spawnParticle(Particle.ENCHANTED_HIT,
                player.getLocation().add(0, 1, 0),
                30, 0.5, 0.5, 0.5, 0.1);
            player.spawnParticle(Particle.DUST,
                player.getLocation().add(0, 1, 0),
                20, 0.5, 0.5, 0.5, 0.0, redDust, true);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.7f);
            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 2.0f);

            return true;
        }

        // Progress feedback: warn 1 hit before trigger
        if (count == threshold - 1) {
            player.spawnParticle(Particle.CRIT,
                player.getLocation().add(0, 1, 0),
                15, 0.3, 0.5, 0.3, 0.1);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.8f);
        }

        return false;
    }

    /**
     * Gets the current hit count for a player.
     */
    public int getHitCount(Player player) {
        return hitCounts.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Resets hit count for a player.
     */
    public void resetHitCount(Player player) {
        hitCounts.remove(player.getUniqueId());
    }

    /**
     * Clears hit data for a player (on logout).
     */
    public void clearHitData(UUID playerId) {
        hitCounts.remove(playerId);
    }
}
