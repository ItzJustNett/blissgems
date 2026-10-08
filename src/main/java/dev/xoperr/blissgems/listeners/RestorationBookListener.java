package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The Restoration Book only works through the pedestal ritual (see PedestalListener):
 * right-clicking it just explains how to use it.
 */
public class RestorationBookListener
implements Listener {
    private static final String ITEM_ID = "restoration_book";

    @EventHandler
    public void onRestorationBookUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || !ITEM_ID.equals(CustomItemManager.getIdByItem(item))) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage("§5§oThrow the Restoration Book onto the §d§lPedestal§5§o while your gem is §c§lBROKEN§5§o to begin the Restoration Ritual.");
    }
}
