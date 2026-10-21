package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Gems are nautilus shells underneath (so Bedrock players can hold them off hand), and nautilus
 * shells craft conduits. A gem in a crafting grid never produces anything.
 */
public class GemCraftingGuard implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item == null) continue;
            String id = CustomItemManager.getIdByItem(item);
            if (id != null && (GemType.isGem(id) || id.startsWith("gold_gem"))) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }
}
