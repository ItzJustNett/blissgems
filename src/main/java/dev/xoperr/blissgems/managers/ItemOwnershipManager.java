package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Tracks item ownership via PersistentDataContainer.
 * Used by Strength Gem's Shadow Stalker ability to determine
 * who crafted/picked up an item for player tracking.
 */
public class ItemOwnershipManager implements Listener {
    private final BlissGems plugin;
    private final NamespacedKey OWNERSHIP_KEY;

    public ItemOwnershipManager(BlissGems plugin) {
        this.plugin = plugin;
        this.OWNERSHIP_KEY = new NamespacedKey(plugin, "item_owner");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack result = event.getCurrentItem();
        if (result == null) return;

        setItemOwner(result, player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        Item itemEntity = event.getItem();
        ItemStack item = itemEntity.getItemStack();

        setItemOwner(item, player.getUniqueId());
        itemEntity.setItemStack(item);
    }

    /**
     * Get the owner UUID from an item's PDC.
     * Returns null if no ownership data exists.
     */
    public UUID getItemOwner(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (pdc.has(OWNERSHIP_KEY, PersistentDataType.STRING)) {
            String uuidStr = pdc.get(OWNERSHIP_KEY, PersistentDataType.STRING);
            try {
                return UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Set ownership on an item.
     */
    public void setItemOwner(ItemStack item, UUID owner) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(OWNERSHIP_KEY, PersistentDataType.STRING, owner.toString());
        item.setItemMeta(meta);
    }
}
