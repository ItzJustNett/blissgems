/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Server
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.entity.Display$Billboard
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.ItemDisplay
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.util.Transformation
 *  org.bukkit.util.Vector
 *  org.joml.AxisAngle4f
 *  org.joml.Vector3f
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.managers.GemManager;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class GemRitualManager {
    private final BlissGems plugin;
    public static final String RITUAL_TAG = "blissgems_ritual_display";

    public GemRitualManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    public static int sweepWorld(World world) {
        int removed = 0;
        for (Entity e : world.getEntitiesByClasses(new Class[]{ItemDisplay.class})) {
            if (!e.getScoreboardTags().contains(RITUAL_TAG)) continue;
            e.remove();
            ++removed;
        }
        return removed;
    }

    public static int sweepAll(Server server) {
        int removed = 0;
        for (World world : server.getWorlds()) {
            removed += GemRitualManager.sweepWorld(world);
        }
        return removed;
    }

    public void performGemRitual(Player player, GemType gemType, boolean isFirstGem, int tier) {
        this.performGemRitual(player, gemType != null ? gemType.getId() : null, isFirstGem, tier);
    }

    public void performGemRitual(final Player player, final String gemId, boolean isFirstGem, int tier) {
        if (!isFirstGem && this.plugin.getAchievementManager() != null) {
            this.plugin.getAchievementManager().unlock(player, Achievement.REAWAKENING);
        }
        final Location loc = player.getLocation().clone();
        final Color gemColor = this.getGemColor(gemId);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 300, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 1, false, false));
        final ArrayList<ItemDisplay> orbitingGems = new ArrayList<ItemDisplay>();
        List<String> allGems = this.getRitualGemIds(tier);
        Location playerLoc = player.getLocation();
        for (int i = 0; i < allGems.size(); ++i) {
            String gemItemId = this.getGemItemId(allGems.get(i), tier);
            ItemStack gemItem = CustomItemManager.getItemById(gemItemId, 10);
            if (gemItem == null) continue;
            double angleOffset = (double)i / (double)allGems.size() * 2.0 * Math.PI;
            double radius = 2.5;
            double height = 1.5;
            double x = Math.cos(angleOffset) * radius;
            double z = Math.sin(angleOffset) * radius;
            Location gemLoc = playerLoc.clone().add(x, height, z);
            ItemDisplay itemDisplay = (ItemDisplay)player.getWorld().spawn(gemLoc, ItemDisplay.class);
            itemDisplay.setItemStack(gemItem);
            itemDisplay.setBillboard(Display.Billboard.FIXED);
            itemDisplay.setViewRange(100.0f);
            itemDisplay.setGlowing(true);
            itemDisplay.addScoreboardTag(RITUAL_TAG);
            orbitingGems.add(itemDisplay);
        }
        new BukkitRunnable(){
            int ticks = 0;
            final int maxTicks = 80;

            public void run() {
                if (!player.isOnline() || this.ticks >= 80) {
                    for (ItemDisplay gem : orbitingGems) {
                        if (gem == null || !gem.isValid()) continue;
                        gem.remove();
                    }
                    orbitingGems.clear();
                    this.cancel();
                    return;
                }
                for (int i = 0; i < orbitingGems.size(); ++i) {
                    ItemDisplay gem = (ItemDisplay)orbitingGems.get(i);
                    if (gem == null || !gem.isValid()) continue;
                    double angleOffset = (double)i / (double)orbitingGems.size() * 2.0 * Math.PI;
                    double angle = (double)this.ticks / 20.0 * Math.PI + angleOffset;
                    double radius = 2.5;
                    double height = 1.5 + Math.sin((double)this.ticks / 10.0) * 0.3;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location newLoc = player.getLocation().clone().add(x, height, z);
                    gem.teleport(newLoc);
                    float rotation = this.ticks * 5 % 360;
                    Transformation transform = new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), new AxisAngle4f((float)Math.toRadians(rotation), 0.0f, 1.0f, 0.0f), new Vector3f(0.7f, 0.7f, 0.7f), new AxisAngle4f(0.0f, 0.0f, 0.0f, 1.0f));
                    gem.setTransformation(transform);
                }
                if (this.ticks % 10 == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.0f + (float)this.ticks / 80.0f);
                }
                ++this.ticks;
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            final Location playerCenter = player.getLocation().add(0.0, 1.5, 0.0);
            final ArrayList<ItemDisplay> selectionGems = new ArrayList<ItemDisplay>();
            final ArrayList<String> selectionIds = new ArrayList<String>();
            for (int i = 0; i < allGems.size(); ++i) {
                String gemItemId = this.getGemItemId((String)allGems.get(i), tier);
                ItemStack gemItem = CustomItemManager.getItemById(gemItemId, 10);
                if (gemItem == null) continue;
                double angleOffset = (double)i / (double)allGems.size() * 2.0 * Math.PI;
                double startAngle = Math.PI * 4 + angleOffset;
                double radius = 2.5;
                double height = 1.5;
                double x = Math.cos(startAngle) * radius;
                double z = Math.sin(startAngle) * radius;
                Location startLoc = player.getLocation().clone().add(x, height, z);
                ItemDisplay itemDisplay = (ItemDisplay)player.getWorld().spawn(startLoc, ItemDisplay.class);
                itemDisplay.setItemStack(gemItem);
                itemDisplay.setBillboard(Display.Billboard.FIXED);
                itemDisplay.setViewRange(100.0f);
                itemDisplay.setGlowing(true);
                itemDisplay.addScoreboardTag(RITUAL_TAG);
                selectionGems.add(itemDisplay);
                selectionIds.add((String)allGems.get(i));
            }
            new BukkitRunnable(){
                int ticks = 0;
                final int maxTicks = 40;

                public void run() {
                    if (!player.isOnline() || this.ticks >= 40) {
                        for (ItemDisplay gem : selectionGems) {
                            if (gem == null || !gem.isValid()) continue;
                            gem.remove();
                        }
                        selectionGems.clear();
                        this.cancel();
                        return;
                    }
                    double progress = (double)this.ticks / 40.0;
                    for (int i = 0; i < selectionGems.size(); ++i) {
                        ItemDisplay gem = (ItemDisplay)selectionGems.get(i);
                        if (gem == null || !gem.isValid()) continue;
                        String currentGem = (String)selectionIds.get(i);
                        double angleOffset = (double)i / (double)selectionGems.size() * 2.0 * Math.PI;
                        double startAngle = Math.PI * 4 + angleOffset;
                        double startRadius = 2.5;
                        double startHeight = 1.5;
                        Location startLoc = player.getLocation().add(Math.cos(startAngle) * startRadius, startHeight, Math.sin(startAngle) * startRadius);
                        boolean isWinner = currentGem.equals(gemId);
                        Location targetLoc = isWinner ? playerCenter.clone() : startLoc.clone().add(0.0, -3.0, 0.0);
                        Location currentLoc = startLoc.clone().add((targetLoc.getX() - startLoc.getX()) * progress, (targetLoc.getY() - startLoc.getY()) * progress, (targetLoc.getZ() - startLoc.getZ()) * progress);
                        gem.teleport(currentLoc);
                        float rotation = isWinner ? (float)(this.ticks * 15 % 360) : (float)(this.ticks * 3 % 360);
                        Transformation transform = new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), new AxisAngle4f((float)Math.toRadians(rotation), 0.0f, 1.0f, 0.0f), isWinner ? new Vector3f(1.0f, 1.0f, 1.0f) : new Vector3f(0.7f, 0.7f, 0.7f), new AxisAngle4f(0.0f, 0.0f, 0.0f, 1.0f));
                        gem.setTransformation(transform);
                        if (!isWinner) continue;
                        Color color = GemRitualManager.this.getGemColor(currentGem);
                        player.getWorld().spawnParticle(Particle.FIREWORK, currentLoc, 3, 0.1, 0.1, 0.1, 0.05);
                        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, currentLoc, 2, 0.1, 0.1, 0.1, 0.01);
                    }
                    if (this.ticks % 5 == 0) {
                        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.7f, 1.0f + (float)progress);
                    }
                    ++this.ticks;
                }
            }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
            this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.8f);
            }, 40L);
        }, 80L);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> new BukkitRunnable(){
            int ticks = 0;

            public void run() {
                if (!player.isOnline() || this.ticks >= 20) {
                    this.cancel();
                    return;
                }
                double radius = (double)this.ticks / 20.0 * 5.0;
                for (int i = 0; i < 32; ++i) {
                    double angle = (double)i / 32.0 * 2.0 * Math.PI;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location particleLoc = loc.clone().add(x, 0.1, z);
                    Particle.DustOptions dust = new Particle.DustOptions(gemColor, 1.5f);
                    player.getWorld().spawnParticle(Particle.DUST, particleLoc, 3, 0.1, 0.1, 0.1, 0.0, (Object)dust, true);
                    player.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0.0, 0.0, 0.0, 0.01);
                }
                if (this.ticks % 5 == 0) {
                    player.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.5f);
                }
                ++this.ticks;
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L), 120L);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> new BukkitRunnable(){
            int ticks = 0;
            final int maxTicks = 40;

            public void run() {
                if (!player.isOnline() || this.ticks >= 40) {
                    this.cancel();
                    return;
                }
                double progress = (double)this.ticks / 40.0;
                double height = progress * 5.0;
                for (int spiral = 0; spiral < 3; ++spiral) {
                    double spiralOffset = (double)spiral / 3.0 * 2.0 * Math.PI;
                    double angle = progress * 6.0 * Math.PI + spiralOffset;
                    double radius = 1.5 * (1.0 - progress * 0.5);
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location particleLoc = loc.clone().add(x, height, z);
                    Particle.DustOptions dust = new Particle.DustOptions(gemColor, 1.8f);
                    player.getWorld().spawnParticle(Particle.DUST, particleLoc, 5, 0.1, 0.1, 0.1, 0.0, (Object)dust, true);
                    player.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 8, 0.2, 0.2, 0.2, 0.5);
                    player.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 2, 0.1, 0.1, 0.1, 0.02);
                }
                for (double y = 0.0; y <= height; y += 0.3) {
                    Particle.DustOptions pillarDust = new Particle.DustOptions(gemColor, 0.8f);
                    player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0.0, y, 0.0), 2, 0.15, 0.1, 0.15, 0.0, (Object)pillarDust, true);
                }
                if (this.ticks % 10 == 0) {
                    player.playSound(loc, Sound.BLOCK_BELL_USE, 0.7f, 1.0f + (float)progress);
                    player.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.5f);
                }
                ++this.ticks;
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L), 140L);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            Location center = loc.clone().add(0.0, 5.0, 0.0);
            Particle.DustOptions explosionDust = new Particle.DustOptions(gemColor, 2.5f);
            player.getWorld().spawnParticle(Particle.DUST, center, 200, 1.5, 1.5, 1.5, 0.0, (Object)explosionDust, true);
            player.getWorld().spawnParticle(Particle.FIREWORK, center, 100, 1.0, 1.0, 1.0, 0.2);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center, 80, 1.2, 1.2, 1.2, 0.1);
            player.getWorld().spawnParticle(Particle.END_ROD, center, 60, 1.5, 1.5, 1.5, 0.15);
            player.getWorld().spawnParticle(Particle.ENCHANT, center, 150, 2.0, 2.0, 2.0, 1.0);
            for (double y = 0.0; y <= 10.0; y += 0.2) {
                Particle.DustOptions beamDust = new Particle.DustOptions(gemColor, 1.5f);
                player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0.0, y, 0.0), 8, 0.2, 0.1, 0.2, 0.0, (Object)beamDust, true);
                player.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0.0, y, 0.0), 3, 0.15, 0.1, 0.15, 0.02);
            }
            player.playSound(loc, Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
            player.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 2.0f);
            player.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
            player.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 1.5f);
            new BukkitRunnable(){
                int ticks = 0;

                public void run() {
                    if (!player.isOnline() || this.ticks >= 20) {
                        this.cancel();
                        return;
                    }
                    for (int i = 0; i < 10; ++i) {
                        double randomX = (Math.random() - 0.5) * 3.0;
                        double randomZ = (Math.random() - 0.5) * 3.0;
                        double heightOffset = 5.0 - (double)this.ticks / 20.0 * 4.5;
                        Location convergeStart = loc.clone().add(randomX, heightOffset, randomZ);
                        Location playerLoc = player.getLocation().add(0.0, 1.0, 0.0);
                        Particle.DustOptions convergeDust = new Particle.DustOptions(gemColor, 1.2f);
                        player.getWorld().spawnParticle(Particle.DUST, convergeStart, 1, 0.0, 0.0, 0.0, 0.0, (Object)convergeDust, true);
                        player.getWorld().spawnParticle(Particle.END_ROD, convergeStart, 1, 0.0, 0.0, 0.0, 0.01);
                    }
                    ++this.ticks;
                }
            }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        }, 180L);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            Location playerCenter = player.getLocation().add(0.0, 1.0, 0.0);
            Particle.DustOptions finalDust = new Particle.DustOptions(gemColor, 2.0f);
            player.getWorld().spawnParticle(Particle.DUST, playerCenter, 150, 0.8, 1.0, 0.8, 0.0, (Object)finalDust, true);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, playerCenter, 50, 0.5, 0.8, 0.5, 0.1);
            player.getWorld().spawnParticle(Particle.ENCHANT, playerCenter, 100, 0.6, 1.0, 0.6, 0.8);
            player.getWorld().spawnParticle(Particle.FIREWORK, playerCenter, 30, 0.5, 0.5, 0.5, 0.1);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, playerCenter, 40, 0.6, 0.8, 0.6, 0.0);
            for (int i = 0; i < 360; i += 10) {
                double angle = Math.toRadians(i);
                double x = Math.cos(angle) * 2.0;
                double z = Math.sin(angle) * 2.0;
                Particle.DustOptions burstDust = new Particle.DustOptions(gemColor, 1.5f);
                player.getWorld().spawnParticle(Particle.DUST, playerCenter.clone().add(x, 0.0, z), 5, 0.1, 0.1, 0.1, 0.0, (Object)burstDust, true);
                player.getWorld().spawnParticle(Particle.END_ROD, playerCenter.clone().add(x, 0.0, z), 2, 0.0, 0.0, 0.0, 0.05);
            }
            player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.playSound(loc, Sound.BLOCK_BELL_USE, 1.0f, 2.0f);
            player.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 2.0f);
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false));
        }, 200L);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> new BukkitRunnable(){
            int ticks = 0;

            public void run() {
                if (!player.isOnline() || this.ticks >= 40) {
                    this.cancel();
                    return;
                }
                double angle = (double)this.ticks / 40.0 * 4.0 * Math.PI;
                double radius = 1.5;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location orbitLoc = player.getLocation().add(x, 1.5, z);
                Particle.DustOptions orbitDust = new Particle.DustOptions(gemColor, 1.0f);
                player.getWorld().spawnParticle(Particle.DUST, orbitLoc, 3, 0.1, 0.1, 0.1, 0.0, (Object)orbitDust, true);
                player.getWorld().spawnParticle(Particle.END_ROD, orbitLoc, 1, 0.0, 0.0, 0.0, 0.01);
                if (this.ticks % 5 == 0) {
                    Location playerLoc = player.getLocation().add(0.0, 1.0, 0.0);
                    player.getWorld().spawnParticle(Particle.END_ROD, playerLoc, 3, 0.5, 0.5, 0.5, 0.02);
                }
                ++this.ticks;
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L), 200L);
    }

    public long performRestorationRitual(final Player player, String gemId, int tier) {
        final Location loc = player.getLocation().clone();
        final World world = player.getWorld();
        final int buildUpTicks = this.plugin.getConfig().getInt("restoration.build-up-ticks", 120);
        double witnessRadius = this.plugin.getConfig().getDouble("restoration.witness-radius", 12.0);
        double witnessLaunch = this.plugin.getConfig().getDouble("restoration.witness-launch-power", 1.4);
        boolean controlWeather = this.plugin.getConfig().getBoolean("restoration.control-weather", true);
        boolean hadStorm = world.isThundering();
        boolean hadRain = world.hasStorm();
        if (controlWeather) {
            world.setStorm(true);
            world.setWeatherDuration(buildUpTicks + 400);
            this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                world.setThundering(true);
                world.setThunderDuration(buildUpTicks + 300);
            }, (long)buildUpTicks / 3L);
        }
        new BukkitRunnable(){
            int ticks = 0;

            public void run() {
                if (!player.isOnline() || this.ticks >= buildUpTicks) {
                    this.cancel();
                    return;
                }
                double progress = (double)this.ticks / (double)buildUpTicks;
                int strikeGap = Math.max(10, (int)(160.0 * (1.0 - progress)));
                if (this.ticks % strikeGap == 0) {
                    double spread = 14.0 * (1.0 - progress) + 2.0;
                    double angle = Math.random() * 2.0 * Math.PI;
                    Location strike = loc.clone().add(Math.cos(angle) * spread, 0.0, Math.sin(angle) * spread);
                    world.strikeLightningEffect(strike);
                    world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f + (float)progress * 0.6f);
                }
                if (this.ticks % 2 == 0) {
                    double angle = (double)this.ticks / 6.0;
                    double radius = 3.0 * (1.0 - progress) + 0.5;
                    Location swirl = loc.clone().add(Math.cos(angle) * radius, (double)(this.ticks % 40) / 10.0, Math.sin(angle) * radius);
                    world.spawnParticle(Particle.LARGE_SMOKE, swirl, 3, 0.2, 0.2, 0.2, 0.01);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, swirl, 2, 0.2, 0.2, 0.2, 0.05);
                }
                ++this.ticks;
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            world.strikeLightningEffect(loc);
            this.performGemRitual(player, gemId, false, tier);
        }, (long)buildUpTicks);
        long finaleTick = (long)buildUpTicks + 210L;
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            Location center = player.isOnline() ? player.getLocation() : loc;
            world.strikeLightningEffect(center);
            world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.6f);
            world.playSound(center, Sound.ITEM_TOTEM_USE, 1.0f, 0.8f);
            world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 3, 1.0, 0.5, 1.0, 0.0);
            world.spawnParticle(Particle.FLASH, center, 5, 0.5, 0.5, 0.5, 0.0);
            for (Entity entity : world.getNearbyEntities(center, witnessRadius, witnessRadius, witnessRadius)) {
                Player witness;
                if (!(entity instanceof Player) || (witness = (Player)entity).equals((Object)player)) continue;
                Vector away = witness.getLocation().toVector().subtract(center.toVector());
                if (away.lengthSquared() < 0.01) {
                    away = witness.getLocation().getDirection().multiply(-1);
                }
                away = away.normalize().multiply(witnessLaunch).setY(0.6);
                witness.setVelocity(away);
                witness.playSound(witness.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
            }
            if (controlWeather) {
                world.setThundering(hadStorm);
                world.setStorm(hadRain);
                if (!hadRain) {
                    world.setWeatherDuration(6000);
                }
            }
        }, finaleTick);
        return (long)buildUpTicks + 200L;
    }

    public void performReviveBeaconRitual(final Player player, final Location location) {
        final Color ritualColor = Color.fromRGB((int)255, (int)215, (int)0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, false, false));
        new BukkitRunnable(){
            int ticks = 0;

            public void run() {
                if (!player.isOnline() || this.ticks >= 20) {
                    this.cancel();
                    return;
                }
                double radius = (double)this.ticks / 20.0 * 6.0;
                for (int i = 0; i < 32; ++i) {
                    double angle = (double)i / 32.0 * 2.0 * Math.PI;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location particleLoc = location.clone().add(x, 0.1, z);
                    Particle.DustOptions dust = new Particle.DustOptions(ritualColor, 2.0f);
                    player.getWorld().spawnParticle(Particle.DUST, particleLoc, 5, 0.1, 0.1, 0.1, 0.0, (Object)dust, true);
                    player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, particleLoc, 2, 0.0, 0.0, 0.0, 0.02);
                }
                if (this.ticks % 5 == 0) {
                    player.playSound(location, Sound.BLOCK_BEACON_AMBIENT, 0.7f, 1.5f);
                }
                ++this.ticks;
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> new BukkitRunnable(){
            int ticks = 0;
            final int maxTicks = 30;

            public void run() {
                if (!player.isOnline() || this.ticks >= 30) {
                    this.cancel();
                    return;
                }
                double progress = (double)this.ticks / 30.0;
                double height = progress * 8.0;
                for (double y = 0.0; y <= height; y += 0.3) {
                    Particle.DustOptions pillarDust = new Particle.DustOptions(ritualColor, 1.5f);
                    player.getWorld().spawnParticle(Particle.DUST, location.clone().add(0.0, y, 0.0), 3, 0.2, 0.1, 0.2, 0.0, (Object)pillarDust, true);
                    player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, location.clone().add(0.0, y, 0.0), 1, 0.1, 0.1, 0.1, 0.01);
                }
                if (this.ticks % 10 == 0) {
                    player.playSound(location, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.0f + (float)progress);
                }
                ++this.ticks;
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L), 20L);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            Location center = location.clone().add(0.0, 8.0, 0.0);
            Particle.DustOptions explosionDust = new Particle.DustOptions(ritualColor, 3.0f);
            player.getWorld().spawnParticle(Particle.DUST, center, 300, 2.0, 2.0, 2.0, 0.0, (Object)explosionDust, true);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center, 150, 1.5, 1.5, 1.5, 0.15);
            player.getWorld().spawnParticle(Particle.FIREWORK, center, 80, 1.0, 1.0, 1.0, 0.2);
            player.getWorld().spawnParticle(Particle.END_ROD, center, 100, 2.0, 2.0, 2.0, 0.2);
            for (double y = 0.0; y <= 20.0; y += 0.2) {
                Particle.DustOptions beamDust = new Particle.DustOptions(ritualColor, 2.0f);
                player.getWorld().spawnParticle(Particle.DUST, location.clone().add(0.0, y, 0.0), 12, 0.3, 0.1, 0.3, 0.0, (Object)beamDust, true);
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, location.clone().add(0.0, y, 0.0), 3, 0.2, 0.1, 0.2, 0.03);
            }
            player.playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.0f);
            player.playSound(location, Sound.ITEM_TOTEM_USE, 1.0f, 1.2f);
            player.playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 2.0f);
        }, 50L);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> new BukkitRunnable(){
            int ticks = 0;

            public void run() {
                if (!player.isOnline() || this.ticks >= 40) {
                    this.cancel();
                    return;
                }
                double angle = (double)this.ticks / 40.0 * 4.0 * Math.PI;
                double radius = 2.0;
                for (int ring = 0; ring < 3; ++ring) {
                    double ringHeight = (double)ring * 2.0 + 1.0;
                    double x = Math.cos(angle + (double)ring * Math.PI / 1.5) * radius;
                    double z = Math.sin(angle + (double)ring * Math.PI / 1.5) * radius;
                    Location orbitLoc = location.clone().add(x, ringHeight, z);
                    Particle.DustOptions orbitDust = new Particle.DustOptions(ritualColor, 1.2f);
                    player.getWorld().spawnParticle(Particle.DUST, orbitLoc, 5, 0.1, 0.1, 0.1, 0.0, (Object)orbitDust, true);
                    player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, orbitLoc, 1, 0.0, 0.0, 0.0, 0.01);
                }
                if (this.ticks % 5 == 0) {
                    player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, location.clone().add(0.0, 2.0, 0.0), 5, 0.5, 0.5, 0.5, 0.02);
                }
                ++this.ticks;
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L), 60L);
    }

    private List<String> getRitualGemIds(int tier) {
        ArrayList<String> ids = new ArrayList<String>(this.plugin.getGemManager().getAvailableGemIds());
        ids.removeIf(id -> CustomItemManager.getItemById(this.getGemItemId((String)id, tier), 10) == null);
        return ids;
    }

    private String getGemItemId(String gemId, int tier) {
        return gemId + "_gem_t" + tier;
    }

    public Color getGemColor(String gemId) {
        GemType gemType = GemManager.builtInType(gemId);
        if (gemType != null) {
            return switch (gemType) {
                case ASTRA -> Color.fromRGB((int)106, (int)11, (int)184);
                case FIRE -> Color.fromRGB((int)255, (int)85, (int)85);
                case FLUX -> Color.fromRGB((int)85, (int)255, (int)255);
                case LIFE -> Color.fromRGB((int)85, (int)255, (int)85);
                case PUFF -> Color.fromRGB((int)255, (int)255, (int)255);
                case SPEED -> Color.fromRGB((int)255, (int)255, (int)85);
                case STRENGTH -> Color.fromRGB((int)170, (int)0, (int)0);
                case WEALTH -> Color.fromRGB((int)255, (int)170, (int)0);
                default -> Color.WHITE;
            };
        }
        return this.colorFromChatCode(this.plugin.getGemManager().getGemColorCode(gemId));
    }

    private Color colorFromChatCode(String code) {
        if (code == null || code.length() < 2) {
            return Color.fromRGB((int)255, (int)255, (int)255);
        }
        return switch (Character.toLowerCase(code.charAt(1))) {
            case '0' -> Color.fromRGB((int)0, (int)0, (int)0);
            case '1' -> Color.fromRGB((int)0, (int)0, (int)170);
            case '2' -> Color.fromRGB((int)0, (int)170, (int)0);
            case '3' -> Color.fromRGB((int)0, (int)170, (int)170);
            case '4' -> Color.fromRGB((int)170, (int)0, (int)0);
            case '5' -> Color.fromRGB((int)170, (int)0, (int)170);
            case '6' -> Color.fromRGB((int)255, (int)170, (int)0);
            case '7' -> Color.fromRGB((int)170, (int)170, (int)170);
            case '8' -> Color.fromRGB((int)85, (int)85, (int)85);
            case '9' -> Color.fromRGB((int)85, (int)85, (int)255);
            case 'a' -> Color.fromRGB((int)85, (int)255, (int)85);
            case 'b' -> Color.fromRGB((int)85, (int)255, (int)255);
            case 'c' -> Color.fromRGB((int)255, (int)85, (int)85);
            case 'd' -> Color.fromRGB((int)255, (int)85, (int)255);
            case 'e' -> Color.fromRGB((int)255, (int)255, (int)85);
            default -> Color.fromRGB((int)255, (int)255, (int)255);
        };
    }
}

