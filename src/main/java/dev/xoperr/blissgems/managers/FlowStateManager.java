package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Manages the Flow State mechanic for Flux Gem
 * Actions performed repeatedly become faster
 */
public class FlowStateManager {
    private final BlissGems plugin;
    private final Map<UUID, FlowStateData> playerFlowStates = new HashMap<>();

    // Action types that can trigger flow state
    public enum ActionType {
        BLOCK_BREAK,
        ARROW_SHOOT,
        ATTACK,
        SPRINT,
        JUMP
    }

    private static final int ACTION_TIMEOUT_MS = 3000; // 3 seconds between actions
    private static final int MAX_FLOW_LEVEL = 5;

    public FlowStateManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers an action performed by the player
     * Increases flow state if action is repeated within timeout
     */
    public void registerAction(Player player, ActionType actionType) {
        UUID uuid = player.getUniqueId();
        FlowStateData flowData = playerFlowStates.computeIfAbsent(uuid, k -> new FlowStateData());

        long currentTime = System.currentTimeMillis();

        // Check if same action type as last time
        if (flowData.getLastActionType() == actionType) {
            long timeSinceLastAction = currentTime - flowData.getLastActionTime();

            // If within timeout, increase flow level
            if (timeSinceLastAction <= ACTION_TIMEOUT_MS) {
                int newLevel = Math.min(flowData.getFlowLevel() + 1, MAX_FLOW_LEVEL);
                flowData.setFlowLevel(newLevel);

                // Apply flow state effects
                applyFlowStateEffects(player, newLevel);

                // Visual feedback on level up
                if (newLevel > flowData.getFlowLevel()) {
                    player.spawnParticle(Particle.ELECTRIC_SPARK,
                        player.getLocation().add(0, 1, 0),
                        10 + (newLevel * 2), 0.3, 0.5, 0.3, 0.05);

                    if (newLevel == MAX_FLOW_LEVEL) {
                        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 2.0f);
                        player.sendMessage("§b§l⚡ MAX FLOW STATE!");
                    } else if (newLevel % 2 == 0) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f + (newLevel * 0.2f));
                    }
                }
            } else {
                // Timeout expired, reset to level 1
                flowData.setFlowLevel(1);
                applyFlowStateEffects(player, 1);
            }
        } else {
            // Different action, reset
            flowData.setFlowLevel(1);
            applyFlowStateEffects(player, 1);
        }

        flowData.setLastActionType(actionType);
        flowData.setLastActionTime(currentTime);
    }

    /**
     * Applies flow state effects based on level
     */
    private void applyFlowStateEffects(Player player, int level) {
        if (level <= 0) {
            return;
        }

        // Speed effect increases with flow level
        // Level 1: Speed I, Level 2-3: Speed II, Level 4-5: Speed III
        int speedLevel = Math.min((level + 1) / 2, 2); // 0-2 (Speed I-III)

        player.addPotionEffect(new PotionEffect(
            PotionEffectType.SPEED,
            80, // 4 seconds
            speedLevel,
            false,
            false
        ));

        // At higher levels, add haste
        if (level >= 3) {
            int hasteLevel = level >= 4 ? 1 : 0; // Haste I at level 3-4, Haste II at level 5
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.HASTE,
                80,
                hasteLevel,
                false,
                false
            ));
        }

        // Visual particles
        if (level >= 3) {
            player.spawnParticle(Particle.ELECTRIC_SPARK,
                player.getLocation().add(0, 0.5, 0),
                5, 0.3, 0.3, 0.3, 0.01);
        }
    }

    /**
     * Gets the current flow level for a player
     */
    public int getFlowLevel(Player player) {
        FlowStateData data = playerFlowStates.get(player.getUniqueId());
        if (data == null) {
            return 0;
        }

        // Check if expired
        long timeSinceLastAction = System.currentTimeMillis() - data.getLastActionTime();
        if (timeSinceLastAction > ACTION_TIMEOUT_MS) {
            return 0;
        }

        return data.getFlowLevel();
    }

    /**
     * Resets flow state for a player
     */
    public void resetFlowState(Player player) {
        playerFlowStates.remove(player.getUniqueId());
    }

    /**
     * Clears flow state data for a player (on logout)
     */
    public void clearFlowState(UUID playerId) {
        playerFlowStates.remove(playerId);
    }

    /**
     * Data class for tracking player flow state
     */
    private static class FlowStateData {
        private ActionType lastActionType;
        private long lastActionTime;
        private int flowLevel;

        public FlowStateData() {
            this.lastActionType = null;
            this.lastActionTime = 0;
            this.flowLevel = 0;
        }

        public ActionType getLastActionType() {
            return lastActionType;
        }

        public void setLastActionType(ActionType lastActionType) {
            this.lastActionType = lastActionType;
        }

        public long getLastActionTime() {
            return lastActionTime;
        }

        public void setLastActionTime(long lastActionTime) {
            this.lastActionTime = lastActionTime;
        }

        public int getFlowLevel() {
            return flowLevel;
        }

        public void setFlowLevel(int flowLevel) {
            this.flowLevel = flowLevel;
        }
    }
}
