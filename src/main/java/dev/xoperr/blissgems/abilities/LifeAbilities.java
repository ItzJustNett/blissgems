package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.ParticleUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LifeAbilities {
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
        int duration = this.plugin.getConfigManager().getAbilityDuration("life-drainer");
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration * 20, 1, false, true));

        Particle.DustOptions pinkDust = new Particle.DustOptions(ParticleUtils.LIFE_PINK, 1.5f);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.0, pinkDust, true);
        target.getWorld().spawnParticle(Particle.SCULK_SOUL, target.getLocation().add(0.0, 1.0, 0.0), 20, 0.5, 0.5, 0.5);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 1.5f);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Heart Drainer"));
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

        final Location circleLocation = player.getLocation().clone();
        final Set<UUID> modifiedInCircle = new HashSet<>();
        final NamespacedKey modifierKey = new NamespacedKey(this.plugin, "circle-of-life");

        final int[] ticksElapsed = {0};
        BukkitTask circleTask = this.plugin.getServer().getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            if (!player.isOnline() || player.isDead() || ticksElapsed[0] >= durationTicks) {
                return;
            }

            // Apply effects to owner if in circle
            if (player.getLocation().distance(circleLocation) <= radius) {
                applyCircleModifier(player, modifierKey, healthIncrease, modifiedInCircle);
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1, true, true));
            } else {
                removeCircleModifier(player, modifierKey, modifiedInCircle);
            }

            // Apply effects to nearby entities
            for (Entity entity : circleLocation.getWorld().getNearbyEntities(circleLocation, radius, radius, radius)) {
                if (entity == player) continue;
                if (!(entity instanceof Player)) {
                    if (entity instanceof LivingEntity) {
                        ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0, false, true));
                    }
                    continue;
                }

                Player nearby = (Player) entity;
                boolean trusted = this.plugin.getTrustedPlayersManager().isTrusted(player, nearby);

                if (trusted) {
                    applyCircleModifier(nearby, modifierKey, healthIncrease, modifiedInCircle);
                    nearby.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1, true, true));
                } else {
                    applyCircleModifier(nearby, modifierKey, -healthDecrease, modifiedInCircle);
                    nearby.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0, false, true));
                }
            }

            // Remove modifiers from players who left the circle
            Set<UUID> toRemove = new HashSet<>();
            for (UUID uuid : new HashSet<>(modifiedInCircle)) {
                if (uuid.equals(player.getUniqueId())) continue;
                Player modified = plugin.getServer().getPlayer(uuid);
                if (modified == null || !modified.isOnline() || modified.getLocation().distance(circleLocation) > radius) {
                    if (modified != null && modified.isOnline()) {
                        removeCircleModifierDirect(modified, modifierKey);
                    }
                    toRemove.add(uuid);
                }
            }
            modifiedInCircle.removeAll(toRemove);

            // Particles
            Particle.DustOptions pinkCircleDust = new Particle.DustOptions(ParticleUtils.LIFE_PINK_ALT, 1.0f);
            player.getWorld().spawnParticle(Particle.DUST, circleLocation.clone().add(0, 1, 0), 30, 4.0, 0.5, 4.0, 0.0, pinkCircleDust, true);
            player.getWorld().spawnParticle(Particle.HEART, circleLocation.clone().add(0, 1, 0), 20, 4.0, 0.5, 4.0);
            player.getWorld().spawnParticle(Particle.SCULK_SOUL, circleLocation.clone().add(0, 0.5, 0), 30, 4.0, 0.2, 4.0);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, circleLocation.clone().add(0, 0.2, 0), 15, 3.0, 0.1, 3.0);

            // Circle border
            int circlePoints = 32;
            for (int i = 0; i < circlePoints; i++) {
                double angle = (i / (double) circlePoints) * 2 * Math.PI;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                player.getWorld().spawnParticle(Particle.DUST,
                    circleLocation.clone().add(x, 0.2, z),
                    2, 0.1, 0.1, 0.1, 0.0, pinkCircleDust, true);
                player.getWorld().spawnParticle(Particle.HEART,
                    circleLocation.clone().add(x, 1.0, z),
                    1, 0.05, 0.05, 0.05, 0);
                player.getWorld().spawnParticle(Particle.SCULK_SOUL,
                    circleLocation.clone().add(x, 1.8, z),
                    1, 0.05, 0.05, 0.05, 0);
            }

            ticksElapsed[0] += 20;
        }, 0L, 20L);

        // Cancel task and clean up modifiers after duration
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            circleTask.cancel();
            for (UUID uuid : modifiedInCircle) {
                Player modified = plugin.getServer().getPlayer(uuid);
                if (modified != null && modified.isOnline()) {
                    removeCircleModifierDirect(modified, modifierKey);
                }
            }
            modifiedInCircle.clear();
        }, durationTicks);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Circle of Life"));
    }

    private void applyCircleModifier(Player player, NamespacedKey key, double amount, Set<UUID> tracked) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;

        boolean hasModifier = attr.getModifiers().stream().anyMatch(m -> m.getKey().equals(key));
        if (hasModifier) return;

        attr.addTransientModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER));
        tracked.add(player.getUniqueId());
    }

    private void removeCircleModifier(Player player, NamespacedKey key, Set<UUID> tracked) {
        removeCircleModifierDirect(player, key);
        tracked.remove(player.getUniqueId());
    }

    private void removeCircleModifierDirect(Player player, NamespacedKey key) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;
        attr.getModifiers().stream()
            .filter(m -> m.getKey().equals(key))
            .forEach(attr::removeModifier);
    }

    public void vitalityVortex(Player player) {
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

        PotionEffect[] effects = getBiomeEffects(biomeName, durationTicks);
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }

        // Pink particle burst on activation
        Particle.DustOptions pinkDust = new Particle.DustOptions(ParticleUtils.LIFE_PINK, 1.5f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 50, 1.0, 1.0, 1.0, 0.0, pinkDust, true);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.5, 0), 15, 0.5, 0.5, 0.5);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Vitality Vortex"));
    }

    private PotionEffect[] getBiomeEffects(String biomeName, int durationTicks) {
        // Forest/Plains/Meadow
        if (biomeName.contains("FOREST") || biomeName.contains("PLAINS") || biomeName.contains("MEADOW")) {
            return new PotionEffect[]{
                new PotionEffect(PotionEffectType.REGENERATION, durationTicks, 1, true, true),
                new PotionEffect(PotionEffectType.SATURATION, durationTicks, 0, true, true)
            };
        }
        // Desert/Badlands/Savanna
        if (biomeName.contains("DESERT") || biomeName.contains("BADLANDS") || biomeName.contains("SAVANNA")) {
            return new PotionEffect[]{
                new PotionEffect(PotionEffectType.FIRE_RESISTANCE, durationTicks, 0, true, true),
                new PotionEffect(PotionEffectType.STRENGTH, durationTicks, 0, true, true)
            };
        }
        // Ocean/River/Beach/Swamp
        if (biomeName.contains("OCEAN") || biomeName.contains("RIVER") || biomeName.contains("BEACH") || biomeName.contains("SWAMP")) {
            return new PotionEffect[]{
                new PotionEffect(PotionEffectType.WATER_BREATHING, durationTicks, 0, true, true),
                new PotionEffect(PotionEffectType.DOLPHINS_GRACE, durationTicks, 0, true, true)
            };
        }
        // Taiga/Snowy/Frozen
        if (biomeName.contains("TAIGA") || biomeName.contains("SNOWY") || biomeName.contains("FROZEN") || biomeName.contains("ICE")) {
            return new PotionEffect[]{
                new PotionEffect(PotionEffectType.RESISTANCE, durationTicks, 0, true, true),
                new PotionEffect(PotionEffectType.SPEED, durationTicks, 0, true, true)
            };
        }
        // Jungle
        if (biomeName.contains("JUNGLE")) {
            return new PotionEffect[]{
                new PotionEffect(PotionEffectType.SPEED, durationTicks, 1, true, true),
                new PotionEffect(PotionEffectType.JUMP_BOOST, durationTicks, 0, true, true)
            };
        }
        // Nether biomes
        if (biomeName.contains("NETHER") || biomeName.contains("CRIMSON") || biomeName.contains("WARPED")
            || biomeName.contains("BASALT") || biomeName.contains("SOUL_SAND")) {
            return new PotionEffect[]{
                new PotionEffect(PotionEffectType.FIRE_RESISTANCE, durationTicks, 0, true, true),
                new PotionEffect(PotionEffectType.STRENGTH, durationTicks, 1, true, true)
            };
        }
        // End biomes
        if (biomeName.contains("END") && !biomeName.contains("ENDER")) {
            return new PotionEffect[]{
                new PotionEffect(PotionEffectType.SLOW_FALLING, durationTicks, 0, true, true),
                new PotionEffect(PotionEffectType.NIGHT_VISION, durationTicks, 0, true, true)
            };
        }
        // Mountain/Stony/Cave
        if (biomeName.contains("MOUNTAIN") || biomeName.contains("STONY") || biomeName.contains("CAVE")
            || biomeName.contains("DRIPSTONE") || biomeName.contains("LUSH") || biomeName.contains("DEEP_DARK")) {
            return new PotionEffect[]{
                new PotionEffect(PotionEffectType.HASTE, durationTicks, 1, true, true),
                new PotionEffect(PotionEffectType.NIGHT_VISION, durationTicks, 0, true, true)
            };
        }
        // Default
        return new PotionEffect[]{
            new PotionEffect(PotionEffectType.REGENERATION, durationTicks, 0, true, true),
            new PotionEffect(PotionEffectType.SPEED, durationTicks, 0, true, true)
        };
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

        // Skip trusted players
        if (targetEntity instanceof Player) {
            Player targetPlayer = (Player) targetEntity;
            if (this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                player.sendMessage("\u00a7c\u00a7oYou cannot Heart Lock a trusted player!");
                return;
            }
        }

        int duration = this.plugin.getConfigManager().getAbilityDuration("life-heart-lock");
        int durationTicks = duration * 20;

        AttributeInstance targetMaxHealthAttr = targetEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (targetMaxHealthAttr == null) {
            player.sendMessage("\u00a7cCannot Heart Lock this target!");
            return;
        }

        double currentHealth = targetEntity.getHealth();
        double maxHealth = targetMaxHealthAttr.getValue();
        double reduction = currentHealth - maxHealth; // Negative value — caps max health at current

        NamespacedKey modifierKey = new NamespacedKey(this.plugin, "heart-lock");

        // Remove any existing heart lock modifier first
        targetMaxHealthAttr.getModifiers().stream()
            .filter(m -> m.getKey().equals(modifierKey))
            .forEach(targetMaxHealthAttr::removeModifier);

        // Apply modifier to cap max health at current health
        targetMaxHealthAttr.addTransientModifier(new AttributeModifier(modifierKey, reduction, AttributeModifier.Operation.ADD_NUMBER));

        // Schedule removal after duration
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (targetEntity.isValid()) {
                AttributeInstance attr = targetEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (attr != null) {
                    attr.getModifiers().stream()
                        .filter(m -> m.getKey().equals(modifierKey))
                        .forEach(attr::removeModifier);
                }
            }
        }, durationTicks);

        // Pink + dark particle effects
        Particle.DustOptions pinkDust = new Particle.DustOptions(ParticleUtils.LIFE_PINK, 1.5f);
        Particle.DustOptions darkPink = new Particle.DustOptions(Color.fromRGB(150, 0, 100), 1.2f);
        targetEntity.getWorld().spawnParticle(Particle.DUST, targetEntity.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.0, pinkDust, true);
        targetEntity.getWorld().spawnParticle(Particle.DUST, targetEntity.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.0, darkPink, true);

        // Glass break sound
        targetEntity.getWorld().playSound(targetEntity.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.5f, 0.5f);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Heart Lock"));
        if (targetEntity instanceof Player) {
            ((Player) targetEntity).sendMessage("\u00a7d\u00a7oYour max health has been locked!");
        }
    }

    private LivingEntity getTargetEntity(Player player, int range) {
        return player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), (double)range, entity -> entity instanceof LivingEntity && entity != player) != null ? (LivingEntity)player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), (double)range, entity -> entity instanceof LivingEntity && entity != player).getHitEntity() : null;
    }
}
