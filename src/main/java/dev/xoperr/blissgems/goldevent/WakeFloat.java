package dev.xoperr.blissgems.goldevent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * The end of the dream: every dreamer who isn't a memory floats slowly upward for 20 blocks,
 * then wakes (their real self restored) and is kicked with "Wake Up.".
 */
public final class WakeFloat {
    private static final double RISE = 0.07;
    private static final double HEIGHT = 20.0;
    private static final Map<UUID, Location> FROM = new HashMap<>();
    private static BukkitTask task;

    private WakeFloat() {
    }

    public static int start(Plugin plugin, GoldenDream dream) {
        int added = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (FROM.containsKey(p.getUniqueId()) || !GoldenDreamWorld.isDreamWorld(p.getWorld()) || p.hasMetadata("NPC")) continue;
            if (dream.roster().isMemory(p.getUniqueId()) || p.isDead()) continue;
            FROM.put(p.getUniqueId(), p.getLocation().clone());
            p.setGravity(false);
            p.setAllowFlight(true);
            added++;
        }
        if (FROM.isEmpty() || task != null) return added;
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Iterator<Map.Entry<UUID, Location>> it = FROM.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, Location> e = it.next();
                Player p = Bukkit.getPlayer(e.getKey());
                if (p == null || !p.isOnline()) {
                    it.remove();
                    continue;
                }
                if (p.isDead() || !GoldenDreamWorld.isDreamWorld(p.getWorld())) {
                    ground(p);
                    it.remove();
                    continue;
                }
                if (p.getLocation().getY() < e.getValue().getY() + HEIGHT) {
                    p.setVelocity(new Vector(0, RISE, 0));
                    continue;
                }
                ground(p);
                if (dream.world().inDream(p.getUniqueId())) {
                    dream.world().wake(p);
                } else if (e.getValue().isWorldLoaded()) {
                    p.setVelocity(new Vector());
                    p.teleport(e.getValue());
                    p.setFallDistance(0);
                }
                p.kick(Component.text("Wake Up."));
                it.remove();
            }
            if (FROM.isEmpty() && task != null) {
                task.cancel();
                task = null;
            }
        }, 0L, 1L);
        return added;
    }

    private static void ground(Player p) {
        p.setGravity(true);
        if (p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) p.setAllowFlight(false);
    }

    public static void shutdown() {
        for (UUID id : FROM.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) ground(p);
        }
        FROM.clear();
        if (task != null) task.cancel();
        task = null;
    }
}
