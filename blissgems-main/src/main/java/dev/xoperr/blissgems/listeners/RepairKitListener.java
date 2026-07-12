package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles Repair Kit item interactions and pedestal creation
 */
public class RepairKitListener implements Listener {
    private final BlissGems plugin;

    public RepairKitListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles repair kit being used (right-click)
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) {
            return;
        }

        String itemId = CustomItemManager.getIdByItem(item);
        if (itemId == null || !itemId.equals("repair_kit")) {
            return;
        }

        event.setCancelled(true);

        player.sendMessage("§d§oTo use the Repair Kit, drop it on top of a Beacon to create a Pedestal!");
        player.sendMessage("§d§oThe Pedestal will restore energy to all nearby players.");
    }

    /**
     * Handles repair kit being dropped
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Item droppedItem = event.getItemDrop();
        ItemStack item = droppedItem.getItemStack();
        Player player = event.getPlayer();

        String itemId = CustomItemManager.getIdByItem(item);
        if (itemId == null || !itemId.equals("repair_kit")) {
            return;
        }

        // Check in 1 second if the item landed on a beacon
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!droppedItem.isValid() || droppedItem.isDead()) {
                    return;
                }

                Location itemLoc = droppedItem.getLocation();
                Block blockBelow = itemLoc.subtract(0, 1, 0).getBlock();

                // Check if dropped on a beacon
                if (blockBelow.getType() == Material.BEACON) {
                    // Remove the dropped item
                    droppedItem.remove();

                    // Create pedestal
                    boolean success = plugin.getRepairKitManager().createPedestal(blockBelow.getLocation());

                    if (success) {
                        player.sendMessage("§d§l✦ §d§oPedestal created! It will restore energy to nearby players.");

                        // Notify nearby players
                        for (org.bukkit.entity.Entity entity : itemLoc.getWorld().getNearbyEntities(itemLoc, 15, 15, 15)) {
                            if (entity instanceof Player && entity != player) {
                                Player nearbyPlayer = (Player) entity;
                                nearbyPlayer.sendMessage("§d§o" + player.getName() + " created a Repair Kit Pedestal nearby!");
                            }
                        }
                    } else {
                        // Give item back if failed
                        player.getInventory().addItem(item);
                        player.sendMessage("§c§oCouldn't create pedestal at this location!");
                    }
                }
            }
        }.runTaskLater((Plugin) this.plugin, 20L); // Check after 1 second
    }

    /**
     * Alternative method: detect when repair kit item spawns near beacon
     */
    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        ItemStack itemStack = item.getItemStack();

        String itemId = CustomItemManager.getIdByItem(itemStack);
        if (itemId == null || !itemId.equals("repair_kit")) {
            return;
        }

        // Check in 2 seconds if near a beacon
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!item.isValid() || item.isDead()) {
                    return;
                }

                Location itemLoc = item.getLocation();

                // Check 1 block below
                Block blockBelow = itemLoc.clone().subtract(0, 1, 0).getBlock();

                if (blockBelow.getType() == Material.BEACON) {
                    // Remove item
                    item.remove();

                    // Create pedestal
                    boolean success = plugin.getRepairKitManager().createPedestal(blockBelow.getLocation());

                    if (success) {
                        // Notify nearby players
                        for (org.bukkit.entity.Entity entity : itemLoc.getWorld().getNearbyEntities(itemLoc, 15, 15, 15)) {
                            if (entity instanceof Player) {
                                Player player = (Player) entity;
                                player.sendMessage("§d§l✦ §d§oRepair Kit Pedestal activated!");
                            }
                        }
                    }
                }
            }
        }.runTaskLater((Plugin) this.plugin, 40L); // Check after 2 seconds
    }
}
