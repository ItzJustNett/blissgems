package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Manages elaborate ritual animations for gem-related events
 * (rerolls, first gem receiving, etc.)
 */
public class GemRitualManager {
    private final BlissGems plugin;

    public GemRitualManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    /**
     * Performs an elaborate totem-like ritual animation for gem receiving
     * @param player The player receiving the gem
     * @param gemType The gem type being received
     * @param isFirstGem Whether this is the player's first gem
     */
    public void performGemRitual(Player player, GemType gemType, boolean isFirstGem) {
        Location loc = player.getLocation().clone();
        org.bukkit.Color gemColor = getGemColor(gemType);

        // Apply slow falling during ritual
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 1, false, false));

        // Phase 1: Ground circle formation (0-1 seconds)
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 20) {
                    this.cancel();
                    return;
                }

                // Expanding circle on ground
                double radius = (ticks / 20.0) * 5.0;
                for (int i = 0; i < 32; i++) {
                    double angle = (i / 32.0) * 2 * Math.PI;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location particleLoc = loc.clone().add(x, 0.1, z);

                    Particle.DustOptions dust = new Particle.DustOptions(gemColor, 1.5f);
                    player.getWorld().spawnParticle(Particle.DUST, particleLoc, 3, 0.1, 0.1, 0.1, 0.0, dust, true);
                    player.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0.0, 0.0, 0.0, 0.01);
                }

                // Sound effects
                if (ticks % 5 == 0) {
                    player.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.5f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Phase 2: Spiral totem effect (1-3 seconds)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            new BukkitRunnable() {
                int ticks = 0;
                final int maxTicks = 40; // 2 seconds

                @Override
                public void run() {
                    if (!player.isOnline() || ticks >= maxTicks) {
                        this.cancel();
                        return;
                    }

                    double progress = ticks / (double) maxTicks;
                    double height = progress * 5.0;

                    // Triple helix spiral
                    for (int spiral = 0; spiral < 3; spiral++) {
                        double spiralOffset = (spiral / 3.0) * 2 * Math.PI;
                        double angle = (progress * 6 * Math.PI) + spiralOffset;
                        double radius = 1.5 * (1 - progress * 0.5);

                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        Location particleLoc = loc.clone().add(x, height, z);

                        Particle.DustOptions dust = new Particle.DustOptions(gemColor, 1.8f);
                        player.getWorld().spawnParticle(Particle.DUST, particleLoc, 5, 0.1, 0.1, 0.1, 0.0, dust, true);
                        player.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 8, 0.2, 0.2, 0.2, 0.5);
                        player.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 2, 0.1, 0.1, 0.1, 0.02);
                    }

                    // Pillar particles
                    for (double y = 0; y <= height; y += 0.3) {
                        Particle.DustOptions pillarDust = new Particle.DustOptions(gemColor, 0.8f);
                        player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, y, 0), 2, 0.15, 0.1, 0.15, 0.0, pillarDust, true);
                    }

                    // Sound effects
                    if (ticks % 10 == 0) {
                        player.playSound(loc, Sound.BLOCK_BELL_USE, 0.7f, 1.0f + (float) progress);
                        player.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.5f);
                    }

                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }, 20L);

        // Phase 3: Explosion and convergence (3-4 seconds)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Location center = loc.clone().add(0, 5, 0);

            // Massive totem-like explosion
            Particle.DustOptions explosionDust = new Particle.DustOptions(gemColor, 2.5f);
            player.getWorld().spawnParticle(Particle.DUST, center, 200, 1.5, 1.5, 1.5, 0.0, explosionDust, true);
            player.getWorld().spawnParticle(Particle.FIREWORK, center, 100, 1.0, 1.0, 1.0, 0.2);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center, 80, 1.2, 1.2, 1.2, 0.1);
            player.getWorld().spawnParticle(Particle.END_ROD, center, 60, 1.5, 1.5, 1.5, 0.15);
            player.getWorld().spawnParticle(Particle.ENCHANT, center, 150, 2.0, 2.0, 2.0, 1.0);

            // Beacon beam effect
            for (double y = 0; y <= 10; y += 0.2) {
                Particle.DustOptions beamDust = new Particle.DustOptions(gemColor, 1.5f);
                player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, y, 0), 8, 0.2, 0.1, 0.2, 0.0, beamDust, true);
                player.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, y, 0), 3, 0.15, 0.1, 0.15, 0.02);
            }

            // Epic sounds
            player.playSound(loc, Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
            player.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 2.0f);
            player.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
            player.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 1.5f);

            // Convergence particles falling to player
            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (!player.isOnline() || ticks >= 20) {
                        this.cancel();
                        return;
                    }

                    // Particles converging from above
                    for (int i = 0; i < 10; i++) {
                        double randomX = (Math.random() - 0.5) * 3.0;
                        double randomZ = (Math.random() - 0.5) * 3.0;
                        double heightOffset = 5 - (ticks / 20.0) * 4.5;

                        Location convergeStart = loc.clone().add(randomX, heightOffset, randomZ);
                        Location playerLoc = player.getLocation().add(0, 1, 0);

                        // Particle moving towards player
                        Particle.DustOptions convergeDust = new Particle.DustOptions(gemColor, 1.2f);
                        player.getWorld().spawnParticle(Particle.DUST, convergeStart, 1, 0.0, 0.0, 0.0, 0.0, convergeDust, true);
                        player.getWorld().spawnParticle(Particle.END_ROD, convergeStart, 1, 0.0, 0.0, 0.0, 0.01);
                    }

                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);

        }, 60L);

        // Phase 4: Final burst at player (4 seconds)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Location playerCenter = player.getLocation().add(0, 1, 0);

            // Final absorption burst
            Particle.DustOptions finalDust = new Particle.DustOptions(gemColor, 2.0f);
            player.getWorld().spawnParticle(Particle.DUST, playerCenter, 150, 0.8, 1.0, 0.8, 0.0, finalDust, true);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, playerCenter, 50, 0.5, 0.8, 0.5, 0.1);
            player.getWorld().spawnParticle(Particle.ENCHANT, playerCenter, 100, 0.6, 1.0, 0.6, 0.8);
            player.getWorld().spawnParticle(Particle.FIREWORK, playerCenter, 30, 0.5, 0.5, 0.5, 0.1);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, playerCenter, 40, 0.6, 0.8, 0.6, 0.0);

            // Radial burst
            for (int i = 0; i < 360; i += 10) {
                double angle = Math.toRadians(i);
                double x = Math.cos(angle) * 2.0;
                double z = Math.sin(angle) * 2.0;

                Particle.DustOptions burstDust = new Particle.DustOptions(gemColor, 1.5f);
                player.getWorld().spawnParticle(Particle.DUST, playerCenter.clone().add(x, 0, z), 5, 0.1, 0.1, 0.1, 0.0, burstDust, true);
                player.getWorld().spawnParticle(Particle.END_ROD, playerCenter.clone().add(x, 0, z), 2, 0.0, 0.0, 0.0, 0.05);
            }

            // Final sounds
            player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.playSound(loc, Sound.BLOCK_BELL_USE, 1.0f, 2.0f);
            player.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 2.0f);

            // Give glowing effect briefly
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false));

        }, 80L);

        // Phase 5: Lingering particles (4-6 seconds)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (!player.isOnline() || ticks >= 40) {
                        this.cancel();
                        return;
                    }

                    // Gentle orbiting particles
                    double angle = (ticks / 40.0) * 4 * Math.PI;
                    double radius = 1.5;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location orbitLoc = player.getLocation().add(x, 1.5, z);
                    Particle.DustOptions orbitDust = new Particle.DustOptions(gemColor, 1.0f);
                    player.getWorld().spawnParticle(Particle.DUST, orbitLoc, 3, 0.1, 0.1, 0.1, 0.0, orbitDust, true);
                    player.getWorld().spawnParticle(Particle.END_ROD, orbitLoc, 1, 0.0, 0.0, 0.0, 0.01);

                    // Ambient sparkles
                    if (ticks % 5 == 0) {
                        Location playerLoc = player.getLocation().add(0, 1, 0);
                        player.getWorld().spawnParticle(Particle.END_ROD, playerLoc, 3, 0.5, 0.5, 0.5, 0.02);
                    }

                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);

        }, 80L);
    }

    /**
     * Performs a revive beacon ritual animation
     * @param player The player activating the revive beacon
     * @param location The beacon location
     */
    public void performReviveBeaconRitual(Player player, Location location) {
        org.bukkit.Color ritualColor = Color.fromRGB(255, 215, 0); // Gold

        // Apply levitation during ritual
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, false, false));

        // Phase 1: Ground circle formation (0-1 seconds)
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 20) {
                    this.cancel();
                    return;
                }

                // Expanding circle on ground
                double radius = (ticks / 20.0) * 6.0;
                for (int i = 0; i < 32; i++) {
                    double angle = (i / 32.0) * 2 * Math.PI;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location particleLoc = location.clone().add(x, 0.1, z);

                    Particle.DustOptions dust = new Particle.DustOptions(ritualColor, 2.0f);
                    player.getWorld().spawnParticle(Particle.DUST, particleLoc, 5, 0.1, 0.1, 0.1, 0.0, dust, true);
                    player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, particleLoc, 2, 0.0, 0.0, 0.0, 0.02);
                }

                // Sound effects
                if (ticks % 5 == 0) {
                    player.playSound(location, Sound.BLOCK_BEACON_AMBIENT, 0.7f, 1.5f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Phase 2: Rising pillar effect (1-2.5 seconds)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            new BukkitRunnable() {
                int ticks = 0;
                final int maxTicks = 30; // 1.5 seconds

                @Override
                public void run() {
                    if (!player.isOnline() || ticks >= maxTicks) {
                        this.cancel();
                        return;
                    }

                    double progress = ticks / (double) maxTicks;
                    double height = progress * 8.0;

                    // Central pillar
                    for (double y = 0; y <= height; y += 0.3) {
                        Particle.DustOptions pillarDust = new Particle.DustOptions(ritualColor, 1.5f);
                        player.getWorld().spawnParticle(Particle.DUST, location.clone().add(0, y, 0), 3, 0.2, 0.1, 0.2, 0.0, pillarDust, true);
                        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, location.clone().add(0, y, 0), 1, 0.1, 0.1, 0.1, 0.01);
                    }

                    // Sound effects
                    if (ticks % 10 == 0) {
                        player.playSound(location, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.0f + (float) progress);
                    }

                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }, 20L);

        // Phase 3: Explosion and beacon beam (2.5-3 seconds)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Location center = location.clone().add(0, 8, 0);

            // Massive golden explosion
            Particle.DustOptions explosionDust = new Particle.DustOptions(ritualColor, 3.0f);
            player.getWorld().spawnParticle(Particle.DUST, center, 300, 2.0, 2.0, 2.0, 0.0, explosionDust, true);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center, 150, 1.5, 1.5, 1.5, 0.15);
            player.getWorld().spawnParticle(Particle.FIREWORK, center, 80, 1.0, 1.0, 1.0, 0.2);
            player.getWorld().spawnParticle(Particle.END_ROD, center, 100, 2.0, 2.0, 2.0, 0.2);

            // Beacon beam shooting up
            for (double y = 0; y <= 20; y += 0.2) {
                Particle.DustOptions beamDust = new Particle.DustOptions(ritualColor, 2.0f);
                player.getWorld().spawnParticle(Particle.DUST, location.clone().add(0, y, 0), 12, 0.3, 0.1, 0.3, 0.0, beamDust, true);
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, location.clone().add(0, y, 0), 3, 0.2, 0.1, 0.2, 0.03);
            }

            // Epic sounds
            player.playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.0f);
            player.playSound(location, Sound.ITEM_TOTEM_USE, 1.0f, 1.2f);
            player.playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 2.0f);

        }, 50L);

        // Phase 4: Lingering beacon effect (3-5 seconds)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (!player.isOnline() || ticks >= 40) {
                        this.cancel();
                        return;
                    }

                    // Orbiting particles around beacon
                    double angle = (ticks / 40.0) * 4 * Math.PI;
                    double radius = 2.0;

                    for (int ring = 0; ring < 3; ring++) {
                        double ringHeight = ring * 2.0 + 1.0;
                        double x = Math.cos(angle + ring * Math.PI / 1.5) * radius;
                        double z = Math.sin(angle + ring * Math.PI / 1.5) * radius;

                        Location orbitLoc = location.clone().add(x, ringHeight, z);
                        Particle.DustOptions orbitDust = new Particle.DustOptions(ritualColor, 1.2f);
                        player.getWorld().spawnParticle(Particle.DUST, orbitLoc, 5, 0.1, 0.1, 0.1, 0.0, orbitDust, true);
                        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, orbitLoc, 1, 0.0, 0.0, 0.0, 0.01);
                    }

                    // Central glow
                    if (ticks % 5 == 0) {
                        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, location.clone().add(0, 2, 0), 5, 0.5, 0.5, 0.5, 0.02);
                    }

                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);

        }, 60L);
    }

    /**
     * Get the color associated with a gem type
     */
    private org.bukkit.Color getGemColor(GemType gemType) {
        return switch (gemType) {
            case ASTRA -> Color.fromRGB(106, 11, 184);    // Deep purple
            case FIRE -> Color.fromRGB(255, 85, 85);      // Bright red
            case FLUX -> Color.fromRGB(85, 255, 255);     // Cyan/aqua
            case LIFE -> Color.fromRGB(85, 255, 85);      // Bright green
            case PUFF -> Color.fromRGB(255, 255, 255);    // White
            case SPEED -> Color.fromRGB(255, 255, 85);    // Yellow
            case STRENGTH -> Color.fromRGB(170, 0, 0);    // Dark red
            case WEALTH -> Color.fromRGB(255, 170, 0);    // Gold
        };
    }
}
