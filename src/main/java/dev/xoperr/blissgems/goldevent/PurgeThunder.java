package dev.xoperr.blissgems.goldevent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * The purge thunder: a giant hand-drawn lightning bolt made of END_ROD dots, placed 24 blocks past the
 * nearest player and 60 blocks up so it fills their view. The strokes (MAIN = trunk, BRANCH = forks)
 * are 2D sketches; each strike jitters them into 3D, forks twigs off the tips and joins strokes that
 * touch. It draws itself in over 0.7 s, then hangs for 21 s, flashing.
 */
public final class PurgeThunder {
    private static final double[][][] MAIN = {
        {{-23.6, 53.8}, {-13.7, 48.0}, {15.6, 29.0}},
        {{14.6, 30.0}, {15.9, 30.1}, {20.2, 32.4}, {24.8, 33.8}, {36.2, 35.1}},
        {{15.5, 29.6}, {19.6, 22.2}, {25.5, 15.2}},
        {{16.5, 28.2}, {21.8, 28.6}, {30.5, 30.5}},
        {{30.2, 30.2}, {40.7, 17.1}},
        {{19.3, 24.5}, {12.3, 13.7}, {12.4, 11.4}, {14.0, 5.3}},
        {{13.6, 17.6}, {14.5, 15.9}, {20.4, 9.6}, {26.6, 4.2}, {26.3, 3.9}},
        {{13.9, 30.5}, {0.8, 15.4}, {-3.2, 11.9}, {-2.4, 7.7}, {-1.0, 5.6}, {-0.4, 3.3}},
        {{8.9, 22.6}, {10.8, 3.8}},
        {{-18.4, 50.1}, {-18.8, 50.3}, {-27.7, 44.4}, {-42.7, 39.2}, {-47.7, 38.0}, {-56.4, 43.7}},
        {{-26.2, 47.1}, {-28.3, 47.8}, {-45.5, 57.7}},
        {{-21.5, 54.0}, {-20.6, 57.8}, {-20.3, 68.9}, {-19.2, 73.2}},
        {{-20.8, 52.9}, {-29.1, 59.1}, {-34.2, 64.0}, {-35.8, 66.3}, {-36.6, 66.5}},
        {{-18.0, 50.5}, {-18.0, 53.1}, {-15.6, 63.4}, {-10.6, 62.8}, {-6.7, 60.7}, {-5.1, 60.1}, {-4.4, 60.4}, {-1.1, 71.7}},
        {{-21.0, 54.8}, {-24.4, 72.2}}
    };
    private static final double[][][] BRANCH = {
        {{-27.8, 58.2}, {-32.4, 57.7}},
        {{-30.5, 59.6}, {-43.0, 59.6}, {-46.6, 65.0}},
        {{-32.7, 63.3}, {-29.3, 68.8}, {-31.9, 73.0}, {-32.5, 74.9}},
        {{-27.8, 55.8}, {-28.7, 54.8}, {-30.3, 48.9}, {-31.0, 48.3}, {-28.5, 47.0}, {-27.2, 45.5}},
        {{-24.7, 53.0}, {-25.6, 46.3}, {-23.4, 45.0}, {-21.5, 42.8}},
        {{-17.6, 49.2}, {-18.0, 47.0}, {-20.1, 44.5}, {-23.9, 41.2}, {-26.0, 40.9}},
        {{-35.2, 51.8}, {-37.8, 48.0}, {-40.9, 45.6}},
        {{-40.4, 52.7}, {-46.1, 45.6}},
        {{-38.2, 51.8}, {-37.4, 52.0}, {-33.8, 56.8}, {-32.5, 57.1}},
        {{-38.0, 58.4}, {-46.1, 60.7}, {-49.6, 61.0}},
        {{20.3, 18.7}, {33.1, 16.3}, {35.5, 16.3}},
        {{29.3, 13.4}, {25.9, 8.4}, {24.4, 5.1}},
        {{26.0, 15.9}, {30.2, 15.6}, {35.4, 14.3}},
        {{36.4, 21.7}, {35.8, 13.9}},
        {{37.4, 20.1}, {49.6, 20.1}},
        {{34.0, 27.2}, {42.3, 33.0}},
        {{37.0, 25.5}, {37.6, 24.8}, {44.8, 23.0}, {49.7, 20.1}},
        {{30.5, 30.7}, {30.8, 19.5}},
        {{24.5, 28.6}, {22.6, 21.0}},
        {{18.9, 21.7}, {13.3, 23.1}, {11.2, 23.1}},
        {{7.1, 19.2}, {10.5, 16.3}, {14.2, 10.9}},
        {{9.3, 8.6}, {5.3, 1.5}, {4.3, -1.5}},
        {{-2.9, 6.2}, {-0.6, 4.0}, {2.8, 2.0}},
        {{1.2, 15.4}, {1.3, 10.6}, {2.3, 6.8}},
        {{-0.8, 17.3}, {-0.9, 16.4}, {-2.8, 14.0}},
        {{10.8, 25.8}, {23.6, 13.1}},
        {{22.0, 8.6}, {21.8, 1.6}, {22.4, 0.2}},
        {{20.3, 10.1}, {27.1, 8.8}, {45.8, 3.0}},
        {{18.0, 12.2}, {16.6, 5.0}},
        {{35.6, 21.8}, {41.2, 14.9}, {50.0, 5.6}},
        {{35.6, 25.0}, {32.8, 16.2}, {30.4, 11.2}},
        {{26.3, 29.8}, {30.7, 32.0}, {44.2, 35.2}},
        {{29.3, 34.7}, {38.2, 35.9}, {44.9, 37.8}},
        {{25.0, 33.8}, {28.6, 39.0}, {31.5, 41.9}},
        {{21.0, 32.6}, {20.8, 30.7}, {22.0, 28.1}, {21.6, 26.9}},
        {{18.5, 41.7}, {18.2, 37.9}, {16.5, 30.5}}
    };
    private static final double STEP_MAIN = 0.5;
    private static final double STEP_BRANCH = 0.75;
    private static final double JOINT = 3.4;
    private static final double ATTACH = 6.0;
    private static final double STAND_OFF = 24.0;
    private static final double LIFT = 60.0;
    private static final int REVEAL_TICKS = 14;
    private static final int REFRESH = 10;
    private static final int HOLD_TICKS = 420;
    private static final int BURST_EVERY = 20;

    private PurgeThunder() {
    }

    public static void strike(Plugin plugin, GoldenDream dream, Location origin) {
        World w = origin.getWorld();
        if (w == null) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        if (dream != null && dream.world().isMemoryWorld(w)) dream.world().storm(494);

        Player viewer = null;
        double best = Double.MAX_VALUE;
        for (Player p : w.getPlayers()) {
            double d = p.getLocation().distanceSquared(origin);
            if (d < best) {
                best = d;
                viewer = p;
            }
        }
        // (dx, dz) points away from the viewer; the bolt's sketch plane is perpendicular to it
        double dx = 1.0, dz = 0.0;
        if (viewer != null) {
            double ox = origin.getX() - viewer.getLocation().getX(), oz = origin.getZ() - viewer.getLocation().getZ();
            double len = Math.hypot(ox, oz);
            if (len > 0.5) {
                dx = ox / len;
                dz = oz / len;
            } else {
                Vector look = viewer.getLocation().getDirection();
                double h = Math.hypot(look.getX(), look.getZ());
                if (h > 0.05) {
                    dx = look.getX() / h;
                    dz = look.getZ() / h;
                }
            }
        }
        double baseX = origin.getX() + dx * STAND_OFF;
        double baseZ = origin.getZ() + dz * STAND_OFF;
        double groundY = origin.getY();
        int bx = (int) Math.floor(baseX), bz = (int) Math.floor(baseZ);
        if (w.isChunkLoaded(bx >> 4, bz >> 4)) {
            int top = w.getHighestBlockYAt(bx, bz);
            if (top > w.getMinHeight()) groundY = top + 1;
        }
        double baseY = groundY + LIFT;
        Frame f = new Frame(baseX, baseY, baseZ, -dz, dx, dx, dz);

        List<double[][]> shapes = new ArrayList<>();
        List<Boolean> isMain = new ArrayList<>();
        for (double[][] s : MAIN) {
            shapes.add(taller(s));
            isMain.add(true);
        }
        for (double[][] s : BRANCH) {
            shapes.add(taller(s));
            isMain.add(false);
        }
        Integer[] order = new Integer[shapes.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingDouble(i -> -topOf(shapes.get(i))));

        int n = shapes.size();
        List<List<List<double[]>>> strokes = new ArrayList<>(n);
        int[] spineLen = new int[n];
        boolean[] main = new boolean[n];
        boolean[] grounded = new boolean[n];
        for (int k = 0; k < n; k++) {
            int src = order[k];
            main[k] = isMain.get(src);
            List<double[]> spine = jointed(shapes.get(src), main[k], r);
            double[] end = spine.get(spine.size() - 1);
            if (main[k] && end[1] > 0.0 && end[1] <= 24.0) {
                spine.add(new double[]{end[0] + (r.nextDouble() - 0.5) * 1.5 * 2.0, 0.0, end[2]});
                end = spine.get(spine.size() - 1);
            }
            spineLen[k] = spine.size();
            List<List<double[]>> parts = new ArrayList<>();
            parts.add(spine);
            grounded[k] = end[1] <= 0.5;
            if (!grounded[k]) spine.addAll(twig(spine, main[k], r));
            if (main[k]) {
                int at = 1 + r.nextInt(Math.max(1, spineLen[k] - 2));
                List<double[]> fork = new ArrayList<>();
                fork.add(spine.get(at));
                fork.addAll(twig(spine.subList(0, at + 1), false, r));
                parts.add(fork);
            }
            strokes.add(parts);
        }

        // each stroke hangs from the nearest point of another stroke within ATTACH of its start
        int[] parent = new int[n];
        int[] parentAt = new int[n];
        for (int k = 0; k < n; k++) {
            parent[k] = -1;
            parentAt[k] = -1;
            double[] start = strokes.get(k).get(0).get(0);
            double near = ATTACH;
            for (int o = 0; o < n; o++) {
                if (o == k) continue;
                List<double[]> spine = strokes.get(o).get(0);
                for (int i = 0; i < spineLen[o]; i++) {
                    double d = Math.hypot(spine.get(i)[0] - start[0], spine.get(i)[1] - start[1]);
                    if (d < near) {
                        near = d;
                        parent[k] = o;
                        parentAt[k] = i;
                    }
                }
            }
        }
        List<List<List<double[]>>> placed = new ArrayList<>(n);
        for (int k = 0; k < n; k++) placed.add(null);
        boolean[] done = new boolean[n];
        int left = n;
        while (left > 0) {
            boolean progress = false;
            for (int k = 0; k < n; k++) {
                if (done[k] || (parent[k] != -1 && !done[parent[k]])) continue;
                double[] start = strokes.get(k).get(0).get(0);
                double[] anchor = parent[k] == -1 ? start.clone() : placed.get(parent[k]).get(0).get(parentAt[k]);
                double swing = (main[k] ? 0.12 : 0.25) * (r.nextDouble() * 2.0 - 1.0);
                double cos = Math.cos(swing), sin = Math.sin(swing);
                List<List<double[]>> moved = new ArrayList<>();
                for (List<double[]> part : strokes.get(k)) {
                    List<double[]> m = new ArrayList<>(part.size());
                    for (double[] p : part) {
                        double lx = p[0] - start[0], lz = p[2] - start[2];
                        m.add(new double[]{anchor[0] + lx * cos - lz * sin, anchor[1] + (p[1] - start[1]), anchor[2] + lx * sin + lz * cos});
                    }
                    moved.add(m);
                }
                placed.set(k, moved);
                done[k] = true;
                left--;
                progress = true;
            }
            if (!progress) {
                // a loop of strokes hanging off each other: cut it at its lowest index
                int k = 0;
                while (done[k]) k++;
                boolean[] seen = new boolean[n];
                while (!seen[k]) {
                    seen[k] = true;
                    k = parent[k];
                }
                int low = k;
                for (int c = parent[k]; c != k; c = parent[c]) low = Math.min(low, c);
                parent[low] = -1;
            }
        }

        List<Dot> dots = new ArrayList<>();
        List<Dot> sparks = new ArrayList<>();
        List<double[]> tips = new ArrayList<>();
        List<Dot> trunk = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            for (List<double[]> part : placed.get(k)) {
                dots(part, main[k] ? STEP_MAIN : STEP_BRANCH, main[k], dots, f);
                if (main[k]) dots(part, STEP_MAIN, true, sparks, f);
            }
            if (order[k] == 0) dots(placed.get(k).get(0).subList(0, spineLen[k]), STEP_MAIN, true, trunk, f);
            if (grounded[k]) tips.add(f.place(placed.get(k).get(0).get(spineLen[k] - 1)));
        }
        List<Dot> burstOn = trunk.isEmpty() ? dots : trunk;
        Location burst;
        if (burstOn.isEmpty()) {
            burst = new Location(w, baseX, baseY, baseZ);
        } else {
            Dot d = burstOn.get(Math.min(burstOn.size() - 1, (int) (burstOn.size() * 0.5)));
            burst = new Location(w, d.x, d.y, d.z);
        }

        new BukkitRunnable() {
            int t;
            int i;

            @Override
            public void run() {
                if (this.t < REVEAL_TICKS) {
                    int upTo = (int) Math.round(dots.size() * (this.t + 1) / (double) REVEAL_TICKS);
                    for (; this.i < upTo && this.i < dots.size(); this.i++) glow(w, dots.get(this.i));
                }
                if (this.t == REVEAL_TICKS) {
                    for (double[] tip : tips) flash(w, new Location(w, tip[0], tip[1], tip[2]), 1);
                    for (Dot s : sparks) w.spawnParticle(Particle.ELECTRIC_SPARK, s.x, s.y, s.z, 1, 0, 0, 0, 0, null, true);
                }
                if (this.t >= REVEAL_TICKS) {
                    if ((this.t - REVEAL_TICKS) % BURST_EVERY == 0) flash(w, burst, 5);
                    if ((this.t - REVEAL_TICKS) % REFRESH == 0) for (Dot d : dots) glow(w, d);
                }
                if (++this.t > REVEAL_TICKS + HOLD_TICKS) this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static double topOf(double[][] s) {
        double top = -Double.MAX_VALUE;
        for (double[] p : s) top = Math.max(top, p[1]);
        return top;
    }

    /** Resamples a 2D stroke into ~3.4-block joints, jittered sideways and in depth; top point first. */
    private static List<double[]> jointed(double[][] s, boolean main, ThreadLocalRandom r) {
        double[][] pts = s;
        if (s[0][1] < s[s.length - 1][1]) {
            pts = new double[s.length][];
            for (int i = 0; i < s.length; i++) pts[i] = s[s.length - 1 - i];
        }
        double jit = main ? 0.7 : 0.44;
        double jitDepth = main ? 0.9 : 3.4;
        double maxDepth = main ? 3.0 : 14.0;
        double depth = main ? 0.0 : (r.nextDouble() * 2.0 - 1.0) * maxDepth * 0.6;
        List<double[]> out = new ArrayList<>();
        out.add(new double[]{pts[0][0], pts[0][1], depth});
        for (int i = 1; i < pts.length; i++) {
            double ax = pts[i - 1][0], ay = pts[i - 1][1], bx = pts[i][0], by = pts[i][1];
            double len = Math.hypot(bx - ax, by - ay);
            int segs = Math.max(1, (int) Math.round(len / JOINT));
            for (int k = 1; k < segs; k++) {
                double f = k / (double) segs;
                double side = (r.nextDouble() - 0.5) * 2.0 * jit;
                double z = Math.max(-maxDepth, Math.min(maxDepth, depth + (r.nextDouble() - 0.5) * 2.0 * jitDepth));
                double norm = Math.max(len, 1.0E-6);
                out.add(new double[]{ax + (bx - ax) * f + -(by - ay) / norm * side, ay + (by - ay) * f + (bx - ax) / norm * side, z});
            }
            out.add(new double[]{bx, by, Math.max(-maxDepth, Math.min(maxDepth, depth + (r.nextDouble() - 0.5) * 2.0 * jitDepth))});
        }
        return out;
    }

    /** 2-3 shrinking zig-zag segments continuing from the end of a stroke. */
    private static List<double[]> twig(List<double[]> spine, boolean main, ThreadLocalRandom r) {
        List<double[]> out = new ArrayList<>();
        if (spine.size() < 2) return out;
        double[] a = spine.get(spine.size() - 2), b = spine.get(spine.size() - 1);
        double vx = b[0] - a[0], vy = b[1] - a[1];
        double len = Math.hypot(vx, vy);
        if (len < 1.0E-6) {
            vx = 0.0;
            vy = -1.0;
            len = 1.0;
        }
        double ux = vx / len, uy = vy / len;
        double turn = (r.nextBoolean() ? 1 : -1) * (0.35 + r.nextDouble() * 0.5);
        double x = b[0], y = b[1];
        double step = main ? 5.2 : 3.6;
        int count = 2 + r.nextInt(2);
        for (int i = 0; i < count; i++) {
            double c = Math.cos(turn), s = Math.sin(turn);
            double tx = ux * c - uy * s, ty = ux * s + uy * c - 0.25;
            double tl = Math.hypot(tx, ty);
            x += tx / tl * step;
            y += ty / tl * step;
            out.add(new double[]{x, y, (r.nextDouble() - 0.5) * 2.0 * (main ? 0.9 : 3.4)});
            step *= 0.7;
            turn *= -0.6;
        }
        return out;
    }

    private static void dots(List<double[]> path, double step, boolean main, List<Dot> out, Frame f) {
        double carry = 0.0;
        for (int i = 1; i < path.size(); i++) {
            double[] a = path.get(i - 1), b = path.get(i);
            double len = Math.sqrt(sq(b[0] - a[0]) + sq(b[1] - a[1]) + sq(b[2] - a[2]));
            if (len < 1.0E-6) continue;
            double d = carry;
            for (; d <= len; d += step) {
                double t = d / len;
                double[] p = f.place(new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t});
                out.add(new Dot(p[0], p[1], p[2], main));
            }
            carry = d - len;
        }
    }

    private static double sq(double v) {
        return v * v;
    }

    private static void glow(World w, Dot d) {
        w.spawnParticle(Particle.END_ROD, d.x, d.y, d.z, 1, 0, 0, 0, 0.0, null, true);
    }

    /** Sketch is twice as wide and four times as tall in blocks. */
    private static double[][] taller(double[][] s) {
        double[][] out = new double[s.length][];
        for (int i = 0; i < s.length; i++) out[i] = new double[]{s[i][0] * 2.0, s[i][1] * 2.0 * 2.0};
        return out;
    }

    private static void flash(World w, Location at, int count) {
        if (Particle.FLASH.getDataType() == Color.class) w.spawnParticle(Particle.FLASH, at, count, 0, 0, 0, 0.0, Color.WHITE, true);
        else w.spawnParticle(Particle.FLASH, at, count, 0, 0, 0, 0.0, null, true);
    }

    /** Maps sketch coordinates (side, up, depth) to world coordinates. */
    private record Frame(double ox, double oy, double oz, double sideX, double sideZ, double depthX, double depthZ) {
        double[] place(double[] p) {
            return new double[]{this.ox + this.sideX * p[0] + this.depthX * p[2], this.oy + p[1], this.oz + this.sideZ * p[0] + this.depthZ * p[2]};
        }
    }

    private record Dot(double x, double y, double z, boolean main) {
    }
}
