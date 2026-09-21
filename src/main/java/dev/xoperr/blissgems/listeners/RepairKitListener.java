/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.entity.ItemSpawnEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
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

public class RepairKitListener
implements Listener {
    private final BlissGems plugin;

    public RepairKitListener(BlissGems plugin) {
        this.plugin = plugin;
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
        String itemId = CustomItemManager.getIdByItem(item);
        if (itemId == null || !itemId.equals("repair_kit")) {
            return;
        }
        event.setCancelled(true);
        player.sendMessage("\u00a7d\u00a7oTo use the Repair Kit, drop it on top of a Beacon to create a Pedestal!");
        player.sendMessage("\u00a7d\u00a7oThe Pedestal will restore energy to all nearby players.");
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        final Item droppedItem = event.getItemDrop();
        final ItemStack item = droppedItem.getItemStack();
        final Player player = event.getPlayer();
        String itemId = CustomItemManager.getIdByItem(item);
        if (itemId == null || !itemId.equals("repair_kit")) {
            return;
        }
        new BukkitRunnable(){

            public void run() {
                if (!droppedItem.isValid() || droppedItem.isDead()) {
                    return;
                }
                Location itemLoc = droppedItem.getLocation();
                Block blockBelow = itemLoc.subtract(0.0, 1.0, 0.0).getBlock();
                if (blockBelow.getType() == Material.BEACON) {
                    if (RepairKitListener.this.plugin.getSpawnBeaconManager() != null && !RepairKitListener.this.plugin.getSpawnBeaconManager().canRepairGems(player)) {
                        return;
                    }
                    droppedItem.remove();
                    boolean success = RepairKitListener.this.plugin.getRepairKitManager().createPedestal(blockBelow.getLocation());
                    if (success) {
                        player.sendMessage("\u00a7d\u00a7l\u2726 \u00a7d\u00a7oPedestal created! It will restore energy to nearby players.");
                        for (Entity entity : itemLoc.getWorld().getNearbyEntities(itemLoc, 15.0, 15.0, 15.0)) {
                            if (!(entity instanceof Player) || entity == player) continue;
                            Player nearbyPlayer = (Player)entity;
                            nearbyPlayer.sendMessage("\u00a7d\u00a7o" + player.getName() + " created a Repair Kit Pedestal nearby!");
                        }
                        RepairKitListener.this.broadcastRepairRitual(player);
                    } else {
                        player.getInventory().addItem(new ItemStack[]{item});
                        player.sendMessage("\u00a7c\u00a7oCouldn't create pedestal at this location!");
                    }
                }
            }
        }.runTaskLater((Plugin)this.plugin, 20L);
    }

    private void broadcastRepairRitual(Player player) {
        if (!this.plugin.getConfig().getBoolean("repair-kit.broadcast", true)) {
            return;
        }
        this.plugin.getServer().broadcastMessage("\u00a7d\u00a7l\u2726 \u00a7d" + player.getName() + " \u00a77has begun a \u00a7d\u00a7lRepair Ritual\u00a77!");
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        final Item item = event.getEntity();
        ItemStack itemStack = item.getItemStack();
        String itemId = CustomItemManager.getIdByItem(itemStack);
        if (itemId == null || !itemId.equals("repair_kit")) {
            return;
        }
        new BukkitRunnable(){

            public void run() {
                if (!item.isValid() || item.isDead()) {
                    return;
                }
                Location itemLoc = item.getLocation();
                Block blockBelow = itemLoc.clone().subtract(0.0, 1.0, 0.0).getBlock();
                if (blockBelow.getType() == Material.BEACON) {
                    item.remove();
                    boolean success = RepairKitListener.this.plugin.getRepairKitManager().createPedestal(blockBelow.getLocation());
                    if (success) {
                        for (Entity entity : itemLoc.getWorld().getNearbyEntities(itemLoc, 15.0, 15.0, 15.0)) {
                            if (!(entity instanceof Player)) continue;
                            Player player = (Player)entity;
                            player.sendMessage("\u00a7d\u00a7l\u2726 \u00a7d\u00a7oRepair Kit Pedestal activated!");
                        }
                    }
                }
            }
        }.runTaskLater((Plugin)this.plugin, 40L);
    }
}

