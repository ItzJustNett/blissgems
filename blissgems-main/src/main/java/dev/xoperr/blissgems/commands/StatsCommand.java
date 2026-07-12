package dev.xoperr.blissgems.commands;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class StatsCommand {
    private final BlissGems plugin;

    public StatsCommand(BlissGems plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player, String[] args) {
        if (args.length == 0) {
            showTopKillers(player);
        } else {
            switch (args[0].toLowerCase()) {
                case "top" -> showTopKillers(player);
                case "me", "my", "personal" -> showPersonalStats(player);
                case "gems", "usage" -> showGemUsage(player);
                default -> player.sendMessage("§c/bliss stats [top|me|gems]");
            }
        }
    }

    private void showTopKillers(Player player) {
        player.sendMessage("§6§m" + "═".repeat(50));
        player.sendMessage("§e§l🏆 TOP KILLERS 🏆");
        player.sendMessage("§6§m" + "═".repeat(50));

        List<Map.Entry<UUID, Integer>> topKillers = plugin.getStatsManager().getTopKillers(10);

        if (topKillers.isEmpty()) {
            player.sendMessage("§7No kills recorded yet.");
        } else {
            int rank = 1;
            for (Map.Entry<UUID, Integer> entry : topKillers) {
                Player target = Bukkit.getPlayer(entry.getKey());
                String playerName = target != null ? target.getName() : "Unknown";
                String medal = switch (rank) {
                    case 1 -> "🥇";
                    case 2 -> "🥈";
                    case 3 -> "🥉";
                    default -> "  ";
                };
                player.sendMessage("§e" + medal + " §7#" + rank + " §f" + playerName + " §7- §f" + entry.getValue() + " §7kills");
                rank++;
            }
        }

        player.sendMessage("§6§m" + "═".repeat(50));
        player.sendMessage("§8Use §7/bliss stats me §8for your personal stats");
        player.sendMessage("§8Use §7/bliss stats gems §8for gem distribution");
    }

    private void showPersonalStats(Player player) {
        player.sendMessage("§6§m" + "═".repeat(50));
        player.sendMessage("§e§l📊 YOUR STATS 📊");
        player.sendMessage("§6§m" + "═".repeat(50));

        int kills = plugin.getStatsManager().getKills(player);
        int deaths = plugin.getStatsManager().getDeaths(player);
        long timePlayedMs = plugin.getStatsManager().getTimePlayed(player);
        GemType currentGem = plugin.getGemManager().getGemType(player);
        int energy = plugin.getEnergyManager().getEnergy(player);

        player.sendMessage("§7Kills: §f" + kills);
        player.sendMessage("§7Deaths: §f" + deaths);
        player.sendMessage("§7K/D Ratio: §f" + (deaths == 0 ? kills : String.format("%.2f", (double) kills / deaths)));
        player.sendMessage("§7Time Played: §f" + formatTime(timePlayedMs));
        player.sendMessage("§7Current Gem: §f" + (currentGem != null ? currentGem.getDisplayName() : "None"));
        player.sendMessage("§7Energy: §f" + energy + "§8/§f10");

        player.sendMessage("§6§m" + "═".repeat(50));
    }

    private void showGemUsage(Player player) {
        player.sendMessage("§6§m" + "═".repeat(50));
        player.sendMessage("§e§l💎 GEM DISTRIBUTION 💎");
        player.sendMessage("§6§m" + "═".repeat(50));

        Map<String, Integer> gemUsage = plugin.getStatsManager().getGemUsageStats();

        for (Map.Entry<String, Integer> entry : gemUsage.entrySet()) {
            String gem = entry.getKey();
            int count = entry.getValue();
            String icon = switch (gem) {
                case "Fire Gem" -> "🔥";
                case "Speed Gem" -> "⚡";
                case "Wealth Gem" -> "💰";
                case "Astra Gem" -> "✨";
                case "Puff Gem" -> "💨";
                case "Flux Gem" -> "⚡";
                case "Life Gem" -> "❤️";
                case "Strength Gem" -> "💪";
                default -> "  ";
            };
            player.sendMessage("§7" + icon + " " + gem + ": §f" + count + " §7player" + (count == 1 ? "" : "s"));
        }

        player.sendMessage("§6§m" + "═".repeat(50));
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
}
