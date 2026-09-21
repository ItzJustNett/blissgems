/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Display$Brightness
 *  org.bukkit.entity.ItemDisplay
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.util.Transformation
 *  org.joml.AxisAngle4f
 *  org.joml.Vector3f
 */
package dev.xoperr.blissgems.core.managers;

import dev.xoperr.blissgems.core.api.particle.CustomParticle;
import dev.xoperr.blissgems.core.api.particle.ParticleBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class ParticleManager {
    private final Plugin plugin;
    private final List<CustomParticle> activeParticles;

    public ParticleManager(Plugin plugin) {
        this.plugin = plugin;
        this.activeParticles = new ArrayList<CustomParticle>();
        this.startCleanupTask();
    }

    public CustomParticle spawnParticle(ParticleBuilder builder, Location location) {
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("Location and world cannot be null");
        }
        ItemDisplay display = (ItemDisplay)location.getWorld().spawn(location, ItemDisplay.class, entity -> {
            entity.setItemStack(builder.buildItemStack());
            entity.setBillboard(builder.getBillboard());
            entity.setBrightness(new Display.Brightness(builder.getBlockLight(), builder.getSkyLight()));
            entity.setViewRange(builder.getViewRange());
            entity.setInterpolationDuration(builder.getInterpolationDuration());
            entity.setInterpolationDelay(builder.getInterpolationDelay());
            float scale = builder.getScale();
            entity.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), new AxisAngle4f(0.0f, 0.0f, 0.0f, 1.0f), new Vector3f(scale, scale, scale), new AxisAngle4f(0.0f, 0.0f, 0.0f, 1.0f)));
        });
        CustomParticle particle = new CustomParticle(display, builder.getParticleId());
        this.activeParticles.add(particle);
        return particle;
    }

    public void removeAllParticles() {
        for (CustomParticle particle : new ArrayList<CustomParticle>(this.activeParticles)) {
            particle.remove();
        }
        this.activeParticles.clear();
    }

    public int getActiveParticleCount() {
        this.cleanupInvalidParticles();
        return this.activeParticles.size();
    }

    public void cleanup() {
        this.removeAllParticles();
    }

    private void startCleanupTask() {
        this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, this::cleanupInvalidParticles, 100L, 100L);
    }

    private void cleanupInvalidParticles() {
        Iterator<CustomParticle> iterator = this.activeParticles.iterator();
        while (iterator.hasNext()) {
            CustomParticle particle = iterator.next();
            if (particle.isActive()) continue;
            iterator.remove();
        }
    }
}

