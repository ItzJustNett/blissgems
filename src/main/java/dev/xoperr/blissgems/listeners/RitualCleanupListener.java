/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.ItemDisplay
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.world.ChunkLoadEvent
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class RitualCleanupListener
implements Listener {
    private final BlissGems plugin;

    public RitualCleanupListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled=true)
    public void onChunkLoad(ChunkLoadEvent event) {
        int removed = 0;
        for (Entity e : event.getChunk().getEntities()) {
            if (!(e instanceof ItemDisplay) || !e.getScoreboardTags().contains("blissgems_ritual_display")) continue;
            e.remove();
            ++removed;
        }
        if (removed > 0) {
            this.plugin.getLogger().info("Removed " + removed + " orphaned ritual gem display(s) from " + event.getWorld().getName() + " [" + event.getChunk().getX() + "," + event.getChunk().getZ() + "]");
        }
    }
}

