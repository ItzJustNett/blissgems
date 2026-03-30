package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Handles teleportation events to prevent ability charging exploits.
 * Cancels any active ability charging when a player teleports to prevent
 * them from starting a charge outside protected areas and finishing it inside.
 */
public class TeleportListener implements Listener {
    private final BlissGems plugin;

    public TeleportListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        // Cancel Fire Fireball charging
        if (plugin.getFireAbilities().isCharging(player)) {
            plugin.getFireAbilities().cancelCharging(player);
            player.sendMessage("§c§oYour fireball charge was cancelled due to teleportation.");
        }

        // Cancel Flux Beam charging
        if (plugin.getFluxAbilities().isCharging(player)) {
            plugin.getFluxAbilities().cancelCharging(player);
            player.sendMessage("§b⚡ §oYour flux beam charge was cancelled due to teleportation.");
        }
    }
}
