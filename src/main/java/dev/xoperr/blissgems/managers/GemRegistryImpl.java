/*
 * Decompiled with CFR 0.152.
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.CooldownEntry;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.api.GemDefinition;
import dev.xoperr.blissgems.api.GemPassiveHandler;
import dev.xoperr.blissgems.api.GemRegistry;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GemRegistryImpl
implements GemRegistry {
    private static final Pattern GEM_ITEM_PATTERN = Pattern.compile("^(.+)_gem_t(\\d+)$");
    private final BlissGems plugin;
    private final Logger logger;
    private final Map<String, GemDefinition> gems = new ConcurrentHashMap<String, GemDefinition>();
    private final Map<String, GemAbilityHandler> abilityHandlers = new ConcurrentHashMap<String, GemAbilityHandler>();
    private final Map<String, GemPassiveHandler> passiveHandlers = new ConcurrentHashMap<String, GemPassiveHandler>();
    private final Map<String, List<CooldownEntry>> cooldownEntries = new ConcurrentHashMap<String, List<CooldownEntry>>();

    public GemRegistryImpl(BlissGems plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    @Override
    public void registerGem(GemDefinition definition) {
        String id = definition.getId();
        if (this.gems.containsKey(id)) {
            this.plugin.getLogger().warning("Gem ID '" + id + "' is already registered \u2014 keeping the existing gem, skipping the duplicate.");
            return;
        }
        this.gems.put(id, definition);
        if (!"BlissGems".equals(definition.getPluginName())) {
            this.registerAddonItems(definition);
        }
        this.logger.info("[AddonAPI] Registered gem: " + id + " (" + definition.getPluginName() + ")");
    }

    private void registerAddonItems(GemDefinition def) {
        if (def.getT1CustomModelData() > 0) {
            String t1Id = def.buildItemId(1);
            String t1Name = def.getT1DisplayName() != null ? def.getT1DisplayName() : def.getColor() + "\u00a7l" + def.getDisplayName().toUpperCase() + " GEM";
            CustomItemManager.registerAddonItem(t1Id, def.getMaterial(), def.getT1CustomModelData(), t1Name, def.getT1Lore());
        }
        if (def.getMaxTier() >= 2 && def.getT2CustomModelData() > 0) {
            String t2Id = def.buildItemId(2);
            String t2Name = def.getT2DisplayName() != null ? def.getT2DisplayName() : def.getColor() + "\u00a7l" + def.getDisplayName().toUpperCase() + " GEM";
            CustomItemManager.registerAddonItem(t2Id, def.getMaterial(), def.getT2CustomModelData(), t2Name, def.getT2Lore());
        }
    }

    @Override
    public void registerAbilities(String gemId, GemAbilityHandler handler) {
        this.abilityHandlers.put(gemId, handler);
    }

    @Override
    public void registerPassives(String gemId, GemPassiveHandler handler) {
        this.passiveHandlers.put(gemId, handler);
    }

    @Override
    public void registerCooldowns(String gemId, List<CooldownEntry> entries) {
        this.cooldownEntries.put(gemId, Collections.unmodifiableList(new ArrayList<CooldownEntry>(entries)));
    }

    @Override
    public GemDefinition getGem(String gemId) {
        return this.gems.get(gemId);
    }

    @Override
    public GemAbilityHandler getAbilityHandler(String gemId) {
        return this.abilityHandlers.get(gemId);
    }

    @Override
    public GemPassiveHandler getPassiveHandler(String gemId) {
        return this.passiveHandlers.get(gemId);
    }

    @Override
    public List<CooldownEntry> getCooldownEntries(String gemId) {
        return this.cooldownEntries.getOrDefault(gemId, Collections.emptyList());
    }

    @Override
    public Collection<GemDefinition> getAllGems() {
        return Collections.unmodifiableCollection(this.gems.values());
    }

    @Override
    public boolean isRegisteredGem(String itemId) {
        return this.gemIdFromItemId(itemId) != null;
    }

    @Override
    public String gemIdFromItemId(String itemId) {
        String candidateId;
        if (itemId == null) {
            return null;
        }
        Matcher matcher = GEM_ITEM_PATTERN.matcher(itemId);
        if (matcher.matches() && this.gems.containsKey(candidateId = matcher.group(1))) {
            return candidateId;
        }
        return null;
    }

    @Override
    public int tierFromItemId(String itemId) {
        if (itemId == null) {
            return 1;
        }
        Matcher matcher = GEM_ITEM_PATTERN.matcher(itemId);
        if (matcher.matches()) {
            try {
                return Integer.parseInt(matcher.group(2));
            }
            catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }
}

