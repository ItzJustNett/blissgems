package dev.xoperr.blissmythics;

import dev.xoperr.blissgems.api.BlissGemsAPI;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public final class MythicDeathListener implements Listener {
   private static final Set<String> MYTHIC_IDS = Set.of("heretic", "auratus");
   private final BlissMythics plugin;
   private final BlissGemsAPI api;
   private final Set<UUID> pendingRemoval = new HashSet<>();
   private final File file;

   public MythicDeathListener(BlissMythics var1, BlissGemsAPI var2) {
      this.plugin = var1;
      this.api = var2;
      this.file = new File(var1.getDataFolder(), "pending-mythic-removals.yml");
      this.load();
   }

   private boolean isMythicGemItem(ItemStack var1) {
      if (var1 != null && !var1.getType().isAir()) {
         String var2 = CustomItemManager.getIdByItem(var1);
         if (var2 == null) {
            return false;
         } else {
            String var3 = this.api.getGemRegistry().gemIdFromItemId(var2);
            return var3 != null && MYTHIC_IDS.contains(var3);
         }
      } else {
         return false;
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onDeath(PlayerDeathEvent var1) {
      if (!var1.getKeepInventory()) {
         Player var2 = var1.getEntity();
         boolean var3 = false;

         for (ItemStack var7 : var2.getInventory().getContents()) {
            if (this.isMythicGemItem(var7)) {
               var1.getDrops().add(var7.clone());
               var3 = true;
            }
         }

         if (var3) {
            this.pendingRemoval.add(var2.getUniqueId());
            this.save();
            var2.sendMessage("§d§oYour mythic gem was dropped where you died!");
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onRespawn(PlayerRespawnEvent var1) {
      Player var2 = var1.getPlayer();
      UUID var3 = var2.getUniqueId();
      if (this.pendingRemoval.contains(var3)) {
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (var2.isOnline()) {
               for (int var3x = 0; var3x < var2.getInventory().getSize(); var3x++) {
                  ItemStack var4 = var2.getInventory().getItem(var3x);
                  if (this.isMythicGemItem(var4)) {
                     var2.getInventory().setItem(var3x, null);
                  }
               }

               this.pendingRemoval.remove(var3);
               this.save();
            }
         }, 3L);
      }
   }

   void save() {
      YamlConfiguration var1 = new YamlConfiguration();
      var1.set("pending", this.pendingRemoval.stream().map(UUID::toString).collect(Collectors.toList()));

      try {
         if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
         }

         var1.save(this.file);
      } catch (IOException var3) {
         this.plugin.getLogger().warning("Could not save pending mythic removals: " + var3.getMessage());
      }
   }

   private void load() {
      if (this.file.exists()) {
         YamlConfiguration var1 = YamlConfiguration.loadConfiguration(this.file);

         for (String var3 : var1.getStringList("pending")) {
            try {
               this.pendingRemoval.add(UUID.fromString(var3));
            } catch (IllegalArgumentException var5) {
            }
         }
      }
   }
}
