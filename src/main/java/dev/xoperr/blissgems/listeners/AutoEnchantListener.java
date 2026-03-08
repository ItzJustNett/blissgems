/*
 * Auto-enchant listener for Tier 2 gems.
 * Uses PDC markers on items to track auto-applied enchantments,
 * so cleanup works even if items are moved, dropped, or traded.
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.abilities.WealthAbilities;
import dev.xoperr.blissgems.managers.GemManager;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AutoEnchantListener implements Listener {
    private final BlissGems plugin;

    // PDC key prefix for storing original enchant levels: "ae_orig_<enchant_key>"
    // Value is the original level (0 = enchant was not present before auto-enchant)
    private static final String PDC_PREFIX = "ae_orig_";

    public AutoEnchantListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        // Strip auto-enchants from the item leaving the held slot immediately
        ItemStack prevItem = player.getInventory().getItem(event.getPreviousSlot());
        if (prevItem != null && !prevItem.getType().isAir()) {
            stripAutoEnchants(prevItem);
        }

        // Apply to new held slot after 1 tick (item might not be resolved yet)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                applyAutoEnchants(player, event.getNewSlot());
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                // Clean up any stale auto-enchants on all items first
                stripAllAutoEnchants(player);
                int heldSlot = player.getInventory().getHeldItemSlot();
                applyAutoEnchants(player, heldSlot);
            }
        }.runTaskLater(plugin, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Strip all auto-enchants so items are saved clean
        stripAllAutoEnchants(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        scheduleRefresh((Player) event.getWhoClicked());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        scheduleRefresh((Player) event.getWhoClicked());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.isCancelled()) return;
        // Strip auto-enchants and amplify enchants directly from the dropped item entity
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        boolean modified = stripAutoEnchants(droppedItem);
        modified |= WealthAbilities.stripAmplifyEnchants(droppedItem);
        if (modified) {
            event.getItemDrop().setItemStack(droppedItem);
        }
        // Also refresh held slot in case something changed
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler
    public void onPlayerSwapHand(PlayerSwapHandItemsEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    private void scheduleRefresh(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                refreshAutoEnchants(player);
            }
        }.runTaskLater(plugin, 1L);
    }

    /**
     * Scans ALL inventory items and strips auto-enchants from anything
     * not in the currently held slot. Then re-applies to held slot.
     */
    private void refreshAutoEnchants(Player player) {
        int heldSlot = player.getInventory().getHeldItemSlot();

        // Strip auto-enchants from every slot that isn't the held slot
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (i == heldSlot) continue;
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && !item.getType().isAir()) {
                stripAutoEnchants(item);
            }
        }

        // Also strip from off-hand (auto-enchants should only be on main hand)
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (!offHand.getType().isAir()) {
            stripAutoEnchants(offHand);
        }

        // Re-apply to current held slot
        applyAutoEnchants(player, heldSlot);
    }

    public void applyAutoEnchants(Player player, int slot) {
        if (!plugin.getConfig().getBoolean("auto-enchant.enabled", true)) {
            return;
        }

        GemManager.ActiveGem activeGem = plugin.getGemManager().getActiveGem(player);
        if (activeGem == null) {
            return;
        }
        // Most gems require T2 for auto-enchant, but Strength gets Sharpness at both tiers
        if (activeGem.getTier() < 2 && activeGem.getType() != GemType.STRENGTH) {
            return;
        }

        if (!plugin.getEnergyManager().arePassivesActive(player)) {
            return;
        }

        ItemStack item = player.getInventory().getItem(slot);
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        GemType gemType = activeGem.getType();
        int tier = activeGem.getTier();
        Map<Enchantment, Integer> enchantsToAdd = getEnchantsForGem(gemType, item, tier);

        if (enchantsToAdd.isEmpty()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean modified = false;

        for (Map.Entry<Enchantment, Integer> entry : enchantsToAdd.entrySet()) {
            Enchantment enchant = entry.getKey();
            int targetLevel = entry.getValue();
            int currentLevel = meta.getEnchantLevel(enchant);

            NamespacedKey origKey = new NamespacedKey(plugin, PDC_PREFIX + enchant.getKey().getKey());

            // Skip if already auto-enchanted at the correct level
            if (pdc.has(origKey, PersistentDataType.INTEGER)) {
                continue;
            }

            // Only boost if current level is below target
            if (currentLevel < targetLevel) {
                // Store original level in PDC (0 if enchant wasn't present)
                pdc.set(origKey, PersistentDataType.INTEGER, currentLevel);
                meta.addEnchant(enchant, targetLevel, true);
                modified = true;
            }
        }

        if (modified) {
            item.setItemMeta(meta);
        }
    }

    /**
     * Strip all auto-enchant PDC markers from an item, restoring original enchant levels.
     * Returns true if the item was modified.
     */
    public boolean stripAutoEnchants(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean modified = false;

        // Check all known enchantments for auto-enchant PDC markers
        for (Enchantment enchant : Enchantment.values()) {
            NamespacedKey origKey = new NamespacedKey(plugin, PDC_PREFIX + enchant.getKey().getKey());

            if (pdc.has(origKey, PersistentDataType.INTEGER)) {
                int originalLevel = pdc.get(origKey, PersistentDataType.INTEGER);

                if (originalLevel == 0) {
                    meta.removeEnchant(enchant);
                } else {
                    meta.addEnchant(enchant, originalLevel, true);
                }

                pdc.remove(origKey);
                modified = true;
            }
        }

        if (modified) {
            item.setItemMeta(meta);
        }
        return modified;
    }

    /**
     * Strip auto-enchants from ALL items in a player's inventory.
     */
    public void stripAllAutoEnchants(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && !item.getType().isAir()) {
                stripAutoEnchants(item);
            }
        }
        // Off-hand
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (!offHand.getType().isAir()) {
            stripAutoEnchants(offHand);
        }
    }

    /**
     * Check if an item has any auto-enchant markers (used by amplify to skip).
     */
    public boolean hasAutoEnchants(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        for (Enchantment enchant : Enchantment.values()) {
            NamespacedKey origKey = new NamespacedKey(plugin, PDC_PREFIX + enchant.getKey().getKey());
            if (pdc.has(origKey, PersistentDataType.INTEGER)) {
                return true;
            }
        }
        return false;
    }

    private Map<Enchantment, Integer> getEnchantsForGem(GemType gemType, ItemStack item, int tier) {
        Map<Enchantment, Integer> enchants = new HashMap<>();
        Material type = item.getType();

        switch (gemType) {
            case SPEED:
                if (plugin.getConfig().getBoolean("auto-enchant.speed.efficiency", true)) {
                    if (isTool(type)) {
                        enchants.put(Enchantment.EFFICIENCY, 5);
                    }
                }
                break;

            case WEALTH:
                if (plugin.getConfig().getBoolean("auto-enchant.wealth.fortune", true)) {
                    if (isPickaxe(type) || isShovel(type) || isAxe(type)) {
                        enchants.put(Enchantment.FORTUNE, 3);
                    }
                }
                if (plugin.getConfig().getBoolean("auto-enchant.wealth.looting", true)) {
                    if (isSword(type)) {
                        enchants.put(Enchantment.LOOTING, 3);
                    }
                }
                if (plugin.getConfig().getBoolean("auto-enchant.wealth.mending", true)) {
                    if (isTool(type) || isWeapon(type) || isArmor(type)) {
                        enchants.put(Enchantment.MENDING, 1);
                    }
                }
                break;

            case FIRE:
                if (plugin.getConfig().getBoolean("auto-enchant.fire.flame", true)) {
                    if (type == Material.BOW) {
                        enchants.put(Enchantment.FLAME, 1);
                    }
                }
                if (plugin.getConfig().getBoolean("auto-enchant.fire.fire-aspect", true)) {
                    if (isSword(type)) {
                        enchants.put(Enchantment.FIRE_ASPECT, 2);
                    }
                }
                break;

            case PUFF:
                if (plugin.getConfig().getBoolean("auto-enchant.puff.feather-falling", true)) {
                    if (isBoots(type)) {
                        enchants.put(Enchantment.FEATHER_FALLING, 4);
                    }
                }
                if (plugin.getConfig().getBoolean("auto-enchant.puff.power", true)) {
                    if (type == Material.BOW) {
                        enchants.put(Enchantment.POWER, 5);
                    }
                }
                if (plugin.getConfig().getBoolean("auto-enchant.puff.punch", true)) {
                    if (type == Material.BOW) {
                        enchants.put(Enchantment.PUNCH, 2);
                    }
                }
                break;

            case STRENGTH:
                if (plugin.getConfig().getBoolean("auto-enchant.strength.sharpness", true)) {
                    if (isSword(type) || isAxe(type)) {
                        int sharpnessLevel = (tier >= 2) ? 5 : 2;
                        enchants.put(Enchantment.SHARPNESS, sharpnessLevel);
                    }
                }
                break;

            case LIFE:
                if (plugin.getConfig().getBoolean("auto-enchant.life.unbreaking", true)) {
                    if (isTool(type) || isWeapon(type) || isArmor(type)) {
                        enchants.put(Enchantment.UNBREAKING, 3);
                    }
                }
                break;

            default:
                break;
        }

        return enchants;
    }

    private boolean isTool(Material type) {
        return isPickaxe(type) || isShovel(type) || isAxe(type) || isHoe(type);
    }

    private boolean isPickaxe(Material type) {
        return type == Material.WOODEN_PICKAXE || type == Material.STONE_PICKAXE ||
               type == Material.IRON_PICKAXE || type == Material.GOLDEN_PICKAXE ||
               type == Material.DIAMOND_PICKAXE || type == Material.NETHERITE_PICKAXE;
    }

    private boolean isShovel(Material type) {
        return type == Material.WOODEN_SHOVEL || type == Material.STONE_SHOVEL ||
               type == Material.IRON_SHOVEL || type == Material.GOLDEN_SHOVEL ||
               type == Material.DIAMOND_SHOVEL || type == Material.NETHERITE_SHOVEL;
    }

    private boolean isAxe(Material type) {
        return type == Material.WOODEN_AXE || type == Material.STONE_AXE ||
               type == Material.IRON_AXE || type == Material.GOLDEN_AXE ||
               type == Material.DIAMOND_AXE || type == Material.NETHERITE_AXE;
    }

    private boolean isHoe(Material type) {
        return type == Material.WOODEN_HOE || type == Material.STONE_HOE ||
               type == Material.IRON_HOE || type == Material.GOLDEN_HOE ||
               type == Material.DIAMOND_HOE || type == Material.NETHERITE_HOE;
    }

    private boolean isSword(Material type) {
        return type == Material.WOODEN_SWORD || type == Material.STONE_SWORD ||
               type == Material.IRON_SWORD || type == Material.GOLDEN_SWORD ||
               type == Material.DIAMOND_SWORD || type == Material.NETHERITE_SWORD;
    }

    private boolean isWeapon(Material type) {
        return isSword(type) || isAxe(type) || type == Material.BOW ||
               type == Material.CROSSBOW || type == Material.TRIDENT;
    }

    private boolean isArmor(Material type) {
        String name = type.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") ||
               name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    private boolean isBoots(Material type) {
        return type.name().endsWith("_BOOTS");
    }

    public void clearCache(UUID uuid) {
        // No in-memory state to clear anymore (PDC is on items)
    }
}
