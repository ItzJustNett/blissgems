/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.entity.Arrow
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockReceiveGameEvent
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.bukkit.event.entity.FoodLevelChangeEvent
 *  org.bukkit.event.entity.ProjectileHitEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerToggleSneakEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.projectiles.ProjectileSource
 *  org.bukkit.util.Vector
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.GemType;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.GameMode;
import dev.xoperr.blissgems.abilities.FluxAbilities;
import dev.xoperr.blissgems.abilities.SpeedAbilities;
import org.bukkit.block.Block;
import org.bukkit.Location;
import org.bukkit.entity.Creeper;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;
import org.bukkit.plugin.Plugin;
import dev.xoperr.blissgems.utils.ParticleUtils;

public class PassiveListener
implements Listener {
    private final BlissGems plugin;
    private final Map<UUID, Integer> jumpsRemaining = new HashMap<>();

    public PassiveListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    private boolean isHoldingPuffGem(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null) {
            String oraxenId = CustomItemManager.getIdByItem(mainHand);
            if (oraxenId != null && oraxenId.contains("puff_gem")) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        if (!this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.ASTRA)) {
            return;
        }
        if (!this.plugin.getEnergyManager().arePassivesActive(player)) {
            return;
        }
        int tier = this.plugin.getGemManager().getTierFromOffhand(player);
        double phaseChance = this.plugin.getConfigManager().getPhaseChance(tier);
        if (Math.random() < phaseChance) {
            event.setCancelled(true);
            player.sendMessage("\u00a7d\u00a7oYou phased through the attack!");
        }
    }

    @EventHandler
    public void onPlayerDamageEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        Entity entity2 = event.getEntity();
        if (!(entity2 instanceof LivingEntity)) {
            return;
        }
        LivingEntity victim = (LivingEntity)entity2;
        if (!this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.LIFE)) {
            return;
        }
        if (!this.plugin.getEnergyManager().arePassivesActive(player)) {
            return;
        }
        if (Tag.ENTITY_TYPES_SENSITIVE_TO_SMITE.isTagged(victim.getType())) {
            int tier = this.plugin.getGemManager().getTierFromOffhand(player);
            double multiplier = this.plugin.getConfigManager().getUndeadDamageMultiplier(tier);
            event.setDamage(event.getDamage() * multiplier);
        }
    }

    @EventHandler
    public void onPuffGemHitPlayer(EntityDamageByEntityEvent event) {
        // Puff Gem - Launch player on hit
        Entity damager = event.getDamager();
        if (!(damager instanceof Player)) {
            return;
        }
        Player player = (Player) damager;

        Entity target = event.getEntity();
        if (!(target instanceof Player)) {
            return;
        }
        Player targetPlayer = (Player) target;

        // Check if player is holding Puff gem in main hand
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand == null) {
            return;
        }
        String oraxenId = CustomItemManager.getIdByItem(mainHand);
        if (oraxenId == null || !oraxenId.contains("puff_gem")) {
            return;
        }
        if (!this.plugin.getEnergyManager().arePassivesActive(player)) {
            return;
        }

        // Get tier and launch velocity
        int tier = GemType.getTierFromOraxenId(oraxenId);
        double launchVelocityValue = this.plugin.getConfigManager().getLaunchVelocity(tier);

        // Launch target player up
        Vector launchVelocity = new Vector(0, launchVelocityValue, 0);
        targetPlayer.setVelocity(launchVelocity);

        // Play launch sound
        targetPlayer.getWorld().playSound(targetPlayer.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 0.8f);
        targetPlayer.getWorld().spawnParticle(Particle.CLOUD, targetPlayer.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);

        targetPlayer.sendMessage("\u00a7b\u00a7oYou've been launched into the sky!");
        player.sendMessage("\u00a7b\u00a7oYou launched " + targetPlayer.getName() + " into the sky!");

        // After 3 seconds, slam them down
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!targetPlayer.isOnline()) return;

            // Slam down with high velocity
            Vector slamVelocity = new Vector(0, -3.5, 0);
            targetPlayer.setVelocity(slamVelocity);

            targetPlayer.getWorld().playSound(targetPlayer.getLocation(), Sound.ENTITY_BREEZE_LAND, 1.0f, 0.5f);
            targetPlayer.sendMessage("\u00a7c\u00a7oYou're being slammed down!");
        }, 60L); // 3 seconds = 60 ticks
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        // Check for ability-based fall damage immunity (from breezyBash)
        if (this.plugin.getPuffAbilities().hasFallDamageImmunity(player)) {
            event.setCancelled(true);
            this.plugin.getPuffAbilities().removeFallDamageImmunity(player);
            return;
        }

        // Check for passive immunity (gem in either hand)
        if (!this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.PUFF) && !isHoldingPuffGem(player)) {
            return;
        }
        if (!this.plugin.getEnergyManager().arePassivesActive(player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Only handle triple jump for survival/adventure mode
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        boolean hasPuff = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.PUFF) || isHoldingPuffGem(player);
        boolean passivesActive = hasPuff && this.plugin.getEnergyManager().arePassivesActive(player);

        if (passivesActive && player.isOnGround()) {
            // Reset jump counter on landing: 2 extra midair jumps = triple jump
            jumpsRemaining.put(player.getUniqueId(), 2);
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
        } else if (!passivesActive && player.getAllowFlight() && player.getGameMode() == GameMode.SURVIVAL) {
            player.setAllowFlight(false);
            jumpsRemaining.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onTripleJumpFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        // Don't interfere with creative/spectator flight
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        boolean hasPuff = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.PUFF) || isHoldingPuffGem(player);
        if (!hasPuff) {
            return;
        }
        if (!this.plugin.getEnergyManager().arePassivesActive(player)) {
            return;
        }

        // Skip if player is stunned/frozen
        UUID uuid = player.getUniqueId();
        if (FluxAbilities.isPlayerStunned(uuid) || SpeedAbilities.isPlayerFrozen(uuid)) {
            event.setCancelled(true);
            return;
        }

        int remaining = jumpsRemaining.getOrDefault(uuid, 0);
        if (remaining <= 0) {
            event.setCancelled(true);
            return;
        }

        // Cancel the flight — this is a midair jump, not actual flight
        event.setCancelled(true);
        player.setAllowFlight(false);
        jumpsRemaining.put(uuid, remaining - 1);

        // Apply jump velocity
        int tier = this.plugin.getGemManager().getTierFromOffhand(player);
        if (tier == 0) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            String oraxenId = CustomItemManager.getIdByItem(mainHand);
            if (oraxenId != null) {
                tier = GemType.getTierFromOraxenId(oraxenId);
            }
        }
        if (tier == 0) tier = 1;

        double jumpVelocity = this.plugin.getConfigManager().getDoubleJumpVelocity(tier);
        Vector velocity = player.getVelocity();
        velocity.setY(jumpVelocity);
        player.setVelocity(velocity);

        // Cloud particles + sound
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.3, 0.1, 0.3, 0.05);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.5f, 1.5f);

        // Re-enable flight for next jump if jumps remain
        if (remaining - 1 > 0) {
            this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                if (player.isOnline() && !player.isOnGround()
                    && player.getGameMode() != GameMode.CREATIVE
                    && player.getGameMode() != GameMode.SPECTATOR) {
                    player.setAllowFlight(true);
                }
            }, 2L);
        }
    }

    @EventHandler
    public void onFarmlandTrample(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) {
            return;
        }
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.FARMLAND) {
            return;
        }
        Player player = event.getPlayer();
        boolean hasPuff = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.PUFF) || isHoldingPuffGem(player);
        if (!hasPuff) {
            return;
        }
        if (!this.plugin.getEnergyManager().arePassivesActive(player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Check if player has Fire gem (always has auto-smelt)
        boolean hasFireGem = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FIRE);

        // Check if player has Wealth gem Tier 2 with auto-smelt enabled
        boolean hasWealthAutoSmelt = false;
        if (this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.WEALTH)) {
            int tier = this.plugin.getGemManager().getGemTier(player);
            if (tier >= 2 && this.plugin.getWealthAbilities().isAutoSmeltEnabled(player)) {
                hasWealthAutoSmelt = true;
            }
        }

        // If neither condition is met, return
        if (!hasFireGem && !hasWealthAutoSmelt) {
            return;
        }

        if (!this.plugin.getEnergyManager().arePassivesActive(player)) {
            return;
        }

        Material blockType = event.getBlock().getType();
        ItemStack result = this.getSmeltedItem(blockType);
        if (result != null) {
            event.setDropItems(false);
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), result);
        }
    }

    private ItemStack getSmeltedItem(Material blockType) {
        return switch (blockType) {
            case IRON_ORE, DEEPSLATE_IRON_ORE -> new ItemStack(Material.IRON_INGOT);
            case GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> new ItemStack(Material.GOLD_INGOT);
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> new ItemStack(Material.COPPER_INGOT);
            case ANCIENT_DEBRIS -> new ItemStack(Material.NETHERITE_SCRAP);
            case SAND -> new ItemStack(Material.GLASS);
            case COBBLESTONE -> new ItemStack(Material.STONE);
            case RAW_IRON_BLOCK -> new ItemStack(Material.IRON_BLOCK);
            case RAW_GOLD_BLOCK -> new ItemStack(Material.GOLD_BLOCK);
            case RAW_COPPER_BLOCK -> new ItemStack(Material.COPPER_BLOCK);
            default -> null;
        };
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        HumanEntity humanEntity = event.getEntity();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        if (!this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.LIFE)) {
            return;
        }
        if (!this.plugin.getEnergyManager().arePassivesActive(player)) {
            return;
        }
        int foodLevelBefore = player.getFoodLevel();
        int foodLevelAfter = event.getFoodLevel();
        if (foodLevelAfter > foodLevelBefore) {
            int gain = foodLevelAfter - foodLevelBefore;
            int tier = this.plugin.getGemManager().getTierFromOffhand(player);
            float saturationGain = (float)gain * (float)this.plugin.getConfigManager().getSaturationMultiplier(tier);
            player.setSaturation(player.getSaturation() + saturationGain);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        // Flux Gem - Shocking Arrows
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Arrow)) {
            return;
        }

        ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof Player)) {
            return;
        }

        Player player = (Player) shooter;
        if (!this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FLUX)) {
            return;
        }
        if (!this.plugin.getEnergyManager().arePassivesActive(player)) {
            return;
        }

        Entity hitEntity = event.getHitEntity();
        if (hitEntity == null || !(hitEntity instanceof LivingEntity)) {
            return;
        }

        // 15% chance to trigger shocking arrow
        double chance = this.plugin.getConfig().getDouble("gems.passives.flux.shocking-chance", 0.15);
        if (Math.random() > chance) {
            return;
        }

        LivingEntity target = (LivingEntity) hitEntity;

        // Deal electric damage based on tier
        int tier = this.plugin.getGemManager().getTierFromOffhand(player);
        double shockDamage = this.plugin.getConfigManager().getShockingArrowDamage(tier);
        target.damage(shockDamage, player);

        // Apply 1-second stun (Slowness 255 + Mining Fatigue 255)
        int stunDuration = this.plugin.getConfig().getInt("abilities.durations.flux-shocking-stun", 1) * 20;
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, stunDuration, 255, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, stunDuration, 255, false, false));

        // Spawn electric particles
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 60, 0.7, 0.7, 0.7);
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5);
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5);
        target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.5f);

        // Notify player about the shock
        player.sendMessage("\u00a7b\u00a7oShocking Arrow hit!");
    }

    @EventHandler
    public void onSculkShriekerActivate(BlockReceiveGameEvent event) {
        // Puff Gem - Sculk Shrieker immunity (Tier 2 only)
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }

        Player player = (Player) entity;
        if (!this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.PUFF)) {
            return;
        }
        if (!this.plugin.getEnergyManager().arePassivesActive(player)) {
            return;
        }

        // Check if sculk immunity is enabled for this tier
        int tier = this.plugin.getGemManager().getTierFromOffhand(player);
        if (!this.plugin.getConfigManager().isSculkImmunity(tier)) {
            return;
        }

        // Check if this is a sculk shrieker or sensor
        Material blockType = event.getBlock().getType();
        if (blockType == Material.SCULK_SHRIEKER || blockType == Material.SCULK_SENSOR || blockType == Material.CALIBRATED_SCULK_SENSOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        // Life Gem - Golden Apple Absorption Bonus (tier-specific)
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item.getType() != Material.GOLDEN_APPLE && item.getType() != Material.ENCHANTED_GOLDEN_APPLE) {
            return;
        }

        if (!this.plugin.getGemManager().hasGemType(player, GemType.LIFE)) {
            return;
        }

        if (!this.plugin.getEnergyManager().arePassivesActive(player)) {
            return;
        }

        // Get tier-specific absorption level
        int tier = this.plugin.getGemManager().getGemTier(player);
        int absorptionLevel = this.plugin.getConfigManager().getGoldenAppleAbsorptionLevel(tier);

        // Schedule for next tick to apply after the apple's default effects
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, absorptionLevel, true, true));
            player.sendMessage("\u00a7a\u00a7oLife Gem enhanced your golden apple!");
        }, 1L);
    }

    @EventHandler
    public void onFireballExplosion(EntityDamageByEntityEvent event) {
        // Prevent Fire Gem fireball from damaging its owner
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player damaged = (Player) event.getEntity();
        Entity damager = event.getDamager();

        // Check if damage is from a Fireball
        if (damager instanceof org.bukkit.entity.Fireball) {
            org.bukkit.entity.Fireball fireball = (org.bukkit.entity.Fireball) damager;

            // Check if the shooter is the damaged player
            if (fireball.getShooter() instanceof Player) {
                Player shooter = (Player) fireball.getShooter();

                // If player shot their own fireball, cancel the damage
                if (shooter.getUniqueId().equals(damaged.getUniqueId())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // ==========================================================================
    // Astra Gem — Soul Healing (passive: heal on kill)
    // ==========================================================================

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity killed = event.getEntity();
        Player killer = killed.getKiller();

        if (killer == null) return;

        // Check if killer has Astra gem (offhand or mainhand)
        boolean hasAstra = this.plugin.getGemManager().hasGemTypeInOffhand(killer, GemType.ASTRA)
                        || isHoldingAstraGem(killer);
        if (!hasAstra) return;

        if (!this.plugin.getEnergyManager().arePassivesActive(killer)) return;

        // Soul Healing — heal on kill
        this.plugin.getSoulManager().absorbSoul(killer, killed);
    }

    // ==========================================================================
    // Astra Gem — Soul Capture (passive: sneak + hit mob to capture)
    // ==========================================================================

    @EventHandler
    public void onAstraSoulCapture(EntityDamageByEntityEvent event) {
        // Only trigger on sneak + attack against non-player mobs
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        if (!player.isSneaking()) return;

        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity) || target instanceof Player) return;

        // Check Astra gem in offhand or mainhand
        boolean hasAstra = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.ASTRA)
                        || isHoldingAstraGem(player);
        if (!hasAstra) return;

        if (!this.plugin.getEnergyManager().arePassivesActive(player)) return;

        // Attempt soul capture — cancel the damage so the mob is captured, not killed
        LivingEntity mob = (LivingEntity) target;
        if (this.plugin.getSoulManager().captureMob(player, mob)) {
            event.setCancelled(true);
        }
    }

    private boolean isHoldingAstraGem(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null) {
            String oraxenId = CustomItemManager.getIdByItem(mainHand);
            if (oraxenId != null && oraxenId.contains("astra_gem")) {
                return true;
            }
        }
        return false;
    }

    private boolean isHoldingFluxGem(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null) {
            String oraxenId = CustomItemManager.getIdByItem(mainHand);
            if (oraxenId != null && oraxenId.contains("flux_gem")) {
                return true;
            }
        }
        return false;
    }

    // ==========================================================================
    // Flux Gem — Conduction (passive: sneak + left click to teleport to copper)
    // ==========================================================================

    @EventHandler
    public void onFluxConduction(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        // Check Flux gem in offhand or mainhand
        boolean hasFlux = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FLUX)
                        || isHoldingFluxGem(player);
        if (!hasFlux) return;

        if (!this.plugin.getEnergyManager().arePassivesActive(player)) return;

        // Check cooldown
        String abilityKey = "flux-conduction";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }

        // Find nearest copper block within range
        int range = this.plugin.getConfig().getInt("abilities.flux-conduction.range", 10);
        Location playerLoc = player.getLocation();
        Block closest = null;
        double closestDist = Double.MAX_VALUE;

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    Block block = playerLoc.getBlock().getRelative(x, y, z);
                    if (block.getType().name().contains("COPPER") && !block.getType().name().contains("COPPERHEAD")) {
                        double dist = block.getLocation().distanceSquared(playerLoc);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = block;
                        }
                    }
                }
            }
        }

        if (closest == null) {
            return; // No copper nearby, silently fail
        }

        // Teleport on top of the copper block
        Location departure = player.getLocation().clone();
        Location tpLoc = closest.getLocation().add(0.5, 1, 0.5);
        tpLoc.setYaw(player.getLocation().getYaw());
        tpLoc.setPitch(player.getLocation().getPitch());
        player.teleport(tpLoc);

        // Particles at departure
        Particle.DustOptions cyan = new Particle.DustOptions(ParticleUtils.FLUX_CYAN, 1.2f);
        departure.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, departure.add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
        departure.getWorld().spawnParticle(Particle.DUST, departure, 40, 0.5, 0.5, 0.5, 0.0, cyan, true);

        // Particles at arrival
        tpLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, tpLoc.clone().add(0, 0.5, 0), 50, 0.5, 0.5, 0.5, 0.1);
        tpLoc.getWorld().spawnParticle(Particle.DUST, tpLoc.clone().add(0, 0.5, 0), 40, 0.5, 0.5, 0.5, 0.0, cyan, true);

        // Sound
        player.playSound(departure, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
        player.playSound(tpLoc, Sound.BLOCK_COPPER_BULB_TURN_ON, 1.0f, 1.2f);

        player.sendMessage("\u00a7b\u26a1 \u00a7oConducted to copper!");

        this.plugin.getAbilityManager().useAbility(player, abilityKey);
    }

    // ==========================================================================
    // Flux Gem — Charged Creeper Immunity
    // ==========================================================================

    @EventHandler
    public void onChargedCreeperDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Creeper)) return;

        Creeper creeper = (Creeper) event.getDamager();
        if (!creeper.isPowered()) return;

        Player player = (Player) event.getEntity();

        // Check Flux gem
        boolean hasFlux = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FLUX)
                        || isHoldingFluxGem(player);
        if (!hasFlux) return;

        if (!this.plugin.getEnergyManager().arePassivesActive(player)) return;

        // Reduce damage
        double reduction = this.plugin.getConfig().getDouble("gems.passives.flux.charged-creeper-reduction", 1.0);
        event.setDamage(event.getDamage() * (1.0 - reduction));

        // Protective particles
        Particle.DustOptions cyan = new Particle.DustOptions(ParticleUtils.FLUX_CYAN, 1.2f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.0, cyan, true);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);

        player.sendMessage("\u00a7b\u26a1 \u00a7oFlux gem absorbed the charged creeper blast!");
    }
}

