package dev.xoperr.blissmythics;

import dev.xoperr.blissgems.api.BlissGemsAPI;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.api.GemPassiveHandler;
import dev.xoperr.blissgems.utils.CustomItemManager;
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
import org.bukkit.Particle.DustOptions;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class AuratusGem implements GemAbilityHandler, GemPassiveHandler, Listener {
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
   private final Map<UUID, List<Long>> chainUses = new HashMap<>();
   private final Map<UUID, Long> aegisWindow = new HashMap<>();
   private final Map<UUID, Long> anchorWindow = new HashMap<>();
   private final Map<UUID, Long> slamWindow = new HashMap<>();

   private final double perforatorDamage;
   private final double slamDamage;
   private final double aegisHeal;
   private final int aegisCooldown;

   public AuratusGem(BlissMythics var1, BlissGemsAPI var2) {
      this.plugin = var1;
      this.api = var2;
      this.perforatorDamage = var1.getConfig().getDouble("auratus.perforator-damage", 6.0);
      this.slamDamage = var1.getConfig().getDouble("auratus.slam-damage", 12.0);
      this.aegisHeal = var1.getConfig().getDouble("auratus.aegis-heal", 5.0);
      this.aegisCooldown = var1.getConfig().getInt("auratus.cooldowns.aegis", 45);
      Bukkit.getScheduler().runTaskTimer(var1, this::hasteTick, 20L, 20L);
   }

   // Grants Haste II while sneaking and holding Auratus in EITHER hand. The BlissGems
   // passive system only runs for offhand gems, so this self-contained task makes the
   // sneak-haste work while the gem is used as a main-hand weapon too.
   private void hasteTick() {
      int var1;
      try {
         var1 = this.api.getConfigManager().getPassiveUpdateInterval() + 20;
      } catch (Throwable var4) {
         var1 = 60;
      }

      for (Player var3 : Bukkit.getOnlinePlayers()) {
         if (!var3.isDead() && var3.isSneaking() && this.holdingAuratus(var3)) {
            var3.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, var1, 1, true, false, true));
         }
      }
   }

   private boolean holdingAuratus(Player var1) {
      return this.isAuratusItem(var1.getInventory().getItemInMainHand())
         || this.isAuratusItem(var1.getInventory().getItemInOffHand());
   }

   private boolean isAuratusItem(ItemStack var1) {
      if (var1 == null || var1.getType().isAir()) {
         return false;
      } else {
         String var2 = CustomItemManager.getIdByItem(var1);
         return var2 != null && "auratus".equals(this.api.getGemRegistry().gemIdFromItemId(var2));
      }
   }

   public int chainCharges(Player var1) {
      List<Long> var2 = this.chainUses.get(var1.getUniqueId());
      if (var2 == null) {
         return 2;
      } else {
         long var3 = System.currentTimeMillis();
         var2.removeIf(var2x -> var3 - var2x >= 8000L);
         return Math.max(0, 2 - var2.size());
      }
   }

   public int nextChainChargeIn(Player var1) {
      List<Long> var2 = this.chainUses.get(var1.getUniqueId());
      if (var2 != null && !var2.isEmpty()) {
         long var3 = System.currentTimeMillis();
         long var5 = Long.MAX_VALUE;

         for (long var8 : var2) {
            var5 = Math.min(var5, var8 + 8000L - var3);
         }

         return (int)Math.max(0L, (var5 + 999L) / 1000L);
      } else {
         return 0;
      }
   }

   public void onPrimary(Player var1, int var2) {
      if (this.chainCharges(var1) <= 0) {
         var1.sendMessage("§6Venerated Perforators §7recharging: §c" + this.nextChainChargeIn(var1) + "s");
      } else if (this.fireChain(var1)) {
         this.chainUses.computeIfAbsent(var1.getUniqueId(), var0 -> new ArrayList<>()).add(System.currentTimeMillis());
      }
   }

   public void onSecondary(Player var1, int var2) {
      if (this.api.getAbilityManager().isOnCooldown(var1, "auratus-aegis")) {
         int var3 = this.api.getAbilityManager().getRemainingCooldown(var1, "auratus-aegis");
         var1.sendMessage("§6Echoing Aegis §7on cooldown: §c" + var3 + "s");
      } else {
         this.api.getAbilityManager().setCooldown(var1, "auratus-aegis", aegisCooldown);
         this.aegisWindow.put(var1.getUniqueId(), System.currentTimeMillis() + 1500L);
         var1.getWorld().playSound(var1.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.2F, 1.6F);
         this.throwParryWeb(var1);
         var1.sendMessage("§6§oEchoing Aegis! Parry window open...");
      }
   }

   private void throwParryWeb(Player var1) {
      ItemStack var2 = new ItemStack(Material.ECHO_SHARD);
      ItemMeta var3 = var2.getItemMeta();
      var3.setCustomModelData(12346);
      var2.setItemMeta(var3);
      Location var4 = var1.getEyeLocation();
      final Vector var5 = var4.getDirection().normalize();
      Location var6 = var4.clone().add(var5.clone().multiply(1.3));
      final ItemDisplay var7 = (ItemDisplay)var1.getWorld()
         .spawn(
            var6,
            ItemDisplay.class,
            var1x -> {
               var1x.setItemStack(var2);
               var1x.setBillboard(Billboard.CENTER);
               var1x.setBrightness(new Brightness(15, 15));
               var1x.setPersistent(false);
               var1x.setTransformation(
                  new Transformation(
                     new Vector3f(0.0F, 0.0F, 0.0F),
                     new Quaternionf().rotationX((float)Math.toRadians(90.0)),
                     new Vector3f(1.4F, 1.4F, 1.4F),
                     new Quaternionf()
                  )
               );
               var1x.setTeleportDuration(2);
            }
         );
      (new BukkitRunnable() {
         int ticks = 0;
         final Vector step = var5.clone().multiply(0.45);

         public void run() {
            if (++this.ticks <= 24 && var7.isValid()) {
               var7.teleport(var7.getLocation().add(this.step));
            } else {
               if (var7.isValid()) {
                  var7.remove();
               }

               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 1L, 1L);
   }

   private boolean fireChain(Player var1) {
      Location var2 = var1.getEyeLocation();
      Vector var3 = var2.getDirection().normalize();
      RayTraceResult var4 = var1.getWorld().rayTraceEntities(var2, var3, 55.0, 0.8, var1x -> var1x != var1 && var1x instanceof LivingEntity);
      if (var4 != null && var4.getHitEntity() instanceof LivingEntity var8) {
         this.launchChainAt(var1, var8);
         return true;
      } else {
         RayTraceResult var5 = var1.getWorld().rayTraceBlocks(var2, var3, 55.0);
         Location var9 = null;
         if (var5 != null && var5.getHitPosition() != null) {
            var9 = var5.getHitPosition().toLocation(var1.getWorld());
         } else if (isActive(this.anchorWindow, var1.getUniqueId())) {
            var9 = var1.getLocation().add(0.0, 14.0, 0.0);
            var1.sendMessage("§6§oSky anchor!");
         }

         if (var9 == null) {
            Location var10 = var2.clone().add(var3.clone().multiply(55.0));
            this.launchChainToPoint(var1, var10, null);
            return false;
         } else {
            Location var7 = var9;
            this.launchChainToPoint(var1, var7, () -> this.yankPlayerTo(var1, var7));
            return true;
         }
      }
   }

   private void yankPlayerTo(Player var1, Location var2) {
      if (var1.isOnline() && !var1.isDead()) {
         Vector var3 = var2.toVector().subtract(var1.getLocation().toVector());
         double var4 = Math.max(var3.length(), 0.01);
         double var6 = var3.getX();
         double var8 = var3.getZ();
         double var10 = Math.sqrt(var6 * var6 + var8 * var8);
         double var12 = var10 > 0.001 ? Math.toDegrees(Math.atan2(var3.getY(), var10)) : 90.0;
         double var14 = Math.max(0.0, Math.min(1.0, (var12 - 25.0) / 20.0));
         double var16 = var3.getY() - 1.45 * (1.0 - var14) + 0.25;

         int var18;
         for (var18 = 6; var18 < 56; var18++) {
            double var19 = (1.0 - Math.pow(0.98, var18)) / 0.02;
            double var21 = (1.0 - Math.pow(0.91, var18)) / 0.09;
            double var23 = (var16 + 3.92 * (var18 - var19)) / var19;
            double var25 = var10 > 0.001 ? var10 / var21 : 0.0;
            if (var25 <= 3.9 && Math.abs(var23) <= 3.9) {
               break;
            }
         }

         int var29 = (int)Math.max(8L, Math.min(28L, Math.round(6.0 + var4 * 0.45)));
         int var20 = (int)Math.round((1.0 - var14) * var18 + var14 * var29);

         while (true) {
            double var32 = (1.0 - Math.pow(0.98, var20)) / 0.02;
            double var27 = (1.0 - Math.pow(0.91, var20)) / 0.09;
            double var30 = (var16 + 3.92 * (var20 - var32)) / var32;
            double var31 = var10 / var27;
            if (var31 <= 3.9 || var20 >= 56) {
               Vector var33 = var10 > 0.001 ? new Vector(var6 / var10 * var31, var30, var8 / var10 * var31) : new Vector(0.0, var30, 0.0);
               var33.setX(Math.max(-3.9, Math.min(3.9, var33.getX())));
               var33.setY(Math.max(-3.9, Math.min(3.9, var33.getY())));
               var33.setZ(Math.max(-3.9, Math.min(3.9, var33.getZ())));
               var1.setVelocity(var33);
               var1.getWorld().playSound(var1.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.4F, 0.6F);
               this.slamWindow.put(var1.getUniqueId(), System.currentTimeMillis() + 3000L);
               this.watchGroundSlam(var1);
               return;
            }

            var20 += 2;
         }
      }
   }

   private void launchChainToPoint(final Player var1, final Location var2, final Runnable var3) {
      Location var4 = var1.getEyeLocation();
      Vector var5 = var2.toVector().subtract(var4.toVector()).normalize();
      var1.getWorld().playSound(var4, Sound.BLOCK_CHAIN_PLACE, 1.2F, 1.1F);
      final ItemStack var6 = new ItemStack(Material.ECHO_SHARD);
      ItemMeta var7 = var6.getItemMeta();
      var7.setCustomModelData(12348);
      var6.setItemMeta(var7);
      ItemStack var8 = new ItemStack(Material.ECHO_SHARD);
      ItemMeta var9 = var8.getItemMeta();
      var9.setCustomModelData(12349);
      var8.setItemMeta(var9);
      final ArrayList<ItemDisplay> var10 = new ArrayList<>();
      Location var11 = var4.clone().add(var5.clone().multiply(0.9));
      var11.setYaw(0.0F);
      var11.setPitch(0.0F);
      final Quaternionf var12 = rotationFor(var5);
      final ItemDisplay var13 = (ItemDisplay)var1.getWorld().spawn(var11, ItemDisplay.class, var2x -> {
         var2x.setItemStack(var8);
         var2x.setGlowing(true);
         var2x.setBrightness(new Brightness(15, 15));
         var2x.setPersistent(false);
         var2x.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F), var12, new Vector3f(0.4F, 0.4F, 0.4F), new Quaternionf()));
         var2x.setTeleportDuration(1);
      });
      (new BukkitRunnable() {
         final Location tip = var13.getLocation();
         double linkCarry = 0.0;
         int ticks = 0;

         public void run() {
            this.ticks++;
            if (this.ticks <= 60 && var1.isOnline() && !var1.isDead()) {
               Vector var1x = var2.toVector().subtract(this.tip.toVector());
               double var2x = var1x.length();
               double var4x = Math.min(4.5, var2x);
               if (var2x > 0.001) {
                  Vector var6x = var1x.multiply(1.0 / var2x);

                  double var7x;
                  for (var7x = this.linkCarry; var7x < var4x; var7x += 0.75) {
                     Location var9x = this.tip.clone().add(var6x.clone().multiply(var7x));
                     ItemDisplay var10x = (ItemDisplay)var1.getWorld().spawn(var9x, ItemDisplay.class, var2xxx -> {
                        var2xxx.setItemStack(var6);
                        var2xxx.setGlowing(true);
                        var2xxx.setBrightness(new Brightness(15, 15));
                        var2xxx.setPersistent(false);
                        var2xxx.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F), var12, new Vector3f(0.4F, 0.4F, 0.4F), new Quaternionf()));
                     });
                     var10.add(var10x);
                  }

                  this.linkCarry = var7x - var4x;
                  this.tip.add(var6x.multiply(var4x));
                  if (var13.isValid()) {
                     Location var11x = this.tip.clone();
                     var11x.setYaw(0.0F);
                     var11x.setPitch(0.0F);
                     var13.teleport(var11x);
                  }
               }

               if (this.tip.distanceSquared(var2) <= 0.0625) {
                  if (var3 != null) {
                     var3.run();
                     this.dispose(12L);
                  } else {
                     var1.getWorld().playSound(this.tip, Sound.BLOCK_CHAIN_FALL, 1.0F, 0.9F);
                     this.startRetract();
                  }

                  this.cancel();
               }
            } else {
               this.dispose(0L);
               this.cancel();
            }
         }

         private void startRetract() {
            final Location var1x = this.tip;
            (new BukkitRunnable() {
               int retractTicks = 0;

               public void run() {
                  if (++this.retractTicks <= 60 && var1.isOnline() && !var1.isDead() && var13.isValid()) {
                     Location var1xx = var1.getEyeLocation();
                     Vector var2x = var1xx.toVector().subtract(var1x.toVector());
                     double var3x = var2x.length();
                     if (var3x <= 4.5) {
                        dispose(0L);
                        this.cancel();
                     } else {
                        var1x.add(var2x.multiply(4.5 / var3x));
                        Location var5 = var1x.clone();
                        var5.setYaw(0.0F);
                        var5.setPitch(0.0F);
                        var13.teleport(var5);
                        double var6x = var1x.distanceSquared(var1xx);
                        var10.removeIf(var3xxx -> {
                           if (!var3xxx.isValid()) {
                              return true;
                           } else if (var3xxx.getLocation().distanceSquared(var1xx) >= var6x) {
                              var3xxx.remove();
                              return true;
                           } else {
                              return false;
                           }
                        });
                     }
                  } else {
                     dispose(0L);
                     this.cancel();
                  }
               }
            }).runTaskTimer(AuratusGem.this.plugin, 1L, 1L);
         }

         private void dispose(long var1x) {
            ArrayList<ItemDisplay> var3x = new ArrayList<>(var10);
            var3x.add(var13);
            Runnable var4x = () -> {
               for (ItemDisplay var2x : var3x) {
                  if (var2x.isValid()) {
                     var2x.remove();
                  }
               }
            };
            if (var1x <= 0L) {
               var4x.run();
            } else {
               Bukkit.getScheduler().runTaskLater(AuratusGem.this.plugin, var4x, var1x);
            }
         }
      }).runTaskTimer(this.plugin, 1L, 1L);
   }

   private void launchChainAt(final Player var1, final LivingEntity var2) {
      Location var3 = var1.getEyeLocation();
      Vector var4 = var3.getDirection().normalize();
      var1.getWorld().playSound(var3, Sound.BLOCK_CHAIN_PLACE, 1.2F, 1.1F);
      final ItemStack var5 = new ItemStack(Material.ECHO_SHARD);
      ItemMeta var6 = var5.getItemMeta();
      var6.setCustomModelData(12348);
      var5.setItemMeta(var6);
      ItemStack var7 = new ItemStack(Material.ECHO_SHARD);
      ItemMeta var8 = var7.getItemMeta();
      var8.setCustomModelData(12349);
      var7.setItemMeta(var8);
      final ArrayList<ItemDisplay> var9 = new ArrayList<>();
      Location var10 = var3.clone().add(var4.clone().multiply(0.9));
      var10.setYaw(0.0F);
      var10.setPitch(0.0F);
      final ItemDisplay var11 = (ItemDisplay)var1.getWorld().spawn(var10, ItemDisplay.class, var2x -> {
         var2x.setItemStack(var7);
         var2x.setGlowing(true);
         var2x.setBrightness(new Brightness(15, 15));
         var2x.setPersistent(false);
         var2x.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F), rotationFor(var4), new Vector3f(0.4F, 0.4F, 0.4F), new Quaternionf()));
         var2x.setTeleportDuration(1);
      });
      (new BukkitRunnable() {
         final Location tip = var11.getLocation();
         double linkCarry = 0.0;
         int ticks = 0;

         public void run() {
            this.ticks++;
            if (this.ticks <= 60 && var2.isValid() && !var2.isDead() && var1.isOnline() && var2.getWorld() == this.tip.getWorld()) {
               Location var1x = var2.getLocation().add(0.0, 1.0, 0.0);
               Vector var2x = var1x.toVector().subtract(this.tip.toVector());
               double var3x = var2x.length();
               double var5x = Math.min(4.5, var3x);
               if (var3x > 0.001) {
                  Vector var7x = var2x.multiply(1.0 / var3x);
                  Quaternionf var8x = AuratusGem.rotationFor(var7x);

                  double var9x;
                  for (var9x = this.linkCarry; var9x < var5x; var9x += 0.75) {
                     Location var11x = this.tip.clone().add(var7x.clone().multiply(var9x));
                     ItemDisplay var12 = (ItemDisplay)var1.getWorld().spawn(var11x, ItemDisplay.class, var2xxx -> {
                        var2xxx.setItemStack(var5);
                        var2xxx.setGlowing(true);
                        var2xxx.setBrightness(new Brightness(15, 15));
                        var2xxx.setPersistent(false);
                        var2xxx.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F), var8x, new Vector3f(0.4F, 0.4F, 0.4F), new Quaternionf()));
                     });
                     var9.add(var12);
                  }

                  this.linkCarry = var9x - var5x;
                  this.tip.add(var7x.multiply(var5x));
                  if (var11.isValid()) {
                     Location var13 = this.tip.clone();
                     var13.setYaw(0.0F);
                     var13.setPitch(0.0F);
                     var11.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F), var8x, new Vector3f(0.4F, 0.4F, 0.4F), new Quaternionf()));
                     var11.teleport(var13);
                  }
               }

               if (this.tip.distanceSquared(var2.getLocation().add(0.0, 1.0, 0.0)) <= 1.44) {
                  this.grapple();
                  this.dispose(12L);
                  this.cancel();
               }
            } else {
               this.dispose(0L);
               this.cancel();
            }
         }

         private void grapple() {
            var2.damage(perforatorDamage, var1);
            Vector var1x = var1.getLocation().toVector().subtract(var2.getLocation().toVector());
            double var2x = Math.max(var1x.length(), 0.01);
            var1x.normalize().multiply(Math.min(3.2, 0.8 + var2x * 0.42));
            var1x.setY(Math.min(1.3, Math.abs(var1x.getY()) + 0.4));
            var2.setVelocity(var1x);
            var1.getWorld().playSound(var2.getLocation(), Sound.BLOCK_CHAIN_HIT, 1.4F, 0.7F);
         }

         private void dispose(long var1x) {
            ArrayList<ItemDisplay> var3x = new ArrayList<>(var9);
            var3x.add(var11);
            Runnable var4x = () -> {
               for (ItemDisplay var2x : var3x) {
                  if (var2x.isValid()) {
                     var2x.remove();
                  }
               }
            };
            if (var1x <= 0L) {
               var4x.run();
            } else {
               Bukkit.getScheduler().runTaskLater(AuratusGem.this.plugin, var4x, var1x);
            }
         }
      }).runTaskTimer(this.plugin, 1L, 1L);
   }

   private static Quaternionf rotationFor(Vector var0) {
      float var1 = (float)Math.atan2(var0.getX(), var0.getZ());
      float var2 = (float)(-Math.asin(Math.max(-1.0, Math.min(1.0, var0.getY()))));
      return new Quaternionf().rotationY(var1).rotateX(var2);
   }

   private void watchGroundSlam(final Player var1) {
      (new BukkitRunnable() {
         int ticks = 0;

         public void run() {
            if (++this.ticks > 70 || !var1.isOnline() || var1.isDead() || !AuratusGem.isActive(AuratusGem.this.slamWindow, var1.getUniqueId())) {
               AuratusGem.this.slamWindow.remove(var1.getUniqueId());
               this.cancel();
            } else if (this.ticks > 6 && var1.isOnGround()) {
               if (var1.isSneaking()) {
                  var1.getWorld().playSound(var1.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2F, 0.8F);
                  var1.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, var1.getLocation(), 1);
                  AuratusGem.this.spawnSmashDecal(var1.getLocation());

                  boolean var3 = false;

                  for (LivingEntity var2 : var1.getLocation().getNearbyLivingEntities(4.0)) {
                     if (var2 != var1) {
                        var2.damage(slamDamage, var1);
                        var2.setVelocity(var2.getVelocity().add(new Vector(0.0, 0.8, 0.0)));
                        var3 = true;
                     }
                  }

                  // Forward dash out of the crater after slamming.
                  Vector var4 = var1.getLocation().getDirection();
                  var4.setY(0.0);
                  if (var4.lengthSquared() > 0.001) {
                     var4.normalize().multiply(1.4).setY(0.35);
                     var1.setVelocity(var4);
                  }

                  // A connecting slam refunds one Venerated Perforators chain charge.
                  if (var3) {
                     List<Long> var5 = AuratusGem.this.chainUses.get(var1.getUniqueId());
                     if (var5 != null && !var5.isEmpty()) {
                        var5.remove(var5.size() - 1);
                        var1.getWorld().playSound(var1.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.2F, 1.4F);
                        var1.sendMessage("§6§oChain recovered!");
                     }
                  }
               }

               AuratusGem.this.slamWindow.remove(var1.getUniqueId());
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 2L, 1L);
   }

   private void spawnSmashDecal(Location var1) {
      ItemStack var2 = new ItemStack(Material.ECHO_SHARD);
      ItemMeta var3 = var2.getItemMeta();
      var3.setCustomModelData(12345);
      var2.setItemMeta(var3);
      ItemDisplay var4 = (ItemDisplay)var1.getWorld()
         .spawn(
            var1.clone().add(0.0, 0.15, 0.0),
            ItemDisplay.class,
            var1x -> {
               var1x.setItemStack(var2);
               var1x.setBrightness(new Brightness(15, 15));
               var1x.setPersistent(false);
               var1x.setTransformation(
                  new Transformation(
                     new Vector3f(0.0F, 0.0F, 0.0F),
                     new Quaternionf().rotationX((float)Math.toRadians(90.0)),
                     new Vector3f(3.0F, 3.0F, 3.0F),
                     new Quaternionf()
                  )
               );
            }
         );
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         if (var4.isValid()) {
            var4.remove();
         }
      }, 15L);
   }

   public void applyPassives(Player var1, int var2) {
      // Sneak-haste is handled by hasteTick() so it works in either hand, not just the offhand.
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onFall(EntityDamageEvent var1) {
      if (var1.getCause() == DamageCause.FALL && var1.getEntity() instanceof Player var2 && this.plugin.holds(var2, "auratus")) {
         var1.setDamage(var1.getDamage() * 0.2);
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onHaulingStrike(EntityDamageByEntityEvent var1) {
      if (var1.getCause() == DamageCause.ENTITY_ATTACK
         && var1.getDamager() instanceof Player var2
         && var1.getEntity() instanceof LivingEntity var3
         && this.plugin.holds(var2, "auratus")) {
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (var3.isValid() && !var3.isDead()) {
               Vector var2x = var2.getLocation().toVector().subtract(var3.getLocation().toVector());
               var2x.setY(0);
               if (!(var2x.lengthSquared() < 0.01)) {
                  var3.setVelocity(var2x.normalize().multiply(0.5).setY(0.1));
               }
            }
         }, 1L);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onArmorDamage(PlayerItemDamageEvent var1) {
      Player var2 = var1.getPlayer();
      if (this.plugin.holds(var2, "auratus")
         && var1.getItem().getType().name().matches(".*(_HELMET|_CHESTPLATE|_LEGGINGS|_BOOTS)$")
         && ThreadLocalRandom.current().nextDouble() < 0.1) {
         var1.setCancelled(true);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onAegisParry(EntityDamageByEntityEvent var1) {
      if (var1.getEntity() instanceof Player var2) {
         UUID var10 = var2.getUniqueId();
         if (isActive(this.aegisWindow, var10)) {
            var1.setCancelled(true);
            this.aegisWindow.remove(var10);
            LivingEntity var4 = null;
            if (var1.getDamager() instanceof LivingEntity var5) {
               var4 = var5;
            } else if (var1.getDamager() instanceof Projectile var6 && var6.getShooter() instanceof LivingEntity var7) {
               var4 = var7;
            }

            double var11 = Math.max(var1.getDamage(), 2.0);
            if (var4 != null && var4 != var2) {
               var4.damage(var11, var2);
               if (var4 instanceof Player var12) {
                  var12.damageItemStack(EquipmentSlot.HAND, 40);
               }

               this.chainToTheHeavens(var2, var4);
            }

            double var13 = var2.getAttribute(Attribute.MAX_HEALTH) != null ? var2.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;
            var2.setHealth(Math.min(var13, var2.getHealth() + aegisHeal));
            var2.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 2, false, true, true));
            PotionEffectType var9 = (PotionEffectType)Registry.EFFECT.get(NamespacedKey.minecraft("weaving"));
            if (var9 != null) {
               var2.addPotionEffect(new PotionEffect(var9, 200, 0, false, true, true));
            }

            this.anchorWindow.put(var10, System.currentTimeMillis() + 5000L);
            var2.getWorld().playSound(var2.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.4F, 1.3F);
            var2.getWorld().playSound(var2.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.4F, 0.8F);
            this.ring(var2.getLocation().add(0.0, 1.0, 0.0), 1.6, Color.fromRGB(255, 220, 90));
            var2.sendMessage("§6§oParried!");
         }
      }
   }

   private static boolean isActive(Map<UUID, Long> var0, UUID var1) {
      Long var2 = (Long)var0.get(var1);
      if (var2 == null) {
         return false;
      } else if (var2 < System.currentTimeMillis()) {
         var0.remove(var1);
         return false;
      } else {
         return true;
      }
   }

   // Third power: a parried attacker is hoisted into the air on a chain and stunned there.
   private void chainToTheHeavens(Player var1, LivingEntity var2) {
      Location var3 = var2.getLocation().clone();
      var2.getWorld().playSound(var3, Sound.BLOCK_CHAIN_PLACE, 1.4F, 0.5F);
      this.spawnChain(var1, var3.clone().add(0.0, -0.4, 0.0), var3.clone().add(0.0, 3.2, 0.0));
      var2.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 26, 2, false, false, true));
      var2.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 70, 250, false, false, true));
      var2.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 70, 2, false, false, true));
      if (var2 instanceof Player var4) {
         var4.sendMessage("§6§oChained to the heavens!");
      }
   }

   private void spawnChain(Player var1, Location var2, Location var3) {
      Vector var4 = var3.toVector().subtract(var2.toVector());
      double var5 = var4.length();
      if (!(var5 < 0.5)) {
         Vector var7 = var4.clone().normalize();
         Quaternionf var8 = rotationFor(var7);
         ItemStack var9 = new ItemStack(Material.ECHO_SHARD);
         ItemMeta var10 = var9.getItemMeta();
         var10.setCustomModelData(12348);
         var9.setItemMeta(var10);
         ItemStack var11 = new ItemStack(Material.ECHO_SHARD);
         ItemMeta var12 = var11.getItemMeta();
         var12.setCustomModelData(12349);
         var11.setItemMeta(var12);
         ArrayList<ItemDisplay> var13 = new ArrayList<>();
         double var14 = 0.15000000223517418;
         double var16 = Math.max(var14, Math.floor(0.8 / var14) * var14);

         for (double var18 = Math.floor(0.9 / var14) * var14; var18 < var5; var18 += var16) {
            boolean var20 = var18 + var16 >= var5;
            Location var21 = var2.clone().add(var7.clone().multiply(var18));
            var21.setYaw(0.0F);
            var21.setPitch(0.0F);
            ItemStack var22 = var20 ? var11 : var9;
            ItemDisplay var23 = (ItemDisplay)var1.getWorld().spawn(var21, ItemDisplay.class, var2x -> {
               var2x.setItemStack(var22);
               var2x.setGlowing(true);
               var2x.setBrightness(new Brightness(15, 15));
               var2x.setPersistent(false);
               var2x.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F), var8, new Vector3f(0.4F, 0.4F, 0.4F), new Quaternionf()));
            });
            var13.add(var23);
         }

         Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            for (ItemDisplay var2x : var13) {
               if (var2x.isValid()) {
                  var2x.remove();
               }
            }
         }, 12L);
      }
   }

   private void ring(Location var1, double var2, Color var4) {
      DustOptions var5 = new DustOptions(var4, 1.5F);

      for (int var6 = 0; var6 < 24; var6++) {
         double var7 = (Math.PI * 2) * var6 / 24.0;
         var1.getWorld().spawnParticle(Particle.DUST, var1.clone().add(Math.cos(var7) * var2, 0.0, Math.sin(var7) * var2), 1, 0.0, 0.0, 0.0, 0.0, var5);
      }
   }

   public void cleanup(Player var1) {
      UUID var2 = var1.getUniqueId();
      this.chainUses.remove(var2);
      this.aegisWindow.remove(var2);
      this.anchorWindow.remove(var2);
      this.slamWindow.remove(var2);
   }
}
