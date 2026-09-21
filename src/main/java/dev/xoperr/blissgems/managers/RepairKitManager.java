/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scheduler.BukkitTask
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Achievement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class RepairKitManager {
    private final BlissGems plugin;
    private final Map<Location, PedestalData> activePedestals = new HashMap<Location, PedestalData>();

    public RepairKitManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    public boolean createPedestal(final Location location) {
        final Block block = location.getBlock();
        if (block.getType() != Material.BEACON) {
            return false;
        }
        if (this.activePedestals.containsKey(location)) {
            return false;
        }
        final int energyPerSecond = this.plugin.getConfig().getInt("repair-kit.energy-per-second", 1);
        int maxTotalEnergy = this.plugin.getConfig().getInt("repair-kit.max-total-energy", 10);
        final double healRadius = this.plugin.getConfig().getDouble("repair-kit.heal-radius", 10.0);
        final int updateInterval = this.plugin.getConfig().getInt("repair-kit.update-interval", 20);
        final boolean prioritizeLowest = this.plugin.getConfig().getBoolean("repair-kit.prioritize-lowest", true);
        final PedestalData pedestal = new PedestalData(location, maxTotalEnergy);
        this.activePedestals.put(location, pedestal);
        location.getWorld().playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
        location.getWorld().spawnParticle(Particle.END_ROD, location.clone().add(0.5, 1.0, 0.5), 50, 0.5, 1.0, 0.5, 0.1);
        BukkitTask task = new BukkitRunnable(){
            int ticksElapsed = 0;

            public void run() {
                if (block.getType() != Material.BEACON || pedestal.isExpired()) {
                    RepairKitManager.this.removePedestal(location);
                    this.cancel();
                    return;
                }
                ++this.ticksElapsed;
                if (this.ticksElapsed % updateInterval == 0) {
                    List<Player> nearbyPlayers = RepairKitManager.this.getNearbyPlayersNeedingEnergy(location, healRadius);
                    if (nearbyPlayers.isEmpty()) {
                        if (this.ticksElapsed >= 1200) {
                            RepairKitManager.this.removePedestal(location);
                            this.cancel();
                            return;
                        }
                    } else {
                        if (prioritizeLowest) {
                            nearbyPlayers.sort(Comparator.comparingInt(p -> RepairKitManager.this.plugin.getEnergyManager().getEnergy((Player)p)));
                        }
                        int energyRestored = 0;
                        for (Player player : nearbyPlayers) {
                            int maxEnergy;
                            int currentEnergy = RepairKitManager.this.plugin.getEnergyManager().getEnergy(player);
                            if (currentEnergy >= (maxEnergy = RepairKitManager.this.plugin.getConfigManager().getMaxEnergy())) continue;
                            int toRestore = Math.min(energyPerSecond, maxEnergy - currentEnergy);
                            if ((toRestore = Math.min(toRestore, pedestal.getRemainingEnergy())) > 0) {
                                RepairKitManager.this.plugin.getEnergyManager().addEnergy(player, toRestore);
                                pedestal.consumeEnergy(toRestore);
                                energyRestored += toRestore;
                                if (RepairKitManager.this.plugin.getAchievementManager() != null) {
                                    RepairKitManager.this.plugin.getAchievementManager().addProgress(player, Achievement.GOOD_AS_NEW, toRestore);
                                }
                                Particle feedbackParticle = Particle.valueOf((String)RepairKitManager.this.plugin.getConfig().getString("repair-kit.particle", "HAPPY_VILLAGER"));
                                player.spawnParticle(feedbackParticle, player.getLocation().add(0.0, 1.0, 0.0), 10, 0.3, 0.5, 0.3, 0.0);
                                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
                                String msg = RepairKitManager.this.plugin.getConfigManager().getFormattedMessage("gem-energy-restored", "amount", String.valueOf(toRestore));
                                if (msg != null && !msg.isEmpty()) {
                                    player.sendMessage(msg);
                                }
                            }
                            if (pedestal.getRemainingEnergy() > 0) continue;
                            break;
                        }
                        if (pedestal.getRemainingEnergy() <= 0) {
                            RepairKitManager.this.removePedestal(location);
                            for (Player player : RepairKitManager.this.getNearbyPlayers(location, healRadius)) {
                                player.sendMessage("\u00a7d\u00a7oThe Repair Kit has been exhausted!");
                            }
                            this.cancel();
                            return;
                        }
                    }
                    if (RepairKitManager.this.plugin.getConfig().getBoolean("repair-kit.play-effects", true)) {
                        Particle particle = Particle.valueOf((String)RepairKitManager.this.plugin.getConfig().getString("repair-kit.particle", "HAPPY_VILLAGER"));
                        int particleCount = RepairKitManager.this.plugin.getConfig().getInt("repair-kit.particle-count", 5);
                        location.getWorld().spawnParticle(particle, location.clone().add(0.5, 1.0, 0.5), particleCount, 0.3, 0.5, 0.3, 0.0);
                        if (this.ticksElapsed % (updateInterval * 3) == 0) {
                            for (int i = 0; i < 16; ++i) {
                                double angle = (double)i / 16.0 * 2.0 * Math.PI;
                                double x = Math.cos(angle) * healRadius;
                                double z = Math.sin(angle) * healRadius;
                                location.getWorld().spawnParticle(Particle.END_ROD, location.clone().add(x, 0.5, z), 1, 0.0, 0.0, 0.0, 0.0);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        pedestal.setTask(task);
        return true;
    }

    public void removePedestal(Location location) {
        PedestalData pedestal = this.activePedestals.remove(location);
        if (pedestal != null) {
            if (pedestal.getTask() != null) {
                pedestal.getTask().cancel();
            }
            location.getWorld().playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
            location.getWorld().spawnParticle(Particle.SMOKE, location.clone().add(0.5, 1.0, 0.5), 30, 0.5, 1.0, 0.5, 0.05);
        }
    }

    private List<Player> getNearbyPlayersNeedingEnergy(Location location, double radius) {
        ArrayList<Player> players = new ArrayList<Player>();
        int maxEnergy = this.plugin.getConfigManager().getMaxEnergy();
        for (Player player : this.getNearbyPlayers(location, radius)) {
            if (this.plugin.getEnergyManager().getEnergy(player) >= maxEnergy) continue;
            players.add(player);
        }
        return players;
    }

    private List<Player> getNearbyPlayers(Location location, double radius) {
        ArrayList<Player> players = new ArrayList<Player>();
        for (Entity entity : location.getWorld().getNearbyEntities(location, radius, radius, radius)) {
            if (!(entity instanceof Player)) continue;
            players.add((Player)entity);
        }
        return players;
    }

    public boolean isPedestal(Location location) {
        return this.activePedestals.containsKey(location);
    }

    public void cleanup() {
        for (Location location : new ArrayList<Location>(this.activePedestals.keySet())) {
            this.removePedestal(location);
        }
    }

    private static class PedestalData {
        private final Location location;
        private int remainingEnergy;
        private BukkitTask task;

        public PedestalData(Location location, int maxEnergy) {
            this.location = location;
            this.remainingEnergy = maxEnergy;
        }

        public int getRemainingEnergy() {
            return this.remainingEnergy;
        }

        public void consumeEnergy(int amount) {
            this.remainingEnergy -= amount;
        }

        public boolean isExpired() {
            return this.remainingEnergy <= 0;
        }

        public void setTask(BukkitTask task) {
            this.task = task;
        }

        public BukkitTask getTask() {
            return this.task;
        }
    }
}

