/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.entity.Display$Billboard
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.ItemDisplay
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.projectiles.ProjectileSource
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.util.Transformation
 *  org.bukkit.util.Vector
 *  org.joml.AxisAngle4f
 *  org.joml.Vector3f
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.managers.GoldGemManager;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class GoldHarvestCeremony
implements Listener {
    private static final String DISPLAY_TAG = "blissgems_ritual_display";
    private final BlissGems plugin;
    private final Map<UUID, Location> pinned = new HashMap<UUID, Location>();

    public GoldHarvestCeremony(BlissGems plugin) {
        this.plugin = plugin;
    }

    public boolean isInCeremony(Player player) {
        return this.pinned.containsKey(player.getUniqueId());
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onFatalBlow(EntityDamageByEntityEvent event) {
        Player victim;
        if (!this.plugin.getConfig().getBoolean("gold.harvest-ceremony.enabled", true)) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Player) || this.isInCeremony(victim = (Player)entity)) {
            return;
        }
        Player killer = this.resolveKiller(event.getDamager());
        if (killer == null || this.isInCeremony(killer)) {
            return;
        }
        if (!killer.getWorld().equals((Object)victim.getWorld())) {
            return;
        }
        if (victim.getHealth() - event.getFinalDamage() > 0.0) {
            return;
        }
        ItemStack mainHand = victim.getInventory().getItemInMainHand();
        ItemStack offHand = victim.getInventory().getItemInOffHand();
        boolean hasTotem = (mainHand != null && mainHand.getType() == Material.TOTEM_OF_UNDYING)
                || (offHand != null && offHand.getType() == Material.TOTEM_OF_UNDYING);
        if (hasTotem) {
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

    private Player resolveKiller(Entity damager) {
        Projectile projectile;
        ProjectileSource projectileSource;
        if (damager instanceof Player) {
            Player player = (Player)damager;
            return player;
        }
        if (damager instanceof Projectile && (projectileSource = (projectile = (Projectile)damager).getShooter()) instanceof Player) {
            Player shooter = (Player)projectileSource;
            return shooter;
        }
        return null;
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onCeremonyDamage(EntityDamageEvent event) {
        Player player;
        Entity entity = event.getEntity();
        if (entity instanceof Player && this.isInCeremony(player = (Player)entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.pinned.remove(event.getPlayer().getUniqueId());
    }

    private void begin(final Player killer, final Player victim, GoldGemManager.Harvest taken) {
        final int duration = Math.max(20, this.plugin.getConfig().getInt("gold.harvest-ceremony.duration-ticks", 80));
        final double riseHeight = this.plugin.getConfig().getDouble("gold.harvest-ceremony.rise-height", 3.0);
        final boolean tint = this.plugin.getConfig().getBoolean("gold.harvest-ceremony.screen-tint", true);
        final Color colour = this.plugin.getGoldGemManager().soulColour(taken.gemId());
        Location a = victim.getLocation();
        Location b = killer.getLocation();
        final Location stage = new Location(victim.getWorld(), (a.getX() + b.getX()) / 2.0, (a.getY() + b.getY()) / 2.0 + 1.2, (a.getZ() + b.getZ()) / 2.0);
        final ItemDisplay display = this.spawnGem(stage, taken);
        this.pin(killer);
        this.pin(victim);
        victim.getWorld().playSound(stage, Sound.BLOCK_BEACON_ACTIVATE, 1.6f, 0.5f);
        new BukkitRunnable(){
            int ticks = 0;

            public void run() {
                boolean bothHere;
                boolean bl = bothHere = killer.isOnline() && victim.isOnline() && GoldHarvestCeremony.this.isInCeremony(killer) && GoldHarvestCeremony.this.isInCeremony(victim);
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
                double progress = (double)this.ticks / (double)duration;
                Location gemAt = stage.clone().add(0.0, riseHeight * progress, 0.0);
                display.teleport(gemAt);
                display.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), new AxisAngle4f((float)Math.toRadians(this.ticks * 8), 0.0f, 1.0f, 0.0f), new Vector3f(1.0f, 1.0f, 1.0f), new AxisAngle4f(0.0f, 0.0f, 0.0f, 1.0f)));
                victim.getWorld().spawnParticle(Particle.DUST, gemAt, 12, 0.25, 0.25, 0.25, 0.0, (Object)new Particle.DustOptions(colour, 1.4f));
                GoldHarvestCeremony.this.stare(killer, gemAt, this.ticks);
                GoldHarvestCeremony.this.stare(victim, gemAt, this.ticks);
                if (tint) {
                    GoldHarvestCeremony.this.tint(killer, colour, progress);
                    GoldHarvestCeremony.this.tint(victim, colour, progress);
                }
                if (this.ticks % 8 == 0) {
                    float pitch = 0.6f + (float)progress;
                    victim.getWorld().playSound(gemAt, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, pitch);
                }
                ++this.ticks;
            }
        }.runTaskTimer((Plugin)this.plugin, 1L, 1L);
    }

    private ItemDisplay spawnGem(Location at, GoldGemManager.Harvest taken) {
        ItemDisplay display = (ItemDisplay)at.getWorld().spawn(at, ItemDisplay.class);
        ItemStack icon = CustomItemManager.getItemById(taken.gemId() + "_gem_t" + taken.tier(), 10);
        if (icon != null) {
            display.setItemStack(icon);
        }
        display.setBillboard(Display.Billboard.FIXED);
        display.setViewRange(100.0f);
        display.setGlowing(true);
        display.addScoreboardTag(DISPLAY_TAG);
        return display;
    }

    private void finish(Player killer, Player victim, Color colour) {
        GoldGemManager gold = this.plugin.getGoldGemManager();
        GoldGemManager.Harvest taken = gold.takeGem(victim, null);
        if (taken != null) {
            gold.absorb(killer, victim, taken);
        }
        victim.getWorld().spawnParticle(Particle.DUST, victim.getEyeLocation(), 60, 0.6, 0.8, 0.6, 0.0, (Object)new Particle.DustOptions(colour, 2.0f));
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 1.4f);
        victim.setHealth(Math.max(1.0, Math.min(victim.getHealth(), 2.0)));
        victim.sendMessage(org.bukkit.ChatColor.GOLD + "§l[GOLD GEM] " + org.bukkit.ChatColor.YELLOW + "Your gem has been severed and absorbed into the Gold Gem!");
    }

    private void pin(Player player) {
        this.pinned.put(player.getUniqueId(), player.getLocation().clone());
        int padding = 200;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, padding, 255, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, padding, 250, false, false));
    }

    private void release(Player player) {
        this.pinned.remove(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
    }

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
        look.setYaw(look.getYaw() + (float)(Math.sin((double)ticks / 7.0) * 7.0));
        look.setPitch(look.getPitch() + (float)(Math.sin((double)ticks / 11.0) * 4.0));
        player.teleport(look);
    }

    private void tint(Player player, Color colour, double progress) {
        int density = (int)(10.0 + 30.0 * progress);
        Location eye = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.4));
        player.spawnParticle(Particle.DUST, eye, density, 0.35, 0.35, 0.35, 0.0, (Object)new Particle.DustOptions(colour, 2.4f));
    }

    public void cleanup() {
        for (UUID id : new ArrayList<UUID>(this.pinned.keySet())) {
            Player player = this.plugin.getServer().getPlayer(id);
            if (player != null) {
                this.release(player);
                continue;
            }
            this.pinned.remove(id);
        }
    }
}

