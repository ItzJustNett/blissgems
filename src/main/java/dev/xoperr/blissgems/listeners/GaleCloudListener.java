/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.entity.Snowball
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.ProjectileHitEvent
 *  org.bukkit.projectiles.ProjectileSource
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

public class GaleCloudListener
implements Listener {
    private final BlissGems plugin;

    public GaleCloudListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onGaleCloudLand(ProjectileHitEvent event) {
        Player player;
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Snowball)) {
            return;
        }
        if (!projectile.getScoreboardTags().contains("blissgems_gale_cloud")) {
            return;
        }
        Location impact = event.getHitEntity() != null ? event.getHitEntity().getLocation() : projectile.getLocation();
        ProjectileSource shooter = projectile.getShooter();
        Player thrower = shooter instanceof Player ? (player = (Player)shooter) : null;
        this.plugin.getSpeedAbilities().spawnGaleCloud(impact, thrower);
    }
}

