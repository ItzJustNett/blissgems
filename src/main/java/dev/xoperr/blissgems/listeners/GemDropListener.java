package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents dropping of gems using PDC-based checking
 * Uses the exact same approach as DropItemControl's ItemDropListener
 */
public class GemDropListener implements Listener {
    private final BlissGems plugin;

    public GemDropListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        // Check if item is marked as undroppable via PDC (same as DropItemControl's isItemLocked)
        boolean isUndroppable = CustomItemManager.isUndroppable(droppedItem);

        if (isUndroppable) {
            event.setCancelled(true);

            // Send message to player
            String message = plugin.getConfigManager().getFormattedMessage("cannot-drop-gem");
            if (message != null && !message.isEmpty()) {
                player.sendMessage(message);
            } else {
                // Fallback message if config not set
                player.sendMessage("§c§lYou cannot drop your gem!");
            }
        }
    }
}
