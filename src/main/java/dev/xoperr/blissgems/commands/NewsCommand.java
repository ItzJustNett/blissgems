package dev.xoperr.blissgems.commands;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.managers.MythicWorldEventManager;
import dev.xoperr.blissgems.managers.SpawnBeaconManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class NewsCommand implements CommandExecutor {
    private final BlissGems plugin;

    public NewsCommand(BlissGems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&l================== &e&lBLISSGEMS NEWS &6&l=================="));
        sender.sendMessage("");

        // 1. Broken Mythic Respawns
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e✦ &6&lMYTHIC ARTIFACT RESPAWNS:"));
        MythicWorldEventManager mythicManager = plugin.getMythicWorldEventManager();
        if (mythicManager != null) {
            List<MythicWorldEventManager.BrokenMythicEntry> broken = mythicManager.getAllEntries();
            if (broken.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "  • All mythic artifacts are intact and in player hands.");
            } else {
                for (MythicWorldEventManager.BrokenMythicEntry entry : broken) {
                    if (entry.isPending()) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "  &7• &f" + entry.displayName + "&7: &cRespawns in " + entry.getRemainingFormatted()));
                    } else {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "  &7• &f" + entry.displayName + "&7: &aReformed at X: " + entry.respawnX + ", Y: " + entry.respawnY + ", Z: " + entry.respawnZ));
                    }
                }
            }
        } else {
            sender.sendMessage(ChatColor.GRAY + "  • World event manager offline.");
        }
        sender.sendMessage("");

        // 2. Spawn Beacon Repairs Schedule
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b✦ &3&lSPAWN BEACON GEM REPAIRS:"));
        SpawnBeaconManager beaconManager = plugin.getSpawnBeaconManager();
        if (beaconManager != null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "  &7• Status: " + beaconManager.getRepairScheduleStatusString()));
            if (beaconManager.getBeaconLocation() != null) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "  &7• Coordinates: &fX: " + (int) beaconManager.getBeaconLocation().getX() +
                                ", Y: " + (int) beaconManager.getBeaconLocation().getY() +
                                ", Z: " + (int) beaconManager.getBeaconLocation().getZ()));
            }
        }
        sender.sendMessage("");

        // 3. Combat Rules & Active Settings
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d✦ &5&lCOMBAT & WORLD RULES:"));
        int pearlCd = plugin.getEnchantLimiterManager() != null ? plugin.getEnchantLimiterManager().getEnderPearlCooldownSeconds() : 15;
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &7• Ender Pearl Cooldown: &e" + pearlCd + "s"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &7• Tempered Mace: &cMax Density II &7| &cNo Breach/WindBurst &7| &660s Combat CD"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &7• Mythic Netherite Set: &aProt III &7| &d2x Durability (50% damage reduction)"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &7• Merchants: &e1 Trade per item per player &7| &dShift+RClick for Soul Form"));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6&l====================================================="));
        sender.sendMessage("");
        return true;
    }
}
