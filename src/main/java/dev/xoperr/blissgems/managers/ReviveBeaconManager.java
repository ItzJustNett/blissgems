package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages active revive beacons and their locations
 */
public class ReviveBeaconManager {
    private final BlissGems plugin;
    private final Map<UUID, ReviveBeacon> activeBeacons = new HashMap<>();

    public ReviveBeaconManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    /**
     * Activates a revive beacon for a player
     * @param player The player activating the beacon
     * @param location The location of the beacon
     * @param duration Duration in seconds
     * @param range Range in blocks
     */
    public void activateBeacon(Player player, Location location, int duration, double range) {
        UUID playerId = player.getUniqueId();

        // Remove old beacon if exists
        if (activeBeacons.containsKey(playerId)) {
            activeBeacons.get(playerId).cancel();
        }

        // Create new beacon
        ReviveBeacon beacon = new ReviveBeacon(plugin, player, location, duration, range);
        activeBeacons.put(playerId, beacon);
        beacon.start();
    }

    /**
     * Checks if a player has an active beacon and if they're in range
     * @param player The player to check
     * @return true if the player can be revived by their beacon
     */
    public boolean canRevive(Player player) {
        UUID playerId = player.getUniqueId();
        if (!activeBeacons.containsKey(playerId)) {
            return false;
        }

        ReviveBeacon beacon = activeBeacons.get(playerId);
        return beacon.isActive() && beacon.isInRange(player.getLocation());
    }

    /**
     * Gets the revive location for a player
     * @param player The player
     * @return The revive location, or null if no beacon
     */
    public Location getReviveLocation(Player player) {
        UUID playerId = player.getUniqueId();
        if (!activeBeacons.containsKey(playerId)) {
            return null;
        }

        return activeBeacons.get(playerId).getLocation();
    }

    /**
     * Removes a player's beacon
     * @param player The player
     */
    public void removeBeacon(Player player) {
        UUID playerId = player.getUniqueId();
        if (activeBeacons.containsKey(playerId)) {
            activeBeacons.get(playerId).cancel();
            activeBeacons.remove(playerId);
        }
    }

    /**
     * Cleans up all beacons
     */
    public void cleanup() {
        for (ReviveBeacon beacon : activeBeacons.values()) {
            beacon.cancel();
        }
        activeBeacons.clear();
    }

    /**
     * Inner class representing an active revive beacon
     */
    private static class ReviveBeacon {
        private final BlissGems plugin;
        private final Player player;
        private final Location location;
        private final double range;
        private final long expiryTime;
        private int taskId = -1;
        private boolean active = true;

        public ReviveBeacon(BlissGems plugin, Player player, Location location, int duration, double range) {
            this.plugin = plugin;
            this.player = player;
            this.location = location.clone();
            this.range = range;
            this.expiryTime = System.currentTimeMillis() + (duration * 1000L);
        }

        public void start() {
            // Schedule expiry task
            int durationTicks = (int) ((expiryTime - System.currentTimeMillis()) / 50);
            taskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                active = false;
                player.sendMessage("§c§lYour Revive Beacon has expired!");
            }, durationTicks);

            // Spawn particles periodically
            plugin.getServer().getScheduler().runTaskTimer(plugin, (task) -> {
                if (!active || System.currentTimeMillis() >= expiryTime) {
                    task.cancel();
                    return;
                }

                // Spawn particles at beacon location
                if (location.getWorld() != null) {
                    location.getWorld().spawnParticle(
                        org.bukkit.Particle.TOTEM_OF_UNDYING,
                        location.clone().add(0, 1, 0),
                        10,
                        0.5, 0.5, 0.5,
                        0.05
                    );
                }
            }, 0L, 20L); // Every second
        }

        public boolean isActive() {
            return active && System.currentTimeMillis() < expiryTime;
        }

        public boolean isInRange(Location loc) {
            if (location.getWorld() == null || loc.getWorld() == null) {
                return false;
            }
            if (!location.getWorld().equals(loc.getWorld())) {
                return false;
            }
            return location.distance(loc) <= range;
        }

        public Location getLocation() {
            return location.clone();
        }

        public void cancel() {
            active = false;
            if (taskId != -1) {
                plugin.getServer().getScheduler().cancelTask(taskId);
            }
        }
    }
}
