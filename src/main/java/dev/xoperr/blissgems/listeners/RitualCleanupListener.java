package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.managers.GemRitualManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * Removes orphaned gem-ritual display entities (tagged {@link GemRitualManager#RITUAL_TAG})
 * as their chunks load. Ritual displays that were leaked while their chunk was unloaded
 * — e.g. a player who first-joined and insta-rejoined mid-ritual — can't be cleaned up by
 * the ritual's own runnables (isValid() is false in an unloaded chunk), so we catch them
 * here the moment the chunk comes back, plus once on enable for already-loaded chunks.
 */
public class RitualCleanupListener implements Listener {

    private final BlissGems plugin;

    public RitualCleanupListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        int removed = 0;
        for (Entity e : event.getChunk().getEntities()) {
            if (e instanceof ItemDisplay && e.getScoreboardTags().contains(GemRitualManager.RITUAL_TAG)) {
                e.remove();
                removed++;
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("Removed " + removed + " orphaned ritual gem display(s) from "
                + event.getWorld().getName() + " [" + event.getChunk().getX() + "," + event.getChunk().getZ() + "]");
        }
    }
}
