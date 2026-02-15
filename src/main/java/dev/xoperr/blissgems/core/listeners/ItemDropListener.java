package dev.xoperr.blissgems.core.listeners;

import dev.xoperr.blissgems.core.managers.ProtectionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents players from dropping protected gems.
 */
public class ItemDropListener implements Listener {

    private final ProtectionManager protectionManager;

    public ItemDropListener(ProtectionManager protectionManager) {
        this.protectionManager = protectionManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        if (protectionManager.isGem(droppedItem)) {
            event.setCancelled(true);
            // Optional: Send message to player
            // player.sendMessage("§cYou cannot drop gems!");
        }
    }
}
