/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.metadata.FixedMetadataValue
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.plugin.Plugin
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class KillTrackingListener
implements Listener {
    private final BlissGems plugin;
    private final Map<UUID, Long> lastDamageTime = new HashMap<UUID, Long>();
    private static final long KILL_WINDOW_MS = 3000L;

    public KillTrackingListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
        if (event.getEntity().getType() != EntityType.PLAYER || event.getDamager().getType() != EntityType.PLAYER) {
            return;
        }
        Player victim = (Player)event.getEntity();
        Player damager = (Player)event.getDamager();
        if (this.plugin.getGemManager().getGemType(damager) != null) {
            victim.setMetadata("gem_damage_by", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)damager.getUniqueId()));
            this.lastDamageTime.put(victim.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer != null) {
            Long lastDamage;
            if (this.plugin.getGemManager().getGemType(killer) != null && (lastDamage = this.lastDamageTime.get(victim.getUniqueId())) != null && System.currentTimeMillis() - lastDamage < 3000L) {
                this.plugin.getStatsManager().recordKill(killer, victim);
            }
            this.plugin.getStatsManager().recordDeath(victim);
        }
        this.lastDamageTime.remove(victim.getUniqueId());
        if (victim.hasMetadata("gem_damage_by")) {
            victim.removeMetadata("gem_damage_by", (Plugin)this.plugin);
        }
    }
}

