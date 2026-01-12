/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 */
package dev.xoperr.blissgems.utils;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.logging.Level;

public class ConfigManager {
    private final BlissGems plugin;
    private FileConfiguration config;
    private static final String CONFIG_VERSION = "1.0.0";

    public ConfigManager(BlissGems plugin) {
        this.plugin = plugin;
        this.reload();
        this.autoRepairConfig();
    }

    public void reload() {
        this.plugin.reloadConfig();
        this.config = this.plugin.getConfig();
    }

    /**
     * Auto-repairs broken or outdated configs by adding missing keys
     */
    private void autoRepairConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");

        // If config doesn't exist, saveDefaultConfig will handle it
        if (!configFile.exists()) {
            return;
        }

        // Load default config from JAR
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
            new java.io.InputStreamReader(
                plugin.getResource("config.yml"),
                java.nio.charset.StandardCharsets.UTF_8
            )
        );

        // Check version
        String currentVersion = config.getString("config-version", "unknown");
        String expectedVersion = defaultConfig.getString("config-version", CONFIG_VERSION);
        boolean isOutdated = !currentVersion.equals(expectedVersion);

        // Check if repair is needed
        boolean needsRepair = false;
        int missingKeys = 0;

        // Get all keys from default config
        Set<String> defaultKeys = defaultConfig.getKeys(true);

        for (String key : defaultKeys) {
            // Skip checking values that aren't leaf nodes
            if (defaultConfig.isConfigurationSection(key)) {
                continue;
            }

            // If key is missing, mark for repair
            if (!config.contains(key)) {
                needsRepair = true;
                missingKeys++;
            }
        }

        // If no repair needed, exit early
        if (!needsRepair) {
            // Just update version if outdated but complete
            if (isOutdated) {
                config.set("config-version", expectedVersion);
                try {
                    config.save(configFile);
                    plugin.getLogger().info("Updated config version from " + currentVersion + " to " + expectedVersion);
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to update config version: " + e.getMessage());
                }
            }
            return;
        }

        // Backup old config
        try {
            File backupFile = new File(plugin.getDataFolder(), "config.yml.backup");
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Created config backup: config.yml.backup");
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to backup config: " + e.getMessage());
        }

        // Add missing keys
        if (isOutdated) {
            plugin.getLogger().warning("Detected outdated config! (v" + currentVersion + " -> v" + expectedVersion + ")");
        }
        plugin.getLogger().warning("Auto-repairing config: " + missingKeys + " missing entries detected.");

        for (String key : defaultKeys) {
            if (defaultConfig.isConfigurationSection(key)) {
                continue;
            }

            if (!config.contains(key)) {
                config.set(key, defaultConfig.get(key));
                plugin.getLogger().info("  + Added: " + key);
            }
        }

        // Update version to latest
        config.set("config-version", expectedVersion);

        // Save repaired config
        try {
            config.save(configFile);
            plugin.getLogger().info("Config auto-repair complete! " + missingKeys + " keys added.");
            plugin.getLogger().info("Your old config was backed up to config.yml.backup");
            this.reload(); // Reload to use the repaired config
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save repaired config!", e);
        }
    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    public int getEnergyGainOnKill() {
        return this.config.getInt("energy.gain-on-kill", 1);
    }

    public int getEnergyLossOnDeath() {
        return this.config.getInt("energy.loss-on-death", 1);
    }

    public int getMaxEnergy() {
        return this.config.getInt("energy.max-energy", 10);
    }

    public int getStartingEnergy() {
        return this.config.getInt("energy.starting-energy", 5);
    }

    public int getRuinedThreshold() {
        return this.config.getInt("energy.ruined-threshold", 1);
    }

    public int getBrokenThreshold() {
        return this.config.getInt("energy.broken-threshold", 0);
    }

    public boolean isBanOnZeroEnergyEnabled() {
        return this.config.getBoolean("energy.ban-on-zero-energy", false);
    }

    public void setBanOnZeroEnergy(boolean enabled) {
        this.config.set("energy.ban-on-zero-energy", enabled);
        try {
            this.config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save config: " + e.getMessage());
        }
    }

    public boolean isGemEnabled(GemType type) {
        return this.config.getBoolean("gems.enabled." + type.getId(), true);
    }

    public boolean isSingleGemOnly() {
        return this.config.getBoolean("gems.single-gem-only", true);
    }

    public int getPassiveUpdateInterval() {
        return this.config.getInt("passives.update-interval", 20);
    }

    // ==========================================
    // TIER-SPECIFIC PASSIVE GETTERS
    // ==========================================

    // Astra Passives
    public double getPhaseChance(int tier) {
        String path = "passives.astra.tier" + tier + ".phase-chance";
        return this.config.getDouble(path, tier == 1 ? 0.10 : 0.15);
    }

    // Life Passives
    public double getLifeHealAmount(int tier) {
        String path = "passives.life.tier" + tier + ".heal-amount";
        return this.config.getDouble(path, tier == 1 ? 0.3 : 0.5);
    }

    public int getLifeHealInterval(int tier) {
        String path = "passives.life.tier" + tier + ".heal-interval";
        return this.config.getInt(path, 100);
    }

    public double getUndeadDamageMultiplier(int tier) {
        String path = "passives.life.tier" + tier + ".undead-damage-multiplier";
        return this.config.getDouble(path, tier == 1 ? 2.0 : 3.0);
    }

    public double getSaturationMultiplier(int tier) {
        String path = "passives.life.tier" + tier + ".saturation-multiplier";
        return this.config.getDouble(path, tier == 1 ? 1.5 : 2.0);
    }

    public int getGoldenAppleAbsorptionLevel(int tier) {
        String path = "passives.life.tier" + tier + ".golden-apple-absorption-level";
        return this.config.getInt(path, tier == 1 ? 0 : 1);
    }

    // Flux Passives
    public double getShockingArrowDamage(int tier) {
        String path = "passives.flux.tier" + tier + ".shocking-arrow-damage";
        return this.config.getDouble(path, tier == 1 ? 2.0 : 3.0);
    }

    public double getFlowStateSpeedBoost(int tier) {
        String path = "passives.flux.tier" + tier + ".flow-state-speed-boost";
        return this.config.getDouble(path, tier == 1 ? 0.15 : 0.2);
    }

    // Puff Passives
    public boolean isDoubleJumpEnabled(int tier) {
        String path = "passives.puff.tier" + tier + ".double-jump-enabled";
        return this.config.getBoolean(path, true);
    }

    public double getDoubleJumpVelocity(int tier) {
        String path = "passives.puff.tier" + tier + ".double-jump-velocity";
        return this.config.getDouble(path, tier == 1 ? 0.6 : 0.8);
    }

    public double getLaunchVelocity(int tier) {
        String path = "passives.puff.tier" + tier + ".launch-velocity";
        return this.config.getDouble(path, tier == 1 ? 3.5 : 4.5);
    }

    public boolean isFallDamageImmunity(int tier) {
        String path = "passives.puff.tier" + tier + ".fall-damage-immunity";
        return this.config.getBoolean(path, true);
    }

    public boolean isSculkImmunity(int tier) {
        String path = "passives.puff.tier" + tier + ".sculk-immunity";
        return this.config.getBoolean(path, tier == 2);
    }

    // Fire Passives
    public boolean isAutoSmeltEnabled(int tier) {
        String path = "passives.fire.tier" + tier + ".auto-smelt";
        return this.config.getBoolean(path, true);
    }

    public boolean isFireResistanceEnabled(int tier) {
        String path = "passives.fire.tier" + tier + ".fire-resistance";
        return this.config.getBoolean(path, tier == 2);
    }

    // Speed Passives
    public boolean isSoulSandImmunity(int tier) {
        String path = "passives.speed.tier" + tier + ".soul-sand-immunity";
        return this.config.getBoolean(path, true);
    }

    public int getSpeedLevel(int tier) {
        String path = "passives.speed.tier" + tier + ".speed-level";
        return this.config.getInt(path, tier == 1 ? 0 : 1);
    }

    // Strength Passives
    public int getStrengthLevel(int tier) {
        String path = "passives.strength.tier" + tier + ".strength-level";
        return this.config.getInt(path, tier == 1 ? 0 : 1);
    }

    // Wealth Passives
    public int getLuckLevel(int tier) {
        String path = "passives.wealth.tier" + tier + ".luck-level";
        return this.config.getInt(path, tier == 1 ? 0 : 1);
    }

    public double getVillagerDiscount(int tier) {
        String path = "passives.wealth.tier" + tier + ".villager-discount";
        return this.config.getDouble(path, tier == 1 ? 0.10 : 0.20);
    }

    // ==========================================
    // LEGACY GETTERS (for backwards compatibility - will use tier 2 defaults)
    // ==========================================

    @Deprecated
    public double getPhaseChance() {
        return getPhaseChance(2);
    }

    @Deprecated
    public double getLifeHealAmount() {
        return getLifeHealAmount(2);
    }

    @Deprecated
    public int getLifeHealInterval() {
        return getLifeHealInterval(2);
    }

    @Deprecated
    public double getUndeadDamageMultiplier() {
        return getUndeadDamageMultiplier(2);
    }

    @Deprecated
    public double getSaturationMultiplier() {
        return getSaturationMultiplier(2);
    }

    public int getGlobalAbilityCooldown() {
        return this.config.getInt("abilities.global-cooldown", 1);
    }

    public int getAbilityCooldown(String abilityKey) {
        return this.config.getInt("abilities.cooldowns." + abilityKey, 10);
    }

    public double getAbilityDamage(String abilityKey) {
        return this.config.getDouble("abilities.damage." + abilityKey, 4.0);
    }

    public int getAbilityDuration(String abilityKey) {
        return this.config.getInt("abilities.durations." + abilityKey, 10);
    }

    public boolean isAutoEnchantEnabled() {
        return this.config.getBoolean("auto-enchant.enabled", true);
    }

    public boolean shouldPlayUpgradeEffects() {
        return this.config.getBoolean("upgrader.play-effects", true);
    }

    public String getUpgradeParticle() {
        return this.config.getString("upgrader.particle", "ENCHANT");
    }

    public int getUpgradeParticleCount() {
        return this.config.getInt("upgrader.particle-count", 50);
    }

    public String getUpgradeSound() {
        return this.config.getString("upgrader.sound", "ENTITY_PLAYER_LEVELUP");
    }

    public int getTraderCooldown() {
        return this.config.getInt("trader.cooldown", 5);
    }

    public boolean shouldPlayTradeEffects() {
        return this.config.getBoolean("trader.play-effects", true);
    }

    public boolean isEnergyBottleDropEnabled() {
        return this.config.getBoolean("energy-bottle.drop-enabled", true);
    }

    public int getRepairKitEnergyPerSecond() {
        return this.config.getInt("repair-kit.energy-per-second", 1);
    }

    public int getRepairKitMaxEnergy() {
        return this.config.getInt("repair-kit.max-total-energy", 10);
    }

    public int getRepairKitRadius() {
        return this.config.getInt("repair-kit.heal-radius", 10);
    }

    public int getRepairKitUpdateInterval() {
        return this.config.getInt("repair-kit.update-interval", 20);
    }

    public boolean shouldPrioritizeLowestEnergy() {
        return this.config.getBoolean("repair-kit.prioritize-lowest", true);
    }

    public String getMessage(String key) {
        return this.config.getString("messages." + key, "").replace("&", "\u00a7");
    }

    public String getPrefix() {
        return this.getMessage("prefix");
    }

    public String getFormattedMessage(String key, Object ... replacements) {
        String message = this.getMessage(key);

        // If message is empty or just whitespace, return NULL (not empty string)
        // This way callers can check for null and skip sendMessage
        if (message == null || message.trim().isEmpty()) {
            return null;
        }

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 >= replacements.length) continue;
            message = message.replace("{" + String.valueOf(replacements[i]) + "}", String.valueOf(replacements[i + 1]));
        }

        // Add prefix with proper spacing and ensure message is visible
        String result = this.getPrefix() + " \u00a7f" + message;

        // Final check: if result is effectively just the prefix, return null
        String cleanResult = result.replaceAll("§[0-9a-fk-or]", "").trim();
        if (cleanResult.equals("BlissGems »") || cleanResult.isEmpty()) {
            return null;
        }

        return result;
    }

    // Helper method that safely sends formatted messages (won't send if null/empty)
    public void sendFormattedMessage(org.bukkit.command.CommandSender sender, String key, Object... replacements) {
        String message = getFormattedMessage(key, replacements);
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(message);
        }
    }
}

