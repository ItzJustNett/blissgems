/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.entity.Allay
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.ItemFrame
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.entity.EntityPickupItemEvent
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.inventory.InventoryType
 *  org.bukkit.event.player.PlayerInteractEntityEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.inventory.meta.BundleMeta
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.api.GemDefinition;
import dev.xoperr.blissgems.listeners.ComprehensiveGemProtectionListener;
import dev.xoperr.blissgems.managers.GemLockManager;
import dev.xoperr.blissgems.managers.GemRegistryImpl;
import dev.xoperr.blissgems.utils.AbilityBinding;
import dev.xoperr.blissgems.utils.AbilitySlot;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Allay;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class GemInteractListener
implements Listener {
    private final BlissGems plugin;
    private final Map<UUID, Long> traderCooldowns;
    private final Map<UUID, Long> clickDisabledMessageCooldowns = new HashMap<UUID, Long>();
    private static final long CLICK_DISABLED_MSG_INTERVAL = 30000L;
    private static final long TRADE_ABILITY_GRACE_MS = 500L;

    public GemInteractListener(BlissGems plugin) {
        this.plugin = plugin;
        this.traderCooldowns = new HashMap<UUID, Long>();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        boolean hasBundleInOffHand;
        boolean isLeft;
        Action a = event.getAction();
        boolean isRight = a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK;
        boolean bl = isLeft = a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK;
        if (!isRight && !isLeft) {
            return;
        }
        if (isLeft) {
            this.handleLeftClickAbility(event);
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        String mainHandId = CustomItemManager.getIdByItem(mainHand);
        String offHandId = CustomItemManager.getIdByItem(offHand);
        boolean hasGemInMainHand = mainHandId != null && GemType.isGem(mainHandId);
        boolean hasGemInOffHand = offHandId != null && GemType.isGem(offHandId);
        boolean hasBundleInMainHand = mainHand != null && mainHand.getType() == Material.BUNDLE;
        boolean bl2 = hasBundleInOffHand = offHand != null && offHand.getType() == Material.BUNDLE;
        if (hasGemInMainHand && hasBundleInOffHand || hasGemInOffHand && hasBundleInMainHand || hasGemInMainHand && item != null && item.getType() == Material.BUNDLE || hasGemInOffHand && item != null && item.getType() == Material.BUNDLE) {
            event.setCancelled(true);
            String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-bundle", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        if (item != null && item.getType() == Material.BUNDLE && (hasGemInMainHand || hasGemInOffHand)) {
            event.setCancelled(true);
            String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-bundle", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        if (item != null && item.getType() == Material.BUNDLE && this.bundleContainsGem(item)) {
            event.setCancelled(true);
            player.sendMessage("\u00a7c\u00a7lThis bundle contains a gem! Remove the gem first before using it.");
            return;
        }
        String oraxenId = CustomItemManager.getIdByItem(item);
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
        Long lastTrade = this.traderCooldowns.get(player.getUniqueId());
        if (lastTrade != null && System.currentTimeMillis() - lastTrade < 500L) {
            return;
        }
        AbilityBinding input = AbilityBinding.rightClick(player.isSneaking());
        this.dispatchBoundAbility(player, input);
    }

    private void handleLeftClickAbility(PlayerInteractEvent event) {
        boolean extraSlot;
        AbilitySlot slot;
        boolean offhandGold;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        String id = item != null ? CustomItemManager.getIdByItem(item) : null;
        boolean heldGem = id != null && (id.endsWith("_gem_t1") || id.endsWith("_gem_t2"));
        boolean bl = offhandGold = this.plugin.getGoldGemManager() != null && this.plugin.getGoldGemManager().isGoldGem(player.getInventory().getItemInOffHand());
        if (!(heldGem || offhandGold && event.getAction() == Action.LEFT_CLICK_AIR)) {
            return;
        }
        AbilityBinding input = AbilityBinding.leftClick(player.isSneaking());
        AbilitySlot abilitySlot = slot = this.plugin.getAbilityBindingManager() != null ? this.plugin.getAbilityBindingManager().getSlot(player, input) : null;
        if (slot == null) {
            return;
        }
        boolean bl2 = extraSlot = slot == AbilitySlot.QUINARY || slot == AbilitySlot.SENARY;
        if (extraSlot && !offhandGold && !"gold_gem_t1".equals(id)) {
            return;
        }
        event.setCancelled(true);
        if (!this.gateAbility(player)) {
            return;
        }
        this.plugin.getBlissCommand().triggerSlot(player, slot);
    }

    private void dispatchBoundAbility(Player player, AbilityBinding input) {
        AbilitySlot slot;
        if (!this.gateAbility(player)) {
            return;
        }
        AbilitySlot abilitySlot = slot = this.plugin.getAbilityBindingManager() != null ? this.plugin.getAbilityBindingManager().getSlot(player, input) : null;
        if (slot != null) {
            this.plugin.getBlissCommand().triggerSlot(player, slot);
            return;
        }
        String oraxenId = CustomItemManager.getIdByItem(player.getInventory().getItemInMainHand());
        if (oraxenId == null || !oraxenId.endsWith("_gem_t1") && !oraxenId.endsWith("_gem_t2")) {
            oraxenId = CustomItemManager.getIdByItem(player.getInventory().getItemInOffHand());
        }
        if (oraxenId == null) {
            return;
        }
        int tier = oraxenId.endsWith("_gem_t2") ? 2 : 1;
        GemType gemType = GemType.fromOraxenId(oraxenId);
        if (gemType != null) {
            switch (gemType) {
                case ASTRA: {
                    this.plugin.getAstraAbilities().onRightClick(player, tier);
                    return;
                }
                case FIRE: {
                    this.plugin.getFireAbilities().onRightClick(player, tier);
                    return;
                }
                case FLUX: {
                    this.plugin.getFluxAbilities().onRightClick(player, tier);
                    return;
                }
                case LIFE: {
                    this.plugin.getLifeAbilities().onRightClick(player, tier);
                    return;
                }
                case PUFF: {
                    this.plugin.getPuffAbilities().onRightClick(player, tier);
                    return;
                }
                case SPEED: {
                    this.plugin.getSpeedAbilities().onRightClick(player, tier);
                    return;
                }
                case STRENGTH: {
                    this.plugin.getStrengthAbilities().onRightClick(player, tier);
                    return;
                }
                case WEALTH: {
                    this.plugin.getWealthAbilities().onRightClick(player, tier);
                    return;
                }
            }
        }
    }

    private boolean gateAbility(Player player) {
        GemLockManager lockMgr = this.plugin.getGemLockManager();
        if (lockMgr != null && lockMgr.isLocked(player)) {
            int left = lockMgr.getRemainingSeconds(player.getUniqueId());
            player.sendMessage("\u00a76\ua42c \u00a7c\u00a7oYour gem is locked! \u00a77(" + left + "s)");
            return false;
        }
        if (!this.plugin.getClickActivationManager().isClickActivationEnabled(player)) {
            long now = System.currentTimeMillis();
            Long lastSent = this.clickDisabledMessageCooldowns.get(player.getUniqueId());
            if (lastSent == null || now - lastSent >= 30000L) {
                this.clickDisabledMessageCooldowns.put(player.getUniqueId(), now);
                String msg = this.plugin.getConfigManager().getFormattedMessage("click-activation-disabled", new Object[0]);
                if (msg != null && !msg.isEmpty()) {
                    player.sendMessage(msg);
                }
            }
            return false;
        }
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        if (energy <= 0) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return false;
        }
        return true;
    }

    private void handleEnergyBottle(Player player, ItemStack item, PlayerInteractEvent event) {
        String msg;
        event.setCancelled(true);
        int currentEnergy = this.plugin.getEnergyManager().getEnergy(player);
        int maxEnergy = this.plugin.getConfigManager().getMaxEnergy();
        if (currentEnergy >= maxEnergy) {
            String msg2 = this.plugin.getConfigManager().getFormattedMessage("energy-already-max", new Object[0]);
            if (msg2 == null || msg2.isEmpty()) {
                player.sendMessage("\u00a7c\u00a7oYou already have maximum energy!");
            } else {
                player.sendMessage(msg2);
            }
            return;
        }
        this.plugin.getEnergyManager().addEnergy(player, 1);
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else if (event.getHand() == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(null);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
        if (this.plugin.getConfigManager().isEnergyBottleDropEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.spawnParticle(Particle.HEART, player.getLocation().add(0.0, 1.0, 0.0), 20, 0.5, 0.5, 0.5);
        }
        if ((msg = this.plugin.getConfigManager().getFormattedMessage("energy-bottle-consumed", new Object[0])) != null && !msg.isEmpty()) {
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
        String currentGemId = this.plugin.getGemManager().getGemId(player);
        ArrayList<String> availableGems = new ArrayList<String>(this.plugin.getGemManager().getAvailableGemIds());
        availableGems.remove(currentGemId);
        if (availableGems.isEmpty()) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "No other gem types available!");
            return;
        }
        String newGemId = (String)availableGems.get((int)(Math.random() * (double)availableGems.size()));
        if (this.plugin.getGemManager().replaceGem(player, newGemId)) {
            String msg;
            if (traderItem.getAmount() > 1) {
                traderItem.setAmount(traderItem.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            this.traderCooldowns.put(player.getUniqueId(), now);
            if (this.plugin.getAchievementManager() != null) {
                this.plugin.getAchievementManager().unlock(player, Achievement.TIME_FOR_A_CHANGE);
            }
            if (this.plugin.getConfigManager().shouldPlayTradeEffects()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.spawnParticle(Particle.PORTAL, player.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5);
            }
            if ((msg = this.plugin.getConfigManager().getFormattedMessage("trade-success", "gem", this.plugin.getGemManager().getGemDisplayName(newGemId))) != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
        } else {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Failed to trade gem!");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("\u00a7b\ud83d\udd2e Flux Charging Station")) {
            if (this.plugin.getFluxEnergyManager() != null) {
                this.plugin.getFluxEnergyManager().onInventoryClick(event);
            }
            return;
        }
        HumanEntity humanEntity = event.getWhoClicked();
        if (humanEntity instanceof Player) {
            int currentGemCount;
            String cursorOraxenId;
            String cursorId;
            boolean hasContainerOpen;
            PlayerInventory inv;
            boolean inventoryFull;
            int hotbarSlot;
            Player player = (Player)humanEntity;
            ItemStack clickedItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();
            ItemStack hotbarItem = null;
            if (event.getClick().toString().contains("NUMBER_KEY") && (hotbarSlot = event.getHotbarButton()) >= 0 && hotbarSlot < 9) {
                hotbarItem = player.getInventory().getItem(hotbarSlot);
            }
            boolean bl = inventoryFull = (inv = player.getInventory()).firstEmpty() == -1;
            if (inventoryFull && event.getClickedInventory() == inv) {
                String slotId;
                String clickedId;
                String string = clickedId = clickedItem != null ? CustomItemManager.getIdByItem(clickedItem) : null;
                if (clickedId != null && GemType.isGem(clickedId)) {
                    event.setCancelled(true);
                    String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-move-gem-full-inventory", new Object[0]);
                    if (msg != null && !msg.isEmpty()) {
                        player.sendMessage(msg);
                    }
                    return;
                }
                if (cursorItem != null && cursorItem.getType() != Material.AIR && clickedItem != null && (slotId = CustomItemManager.getIdByItem(clickedItem)) != null && GemType.isGem(slotId)) {
                    event.setCancelled(true);
                    player.sendMessage("\u00a7c\u00a7lYou cannot swap items with your gem when inventory is full!");
                    return;
                }
            }
            if (event.getView().getTopInventory().getType() == InventoryType.CRAFTING) {
                String clickedOraxenId;
                int slot;
                if (event.getClickedInventory() == event.getView().getTopInventory() && (slot = event.getRawSlot()) >= 1 && slot <= 4) {
                    String hotbarId;
                    String cursorId2;
                    if (cursorItem != null && cursorItem.getType() != Material.AIR && (cursorId2 = CustomItemManager.getIdByItem(cursorItem)) != null && GemType.isGem(cursorId2)) {
                        event.setCancelled(true);
                        return;
                    }
                    if (hotbarItem != null && (hotbarId = CustomItemManager.getIdByItem(hotbarItem)) != null && GemType.isGem(hotbarId)) {
                        event.setCancelled(true);
                        return;
                    }
                }
                if (event.isShiftClick() && event.getClickedInventory() == player.getInventory() && clickedItem != null && (clickedOraxenId = CustomItemManager.getIdByItem(clickedItem)) != null && GemType.isGem(clickedOraxenId)) {
                    event.setCancelled(true);
                    return;
                }
            }
            boolean bl2 = hasContainerOpen = event.getView().getTopInventory() != null && event.getView().getTopInventory().getHolder() != player;
            if (hasContainerOpen) {
                String hotbarOraxenId;
                String clickedOraxenId;
                String msg;
                String cursorOraxenId2;
                String offhandOraxenId;
                ItemStack offhandItem;
                if (event.getClick().toString().equals("SWAP_OFFHAND") && (offhandItem = player.getInventory().getItemInOffHand()) != null && (offhandOraxenId = CustomItemManager.getIdByItem(offhandItem)) != null && GemType.isGem(offhandOraxenId)) {
                    event.setCancelled(true);
                    ComprehensiveGemProtectionListener.resyncOffhand(this.plugin, player);
                    String msg2 = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-container", new Object[0]);
                    if (msg2 != null && !msg2.isEmpty()) {
                        player.sendMessage(msg2);
                    }
                    return;
                }
                if (event.getClickedInventory() != player.getInventory() && cursorItem != null && (cursorOraxenId2 = CustomItemManager.getIdByItem(cursorItem)) != null && GemType.isGem(cursorOraxenId2)) {
                    event.setCancelled(true);
                    msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-container", new Object[0]);
                    if (msg != null && !msg.isEmpty()) {
                        player.sendMessage(msg);
                    }
                    return;
                }
                if (event.getClick().isShiftClick() && event.getClickedInventory() == player.getInventory() && clickedItem != null && (clickedOraxenId = CustomItemManager.getIdByItem(clickedItem)) != null && GemType.isGem(clickedOraxenId)) {
                    event.setCancelled(true);
                    msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-container", new Object[0]);
                    if (msg != null && !msg.isEmpty()) {
                        player.sendMessage(msg);
                    }
                    return;
                }
                if (event.getClickedInventory() != player.getInventory() && hotbarItem != null && (hotbarOraxenId = CustomItemManager.getIdByItem(hotbarItem)) != null && GemType.isGem(hotbarOraxenId)) {
                    event.setCancelled(true);
                    msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-container", new Object[0]);
                    if (msg != null && !msg.isEmpty()) {
                        player.sendMessage(msg);
                    }
                    return;
                }
                if (event.getClick().toString().equals("DOUBLE_CLICK") && cursorItem != null && (cursorOraxenId2 = CustomItemManager.getIdByItem(cursorItem)) != null && GemType.isGem(cursorOraxenId2)) {
                    event.setCancelled(true);
                    msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-container", new Object[0]);
                    if (msg != null && !msg.isEmpty()) {
                        player.sendMessage(msg);
                    }
                    return;
                }
            }
            boolean hasBundleInInventory = false;
            boolean hasGemInInventory = false;
            if (clickedItem != null) {
                String clickedId;
                if (clickedItem.getType() == Material.BUNDLE) {
                    hasBundleInInventory = true;
                    String string = cursorId = cursorItem != null ? CustomItemManager.getIdByItem(cursorItem) : null;
                    if (cursorId != null && GemType.isGem(cursorId)) {
                        event.setCancelled(true);
                        String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-bundle", new Object[0]);
                        if (msg != null && !msg.isEmpty()) {
                            player.sendMessage(msg);
                        }
                        return;
                    }
                }
                if ((clickedId = CustomItemManager.getIdByItem(clickedItem)) != null && GemType.isGem(clickedId)) {
                    hasGemInInventory = true;
                    if (cursorItem != null && cursorItem.getType() == Material.BUNDLE) {
                        event.setCancelled(true);
                        String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-bundle", new Object[0]);
                        if (msg != null && !msg.isEmpty()) {
                            player.sendMessage(msg);
                        }
                        return;
                    }
                }
            }
            if (cursorItem != null) {
                if (cursorItem.getType() == Material.BUNDLE) {
                    hasBundleInInventory = true;
                }
                if ((cursorId = CustomItemManager.getIdByItem(cursorItem)) != null && GemType.isGem(cursorId)) {
                    hasGemInInventory = true;
                }
            }
            if (hotbarItem != null) {
                if (hotbarItem.getType() == Material.BUNDLE && hasGemInInventory) {
                    event.setCancelled(true);
                    String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-bundle", new Object[0]);
                    if (msg != null && !msg.isEmpty()) {
                        player.sendMessage(msg);
                    }
                    return;
                }
                String hotbarId = CustomItemManager.getIdByItem(hotbarItem);
                if (hotbarId != null && GemType.isGem(hotbarId) && hasBundleInInventory) {
                    event.setCancelled(true);
                    String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-store-gem-bundle", new Object[0]);
                    if (msg != null && !msg.isEmpty()) {
                        player.sendMessage(msg);
                    }
                    return;
                }
            }
            if (this.plugin.getConfigManager().isSingleGemOnly() && cursorItem != null && event.getClickedInventory() == player.getInventory() && (cursorOraxenId = CustomItemManager.getIdByItem(cursorItem)) != null && GemType.isGem(cursorOraxenId) && (currentGemCount = this.countGemsInInventory(player)) > 0) {
                String clickedOraxenId;
                boolean isMovingOwnGem = false;
                if (clickedItem != null && (clickedOraxenId = CustomItemManager.getIdByItem(clickedItem)) != null && GemType.isGem(clickedOraxenId)) {
                    isMovingOwnGem = true;
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
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.plugin.getGemManager().updateActiveGem(player), 1L);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        PlayerInventory inv;
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getEntity();
        Item itemEntity = event.getItem();
        ItemStack item = itemEntity.getItemStack();
        String oraxenId = CustomItemManager.getIdByItem(item);
        if (!this.plugin.getConfigManager().isSingleGemOnly()) {
            return;
        }
        if (oraxenId != null && GemType.isGem(oraxenId) && this.countGemsInInventory(player) > 0 && (inv = player.getInventory()).firstEmpty() == -1) {
            event.setCancelled(true);
            return;
        }
        if (oraxenId == null || !GemType.isGem(oraxenId)) {
            return;
        }
        int currentGemCount = this.countGemsInInventory(player);
        if (currentGemCount > 0) {
            event.setCancelled(true);
            String msg = this.plugin.getConfigManager().getFormattedMessage("already-have-gem", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        PlayerInventory inv;
        int slot;
        Iterator iterator;
        String draggedId;
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getWhoClicked();
        ItemStack draggedItem = event.getOldCursor();
        if (draggedItem != null && (draggedId = CustomItemManager.getIdByItem(draggedItem)) != null && GemType.isGem(draggedId) && event.getInventory().getType() != InventoryType.PLAYER) {
            iterator = event.getRawSlots().iterator();
            while (iterator.hasNext()) {
                slot = (Integer)iterator.next();
                if (slot >= event.getView().getTopInventory().getSize()) continue;
                event.setCancelled(true);
                player.sendMessage("\u00a7c\u00a7lYou cannot move gems to other containers!");
                return;
            }
        }
        if (this.countGemsInInventory(player) > 0 && (inv = player.getInventory()).firstEmpty() == -1) {
            iterator = event.getRawSlots().iterator();
            while (iterator.hasNext()) {
                slot = (Integer)iterator.next();
                if (slot < event.getView().getTopInventory().getSize()) continue;
                event.setCancelled(true);
                player.sendMessage("\u00a7c\u00a7lYour inventory is full! Cannot move items while carrying a gem.");
                return;
            }
        }
    }

    private int countGemsInInventory(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            String oraxenId = CustomItemManager.getIdByItem(item);
            if (oraxenId != null && GemType.isGem(oraxenId)) {
                ++count;
                continue;
            }
            if (item.getType() != Material.BUNDLE || !this.bundleContainsGem(item)) continue;
            ++count;
        }
        return count;
    }

    private boolean bundleContainsGem(ItemStack bundle) {
        BundleMeta bundleMeta;
        if (bundle == null || bundle.getType() != Material.BUNDLE) {
            return false;
        }
        if (bundle.getItemMeta() instanceof BundleMeta && (bundleMeta = (BundleMeta)bundle.getItemMeta()).hasItems()) {
            for (ItemStack bundledItem : bundleMeta.getItems()) {
                String itemId;
                if (bundledItem == null || (itemId = CustomItemManager.getIdByItem(bundledItem)) == null || !GemType.isGem(itemId)) continue;
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (event.getRightClicked() instanceof ItemFrame || event.getRightClicked() instanceof Allay) {
            String mainHandId = CustomItemManager.getIdByItem(mainHand);
            String offHandId = CustomItemManager.getIdByItem(offHand);
            if (mainHandId != null && GemType.isGem(mainHandId) || offHandId != null && GemType.isGem(offHandId)) {
                event.setCancelled(true);
                String msg = this.plugin.getConfigManager().getFormattedMessage("cannot-place-gem-itemframe", new Object[0]);
                if (msg != null && !msg.isEmpty()) {
                    player.sendMessage(msg);
                }
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        String cursorId;
        ItemStack cursorItem;
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getPlayer();
        if (event.getView().getTitle().equals("\u00a7b\ud83d\udd2e Flux Charging Station")) {
            if (this.plugin.getFluxEnergyManager() != null) {
                this.plugin.getFluxEnergyManager().onInventoryClose(event);
            }
            return;
        }
        if (event.getView().getTitle().equals("\u00a76\u00a7lPockets")) {
            this.plugin.getWealthAbilities().savePocketsInventory(player.getUniqueId());
        }
        if (event.getView().getTopInventory().getType() == InventoryType.CRAFTING) {
            for (int i = 1; i <= 4; ++i) {
                String craftId;
                ItemStack craftSlot = event.getView().getTopInventory().getItem(i);
                if (craftSlot == null || craftSlot.getType().isAir() || (craftId = CustomItemManager.getIdByItem(craftSlot)) == null || !GemType.isGem(craftId)) continue;
                event.getView().getTopInventory().setItem(i, null);
                this.forceGemIntoInventory(player, craftSlot);
            }
        }
        if ((cursorItem = player.getItemOnCursor()) != null && cursorItem.getType() != Material.AIR && (cursorId = CustomItemManager.getIdByItem(cursorItem)) != null && GemType.isGem(cursorId)) {
            player.setItemOnCursor(null);
            this.forceGemIntoInventory(player, cursorItem);
        }
        this.enforceOneGemOnly(player);
    }

    private void enforceOneGemOnly(Player player) {
        if (!this.plugin.getConfigManager().isSingleGemOnly()) {
            return;
        }
        PlayerInventory inv = player.getInventory();
        boolean foundFirstGem = false;
        ArrayList<ItemStack> gemsToDrop = new ArrayList<ItemStack>();
        for (int i = 0; i < inv.getSize(); ++i) {
            List<ItemStack> extractedGems;
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            String itemId = CustomItemManager.getIdByItem(item);
            if (itemId != null && GemType.isGem(itemId)) {
                if (!foundFirstGem) {
                    foundFirstGem = true;
                    continue;
                }
                gemsToDrop.add(item.clone());
                inv.setItem(i, null);
                continue;
            }
            if (item.getType() != Material.BUNDLE || (extractedGems = this.extractGemsFromBundle(item, i, inv)).isEmpty()) continue;
            for (ItemStack extractedGem : extractedGems) {
                if (!foundFirstGem) {
                    foundFirstGem = true;
                    this.forceGemIntoInventory(player, extractedGem);
                    continue;
                }
                gemsToDrop.add(extractedGem);
            }
        }
        if (!gemsToDrop.isEmpty()) {
            for (ItemStack gemToDrop : gemsToDrop) {
                player.getWorld().dropItemNaturally(player.getLocation(), gemToDrop);
            }
            player.sendMessage("\u00a7c\u00a7lYou can only have one gem! Extra gems have been dropped.");
        }
    }

    private List<ItemStack> extractGemsFromBundle(ItemStack bundle, int slotIndex, PlayerInventory inv) {
        BundleMeta bundleMeta;
        ArrayList<ItemStack> extractedGems = new ArrayList<ItemStack>();
        if (bundle == null || bundle.getType() != Material.BUNDLE) {
            return extractedGems;
        }
        if (bundle.getItemMeta() instanceof BundleMeta && (bundleMeta = (BundleMeta)bundle.getItemMeta()).hasItems()) {
            ArrayList<ItemStack> remainingItems = new ArrayList<ItemStack>();
            for (ItemStack bundledItem : bundleMeta.getItems()) {
                if (bundledItem == null) continue;
                String itemId = CustomItemManager.getIdByItem(bundledItem);
                if (itemId != null && GemType.isGem(itemId)) {
                    extractedGems.add(bundledItem.clone());
                    continue;
                }
                remainingItems.add(bundledItem);
            }
            if (!extractedGems.isEmpty()) {
                bundleMeta.setItems(remainingItems);
                bundle.setItemMeta((ItemMeta)bundleMeta);
                inv.setItem(slotIndex, bundle);
            }
        }
        return extractedGems;
    }

    private void forceGemIntoInventory(Player player, ItemStack gem) {
        String slotId;
        ItemStack slotItem;
        int i;
        PlayerInventory inv = player.getInventory();
        int emptySlot = inv.firstEmpty();
        if (emptySlot != -1) {
            inv.setItem(emptySlot, gem);
            return;
        }
        for (i = 9; i < 36; ++i) {
            slotItem = inv.getItem(i);
            if (slotItem == null || slotItem.getType() == Material.AIR || (slotId = CustomItemManager.getIdByItem(slotItem)) != null && GemType.isGem(slotId)) continue;
            ItemStack replacedItem = slotItem.clone();
            inv.setItem(i, gem);
            player.getWorld().dropItemNaturally(player.getLocation(), replacedItem);
            String msg = this.plugin.getConfigManager().getFormattedMessage("gem-forced-into-inventory", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        for (i = 0; i < 9; ++i) {
            slotItem = inv.getItem(i);
            if (slotItem == null || slotItem.getType() == Material.AIR || (slotId = CustomItemManager.getIdByItem(slotItem)) != null && GemType.isGem(slotId)) continue;
            ItemStack replacedItem = slotItem.clone();
            inv.setItem(i, gem);
            player.getWorld().dropItemNaturally(player.getLocation(), replacedItem);
            String msg = this.plugin.getConfigManager().getFormattedMessage("gem-forced-into-inventory", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        inv.setItem(0, gem);
        String msg = this.plugin.getConfigManager().getFormattedMessage("gem-forced-into-hotbar", new Object[0]);
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    public void triggerAbilityViaCommand(Player player, boolean secondary) {
        GemAbilityHandler handler;
        boolean unlocksAllAtTier1;
        String gemId;
        int energy;
        boolean isGemItem;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        String oraxenId = CustomItemManager.getIdByItem(mainHand);
        boolean bl = isGemItem = oraxenId != null && (GemType.isGem(oraxenId) || this.plugin.getGemRegistry() != null && this.plugin.getGemRegistry().isRegisteredGem(oraxenId));
        if (!isGemItem) {
            oraxenId = CustomItemManager.getIdByItem(offHand);
            boolean bl2 = isGemItem = oraxenId != null && (GemType.isGem(oraxenId) || this.plugin.getGemRegistry() != null && this.plugin.getGemRegistry().isRegisteredGem(oraxenId));
            if (!isGemItem) {
                player.sendMessage("\u00a7c\u00a7lYou must be holding a gem to use this command!");
                return;
            }
        }
        if ((energy = this.plugin.getEnergyManager().getEnergy(player)) <= 0) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        String string = gemId = registry != null ? registry.gemIdFromItemId(oraxenId) : null;
        int tier = registry != null ? registry.tierFromItemId(oraxenId) : (oraxenId.endsWith("_gem_t2") ? 2 : 1);
        GemDefinition def = gemId != null && registry != null ? registry.getGem(gemId) : null;
        boolean bl3 = unlocksAllAtTier1 = def != null && def.getMaxTier() < 2;
        if (secondary && tier < 2 && !unlocksAllAtTier1) {
            player.sendMessage("\u00a7c\u00a7lSecondary abilities require Tier 2 gem!");
            return;
        }
        GemType gemType = GemType.fromOraxenId(oraxenId);
        if (gemType != null) {
            boolean wasSneaking = player.isSneaking();
            if (secondary && !wasSneaking) {
                player.sendMessage("\u00a7e\u00a7oShift + use ability or right-click for secondary ability!");
                return;
            }
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
            return;
        }
        if (gemId != null && registry != null && (handler = registry.getAbilityHandler(gemId)) != null) {
            if (secondary) {
                handler.onSecondary(player, tier);
            } else {
                handler.onPrimary(player, tier);
            }
        }
    }
}

