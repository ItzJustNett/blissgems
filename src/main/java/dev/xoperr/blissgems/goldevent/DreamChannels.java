package dev.xoperr.blissgems.goldevent;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Splits chat, death and advancement messages between the dream and the real world, and keeps
 * join/quit messages out of the dream (dreamers shouldn't know who is online outside).
 */
public final class DreamChannels implements Listener {
    private static boolean dreamSide(Player p) {
        return p != null && GoldenDreamWorld.isDreamWorld(p.getWorld());
    }

    private static boolean silenced(Component c) {
        return c == null || Component.empty().equals(c);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncChatEvent event) {
        boolean side = dreamSide(event.getPlayer());
        event.viewers().removeIf(v -> v instanceof Player p && dreamSide(p) != side);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Component msg = event.deathMessage();
        if (silenced(msg)) return;
        boolean side = dreamSide(event.getEntity());
        if (!splitFrom(side)) return;
        event.deathMessage(null);
        sendSide(side, msg);
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        Component msg = event.message();
        if (silenced(msg)) return;
        boolean side = dreamSide(event.getPlayer());
        if (!splitFrom(side)) return;
        event.message(null);
        sendSide(side, msg);
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        hideFromDream(event.joinMessage(), event::joinMessage);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        hideFromDream(event.quitMessage(), event::quitMessage);
    }

    private static void hideFromDream(Component msg, Consumer<Component> setter) {
        if (silenced(msg) || !splitFrom(false)) return;
        setter.accept(null);
        sendSide(false, msg);
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    /** True if someone online is on the other side. */
    private static boolean splitFrom(boolean side) {
        for (Player p : Bukkit.getOnlinePlayers()) if (dreamSide(p) != side) return true;
        return false;
    }

    public static void sendSide(boolean dream, Component msg) {
        for (Player p : Bukkit.getOnlinePlayers()) if (dreamSide(p) == dream) p.sendMessage(msg);
    }
}
