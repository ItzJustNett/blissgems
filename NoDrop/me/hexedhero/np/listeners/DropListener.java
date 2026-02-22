package me.hexedhero.np.listeners;

import me.hexedhero.np.utils.Common;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.plugin.Plugin;

public class DropListener implements Listener {
   final Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("NoDrop");

   @EventHandler
   public void DL(PlayerDropItemEvent event) {
      Player player = event.getPlayer();
      if (this.PlayersWorld(player)) {
         event.setCancelled(true);
         if (this.plugin.getConfig().getBoolean("Message.Enabled")) {
            Common.tell(player, this.plugin.getConfig().getString("Message.Content"));
         }
      }

   }

   private boolean PlayersWorld(Player player) {
      return this.plugin.getConfig().getStringList("Worlds").contains(player.getWorld().getName());
   }
}
