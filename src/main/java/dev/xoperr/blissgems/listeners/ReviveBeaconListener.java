package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
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
        if (!event.getAction().toString().contains("RIGHT_CLICK")) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }

        String oraxenId = CustomItemManager.getIdByItem(item);
        if (oraxenId == null || !oraxenId.equals("revive_beacon")) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);

        FileConfiguration config = plugin.getConfig();
        int duration = config.getInt("revive-beacon.duration", 300); // 5 minutes default
        double range = config.getDouble("revive-beacon.range", 10.0); // 10 blocks default

        Location beaconLoc = player.getLocation().clone();

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

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

        plugin.getGemRitualManager().performReviveBeaconRitual(player, beaconLoc);

        // Let the ritual animation play out before the beacon goes live
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getReviveBeaconManager().activateBeacon(player, beaconLoc, duration, range);

            player.sendMessage("");
            player.sendMessage("§a§l✦ Revive Beacon is now active!");
            player.sendMessage("§7If you die within range, you'll be revived and unbanned!");
            player.sendMessage("");

        }, 60L); // 3 seconds delay
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (!plugin.getReviveBeaconManager().canRevive(player)) {
            return;
        }

        Location reviveLoc = plugin.getReviveBeaconManager().getReviveLocation(player);
        if (reviveLoc == null) {
            return;
        }

        // A revived player keeps everything: no drops, no XP loss
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();

        final int currentEnergy = plugin.getEnergyManager().getEnergy(player);
        final int energyLoss = plugin.getConfigManager().getEnergyLossOnDeath();
        final int energyAfterDeath = currentEnergy - energyLoss;
        final boolean willBeBanned = energyAfterDeath <= 0 && plugin.getConfigManager().isBanOnZeroEnergyEnabled();

        // Next tick, so the death is fully applied before we respawn them
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.spigot().respawn();

            if (willBeBanned || player.isBanned()) {
                BanList banList = Bukkit.getBanList(BanList.Type.PROFILE);
                if (banList.isBanned(player.getName()) || banList.isBanned(player.getUniqueId().toString())) {
                    banList.pardon(player.getName());
                    banList.pardon(player.getUniqueId().toString());
                    player.sendMessage("§d§l✦ You have been unbanned by the Revive Beacon!");
                }
            }

            if (energyAfterDeath <= 0) {
                int restoreAmount = plugin.getConfig().getInt("revive-beacon.restore-energy", 3);
                plugin.getEnergyManager().setEnergy(player, restoreAmount);
                player.sendMessage("§b§l✦ Your energy has been restored to " + restoreAmount + "!");
            }

            player.teleport(reviveLoc);

            double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            player.setHealth(Math.min(maxHealth, plugin.getConfig().getDouble("revive-beacon.revive-health", maxHealth / 2)));
            player.setFoodLevel(20);
            player.setFireTicks(0);

            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1)); // 5 seconds Regen II
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1)); // 10 seconds Absorption II
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0)); // 10 seconds Fire Resistance
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0)); // 5 seconds Glowing

            player.getWorld().spawnParticle(
                Particle.TOTEM_OF_UNDYING,
                player.getLocation().clone().add(0, 1, 0),
                100,
                1.0, 2.0, 1.0,
                0.2
            );
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

            player.sendMessage("");
            player.sendMessage("§a§l✦ §f§lYOU HAVE BEEN REVIVED!");
            player.sendMessage("§7Your Revive Beacon has saved you from death!");
            player.sendMessage("");

            // Beacons are single use
            plugin.getReviveBeaconManager().removeBeacon(player);

        }, 1L);
    }
}
