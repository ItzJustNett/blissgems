/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitRunnable
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class PassiveManager {
    private final BlissGems plugin;

    public PassiveManager(BlissGems plugin) {
        this.plugin = plugin;
        this.startPassiveEffectTask();
    }

    private void startPassiveEffectTask() {
        int interval = this.plugin.getConfigManager().getPassiveUpdateInterval();
        new BukkitRunnable(){

            public void run() {
                for (Player player : PassiveManager.this.plugin.getServer().getOnlinePlayers()) {
                    if (!PassiveManager.this.plugin.getGemManager().hasGemInOffhand(player) || !PassiveManager.this.plugin.getEnergyManager().arePassivesActive(player)) continue;
                    PassiveManager.this.applyPassiveEffects(player);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, (long)interval, (long)interval);
    }

    private void applyPassiveEffects(Player player) {
        GemType gemType = this.plugin.getGemManager().getGemTypeFromOffhand(player);
        if (gemType == null) {
            return;
        }
        switch (gemType) {
            case ASTRA: {
                this.applyAstraPassives(player);
                break;
            }
            case FIRE: {
                this.applyFirePassives(player);
                break;
            }
            case FLUX: {
                this.applyFluxPassives(player);
                break;
            }
            case LIFE: {
                this.applyLifePassives(player);
                break;
            }
            case PUFF: {
                this.applyPuffPassives(player);
                break;
            }
            case SPEED: {
                this.applySpeedPassives(player);
                break;
            }
            case STRENGTH: {
                this.applyStrengthPassives(player);
                break;
            }
            case WEALTH: {
                this.applyWealthPassives(player);
            }
        }
    }

    private void applyAstraPassives(Player player) {
    }

    private void applyFirePassives(Player player) {
        int tier = this.plugin.getGemManager().getTierFromOffhand(player);
        if (this.plugin.getConfigManager().isFireResistanceEnabled(tier)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60, 0, true, false));
        }
    }

    private void applyFluxPassives(Player player) {
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.HUNGER);
    }

    private void applyLifePassives(Player player) {
        player.removePotionEffect(PotionEffectType.WITHER);
        int tier = this.plugin.getGemManager().getTierFromOffhand(player);
        double healAmount = this.plugin.getConfigManager().getLifeHealAmount(tier);
        double currentHealth = player.getHealth();
        double maxHealth = player.getMaxHealth();
        if (currentHealth < maxHealth) {
            player.setHealth(Math.min(maxHealth, currentHealth + healAmount));
        }
    }

    private void applyPuffPassives(Player player) {
    }

    private void applySpeedPassives(Player player) {
        int tier = this.plugin.getGemManager().getTierFromOffhand(player);
        int speedLevel = this.plugin.getConfigManager().getSpeedLevel(tier);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, speedLevel, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 60, 0, true, false));
    }

    private void applyStrengthPassives(Player player) {
        int tier = this.plugin.getGemManager().getTierFromOffhand(player);
        int strengthLevel = this.plugin.getConfigManager().getStrengthLevel(tier);
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, strengthLevel, true, false));
    }

    private void applyWealthPassives(Player player) {
        int tier = this.plugin.getGemManager().getTierFromOffhand(player);
        int luckLevel = this.plugin.getConfigManager().getLuckLevel(tier);
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 60, luckLevel, true, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 60, 0, true, false));
    }
}

