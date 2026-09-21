package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class EndSkyVisualManager {
    private final BlissGems plugin;
    private final BossBar endSkyBossBar;
    private final Set<UUID> playersInEnd = new HashSet<>();
    private BukkitTask task;
    private boolean enabled;

    public EndSkyVisualManager(BlissGems plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("end-sky.enabled", true);
        this.endSkyBossBar = Bukkit.createBossBar(
                ChatColor.translateAlternateColorCodes('&', "&2✦ &a&lTHE END &2✦"),
                BarColor.GREEN,
                BarStyle.SOLID
        );
        this.endSkyBossBar.setVisible(true);
        if (enabled) {
            this.start();
        }
    }

    public void start() {
        if (task != null) task.cancel();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void tick() {
        Set<UUID> currentInIsland = new HashSet<>();
        Particle.DustOptions darkGreenDust = new Particle.DustOptions(Color.fromRGB(15, 60, 20), 1.8f);
        Particle.DustOptions limeDust = new Particle.DustOptions(Color.fromRGB(50, 160, 60), 1.2f);

        for (Player player : Bukkit.getOnlinePlayers()) {
            World world = player.getWorld();
            if (world.getEnvironment() == World.Environment.THE_END || world.getName().toLowerCase().endsWith("_the_end")) {
                Location loc = player.getLocation();
                double distSq = loc.getX() * loc.getX() + loc.getZ() * loc.getZ();
                if (distSq <= 600 * 600) { // Within 600 blocks of main island center
                    currentInIsland.add(player.getUniqueId());
                    if (!endSkyBossBar.getPlayers().contains(player)) {
                        endSkyBossBar.addPlayer(player);
                    }

                    // Ambient sky visual particles
                    Location eyeLoc = player.getEyeLocation();
                    for (int i = 0; i < 15; i++) {
                        double ox = ThreadLocalRandom.current().nextDouble(-15, 15);
                        double oy = ThreadLocalRandom.current().nextDouble(5, 20); // Sky level
                        double oz = ThreadLocalRandom.current().nextDouble(-15, 15);
                        Location pLoc = eyeLoc.clone().add(ox, oy, oz);
                        player.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, (Object) darkGreenDust);
                        if (i % 3 == 0) {
                            player.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, (Object) limeDust);
                        }
                    }
                    if (ThreadLocalRandom.current().nextInt(3) == 0) {
                        double ox = ThreadLocalRandom.current().nextDouble(-10, 10);
                        double oy = ThreadLocalRandom.current().nextDouble(8, 16);
                        double oz = ThreadLocalRandom.current().nextDouble(-10, 10);
                        player.spawnParticle(Particle.DRAGON_BREATH, eyeLoc.clone().add(ox, oy, oz), 4, 0.2, 0.2, 0.2, 0.01);
                    }
                    continue;
                }
            }

            if (endSkyBossBar.getPlayers().contains(player)) {
                endSkyBossBar.removePlayer(player);
            }
        }

        playersInEnd.clear();
        playersInEnd.addAll(currentInIsland);
    }

    public void cleanup() {
        if (task != null) task.cancel();
        if (endSkyBossBar != null) {
            endSkyBossBar.removeAll();
        }
    }
}
