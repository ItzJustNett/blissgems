package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The Repair Kit only works through the pedestal ritual (see PedestalListener):
 * right-clicking it just explains how to use it.
 */
public class RepairKitListener
implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || !"repair_kit".equals(CustomItemManager.getIdByItem(item))) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage("§d§oThrow the Repair Kit onto the §d§lPedestal§d§o to begin the Repair Ritual.");
    }
}
