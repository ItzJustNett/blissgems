package dev.xoperr.blissgems.core.listeners;

import dev.xoperr.blissgems.core.managers.ProtectionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Prevents players from moving protected gems into restricted inventories
 * (chests, hoppers, furnaces, grindstones, etc.)
 */
public class InventoryInteractListener implements Listener {

    private final ProtectionManager protectionManager;
    private final Set<InventoryType> restrictedInventories;
    private final Set<InventoryType> specialToolInventories;

    public InventoryInteractListener(ProtectionManager protectionManager) {
        this.protectionManager = protectionManager;

        // Inventories where gems cannot be stored
        this.restrictedInventories = new HashSet<>(Arrays.asList(
                InventoryType.CHEST,
                InventoryType.DISPENSER,
                InventoryType.DROPPER,
                InventoryType.HOPPER,
                InventoryType.FURNACE,
                InventoryType.BREWING,
                InventoryType.BARREL,
                InventoryType.SHULKER_BOX,
                InventoryType.BLAST_FURNACE,
                InventoryType.SMOKER,
                InventoryType.LECTERN,
                InventoryType.STONECUTTER,
                InventoryType.COMPOSTER,
                InventoryType.CHISELED_BOOKSHELF
        ));

        // Special tool inventories where gems cannot be used
        this.specialToolInventories = new HashSet<>(Arrays.asList(
                InventoryType.GRINDSTONE,
                InventoryType.ENCHANTING,
                InventoryType.ANVIL,
                InventoryType.SMITHING,
                InventoryType.MERCHANT,
                InventoryType.LOOM,
                InventoryType.CARTOGRAPHY
        ));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        // Check if player is trying to place a gem from cursor into restricted inventory
        if (event.getClickedInventory() != null &&
            event.getClickedInventory().getType() != InventoryType.PLAYER &&
            cursorItem != null &&
            !cursorItem.getType().isAir()) {

            if (protectionManager.isGem(cursorItem)) {
                if (restrictedInventories.contains(event.getClickedInventory().getType()) ||
                    specialToolInventories.contains(event.getClickedInventory().getType())) {
                    event.setCancelled(true);
                    // Optional: Send message
                    // player.sendMessage("§cYou cannot move gems to this inventory!");
                    return;
                }
            }
        }

        // Check if player is shift-clicking a gem to move it to another inventory
        if (event.getClickedInventory() != null &&
            event.isShiftClick() &&
            event.getClickedInventory().getType() == InventoryType.PLAYER &&
            clickedItem != null &&
            !clickedItem.getType().isAir() &&
            event.getView().getTopInventory().getType() != InventoryType.CRAFTING) {

            if (protectionManager.isGem(clickedItem)) {
                if (restrictedInventories.contains(event.getView().getTopInventory().getType()) ||
                    specialToolInventories.contains(event.getView().getTopInventory().getType())) {
                    event.setCancelled(true);
                    // Optional: Send message
                    // player.sendMessage("§cYou cannot move gems to this inventory!");
                }
            }
        }
    }
}
