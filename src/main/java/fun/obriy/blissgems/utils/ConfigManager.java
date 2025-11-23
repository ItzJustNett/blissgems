/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 */
package fun.obriy.blissgems.utils;

import fun.obriy.blissgems.BlissGems;
import fun.obriy.blissgems.utils.GemType;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final BlissGems plugin;
    private FileConfiguration config;

    public ConfigManager(BlissGems plugin) {
        this.plugin = plugin;
        this.reload();
    }

    public void reload() {
        this.plugin.reloadConfig();
        this.config = this.plugin.getConfig();
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

    public boolean isGemEnabled(GemType type) {
        return this.config.getBoolean("gems.enabled." + type.getId(), true);
    }

    public boolean isSingleGemOnly() {
        return this.config.getBoolean("gems.single-gem-only", true);
    }

    public int getPassiveUpdateInterval() {
        return this.config.getInt("passives.update-interval", 20);
    }

    public double getPhaseChance() {
        return this.config.getDouble("passives.astra.phase-chance", 0.15);
    }

    public double getLifeHealAmount() {
        return this.config.getDouble("passives.life.heal-amount", 0.5);
    }

    public int getLifeHealInterval() {
        return this.config.getInt("passives.life.heal-interval", 100);
    }

    public double getUndeadDamageMultiplier() {
        return this.config.getDouble("passives.life.undead-damage-multiplier", 3.0);
    }

    public double getSaturationMultiplier() {
        return this.config.getDouble("passives.life.saturation-multiplier", 2.0);
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
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 >= replacements.length) continue;
            message = message.replace("{" + String.valueOf(replacements[i]) + "}", String.valueOf(replacements[i + 1]));
        }
        return this.getPrefix() + " " + message;
    }
}

