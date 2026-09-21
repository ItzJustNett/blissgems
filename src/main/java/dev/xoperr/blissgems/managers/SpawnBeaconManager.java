package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Attributes;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.*;
import org.bukkit.block.Skull;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

public class SpawnBeaconManager {
    private final BlissGems plugin;
    private Location beaconLocation;
    private double radius;
    private List<Integer> openHoursUtc;
    private int durationMinutes;
    private boolean requireBeaconForRepairs;

    private long lastReviveBeaconTossTime = 0L;
    private UUID lastReviveBeaconTosser = null;

    public SpawnBeaconManager(BlissGems plugin) {
        this.plugin = plugin;
        this.reload();
    }

    public void reload() {
        String worldName = plugin.getConfig().getString("spawn-beacon.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }
        double x = plugin.getConfig().getDouble("spawn-beacon.x", 0.5);
        double y = plugin.getConfig().getDouble("spawn-beacon.y", 64.0);
        double z = plugin.getConfig().getDouble("spawn-beacon.z", 0.5);
        this.beaconLocation = (world != null) ? new Location(world, x, y, z) : null;
        this.radius = plugin.getConfig().getDouble("spawn-beacon.radius", 15.0);
        this.openHoursUtc = plugin.getConfig().getIntegerList("spawn-beacon.repair-schedule.open-hours-utc");
        if (openHoursUtc.isEmpty()) {
            openHoursUtc = Arrays.asList(0, 6, 12, 18);
        }
        this.durationMinutes = plugin.getConfig().getInt("spawn-beacon.repair-schedule.duration-minutes", 60);
        this.requireBeaconForRepairs = plugin.getConfig().getBoolean("spawn-beacon.require-beacon-for-repairs", true);
    }

    public Location getBeaconLocation() {
        return beaconLocation;
    }

    public double getRadius() {
        return radius;
    }

    public boolean isRequireBeaconForRepairs() {
        return requireBeaconForRepairs;
    }

    public boolean isWithinBeacon(Location loc) {
        if (beaconLocation == null || loc == null) return false;
        if (loc.getWorld() != beaconLocation.getWorld()) return false;
        return loc.distanceSquared(beaconLocation) <= (radius * radius);
    }

    public boolean isRepairWindowOpen() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        int currentHour = now.getHour();
        int currentMinute = now.getMinute();

        for (int openHour : openHoursUtc) {
            int startMinute = openHour * 60;
            int endMinute = startMinute + durationMinutes;
            int nowMinute = currentHour * 60 + currentMinute;

            if (nowMinute >= startMinute && nowMinute < endMinute) {
                return true;
            }
        }
        return false;
    }

    public String getRepairScheduleStatusString() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        int currentHour = now.getHour();
        int currentMinute = now.getMinute();
        int nowMinute = currentHour * 60 + currentMinute;

        // Check if currently open
        for (int openHour : openHoursUtc) {
            int startMinute = openHour * 60;
            int endMinute = startMinute + durationMinutes;
            if (nowMinute >= startMinute && nowMinute < endMinute) {
                int remainingMinutes = endMinute - nowMinute;
                return ChatColor.GREEN + "OPEN " + ChatColor.GRAY + "(Closes in " + remainingMinutes + "m)";
            }
        }

        // Find next opening
        int minDiff = Integer.MAX_VALUE;
        for (int openHour : openHoursUtc) {
            int startMinute = openHour * 60;
            int diff = startMinute - nowMinute;
            if (diff < 0) {
                diff += 24 * 60; // Next day
            }
            if (diff < minDiff) {
                minDiff = diff;
            }
        }

        int h = minDiff / 60;
        int m = minDiff % 60;
        return ChatColor.RED + "CLOSED " + ChatColor.GRAY + "(Opens in " + (h > 0 ? h + "h " : "") + m + "m)";
    }

    public boolean canRepairGems(Player player) {
        if (!requireBeaconForRepairs) return true;
        if (!isWithinBeacon(player.getLocation())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c&l[SPAWN BEACON] &cGem repairs can only be conducted at the Spawn Beacon! " +
                            "&7(Location: &fX: " + (int)beaconLocation.getX() + ", Y: " + (int)beaconLocation.getY() + ", Z: " + (int)beaconLocation.getZ() + "&7)"));
            return false;
        }
        if (!isRepairWindowOpen()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c&l[SPAWN BEACON] &cThe Spawn Beacon repair matrix is currently &lCLOSED&c! " +
                            "&7Status: " + getRepairScheduleStatusString() + ". &eUse /news for schedule."));
            return false;
        }
        return true;
    }

    public void onReviveBeaconDropped(Player tosser, Item itemEntity) {
        if (!isWithinBeacon(itemEntity.getLocation())) return;

        lastReviveBeaconTossTime = System.currentTimeMillis();
        lastReviveBeaconTosser = tosser.getUniqueId();
        itemEntity.remove();

        Location loc = beaconLocation != null ? beaconLocation : tosser.getLocation();
        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 1, 0), 50, 1.0, 2.0, 1.0, 0.1);
        loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 1, 0), 60, 0.5, 1.5, 0.5, 0.2);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 1.2f);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.8f);

        tosser.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&e&l✦ [SPAWN BEACON] &6Revive Beacon absorbed! Toss the victim's &fPlayer Head &6at the beacon within 30 seconds to resurrect them!"));
    }

    public boolean onPlayerHeadDropped(Player tosser, Item headEntity) {
        if (!isWithinBeacon(headEntity.getLocation())) return false;
        if (System.currentTimeMillis() - lastReviveBeaconTossTime > 30000L) {
            return false; // Window expired
        }

        ItemStack item = headEntity.getItemStack();
        if (item.getType() != Material.PLAYER_HEAD) return false;

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return false;

        OfflinePlayer target = meta.getOwningPlayer();
        if (target == null || target.getName() == null) {
            tosser.sendMessage(ChatColor.RED + "This player head is unidentifiable.");
            return false;
        }

        headEntity.remove();
        lastReviveBeaconTossTime = 0L; // Consume ritual

        // Execute Resurrection!
        resurrectPlayer(tosser, target);
        return true;
    }

    public void resurrectPlayer(Player summoner, OfflinePlayer victim) {
        Location targetLoc = beaconLocation != null ? beaconLocation.clone().add(0, 1, 0) : summoner.getLocation();

        // Pardon ban
        try {
            BanList banList = Bukkit.getBanList(BanList.Type.PROFILE);
            if (banList.isBanned(victim.getName())) {
                banList.pardon(victim.getName());
            }
            if (victim.getUniqueId() != null && banList.isBanned(victim.getUniqueId().toString())) {
                banList.pardon(victim.getUniqueId().toString());
            }
        } catch (Exception ignored) {}

        // Restore Energy
        int restoreAmount = plugin.getConfig().getInt("revive-beacon.restore-energy", 3);
        if (victim.isOnline()) {
            Player onlineVictim = victim.getPlayer();
            plugin.getEnergyManager().setEnergy(onlineVictim, restoreAmount);
            onlineVictim.teleport(targetLoc);
            double maxHealth = onlineVictim.getAttribute(Attributes.maxHealth()).getValue();
            onlineVictim.setHealth(maxHealth);
            onlineVictim.setFoodLevel(20);
            onlineVictim.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
            onlineVictim.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1));
            onlineVictim.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0));
            onlineVictim.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, targetLoc, 150, 1.0, 2.0, 1.0, 0.3);
            onlineVictim.getWorld().playSound(targetLoc, Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
            onlineVictim.getWorld().playSound(targetLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.0f);
            onlineVictim.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a&l✦ [RESURRECTION] &fYou have been brought back from death at the Spawn Beacon by &e" + summoner.getName() + "&f!"));
        } else {
            // Restore energy offline
            plugin.getEnergyManager().setEnergy(victim.getUniqueId(), restoreAmount);
        }

        // Global Broadcast
        String broadcast = ChatColor.translateAlternateColorCodes('&',
                "&d&l✦ [RESURRECTION] &e" + victim.getName() + " &7has been resurrected at the &bSpawn Beacon &7by &e" + summoner.getName() + "&7!");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(broadcast);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        }
    }
}
