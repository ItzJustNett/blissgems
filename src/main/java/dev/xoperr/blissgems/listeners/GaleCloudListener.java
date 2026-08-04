package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.abilities.SpeedAbilities;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Turns a landed Gale Cloud snowball into its lingering slow field.
 */
public class GaleCloudListener implements Listener {
    private final BlissGems plugin;

    public GaleCloudListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onGaleCloudLand(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Snowball)) {
            return;
        }
        if (!projectile.getScoreboardTags().contains(SpeedAbilities.GALE_CLOUD_TAG)) {
            return;
        }

        // Prefer the entity it struck, falling back to the block face it splashed against
        Location impact = event.getHitEntity() != null
            ? event.getHitEntity().getLocation()
            : projectile.getLocation();

        ProjectileSource shooter = projectile.getShooter();
        Player thrower = shooter instanceof Player player ? player : null;

        this.plugin.getSpeedAbilities().spawnGaleCloud(impact, thrower);
    }
}
