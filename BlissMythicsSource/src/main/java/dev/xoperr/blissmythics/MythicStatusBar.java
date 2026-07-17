package dev.xoperr.blissmythics;

import dev.xoperr.blissgems.api.BlissGemsAPI;
import dev.xoperr.blissgems.api.CooldownEntry;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.List;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class MythicStatusBar {
   private final BlissMythics plugin;
   private final BlissGemsAPI api;
   private final HereticGem heretic;
   private final AuratusGem auratus;

   public MythicStatusBar(BlissMythics var1, BlissGemsAPI var2, HereticGem var3, AuratusGem var4) {
      this.plugin = var1;
      this.api = var2;
      this.heretic = var3;
      this.auratus = var4;
      Bukkit.getScheduler().runTaskTimer(var1, this::tick, 20L, 10L);
   }

   private void tick() {
      for (Player var2 : Bukkit.getOnlinePlayers()) {
         String var3 = this.heldMythicGemId(var2);
         if (var3 != null) {
            String var4 = this.build(var2, var3);
            if (!var4.isEmpty()) {
               var2.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(var4));
            }
         }
      }
   }

   private String heldMythicGemId(Player var1) {
      String var2 = this.gemIdOf(var1.getInventory().getItemInMainHand());
      if (var2 == null) {
         var2 = this.gemIdOf(var1.getInventory().getItemInOffHand());
      }

      return var2;
   }

   private String gemIdOf(ItemStack var1) {
      if (var1 != null && !var1.getType().isAir()) {
         String var2 = CustomItemManager.getIdByItem(var1);
         if (var2 == null) {
            return null;
         } else {
            String var3 = this.api.getGemRegistry().gemIdFromItemId(var2);
            return var3 == null || !"heretic".equals(var3) && !"auratus".equals(var3) ? null : var3;
         }
      } else {
         return null;
      }
   }

   private String build(Player var1, String var2) {
      List<CooldownEntry> var3 = this.api.getGemRegistry().getCooldownEntries(var2);
      if (var3 != null && !var3.isEmpty()) {
         String var4 = "heretic".equals(var2) ? "§4" : "§6";
         StringBuilder var5 = new StringBuilder();
         boolean var6 = true;

         for (CooldownEntry var8 : var3) {
            if (!var6) {
               var5.append(" ").append(var4).append("| ");
            }

            var6 = false;
            int var9 = this.api.getAbilityManager().getRemainingCooldown(var1, var8.getAbilityKey());
            boolean var10 = var8.getAbilityKey().equals("auratus-perforators") || var8.getAbilityKey().equals("heretic-bloodsaws");
            if (var10) {
               String var11 = var8.getDisplayName();
               if (var8.getAbilityKey().equals("auratus-perforators")) {
                  int var12 = this.auratus.chainCharges(var1);
                  var5.append("§f").append(var11).append(" ");
                  if (var12 >= 2) {
                     var5.append("§a2");
                  } else if (var12 == 1) {
                     var5.append("§e1");
                  } else {
                     var5.append("§c").append(this.auratus.nextChainChargeIn(var1)).append("s");
                  }
               } else {
                  var5.append("§f").append(var11).append(" ");
                  if (var9 > 0) {
                     var5.append("§c").append(var9).append("s");
                  } else if (this.heretic.chargeWindowActive(var1)) {
                     var5.append("§e1");
                  } else {
                     var5.append("§a2");
                  }
               }
            } else {
               var5.append(var4).append(var8.getDisplayName()).append(" ");
               if (var9 > 0) {
                  var5.append("§c").append(var9).append("s");
               } else {
                  var5.append("§aReady");
               }
            }
         }

         return var5.toString();
      } else {
         return "";
      }
   }
}
