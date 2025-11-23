package fun.obriy.blissgems.managers;

import fun.obriy.blissgems.BlissGems;
import fun.obriy.blissgems.utils.GemType;
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
            new String[]{"flux-ground", "Ground"},
            new String[]{"flux-chain-lightning", "Chain"}
        ));

        // Puff abilities
        GEM_ABILITIES.put(GemType.PUFF, Arrays.asList(
            new String[]{"puff-dash", "Dash"},
            new String[]{"puff-breezy-bash", "Bash"}
        ));

        // Speed abilities
        GEM_ABILITIES.put(GemType.SPEED, Arrays.asList(
            new String[]{"speed-sedative", "Sedative"},
            new String[]{"speed-storm", "Storm"}
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

                String cooldownDisplay = buildCooldownDisplay(player);
                if (!cooldownDisplay.isEmpty()) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(cooldownDisplay));
                }
            }
        }, 0L, 10L); // Update every 0.5 seconds
    }

    private boolean isHoldingGem(Player player) {
        // Check main hand
        org.bukkit.inventory.ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null) {
            String oraxenId = io.th0rgal.oraxen.api.OraxenItems.getIdByItem(mainHand);
            if (oraxenId != null && GemType.isGem(oraxenId)) {
                return true;
            }
        }

        // Check offhand
        org.bukkit.inventory.ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null) {
            String oraxenId = io.th0rgal.oraxen.api.OraxenItems.getIdByItem(offHand);
            if (oraxenId != null && GemType.isGem(oraxenId)) {
                return true;
            }
        }

        return false;
    }

    private String buildCooldownDisplay(Player player) {
        StringBuilder display = new StringBuilder();
        AbilityManager abilityManager = this.plugin.getAbilityManager();

        // Get player's current gem type
        GemType gemType = this.plugin.getGemManager().getGemType(player);
        if (gemType == null) {
            return "";
        }

        // Get abilities for this gem type
        List<String[]> abilities = GEM_ABILITIES.get(gemType);
        if (abilities == null) {
            return "";
        }

        boolean first = true;
        for (String[] ability : abilities) {
            String abilityKey = ability[0];
            String displayName = ability[1];

            int remaining = abilityManager.getRemainingCooldown(player, abilityKey);

            if (!first) {
                display.append(" \u00a77| ");
            }
            first = false;

            if (remaining > 0) {
                // Color based on remaining time
                String color;
                if (remaining <= 3) {
                    color = "\u00a7a"; // Green - almost ready
                } else if (remaining <= 10) {
                    color = "\u00a7e"; // Yellow - soon
                } else {
                    color = "\u00a7c"; // Red - waiting
                }
                display.append(color).append(displayName).append(": ").append(remaining).append("s");
            } else {
                // Ready to use
                display.append("\u00a72").append(displayName).append(": \u00a7aReady");
            }
        }

        return display.toString();
    }

    public void stop() {
        if (displayTask != null) {
            displayTask.cancel();
        }
    }
}
