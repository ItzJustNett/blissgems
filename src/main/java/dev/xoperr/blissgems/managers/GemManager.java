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
import dev.xoperr.blissgems.api.GemRegistry;
import java.util.ArrayList;
import java.util.List;
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

    /**
     * Get the custom item ID of the gem the player is holding.
     * Passives work with the gem in either hand; the offhand wins if both hold gems.
     */
    private String getHeldGemItemId(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        String itemId = offhand != null ? CustomItemManager.getIdByItem((ItemStack)offhand) : null;
        if (itemId != null && this.isAnyGem(itemId)) {
            return itemId;
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        itemId = mainHand != null ? CustomItemManager.getIdByItem((ItemStack)mainHand) : null;
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
        return this.isGemOfType(player.getInventory().getItemInOffHand(), type)
            || this.isGemOfType(player.getInventory().getItemInMainHand(), type);
    }

    private boolean isGemOfType(ItemStack item, GemType type) {
        if (item == null) {
            return false;
        }
        String itemId = CustomItemManager.getIdByItem((ItemStack)item);
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

    /**
     * Get the string gem ID from the held gem (either hand, offhand priority).
     * Works for both built-in and addon gems.
     */
    public String getGemIdFromOffhand(Player player) {
        String itemId = this.getHeldGemItemId(player);
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
        String itemId = this.getHeldGemItemId(player);
        if (itemId == null) {
            return 1; // Default to tier 1
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
        // Check the offhand first — gems are normally held there, and getContents()
        // doesn't reliably include it across API versions.
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null) {
            String offId = CustomItemManager.getIdByItem(offhand);
            if (offId != null && (GemType.isGem(offId) || (registry != null && registry.isRegisteredGem(offId)))) {
                return offhand;
            }
        }
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

    /** Resolve a gem ID to its built-in {@link GemType}, or null if it is an addon gem. */
    public static GemType builtInType(String gemId) {
        if (gemId == null) return null;
        for (GemType type : GemType.values()) {
            if (type.getId().equalsIgnoreCase(gemId)) return type;
        }
        return null;
    }

    /**
     * Every gem ID that may be RANDOMLY granted (first join, reroll): enabled built-in gems
     * plus registered addon gems — MINUS any listed in config {@code gems.exclude-from-random}
     * (default: the mythic gems auratus + heretic, so newcomers can't roll them).
     * Built-in gems are also in the registry, so registry entries that resolve to a GemType
     * are skipped to avoid re-adding a config-disabled gem or double-counting.
     */
    public List<String> getAvailableGemIds() {
        List<String> ids = new ArrayList<>();
        for (GemType type : GemType.values()) {
            if (this.plugin.getConfigManager().isGemEnabled(type)) {
                ids.add(type.getId());
            }
        }
        GemRegistry registry = this.plugin.getGemRegistry();
        if (registry != null) {
            List<String> excluded = this.plugin.getConfig().contains("gems.exclude-from-random")
                ? this.plugin.getConfig().getStringList("gems.exclude-from-random")
                : List.of("auratus", "heretic");
            for (GemDefinition def : registry.getAllGems()) {
                if (builtInType(def.getId()) == null
                        && !ids.contains(def.getId())
                        && !excluded.contains(def.getId())) {
                    ids.add(def.getId());
                }
            }
        }
        return ids;
    }

    /** Display name for any gem ID (built-in or addon). Falls back to the raw ID. */
    public String getGemDisplayName(String gemId) {
        GemType type = builtInType(gemId);
        if (type != null) return type.getDisplayName();
        GemRegistry registry = this.plugin.getGemRegistry();
        GemDefinition def = registry != null ? registry.getGem(gemId) : null;
        return def != null ? def.getDisplayName() : gemId;
    }

    /** Chat color code for any gem ID (built-in or addon). Falls back to gray. */
    public String getGemColorCode(String gemId) {
        GemType type = builtInType(gemId);
        if (type != null) return type.getColor();
        GemRegistry registry = this.plugin.getGemRegistry();
        GemDefinition def = registry != null ? registry.getGem(gemId) : null;
        return def != null ? def.getColor() : "§7";
    }

    /**
     * Give a gem (built-in OR addon, by string id) directly into the offhand — the string
     * counterpart of {@link #giveGemToOffhand(Player, GemType, int)} used by reroll.
     */
    public boolean giveGemToOffhand(Player player, String gemId, int tier) {
        GemType type = builtInType(gemId);
        if (type != null) {
            return giveGemToOffhand(player, type, tier);
        }
        GemRegistry registry = this.plugin.getGemRegistry();
        GemDefinition def = registry != null ? registry.getGem(gemId) : null;
        if (def == null) return false;
        String itemId = def.buildItemId(tier);
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        ItemStack gem = CustomItemManager.getItemById(itemId, energy);
        if (gem == null) return false;
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

    /**
     * In-place replace of the player's current gem with another gem id — built-in OR addon
     * (mythic/expansion) — preserving the current tier. String counterpart of
     * {@link #replaceGemType(Player, GemType)}; used by the trader so expansion gems trade.
     */
    public boolean replaceGem(Player player, String newGemId) {
        if (newGemId == null) {
            return false;
        }
        ItemStack currentGem = this.findGemInInventory(player);
        if (currentGem == null) {
            return false;
        }
        String currentId = CustomItemManager.getIdByItem((ItemStack)currentGem);
        if (currentId == null) {
            return false;
        }
        int tier = GemType.getTierFromOraxenId(currentId);
        String newId = newGemId + "_gem_t" + tier;
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
        // The storage loop may not cover the offhand slot; replace it there directly.
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && offhand.equals((Object) currentGem)) {
            player.getInventory().setItemInOffHand(newGem);
            this.updateActiveGem(player);
            return true;
        }
        return false;
    }

    /**
     * Give a gem placed directly into the player's offhand (the canonical gem slot).
     * If the offhand is occupied by something else, falls back to a normal inventory add.
     * Used by reroll so the new gem lands where gem resolution looks first — preventing a
     * stale gem elsewhere from continuing to drive abilities.
     */
    public boolean giveGemToOffhand(Player player, GemType type, int tier) {
        String itemId = GemType.buildOraxenId(type, tier);
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        ItemStack gem = CustomItemManager.getItemById(itemId, energy);
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

    /**
     * Upgrade the player's held tier-1 gem to tier 2 — built-in OR addon (mythic/expansion).
     * String counterpart of {@link #upgradeGem(Player, GemType)}; used by the upgrader so
     * expansion gems can be upgraded (their built-in GemType is null).
     */
    public boolean upgradeGem(Player player, String gemId) {
        if (gemId == null) {
            return false;
        }
        ItemStack currentGem = this.findGemInInventory(player);
        if (currentGem == null) {
            return false;
        }
        String currentId = CustomItemManager.getIdByItem((ItemStack)currentGem);
        if (currentId == null) {
            return false;
        }
        // Only the tier-1 form of the gem the player actually holds may be upgraded.
        if (!currentId.equals(gemId + "_gem_t1")) {
            return false;
        }
        // Addon gems may cap at tier 1 — respect their declared max tier.
        if (builtInType(gemId) == null) {
            GemRegistry registry = this.plugin.getGemRegistry();
            GemDefinition def = registry != null ? registry.getGem(gemId) : null;
            if (def == null || def.getMaxTier() < 2) {
                return false;
            }
        }
        String newId = gemId + "_gem_t2";
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
        // The storage loop may not cover the offhand slot; upgrade it there directly.
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && offhand.equals((Object) currentGem)) {
            player.getInventory().setItemInOffHand(newGem);
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

