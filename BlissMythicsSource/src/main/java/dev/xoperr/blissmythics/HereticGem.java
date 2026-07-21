package dev.xoperr.blissmythics;

import dev.xoperr.blissgems.api.BlissGemsAPI;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.api.GemPassiveHandler;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPotionEffectEvent.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class HereticGem implements GemAbilityHandler, GemPassiveHandler, Listener {
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
   private final Map<UUID, Long> firstSawAt = new HashMap<>();
   private final Map<UUID, Long> bleeding = new HashMap<>();
   private final Map<UUID, Set<UUID>> links = new HashMap<>();
   private final Map<UUID, Long> linkExpiry = new HashMap<>();
   private final Map<UUID, Long> lastCombo = new HashMap<>();
   private final Set<UUID> mirroring = new HashSet<>();
   private final Set<UUID> crashing = new HashSet<>();

   private final double critMultiplier;
   private final double bleedMultiplier;
   private final double bloodsawsDamage;
   private final double bloodlinkDamage;
   private final double hurricaneDamage;
   private final double maxHitDamage;
   private final int bloodsawsCooldown;
   private final int bloodlinkCooldown;

   public HereticGem(BlissMythics var1, BlissGemsAPI var2) {
      this.plugin = var1;
      this.api = var2;
      this.critMultiplier = var1.getConfig().getDouble("heretic.crit-multiplier", 1.5);
      this.bleedMultiplier = var1.getConfig().getDouble("heretic.bleed-multiplier", 2.0);
      this.bloodsawsDamage = var1.getConfig().getDouble("heretic.bloodsaws-damage", 6.0);
      this.bloodlinkDamage = var1.getConfig().getDouble("heretic.bloodlink-damage", 7.0);
      this.hurricaneDamage = var1.getConfig().getDouble("heretic.hurricane-damage", 4.0);
      this.maxHitDamage = var1.getConfig().getDouble("heretic.max-hit-cap", 13.0);
      this.bloodsawsCooldown = var1.getConfig().getInt("heretic.cooldowns.bloodsaws", 25);
      this.bloodlinkCooldown = var1.getConfig().getInt("heretic.cooldowns.bloodlink", 60);
      (new BukkitRunnable() {
         public void run() {
            HereticGem.this.bleedTick();
         }
      }).runTaskTimer(var1, 10L, 10L);
   }

   public boolean chargeWindowActive(Player var1) {
      Long var2 = this.firstSawAt.get(var1.getUniqueId());
      return var2 != null && System.currentTimeMillis() - var2 <= 4000L;
   }

   public void onPrimary(Player var1, int var2) {
      UUID var3 = var1.getUniqueId();
      if (this.api.getAbilityManager().isOnCooldown(var1, "heretic-bloodsaws")) {
         int var7 = this.api.getAbilityManager().getRemainingCooldown(var1, "heretic-bloodsaws");
         var1.sendMessage("§4Bloodsaws §7on cooldown: §c" + var7 + "s");
      } else {
         long var4 = System.currentTimeMillis();
         Long var6 = this.firstSawAt.get(var3);
         this.launchSaw(var1);
         if (var6 != null && var4 - var6 <= 4000L) {
            this.firstSawAt.remove(var3);
            this.spore(var1);
            this.api.getAbilityManager().setCooldown(var1, "heretic-bloodsaws", bloodsawsCooldown);
         } else {
            this.firstSawAt.put(var3, var4);
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
               Long var5 = this.firstSawAt.get(var3);
               if (var5 != null && var5 == var4) {
                  this.firstSawAt.remove(var3);
                  this.api.getAbilityManager().setCooldown(var1, "heretic-bloodsaws", bloodsawsCooldown);
               }
            }, 80L);
         }
      }
   }

   public void onSecondary(Player var1, int var2) {
      if (this.api.getAbilityManager().isOnCooldown(var1, "heretic-bloodlink")) {
         int var3 = this.api.getAbilityManager().getRemainingCooldown(var1, "heretic-bloodlink");
         var1.sendMessage("§4Bloodlinking §7on cooldown: §c" + var3 + "s");
      } else {
         this.api.getAbilityManager().setCooldown(var1, "heretic-bloodlink", bloodlinkCooldown);
         var1.sendMessage("§4§oBloodlinking!");
         this.bloodlinkSequence(var1);
      }
   }

   private void launchSaw(final Player var1) {
      var1.getWorld().playSound(var1.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
      final Vector[] var2 = new Vector[]{var1.getEyeLocation().getDirection().normalize()};
      final Location[] var3 = new Location[]{var1.getEyeLocation().add(var2[0].clone().multiply(1.2))};
      final HashSet var4 = new HashSet();
      final DustOptions var5 = new DustOptions(Color.fromRGB(140, 0, 0), 1.6F);
      ItemStack var6 = new ItemStack(Material.ECHO_SHARD);
      ItemMeta var7 = var6.getItemMeta();
      var7.setCustomModelData(12347);
      var6.setItemMeta(var7);
      final ItemDisplay var8 = (ItemDisplay)var1.getWorld().spawn(var3[0], ItemDisplay.class, var1x -> {
         var1x.setItemStack(var6);
         var1x.setBillboard(Billboard.CENTER);
         var1x.setBrightness(new Brightness(15, 15));
         var1x.setPersistent(false);
         var1x.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F), new Quaternionf(), new Vector3f(1.1F, 1.1F, 1.1F), new Quaternionf()));
         var1x.setTeleportDuration(1);
      });
      (new BukkitRunnable() {
         int life = 0;
         int bounces = 0;

         public void cancel() {
            if (var8.isValid()) {
               var8.remove();
            }

            super.cancel();
         }

         public void run() {
            if (++this.life <= 60 && var1.isOnline()) {
               for (int var1x = 0; var1x < 2; var1x++) {
                  Location var2x = var3[0].clone().add(var2[0].clone().multiply(0.45));
                  Block var3x = var2x.getBlock();
                  if (var3x.getType().isSolid()) {
                     if (++this.bounces > 3) {
                        this.cancel();
                        return;
                     }

                     Vector var4x = var2[0];
                     Location var5x = var3[0].clone().add(new Vector(var4x.getX(), 0.0, 0.0).multiply(0.45));
                     Location var6x = var3[0].clone().add(new Vector(0.0, var4x.getY(), 0.0).multiply(0.45));
                     Location var7x = var3[0].clone().add(new Vector(0.0, 0.0, var4x.getZ()).multiply(0.45));
                     if (var5x.getBlock().getType().isSolid()) {
                        var4x.setX(-var4x.getX());
                     }

                     if (var6x.getBlock().getType().isSolid()) {
                        var4x.setY(-var4x.getY());
                     }

                     if (var7x.getBlock().getType().isSolid()) {
                        var4x.setZ(-var4x.getZ());
                     }

                     var2x = var3[0].clone().add(var4x.clone().multiply(0.45));
                     if (var2x.getBlock().getType().isSolid()) {
                        this.cancel();
                        return;
                     }

                     var3[0].getWorld().playSound(var3[0], Sound.BLOCK_ANVIL_LAND, 0.4F, 1.8F);
                  }

                  var3[0] = var2x;
                  var3[0].getWorld().spawnParticle(Particle.DUST, var3[0], 2, 0.2, 0.2, 0.2, 0.0, var5);

                  for (LivingEntity var9 : var3[0].getNearbyLivingEntities(1.3)) {
                     if (var9 != var1 && !var4.contains(var9.getUniqueId())) {
                        var4.add(var9.getUniqueId());
                        var9.damage(bloodsawsDamage, var1);
                        var9.getWorld().playSound(var9.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0F, 0.7F);
                     }
                  }
               }

               if (var8.isValid()) {
                  var8.teleport(var3[0]);
               }
            } else {
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   private void spore(final Player var1) {
      this.lastCombo.put(var1.getUniqueId(), System.currentTimeMillis());
      Block var2 = var1.getTargetBlockExact(15);
      final Location var3 = var2 != null ? var2.getLocation().add(0.5, 1.0, 0.5) : var1.getLocation();
      var1.getWorld().playSound(var3, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.4F, 0.5F);
      final DustOptions var4 = new DustOptions(Color.fromRGB(170, 10, 10), 2.0F);
      (new BukkitRunnable() {
         int life = 0;

         public void run() {
            if (++this.life > 12) {
               this.cancel();
            } else {
               for (int var1x = 0; var1x < 25; var1x++) {
                  double var2x = Math.random() * Math.PI * 2.0;
                  double var4x = Math.random() * 1.2;
                  Location var6 = var3.clone().add(Math.cos(var2x) * var4x, Math.random() * 2.5, Math.sin(var2x) * var4x);
                  var3.getWorld().spawnParticle(Particle.DUST, var6, 1, 0.0, 0.0, 0.0, 0.0, var4);
               }

               var3.getWorld().spawnParticle(Particle.LAVA, var3, 2, 0.5, 0.3, 0.5, 0.0);
               long var7 = System.currentTimeMillis() + 8000L;

               for (Player var8 : var3.getNearbyPlayers(5.0)) {
                  if (var8 != var1) {
                     HereticGem.this.bleeding.put(var8.getUniqueId(), var7);
                     var8.sendMessage("§4§oYou are bleeding! Damage taken is amplified!");
                  }
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 10L);
   }

   private void bloodlinkSequence(final Player var1) {
      final HashSet<UUID> var2 = new HashSet<>();
      this.crashing.add(var1.getUniqueId());
      (new BukkitRunnable() {
         int ticks = 0;
         int airTicks = 0;
         int hops = 0;
         int phase = 0;

         public void cancel() {
            HereticGem.this.crashing.remove(var1.getUniqueId());
            Bukkit.getScheduler().runTaskLater(HereticGem.this.plugin, () -> HereticGem.this.crashing.remove(var1.getUniqueId()), 30L);
            this.finish();
            super.cancel();
         }

         public void run() {
            if (++this.ticks > 200 || !var1.isOnline() || var1.isDead()) {
               this.cancel();
            } else if (this.phase == 0) {
               Vector var5 = new Vector(0.0, 0.95, 0.0);
               Vector var9 = var1.getEyeLocation().getDirection();
               var9.setY(0);
               if (var9.lengthSquared() > 0.01) {
                  var5.add(var9.normalize().multiply(0.5));
               }

               var1.setVelocity(var5);
               var1.getWorld().playSound(var1.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.2F, 0.5F);
               this.phase = 1;
               this.airTicks = 0;
            } else {
               if (this.ticks % 2 == 0) {
                  var1.getWorld().spawnParticle(Particle.DUST, var1.getLocation(), 8, 0.3, 0.3, 0.3, 0.0, new DustOptions(Color.fromRGB(120, 0, 0), 1.4F));
               }

               this.airTicks++;
               if (this.phase == 1 && this.airTicks >= 9) {
                  Vector var4 = new Vector(0.0, -2.8, 0.0);
                  Vector var8 = var1.getEyeLocation().getDirection();
                  var8.setY(0);
                  if (var8.lengthSquared() > 0.01) {
                     var4.add(var8.normalize().multiply(0.6));
                  }

                  var1.setVelocity(var4);
                  this.phase = 2;
               } else {
                  if (this.phase == 2 && this.airTicks >= 12 && var1.isOnGround()) {
                     var1.getWorld().playSound(var1.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.7F);
                     var1.getWorld().spawnParticle(Particle.EXPLOSION, var1.getLocation(), 2, 0.5, 0.2, 0.5, 0.0);

                     boolean var10 = false;

                     for (LivingEntity var2x : var1.getLocation().getNearbyLivingEntities(4.5)) {
                        if (var2x != var1) {
                           var2x.damage(bloodlinkDamage, var1);
                           var2x.setVelocity(var2x.getVelocity().add(new Vector(0.0, 0.5, 0.0)));
                           var10 = true;
                           if (var2x instanceof Player var3) {
                              var2.add(var3.getUniqueId());
                           }
                        }
                     }

                     // Third power: landing a Bloodlink slam soon after a Bloodsaws combo
                     // unleashes a Bloodstorm hurricane on this spot.
                     if (var10 && HereticGem.this.consumeCombo(var1)) {
                        HereticGem.this.spawnHurricane(var1, var1.getLocation().clone().add(0.0, 0.1, 0.0));
                     }

                     if (++this.hops >= 3) {
                        this.cancel();
                        return;
                     }

                     this.phase = 0;
                  }
               }
            }
         }

         private void finish() {
            if (var2.size() >= 2) {
               long var1x = System.currentTimeMillis() + 15000L;

               for (UUID var4 : var2) {
                  HashSet var5 = new HashSet(var2);
                  var5.remove(var4);
                  HereticGem.this.links.put(var4, var5);
                  HereticGem.this.linkExpiry.put(var4, var1x);
                  Player var6 = Bukkit.getPlayer(var4);
                  if (var6 != null) {
                     var6.sendMessage("§4§oYou have been bloodlinked! Damage you take is shared.");
                  }
               }

               var1.sendMessage("§4§o" + var2.size() + " players bloodlinked!");
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onCrashFall(EntityDamageEvent var1) {
      if (var1.getCause() == DamageCause.FALL) {
         if (var1.getEntity() instanceof Player var2) {
            if (this.crashing.contains(var2.getUniqueId())) {
               var1.setCancelled(true);
            }
         }
      }
   }

   public void applyPassives(Player var1, int var2) {
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onBleedDamage(EntityDamageEvent var1) {
      if (var1.getEntity() instanceof Player var2) {
         Long var4 = this.bleeding.get(var2.getUniqueId());
         if (var4 != null) {
            if (var4 < System.currentTimeMillis()) {
               this.bleeding.remove(var2.getUniqueId());
            } else {
               var1.setDamage(var1.getDamage() * bleedMultiplier);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onAutoCrit(EntityDamageByEntityEvent var1) {
      if (var1.getCause() == DamageCause.ENTITY_ATTACK) {
         if (var1.getDamager() instanceof Player var2) {
            if (this.plugin.holds(var2, "heretic")) {
               var1.setDamage(var1.getDamage() * critMultiplier);
               var1.getEntity().getWorld().spawnParticle(Particle.CRIT, var1.getEntity().getLocation().add(0.0, 1.0, 0.0), 12, 0.4, 0.4, 0.4, 0.3);
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onBloodHardening(EntityDamageEvent var1) {
      if (var1.getEntity() instanceof Player var2) {
         if (this.plugin.holds(var2, "heretic")) {
            double var5 = var1.getFinalDamage();
            if (var5 > maxHitDamage && var5 > 0.0) {
               var1.setDamage(var1.getDamage() * (maxHitDamage / var5));
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onStrengthGain(EntityPotionEffectEvent var1) {
      if (var1.getEntity() instanceof Player var2) {
         if (var1.getModifiedType() == PotionEffectType.STRENGTH) {
            if (var1.getAction() != Action.REMOVED && var1.getAction() != Action.CLEARED) {
               if (this.plugin.holds(var2, "heretic")) {
                  Bukkit.getScheduler()
                     .runTaskLater(
                        this.plugin,
                        () -> {
                           PotionEffect var1x = var2.getPotionEffect(PotionEffectType.STRENGTH);
                           if (var1x != null && var1x.getDuration() != -1) {
                              int var2x = var1x.getAmplifier() >= 1 ? 9600 : 19200;
                              if (var1x.getDuration() < var2x - 100) {
                                 var2.addPotionEffect(
                                    new PotionEffect(
                                       PotionEffectType.STRENGTH, var2x, var1x.getAmplifier(), var1x.isAmbient(), var1x.hasParticles(), var1x.hasIcon()
                                    )
                                 );
                              }
                           }
                        },
                        1L
                     );
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onLinkedDamage(EntityDamageEvent var1) {
      if (var1.getEntity() instanceof Player var2) {
         UUID var14 = var2.getUniqueId();
         if (!this.mirroring.contains(var14)) {
            Set<UUID> var4 = this.links.get(var14);
            if (var4 != null) {
               Long var5 = this.linkExpiry.get(var14);
               if (var5 != null && var5 >= System.currentTimeMillis()) {
                  double var6 = var1.getFinalDamage();
                  if (!(var6 <= 0.0)) {
                     for (UUID var9 : var4) {
                        Player var10 = Bukkit.getPlayer(var9);
                        if (var10 != null && !var10.isDead() && !this.mirroring.contains(var9)) {
                           this.mirroring.add(var9);

                           try {
                              var10.damage(var6);
                              var10.getWorld()
                                 .spawnParticle(
                                    Particle.DUST,
                                    var10.getLocation().add(0.0, 1.0, 0.0),
                                    10,
                                    0.3,
                                    0.5,
                                    0.3,
                                    0.0,
                                    new DustOptions(Color.fromRGB(150, 0, 0), 1.5F)
                                 );
                           } finally {
                              this.mirroring.remove(var9);
                           }
                        }
                     }
                  }
               } else {
                  this.links.remove(var14);
                  this.linkExpiry.remove(var14);
               }
            }
         }
      }
   }

   private void bleedTick() {
      long var1 = System.currentTimeMillis();
      Iterator var3 = this.bleeding.entrySet().iterator();

      while (var3.hasNext()) {
         Entry var4 = (Entry)var3.next();
         if ((Long)var4.getValue() < var1) {
            var3.remove();
         } else {
            Player var5 = Bukkit.getPlayer((UUID)var4.getKey());
            if (var5 != null && var5.isOnline()) {
               var5.getWorld()
                  .spawnParticle(Particle.DUST, var5.getLocation().add(0.0, 1.0, 0.0), 4, 0.25, 0.5, 0.25, 0.0, new DustOptions(Color.fromRGB(160, 0, 0), 1.2F));
            }
         }
      }
   }

   private boolean consumeCombo(Player var1) {
      Long var2 = this.lastCombo.get(var1.getUniqueId());
      if (var2 != null && System.currentTimeMillis() - var2 <= 10000L) {
         this.lastCombo.remove(var1.getUniqueId());
         return true;
      } else {
         return false;
      }
   }

   private void spawnHurricane(final Player var1, final Location var2) {
      var2.getWorld().playSound(var2, Sound.ENTITY_WITHER_SPAWN, 1.2F, 1.4F);
      var1.sendMessage("§4§oBloodstorm unleashed!");
      (new BukkitRunnable() {
         int life = 0;

         public void run() {
            if (++this.life > 120 || !var1.isOnline()) {
               this.cancel();
            } else {
               double var1x = this.life * 0.35;

               for (int var3 = 0; var3 < 3; var3++) {
                  double var4 = var1x + var3 * (Math.PI * 2.0 / 3.0);

                  for (double var6 = 0.0; var6 < 4.0; var6 += 0.5) {
                     double var8 = 1.0 + var6 * 0.6;
                     double var10 = Math.cos(var4 + var6) * var8;
                     double var12 = Math.sin(var4 + var6) * var8;
                     var2.getWorld()
                        .spawnParticle(
                           Particle.DUST, var2.clone().add(var10, var6, var12), 1, 0.0, 0.0, 0.0, 0.0, new DustOptions(Color.fromRGB(150, 0, 0), 1.4F)
                        );
                  }
               }

               if (this.life % 10 == 0) {
                  for (Player var15 : var2.getNearbyPlayers(8.0)) {
                     if (var15 != var1 && !HereticGem.this.api.getTrustedPlayersManager().isTrusted(var1, var15)) {
                        Location var5 = var2.clone().add(0.0, 2.0, 0.0);
                        Vector var6 = var15.getLocation().add(0.0, 1.0, 0.0).toVector().subtract(var5.toVector());
                        double var7 = var6.length();
                        if (var7 > 0.001) {
                           Vector var9 = var6.multiply(1.0 / var7);

                           for (double var10 = 0.0; var10 < var7; var10 += 0.5) {
                              var2.getWorld()
                                 .spawnParticle(
                                    Particle.DUST,
                                    var5.clone().add(var9.clone().multiply(var10)),
                                    1,
                                    0.0,
                                    0.0,
                                    0.0,
                                    0.0,
                                    new DustOptions(Color.fromRGB(200, 0, 0), 1.2F)
                                 );
                           }
                        }

                        var15.damage(HereticGem.this.hurricaneDamage, var1);
                        var15.getWorld().playSound(var15.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0F, 0.6F);
                     }
                  }
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   public void cleanup(Player var1) {
      UUID var2 = var1.getUniqueId();
      this.firstSawAt.remove(var2);
      this.lastCombo.remove(var2);
   }
}
