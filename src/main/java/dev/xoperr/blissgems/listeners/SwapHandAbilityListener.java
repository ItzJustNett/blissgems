/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerSwapHandItemsEvent
 *  org.bukkit.inventory.ItemStack
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.managers.GemRegistryImpl;
import dev.xoperr.blissgems.utils.AbilityBinding;
import dev.xoperr.blissgems.utils.AbilitySlot;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class SwapHandAbilityListener
implements Listener {
    private final BlissGems plugin;

    public SwapHandAbilityListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();
        boolean mainIsGem = this.isGem(mainHand);
        boolean offIsGem = this.isGem(offHand);
        // Binding Vow: F with nothing in either hand is the gem's swap input
        boolean vow = this.plugin.getBindingVows() != null && this.plugin.getBindingVows().firesFrom(player, mainHand) && (offHand == null || offHand.getType().isAir());
        if (!mainIsGem && !offIsGem && !vow) {
            return;
        }
        if (this.plugin.getBlissCommand() == null) {
            return;
        }
        AbilityBinding input = AbilityBinding.swapHand(player.isSneaking());
        AbilitySlot slot = this.plugin.getAbilityBindingManager() != null ? this.plugin.getAbilityBindingManager().getSlot(player, input) : null;
        if (vow && slot == null) {
            // the client never reports an empty-hand right click on air, so F stands in for it
            slot = this.plugin.getAbilityBindingManager() != null ? this.plugin.getAbilityBindingManager().getSlot(player, AbilityBinding.rightClick(player.isSneaking())) : null;
        } else if (!this.plugin.getClickActivationManager().isClickActivationEnabled(player)) {
            // F is a normal swap key for gems unless the player bound an ability to it in /bliss ability
            return;
        }
        if (slot == null) {
            return;
        }
        event.setCancelled(true);
        // The event reports the hands after the swap, so the gem's current hand is the opposite one
        if (this.plugin.getConfig().getBoolean("abilities.require-main-hand", false) && !offIsGem && !vow) {
            return;
        }
        this.plugin.getBlissCommand().triggerSlot(player, slot);
    }

    private boolean isGem(ItemStack item) {
        if (item == null) {
            return false;
        }
        String id = CustomItemManager.getIdByItem(item);
        if (id == null) {
            return false;
        }
        if (GemType.isGem(id)) {
            return true;
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        return registry != null && registry.isRegisteredGem(id);
    }
}

