package dev.xoperr.blissgems.core.api.particle;

import dev.xoperr.blissgems.core.managers.ParticleManager;
import org.bukkit.Location;

/**
 * Public API for creating and managing custom particles using Display entities.
 * Supports custom models from resource packs.
 *
 * Usage example:
 * <pre>
 * // Create a custom particle
 * CustomParticle particle = ParticleAPI.create("flame_burst")
 *     .material(Material.PAPER)
 *     .customModelData(1001)
 *     .scale(0.8f)
 *     .brightness(15, 15)
 *     .spawn(location);
 *
 * // Move the particle
 * particle.teleport(newLocation);
 *
 * // Remove the particle
 * particle.remove();
 * </pre>
 */
public class ParticleAPI {

    private static ParticleManager manager;

    /**
     * Initialize the API with a ParticleManager instance.
     * This is called internally by XoperrCore on plugin enable.
     *
     * @param particleManager The manager instance
     */
    public static void initialize(ParticleManager particleManager) {
        manager = particleManager;
    }

    /**
     * Create a new particle builder.
     *
     * @param particleId Unique identifier for this particle type
     * @return A ParticleBuilder for configuring and spawning the particle
     */
    public static ParticleBuilder create(String particleId) {
        checkInitialized();
        return new ParticleBuilder(particleId);
    }

    /**
     * Internal method called by ParticleBuilder to spawn the particle.
     */
    static CustomParticle spawnParticle(ParticleBuilder builder, Location location) {
        checkInitialized();
        return manager.spawnParticle(builder, location);
    }

    /**
     * Remove all active particles.
     */
    public static void removeAllParticles() {
        checkInitialized();
        manager.removeAllParticles();
    }

    /**
     * Get the number of active particles.
     */
    public static int getActiveParticleCount() {
        checkInitialized();
        return manager.getActiveParticleCount();
    }

    private static void checkInitialized() {
        if (manager == null) {
            throw new IllegalStateException("ParticleAPI has not been initialized! Ensure XoperrCore is loaded.");
        }
    }
}
