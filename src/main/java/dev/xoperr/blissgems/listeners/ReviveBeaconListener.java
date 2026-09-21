/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.BanList
 *  org.bukkit.BanList$Type
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Attributes;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ReviveBeaconListener
implements Listener {
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
        FileConfiguration config = this.plugin.getConfig();
        int duration = config.getInt("revive-beacon.duration", 300);
        double range = config.getDouble("revive-beacon.range", 10.0);
        Location beaconLoc = player.getLocation().clone();
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
        int minutes = duration / 60;
        int seconds = duration % 60;
        String timeStr = minutes > 0 ? String.format("%dm %ds", minutes, seconds) : String.format("%ds", seconds);
        player.sendMessage("");
        player.sendMessage("\u00a7e\u00a7l\u00a7m                                      ");
        player.sendMessage("\u00a7e\u00a7l\u00a7nREVIVE BEACON RITUAL");
        player.sendMessage("");
        player.sendMessage("\u00a77\u00a7oThe beacon of resurrection awakens...");
        player.sendMessage("\u00a77\u00a7oDuration: \u00a7f" + timeStr);
        player.sendMessage("\u00a77\u00a7oRange: \u00a7f" + (int)range + " blocks");
        player.sendMessage("\u00a7e\u00a7l\u00a7m                                      ");
        player.sendMessage("");
        this.plugin.getGemRitualManager().performReviveBeaconRitual(player, beaconLoc);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            this.plugin.getReviveBeaconManager().activateBeacon(player, beaconLoc, duration, range);
            player.sendMessage("");
            player.sendMessage("\u00a7a\u00a7l\u2726 Revive Beacon is now active!");
            player.sendMessage("\u00a77If you die within range, you'll be revived and unbanned!");
            player.sendMessage("");
        }, 60L);
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!this.plugin.getReviveBeaconManager().canRevive(player)) {
            return;
        }
        Location reviveLoc = this.plugin.getReviveBeaconManager().getReviveLocation(player);
        if (reviveLoc == null) {
            return;
        }
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        int currentEnergy = this.plugin.getEnergyManager().getEnergy(player);
        int energyLoss = this.plugin.getConfigManager().getEnergyLossOnDeath();
        int energyAfterDeath = currentEnergy - energyLoss;
        boolean willBeBanned = energyAfterDeath <= 0 && this.plugin.getConfigManager().isBanOnZeroEnergyEnabled();
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            BanList banList;
            player.spigot().respawn();
            if ((willBeBanned || player.isBanned()) && ((banList = Bukkit.getBanList((BanList.Type)BanList.Type.PROFILE)).isBanned(player.getName()) || banList.isBanned(player.getUniqueId().toString()))) {
                banList.pardon(player.getName());
                banList.pardon(player.getUniqueId().toString());
                player.sendMessage("\u00a7d\u00a7l\u2726 You have been unbanned by the Revive Beacon!");
            }
            if (energyAfterDeath <= 0) {
                int restoreAmount = this.plugin.getConfig().getInt("revive-beacon.restore-energy", 3);
                this.plugin.getEnergyManager().setEnergy(player, restoreAmount);
                player.sendMessage("\u00a7b\u00a7l\u2726 Your energy has been restored to " + restoreAmount + "!");
            }
            player.teleport(reviveLoc);
            double maxHealth = player.getAttribute(Attributes.maxHealth()).getValue();
            player.setHealth(Math.min(maxHealth, this.plugin.getConfig().getDouble("revive-beacon.revive-health", maxHealth / 2.0)));
            player.setFoodLevel(20);
            player.setFireTicks(0);
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().clone().add(0.0, 1.0, 0.0), 100, 1.0, 2.0, 1.0, 0.2);
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
            player.sendMessage("");
            player.sendMessage("\u00a7a\u00a7l\u2726 \u00a7f\u00a7lYOU HAVE BEEN REVIVED!");
            player.sendMessage("\u00a77Your Revive Beacon has saved you from death!");
            player.sendMessage("");
            this.plugin.getReviveBeaconManager().removeBeacon(player);
        }, 1L);
    }
}

