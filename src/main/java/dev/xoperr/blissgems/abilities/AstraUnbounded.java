package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.pedestal.PedestalManager;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * Astra - Unbounded: hit a player with the Astra gem and your spirit leaves your body to ride
 * theirs. You become a ghost locked to their view (spectator) for up to 10 s and they are haunted:
 * their gem is disabled for 15 s and they glow while you ride. Sneak to let go early. When it ends
 * you are put back exactly where you were. Rides are saved to disk, so a crash or a disconnect
 * mid-ride puts you back on your next login.
 */
public final class AstraUnbounded implements Listener {
    private static final String KEY = "astra-unbounded";
    private final BlissGems plugin;
    private final File file;
    private final YamlConfiguration saved;
    private final Map<UUID, Ride> rides = new HashMap<>();

    public AstraUnbounded(BlissGems plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "unbounded_rides.yml");
        this.saved = YamlConfiguration.loadConfiguration(this.file);
    }

    public boolean isRiding(Player p) {
        return this.rides.containsKey(p.getUniqueId());
    }

    private static void say(Player p, String msg) {
        p.sendMessage(PedestalManager.color(msg));
    }

    public void start(Player caster, Player victim) {
        if (this.isRiding(caster) || caster.getGameMode() == GameMode.SPECTATOR || victim.getGameMode() == GameMode.SPECTATOR) return;
        if (this.plugin.getTrustedPlayersManager() != null && this.plugin.getTrustedPlayersManager().isTrusted(caster, victim)) {
            say(caster, "&5🔮 &fYou cannot cast negative powers on allies!");
            return;
        }
        if (this.plugin.getAbilityManager().isOnCooldown(caster, KEY)) {
            say(caster, "&5🔮 &cYour &7Unbounded &cskill is on cooldown for &5" + this.plugin.getAbilityManager().getRemainingCooldown(caster, KEY) + "s");
            return;
        }
        if (!this.plugin.getAbilityManager().canUseAbility(caster, KEY)) return;
        int rideSeconds = this.plugin.getConfigManager().getAbilityDuration(KEY);
        if (rideSeconds <= 0) rideSeconds = 10;
        int hauntSeconds = this.plugin.getConfig().getInt("abilities.durations.astra-haunt", 15);

        Ride ride = new Ride(caster.getUniqueId(), victim.getUniqueId(), caster.getLocation().clone(), caster.getGameMode());
        this.rides.put(caster.getUniqueId(), ride);
        this.saved.set(caster.getUniqueId() + ".back", ride.back);
        this.saved.set(caster.getUniqueId() + ".mode", ride.mode.name());
        this.save();

        caster.getWorld().spawnParticle(Particle.SOUL, caster.getLocation().add(0, 1, 0), 25, 0.3, 0.6, 0.3, 0.03);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ALLAY_ITEM_THROWN, 1.0f, 0.5f);
        caster.setGameMode(GameMode.SPECTATOR);
        caster.setSpectatorTarget(victim);
        if (this.plugin.getGemLockManager() != null) this.plugin.getGemLockManager().lock(victim, hauntSeconds);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, rideSeconds * 20, 0, false, false, true));
        victim.playSound(victim.getLocation(), Sound.ENTITY_VEX_CHARGE, 1.0f, 0.6f);
        say(caster, "&5🔮 &bYou have used &7Unbounded &bskill on &a" + victim.getName() + " &7(sneak to let go)");
        say(caster, "&5🔮 &bYou haunted and disabled &e" + victim.getName() + "&b's gem for &e" + hauntSeconds + "s");
        say(victim, "&5🔮 &cSomething rides with you... your gem is haunted for &e" + hauntSeconds + "s");

        int endTick = Bukkit.getCurrentTick() + rideSeconds * 20;
        ride.task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            Player c = Bukkit.getPlayer(ride.caster);
            Player v = Bukkit.getPlayer(ride.victim);
            boolean over = c == null || !c.isOnline() || v == null || !v.isOnline() || v.isDead()
                || Bukkit.getCurrentTick() >= endTick || c.getGameMode() != GameMode.SPECTATOR;
            if (!over && c.getSpectatorTarget() != v) {
                // sneaking lets go of the target; changing worlds drops it too
                if (c.isSneaking() || c.getWorld() != v.getWorld()) over = true;
                else c.setSpectatorTarget(v);
            }
            if (over) this.end(ride.caster);
        }, 2L, 2L);
    }

    /** Puts the caster back in their body. */
    public void end(UUID casterId) {
        Ride ride = this.rides.remove(casterId);
        if (ride != null && ride.task != null) ride.task.cancel();
        Player c = Bukkit.getPlayer(casterId);
        if (c != null && c.isOnline()) {
            this.restore(c);
            if (ride != null) {
                this.plugin.getAbilityManager().useAbility(c, KEY);
                Player v = Bukkit.getPlayer(ride.victim);
                if (v != null) v.removePotionEffect(PotionEffectType.GLOWING);
            }
        }
    }

    private void restore(Player c) {
        String k = c.getUniqueId().toString();
        Location back = this.saved.getLocation(k + ".back");
        String mode = this.saved.getString(k + ".mode", "SURVIVAL");
        if (back == null) return;
        c.setSpectatorTarget(null);
        c.setVelocity(new Vector());
        if (back.isWorldLoaded()) c.teleport(back); else c.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        try {
            c.setGameMode(GameMode.valueOf(mode));
        } catch (IllegalArgumentException e) {
            c.setGameMode(GameMode.SURVIVAL);
        }
        c.setFallDistance(0);
        c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.getLocation().add(0, 1, 0), 30, 0.3, 0.6, 0.3, 0.05);
        c.playSound(c.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.7f);
        this.saved.set(k, null);
        this.save();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (this.rides.containsKey(id)) this.end(id);
        for (Ride r : new java.util.ArrayList<>(this.rides.values())) if (r.victim.equals(id)) this.end(r.caster);
    }

    /** A ride cut short by a crash is undone on the next login. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (this.saved.contains(p.getUniqueId().toString()) && !this.rides.containsKey(p.getUniqueId())) {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (p.isOnline()) this.restore(p);
            });
        }
    }

    public void shutdown() {
        for (UUID id : new java.util.ArrayList<>(this.rides.keySet())) this.end(id);
    }

    private void save() {
        try {
            this.saved.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save unbounded_rides.yml: " + e.getMessage());
        }
    }

    private static final class Ride {
        final UUID caster;
        final UUID victim;
        final Location back;
        final GameMode mode;
        BukkitTask task;

        Ride(UUID caster, UUID victim, Location back, GameMode mode) {
            this.caster = caster;
            this.victim = victim;
            this.back = back;
            this.mode = mode;
        }
    }
}
