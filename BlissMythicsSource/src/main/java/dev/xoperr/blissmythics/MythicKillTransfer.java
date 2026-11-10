package dev.xoperr.blissmythics;

import dev.xoperr.blissgems.api.BlissGemsAPI;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

/**
 * A mythic holder killed by another player loses the mythic to their killer. The killer's own
 * gem is stashed the same way as when picking a mythic up; the victim's stashed gem comes back
 * on respawn.
 */
public final class MythicKillTransfer implements Listener {
   private static final Set<String> MYTHIC_IDS = Set.of("heretic", "auratus");
   private final BlissMythics plugin;
   private final BlissGemsAPI api;
   private final MythicSwapListener swap;
   private final Set<UUID> sweepOnRespawn = new HashSet<>();

   public MythicKillTransfer(BlissMythics plugin, BlissGemsAPI api, MythicSwapListener swap) {
      this.plugin = plugin;
      this.api = api;
      this.swap = swap;
   }

   private boolean isMythic(ItemStack item) {
      if (item == null || item.getType().isAir()) {
         return false;
      }
      String itemId = CustomItemManager.getIdByItem(item);
      String gemId = itemId == null ? null : this.api.getGemRegistry().gemIdFromItemId(itemId);
      return gemId != null && MYTHIC_IDS.contains(gemId);
   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onDeath(PlayerDeathEvent event) {
      Player victim = event.getEntity();
      Player killer = victim.getKiller();
      if (killer == null || killer.equals(victim)) {
         return;
      }
      ItemStack mythic = null;
      for (ItemStack item : victim.getInventory().getContents()) {
         if (this.isMythic(item)) {
            mythic = item.clone();
            break;
         }
      }
      if (mythic == null) {
         return;
      }
      event.getDrops().removeIf(this::isMythic);
      this.sweepOnRespawn.add(victim.getUniqueId());
      this.swap.stashNormalGem(killer);
      killer.getInventory().addItem(mythic).values().forEach(left -> killer.getWorld().dropItem(killer.getLocation(), left));
      this.api.getGemManager().updateActiveGem(killer);
      killer.sendMessage("§d§lYou took " + victim.getName() + "'s mythic gem!");
      victim.sendMessage("§d§o" + killer.getName() + " took your mythic gem.");
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onRespawn(PlayerRespawnEvent event) {
      Player player = event.getPlayer();
      if (!this.sweepOnRespawn.remove(player.getUniqueId())) {
         return;
      }
      // BlissGems hands kept gems back on respawn; the mythic went to the killer, so take it out
      // again before the stash check (3 ticks) gives the player's own gem back.
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         if (!player.isOnline()) {
            return;
         }
         for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            if (this.isMythic(player.getInventory().getItem(slot))) {
               player.getInventory().setItem(slot, null);
            }
         }
         this.api.getGemManager().updateActiveGem(player);
      }, 2L);
   }
}
