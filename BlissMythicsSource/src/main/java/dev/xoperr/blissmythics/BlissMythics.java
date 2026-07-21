package dev.xoperr.blissmythics;

import dev.xoperr.blissgems.api.BlissGemsAPI;
import dev.xoperr.blissgems.api.CooldownEntry;
import dev.xoperr.blissgems.api.GemDefinition;
import dev.xoperr.blissgems.api.GemRegistry;
import dev.xoperr.blissgems.api.GemDefinition.Builder;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlissMythics extends JavaPlugin {
   private BlissGemsAPI api;
   private HereticGem heretic;
   private AuratusGem auratus;
   private MythicDeathListener deathListener;

   public void onEnable() {
      this.saveDefaultConfig();
      this.api = (BlissGemsAPI)this.getServer().getServicesManager().load(BlissGemsAPI.class);
      if (this.api == null) {
         this.getLogger().severe("BlissGems API not found - disabling.");
         this.getServer().getPluginManager().disablePlugin(this);
      } else {
         GemRegistry var1 = this.api.getGemRegistry();
         GemDefinition var2 = new Builder("heretic")
            .displayName("Heretic")
            .description("fear is only salvation")
            .color("§4")
            .plugin("BlissMythics")
            .maxTier(2)
            .material(Material.AMETHYST_SHARD)
            .t1CustomModelData(5001)
            .t2CustomModelData(5002)
            .t2DisplayName("§4§lHeretic §fGem")
            .t2Lore(
               List.of(
                  "§f§lfear is only salvation",
                  "§f (§dMythic§f)",
                  "",
                  "§4Passives:",
                  "§7- Hemorrhage",
                  "§7- Enduring Strength",
                  "§7- Auto Crit",
                  "§7- Blood Hardening",
                  "",
                  "§4Powers:",
                  "§7- §4Bloodsaws §8(use, 2 charges)",
                  "§7- §4Bloodlinking §8(shift+use)",
                  "§7- §4Bloodstorm §8(combo + slam)"
               )
            )
            .build();
         var1.registerGem(var2);
         GemDefinition var3 = new Builder("auratus")
            .displayName("Auratus")
            .description("blessed by the honored one")
            .color("§6")
            .plugin("BlissMythics")
            .maxTier(2)
            .material(Material.AMETHYST_SHARD)
            .t1CustomModelData(5003)
            .t2CustomModelData(5004)
            .t2DisplayName("§6§lAuratus §fGem")
            .t2Lore(
               List.of(
                  "§f§lblessed by the honored one",
                  "§f (§dMythic§f)",
                  "",
                  "§6Passives:",
                  "§7- Divine Purity",
                  "§7- Angels Grasp",
                  "§7- Feathered Fall",
                  "§7- Hauling Strike",
                  "",
                  "§6Powers:",
                  "§7- §6Venerated Perforators §8(use, 2 chains)",
                  "§7- §6Echoing Aegis §8(shift+use)",
                  "§7- §6Chained Judgment §8(parry finisher)"
               )
            )
            .build();
         var1.registerGem(var3);
         this.registerItems(var2, var3);
         this.heretic = new HereticGem(this, this.api);
         this.auratus = new AuratusGem(this, this.api);
         var1.registerAbilities("heretic", this.heretic);
         var1.registerPassives("heretic", this.heretic);
         var1.registerAbilities("auratus", this.auratus);
         var1.registerPassives("auratus", this.auratus);
         var1.registerCooldowns(
            "heretic", List.of(new CooldownEntry("heretic-bloodsaws", "\ud83e\udd1f"), new CooldownEntry("heretic-bloodlink", "\ud83e\udd0c"))
         );
         var1.registerCooldowns("auratus", List.of(new CooldownEntry("auratus-perforators", "⊵"), new CooldownEntry("auratus-aegis", "⊴")));
         // MythicDeathListener intentionally NOT registered: BlissGems already keeps/restores
         // gems on death (gems.droppable-on-death). Dropping mythics here as well produced a
         // duplicated drop, so mythics now follow the standard gem death behaviour.
         new MythicStatusBar(this, this.api, this.heretic, this.auratus);
         this.getServer().getPluginManager().registerEvents(this.heretic, this);
         this.getServer().getPluginManager().registerEvents(this.auratus, this);
         this.getLogger().info("Registered mythic gems: heretic, auratus");
      }
   }

   public void onDisable() {
      if (this.deathListener != null) {
         this.deathListener.save();
      }
   }

   private void registerItems(GemDefinition... var1) {
      for (GemDefinition var5 : var1) {
         for (int var6 = 1; var6 <= var5.getMaxTier(); var6++) {
            String var7 = var5.buildItemId(var6);
            int var8 = var6 == 2 ? var5.getT2CustomModelData() : var5.getT1CustomModelData();
            String var9 = var6 == 2 && var5.getT2DisplayName() != null ? var5.getT2DisplayName() : var5.getColor() + "§l" + var5.getDisplayName() + " §fGem";
            List var10 = var6 == 2 ? var5.getT2Lore() : var5.getT1Lore();

            try {
               CustomItemManager.registerAddonItem(var7, var5.getMaterial(), var8, var9, var10);
            } catch (IllegalArgumentException var12) {
            }
         }
      }
   }

   public BlissGemsAPI api() {
      return this.api;
   }

   public boolean holds(Player var1, String var2) {
      return this.api != null && this.api.playerHasGem(var1, var2);
   }
}
