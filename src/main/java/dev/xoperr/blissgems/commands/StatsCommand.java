/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 */
package dev.xoperr.blissgems.commands;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.GemType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class StatsCommand {
    private final BlissGems plugin;

    public StatsCommand(BlissGems plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player, String[] args) {
        if (args.length == 0) {
            this.showTopKillers(player);
        } else {
            switch (args[0].toLowerCase()) {
                case "top": {
                    this.showTopKillers(player);
                    break;
                }
                case "me": 
                case "my": 
                case "personal": {
                    this.showPersonalStats(player);
                    break;
                }
                case "gems": 
                case "usage": {
                    this.showGemUsage(player);
                    break;
                }
                default: {
                    player.sendMessage("\u00a7c/bliss stats [top|me|gems]");
                }
            }
        }
    }

    private void showTopKillers(Player player) {
        player.sendMessage("\u00a76\u00a7m" + "\u2550".repeat(50));
        player.sendMessage("\u00a7e\u00a7l\ud83c\udfc6 TOP KILLERS \ud83c\udfc6");
        player.sendMessage("\u00a76\u00a7m" + "\u2550".repeat(50));
        List<Map.Entry<UUID, Integer>> topKillers = this.plugin.getStatsManager().getTopKillers(10);
        if (topKillers.isEmpty()) {
            player.sendMessage("\u00a77No kills recorded yet.");
        } else {
            int rank = 1;
            for (Map.Entry<UUID, Integer> entry : topKillers) {
                Player target = Bukkit.getPlayer((UUID)entry.getKey());
                String playerName = target != null ? target.getName() : "Unknown";
                String medal = switch (rank) {
                    case 1 -> "\ud83e\udd47";
                    case 2 -> "\ud83e\udd48";
                    case 3 -> "\ud83e\udd49";
                    default -> "  ";
                };
                player.sendMessage("\u00a7e" + medal + " \u00a77#" + rank + " \u00a7f" + playerName + " \u00a77- \u00a7f" + String.valueOf(entry.getValue()) + " \u00a77kills");
                ++rank;
            }
        }
        player.sendMessage("\u00a76\u00a7m" + "\u2550".repeat(50));
        player.sendMessage("\u00a78Use \u00a77/bliss stats me \u00a78for your personal stats");
        player.sendMessage("\u00a78Use \u00a77/bliss stats gems \u00a78for gem distribution");
    }

    private void showPersonalStats(Player player) {
        player.sendMessage("\u00a76\u00a7m" + "\u2550".repeat(50));
        player.sendMessage("\u00a7e\u00a7l\ud83d\udcca YOUR STATS \ud83d\udcca");
        player.sendMessage("\u00a76\u00a7m" + "\u2550".repeat(50));
        int kills = this.plugin.getStatsManager().getKills(player);
        int deaths = this.plugin.getStatsManager().getDeaths(player);
        long timePlayedMs = this.plugin.getStatsManager().getTimePlayed(player);
        GemType currentGem = this.plugin.getGemManager().getGemType(player);
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        player.sendMessage("\u00a77Kills: \u00a7f" + kills);
        player.sendMessage("\u00a77Deaths: \u00a7f" + deaths);
        player.sendMessage("\u00a77K/D Ratio: \u00a7f" + String.valueOf(deaths == 0 ? Integer.valueOf(kills) : String.format("%.2f", (double)kills / (double)deaths)));
        player.sendMessage("\u00a77Time Played: \u00a7f" + this.formatTime(timePlayedMs));
        player.sendMessage("\u00a77Current Gem: \u00a7f" + (currentGem != null ? currentGem.getDisplayName() : "None"));
        player.sendMessage("\u00a77Energy: \u00a7f" + energy + "\u00a78/\u00a7f10");
        player.sendMessage("\u00a76\u00a7m" + "\u2550".repeat(50));
    }

    private void showGemUsage(Player player) {
        player.sendMessage("\u00a76\u00a7m" + "\u2550".repeat(50));
        player.sendMessage("\u00a7e\u00a7l\ud83d\udc8e GEM DISTRIBUTION \ud83d\udc8e");
        player.sendMessage("\u00a76\u00a7m" + "\u2550".repeat(50));
        Map<String, Integer> gemUsage = this.plugin.getStatsManager().getGemUsageStats();
        for (Map.Entry<String, Integer> entry : gemUsage.entrySet()) {
            String gem = entry.getKey();
            int count = entry.getValue();
            String icon = switch (gem) {
                case "Fire Gem" -> "\ud83d\udd25";
                case "Speed Gem" -> "\u26a1";
                case "Wealth Gem" -> "\ud83d\udcb0";
                case "Astra Gem" -> "\u2728";
                case "Puff Gem" -> "\ud83d\udca8";
                case "Flux Gem" -> "\u26a1";
                case "Life Gem" -> "\u2764\ufe0f";
                case "Strength Gem" -> "\ud83d\udcaa";
                default -> "  ";
            };
            player.sendMessage("\u00a77" + icon + " " + gem + ": \u00a7f" + count + " \u00a77player" + (count == 1 ? "" : "s"));
        }
        player.sendMessage("\u00a76\u00a7m" + "\u2550".repeat(50));
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        long days = hours / 24L;
        if (days > 0L) {
            return days + "d " + hours % 24L + "h";
        }
        if (hours > 0L) {
            return hours + "h " + minutes % 60L + "m";
        }
        if (minutes > 0L) {
            return minutes + "m " + seconds % 60L + "s";
        }
        return seconds + "s";
    }
}

