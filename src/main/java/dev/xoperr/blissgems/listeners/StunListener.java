package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.abilities.FluxAbilities;
import dev.xoperr.blissgems.abilities.SpeedAbilities;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Handles stun/freeze restrictions for:
 * - Flux Gem Ground ability (stun)
 * - Speed Gem Speed Storm ability (freeze)
 */
public class StunListener implements Listener {
    private final BlissGems plugin;

    public StunListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if player is stunned (Flux) or frozen (Speed Storm)
     */
    private boolean isImmobilized(UUID playerId) {
        return FluxAbilities.isPlayerStunned(playerId) || SpeedAbilities.isPlayerFrozen(playerId);
    }

    /**
     * Check if player is frozen (Speed Storm or Flux Ground — same restrictions)
     */
    private boolean isFrozen(UUID playerId) {
        return SpeedAbilities.isPlayerFrozen(playerId) || FluxAbilities.isPlayerStunned(playerId);
    }

    // ========================================================================
    // Movement Blocking (Speed Storm freeze only - stricter)
    // ========================================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Only block movement for Speed Storm freeze (not Flux stun)
        if (isFrozen(player.getUniqueId())) {
            Location from = event.getFrom();
            Location to = event.getTo();

            // Allow head rotation but block position change
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                // Keep the same position but allow looking around
                Location stayLoc = from.clone();
                stayLoc.setYaw(to.getYaw());
                stayLoc.setPitch(to.getPitch());
                event.setTo(stayLoc);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        if (isImmobilized(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // ========================================================================
    // Item Usage Blocking
    // ========================================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) {
            return;
        }

        UUID playerId = player.getUniqueId();

        // Check if player is immobilized (stunned or frozen)
        if (isImmobilized(playerId)) {
            Material type = item.getType();

            // Block ender pearls and chorus fruit completely
            if (type == Material.ENDER_PEARL || type == Material.CHORUS_FRUIT) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getFormattedMessage(
                    "cannot-use-while-stunned", "item",
                    type == Material.ENDER_PEARL ? "ender pearls" : "chorus fruit"));
                return;
            }

            // For frozen players (Speed Storm), block ALL interactions except gapples
            if (isFrozen(playerId)) {
                // Allow only golden apples
                if (type != Material.GOLDEN_APPLE && type != Material.ENCHANTED_GOLDEN_APPLE) {
                    event.setCancelled(true);
                    if (item.getType().isEdible() ||
                        type == Material.POTION ||
                        type == Material.SPLASH_POTION ||
                        type == Material.LINGERING_POTION) {
                        player.sendMessage(plugin.getConfigManager().getMessage("can-only-eat-golden-apple-stunned"));
                    }
                    return;
                }
            } else {
                // For stunned players (Flux), only block non-gapple consumables
                if (type != Material.GOLDEN_APPLE && type != Material.ENCHANTED_GOLDEN_APPLE) {
                    if (item.getType().isEdible() ||
                        type == Material.POTION ||
                        type == Material.SPLASH_POTION ||
                        type == Material.LINGERING_POTION) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.getConfigManager().getMessage("can-only-eat-golden-apple-stunned"));
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (isImmobilized(player.getUniqueId())) {
            Material type = item.getType();

            // Only allow golden apples
            if (type != Material.GOLDEN_APPLE && type != Material.ENCHANTED_GOLDEN_APPLE) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getMessage("can-only-eat-golden-apple-stunned"));
            }
        }
    }

    // ========================================================================
    // Projectile Blocking (pearls, snowballs, etc.)
    // ========================================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player player) {
            if (isImmobilized(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    // ========================================================================
    // Inventory Blocking (Speed Storm freeze only)
    // ========================================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            // Only block inventory for frozen players (Speed Storm)
            if (isFrozen(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage("§c§lYou cannot open inventories while frozen!");
            }
        }
    }

    // ========================================================================
    // Item Drop Blocking (Speed Storm freeze only)
    // ========================================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // ========================================================================
    // Combat Blocking (Speed Storm freeze only)
    // ========================================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Block frozen players from dealing damage
        if (event.getDamager() instanceof Player player) {
            if (isFrozen(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage("§c§lYou cannot attack while frozen!");
            }
        }
    }

    // ========================================================================
    // Block Interaction Blocking (Speed Storm freeze only)
    // ========================================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
