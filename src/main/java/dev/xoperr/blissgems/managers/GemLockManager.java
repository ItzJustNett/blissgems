package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players currently have their gem "locked" (by Auratus's Gem Lock ability),
 * and implements the ability itself.
 *
 * A locked player can't use ANY gem ability and gets no passives for the duration; their
 * action bar shows the lock icon instead of cooldowns (see CooldownDisplayManager).
 * Lock state is in-memory and clears on logout.
 *
 * The lock glyph (from the Oraxen pack) — same one used for the action-bar indicator.
 */
public class GemLockManager {

    /** Oraxen pack glyph "lock" (gems/icons/lock), char U+A42C. */
    public static final String LOCK_GLYPH = "ꐬ";

    private final BlissGems plugin;
    private final Map<UUID, Long> lockedUntil = new ConcurrentHashMap<>();

    public GemLockManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    public boolean isLocked(Player player) {
        return player != null && isLocked(player.getUniqueId());
    }

    public boolean isLocked(UUID id) {
        Long until = lockedUntil.get(id);
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            lockedUntil.remove(id);
            return false;
        }
        return true;
    }

    public int getRemainingSeconds(UUID id) {
        Long until = lockedUntil.get(id);
        if (until == null) return 0;
        long ms = until - System.currentTimeMillis();
        return ms <= 0 ? 0 : (int) Math.ceil(ms / 1000.0);
    }

    public void lock(Player target, int seconds) {
        lockedUntil.put(target.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
    }

    public void clear(UUID id) {
        lockedUntil.remove(id);
    }

    /**
     * Auratus "Gem Lock" (Tier 2, F / tertiary): raycast a player in front of the caster and
     * lock their gem for the configured duration. Energy + tier were already checked upstream.
     */
    public void castGemLock(Player caster) {
        String key = "auratus-gemlock";
        // Explicit cooldown feedback — Auratus's action bar is drawn by MythicStatusBar,
        // which won't surface this ability's cooldown, so canUseAbility's silent path isn't enough.
        if (plugin.getAbilityManager().isOnCooldown(caster, key)) {
            caster.sendMessage("§6" + LOCK_GLYPH + " §7Gem Lock on cooldown: §c"
                + plugin.getAbilityManager().getRemainingCooldown(caster, key) + "s");
            return;
        }
        if (!plugin.getAbilityManager().canUseAbility(caster, key)) {
            return; // energy / region / void checks (already messaged)
        }

        int range = plugin.getConfig().getInt("abilities.auratus-gemlock.range", 20);
        int duration = plugin.getConfig().getInt("abilities.auratus-gemlock.duration", 10);

        Player target = raycastPlayer(caster, range);
        if (target == null) {
            caster.sendMessage("§c§oNo player in your sights to lock!");
            return;
        }
        if (target.getUniqueId().equals(caster.getUniqueId())) {
            caster.sendMessage("§c§oYou can't lock your own gem!");
            return;
        }
        if (plugin.getTrustedPlayersManager() != null
                && plugin.getTrustedPlayersManager().isTrusted(caster, target)) {
            caster.sendMessage("§c§oYou can't lock a trusted player!");
            return;
        }
        if (!plugin.getGemManager().hasActiveGem(target)) {
            caster.sendMessage("§c§o" + target.getName() + " has no gem to lock!");
            return;
        }

        lock(target, duration);
        plugin.getAbilityManager().useAbility(caster, key);

        // Feedback
        caster.sendMessage("§6§oYou locked " + target.getName() + "'s gem for " + duration + "s!");
        target.sendMessage("§6§l" + LOCK_GLYPH + " §c§lYour gem has been LOCKED! §7(" + duration + "s)");

        // Effects on the target
        Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(255, 200, 60), 1.6f);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1.0, 0), 40, 0.5, 0.8, 0.5, 0.0, gold, true);
        target.getWorld().spawnParticle(Particle.WAX_ON, target.getLocation().add(0, 1.0, 0), 20, 0.5, 0.8, 0.5, 0.05);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.7f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.6f);
        caster.playSound(caster.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
    }

    private Player raycastPlayer(Player caster, int range) {
        RayTraceResult result = caster.getWorld().rayTraceEntities(
            caster.getEyeLocation(),
            caster.getEyeLocation().getDirection(),
            range,
            0.5,
            entity -> entity instanceof Player && !entity.equals(caster));
        if (result != null && result.getHitEntity() instanceof Player) {
            return (Player) result.getHitEntity();
        }
        return null;
    }
}
