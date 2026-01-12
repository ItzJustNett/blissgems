package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Manages critical hit tracking for Strength Gem's Chad Strength ability
 * Every X critical hits charges a special attack that deals 2x damage
 */
public class CriticalHitManager {
    private final BlissGems plugin;
    private final Map<UUID, CriticalHitData> playerCritData = new HashMap<>();

    // Tier 1: Every 8 crits = 2x damage
    // Tier 2: Every 3 crits = 2x damage
    private static final int TIER1_CRIT_THRESHOLD = 8;
    private static final int TIER2_CRIT_THRESHOLD = 3;
    private static final double DAMAGE_MULTIPLIER = 2.0;

    public CriticalHitManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers a critical hit from a player
     * Returns the damage multiplier to apply (1.0 = normal, 2.0 = charged)
     */
    public double registerCriticalHit(Player player, int gemTier) {
        UUID uuid = player.getUniqueId();
        CriticalHitData data = playerCritData.computeIfAbsent(uuid, k -> new CriticalHitData());

        // Determine threshold based on tier
        int threshold = (gemTier >= 2) ? TIER2_CRIT_THRESHOLD : TIER1_CRIT_THRESHOLD;

        // Check if charged attack is ready
        if (data.getCritCount() >= threshold) {
            // Reset counter
            data.resetCritCount();

            // Visual effects for charged attack
            player.spawnParticle(Particle.CRIT,
                player.getLocation().add(0, 1, 0),
                50, 0.5, 0.5, 0.5, 0.3);
            player.spawnParticle(Particle.ENCHANTED_HIT,
                player.getLocation().add(0, 1, 0),
                30, 0.5, 0.5, 0.5, 0.1);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.7f);
            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 2.0f);

            player.sendMessage("§c§l⚔ CHARGED ATTACK! §c2x Damage!");

            return DAMAGE_MULTIPLIER;
        } else {
            // Increment counter
            data.incrementCritCount();

            // Visual feedback
            int critsUntilCharged = threshold - data.getCritCount();

            // Show particles based on how close to charged
            if (data.getCritCount() >= threshold - 1) {
                // Almost charged - more particles
                player.spawnParticle(Particle.CRIT,
                    player.getLocation().add(0, 1, 0),
                    15, 0.3, 0.5, 0.3, 0.1);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.8f);
                player.sendMessage("§e§l⚡ §e1 more crit for CHARGED ATTACK!");
            } else if (data.getCritCount() % 2 == 0) {
                // Every 2 crits, give feedback
                player.spawnParticle(Particle.CRIT,
                    player.getLocation().add(0, 1, 0),
                    5, 0.3, 0.3, 0.3, 0.05);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
            }

            return 1.0; // Normal damage
        }
    }

    /**
     * Gets the current crit count for a player
     */
    public int getCritCount(Player player) {
        CriticalHitData data = playerCritData.get(player.getUniqueId());
        return data != null ? data.getCritCount() : 0;
    }

    /**
     * Gets crits remaining until charged attack
     */
    public int getCritsUntilCharged(Player player, int gemTier) {
        int threshold = (gemTier >= 2) ? TIER2_CRIT_THRESHOLD : TIER1_CRIT_THRESHOLD;
        int currentCount = getCritCount(player);
        return Math.max(0, threshold - currentCount);
    }

    /**
     * Checks if player has a charged attack ready
     */
    public boolean isChargedAttackReady(Player player, int gemTier) {
        int threshold = (gemTier >= 2) ? TIER2_CRIT_THRESHOLD : TIER1_CRIT_THRESHOLD;
        return getCritCount(player) >= threshold;
    }

    /**
     * Resets crit count for a player
     */
    public void resetCritCount(Player player) {
        CriticalHitData data = playerCritData.get(player.getUniqueId());
        if (data != null) {
            data.resetCritCount();
        }
    }

    /**
     * Clears crit data for a player (on logout)
     */
    public void clearCritData(UUID playerId) {
        playerCritData.remove(playerId);
    }

    /**
     * Data class for tracking player critical hits
     */
    private static class CriticalHitData {
        private int critCount;

        public CriticalHitData() {
            this.critCount = 0;
        }

        public int getCritCount() {
            return critCount;
        }

        public void incrementCritCount() {
            critCount++;
        }

        public void resetCritCount() {
            critCount = 0;
        }
    }
}
