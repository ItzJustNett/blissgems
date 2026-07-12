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
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Display$Billboard
 *  org.bukkit.entity.Display$Brightness
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.ItemDisplay
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.bukkit.event.entity.EntityPotionEffectEvent
 *  org.bukkit.event.entity.EntityPotionEffectEvent$Action
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitRunnable
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class HereticGem
implements GemAbilityHandler,
GemPassiveHandler,
Listener {
    private static final String KEY_SAWS = "heretic-bloodsaws";
    private static final String KEY_LINK = "heretic-bloodlink";
    private static final int CD_SAWS = 25;
    private static final int CD_LINK = 60;
    private static final long SAW_WINDOW_MS = 4000L;
    private static final long BLEED_MS = 8000L;
    private static final long LINK_MS = 15000L;
    private static final double MAX_HIT = 13.0;
    private final BlissMythics plugin;
    private final BlissGemsAPI api;
    private final Map<UUID, Long> firstSawAt = new HashMap<UUID, Long>();
    private final Map<UUID, Long> bleeding = new HashMap<UUID, Long>();
    private final Map<UUID, Set<UUID>> links = new HashMap<UUID, Set<UUID>>();
    private final Map<UUID, Long> linkExpiry = new HashMap<UUID, Long>();
    private final Set<UUID> mirroring = new HashSet<UUID>();
    private final Set<UUID> crashing = new HashSet<UUID>();

    public HereticGem(BlissMythics blissMythics, BlissGemsAPI blissGemsAPI) {
        this.plugin = blissMythics;
        this.api = blissGemsAPI;
        new BukkitRunnable(){

            public void run() {
                HereticGem.this.bleedTick();
            }
        }.runTaskTimer((Plugin)blissMythics, 10L, 10L);
    }

    public boolean chargeWindowActive(Player player) {
        Long l = this.firstSawAt.get(player.getUniqueId());
        return l != null && System.currentTimeMillis() - l <= 4000L;
    }

    public void onPrimary(Player player, int n) {
        UUID uUID = player.getUniqueId();
        if (this.api.getAbilityManager().isOnCooldown(player, KEY_SAWS)) {
            int n2 = this.api.getAbilityManager().getRemainingCooldown(player, KEY_SAWS);
            player.sendMessage("\u00a74Bloodsaws \u00a77on cooldown: \u00a7c" + n2 + "s");
            return;
        }
        long l = System.currentTimeMillis();
        Long l2 = this.firstSawAt.get(uUID);
        this.launchSaw(player);
        if (l2 == null || l - l2 > 4000L) {
            this.firstSawAt.put(uUID, l);
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                Long l2 = this.firstSawAt.get(uUID);
                if (l2 != null && l2 == l) {
                    this.firstSawAt.remove(uUID);
                    this.api.getAbilityManager().setCooldown(player, KEY_SAWS, 25);
                }
            }, 80L);
        } else {
            this.firstSawAt.remove(uUID);
            this.spore(player);
            this.api.getAbilityManager().setCooldown(player, KEY_SAWS, 25);
        }
    }

    public void onSecondary(Player player, int n) {
        if (this.api.getAbilityManager().isOnCooldown(player, KEY_LINK)) {
            int n2 = this.api.getAbilityManager().getRemainingCooldown(player, KEY_LINK);
            player.sendMessage("\u00a74Bloodlinking \u00a77on cooldown: \u00a7c" + n2 + "s");
            return;
        }
        this.api.getAbilityManager().setCooldown(player, KEY_LINK, 60);
        player.sendMessage("\u00a74\u00a7oBloodlinking!");
        this.bloodlinkSequence(player);
    }

    private void launchSaw(final Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.6f);
        final Vector[] vectorArray = new Vector[]{player.getEyeLocation().getDirection().normalize()};
        final Location[] locationArray = new Location[]{player.getEyeLocation().add(vectorArray[0].clone().multiply(1.2))};
        final HashSet hashSet = new HashSet();
        final Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB((int)140, (int)0, (int)0), 1.6f);
        ItemStack itemStack = new ItemStack(Material.ECHO_SHARD);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setCustomModelData(Integer.valueOf(12347));
        itemStack.setItemMeta(itemMeta);
        final ItemDisplay itemDisplay2 = (ItemDisplay)player.getWorld().spawn(locationArray[0], ItemDisplay.class, itemDisplay -> {
            itemDisplay.setItemStack(itemStack);
            itemDisplay.setBillboard(Display.Billboard.CENTER);
            itemDisplay.setBrightness(new Display.Brightness(15, 15));
            itemDisplay.setPersistent(false);
            itemDisplay.setTransformation(new Transformation(new Vector3f(0.0f, 0.0f, 0.0f), new Quaternionf(), new Vector3f(1.1f, 1.1f, 1.1f), new Quaternionf()));
            itemDisplay.setTeleportDuration(1);
        });
        new BukkitRunnable(this){
            int life = 0;
            int bounces = 0;

            public void cancel() {
                if (itemDisplay2.isValid()) {
                    itemDisplay2.remove();
                }
                super.cancel();
            }

            public void run() {
                if (++this.life > 60 || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                for (int i = 0; i < 2; ++i) {
                    Location location = locationArray[0].clone().add(vectorArray[0].clone().multiply(0.45));
                    Block block = location.getBlock();
                    if (block.getType().isSolid()) {
                        if (++this.bounces > 3) {
                            this.cancel();
                            return;
                        }
                        Vector vector = vectorArray[0];
                        Location location2 = locationArray[0].clone().add(new Vector(vector.getX(), 0.0, 0.0).multiply(0.45));
                        Location location3 = locationArray[0].clone().add(new Vector(0.0, vector.getY(), 0.0).multiply(0.45));
                        Location location4 = locationArray[0].clone().add(new Vector(0.0, 0.0, vector.getZ()).multiply(0.45));
                        if (location2.getBlock().getType().isSolid()) {
                            vector.setX(-vector.getX());
                        }
                        if (location3.getBlock().getType().isSolid()) {
                            vector.setY(-vector.getY());
                        }
                        if (location4.getBlock().getType().isSolid()) {
                            vector.setZ(-vector.getZ());
                        }
                        if ((location = locationArray[0].clone().add(vector.clone().multiply(0.45))).getBlock().getType().isSolid()) {
                            this.cancel();
                            return;
                        }
                        locationArray[0].getWorld().playSound(locationArray[0], Sound.BLOCK_ANVIL_LAND, 0.4f, 1.8f);
                    }
                    locationArray[0] = location;
                    locationArray[0].getWorld().spawnParticle(Particle.DUST, locationArray[0], 2, 0.2, 0.2, 0.2, 0.0, (Object)dustOptions);
                    for (Location location2 : locationArray[0].getNearbyLivingEntities(1.3)) {
                        if (location2 == player || hashSet.contains(location2.getUniqueId())) continue;
                        hashSet.add(location2.getUniqueId());
                        location2.damage(6.0, (Entity)player);
                        location2.getWorld().playSound(location2.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.7f);
                    }
                }
                if (itemDisplay2.isValid()) {
                    itemDisplay2.teleport(locationArray[0]);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    private void spore(final Player player) {
        Block block = player.getTargetBlockExact(15);
        final Location location = block != null ? block.getLocation().add(0.5, 1.0, 0.5) : player.getLocation();
        player.getWorld().playSound(location, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.4f, 0.5f);
        final Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB((int)170, (int)10, (int)10), 2.0f);
        new BukkitRunnable(this){
            int life = 0;
            final /* synthetic */ HereticGem this$0;
            {
                this.this$0 = hereticGem;
            }

            public void run() {
                if (++this.life > 12) {
                    this.cancel();
                    return;
                }
                for (int i = 0; i < 25; ++i) {
                    double d = Math.random() * Math.PI * 2.0;
                    double d2 = Math.random() * 1.2;
                    Location location2 = location.clone().add(Math.cos(d) * d2, Math.random() * 2.5, Math.sin(d) * d2);
                    location.getWorld().spawnParticle(Particle.DUST, location2, 1, 0.0, 0.0, 0.0, 0.0, (Object)dustOptions);
                }
                location.getWorld().spawnParticle(Particle.LAVA, location, 2, 0.5, 0.3, 0.5, 0.0);
                long l = System.currentTimeMillis() + 8000L;
                for (Player player2 : location.getNearbyPlayers(5.0)) {
                    if (player2 == player) continue;
                    this.this$0.bleeding.put(player2.getUniqueId(), l);
                    player2.sendMessage("\u00a74\u00a7oYou are bleeding! Damage taken is amplified!");
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 10L);
    }

    private void bloodlinkSequence(final Player player) {
        final HashSet hashSet = new HashSet();
        this.crashing.add(player.getUniqueId());
        new BukkitRunnable(this){
            int ticks = 0;
            int airTicks = 0;
            int hops = 0;
            int phase = 0;
            final /* synthetic */ HereticGem this$0;
            {
                this.this$0 = hereticGem;
            }

            public void cancel() {
                this.this$0.crashing.remove(player.getUniqueId());
                Bukkit.getScheduler().runTaskLater((Plugin)this.this$0.plugin, () -> this.this$0.crashing.remove(player.getUniqueId()), 30L);
                this.finish();
                super.cancel();
            }

            public void run() {
                if (++this.ticks > 200 || !player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }
                if (this.phase == 0) {
                    Vector vector = new Vector(0.0, 0.95, 0.0);
                    Player player2 = this.nearestTarget();
                    if (player2 != null) {
                        Vector vector2 = player2.getLocation().toVector().subtract(player.getLocation().toVector());
                        vector2.setY(0);
                        if (vector2.lengthSquared() > 0.01) {
                            vector.add(vector2.normalize().multiply(0.5));
                        }
                    }
                    player.setVelocity(vector);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.2f, 0.5f);
                    this.phase = 1;
                    this.airTicks = 0;
                    return;
                }
                if (this.ticks % 2 == 0) {
                    player.getWorld().spawnParticle(Particle.DUST, player.getLocation(), 8, 0.3, 0.3, 0.3, 0.0, (Object)new Particle.DustOptions(Color.fromRGB((int)120, (int)0, (int)0), 1.4f));
                }
                ++this.airTicks;
                if (this.phase == 1 && this.airTicks >= 9) {
                    Vector vector = new Vector(0.0, -2.8, 0.0);
                    Player player3 = this.nearestTarget();
                    if (player3 != null) {
                        Vector vector3 = player3.getLocation().toVector().subtract(player.getLocation().toVector());
                        vector3.setY(0);
                        if (vector3.lengthSquared() > 0.01) {
                            vector.add(vector3.normalize().multiply(0.6));
                        }
                    }
                    player.setVelocity(vector);
                    this.phase = 2;
                    return;
                }
                if (this.phase == 2 && this.airTicks >= 12 && player.isOnGround()) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
                    player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 2, 0.5, 0.2, 0.5, 0.0);
                    for (LivingEntity livingEntity : player.getLocation().getNearbyLivingEntities(4.5)) {
                        if (livingEntity == player) continue;
                        livingEntity.damage(7.0, (Entity)player);
                        livingEntity.setVelocity(livingEntity.getVelocity().add(new Vector(0.0, 0.5, 0.0)));
                        if (!(livingEntity instanceof Player)) continue;
                        Player player4 = (Player)livingEntity;
                        hashSet.add(player4.getUniqueId());
                    }
                    if (++this.hops >= 3) {
                        this.cancel();
                        return;
                    }
                    this.phase = 0;
                }
            }

            private Player nearestTarget() {
                Player player3 = null;
                double d = 196.0;
                for (Player player2 : player.getLocation().getNearbyPlayers(14.0)) {
                    double d2;
                    if (player2 == player || !((d2 = player2.getLocation().distanceSquared(player.getLocation())) < d)) continue;
                    d = d2;
                    player3 = player2;
                }
                return player3;
            }

            private void finish() {
                if (hashSet.size() >= 2) {
                    long l = System.currentTimeMillis() + 15000L;
                    for (UUID uUID : hashSet) {
                        HashSet hashSet2 = new HashSet(hashSet);
                        hashSet2.remove(uUID);
                        this.this$0.links.put(uUID, hashSet2);
                        this.this$0.linkExpiry.put(uUID, l);
                        Player player2 = Bukkit.getPlayer((UUID)uUID);
                        if (player2 == null) continue;
                        player2.sendMessage("\u00a74\u00a7oYou have been bloodlinked! Damage you take is shared.");
                    }
                    player.sendMessage("\u00a74\u00a7o" + hashSet.size() + " players bloodlinked!");
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onCrashFall(EntityDamageEvent entityDamageEvent) {
        if (entityDamageEvent.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        Entity entity = entityDamageEvent.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        if (this.crashing.contains(player.getUniqueId())) {
            entityDamageEvent.setCancelled(true);
        }
    }

    public void applyPassives(Player player, int n) {
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onBleedDamage(EntityDamageEvent entityDamageEvent) {
        Object object = entityDamageEvent.getEntity();
        if (!(object instanceof Player)) {
            return;
        }
        Player player = (Player)object;
        object = this.bleeding.get(player.getUniqueId());
        if (object == null) {
            return;
        }
        if ((Long)object < System.currentTimeMillis()) {
            this.bleeding.remove(player.getUniqueId());
            return;
        }
        entityDamageEvent.setDamage(entityDamageEvent.getDamage() * 2.0);
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onAutoCrit(EntityDamageByEntityEvent entityDamageByEntityEvent) {
        if (entityDamageByEntityEvent.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }
        Entity entity = entityDamageByEntityEvent.getDamager();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        if (!this.plugin.holds(player, "heretic")) {
            return;
        }
        entityDamageByEntityEvent.setDamage(entityDamageByEntityEvent.getDamage() * 1.5);
        entityDamageByEntityEvent.getEntity().getWorld().spawnParticle(Particle.CRIT, entityDamageByEntityEvent.getEntity().getLocation().add(0.0, 1.0, 0.0), 12, 0.4, 0.4, 0.4, 0.3);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBloodHardening(EntityDamageEvent entityDamageEvent) {
        Entity entity = entityDamageEvent.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        if (!this.plugin.holds(player, "heretic")) {
            return;
        }
        double d = entityDamageEvent.getFinalDamage();
        if (d > 13.0 && d > 0.0) {
            entityDamageEvent.setDamage(entityDamageEvent.getDamage() * (13.0 / d));
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onStrengthGain(EntityPotionEffectEvent entityPotionEffectEvent) {
        Entity entity = entityPotionEffectEvent.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        if (entityPotionEffectEvent.getModifiedType() != PotionEffectType.STRENGTH) {
            return;
        }
        if (entityPotionEffectEvent.getAction() == EntityPotionEffectEvent.Action.REMOVED || entityPotionEffectEvent.getAction() == EntityPotionEffectEvent.Action.CLEARED) {
            return;
        }
        if (!this.plugin.holds(player, "heretic")) {
            return;
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            int n;
            PotionEffect potionEffect = player.getPotionEffect(PotionEffectType.STRENGTH);
            if (potionEffect == null || potionEffect.getDuration() == -1) {
                return;
            }
            int n2 = n = potionEffect.getAmplifier() >= 1 ? 9600 : 19200;
            if (potionEffect.getDuration() < n - 100) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, n, potionEffect.getAmplifier(), potionEffect.isAmbient(), potionEffect.hasParticles(), potionEffect.hasIcon()));
            }
        }, 1L);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onLinkedDamage(EntityDamageEvent entityDamageEvent) {
        Object object = entityDamageEvent.getEntity();
        if (!(object instanceof Player)) {
            return;
        }
        Player player = (Player)object;
        object = player.getUniqueId();
        if (this.mirroring.contains(object)) {
            return;
        }
        Set<UUID> set = this.links.get(object);
        if (set == null) {
            return;
        }
        Long l = this.linkExpiry.get(object);
        if (l == null || l < System.currentTimeMillis()) {
            this.links.remove(object);
            this.linkExpiry.remove(object);
            return;
        }
        double d = entityDamageEvent.getFinalDamage();
        if (d <= 0.0) {
            return;
        }
        for (UUID uUID : set) {
            Player player2 = Bukkit.getPlayer((UUID)uUID);
            if (player2 == null || player2.isDead() || this.mirroring.contains(uUID)) continue;
            this.mirroring.add(uUID);
            try {
                player2.damage(d);
                player2.getWorld().spawnParticle(Particle.DUST, player2.getLocation().add(0.0, 1.0, 0.0), 10, 0.3, 0.5, 0.3, 0.0, (Object)new Particle.DustOptions(Color.fromRGB((int)150, (int)0, (int)0), 1.5f));
            }
            finally {
                this.mirroring.remove(uUID);
            }
        }
    }

    private void bleedTick() {
        long l = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> iterator = this.bleeding.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (entry.getValue() < l) {
                iterator.remove();
                continue;
            }
            Player player = Bukkit.getPlayer((UUID)entry.getKey());
            if (player == null || !player.isOnline()) continue;
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 4, 0.25, 0.5, 0.25, 0.0, (Object)new Particle.DustOptions(Color.fromRGB((int)160, (int)0, (int)0), 1.2f));
        }
    }

    public void cleanup(Player player) {
        UUID uUID = player.getUniqueId();
        this.firstSawAt.remove(uUID);
    }
}

