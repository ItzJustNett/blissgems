/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
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
        for (ItemStack item : contents) {
            String itemId;
            if (item == null || (itemId = CustomItemManager.getIdByItem((ItemStack)item)) == null || !GemType.isGem(itemId)) continue;
            GemType type = GemType.fromOraxenId(itemId);
            int tier = GemType.getTierFromOraxenId(itemId);
            if (type == null || !this.plugin.getConfigManager().isGemEnabled(type)) continue;
            foundGem = new ActiveGem(type, tier);
            break;
        }

        // Clean up charging if gem changed or removed
        if (currentGem != null && (foundGem == null || currentGem.type != foundGem.type)) {
            // Gem was removed or type changed - clean up abilities
            if (currentGem.type == GemType.FIRE) {
                plugin.getFireAbilities().cleanup(player);
            } else if (currentGem.type == GemType.FLUX) {
                plugin.getFluxAbilities().cleanup(player);
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
        if (itemId == null || !GemType.isGem(itemId)) {
            return false;
        }
        GemType type = GemType.fromOraxenId(itemId);
        return type != null && this.plugin.getConfigManager().isGemEnabled(type);
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

    public int getTierFromOffhand(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null) {
            return 1; // Default to tier 1
        }
        String itemId = CustomItemManager.getIdByItem((ItemStack)offhand);
        if (itemId == null || !GemType.isGem(itemId)) {
            return 1; // Default to tier 1
        }
        return GemType.getTierFromOraxenId(itemId);
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

    public ItemStack findGemInInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            String itemId;
            if (item == null || (itemId = CustomItemManager.getIdByItem((ItemStack)item)) == null || !GemType.isGem(itemId)) continue;
            return item;
        }
        return null;
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
        private final int tier;

        public ActiveGem(GemType type, int tier) {
            this.type = type;
            this.tier = tier;
        }

        public GemType getType() {
            return this.type;
        }

        public int getTier() {
            return this.tier;
        }
    }
}

