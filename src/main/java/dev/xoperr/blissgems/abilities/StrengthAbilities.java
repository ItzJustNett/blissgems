/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatMessageType
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scheduler.BukkitTask
 *  org.bukkit.util.RayTraceResult
 *  org.bukkit.util.Vector
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.ParticleUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class StrengthAbilities
implements GemAbilityHandler {
    private final BlissGems plugin;
    private static final Set<PotionEffectType> STRIPPABLE_EFFECTS = Set.of(PotionEffectType.SPEED, PotionEffectType.STRENGTH, PotionEffectType.REGENERATION, PotionEffectType.FIRE_RESISTANCE, PotionEffectType.WATER_BREATHING, PotionEffectType.INVISIBILITY, PotionEffectType.NIGHT_VISION, PotionEffectType.ABSORPTION, PotionEffectType.HEALTH_BOOST, PotionEffectType.RESISTANCE, PotionEffectType.HASTE, PotionEffectType.JUMP_BOOST, PotionEffectType.CONDUIT_POWER, PotionEffectType.DOLPHINS_GRACE, PotionEffectType.LUCK, PotionEffectType.HERO_OF_THE_VILLAGE, PotionEffectType.SLOW_FALLING, PotionEffectType.SATURATION);
    private final Map<UUID, UUID> trackingTargets = new HashMap<UUID, UUID>();
    private final Map<UUID, BukkitTask> trackingTasks = new HashMap<UUID, BukkitTask>();
    private final Map<UUID, Map<UUID, Integer>> stalkCounts = new HashMap<UUID, Map<UUID, Integer>>();
    private final Map<UUID, Integer> chadHitsRemaining = new HashMap<UUID, Integer>();
    private final Map<UUID, Long> chadExpiry = new HashMap<UUID, Long>();

    public StrengthAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    public void onRightClick(Player player, int tier) {
        if (tier >= 2 && player.isSneaking()) {
            this.frailer(player);
        } else {
            this.chadStrength(player);
        }
    }

    @Override
    public void onPrimary(Player player, int tier) {
        this.chadStrength(player);
    }

    @Override
    public void onSecondary(Player player, int tier) {
        this.frailer(player);
    }

    @Override
    public void onTertiary(Player player, int tier) {
        this.shadowStalker(player);
    }

    @Override
    public void onQuaternary(Player player, int tier) {
        this.nullify(player);
    }

    public void chadStrength(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "strength-chad";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        int empoweredHits = this.plugin.getConfig().getInt("abilities.strength-chad.empowered-hits", 4);
        int durationSeconds = this.plugin.getConfig().getInt("abilities.strength-chad.duration", 10);
        this.chadHitsRemaining.put(player.getUniqueId(), empoweredHits);
        this.chadExpiry.put(player.getUniqueId(), player.getWorld().getFullTime() + (long)durationSeconds * 20L);
        Particle.DustOptions redDust = new Particle.DustOptions(ParticleUtils.STRENGTH_RED, 1.6f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 45, 0.5, 0.9, 0.5, 0.0, (Object)redDust, true);
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0.0, 1.0, 0.0), 25, 0.5, 0.9, 0.5, 0.4);
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.7f, 1.4f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.6f);
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, durationSeconds);
        this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "ability-activated", "ability", "Chad Strength");
        player.sendMessage("\u00a7c\u00a7l\u2694 \u00a7cYour next \u00a7l" + empoweredHits + "\u00a7c hits are empowered for \u00a7l" + durationSeconds + "s\u00a7c!");
    }

    public double consumeChadBonus(Player player) {
        UUID id = player.getUniqueId();
        Integer remaining = this.chadHitsRemaining.get(id);
        if (remaining == null || remaining <= 0) {
            return 0.0;
        }
        Long expiry = this.chadExpiry.get(id);
        if (expiry != null && player.getWorld().getFullTime() > expiry) {
            this.chadHitsRemaining.remove(id);
            this.chadExpiry.remove(id);
            return 0.0;
        }
        Integer n = remaining;
        remaining = remaining - 1;
        if (remaining <= 0) {
            this.chadHitsRemaining.remove(id);
            this.chadExpiry.remove(id);
            this.plugin.getAbilityManager().endAbilityDuration(player, "strength-chad");
        } else {
            this.chadHitsRemaining.put(id, remaining);
        }
        return this.plugin.getConfig().getDouble("abilities.strength-chad.bonus-damage", 7.0);
    }

    public void nullify(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "strength-nullify";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        int range = this.plugin.getConfig().getInt("abilities.strength-nullify.range", 20);
        LivingEntity target = this.getTargetEntity(player, range);
        if (target == null) {
            player.sendMessage("\u00a7cNo target found!");
            return;
        }
        if (target instanceof Player) {
            Player targetPlayer = (Player)target;
            if (this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                player.sendMessage("\u00a7cYou cannot nullify a trusted player!");
                return;
            }
        }
        final ArrayList<PotionEffect> stripped = new ArrayList<PotionEffect>();
        for (PotionEffect effect : target.getActivePotionEffects()) {
            if (effect.isAmbient() || !STRIPPABLE_EFFECTS.contains(effect.getType())) continue;
            stripped.add(effect);
        }
        int strippedCount = stripped.size();
        for (PotionEffect potionEffect : stripped) {
            target.removePotionEffect(potionEffect.getType());
        }
        int restoreSeconds = this.plugin.getConfig().getInt("abilities.strength-nullify.duration", 8);
        if (restoreSeconds > 0 && !stripped.isEmpty()) {
            final UUID uUID = target.getUniqueId();
            final int suppressionTicks = restoreSeconds * 20;
            new BukkitRunnable(){

                public void run() {
                    Entity e = Bukkit.getEntity((UUID)uUID);
                    if (!(e instanceof LivingEntity) || e.isDead()) {
                        return;
                    }
                    LivingEntity le = (LivingEntity)e;
                    for (PotionEffect effect : stripped) {
                        int remaining = effect.getDuration() - suppressionTicks;
                        if (remaining <= 0) continue;
                        le.addPotionEffect(new PotionEffect(effect.getType(), remaining, effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon()));
                    }
                }
            }.runTaskLater((Plugin)this.plugin, (long)suppressionTicks);
        }
        Particle.DustOptions dustOptions = new Particle.DustOptions(ParticleUtils.STRENGTH_RED, 1.5f);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 40, 0.5, 0.8, 0.5, 0.0, (Object)dustOptions, true);
        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.8, 0.5, 0.05);
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0.0, 1.0, 0.0), 10, 0.5, 0.5, 0.5);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 0.8f, 1.2f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 0.8f, 1.2f);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        String targetName = target instanceof Player ? ((Player)target).getName() : "target";
        String nullifyMsg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Nullify");
        if (nullifyMsg != null) {
            player.sendMessage(nullifyMsg + " \u00a77(Stripped " + strippedCount + " effects from " + targetName + ")");
        }
        if (target instanceof Player) {
            ((Player)target).sendMessage("\u00a7c\u00a7oYour potion effects have been nullified!");
        }
    }

    public void frailer(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "strength-frailer";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        int range = this.plugin.getConfig().getInt("abilities.strength-frailer.range", 15);
        LivingEntity target = this.getTargetEntity(player, range);
        if (target == null) {
            player.sendMessage("\u00a7cNo target found!");
            return;
        }
        if (target instanceof Player) {
            Player targetPlayer = (Player)target;
            if (this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                player.sendMessage("\u00a7cYou cannot use Frailer on a trusted player!");
                return;
            }
        }
        int weaknessDuration = this.plugin.getConfig().getInt("abilities.durations.strength-frailer-weakness", 20) * 20;
        int witherDuration = this.plugin.getConfig().getInt("abilities.durations.strength-frailer-wither", 40) * 20;
        int slownessDuration = this.plugin.getConfig().getInt("abilities.durations.strength-frailer-slowness", 20) * 20;
        int slownessLevel = this.plugin.getConfig().getInt("abilities.strength-frailer.slowness-level", 2);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, weaknessDuration, 0, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, witherDuration, 0, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessDuration, Math.max(0, slownessLevel - 1), false, true));
        Particle.DustOptions redDust = new Particle.DustOptions(ParticleUtils.STRENGTH_RED, 1.5f);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 40, 0.5, 0.8, 0.5, 0.0, (Object)redDust, true);
        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0.0, 1.0, 0.0), 25, 0.5, 0.8, 0.5, 0.05);
        target.getWorld().spawnParticle(Particle.CRIMSON_SPORE, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 0.8f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 0.8f);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "ability-activated", "ability", "Frailer Power");
        if (target instanceof Player) {
            ((Player)target).sendMessage("\u00a7c\u00a7oYou have been weakened by Frailer Power!");
        }
    }

    public void shadowStalker(final Player player) {
        SkullMeta skullMeta;
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "strength-shadow-stalker";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        if (this.trackingTargets.containsKey(player.getUniqueId())) {
            this.endTracking(player);
            player.sendMessage("\u00a7c\u00a7oStopped tracking.");
            return;
        }
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            player.sendMessage("\u00a7c\u00a7oHold a player head or an item owned by your target!");
            return;
        }
        UUID targetUUID = null;
        if (heldItem.getType() == Material.PLAYER_HEAD && heldItem.hasItemMeta() && (skullMeta = (SkullMeta)heldItem.getItemMeta()).getOwningPlayer() != null) {
            targetUUID = skullMeta.getOwningPlayer().getUniqueId();
        }
        if (targetUUID == null) {
            targetUUID = CustomItemManager.getOwner(heldItem);
        }
        if (targetUUID == null) {
            player.sendMessage("\u00a7c\u00a7oThis item is not a player head and has no owner!");
            return;
        }
        if (targetUUID.equals(player.getUniqueId())) {
            player.sendMessage("\u00a7c\u00a7oYou cannot track yourself!");
            return;
        }
        Player targetPlayer = Bukkit.getPlayer((UUID)targetUUID);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            player.sendMessage("\u00a7c\u00a7oTarget player is not online!");
            return;
        }
        if (heldItem.getAmount() > 1) {
            heldItem.setAmount(heldItem.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
        int durationSeconds = this.plugin.getConfig().getInt("abilities.strength-shadow-stalker.duration", 60);
        final int duration = durationSeconds * 20;
        final int invisRange = this.plugin.getConfig().getInt("abilities.strength-shadow-stalker.invisibility-max-range", 2500);
        final UUID trackerId = player.getUniqueId();
        final UUID trackedId = targetUUID;
        this.trackingTargets.put(trackerId, trackedId);
        if (this.plugin.getAchievementManager() != null) {
            Map<UUID, Integer> targetCounts = this.stalkCounts.computeIfAbsent(trackerId, k -> new HashMap<>());
            int count = targetCounts.getOrDefault(trackedId, 0) + 1;
            targetCounts.put(trackedId, count);
            if (count >= 17) {
                this.plugin.getAchievementManager().unlock(player, Achievement.ITS_RABBIT_SEASON);
            }
        }
        player.sendMessage("\u00a75\u00a7l\u2620 \u00a7dNow tracking \u00a7f" + targetPlayer.getName() + "\u00a7d.");
        targetPlayer.sendMessage("\u00a7c\u00a7l\u26a0 \u00a7c\u00a7oYou are being hunted...");
        targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_WARDEN_NEARBY_CLOSER, 0.7f, 1.5f);
        Particle.DustOptions redDust = new Particle.DustOptions(ParticleUtils.STRENGTH_RED, 1.5f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 40, 0.5, 0.5, 0.5, 0.0, (Object)redDust, true);
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SNIFF, 1.0f, 1.2f);
        BukkitTask trackTask = new BukkitRunnable(){
            int ticksElapsed = 0;

            public void run() {
                Player tracker = Bukkit.getPlayer((UUID)trackerId);
                Player tracked = Bukkit.getPlayer((UUID)trackedId);
                if (!(tracker != null && tracker.isOnline() && tracked != null && tracked.isOnline() && this.ticksElapsed < duration && StrengthAbilities.this.trackingTargets.containsKey(trackerId))) {
                    StrengthAbilities.this.endTracking(tracker != null ? tracker : player);
                    this.cancel();
                    return;
                }
                ++this.ticksElapsed;
                if (this.ticksElapsed % 20 == 0) {
                    boolean sameWorld = tracker.getWorld().equals((Object)tracked.getWorld());
                    if (sameWorld) {
                        double distance = tracker.getLocation().distance(tracked.getLocation());
                        boolean targetInvisible = tracked.hasPotionEffect(PotionEffectType.INVISIBILITY);
                        if (targetInvisible && distance > (double)invisRange) {
                            TextComponent msg = new TextComponent("\u00a7c\u00a7lTRACKING \u00a77\u2026 \u00a7c" + tracked.getName() + " \u00a77(invisible, too far)");
                            tracker.spigot().sendMessage(ChatMessageType.ACTION_BAR, (BaseComponent)msg);
                        } else {
                            String direction = StrengthAbilities.this.getDirectionArrow(tracker.getLocation(), tracked.getLocation());
                            TextComponent msg = new TextComponent("\u00a7c\u00a7lTRACKING \u00a77" + direction + " \u00a7c" + tracked.getName() + " \u00a77" + (int)distance + "m");
                            tracker.spigot().sendMessage(ChatMessageType.ACTION_BAR, (BaseComponent)msg);
                            for (UUID trustedId : StrengthAbilities.this.plugin.getTrustedPlayersManager().getTrustedPlayers(tracker)) {
                                Player trustedPlayer = Bukkit.getPlayer((UUID)trustedId);
                                if (trustedPlayer == null || !trustedPlayer.isOnline() || !trustedPlayer.getWorld().equals((Object)tracker.getWorld()) || !(trustedPlayer.getLocation().distance(tracker.getLocation()) < 50.0)) continue;
                                TextComponent trustedMsg = new TextComponent("\u00a78[\u00a7cTRACK\u00a78] \u00a77" + direction + " \u00a7c" + tracked.getName() + " \u00a77" + (int)distance + "m");
                                trustedPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, (BaseComponent)trustedMsg);
                            }
                        }
                        if (this.ticksElapsed % 40 == 0) {
                            Particle.DustOptions trailDust = new Particle.DustOptions(ParticleUtils.STRENGTH_RED, 0.6f);
                            tracker.getWorld().spawnParticle(Particle.DUST, tracker.getLocation().add(0.0, 2.2, 0.0), 3, 0.15, 0.1, 0.15, 0.0, (Object)trailDust, true);
                        }
                    } else {
                        TextComponent msg = new TextComponent("\u00a7c\u00a7lTRACKING \u00a77\u2026 \u00a7c" + tracked.getName() + " \u00a77(different dimension)");
                        tracker.spigot().sendMessage(ChatMessageType.ACTION_BAR, (BaseComponent)msg);
                    }
                }
                if (this.ticksElapsed % 4 == 0 && tracker.getWorld().equals((Object)tracked.getWorld())) {
                    StrengthAbilities.this.drawTrackerBeam(tracker, tracked.getLocation().add(0.0, 1.0, 0.0));
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        this.trackingTasks.put(trackerId, trackTask);
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, durationSeconds);
        player.sendMessage("\u00a7c\u00a7l\u2620 Shadow Stalker \u00a77Tracking \u00a7c" + targetPlayer.getName() + " \u00a77for " + durationSeconds + " seconds.");
    }

    private void drawTrackerBeam(Player tracker, Location target) {
        Location from = tracker.getEyeLocation();
        double distance = from.distance(target);
        if (distance < 0.1) {
            return;
        }
        Particle.DustOptions beamDust = new Particle.DustOptions(ParticleUtils.STRENGTH_RED, 0.9f);
        Vector step = target.toVector().subtract(from.toVector()).multiply(1.0 / distance).multiply(0.5);
        int points = (int)(distance / 0.5);
        Location point = from.clone();
        for (int i = 0; i < points; ++i) {
            point.add(step);
            tracker.spawnParticle(Particle.DUST, point, 1, 0.0, 0.0, 0.0, 0.0, (Object)beamDust, true);
        }
    }

    private void endTracking(Player player) {
        Player tracked;
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        UUID trackedId = this.trackingTargets.remove(uuid);
        BukkitTask task = this.trackingTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        if (trackedId == null && task == null) {
            return;
        }
        this.plugin.getAbilityManager().endAbilityDuration(player, "strength-shadow-stalker");
        if (player.isOnline()) {
            player.sendMessage("\u00a7c\u00a7oShadow Stalker tracking ended.");
        }
        if (trackedId != null && (tracked = Bukkit.getPlayer((UUID)trackedId)) != null && tracked.isOnline()) {
            tracked.sendMessage("\u00a7a\u00a7oYou are no longer being hunted.");
        }
    }

    public boolean isTrackingActive(Player player) {
        return this.trackingTargets.containsKey(player.getUniqueId());
    }

    private LivingEntity getTargetEntity(Player player, int range) {
        RayTraceResult result = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), (double)range, entity -> entity instanceof LivingEntity && entity != player);
        return result != null ? (LivingEntity)result.getHitEntity() : null;
    }

    private String getDirectionArrow(Location from, Location to) {
        double relative;
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        double angle = Math.atan2(direction.getZ(), direction.getX());
        double yaw = Math.toRadians(from.getYaw());
        for (relative = angle - yaw; relative > Math.PI; relative -= Math.PI * 2) {
        }
        while (relative < -Math.PI) {
            relative += Math.PI * 2;
        }
        if (relative > -0.7853981633974483 && relative <= 0.7853981633974483) {
            return "\u2192";
        }
        if (relative > 0.7853981633974483 && relative <= 2.356194490192345) {
            return "\u2193";
        }
        if (relative > -2.356194490192345 && relative <= -0.7853981633974483) {
            return "\u2191";
        }
        return "\u2190";
    }

    @Override
    public void cleanup(Player player) {
        this.endTracking(player);
        this.chadHitsRemaining.remove(player.getUniqueId());
        this.chadExpiry.remove(player.getUniqueId());
    }
}

