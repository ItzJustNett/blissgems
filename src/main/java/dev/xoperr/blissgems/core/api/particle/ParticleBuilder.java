/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.entity.Display$Billboard
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package dev.xoperr.blissgems.core.api.particle;

import dev.xoperr.blissgems.core.api.particle.CustomParticle;
import dev.xoperr.blissgems.core.api.particle.ParticleAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ParticleBuilder {
    private final String particleId;
    private Material material = Material.PAPER;
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

    public ParticleBuilder material(Material material) {
        this.material = material;
        return this;
    }

    public ParticleBuilder customModelData(int customModelData) {
        this.customModelData = customModelData;
        return this;
    }

    public ParticleBuilder scale(float scale) {
        this.scale = scale;
        return this;
    }

    public ParticleBuilder brightness(int blockLight, int skyLight) {
        this.blockLight = Math.max(0, Math.min(15, blockLight));
        this.skyLight = Math.max(0, Math.min(15, skyLight));
        return this;
    }

    public ParticleBuilder viewRange(float viewRange) {
        this.viewRange = viewRange;
        return this;
    }

    public ParticleBuilder interpolationDuration(int ticks) {
        this.interpolationDuration = ticks;
        return this;
    }

    public ParticleBuilder interpolationDelay(int ticks) {
        this.interpolationDelay = ticks;
        return this;
    }

    public ParticleBuilder billboard(Display.Billboard billboard) {
        this.billboard = billboard;
        return this;
    }

    public CustomParticle spawn(Location location) {
        return ParticleAPI.spawnParticle(this, location);
    }

    public String getParticleId() {
        return this.particleId;
    }

    public Material getMaterial() {
        return this.material;
    }

    public int getCustomModelData() {
        return this.customModelData;
    }

    public float getScale() {
        return this.scale;
    }

    public int getBlockLight() {
        return this.blockLight;
    }

    public int getSkyLight() {
        return this.skyLight;
    }

    public float getViewRange() {
        return this.viewRange;
    }

    public int getInterpolationDuration() {
        return this.interpolationDuration;
    }

    public int getInterpolationDelay() {
        return this.interpolationDelay;
    }

    public Display.Billboard getBillboard() {
        return this.billboard;
    }

    public ItemStack buildItemStack() {
        ItemMeta meta;
        ItemStack item = new ItemStack(this.material);
        if (this.customModelData > 0 && (meta = item.getItemMeta()) != null) {
            meta.setCustomModelData(Integer.valueOf(this.customModelData));
            item.setItemMeta(meta);
        }
        return item;
    }
}

