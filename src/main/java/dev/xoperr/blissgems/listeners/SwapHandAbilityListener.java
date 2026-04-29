package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemRegistry;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Routes the swap-offhand key (default F) to gem tertiary/quaternary abilities:
 *   F       → tertiary
 *   Shift+F → quaternary
 * The swap itself is always cancelled when the player is holding a gem in
 * either hand, so the key is dedicated to ability activation.
 */
public class SwapHandAbilityListener implements Listener {

    private final BlissGems plugin;

    public SwapHandAbilityListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();
        boolean mainIsGem = isGem(mainHand);
        boolean offIsGem = isGem(offHand);
        if (!mainIsGem && !offIsGem) {
            return;
        }

        event.setCancelled(true);

        if (plugin.getBlissCommand() == null) {
            return;
        }

        // Respect toggle_click setting — block abilities but still cancel the swap
        if (!plugin.getClickActivationManager().isClickActivationEnabled(player)) {
            return;
        }

        if (player.isSneaking()) {
            plugin.getBlissCommand().triggerQuaternary(player);
        } else {
            plugin.getBlissCommand().triggerTertiary(player);
        }
    }

    private boolean isGem(ItemStack item) {
        if (item == null) return false;
        String id = CustomItemManager.getIdByItem(item);
        if (id == null) return false;
        if (GemType.isGem(id)) return true;
        GemRegistry registry = plugin.getGemRegistry();
        return registry != null && registry.isRegisteredGem(id);
    }
}
