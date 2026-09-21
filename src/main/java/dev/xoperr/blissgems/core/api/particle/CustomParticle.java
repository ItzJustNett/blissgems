/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Display$Brightness
 *  org.bukkit.entity.ItemDisplay
 *  org.bukkit.util.Transformation
 *  org.joml.Vector3f
 */
package dev.xoperr.blissgems.core.api.particle;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

public class CustomParticle {
    private final ItemDisplay display;
    private final String particleId;
    private boolean isActive;

    public CustomParticle(ItemDisplay display, String particleId) {
        this.display = display;
        this.particleId = particleId;
        this.isActive = true;
    }

    public String getParticleId() {
        return this.particleId;
    }

    public boolean isActive() {
        return this.isActive && this.display != null && this.display.isValid();
    }

    public Location getLocation() {
        return this.display != null ? this.display.getLocation() : null;
    }

    public void teleport(Location location) {
        if (this.display != null && this.isActive) {
            this.display.teleport(location);
        }
    }

    public void setScale(float scale) {
        if (this.display != null && this.isActive) {
            Transformation transformation = this.display.getTransformation();
            this.display.setTransformation(new Transformation(transformation.getTranslation(), transformation.getLeftRotation(), new Vector3f(scale, scale, scale), transformation.getRightRotation()));
        }
    }

    public void setBrightness(int blockLight, int skyLight) {
        if (this.display != null && this.isActive) {
            this.display.setBrightness(new Display.Brightness(blockLight, skyLight));
        }
    }

    public void setViewRange(float range) {
        if (this.display != null && this.isActive) {
            this.display.setViewRange(range);
        }
    }

    public void setInterpolationDuration(int ticks) {
        if (this.display != null && this.isActive) {
            this.display.setInterpolationDuration(ticks);
        }
    }

    public void setInterpolationDelay(int ticks) {
        if (this.display != null && this.isActive) {
            this.display.setInterpolationDelay(ticks);
        }
    }

    public void remove() {
        if (this.display != null && this.isActive) {
            this.display.remove();
            this.isActive = false;
        }
    }

    public ItemDisplay getDisplay() {
        return this.display;
    }
}

