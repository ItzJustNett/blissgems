package dev.xoperr.blissgems.villagerevent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/** A forked lightning bolt drawn with particles, stroked down from the sky a few points per tick. */
public final class DrawnBolt {
    private DrawnBolt() {
    }

    /** Bolt landing within {@code radius} of {@code center}, starting {@code height} blocks above the ground there. */
    public static List<Location> path(World world, Location center, double radius, double height, ThreadLocalRandom r) {
        double angle = r.nextDouble() * Math.PI * 2.0;
        double dist = radius * Math.sqrt(r.nextDouble());
        double gx = center.getX() + Math.cos(angle) * dist;
        double gz = center.getZ() + Math.sin(angle) * dist;
        double ground = world.getHighestBlockYAt((int) Math.floor(gx), (int) Math.floor(gz)) + 1;
        double top = ground + height;
        List<Location> points = new ArrayList<>();
        List<double[]> joints = new ArrayList<>();
        double x = gx, y = top, z = gz;
        for (int seg = 1; seg <= 12; seg++) {
            double ny = top - (top - ground) * seg / 12.0;
            double nx = x + (r.nextDouble() - 0.5) * 1.5;
            double nz = z + (r.nextDouble() - 0.5) * 1.5;
            line(points, world, x, y, z, nx, ny, nz);
            joints.add(new double[]{nx, ny, nz});
            x = nx;
            y = ny;
            z = nz;
        }
        int forks = 2 + r.nextInt(3);
        for (int f = 0; f < forks; f++) {
            double[] j = joints.get(r.nextInt(Math.max(1, joints.size() - 3)));
            double fx = j[0], fy = j[1], fz = j[2];
            int steps = 2 + r.nextInt(3);
            for (int s = 0; s < steps; s++) {
                double nx = fx + (r.nextDouble() - 0.5) * 1.6 + (r.nextDouble() - 0.5) * 1.5;
                double nz = fz + (r.nextDouble() - 0.5) * 1.6 + (r.nextDouble() - 0.5) * 1.5;
                double ny = fy - 1.3;
                line(points, world, fx, fy, fz, nx, ny, nz);
                fx = nx;
                fy = ny;
                fz = nz;
            }
        }
        return points;
    }

    public static void draw(Plugin plugin, World world, List<Location> points, int perTick) {
        if (points.isEmpty()) {
            return;
        }
        new BukkitRunnable() {
            int i;

            @Override
            public void run() {
                int end = Math.min(points.size(), this.i + perTick);
                while (this.i < end) {
                    world.spawnParticle(Particle.WAX_OFF, points.get(this.i++), 1, 0, 0, 0, 0);
                }
                if (this.i >= points.size()) this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static void line(List<Location> out, World w, double x1, double y1, double z1, double x2, double y2, double z2) {
        for (int i = 0; i < 5; i++) {
            double t = i / 5.0;
            out.add(new Location(w, x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, z1 + (z2 - z1) * t));
        }
    }
}
