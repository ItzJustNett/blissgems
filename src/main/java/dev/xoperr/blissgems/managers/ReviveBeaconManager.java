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
     * Activates a revive beacon for a player, replacing any beacon they already had.
     * @param duration Duration in seconds
     * @param range Range in blocks
     */
    public void activateBeacon(Player player, Location location, int duration, double range) {
        UUID playerId = player.getUniqueId();

        ReviveBeacon previous = activeBeacons.get(playerId);
        if (previous != null) {
            previous.cancel();
        }

        ReviveBeacon beacon = new ReviveBeacon(plugin, player, location, duration, range);
        activeBeacons.put(playerId, beacon);
        beacon.start();
    }

    /**
     * @return true if the player has an active beacon and is standing in its range
     */
    public boolean canRevive(Player player) {
        ReviveBeacon beacon = activeBeacons.get(player.getUniqueId());
        return beacon != null && beacon.isActive() && beacon.isInRange(player.getLocation());
    }

    /**
     * @return the revive location, or null if the player has no beacon
     */
    public Location getReviveLocation(Player player) {
        ReviveBeacon beacon = activeBeacons.get(player.getUniqueId());
        return beacon != null ? beacon.getLocation() : null;
    }

    public void removeBeacon(Player player) {
        ReviveBeacon beacon = activeBeacons.remove(player.getUniqueId());
        if (beacon != null) {
            beacon.cancel();
        }
    }

    public void cleanup() {
        for (ReviveBeacon beacon : activeBeacons.values()) {
            beacon.cancel();
        }
        activeBeacons.clear();
    }

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
            int durationTicks = (int) ((expiryTime - System.currentTimeMillis()) / 50);
            taskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                active = false;
                player.sendMessage("§c§lYour Revive Beacon has expired!");
            }, durationTicks);

            plugin.getServer().getScheduler().runTaskTimer(plugin, (task) -> {
                if (!active || System.currentTimeMillis() >= expiryTime) {
                    task.cancel();
                    return;
                }

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
            if (location.getWorld() == null || !location.getWorld().equals(loc.getWorld())) {
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
