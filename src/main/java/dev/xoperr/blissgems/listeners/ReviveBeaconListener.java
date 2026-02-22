package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
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

            // Beacon location
            Location beaconLoc = player.getLocation().clone();

            // Consume the item
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }

            // Send initial messages
            int minutes = duration / 60;
            int seconds = duration % 60;
            String timeStr = minutes > 0
                ? String.format("%dm %ds", minutes, seconds)
                : String.format("%ds", seconds);

            player.sendMessage("");
            player.sendMessage("§e§l§m                                      ");
            player.sendMessage("§e§l§nREVIVE BEACON RITUAL");
            player.sendMessage("");
            player.sendMessage("§7§oThe beacon of resurrection awakens...");
            player.sendMessage("§7§oDuration: §f" + timeStr);
            player.sendMessage("§7§oRange: §f" + (int)range + " blocks");
            player.sendMessage("§e§l§m                                      ");
            player.sendMessage("");

            // Start the ritual animation
            plugin.getGemRitualManager().performReviveBeaconRitual(player, beaconLoc);

            // Activate the beacon after ritual starts (small delay)
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getReviveBeaconManager().activateBeacon(player, beaconLoc, duration, range);

                player.sendMessage("");
                player.sendMessage("§a§l✦ Revive Beacon is now active!");
                player.sendMessage("§7If you die within range, you'll be revived and unbanned!");
                player.sendMessage("");

            }, 60L); // 3 seconds delay
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

        // Check if player will be banned due to 0 energy
        final int currentEnergy = plugin.getEnergyManager().getEnergy(player);
        final int energyLoss = plugin.getConfigManager().getEnergyLossOnDeath();
        final int energyAfterDeath = currentEnergy - energyLoss;
        final boolean willBeBanned = energyAfterDeath <= 0 && plugin.getConfigManager().isBanOnZeroEnergyEnabled();

        // Schedule revival (next tick to ensure proper state)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Revive player
            player.spigot().respawn();

            // Unban player if they were banned due to 0 energy
            if (willBeBanned || player.isBanned()) {
                BanList banList = Bukkit.getBanList(BanList.Type.PROFILE);
                if (banList.isBanned(player.getName()) || banList.isBanned(player.getUniqueId().toString())) {
                    banList.pardon(player.getName());
                    banList.pardon(player.getUniqueId().toString());
                    player.sendMessage("§d§l✦ You have been unbanned by the Revive Beacon!");
                }
            }

            // Restore some energy if they hit 0
            if (energyAfterDeath <= 0) {
                int restoreAmount = plugin.getConfig().getInt("revive-beacon.restore-energy", 3);
                plugin.getEnergyManager().setEnergy(player, restoreAmount);
                player.sendMessage("§b§l✦ Your energy has been restored to " + restoreAmount + "!");
            }

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
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0)); // 5 seconds Glowing

            // Visual and audio effects
            player.getWorld().spawnParticle(
                Particle.TOTEM_OF_UNDYING,
                player.getLocation().clone().add(0, 1, 0),
                100,
                1.0, 2.0, 1.0,
                0.2
            );
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

            // Send message
            player.sendMessage("");
            player.sendMessage("§a§l✦ §f§lYOU HAVE BEEN REVIVED!");
            player.sendMessage("§7Your Revive Beacon has saved you from death!");
            player.sendMessage("");

            // Remove the beacon (single use)
            plugin.getReviveBeaconManager().removeBeacon(player);

        }, 1L);
    }
}
