/*
 * Flux Gem Abilities
 * - Flux Beam (Tier 1+2): Chargeable electric beam that damages health and armor
 * - Ground (Tier 2 + Shift): Stuns target player
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.ParticleUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class FluxAbilities {
    private final BlissGems plugin;
    private static final Set<UUID> stunnedPlayers = new HashSet<>();

    // Charging system (similar to Fire gem)
    private final Map<UUID, Integer> chargingPlayers = new HashMap<>();
    private final Map<UUID, BukkitTask> chargingTasks = new HashMap<>();
    private static final int MAX_CHARGE = 100;
    private static final int CHARGE_DURATION_TICKS = 300; // 15 seconds

    public FluxAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    public static boolean isPlayerStunned(UUID playerId) {
        return stunnedPlayers.contains(playerId);
    }

    public boolean isCharging(Player player) {
        return chargingPlayers.containsKey(player.getUniqueId());
    }

    public int getCharge(Player player) {
        return chargingPlayers.getOrDefault(player.getUniqueId(), 0);
    }

    public void onRightClick(Player player, int tier) {
        // Ground is Tier 2 + Shift, Flux Beam is primary
        if (tier == 2 && player.isSneaking()) {
            this.ground(player);
        } else {
            this.fluxBeam(player);
        }
    }

    public void fluxBeam(Player player) {
        String abilityKey = "flux-beam";

        // If already charging, fire the shot
        if (isCharging(player)) {
            fireChargedBeam(player);
            return;
        }

        // Check cooldown before starting charge
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        // Start charging
        UUID uuid = player.getUniqueId();
        chargingPlayers.put(uuid, 0);

        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.5f);
        player.sendMessage("§b⚡ §oCharging Flux Beam... Right-click again to fire!");

        // Charging task - increases charge over 15 seconds
        BukkitTask task = new BukkitRunnable() {
            int ticksElapsed = 0;
            boolean maxChargeNotified = false;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !chargingPlayers.containsKey(uuid)) {
                    cancelCharging(player);
                    this.cancel();
                    return;
                }

                ticksElapsed++;

                // Calculate charge based on elapsed time
                int newCharge = Math.min((ticksElapsed * MAX_CHARGE) / CHARGE_DURATION_TICKS, MAX_CHARGE);
                chargingPlayers.put(uuid, newCharge);

                // Show charge bar in action bar
                showChargeBar(player, newCharge);

                // Flux cyan particles AROUND player (not blocking view) - RGB(94, 215, 255)
                Location playerLoc = player.getLocation();
                Particle.DustOptions cyanDust = new Particle.DustOptions(ParticleUtils.FLUX_CYAN, 1.0f);

                // Ring of cyan dust particles around player at WAIST/FEET height
                double ringRadius = 1.4;
                for (int i = 0; i < 8; i++) {
                    double angle = (i / 8.0) * 2 * Math.PI + (ticksElapsed * 0.1);
                    double x = Math.cos(angle) * ringRadius;
                    double z = Math.sin(angle) * ringRadius;

                    // Ground level ring - cyan dust
                    player.getWorld().spawnParticle(Particle.DUST,
                        playerLoc.clone().add(x, 0.2, z),
                        3, 0.1, 0.1, 0.1, 0.0, cyanDust, true);

                    // Waist height ring - electric spark for accent
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        playerLoc.clone().add(x * 0.8, 0.8, z * 0.8),
                        2, 0.05, 0.05, 0.05, 0.02);
                }

                // Spiral of particles going upward
                double spiralAngle = (ticksElapsed * 0.3) % (2 * Math.PI);
                double spiralRadius = 1.0;
                for (int h = 0; h < 3; h++) {
                    double heightOffset = h * 0.4;
                    double x = Math.cos(spiralAngle + h) * spiralRadius;
                    double z = Math.sin(spiralAngle + h) * spiralRadius;
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        playerLoc.clone().add(x, heightOffset, z),
                        1, 0.05, 0.05, 0.05, 0);
                }

                // Additional cyan particles that increase with charge
                if (ticksElapsed % 5 == 0) {
                    int particleCount = 5 + (newCharge / 15);
                    Particle.DustOptions cyanDustCharge = new Particle.DustOptions(ParticleUtils.FLUX_CYAN, 0.9f);

                    // Cyan dust particles around player at waist level
                    player.getWorld().spawnParticle(Particle.DUST,
                        playerLoc.clone().add(0, 0.8, 0),
                        particleCount * 2, 1.2, 0.5, 1.2, 0.0, cyanDustCharge, true);

                    // Smoke particles for effect (as per Skript)
                    player.getWorld().spawnParticle(Particle.SMOKE,
                        playerLoc.clone().add(0, 0.5, 0),
                        particleCount, 1.5, 0.4, 1.5, 0.02);

                    // End rod particles on the ground around player
                    for (int i = 0; i < 8; i++) {
                        double angle = (i / 8.0) * 2 * Math.PI;
                        double x = Math.cos(angle) * 1.8;
                        double z = Math.sin(angle) * 1.8;
                        player.getWorld().spawnParticle(Particle.END_ROD,
                            player.getLocation().add(x, 0.1, z),
                            2, 0.1, 0.1, 0.1, 0);
                    }
                }

                // Sound feedback at milestones
                if (newCharge == 25 || newCharge == 50 || newCharge == 75) {
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.3f, 1.0f + (newCharge / 100.0f));
                }

                // Max charge reached
                if (newCharge >= MAX_CHARGE && !maxChargeNotified) {
                    player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 2.0f);
                    player.sendMessage("§b⚡§l Fully charged! §eRight-click to fire!");
                    maxChargeNotified = true;
                }

                // Keep showing max charge cyan particles
                if (newCharge >= MAX_CHARGE && ticksElapsed % 3 == 0) {
                    Particle.DustOptions cyanDustMax = new Particle.DustOptions(ParticleUtils.FLUX_CYAN, 1.0f);

                    // Large cyan burst around player at LOW height
                    player.getWorld().spawnParticle(Particle.DUST,
                        playerLoc.clone().add(0, 0.6, 0),
                        30, 1.8, 0.4, 1.8, 0.0, cyanDustMax, true);

                    // Smoke for extra effect (as per Skript)
                    player.getWorld().spawnParticle(Particle.SMOKE,
                        playerLoc.clone().add(0, 0.3, 0),
                        20, 1.5, 0.3, 1.5, 0.08);

                    // End rod particles on ground
                    player.getWorld().spawnParticle(Particle.END_ROD,
                        player.getLocation().add(0, 0.1, 0),
                        15, 2.0, 0.1, 2.0, 0);
                }
            }
        }.runTaskTimer(this.plugin, 0L, 1L);

        chargingTasks.put(uuid, task);

        // Auto-fire after 15 seconds (when fully charged)
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            if (isCharging(player)) {
                fireChargedBeam(player);
            }
        }, CHARGE_DURATION_TICKS + 20L); // Extra 1 second grace period
    }

    private void showChargeBar(Player player, int charge) {
        int bars = charge / 5;
        StringBuilder bar = new StringBuilder("§b⚡ Flux Beam: §e");

        for (int i = 0; i < 20; i++) {
            if (i < bars) {
                bar.append("█");
            } else {
                bar.append("§8█");
            }
        }

        bar.append(" §b").append(charge).append("%");
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar.toString()));
    }

    private void fireChargedBeam(Player player) {
        UUID uuid = player.getUniqueId();
        int charge = chargingPlayers.getOrDefault(uuid, 0);

        // Cancel charging task
        BukkitTask task = chargingTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        chargingPlayers.remove(uuid);

        if (charge < 10) {
            player.sendMessage("§c§oNot enough charge!");
            return;
        }

        // Find target using raycast
        LivingEntity target = getTargetEntity(player, 30);
        if (target == null) {
            player.sendMessage("§cNo target found!");
            return;
        }

        // Check if target is trusted (friendly fire prevention)
        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            if (plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                // Restore armor instead of damaging (uses same config value)
                int maxArmorDamage = plugin.getConfig().getInt("abilities.damage.flux-beam-max-armor-damage", 150);
                int armorRestore = (int)((charge / 100.0) * maxArmorDamage);
                restoreArmorDurability(targetPlayer, armorRestore);
                player.sendMessage("§a§lRestored " + targetPlayer.getName() + "'s armor! (+" + armorRestore + " durability)");

                // Show beam particles to target
                drawBeamParticles(player.getEyeLocation(), targetPlayer.getEyeLocation().add(0, 1, 0));
                return;
            }
        }

        // Calculate damage based on charge (read from config)
        double baseDamage = plugin.getConfig().getDouble("abilities.damage.flux-beam-base", 5.0); // Default 5.0 HP (2.5 hearts)
        double damageMultiplier = 1.0 + (charge / 50.0); // 1.0x to 3.0x at max charge
        double finalDamage = baseDamage * damageMultiplier;

        // Armor durability damage (read from config)
        int maxArmorDamage = plugin.getConfig().getInt("abilities.damage.flux-beam-max-armor-damage", 150);
        int armorDamage = (int)((charge / 100.0) * maxArmorDamage); // 0 to max at 100%

        // Apply health damage (bypasses armor using setHealth)
        double currentHealth = target.getHealth();
        double newHealth = Math.max(0, currentHealth - finalDamage);
        target.setHealth(newHealth);

        // Apply armor damage if target is a player
        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            damageArmorPieces(targetPlayer, armorDamage);

            // Show damage indicator to target
            targetPlayer.sendTitle("",
                "§c§l-" + String.format("%.1f", finalDamage) + " HP",
                5, 15, 10);

            // Send action bar damage notification
            targetPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent("§c§l⚡ FLUX BEAM HIT! -" + String.format("%.1f", finalDamage) + " HP §7(Armor: -" + armorDamage + ")"));
        }

        // Visual and sound effects
        drawBeamParticles(player.getEyeLocation(), target.getEyeLocation().add(0, 1, 0));

        // MASSIVE hit particle explosion - much more visible
        Location hitLoc = target.getLocation().add(0, 1, 0);
        Particle.DustOptions redDamage = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 50, 50), 1.5f);
        Particle.DustOptions cyanHit = new Particle.DustOptions(ParticleUtils.FLUX_CYAN, 1.3f);

        // Large cyan explosion burst
        target.getWorld().spawnParticle(Particle.DUST, hitLoc, 150, 1.2, 1.5, 1.2, 0.0, cyanHit, true);

        // Red damage indicator particles
        target.getWorld().spawnParticle(Particle.DUST, hitLoc, 100, 0.9, 1.2, 0.9, 0.0, redDamage, true);

        // Electric sparks everywhere
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 200, 1.0, 1.3, 1.0, 0.15);

        // Critical hit markers
        target.getWorld().spawnParticle(Particle.CRIT, hitLoc, 80, 0.8, 1.0, 0.8, 0.3);

        // Enchanted hit sparkles
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, hitLoc, 100, 0.9, 1.2, 0.9, 0.2);

        // Soul fire flames for dramatic effect
        target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, hitLoc, 60, 0.7, 1.0, 0.7, 0.08);

        // Explosion particles (no damage)
        target.getWorld().spawnParticle(Particle.EXPLOSION, hitLoc, 5, 0.5, 0.5, 0.5, 0);

        // Damage indicator ring around target
        for (int i = 0; i < 32; i++) {
            double angle = (i / 32.0) * 2 * Math.PI;
            double radius = 1.5;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            target.getWorld().spawnParticle(Particle.DUST,
                target.getLocation().add(x, 0.5, z),
                5, 0.1, 0.3, 0.1, 0.0, redDamage, true);
            target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                target.getLocation().add(x, 0.5, z),
                3, 0.1, 0.2, 0.1, 0.05);
        }

        // Sounds - louder and more impactful
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.8f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, 1.5f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.8f);

        this.plugin.getAbilityManager().useAbility(player, "flux-beam");

        // Enhanced feedback message
        String targetName = target instanceof Player ? ((Player) target).getName() : target.getType().name();
        player.sendMessage("§b⚡§l FLUX BEAM HIT! §r§o" + targetName + " §7(" + charge + "% charge)");
        player.sendMessage("§c  ❤ Damage: " + String.format("%.1f", finalDamage) + " HP §8| §7Armor: -" + armorDamage);
    }

    private void drawBeamParticles(Location from, Location to) {
        double distance = from.distance(to);
        int points = (int)(distance * 8); // Double density for visibility

        // Flux cyan color for beam core
        Particle.DustOptions cyanCore = new Particle.DustOptions(ParticleUtils.FLUX_CYAN, 1.2f);
        Particle.DustOptions cyanOuter = new Particle.DustOptions(ParticleUtils.FLUX_CYAN, 0.8f);

        for (int i = 0; i <= points; i++) {
            double ratio = (double) i / points;
            double x = from.getX() + (to.getX() - from.getX()) * ratio;
            double y = from.getY() + (to.getY() - from.getY()) * ratio;
            double z = from.getZ() + (to.getZ() - from.getZ()) * ratio;
            Location point = new Location(from.getWorld(), x, y, z);

            // Core beam - bright electric sparks (heavy density)
            from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, x, y, z, 8, 0.08, 0.08, 0.08, 0.03);

            // Inner cyan beam layer
            from.getWorld().spawnParticle(Particle.DUST, x, y, z, 6, 0.1, 0.1, 0.1, 0.0, cyanCore, true);

            // Outer cyan glow
            from.getWorld().spawnParticle(Particle.DUST, x, y, z, 4, 0.15, 0.15, 0.15, 0.0, cyanOuter, true);

            // End rod for bright core trail
            from.getWorld().spawnParticle(Particle.END_ROD, x, y, z, 3, 0.05, 0.05, 0.05, 0.01);

            // Soul fire flame for electric effect
            from.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, x, y, z, 2, 0.08, 0.08, 0.08, 0.01);

            // Add spiral electric arcs around beam for dramatic effect
            if (i % 3 == 0) {
                double angle = i * 0.5;
                double arcRadius = 0.25;
                for (int arc = 0; arc < 3; arc++) {
                    double arcAngle = angle + (arc * Math.PI * 2 / 3);
                    double offsetX = Math.cos(arcAngle) * arcRadius;
                    double offsetZ = Math.sin(arcAngle) * arcRadius;

                    from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        x + offsetX, y, z + offsetZ, 1, 0.02, 0.02, 0.02, 0);
                }
            }
        }
    }

    private void damageArmorPieces(Player player, int durabilityDamage) {
        if (durabilityDamage <= 0) return;

        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.getType() != Material.AIR) {
                ItemMeta meta = armor.getItemMeta();
                if (meta instanceof Damageable) {
                    Damageable damageable = (Damageable) meta;
                    int newDamage = damageable.getDamage() + durabilityDamage;
                    // Cap at max durability
                    int maxDurability = armor.getType().getMaxDurability();
                    if (newDamage >= maxDurability) {
                        // Armor breaks
                        armor.setAmount(0);
                    } else {
                        damageable.setDamage(newDamage);
                        armor.setItemMeta(meta);
                    }
                }
            }
        }
    }

    private void restoreArmorDurability(Player player, int durabilityRestore) {
        if (durabilityRestore <= 0) return;

        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.getType() != Material.AIR) {
                ItemMeta meta = armor.getItemMeta();
                if (meta instanceof Damageable) {
                    Damageable damageable = (Damageable) meta;
                    int newDamage = Math.max(0, damageable.getDamage() - durabilityRestore);
                    damageable.setDamage(newDamage);
                    armor.setItemMeta(meta);
                }
            }
        }
        // Positive particles
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
            player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 2.0f);
    }

    public void cancelCharging(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = chargingTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        chargingPlayers.remove(uuid);
    }

    public void cleanup(Player player) {
        cancelCharging(player);
    }

    public void ground(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("§c§oThis ability requires Tier 2!");
            return;
        }

        String abilityKey = "flux-ground";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        // Find target entity FIRST before doing anything (works on all living entities now)
        LivingEntity target = this.getTargetEntity(player, 20);
        if (target == null) {
            player.sendMessage("§cNo target found!");
            return;
        }

        // Check if target is a trusted player (friendly fire prevention)
        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            if (this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                player.sendMessage("§c§lYou cannot stun " + targetPlayer.getName() + " because they are trusted!");
                return;
            }
        }

        // Stun duration from config (default 5 seconds)
        int stunDurationSeconds = this.plugin.getConfig().getInt("abilities.durations.flux-ground-freeze", 5);
        int stunDuration = stunDurationSeconds * 20; // Convert to ticks

        // Apply stun effects to ALL living entities (players and mobs)
        // Slowness 255 freezes horizontal movement
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, stunDuration, 255, false, true));
        // Slow Falling prevents them from jumping or falling - keeps them grounded
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, stunDuration, 0, false, true));
        // Mining Fatigue and Weakness to prevent actions
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, stunDuration, 255, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, stunDuration, 255, false, true));

        // Add to stunned players list (only if it's a player)
        if (target instanceof Player) {
            stunnedPlayers.add(target.getUniqueId());

            // Remove from stunned list after duration
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                stunnedPlayers.remove(target.getUniqueId());
            }, stunDuration);
        }

        // Visual effects - Dark cyan for Ground Stun (RGB 16, 131, 173)
        Particle.DustOptions darkCyanDust = new Particle.DustOptions(ParticleUtils.FLUX_DARK_CYAN, 1.5f);

        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 200, 1.0, 1.8, 1.0, 0.0, darkCyanDust, true);
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0.0, 1.0, 0.0), 100, 0.7, 1.2, 0.7);
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation(), 60, 0.7, 0.2, 0.7);
        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0.0, 0.5, 0.0), 40, 0.5, 0.5, 0.5);

        // Ground effect circle with dark cyan
        for (int i = 0; i < 24; i++) {
            double angle = (i / 24.0) * 2 * Math.PI;
            double x = Math.cos(angle) * 1.8;
            double z = Math.sin(angle) * 1.8;
            target.getWorld().spawnParticle(Particle.DUST,
                target.getLocation().add(x, 0.1, z),
                8, 0.15, 0.15, 0.15, 0.0, darkCyanDust, true);
            target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                target.getLocation().add(x, 0.5, z),
                3, 0.1, 0.1, 0.1, 0);
        }

        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.5f);

        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.2f);
        player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 50, 1.2, 1.2, 1.2);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);

        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Ground");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }

        // Send message to target only if it's a player
        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            targetPlayer.sendMessage("§c§lYou have been grounded for " + stunDurationSeconds + " seconds! You cannot move or jump!");
        }

        // Feedback for caster
        String targetName = target instanceof Player ? ((Player) target).getName() : target.getType().name();
        player.sendMessage("§b⚡ §oGrounded " + targetName + " for " + stunDurationSeconds + " seconds!");
    }

    private LivingEntity getTargetEntity(Player player, int range) {
        return player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), (double)range, entity -> entity instanceof LivingEntity && entity != player) != null ? (LivingEntity)player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), (double)range, entity -> entity instanceof LivingEntity && entity != player).getHitEntity() : null;
    }
}
