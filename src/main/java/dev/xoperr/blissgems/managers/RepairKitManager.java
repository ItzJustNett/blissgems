package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages Repair Kit pedestals that restore gem energy for nearby players
 */
public class RepairKitManager {
    private final BlissGems plugin;
    private final Map<Location, PedestalData> activePedestals = new HashMap<>();

    public RepairKitManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a pedestal at the given location and starts the repair process
     */
    public boolean createPedestal(Location location) {
        // Check if beacon exists at this location
        Block block = location.getBlock();
        if (block.getType() != Material.BEACON) {
            return false;
        }

        // Check if pedestal already exists here
        if (activePedestals.containsKey(location)) {
            return false;
        }

        // Get config values
        int energyPerSecond = plugin.getConfig().getInt("repair-kit.energy-per-second", 1);
        int maxTotalEnergy = plugin.getConfig().getInt("repair-kit.max-total-energy", 10);
        double healRadius = plugin.getConfig().getDouble("repair-kit.heal-radius", 10.0);
        int updateInterval = plugin.getConfig().getInt("repair-kit.update-interval", 20);
        boolean prioritizeLowest = plugin.getConfig().getBoolean("repair-kit.prioritize-lowest", true);

        // Create pedestal data
        PedestalData pedestal = new PedestalData(location, maxTotalEnergy);
        activePedestals.put(location, pedestal);

        // Play creation effects
        location.getWorld().playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
        location.getWorld().spawnParticle(Particle.END_ROD,
            location.clone().add(0.5, 1.0, 0.5),
            50, 0.5, 1.0, 0.5, 0.1);

        // Start repair task
        BukkitTask task = new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                // Check if pedestal still exists
                if (block.getType() != Material.BEACON || pedestal.isExpired()) {
                    removePedestal(location);
                    this.cancel();
                    return;
                }

                ticksElapsed++;

                // Every update interval (default 1 second)
                if (ticksElapsed % updateInterval == 0) {
                    // Find nearby players who need energy restoration
                    List<Player> nearbyPlayers = getNearbyPlayersNeedingEnergy(location, healRadius);

                    if (nearbyPlayers.isEmpty()) {
                        // No players need energy - check if we should stop
                        if (ticksElapsed >= 60 * 20) { // After 1 minute of no activity
                            removePedestal(location);
                            this.cancel();
                            return;
                        }
                    } else {
                        // Sort by energy if prioritizing lowest
                        if (prioritizeLowest) {
                            nearbyPlayers.sort(Comparator.comparingInt(p ->
                                plugin.getEnergyManager().getEnergy(p)));
                        }

                        // Restore energy to players
                        int energyRestored = 0;
                        for (Player player : nearbyPlayers) {
                            int currentEnergy = plugin.getEnergyManager().getEnergy(player);
                            int maxEnergy = plugin.getConfigManager().getMaxEnergy();

                            if (currentEnergy < maxEnergy) {
                                int toRestore = Math.min(energyPerSecond, maxEnergy - currentEnergy);
                                toRestore = Math.min(toRestore, pedestal.getRemainingEnergy());

                                if (toRestore > 0) {
                                    plugin.getEnergyManager().addEnergy(player, toRestore);
                                    pedestal.consumeEnergy(toRestore);
                                    energyRestored += toRestore;

                                    // Visual feedback for player
                                    player.spawnParticle(Particle.HAPPY_VILLAGER,
                                        player.getLocation().add(0, 1, 0),
                                        10, 0.3, 0.5, 0.3, 0);
                                    player.playSound(player.getLocation(),
                                        Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                                        0.5f, 1.5f);

                                    String msg = plugin.getConfigManager()
                                        .getFormattedMessage("gem-energy-restored",
                                            "amount", String.valueOf(toRestore));
                                    if (msg != null && !msg.isEmpty()) {
                                        player.sendMessage(msg);
                                    }
                                }

                                // Check if pedestal is exhausted
                                if (pedestal.getRemainingEnergy() <= 0) {
                                    break;
                                }
                            }
                        }

                        // Check if pedestal should be removed (exhausted)
                        if (pedestal.getRemainingEnergy() <= 0) {
                            removePedestal(location);

                            // Notify nearby players
                            for (Player player : getNearbyPlayers(location, healRadius)) {
                                player.sendMessage("§d§oThe Repair Kit has been exhausted!");
                            }

                            this.cancel();
                            return;
                        }
                    }

                    // Visual effects every second
                    if (plugin.getConfig().getBoolean("repair-kit.play-effects", true)) {
                        Particle particle = Particle.valueOf(
                            plugin.getConfig().getString("repair-kit.particle", "HAPPY_VILLAGER"));
                        int particleCount = plugin.getConfig().getInt("repair-kit.particle-count", 5);

                        // Particle beam upward from beacon
                        location.getWorld().spawnParticle(particle,
                            location.clone().add(0.5, 1.0, 0.5),
                            particleCount, 0.3, 0.5, 0.3, 0);

                        // Particle ring showing radius
                        if (ticksElapsed % (updateInterval * 3) == 0) {
                            for (int i = 0; i < 16; i++) {
                                double angle = (i / 16.0) * 2 * Math.PI;
                                double x = Math.cos(angle) * healRadius;
                                double z = Math.sin(angle) * healRadius;
                                location.getWorld().spawnParticle(Particle.END_ROD,
                                    location.clone().add(x, 0.5, z),
                                    1, 0, 0, 0, 0);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this.plugin, 0L, 1L);

        pedestal.setTask(task);
        return true;
    }

    /**
     * Removes a pedestal and stops its task
     */
    public void removePedestal(Location location) {
        PedestalData pedestal = activePedestals.remove(location);
        if (pedestal != null) {
            if (pedestal.getTask() != null) {
                pedestal.getTask().cancel();
            }

            // Play removal effects
            location.getWorld().playSound(location, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
            location.getWorld().spawnParticle(Particle.SMOKE,
                location.clone().add(0.5, 1.0, 0.5),
                30, 0.5, 1.0, 0.5, 0.05);
        }
    }

    /**
     * Gets nearby players who need energy restoration
     */
    private List<Player> getNearbyPlayersNeedingEnergy(Location location, double radius) {
        List<Player> players = new ArrayList<>();
        int maxEnergy = plugin.getConfigManager().getMaxEnergy();

        for (Player player : getNearbyPlayers(location, radius)) {
            if (plugin.getEnergyManager().getEnergy(player) < maxEnergy) {
                players.add(player);
            }
        }

        return players;
    }

    /**
     * Gets all nearby players
     */
    private List<Player> getNearbyPlayers(Location location, double radius) {
        List<Player> players = new ArrayList<>();
        for (org.bukkit.entity.Entity entity : location.getWorld().getNearbyEntities(location, radius, radius, radius)) {
            if (entity instanceof Player) {
                players.add((Player) entity);
            }
        }
        return players;
    }

    /**
     * Checks if a pedestal exists at the given location
     */
    public boolean isPedestal(Location location) {
        return activePedestals.containsKey(location);
    }

    /**
     * Cleans up all pedestals (called on plugin disable)
     */
    public void cleanup() {
        for (Location location : new ArrayList<>(activePedestals.keySet())) {
            removePedestal(location);
        }
    }

    /**
     * Data class for pedestal information
     */
    private static class PedestalData {
        private final Location location;
        private int remainingEnergy;
        private BukkitTask task;

        public PedestalData(Location location, int maxEnergy) {
            this.location = location;
            this.remainingEnergy = maxEnergy;
        }

        public int getRemainingEnergy() {
            return remainingEnergy;
        }

        public void consumeEnergy(int amount) {
            remainingEnergy -= amount;
        }

        public boolean isExpired() {
            return remainingEnergy <= 0;
        }

        public void setTask(BukkitTask task) {
            this.task = task;
        }

        public BukkitTask getTask() {
            return task;
        }
    }
}
