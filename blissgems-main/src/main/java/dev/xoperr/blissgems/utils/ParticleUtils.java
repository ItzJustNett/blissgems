package dev.xoperr.blissgems.utils;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * Particle utility class containing all gem particle effects
 * RGB values and patterns are from the Skript implementation research
 */
public class ParticleUtils {

    // ===== GEM COLORS (from research/COMPLETE_GEM_ABILITIES_PARTICLES.md) =====

    // ASTRA - Deep Purple
    public static final Color ASTRA_PURPLE = Color.fromRGB(106, 11, 184);

    // FIRE - Bright Orange
    public static final Color FIRE_ORANGE = Color.fromRGB(255, 119, 0);

    // FLUX - Cyan/Electric Blue
    public static final Color FLUX_CYAN = Color.fromRGB(94, 215, 255);
    public static final Color FLUX_DARK_CYAN = Color.fromRGB(16, 131, 173); // Ground Stun

    // LIFE - Pink/Magenta
    public static final Color LIFE_PINK = Color.fromRGB(255, 0, 180);
    public static final Color LIFE_PINK_ALT = Color.fromRGB(255, 0, 179); // Circle of Life

    // PUFF - Pure White
    public static final Color PUFF_WHITE = Color.fromRGB(255, 255, 255);

    // SPEED - Bright Yellow/Lime
    public static final Color SPEED_YELLOW = Color.fromRGB(244, 255, 28);

    // STRENGTH - Deep Red
    public static final Color STRENGTH_RED = Color.fromRGB(199, 0, 10);

    // WEALTH - Bright Green
    public static final Color WEALTH_GREEN = Color.fromRGB(0, 166, 44);

    // ===== PARTICLE DRAWING METHODS =====

    /**
     * Draws a horizontal circle of dust particles with specified color and size
     * @param location Center location
     * @param radius Circle radius in blocks
     * @param color RGB color
     * @param size Particle size (typically 0.9-1.5)
     * @param density Number of particles in circle (default 32 for smooth circle)
     */
    public static void drawColoredCircle(Location location, double radius, Color color, float size, int density) {
        World world = location.getWorld();
        if (world == null) return;

        Particle.DustOptions dustOptions = new Particle.DustOptions(color, size);

        for (int i = 0; i < density; i++) {
            double angle = (i / (double) density) * 2 * Math.PI;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            Location particleLoc = location.clone().add(x, 0, z);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dustOptions, true);
        }
    }

    /**
     * Draws expanding circles animation (used by most gems)
     * Draws two circles per tick with increasing radii
     * @param location Center location
     * @param color RGB color
     * @param size Particle size
     * @param maxRadius Maximum radius (usually 3-6 blocks)
     */
    public static void drawExpandingCircles(Location location, Color color, float size, double maxRadius, Runnable onComplete) {
        double[] radii = generateRadiiArray(maxRadius);

        // Animation runs over multiple ticks
        new org.bukkit.scheduler.BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (index >= radii.length) {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    this.cancel();
                    return;
                }

                // Draw two circles per tick for smooth expansion
                drawColoredCircle(location, radii[index], color, size, 32);
                if (index + 1 < radii.length) {
                    drawColoredCircle(location, radii[index + 1], color, size, 32);
                    index += 2;
                } else {
                    index++;
                }
            }
        }.runTaskTimer(org.bukkit.Bukkit.getPluginManager().getPlugin("BlissGems"), 0L, 1L);
    }

    /**
     * Generates array of radii for expanding circle animation
     * Pattern: 0.75 → 1 → 1.25 → 1.5 → ... → maxRadius
     */
    private static double[] generateRadiiArray(double maxRadius) {
        int steps = (int) ((maxRadius - 0.75) / 0.25) + 1;
        double[] radii = new double[steps];
        for (int i = 0; i < steps; i++) {
            radii[i] = 0.75 + (i * 0.25);
        }
        return radii;
    }

    /**
     * Draws a line of dust particles between two locations
     * Used for energy beams, tethers, and connections
     * @param from Starting location
     * @param to Ending location
     * @param color RGB color
     * @param size Particle size
     * @param density Particles per block (default 10)
     */
    public static void drawColoredLine(Location from, Location to, Color color, float size, int density) {
        World world = from.getWorld();
        if (world == null) return;

        Particle.DustOptions dustOptions = new Particle.DustOptions(color, size);

        double distance = from.distance(to);
        int points = (int) (distance * density);

        for (int i = 0; i <= points; i++) {
            double ratio = (double) i / points;
            double x = from.getX() + (to.getX() - from.getX()) * ratio;
            double y = from.getY() + (to.getY() - from.getY()) * ratio;
            double z = from.getZ() + (to.getZ() - from.getZ()) * ratio;

            Location particleLoc = new Location(world, x, y, z);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dustOptions, true);
        }
    }

    /**
     * Draws a pulsing line that redraws multiple times
     * Used for Life gem drains, Flux ground stun, etc.
     * @param from Starting location
     * @param to Ending location
     * @param color RGB color
     * @param size Particle size
     * @param iterations Number of times to redraw (e.g., 50 for 5 seconds at 10 FPS)
     * @param ticksBetween Ticks between each draw (2 ticks = 10 FPS)
     */
    public static void drawPulsingLine(Location from, Location to, Color color, float size, int iterations, int ticksBetween, Runnable onComplete) {
        new org.bukkit.scheduler.BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= iterations) {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    this.cancel();
                    return;
                }

                drawColoredLine(from, to, color, size, 10);
                count++;
            }
        }.runTaskTimer(org.bukkit.Bukkit.getPluginManager().getPlugin("BlissGems"), 0L, ticksBetween);
    }

    /**
     * Draws a multi-layered dome effect (Astra Void)
     * Creates concentric circles at different heights
     * @param location Center location
     * @param color RGB color
     * @param size Particle size
     * @param maxRadius Maximum radius at ground level
     */
    public static void drawDome(Location location, Color color, float size, double maxRadius) {
        // Layer structure (from research):
        // Ground: radius 6
        // +1 block: radius 5
        // +2 blocks: radius 4
        // +3 blocks: radius 3
        // +4 blocks: radius 2
        // +5 blocks: radius 1

        double[] layerRadii = {maxRadius, maxRadius - 1, maxRadius - 2, maxRadius - 3, maxRadius - 4, maxRadius - 5};

        for (int i = 0; i < layerRadii.length; i++) {
            if (layerRadii[i] <= 0) continue;
            Location layerLoc = location.clone().add(0, i, 0);
            drawColoredCircle(layerLoc, layerRadii[i], color, size, 48);
        }
    }

    /**
     * Draws a spiral of particles going upward (charging effects)
     * @param location Center location (player position)
     * @param color RGB color
     * @param size Particle size
     * @param height Total height in blocks
     * @param radius Spiral radius
     * @param rotationOffset Offset for animated rotation
     */
    public static void drawSpiral(Location location, Color color, float size, double height, double radius, double rotationOffset) {
        World world = location.getWorld();
        if (world == null) return;

        Particle.DustOptions dustOptions = new Particle.DustOptions(color, size);

        int points = (int) (height * 8); // 8 particles per block height

        for (int i = 0; i < points; i++) {
            double heightOffset = (i / (double) points) * height;
            double angle = (i / (double) points) * 4 * Math.PI + rotationOffset; // 2 full rotations

            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            Location particleLoc = location.clone().add(x, heightOffset, z);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dustOptions, true);
        }
    }

    /**
     * Draws a ring of particles at a specific height (charging effects)
     * @param location Center location
     * @param color RGB color
     * @param size Particle size
     * @param radius Ring radius
     * @param heightOffset Y offset from location
     * @param density Number of particles in ring
     */
    public static void drawRing(Location location, Color color, float size, double radius, double heightOffset, int density) {
        World world = location.getWorld();
        if (world == null) return;

        Particle.DustOptions dustOptions = new Particle.DustOptions(color, size);

        for (int i = 0; i < density; i++) {
            double angle = (i / (double) density) * 2 * Math.PI;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            Location particleLoc = location.clone().add(x, heightOffset, z);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dustOptions, true);
        }
    }

    /**
     * Draws a beam with circular cross-section (Flux Ray)
     * Creates an animated beam effect with tapering
     * @param from Starting location
     * @param direction Direction vector
     * @param length Beam length in blocks
     * @param startRadius Starting radius of beam
     * @param particleType Particle type (SONIC_BOOM for Flux)
     */
    public static void drawBeam(Location from, Vector direction, double length, double startRadius, Particle particleType) {
        World world = from.getWorld();
        if (world == null) return;

        direction = direction.normalize();

        int segments = 25; // Number of segments along beam
        int pointsPerCircle = 8; // Points in each cross-section circle
        double segmentLength = length / segments;

        for (int seg = 0; seg < segments; seg++) {
            // Calculate position along beam
            Location segmentLoc = from.clone().add(direction.clone().multiply(seg * segmentLength));

            // Calculate tapering radius
            double radius = startRadius * (1.0 - (seg / (double) segments));
            if (radius < 0.5) radius = 0.5; // Minimum radius

            // Draw circular cross-section
            for (int i = 0; i < pointsPerCircle; i++) {
                double angle = (i / (double) pointsPerCircle) * 2 * Math.PI;

                // Calculate perpendicular vectors
                Vector perpendicular1 = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                Vector perpendicular2 = direction.clone().crossProduct(perpendicular1).normalize();

                // Calculate point on circle
                Vector offset = perpendicular1.clone().multiply(Math.cos(angle) * radius)
                    .add(perpendicular2.clone().multiply(Math.sin(angle) * radius));

                Location particleLoc = segmentLoc.clone().add(offset);
                world.spawnParticle(particleType, particleLoc, 3, 0, 0, 0, 0.000000001, null, true);
            }
        }
    }

    /**
     * Creates a particle trail behind moving entity (Puff double jump)
     * @param location Current location
     * @param particleType Particle type (CLOUD for Puff)
     * @param count Number of particles
     */
    public static void drawTrail(Location location, Particle particleType, int count) {
        World world = location.getWorld();
        if (world == null) return;

        world.spawnParticle(particleType, location, count, 0.1, 0.1, 0.1, 0, null, true);
    }
}
