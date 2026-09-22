/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.ProjectileLaunchEvent
 *  org.bukkit.event.inventory.InventoryOpenEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerItemConsumeEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerToggleFlightEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.projectiles.ProjectileSource
 *  org.bukkit.util.Vector
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.abilities.FluxAbilities;
import dev.xoperr.blissgems.abilities.SpeedAbilities;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

public class StunListener
implements Listener {
    private final BlissGems plugin;

    public StunListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    private boolean isImmobilized(UUID playerId) {
        return FluxAbilities.isPlayerStunned(playerId) || SpeedAbilities.isPlayerFrozen(playerId);
    }

    private boolean isFrozen(UUID playerId) {
        return SpeedAbilities.isPlayerFrozen(playerId);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (this.isFrozen(player.getUniqueId())) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                Location stayLoc = from.clone();
                stayLoc.setYaw(to.getYaw());
                stayLoc.setPitch(to.getPitch());
                event.setTo(stayLoc);
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        if (this.isImmobilized(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (this.isImmobilized(playerId)) {
            Material type = item.getType();
            if (type == Material.ENDER_PEARL || type == Material.CHORUS_FRUIT) {
                event.setCancelled(true);
                player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("cannot-use-while-stunned", "item", type == Material.ENDER_PEARL ? "ender pearls" : "chorus fruit"));
                return;
            }
            if (this.isFrozen(playerId)) {
                if (type != Material.GOLDEN_APPLE && type != Material.ENCHANTED_GOLDEN_APPLE) {
                    event.setCancelled(true);
                    if (item.getType().isEdible() || type == Material.POTION || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION) {
                        player.sendMessage(this.plugin.getConfigManager().getMessage("can-only-eat-golden-apple-stunned"));
                    }
                    return;
                }
            } else if (type != Material.GOLDEN_APPLE && type != Material.ENCHANTED_GOLDEN_APPLE && (item.getType().isEdible() || type == Material.POTION || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION)) {
                event.setCancelled(true);
                player.sendMessage(this.plugin.getConfigManager().getMessage("can-only-eat-golden-apple-stunned"));
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Material type;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (this.isImmobilized(player.getUniqueId()) && (type = item.getType()) != Material.GOLDEN_APPLE && type != Material.ENCHANTED_GOLDEN_APPLE) {
            event.setCancelled(true);
            player.sendMessage(this.plugin.getConfigManager().getMessage("can-only-eat-golden-apple-stunned"));
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Player player;
        ProjectileSource projectileSource = event.getEntity().getShooter();
        if (projectileSource instanceof Player && this.isImmobilized((player = (Player)projectileSource).getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        Player player;
        HumanEntity humanEntity = event.getPlayer();
        if (humanEntity instanceof Player && this.isFrozen((player = (Player)humanEntity).getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("\u00a7c\u00a7lYou cannot open inventories while frozen!");
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (this.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player player;
        Entity entity = event.getDamager();
        if (entity instanceof Player && this.isFrozen((player = (Player)entity).getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("\u00a7c\u00a7lYou cannot attack while frozen!");
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onImmobilizedKnockback(EntityDamageByEntityEvent event) {
        Player player;
        Entity entity = event.getEntity();
        if (!(entity instanceof Player) || !this.isFrozen((player = (Player)entity).getUniqueId())) {
            return;
        }
        this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
            if (player.isOnline() && this.isFrozen(player.getUniqueId())) {
                player.setVelocity(new Vector(0, 0, 0));
            }
        });
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (this.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (this.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}

