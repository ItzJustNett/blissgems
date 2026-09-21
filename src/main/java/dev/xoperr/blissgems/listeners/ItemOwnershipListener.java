/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class ItemOwnershipListener
implements Listener {
    private static final long SWEEP_INTERVAL_TICKS = 40L;
    private final BlissGems plugin;
    private BukkitTask sweepTask;

    public ItemOwnershipListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (this.sweepTask != null) {
            return;
        }
        this.sweepTask = this.plugin.getServer().getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            for (Player player : this.plugin.getServer().getOnlinePlayers()) {
                this.stampInventory(player);
            }
        }, 40L, 40L);
    }

    public void stop() {
        if (this.sweepTask != null) {
            this.sweepTask.cancel();
            this.sweepTask = null;
        }
    }

    private void stampInventory(Player player) {
        ItemStack offhand;
        UUID owner = player.getUniqueId();
        PlayerInventory inv = player.getInventory();
        ItemStack[] storage = inv.getStorageContents();
        boolean storageChanged = false;
        for (ItemStack item : storage) {
            if (!this.stampIfEligible(item, owner)) continue;
            storageChanged = true;
        }
        if (storageChanged) {
            inv.setStorageContents(storage);
        }
        ItemStack[] armor = inv.getArmorContents();
        boolean armorChanged = false;
        for (ItemStack item : armor) {
            if (!this.stampIfEligible(item, owner)) continue;
            armorChanged = true;
        }
        if (armorChanged) {
            inv.setArmorContents(armor);
        }
        if (this.stampIfEligible(offhand = inv.getItemInOffHand(), owner)) {
            inv.setItemInOffHand(offhand);
        }
    }

    private boolean stampIfEligible(ItemStack item, UUID owner) {
        if (item == null || item.getType() == Material.AIR || item.getMaxStackSize() != 1) {
            return false;
        }
        return CustomItemManager.setOwner(item, owner);
    }

    public static void setOwnerLore(ItemStack item, String ownerName) {
        if (item == null || ownerName == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        java.util.List<String> lore = meta.getLore() != null ? new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<>();
        String prefix = "§7Owner: §e";
        boolean found = false;
        for (int i = 0; i < lore.size(); i++) {
            String stripped = org.bukkit.ChatColor.stripColor(lore.get(i));
            if (stripped != null && stripped.startsWith("Owner: ")) {
                lore.set(i, prefix + ownerName);
                found = true;
                break;
            }
        }
        if (!found) {
            lore.add(prefix + ownerName);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}
