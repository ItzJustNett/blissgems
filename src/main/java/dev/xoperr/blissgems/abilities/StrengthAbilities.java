package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Achievement;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StrengthAbilities {
    private final BlissGems plugin;

    // Shadow Stalker tracking state
    private final Map<UUID, UUID> trackingTargets = new HashMap<>();   // tracker -> tracked
    private final Map<UUID, BukkitTask> trackingTasks = new HashMap<>();
    // Achievement: tracks per-target stalk count (trackerUUID -> (targetUUID -> count))
    private final Map<UUID, Map<UUID, Integer>> stalkCounts = new HashMap<>();

    public StrengthAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    // ========================================================================
    // Right-click routing (for GemInteractListener)
    // ========================================================================

    public void onRightClick(Player player, int tier) {
        if (tier >= 2 && player.isSneaking()) {
            this.frailer(player);
        } else {
            this.nullify(player);
        }
    }

    // ========================================================================
    // NULLIFY — Tier 2 Primary
    // Strips ALL potion effects from target via raycast
    // ========================================================================

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

        // Skip trusted players
        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            if (this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                player.sendMessage("\u00a7cYou cannot nullify a trusted player!");
                return;
            }
        }

        // Strip ALL potion effects
        int strippedCount = 0;
        for (PotionEffect effect : target.getActivePotionEffects()) {
            target.removePotionEffect(effect.getType());
            strippedCount++;
        }

        // Particles on target — effects being "ripped away"
        Particle.DustOptions redDust = new Particle.DustOptions(ParticleUtils.STRENGTH_RED, 1.5f);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0),
            40, 0.5, 0.8, 0.5, 0.0, redDust, true);
        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1, 0),
            30, 0.5, 0.8, 0.5, 0.05);
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0),
            10, 0.5, 0.5, 0.5);

        // Sound
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 0.8f, 1.2f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 0.8f, 1.2f);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);

        String targetName = (target instanceof Player) ? ((Player) target).getName() : "target";
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Nullify")
            + " \u00a77(Stripped " + strippedCount + " effects from " + targetName + ")");

        if (target instanceof Player) {
            ((Player) target).sendMessage("\u00a7c\u00a7oYour potion effects have been nullified!");
        }
    }

    // ========================================================================
    // FRAILER — Tier 2 Secondary
    // Clears potions + applies Weakness I 20s + Wither I 40s
    // ========================================================================

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

        // Skip trusted players
        if (target instanceof Player) {
            Player targetPlayer = (Player) target;
            if (this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                player.sendMessage("\u00a7cYou cannot use Frailer on a trusted player!");
                return;
            }
        }

        // Strip ALL potion effects first
        for (PotionEffect effect : target.getActivePotionEffects()) {
            target.removePotionEffect(effect.getType());
        }

        // Apply Weakness I for 20 seconds and Wither I for 40 seconds
        int weaknessDuration = this.plugin.getConfig().getInt("abilities.durations.strength-frailer-weakness", 20) * 20;
        int witherDuration = this.plugin.getConfig().getInt("abilities.durations.strength-frailer-wither", 40) * 20;
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, weaknessDuration, 0, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, witherDuration, 0, false, true));

        // Particles
        Particle.DustOptions redDust = new Particle.DustOptions(ParticleUtils.STRENGTH_RED, 1.5f);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0),
            40, 0.5, 0.8, 0.5, 0.0, redDust, true);
        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0, 1, 0),
            25, 0.5, 0.8, 0.5, 0.05);
        target.getWorld().spawnParticle(Particle.CRIMSON_SPORE, target.getLocation().add(0, 1, 0),
            30, 0.5, 0.5, 0.5);

        // Sound
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 0.8f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 0.8f);

        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Frailer Power"));

        if (target instanceof Player) {
            ((Player) target).sendMessage("\u00a7c\u00a7oYou have been weakened by Frailer Power!");
        }
    }

    // ========================================================================
    // SHADOW STALKER — Tier 2 Tertiary
    // Consume player head or owned item to track a player
    // ========================================================================

    public void shadowStalker(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }

        String abilityKey = "strength-shadow-stalker";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        // Cancel existing tracking if active
        if (trackingTargets.containsKey(player.getUniqueId())) {
            endTracking(player);
            player.sendMessage("\u00a7c\u00a7oStopped tracking.");
            return;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            player.sendMessage("\u00a7c\u00a7oHold a player head or an item owned by another player!");
            return;
        }

        UUID targetUUID = null;

        // Check 1: Player head with skull meta
        if (heldItem.getType() == Material.PLAYER_HEAD && heldItem.hasItemMeta()) {
            SkullMeta skullMeta = (SkullMeta) heldItem.getItemMeta();
            if (skullMeta.getOwningPlayer() != null) {
                targetUUID = skullMeta.getOwningPlayer().getUniqueId();
            }
        }

        // Check 2: Item with PDC ownership data
        if (targetUUID == null) {
            targetUUID = this.plugin.getItemOwnershipManager().getItemOwner(heldItem);
        }

        if (targetUUID == null) {
            player.sendMessage("\u00a7c\u00a7oThis item has no trackable ownership! Use a player head or an item owned by another player.");
            return;
        }

        // Can't track yourself
        if (targetUUID.equals(player.getUniqueId())) {
            player.sendMessage("\u00a7c\u00a7oYou cannot track yourself!");
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(targetUUID);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            player.sendMessage("\u00a7c\u00a7oTarget player is not online!");
            return;
        }

        // Consume the item
        if (heldItem.getAmount() > 1) {
            heldItem.setAmount(heldItem.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // Start tracking
        int durationSeconds = this.plugin.getConfig().getInt("abilities.strength-shadow-stalker.duration", 60);
        int duration = durationSeconds * 20;
        int invisRange = this.plugin.getConfig().getInt("abilities.strength-shadow-stalker.invisibility-max-range", 2500);

        UUID trackerId = player.getUniqueId();
        UUID trackedId = targetUUID;
        trackingTargets.put(trackerId, trackedId);

        // Achievement: It's Rabbit Season (track same player 17 times)
        if (this.plugin.getAchievementManager() != null) {
            Map<UUID, Integer> targetCounts = stalkCounts.computeIfAbsent(trackerId, k -> new HashMap<>());
            int count = targetCounts.getOrDefault(trackedId, 0) + 1;
            targetCounts.put(trackedId, count);
            if (count >= 17) {
                this.plugin.getAchievementManager().unlock(player, Achievement.ITS_RABBIT_SEASON);
            }
        }

        // Notify victim
        targetPlayer.sendMessage("\u00a7c\u00a7l\u26a0 \u00a7c\u00a7oYou are being hunted...");
        targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_WARDEN_NEARBY_CLOSER, 0.7f, 1.5f);

        // Activation effects on tracker
        Particle.DustOptions redDust = new Particle.DustOptions(ParticleUtils.STRENGTH_RED, 1.5f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0),
            40, 0.5, 0.5, 0.5, 0.0, redDust, true);
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SNIFF, 1.0f, 1.2f);

        // Start tracking task
        BukkitTask trackTask = new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                Player tracker = Bukkit.getPlayer(trackerId);
                Player tracked = Bukkit.getPlayer(trackedId);

                if (tracker == null || !tracker.isOnline() ||
                    tracked == null || !tracked.isOnline() ||
                    ticksElapsed >= duration ||
                    !trackingTargets.containsKey(trackerId)) {
                    endTracking(tracker != null ? tracker : player);
                    this.cancel();
                    return;
                }

                ticksElapsed++;

                // Show directional indicator every 20 ticks (1 second)
                if (ticksElapsed % 20 == 0) {
                    boolean sameWorld = tracker.getWorld().equals(tracked.getWorld());

                    if (sameWorld) {
                        double distance = tracker.getLocation().distance(tracked.getLocation());

                        // Invisibility check: can only track invisible targets within max range
                        boolean targetInvisible = tracked.hasPotionEffect(PotionEffectType.INVISIBILITY);
                        if (targetInvisible && distance > invisRange) {
                            TextComponent msg = new TextComponent(
                                "\u00a7c\u00a7lTRACKING \u00a77\u2026 \u00a7c" + tracked.getName() + " \u00a77(invisible, too far)"
                            );
                            tracker.spigot().sendMessage(ChatMessageType.ACTION_BAR, msg);
                        } else {
                            String direction = getDirectionArrow(tracker.getLocation(), tracked.getLocation());
                            TextComponent msg = new TextComponent(
                                "\u00a7c\u00a7lTRACKING \u00a77" + direction + " \u00a7c" + tracked.getName() + " \u00a77" + (int) distance + "m"
                            );
                            tracker.spigot().sendMessage(ChatMessageType.ACTION_BAR, msg);

                            // Also show to trusted players of the tracker
                            for (UUID trustedId : plugin.getTrustedPlayersManager().getTrustedPlayers(tracker)) {
                                Player trustedPlayer = Bukkit.getPlayer(trustedId);
                                if (trustedPlayer != null && trustedPlayer.isOnline()
                                    && trustedPlayer.getWorld().equals(tracker.getWorld())
                                    && trustedPlayer.getLocation().distance(tracker.getLocation()) < 50) {
                                    TextComponent trustedMsg = new TextComponent(
                                        "\u00a78[\u00a7cTRACK\u00a78] \u00a77" + direction + " \u00a7c" + tracked.getName() + " \u00a77" + (int) distance + "m"
                                    );
                                    trustedPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, trustedMsg);
                                }
                            }
                        }

                        // Subtle red dust particles on tracker
                        if (ticksElapsed % 40 == 0) {
                            Particle.DustOptions trailDust = new Particle.DustOptions(ParticleUtils.STRENGTH_RED, 0.6f);
                            tracker.getWorld().spawnParticle(Particle.DUST,
                                tracker.getLocation().add(0, 2.2, 0),
                                3, 0.15, 0.1, 0.15, 0.0, trailDust, true);
                        }
                    } else {
                        TextComponent msg = new TextComponent(
                            "\u00a7c\u00a7lTRACKING \u00a77\u2026 \u00a7c" + tracked.getName() + " \u00a77(different dimension)"
                        );
                        tracker.spigot().sendMessage(ChatMessageType.ACTION_BAR, msg);
                    }
                }
            }
        }.runTaskTimer(this.plugin, 0L, 1L);

        trackingTasks.put(trackerId, trackTask);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);

        player.sendMessage("\u00a7c\u00a7l\u2620 Shadow Stalker \u00a77Tracking \u00a7c" + targetPlayer.getName()
            + " \u00a77for " + durationSeconds + " seconds.");
    }

    private void endTracking(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        UUID trackedId = trackingTargets.remove(uuid);
        BukkitTask task = trackingTasks.remove(uuid);
        if (task != null) task.cancel();

        if (player.isOnline()) {
            player.sendMessage("\u00a7c\u00a7oShadow Stalker tracking ended.");
        }

        // Notify the tracked player
        if (trackedId != null) {
            Player tracked = Bukkit.getPlayer(trackedId);
            if (tracked != null && tracked.isOnline()) {
                tracked.sendMessage("\u00a7a\u00a7oYou are no longer being hunted.");
            }
        }
    }

    /**
     * Check if the player has an active Shadow Stalker tracking HUD (used by CooldownDisplayManager
     * to avoid overwriting the TRACKING action bar display).
     */
    public boolean isTrackingActive(Player player) {
        return trackingTargets.containsKey(player.getUniqueId());
    }

    // ========================================================================
    // Utility methods
    // ========================================================================

    private LivingEntity getTargetEntity(Player player, int range) {
        var result = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            (double) range,
            entity -> entity instanceof LivingEntity && entity != player
        );
        return result != null ? (LivingEntity) result.getHitEntity() : null;
    }

    private String getDirectionArrow(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        double angle = Math.atan2(direction.getZ(), direction.getX());
        double yaw = Math.toRadians(from.getYaw());

        double relative = angle - yaw;
        // Normalize to -PI to PI
        while (relative > Math.PI) relative -= 2 * Math.PI;
        while (relative < -Math.PI) relative += 2 * Math.PI;

        if (relative > -Math.PI / 4 && relative <= Math.PI / 4) return "\u2192"; // right
        if (relative > Math.PI / 4 && relative <= 3 * Math.PI / 4) return "\u2193"; // behind
        if (relative > -3 * Math.PI / 4 && relative <= -Math.PI / 4) return "\u2191"; // ahead
        return "\u2190"; // left
    }

    /**
     * Cleanup when player leaves.
     */
    public void cleanup(Player player) {
        endTracking(player);
    }
}
