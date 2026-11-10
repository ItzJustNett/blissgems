package dev.xoperr.blissgems.lore;

import dev.xoperr.blissgems.BlissGems;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * The Wisps of Bliss lore: the Prophet, Manifestation and Balance. Staff can take one on
 * ({@code /bliss wisp <prophet|manifestation|balance|off>}): every other player stops seeing
 * them at all (no body, name tag or tab entry), only the wisp's light with its name above it,
 * and everything they say in chat is spoken as the wisp. {@code /bliss wispsay <wisp> <message>} speaks as one without morphing
 * (works from the console too).
 */
public final class Wisps implements Listener {
    public enum Wisp {
        PROPHET("The Prophet", TextColor.color(0xFFD24A)),
        MANIFESTATION("Manifestation", TextColor.color(0xB45CFF)),
        BALANCE("Balance", TextColor.color(0xE8E8E8));

        final String title;
        final TextColor color;

        Wisp(String title, TextColor color) {
            this.title = title;
            this.color = color;
        }

        static Wisp parse(String s) {
            try {
                return Wisp.valueOf(s.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private final BlissGems plugin;
    private final Map<UUID, Wisp> morphed = new ConcurrentHashMap<>();
    private final Map<UUID, TextDisplay> labels = new HashMap<>();

    public Wisps(BlissGems plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 2L);
    }

    public void handleMorph(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§7/bliss wisp <prophet|manifestation|balance|off>");
            return;
        }
        if (args[1].equalsIgnoreCase("off")) {
            this.unmorph(player);
            player.sendMessage("§7You are yourself again.");
            return;
        }
        Wisp wisp = Wisp.parse(args[1]);
        if (wisp == null) {
            player.sendMessage("§cUnknown wisp. prophet, manifestation or balance.");
            return;
        }
        this.unmorph(player);
        this.morphed.put(player.getUniqueId(), wisp);
        // a wisp has no user: hidden from everyone else (body, name tag and tab list)
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) viewer.hidePlayer(this.plugin, player);
        }
        TextDisplay label = player.getWorld().spawn(player.getLocation().add(0, 2.2, 0), TextDisplay.class, d -> {
            d.text(Component.text(wisp.title, wisp.color).decorate(TextDecoration.BOLD));
            d.setBillboard(Display.Billboard.CENTER);
            d.setPersistent(false);
            d.setSeeThrough(true);
            d.setTeleportDuration(2);
        });
        this.labels.put(player.getUniqueId(), label);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.8f);
        player.sendMessage(Component.text("You are now " + wisp.title + ". Everything you say is spoken as the wisp.", wisp.color));
    }

    public void handleSay(CommandSender sender, String[] args) {
        Wisp wisp = args.length >= 3 ? Wisp.parse(args[1]) : null;
        if (wisp == null) {
            sender.sendMessage("§7/bliss wispsay <prophet|manifestation|balance> <message>");
            return;
        }
        Bukkit.broadcast(this.line(wisp, Component.text(String.join(" ", Arrays.copyOfRange(args, 2, args.length)))));
    }

    private Component line(Wisp wisp, Component message) {
        return Component.text("✦ ", wisp.color)
            .append(Component.text(wisp.title, wisp.color).decorate(TextDecoration.BOLD))
            .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
            .append(message.color(wisp.color).decorate(TextDecoration.ITALIC));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Wisp wisp = this.morphed.get(event.getPlayer().getUniqueId());
        if (wisp == null) return;
        event.renderer((source, displayName, message, viewer) -> this.line(wisp, message));
    }

    /** Timer runs; the server tick's parity depends on when the 2-tick timer started. */
    private int runs;

    private void tick() {
        this.runs++;
        double t = Bukkit.getCurrentTick() * 0.08;
        for (Map.Entry<UUID, Wisp> e : this.morphed.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p == null) continue;
            Location at = p.getLocation().add(0, 1.1, 0);
            TextDisplay label = this.labels.get(e.getKey());
            if (label != null && label.isValid()) label.teleport(at.clone().add(0, 1.3, 0));
            switch (e.getValue()) {
                case PROPHET -> this.prophet(at, t);
                case BALANCE -> this.balance(at, t, p.getLocation().getYaw());
                case MANIFESTATION -> this.manifestation(at, t);
            }
        }
    }

    private static void dust(Location at, Color color, float size) {
        at.getWorld().spawnParticle(Particle.DUST, at, 1, 0, 0, 0, 0.0, new Particle.DustOptions(color, size), true);
    }

    /** The Prophet: a whirl of gold motes around a bright core. */
    private void prophet(Location at, double t) {
        Color gold = Color.fromRGB(255, 210, 74);
        Color pale = Color.fromRGB(255, 240, 160);
        for (int arm = 0; arm < 4; arm++) {
            for (int i = 0; i < 5; i++) {
                double a = t * 2.0 + arm * Math.PI / 2 + i * 0.35;
                double r = 0.25 + i * 0.13;
                dust(at.clone().add(Math.cos(a) * r, Math.sin(a * 0.5 + i) * 0.35, Math.sin(a) * r), i % 2 == 0 ? gold : pale, 1.1f);
            }
        }
        at.getWorld().spawnParticle(Particle.END_ROD, at, 1, 0.15, 0.25, 0.15, 0.0, null, true);
    }

    /** Balance: a pale ghost light inside a slowly turning flower of life. */
    private void balance(Location at, double t, float yaw) {
        Color white = Color.fromRGB(240, 240, 240);
        Color grey = Color.fromRGB(190, 180, 170);
        at.getWorld().spawnParticle(Particle.DUST, at, 4, 0.2, 0.35, 0.2, 0.0, new Particle.DustOptions(white, 1.2f), true);
        if (this.runs % 3 != 0) return; // every 6 ticks (the timer runs every 2)
        // six circles round a seventh, drawn upright facing the way the wisp looks, turning
        double face = Math.toRadians(-yaw);
        double r = 0.55;
        Location centre = at.clone().add(0, 0.6, 0);
        for (int c = 0; c < 7; c++) {
            double ca = c == 0 ? 0 : (c - 1) * Math.PI / 3 + t * 0.5;
            double cx = c == 0 ? 0 : Math.cos(ca) * r;
            double cy = c == 0 ? 0 : Math.sin(ca) * r;
            for (int i = 0; i < 18; i++) {
                double a = Math.PI * 2 * i / 18.0;
                double x = cx + Math.cos(a) * r;
                double y = cy + Math.sin(a) * r;
                dust(centre.clone().add(Math.cos(face) * x, y, Math.sin(face) * x), c == 0 ? white : grey, 0.7f);
            }
        }
    }

    /** Manifestation: three tilted orbits of white light with colours running along them. */
    private void manifestation(Location at, double t) {
        Color[] colours = {Color.fromRGB(180, 92, 255), Color.fromRGB(80, 200, 255), Color.fromRGB(90, 230, 120), Color.fromRGB(255, 210, 74), Color.fromRGB(255, 90, 90)};
        at.getWorld().spawnParticle(Particle.DUST, at, 3, 0.15, 0.2, 0.15, 0.0, new Particle.DustOptions(Color.fromRGB(180, 92, 255), 1.2f), true);
        if (this.runs % 2 != 0) return; // every 4 ticks
        for (int ring = 0; ring < 3; ring++) {
            double tilt = ring * Math.PI / 3 + t * 0.3;
            for (int i = 0; i < 24; i++) {
                double a = Math.PI * 2 * i / 24.0;
                double x = Math.cos(a) * 1.1;
                double y = Math.sin(a) * 1.1;
                // tilt the ring about the X axis, then spin it about Y
                double y2 = y * Math.cos(tilt);
                double z2 = y * Math.sin(tilt);
                double spin = t + ring;
                double rx = x * Math.cos(spin) - z2 * Math.sin(spin);
                double rz = x * Math.sin(spin) + z2 * Math.cos(spin);
                boolean lit = (i + (int) (t * 6)) % 8 < 2;
                dust(at.clone().add(rx, y2, rz), lit ? colours[(ring + i / 8) % colours.length] : Color.WHITE, lit ? 1.0f : 0.6f);
            }
        }
    }

    private void unmorph(Player player) {
        if (this.morphed.remove(player.getUniqueId()) == null) return;
        TextDisplay label = this.labels.remove(player.getUniqueId());
        if (label != null) label.remove();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) viewer.showPlayer(this.plugin, player);
        }
    }

    /** Someone joining doesn't see a wisp's player either. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (UUID id : this.morphed.keySet()) {
            Player wisp = Bukkit.getPlayer(id);
            if (wisp != null && !wisp.equals(event.getPlayer())) event.getPlayer().hidePlayer(this.plugin, wisp);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.unmorph(event.getPlayer());
    }

    public void shutdown() {
        for (UUID id : this.morphed.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) this.unmorph(p);
        }
    }
}
