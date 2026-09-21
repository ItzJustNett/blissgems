/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 */
package dev.xoperr.blissgems.core.api.particle;

import dev.xoperr.blissgems.core.api.particle.CustomParticle;
import dev.xoperr.blissgems.core.api.particle.ParticleBuilder;
import dev.xoperr.blissgems.core.managers.ParticleManager;
import org.bukkit.Location;

public class ParticleAPI {
    private static ParticleManager manager;

    public static void initialize(ParticleManager particleManager) {
        manager = particleManager;
    }

    public static ParticleBuilder create(String particleId) {
        ParticleAPI.checkInitialized();
        return new ParticleBuilder(particleId);
    }

    static CustomParticle spawnParticle(ParticleBuilder builder, Location location) {
        ParticleAPI.checkInitialized();
        return manager.spawnParticle(builder, location);
    }

    public static void removeAllParticles() {
        ParticleAPI.checkInitialized();
        manager.removeAllParticles();
    }

    public static int getActiveParticleCount() {
        ParticleAPI.checkInitialized();
        return manager.getActiveParticleCount();
    }

    private static void checkInitialized() {
        if (manager == null) {
            throw new IllegalStateException("ParticleAPI has not been initialized! Ensure XoperrCore is loaded.");
        }
    }
}

