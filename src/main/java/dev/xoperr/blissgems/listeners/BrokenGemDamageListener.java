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

/**
 * Gem holders hit harder when their victim's gem is Broken, so a shattered gem is a
 * real liability rather than something you can sit on until the next Repair Kit.
 */
public class BrokenGemDamageListener implements Listener {
    private final BlissGems plugin;

    public BrokenGemDamageListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGemHolderDamagePlayer(EntityDamageByEntityEvent event) {
        if (!this.plugin.getConfig().getBoolean("combat.broken-gem-bonus.enabled", true)) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.equals(victim)) {
            return;
        }

        // Only a Broken victim takes the extra damage.
        if (this.plugin.getEnergyManager().getEnergyState(victim) != EnergyState.BROKEN) {
            return;
        }
        // ...and only a gem holder deals it.
        if (!this.plugin.getGemManager().hasGemInOffhand(attacker)) {
            return;
        }
        // Respect WorldGuard regions where gems are switched off.
        if (this.plugin.getRegionManager() != null
                && this.plugin.getRegionManager().areGemsDisabled(attacker)) {
            return;
        }

        double multiplier = this.plugin.getConfig()
            .getDouble("combat.broken-gem-bonus.damage-multiplier", 1.5);
        if (multiplier <= 1.0) {
            return;
        }
        event.setDamage(event.getDamage() * multiplier);
    }

    /** Unwrap arrows and other projectiles back to the player who fired them. */
    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }
}
