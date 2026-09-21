/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.api.GemDefinition;
import dev.xoperr.blissgems.managers.GemRegistryImpl;
import dev.xoperr.blissgems.managers.GoldGemManager;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GemManager {
    private final BlissGems plugin;
    private final Map<UUID, ActiveGem> activeGems;
    private final Map<UUID, Integer> channelTierOverride = new ConcurrentHashMap<UUID, Integer>();

    public GemManager(BlissGems plugin) {
        this.plugin = plugin;
        this.activeGems = new HashMap<UUID, ActiveGem>();
    }

    public void updateActiveGem(Player player) {
        GemAbilityHandler handler;
        ActiveGem currentGem = this.activeGems.get(player.getUniqueId());
        ItemStack[] contents = player.getInventory().getContents();
        ActiveGem foundGem = null;
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        for (ItemStack item : contents) {
            int tier;
            String itemId;
            if (item == null || (itemId = CustomItemManager.getIdByItem(item)) == null) continue;
            if (GemType.isGem(itemId)) {
                GemType type = GemType.fromOraxenId(itemId);
                tier = GemType.getTierFromOraxenId(itemId);
                if (type != null && this.plugin.getConfigManager().isGemEnabled(type)) {
                    foundGem = new ActiveGem(type, tier);
                    break;
                }
            }
            if (registry == null || !registry.isRegisteredGem(itemId)) continue;
            String gemId = registry.gemIdFromItemId(itemId);
            tier = registry.tierFromItemId(itemId);
            foundGem = new ActiveGem(gemId, tier);
            break;
        }
        if (!(currentGem == null || foundGem != null && Objects.equals(currentGem.getGemId(), foundGem.getGemId()) || currentGem.getGemId() == null || registry == null || (handler = registry.getAbilityHandler(currentGem.getGemId())) == null)) {
            handler.cleanup(player);
        }
        if (foundGem != null) {
            this.activeGems.put(player.getUniqueId(), foundGem);
        } else {
            this.activeGems.remove(player.getUniqueId());
        }
    }

    public ActiveGem getActiveGem(Player player) {
        return this.activeGems.get(player.getUniqueId());
    }

    public boolean hasActiveGem(Player player) {
        return this.activeGems.containsKey(player.getUniqueId());
    }

    public GemType getGemType(Player player) {
        ActiveGem gem = this.getActiveGem(player);
        return gem != null ? gem.getType() : null;
    }

    public void setChannelTierOverride(UUID id, int tier) {
        this.channelTierOverride.put(id, tier);
    }

    public void clearChannelTierOverride(UUID id) {
        this.channelTierOverride.remove(id);
    }

    public int getGemTier(Player player) {
        Integer override = this.channelTierOverride.get(player.getUniqueId());
        if (override != null) {
            return override;
        }
        ActiveGem gem = this.getActiveGem(player);
        return gem != null ? gem.getTier() : 1;
    }

    public String getGemId(Player player) {
        ActiveGem gem = this.getActiveGem(player);
        return gem != null ? gem.getGemId() : null;
    }

    public boolean hasGemType(Player player, GemType type) {
        ActiveGem gem = this.getActiveGem(player);
        return gem != null && gem.getType() == type;
    }

    private String getHeldGemItemId(Player player) {
        String itemId;
        ItemStack offhand = player.getInventory().getItemInOffHand();
        String string = itemId = offhand != null ? CustomItemManager.getIdByItem(offhand) : null;
        if (itemId != null && this.isAnyGem(itemId)) {
            return itemId;
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        String string2 = itemId = mainHand != null ? CustomItemManager.getIdByItem(mainHand) : null;
        if (itemId != null && this.isAnyGem(itemId)) {
            return itemId;
        }
        return null;
    }

    public boolean hasGemInOffhand(Player player) {
        String itemId = this.getHeldGemItemId(player);
        if (itemId == null) {
            return false;
        }
        if (GemType.isGem(itemId)) {
            GemType type = GemType.fromOraxenId(itemId);
            return type != null && this.plugin.getConfigManager().isGemEnabled(type);
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        return registry != null && registry.isRegisteredGem(itemId);
    }

    public boolean hasGemTypeInOffhand(Player player, GemType type) {
        return this.isGemOfType(player.getInventory().getItemInOffHand(), type) || this.isGemOfType(player.getInventory().getItemInMainHand(), type) || this.hasHarvestedSoul(player, type);
    }

    private boolean hasHarvestedSoul(Player player, GemType type) {
        GoldGemManager gold = this.plugin.getGoldGemManager();
        if (gold == null || type == null) {
            return false;
        }
        return gold.getHarvested(player.getUniqueId()).containsKey(type.getId()) && gold.holdsGoldGem(player);
    }

    public int getTierFor(Player player, GemType type) {
        if (type != null && !this.isGemOfType(player.getInventory().getItemInOffHand(), type) && !this.isGemOfType(player.getInventory().getItemInMainHand(), type)) {
            GoldGemManager.Harvest soul;
            GoldGemManager gold = this.plugin.getGoldGemManager();
            GoldGemManager.Harvest harvest = soul = gold != null ? gold.getHarvested(player.getUniqueId()).get(type.getId()) : null;
            if (soul != null) {
                return soul.tier();
            }
        }
        return this.getTierFromOffhand(player);
    }

    private boolean isGemOfType(ItemStack item, GemType type) {
        if (item == null) {
            return false;
        }
        String itemId = CustomItemManager.getIdByItem(item);
        if (itemId == null || !GemType.isGem(itemId)) {
            return false;
        }
        return GemType.fromOraxenId(itemId) == type;
    }

    public GemType getGemTypeFromOffhand(Player player) {
        String itemId = this.getHeldGemItemId(player);
        if (itemId == null || !GemType.isGem(itemId)) {
            return null;
        }
        return GemType.fromOraxenId(itemId);
    }

    public String getGemIdFromOffhand(Player player) {
        String itemId = this.getHeldGemItemId(player);
        if (itemId == null) {
            return null;
        }
        GemType type = GemType.fromOraxenId(itemId);
        if (type != null) {
            return type.getId();
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        if (registry != null) {
            return registry.gemIdFromItemId(itemId);
        }
        return null;
    }

    public int getTierFromOffhand(Player player) {
        String itemId = this.getHeldGemItemId(player);
        if (itemId == null) {
            return 1;
        }
        if (GemType.isGem(itemId)) {
            return GemType.getTierFromOraxenId(itemId);
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        if (registry != null && registry.isRegisteredGem(itemId)) {
            return registry.tierFromItemId(itemId);
        }
        return 1;
    }

    private String getPassiveGemItemId(Player player) {
        String held = this.getHeldGemItemId(player);
        if (held != null) {
            return held;
        }
        if (!this.plugin.getConfig().getBoolean("passives.apply-in-hotbar", true)) {
            return null;
        }
        for (int slot = 0; slot < 9; ++slot) {
            String id;
            ItemStack it = player.getInventory().getItem(slot);
            String string = id = it != null ? CustomItemManager.getIdByItem(it) : null;
            if (id == null || !this.isAnyGem(id)) continue;
            return id;
        }
        return null;
    }

    public boolean hasGemForPassives(Player player) {
        return this.getPassiveGemItemId(player) != null;
    }

    public GemType getGemTypeForPassives(Player player) {
        String itemId = this.getPassiveGemItemId(player);
        if (itemId == null || !GemType.isGem(itemId)) {
            return null;
        }
        return GemType.fromOraxenId(itemId);
    }

    public String getGemIdForPassives(Player player) {
        String itemId = this.getPassiveGemItemId(player);
        if (itemId == null) {
            return null;
        }
        GemType type = GemType.fromOraxenId(itemId);
        if (type != null) {
            return type.getId();
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        return registry != null ? registry.gemIdFromItemId(itemId) : null;
    }

    public int getTierForPassives(Player player) {
        String itemId = this.getPassiveGemItemId(player);
        if (itemId == null) {
            return 1;
        }
        if (GemType.isGem(itemId)) {
            return GemType.getTierFromOraxenId(itemId);
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        if (registry != null && registry.isRegisteredGem(itemId)) {
            return registry.tierFromItemId(itemId);
        }
        return 1;
    }

    public boolean giveGem(Player player, GemType type, int tier) {
        int energy;
        String itemId = GemType.buildOraxenId(type, tier);
        ItemStack gem = CustomItemManager.getItemById(itemId, energy = this.plugin.getEnergyManager().getEnergy(player));
        if (gem != null) {
            player.getInventory().addItem(new ItemStack[]{gem});
            this.updateActiveGem(player);
            return true;
        }
        return false;
    }

    public boolean giveGem(Player player, String gemId, int tier) {
        int energy;
        String itemId;
        ItemStack gem;
        GemDefinition def;
        for (GemType type : GemType.values()) {
            if (!type.getId().equalsIgnoreCase(gemId)) continue;
            return this.giveGem(player, type, tier);
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        if (registry != null && (def = registry.getGem(gemId)) != null && (gem = CustomItemManager.getItemById(itemId = def.buildItemId(tier), energy = this.plugin.getEnergyManager().getEnergy(player))) != null) {
            player.getInventory().addItem(new ItemStack[]{gem});
            this.updateActiveGem(player);
            return true;
        }
        return false;
    }

    public ItemStack findGemInInventory(Player player) {
        String itemId;
        String offId;
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && (offId = CustomItemManager.getIdByItem(offhand)) != null && (GemType.isGem(offId) || registry != null && registry.isRegisteredGem(offId))) {
            return offhand;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || (itemId = CustomItemManager.getIdByItem(item)) == null) continue;
            if (GemType.isGem(itemId)) {
                return item;
            }
            if (registry == null || !registry.isRegisteredGem(itemId)) continue;
            return item;
        }
        for (ItemStack item : player.getEnderChest().getContents()) {
            if (item == null || (itemId = CustomItemManager.getIdByItem(item)) == null) continue;
            if (GemType.isGem(itemId)) {
                return item;
            }
            if (registry == null || !registry.isRegisteredGem(itemId)) continue;
            return item;
        }
        return null;
    }

    public boolean isAnyGem(String itemId) {
        if (itemId == null) {
            return false;
        }
        if (GemType.isGem(itemId)) {
            return true;
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        return registry != null && registry.isRegisteredGem(itemId);
    }

    public static GemType builtInType(String gemId) {
        if (gemId == null) {
            return null;
        }
        for (GemType type : GemType.values()) {
            if (!type.getId().equalsIgnoreCase(gemId)) continue;
            return type;
        }
        return null;
    }

    public List<String> getAvailableGemIds() {
        ArrayList<String> ids = new ArrayList<String>();
        for (GemType type : GemType.values()) {
            if (!this.plugin.getConfigManager().isGemEnabled(type)) continue;
            ids.add(type.getId());
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        if (registry != null) {
            List excluded = this.plugin.getConfig().contains("gems.exclude-from-random") ? this.plugin.getConfig().getStringList("gems.exclude-from-random") : List.of("auratus", "heretic", "gold");
            for (GemDefinition def : registry.getAllGems()) {
                if ("gold".equals(def.getId()) || GemManager.builtInType(def.getId()) != null || ids.contains(def.getId()) || excluded.contains(def.getId())) continue;
                ids.add(def.getId());
            }
        }
        return ids;
    }

    public String getGemDisplayName(String gemId) {
        GemType type = GemManager.builtInType(gemId);
        if (type != null) {
            return type.getDisplayName();
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        GemDefinition def = registry != null ? registry.getGem(gemId) : null;
        return def != null ? def.getDisplayName() : gemId;
    }

    public String getGemColorCode(String gemId) {
        GemType type = GemManager.builtInType(gemId);
        if (type != null) {
            return type.getColor();
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        GemDefinition def = registry != null ? registry.getGem(gemId) : null;
        return def != null ? def.getColor() : "\u00a77";
    }

    public boolean giveGemToOffhand(Player player, String gemId, int tier) {
        int energy;
        GemDefinition def;
        GemType type = GemManager.builtInType(gemId);
        if (type != null) {
            return this.giveGemToOffhand(player, type, tier);
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        GemDefinition gemDefinition = def = registry != null ? registry.getGem(gemId) : null;
        if (def == null) {
            return false;
        }
        String itemId = def.buildItemId(tier);
        ItemStack gem = CustomItemManager.getItemById(itemId, energy = this.plugin.getEnergyManager().getEnergy(player));
        if (gem == null) {
            return false;
        }
        ItemStack current = player.getInventory().getItemInOffHand();
        if (current == null || current.getType().isAir()) {
            player.getInventory().setItemInOffHand(gem);
        } else {
            player.getInventory().addItem(new ItemStack[]{gem});
        }
        this.updateActiveGem(player);
        return true;
    }

    public boolean replaceGemType(Player player, GemType newType) {
        return this.replaceGem(player, newType.getId());
    }

    public boolean replaceGem(Player player, String newGemId) {
        int energy;
        if (newGemId == null) {
            return false;
        }
        ItemStack currentGem = this.findGemInInventory(player);
        if (currentGem == null) {
            return false;
        }
        String currentId = CustomItemManager.getIdByItem(currentGem);
        if (currentId == null) {
            return false;
        }
        int tier = GemType.getTierFromOraxenId(currentId);
        String newId = newGemId + "_gem_t" + tier;
        ItemStack newGem = CustomItemManager.getItemById(newId, energy = this.plugin.getEnergyManager().getEnergy(player));
        if (newGem == null) {
            return false;
        }
        for (int i = 0; i < player.getInventory().getSize(); ++i) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || !item.equals((Object)currentGem)) continue;
            player.getInventory().setItem(i, newGem);
            this.updateActiveGem(player);
            return true;
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && offhand.equals((Object)currentGem)) {
            player.getInventory().setItemInOffHand(newGem);
            this.updateActiveGem(player);
            return true;
        }
        return false;
    }

    public boolean giveGemToOffhand(Player player, GemType type, int tier) {
        int energy;
        String itemId = GemType.buildOraxenId(type, tier);
        ItemStack gem = CustomItemManager.getItemById(itemId, energy = this.plugin.getEnergyManager().getEnergy(player));
        if (gem == null) {
            return false;
        }
        ItemStack current = player.getInventory().getItemInOffHand();
        if (current == null || current.getType().isAir()) {
            player.getInventory().setItemInOffHand(gem);
        } else {
            player.getInventory().addItem(new ItemStack[]{gem});
        }
        this.updateActiveGem(player);
        return true;
    }

    public boolean upgradeGem(Player player, GemType type) {
        return type != null && this.upgradeGem(player, type.getId());
    }

    public boolean upgradeGem(Player player, String gemId) {
        int energy;
        String newId;
        ItemStack newGem;
        if (gemId == null) {
            return false;
        }
        ItemStack currentGem = this.findGemInInventory(player);
        if (currentGem == null) {
            return false;
        }
        String currentId = CustomItemManager.getIdByItem(currentGem);
        if (currentId == null) {
            return false;
        }
        if (!currentId.equals(gemId + "_gem_t1")) {
            return false;
        }
        if (GemManager.builtInType(gemId) == null) {
            GemDefinition def;
            GemRegistryImpl registry = this.plugin.getGemRegistry();
            GemDefinition gemDefinition = def = registry != null ? registry.getGem(gemId) : null;
            if (def == null || def.getMaxTier() < 2) {
                return false;
            }
        }
        if ((newGem = CustomItemManager.getItemById(newId = gemId + "_gem_t2", energy = this.plugin.getEnergyManager().getEnergy(player))) == null) {
            return false;
        }
        for (int i = 0; i < player.getInventory().getSize(); ++i) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || !item.equals((Object)currentGem)) continue;
            player.getInventory().setItem(i, newGem);
            this.updateActiveGem(player);
            return true;
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && offhand.equals((Object)currentGem)) {
            player.getInventory().setItemInOffHand(newGem);
            this.updateActiveGem(player);
            return true;
        }
        return false;
    }

    public boolean downgradeGem(Player player, String gemId) {
        if (gemId == null) return false;
        ItemStack currentGem = this.findGemInInventory(player);
        if (currentGem == null) return false;
        String currentId = CustomItemManager.getIdByItem(currentGem);
        if (currentId == null || !currentId.equals(gemId + "_gem_t2")) return false;
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        ItemStack t1Gem = CustomItemManager.getItemById(gemId + "_gem_t1", energy);
        if (t1Gem == null) return false;
        for (int i = 0; i < player.getInventory().getSize(); ++i) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || !item.equals((Object)currentGem)) continue;
            player.getInventory().setItem(i, t1Gem);
            this.updateActiveGem(player);
            return true;
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && offhand.equals((Object)currentGem)) {
            player.getInventory().setItemInOffHand(t1Gem);
            this.updateActiveGem(player);
            return true;
        }
        return false;
    }

    public void updateGemTextures(Player player) {
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            CustomItemManager.updateGemTexture(item, energy);
        }
    }

    public void clearCache(UUID uuid) {
        this.activeGems.remove(uuid);
    }

    public static class ActiveGem {
        private final GemType type;
        private final String gemId;
        private final int tier;

        public ActiveGem(GemType type, int tier) {
            this.type = type;
            this.gemId = type != null ? type.getId() : null;
            this.tier = tier;
        }

        public ActiveGem(String gemId, int tier) {
            this.type = null;
            this.gemId = gemId;
            this.tier = tier;
        }

        public GemType getType() {
            return this.type;
        }

        public String getGemId() {
            return this.gemId;
        }

        public int getTier() {
            return this.tier;
        }
    }
}

