package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * Stamps a hidden owner UUID onto UNSTACKABLE items, possession-based: whoever first
 * holds an unstackable item becomes its owner.
 *
 * Implemented as a lightweight periodic sweep of every online player's inventory so it
 * covers <em>all</em> acquisition paths uniformly — crafting, villager trades, chest
 * loot, fishing, mob drops, {@code /give}, creative, etc. Only items with a max stack
 * size of 1 are ever stamped, and an existing owner is never overwritten, so a stolen
 * item keeps pointing at whoever first owned it.
 *
 * Shadow Stalker consumes such an item to track its owner.
 */
public class ItemOwnershipListener implements Listener {
    private static final long SWEEP_INTERVAL_TICKS = 40L; // 2 seconds

    private final BlissGems plugin;
    private BukkitTask sweepTask;

    public ItemOwnershipListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    /** Start the periodic possession sweep. Call once from onEnable after registering. */
    public void start() {
        if (sweepTask != null) {
            return;
        }
        sweepTask = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
            for (Player player : this.plugin.getServer().getOnlinePlayers()) {
                stampInventory(player);
            }
        }, SWEEP_INTERVAL_TICKS, SWEEP_INTERVAL_TICKS);
    }

    public void stop() {
        if (sweepTask != null) {
            sweepTask.cancel();
            sweepTask = null;
        }
    }

    /** Stamp every unstamped unstackable item the player currently holds (storage, armor, offhand). */
    private void stampInventory(Player player) {
        UUID owner = player.getUniqueId();
        PlayerInventory inv = player.getInventory();

        ItemStack[] storage = inv.getStorageContents();
        boolean storageChanged = false;
        for (ItemStack item : storage) {
            if (stampIfEligible(item, owner)) {
                storageChanged = true;
            }
        }
        if (storageChanged) {
            inv.setStorageContents(storage);
        }

        ItemStack[] armor = inv.getArmorContents();
        boolean armorChanged = false;
        for (ItemStack item : armor) {
            if (stampIfEligible(item, owner)) {
                armorChanged = true;
            }
        }
        if (armorChanged) {
            inv.setArmorContents(armor);
        }

        ItemStack offhand = inv.getItemInOffHand();
        if (stampIfEligible(offhand, owner)) {
            inv.setItemInOffHand(offhand);
        }
    }

    private boolean stampIfEligible(ItemStack item, UUID owner) {
        if (item == null || item.getType() == Material.AIR || item.getMaxStackSize() != 1) {
            return false; // only unstackable items get an owner stamp
        }
        return CustomItemManager.setOwner(item, owner);
    }
}
