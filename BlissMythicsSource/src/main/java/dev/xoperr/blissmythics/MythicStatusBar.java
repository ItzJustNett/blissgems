package dev.xoperr.blissmythics;

import dev.xoperr.blissgems.api.BlissGemsAPI;
import dev.xoperr.blissgems.utils.CustomItemManager;
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

   // Bliss layout, all icons 8px so they sit inside the action bar line; mythics have no gem icon:
   //   Heretic: [bloodsaws] charges  Bloodstorm  [bloodlinking] cooldown
   //   Auratus: [perforators] chains  [aegis] cooldown
   private String build(Player var1, String var2) {
      StringBuilder var3 = new StringBuilder();
      if ("heretic".equals(var2)) {
         int var4 = this.api.getAbilityManager().getRemainingCooldown(var1, "heretic-bloodsaws");
         var3.append("§f\ua423 ");
         if (var4 > 0) {
            var3.append("§c").append(var4).append("s");
         } else if (this.heretic.chargeWindowActive(var1)) {
            var3.append("§e1");
         } else {
            var3.append("§a2");
         }

         var3.append("  ").append(this.status(var1, "heretic-bloodstorm"));
         var3.append("  §f\ua421 ").append(this.status(var1, "heretic-bloodlink"));
      } else {
         int var5 = this.auratus.chainCharges(var1);
         var3.append("§f\u22b3 ");
         if (var5 >= 2) {
            var3.append("§a2");
         } else if (var5 == 1) {
            var3.append("§e1");
         } else {
            var3.append("§c").append(this.auratus.nextChainChargeIn(var1)).append("s");
         }

         var3.append("  §f\u22b2 ").append(this.status(var1, "auratus-aegis"));
      }

      return var3.toString();
   }

   private String status(Player var1, String var2) {
      int var3 = this.api.getAbilityManager().getRemainingCooldown(var1, var2);
      return var3 > 0 ? "§c" + var3 + "s" : "§aReady!";
   }
}
