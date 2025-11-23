/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.th0rgal.oraxen.api.OraxenItems
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 */
package fun.obriy.blissgems.managers;

import fun.obriy.blissgems.BlissGems;
import fun.obriy.blissgems.utils.GemType;
import io.th0rgal.oraxen.api.OraxenItems;
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
        ItemStack[] contents = player.getInventory().getContents();
        ActiveGem foundGem = null;
        for (ItemStack item : contents) {
            String oraxenId;
            if (item == null || (oraxenId = OraxenItems.getIdByItem((ItemStack)item)) == null || !GemType.isGem(oraxenId)) continue;
            GemType type = GemType.fromOraxenId(oraxenId);
            int tier = GemType.getTierFromOraxenId(oraxenId);
            if (type == null || !this.plugin.getConfigManager().isGemEnabled(type)) continue;
            foundGem = new ActiveGem(type, tier);
            break;
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
        String oraxenId = OraxenItems.getIdByItem((ItemStack)offhand);
        if (oraxenId == null || !GemType.isGem(oraxenId)) {
            return false;
        }
        GemType type = GemType.fromOraxenId(oraxenId);
        return type != null && this.plugin.getConfigManager().isGemEnabled(type);
    }

    public boolean hasGemTypeInOffhand(Player player, GemType type) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null) {
            return false;
        }
        String oraxenId = OraxenItems.getIdByItem((ItemStack)offhand);
        if (oraxenId == null || !GemType.isGem(oraxenId)) {
            return false;
        }
        GemType gemType = GemType.fromOraxenId(oraxenId);
        return gemType == type;
    }

    public GemType getGemTypeFromOffhand(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null) {
            return null;
        }
        String oraxenId = OraxenItems.getIdByItem((ItemStack)offhand);
        if (oraxenId == null || !GemType.isGem(oraxenId)) {
            return null;
        }
        return GemType.fromOraxenId(oraxenId);
    }

    public boolean giveGem(Player player, GemType type, int tier) {
        String oraxenId = GemType.buildOraxenId(type, tier);
        ItemStack gem = OraxenItems.getItemById((String)oraxenId).build();
        if (gem != null) {
            player.getInventory().addItem(new ItemStack[]{gem});
            this.updateActiveGem(player);
            return true;
        }
        return false;
    }

    public ItemStack findGemInInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            String oraxenId;
            if (item == null || (oraxenId = OraxenItems.getIdByItem((ItemStack)item)) == null || !GemType.isGem(oraxenId)) continue;
            return item;
        }
        return null;
    }

    public boolean replaceGemType(Player player, GemType newType) {
        ItemStack currentGem = this.findGemInInventory(player);
        if (currentGem == null) {
            return false;
        }
        String currentId = OraxenItems.getIdByItem((ItemStack)currentGem);
        if (currentId == null) {
            return false;
        }
        int tier = GemType.getTierFromOraxenId(currentId);
        String newId = GemType.buildOraxenId(newType, tier);
        ItemStack newGem = OraxenItems.getItemById((String)newId).build();
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
        String currentId = OraxenItems.getIdByItem((ItemStack)currentGem);
        if (currentId == null) {
            return false;
        }
        GemType currentType = GemType.fromOraxenId(currentId);
        int currentTier = GemType.getTierFromOraxenId(currentId);
        if (currentType != type || currentTier != 1) {
            return false;
        }
        String newId = GemType.buildOraxenId(type, 2);
        ItemStack newGem = OraxenItems.getItemById((String)newId).build();
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

