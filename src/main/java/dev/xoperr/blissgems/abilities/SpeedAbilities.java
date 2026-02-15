/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.ParticleUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpeedAbilities {
    private final BlissGems plugin;

    // Track players with Adrenaline Rush active
    private final Map<UUID, BukkitRunnable> adrenalineRushTasks = new HashMap<>();

    public SpeedAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if player has Adrenaline Rush active
     */
    public boolean isAdrenalineRushActive(Player player) {
        return adrenalineRushTasks.containsKey(player.getUniqueId());
    }

    public void onRightClick(Player player, int tier) {
        if (tier == 2 && player.isSneaking()) {
            // Hold shift for Speed Storm
            this.speedStorm(player);
        } else if (tier == 2 && !player.isSneaking()) {
            // T2 right-click for Adrenaline Rush
            this.adrenalineRush(player);
        } else {
            // T1 right-click for Sloth's Sedative
            this.slothSedative(player);
        }
    }

    public void slothSedative(Player player) {
        String abilityKey = "speed-sedative";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        int hitCount = 0;
        for (Entity entity : player.getNearbyEntities(10.0, 10.0, 10.0)) {
            if (!(entity instanceof LivingEntity)) continue;
            LivingEntity target = (LivingEntity)entity;
            if (entity instanceof Player && player.equals((Object)entity)) continue;

            // Skip trusted players (friendly fire prevention)
            if (entity instanceof Player) {
                Player targetPlayer = (Player) entity;
                if (this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                    continue;
                }
            }

            // Apply MASSIVE slowness debuff
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 3, false, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 200, 2, false, true));

            // MASSIVE particle effects on each target
            target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0.0, 1.0, 0.0), 40, 0.7, 0.7, 0.7);
            target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5);
            target.getWorld().spawnParticle(Particle.ASH, target.getLocation().add(0.0, 0.5, 0.0), 25, 0.5, 0.5, 0.5);
            hitCount++;
        }

        // Visual effects on caster with yellow/lime dust (RGB 244, 255, 28)
        Particle.DustOptions yellowDust = new Particle.DustOptions(ParticleUtils.SPEED_YELLOW, 1.0f);

        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_HURT, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 0.7f);
        player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 100, 3.5, 3.5, 3.5, 0.0, yellowDust, true);
        player.spawnParticle(Particle.CLOUD, player.getLocation().add(0.0, 1.0, 0.0), 60, 3.0, 3.0, 3.0);
        player.spawnParticle(Particle.SMOKE, player.getLocation().add(0.0, 1.0, 0.0), 40, 3.0, 3.0, 3.0);

        // Circle showing AoE range with yellow dust
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * 2 * Math.PI;
            double x = Math.cos(angle) * 10.0;
            double z = Math.sin(angle) * 10.0;
            player.getWorld().spawnParticle(Particle.DUST,
                player.getLocation().add(x, 0.5, z),
                3, 0.1, 0.1, 0.1, 0.0, yellowDust, true);
        }

        this.plugin.getAbilityManager().useAbility(player, abilityKey);

        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Sloth's Sedative");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg + " §7(Slowed " + hitCount + " enemies!)");
        }
        player.sendMessage("§e§oSloth's Sedative slows all nearby enemies with Slowness IV!");
    }

    public void speedStorm(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "speed-storm";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        // Apply MASSIVE speed boost
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 5, false, true));

        // MASSIVE visual effects with yellow/lime dust (RGB 244, 255, 28)
        Particle.DustOptions yellowDust = new Particle.DustOptions(ParticleUtils.SPEED_YELLOW, 1.0f);

        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.8f);
        player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 200, 1.5, 1.5, 1.5, 0.0, yellowDust, true);
        player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 100, 1.5, 1.5, 1.5);
        player.spawnParticle(Particle.GUST, player.getLocation().add(0.0, 1.0, 0.0), 50, 1.0, 1.0, 1.0);
        player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0.0, 1.0, 0.0), 40, 1.0, 1.0, 1.0);

        // Speed trail circles with yellow dust
        for (int i = 0; i < 16; i++) {
            double angle = (i / 16.0) * 2 * Math.PI;
            double x = Math.cos(angle) * 2.0;
            double z = Math.sin(angle) * 2.0;
            player.getWorld().spawnParticle(Particle.DUST,
                player.getLocation().add(x, 0.5, z),
                3, 0.1, 0.1, 0.1, 0.0, yellowDust, true);
        }

        this.plugin.getAbilityManager().useAbility(player, abilityKey);

        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Speed Storm");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        player.sendMessage("§a§oSpeed Storm grants you Speed VI for 10 seconds!");
    }

    public void adrenalineRush(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("§c§oThis ability requires Tier 2!");
            return;
        }

        String abilityKey = "adrenaline-rush";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        // Prevent spam: don't allow re-activation if already active (no early cancellation)
        if (adrenalineRushTasks.containsKey(player.getUniqueId())) {
            player.sendMessage("§c§oAdrenaline Rush is already active!");
            return;
        }

        // Get player's current energy (0-10)
        int energy = this.plugin.getEnergyManager().getEnergy(player);

        // Speed level = energy level (0-10)
        // But cap at level 9 (Speed X would be level 9 in potion effect)
        int speedLevel = Math.min(energy, 9);

        // Duration: 6 seconds (balanced)
        int duration = this.plugin.getConfig().getInt("abilities.adrenaline-rush.duration", 6) * 20; // Convert to ticks

        // Apply Speed effect based on energy
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, speedLevel, false, true));

        // Apply MASSIVE attack speed boost (remove attack cooldown)
        AttributeInstance attackSpeed = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (attackSpeed != null) {
            // Remove existing modifier if present
            AttributeModifier existingModifier = null;
            for (AttributeModifier modifier : attackSpeed.getModifiers()) {
                if (modifier.getName().equals("adrenaline_rush_attack_speed")) {
                    existingModifier = modifier;
                    break;
                }
            }
            if (existingModifier != null) {
                attackSpeed.removeModifier(existingModifier);
            }

            // Add new attack speed modifier (100 = instant attacks, no cooldown)
            AttributeModifier speedModifier = new AttributeModifier(
                UUID.randomUUID(),
                "adrenaline_rush_attack_speed",
                100.0,
                AttributeModifier.Operation.ADD_NUMBER
            );
            attackSpeed.addModifier(speedModifier);
        }

        // MASSIVE visual effects with yellow/lime dust and electricity
        Particle.DustOptions yellowDust = new Particle.DustOptions(ParticleUtils.SPEED_YELLOW, 1.5f);

        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 2.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 2.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

        player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 250, 1.0, 1.5, 1.0, 0.0, yellowDust, true);
        player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 150, 1.0, 1.5, 1.0);
        player.spawnParticle(Particle.GUST, player.getLocation().add(0.0, 1.0, 0.0), 80, 0.8, 1.0, 0.8);
        player.spawnParticle(Particle.EXPLOSION, player.getLocation().add(0.0, 1.0, 0.0), 5, 0.5, 0.5, 0.5);

        // Adrenaline circle effect
        for (int i = 0; i < 24; i++) {
            double angle = (i / 24.0) * 2 * Math.PI;
            double x = Math.cos(angle) * 1.5;
            double z = Math.sin(angle) * 1.5;
            player.getWorld().spawnParticle(Particle.DUST,
                player.getLocation().add(x, 1.0, z),
                5, 0.1, 0.1, 0.1, 0.0, yellowDust, true);
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                player.getLocation().add(x, 1.0, z),
                3, 0.1, 0.1, 0.1);
        }

        // Particle trail task
        BukkitRunnable particleTask = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = duration;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticks >= maxTicks) {
                    removeAdrenalineRushEffects(player);
                    adrenalineRushTasks.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                // Calculate remaining seconds
                int remainingTicks = maxTicks - ticks;
                int remainingSeconds = (remainingTicks + 19) / 20; // Round up

                // Show active timer in action bar
                String speedLevelRoman = toRoman(speedLevel + 1);
                String actionBar = "§e§l⚡ ADRENALINE RUSH §7- Speed " + speedLevelRoman + " + §c§lINSTANT ATTACKS §7(" + remainingSeconds + "s)";
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(actionBar));

                // Continuous particle trail
                player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 0.3, 0.0), 15, 0.3, 0.3, 0.3, 0.0, yellowDust, true);
                player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 0.5, 0.0), 5, 0.3, 0.3, 0.3, 0.05);

                // Every second, show intensity
                if (ticks % 20 == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 2.0f);
                }

                ticks++;
            }
        };

        particleTask.runTaskTimer(this.plugin, 0L, 1L);
        adrenalineRushTasks.put(player.getUniqueId(), particleTask);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);

        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Adrenaline Rush");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }

        String speedLevelRoman = toRoman(speedLevel + 1);
        player.sendMessage("§e§l⚡ ADRENALINE RUSH §7- Speed " + speedLevelRoman + " §7+ §cINSTANT ATTACKS §7for §e6 seconds§7!");
        player.sendMessage("§7§o(Speed scales with your energy: " + energy + "/10)");
    }

    private void removeAdrenalineRushEffects(Player player) {
        // Remove attack speed modifier
        AttributeInstance attackSpeed = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (attackSpeed != null) {
            AttributeModifier existingModifier = null;
            for (AttributeModifier modifier : attackSpeed.getModifiers()) {
                if (modifier.getName().equals("adrenaline_rush_attack_speed")) {
                    existingModifier = modifier;
                    break;
                }
            }
            if (existingModifier != null) {
                attackSpeed.removeModifier(existingModifier);
            }
        }

        // End effects notification
        if (player.isOnline()) {
            player.sendMessage("§c§oAdrenaline Rush has worn off!");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 1.0f);
        }
    }

    private String toRoman(int number) {
        String[] romanNumerals = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (number < 1 || number > 10) return String.valueOf(number);
        return romanNumerals[number - 1];
    }

    // Clean up on player quit
    public void cleanup(UUID playerId) {
        if (adrenalineRushTasks.containsKey(playerId)) {
            adrenalineRushTasks.get(playerId).cancel();
            adrenalineRushTasks.remove(playerId);
        }
    }
}

