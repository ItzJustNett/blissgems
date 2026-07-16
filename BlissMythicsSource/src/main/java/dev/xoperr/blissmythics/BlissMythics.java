/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.xoperr.blissgems.api.BlissGemsAPI
 *  dev.xoperr.blissgems.api.CooldownEntry
 *  dev.xoperr.blissgems.api.GemAbilityHandler
 *  dev.xoperr.blissgems.api.GemDefinition
 *  dev.xoperr.blissgems.api.GemDefinition$Builder
 *  dev.xoperr.blissgems.api.GemPassiveHandler
 *  dev.xoperr.blissgems.api.GemRegistry
 *  dev.xoperr.blissgems.utils.CustomItemManager
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package dev.xoperr.blissmythics;

import dev.xoperr.blissgems.api.BlissGemsAPI;
import dev.xoperr.blissgems.api.CooldownEntry;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.api.GemDefinition;
import dev.xoperr.blissgems.api.GemPassiveHandler;
import dev.xoperr.blissgems.api.GemRegistry;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissmythics.AuratusGem;
import dev.xoperr.blissmythics.HereticGem;
import dev.xoperr.blissmythics.MythicDeathListener;
import dev.xoperr.blissmythics.MythicStatusBar;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlissMythics
extends JavaPlugin {
    private BlissGemsAPI api;
    private HereticGem heretic;
    private AuratusGem auratus;
    private MythicDeathListener deathListener;

    public void onEnable() {
        this.api = (BlissGemsAPI)this.getServer().getServicesManager().load(BlissGemsAPI.class);
        if (this.api == null) {
            this.getLogger().severe("BlissGems API not found - disabling.");
            this.getServer().getPluginManager().disablePlugin((Plugin)this);
            return;
        }
        GemRegistry gemRegistry = this.api.getGemRegistry();
        GemDefinition gemDefinition = new GemDefinition.Builder("heretic").displayName("Heretic").description("fear is only salvation").color("\u00a74").plugin("BlissMythics").maxTier(2).material(Material.AMETHYST_SHARD).t1CustomModelData(5001).t2CustomModelData(5002).t2DisplayName("\u00a74\u00a7lHeretic \u00a7fGem").t2Lore(List.of("\u00a7f\u00a7lfear is only salvation", "\u00a7f (\u00a7dMythic\u00a7f)", "", "\u00a74Passives:", "\u00a77- Hemorrhage", "\u00a77- Enduring Strength", "\u00a77- Auto Crit", "\u00a77- Blood Hardening", "", "\u00a74Powers:", "\u00a77- \u00a74Bloodsaws \u00a78(use, 2 charges)", "\u00a77- \u00a74Bloodlinking \u00a78(shift+use)")).build();
        gemRegistry.registerGem(gemDefinition);
        GemDefinition gemDefinition2 = new GemDefinition.Builder("auratus").displayName("Auratus").description("blessed by the honored one").color("\u00a76").plugin("BlissMythics").maxTier(2).material(Material.AMETHYST_SHARD).t1CustomModelData(5003).t2CustomModelData(5004).t2DisplayName("\u00a76\u00a7lAuratus \u00a7fGem").t2Lore(List.of("\u00a7f\u00a7lblessed by the honored one", "\u00a7f (\u00a7dMythic\u00a7f)", "", "\u00a76Passives:", "\u00a77- Divine Purity", "\u00a77- Angels Grasp", "\u00a77- Feathered Fall", "\u00a77- Hauling Strike", "", "\u00a76Powers:", "\u00a77- \u00a76Venerated Perforators \u00a78(use, 2 chains)", "\u00a77- \u00a76Echoing Aegis \u00a78(shift+use)")).build();
        gemRegistry.registerGem(gemDefinition2);
        this.registerItems(gemDefinition, gemDefinition2);
        this.heretic = new HereticGem(this, this.api);
        this.auratus = new AuratusGem(this, this.api);
        gemRegistry.registerAbilities("heretic", (GemAbilityHandler)this.heretic);
        gemRegistry.registerPassives("heretic", (GemPassiveHandler)this.heretic);
        gemRegistry.registerAbilities("auratus", (GemAbilityHandler)this.auratus);
        gemRegistry.registerPassives("auratus", (GemPassiveHandler)this.auratus);
        gemRegistry.registerCooldowns("heretic", List.of(new CooldownEntry("heretic-bloodsaws", "\ud83e\udd1f"), new CooldownEntry("heretic-bloodlink", "\ud83e\udd0c")));
        gemRegistry.registerCooldowns("auratus", List.of(new CooldownEntry("auratus-perforators", "\u22b5"), new CooldownEntry("auratus-aegis", "\u22b4")));
        this.deathListener = new MythicDeathListener(this, this.api);
        new MythicStatusBar(this, this.api, this.heretic, this.auratus);
        this.getServer().getPluginManager().registerEvents((Listener)this.heretic, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)this.auratus, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)this.deathListener, (Plugin)this);
        this.getLogger().info("Registered mythic gems: heretic, auratus");
    }

    public void onDisable() {
        if (this.deathListener != null) {
            this.deathListener.save();
        }
    }

    private void registerItems(GemDefinition ... gemDefinitionArray) {
        for (GemDefinition gemDefinition : gemDefinitionArray) {
            for (int i = 1; i <= gemDefinition.getMaxTier(); ++i) {
                String string = gemDefinition.buildItemId(i);
                int n = i == 2 ? gemDefinition.getT2CustomModelData() : gemDefinition.getT1CustomModelData();
                Object object = i == 2 && gemDefinition.getT2DisplayName() != null ? gemDefinition.getT2DisplayName() : gemDefinition.getColor() + "\u00a7l" + gemDefinition.getDisplayName() + " \u00a7fGem";
                List list = i == 2 ? gemDefinition.getT2Lore() : gemDefinition.getT1Lore();
                try {
                    CustomItemManager.registerAddonItem((String)string, (Material)gemDefinition.getMaterial(), (int)n, (String)object, (List)list);
                    continue;
                }
                catch (IllegalArgumentException illegalArgumentException) {
                    // empty catch block
                }
            }
        }
    }

    public BlissGemsAPI api() {
        return this.api;
    }

    public boolean holds(Player player, String string) {
        return this.api != null && this.api.playerHasGem(player, string);
    }
}

