/*
 * Auto-enchant listener for Tier 2 gems
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.managers.GemManager;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AutoEnchantListener implements Listener {
    private final BlissGems plugin;
    // Track which enchantments we've added so we can remove them
    private final Map<UUID, Map<Integer, Map<Enchantment, Integer>>> addedEnchants = new HashMap<>();

    public AutoEnchantListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        // Remove enchants from previous slot
        removeAutoEnchants(player, event.getPreviousSlot());

        // Delay to ensure item is in hand
        new BukkitRunnable() {
            @Override
            public void run() {
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
                int heldSlot = player.getInventory().getHeldItemSlot();
                applyAutoEnchants(player, heldSlot);
            }
        }.runTaskLater(plugin, 5L);
    }

    public void applyAutoEnchants(Player player, int slot) {
        if (!plugin.getConfig().getBoolean("auto-enchant.enabled", true)) {
            return;
        }

        GemManager.ActiveGem activeGem = plugin.getGemManager().getActiveGem(player);
        if (activeGem == null || activeGem.getTier() < 2) {
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
        Map<Enchantment, Integer> enchantsToAdd = getEnchantsForGem(gemType, item);

        if (enchantsToAdd.isEmpty()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        // Track what we're adding
        UUID uuid = player.getUniqueId();
        addedEnchants.computeIfAbsent(uuid, k -> new HashMap<>());
        Map<Enchantment, Integer> slotEnchants = new HashMap<>();

        for (Map.Entry<Enchantment, Integer> entry : enchantsToAdd.entrySet()) {
            Enchantment enchant = entry.getKey();
            int level = entry.getValue();

            // Only add if item doesn't already have this enchant at same or higher level
            int currentLevel = meta.getEnchantLevel(enchant);
            if (currentLevel < level) {
                meta.addEnchant(enchant, level, true);
                slotEnchants.put(enchant, currentLevel); // Store original level (0 if not present)
            }
        }

        if (!slotEnchants.isEmpty()) {
            item.setItemMeta(meta);
            addedEnchants.get(uuid).put(slot, slotEnchants);
        }
    }

    public void removeAutoEnchants(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        Map<Integer, Map<Enchantment, Integer>> playerEnchants = addedEnchants.get(uuid);
        if (playerEnchants == null) {
            return;
        }

        Map<Enchantment, Integer> slotEnchants = playerEnchants.remove(slot);
        if (slotEnchants == null) {
            return;
        }

        ItemStack item = player.getInventory().getItem(slot);
        if (item == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        for (Map.Entry<Enchantment, Integer> entry : slotEnchants.entrySet()) {
            Enchantment enchant = entry.getKey();
            int originalLevel = entry.getValue();

            if (originalLevel == 0) {
                meta.removeEnchant(enchant);
            } else {
                meta.addEnchant(enchant, originalLevel, true);
            }
        }

        item.setItemMeta(meta);
    }

    private Map<Enchantment, Integer> getEnchantsForGem(GemType gemType, ItemStack item) {
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
                break;

            case STRENGTH:
                if (plugin.getConfig().getBoolean("auto-enchant.strength.sharpness", true)) {
                    if (isSword(type) || isAxe(type)) {
                        enchants.put(Enchantment.SHARPNESS, 5);
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
        addedEnchants.remove(uuid);
    }
}
