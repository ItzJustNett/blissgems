package org.hyn.dropitemcontrol.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.hyn.dropitemcontrol.Dropitemcontrol;
import org.hyn.dropitemcontrol.data.PlayerDataManager;

public class ItemLockManager {
   private final Dropitemcontrol plugin;
   private final NamespacedKey lockKey;
   private final Pattern limitPermissionPattern;
   private final PlayerDataManager dataManager;

   public ItemLockManager(Dropitemcontrol plugin, PlayerDataManager dataManager) {
      this.plugin = plugin;
      this.lockKey = new NamespacedKey(plugin, "locked_item");
      this.limitPermissionPattern = Pattern.compile("dropitemcontrol\\.limit\\.(\\d+)");
      this.dataManager = dataManager;
   }

   public boolean isItemLocked(ItemStack item) {
      if (item != null && item.getType() != Material.AIR) {
         if (!item.hasItemMeta()) {
            return false;
         } else {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
               return false;
            } else {
               PersistentDataContainer container = meta.getPersistentDataContainer();
               return container.has(this.lockKey, PersistentDataType.BYTE) && (Byte)container.get(this.lockKey, PersistentDataType.BYTE) == 1;
            }
         }
      } else {
         return false;
      }
   }

   public boolean isItemTypeLocked(Player player, ItemStack item) {
      if (item != null && item.getType() != Material.AIR) {
         if (!this.plugin.getConfig().getBoolean("settings.lock-by-item-type", true)) {
            return false;
         } else {
            return this.isItemLocked(item) ? true : this.dataManager.isItemTypeLocked(player.getUniqueId(), item.getType());
         }
      } else {
         return false;
      }
   }

   public boolean lockItem(Player player, ItemStack item) {
      if (item != null && item.getType() != Material.AIR) {
         UUID playerId = player.getUniqueId();
         int maxLockedItems = this.getPlayerItemLimit(player);
         int lockedCount = this.dataManager.getLockedItemCount(playerId);
         if (lockedCount >= maxLockedItems) {
            return false;
         } else if (this.isItemLocked(item)) {
            return false;
         } else {
            ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : this.plugin.getServer().getItemFactory().getItemMeta(item.getType());
            if (meta == null) {
               return false;
            } else {
               PersistentDataContainer container = meta.getPersistentDataContainer();
               container.set(this.lockKey, PersistentDataType.BYTE, (byte)1);
               if (this.plugin.getConfig().getBoolean("settings.show-lock-lore", true)) {
                  List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList();
                  if (lore == null) {
                     lore = new ArrayList();
                  }

                  String lockLore = ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("settings.lock-lore", "&c&lĐã khóa - Không thể vứt"));
                  ((List)lore).add(lockLore);
                  meta.setLore((List)lore);
               }

               item.setItemMeta(meta);
               this.dataManager.saveLockedItem(player, item);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public boolean unlockItem(Player player, ItemStack item) {
      if (item != null && item.getType() != Material.AIR) {
         boolean isItemLocked = this.isItemLocked(item);
         boolean isTypeLocked = this.isItemTypeLocked(player, item);
         if (!isItemLocked && !isTypeLocked) {
            return false;
         } else {
            if (item.hasItemMeta()) {
               ItemMeta meta = item.getItemMeta();
               if (meta != null) {
                  PersistentDataContainer container = meta.getPersistentDataContainer();
                  if (container.has(this.lockKey, PersistentDataType.BYTE)) {
                     container.remove(this.lockKey);
                     if (this.plugin.getConfig().getBoolean("settings.show-lock-lore", true)) {
                        List<String> lore = meta.getLore();
                        if (lore != null && !lore.isEmpty()) {
                           String lockLore = ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("settings.lock-lore", "&c&lĐã khóa - Không thể vứt"));
                           lore.remove(lockLore);
                           meta.setLore(lore.isEmpty() ? null : lore);
                        }
                     }

                     item.setItemMeta(meta);
                  }
               }
            }

            this.dataManager.removeLockedItem(player, item);
            if (isTypeLocked && !isItemLocked) {
               this.plugin.getLogger().info("Item " + item.getType().toString() + " bị khóa theo loại, đang mở khóa...");
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public int lockAllItemsOfType(Player player, Material itemType) {
      int count = 0;
      UUID playerId = player.getUniqueId();
      int maxLockedItems = this.getPlayerItemLimit(player);
      int lockedCount = this.dataManager.getLockedItemCount(playerId);
      if (lockedCount >= maxLockedItems) {
         return 0;
      } else {
         ItemStack mainHand = player.getInventory().getItemInMainHand();
         if (mainHand != null && mainHand.getType() == itemType && this.lockItem(player, mainHand)) {
            ++count;
         }

         lockedCount = this.dataManager.getLockedItemCount(playerId);
         if (lockedCount >= maxLockedItems) {
            return count;
         } else {
            ItemStack[] var8 = player.getInventory().getContents();
            int var9 = var8.length;

            for(int var10 = 0; var10 < var9; ++var10) {
               ItemStack item = var8[var10];
               if (item != null && item.getType() == itemType && !this.isItemLocked(item)) {
                  if (this.lockItem(player, item)) {
                     ++count;
                  }

                  lockedCount = this.dataManager.getLockedItemCount(playerId);
                  if (lockedCount >= maxLockedItems) {
                     break;
                  }
               }
            }

            return count;
         }
      }
   }

   public int unlockAllItemsOfType(Player player, Material itemType) {
      int count = 0;
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      if (mainHand != null && mainHand.getType() == itemType && this.isItemLocked(mainHand) && this.unlockItem(player, mainHand)) {
         ++count;
      }

      ItemStack[] var5 = player.getInventory().getContents();
      int var6 = var5.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         ItemStack item = var5[var7];
         if (item != null && item.getType() == itemType && this.isItemLocked(item) && this.unlockItem(player, item)) {
            ++count;
         }
      }

      return count;
   }

   public int getLockedItemCount(Player player) {
      return this.dataManager.getLockedItemCount(player.getUniqueId());
   }

   public int getPlayerItemLimit(Player player) {
      int defaultLimit = this.plugin.getConfig().getInt("max-locked-items", 5);
      int highestLimit = defaultLimit;
      String[] var4 = (String[])player.getEffectivePermissions().stream().map((permInfo) -> {
         return permInfo.getPermission();
      }).filter((perm) -> {
         return perm != null;
      }).toArray((x$0) -> {
         return new String[x$0];
      });
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         String permission = var4[var6];
         Matcher matcher = this.limitPermissionPattern.matcher(permission);
         if (matcher.matches()) {
            try {
               int limit = Integer.parseInt(matcher.group(1));
               if (limit > highestLimit) {
                  highestLimit = limit;
               }
            } catch (NumberFormatException var10) {
            }
         }
      }

      return highestLimit;
   }

   public int getDefaultMaxLockedItems() {
      return this.plugin.getConfig().getInt("max-locked-items", 5);
   }
}
