/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.util.Vector
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.utils.ParticleUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PuffAbilities implements GemAbilityHandler {
    private final BlissGems plugin;
    private final Set<UUID> fallDamageImmune = new HashSet<>();

    public PuffAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    public boolean hasFallDamageImmunity(Player player) {
        return fallDamageImmune.contains(player.getUniqueId());
    }

    public void removeFallDamageImmunity(Player player) {
        fallDamageImmune.remove(player.getUniqueId());
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

    @Override
    public void onTertiary(Player player, int tier) {
        this.groupBreezyBash(player);
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
        // Flatter, longer lunge so the dash is a viable forward attack (was Y 0.3, x2.5).
        double power = this.plugin.getConfig().getDouble("abilities.puff-dash.power", 3.2);
        double vertical = this.plugin.getConfig().getDouble("abilities.puff-dash.vertical", 0.18);
        Vector direction = player.getLocation().getDirection();
        direction.setY(vertical);
        direction.multiply(power);
        player.setVelocity(direction);
        // Damage what the dash slams into — players included (trusted allies excepted).
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            double damage = this.plugin.getConfigManager().getAbilityDamage("puff-dash");
            for (Entity entity : player.getNearbyEntities(2.5, 2.0, 2.5)) {
                if (!(entity instanceof LivingEntity) || entity == player) continue;
                if (entity instanceof Player && this.plugin.getTrustedPlayersManager().isTrusted(player, (Player) entity)) {
                    continue;
                }
                ((LivingEntity) entity).damage(damage, (Entity) player);
            }
        }, 5L);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.5f);

        // Puff white dust particles (RGB 255, 255, 255) + clouds
        Particle.DustOptions whiteDust = new Particle.DustOptions(ParticleUtils.PUFF_WHITE, 1.5f);
        player.spawnParticle(Particle.DUST, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.0, whiteDust, true);
        player.spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Dash");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    public void breezyBash(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "puff-breezy-bash";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        Vector velocity = player.getVelocity();
        velocity.setY(2.0);
        player.setVelocity(velocity);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1.0f, 1.0f);

        // Breezy Bash white dust particles (RGB 255, 255, 255) + clouds
        Particle.DustOptions whiteDust = new Particle.DustOptions(ParticleUtils.PUFF_WHITE, 1.5f);
        player.spawnParticle(Particle.DUST, player.getLocation(), 50, 1.0, 1.0, 1.0, 0.0, whiteDust, true);
        player.spawnParticle(Particle.CLOUD, player.getLocation(), 40, 1.0, 1.0, 1.0, 0.2);

        // Add temporary fall damage immunity
        UUID uuid = player.getUniqueId();
        fallDamageImmune.add(uuid);

        // Remove immunity after 10 seconds
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            fallDamageImmune.remove(uuid);
        }, 200L);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Breezy Bash");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    /**
     * Puff's 4th ability (Quaternary): an updraft that flings nearby non-trusted players
     * into the sky. This promotes the mace-hit "launch" into a real triggerable ability so
     * Puff has a full four-slot kit.
     */
    public void updraft(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("§c§oThis ability requires Tier 2!");
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
            Player target = (Player) entity;
            if (this.plugin.getTrustedPlayersManager().isTrusted(player, target)) {
                continue;
            }
            target.setVelocity(new Vector(0.0, launchVelocity, 0.0));
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 0.8f);
            target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
            target.sendMessage("§b§oAn updraft flings you into the sky!");
            hitCount++;
        }

        Particle.DustOptions whiteDust = new Particle.DustOptions(ParticleUtils.PUFF_WHITE, 1.5f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 60, 2.0, 1.0, 2.0, 0.0, whiteDust, true);
        player.getWorld().spawnParticle(Particle.GUST, player.getLocation().add(0, 0.5, 0), 20, 2.0, 0.5, 2.0);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1.0f, 0.7f);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Updraft");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        if (hitCount > 0) {
            player.sendMessage("§f§o" + hitCount + " player(s) sent flying!");
        }
    }

    public void groupBreezyBash(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "puff-group-bash";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        double radius = this.plugin.getConfig().getDouble("abilities.puff-group-bash.radius", 10.0);
        double knockback = this.plugin.getConfig().getDouble("abilities.puff-group-bash.knockback", 2.5);

        int hitCount = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Player)) continue;
            Player target = (Player) entity;

            // Skip trusted players
            if (this.plugin.getTrustedPlayersManager().isTrusted(player, target)) {
                continue;
            }

            // Calculate knockback direction: caster → target
            Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector());
            if (direction.lengthSquared() < 0.01) {
                direction = new Vector(1, 0, 0); // Fallback if on same block
            }
            direction.normalize().multiply(knockback);
            direction.setY(1.5);
            target.setVelocity(direction);

            target.sendMessage("\u00a7f\u00a7oA gust of wind blows you away!");
            hitCount++;
        }

        // Particles + sound
        Particle.DustOptions whiteDust = new Particle.DustOptions(ParticleUtils.PUFF_WHITE, 1.5f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 60, 2.0, 1.0, 2.0, 0.0, whiteDust, true);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.5, 0), 50, 2.0, 0.5, 2.0, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 0.8f);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Group Breezy Bash");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        if (hitCount > 0) {
            player.sendMessage("\u00a7f\u00a7o" + hitCount + " player(s) knocked away!");
        }
    }
}

