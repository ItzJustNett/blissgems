/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.entity.Player
 *  org.bukkit.util.RayTraceResult
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public class GemLockManager {
    public static final String LOCK_GLYPH = "\ua42c";
    private final BlissGems plugin;
    private final Map<UUID, Long> lockedUntil = new ConcurrentHashMap<UUID, Long>();

    public GemLockManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    public boolean isLocked(Player player) {
        return player != null && this.isLocked(player.getUniqueId());
    }

    public boolean isLocked(UUID id) {
        Long until = this.lockedUntil.get(id);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() >= until) {
            this.lockedUntil.remove(id);
            return false;
        }
        return true;
    }

    public int getRemainingSeconds(UUID id) {
        Long until = this.lockedUntil.get(id);
        if (until == null) {
            return 0;
        }
        long ms = until - System.currentTimeMillis();
        return ms <= 0L ? 0 : (int)Math.ceil((double)ms / 1000.0);
    }

    public void lock(Player target, int seconds) {
        this.lockedUntil.put(target.getUniqueId(), System.currentTimeMillis() + (long)seconds * 1000L);
    }

    public void clear(UUID id) {
        this.lockedUntil.remove(id);
    }

    public void castGemLock(Player caster) {
        String key = "auratus-gemlock";
        if (this.plugin.getAbilityManager().isOnCooldown(caster, key)) {
            caster.sendMessage("\u00a76\ua42c \u00a77Gem Lock on cooldown: \u00a7c" + this.plugin.getAbilityManager().getRemainingCooldown(caster, key) + "s");
            return;
        }
        if (!this.plugin.getAbilityManager().canUseAbility(caster, key)) {
            return;
        }
        int range = this.plugin.getConfig().getInt("abilities.auratus-gemlock.range", 20);
        int duration = this.plugin.getConfig().getInt("abilities.auratus-gemlock.duration", 10);
        Player target = this.raycastPlayer(caster, range);
        if (target == null) {
            caster.sendMessage("\u00a7c\u00a7oNo player in your sights to lock!");
            return;
        }
        if (target.getUniqueId().equals(caster.getUniqueId())) {
            caster.sendMessage("\u00a7c\u00a7oYou can't lock your own gem!");
            return;
        }
        if (this.plugin.getTrustedPlayersManager() != null && this.plugin.getTrustedPlayersManager().isTrusted(caster, target)) {
            caster.sendMessage("\u00a7c\u00a7oYou can't lock a trusted player!");
            return;
        }
        if (!this.plugin.getGemManager().hasActiveGem(target)) {
            caster.sendMessage("\u00a7c\u00a7o" + target.getName() + " has no gem to lock!");
            return;
        }
        this.lock(target, duration);
        this.plugin.getAbilityManager().useAbility(caster, key);
        caster.sendMessage("\u00a76\u00a7oYou locked " + target.getName() + "'s gem for " + duration + "s!");
        target.sendMessage("\u00a76\u00a7l\ua42c \u00a7c\u00a7lYour gem has been LOCKED! \u00a77(" + duration + "s)");
        Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB((int)255, (int)200, (int)60), 1.6f);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 40, 0.5, 0.8, 0.5, 0.0, (Object)gold, true);
        target.getWorld().spawnParticle(Particle.WAX_ON, target.getLocation().add(0.0, 1.0, 0.0), 20, 0.5, 0.8, 0.5, 0.05);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.7f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.6f);
        caster.playSound(caster.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
    }

    private Player raycastPlayer(Player caster, int range) {
        RayTraceResult result = caster.getWorld().rayTraceEntities(caster.getEyeLocation(), caster.getEyeLocation().getDirection(), (double)range, 0.5, entity -> entity instanceof Player && !entity.equals((Object)caster));
        if (result != null && result.getHitEntity() instanceof Player) {
            return (Player)result.getHitEntity();
        }
        return null;
    }
}

