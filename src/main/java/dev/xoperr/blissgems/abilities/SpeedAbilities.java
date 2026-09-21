/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Snowball
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scheduler.BukkitTask
 *  org.bukkit.util.Vector
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.utils.ParticleUtils;
import dev.xoperr.blissgems.utils.PlayerCloneNPC;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class SpeedAbilities
implements GemAbilityHandler {
    private final BlissGems plugin;
    private final Set<UUID> speedStormActivePlayers = new HashSet<UUID>();
    private final Map<UUID, BukkitTask> speedStormTasks = new HashMap<UUID, BukkitTask>();
    private static final Set<UUID> frozenPlayers = new HashSet<UUID>();
    private static final long CLONE_WINDUP_TICKS = 5L;
    private static final long CLONE_LIFETIME_TICKS = 20L;
    private final Map<UUID, Integer> blurCharges = new HashMap<UUID, Integer>();
    private final Map<UUID, BukkitTask> blurExpiryTasks = new HashMap<UUID, BukkitTask>();
    private final Map<UUID, Integer> galeCharges = new HashMap<UUID, Integer>();
    private final Map<UUID, BukkitTask> galeExpiryTasks = new HashMap<UUID, BukkitTask>();
    public static final String GALE_CLOUD_TAG = "blissgems_gale_cloud";

    public SpeedAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    public static boolean isPlayerFrozen(UUID playerId) {
        return frozenPlayers.contains(playerId);
    }

    public static void freezePlayer(UUID playerId) {
        frozenPlayers.add(playerId);
    }

    public static void unfreezePlayer(UUID playerId) {
        frozenPlayers.remove(playerId);
    }

    public void onRightClick(Player player, int tier) {
        if (tier == 2 && player.isSneaking()) {
            this.speedStorm(player);
        } else {
            this.blur(player);
        }
    }

    @Override
    public void onPrimary(Player player, int tier) {
        this.onRightClick(player, tier);
    }

    @Override
    public void onSecondary(Player player, int tier) {
        this.speedStorm(player);
    }

    @Override
    public void onTertiary(Player player, int tier) {
        this.activateTerminalVelocity(player);
    }

    @Override
    public void onQuaternary(Player player, int tier) {
        this.galeClouds(player);
    }

    @Override
    public void cleanup(Player player) {
        this.cleanup(player.getUniqueId());
    }

    public void activateTerminalVelocity(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        this.terminalVelocity(player);
    }

    public boolean isSpeedStormActive(Player player) {
        return this.speedStormActivePlayers.contains(player.getUniqueId());
    }

    public int getBlurCharges(UUID playerId) {
        return this.blurCharges.getOrDefault(playerId, 0);
    }

    public void blur(Player player) {
        final UUID uuid = player.getUniqueId();
        if (this.blurCharges.getOrDefault(uuid, 0) > 0) {
            this.fireBlurStrike(player);
            return;
        }
        String abilityKey = "speed-blur";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        int strikeCount = this.plugin.getConfig().getInt("abilities.blur.strikes", 5);
        int timeoutSeconds = this.plugin.getConfig().getInt("abilities.blur.charge-timeout-seconds", 30);
        this.blurCharges.put(uuid, strikeCount);
        Particle.DustOptions yellowDust = new Particle.DustOptions(ParticleUtils.SPEED_YELLOW, 2.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 2.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.8f);
        player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 150, 0.8, 0.8, 0.8, 0.0, (Object)yellowDust, true);
        player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 80, 0.8, 0.8, 0.8);
        player.spawnParticle(Particle.GUST, player.getLocation().add(0.0, 4.0, 0.0), 40, 0.5, 0.5, 0.5);
        BukkitTask expiry = new BukkitRunnable(){

            public void run() {
                SpeedAbilities.this.blurExpiryTasks.remove(uuid);
                SpeedAbilities.this.blurCharges.remove(uuid);
            }
        }.runTaskLater((Plugin)this.plugin, (long)timeoutSeconds * 20L);
        BukkitTask oldExpiry = this.blurExpiryTasks.put(uuid, expiry);
        if (oldExpiry != null) {
            oldExpiry.cancel();
        }
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Blur");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    public void galeClouds(Player player) {
        final UUID uuid = player.getUniqueId();
        if (this.galeCharges.getOrDefault(uuid, 0) > 0) {
            this.throwGaleCloud(player);
            return;
        }
        String abilityKey = "speed-gale-clouds";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        int cloudCount = this.plugin.getConfig().getInt("abilities.gale-clouds.clouds", 3);
        int timeoutSeconds = this.plugin.getConfig().getInt("abilities.gale-clouds.charge-timeout-seconds", 20);
        this.galeCharges.put(uuid, cloudCount);
        BukkitTask expiry = new BukkitRunnable(){

            public void run() {
                SpeedAbilities.this.galeExpiryTasks.remove(uuid);
                SpeedAbilities.this.galeCharges.remove(uuid);
            }
        }.runTaskLater((Plugin)this.plugin, (long)timeoutSeconds * 20L);
        BukkitTask oldExpiry = this.galeExpiryTasks.put(uuid, expiry);
        if (oldExpiry != null) {
            oldExpiry.cancel();
        }
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_INHALE, 1.0f, 1.4f);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Gale Clouds");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        this.throwGaleCloud(player);
    }

    private void throwGaleCloud(Player player) {
        UUID uuid = player.getUniqueId();
        int remaining = this.galeCharges.getOrDefault(uuid, 0);
        if (remaining <= 0) {
            return;
        }
        Snowball cloud = (Snowball)player.launchProjectile(Snowball.class);
        cloud.setVelocity(player.getLocation().getDirection().multiply(1.4));
        cloud.addScoreboardTag(GALE_CLOUD_TAG);
        cloud.setItem(new ItemStack(Material.WHITE_DYE));
        player.getWorld().spawnParticle(Particle.CLOUD, player.getEyeLocation(), 15, 0.2, 0.2, 0.2, 0.02);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.9f, 1.6f);
        if (--remaining <= 0) {
            this.galeCharges.remove(uuid);
            BukkitTask expiry = this.galeExpiryTasks.remove(uuid);
            if (expiry != null) {
                expiry.cancel();
            }
        } else {
            this.galeCharges.put(uuid, remaining);
        }
        player.sendMessage("\u00a7e\u00a7oGale Cloud thrown! \u00a77(" + remaining + " left)");
    }

    public void spawnGaleCloud(final Location center, final Player thrower) {
        final double radius = this.plugin.getConfig().getDouble("abilities.gale-clouds.radius", 3.5);
        final int durationSeconds = this.plugin.getConfig().getInt("abilities.gale-clouds.duration-seconds", 5);
        final int slownessLevel = this.plugin.getConfig().getInt("abilities.gale-clouds.slowness-level", 2);
        final int slownessSeconds = this.plugin.getConfig().getInt("abilities.gale-clouds.slowness-duration-seconds", 4);
        final int windChargeSeconds = this.plugin.getConfig().getInt("abilities.gale-clouds.wind-charge-cooldown-seconds", 8);
        center.getWorld().playSound(center, Sound.ENTITY_BREEZE_LAND, 1.0f, 1.2f);
        new BukkitRunnable(){
            int ticks = 0;
            final int maxTicks = durationSeconds * 20;

            public void run() {
                if (this.ticks >= this.maxTicks) {
                    this.cancel();
                    return;
                }
                for (int i = 0; i < 6; ++i) {
                    double angle = (double)this.ticks / 8.0 + (double)i / 6.0 * 2.0 * Math.PI;
                    double x = Math.cos(angle) * radius * 0.8;
                    double z = Math.sin(angle) * radius * 0.8;
                    center.getWorld().spawnParticle(Particle.CLOUD, center.clone().add(x, 0.4, z), 2, 0.15, 0.15, 0.15, 0.01);
                }
                center.getWorld().spawnParticle(Particle.GUST, center.clone().add(0.0, 0.5, 0.0), 1, 0.4, 0.2, 0.4, 0.0);
                if (this.ticks % 10 == 0) {
                    for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                        LivingEntity living;
                        if (!(entity instanceof LivingEntity) || (living = (LivingEntity)entity).equals((Object)thrower)) continue;
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessSeconds * 20, Math.max(0, slownessLevel - 1), true, true));
                        if (!(living instanceof Player)) continue;
                        Player hit = (Player)living;
                        hit.setCooldown(Material.WIND_CHARGE, windChargeSeconds * 20);
                    }
                }
                ++this.ticks;
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    private void fireBlurStrike(final Player player) {
        UUID uuid = player.getUniqueId();
        int remaining = this.blurCharges.getOrDefault(uuid, 0);
        if (remaining <= 0) {
            return;
        }
        final double damage = this.plugin.getConfigManager().getAbilityDamage("blur");
        final double knockbackPower = this.plugin.getConfig().getDouble("abilities.blur.knockback", 1.5);
        final Location strikeLoc = player.getTargetBlock(null, 20).getLocation().add(0.5, 1.0, 0.5);
        strikeLoc.getWorld().strikeLightningEffect(strikeLoc);
        final Particle.DustOptions strikeDust = new Particle.DustOptions(ParticleUtils.SPEED_YELLOW, 2.5f);
        strikeLoc.getWorld().spawnParticle(Particle.DUST, strikeLoc, 250, 1.5, 2.0, 1.5, 0.0, (Object)strikeDust, true);
        strikeLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, strikeLoc, 150, 1.2, 1.5, 1.2, 0.1);
        Location cloudLoc = strikeLoc.clone().add(0.0, 3.0, 0.0);
        strikeLoc.getWorld().spawnParticle(Particle.EXPLOSION, cloudLoc, 8, 1.0, 0.5, 1.0);
        strikeLoc.getWorld().spawnParticle(Particle.GUST, cloudLoc, 60, 1.0, 0.5, 1.0);
        for (int i = 0; i < 32; ++i) {
            double angle = (double)i / 32.0 * 2.0 * Math.PI;
            double x = Math.cos(angle) * 3.0;
            double z = Math.sin(angle) * 3.0;
            strikeLoc.getWorld().spawnParticle(Particle.DUST, strikeLoc.clone().add(x, 0.2, z), 8, 0.2, 0.1, 0.2, 0.0, (Object)strikeDust, true);
        }
        strikeLoc.getWorld().playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.0f);
        strikeLoc.getWorld().playSound(strikeLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
        PlayerCloneNPC.play((Plugin)this.plugin, player, strikeLoc, 5L, 20L);
        new BukkitRunnable(){

            public void run() {
                strikeLoc.getWorld().playSound(strikeLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.4f);
                strikeLoc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, strikeLoc.clone().add(0.0, 1.0, 0.0), 3, 0.6, 0.3, 0.6, 0.0);
                for (Entity entity : strikeLoc.getWorld().getNearbyEntities(strikeLoc, 3.5, 3.5, 3.5)) {
                    if (!(entity instanceof LivingEntity)) continue;
                    LivingEntity target = (LivingEntity)entity;
                    if (entity.equals((Object)player)) continue;
                    if (entity instanceof Player) {
                        Player targetPlayer = (Player)entity;
                        if (SpeedAbilities.this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) continue;
                    }
                    target.damage(damage, (Entity)player);
                    final LivingEntity knockTarget = target;
                    final Location knockOrigin = strikeLoc.clone();
                    new BukkitRunnable(){

                        public void run() {
                            if (knockTarget.isValid() && !knockTarget.isDead()) {
                                Vector away = knockTarget.getLocation().toVector().subtract(knockOrigin.toVector());
                                if (away.lengthSquared() < 1.0E-4 && (away = knockOrigin.getDirection().setY(0)).lengthSquared() < 1.0E-4) {
                                    away = new Vector(1, 0, 0);
                                }
                                Vector knockback = away.normalize().multiply(knockbackPower).setY(0.5);
                                knockTarget.setVelocity(knockback);
                            }
                        }
                    }.runTaskLater((Plugin)SpeedAbilities.this.plugin, 1L);
                    target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 40, 0.5, 0.5, 0.5, 0.0, (Object)strikeDust, true);
                    target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0.0, 1.0, 0.0), 25, 0.5, 0.5, 0.5);
                }
            }
        }.runTaskLater((Plugin)this.plugin, 5L);
        new BukkitRunnable(){

            public void run() {
                strikeLoc.getWorld().spawnParticle(Particle.DUST, strikeLoc.clone().add(0.0, 1.0, 0.0), 60, 0.4, 0.9, 0.4, 0.0, (Object)strikeDust, true);
                strikeLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, strikeLoc.clone().add(0.0, 1.0, 0.0), 40, 0.4, 0.9, 0.4, 0.05);
            }
        }.runTaskLater((Plugin)this.plugin, 20L);
        if (--remaining <= 0) {
            this.blurCharges.remove(uuid);
            BukkitTask expiry = this.blurExpiryTasks.remove(uuid);
            if (expiry != null) {
                expiry.cancel();
            }
        } else {
            this.blurCharges.put(uuid, remaining);
        }
    }

    public void speedStorm(final Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "speed-storm";
        if (this.isSpeedStormActive(player)) {
            this.endSpeedStorm(player);
            return;
        }
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        int durationSeconds = this.plugin.getConfig().getInt("abilities.durations.speed-storm", 10);
        final int duration = durationSeconds * 20;
        final double radius = this.plugin.getConfig().getDouble("abilities.speed-storm.radius", 8.0);
        this.speedStormActivePlayers.add(uuid);
        Particle.DustOptions yellowDust = new Particle.DustOptions(ParticleUtils.SPEED_YELLOW, 2.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.8f);
        player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 300, 2.0, 2.0, 2.0, 0.0, (Object)yellowDust, true);
        player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 200, 2.0, 2.0, 2.0);
        player.spawnParticle(Particle.GUST, player.getLocation().add(0.0, 1.0, 0.0), 100, 1.5, 1.5, 1.5);
        player.spawnParticle(Particle.EXPLOSION, player.getLocation().add(0.0, 1.0, 0.0), 10, 1.0, 1.0, 1.0);
        BukkitTask stormTask = new BukkitRunnable(){
            int ticksElapsed = 0;

            public void run() {
                if (!player.isOnline() || player.isDead() || this.ticksElapsed >= duration) {
                    SpeedAbilities.this.endSpeedStorm(player);
                    this.cancel();
                    return;
                }
                ++this.ticksElapsed;
                Location center = player.getLocation();
                if (this.ticksElapsed % 20 == 0) {
                    for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                        if (!(entity instanceof Player)) continue;
                        Player target = (Player)entity;
                        boolean isTrusted = SpeedAbilities.this.plugin.getTrustedPlayersManager().isTrusted(player, target);
                        if (isTrusted || target.equals((Object)player)) {
                            int allyBuffTicks = SpeedAbilities.this.plugin.getConfig().getInt("abilities.speed-storm.ally-buff-duration", 45) * 20;
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, allyBuffTicks, 2, false, true));
                            target.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, allyBuffTicks, 1, false, true));
                            SpeedAbilities.unfreezePlayer(target.getUniqueId());
                            continue;
                        }
                        int slowLevel = SpeedAbilities.this.plugin.getConfig().getInt("abilities.speed-storm.enemy-slowness-level", 3);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, slowLevel, false, true));
                        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 1, false, true));
                    }
                }
                if (this.ticksElapsed % 5 == 0) {
                    double z;
                    Particle.DustOptions fieldDust = new Particle.DustOptions(ParticleUtils.SPEED_YELLOW, 1.8f);
                    for (int i = 0; i < 40; ++i) {
                        double angle = (double)i / 40.0 * 2.0 * Math.PI;
                        double x = Math.cos(angle) * radius;
                        z = Math.sin(angle) * radius;
                        center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, 0.3, z), 5, 0.2, 0.1, 0.2, 0.0, (Object)fieldDust, true);
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(x, 0.5, z), 2, 0.1, 0.1, 0.1, 0.02);
                    }
                    for (int dir = 0; dir < 8; ++dir) {
                        double angle = (double)dir * Math.PI / 4.0;
                        double x = Math.cos(angle) * radius;
                        z = Math.sin(angle) * radius;
                        for (double y = 0.0; y < 4.0; y += 0.5) {
                            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, y, z), 3, 0.1, 0.1, 0.1, 0.0, (Object)fieldDust, true);
                        }
                    }
                    for (double y = 0.0; y < 3.0; y += 0.3) {
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0.0, y, 0.0), 4, 0.2, 0.1, 0.2, 0.01);
                    }
                }
                if (this.ticksElapsed % 40 == 0) {
                    center.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.8f);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        this.speedStormTasks.put(uuid, stormTask);
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, durationSeconds);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Speed Storm");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        player.sendMessage("\u00a7e\u00a7oSpeed Storm active for " + durationSeconds + "s! Allies gain Speed + Haste, enemies freeze!");
    }

    private void endSpeedStorm(Player player) {
        UUID uuid = player.getUniqueId();
        if (!this.speedStormActivePlayers.remove(uuid)) {
            return;
        }
        BukkitTask task = this.speedStormTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        this.plugin.getAbilityManager().endAbilityDuration(player, "speed-storm");
        double radius = this.plugin.getConfig().getDouble("abilities.speed-storm.radius", 8.0);
        if (player.isOnline()) {
            for (Entity entity : player.getLocation().getWorld().getNearbyEntities(player.getLocation(), radius + 5.0, radius + 5.0, radius + 5.0)) {
                UUID targetId;
                Player target;
                if (!(entity instanceof Player) || (target = (Player)entity).equals((Object)player) || !SpeedAbilities.isPlayerFrozen(targetId = target.getUniqueId())) continue;
                boolean stillInOtherField = false;
                for (UUID stormOwner : this.speedStormActivePlayers) {
                    Player owner = this.plugin.getServer().getPlayer(stormOwner);
                    if (owner == null || !owner.isOnline() || owner.equals((Object)player) || !(target.getLocation().distance(owner.getLocation()) <= radius)) continue;
                    stillInOtherField = true;
                    break;
                }
                if (stillInOtherField) continue;
                SpeedAbilities.unfreezePlayer(targetId);
                target.sendMessage("\u00a7e\u00a7oYou are no longer frozen.");
            }
            player.sendMessage("\u00a7e\u00a7oSpeed Storm faded.");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 1.5f);
        }
    }

    public void terminalVelocity(Player player) {
        String abilityKey = "speed-terminal";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        int duration = this.plugin.getConfig().getInt("abilities.durations.terminal-velocity", 10) * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 2, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration, 4, false, true));
        Particle.DustOptions yellowDust = new Particle.DustOptions(ParticleUtils.SPEED_YELLOW, 2.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 2.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.5f, 2.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
        player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 300, 1.5, 2.0, 1.5, 0.0, (Object)yellowDust, true);
        player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 200, 1.5, 2.0, 1.5);
        player.spawnParticle(Particle.GUST, player.getLocation().add(0.0, 1.0, 0.0), 100, 1.0, 1.5, 1.0);
        player.spawnParticle(Particle.EXPLOSION, player.getLocation().add(0.0, 1.0, 0.0), 8, 1.0, 1.0, 1.0);
        player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0.0, 1.0, 0.0), 60, 1.0, 1.0, 1.0);
        for (int i = 0; i < 32; ++i) {
            double angle = (double)i / 32.0 * 2.0 * Math.PI;
            double x = Math.cos(angle) * 2.5;
            double z = Math.sin(angle) * 2.5;
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(x, 0.5, z), 8, 0.2, 0.1, 0.2, 0.0, (Object)yellowDust, true);
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(x, 1.0, z), 4, 0.1, 0.1, 0.1);
        }
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, duration / 20);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Terminal Velocity");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        player.sendMessage("\u00a7e\u00a7l\u26a1 TERMINAL VELOCITY \u00a77- Speed III + Haste V for " + duration / 20 + " seconds!");
    }

    public void cleanup(UUID playerId) {
        Player p;
        if (this.speedStormTasks.containsKey(playerId)) {
            this.speedStormTasks.get(playerId).cancel();
            this.speedStormTasks.remove(playerId);
        }
        if (this.speedStormActivePlayers.remove(playerId) && (p = this.plugin.getServer().getPlayer(playerId)) != null) {
            this.plugin.getAbilityManager().endAbilityDuration(p, "speed-storm");
        }
        this.blurCharges.remove(playerId);
        BukkitTask blurExpiry = this.blurExpiryTasks.remove(playerId);
        if (blurExpiry != null) {
            blurExpiry.cancel();
        }
        SpeedAbilities.unfreezePlayer(playerId);
    }

    public static void cleanupAllFrozen() {
        frozenPlayers.clear();
    }
}

