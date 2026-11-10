package dev.xoperr.blissgems.lore;

import dev.xoperr.blissgems.BlissGems;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * The Last Raid's Honored One wears a yellow halo above the head and under the feet, and two white
 * beams circle round their back (Manifestation's swirl, but two pale beams instead of eight).
 * The Honored One switches it with {@code /bliss honored}; staff move the title with
 * {@code /bliss honored set <player>} or {@code clear}.
 */
public final class HonoredOne {
    private static final Particle.DustOptions HALO = new Particle.DustOptions(Color.fromRGB(255, 214, 64), 0.8f);
    private static final Particle.DustOptions BEAM = new Particle.DustOptions(Color.fromRGB(250, 250, 250), 1.1f);

    private final BlissGems plugin;
    private final File file;
    private UUID honored;
    private boolean auraOn = true;

    public HonoredOne(BlissGems plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "honored.yml");
        YamlConfiguration y = YamlConfiguration.loadConfiguration(this.file);
        String id = y.getString("honored");
        if (id != null) {
            try {
                this.honored = UUID.fromString(id);
            } catch (IllegalArgumentException ignored) {
            }
        }
        this.auraOn = y.getBoolean("aura", true);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 2L);
    }

    /** Called when the Last Raid crowns its top damage dealer. */
    public void crown(UUID id) {
        this.honored = id;
        this.auraOn = true;
        this.save();
    }

    public void command(Player player, String[] args) {
        String sub = args.length >= 2 ? args[1].toLowerCase() : "toggle";
        if ((sub.equals("set") || sub.equals("clear")) && player.hasPermission("blissgems.admin")) {
            Player target = sub.equals("set") && args.length >= 3 ? Bukkit.getPlayer(args[2]) : null;
            if (sub.equals("set") && target == null) {
                player.sendMessage("§c/bliss honored set <online player>");
                return;
            }
            this.honored = target != null ? target.getUniqueId() : null;
            this.auraOn = true;
            this.save();
            player.sendMessage(target != null ? "§e" + target.getName() + " §6is now the Honored One." : "§7There is no Honored One now.");
            return;
        }
        if (!player.getUniqueId().equals(this.honored)) {
            player.sendMessage("§7Only the Honored One has a halo to wear.");
            return;
        }
        this.auraOn = !this.auraOn;
        this.save();
        player.sendMessage(this.auraOn ? "§6Your halo shines again." : "§7Your halo fades.");
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("honored", this.honored != null ? this.honored.toString() : null);
        y.set("aura", this.auraOn);
        try {
            y.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("honored.yml: " + e.getMessage());
        }
    }

    private void tick() {
        if (this.honored == null || !this.auraOn) return;
        Player p = Bukkit.getPlayer(this.honored);
        if (p == null || p.isDead() || p.getGameMode() == GameMode.SPECTATOR || p.isInvisible()) return;
        Location feet = p.getLocation();
        double t = Bukkit.getCurrentTick() * 0.15;
        // halos: a small ring over the head, a wider one round the feet
        for (int i = 0; i < 14; i++) {
            double a = Math.PI * 2 * i / 14.0 + t * 0.3;
            dust(feet.clone().add(Math.cos(a) * 0.3, 2.25, Math.sin(a) * 0.3), HALO);
            dust(feet.clone().add(Math.cos(a) * 0.55, 0.05, Math.sin(a) * 0.55), HALO);
        }
        // two white beams sweeping round behind the back, each a short trailing arc
        double back = Math.toRadians(feet.getYaw() + 90.0) + Math.PI;
        for (int beam = 0; beam < 2; beam++) {
            for (int k = 0; k < 6; k++) {
                double a = back + Math.sin(t + beam * Math.PI) * 1.4 - k * 0.12;
                double y = 0.9 + Math.sin(t * 0.7 + beam * Math.PI) * 0.6 + k * 0.03;
                dust(feet.clone().add(Math.cos(a) * 0.75, y, Math.sin(a) * 0.75), BEAM);
            }
        }
    }

    private static void dust(Location at, Particle.DustOptions options) {
        at.getWorld().spawnParticle(Particle.DUST, at, 1, 0, 0, 0, 0.0, options, true);
    }
}
