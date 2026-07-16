/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.xoperr.blissgems.api.BlissGemsAPI
 *  dev.xoperr.blissgems.api.GemAbilityHandler
 *  dev.xoperr.blissgems.api.GemPassiveHandler
 *  org.bukkit.Bukkit
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Registry
 *  org.bukkit.Sound
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.entity.Display$Billboard
 *  org.bukkit.entity.Display$Brightness
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.ItemDisplay
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.bukkit.event.player.PlayerItemDamageEvent
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.util.RayTraceResult
 *  org.bukkit.util.Transformation
 *  org.bukkit.util.Vector
 *  org.joml.Quaternionf
 *  org.joml.Vector3f
 */
package dev.xoperr.blissmythics;

import dev.xoperr.blissgems.api.BlissGemsAPI;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.api.GemPassiveHandler;
import dev.xoperr.blissmythics.BlissMythics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class AuratusGem
implements GemAbilityHandler,
GemPassiveHandler,
Listener {
    private static final String KEY_CHAINS = "auratus-perforators";
    private static final String KEY_AEGIS = "auratus-aegis";
    private static final double CHAIN_RANGE = 55.0;
    private static final int CD_CHAINS = 20;
    private static final int CD_AEGIS = 45;
    private static final long CHAIN_REGEN_MS = 8000L;
    private static final int CHAIN_CHARGES = 2;
    private static final long AEGIS_MS = 1500L;
    private static final long ANCHOR_MS = 5000L;
    private static final double CHAIN_FLY_SPEED = 4.5;
    private static final int CHAIN_FLY_MAX_TICKS = 60;
    private final BlissMythics plugin;
    private final BlissGemsAPI api;
    private final Map<UUID, List<Long>> chainUses = new HashMap<UUID, List<Long>>();
    private final Map<UUID, Long> aegisWindow = new HashMap<UUID, Long>();
    private final Map<UUID, Long> anchorWindow = new HashMap<UUID, Long>();
    private final Map<UUID, Long> slamWindow = new HashMap<UUID, Long>();

    public AuratusGem(BlissMythics blissMythics, BlissGemsAPI blissGemsAPI) {
        this.plugin = blissMythics;
        this.api = blissGemsAPI;
    }

    public int chainCharges(Player player) {
        List<Long> list = this.chainUses.get(player.getUniqueId());
        if (list == null) {
            return 2;
        }
        long l = System.currentTimeMillis();
        list.removeIf(l2 -> l - l2 >= 8000L);
        return Math.max(0, 2 - list.size());
    }

    public int nextChainChargeIn(Player player) {
        List<Long> list = this.chainUses.get(player.getUniqueId());
        if (list != null && !list.isEmpty()) {
            long l = System.currentTimeMillis();
            long l2 = Long.MAX_VALUE;
            for (long l3 : list) {
                l2 = Math.min(l2, l3 + 8000L - l);
            }
            return (int)Math.max(0L, (l2 + 999L) / 1000L);
        }
        return 0;
    }

    public void onPrimary(Player player, int n) {
        if (this.chainCharges(player) <= 0) {
            player.sendMessage("\u00a76Venerated Perforators \u00a77recharging: \u00a7c" + this.nextChainChargeIn(player) + "s");
        } else if (this.fireChain(player)) {
            this.chainUses.computeIfAbsent(player.getUniqueId(), uUID -> new ArrayList()).add(System.currentTimeMillis());
        }
    }

    public void onSecondary(Player player, int n) {
        if (this.api.getAbilityManager().isOnCooldown(player, KEY_AEGIS)) {
            int n2 = this.api.getAbilityManager().getRemainingCooldown(player, KEY_AEGIS);
            player.sendMessage("\u00a76Echoing Aegis \u00a77on cooldown: \u00a7c" + n2 + "s");
        } else {
            this.api.getAbilityManager().setCooldown(player, KEY_AEGIS, 45);
            this.aegisWindow.put(player.getUniqueId(), System.currentTimeMillis() + 1500L);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.6f);
            this.throwParryWeb(player);
            player.sendMessage("\u00a76\u00a7oEchoing Aegis! Parry window open...");
        }
    }

    private void throwParryWeb(Player player) {
        ItemStack itemStack = new ItemStack(Material.ECHO_SHARD);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setCustomModelData(Integer.valueOf(12346));
        itemStack.setItemMeta(itemMeta);
        Location location = player.getEyeLocation();
        final Vector vector = location.getDirection().normalize();
        Location location2 = location.clone().add(vector.clone().multiply(1.3));
        final ItemDisplay itemDisplay2 = (ItemDisplay)player.getWorld().spawn(location2, ItemDisplay.class, itemDisplay -> {
            itemDisplay.setItemStack(itemStack);
            itemDisplay.setBillboard(Display.Billboard.CENTER);
            itemDisplay.setBrightness(new Display.Brightness(15, 15));
            itemDisplay.setPersistent(false);
            itemDisplay.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), new Quaternionf().rotationX((float)Math.toRadians(90.0)), new Vector3f(1.4f, 1.4f, 1.4f), new Quaternionf()));
            itemDisplay.setTeleportDuration(2);
        });
        new BukkitRunnable(this){
            int ticks = 0;
            final Vector step = vector.clone().multiply(0.45);

            public void run() {
                if (++this.ticks <= 24 && itemDisplay2.isValid()) {
                    itemDisplay2.teleport(itemDisplay2.getLocation().add(this.step));
                } else {
                    if (itemDisplay2.isValid()) {
                        itemDisplay2.remove();
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 1L, 1L);
    }

    private boolean fireChain(Player player) {
        Entity entity2;
        Location location = player.getEyeLocation();
        Vector vector = location.getDirection().normalize();
        RayTraceResult rayTraceResult = player.getWorld().rayTraceEntities(location, vector, 55.0, 0.8, entity -> entity != player && entity instanceof LivingEntity);
        if (rayTraceResult != null && (entity2 = rayTraceResult.getHitEntity()) instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)entity2;
            this.launchChainAt(player, livingEntity);
            return true;
        }
        RayTraceResult rayTraceResult2 = player.getWorld().rayTraceBlocks(location, vector, 55.0);
        entity2 = null;
        if (rayTraceResult2 != null && rayTraceResult2.getHitPosition() != null) {
            entity2 = rayTraceResult2.getHitPosition().toLocation(player.getWorld());
        } else if (AuratusGem.isActive(this.anchorWindow, player.getUniqueId())) {
            entity2 = player.getLocation().add(0.0, 14.0, 0.0);
            player.sendMessage("\u00a76\u00a7oSky anchor!");
        }
        if (entity2 == null) {
            Location location2 = location.clone().add(vector.clone().multiply(55.0));
            this.launchChainToPoint(player, location2, null);
            return false;
        }
        Entity entity3 = entity2;
        this.launchChainToPoint(player, (Location)entity3, () -> this.lambda$fireChain$1(player, (Location)entity3));
        return true;
    }

    private void yankPlayerTo(Player player, Location location) {
        double d;
        double d2;
        double d3;
        int n;
        double d4;
        if (!player.isOnline() || player.isDead()) {
            return;
        }
        Vector vector = location.toVector().subtract(player.getLocation().toVector());
        double d5 = Math.max(vector.length(), 0.01);
        double d6 = vector.getX();
        double d7 = Math.sqrt(d6 * d6 + (d4 = vector.getZ()) * d4);
        double d8 = d7 > 0.001 ? Math.toDegrees(Math.atan2(vector.getY(), d7)) : 90.0;
        double d9 = Math.max(0.0, Math.min(1.0, (d8 - 25.0) / 20.0));
        double d10 = vector.getY() - 1.45 * (1.0 - d9) + 0.25;
        for (n = 6; n < 56; ++n) {
            double d11 = (1.0 - Math.pow(0.98, n)) / 0.02;
            d3 = (1.0 - Math.pow(0.91, n)) / 0.09;
            d2 = (d10 + 3.92 * ((double)n - d11)) / d11;
            double d12 = d = d7 > 0.001 ? d7 / d3 : 0.0;
            if (d <= 3.9 && Math.abs(d2) <= 3.9) break;
        }
        int n2 = (int)Math.max(8L, Math.min(28L, Math.round(6.0 + d5 * 0.45)));
        int n3 = (int)Math.round((1.0 - d9) * (double)n + d9 * (double)n2);
        while (true) {
            d = (1.0 - Math.pow(0.98, n3)) / 0.02;
            double d13 = (1.0 - Math.pow(0.91, n3)) / 0.09;
            d3 = (d10 + 3.92 * ((double)n3 - d)) / d;
            d2 = d7 / d13;
            if (d2 <= 3.9 || n3 >= 56) break;
            n3 += 2;
        }
        Vector vector2 = d7 > 0.001 ? new Vector(d6 / d7 * d2, d3, d4 / d7 * d2) : new Vector(0.0, d3, 0.0);
        vector2.setX(Math.max(-3.9, Math.min(3.9, vector2.getX())));
        vector2.setY(Math.max(-3.9, Math.min(3.9, vector2.getY())));
        vector2.setZ(Math.max(-3.9, Math.min(3.9, vector2.getZ())));
        player.setVelocity(vector2);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.4f, 0.6f);
        this.slamWindow.put(player.getUniqueId(), System.currentTimeMillis() + 3000L);
        this.watchGroundSlam(player);
    }

    private void launchChainToPoint(final Player player, final Location location, final Runnable runnable) {
        Location location2 = player.getEyeLocation();
        Vector vector = location.toVector().subtract(location2.toVector()).normalize();
        player.getWorld().playSound(location2, Sound.BLOCK_CHAIN_PLACE, 1.2f, 1.1f);
        final ItemStack itemStack = new ItemStack(Material.ECHO_SHARD);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setCustomModelData(Integer.valueOf(12348));
        itemStack.setItemMeta(itemMeta);
        ItemStack itemStack2 = new ItemStack(Material.ECHO_SHARD);
        ItemMeta itemMeta2 = itemStack2.getItemMeta();
        itemMeta2.setCustomModelData(Integer.valueOf(12349));
        itemStack2.setItemMeta(itemMeta2);
        final ArrayList arrayList = new ArrayList();
        Location location3 = location2.clone().add(vector.clone().multiply(0.9));
        location3.setYaw(0.0f);
        location3.setPitch(0.0f);
        final Quaternionf quaternionf = AuratusGem.rotationFor(vector);
        final ItemDisplay itemDisplay2 = (ItemDisplay)player.getWorld().spawn(location3, ItemDisplay.class, itemDisplay -> {
            itemDisplay.setItemStack(itemStack2);
            itemDisplay.setGlowing(true);
            itemDisplay.setBrightness(new Display.Brightness(15, 15));
            itemDisplay.setPersistent(false);
            itemDisplay.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), quaternionf, new Vector3f(0.4f, 0.4f, 0.4f), new Quaternionf()));
            itemDisplay.setTeleportDuration(1);
        });
        new BukkitRunnable(this){
            final Location tip;
            double linkCarry;
            int ticks;
            final /* synthetic */ AuratusGem this$0;
            {
                this.this$0 = auratusGem;
                this.tip = itemDisplay2.getLocation();
                this.linkCarry = 0.0;
                this.ticks = 0;
            }

            public void run() {
                ++this.ticks;
                if (this.ticks > 60 || !player.isOnline() || player.isDead()) {
                    this.dispose(0L);
                    this.cancel();
                    return;
                }
                Vector vector = location.toVector().subtract(this.tip.toVector());
                double d = vector.length();
                double d2 = Math.min(4.5, d);
                if (d > 0.001) {
                    Location location2;
                    double d3;
                    Vector vector2 = vector.multiply(1.0 / d);
                    for (d3 = this.linkCarry; d3 < d2; d3 += 0.75) {
                        location2 = this.tip.clone().add(vector2.clone().multiply(d3));
                        ItemDisplay itemDisplay22 = (ItemDisplay)player.getWorld().spawn(location2, ItemDisplay.class, itemDisplay -> {
                            itemDisplay.setItemStack(itemStack);
                            itemDisplay.setGlowing(true);
                            itemDisplay.setBrightness(new Display.Brightness(15, 15));
                            itemDisplay.setPersistent(false);
                            itemDisplay.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), quaternionf, new Vector3f(0.4f, 0.4f, 0.4f), new Quaternionf()));
                        });
                        arrayList.add(itemDisplay22);
                    }
                    this.linkCarry = d3 - d2;
                    this.tip.add(vector2.multiply(d2));
                    if (itemDisplay2.isValid()) {
                        location2 = this.tip.clone();
                        location2.setYaw(0.0f);
                        location2.setPitch(0.0f);
                        itemDisplay2.teleport(location2);
                    }
                }
                if (this.tip.distanceSquared(location) <= 0.0625) {
                    if (runnable != null) {
                        runnable.run();
                        this.dispose(12L);
                    } else {
                        player.getWorld().playSound(this.tip, Sound.BLOCK_CHAIN_FALL, 1.0f, 0.9f);
                        this.startRetract();
                    }
                    this.cancel();
                }
            }

            private void startRetract() {
                final Location location2 = this.tip;
                new BukkitRunnable(this){
                    int retractTicks = 0;
                    final /* synthetic */ 2 this$1;
                    {
                        this.this$1 = var1_1;
                    }

                    public void run() {
                        if (++this.retractTicks > 60 || !player.isOnline() || player.isDead() || !itemDisplay2.isValid()) {
                            this.this$1.dispose(0L);
                            this.cancel();
                            return;
                        }
                        Location location = player.getEyeLocation();
                        Vector vector = location.toVector().subtract(location2.toVector());
                        double d = vector.length();
                        if (d <= 4.5) {
                            this.this$1.dispose(0L);
                            this.cancel();
                            return;
                        }
                        location2.add(vector.multiply(4.5 / d));
                        Location location22 = location2.clone();
                        location22.setYaw(0.0f);
                        location22.setPitch(0.0f);
                        itemDisplay2.teleport(location22);
                        double d2 = location2.distanceSquared(location);
                        arrayList.removeIf(itemDisplay -> {
                            if (!itemDisplay.isValid()) {
                                return true;
                            }
                            if (itemDisplay.getLocation().distanceSquared(location) >= d2) {
                                itemDisplay.remove();
                                return true;
                            }
                            return false;
                        });
                    }
                }.runTaskTimer((Plugin)this.this$0.plugin, 1L, 1L);
            }

            private void dispose(long l) {
                ArrayList<ItemDisplay> arrayList2 = new ArrayList<ItemDisplay>(arrayList);
                arrayList2.add(itemDisplay2);
                Runnable runnable2 = () -> {
                    for (ItemDisplay itemDisplay : arrayList2) {
                        if (!itemDisplay.isValid()) continue;
                        itemDisplay.remove();
                    }
                };
                if (l <= 0L) {
                    runnable2.run();
                } else {
                    Bukkit.getScheduler().runTaskLater((Plugin)this.this$0.plugin, runnable2, l);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 1L, 1L);
    }

    private void launchChainAt(final Player player, final LivingEntity livingEntity) {
        Location location = player.getEyeLocation();
        Vector vector = location.getDirection().normalize();
        player.getWorld().playSound(location, Sound.BLOCK_CHAIN_PLACE, 1.2f, 1.1f);
        final ItemStack itemStack = new ItemStack(Material.ECHO_SHARD);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setCustomModelData(Integer.valueOf(12348));
        itemStack.setItemMeta(itemMeta);
        ItemStack itemStack2 = new ItemStack(Material.ECHO_SHARD);
        ItemMeta itemMeta2 = itemStack2.getItemMeta();
        itemMeta2.setCustomModelData(Integer.valueOf(12349));
        itemStack2.setItemMeta(itemMeta2);
        final ArrayList arrayList = new ArrayList();
        Location location2 = location.clone().add(vector.clone().multiply(0.9));
        location2.setYaw(0.0f);
        location2.setPitch(0.0f);
        final ItemDisplay itemDisplay2 = (ItemDisplay)player.getWorld().spawn(location2, ItemDisplay.class, itemDisplay -> {
            itemDisplay.setItemStack(itemStack2);
            itemDisplay.setGlowing(true);
            itemDisplay.setBrightness(new Display.Brightness(15, 15));
            itemDisplay.setPersistent(false);
            itemDisplay.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), AuratusGem.rotationFor(vector), new Vector3f(0.4f, 0.4f, 0.4f), new Quaternionf()));
            itemDisplay.setTeleportDuration(1);
        });
        new BukkitRunnable(this){
            final Location tip;
            double linkCarry;
            int ticks;
            final /* synthetic */ AuratusGem this$0;
            {
                this.this$0 = auratusGem;
                this.tip = itemDisplay2.getLocation();
                this.linkCarry = 0.0;
                this.ticks = 0;
            }

            public void run() {
                ++this.ticks;
                if (this.ticks > 60 || !livingEntity.isValid() || livingEntity.isDead() || !player.isOnline() || livingEntity.getWorld() != this.tip.getWorld()) {
                    this.dispose(0L);
                    this.cancel();
                    return;
                }
                Location location = livingEntity.getLocation().add(0.0, 1.0, 0.0);
                Vector vector = location.toVector().subtract(this.tip.toVector());
                double d = vector.length();
                double d2 = Math.min(4.5, d);
                if (d > 0.001) {
                    Location location2;
                    double d3;
                    Vector vector2 = vector.multiply(1.0 / d);
                    Quaternionf quaternionf = AuratusGem.rotationFor(vector2);
                    for (d3 = this.linkCarry; d3 < d2; d3 += 0.75) {
                        location2 = this.tip.clone().add(vector2.clone().multiply(d3));
                        ItemDisplay itemDisplay22 = (ItemDisplay)player.getWorld().spawn(location2, ItemDisplay.class, itemDisplay -> {
                            itemDisplay.setItemStack(itemStack);
                            itemDisplay.setGlowing(true);
                            itemDisplay.setBrightness(new Display.Brightness(15, 15));
                            itemDisplay.setPersistent(false);
                            itemDisplay.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), quaternionf, new Vector3f(0.4f, 0.4f, 0.4f), new Quaternionf()));
                        });
                        arrayList.add(itemDisplay22);
                    }
                    this.linkCarry = d3 - d2;
                    this.tip.add(vector2.multiply(d2));
                    if (itemDisplay2.isValid()) {
                        location2 = this.tip.clone();
                        location2.setYaw(0.0f);
                        location2.setPitch(0.0f);
                        itemDisplay2.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), quaternionf, new Vector3f(0.4f, 0.4f, 0.4f), new Quaternionf()));
                        itemDisplay2.teleport(location2);
                    }
                }
                if (this.tip.distanceSquared(livingEntity.getLocation().add(0.0, 1.0, 0.0)) <= 1.44) {
                    this.grapple();
                    this.dispose(12L);
                    this.cancel();
                }
            }

            private void grapple() {
                livingEntity.damage(6.0, (Entity)player);
                Vector vector = player.getLocation().toVector().subtract(livingEntity.getLocation().toVector());
                double d = Math.max(vector.length(), 0.01);
                vector.normalize().multiply(Math.min(3.2, 0.8 + d * 0.42));
                vector.setY(Math.min(1.3, Math.abs(vector.getY()) + 0.4));
                livingEntity.setVelocity(vector);
                player.getWorld().playSound(livingEntity.getLocation(), Sound.BLOCK_CHAIN_HIT, 1.4f, 0.7f);
            }

            private void dispose(long l) {
                ArrayList<ItemDisplay> arrayList2 = new ArrayList<ItemDisplay>(arrayList);
                arrayList2.add(itemDisplay2);
                Runnable runnable = () -> {
                    for (ItemDisplay itemDisplay : arrayList2) {
                        if (!itemDisplay.isValid()) continue;
                        itemDisplay.remove();
                    }
                };
                if (l <= 0L) {
                    runnable.run();
                } else {
                    Bukkit.getScheduler().runTaskLater((Plugin)this.this$0.plugin, runnable, l);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 1L, 1L);
    }

    private static Quaternionf rotationFor(Vector vector) {
        float f = (float)Math.atan2(vector.getX(), vector.getZ());
        float f2 = (float)(-Math.asin(Math.max(-1.0, Math.min(1.0, vector.getY()))));
        return new Quaternionf().rotationY(f).rotateX(f2);
    }

    private void watchGroundSlam(final Player player) {
        new BukkitRunnable(this){
            int ticks = 0;
            final /* synthetic */ AuratusGem this$0;
            {
                this.this$0 = auratusGem;
            }

            public void run() {
                if (++this.ticks <= 70 && player.isOnline() && !player.isDead() && AuratusGem.isActive(this.this$0.slamWindow, player.getUniqueId())) {
                    if (this.ticks > 6 && player.isOnGround()) {
                        if (player.isSneaking()) {
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
                            player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation(), 1);
                            this.this$0.spawnSmashDecal(player.getLocation());
                            for (LivingEntity livingEntity : player.getLocation().getNearbyLivingEntities(4.0)) {
                                if (livingEntity == player) continue;
                                livingEntity.damage(6.0, (Entity)player);
                                livingEntity.setVelocity(livingEntity.getVelocity().add(new Vector(0.0, 0.8, 0.0)));
                            }
                        }
                        this.this$0.slamWindow.remove(player.getUniqueId());
                        this.cancel();
                    }
                } else {
                    this.this$0.slamWindow.remove(player.getUniqueId());
                    this.cancel();
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 2L, 1L);
    }

    private void spawnSmashDecal(Location location) {
        ItemStack itemStack = new ItemStack(Material.ECHO_SHARD);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setCustomModelData(Integer.valueOf(12345));
        itemStack.setItemMeta(itemMeta);
        ItemDisplay itemDisplay2 = (ItemDisplay)location.getWorld().spawn(location.clone().add(0.0, 0.15, 0.0), ItemDisplay.class, itemDisplay -> {
            itemDisplay.setItemStack(itemStack);
            itemDisplay.setBrightness(new Display.Brightness(15, 15));
            itemDisplay.setPersistent(false);
            itemDisplay.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), new Quaternionf().rotationX((float)Math.toRadians(90.0)), new Vector3f(3.0f, 3.0f, 3.0f), new Quaternionf()));
        });
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (itemDisplay2.isValid()) {
                itemDisplay2.remove();
            }
        }, 15L);
    }

    public void applyPassives(Player player, int n) {
        if (player.isSneaking()) {
            int n2 = 40;
            try {
                n2 = this.api.getConfigManager().getPassiveUpdateInterval();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, n2 + 20, 1, true, false, true));
        }
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onFall(EntityDamageEvent entityDamageEvent) {
        Player player;
        Entity entity;
        if (entityDamageEvent.getCause() == EntityDamageEvent.DamageCause.FALL && (entity = entityDamageEvent.getEntity()) instanceof Player && this.plugin.holds(player = (Player)entity, "auratus")) {
            entityDamageEvent.setDamage(entityDamageEvent.getDamage() * 0.2);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onHaulingStrike(EntityDamageByEntityEvent entityDamageByEntityEvent) {
        Entity entity;
        if (entityDamageByEntityEvent.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK && (entity = entityDamageByEntityEvent.getDamager()) instanceof Player) {
            Player player = (Player)entity;
            entity = entityDamageByEntityEvent.getEntity();
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity)entity;
                if (this.plugin.holds(player, "auratus")) {
                    Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                        if (livingEntity.isValid() && !livingEntity.isDead()) {
                            Vector vector = player.getLocation().toVector().subtract(livingEntity.getLocation().toVector());
                            vector.setY(0);
                            if (!(vector.lengthSquared() < 0.01)) {
                                livingEntity.setVelocity(vector.normalize().multiply(0.5).setY(0.1));
                            }
                        }
                    }, 1L);
                }
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onArmorDamage(PlayerItemDamageEvent playerItemDamageEvent) {
        Player player = playerItemDamageEvent.getPlayer();
        if (this.plugin.holds(player, "auratus") && playerItemDamageEvent.getItem().getType().name().matches(".*(_HELMET|_CHESTPLATE|_LEGGINGS|_BOOTS)$") && ThreadLocalRandom.current().nextDouble() < 0.1) {
            playerItemDamageEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onAegisParry(EntityDamageByEntityEvent entityDamageByEntityEvent) {
        LivingEntity livingEntity;
        Object object = entityDamageByEntityEvent.getEntity();
        if (!(object instanceof Player)) {
            return;
        }
        Player player = (Player)object;
        object = player.getUniqueId();
        if (!AuratusGem.isActive(this.aegisWindow, (UUID)object)) {
            return;
        }
        entityDamageByEntityEvent.setCancelled(true);
        this.aegisWindow.remove(object);
        LivingEntity livingEntity2 = null;
        Entity entity = entityDamageByEntityEvent.getDamager();
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity3;
            livingEntity2 = livingEntity3 = (LivingEntity)entity;
        } else {
            Projectile projectile;
            entity = entityDamageByEntityEvent.getDamager();
            if (entity instanceof Projectile && (entity = (projectile = (Projectile)entity).getShooter()) instanceof LivingEntity) {
                livingEntity2 = livingEntity = (LivingEntity)entity;
            }
        }
        double d = Math.max(entityDamageByEntityEvent.getDamage(), 2.0);
        if (livingEntity2 != null && livingEntity2 != player) {
            livingEntity2.damage(d, (Entity)player);
            if (livingEntity2 instanceof Player) {
                livingEntity = (Player)livingEntity2;
                livingEntity.damageItemStack(EquipmentSlot.HAND, 40);
            }
        }
        double d2 = player.getAttribute(Attribute.MAX_HEALTH) != null ? player.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;
        player.setHealth(Math.min(d2, player.getHealth() + 5.0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 2, false, true, true));
        PotionEffectType potionEffectType = (PotionEffectType)Registry.EFFECT.get(NamespacedKey.minecraft((String)"weaving"));
        if (potionEffectType != null) {
            player.addPotionEffect(new PotionEffect(potionEffectType, 200, 0, false, true, true));
        }
        this.anchorWindow.put((UUID)object, System.currentTimeMillis() + 5000L);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.4f, 1.3f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.4f, 0.8f);
        this.ring(player.getLocation().add(0.0, 1.0, 0.0), 1.6, Color.fromRGB((int)255, (int)220, (int)90));
        player.sendMessage("\u00a76\u00a7oParried!");
    }

    private static boolean isActive(Map<UUID, Long> map, UUID uUID) {
        Long l = map.get(uUID);
        if (l == null) {
            return false;
        }
        if (l < System.currentTimeMillis()) {
            map.remove(uUID);
            return false;
        }
        return true;
    }

    private void spawnChain(Player player, Location location, Location location2) {
        Vector vector = location2.toVector().subtract(location.toVector());
        double d = vector.length();
        if (d < 0.5) {
            return;
        }
        Vector vector2 = vector.clone().normalize();
        Quaternionf quaternionf = AuratusGem.rotationFor(vector2);
        ItemStack itemStack = new ItemStack(Material.ECHO_SHARD);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setCustomModelData(Integer.valueOf(12348));
        itemStack.setItemMeta(itemMeta);
        ItemStack itemStack2 = new ItemStack(Material.ECHO_SHARD);
        ItemMeta itemMeta2 = itemStack2.getItemMeta();
        itemMeta2.setCustomModelData(Integer.valueOf(12349));
        itemStack2.setItemMeta(itemMeta2);
        ArrayList<ItemDisplay> arrayList = new ArrayList<ItemDisplay>();
        double d2 = 0.15000000223517418;
        double d3 = Math.max(d2, Math.floor(0.8 / d2) * d2);
        for (double d4 = Math.floor(0.9 / d2) * d2; d4 < d; d4 += d3) {
            boolean bl = d4 + d3 >= d;
            Location location3 = location.clone().add(vector2.clone().multiply(d4));
            location3.setYaw(0.0f);
            location3.setPitch(0.0f);
            ItemStack itemStack3 = bl ? itemStack2 : itemStack;
            ItemDisplay itemDisplay2 = (ItemDisplay)player.getWorld().spawn(location3, ItemDisplay.class, itemDisplay -> {
                itemDisplay.setItemStack(itemStack3);
                itemDisplay.setGlowing(true);
                itemDisplay.setBrightness(new Display.Brightness(15, 15));
                itemDisplay.setPersistent(false);
                itemDisplay.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), quaternionf, new Vector3f(0.4f, 0.4f, 0.4f), new Quaternionf()));
            });
            arrayList.add(itemDisplay2);
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            for (ItemDisplay itemDisplay : arrayList) {
                if (!itemDisplay.isValid()) continue;
                itemDisplay.remove();
            }
        }, 12L);
    }

    private void ring(Location location, double d, Color color) {
        Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.5f);
        for (int i = 0; i < 24; ++i) {
            double d2 = Math.PI * 2 * (double)i / 24.0;
            location.getWorld().spawnParticle(Particle.DUST, location.clone().add(Math.cos(d2) * d, 0.0, Math.sin(d2) * d), 1, 0.0, 0.0, 0.0, 0.0, (Object)dustOptions);
        }
    }

    public void cleanup(Player player) {
        UUID uUID = player.getUniqueId();
        this.chainUses.remove(uUID);
        this.aegisWindow.remove(uUID);
        this.anchorWindow.remove(uUID);
        this.slamWindow.remove(uUID);
    }

    private /* synthetic */ void lambda$fireChain$1(Player player, Location location) {
        this.yankPlayerTo(player, location);
    }
}

