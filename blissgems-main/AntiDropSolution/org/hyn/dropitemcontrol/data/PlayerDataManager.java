package org.hyn.dropitemcontrol.data;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.hyn.dropitemcontrol.Dropitemcontrol;

public class PlayerDataManager {
   private final Dropitemcontrol plugin;
   private final File dataFolder;
   private final Map<UUID, Set<String>> cachedPlayerItems;
   private final Map<UUID, Set<Material>> cachedPlayerItemTypes;

   public PlayerDataManager(Dropitemcontrol plugin) {
      this.plugin = plugin;
      this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
      this.cachedPlayerItems = new HashMap();
      this.cachedPlayerItemTypes = new HashMap();
      if (!this.dataFolder.exists()) {
         this.dataFolder.mkdirs();
      }

   }

   private File getPlayerFile(UUID playerId) {
      return new File(this.dataFolder, playerId.toString() + ".yml");
   }

   private FileConfiguration getPlayerConfig(UUID playerId) {
      File playerFile = this.getPlayerFile(playerId);
      return YamlConfiguration.loadConfiguration(playerFile);
   }

   public void saveLockedItem(Player player, ItemStack item) {
      if (item != null && item.getType() != Material.AIR) {
         UUID playerId = player.getUniqueId();
         FileConfiguration config = this.getPlayerConfig(playerId);
         String itemId = this.generateItemId(item);
         Material itemType = item.getType();
         List<String> lockedItems = config.getStringList("locked-items");
         if (!lockedItems.contains(itemId)) {
            lockedItems.add(itemId);
         }

         String path = "items." + itemId;
         config.set(path + ".type", itemType.toString());
         config.set(path + ".amount", item.getAmount());
         if (item.hasItemMeta()) {
            if (item.getItemMeta().hasDisplayName()) {
               config.set(path + ".name", item.getItemMeta().getDisplayName());
            }

            if (item.getItemMeta().hasLore()) {
               config.set(path + ".lore", item.getItemMeta().getLore());
            }
         }

         config.set("locked-items", lockedItems);
         if (this.plugin.getConfig().getBoolean("settings.lock-by-item-type", true)) {
            List<String> lockedTypes = config.getStringList("locked-types");
            String typeName = itemType.toString();
            if (!lockedTypes.contains(typeName)) {
               lockedTypes.add(typeName);
               config.set("locked-types", lockedTypes);
               Set<Material> cachedTypes = (Set)this.cachedPlayerItemTypes.computeIfAbsent(playerId, (k) -> {
                  return new HashSet();
               });
               cachedTypes.add(itemType);
            }
         }

         Set<String> cachedItems = (Set)this.cachedPlayerItems.computeIfAbsent(playerId, (k) -> {
            return new HashSet();
         });
         cachedItems.add(itemId);

         try {
            config.save(this.getPlayerFile(playerId));
         } catch (IOException var12) {
            Logger var10000 = this.plugin.getLogger();
            String var10001 = player.getName();
            var10000.warning("Không thể lưu dữ liệu cho người chơi " + var10001 + ": " + var12.getMessage());
         }

      }
   }

   public void removeLockedItem(Player player, ItemStack item) {
      if (item != null && item.getType() != Material.AIR) {
         UUID playerId = player.getUniqueId();
         FileConfiguration config = this.getPlayerConfig(playerId);
         String itemId = this.generateItemId(item);
         Material itemType = item.getType();
         List<String> lockedItems = config.getStringList("locked-items");
         boolean itemRemoved = lockedItems.remove(itemId);
         Logger var10000;
         String var10001;
         if (!itemRemoved) {
            var10000 = this.plugin.getLogger();
            var10001 = itemType.toString();
            var10000.info("Item " + var10001 + " không tìm thấy trong danh sách khóa của người chơi " + player.getName());
            if (this.plugin.getConfig().getBoolean("settings.lock-by-item-type", true)) {
               List<String> lockedTypes = config.getStringList("locked-types");
               boolean typeRemoved = lockedTypes.remove(itemType.toString());
               if (typeRemoved) {
                  this.plugin.getLogger().info("Đã xóa loại item " + itemType.toString() + " khỏi danh sách loại đã khóa");
                  config.set("locked-types", lockedTypes);
                  Set<Material> cachedTypes = (Set)this.cachedPlayerItemTypes.get(playerId);
                  if (cachedTypes != null) {
                     cachedTypes.remove(itemType);
                  }
               }
            }

            try {
               config.save(this.getPlayerFile(playerId));
               this.plugin.getLogger().info("Đã lưu file dữ liệu của người chơi " + player.getName() + " sau khi mở khóa loại item");
            } catch (IOException var13) {
               var10000 = this.plugin.getLogger();
               var10001 = player.getName();
               var10000.warning("Không thể lưu dữ liệu cho người chơi " + var10001 + ": " + var13.getMessage());
            }

         } else {
            config.set("items." + itemId, (Object)null);
            config.set("locked-items", lockedItems);
            if (this.plugin.getConfig().getBoolean("settings.lock-by-item-type", true)) {
               boolean hasItemOfSameType = false;
               Iterator var10 = lockedItems.iterator();

               while(var10.hasNext()) {
                  String remainingItemId = (String)var10.next();
                  String typePath = "items." + remainingItemId + ".type";
                  if (config.getString(typePath, "").equals(itemType.toString())) {
                     hasItemOfSameType = true;
                     break;
                  }
               }

               if (!hasItemOfSameType) {
                  List<String> lockedTypes = config.getStringList("locked-types");
                  boolean typeRemoved = lockedTypes.remove(itemType.toString());
                  if (typeRemoved) {
                     this.plugin.getLogger().info("Đã xóa loại item " + itemType.toString() + " khỏi danh sách loại đã khóa");
                     config.set("locked-types", lockedTypes);
                     Set<Material> cachedTypes = (Set)this.cachedPlayerItemTypes.get(playerId);
                     if (cachedTypes != null) {
                        cachedTypes.remove(itemType);
                     }
                  }
               }
            }

            Set<String> cachedItems = (Set)this.cachedPlayerItems.get(playerId);
            if (cachedItems != null) {
               cachedItems.remove(itemId);
            }

            try {
               config.save(this.getPlayerFile(playerId));
               this.plugin.getLogger().info("Đã lưu file dữ liệu của người chơi " + player.getName() + " sau khi mở khóa item");
            } catch (IOException var14) {
               var10000 = this.plugin.getLogger();
               var10001 = player.getName();
               var10000.warning("Không thể lưu dữ liệu cho người chơi " + var10001 + ": " + var14.getMessage());
            }

         }
      }
   }

   public int getLockedItemCount(UUID playerId) {
      Set cachedTypes;
      FileConfiguration config;
      List lockedTypes;
      HashSet cachedTypes;
      if (!this.plugin.getConfig().getBoolean("settings.count-by-item-type", true)) {
         cachedTypes = (Set)this.cachedPlayerItems.get(playerId);
         if (cachedTypes != null) {
            return cachedTypes.size();
         } else {
            config = this.getPlayerConfig(playerId);
            lockedTypes = config.getStringList("locked-items");
            cachedTypes = new HashSet(lockedTypes);
            this.cachedPlayerItems.put(playerId, cachedTypes);
            return lockedTypes.size();
         }
      } else {
         cachedTypes = (Set)this.cachedPlayerItemTypes.get(playerId);
         if (cachedTypes != null) {
            return cachedTypes.size();
         } else {
            config = this.getPlayerConfig(playerId);
            lockedTypes = config.getStringList("locked-types");
            cachedTypes = new HashSet();
            Iterator var5 = lockedTypes.iterator();

            while(var5.hasNext()) {
               String typeName = (String)var5.next();

               try {
                  Material material = Material.valueOf(typeName);
                  cachedTypes.add(material);
               } catch (IllegalArgumentException var8) {
               }
            }

            this.cachedPlayerItemTypes.put(playerId, cachedTypes);
            return lockedTypes.size();
         }
      }
   }

   public boolean isItemTypeLocked(UUID playerId, Material itemType) {
      Set<Material> cachedTypes = (Set)this.cachedPlayerItemTypes.get(playerId);
      if (cachedTypes != null) {
         return cachedTypes.contains(itemType);
      } else {
         FileConfiguration config = this.getPlayerConfig(playerId);
         List<String> lockedTypes = config.getStringList("locked-types");
         Set<Material> cachedTypes = new HashSet();
         Iterator var6 = lockedTypes.iterator();

         while(var6.hasNext()) {
            String typeName = (String)var6.next();

            try {
               Material material = Material.valueOf(typeName);
               cachedTypes.add(material);
            } catch (IllegalArgumentException var9) {
            }
         }

         this.cachedPlayerItemTypes.put(playerId, cachedTypes);
         return lockedTypes.contains(itemType.toString());
      }
   }

   private String generateItemId(ItemStack item) {
      return String.valueOf(item.hashCode());
   }

   public void loadPlayerData(Player player) {
      UUID playerId = player.getUniqueId();
      FileConfiguration config = this.getPlayerConfig(playerId);
      List<String> lockedItems = config.getStringList("locked-items");
      Set<String> cachedItems = new HashSet(lockedItems);
      this.cachedPlayerItems.put(playerId, cachedItems);
      List<String> lockedTypes = config.getStringList("locked-types");
      Set<Material> cachedTypes = new HashSet();
      Iterator var8 = lockedTypes.iterator();

      while(var8.hasNext()) {
         String typeName = (String)var8.next();

         try {
            Material material = Material.valueOf(typeName);
            cachedTypes.add(material);
         } catch (IllegalArgumentException var11) {
         }
      }

      this.cachedPlayerItemTypes.put(playerId, cachedTypes);
   }

   public void unloadPlayerData(UUID playerId) {
      this.cachedPlayerItems.remove(playerId);
      this.cachedPlayerItemTypes.remove(playerId);
   }
}
