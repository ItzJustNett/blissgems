/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatMessageType
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Display$Brightness
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Fireball
 *  org.bukkit.entity.ItemDisplay
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.projectiles.ProjectileSource
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scheduler.BukkitTask
 *  org.bukkit.util.RayTraceResult
 *  org.bukkit.util.Transformation
 *  org.bukkit.util.Vector
 *  org.joml.Vector3f
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.utils.ParticleUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

public class FireAbilities
implements GemAbilityHandler {
    private final BlissGems plugin;
    private final Map<UUID, Integer> chargingPlayers = new HashMap<UUID, Integer>();
    private final Map<UUID, BukkitTask> chargingTasks = new HashMap<UUID, BukkitTask>();
    private final Map<UUID, ItemDisplay> chargeDisplays = new HashMap<UUID, ItemDisplay>();
    private final Map<UUID, Location> activeCampfires = new HashMap<UUID, Location>();
    private final Map<UUID, BukkitTask> campfireTasks = new HashMap<UUID, BukkitTask>();
    private final Set<UUID> crispActivePlayers = new HashSet<UUID>();
    private final Map<UUID, BukkitTask> crispTasks = new HashMap<UUID, BukkitTask>();
    private final Map<UUID, Map<Location, Material>> crispOriginalBlocks = new HashMap<UUID, Map<Location, Material>>();
    private final Map<UUID, Map<Location, Material>> crispOriginalWater = new HashMap<UUID, Map<Location, Material>>();
    private final Set<UUID> meteorShowersActive = new HashSet<UUID>();
    private final Map<UUID, BukkitTask> meteorTasks = new HashMap<UUID, BukkitTask>();
    private static final int MAX_CHARGE = 100;
    private static final int CHARGE_DURATION_TICKS = 300;
    private static final Material[] NETHER_BLOCKS = new Material[]{Material.NETHERRACK, Material.NETHER_BRICKS, Material.NETHER_BRICK_FENCE, Material.MAGMA_BLOCK, Material.SOUL_SAND};
    private final Random random = new Random();

    private static boolean isContainer(Material material) {
        String name = material.name();
        return name.contains("CHEST") || name.contains("SHULKER_BOX") || name.contains("BARREL") || name.contains("HOPPER") || name.contains("FURNACE") || name.contains("BLAST_FURNACE") || name.contains("SMOKER") || name.contains("DROPPER") || name.contains("DISPENSER") || name.contains("BREWING_STAND") || material == Material.JUKEBOX || material == Material.LECTERN || material == Material.CHISELED_BOOKSHELF;
    }

    public FireAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    public boolean isCharging(Player player) {
        return this.chargingPlayers.containsKey(player.getUniqueId());
    }

    public int getCharge(Player player) {
        return this.chargingPlayers.getOrDefault(player.getUniqueId(), 0);
    }

    public boolean isCrispActive(Player player) {
        return this.crispActivePlayers.contains(player.getUniqueId());
    }

    public boolean isProtectedBlock(Location loc) {
        for (Map<Location, Material> blocks : this.crispOriginalBlocks.values()) {
            if (!blocks.containsKey(loc)) continue;
            return true;
        }
        return false;
    }

    public void onRightClick(Player player, int tier) {
        if (tier == 2 && player.isSneaking()) {
            this.cozyCampfire(player);
        } else {
            this.chargedFireball(player);
        }
    }

    @Override
    public void onPrimary(Player player, int tier) {
        this.chargedFireball(player);
    }

    @Override
    public void onSecondary(Player player, int tier) {
        this.cozyCampfire(player);
    }

    @Override
    public void onTertiary(Player player, int tier) {
        this.crisp(player);
    }

    @Override
    public void onQuaternary(Player player, int tier) {
        this.meteorShower(player);
    }

    public void chargedFireball(final Player player) {
        String abilityKey = "fire-fireball";
        if (this.isCharging(player)) {
            this.fireChargedShot(player);
            return;
        }
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        final UUID uuid = player.getUniqueId();
        this.chargingPlayers.put(uuid, 0);
        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.0f, 0.5f);
        player.sendMessage("\u00a76\u00a7oCharging fireball... Right-click again to fire!");
        this.spawnChargeDisplay(player);
        this.spawnChargeStartBurst(player);
        BukkitTask task = new BukkitRunnable(){
            int ticksElapsed = 0;
            boolean maxChargeNotified = false;
            boolean decaying = false;

            public void run() {
                int newCharge;
                if (!player.isOnline() || player.isDead() || !FireAbilities.this.chargingPlayers.containsKey(uuid)) {
                    FireAbilities.this.cancelCharging(player);
                    this.cancel();
                    return;
                }
                ++this.ticksElapsed;
                int currentCharge = FireAbilities.this.chargingPlayers.getOrDefault(uuid, 0);
                int chargeDurationTicks = Math.max(1, FireAbilities.this.plugin.getConfig().getInt("abilities.fire-fireball.charge-duration-ticks", 300));
                if (!this.decaying) {
                    newCharge = Math.min(this.ticksElapsed * 100 / chargeDurationTicks, 100);
                    if (newCharge >= 100) {
                        this.decaying = true;
                        newCharge = 100;
                    }
                } else {
                    Block blockBelow = player.getLocation().subtract(0.0, 1.0, 0.0).getBlock();
                    newCharge = blockBelow.getType() == Material.OBSIDIAN || blockBelow.getType() == Material.CRYING_OBSIDIAN ? currentCharge : currentCharge - 1;
                    if (newCharge <= 0) {
                        player.sendMessage("\u00a7c\u00a7oFireball charge fizzled!");
                        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
                        FireAbilities.this.cancelCharging(player);
                        this.cancel();
                        return;
                    }
                }
                FireAbilities.this.chargingPlayers.put(uuid, newCharge);
                FireAbilities.this.updateChargeDisplay(player, newCharge);
                FireAbilities.this.showChargeBar(player, newCharge);
                if (newCharge == 25 || newCharge == 50 || newCharge == 75) {
                    player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.0f + (float)newCharge / 100.0f);
                }
                if (newCharge >= 100 && !this.maxChargeNotified) {
                    player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.5f);
                    player.sendMessage("\u00a76\u00a7lFully charged! \u00a7eRight-click to fire!");
                    this.maxChargeNotified = true;
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        this.chargingTasks.put(uuid, task);
    }

    private void showChargeBar(Player player, int charge) {
        int bars = charge / 5;
        StringBuilder bar = new StringBuilder("\u00a76Fireball: \u00a7c");
        for (int i = 0; i < 20; ++i) {
            if (i < bars) {
                bar.append("\u2588");
                continue;
            }
            bar.append("\u00a78\u2588");
        }
        bar.append(" \u00a7e").append(charge).append("%");
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, (BaseComponent)new TextComponent(bar.toString()));
    }

    private void fireChargedShot(Player player) {
        UUID uuid = player.getUniqueId();
        int charge = this.chargingPlayers.getOrDefault(uuid, 0);
        BukkitTask task = this.chargingTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        this.chargingPlayers.remove(uuid);
        this.removeChargeDisplay(uuid);
        if (charge < 10) {
            player.sendMessage("\u00a7c\u00a7oNot enough charge!");
            return;
        }
        double yieldBase = this.plugin.getConfig().getDouble("abilities.fire-fireball.yield-base", 1.5);
        double yieldPerPercent = this.plugin.getConfig().getDouble("abilities.fire-fireball.yield-per-percent", 0.025);
        float yield = (float)(yieldBase + yieldPerPercent * (double)charge);
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        Fireball fireball = (Fireball)player.getWorld().spawn(eyeLoc.clone().add(direction.clone().multiply(1.5)), Fireball.class);
        fireball.setShooter((ProjectileSource)player);
        fireball.setVelocity(direction.multiply(1.5 + (double)charge / 100.0));
        fireball.setYield(yield);
        fireball.setIsIncendiary(true);
        int particles = 20 + charge / 2;
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f + (float)charge / 200.0f);
        player.spawnParticle(Particle.FLAME, eyeLoc, particles, 0.5, 0.5, 0.5, 0.1);
        this.plugin.getAbilityManager().useAbility(player, "fire-fireball");
        player.sendMessage("\u00a76\u00a7oFired at " + charge + "% power!");
    }

    public void cancelCharging(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = this.chargingTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        this.chargingPlayers.remove(uuid);
        this.removeChargeDisplay(uuid);
    }

    private void spawnChargeStartBurst(Player player) {
        Location loc = player.getLocation();
        World world = player.getWorld();
        Particle.DustOptions orangeDust = new Particle.DustOptions(ParticleUtils.FIRE_ORANGE, 1.5f);
        for (int i = 0; i < 24; ++i) {
            double angle = (double)i / 24.0 * 2.0 * Math.PI;
            double x = Math.cos(angle) * 1.4;
            double z = Math.sin(angle) * 1.4;
            world.spawnParticle(Particle.FLAME, loc.clone().add(x, 0.2, z), 2, 0.05, 0.05, 0.05, 0.02);
            world.spawnParticle(Particle.DUST, loc.clone().add(x, 0.4, z), 1, 0.05, 0.05, 0.05, 0.0, (Object)orangeDust, true);
        }
        world.spawnParticle(Particle.FLAME, loc.clone().add(0.0, 0.6, 0.0), 25, 0.4, 0.4, 0.4, 0.06);
        world.spawnParticle(Particle.LAVA, loc.clone().add(0.0, 0.3, 0.0), 6, 0.5, 0.1, 0.5, 0.0);
    }

    private void spawnChargeDisplay(Player player) {
        try {
            UUID uuid = player.getUniqueId();
            this.removeChargeDisplay(uuid);
            Location loc = this.displayLocation(player);
            ItemDisplay display = (ItemDisplay)player.getWorld().spawnEntity(loc, EntityType.ITEM_DISPLAY);
            display.setItemStack(new ItemStack(Material.FIRE_CHARGE));
            display.setBrightness(new Display.Brightness(15, 15));
            display.setTeleportDuration(2);
            this.applyDisplayScale(display, 0.3f);
            this.chargeDisplays.put(uuid, display);
        }
        catch (Throwable t) {
            this.plugin.getLogger().warning("Failed to spawn fireball charge display: " + t.getMessage());
        }
    }

    private void updateChargeDisplay(Player player, int charge) {
        ItemDisplay display = this.chargeDisplays.get(player.getUniqueId());
        if (display == null || !display.isValid()) {
            return;
        }
        float scale = 0.3f + (float)Math.max(0, Math.min(charge, 100)) / 100.0f * 0.7f;
        this.applyDisplayScale(display, scale);
        display.teleport(this.displayLocation(player));
    }

    private void removeChargeDisplay(UUID uuid) {
        ItemDisplay display = this.chargeDisplays.remove(uuid);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    private Location displayLocation(Player player) {
        return player.getLocation().add(0.0, 2.5, 0.0);
    }

    private void applyDisplayScale(ItemDisplay display, float scale) {
        Transformation t = display.getTransformation();
        display.setTransformation(new Transformation(t.getTranslation(), t.getLeftRotation(), new Vector3f(scale, scale, scale), t.getRightRotation()));
    }

    public void cozyCampfire(final Player player) {
        Block targetBlock;
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "fire-campfire";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        final UUID uuid = player.getUniqueId();
        if (this.activeCampfires.containsKey(uuid)) {
            this.removeCampfire(player);
        }
        double maxRange = this.plugin.getConfig().getDouble("abilities.fire-campfire.range", 15.0);
        RayTraceResult rayResult = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), maxRange);
        if (rayResult != null && rayResult.getHitBlock() != null) {
            targetBlock = rayResult.getHitBlock().getRelative(rayResult.getHitBlockFace());
        } else {
            targetBlock = player.getLocation().add(player.getEyeLocation().getDirection().multiply(maxRange)).getBlock();
        }
        if (targetBlock.getType() != Material.AIR && targetBlock.getType() != Material.CAVE_AIR) {
            player.sendMessage("\u00a7c\u00a7oCannot place campfire here!");
            return;
        }
        targetBlock.setType(Material.CAMPFIRE);
        final Location campfireLocation = targetBlock.getLocation().clone();
        World campfireWorld = campfireLocation.getWorld();
        this.activeCampfires.put(uuid, campfireLocation);
        final double radius = this.plugin.getConfig().getDouble("abilities.fire-campfire.radius", 5.0);
        final double damage = this.plugin.getConfig().getDouble("abilities.damage.fire-campfire", 2.0);
        final int burnDuration = this.plugin.getConfig().getInt("abilities.fire-campfire.burn-duration", 3);
        final int duration = this.plugin.getConfig().getInt("abilities.durations.fire-campfire", 60) * 20;
        player.playSound(campfireLocation, Sound.BLOCK_CAMPFIRE_CRACKLE, 1.0f, 1.0f);
        player.sendMessage("\u00a76\u00a7oPlaced Campfire! Heals you and burns enemies for 1 minute.");
        BukkitTask campfireTask = new BukkitRunnable(){
            int ticksElapsed = 0;

            public void run() {
                Block currentBlock = campfireLocation.getBlock();
                if (currentBlock.getType() != Material.CAMPFIRE) {
                    FireAbilities.this.activeCampfires.remove(uuid);
                    FireAbilities.this.campfireTasks.remove(uuid);
                    player.sendMessage("\u00a76\u00a7oCampfire was destroyed!");
                    this.cancel();
                    return;
                }
                if (this.ticksElapsed >= duration) {
                    FireAbilities.this.removeCampfireBlock(campfireLocation);
                    FireAbilities.this.activeCampfires.remove(uuid);
                    FireAbilities.this.campfireTasks.remove(uuid);
                    player.sendMessage("\u00a76\u00a7oCampfire expired!");
                    this.cancel();
                    return;
                }
                if (this.ticksElapsed % 20 == 0) {
                    for (Entity entity : campfireLocation.getWorld().getNearbyEntities(campfireLocation, radius, radius, radius)) {
                        Player nearby;
                        if (!(entity instanceof Player) || !(nearby = (Player)entity).equals((Object)player) && !FireAbilities.this.plugin.getTrustedPlayersManager().isTrusted(player, nearby)) continue;
                        nearby.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 3));
                    }
                    for (Entity entity : campfireLocation.getWorld().getNearbyEntities(campfireLocation, radius, radius, radius)) {
                        if (!(entity instanceof LivingEntity) || entity == player) continue;
                        LivingEntity target = (LivingEntity)entity;
                        if (entity instanceof Player) {
                            Player targetPlayer = (Player)entity;
                            if (FireAbilities.this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) continue;
                        }
                        target.damage(damage, (Entity)player);
                        target.setFireTicks(burnDuration * 20);
                        target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0.0, 1.0, 0.0), 10, 0.3, 0.5, 0.3, 0.02);
                    }
                    campfireLocation.getWorld().playSound(campfireLocation, Sound.BLOCK_CAMPFIRE_CRACKLE, 0.5f, 1.0f);
                }
                if (this.ticksElapsed % 5 == 0) {
                    int circlePoints = 48;
                    Particle.DustOptions orangeDust = new Particle.DustOptions(ParticleUtils.FIRE_ORANGE, 1.0f);
                    for (int i = 0; i < circlePoints; ++i) {
                        double angle = (double)i / (double)circlePoints * 2.0 * Math.PI;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        campfireLocation.getWorld().spawnParticle(Particle.DUST, campfireLocation.clone().add(x, 0.3, z), 3, 0.1, 0.1, 0.1, 0.0, (Object)orangeDust, true);
                        campfireLocation.getWorld().spawnParticle(Particle.FLAME, campfireLocation.clone().add(x, 0.8, z), 2, 0.1, 0.1, 0.1, 0.01);
                        if (i % 4 != 0) continue;
                        campfireLocation.getWorld().spawnParticle(Particle.LAVA, campfireLocation.clone().add(x, 1.2, z), 1, 0.0, 0.0, 0.0, 0.0);
                    }
                    campfireLocation.getWorld().spawnParticle(Particle.FLAME, campfireLocation.clone().add(0.5, 1.0, 0.5), 15, 0.3, 0.5, 0.3, 0.02);
                    campfireLocation.getWorld().spawnParticle(Particle.LAVA, campfireLocation.clone().add(0.5, 0.5, 0.5), 8, 0.5, 0.2, 0.5, 0.0);
                }
                ++this.ticksElapsed;
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        this.campfireTasks.put(uuid, campfireTask);
        int durationSeconds = this.plugin.getConfig().getInt("abilities.durations.fire-campfire", 60);
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, durationSeconds);
    }

    private void removeCampfireBlock(Location location) {
        World world;
        Block block = location.getBlock();
        if (block.getType() == Material.CAMPFIRE) {
            block.setType(Material.AIR);
            location.getWorld().playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
        }
        if ((world = location.getWorld()) == null) {
            return;
        }
        for (int dx = -2; dx <= 2; ++dx) {
            for (int dy = -1; dy <= 2; ++dy) {
                for (int dz = -2; dz <= 2; ++dz) {
                    Block near = world.getBlockAt(location.getBlockX() + dx, location.getBlockY() + dy, location.getBlockZ() + dz);
                    if (near.getType() != Material.FIRE) continue;
                    near.setType(Material.AIR);
                }
            }
        }
    }

    public void removeCampfire(Player player) {
        Location loc;
        UUID uuid = player.getUniqueId();
        BukkitTask task = this.campfireTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        if ((loc = this.activeCampfires.remove(uuid)) != null) {
            this.removeCampfireBlock(loc);
        }
    }

    public void crisp(final Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "fire-crisp";
        if (this.isCrispActive(player)) {
            player.sendMessage("\u00a7c\u00a7oCrisp is already active!");
            return;
        }
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        final UUID uuid = player.getUniqueId();
        Location center = player.getLocation().clone();
        final int radius = this.plugin.getConfig().getInt("abilities.fire-crisp.radius", 10);
        int durationSeconds = this.plugin.getConfig().getInt("abilities.durations.fire-crisp", 15);
        final int duration = durationSeconds * 20;
        this.crispActivePlayers.add(uuid);
        player.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 1.5f, 0.5f);
        player.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.7f);
        player.sendMessage("\u00a76\u00a7lCrisp! \u00a7eEvaporating water and scorching the earth for " + durationSeconds + "s!");
        this.crispOriginalBlocks.put(uuid, new HashMap());
        this.crispOriginalWater.put(uuid, new HashMap());
        this.evaporateAndScorch(center, radius, uuid);
        for (int i = 0; i < 36; ++i) {
            double angle = (double)i / 36.0 * 2.0 * Math.PI;
            for (double r = 0.0; r <= (double)radius; r += 0.5) {
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;
                center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(x, 0.3, z), 1, 0.05, 0.1, 0.05, 0.02);
            }
        }
        Particle.DustOptions orangeDust = new Particle.DustOptions(ParticleUtils.FIRE_ORANGE, 1.5f);
        for (int i = 0; i < 48; ++i) {
            double angle = (double)i / 48.0 * 2.0 * Math.PI;
            double x = Math.cos(angle) * (double)radius;
            double z = Math.sin(angle) * (double)radius;
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, 0.5, z), 5, 0.1, 0.3, 0.1, 0.0, (Object)orangeDust, true);
        }
        BukkitTask crispTask = new BukkitRunnable(){
            int ticksElapsed = 0;

            public void run() {
                if (!player.isOnline() || player.isDead() || this.ticksElapsed >= duration) {
                    FireAbilities.this.endCrisp(player);
                    this.cancel();
                    return;
                }
                ++this.ticksElapsed;
                if (this.ticksElapsed % 10 == 0) {
                    Location playerLoc = player.getLocation();
                    FireAbilities.this.evaporateWater(playerLoc, radius, uuid);
                    if (this.ticksElapsed % 20 == 0) {
                        for (int i = 0; i < 8; ++i) {
                            double angle = FireAbilities.this.random.nextDouble() * 2.0 * Math.PI;
                            double r = FireAbilities.this.random.nextDouble() * (double)radius;
                            double x = Math.cos(angle) * r;
                            double z = Math.sin(angle) * r;
                            playerLoc.getWorld().spawnParticle(Particle.LAVA, playerLoc.clone().add(x, 0.2, z), 1, 0.1, 0.1, 0.1, 0.0);
                            playerLoc.getWorld().spawnParticle(Particle.FLAME, playerLoc.clone().add(x, 0.3, z), 1, 0.1, 0.1, 0.1, 0.01);
                        }
                        playerLoc.getWorld().playSound(playerLoc, Sound.BLOCK_FIRE_AMBIENT, 0.4f, 0.8f);
                    }
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        this.crispTasks.put(uuid, crispTask);
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, durationSeconds);
    }

    private void evaporateAndScorch(Location center, int radius, UUID ownerUuid) {
        this.evaporateWater(center, radius, ownerUuid);
        Map<Location, Material> originalBlocks = this.crispOriginalBlocks.get(ownerUuid);
        for (int x = -radius; x <= radius; ++x) {
            for (int z = -radius; z <= radius; ++z) {
                Location loc;
                Block surface;
                if (x * x + z * z > radius * radius || FireAbilities.isContainer((surface = (loc = center.clone().add((double)x, 0.0, (double)z)).getWorld().getHighestBlockAt(loc)).getType()) || !surface.getType().isSolid() || surface.getType().name().startsWith("NETHER") || surface.getType() == Material.MAGMA_BLOCK || surface.getType() == Material.SOUL_SAND) continue;
                Location blockLoc = surface.getLocation();
                if (originalBlocks != null) {
                    originalBlocks.put(blockLoc, surface.getType());
                }
                Material netherMat = NETHER_BLOCKS[this.random.nextInt(NETHER_BLOCKS.length)];
                surface.setType(netherMat);
            }
        }
    }

    private void evaporateWater(Location center, int radius, UUID ownerUuid) {
        Map<Location, Material> waterMap = this.crispOriginalWater.get(ownerUuid);
        for (int x = -radius; x <= radius; ++x) {
            for (int y = -radius; y <= radius; ++y) {
                for (int z = -radius; z <= radius; ++z) {
                    Block block;
                    Material t;
                    if (x * x + y * y + z * z > radius * radius || (t = (block = center.clone().add((double)x, (double)y, (double)z).getBlock()).getType()) != Material.WATER && t != Material.BUBBLE_COLUMN && t != Material.KELP && t != Material.KELP_PLANT && t != Material.SEAGRASS && t != Material.TALL_SEAGRASS) continue;
                    if (waterMap != null) {
                        waterMap.putIfAbsent(block.getLocation(), t);
                    }
                    center.getWorld().spawnParticle(Particle.CLOUD, block.getLocation().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.02);
                    block.setType(Material.AIR);
                }
            }
        }
    }

    private void endCrisp(Player player) {
        Map<Location, Material> originalWater;
        Map<Location, Material> originalBlocks;
        UUID uuid = player.getUniqueId();
        boolean wasActive = this.crispActivePlayers.remove(uuid);
        BukkitTask task = this.crispTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        if ((originalBlocks = this.crispOriginalBlocks.remove(uuid)) != null) {
            for (Map.Entry<Location, Material> entry : originalBlocks.entrySet()) {
                Block block = entry.getKey().getBlock();
                String blockName = block.getType().name();
                if (!blockName.startsWith("NETHER") && block.getType() != Material.MAGMA_BLOCK && block.getType() != Material.SOUL_SAND) continue;
                block.setType(entry.getValue());
            }
        }
        if ((originalWater = this.crispOriginalWater.remove(uuid)) != null) {
            for (Map.Entry<Location, Material> entry : originalWater.entrySet()) {
                Block block = entry.getKey().getBlock();
                if (block.getType() != Material.AIR) continue;
                block.setType(entry.getValue());
            }
        }
        if (wasActive && player.isOnline()) {
            player.sendMessage("\u00a76\u00a7oCrisp faded. Terrain restored.");
        }
    }

    public void meteorShower(final Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "fire-meteor-shower";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        final UUID uuid = player.getUniqueId();
        Location target = this.getGroundTarget(player, 50);
        if (target == null) {
            player.sendMessage("\u00a7c\u00a7oNo valid target area found!");
            return;
        }
        int durationSeconds = this.plugin.getConfig().getInt("abilities.durations.fire-meteor-shower", 8);
        final int duration = durationSeconds * 20;
        final double aoeRadius = this.plugin.getConfig().getDouble("abilities.fire-meteor-shower.radius", 8.0);
        final double damage = this.plugin.getConfig().getDouble("abilities.damage.fire-meteor-shower", 5.0);
        final int meteorInterval = this.plugin.getConfig().getInt("abilities.fire-meteor-shower.interval-ticks", 10);
        this.meteorShowersActive.add(uuid);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.3f);
        player.sendMessage("\u00a76\u00a7lMeteor Shower! \u00a7eFire rains down for " + durationSeconds + "s!");
        Particle.DustOptions orangeDust = new Particle.DustOptions(ParticleUtils.FIRE_ORANGE, 1.2f);
        for (int i = 0; i < 48; ++i) {
            double angle = (double)i / 48.0 * 2.0 * Math.PI;
            double x = Math.cos(angle) * aoeRadius;
            double z = Math.sin(angle) * aoeRadius;
            target.getWorld().spawnParticle(Particle.DUST, target.clone().add(x, 0.5, z), 3, 0.1, 0.2, 0.1, 0.0, (Object)orangeDust, true);
        }
        final Location finalTarget = target.clone();
        BukkitTask meteorTask = new BukkitRunnable(){
            int ticksElapsed = 0;

            public void run() {
                if (!player.isOnline() || player.isDead() || this.ticksElapsed >= duration) {
                    FireAbilities.this.meteorShowersActive.remove(uuid);
                    FireAbilities.this.meteorTasks.remove(uuid);
                    if (player.isOnline()) {
                        player.sendMessage("\u00a76\u00a7oMeteor Shower ended.");
                    }
                    this.cancel();
                    return;
                }
                ++this.ticksElapsed;
                if (this.ticksElapsed % meteorInterval == 0) {
                    double angle = FireAbilities.this.random.nextDouble() * 2.0 * Math.PI;
                    double r = Math.sqrt(FireAbilities.this.random.nextDouble()) * aoeRadius;
                    double mx = Math.cos(angle) * r;
                    double mz = Math.sin(angle) * r;
                    int spawnHeight = 20 + FireAbilities.this.random.nextInt(10);
                    Location spawnLoc = finalTarget.clone().add(mx, (double)spawnHeight, mz);
                    Location impactLoc = finalTarget.clone().add(mx, 0.0, mz);
                    Block groundBlock = finalTarget.getWorld().getHighestBlockAt(impactLoc);
                    impactLoc.setY((double)groundBlock.getY() + 0.5);
                    Vector trajectory = impactLoc.toVector().subtract(spawnLoc.toVector()).normalize();
                    FireAbilities.this.plugin.getServer().getScheduler().runTaskLater((Plugin)FireAbilities.this.plugin, () -> {
                        Block below;
                        Block impactBlock;
                        if (!player.isOnline()) {
                            return;
                        }
                        double totalDist = spawnLoc.distance(impactLoc);
                        for (double d = 0.0; d < totalDist; d += 0.8) {
                            Location trailLoc = spawnLoc.clone().add(trajectory.clone().multiply(d));
                            finalTarget.getWorld().spawnParticle(Particle.FLAME, trailLoc, 30, 0.3, 0.3, 0.3, 0.03);
                            finalTarget.getWorld().spawnParticle(Particle.LAVA, trailLoc, 10, 0.15, 0.15, 0.15, 0.0);
                        }
                        finalTarget.getWorld().spawnParticle(Particle.FLAME, impactLoc, 400, 3.0, 1.5, 3.0, 0.1);
                        finalTarget.getWorld().spawnParticle(Particle.LAVA, impactLoc, 150, 1.5, 0.6, 1.5, 0.0);
                        finalTarget.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 30, 0.9, 0.3, 0.9, 0.0);
                        finalTarget.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, impactLoc, 100, 1.5, 0.9, 1.5, 0.05);
                        finalTarget.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.5f);
                        finalTarget.getWorld().playSound(impactLoc, Sound.BLOCK_LAVA_POP, 1.0f, 0.8f);
                        double impactRadius = 2.5;
                        for (Entity entity : finalTarget.getWorld().getNearbyEntities(impactLoc, impactRadius, impactRadius, impactRadius)) {
                            if (!(entity instanceof LivingEntity) || entity == player) continue;
                            LivingEntity livingTarget = (LivingEntity)entity;
                            if (entity instanceof Player) {
                                Player targetPlayer = (Player)entity;
                                if (FireAbilities.this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) continue;
                            }
                            livingTarget.damage(damage, (Entity)player);
                            livingTarget.setFireTicks(60);
                            livingTarget.getWorld().spawnParticle(Particle.FLAME, livingTarget.getLocation().add(0.0, 1.0, 0.0), 150, 1.2, 1.8, 1.2, 0.05);
                        }
                        float meteorYield = (float)FireAbilities.this.plugin.getConfig().getDouble("abilities.fire-meteor-shower.yield", 2.0);
                        boolean breakBlocks = FireAbilities.this.plugin.getConfig().getBoolean("abilities.fire-meteor-shower.break-blocks", true);
                        if (meteorYield > 0.0f) {
                            impactLoc.getWorld().createExplosion(impactLoc, meteorYield, true, breakBlocks, (Entity)player);
                        }
                        if (((impactBlock = impactLoc.getBlock()).getType() == Material.AIR || impactBlock.getType() == Material.CAVE_AIR) && (below = impactBlock.getRelative(0, -1, 0)).getType().isSolid()) {
                            impactBlock.setType(Material.FIRE);
                        }
                    }, 0L);
                }
                if (this.ticksElapsed % 20 == 0) {
                    Particle.DustOptions warningDust = new Particle.DustOptions(ParticleUtils.FIRE_ORANGE, 0.8f);
                    for (int i = 0; i < 24; ++i) {
                        double angle = (double)i / 24.0 * 2.0 * Math.PI;
                        double x = Math.cos(angle) * aoeRadius;
                        double z = Math.sin(angle) * aoeRadius;
                        finalTarget.getWorld().spawnParticle(Particle.DUST, finalTarget.clone().add(x, 0.3, z), 20, 0.3, 0.3, 0.3, 0.0, (Object)warningDust, true);
                    }
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        this.meteorTasks.put(uuid, meteorTask);
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, durationSeconds);
    }

    private Location getGroundTarget(Player player, int maxDistance) {
        RayTraceResult result = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), (double)maxDistance);
        if (result != null && result.getHitBlock() != null) {
            return result.getHitBlock().getLocation().add(0.5, 1.0, 0.5);
        }
        Location projected = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(20));
        Block highest = player.getWorld().getHighestBlockAt(projected);
        return highest.getLocation().add(0.5, 1.0, 0.5);
    }

    @Override
    public void cleanup(Player player) {
        this.cancelCharging(player);
        this.removeCampfire(player);
        this.endCrisp(player);
        UUID uuid = player.getUniqueId();
        this.crispOriginalBlocks.remove(uuid);
        this.crispOriginalWater.remove(uuid);
        this.meteorShowersActive.remove(uuid);
        BukkitTask meteorTask = this.meteorTasks.remove(uuid);
        if (meteorTask != null) {
            meteorTask.cancel();
        }
    }
}

