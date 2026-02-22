/*
 * Speed Gem Abilities — lightning strikes and velocity boosts
 *
 * Tier 1:
 *   - Blur (Primary): Successive lightning strikes dealing damage and knockback
 *
 * Tier 2 (all Tier 1 abilities plus):
 *   - Blur (Primary, no shift): Same as T1
 *   - Speed Storm (Shift): Creates a field that freezes enemies while granting allies Speed and Haste
 *   - Terminal Velocity (Command): Speed III + Haste II for 9-10 seconds
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.ParticleUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SpeedAbilities {
    private final BlissGems plugin;

    // Speed Storm field state
    private final Set<UUID> speedStormActivePlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> speedStormTasks = new HashMap<>();

    public SpeedAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    // ========================================================================
    // Right-click routing
    // ========================================================================

    public void onRightClick(Player player, int tier) {
        if (tier == 2 && player.isSneaking()) {
            // Shift right-click for Speed Storm
            this.speedStorm(player);
        } else {
            // Right-click for Blur (both T1 and T2)
            this.blur(player);
        }
    }

    /**
     * Activates Terminal Velocity (called from command /bliss ability:secondary)
     */
    public void activateTerminalVelocity(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("§c§oThis ability requires Tier 2!");
            return;
        }
        terminalVelocity(player);
    }

    // ========================================================================
    // State checkers
    // ========================================================================

    public boolean isSpeedStormActive(Player player) {
        return speedStormActivePlayers.contains(player.getUniqueId());
    }

    // ========================================================================
    // 1. BLUR — Tier 1+2 Primary
    //    Successive lightning strikes dealing damage and knockback
    // ========================================================================

    public void blur(Player player) {
        String abilityKey = "speed-blur";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        int strikeCount = this.plugin.getConfig().getInt("abilities.blur.strikes", 3);
        int strikeDelay = this.plugin.getConfig().getInt("abilities.blur.strike-delay", 8); // ticks between strikes
        double damage = this.plugin.getConfigManager().getAbilityDamage("blur");
        double knockbackPower = this.plugin.getConfig().getDouble("abilities.blur.knockback", 1.5);

        Location targetLoc = player.getTargetBlock(null, 20).getLocation().add(0.5, 1, 0.5);

        // MASSIVE activation visual with HIGHLY VISIBLE yellow particles
        Particle.DustOptions yellowDust = new Particle.DustOptions(ParticleUtils.SPEED_YELLOW, 2.0f);

        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 2.0f);
        player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 150, 0.8, 0.8, 0.8, 0.0, yellowDust, true);
        player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 80, 0.8, 0.8, 0.8);
        player.spawnParticle(Particle.GUST, player.getLocation().add(0.0, 1.0, 0.0), 40, 0.5, 0.5, 0.5);

        // Launch successive lightning strikes
        new BukkitRunnable() {
            int currentStrike = 0;

            @Override
            public void run() {
                if (!player.isOnline() || currentStrike >= strikeCount) {
                    this.cancel();
                    return;
                }

                // Strike at target location with slight random offset
                double offsetX = (Math.random() - 0.5) * 3.0;
                double offsetZ = (Math.random() - 0.5) * 3.0;
                Location strikeLoc = targetLoc.clone().add(offsetX, 0, offsetZ);

                // Spawn lightning (visual only, no fire)
                strikeLoc.getWorld().strikeLightningEffect(strikeLoc);

                // EXTREMELY VISIBLE yellow particle burst at strike location
                Particle.DustOptions strikeDust = new Particle.DustOptions(ParticleUtils.SPEED_YELLOW, 2.5f);
                strikeLoc.getWorld().spawnParticle(Particle.DUST, strikeLoc, 250, 1.5, 2.0, 1.5, 0.0, strikeDust, true);
                strikeLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, strikeLoc, 150, 1.2, 1.5, 1.2, 0.1);
                strikeLoc.getWorld().spawnParticle(Particle.EXPLOSION, strikeLoc, 8, 1.0, 1.0, 1.0);
                strikeLoc.getWorld().spawnParticle(Particle.GUST, strikeLoc, 60, 1.0, 1.5, 1.0);

                // Ground circle of HIGHLY VISIBLE yellow particles
                for (int i = 0; i < 32; i++) {
                    double angle = (i / 32.0) * 2 * Math.PI;
                    double x = Math.cos(angle) * 3.0;
                    double z = Math.sin(angle) * 3.0;
                    strikeLoc.getWorld().spawnParticle(Particle.DUST,
                        strikeLoc.clone().add(x, 0.2, z),
                        8, 0.2, 0.1, 0.2, 0.0, strikeDust, true);
                }

                // Damage and knockback enemies in radius
                for (Entity entity : strikeLoc.getWorld().getNearbyEntities(strikeLoc, 3.5, 3.5, 3.5)) {
                    if (!(entity instanceof LivingEntity)) continue;
                    LivingEntity target = (LivingEntity) entity;
                    if (entity.equals(player)) continue;

                    // Skip trusted players
                    if (entity instanceof Player) {
                        Player targetPlayer = (Player) entity;
                        if (plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                            continue;
                        }
                    }

                    // Deal damage
                    target.damage(damage, player);

                    // Apply knockback
                    org.bukkit.util.Vector knockback = target.getLocation().toVector()
                        .subtract(strikeLoc.toVector())
                        .normalize()
                        .multiply(knockbackPower)
                        .setY(0.4);
                    target.setVelocity(knockback);

                    // Hit particles
                    target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0.0, strikeDust, true);
                    target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 25, 0.5, 0.5, 0.5);
                }

                // Sound
                strikeLoc.getWorld().playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.0f);
                strikeLoc.getWorld().playSound(strikeLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);

                currentStrike++;
            }
        }.runTaskTimer(this.plugin, 0L, strikeDelay);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);

        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Blur");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        player.sendMessage("§e§oBlur unleashes " + strikeCount + " successive lightning strikes!");
    }

    // ========================================================================
    // 2. SPEED STORM — Tier 2 Secondary (Shift)
    //    Creates a field that freezes enemies while granting allies Speed and Haste
    // ========================================================================

    public void speedStorm(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("§c§oThis ability requires Tier 2!");
            return;
        }

        String abilityKey = "speed-storm";

        // Toggle off if already active
        if (isSpeedStormActive(player)) {
            endSpeedStorm(player);
            return;
        }

        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        int durationSeconds = this.plugin.getConfig().getInt("abilities.durations.speed-storm", 10);
        int duration = durationSeconds * 20;
        double radius = this.plugin.getConfig().getDouble("abilities.speed-storm.radius", 8.0);

        speedStormActivePlayers.add(uuid);

        // MASSIVE activation visual with HIGHLY VISIBLE yellow particles
        Particle.DustOptions yellowDust = new Particle.DustOptions(ParticleUtils.SPEED_YELLOW, 2.0f);

        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.8f);
        player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 300, 2.0, 2.0, 2.0, 0.0, yellowDust, true);
        player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 200, 2.0, 2.0, 2.0);
        player.spawnParticle(Particle.GUST, player.getLocation().add(0.0, 1.0, 0.0), 100, 1.5, 1.5, 1.5);
        player.spawnParticle(Particle.EXPLOSION, player.getLocation().add(0.0, 1.0, 0.0), 10, 1.0, 1.0, 1.0);

        // Speed Storm field task
        BukkitTask stormTask = new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || ticksElapsed >= duration) {
                    endSpeedStorm(player);
                    this.cancel();
                    return;
                }

                ticksElapsed++;
                Location center = player.getLocation();

                // Apply effects every 20 ticks (1 second)
                if (ticksElapsed % 20 == 0) {
                    for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                        if (!(entity instanceof Player)) continue;
                        Player target = (Player) entity;

                        boolean isTrusted = plugin.getTrustedPlayersManager().isTrusted(player, target);

                        if (isTrusted || target.equals(player)) {
                            // Grant Speed and Haste to allies
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 2, false, true));
                            target.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, 1, false, true));
                        } else {
                            // Freeze enemies
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 255, false, true));
                            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 255, false, true));
                            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 1, false, true));
                        }
                    }
                }

                // HIGHLY VISIBLE field visuals every 5 ticks
                if (ticksElapsed % 5 == 0) {
                    Particle.DustOptions fieldDust = new Particle.DustOptions(ParticleUtils.SPEED_YELLOW, 1.8f);

                    // Ground circle
                    for (int i = 0; i < 40; i++) {
                        double angle = (i / 40.0) * 2 * Math.PI;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        center.getWorld().spawnParticle(Particle.DUST,
                            center.clone().add(x, 0.3, z), 5, 0.2, 0.1, 0.2, 0.0, fieldDust, true);
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            center.clone().add(x, 0.5, z), 2, 0.1, 0.1, 0.1, 0.02);
                    }

                    // Vertical pillars at cardinal points
                    for (int dir = 0; dir < 8; dir++) {
                        double angle = dir * Math.PI / 4;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        for (double y = 0; y < 4; y += 0.5) {
                            center.getWorld().spawnParticle(Particle.DUST,
                                center.clone().add(x, y, z), 3, 0.1, 0.1, 0.1, 0.0, fieldDust, true);
                        }
                    }

                    // Central pillar
                    for (double y = 0; y < 3; y += 0.3) {
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            center.clone().add(0, y, 0), 4, 0.2, 0.1, 0.2, 0.01);
                    }
                }

                // Ambient sound every 2 seconds
                if (ticksElapsed % 40 == 0) {
                    center.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.8f);
                }
            }
        }.runTaskTimer(this.plugin, 0L, 1L);

        speedStormTasks.put(uuid, stormTask);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);

        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Speed Storm");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        player.sendMessage("§e§oSpeed Storm active for " + durationSeconds + "s! Allies gain Speed + Haste, enemies freeze!");
    }

    private void endSpeedStorm(Player player) {
        UUID uuid = player.getUniqueId();
        speedStormActivePlayers.remove(uuid);

        BukkitTask task = speedStormTasks.remove(uuid);
        if (task != null) task.cancel();

        if (player.isOnline()) {
            player.sendMessage("§e§oSpeed Storm faded.");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 1.5f);
        }
    }

    // ========================================================================
    // 3. TERMINAL VELOCITY — Tier 2
    //    Speed III + Haste II for 9-10 seconds
    // ========================================================================

    public void terminalVelocity(Player player) {
        String abilityKey = "speed-terminal";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        int duration = this.plugin.getConfig().getInt("abilities.durations.terminal-velocity", 10) * 20;

        // Apply Speed III + Haste II
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 2, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration, 1, false, true));

        // MASSIVE visual effects with HIGHLY VISIBLE yellow particles
        Particle.DustOptions yellowDust = new Particle.DustOptions(ParticleUtils.SPEED_YELLOW, 2.0f);

        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 2.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.5f, 2.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);

        player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 300, 1.5, 2.0, 1.5, 0.0, yellowDust, true);
        player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 200, 1.5, 2.0, 1.5);
        player.spawnParticle(Particle.GUST, player.getLocation().add(0.0, 1.0, 0.0), 100, 1.0, 1.5, 1.0);
        player.spawnParticle(Particle.EXPLOSION, player.getLocation().add(0.0, 1.0, 0.0), 8, 1.0, 1.0, 1.0);
        player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0.0, 1.0, 0.0), 60, 1.0, 1.0, 1.0);

        // Velocity circle effect with HIGHLY VISIBLE particles
        for (int i = 0; i < 32; i++) {
            double angle = (i / 32.0) * 2 * Math.PI;
            double x = Math.cos(angle) * 2.5;
            double z = Math.sin(angle) * 2.5;
            player.getWorld().spawnParticle(Particle.DUST,
                player.getLocation().add(x, 0.5, z),
                8, 0.2, 0.1, 0.2, 0.0, yellowDust, true);
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                player.getLocation().add(x, 1.0, z),
                4, 0.1, 0.1, 0.1);
        }

        this.plugin.getAbilityManager().useAbility(player, abilityKey);

        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Terminal Velocity");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        player.sendMessage("§e§l⚡ TERMINAL VELOCITY §7- Speed III + Haste II for " + (duration / 20) + " seconds!");
    }

    // ========================================================================
    // Cleanup
    // ========================================================================

    public void cleanup(UUID playerId) {
        if (speedStormTasks.containsKey(playerId)) {
            speedStormTasks.get(playerId).cancel();
            speedStormTasks.remove(playerId);
        }
        speedStormActivePlayers.remove(playerId);
    }
}

