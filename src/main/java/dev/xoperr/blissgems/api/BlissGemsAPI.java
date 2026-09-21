/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package dev.xoperr.blissgems.api;

import dev.xoperr.blissgems.api.GemRegistry;
import dev.xoperr.blissgems.managers.AbilityManager;
import dev.xoperr.blissgems.managers.EnergyManager;
import dev.xoperr.blissgems.managers.GemManager;
import dev.xoperr.blissgems.managers.TrustedPlayersManager;
import dev.xoperr.blissgems.utils.ConfigManager;
import org.bukkit.entity.Player;

public interface BlissGemsAPI {
    public GemRegistry getGemRegistry();

    public AbilityManager getAbilityManager();

    public EnergyManager getEnergyManager();

    public ConfigManager getConfigManager();

    public GemManager getGemManager();

    public TrustedPlayersManager getTrustedPlayersManager();

    public boolean playerHasGem(Player var1, String var2);
}

