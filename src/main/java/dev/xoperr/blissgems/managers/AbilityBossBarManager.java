package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class AbilityBossBarManager {
    private final BlissGems plugin;
    private final Map<UUID, Map<String, BossBar>> playerBars = new HashMap<>();
    private BukkitTask updateTask;

    public AbilityBossBarManager(BlissGems plugin) {
        this.plugin = plugin;
        this.startUpdateLoop();
    }

    private void startUpdateLoop() {
        this.updateTask = new BukkitRunnable() {
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    AbilityBossBarManager.this.updatePlayer(player);
                }
            }
        }.runTaskTimer((Plugin) this.plugin, 0L, 5L);
    }

    public void updatePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        AbilityManager am = this.plugin.getAbilityManager();
        if (am == null) {
            return;
        }
        Map<String, Long> activeAbilities = this.getActiveAbilities(uuid);
        if (activeAbilities == null || activeAbilities.isEmpty()) {
            this.removeAllBars(player);
            return;
        }
        Map<String, BossBar> bars = this.playerBars.computeIfAbsent(uuid, k -> new HashMap<>());
        for (Map.Entry<String, Long> entry : activeAbilities.entrySet()) {
            String abilityKey = entry.getKey();
            long endTime = entry.getValue();
            long remaining = endTime - System.currentTimeMillis();
            if (remaining <= 0) {
                BossBar old = bars.remove(abilityKey);
                if (old != null) {
                    old.removePlayer(player);
                }
                continue;
            }
            int durationSeconds = this.plugin.getConfigManager().getAbilityDuration(abilityKey);
            if (durationSeconds <= 0) {
                durationSeconds = 10;
            }
            double progress = Math.min(1.0, (double) remaining / ((double) durationSeconds * 1000.0));
            String displayName = this.formatAbilityName(abilityKey);
            BossBar bar = bars.get(abilityKey);
            if (bar == null) {
                bar = Bukkit.createBossBar(displayName + " " + (int) Math.ceil((double) remaining / 1000.0) + "s", this.getColorForAbility(abilityKey), BarStyle.SOLID);
                bar.addPlayer(player);
                bars.put(abilityKey, bar);
            }
            bar.setTitle(displayName + " " + (int) Math.ceil((double) remaining / 1000.0) + "s");
            bar.setProgress(progress);
        }
    }

    private Map<String, Long> getActiveAbilities(UUID uuid) {
        AbilityManager am = this.plugin.getAbilityManager();
        if (am == null) {
            return null;
        }
        Map<String, Long> active = am.getActiveAbilitiesFor(uuid);
        return active.isEmpty() ? null : active;
    }

    private BarColor getColorForAbility(String key) {
        if (key.startsWith("fire-")) return BarColor.RED;
        if (key.startsWith("flux-")) return BarColor.BLUE;
        if (key.startsWith("life-")) return BarColor.PINK;
        if (key.startsWith("speed-")) return BarColor.YELLOW;
        if (key.startsWith("strength-")) return BarColor.PURPLE;
        if (key.startsWith("astra-")) return BarColor.WHITE;
        if (key.startsWith("puff-")) return BarColor.GREEN;
        if (key.startsWith("wealth-")) return BarColor.GREEN;
        if (key.startsWith("gold-")) return BarColor.YELLOW;
        return BarColor.WHITE;
    }

    private String formatAbilityName(String key) {
        String[] parts = key.split("-");
        if (parts.length < 2) return key;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            if (i > 1) sb.append(" ");
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return sb.toString();
    }

    public void removeAllBars(Player player) {
        Map<String, BossBar> bars = this.playerBars.remove(player.getUniqueId());
        if (bars != null) {
            for (BossBar bar : bars.values()) {
                bar.removePlayer(player);
            }
        }
    }

    public void stop() {
        if (this.updateTask != null) {
            this.updateTask.cancel();
        }
        for (Map<String, BossBar> bars : this.playerBars.values()) {
            for (BossBar bar : bars.values()) {
                bar.removeAll();
            }
        }
        this.playerBars.clear();
    }
}
