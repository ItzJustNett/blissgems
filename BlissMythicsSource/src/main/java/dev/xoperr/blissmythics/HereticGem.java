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
import org.bukkit.World;
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
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class HereticGem implements GemAbilityHandler, GemPassiveHandler, Listener {
   private static final String KEY_SAWS = "heretic-bloodsaws";
   private static final String KEY_LINK = "heretic-bloodlink";
   private static final String KEY_STORM = "heretic-bloodstorm";
   private final BlissMythics plugin;
   private final BlissGemsAPI api;
   private final Map<UUID, Long> firstSawAt = new HashMap<>();
   private final Map<UUID, Long> bleeding = new HashMap<>();
   private final Map<UUID, Set<UUID>> links = new HashMap<>();
   private final Map<UUID, Long> linkExpiry = new HashMap<>();
   private final Set<UUID> mirroring = new HashSet<>();
   private final Set<UUID> crashing = new HashSet<>();

   private final double critMultiplier;
   private final double bleedMultiplier;
   private final double bloodsawsDamage;
   private final double bloodlinkDamage;
   private final double maxHitDamage;
   private final int bloodsawsCooldown;
   private final int bloodlinkCooldown;
   private final int bloodstormCooldown;
   private final double divebombSpeed;
   private final double divebombSteer;
   private final long stormWindowMs;
   private final long sawWindowMs;
   private final long bleedMs;
   private final long linkMs;
   private final int bloodlinkRiseTicks;
   private final double bloodlinkRange;
   private final double bloodlinkKnockup;
   private final double bloodlinkMinRise;
   private final int bloodlinkMaxHits;
   private final int sawDragTicks;
   private final int bloodsawMaxBounces;
   private final int bloodsawLifeTicks;

   public HereticGem(BlissMythics var1, BlissGemsAPI var2) {
      this.plugin = var1;
      this.api = var2;
      this.critMultiplier = var1.getConfig().getDouble("heretic.crit-multiplier", 1.5);
      this.bleedMultiplier = var1.getConfig().getDouble("heretic.bleed-multiplier", 2.0);
      this.bloodsawsDamage = var1.getConfig().getDouble("heretic.bloodsaws-damage", 6.0);
      this.bloodlinkDamage = var1.getConfig().getDouble("heretic.bloodlink-damage", 9.0);
      this.maxHitDamage = var1.getConfig().getDouble("heretic.max-hit-cap", 13.0);
      this.bloodsawsCooldown = var1.getConfig().getInt("heretic.cooldowns.bloodsaws", 25);
      this.bloodlinkCooldown = var1.getConfig().getInt("heretic.cooldowns.bloodlink", 60);
      this.bloodstormCooldown = var1.getConfig().getInt("heretic.cooldowns.bloodstorm", 90);
      this.divebombSpeed = var1.getConfig().getDouble("heretic.divebomb-speed", 2.6);
      this.divebombSteer = Math.max(0.0, Math.min(1.0, var1.getConfig().getDouble("heretic.divebomb-steer", 0.85)));
      this.stormWindowMs = var1.getConfig().getLong("heretic.bloodstorm-window-ms", 750L);
      this.sawWindowMs = var1.getConfig().getLong("heretic.saw-window-ms", 4000L);
      this.bleedMs = var1.getConfig().getLong("heretic.bleed-ms", 8000L);
      this.linkMs = var1.getConfig().getLong("heretic.bloodlink-duration-ms", 15000L);
      this.bloodlinkRiseTicks = var1.getConfig().getInt("heretic.bloodlink-rise-ticks", 11);
      this.bloodlinkRange = var1.getConfig().getDouble("heretic.bloodlink-range", 15.0);
      this.bloodlinkKnockup = var1.getConfig().getDouble("heretic.bloodlink-knockup", 0.6);
      this.bloodlinkMinRise = var1.getConfig().getDouble("heretic.bloodlink-min-rise", 2.7);
      this.bloodlinkMaxHits = var1.getConfig().getInt("heretic.bloodlink-max-hits", 0);
      this.sawDragTicks = var1.getConfig().getInt("heretic.bloodsaw-drag-ticks", 20);
      this.bloodsawMaxBounces = var1.getConfig().getInt("heretic.bloodsaw-max-bounces", 3);
      this.bloodsawLifeTicks = var1.getConfig().getInt("heretic.bloodsaw-life-ticks", 60);
      (new BukkitRunnable() {
         public void run() {
            HereticGem.this.bleedTick();
         }
      }).runTaskTimer(var1, 10L, 10L);
   }

   public boolean chargeWindowActive(Player var1) {
      Long var2 = this.firstSawAt.get(var1.getUniqueId());
      return var2 != null && System.currentTimeMillis() - var2 <= this.sawWindowMs;
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
         if (var6 != null && var4 - var6 <= this.sawWindowMs) {
            this.firstSawAt.remove(var3);
            // Both saws thrown at (almost) the same time: the "spore" Bloodstorm.
            if (var4 - var6 <= this.stormWindowMs) {
               this.bloodstorm(var1);
            }
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
      // Everything the blade has bitten, mapped to the tick its drag runs out.
      final Map<LivingEntity, Integer> dragged = new HashMap<>();
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
            if (++this.life <= HereticGem.this.bloodsawLifeTicks && var1.isOnline()) {
               for (int var1x = 0; var1x < 2; var1x++) {
                  Location var2x = var3[0].clone().add(var2[0].clone().multiply(0.45));
                  Block var3x = var2x.getBlock();
                  breakCobwebsAround(var2x);
                  if (var3x.getType().isSolid()) {
                     if (++this.bounces > HereticGem.this.bloodsawMaxBounces) {
                        this.cancel();
                        return;
                     }

                     // A bounced saw must not camp in the fight area (trees, walls) and hit
                     // players who are knocked into it seconds later - cut its remaining life.
                     this.life = Math.max(this.life, HereticGem.this.bloodsawLifeTicks - 12);

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
                        dragged.put(var9, this.life + HereticGem.this.sawDragTicks);
                     }
                  }
               }

               // Anything the blade cut rides along in its wake until its drag runs out.
               dragged.entrySet().removeIf(var1x -> var1x.getValue() <= this.life || !var1x.getKey().isValid() || var1x.getKey().isDead());

               for (LivingEntity var10 : dragged.keySet()) {
                  Vector var11 = var3[0].toVector().subtract(var10.getLocation().add(0.0, 1.0, 0.0).toVector());
                  double var12 = var11.length();
                  Vector var14 = var2[0].clone().multiply(0.6);
                  if (var12 > 0.15) {
                     var14.add(var11.multiply(Math.min(1.0, var12) / var12 * 0.4));
                  }

                  var10.setVelocity(var14);
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

   // A saw is 1.3 blocks wide, so webs it clips have to go too - checking only the block its
   // centre happens to land in let webs stop a blade that visibly cut straight through them.
   private static void breakCobwebsAround(Location var0) {
      for (int var1 = -1; var1 <= 1; var1++) {
         for (int var2 = -1; var2 <= 1; var2++) {
            for (int var3 = -1; var3 <= 1; var3++) {
               Block var4 = var0.clone().add(var1, var2, var3).getBlock();
               if (var4.getType() == Material.COBWEB) {
                  var4.setType(Material.AIR);
                  var4.getWorld().playSound(var4.getLocation(), Sound.BLOCK_COBWEB_BREAK, 1.0F, 1.0F);
               }
            }
         }
      }
   }

   // Bloodstorm (Bliss "spore"): a blood fountain where the saws were aimed - a red double helix
   // rising out of the ground and a dark spray falling back around it like an umbrella. Everyone
   // inside bleeds (damage they take is amplified).
   private void bloodstorm(final Player var1) {
      if (this.api.getAbilityManager().isOnCooldown(var1, KEY_STORM)) {
         var1.sendMessage("§4Bloodstorm §7on cooldown: §c" + this.api.getAbilityManager().getRemainingCooldown(var1, KEY_STORM) + "s");
         return;
      }
      this.api.getAbilityManager().setCooldown(var1, KEY_STORM, this.bloodstormCooldown);
      var1.sendMessage("§4§oBloodstorm!");
      Block var2 = var1.getTargetBlockExact(15);
      final Location var3 = var2 != null ? var2.getLocation().add(0.5, 1.0, 0.5) : var1.getLocation();
      var3.getWorld().playSound(var3, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.4F, 0.5F);
      var3.getWorld().playSound(var3, Sound.ENTITY_WITHER_SPAWN, 0.8F, 1.6F);
      final DustOptions var4 = new DustOptions(Color.fromRGB(220, 20, 40), 1.6F);
      final DustOptions var5 = new DustOptions(Color.fromRGB(70, 0, 0), 1.3F);
      final double var6 = this.plugin.getConfig().getDouble("heretic.bloodstorm-radius", 5.0);
      final double var8 = 6.0;
      (new BukkitRunnable() {
         int life = 0;

         public void run() {
            if (++this.life > 120 || !var1.isOnline()) {
               this.cancel();
               return;
            }
            World var9 = var3.getWorld();
            double var10 = this.life * 0.25;
            // the double helix
            for (int var12 = 0; var12 < 2; var12++) {
               for (double var13 = 0.0; var13 < var8; var13 += 0.25) {
                  double var15 = var10 + var13 * 1.2 + var12 * Math.PI;
                  double var17 = 0.35 + var13 * 0.05;
                  var9.spawnParticle(Particle.DUST, var3.clone().add(Math.cos(var15) * var17, var13, Math.sin(var15) * var17), 1, 0.0, 0.0, 0.0, 0.0, var4, true);
               }
            }
            // the umbrella: spokes from the top curving out and down to the edge
            if (this.life % 2 == 0) {
               for (int var19 = 0; var19 < 24; var19++) {
                  double var20 = var19 * (Math.PI * 2.0 / 24.0) + var10 * 0.3;
                  for (double var22 = 0.0; var22 <= 1.0; var22 += 0.08) {
                     double var24 = var22 * var6;
                     double var26 = var8 - Math.pow(var22, 2.0) * (var8 * 0.75);
                     var9.spawnParticle(Particle.DUST, var3.clone().add(Math.cos(var20) * var24, var26, Math.sin(var20) * var24), 1, 0.0, 0.0, 0.0, 0.0, var5, true);
                  }
               }
               var9.spawnParticle(Particle.DUST, var3, 12, 0.6, 0.1, 0.6, 0.0, var5, true);
            }
            if (this.life % 10 == 0) {
               long var28 = System.currentTimeMillis() + HereticGem.this.bleedMs;
               for (Player var30 : var3.getNearbyPlayers(var6, var8)) {
                  if (var30 != var1 && !HereticGem.this.api.getTrustedPlayersManager().isTrusted(var1, var30)) {
                     if (!HereticGem.this.bleeding.containsKey(var30.getUniqueId())) {
                        var30.sendMessage("§4§oYou are bleeding! Damage taken is amplified!");
                     }
                     HereticGem.this.bleeding.put(var30.getUniqueId(), var28);
                  }
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }

   // Horizontal distance an impulse of 1.0 covers over var0 ticks of vanilla air drag (0.91/tick).
   private static double airTravel(int var0) {
      return (1.0 - Math.pow(0.91, var0)) / 0.09;
   }

   // Exactly where the crosshair points: the block face it lands on, or open air at max range.
   // Deliberately blind to entities - the hop is aimed by the player, never snapped to a target.
   private Location aimPoint(Player var1) {
      Location var2 = var1.getEyeLocation();
      Vector var3 = var2.getDirection().normalize();
      RayTraceResult var4 = var1.getWorld().rayTraceBlocks(var2, var3, this.bloodlinkRange);
      return var4 != null && var4.getHitPosition() != null
         ? var4.getHitPosition().toLocation(var1.getWorld())
         : var2.clone().add(var3.clone().multiply(this.bloodlinkRange));
   }

   // Solves the launch impulse that carries the player from var0 to var1 over var2 ticks of
   // vanilla air physics (horizontal drag 0.91/tick, gravity 0.08 with 0.98 drag).
   private static Vector arcImpulse(Location var0, Location var1, int var2) {
      double var3 = var1.getX() - var0.getX();
      double var5 = var1.getY() - var0.getY();
      double var7 = var1.getZ() - var0.getZ();
      double var9 = Math.sqrt(var3 * var3 + var7 * var7);
      double var11 = airTravel(var2);
      double var13 = (1.0 - Math.pow(0.98, var2)) / 0.02;
      double var15 = (var5 + 3.92 * (var2 - var13)) / var13;
      if (var9 <= 0.001) {
         return new Vector(0.0, var15, 0.0);
      } else {
         double var17 = var9 / var11;
         return new Vector(var3 / var9 * var17, var15, var7 / var9 * var17);
      }
   }

   // The full launch impulse for the hop: an arc that tops out over the aimed point, however
   // far away or however far above/below the player it is. The flight time is stretched until
   // the solve fits inside the velocity the server accepts, then clamped.
   private Vector leapImpulse(Player var1, Location var2) {
      Location var3 = var1.getLocation();
      // Aim well above the point so the hop tops out high over it; the glide closes the
      // last stretch, and the extra height is what the glide has to spend.
      Location var4 = var2.clone().add(0.0, 4.0, 0.0);

      Vector var5 = null;
      for (int var6 = Math.max(8, this.bloodlinkRiseTicks); var6 <= 60; var6 += 2) {
         var5 = arcImpulse(var3, var4, var6);
         if (Math.abs(var5.getX()) <= 3.9 && Math.abs(var5.getY()) <= 3.9 && Math.abs(var5.getZ()) <= 3.9) {
            break;
         }
      }

      // Landing on top of the aimed point solves to a near-zero arc, which is why the chained
      // hops used to be a shuffle. Every hop clears the same floor as the first.
      var5.setX(Math.max(-3.9, Math.min(3.9, var5.getX())));
      var5.setY(Math.max(this.bloodlinkMinRise, Math.min(3.9, var5.getY())));
      var5.setZ(Math.max(-3.9, Math.min(3.9, var5.getZ())));
      return var5;
   }

   // At the top of the arc: drop onto the aimed point, keeping just enough horizontal push to
   // cover whatever ground the rise left over.
   private static Vector diveAim(Player var1, Location var2) {
      Vector var3 = new Vector(0.0, -2.8, 0.0);
      if (var2 == null) {
         return var3;
      } else {
         double var4 = var2.getX() - var1.getLocation().getX();
         double var6 = var2.getZ() - var1.getLocation().getZ();
         double var8 = Math.sqrt(var4 * var4 + var6 * var6);
         if (var8 > 0.05) {
            double var10 = Math.max(1.0, var1.getLocation().getY() - var2.getY());
            int var12 = (int)Math.max(2.0, Math.ceil(var10 / 2.8));
            double var13 = Math.min(3.9, var8 / airTravel(var12));
            var3.setX(var4 / var8 * var13);
            var3.setZ(var6 / var8 * var13);
         }

         return var3;
      }
   }

   // One tick of the divebomb: the player's aim steers it (heretic.divebomb-steer), the hop's
   // aimed point only nudges it, always downward, accelerating up to heretic.divebomb-speed.
   private Vector divebombStep(Player var1, Location var2, int var3) {
      Vector var4 = var1.getEyeLocation().getDirection().normalize();
      Vector var5 = var4.clone();
      if (var2 != null && var2.getWorld() == var1.getWorld()) {
         Vector var6 = var2.toVector().subtract(var1.getLocation().toVector());
         if (var6.lengthSquared() > 0.01) {
            var5 = var6.normalize().multiply(1.0 - this.divebombSteer).add(var4.multiply(this.divebombSteer));
         }
      }

      var5.setY(Math.min(var5.getY(), -0.55));
      double var7 = Math.min(this.divebombSpeed, 0.8 + var3 * 0.15);
      return var5.normalize().multiply(var7);
   }

   private void bloodlinkSequence(final Player var1) {
      final HashSet<UUID> var2 = new HashSet<>();
      this.crashing.add(var1.getUniqueId());
      (new BukkitRunnable() {
         int ticks = 0;
         int airTicks = 0;
         int hops = 0;
         int phase = 0;
         Location target = null;
         double lastY = 0.0;
         boolean rising = false;
         int diveTicks = 0;

         public void cancel() {
            HereticGem.this.crashing.remove(var1.getUniqueId());
            Bukkit.getScheduler().runTaskLater(HereticGem.this.plugin, () -> HereticGem.this.crashing.remove(var1.getUniqueId()), 30L);
            if (var1.isOnline()) {
               var1.setGliding(false);
            }
            this.finish();
            super.cancel();
         }

         public void run() {
            if (++this.ticks > 400 || !var1.isOnline() || var1.isDead()) {
               this.cancel();
            } else if (this.phase == 0) {
               // The hop is solved as a whole arc towards the aimed point, so it reaches
               // targets above and below the player, not just ahead of them.
               this.target = HereticGem.this.aimPoint(var1);
               var1.setVelocity(HereticGem.this.leapImpulse(var1, this.target));
               var1.setGliding(false);
               var1.getWorld().playSound(var1.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.2F, 0.5F);
               this.phase = 1;
               this.airTicks = 0;
               this.rising = false;
               this.lastY = var1.getLocation().getY();
            } else {
               if (this.ticks % 2 == 0) {
                  var1.getWorld().spawnParticle(Particle.DUST, var1.getLocation(), 8, 0.3, 0.3, 0.3, 0.0, new DustOptions(Color.fromRGB(120, 0, 0), 1.4F));
               }

               this.airTicks++;
               double var11 = var1.getLocation().getY();
               // The launch impulse only shows up in the server-side position after a client
               // round-trip, so on a laggy tick the first few samples read as "already
               // falling" and the hop dived on the spot. Wait for the climb to actually
               // start before the apex check counts, with a hard timeout so a launch that
               // never takes (ceiling, blocked) still resolves instead of hanging.
               if (!this.rising && (var11 > this.lastY + 0.05 || this.airTicks >= 15)) {
                  this.rising = true;
               }

               // The dive starts at the top of the arc rather than on a fixed tick, so a long
               // or steep hop is allowed to finish climbing before it slams.
               boolean var12 = this.airTicks >= 3
                  && this.rising
                  && (var11 <= this.lastY || this.airTicks >= HereticGem.this.bloodlinkRiseTicks + 60);
               this.lastY = var11;
               if (this.phase == 1 && var12) {
                  // The apex tips into an elytra divebomb: nose down onto the aimed point,
                  // speeding up as it falls. The player's aim still bends the dive a little.
                  var1.setVelocity(diveAim(var1, this.target).normalize().multiply(0.8));
                  var1.setGliding(true);
                  var1.getWorld().playSound(var1.getLocation(), Sound.ITEM_ELYTRA_FLYING, 0.8F, 1.6F);
                  this.phase = 2;
                  this.diveTicks = 0;
               } else {
                  if (this.phase == 2 && !var1.isOnGround()) {
                     var1.setGliding(true);
                     var1.setVelocity(HereticGem.this.divebombStep(var1, this.target, ++this.diveTicks));
                     var1.getWorld().spawnParticle(Particle.DUST, var1.getLocation(), 6, 0.2, 0.2, 0.2, 0.0, new DustOptions(Color.fromRGB(170, 0, 0), 1.8F));
                  }

                  if (this.phase == 2 && var1.isOnGround()) {
                     var1.setGliding(false);
                     var1.getWorld().playSound(var1.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.7F);
                     var1.getWorld().spawnParticle(Particle.EXPLOSION, var1.getLocation(), 2, 0.5, 0.2, 0.5, 0.0);

                     boolean var10 = false;

                     for (LivingEntity var2x : var1.getLocation().getNearbyLivingEntities(4.5)) {
                        if (var2x != var1) {
                           var2x.damage(bloodlinkDamage, var1);
                           var2x.setVelocity(var2x.getVelocity().add(new Vector(0.0, bloodlinkKnockup, 0.0)));
                           var10 = true;
                           if (var2x instanceof Player var3) {
                              var2.add(var3.getUniqueId());
                           }
                        }
                     }

                     // The chain is earned hit by hit: a slam that lands nothing ends it, and
                     // five connected slams is the cap.
                     if (!var10) {
                        var1.sendMessage("§4§oSlam missed — the chain breaks.");
                        this.cancel();
                        return;
                     }

                     // The chain runs as long as the aim holds; a cap of 0 leaves it unbounded.
                     ++this.hops;
                     if (HereticGem.this.bloodlinkMaxHits > 0 && this.hops >= HereticGem.this.bloodlinkMaxHits) {
                        this.cancel();
                        return;
                     }

                     // Every hop gets the same airtime budget as the first, so the 400-tick
                     // guard bounds a single flight instead of the whole chain.
                     this.ticks = 0;
                     this.phase = 0;
                  }
               }
            }
         }

         private void finish() {
            if (var2.size() >= 2) {
               long var1x = System.currentTimeMillis() + HereticGem.this.linkMs;

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
            } else if (var2.size() == 1) {
               // The slam landed but a link needs two victims to share damage between.
               var1.sendMessage("§4§oOnly one player hit — a bloodlink needs at least two.");
            } else {
               var1.sendMessage("§4§oNo players hit — no bloodlink formed.");
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

   public void cleanup(Player var1) {
      UUID var2 = var1.getUniqueId();
      this.firstSawAt.remove(var2);
   }
}
