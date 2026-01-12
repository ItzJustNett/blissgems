package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Handles revive beacon interactions and death reviving
 */
public class ReviveBeaconListener implements Listener {
    private final BlissGems plugin;

    public ReviveBeaconListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) {
            return;
        }

        String oraxenId = CustomItemManager.getIdByItem(item);
        if (oraxenId == null || !oraxenId.equals("revive_beacon")) {
            return;
        }

        // Right-click to activate
        if (event.getAction().toString().contains("RIGHT_CLICK")) {
            event.setCancelled(true);

            // Get config values
            int duration = plugin.getConfig().getInt("revive-beacon.duration", 300); // 5 minutes default
            double range = plugin.getConfig().getDouble("revive-beacon.range", 10.0); // 10 blocks default

            // Activate the beacon at player's location
            Location beaconLoc = player.getLocation().clone();
            plugin.getReviveBeaconManager().activateBeacon(player, beaconLoc, duration, range);

            // Consume the item
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }

            // Visual and audio effects
            player.getWorld().spawnParticle(
                Particle.TOTEM_OF_UNDYING,
                beaconLoc.clone().add(0, 1, 0),
                50,
                1.0, 1.0, 1.0,
                0.1
            );
            player.getWorld().playSound(beaconLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);

            // Send message
            int minutes = duration / 60;
            int seconds = duration % 60;
            String timeStr = minutes > 0
                ? String.format("%dm %ds", minutes, seconds)
                : String.format("%ds", seconds);

            player.sendMessage("§e§lRevive Beacon activated!");
            player.sendMessage("§7Duration: §f" + timeStr);
            player.sendMessage("§7Range: §f" + (int)range + " blocks");
            player.sendMessage("§7If you die within range, you'll be revived!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        // Check if player has an active beacon and is in range
        if (!plugin.getReviveBeaconManager().canRevive(player)) {
            return;
        }

        // Get revive location
        Location reviveLoc = plugin.getReviveBeaconManager().getReviveLocation(player);
        if (reviveLoc == null) {
            return;
        }

        // Prevent death
        event.setCancelled(true);
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();

        // Schedule revival (next tick to ensure proper state)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Revive player
            player.spigot().respawn();

            // Teleport to beacon location
            player.teleport(reviveLoc);

            // Set health and effects
            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            player.setHealth(Math.min(maxHealth, plugin.getConfig().getDouble("revive-beacon.revive-health", maxHealth / 2)));
            player.setFoodLevel(20);
            player.setFireTicks(0);

            // Add temporary effects
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1)); // 5 seconds Regen II
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1)); // 10 seconds Absorption II
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0)); // 10 seconds Fire Resistance

            // Visual and audio effects
            player.getWorld().spawnParticle(
                Particle.TOTEM_OF_UNDYING,
                player.getLocation().clone().add(0, 1, 0),
                100,
                1.0, 2.0, 1.0,
                0.2
            );
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);

            // Send message
            player.sendMessage("§a§l✦ You have been revived by your Revive Beacon!");

            // Remove the beacon (single use)
            plugin.getReviveBeaconManager().removeBeacon(player);

        }, 1L);
    }
}
