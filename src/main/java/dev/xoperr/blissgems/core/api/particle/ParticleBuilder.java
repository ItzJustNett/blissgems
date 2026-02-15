package dev.xoperr.blissgems.core.api.particle;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.inventory.ItemStack;

/**
 * Builder class for creating custom particles with a fluent API.
 *
 * Example usage:
 * <pre>
 * CustomParticle particle = ParticleAPI.create("flame_burst")
 *     .material(Material.DIAMOND)
 *     .customModelData(1001)
 *     .scale(0.5f)
 *     .brightness(15, 15)
 *     .viewRange(64.0f)
 *     .spawn(location);
 * </pre>
 */
public class ParticleBuilder {

    private final String particleId;
    private Material material = Material.PAPER; // Default material for custom models
    private int customModelData = 0;
    private float scale = 1.0f;
    private int blockLight = 15;
    private int skyLight = 15;
    private float viewRange = 32.0f;
    private int interpolationDuration = 0;
    private int interpolationDelay = 0;
    private Display.Billboard billboard = Display.Billboard.CENTER;

    public ParticleBuilder(String particleId) {
        this.particleId = particleId;
    }

    /**
     * Set the material for the particle item.
     * This is the base material that will use the custom model data.
     */
    public ParticleBuilder material(Material material) {
        this.material = material;
        return this;
    }

    /**
     * Set the custom model data from your resource pack.
     * This determines which model is displayed.
     */
    public ParticleBuilder customModelData(int customModelData) {
        this.customModelData = customModelData;
        return this;
    }

    /**
     * Set the scale of the particle.
     *
     * @param scale Scale factor (1.0 = normal size, 0.5 = half size, 2.0 = double size)
     */
    public ParticleBuilder scale(float scale) {
        this.scale = scale;
        return this;
    }

    /**
     * Set the brightness of the particle.
     *
     * @param blockLight Block light level (0-15)
     * @param skyLight Sky light level (0-15)
     */
    public ParticleBuilder brightness(int blockLight, int skyLight) {
        this.blockLight = Math.max(0, Math.min(15, blockLight));
        this.skyLight = Math.max(0, Math.min(15, skyLight));
        return this;
    }

    /**
     * Set the view range of the particle in blocks.
     * Players beyond this distance won't see the particle.
     */
    public ParticleBuilder viewRange(float viewRange) {
        this.viewRange = viewRange;
        return this;
    }

    /**
     * Set the interpolation duration for smooth animations.
     *
     * @param ticks Duration in ticks (20 ticks = 1 second)
     */
    public ParticleBuilder interpolationDuration(int ticks) {
        this.interpolationDuration = ticks;
        return this;
    }

    /**
     * Set the interpolation delay before animations start.
     *
     * @param ticks Delay in ticks
     */
    public ParticleBuilder interpolationDelay(int ticks) {
        this.interpolationDelay = ticks;
        return this;
    }

    /**
     * Set the billboard mode (how the particle faces the player).
     */
    public ParticleBuilder billboard(Display.Billboard billboard) {
        this.billboard = billboard;
        return this;
    }

    /**
     * Spawn the particle at the specified location.
     *
     * @param location The location to spawn the particle
     * @return The created CustomParticle instance
     */
    public CustomParticle spawn(Location location) {
        return ParticleAPI.spawnParticle(this, location);
    }

    // Getters for ParticleManager to access builder values

    public String getParticleId() {
        return particleId;
    }

    public Material getMaterial() {
        return material;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public float getScale() {
        return scale;
    }

    public int getBlockLight() {
        return blockLight;
    }

    public int getSkyLight() {
        return skyLight;
    }

    public float getViewRange() {
        return viewRange;
    }

    public int getInterpolationDuration() {
        return interpolationDuration;
    }

    public int getInterpolationDelay() {
        return interpolationDelay;
    }

    public Display.Billboard getBillboard() {
        return billboard;
    }

    public ItemStack buildItemStack() {
        ItemStack item = new ItemStack(material);
        if (customModelData > 0) {
            item.editMeta(meta -> meta.setCustomModelData(customModelData));
        }
        return item;
    }
}
