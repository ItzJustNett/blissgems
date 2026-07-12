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
import dev.xoperr.blissgems.api.GemRegistry;
import dev.xoperr.blissgems.utils.GemType;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GemManager {
    private final BlissGems plugin;
    private final Map<UUID, ActiveGem> activeGems;

    public GemManager(BlissGems plugin) {
        this.plugin = plugin;
        this.activeGems = new HashMap<UUID, ActiveGem>();
    }

    public void updateActiveGem(Player player) {
        // Get current gem before updating
        ActiveGem currentGem = this.activeGems.get(player.getUniqueId());

        ItemStack[] contents = player.getInventory().getContents();
        ActiveGem foundGem = null;
        GemRegistry registry = this.plugin.getGemRegistry();

        for (ItemStack item : contents) {
            String itemId;
            if (item == null || (itemId = CustomItemManager.getIdByItem((ItemStack)item)) == null) continue;

            // Check built-in gems first
            if (GemType.isGem(itemId)) {
                GemType type = GemType.fromOraxenId(itemId);
                int tier = GemType.getTierFromOraxenId(itemId);
                if (type != null && this.plugin.getConfigManager().isGemEnabled(type)) {
                    foundGem = new ActiveGem(type, tier);
                    break;
                }
            }

            // Check addon gems via registry
            if (registry != null && registry.isRegisteredGem(itemId)) {
                String gemId = registry.gemIdFromItemId(itemId);
                int tier = registry.tierFromItemId(itemId);
                foundGem = new ActiveGem(gemId, tier);
                break;
            }
        }

        // Clean up if gem changed or removed
        if (currentGem != null && (foundGem == null || !java.util.Objects.equals(currentGem.getGemId(), foundGem.getGemId()))) {
            // Gem was removed or type changed - clean up abilities via registry
            if (currentGem.getGemId() != null && registry != null) {
                GemAbilityHandler handler = registry.getAbilityHandler(currentGem.getGemId());
                if (handler != null) {
                    handler.cleanup(player);
                }
            }
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

    public int getGemTier(Player player) {
        ActiveGem gem = this.getActiveGem(player);
        return gem != null ? gem.getTier() : 1;
    }

    /**
     * Get the string gem ID for the player's active gem.
     * Works for both built-in and addon gems.
     */
    public String getGemId(Player player) {
        ActiveGem gem = this.getActiveGem(player);
        return gem != null ? gem.getGemId() : null;
    }

    public boolean hasGemType(Player player, GemType type) {
        ActiveGem gem = this.getActiveGem(player);
        return gem != null && gem.getType() == type;
    }

    public boolean hasGemInOffhand(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null) {
            return false;
        }
        String itemId = CustomItemManager.getIdByItem((ItemStack)offhand);
        if (itemId == null) {
            return false;
        }
        // Check built-in gems
        if (GemType.isGem(itemId)) {
            GemType type = GemType.fromOraxenId(itemId);
            return type != null && this.plugin.getConfigManager().isGemEnabled(type);
        }
        // Check addon gems via registry
        GemRegistry registry = this.plugin.getGemRegistry();
        return registry != null && registry.isRegisteredGem(itemId);
    }

    public boolean hasGemTypeInOffhand(Player player, GemType type) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null) {
            return false;
        }
        String itemId = CustomItemManager.getIdByItem((ItemStack)offhand);
        if (itemId == null || !GemType.isGem(itemId)) {
            return false;
        }
        GemType gemType = GemType.fromOraxenId(itemId);
        return gemType == type;
    }

    public GemType getGemTypeFromOffhand(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null) {
            return null;
        }
        String itemId = CustomItemManager.getIdByItem((ItemStack)offhand);
        if (itemId == null || !GemType.isGem(itemId)) {
            return null;
        }
        return GemType.fromOraxenId(itemId);
    }

    /**
     * Get the string gem ID from the offhand item.
     * Works for both built-in and addon gems.
     */
    public String getGemIdFromOffhand(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null) return null;
        String itemId = CustomItemManager.getIdByItem(offhand);
        if (itemId == null) return null;
        // Check built-in
        GemType type = GemType.fromOraxenId(itemId);
        if (type != null) return type.getId();
        // Check addon via registry
        GemRegistry registry = this.plugin.getGemRegistry();
        if (registry != null) return registry.gemIdFromItemId(itemId);
        return null;
    }

    public int getTierFromOffhand(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null) {
            return 1; // Default to tier 1
        }
        String itemId = CustomItemManager.getIdByItem((ItemStack)offhand);
        if (itemId == null) {
            return 1;
        }
        // Check built-in
        if (GemType.isGem(itemId)) {
            return GemType.getTierFromOraxenId(itemId);
        }
        // Check addon via registry
        GemRegistry registry = this.plugin.getGemRegistry();
        if (registry != null && registry.isRegisteredGem(itemId)) {
            return registry.tierFromItemId(itemId);
        }
        return 1;
    }

    public boolean giveGem(Player player, GemType type, int tier) {
        String itemId = GemType.buildOraxenId(type, tier);
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        ItemStack gem = CustomItemManager.getItemById((String)itemId, energy);
        if (gem != null) {
            player.getInventory().addItem(new ItemStack[]{gem});
            this.updateActiveGem(player);
            return true;
        }
        return false;
    }

    /**
     * Give a gem by string gem ID (works for both built-in and addon gems).
     * @param player Target player
     * @param gemId  The gem ID (e.g. "fire", "ice")
     * @param tier   The tier (1 or 2)
     * @return true if the gem was given successfully
     */
    public boolean giveGem(Player player, String gemId, int tier) {
        // Try built-in gem first
        for (GemType type : GemType.values()) {
            if (type.getId().equalsIgnoreCase(gemId)) {
                return giveGem(player, type, tier);
            }
        }
        // Try addon gem via registry
        GemRegistry registry = this.plugin.getGemRegistry();
        if (registry != null) {
            dev.xoperr.blissgems.api.GemDefinition def = registry.getGem(gemId);
            if (def != null) {
                String itemId = def.buildItemId(tier);
                int energy = this.plugin.getEnergyManager().getEnergy(player);
                ItemStack gem = CustomItemManager.getItemById(itemId, energy);
                if (gem != null) {
                    player.getInventory().addItem(new ItemStack[]{gem});
                    this.updateActiveGem(player);
                    return true;
                }
            }
        }
        return false;
    }

    public ItemStack findGemInInventory(Player player) {
        GemRegistry registry = this.plugin.getGemRegistry();
        // Check main inventory
        for (ItemStack item : player.getInventory().getContents()) {
            String itemId;
            if (item == null || (itemId = CustomItemManager.getIdByItem((ItemStack)item)) == null) continue;
            if (GemType.isGem(itemId)) return item;
            if (registry != null && registry.isRegisteredGem(itemId)) return item;
        }
        // Check ender chest
        for (ItemStack item : player.getEnderChest().getContents()) {
            String itemId;
            if (item == null || (itemId = CustomItemManager.getIdByItem((ItemStack)item)) == null) continue;
            if (GemType.isGem(itemId)) return item;
            if (registry != null && registry.isRegisteredGem(itemId)) return item;
        }
        return null;
    }

    /**
     * Check if an item ID represents any gem (built-in or addon).
     */
    public boolean isAnyGem(String itemId) {
        if (itemId == null) return false;
        if (GemType.isGem(itemId)) return true;
        GemRegistry registry = this.plugin.getGemRegistry();
        return registry != null && registry.isRegisteredGem(itemId);
    }

    
    public boolean replaceGemType(Player player, String newGemId) {
        ItemStack currentGem = this.findGemInInventory(player);
        if (currentGem == null) return false;
        String currentId = CustomItemManager.getIdByItem(currentGem);
        if (currentId == null) return false;
        
        int tier = 1;
        GemRegistry registry = this.plugin.getGemRegistry();
        if (GemType.isGem(currentId)) {
            tier = GemType.getTierFromOraxenId(currentId);
        } else if (registry != null && registry.isRegisteredGem(currentId)) {
            tier = registry.tierFromItemId(currentId);
        }
        
        String newId = null;
        boolean foundBuiltin = false;
        for (GemType t : GemType.values()) {
            if (t.getId().equalsIgnoreCase(newGemId)) {
                newId = GemType.buildOraxenId(t, tier);
                foundBuiltin = true;
                break;
            }
        }
        if (!foundBuiltin && registry != null) {
            dev.xoperr.blissgems.api.GemDefinition def = registry.getGem(newGemId);
            if (def != null) newId = def.buildItemId(tier);
        }
        
        if (newId == null) return false;

        int energy = this.plugin.getEnergyManager().getEnergy(player);
        ItemStack newGem = CustomItemManager.getItemById(newId, energy);
        if (newGem == null) return false;

        for (int i = 0; i < player.getInventory().getSize(); ++i) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || !item.equals(currentGem)) continue;
            player.getInventory().setItem(i, newGem);
            this.updateActiveGem(player);
            return true;
        }
        return false;
    }

    public boolean replaceGemType(Player player, GemType newType) {
        ItemStack currentGem = this.findGemInInventory(player);
        if (currentGem == null) {
            return false;
        }
        String currentId = CustomItemManager.getIdByItem((ItemStack)currentGem);
        if (currentId == null) {
            return false;
        }
        int tier = GemType.getTierFromOraxenId(currentId);
        String newId = GemType.buildOraxenId(newType, tier);
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        ItemStack newGem = CustomItemManager.getItemById((String)newId, energy);
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
        return false;
    }

    
    public boolean upgradeGem(Player player, String gemId) {
        ItemStack currentGem = this.findGemInInventory(player);
        if (currentGem == null) return false;
        
        String currentId = CustomItemManager.getIdByItem(currentGem);
        if (currentId == null) return false;
        
        int currentTier = 1;
        String newId = null;
        
        GemRegistry registry = this.plugin.getGemRegistry();
        GemType currentType = GemType.fromOraxenId(currentId);
        if (currentType != null) {
            currentTier = GemType.getTierFromOraxenId(currentId);
            if (!currentType.getId().equalsIgnoreCase(gemId) || currentTier != 1) return false;
            newId = GemType.buildOraxenId(currentType, 2);
        } else if (registry != null && registry.isRegisteredGem(currentId)) {
            String cId = registry.gemIdFromItemId(currentId);
            currentTier = registry.tierFromItemId(currentId);
            if (!cId.equalsIgnoreCase(gemId) || currentTier != 1) return false;
            dev.xoperr.blissgems.api.GemDefinition def = registry.getGem(gemId);
            if (def == null) return false;
            newId = def.buildItemId(2);
        } else {
            return false;
        }

        int energy = this.plugin.getEnergyManager().getEnergy(player);
        ItemStack newGem = CustomItemManager.getItemById(newId, energy);
        if (newGem == null) return false;
        
        for (int i = 0; i < player.getInventory().getSize(); ++i) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || !item.equals(currentGem)) continue;
            player.getInventory().setItem(i, newGem);
            this.updateActiveGem(player);
            return true;
        }
        return false;
    }

    public boolean upgradeGem(Player player, GemType type) {
        ItemStack currentGem = this.findGemInInventory(player);
        if (currentGem == null) {
            return false;
        }
        String currentId = CustomItemManager.getIdByItem((ItemStack)currentGem);
        if (currentId == null) {
            return false;
        }
        GemType currentType = GemType.fromOraxenId(currentId);
        int currentTier = GemType.getTierFromOraxenId(currentId);
        if (currentType != type || currentTier != 1) {
            return false;
        }
        String newId = GemType.buildOraxenId(type, 2);
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        ItemStack newGem = CustomItemManager.getItemById((String)newId, energy);
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
        return false;
    }

    /**
     * Update the texture of all gems in a player's inventory based on their current energy
     * Called when energy changes
     */
    public void updateGemTextures(Player player) {
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                CustomItemManager.updateGemTexture(item, energy);
            }
        }
    }

    public void clearCache(UUID uuid) {
        this.activeGems.remove(uuid);
    }

    public static class ActiveGem {
        private final GemType type;
        private final String gemId;
        private final int tier;

        /** Constructor for built-in gems */
        public ActiveGem(GemType type, int tier) {
            this.type = type;
            this.gemId = type != null ? type.getId() : null;
            this.tier = tier;
        }

        /** Constructor for addon gems (type is null) */
        public ActiveGem(String gemId, int tier) {
            this.type = null;
            this.gemId = gemId;
            this.tier = tier;
        }

        /** Returns the GemType for built-in gems, null for addon gems */
        public GemType getType() {
            return this.type;
        }

        /** Returns the string gem ID (works for both built-in and addon gems) */
        public String getGemId() {
            return this.gemId;
        }

        public int getTier() {
            return this.tier;
        }
    }
}

