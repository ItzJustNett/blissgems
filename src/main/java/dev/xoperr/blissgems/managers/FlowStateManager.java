/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.entity.Player
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FlowStateManager {
    private final BlissGems plugin;
    private final Map<UUID, FlowStateData> playerFlowStates = new HashMap<UUID, FlowStateData>();
    private static final int ACTION_TIMEOUT_MS = 3000;
    private static final int MAX_FLOW_LEVEL = 5;

    public FlowStateManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    public void registerAction(Player player, ActionType actionType) {
        UUID uuid = player.getUniqueId();
        FlowStateData flowData = this.playerFlowStates.computeIfAbsent(uuid, k -> new FlowStateData());
        long currentTime = System.currentTimeMillis();
        if (flowData.getLastActionType() == actionType) {
            long timeSinceLastAction = currentTime - flowData.getLastActionTime();
            if (timeSinceLastAction <= 3000L) {
                int newLevel = Math.min(flowData.getFlowLevel() + 1, 5);
                flowData.setFlowLevel(newLevel);
                this.applyFlowStateEffects(player, newLevel);
                if (newLevel > flowData.getFlowLevel()) {
                    player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 10 + newLevel * 2, 0.3, 0.5, 0.3, 0.05);
                    if (newLevel == 5) {
                        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 2.0f);
                        player.sendMessage("\u00a7b\u00a7l\u26a1 MAX FLOW STATE!");
                    } else if (newLevel % 2 == 0) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f + (float)newLevel * 0.2f);
                    }
                }
            } else {
                flowData.setFlowLevel(1);
                this.applyFlowStateEffects(player, 1);
            }
        } else {
            flowData.setFlowLevel(1);
            this.applyFlowStateEffects(player, 1);
        }
        flowData.setLastActionType(actionType);
        flowData.setLastActionTime(currentTime);
    }

    private void applyFlowStateEffects(Player player, int level) {
        if (level <= 0) {
            return;
        }
        int maxSpeedLevel = this.plugin.getConfig().getInt("passives.flux.max-flow-speed-level", 0);
        int speedLevel = Math.min((level + 1) / 2, maxSpeedLevel);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, speedLevel, false, false));
        if (level >= 3) {
            int hasteLevel = level >= 4 ? 1 : 0;
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 100, hasteLevel, false, false));
        }
        if (level >= 3) {
            player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 0.5, 0.0), 5, 0.3, 0.3, 0.3, 0.01);
        }
    }

    public int getFlowLevel(Player player) {
        FlowStateData data = this.playerFlowStates.get(player.getUniqueId());
        if (data == null) {
            return 0;
        }
        long timeSinceLastAction = System.currentTimeMillis() - data.getLastActionTime();
        if (timeSinceLastAction > 3000L) {
            return 0;
        }
        return data.getFlowLevel();
    }

    public void resetFlowState(Player player) {
        this.playerFlowStates.remove(player.getUniqueId());
    }

    public void clearFlowState(UUID playerId) {
        this.playerFlowStates.remove(playerId);
    }

    private static class FlowStateData {
        private ActionType lastActionType = null;
        private long lastActionTime = 0L;
        private int flowLevel = 0;

        public ActionType getLastActionType() {
            return this.lastActionType;
        }

        public void setLastActionType(ActionType lastActionType) {
            this.lastActionType = lastActionType;
        }

        public long getLastActionTime() {
            return this.lastActionTime;
        }

        public void setLastActionTime(long lastActionTime) {
            this.lastActionTime = lastActionTime;
        }

        public int getFlowLevel() {
            return this.flowLevel;
        }

        public void setFlowLevel(int flowLevel) {
            this.flowLevel = flowLevel;
        }
    }

    public static enum ActionType {
        BLOCK_BREAK,
        ARROW_SHOOT,
        ATTACK,
        SPRINT,
        JUMP;

    }
}

