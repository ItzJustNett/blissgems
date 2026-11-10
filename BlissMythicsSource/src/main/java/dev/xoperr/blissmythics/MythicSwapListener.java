package dev.xoperr.blissmythics;

import dev.xoperr.blissgems.api.BlissGemsAPI;
import dev.xoperr.blissgems.goldevent.GoldenDreamWorld;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Makes the mythic gems (heretic, auratus) a temporary override of a player's normal gem:
 *
 * <ul>
 *   <li>When a player picks up a mythic gem while holding a normal (built-in/expansion) gem,
 *       that normal gem is pulled out of the inventory and stashed (memory + disk).</li>
 *   <li>The mythic already drops on death on its own — mythic items are not flagged
 *       undroppable, so BlissGems' death protection leaves them in the drops.</li>
 *   <li>A mythic that arrives any other way (/bliss give, a kill) is caught by a sweep every
 *       5 ticks, which stashes the normal gem held next to it.</li>
 *   <li>On respawn, or when they drop the mythic (mythics are droppable), if the player no
 *       longer holds a mythic, the stashed gem is returned - never inside the Golden Dream,
 *       whose wake puts the real inventory back over whatever was handed out there.</li>
 * </ul>
 *
 * Only one gem is stashed at a time: picking up a second mythic while one is already stashed
 * does not overwrite the stash. keepInventory deaths (e.g. Revive Beacon) keep the mythic, so
 * the stash is left untouched until a real death removes the mythic.
 */
public final class MythicSwapListener implements Listener {
   private static final Set<String> MYTHIC_IDS = Set.of("heretic", "auratus");
   private final BlissMythics plugin;
   private final BlissGemsAPI api;
   private final Map<UUID, ItemStack> stashed = new HashMap<>();
   private final File file;

   public MythicSwapListener(BlissMythics plugin, BlissGemsAPI api) {
      this.plugin = plugin;
      this.api = api;
      this.file = new File(plugin.getDataFolder(), "stashed-gems.yml");
      this.load();
      Bukkit.getScheduler().runTaskTimer(plugin, this::sweep, 5L, 5L);
   }

   /** A normal gem held next to a mythic goes into the stash, however the mythic got there. */
   private void sweep() {
      for (Player player : Bukkit.getOnlinePlayers()) {
         if (!player.isDead() && !this.stashed.containsKey(player.getUniqueId()) && this.holdsMythic(player)) {
            this.stashNormalGem(player);
         }
      }
   }

   /** The gem id an item represents, or null if it is not a gem. */
   private String gemIdOf(ItemStack item) {
      if (!CustomItemManager.mayBeGem(item)) {
         return null;
      }
      String itemId = CustomItemManager.getIdByItem(item);
      return itemId == null ? null : this.api.getGemRegistry().gemIdFromItemId(itemId);
   }

   private boolean isMythic(ItemStack item) {
      String gemId = this.gemIdOf(item);
      return gemId != null && MYTHIC_IDS.contains(gemId);
   }

   private boolean holdsMythic(Player player) {
      for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
         if (this.isMythic(player.getInventory().getItem(slot))) {
            return true;
         }
      }
      return false;
   }

   @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
   public void onPickup(EntityPickupItemEvent event) {
      if (!(event.getEntity() instanceof Player player)) {
         return;
      }
      if (!this.isMythic(event.getItem().getItemStack())) {
         return;
      }
      this.stashNormalGem(player);
   }

   /** Pulls the player's normal (non-mythic) gem out of their inventory into the stash. */
   public void stashNormalGem(Player player) {
      UUID id = player.getUniqueId();
      if (this.stashed.containsKey(id)) {
         return; // a gem is already stashed — don't overwrite it
      }
      // Find the player's current normal (non-mythic) gem and stash it.
      for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
         ItemStack item = player.getInventory().getItem(slot);
         String gemId = this.gemIdOf(item);
         if (gemId != null && !MYTHIC_IDS.contains(gemId)) {
            this.stashed.put(id, item.clone());
            player.getInventory().setItem(slot, null);
            this.save();
            player.sendMessage("§d§oYour gem was stored — it returns when the mythic is lost.");
            this.api.getGemManager().updateActiveGem(player);
            return;
         }
      }
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onRespawn(PlayerRespawnEvent event) {
      Player player = event.getPlayer();
      UUID id = player.getUniqueId();
      if (!this.stashed.containsKey(id)) {
         return;
      }
      // a mythic that survived (keepInventory) keeps the stash for later
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.returnStash(player), 3L);
   }

   /** Dropping the mythic hands the stashed gem back. */
   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onDrop(PlayerDropItemEvent event) {
      Player player = event.getPlayer();
      if (!this.isMythic(event.getItemDrop().getItemStack()) || !this.stashed.containsKey(player.getUniqueId())) {
         return;
      }
      Bukkit.getScheduler().runTask(this.plugin, () -> this.returnStash(player));
   }

   private void returnStash(Player player) {
      UUID id = player.getUniqueId();
      if (!player.isOnline() || player.isDead() || !this.stashed.containsKey(id) || this.holdsMythic(player)
         || GoldenDreamWorld.isDreamWorld(player.getWorld())) {
         return;
      }
      player.getInventory().addItem(this.stashed.remove(id)).values().forEach(left -> player.getWorld().dropItem(player.getLocation(), left));
      this.save();
      this.api.getGemManager().updateActiveGem(player);
      player.sendMessage("§d§oYour gem returns to you.");
   }

   private void save() {
      YamlConfiguration data = new YamlConfiguration();
      for (Map.Entry<UUID, ItemStack> entry : this.stashed.entrySet()) {
         data.set(entry.getKey().toString(), entry.getValue());
      }
      try {
         if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
         }
         data.save(this.file);
      } catch (IOException e) {
         this.plugin.getLogger().warning("Could not save stashed gems: " + e.getMessage());
      }
   }

   private void load() {
      if (!this.file.exists()) {
         return;
      }
      YamlConfiguration data = YamlConfiguration.loadConfiguration(this.file);
      for (String key : data.getKeys(false)) {
         ItemStack item = data.getItemStack(key);
         if (item != null) {
            try {
               this.stashed.put(UUID.fromString(key), item);
            } catch (IllegalArgumentException ignored) {
            }
         }
      }
   }
}
