package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.*;
import dev.xoperr.blissgems.utils.CustomItemManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of {@link GemRegistry}. Both built-in and addon gems
 * register through this class.
 */
public class GemRegistryImpl implements GemRegistry {

    private static final Pattern GEM_ITEM_PATTERN = Pattern.compile("^(.+)_gem_t(\\d+)$");

    private final BlissGems plugin;
    private final Logger logger;

    private final Map<String, GemDefinition> gems = new ConcurrentHashMap<>();
    private final Map<String, GemAbilityHandler> abilityHandlers = new ConcurrentHashMap<>();
    private final Map<String, GemPassiveHandler> passiveHandlers = new ConcurrentHashMap<>();
    private final Map<String, List<CooldownEntry>> cooldownEntries = new ConcurrentHashMap<>();

    public GemRegistryImpl(BlissGems plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    @Override
    public void registerGem(GemDefinition definition) {
        String id = definition.getId();
        if (gems.containsKey(id)) {
            // Idempotent: a duplicate registration (two addons claiming the same id, a copy
            // of an addon, or a re-register on reload) must NOT crash the caller's onEnable.
            // Keep the first registration and skip the duplicate.
            this.plugin.getLogger().warning("Gem ID '" + id + "' is already registered — keeping the existing gem, skipping the duplicate.");
            return;
        }

        gems.put(id, definition);

        // For addon gems (non-BlissGems plugins), register items via CustomItemManager
        if (!"BlissGems".equals(definition.getPluginName())) {
            registerAddonItems(definition);
        }

        logger.info("[AddonAPI] Registered gem: " + id + " (" + definition.getPluginName() + ")");
    }

    private void registerAddonItems(GemDefinition def) {
        // Register T1
        if (def.getT1CustomModelData() > 0) {
            String t1Id = def.buildItemId(1);
            String t1Name = def.getT1DisplayName() != null ? def.getT1DisplayName()
                    : def.getColor() + "\u00a7l" + def.getDisplayName().toUpperCase() + " GEM";
            CustomItemManager.registerAddonItem(t1Id, def.getMaterial(),
                    def.getT1CustomModelData(), t1Name, def.getT1Lore());
        }

        // Register T2
        if (def.getMaxTier() >= 2 && def.getT2CustomModelData() > 0) {
            String t2Id = def.buildItemId(2);
            String t2Name = def.getT2DisplayName() != null ? def.getT2DisplayName()
                    : def.getColor() + "\u00a7l" + def.getDisplayName().toUpperCase() + " GEM";
            CustomItemManager.registerAddonItem(t2Id, def.getMaterial(),
                    def.getT2CustomModelData(), t2Name, def.getT2Lore());
        }
    }

    @Override
    public void registerAbilities(String gemId, GemAbilityHandler handler) {
        abilityHandlers.put(gemId, handler);
    }

    @Override
    public void registerPassives(String gemId, GemPassiveHandler handler) {
        passiveHandlers.put(gemId, handler);
    }

    @Override
    public void registerCooldowns(String gemId, List<CooldownEntry> entries) {
        cooldownEntries.put(gemId, Collections.unmodifiableList(new ArrayList<>(entries)));
    }

    @Override
    public GemDefinition getGem(String gemId) {
        return gems.get(gemId);
    }

    @Override
    public GemAbilityHandler getAbilityHandler(String gemId) {
        return abilityHandlers.get(gemId);
    }

    @Override
    public GemPassiveHandler getPassiveHandler(String gemId) {
        return passiveHandlers.get(gemId);
    }

    @Override
    public List<CooldownEntry> getCooldownEntries(String gemId) {
        return cooldownEntries.getOrDefault(gemId, Collections.emptyList());
    }

    @Override
    public Collection<GemDefinition> getAllGems() {
        return Collections.unmodifiableCollection(gems.values());
    }

    @Override
    public boolean isRegisteredGem(String itemId) {
        return gemIdFromItemId(itemId) != null;
    }

    @Override
    public String gemIdFromItemId(String itemId) {
        if (itemId == null) return null;
        Matcher matcher = GEM_ITEM_PATTERN.matcher(itemId);
        if (matcher.matches()) {
            String candidateId = matcher.group(1);
            if (gems.containsKey(candidateId)) {
                return candidateId;
            }
        }
        return null;
    }

    @Override
    public int tierFromItemId(String itemId) {
        if (itemId == null) return 1;
        Matcher matcher = GEM_ITEM_PATTERN.matcher(itemId);
        if (matcher.matches()) {
            try {
                return Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }
}
