/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.EnergyState;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class RestorationBookListener
implements Listener {
    private static final String ITEM_ID = "restoration_book";
    private final BlissGems plugin;
    private final Set<UUID> activeRituals = new HashSet<UUID>();

    public RestorationBookListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRestorationBookUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || !ITEM_ID.equals(CustomItemManager.getIdByItem(item))) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (this.activeRituals.contains(player.getUniqueId())) {
            player.sendMessage("\u00a7c\u00a7oYour Restoration Ritual is already under way!");
            return;
        }
        if (this.plugin.getEnergyManager().getEnergyState(player) != EnergyState.BROKEN) {
            player.sendMessage("\u00a7c\u00a7oThe Restoration Book only works while your gem is \u00a7c\u00a7lBROKEN\u00a7c\u00a7o.");
            return;
        }
        List<String> available = this.plugin.getGemManager().getAvailableGemIds();
        if (available.isEmpty()) {
            player.sendMessage("\u00a7c\u00a7oNo gems are available to restore to!");
            return;
        }
        String newGem = available.get((int)(Math.random() * (double)available.size()));
        int tier = Math.max(1, this.plugin.getGemManager().getGemTier(player));
        item.setAmount(item.getAmount() - 1);
        this.activeRituals.add(player.getUniqueId());
        this.clearExistingGems(player);
        this.broadcastRitualStart(player);
        long grantDelay = this.plugin.getGemRitualManager().performRestorationRitual(player, newGem, tier);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            this.activeRituals.remove(player.getUniqueId());
            if (!player.isOnline()) {
                return;
            }
            this.plugin.getEnergyManager().setEnergy(player, EnergyState.PRISTINE.getMinEnergy());
            if (this.plugin.getGemManager().giveGemToOffhand(player, newGem, tier)) {
                String gemName = this.plugin.getGemManager().getGemDisplayName(newGem);
                String gemColor = this.plugin.getGemManager().getGemColorCode(newGem);
                player.sendMessage("\u00a75\u00a7l\u00bb \u00a7fYour gem has been reforged: " + gemColor + "\u00a7l" + gemName + " \u00a7f(Tier " + tier + ")\u00a75\u00a7l \u00ab");
                this.broadcastRitualComplete(player, gemColor + gemName);
            } else {
                player.sendMessage("\u00a7c\u00a7oThe ritual failed to reforge your gem, contact staff.");
            }
        }, grantDelay);
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onRitualDamage(EntityDamageEvent event) {
        Player player;
        Entity entity = event.getEntity();
        if (entity instanceof Player && this.activeRituals.contains((player = (Player)entity).getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRitualMove(PlayerMoveEvent event) {
        if (!this.activeRituals.contains(event.getPlayer().getUniqueId()) || event.getTo() == null) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }
        Location held = from.clone();
        held.setYaw(to.getYaw());
        held.setPitch(to.getPitch());
        event.setTo(held);
    }

    private void clearExistingGems(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); ++i) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot == null || !this.plugin.getGemManager().isAnyGem(CustomItemManager.getIdByItem(slot))) continue;
            player.getInventory().setItem(i, null);
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (this.plugin.getGemManager().isAnyGem(CustomItemManager.getIdByItem(offhand))) {
            player.getInventory().setItemInOffHand(null);
        }
    }

    private void broadcastRitualStart(Player player) {
        if (!this.plugin.getConfig().getBoolean("restoration.broadcast", true)) {
            return;
        }
        this.plugin.getServer().broadcastMessage("\u00a75\u00a7l\u2726 \u00a7d" + player.getName() + " \u00a77has begun a \u00a75\u00a7lRestoration Ritual\u00a77! The sky darkens...");
    }

    private void broadcastRitualComplete(Player player, String gemDisplay) {
        if (!this.plugin.getConfig().getBoolean("restoration.broadcast", true)) {
            return;
        }
        this.plugin.getServer().broadcastMessage("\u00a75\u00a7l\u2726 \u00a7d" + player.getName() + "\u00a77's gem was reforged as " + gemDisplay + " \u00a77at \u00a7bPristine\u00a77.");
    }
}

