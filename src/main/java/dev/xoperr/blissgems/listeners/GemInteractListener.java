/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.entity.EntityPickupItemEvent
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.GemType;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

public class GemInteractListener
implements Listener {
    private final BlissGems plugin;
    private final Map<UUID, Long> traderCooldowns;

    public GemInteractListener(BlissGems plugin) {
        this.plugin = plugin;
        this.traderCooldowns = new HashMap<UUID, Long>();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }

        // BUNDLE PROTECTION: Prevent gems from being placed in bundles (COMPREHENSIVE)
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        String mainHandId = CustomItemManager.getIdByItem(mainHand);
        String offHandId = CustomItemManager.getIdByItem(offHand);

        boolean hasGemInMainHand = mainHandId != null && GemType.isGem(mainHandId);
        boolean hasGemInOffHand = offHandId != null && GemType.isGem(offHandId);
        boolean hasBundleInMainHand = mainHand != null && mainHand.getType() == Material.BUNDLE;
        boolean hasBundleInOffHand = offHand != null && offHand.getType() == Material.BUNDLE;

        // Block ANY right-click interaction if gem and bundle are in either hand
        if ((hasGemInMainHand && hasBundleInOffHand) ||
            (hasGemInOffHand && hasBundleInMainHand) ||
            (hasGemInMainHand && item != null && item.getType() == Material.BUNDLE) ||
            (hasGemInOffHand && item != null && item.getType() == Material.BUNDLE)) {
            event.setCancelled(true);
            String msg = plugin.getConfigManager().getFormattedMessage("cannot-store-gem-bundle");
            if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
            return;
        }

        // Also block if clicking WITH a bundle while a gem is in either hand
        if (item != null && item.getType() == Material.BUNDLE && (hasGemInMainHand || hasGemInOffHand)) {
            event.setCancelled(true);
            String msg = plugin.getConfigManager().getFormattedMessage("cannot-store-gem-bundle");
            if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
            return;
        }

        String oraxenId = CustomItemManager.getIdByItem((ItemStack)item);
        if (oraxenId == null) {
            return;
        }
        switch (oraxenId) {
            case "energy_bottle": {
                this.handleEnergyBottle(player, item, event);
                break;
            }
            case "gem_trader": {
                this.handleGemTrader(player, item, event);
            }
        }
        if (oraxenId.endsWith("_gem_t1") || oraxenId.endsWith("_gem_t2")) {
            this.handleGemAbility(player, oraxenId, event);
        }
    }

    private void handleGemAbility(Player player, String oraxenId, PlayerInteractEvent event) {
        event.setCancelled(true);

        // Check if click activation is enabled for this player
        if (!this.plugin.getClickActivationManager().isClickActivationEnabled(player)) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("click-activation-disabled", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }

        int energy = this.plugin.getEnergyManager().getEnergy(player);
        if (energy <= 0) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        GemType gemType = GemType.fromOraxenId(oraxenId);
        if (gemType == null) {
            return;
        }
        int tier = oraxenId.endsWith("_gem_t2") ? 2 : 1;
        switch (gemType) {
            case ASTRA: {
                this.plugin.getAstraAbilities().onRightClick(player, tier);
                break;
            }
            case FIRE: {
                this.plugin.getFireAbilities().onRightClick(player, tier);
                break;
            }
            case FLUX: {
                this.plugin.getFluxAbilities().onRightClick(player, tier);
                break;
            }
            case LIFE: {
                this.plugin.getLifeAbilities().onRightClick(player, tier);
                break;
            }
            case PUFF: {
                this.plugin.getPuffAbilities().onRightClick(player, tier);
                break;
            }
            case SPEED: {
                this.plugin.getSpeedAbilities().onRightClick(player, tier);
                break;
            }
            case STRENGTH: {
                this.plugin.getStrengthAbilities().onRightClick(player, tier);
                break;
            }
            case WEALTH: {
                this.plugin.getWealthAbilities().onRightClick(player, tier);
            }
        }
    }

    private void handleEnergyBottle(Player player, ItemStack item, PlayerInteractEvent event) {
        event.setCancelled(true);
        this.plugin.getEnergyManager().addEnergy(player, 1);
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
        if (this.plugin.getConfigManager().isEnergyBottleDropEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.spawnParticle(Particle.HEART, player.getLocation().add(0.0, 1.0, 0.0), 20, 0.5, 0.5, 0.5);
        }
        String msg = this.plugin.getConfigManager().getFormattedMessage("energy-bottle-consumed", new Object[0]);
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    private void handleGemTrader(Player player, ItemStack traderItem, PlayerInteractEvent event) {
        long timeLeft;
        event.setCancelled(true);
        long now = System.currentTimeMillis();
        Long lastUse = this.traderCooldowns.get(player.getUniqueId());
        int cooldownSeconds = this.plugin.getConfigManager().getTraderCooldown();
        if (lastUse != null && (timeLeft = lastUse + (long)cooldownSeconds * 1000L - now) > 0L) {
            int secondsLeft = (int)Math.ceil((double)timeLeft / 1000.0);
            String msg = this.plugin.getConfigManager().getFormattedMessage("trade-cooldown", "seconds", secondsLeft);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        if (!this.plugin.getGemManager().hasActiveGem(player)) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("no-gem", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        GemType currentType = this.plugin.getGemManager().getGemType(player);
        if (currentType == null) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("no-gem", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        ArrayList<GemType> availableTypes = new ArrayList<GemType>();
        for (GemType type : GemType.values()) {
            if (type == currentType || !this.plugin.getConfigManager().isGemEnabled(type)) continue;
            availableTypes.add(type);
        }
        if (availableTypes.isEmpty()) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "No other gem types available!");
            return;
        }
        GemType newType = (GemType)((Object)availableTypes.get((int)(Math.random() * (double)availableTypes.size())));
        if (this.plugin.getGemManager().replaceGemType(player, newType)) {
            if (traderItem.getAmount() > 1) {
                traderItem.setAmount(traderItem.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            this.traderCooldowns.put(player.getUniqueId(), now);
            if (this.plugin.getConfigManager().shouldPlayTradeEffects()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.spawnParticle(Particle.PORTAL, player.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5);
            }
            String msg = this.plugin.getConfigManager().getFormattedMessage("trade-success", "gem", newType.getDisplayName());
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
        } else {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Failed to trade gem!");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (humanEntity instanceof Player) {
            Player player = (Player)humanEntity;

            // Check if clicking on a gem item
            ItemStack clickedItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();
            ItemStack hotbarItem = null;

            // For number key clicks, get the hotbar item
            if (event.getClick().toString().contains("NUMBER_KEY")) {
                int hotbarSlot = event.getHotbarButton();
                if (hotbarSlot >= 0 && hotbarSlot < 9) {
                    hotbarItem = player.getInventory().getItem(hotbarSlot);
                }
            }

            // CRITICAL PROTECTION: Prevent clicking on gems when inventory is full
            // This prevents the gem from ending up on cursor with nowhere to go
            PlayerInventory inv = player.getInventory();
            boolean inventoryFull = inv.firstEmpty() == -1;

            if (inventoryFull && event.getClickedInventory() == inv) {
                // Check if clicking on a gem
                String clickedId = clickedItem != null ? CustomItemManager.getIdByItem(clickedItem) : null;
                if (clickedId != null && GemType.isGem(clickedId)) {
                    // Trying to click on a gem with full inventory
                    event.setCancelled(true);
                    String msg = plugin.getConfigManager().getFormattedMessage("cannot-move-gem-full-inventory");
                    if (msg != null && !msg.isEmpty()) {
                        player.sendMessage(msg);
                    }
                    return;
                }

                // Check if trying to place something where a gem is
                if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                    if (clickedItem != null) {
                        String slotId = CustomItemManager.getIdByItem(clickedItem);
                        if (slotId != null && GemType.isGem(slotId)) {
                            // Trying to swap with a gem slot when inventory is full
                            event.setCancelled(true);
                            player.sendMessage("§c§lYou cannot swap items with your gem when inventory is full!");
                            return;
                        }
                    }
                }
            }

            // Check if player has an open inventory that is NOT their own
            boolean hasContainerOpen = event.getView().getTopInventory() != null
                && event.getView().getTopInventory().getHolder() != player;

            // If a container is open, block ALL gem movements into it
            if (hasContainerOpen) {
                // Block SWAP_OFFHAND (F key) when hovering over container
                if (event.getClick().toString().equals("SWAP_OFFHAND")) {
                    ItemStack offhandItem = player.getInventory().getItemInOffHand();
                    if (offhandItem != null) {
                        String offhandOraxenId = CustomItemManager.getIdByItem(offhandItem);
                        if (offhandOraxenId != null && GemType.isGem(offhandOraxenId)) {
                            event.setCancelled(true);
                            String msg = plugin.getConfigManager().getFormattedMessage("cannot-store-gem-container");
                        if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
                            return;
                        }
                    }
                }
                // Block cursor placement into container
                if (event.getClickedInventory() != player.getInventory() && cursorItem != null) {
                    String cursorOraxenId = CustomItemManager.getIdByItem(cursorItem);
                    if (cursorOraxenId != null && GemType.isGem(cursorOraxenId)) {
                        event.setCancelled(true);
                        String msg = plugin.getConfigManager().getFormattedMessage("cannot-store-gem-container");
                        if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
                        return;
                    }
                }

                // Block shift-click from player inventory to container
                if (event.getClick().isShiftClick() && event.getClickedInventory() == player.getInventory() && clickedItem != null) {
                    String clickedOraxenId = CustomItemManager.getIdByItem(clickedItem);
                    if (clickedOraxenId != null && GemType.isGem(clickedOraxenId)) {
                        event.setCancelled(true);
                        String msg = plugin.getConfigManager().getFormattedMessage("cannot-store-gem-container");
                        if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
                        return;
                    }
                }

                // Block hotbar swap from player inventory to container
                if (event.getClickedInventory() != player.getInventory() && hotbarItem != null) {
                    String hotbarOraxenId = CustomItemManager.getIdByItem(hotbarItem);
                    if (hotbarOraxenId != null && GemType.isGem(hotbarOraxenId)) {
                        event.setCancelled(true);
                        String msg = plugin.getConfigManager().getFormattedMessage("cannot-store-gem-container");
                        if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
                        return;
                    }
                }

                // Block double-click collect into container
                if (event.getClick().toString().equals("DOUBLE_CLICK") && cursorItem != null) {
                    String cursorOraxenId = CustomItemManager.getIdByItem(cursorItem);
                    if (cursorOraxenId != null && GemType.isGem(cursorOraxenId)) {
                        event.setCancelled(true);
                        String msg = plugin.getConfigManager().getFormattedMessage("cannot-store-gem-container");
                        if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
                        return;
                    }
                }
            }

            // Block bundle interactions - bundles don't create separate inventory
            // We need to check multiple scenarios:
            // 1. Clicking a bundle while holding a gem
            // 2. Clicking a gem while holding a bundle
            // 3. Shift-clicking when a bundle is involved
            // 4. Hotbar swap involving bundles and gems

            boolean hasBundleInInventory = false;
            boolean hasGemInInventory = false;

            // Check if clicked item is a bundle or gem
            if (clickedItem != null) {
                if (clickedItem.getType() == Material.BUNDLE) {
                    hasBundleInInventory = true;
                    // If clicking bundle with gem on cursor, block it
                    String cursorId = cursorItem != null ? CustomItemManager.getIdByItem(cursorItem) : null;
                    if (cursorId != null && GemType.isGem(cursorId)) {
                        event.setCancelled(true);
                        String msg = plugin.getConfigManager().getFormattedMessage("cannot-store-gem-bundle");
            if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
                        return;
                    }
                }
                String clickedId = CustomItemManager.getIdByItem(clickedItem);
                if (clickedId != null && GemType.isGem(clickedId)) {
                    hasGemInInventory = true;
                    // If clicking gem with bundle on cursor, block it
                    if (cursorItem != null && cursorItem.getType() == Material.BUNDLE) {
                        event.setCancelled(true);
                        String msg = plugin.getConfigManager().getFormattedMessage("cannot-store-gem-bundle");
            if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
                        return;
                    }
                }
            }

            // Check if cursor item is a bundle or gem
            if (cursorItem != null) {
                if (cursorItem.getType() == Material.BUNDLE) {
                    hasBundleInInventory = true;
                }
                String cursorId = CustomItemManager.getIdByItem(cursorItem);
                if (cursorId != null && GemType.isGem(cursorId)) {
                    hasGemInInventory = true;
                }
            }

            // Check hotbar item for bundle/gem
            if (hotbarItem != null) {
                if (hotbarItem.getType() == Material.BUNDLE && hasGemInInventory) {
                    event.setCancelled(true);
                    String msg = plugin.getConfigManager().getFormattedMessage("cannot-store-gem-bundle");
            if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
                    return;
                }
                String hotbarId = CustomItemManager.getIdByItem(hotbarItem);
                if (hotbarId != null && GemType.isGem(hotbarId) && hasBundleInInventory) {
                    event.setCancelled(true);
                    String msg = plugin.getConfigManager().getFormattedMessage("cannot-store-gem-bundle");
            if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
                    return;
                }
            }

            // Check if trying to add a gem when already have one (in player inventory)
            if (cursorItem != null && event.getClickedInventory() == player.getInventory()) {
                String cursorOraxenId = CustomItemManager.getIdByItem((ItemStack)cursorItem);
                if (cursorOraxenId != null && GemType.isGem(cursorOraxenId)) {
                    // Player is trying to place a gem
                    int currentGemCount = countGemsInInventory(player);
                    // If they already have a gem and this isn't just moving their existing gem
                    if (currentGemCount > 0) {
                        // Check if they're moving their own gem (not adding a new one)
                        boolean isMovingOwnGem = false;
                        if (clickedItem != null) {
                            String clickedOraxenId = CustomItemManager.getIdByItem((ItemStack)clickedItem);
                            if (clickedOraxenId != null && GemType.isGem(clickedOraxenId)) {
                                isMovingOwnGem = true;
                            }
                        }

                        if (!isMovingOwnGem) {
                            event.setCancelled(true);
                            String msg = this.plugin.getConfigManager().getFormattedMessage("already-have-gem", new Object[0]);
                            if (msg != null && !msg.isEmpty()) {
                                player.sendMessage(msg);
                            }
                            return;
                        }
                    }
                }
            }

            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.plugin.getGemManager().updateActiveGem(player), 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player)event.getEntity();
        Item itemEntity = event.getItem();
        ItemStack item = itemEntity.getItemStack();

        String oraxenId = CustomItemManager.getIdByItem((ItemStack)item);

        // If player has a gem, prevent picking up any items if inventory is full
        // This prevents gems from being pushed out when inventory is full
        if (countGemsInInventory(player) > 0) {
            PlayerInventory inv = player.getInventory();
            // Check if inventory has no empty slots
            if (inv.firstEmpty() == -1) {
                // Inventory is full, cancel pickup to prevent gem loss
                event.setCancelled(true);
                return;
            }
            // Also check if adding this item would fill the last slot
            // and potentially cause issues
            int emptySlots = 0;
            for (ItemStack slot : inv.getStorageContents()) {
                if (slot == null || slot.getType() == Material.AIR) {
                    emptySlots++;
                }
            }
            // If only one empty slot left, be extra cautious
            if (emptySlots <= 1) {
                event.setCancelled(true);
                return;
            }
        }

        if (oraxenId == null || !GemType.isGem(oraxenId)) {
            return;
        }

        // Check if player already has a gem
        int currentGemCount = countGemsInInventory(player);
        if (currentGemCount > 0) {
            event.setCancelled(true);
            String msg = this.plugin.getConfigManager().getFormattedMessage("already-have-gem", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();
        Item itemEntity = event.getItem();
        ItemStack item = itemEntity.getItemStack();

        // If player has a gem, prevent picking up items when inventory is full
        if (countGemsInInventory(player) > 0) {
            PlayerInventory inv = player.getInventory();
            if (inv.firstEmpty() == -1) {
                event.setCancelled(true);
                return;
            }
            // Extra check for nearly full inventory
            int emptySlots = 0;
            for (ItemStack slot : inv.getStorageContents()) {
                if (slot == null || slot.getType() == Material.AIR) {
                    emptySlots++;
                }
            }
            if (emptySlots <= 1) {
                event.setCancelled(true);
                return;
            }
        }

        // Check if trying to pick up a gem when already has one
        String oraxenId = CustomItemManager.getIdByItem(item);
        if (oraxenId != null && GemType.isGem(oraxenId)) {
            int currentGemCount = countGemsInInventory(player);
            if (currentGemCount > 0) {
                event.setCancelled(true);
                String msg = this.plugin.getConfigManager().getFormattedMessage("already-have-gem", new Object[0]);
                if (msg != null && !msg.isEmpty()) {
                    player.sendMessage(msg);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Prevent dragging gems to create duplicates or issues
        ItemStack draggedItem = event.getOldCursor();
        if (draggedItem != null) {
            String draggedId = CustomItemManager.getIdByItem(draggedItem);
            if (draggedId != null && GemType.isGem(draggedId)) {
                // Allow dragging within player's own inventory but prevent issues
                if (event.getInventory().getType() != InventoryType.PLAYER) {
                    // Check if dragging to another inventory (chest, etc.)
                    for (int slot : event.getRawSlots()) {
                        if (slot < event.getView().getTopInventory().getSize()) {
                            event.setCancelled(true);
                            player.sendMessage("§c§lYou cannot move gems to other containers!");
                            return;
                        }
                    }
                }
            }
        }

        // Prevent any inventory operations when inventory is full and player has gem
        if (countGemsInInventory(player) > 0) {
            PlayerInventory inv = player.getInventory();
            if (inv.firstEmpty() == -1) {
                // Check if this drag operation involves player inventory
                for (int slot : event.getRawSlots()) {
                    if (slot >= event.getView().getTopInventory().getSize()) {
                        // This involves player inventory and it's full
                        event.setCancelled(true);
                        player.sendMessage("§c§lYour inventory is full! Cannot move items while carrying a gem.");
                        return;
                    }
                }
            }
        }
    }

    private int countGemsInInventory(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            String oraxenId = CustomItemManager.getIdByItem((ItemStack)item);
            if (oraxenId != null && GemType.isGem(oraxenId)) {
                count++;
            }
        }
        return count;
    }

    // Drop prevention moved to GemDropListener (uses DropItemControl's PDC-based approach)

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        // Check if placing gem in item frame
        if (event.getRightClicked() instanceof ItemFrame) {
            // Check both mainhand and offhand for gems
            String mainHandId = CustomItemManager.getIdByItem(mainHand);
            String offHandId = CustomItemManager.getIdByItem(offHand);

            if ((mainHandId != null && GemType.isGem(mainHandId)) ||
                (offHandId != null && GemType.isGem(offHandId))) {
                event.setCancelled(true);
                String msg = plugin.getConfigManager().getFormattedMessage("cannot-place-gem-itemframe");
                if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        ItemStack cursorItem = player.getItemOnCursor();

        // Check if player has a gem on their cursor when closing inventory
        if (cursorItem != null && cursorItem.getType() != Material.AIR) {
            String cursorId = CustomItemManager.getIdByItem(cursorItem);
            if (cursorId != null && GemType.isGem(cursorId)) {
                // Player is closing inventory with a gem on cursor
                // GEMS ARE UNDROPPABLE - must force it back into inventory
                PlayerInventory inv = player.getInventory();
                int emptySlot = inv.firstEmpty();

                if (emptySlot != -1) {
                    // Found an empty slot, put the gem there
                    inv.setItem(emptySlot, cursorItem);
                    player.setItemOnCursor(null);
                } else {
                    // No empty slots - find any non-gem item and replace it with the gem
                    // The non-gem item will be dropped instead
                    boolean gemPlaced = false;

                    // Try to replace item in main inventory first (skip hotbar to avoid disrupting quick access)
                    for (int i = 9; i < 36; i++) {
                        ItemStack slotItem = inv.getItem(i);
                        if (slotItem != null && slotItem.getType() != Material.AIR) {
                            String slotId = CustomItemManager.getIdByItem(slotItem);
                            if (slotId == null || !GemType.isGem(slotId)) {
                                // This is a non-gem item, replace it
                                ItemStack replacedItem = slotItem.clone();
                                inv.setItem(i, cursorItem);
                                player.setItemOnCursor(null);

                                // Drop the replaced item
                                player.getWorld().dropItemNaturally(player.getLocation(), replacedItem);
                                String msg = plugin.getConfigManager().getFormattedMessage("gem-forced-into-inventory");
                                if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
                                gemPlaced = true;
                                break;
                            }
                        }
                    }

                    // If still not placed, try hotbar
                    if (!gemPlaced) {
                        for (int i = 0; i < 9; i++) {
                            ItemStack slotItem = inv.getItem(i);
                            if (slotItem != null && slotItem.getType() != Material.AIR) {
                                String slotId = CustomItemManager.getIdByItem(slotItem);
                                if (slotId == null || !GemType.isGem(slotId)) {
                                    ItemStack replacedItem = slotItem.clone();
                                    inv.setItem(i, cursorItem);
                                    player.setItemOnCursor(null);
                                    player.getWorld().dropItemNaturally(player.getLocation(), replacedItem);
                                    String msg = plugin.getConfigManager().getFormattedMessage("gem-forced-into-inventory");
                                if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
                                    gemPlaced = true;
                                    break;
                                }
                            }
                        }
                    }

                    // If still not placed (somehow entire inventory is gems?), force it into first slot
                    if (!gemPlaced) {
                        inv.setItem(0, cursorItem);
                        player.setItemOnCursor(null);
                        String msg = plugin.getConfigManager().getFormattedMessage("gem-forced-into-hotbar");
                        if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
                    }
                }
            }
        }
    }

    /**
     * Trigger a gem ability via command (bypasses click activation check)
     * @param player The player
     * @param secondary true for secondary ability (shift), false for primary
     */
    public void triggerAbilityViaCommand(Player player, boolean secondary) {
        // Check if player is holding a gem in main or offhand
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        String oraxenId = CustomItemManager.getIdByItem(mainHand);
        if (oraxenId == null || !GemType.isGem(oraxenId)) {
            // Try offhand
            oraxenId = CustomItemManager.getIdByItem(offHand);
            if (oraxenId == null || !GemType.isGem(oraxenId)) {
                player.sendMessage("§c§lYou must be holding a gem to use this command!");
                return;
            }
        }

        // Check energy
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        if (energy <= 0) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }

        GemType gemType = GemType.fromOraxenId(oraxenId);
        if (gemType == null) {
            return;
        }

        int tier = oraxenId.endsWith("_gem_t2") ? 2 : 1;

        // For secondary abilities, check if tier is 2
        if (secondary && tier < 2) {
            player.sendMessage("§c§lSecondary abilities require Tier 2 gem!");
            return;
        }

        // Call the ability with fake sneaking state for secondary
        boolean wasSneaking = player.isSneaking();
        try {
            // Temporarily set sneaking state if needed
            if (secondary && !wasSneaking) {
                // We can't force sneak, so we'll need to pass tier and sneak flag separately
                // For now, tell player to shift
                player.sendMessage("§e§oShift + use ability or right-click for secondary ability!");
                return;
            }

            // Trigger ability based on gem type
            switch (gemType) {
                case ASTRA:
                    this.plugin.getAstraAbilities().onRightClick(player, tier);
                    break;
                case FIRE:
                    this.plugin.getFireAbilities().onRightClick(player, tier);
                    break;
                case FLUX:
                    this.plugin.getFluxAbilities().onRightClick(player, tier);
                    break;
                case LIFE:
                    this.plugin.getLifeAbilities().onRightClick(player, tier);
                    break;
                case PUFF:
                    this.plugin.getPuffAbilities().onRightClick(player, tier);
                    break;
                case SPEED:
                    this.plugin.getSpeedAbilities().onRightClick(player, tier);
                    break;
                case STRENGTH:
                    this.plugin.getStrengthAbilities().onRightClick(player, tier);
                    break;
                case WEALTH:
                    this.plugin.getWealthAbilities().onRightClick(player, tier);
                    break;
            }
        } finally {
            // Restore original sneaking state (not needed since we can't change it)
        }
    }
}

