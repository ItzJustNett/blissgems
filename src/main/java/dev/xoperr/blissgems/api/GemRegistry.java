/*
 * Decompiled with CFR 0.152.
 */
package dev.xoperr.blissgems.api;

import dev.xoperr.blissgems.api.CooldownEntry;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.api.GemDefinition;
import dev.xoperr.blissgems.api.GemPassiveHandler;
import java.util.Collection;
import java.util.List;

public interface GemRegistry {
    public void registerGem(GemDefinition var1);

    public void registerAbilities(String var1, GemAbilityHandler var2);

    public void registerPassives(String var1, GemPassiveHandler var2);

    public void registerCooldowns(String var1, List<CooldownEntry> var2);

    public GemDefinition getGem(String var1);

    public GemAbilityHandler getAbilityHandler(String var1);

    public GemPassiveHandler getPassiveHandler(String var1);

    public List<CooldownEntry> getCooldownEntries(String var1);

    public Collection<GemDefinition> getAllGems();

    public boolean isRegisteredGem(String var1);

    public String gemIdFromItemId(String var1);

    public int tierFromItemId(String var1);
}

