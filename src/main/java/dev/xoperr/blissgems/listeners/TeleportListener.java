/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerTeleportEvent
 *  org.bukkit.event.player.PlayerTeleportEvent$TeleportCause
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportListener
implements Listener {
    private final BlissGems plugin;

    public TeleportListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE && this.plugin.getAstraAbilities().isProjecting(player)) {
            event.setCancelled(true);
            player.sendMessage("\u00a7d\u00a7oYou cannot teleport to players while projecting!");
            return;
        }
        if (this.plugin.getFireAbilities().isCharging(player)) {
            this.plugin.getFireAbilities().cancelCharging(player);
            player.sendMessage("\u00a7c\u00a7oYour fireball charge was cancelled due to teleportation.");
        }
        if (this.plugin.getFluxAbilities().isCharging(player)) {
            this.plugin.getFluxAbilities().cancelCharging(player);
            player.sendMessage("\u00a7b\u26a1 \u00a7oYour flux beam charge was cancelled due to teleportation.");
        }
    }
}

