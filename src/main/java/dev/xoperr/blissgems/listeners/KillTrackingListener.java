package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KillTrackingListener implements Listener {
    private final BlissGems plugin;
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private static final long KILL_WINDOW_MS = 3000; // 3 second window to count as gem kill

    public KillTrackingListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
        if (event.getEntity().getType() != EntityType.PLAYER || event.getDamager().getType() != EntityType.PLAYER) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();

        // Check if damager has a gem
        if (plugin.getGemManager().getGemType(damager) != null) {
            // Mark the victim as damaged by a gem ability
            victim.setMetadata("gem_damage_by", new FixedMetadataValue(plugin, damager.getUniqueId()));
            lastDamageTime.put(victim.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null) {
            // Check if killer has a gem and victim was recently damaged by gem ability
            if (plugin.getGemManager().getGemType(killer) != null) {
                Long lastDamage = lastDamageTime.get(victim.getUniqueId());
                if (lastDamage != null && (System.currentTimeMillis() - lastDamage) < KILL_WINDOW_MS) {
                    // Count as gem kill
                    plugin.getStatsManager().recordKill(killer, victim);
                }
            }

            // Always record death
            plugin.getStatsManager().recordDeath(victim);
        }

        // Cleanup
        lastDamageTime.remove(victim.getUniqueId());
        if (victim.hasMetadata("gem_damage_by")) {
            victim.removeMetadata("gem_damage_by", plugin);
        }
    }
}
