package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

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
        // Achievement: Reawakening (complete a restoration ritual)
        if (!isFirstGem && this.plugin.getAchievementManager() != null) {
            this.plugin.getAchievementManager().unlock(player, Achievement.REAWAKENING);
        }

        Location loc = player.getLocation().clone();
        org.bukkit.Color gemColor = getGemColor(gemType);

        // Apply slow falling during ritual (extends for full 15-second sequence)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 300, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 1, false, false));

        // Phase 0: All 8 gems spinning orbit (0-4 seconds)
        List<ItemDisplay> orbitingGems = new ArrayList<>();
        GemType[] allGems = GemType.values();
        Location playerLoc = player.getLocation();

        // Spawn ItemDisplay entities for each gem
        for (int i = 0; i < allGems.length; i++) {
            String gemItemId = getGemItemId(allGems[i]);
            ItemStack gemItem = CustomItemManager.getItemById(gemItemId); // Use base texture like strength gem
            if (gemItem == null) continue; // Skip if gem item creation fails

            double angleOffset = (i / 8.0) * 2 * Math.PI;
            double radius = 2.5;
            double height = 1.5;

            double x = Math.cos(angleOffset) * radius;
            double z = Math.sin(angleOffset) * radius;
            Location gemLoc = playerLoc.clone().add(x, height, z);

            ItemDisplay itemDisplay = player.getWorld().spawn(gemLoc, ItemDisplay.class);
            itemDisplay.setItemStack(gemItem);
            itemDisplay.setBillboard(Display.Billboard.FIXED);
            itemDisplay.setViewRange(100);
            orbitingGems.add(itemDisplay);
        }

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 80; // 4 seconds

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= maxTicks) {
                    // Clean up gem entities
                    for (ItemDisplay gem : orbitingGems) {
                        if (gem != null && gem.isValid()) {
                            gem.remove();
                        }
                    }
                    orbitingGems.clear();
                    this.cancel();
                    return;
                }

                // Update positions of all 8 gem items
                for (int i = 0; i < orbitingGems.size(); i++) {
                    ItemDisplay gem = orbitingGems.get(i);
                    if (gem == null || !gem.isValid()) continue;

                    double angleOffset = (i / 8.0) * 2 * Math.PI;
                    double angle = (ticks / 20.0) * Math.PI + angleOffset; // 2 rotations in 4s
                    double radius = 2.5;
                    double height = 1.5 + Math.sin(ticks / 10.0) * 0.3; // Gentle bob

                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location newLoc = player.getLocation().clone().add(x, height, z);
                    gem.teleport(newLoc);

                    // Rotate gem for visual effect
                    float rotation = (ticks * 5) % 360;
                    Transformation transform = new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f((float) Math.toRadians(rotation), 0, 1, 0),
                        new Vector3f(1.2f, 1.2f, 1.2f),
                        new AxisAngle4f(0, 0, 0, 1)
                    );
                    gem.setTransformation(transform);
                }

                // Sound every half second
                if (ticks % 10 == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.0f + (ticks / 80.0f));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Phase 1: Selection and divergence - winner goes to player, others fall (4-6 seconds)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Location playerCenter = player.getLocation().add(0, 1.5, 0);
            List<ItemDisplay> selectionGems = new ArrayList<>();

            // Spawn fresh gem entities for selection phase
            for (int i = 0; i < allGems.length; i++) {
                String gemItemId = getGemItemId(allGems[i]);
                ItemStack gemItem = CustomItemManager.getItemById(gemItemId); // Use base texture like strength gem
                if (gemItem == null) continue; // Skip if gem item creation fails

                double angleOffset = (i / 8.0) * 2 * Math.PI;
                double startAngle = (4.0) * Math.PI + angleOffset;
                double radius = 2.5;
                double height = 1.5;

                double x = Math.cos(startAngle) * radius;
                double z = Math.sin(startAngle) * radius;
                Location startLoc = player.getLocation().clone().add(x, height, z);

                ItemDisplay itemDisplay = player.getWorld().spawn(startLoc, ItemDisplay.class);
                itemDisplay.setItemStack(gemItem);
                itemDisplay.setBillboard(Display.Billboard.FIXED);
                itemDisplay.setViewRange(100);
                selectionGems.add(itemDisplay);
            }

            new BukkitRunnable() {
                int ticks = 0;
                final int maxTicks = 40; // 2 seconds

                @Override
                public void run() {
                    if (!player.isOnline() || ticks >= maxTicks) {
                        // Clean up selection gem entities
                        for (ItemDisplay gem : selectionGems) {
                            if (gem != null && gem.isValid()) {
                                gem.remove();
                            }
                        }
                        selectionGems.clear();
                        this.cancel();
                        return;
                    }

                    double progress = ticks / (double) maxTicks;

                    for (int i = 0; i < selectionGems.size(); i++) {
                        ItemDisplay gem = selectionGems.get(i);
                        if (gem == null || !gem.isValid()) continue;

                        GemType currentGem = allGems[i];

                        // Starting orbit position (where phase 0 ended)
                        double angleOffset = (i / 8.0) * 2 * Math.PI;
                        double startAngle = (4.0) * Math.PI + angleOffset;
                        double startRadius = 2.5;
                        double startHeight = 1.5;

                        Location startLoc = player.getLocation().add(
                            Math.cos(startAngle) * startRadius,
                            startHeight,
                            Math.sin(startAngle) * startRadius
                        );

                        Location targetLoc;
                        if (currentGem == gemType) {
                            // Winner gem: move to player center
                            targetLoc = playerCenter.clone();
                        } else {
                            // Loser gems: move down and outward
                            targetLoc = startLoc.clone().add(0, -3.0, 0);
                        }

                        // Interpolate position
                        Location currentLoc = startLoc.clone().add(
                            (targetLoc.getX() - startLoc.getX()) * progress,
                            (targetLoc.getY() - startLoc.getY()) * progress,
                            (targetLoc.getZ() - startLoc.getZ()) * progress
                        );
                        gem.teleport(currentLoc);

                        // Rotate winner gem faster
                        float rotation = currentGem == gemType ? (ticks * 15) % 360 : (ticks * 3) % 360;
                        Transformation transform = new Transformation(
                            new Vector3f(0, 0, 0),
                            new AxisAngle4f((float) Math.toRadians(rotation), 0, 1, 0),
                            currentGem == gemType ? new Vector3f(1.5f, 1.5f, 1.5f) : new Vector3f(1.0f, 1.0f, 1.0f),
                            new AxisAngle4f(0, 0, 0, 1)
                        );
                        gem.setTransformation(transform);

                        // Particle effect for winner only
                        if (currentGem == gemType) {
                            Color color = getGemColor(currentGem);
                            player.getWorld().spawnParticle(Particle.FIREWORK, currentLoc, 3, 0.1, 0.1, 0.1, 0.05);
                            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, currentLoc, 2, 0.1, 0.1, 0.1, 0.01);
                        }
                    }

                    // Sound progression
                    if (ticks % 5 == 0) {
                        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE,
                            0.7f, 1.0f + (float) progress);
                    }

                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);

            // Final selection sound
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.8f);
            }, 40L);

        }, 80L); // Start after 4-second orbit

        // Phase 2: Ground circle formation (6-7 seconds)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
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
        }, 120L);

        // Phase 3: Spiral totem effect (7-9 seconds)
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
        }, 140L);

        // Phase 4: Explosion and convergence (9-10 seconds)
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

        }, 180L);

        // Phase 5: Final burst at player (10 seconds)
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

        }, 200L);

        // Phase 6: Lingering particles (10-12 seconds)
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

        }, 200L);
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
     * Get the custom item ID for displaying a gem (Tier 1)
     */
    private String getGemItemId(GemType gemType) {
        return switch (gemType) {
            case ASTRA -> "astra_gem_t1";
            case FIRE -> "fire_gem_t1";
            case FLUX -> "flux_gem_t1";
            case LIFE -> "life_gem_t1";
            case PUFF -> "puff_gem_t1";
            case SPEED -> "speed_gem_t1";
            case STRENGTH -> "strength_gem_t1";
            case WEALTH -> "wealth_gem_t1";
        };
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
