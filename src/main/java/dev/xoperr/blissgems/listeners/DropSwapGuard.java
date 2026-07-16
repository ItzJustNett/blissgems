package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Anti-dupe for the "drop-and-swap" ghost duplication (a server/protocol desync where
 * dropping an item AND changing hotbar slot on the SAME tick leaves a usable duplicate).
 *
 * Fix: detect the two actions colliding on one tick and cancel whichever lands second,
 * breaking the combo. Non-destructive — one action always goes through:
 *   - drop then same-tick swap  → drop happens, the swap is cancelled
 *   - swap then same-tick drop  → swap happens, the drop is cancelled (item stays in inv)
 * Normal play (a drop OR a swap alone on a tick) is never affected, and this covers ALL
 * items, not just BlissGems ones.
 */
public class DropSwapGuard implements Listener {

    private final BlissGems plugin;
    /** Server tick counter (own, since the compile-time API lacks getCurrentTick). */
    private volatile long tick = 0L;
    private final Map<UUID, Long> lastDropTick = new HashMap<>();
    private final Map<UUID, Long> lastHeldTick = new HashMap<>();

    public DropSwapGuard(BlissGems plugin) {
        this.plugin = plugin;
    }

    /** Start the tick counter. Call from onEnable after registering the listener. */
    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                tick++;
                // Periodically drop stale per-player entries (only same-tick matters).
                if (tick % 200L == 0L) {
                    lastDropTick.entrySet().removeIf(e -> tick - e.getValue() > 5);
                    lastHeldTick.entrySet().removeIf(e -> tick - e.getValue() > 5);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Long dropped = lastDropTick.get(id);
        if (dropped != null && dropped == tick) {
            // A drop already happened this tick — cancel the paired swap to break the combo.
            event.setCancelled(true);
            resync(event.getPlayer());
            return;
        }
        lastHeldTick.put(id, tick);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Long held = lastHeldTick.get(id);
        if (held != null && held == tick) {
            // A slot-swap already happened this tick — cancel the drop; the item stays put.
            event.setCancelled(true);
            resync(event.getPlayer());
            return;
        }
        lastDropTick.put(id, tick);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lastDropTick.remove(id);
        lastHeldTick.remove(id);
    }

    private void resync(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, player::updateInventory);
    }
}
