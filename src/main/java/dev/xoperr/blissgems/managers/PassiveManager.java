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
import dev.xoperr.blissgems.api.GemPassiveHandler;
import dev.xoperr.blissgems.api.GemRegistry;
import dev.xoperr.blissgems.managers.FlowStateManager;
import dev.xoperr.blissgems.managers.GemRegistryImpl;
import dev.xoperr.blissgems.utils.GemType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
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
                    if (player.isDead() || !PassiveManager.this.plugin.getGemManager().hasGemForPassives(player) || !PassiveManager.this.plugin.getEnergyManager().arePassivesActive(player)) continue;
                    PassiveManager.this.applyPassiveEffects(player);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, (long)interval, (long)interval);
    }

    private void applyPassiveEffects(Player player) {
        GemRegistryImpl registry;
        if (this.plugin.getGemLockManager() != null && this.plugin.getGemLockManager().isLocked(player)) {
            return;
        }
        if (this.plugin.getRegionManager() != null && this.plugin.getRegionManager().areGemsDisabled(player)) {
            return;
        }
        int tier = this.plugin.getGemManager().getTierForPassives(player);
        String gemId = this.plugin.getGemManager().getGemIdForPassives(player);
        GemType gemType = this.plugin.getGemManager().getGemTypeForPassives(player);
        if (gemType != null) {
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
            this.applyConfiguredExtraEffects(player, gemType, tier);
            return;
        }
        if (gemId != null && (registry = this.plugin.getGemRegistry()) != null) {
            GemPassiveHandler handler = registry.getPassiveHandler(gemId);
            if (handler != null) {
                handler.applyPassives(player, tier);
            }
            this.applyConfiguredExtraEffectsById(player, gemId, tier);
        }
    }

    private void applyConfiguredExtraEffectsById(Player player, String gemId, int tier) {
        String path = "passives.extra-effects." + gemId + ".t" + tier;
        List<Map<?, ?>> entries = this.plugin.getConfig().getMapList(path);
        if (entries == null || entries.isEmpty()) {
            return;
        }
        int duration = this.plugin.getConfigManager().getPassiveUpdateInterval() + 10;
        for (Map<?, ?> entry : entries) {
            Object typeObj = entry.get("type");
            if (typeObj == null) continue;
            PotionEffectType type = PotionEffectType.getByName((String)typeObj.toString().toUpperCase());
            if (type == null) {
                this.plugin.getLogger().warning("Unknown potion effect '" + String.valueOf(typeObj) + "' in " + path);
                continue;
            }
            Object levelObj = entry.get("level");
            int level = levelObj instanceof Number ? ((Number)levelObj).intValue() : 1;
            int amplifier = Math.max(0, level - 1);
            player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false), true);
        }
    }

    private void applyConfiguredExtraEffects(Player player, GemType gemType, int tier) {
        String path = "passives.extra-effects." + gemType.name().toLowerCase() + ".t" + tier;
        List<Map<?, ?>> entries = this.plugin.getConfig().getMapList(path);
        if (entries == null || entries.isEmpty()) {
            return;
        }
        int duration = this.plugin.getConfigManager().getPassiveUpdateInterval() + 10;
        for (Map<?, ?> entry : entries) {
            Object typeObj = entry.get("type");
            if (typeObj == null) continue;
            PotionEffectType type = PotionEffectType.getByName((String)typeObj.toString().toUpperCase());
            if (type == null) {
                this.plugin.getLogger().warning("Unknown potion effect '" + String.valueOf(typeObj) + "' in " + path);
                continue;
            }
            Object levelObj = entry.get("level");
            int level = levelObj instanceof Number ? ((Number)levelObj).intValue() : 1;
            int amplifier = Math.max(0, level - 1);
            player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false), true);
        }
    }

    private void applyAstraPassives(Player player) {
    }

    private void applyFirePassives(Player player) {
        int tier = this.plugin.getGemManager().getTierFromOffhand(player);
        if (this.plugin.getConfigManager().isFireResistanceEnabled(tier)) {
            int interval = this.plugin.getConfigManager().getPassiveUpdateInterval();
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, interval + 10, 0, true, false), true);
        }
    }

    private void applyFluxPassives(Player player) {
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.HUNGER);
        if (player.isSprinting()) {
            this.plugin.getFlowStateManager().registerAction(player, FlowStateManager.ActionType.SPRINT);
        }
    }

    private void applyLifePassives(Player player) {
        if (player.isDead()) {
            return;
        }
        player.removePotionEffect(PotionEffectType.WITHER);
        int tier = this.plugin.getGemManager().getTierFromOffhand(player);
        double healAmount = this.plugin.getConfigManager().getLifeHealAmount(tier);
        double currentHealth = player.getHealth();
        double maxHealth = player.getMaxHealth();
        if (currentHealth < maxHealth) {
            player.setHealth(Math.min(maxHealth, currentHealth + healAmount));
        }
        int absorptionBase = this.plugin.getConfig().getInt("passives.life.tier" + tier + ".absorption-per-block", 0);
        if (absorptionBase > 0) {
            int radius = this.plugin.getConfig().getInt("passives.life.absorption-scan-radius", 5);
            Map<Material, Integer> blockCounts = new HashMap<Material, Integer>();
            Location loc = player.getLocation();
            for (int x = -radius; x <= radius; ++x) {
                for (int y = -radius; y <= radius; ++y) {
                    for (int z = -radius; z <= radius; ++z) {
                        Block b = loc.getBlock().getRelative(x, y, z);
                        Material mat = b.getType();
                        if (mat != Material.AIR && (mat.isSolid() || Tag.LEAVES.isTagged(mat) || mat.name().contains("MOSS") || mat.name().contains("GRASS"))) {
                            blockCounts.put(mat, blockCounts.getOrDefault(mat, 0) + 1);
                        }
                    }
                }
            }
            int totalAbsorption = 0;
            for (int count : blockCounts.values()) {
                totalAbsorption += absorptionBase * count;
            }
            int maxAbsorption = this.plugin.getConfig().getInt("passives.life.max-absorption-hearts", 20);
            totalAbsorption = Math.min(totalAbsorption, maxAbsorption);
            if (totalAbsorption > 0) {
                int interval = this.plugin.getConfigManager().getPassiveUpdateInterval();
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, interval + 10, Math.max(0, totalAbsorption / 2 - 1), true, false), true);
            }
        }
    }

    private void applyPuffPassives(Player player) {
    }

    private void applySpeedPassives(Player player) {
        int tier = this.plugin.getGemManager().getTierFromOffhand(player);
        int speedLevel = this.plugin.getConfigManager().getSpeedLevel(tier);
        int interval = this.plugin.getConfigManager().getPassiveUpdateInterval();
        int duration = interval + 10;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, speedLevel, true, false), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, duration, 0, true, false), true);
    }

    private void applyStrengthPassives(Player player) {
        int tier = this.plugin.getGemManager().getTierFromOffhand(player);
        int strengthLevel = this.plugin.getConfigManager().getStrengthLevel(tier);
        int interval = this.plugin.getConfigManager().getPassiveUpdateInterval();
        int duration = interval + 10;
        PotionEffect existing = player.getPotionEffect(PotionEffectType.STRENGTH);
        if (existing != null && (existing.getAmplifier() > strengthLevel || existing.getAmplifier() == strengthLevel && existing.getDuration() > duration + 5)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, strengthLevel, true, false), true);
    }

    private void applyWealthPassives(Player player) {
        int tier = this.plugin.getGemManager().getTierFromOffhand(player);
        int luckLevel = this.plugin.getConfigManager().getLuckLevel(tier);
        int interval = this.plugin.getConfigManager().getPassiveUpdateInterval();
        int duration = interval + 10;
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, duration, luckLevel, true, false), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, duration, 0, true, false), true);
    }

    public void registerBuiltInHandlers(GemRegistry registry) {
        registry.registerPassives("astra", (player, tier) -> this.applyAstraPassives(player));
        registry.registerPassives("fire", (player, tier) -> this.applyFirePassives(player));
        registry.registerPassives("flux", (player, tier) -> this.applyFluxPassives(player));
        registry.registerPassives("life", (player, tier) -> this.applyLifePassives(player));
        registry.registerPassives("puff", (player, tier) -> this.applyPuffPassives(player));
        registry.registerPassives("speed", (player, tier) -> this.applySpeedPassives(player));
        registry.registerPassives("strength", (player, tier) -> this.applyStrengthPassives(player));
        registry.registerPassives("wealth", (player, tier) -> this.applyWealthPassives(player));
    }
}

