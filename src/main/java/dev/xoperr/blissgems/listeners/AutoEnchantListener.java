/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerItemHeldEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerSwapHandItemsEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.abilities.WealthAbilities;
import dev.xoperr.blissgems.managers.GemManager;
import dev.xoperr.blissgems.utils.GemType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoEnchantListener
implements Listener {
    private final BlissGems plugin;
    private static final String PDC_PREFIX = "ae_orig_";

    public AutoEnchantListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerItemHeld(final PlayerItemHeldEvent event) {
        final Player player = event.getPlayer();
        ItemStack prevItem = player.getInventory().getItem(event.getPreviousSlot());
        if (prevItem != null && !prevItem.getType().isAir()) {
            this.stripAutoEnchants(prevItem);
        }
        new BukkitRunnable(){

            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                AutoEnchantListener.this.applyAutoEnchants(player, event.getNewSlot());
            }
        }.runTaskLater((Plugin)this.plugin, 1L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        new BukkitRunnable(){

            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                AutoEnchantListener.this.stripAllAutoEnchants(player);
                int heldSlot = player.getInventory().getHeldItemSlot();
                AutoEnchantListener.this.applyAutoEnchants(player, heldSlot);
            }
        }.runTaskLater((Plugin)this.plugin, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.stripAllAutoEnchants(event.getPlayer());
        this.stripAllAmplifyEnchants(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        this.scheduleRefresh((Player)event.getWhoClicked());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        this.scheduleRefresh((Player)event.getWhoClicked());
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.isCancelled()) {
            return;
        }
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        boolean modified = this.stripAutoEnchants(droppedItem);
        if (modified |= WealthAbilities.stripAmplifyEnchants(droppedItem)) {
            event.getItemDrop().setItemStack(droppedItem);
        }
        this.scheduleRefresh(event.getPlayer());
    }

    @EventHandler
    public void onPlayerSwapHand(PlayerSwapHandItemsEvent event) {
        this.scheduleRefresh(event.getPlayer());
    }

    private void scheduleRefresh(final Player player) {
        new BukkitRunnable(){

            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                AutoEnchantListener.this.refreshAutoEnchants(player);
            }
        }.runTaskLater((Plugin)this.plugin, 1L);
    }

    private void refreshAutoEnchants(Player player) {
        int heldSlot = player.getInventory().getHeldItemSlot();
        for (int i = 0; i < player.getInventory().getSize(); ++i) {
            ItemStack item;
            if (i == heldSlot || (item = player.getInventory().getItem(i)) == null || item.getType().isAir()) continue;
            this.stripAutoEnchants(item);
        }
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (!offHand.getType().isAir()) {
            this.stripAutoEnchants(offHand);
        }
        this.applyAutoEnchants(player, heldSlot);
    }

    public void applyAutoEnchants(Player player, int slot) {
        int tier;
        if (!this.plugin.getConfig().getBoolean("auto-enchant.enabled", true)) {
            return;
        }
        GemManager.ActiveGem activeGem = this.plugin.getGemManager().getActiveGem(player);
        if (activeGem == null || activeGem.getType() == null) {
            return;
        }
        if (activeGem.getTier() < 2 && activeGem.getType() != GemType.STRENGTH && !this.plugin.getConfigManager().isTier1AutoEnchantEnabled()) {
            return;
        }
        if (!this.plugin.getEnergyManager().arePassivesActive(player)) {
            return;
        }
        ItemStack item = player.getInventory().getItem(slot);
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        GemType gemType = activeGem.getType();
        Map<Enchantment, Integer> enchantsToAdd = this.getEnchantsForGem(gemType, item, tier = activeGem.getTier());
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
            NamespacedKey origKey = new NamespacedKey((Plugin)this.plugin, PDC_PREFIX + enchant.getKey().getKey());
            if (pdc.has(origKey, PersistentDataType.INTEGER) || currentLevel >= targetLevel) continue;
            pdc.set(origKey, PersistentDataType.INTEGER, currentLevel);
            meta.addEnchant(enchant, targetLevel, true);
            modified = true;
        }
        if (modified) {
            item.setItemMeta(meta);
        }
    }

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
        for (Enchantment enchant : Enchantment.values()) {
            NamespacedKey origKey = new NamespacedKey((Plugin)this.plugin, PDC_PREFIX + enchant.getKey().getKey());
            if (!pdc.has(origKey, PersistentDataType.INTEGER)) continue;
            int originalLevel = (Integer)pdc.get(origKey, PersistentDataType.INTEGER);
            if (originalLevel == 0) {
                meta.removeEnchant(enchant);
            } else {
                meta.addEnchant(enchant, originalLevel, true);
            }
            pdc.remove(origKey);
            modified = true;
        }
        if (modified) {
            item.setItemMeta(meta);
        }
        return modified;
    }

    public void stripAllAutoEnchants(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); ++i) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType().isAir()) continue;
            this.stripAutoEnchants(item);
        }
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (!offHand.getType().isAir()) {
            this.stripAutoEnchants(offHand);
        }
    }

    private void stripAllAmplifyEnchants(Player player) {
        ItemStack offHand;
        for (int i = 0; i < player.getInventory().getSize(); ++i) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType().isAir()) continue;
            WealthAbilities.stripAmplifyEnchants(item);
        }
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean armorModified = false;
        for (int i = 0; i < armor.length; ++i) {
            if (armor[i] == null || armor[i].getType().isAir() || !WealthAbilities.stripAmplifyEnchants(armor[i])) continue;
            armorModified = true;
        }
        if (armorModified) {
            player.getInventory().setArmorContents(armor);
        }
        if (!(offHand = player.getInventory().getItemInOffHand()).getType().isAir()) {
            WealthAbilities.stripAmplifyEnchants(offHand);
        }
    }

    public boolean hasAutoEnchants(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        for (Enchantment enchant : Enchantment.values()) {
            NamespacedKey origKey = new NamespacedKey((Plugin)this.plugin, PDC_PREFIX + enchant.getKey().getKey());
            if (!pdc.has(origKey, PersistentDataType.INTEGER)) continue;
            return true;
        }
        return false;
    }

    private Map<Enchantment, Integer> getEnchantsForGem(GemType gemType, ItemStack item, int tier) {
        HashMap<Enchantment, Integer> enchants = new HashMap<Enchantment, Integer>();
        Material type = item.getType();
        switch (gemType) {
            case SPEED: {
                if (!this.plugin.getConfig().getBoolean("auto-enchant.speed.efficiency", true) || !this.isTool(type)) break;
                enchants.put(Enchantment.EFFICIENCY, tier >= 2 ? 5 : 3);
                break;
            }
            case WEALTH: {
                if (this.plugin.getConfig().getBoolean("auto-enchant.wealth.fortune", true) && (this.isPickaxe(type) || this.isShovel(type) || this.isAxe(type))) {
                    enchants.put(Enchantment.FORTUNE, tier >= 2 ? 3 : 2);
                }
                if (this.plugin.getConfig().getBoolean("auto-enchant.wealth.looting", true) && this.isSword(type)) {
                    enchants.put(Enchantment.LOOTING, tier >= 2 ? 3 : 2);
                }
                if (!this.plugin.getConfig().getBoolean("auto-enchant.wealth.mending", true) || !this.isTool(type) && !this.isWeapon(type) && !this.isArmor(type)) break;
                enchants.put(Enchantment.MENDING, 1);
                break;
            }
            case FIRE: {
                if (this.plugin.getConfig().getBoolean("auto-enchant.fire.flame", true) && type == Material.BOW) {
                    enchants.put(Enchantment.FLAME, 1);
                }
                if (!this.plugin.getConfig().getBoolean("auto-enchant.fire.fire-aspect", true) || !this.isSword(type)) break;
                enchants.put(Enchantment.FIRE_ASPECT, tier >= 2 ? 2 : 1);
                break;
            }
            case PUFF: {
                if (this.plugin.getConfig().getBoolean("auto-enchant.puff.feather-falling", true) && this.isBoots(type)) {
                    enchants.put(Enchantment.FEATHER_FALLING, tier >= 2 ? 4 : 2);
                }
                if (this.plugin.getConfig().getBoolean("auto-enchant.puff.power", true) && type == Material.BOW) {
                    enchants.put(Enchantment.POWER, tier >= 2 ? 5 : 3);
                }
                if (!this.plugin.getConfig().getBoolean("auto-enchant.puff.punch", true) || type != Material.BOW) break;
                enchants.put(Enchantment.PUNCH, tier >= 2 ? 2 : 1);
                break;
            }
            case STRENGTH: {
                if (!this.plugin.getConfig().getBoolean("auto-enchant.strength.sharpness", true) || !this.isSword(type) && !this.isAxe(type)) break;
                int sharpnessLevel = tier >= 2 ? 5 : 2;
                enchants.put(Enchantment.SHARPNESS, sharpnessLevel);
                break;
            }
            case LIFE: {
                if (!this.plugin.getConfig().getBoolean("auto-enchant.life.unbreaking", true) || !this.isTool(type) && !this.isWeapon(type) && !this.isArmor(type)) break;
                enchants.put(Enchantment.UNBREAKING, tier >= 2 ? 3 : 2);
                break;
            }
        }
        return enchants;
    }

    private boolean isTool(Material type) {
        return this.isPickaxe(type) || this.isShovel(type) || this.isAxe(type) || this.isHoe(type);
    }

    private boolean isPickaxe(Material type) {
        return type == Material.WOODEN_PICKAXE || type == Material.STONE_PICKAXE || type == Material.IRON_PICKAXE || type == Material.GOLDEN_PICKAXE || type == Material.DIAMOND_PICKAXE || type == Material.NETHERITE_PICKAXE;
    }

    private boolean isShovel(Material type) {
        return type == Material.WOODEN_SHOVEL || type == Material.STONE_SHOVEL || type == Material.IRON_SHOVEL || type == Material.GOLDEN_SHOVEL || type == Material.DIAMOND_SHOVEL || type == Material.NETHERITE_SHOVEL;
    }

    private boolean isAxe(Material type) {
        return type == Material.WOODEN_AXE || type == Material.STONE_AXE || type == Material.IRON_AXE || type == Material.GOLDEN_AXE || type == Material.DIAMOND_AXE || type == Material.NETHERITE_AXE;
    }

    private boolean isHoe(Material type) {
        return type == Material.WOODEN_HOE || type == Material.STONE_HOE || type == Material.IRON_HOE || type == Material.GOLDEN_HOE || type == Material.DIAMOND_HOE || type == Material.NETHERITE_HOE;
    }

    private boolean isSword(Material type) {
        return type == Material.WOODEN_SWORD || type == Material.STONE_SWORD || type == Material.IRON_SWORD || type == Material.GOLDEN_SWORD || type == Material.DIAMOND_SWORD || type == Material.NETHERITE_SWORD;
    }

    private boolean isWeapon(Material type) {
        return this.isSword(type) || this.isAxe(type) || type == Material.BOW || type == Material.CROSSBOW || type == Material.TRIDENT;
    }

    private boolean isArmor(Material type) {
        String name = type.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    private boolean isBoots(Material type) {
        return type.name().endsWith("_BOOTS");
    }

    public void clearCache(UUID uuid) {
    }
}

