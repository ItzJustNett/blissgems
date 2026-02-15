package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.GemType;
import dev.xoperr.blissgems.utils.CustomItemManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

public class CooldownDisplayManager {
    private final BlissGems plugin;
    private BukkitTask displayTask;

    // Abilities per gem type
    private static final Map<GemType, List<String[]>> GEM_ABILITIES = new HashMap<>();

    static {
        // Fire abilities - {key, displayName}
        GEM_ABILITIES.put(GemType.FIRE, Arrays.asList(
            new String[]{"fire-fireball", "Fireball"},
            new String[]{"fire-campfire", "Campfire"}
        ));

        // Astra abilities
        GEM_ABILITIES.put(GemType.ASTRA, Arrays.asList(
            new String[]{"astra-daggers", "Daggers"},
            new String[]{"astra-projection", "Projection"}
        ));

        // Life abilities
        GEM_ABILITIES.put(GemType.LIFE, Arrays.asList(
            new String[]{"life-drainer", "Drainer"},
            new String[]{"life-circle-of-life", "Circle"}
        ));

        // Flux abilities
        GEM_ABILITIES.put(GemType.FLUX, Arrays.asList(
            new String[]{"flux-beam", "Beam"},
            new String[]{"flux-ground", "Ground"}
        ));

        // Puff abilities
        GEM_ABILITIES.put(GemType.PUFF, Arrays.asList(
            new String[]{"puff-dash", "Dash"},
            new String[]{"puff-breezy-bash", "Bash"}
        ));

        // Speed abilities (T1: Sedative, T2: Adrenaline Rush + Speed Storm)
        GEM_ABILITIES.put(GemType.SPEED, Arrays.asList(
            new String[]{"speed-sedative", "Sedative"},        // T1 primary
            new String[]{"adrenaline-rush", "Adrenaline"},     // T2 primary
            new String[]{"speed-storm", "Storm"}               // T2 secondary (shift)
        ));

        // Strength abilities
        GEM_ABILITIES.put(GemType.STRENGTH, Arrays.asList(
            new String[]{"strength-bloodthorns", "Thorns"},
            new String[]{"strength-chad", "Chad"}
        ));

        // Wealth abilities
        GEM_ABILITIES.put(GemType.WEALTH, Arrays.asList(
            new String[]{"wealth-durability-chip", "Durability"},
            new String[]{"wealth-rich-rush", "Rush"}
        ));
    }

    public CooldownDisplayManager(BlissGems plugin) {
        this.plugin = plugin;
        startDisplayTask();
    }

    private void startDisplayTask() {
        displayTask = this.plugin.getServer().getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            for (Player player : this.plugin.getServer().getOnlinePlayers()) {
                if (!this.plugin.getGemManager().hasActiveGem(player)) {
                    continue;
                }

                // Only show when holding gem in hand
                if (!isHoldingGem(player)) {
                    continue;
                }

                // Don't override Fire gem charging display
                if (this.plugin.getFireAbilities().isCharging(player)) {
                    continue;
                }

                // Don't override Flux gem charging display
                if (this.plugin.getFluxAbilities().isCharging(player)) {
                    continue;
                }

                // Don't override Speed gem Adrenaline Rush active display
                if (this.plugin.getSpeedAbilities().isAdrenalineRushActive(player)) {
                    continue;
                }

                String cooldownDisplay = buildCooldownDisplay(player);
                if (!cooldownDisplay.isEmpty()) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(cooldownDisplay));
                }
            }
        }, 0L, 10L); // Update every 0.5 seconds
    }

    private boolean isHoldingGem(Player player) {
        return getCurrentGemInfo(player) != null;
    }

    /**
     * Get gem info with priority: Main hand > Offhand > Inventory
     */
    private GemInfo getCurrentGemInfo(Player player) {
        // Priority 1: Check main hand (for using abilities)
        org.bukkit.inventory.ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null) {
            String oraxenId = CustomItemManager.getIdByItem(mainHand);
            if (oraxenId != null && GemType.isGem(oraxenId)) {
                GemType type = GemType.fromOraxenId(oraxenId);
                int tier = GemType.getTierFromOraxenId(oraxenId);
                return new GemInfo(type, tier, "✋");
            }
        }

        // Priority 2: Check offhand (for passives)
        org.bukkit.inventory.ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null) {
            String oraxenId = CustomItemManager.getIdByItem(offHand);
            if (oraxenId != null && GemType.isGem(oraxenId)) {
                GemType type = GemType.fromOraxenId(oraxenId);
                int tier = GemType.getTierFromOraxenId(oraxenId);
                return new GemInfo(type, tier, "🛡");
            }
        }

        return null;
    }

    /**
     * Helper class to store gem information with location
     */
    private static class GemInfo {
        final GemType type;
        final int tier;
        final String location;

        GemInfo(GemType type, int tier, String location) {
            this.type = type;
            this.tier = tier;
            this.location = location;
        }
    }

    private String buildCooldownDisplay(Player player) {
        StringBuilder display = new StringBuilder();
        AbilityManager abilityManager = this.plugin.getAbilityManager();

        // Priority: Main hand > Offhand > Inventory
        GemInfo gemInfo = getCurrentGemInfo(player);
        if (gemInfo == null) {
            return "";
        }

        GemType gemType = gemInfo.type;
        int tier = gemInfo.tier;

        // Skip if gem type is null (e.g., from addon gems)
        if (gemType == null) {
            return "";
        }

        // Get gem color from GemType
        String gemColor = gemType.getColor();

        // Get abilities for this gem type
        List<String[]> abilities = GEM_ABILITIES.get(gemType);
        if (abilities == null) {
            return "";
        }

        // Format: (icon) Ready/countdown (icon) Ready/countdown (icon) Ready/countdown

        // Special handling for Speed gem (has different abilities for T1 vs T2)
        if (gemType == GemType.SPEED) {
            if (tier == 1) {
                // T1: Show only Sedative (index 0)
                String[] ability1 = abilities.get(0);
                String ability1Key = ability1[0];
                String ability1Icon = getAbilityIcon(gemType, 0);
                int remaining1 = abilityManager.getRemainingCooldown(player, ability1Key);

                display.append(ability1Icon).append(" ");
                if (remaining1 > 0) {
                    display.append("§c").append(remaining1).append("s");
                } else {
                    display.append("§aReady");
                }
            } else {
                // T2: Show Adrenaline Rush (index 1) and Speed Storm (index 2)
                String[] ability1 = abilities.get(1); // Adrenaline Rush
                String ability1Key = ability1[0];
                String ability1Icon = getAbilityIcon(gemType, 0); // Use primary icon
                int remaining1 = abilityManager.getRemainingCooldown(player, ability1Key);

                display.append(ability1Icon).append(" ");
                if (remaining1 > 0) {
                    display.append("§c").append(remaining1).append("s");
                } else {
                    display.append("§aReady");
                }

                // Show Speed Storm (shift ability)
                if (abilities.size() > 2) {
                    String[] ability2 = abilities.get(2); // Speed Storm
                    String ability2Key = ability2[0];
                    String ability2Icon = getAbilityIcon(gemType, 1);
                    int remaining2 = abilityManager.getRemainingCooldown(player, ability2Key);

                    display.append(" §7| ").append(ability2Icon).append(" ");
                    if (remaining2 > 0) {
                        display.append("§c").append(remaining2).append("s");
                    } else {
                        display.append("§aReady");
                    }
                }
            }
        } else {
            // Standard handling for all other gems
            // Ability 1 (Primary) - always show
            String[] ability1 = abilities.get(0);
            String ability1Key = ability1[0];
            String ability1Icon = getAbilityIcon(gemType, 0);
            int remaining1 = abilityManager.getRemainingCooldown(player, ability1Key);

            display.append(ability1Icon).append(" ");
            if (remaining1 > 0) {
                display.append("§c").append(remaining1).append("s");
            } else {
                display.append("§aReady");
            }

            // Ability 2 (Secondary) - only show for Tier 2
            if (tier == 2 && abilities.size() > 1) {
                String[] ability2 = abilities.get(1);
                String ability2Key = ability2[0];
                String ability2Icon = getAbilityIcon(gemType, 1);
                int remaining2 = abilityManager.getRemainingCooldown(player, ability2Key);

                display.append(" §7| ").append(ability2Icon).append(" ");
                if (remaining2 > 0) {
                    display.append("§c").append(remaining2).append("s");
                } else {
                    display.append("§aReady");
                }
            }
        }

        return display.toString();
    }

    /**
     * Get custom gem icon (placeholder Unicode that can be replaced in resource pack)
     * These use Unicode Private Use Area (U+E000 to U+F8FF) which can be customized
     */
    private String getCustomGemIcon(GemType gemType) {
        return switch (gemType) {
            case ASTRA -> "\uE000";    // Custom Astra icon
            case FIRE -> "\uE001";     // Custom Fire icon
            case FLUX -> "\uE002";     // Custom Flux icon
            case LIFE -> "\uE003";     // Custom Life icon
            case PUFF -> "\uE004";     // Custom Puff icon
            case SPEED -> "\uE005";    // Custom Speed icon
            case STRENGTH -> "\uE006"; // Custom Strength icon
            case WEALTH -> "\uE007";   // Custom Wealth icon
        };
    }

    /**
     * Get ability icon using custom Unicode characters for resource pack
     * abilityIndex: 0 = primary ability, 1 = secondary ability
     */
    private String getAbilityIcon(GemType gemType, int abilityIndex) {
        return switch (gemType) {
            case ASTRA -> abilityIndex == 0 ? "\uE010" : "\uE011";      // Daggers / Projection
            case FIRE -> abilityIndex == 0 ? "\uE012" : "\uE013";       // Fireball / Campfire
            case FLUX -> abilityIndex == 0 ? "\uE014" : "\uE015";        // Beam / Ground
            case LIFE -> abilityIndex == 0 ? "\uE016" : "\uE017";       // Drainer / Circle
            case PUFF -> abilityIndex == 0 ? "\uE018" : "\uE019";         // Dash / Bash
            case SPEED -> abilityIndex == 0 ? "\uE01A" : "\uE01B";      // Sedative / Storm
            case STRENGTH -> abilityIndex == 0 ? "\uE01C" : "\uE01D";    // Thorns/Frailer / Chad
            case WEALTH -> abilityIndex == 0 ? "\uE01E" : "\uE01F";     // Unfortunate / Rush
        };
    }

    private String getGemIcon(GemType gemType) {
        switch (gemType) {
            case ASTRA: return "✦";
            case FIRE: return "🔥";
            case FLUX: return "⚡";
            case LIFE: return "❤";
            case PUFF: return "💨";
            case SPEED: return "⚡";
            case STRENGTH: return "⚔";
            case WEALTH: return "💰";
            default: return "◆";
        }
    }

    private String getGemColor(GemType gemType) {
        switch (gemType) {
            case ASTRA: return "§5";
            case FIRE: return "§c";
            case FLUX: return "§b";
            case LIFE: return "§d";
            case PUFF: return "§f";
            case SPEED: return "§a";
            case STRENGTH: return "§6";
            case WEALTH: return "§e";
            default: return "§7";
        }
    }

    private String getEnergyBar(int energy) {
        StringBuilder bar = new StringBuilder();

        // Color based on energy
        String color;
        String icon;
        if (energy >= 8) {
            color = "§d§l"; // Purple bold for enhanced
            icon = "●";
        } else if (energy >= 5) {
            color = "§b"; // Aqua for pristine
            icon = "●";
        } else if (energy >= 3) {
            color = "§e"; // Yellow for scratched/cracked
            icon = "●";
        } else if (energy >= 2) {
            color = "§6"; // Orange for shattered
            icon = "●";
        } else if (energy == 1) {
            color = "§4"; // Dark red for ruined
            icon = "○";
        } else {
            color = "§c§l"; // Red bold for broken
            icon = "○";
        }

        bar.append(color);
        for (int i = 0; i < 10; i++) {
            if (i < energy) {
                bar.append(icon);
            } else {
                bar.append("§8○");
            }
        }
        bar.append(" §7(").append(energy).append("/10)");

        return bar.toString();
    }

    private String getCooldownBar(int remaining, int total, String abilityName) {
        double percentage = 1.0 - ((double) remaining / total);
        int bars = (int) (percentage * 10);

        StringBuilder bar = new StringBuilder();

        // Color based on remaining time
        String color;
        if (remaining <= 3) {
            color = "§a"; // Green - almost ready
        } else if (remaining <= 10) {
            color = "§e"; // Yellow - soon
        } else if (remaining <= 30) {
            color = "§6"; // Orange - medium
        } else {
            color = "§c"; // Red - long wait
        }

        bar.append(color).append(abilityName).append(" ");

        // Progress bar
        bar.append("§7[");
        for (int i = 0; i < 10; i++) {
            if (i < bars) {
                bar.append("§a▰");
            } else {
                bar.append("§8▱");
            }
        }
        bar.append("§7] ").append(color).append(remaining).append("s");

        return bar.toString();
    }

    public void stop() {
        if (displayTask != null) {
            displayTask.cancel();
        }
    }
}
