package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The moment a Gold Gem takes a soul.
 *
 * The blow that would have killed the victim is caught before it lands. Both players are
 * pinned where they stand, the victim's gem is torn loose and rises between them turning in
 * the air, and both cameras are dragged onto it while the screen washes over in that gem's
 * colour. When the gem finishes rising it goes into the Gold Gem and the victim dies for
 * real, with the kill still credited to the holder.
 *
 * Nothing here decides *whether* a gem can be taken - that is {@link GoldGemManager#canHarvest}
 * - and the ordinary death path still harvests kills the ceremony never saw (a fall finishing
 * the fight, /kill, or the ceremony switched off in config).
 */
public class GoldHarvestCeremony implements Listener {

    /** Scoreboard tag on the rising gem, so an orphaned display can be swept like a ritual's. */
    private static final String DISPLAY_TAG = GemRitualManager.RITUAL_TAG;

    private final BlissGems plugin;

    /** Players currently held in a ceremony, mapped to the spot they are pinned to. */
    private final Map<UUID, Location> pinned = new HashMap<>();

    public GoldHarvestCeremony(BlissGems plugin) {
        this.plugin = plugin;
    }

    public boolean isInCeremony(Player player) {
        return this.pinned.containsKey(player.getUniqueId());
    }

    // ========================================================================
    // Interception
    // ========================================================================

    /**
     * Catch the fatal blow. Runs at HIGHEST so every other plugin has already had its say on
     * the damage number - the health check below has to be made against the damage that would
     * really have been dealt.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFatalBlow(EntityDamageByEntityEvent event) {
        if (!this.plugin.getConfig().getBoolean("gold.harvest-ceremony.enabled", true)) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim) || this.isInCeremony(victim)) {
            return;
        }
        Player killer = this.resolveKiller(event.getDamager());
        if (killer == null || this.isInCeremony(killer)) {
            return;
        }
        // The gem rises between the two of them, which only means anything if they share a world.
        if (!killer.getWorld().equals(victim.getWorld())) {
            return;
        }
        // Only the blow that would have finished them off.
        if (victim.getHealth() - event.getFinalDamage() > 0.0) {
            return;
        }
        GoldGemManager gold = this.plugin.getGoldGemManager();
        if (gold == null || !gold.canHarvest(killer, victim)) {
            return;
        }
        GoldGemManager.Harvest taken = gold.peekGem(victim);
        if (taken == null) {
            return;
        }
        event.setCancelled(true);
        this.begin(killer, victim, taken);
    }

    /** The player behind a hit, whether they swung it themselves or shot it. */
    private Player resolveKiller(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    /** Nothing touches either player while the ceremony runs - not even the world. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCeremonyDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && this.isInCeremony(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * A ceremony participant who logs out is released immediately: the gem stays with whoever
     * still has it, so a disconnect can't be used to keep a gem that was about to be taken
     * *or* to hand the killer a soul they never finished taking.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.pinned.remove(event.getPlayer().getUniqueId());
    }

    // ========================================================================
    // The ceremony
    // ========================================================================

    private void begin(Player killer, Player victim, GoldGemManager.Harvest taken) {
        int duration = Math.max(20, this.plugin.getConfig().getInt("gold.harvest-ceremony.duration-ticks", 80));
        double riseHeight = this.plugin.getConfig().getDouble("gold.harvest-ceremony.rise-height", 3.0);
        boolean tint = this.plugin.getConfig().getBoolean("gold.harvest-ceremony.screen-tint", true);
        Color colour = this.plugin.getGoldGemManager().soulColour(taken.gemId());

        // The gem rises from the midpoint, so it reads as being pulled out from between them.
        Location a = victim.getLocation();
        Location b = killer.getLocation();
        Location stage = new Location(victim.getWorld(),
            (a.getX() + b.getX()) / 2.0,
            (a.getY() + b.getY()) / 2.0 + 1.2,
            (a.getZ() + b.getZ()) / 2.0);

        ItemDisplay display = this.spawnGem(stage, taken);
        this.pin(killer);
        this.pin(victim);

        victim.getWorld().playSound(stage, Sound.BLOCK_BEACON_ACTIVATE, 1.6F, 0.5F);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                boolean bothHere = killer.isOnline() && victim.isOnline()
                    && GoldHarvestCeremony.this.isInCeremony(killer)
                    && GoldHarvestCeremony.this.isInCeremony(victim);
                if (!bothHere || this.ticks >= duration) {
                    this.cancel();
                    if (display.isValid()) {
                        display.remove();
                    }
                    GoldHarvestCeremony.this.release(killer);
                    GoldHarvestCeremony.this.release(victim);
                    if (bothHere) {
                        GoldHarvestCeremony.this.finish(killer, victim, colour);
                    }
                    return;
                }

                double progress = this.ticks / (double) duration;
                Location gemAt = stage.clone().add(0.0, riseHeight * progress, 0.0);
                display.teleport(gemAt);
                display.setTransformation(new org.bukkit.util.Transformation(
                    new Vector3f(0.0F, 0.0F, 0.0F),
                    new AxisAngle4f((float) Math.toRadians(this.ticks * 8), 0.0F, 1.0F, 0.0F),
                    new Vector3f(1.0F, 1.0F, 1.0F),
                    new AxisAngle4f(0.0F, 0.0F, 0.0F, 1.0F)));

                // The rising gem trails its own colour, and both players are dragged onto it.
                victim.getWorld().spawnParticle(Particle.DUST, gemAt, 12, 0.25, 0.25, 0.25, 0.0,
                    new Particle.DustOptions(colour, 1.4F));
                GoldHarvestCeremony.this.stare(killer, gemAt, this.ticks);
                GoldHarvestCeremony.this.stare(victim, gemAt, this.ticks);
                if (tint) {
                    GoldHarvestCeremony.this.tint(killer, colour, progress);
                    GoldHarvestCeremony.this.tint(victim, colour, progress);
                }
                if (this.ticks % 8 == 0) {
                    float pitch = 0.6F + (float) progress;
                    victim.getWorld().playSound(gemAt, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2F, pitch);
                }
                this.ticks++;
            }
        }.runTaskTimer((Plugin) this.plugin, 1L, 1L);
    }

    /** Spawn the gem that hangs in the air between the two players. */
    private ItemDisplay spawnGem(Location at, GoldGemManager.Harvest taken) {
        ItemDisplay display = at.getWorld().spawn(at, ItemDisplay.class);
        ItemStack icon = CustomItemManager.getItemById(taken.gemId() + "_gem_t" + taken.tier(), 10);
        if (icon != null) {
            display.setItemStack(icon);
        }
        display.setBillboard(Display.Billboard.FIXED);
        display.setViewRange(100.0F);
        display.setGlowing(true);
        display.addScoreboardTag(DISPLAY_TAG);
        return display;
    }

    /**
     * Take the gem, hand it to the Gold Gem, and let the death that was postponed happen.
     * The killing damage is dealt by the holder so the kill is still theirs - and because the
     * gem is gone by then, {@link #onFatalBlow} finds nothing to harvest and lets it through.
     */
    private void finish(Player killer, Player victim, Color colour) {
        GoldGemManager gold = this.plugin.getGoldGemManager();
        GoldGemManager.Harvest taken = gold.takeGem(victim, null);
        if (taken != null) {
            gold.absorb(killer, victim, taken);
        }
        victim.getWorld().spawnParticle(Particle.DUST, victim.getEyeLocation(), 60, 0.6, 0.8, 0.6, 0.0,
            new Particle.DustOptions(colour, 2.0F));
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2F, 1.4F);
        victim.damage(victim.getHealth() + 10.0, killer);
    }

    // ========================================================================
    // Holding a player still
    // ========================================================================

    /**
     * Pin a player where they stand. The per-tick camera teleport does the actual holding -
     * the potion effects are there so the client stops trying to walk in the first place and
     * the hold doesn't read as lag.
     */
    private void pin(Player player) {
        this.pinned.put(player.getUniqueId(), player.getLocation().clone());
        int padding = 200;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, padding, 255, false, false));
        // A wrapped-negative jump boost is the standard way to stop a player leaving the ground.
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, padding, 250, false, false));
    }

    private void release(Player player) {
        this.pinned.remove(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
    }

    /**
     * Hold the player on their pinned spot and turn their head onto the gem, with a slow sway
     * on top so the shot drifts rather than locking rigidly.
     */
    private void stare(Player player, Location target, int ticks) {
        Location anchor = this.pinned.get(player.getUniqueId());
        if (anchor == null) {
            return;
        }
        Vector toGem = target.toVector().subtract(player.getEyeLocation().toVector());
        if (toGem.lengthSquared() < 1.0E-4) {
            return;
        }
        Location look = anchor.clone().setDirection(toGem);
        look.setYaw(look.getYaw() + (float) (Math.sin(ticks / 7.0) * 7.0));
        look.setPitch(look.getPitch() + (float) (Math.sin(ticks / 11.0) * 4.0));
        player.teleport(look);
    }

    /**
     * Wash the player's screen in the gem's colour. There is no server-side screen overlay, so
     * this is a dense cloud of that colour packed right against the eye - close enough that it
     * fills the view rather than reading as particles in the world. Only this player is sent
     * it, so bystanders see the gem rise without the colour flooding their screen too.
     */
    private void tint(Player player, Color colour, double progress) {
        int density = (int) (10 + 30 * progress);
        Location eye = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.4));
        player.spawnParticle(Particle.DUST, eye, density, 0.35, 0.35, 0.35, 0.0,
            new Particle.DustOptions(colour, 2.4F));
    }

    /** Release everyone still held, e.g. on plugin shutdown. */
    public void cleanup() {
        for (UUID id : new java.util.ArrayList<>(this.pinned.keySet())) {
            Player player = this.plugin.getServer().getPlayer(id);
            if (player != null) {
                this.release(player);
            } else {
                this.pinned.remove(id);
            }
        }
    }
}
