/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.util.Vector
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.utils.ParticleUtils;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class PuffAbilities
implements GemAbilityHandler {
    private final BlissGems plugin;
    private final Set<UUID> fallDamageImmune = new HashSet<UUID>();

    public PuffAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    public boolean hasFallDamageImmunity(Player player) {
        return this.fallDamageImmune.contains(player.getUniqueId());
    }

    public void removeFallDamageImmunity(Player player) {
        this.fallDamageImmune.remove(player.getUniqueId());
    }

    public void spawnGroundImpactRing(final Location center) {
        final World world = center.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(Particle.EXPLOSION, center.clone().add(0.0, 0.2, 0.0), 1);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.4f);
        world.playSound(center, Sound.ENTITY_BREEZE_LAND, 1.0f, 0.6f);
        new BukkitRunnable(){
            int tick = 0;
            final int maxTicks = 12;

            public void run() {
                if (this.tick >= 12 || !world.isChunkLoaded(center.getBlockX() >> 4, center.getBlockZ() >> 4)) {
                    this.cancel();
                    return;
                }
                double progress = (double)this.tick / 12.0;
                double radius = 0.6 + progress * 4.2;
                int g = Math.max(70, (int)(225.0 - progress * 150.0));
                int b = Math.max(0, (int)(80.0 - progress * 80.0));
                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB((int)255, (int)g, (int)b), 1.7f);
                Location c = center.clone().add(0.0, 0.15, 0.0);
                int points = Math.max(12, (int)(radius * 10.0));
                for (int i = 0; i < points; ++i) {
                    double a = Math.PI * 2 * (double)i / (double)points;
                    world.spawnParticle(Particle.DUST, c.getX() + Math.cos(a) * radius, c.getY(), c.getZ() + Math.sin(a) * radius, 1, 0.0, 0.0, 0.0, 0.0, (Object)dust);
                }
                if (this.tick % 2 == 0) {
                    world.spawnParticle(Particle.LAVA, center.clone().add(0.0, 0.3, 0.0), 3, 0.5, 0.1, 0.5, 0.0);
                    world.spawnParticle(Particle.FLAME, center.clone().add(0.0, 0.2, 0.0), 4, radius * 0.4, 0.05, radius * 0.4, 0.01);
                }
                ++this.tick;
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    public void impactRingOnLand(final Player target) {
        new BukkitRunnable(){
            int waited = 0;

            public void run() {
                if (target == null || !target.isOnline() || this.waited > 100) {
                    this.cancel();
                    return;
                }
                if (target.isOnGround()) {
                    PuffAbilities.this.spawnGroundImpactRing(target.getLocation());
                    this.cancel();
                    return;
                }
                ++this.waited;
            }
        }.runTaskTimer((Plugin)this.plugin, 2L, 1L);
    }

    public void onRightClick(Player player, int tier) {
        if (tier == 2 && player.isSneaking()) {
            this.breezyBash(player);
        } else {
            this.dash(player);
        }
    }

    @Override
    public void onPrimary(Player player, int tier) {
        this.dash(player);
    }

    @Override
    public void onSecondary(Player player, int tier) {
        this.breezyBash(player);
    }

    // Breezy Bash is one ability now (single or group by aim), on both slots
    @Override
    public void onTertiary(Player player, int tier) {
        this.breezyBash(player);
    }

    @Override
    public void onQuaternary(Player player, int tier) {
        this.updraft(player);
    }

    public void dash(Player player) {
        String abilityKey = "puff-dash";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        // A dash, not a lunge: hold a flat, constant velocity for a few ticks, hitting whoever
        // the user passes through once, then stop instead of sailing on in an arc.
        double speed = this.plugin.getConfig().getDouble("abilities.puff-dash.speed", 1.6);
        int ticks = this.plugin.getConfig().getInt("abilities.puff-dash.ticks", 6);
        double damage = this.plugin.getConfigManager().getAbilityDamage("puff-dash");
        Vector direction = player.getLocation().getDirection().setY(0);
        if (direction.lengthSquared() < 1.0E-4) {
            direction = new Vector(0, 0, 1).rotateAroundY(-Math.toRadians(player.getLocation().getYaw()));
        }
        Vector velocity = direction.normalize().multiply(speed).setY(0.05);
        Set<UUID> hit = new HashSet<UUID>();
        new BukkitRunnable() {
            int t;

            public void run() {
                if (!player.isOnline() || player.isDead() || this.t++ >= ticks) {
                    if (player.isOnline()) {
                        // ease out instead of stopping dead
                        player.setVelocity(velocity.clone().multiply(0.6).setY(player.getVelocity().getY()));
                    }
                    this.cancel();
                    return;
                }
                player.setVelocity(velocity);
                player.setFallDistance(0.0f);
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.6, 0), 4, 0.2, 0.3, 0.2, 0.02);
                for (Entity entity : player.getNearbyEntities(1.2, 1.4, 1.2)) {
                    if (!(entity instanceof LivingEntity) || entity == player || !hit.add(entity.getUniqueId()) || entity instanceof Player && PuffAbilities.this.plugin.getTrustedPlayersManager().isTrusted(player, (Player)entity)) continue;
                    ((LivingEntity)entity).damage(damage, (Entity)player);
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1.3f);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.5f);
        Particle.DustOptions whiteDust = new Particle.DustOptions(ParticleUtils.PUFF_WHITE, 1.5f);
        player.spawnParticle(Particle.DUST, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.0, (Object)whiteDust, true);
        player.spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Dash");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    /**
     * Breezy Bash (Bliss): aimed at a player it flings them up and slams them back down; aimed at
     * nobody it flings you and every enemy around you up, no slam.
     */
    public void breezyBash(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "puff-breezy-bash";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        double range = this.plugin.getConfig().getDouble("abilities.puff-breezy-bash.range", 12.0);
        RayTraceResult aim = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), range, 0.6,
            e -> e instanceof Player && e != player && !this.plugin.getTrustedPlayersManager().isTrusted(player, (Player)e));
        Player target = aim != null ? (Player)aim.getHitEntity() : null;
        if (target != null) {
            this.singleBash(player, target);
        } else {
            this.groupBash(player);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 0.8f);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Breezy Bash");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    private void singleBash(Player player, Player target) {
        double launch = this.plugin.getConfig().getDouble("abilities.puff-breezy-bash.launch-velocity", 2.2);
        double slam = this.plugin.getConfig().getDouble("abilities.puff-breezy-bash.slam-velocity", 3.5);
        double damage = this.plugin.getConfig().getDouble("abilities.puff-breezy-bash.slam-damage", 6.0);
        target.setVelocity(new Vector(0.0, launch, 0.0));
        target.getWorld().spawnParticle(Particle.GUST, target.getLocation(), 6, 0.4, 0.4, 0.4);
        target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
        target.sendMessage("\u00a7b\u00a7oA Breezy Bash flings you into the sky!");
        UUID targetId = target.getUniqueId();
        new BukkitRunnable() {
            int ticks = 0;
            boolean slammed = false;

            public void run() {
                Player t = PuffAbilities.this.plugin.getServer().getPlayer(targetId);
                if (t == null || t.isDead() || ++this.ticks > 100) {
                    this.cancel();
                    return;
                }
                // slam at the top of the arc
                if (!this.slammed && (t.getVelocity().getY() <= 0.05 || this.ticks > 30)) {
                    this.slammed = true;
                    t.setVelocity(new Vector(0.0, -slam, 0.0));
                    t.getWorld().playSound(t.getLocation(), Sound.ENTITY_BREEZE_LAND, 1.0f, 0.5f);
                    return;
                }
                if (this.slammed && t.isOnGround()) {
                    t.damage(damage, player.isOnline() ? player : null);
                    PuffAbilities.this.spawnGroundImpactRing(t.getLocation());
                    this.cancel();
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 4L, 1L);
    }

    private void groupBash(Player player) {
        double radius = this.plugin.getConfig().getDouble("abilities.puff-group-bash.radius", 6.0);
        double launch = this.plugin.getConfig().getDouble("abilities.puff-group-bash.launch-velocity", 1.8);
        int hitCount = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Player target) || this.plugin.getTrustedPlayersManager().isTrusted(player, target)) continue;
            target.setVelocity(target.getVelocity().setY(launch));
            target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 20, 0.4, 0.3, 0.4, 0.05);
            target.sendMessage("\u00a7f\u00a7oA gust of wind flings you up!");
            ++hitCount;
        }
        player.setVelocity(player.getVelocity().setY(launch));
        this.fallDamageImmune.add(player.getUniqueId());
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> this.fallDamageImmune.remove(player.getUniqueId()), 200L);
        Particle.DustOptions whiteDust = new Particle.DustOptions(ParticleUtils.PUFF_WHITE, 1.5f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 60, 2.0, 1.0, 2.0, 0.0, (Object)whiteDust, true);
        player.getWorld().spawnParticle(Particle.GUST, player.getLocation().add(0.0, 0.5, 0.0), 12, 2.0, 0.5, 2.0);
        if (hitCount > 0) {
            player.sendMessage("\u00a7f\u00a7o" + hitCount + " player(s) flung up!");
        }
    }

    public void updraft(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "puff-updraft";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        double radius = this.plugin.getConfig().getDouble("abilities.puff-updraft.radius", 6.0);
        double launchVelocity = this.plugin.getConfig().getDouble("abilities.puff-updraft.launch-velocity", 3.2);
        int hitCount = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Player)) continue;
            Player target = (Player)entity;
            if (this.plugin.getTrustedPlayersManager().isTrusted(player, target)) continue;
            target.setVelocity(new Vector(0.0, launchVelocity, 0.0));
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 0.8f);
            target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
            target.sendMessage("\u00a7b\u00a7oAn updraft flings you into the sky!");
            this.impactRingOnLand(target);
            ++hitCount;
        }
        Particle.DustOptions whiteDust = new Particle.DustOptions(ParticleUtils.PUFF_WHITE, 1.5f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 60, 2.0, 1.0, 2.0, 0.0, (Object)whiteDust, true);
        player.getWorld().spawnParticle(Particle.GUST, player.getLocation().add(0.0, 0.5, 0.0), 20, 2.0, 0.5, 2.0);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1.0f, 0.7f);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Updraft");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        if (hitCount > 0) {
            player.sendMessage("\u00a7f\u00a7o" + hitCount + " player(s) sent flying!");
        }
    }
}
