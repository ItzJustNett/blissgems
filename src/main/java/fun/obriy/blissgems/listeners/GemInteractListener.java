/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  io.th0rgal.oraxen.api.OraxenItems
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
package fun.obriy.blissgems.listeners;

import fun.obriy.blissgems.BlissGems;
import fun.obriy.blissgems.utils.GemType;
import io.th0rgal.oraxen.api.OraxenItems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
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
        String oraxenId = OraxenItems.getIdByItem((ItemStack)item);
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
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        if (energy <= 0) {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-energy", new Object[0]));
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
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("energy-added", "amount", 1));
    }

    private void handleGemTrader(Player player, ItemStack traderItem, PlayerInteractEvent event) {
        long timeLeft;
        event.setCancelled(true);
        long now = System.currentTimeMillis();
        Long lastUse = this.traderCooldowns.get(player.getUniqueId());
        int cooldownSeconds = this.plugin.getConfigManager().getTraderCooldown();
        if (lastUse != null && (timeLeft = lastUse + (long)cooldownSeconds * 1000L - now) > 0L) {
            int secondsLeft = (int)Math.ceil((double)timeLeft / 1000.0);
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("trade-cooldown", "seconds", secondsLeft));
            return;
        }
        if (!this.plugin.getGemManager().hasActiveGem(player)) {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-gem", new Object[0]));
            return;
        }
        GemType currentType = this.plugin.getGemManager().getGemType(player);
        if (currentType == null) {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-gem", new Object[0]));
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
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("trade-success", "gem", newType.getDisplayName()));
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

            // Check if trying to add a gem when already have one
            if (cursorItem != null) {
                String cursorOraxenId = OraxenItems.getIdByItem((ItemStack)cursorItem);
                if (cursorOraxenId != null && GemType.isGem(cursorOraxenId)) {
                    // Player is trying to place a gem
                    int currentGemCount = countGemsInInventory(player);
                    // If they already have a gem and this isn't just moving their existing gem
                    if (currentGemCount > 0) {
                        // Check if they're moving their own gem (not adding a new one)
                        boolean isMovingOwnGem = false;
                        if (clickedItem != null) {
                            String clickedOraxenId = OraxenItems.getIdByItem((ItemStack)clickedItem);
                            if (clickedOraxenId != null && GemType.isGem(clickedOraxenId)) {
                                isMovingOwnGem = true;
                            }
                        }

                        if (!isMovingOwnGem && event.getClickedInventory() == player.getInventory()) {
                            event.setCancelled(true);
                            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("already-have-gem", new Object[0]));
                            return;
                        }
                    }
                }
            }

            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.plugin.getGemManager().updateActiveGem(player), 1L);
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player)event.getEntity();
        Item itemEntity = event.getItem();
        ItemStack item = itemEntity.getItemStack();

        String oraxenId = OraxenItems.getIdByItem((ItemStack)item);
        if (oraxenId == null || !GemType.isGem(oraxenId)) {
            return;
        }

        // Check if player already has a gem
        int currentGemCount = countGemsInInventory(player);
        if (currentGemCount > 0) {
            event.setCancelled(true);
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("already-have-gem", new Object[0]));
        }
    }

    private int countGemsInInventory(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            String oraxenId = OraxenItems.getIdByItem((ItemStack)item);
            if (oraxenId != null && GemType.isGem(oraxenId)) {
                count++;
            }
        }
        return count;
    }
}

