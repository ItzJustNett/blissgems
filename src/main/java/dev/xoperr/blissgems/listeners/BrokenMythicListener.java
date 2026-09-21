package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.managers.MythicWorldEventManager;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.ItemStack;

public class BrokenMythicListener implements Listener {
    private final BlissGems plugin;
    private final MythicWorldEventManager eventManager;

    public BrokenMythicListener(BlissGems plugin, MythicWorldEventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    private boolean isTrackedMythic(ItemStack item) {
        if (item == null) return false;
        if (CustomItemManager.isMythic(item) || CustomItemManager.isDoubleDurability(item)) {
            return true;
        }
        String id = CustomItemManager.getIdByItem(item);
        if (id != null && (id.contains("netherite_") || id.startsWith("mace_") || id.equals("prismatic_edge"))) {
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemBreak(PlayerItemBreakEvent event) {
        ItemStack broken = event.getBrokenItem();
        if (isTrackedMythic(broken)) {
            eventManager.handleMythicBreak(broken, event.getPlayer(), "Item Shattered");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item)) return;
        Item itemEntity = (Item) event.getEntity();
        ItemStack stack = itemEntity.getItemStack();

        if (isTrackedMythic(stack)) {
            // Check if damage will destroy the entity
            if (itemEntity.getHealth() - event.getFinalDamage() <= 0) {
                eventManager.handleMythicBreak(stack, null, "Item Destroyed: " + event.getCause().name());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        ItemStack stack = event.getEntity().getItemStack();
        if (isTrackedMythic(stack)) {
            eventManager.handleMythicBreak(stack, null, "Item Despawned");
        }
    }
}
