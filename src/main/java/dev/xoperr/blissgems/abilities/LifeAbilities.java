/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.attribute.AttributeInstance
 *  org.bukkit.attribute.AttributeModifier
 *  org.bukkit.attribute.AttributeModifier$Operation
 *  org.bukkit.block.Biome
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.EquipmentSlotGroup
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitTask
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.utils.Attributes;
import dev.xoperr.blissgems.utils.ParticleUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class LifeAbilities
implements GemAbilityHandler {
    private final BlissGems plugin;

    public LifeAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    public void onRightClick(Player player, int tier) {
        if (tier == 2 && player.isSneaking()) {
            this.circleOfLife(player);
        } else {
            this.heartDrainer(player);
        }
    }

    @Override
    public void onPrimary(Player player, int tier) {
        this.heartDrainer(player);
    }

    @Override
    public void onSecondary(Player player, int tier) {
        this.circleOfLife(player);
    }

    @Override
    public void onTertiary(Player player, int tier) {
        this.vitalityVortex(player);
    }

    @Override
    public void onQuaternary(Player player, int tier) {
        this.heartLock(player);
    }

    public void heartDrainer(Player player) {
        String abilityKey = "life-drainer";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        LivingEntity target = this.getTargetEntity(player, 15);
        if (target == null) {
            player.sendMessage("\u00a7cNo target found!");
            return;
        }
        if (target instanceof Player) {
            Player targetPlayer = (Player)target;
            if (this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                player.sendMessage("\u00a7c\u00a7oYou cannot Heart Drain a trusted player!");
                return;
            }
        }
        int duration = this.plugin.getConfigManager().getAbilityDuration("life-drainer");
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration * 20, 1, false, true));
        double stealHp = this.plugin.getConfig().getDouble("abilities.life-drainer.steal-hp", 4.0);
        if (stealHp > 0.0 && target.getHealth() > 0.0) {
            double drained = Math.min(stealHp, target.getHealth());
            target.damage(stealHp, (Entity)player);
            double newHealth = Math.min(player.getHealth() + drained, player.getMaxHealth());
            player.setHealth(Math.max(0.0, newHealth));
        }
        Particle.DustOptions pinkDust = new Particle.DustOptions(ParticleUtils.LIFE_PINK, 1.5f);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.0, (Object)pinkDust, true);
        target.getWorld().spawnParticle(Particle.SCULK_SOUL, target.getLocation().add(0.0, 1.0, 0.0), 20, 0.5, 0.5, 0.5);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 1.5f);
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, duration);
        this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "ability-activated", "ability", "Heart Drainer");
    }

    public void circleOfLife(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "life-circle-of-life";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        int duration = this.plugin.getConfigManager().getAbilityDuration("life-circle");
        int durationTicks = duration * 20;
        double radius = this.plugin.getConfig().getDouble("abilities.life-circle.radius", 8.0);
        double healthIncrease = this.plugin.getConfig().getDouble("abilities.life-circle.max-health-increase", 4.0);
        double healthDecrease = this.plugin.getConfig().getDouble("abilities.life-circle.max-health-decrease", 4.0);
        Set<UUID> modifiedInCircle = new HashSet<>();
        NamespacedKey modifierKey = new NamespacedKey((Plugin)this.plugin, "circle-of-life");
        int[] ticksElapsed = new int[]{0};
        BukkitTask circleTask = this.plugin.getServer().getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            if (!player.isOnline() || player.isDead() || ticksElapsed[0] >= durationTicks) {
                return;
            }
            Location circleLocation = player.getLocation();
            this.applyCircleModifier(player, modifierKey, healthIncrease, modifiedInCircle);
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1, true, true));
            for (Object entity : circleLocation.getWorld().getNearbyEntities(circleLocation, radius, radius, radius)) {
                if (entity == player) continue;
                if (!(entity instanceof Player)) {
                    if (!(entity instanceof LivingEntity)) continue;
                    ((LivingEntity)entity).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0, false, true));
                    continue;
                }
                Player nearby = (Player)entity;
                boolean trusted = this.plugin.getTrustedPlayersManager().isTrusted(player, nearby);
                if (trusted) {
                    this.applyCircleModifier(nearby, modifierKey, healthIncrease, modifiedInCircle);
                    nearby.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1, true, true));
                    continue;
                }
                this.applyCircleModifier(nearby, modifierKey, -healthDecrease, modifiedInCircle);
                nearby.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0, false, true));
            }
            HashSet<UUID> toRemove = new HashSet<UUID>();
            for (UUID uuid : new HashSet<>(modifiedInCircle)) {
                Player modified;
                if (uuid.equals(player.getUniqueId()) || (modified = this.plugin.getServer().getPlayer(uuid)) != null && modified.isOnline() && !(modified.getLocation().distance(circleLocation) > radius)) continue;
                if (modified != null && modified.isOnline()) {
                    this.removeCircleModifierDirect(modified, modifierKey);
                }
                toRemove.add(uuid);
            }
            modifiedInCircle.removeAll(toRemove);
            Particle.DustOptions pinkCircleDust = new Particle.DustOptions(ParticleUtils.LIFE_PINK_ALT, 1.0f);
            player.getWorld().spawnParticle(Particle.DUST, circleLocation.clone().add(0.0, 1.0, 0.0), 30, 4.0, 0.5, 4.0, 0.0, (Object)pinkCircleDust, true);
            player.getWorld().spawnParticle(Particle.HEART, circleLocation.clone().add(0.0, 1.0, 0.0), 20, 4.0, 0.5, 4.0, 0.0, null, true);
            player.getWorld().spawnParticle(Particle.SCULK_SOUL, circleLocation.clone().add(0.0, 0.5, 0.0), 30, 4.0, 0.2, 4.0, 0.0, null, true);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, circleLocation.clone().add(0.0, 0.2, 0.0), 15, 3.0, 0.1, 3.0, 0.0, null, true);
            int circlePoints = 32;
            for (int i = 0; i < circlePoints; ++i) {
                double angle = (double)i / (double)circlePoints * 2.0 * Math.PI;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                player.getWorld().spawnParticle(Particle.DUST, circleLocation.clone().add(x, 0.2, z), 2, 0.1, 0.1, 0.1, 0.0, (Object)pinkCircleDust, true);
                player.getWorld().spawnParticle(Particle.HEART, circleLocation.clone().add(x, 1.0, z), 1, 0.05, 0.05, 0.05, 0.0, null, true);
                player.getWorld().spawnParticle(Particle.SCULK_SOUL, circleLocation.clone().add(x, 1.8, z), 1, 0.05, 0.05, 0.05, 0.0, null, true);
            }
            ticksElapsed[0] = ticksElapsed[0] + 20;
        }, 0L, 20L);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            circleTask.cancel();
            for (UUID uuid : modifiedInCircle) {
                Player modified = this.plugin.getServer().getPlayer(uuid);
                if (modified == null || !modified.isOnline()) continue;
                this.removeCircleModifierDirect(modified, modifierKey);
            }
            modifiedInCircle.clear();
            this.plugin.getAbilityManager().endAbilityDuration(player, abilityKey);
        }, (long)durationTicks);
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, duration);
        this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "ability-activated", "ability", "Circle of Life");
    }

    private void applyCircleModifier(Player player, NamespacedKey key, double amount, Set<UUID> tracked) {
        AttributeInstance attr = player.getAttribute(Attributes.maxHealth());
        if (attr == null) {
            return;
        }
        boolean hasModifier = attr.getModifiers().stream().anyMatch(m -> this.matchesKey((AttributeModifier)m, key));
        if (hasModifier) {
            return;
        }
        attr.addModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        tracked.add(player.getUniqueId());
    }

    private void removeCircleModifier(Player player, NamespacedKey key, Set<UUID> tracked) {
        this.removeCircleModifierDirect(player, key);
        tracked.remove(player.getUniqueId());
    }

    private void removeCircleModifierDirect(Player player, NamespacedKey key) {
        AttributeInstance attr = player.getAttribute(Attributes.maxHealth());
        if (attr == null) {
            return;
        }
        ArrayList<AttributeModifier> toRemove = new ArrayList<AttributeModifier>();
        for (AttributeModifier m : attr.getModifiers()) {
            if (!this.matchesKey(m, key)) continue;
            toRemove.add(m);
        }
        for (AttributeModifier m : toRemove) {
            attr.removeModifier(m);
        }
    }

    private boolean matchesKey(AttributeModifier m, NamespacedKey key) {
        try {
            NamespacedKey mk = m.getKey();
            if (mk != null && mk.equals((Object)key)) {
                return true;
            }
        }
        catch (Throwable mk) {
            // empty catch block
        }
        String name = m.getName();
        return name != null && (name.equals(key.toString()) || name.equals(key.getKey()));
    }

    public void vitalityVortex(Player player) {
        PotionEffect[] effects;
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "life-vitality-vortex";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        int duration = this.plugin.getConfigManager().getAbilityDuration("life-vitality-vortex");
        int durationTicks = duration * 20;
        Biome biome = player.getLocation().getBlock().getBiome();
        String biomeName = biome.name().toUpperCase();
        for (PotionEffect effect : effects = this.getBiomeEffects(biomeName, durationTicks)) {
            player.addPotionEffect(effect);
        }
        Particle.DustOptions pinkDust = new Particle.DustOptions(ParticleUtils.LIFE_PINK, 1.5f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 50, 1.0, 1.0, 1.0, 0.0, (Object)pinkDust, true);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0.0, 1.5, 0.0), 15, 0.5, 0.5, 0.5);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, duration);
        this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "ability-activated", "ability", "Vitality Vortex");
    }

    private PotionEffect[] getBiomeEffects(String biomeName, int durationTicks) {
        if (biomeName.contains("FOREST") || biomeName.contains("PLAINS") || biomeName.contains("MEADOW")) {
            return new PotionEffect[]{new PotionEffect(PotionEffectType.REGENERATION, durationTicks, 1, true, true), new PotionEffect(PotionEffectType.SATURATION, durationTicks, 0, true, true)};
        }
        if (biomeName.contains("DESERT") || biomeName.contains("BADLANDS") || biomeName.contains("SAVANNA")) {
            return new PotionEffect[]{new PotionEffect(PotionEffectType.FIRE_RESISTANCE, durationTicks, 0, true, true), new PotionEffect(PotionEffectType.STRENGTH, durationTicks, 0, true, true)};
        }
        if (biomeName.contains("OCEAN") || biomeName.contains("RIVER") || biomeName.contains("BEACH") || biomeName.contains("SWAMP")) {
            return new PotionEffect[]{new PotionEffect(PotionEffectType.WATER_BREATHING, durationTicks, 0, true, true), new PotionEffect(PotionEffectType.DOLPHINS_GRACE, durationTicks, 0, true, true)};
        }
        if (biomeName.contains("TAIGA") || biomeName.contains("SNOWY") || biomeName.contains("FROZEN") || biomeName.contains("ICE")) {
            return new PotionEffect[]{new PotionEffect(PotionEffectType.RESISTANCE, durationTicks, 0, true, true), new PotionEffect(PotionEffectType.SPEED, durationTicks, 0, true, true)};
        }
        if (biomeName.contains("JUNGLE")) {
            return new PotionEffect[]{new PotionEffect(PotionEffectType.SPEED, durationTicks, 1, true, true), new PotionEffect(PotionEffectType.JUMP_BOOST, durationTicks, 0, true, true)};
        }
        if (biomeName.contains("NETHER") || biomeName.contains("CRIMSON") || biomeName.contains("WARPED") || biomeName.contains("BASALT") || biomeName.contains("SOUL_SAND")) {
            return new PotionEffect[]{new PotionEffect(PotionEffectType.FIRE_RESISTANCE, durationTicks, 0, true, true), new PotionEffect(PotionEffectType.STRENGTH, durationTicks, 1, true, true)};
        }
        if (biomeName.contains("END") && !biomeName.contains("ENDER")) {
            return new PotionEffect[]{new PotionEffect(PotionEffectType.SLOW_FALLING, durationTicks, 0, true, true), new PotionEffect(PotionEffectType.NIGHT_VISION, durationTicks, 0, true, true)};
        }
        if (biomeName.contains("MOUNTAIN") || biomeName.contains("STONY") || biomeName.contains("CAVE") || biomeName.contains("DRIPSTONE") || biomeName.contains("LUSH") || biomeName.contains("DEEP_DARK")) {
            return new PotionEffect[]{new PotionEffect(PotionEffectType.HASTE, durationTicks, 1, true, true), new PotionEffect(PotionEffectType.NIGHT_VISION, durationTicks, 0, true, true)};
        }
        return new PotionEffect[]{new PotionEffect(PotionEffectType.REGENERATION, durationTicks, 0, true, true), new PotionEffect(PotionEffectType.SPEED, durationTicks, 0, true, true)};
    }

    public void heartLock(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "life-heart-lock";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        int range = this.plugin.getConfig().getInt("abilities.life-heart-lock.range", 15);
        LivingEntity targetEntity = this.getTargetEntity(player, range);
        if (targetEntity == null) {
            player.sendMessage("\u00a7cNo target found!");
            return;
        }
        if (targetEntity instanceof Player) {
            Player targetPlayer = (Player)targetEntity;
            if (this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                player.sendMessage("\u00a7c\u00a7oYou cannot Heart Lock a trusted player!");
                return;
            }
        }
        int duration = this.plugin.getConfigManager().getAbilityDuration("life-heart-lock");
        int durationTicks = duration * 20;
        AttributeInstance targetMaxHealthAttr = targetEntity.getAttribute(Attributes.maxHealth());
        if (targetMaxHealthAttr == null) {
            player.sendMessage("\u00a7cCannot Heart Lock this target!");
            return;
        }
        double currentHealth = targetEntity.getHealth();
        double maxHealth = targetMaxHealthAttr.getValue();
        double reduction = currentHealth - maxHealth;
        NamespacedKey heartLockKey = new NamespacedKey((Plugin)this.plugin, "heart-lock");
        ArrayList<AttributeModifier> existing = new ArrayList<AttributeModifier>();
        for (AttributeModifier m : targetMaxHealthAttr.getModifiers()) {
            if (!this.matchesKey(m, heartLockKey)) continue;
            existing.add(m);
        }
        for (AttributeModifier m : existing) {
            targetMaxHealthAttr.removeModifier(m);
        }
        targetMaxHealthAttr.addModifier(new AttributeModifier(heartLockKey, reduction, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            AttributeInstance attr;
            if (targetEntity.isValid() && (attr = targetEntity.getAttribute(Attributes.maxHealth())) != null) {
                ArrayList<AttributeModifier> toRm = new ArrayList<AttributeModifier>();
                for (AttributeModifier m : attr.getModifiers()) {
                    if (!this.matchesKey(m, heartLockKey)) continue;
                    toRm.add(m);
                }
                for (AttributeModifier m : toRm) {
                    attr.removeModifier(m);
                }
                double newMax = attr.getValue();
                if (targetEntity.getHealth() > newMax) {
                    targetEntity.setHealth(newMax);
                }
            }
            this.plugin.getAbilityManager().endAbilityDuration(player, abilityKey);
        }, (long)durationTicks);
        Particle.DustOptions pinkDust = new Particle.DustOptions(ParticleUtils.LIFE_PINK, 1.5f);
        Particle.DustOptions darkPink = new Particle.DustOptions(Color.fromRGB((int)150, (int)0, (int)100), 1.2f);
        targetEntity.getWorld().spawnParticle(Particle.DUST, targetEntity.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.0, (Object)pinkDust, true);
        targetEntity.getWorld().spawnParticle(Particle.DUST, targetEntity.getLocation().add(0.0, 1.0, 0.0), 20, 0.5, 0.5, 0.5, 0.0, (Object)darkPink, true);
        targetEntity.getWorld().playSound(targetEntity.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.5f, 0.5f);
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, duration);
        this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "ability-activated", "ability", "Heart Lock");
        if (targetEntity instanceof Player) {
            ((Player)targetEntity).sendMessage("\u00a7d\u00a7oYour max health has been locked!");
        }
    }

    @Override
    public void cleanup(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.maxHealth());
        if (attr == null) {
            return;
        }
        ArrayList<AttributeModifier> toRemove = new ArrayList<AttributeModifier>();
        for (AttributeModifier m : attr.getModifiers()) {
            if (!this.isLifeModifier(m)) continue;
            toRemove.add(m);
        }
        for (AttributeModifier m : toRemove) {
            attr.removeModifier(m);
        }
        double max = attr.getValue();
        if (player.getHealth() > max) {
            player.setHealth(max);
        }
        if (!toRemove.isEmpty()) {
            this.plugin.getLogger().info("[LifeAbilities] Removed " + toRemove.size() + " stale life-modifier(s) from " + player.getName());
        }
    }

    private boolean isLifeModifier(AttributeModifier m) {
        if (this.matchesLifeToken(m.getName())) {
            return true;
        }
        try {
            NamespacedKey key = m.getKey();
            if (key != null) {
                if ("blissgems".equals(key.getNamespace())) {
                    return true;
                }
                if (this.matchesLifeToken(key.getKey())) {
                    return true;
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return false;
    }

    private boolean matchesLifeToken(String raw) {
        if (raw == null) {
            return false;
        }
        String norm = raw.toLowerCase().replaceAll("[^a-z0-9]", "");
        return norm.contains("blissgems") || norm.contains("circleoflife") || norm.contains("heartlock");
    }

    private LivingEntity getTargetEntity(Player player, int range) {
        return player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), (double)range, entity -> entity instanceof LivingEntity && entity != player) != null ? (LivingEntity)player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), (double)range, entity -> entity instanceof LivingEntity && entity != player).getHitEntity() : null;
    }
}

