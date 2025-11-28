package fun.obriy.blissgems.listeners;

import fun.obriy.blissgems.BlissGems;
import fun.obriy.blissgems.abilities.FluxAbilities;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class StunListener implements Listener {
    private final BlissGems plugin;

    public StunListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) {
            return;
        }

        // Check if player is stunned
        if (FluxAbilities.isPlayerStunned(player.getUniqueId())) {
            Material type = item.getType();

            // Block ender pearls and chorus fruit
            if (type == Material.ENDER_PEARL || type == Material.CHORUS_FRUIT) {
                event.setCancelled(true);
                player.sendMessage("§c§lYou cannot use " +
                    (type == Material.ENDER_PEARL ? "ender pearls" : "chorus fruit") +
                    " while stunned!");
                return;
            }

            // Allow only golden apples and enchanted golden apples (gapples)
            if (type != Material.GOLDEN_APPLE && type != Material.ENCHANTED_GOLDEN_APPLE) {
                // Check if it's any consumable item
                if (item.getType().isEdible() ||
                    type == Material.POTION ||
                    type == Material.SPLASH_POTION ||
                    type == Material.LINGERING_POTION) {
                    event.setCancelled(true);
                    player.sendMessage("§c§lYou can only eat golden apples while stunned!");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (FluxAbilities.isPlayerStunned(player.getUniqueId())) {
            Material type = item.getType();

            // Only allow golden apples
            if (type != Material.GOLDEN_APPLE && type != Material.ENCHANTED_GOLDEN_APPLE) {
                event.setCancelled(true);
                player.sendMessage("§c§lYou can only eat golden apples while stunned!");
            }
        }
    }
}
