package dev.xoperr.blissgems.core.api.particle;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

/**
 * Represents a custom particle created using Display entities.
 * Can be animated, moved, and configured with custom models from resource packs.
 */
public class CustomParticle {

    private final ItemDisplay display;
    private final String particleId;
    private boolean isActive;

    public CustomParticle(ItemDisplay display, String particleId) {
        this.display = display;
        this.particleId = particleId;
        this.isActive = true;
    }

    /**
     * Get the particle ID.
     */
    public String getParticleId() {
        return particleId;
    }

    /**
     * Check if the particle is still active.
     */
    public boolean isActive() {
        return isActive && display != null && display.isValid();
    }

    /**
     * Get the current location of the particle.
     */
    public Location getLocation() {
        return display != null ? display.getLocation() : null;
    }

    /**
     * Teleport the particle to a new location.
     */
    public void teleport(Location location) {
        if (display != null && isActive) {
            display.teleport(location);
        }
    }

    /**
     * Set the scale of the particle.
     *
     * @param scale Scale factor (1.0 = normal size)
     */
    public void setScale(float scale) {
        if (display != null && isActive) {
            Transformation transformation = display.getTransformation();
            display.setTransformation(new Transformation(
                    transformation.getTranslation(),
                    transformation.getLeftRotation(),
                    new Vector3f(scale, scale, scale),
                    transformation.getRightRotation()
            ));
        }
    }

    /**
     * Set the brightness of the particle.
     *
     * @param blockLight Block light level (0-15)
     * @param skyLight Sky light level (0-15)
     */
    public void setBrightness(int blockLight, int skyLight) {
        if (display != null && isActive) {
            display.setBrightness(new Display.Brightness(blockLight, skyLight));
        }
    }

    /**
     * Set the view range of the particle.
     *
     * @param range View range in blocks
     */
    public void setViewRange(float range) {
        if (display != null && isActive) {
            display.setViewRange(range);
        }
    }

    /**
     * Set the interpolation duration for smooth animations.
     *
     * @param ticks Duration in ticks
     */
    public void setInterpolationDuration(int ticks) {
        if (display != null && isActive) {
            display.setInterpolationDuration(ticks);
        }
    }

    /**
     * Set the interpolation delay.
     *
     * @param ticks Delay in ticks
     */
    public void setInterpolationDelay(int ticks) {
        if (display != null && isActive) {
            display.setInterpolationDelay(ticks);
        }
    }

    /**
     * Remove the particle from the world.
     */
    public void remove() {
        if (display != null && isActive) {
            display.remove();
            isActive = false;
        }
    }

    /**
     * Get the underlying ItemDisplay entity.
     * Use this for advanced customization.
     */
    public ItemDisplay getDisplay() {
        return display;
    }
}
