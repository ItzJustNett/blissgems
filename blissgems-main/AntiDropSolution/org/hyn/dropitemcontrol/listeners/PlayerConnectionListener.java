package org.hyn.dropitemcontrol.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.hyn.dropitemcontrol.Dropitemcontrol;
import org.hyn.dropitemcontrol.data.PlayerDataManager;

public class PlayerConnectionListener implements Listener {
   private final Dropitemcontrol plugin;
   private final PlayerDataManager dataManager;

   public PlayerConnectionListener(Dropitemcontrol plugin, PlayerDataManager dataManager) {
      this.plugin = plugin;
      this.dataManager = dataManager;
   }

   @EventHandler(
      priority = EventPriority.NORMAL
   )
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> {
         this.dataManager.loadPlayerData(player);
      });
   }

   @EventHandler(
      priority = EventPriority.NORMAL
   )
   public void onPlayerQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      this.dataManager.unloadPlayerData(player.getUniqueId());
   }
}
