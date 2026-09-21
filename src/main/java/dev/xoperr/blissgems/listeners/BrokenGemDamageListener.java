/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.projectiles.ProjectileSource
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.EnergyState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public class BrokenGemDamageListener
implements Listener {
    private final BlissGems plugin;

    public BrokenGemDamageListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onGemHolderDamagePlayer(EntityDamageByEntityEvent event) {
        if (!this.plugin.getConfig().getBoolean("combat.broken-gem-bonus.enabled", true)) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player victim = (Player)entity;
        Player attacker = this.resolveAttacker(event.getDamager());
        if (attacker == null || attacker.equals((Object)victim)) {
            return;
        }
        if (this.plugin.getEnergyManager().getEnergyState(victim) != EnergyState.BROKEN) {
            return;
        }
        if (!this.plugin.getGemManager().hasGemInOffhand(attacker)) {
            return;
        }
        if (this.plugin.getRegionManager() != null && this.plugin.getRegionManager().areGemsDisabled(attacker)) {
            return;
        }
        double multiplier = this.plugin.getConfig().getDouble("combat.broken-gem-bonus.damage-multiplier", 1.5);
        if (multiplier <= 1.0) {
            return;
        }
        event.setDamage(event.getDamage() * multiplier);
    }

    private Player resolveAttacker(Entity damager) {
        Projectile projectile;
        ProjectileSource shooter;
        if (damager instanceof Player) {
            Player player = (Player)damager;
            return player;
        }
        if (damager instanceof Projectile && (shooter = (projectile = (Projectile)damager).getShooter()) instanceof Player) {
            Player player = (Player)shooter;
            return player;
        }
        return null;
    }
}

