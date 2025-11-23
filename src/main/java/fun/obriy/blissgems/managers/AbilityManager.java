/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package fun.obriy.blissgems.managers;

import fun.obriy.blissgems.BlissGems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public class AbilityManager {
    private final BlissGems plugin;
    private final Map<UUID, Map<String, Long>> cooldowns;

    public AbilityManager(BlissGems plugin) {
        this.plugin = plugin;
        this.cooldowns = new HashMap<UUID, Map<String, Long>>();
    }

    public boolean isOnCooldown(Player player, String abilityKey) {
        Map<String, Long> playerCooldowns = this.cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            return false;
        }
        Long endTime = playerCooldowns.get(abilityKey);
        if (endTime == null) {
            return false;
        }
        return System.currentTimeMillis() < endTime;
    }

    public int getRemainingCooldown(Player player, String abilityKey) {
        Map<String, Long> playerCooldowns = this.cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) {
            return 0;
        }
        Long endTime = playerCooldowns.get(abilityKey);
        if (endTime == null) {
            return 0;
        }
        long remaining = endTime - System.currentTimeMillis();
        return remaining > 0L ? (int)Math.ceil((double)remaining / 1000.0) : 0;
    }

    public void setCooldown(Player player, String abilityKey, int seconds) {
        Map playerCooldowns = this.cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap());
        playerCooldowns.put(abilityKey, System.currentTimeMillis() + (long)seconds * 1000L);
    }

    public boolean canUseAbility(Player player, String abilityKey) {
        if (!this.plugin.getEnergyManager().canUseAbilities(player)) {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-no-energy", new Object[0]));
            return false;
        }
        if (this.isOnCooldown(player, abilityKey)) {
            // Cooldown is displayed in action bar, no need for chat message
            return false;
        }
        return true;
    }

    public void useAbility(Player player, String abilityKey) {
        int cooldown = this.plugin.getConfigManager().getAbilityCooldown(abilityKey);
        this.setCooldown(player, abilityKey, cooldown);
    }

    public void clearCooldowns(Player player) {
        this.cooldowns.remove(player.getUniqueId());
    }

    public void clearCache(UUID uuid) {
        this.cooldowns.remove(uuid);
    }
}

