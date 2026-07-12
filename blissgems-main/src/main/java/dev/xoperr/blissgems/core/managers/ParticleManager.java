package dev.xoperr.blissgems.core.managers;

import org.bukkit.plugin.Plugin;
import dev.xoperr.blissgems.core.api.particle.CustomParticle;
import dev.xoperr.blissgems.core.api.particle.ParticleBuilder;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manager class handling custom particle creation and lifecycle.
 * Uses ItemDisplay entities to render custom models from resource packs.
 */
public class ParticleManager {

    private final Plugin plugin;
    private final List<CustomParticle> activeParticles;

    public ParticleManager(Plugin plugin) {
        this.plugin = plugin;
        this.activeParticles = new ArrayList<>();

        // Start cleanup task to remove invalid particles
        startCleanupTask();
    }

    /**
     * Spawn a custom particle using the provided builder configuration.
     */
    public CustomParticle spawnParticle(ParticleBuilder builder, Location location) {
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("Location and world cannot be null");
        }

        // Create the ItemDisplay entity
        ItemDisplay display = location.getWorld().spawn(location, ItemDisplay.class, entity -> {
            // Set the item with custom model data
            ItemStack item = builder.buildItemStack();
            entity.setItemStack(item);

            // Configure display properties
            entity.setBillboard(builder.getBillboard());
            entity.setBrightness(new Display.Brightness(builder.getBlockLight(), builder.getSkyLight()));
            entity.setViewRange(builder.getViewRange());
            entity.setInterpolationDuration(builder.getInterpolationDuration());
            entity.setInterpolationDelay(builder.getInterpolationDelay());

            // Set scale transformation
            float scale = builder.getScale();
            Transformation transformation = new Transformation(
                    new Vector3f(0, 0, 0), // translation
                    new AxisAngle4f(0, 0, 0, 1), // left rotation
                    new Vector3f(scale, scale, scale), // scale
                    new AxisAngle4f(0, 0, 0, 1) // right rotation
            );
            entity.setTransformation(transformation);
        });

        // Create the CustomParticle wrapper
        CustomParticle particle = new CustomParticle(display, builder.getParticleId());
        activeParticles.add(particle);

        return particle;
    }

    /**
     * Remove all active particles.
     */
    public void removeAllParticles() {
        for (CustomParticle particle : new ArrayList<>(activeParticles)) {
            particle.remove();
        }
        activeParticles.clear();
    }

    /**
     * Get the number of active particles.
     */
    public int getActiveParticleCount() {
        cleanupInvalidParticles();
        return activeParticles.size();
    }

    /**
     * Cleanup method called on plugin disable.
     */
    public void cleanup() {
        removeAllParticles();
    }

    /**
     * Start a periodic task to clean up invalid particles.
     */
    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanupInvalidParticles, 100L, 100L);
    }

    /**
     * Remove particles that are no longer valid.
     */
    private void cleanupInvalidParticles() {
        Iterator<CustomParticle> iterator = activeParticles.iterator();
        while (iterator.hasNext()) {
            CustomParticle particle = iterator.next();
            if (!particle.isActive()) {
                iterator.remove();
            }
        }
    }
}
