/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.GameMode
 *  org.bukkit.Keyed
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.Tag
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Arrow
 *  org.bukkit.entity.Creeper
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Fireball
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.block.BlockReceiveGameEvent
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.bukkit.event.entity.EntityDeathEvent
 *  org.bukkit.event.entity.EntityPickupItemEvent
 *  org.bukkit.event.entity.FoodLevelChangeEvent
 *  org.bukkit.event.entity.ProjectileHitEvent
 *  org.bukkit.event.entity.ProjectileLaunchEvent
 *  org.bukkit.event.inventory.CraftItemEvent
 *  org.bukkit.event.inventory.FurnaceExtractEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerItemConsumeEvent
 *  org.bukkit.event.player.PlayerItemHeldEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerToggleFlightEvent
 *  org.bukkit.event.player.PlayerToggleSprintEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.Damageable
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.projectiles.ProjectileSource
 *  org.bukkit.util.Vector
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.abilities.FluxAbilities;
import dev.xoperr.blissgems.abilities.SpeedAbilities;
import dev.xoperr.blissgems.abilities.WealthAbilities;
import dev.xoperr.blissgems.managers.FlowStateManager;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import dev.xoperr.blissgems.utils.ParticleUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

public class PassiveListener
implements Listener {
    private final BlissGems plugin;
    private final Map<UUID, Integer> jumpsRemaining = new HashMap<UUID, Integer>();

    public PassiveListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    private boolean isHoldingPuffGem(Player player) {
        String oraxenId;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        return mainHand != null && (oraxenId = CustomItemManager.getIdByItem(mainHand)) != null && oraxenId.contains("puff_gem");
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
        if (!this.canUsePassives(player)) {
            return;
        }
        int tier = this.plugin.getGemManager().getTierFor(player, GemType.ASTRA);
        double phaseChance = this.plugin.getConfigManager().getPhaseChance(tier);
        if (Math.random() < phaseChance) {
            event.setCancelled(true);
            player.sendMessage("\u00a7d\u00a7oYou phased through the attack!");
            if (this.plugin.getAchievementManager() != null && player.getHealth() <= event.getDamage()) {
                this.plugin.getAchievementManager().unlock(player, Achievement.SAVED_BY_THE_DICE);
            }
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
        if (!this.canUsePassives(player)) {
            return;
        }
        if (Tag.ENTITY_TYPES_SENSITIVE_TO_SMITE.isTagged(victim.getType())) {
            int tier = this.plugin.getGemManager().getTierFor(player, GemType.LIFE);
            double multiplier = this.plugin.getConfigManager().getUndeadDamageMultiplier(tier);
            event.setDamage(event.getDamage() * multiplier);
        }
    }

    @EventHandler
    public void onPuffGemHitPlayer(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof Player)) {
            return;
        }
        Player player = (Player)damager;
        Entity target = event.getEntity();
        if (!(target instanceof Player)) {
            return;
        }
        Player targetPlayer = (Player)target;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand == null) {
            return;
        }
        String oraxenId = CustomItemManager.getIdByItem(mainHand);
        if (oraxenId == null || !oraxenId.contains("puff_gem")) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        String abilityKey = "puff-launch";
        if (this.plugin.getAbilityManager().isOnCooldown(player, abilityKey)) {
            return;
        }
        int tier = GemType.getTierFromOraxenId(oraxenId);
        double launchVelocityValue = this.plugin.getConfigManager().getLaunchVelocity(tier);
        Vector launchVelocity = new Vector(0.0, launchVelocityValue, 0.0);
        targetPlayer.setVelocity(launchVelocity);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        targetPlayer.getWorld().playSound(targetPlayer.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 0.8f);
        targetPlayer.getWorld().spawnParticle(Particle.CLOUD, targetPlayer.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
        targetPlayer.sendMessage("\u00a7b\u00a7oYou've been launched into the sky!");
        player.sendMessage("\u00a7b\u00a7oYou launched " + targetPlayer.getName() + " into the sky!");
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!targetPlayer.isOnline()) {
                return;
            }
            Vector slamVelocity = new Vector(0.0, -3.5, 0.0);
            targetPlayer.setVelocity(slamVelocity);
            targetPlayer.getWorld().playSound(targetPlayer.getLocation(), Sound.ENTITY_BREEZE_LAND, 1.0f, 0.5f);
            targetPlayer.sendMessage("\u00a7c\u00a7oYou're being slammed down!");
            this.plugin.getPuffAbilities().impactRingOnLand(targetPlayer);
        }, 60L);
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
        if (this.plugin.getPuffAbilities().hasFallDamageImmunity(player)) {
            event.setCancelled(true);
            this.plugin.getPuffAbilities().removeFallDamageImmunity(player);
            return;
        }
        if (this.plugin.getAstraAbilities().hasDashFallImmunity(player)) {
            event.setCancelled(true);
            return;
        }
        if (!this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.PUFF) && !this.isHoldingPuffGem(player)) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        event.setCancelled(true);
        if (this.plugin.getAchievementManager() != null) {
            int prevented = (int)event.getDamage();
            this.plugin.getAchievementManager().addProgress(player, Achievement.ALMOST_FELL_FROM_GRACE, prevented);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        boolean passivesActive;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        boolean hasPuff = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.PUFF) || this.isHoldingPuffGem(player);
        boolean bl = passivesActive = hasPuff && this.canUsePassives(player);
        if (passivesActive && player.isOnGround()) {
            this.jumpsRemaining.put(player.getUniqueId(), 2);
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
        } else if (!passivesActive && player.getAllowFlight() && player.getGameMode() == GameMode.SURVIVAL) {
            player.setAllowFlight(false);
            this.jumpsRemaining.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onTripleJumpFlight(PlayerToggleFlightEvent event) {
        ItemStack mainHand;
        String oraxenId;
        boolean hasPuff;
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        boolean bl = hasPuff = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.PUFF) || this.isHoldingPuffGem(player);
        if (!hasPuff) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (FluxAbilities.isPlayerStunned(uuid) || SpeedAbilities.isPlayerFrozen(uuid)) {
            event.setCancelled(true);
            return;
        }
        int remaining = this.jumpsRemaining.getOrDefault(uuid, 0);
        if (remaining <= 0) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        player.setAllowFlight(false);
        this.jumpsRemaining.put(uuid, remaining - 1);
        int tier = this.plugin.getGemManager().getTierFor(player, GemType.PUFF);
        if (tier == 0 && (oraxenId = CustomItemManager.getIdByItem(mainHand = player.getInventory().getItemInMainHand())) != null) {
            tier = GemType.getTierFromOraxenId(oraxenId);
        }
        if (tier == 0) {
            tier = 1;
        }
        double jumpVelocity = this.plugin.getConfigManager().getDoubleJumpVelocity(tier);
        Vector velocity = player.getVelocity();
        velocity.setY(jumpVelocity);
        player.setVelocity(velocity);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.3, 0.1, 0.3, 0.05);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.5f, 1.5f);
        if (remaining - 1 > 0) {
            this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                if (player.isOnline() && !player.isOnGround() && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                    player.setAllowFlight(true);
                }
            }, 2L);
        }
    }

    @EventHandler
    public void onFarmlandTrample(PlayerInteractEvent event) {
        boolean hasPuff;
        if (event.getAction() != Action.PHYSICAL) {
            return;
        }
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.FARMLAND) {
            return;
        }
        Player player = event.getPlayer();
        boolean bl = hasPuff = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.PUFF) || this.isHoldingPuffGem(player);
        if (!hasPuff) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onCrispBlockBreak(BlockBreakEvent event) {
        if (this.plugin.getFireAbilities() != null && this.plugin.getFireAbilities().isProtectedBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("\u00a7c\u00a7oThis block is scorched and cannot be broken!");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        int tier;
        Player player = event.getPlayer();
        boolean hasFireGem = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FIRE);
        boolean hasWealthAutoSmelt = false;
        if (this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.WEALTH) && (tier = this.plugin.getGemManager().getGemTier(player)) >= 2 && this.plugin.getWealthAbilities().isAutoSmeltEnabled(player)) {
            hasWealthAutoSmelt = true;
        }
        if (!hasFireGem && !hasWealthAutoSmelt) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool != null && tool.hasItemMeta() && tool.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {
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
        if (!this.canUsePassives(player)) {
            return;
        }
        int foodLevelBefore = player.getFoodLevel();
        int foodLevelAfter = event.getFoodLevel();
        if (foodLevelAfter > foodLevelBefore) {
            int gain = foodLevelAfter - foodLevelBefore;
            int tier = this.plugin.getGemManager().getTierFor(player, GemType.LIFE);
            float saturationGain = (float)gain * (float)this.plugin.getConfigManager().getSaturationMultiplier(tier);
            player.setSaturation(player.getSaturation() + saturationGain);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Arrow)) {
            return;
        }
        ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof Player)) {
            return;
        }
        Player player = (Player)shooter;
        if (!this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FLUX)) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        Entity hitEntity = event.getHitEntity();
        if (hitEntity == null || !(hitEntity instanceof LivingEntity)) {
            return;
        }
        double chance = this.plugin.getConfig().getDouble("gems.passives.flux.shocking-chance", 0.15);
        if (Math.random() > chance) {
            return;
        }
        LivingEntity target = (LivingEntity)hitEntity;
        int tier = this.plugin.getGemManager().getTierFor(player, GemType.FLUX);
        double shockDamage = this.plugin.getConfigManager().getShockingArrowDamage(tier);
        target.damage(shockDamage, (Entity)player);
        int stunDuration = this.plugin.getConfig().getInt("abilities.durations.flux-shocking-stun", 1) * 20;
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, stunDuration, 255, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, stunDuration, 255, false, false));
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0.0, 1.0, 0.0), 60, 0.7, 0.7, 0.7);
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5);
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0.0, 1.0, 0.0), 20, 0.5, 0.5, 0.5);
        target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().add(0.0, 1.0, 0.0), 15, 0.3, 0.3, 0.3);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.5f);
        player.sendMessage("\u00a7b\u00a7oShocking Arrow hit!");
    }

    @EventHandler
    public void onSculkShriekerActivate(BlockReceiveGameEvent event) {
        int tier;
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        boolean inOffhand = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.PUFF);
        if (!inOffhand && !this.isHoldingPuffGem(player)) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        int n = tier = inOffhand ? this.plugin.getGemManager().getTierFor(player, GemType.PUFF) : GemType.getTierFromOraxenId(CustomItemManager.getIdByItem(player.getInventory().getItemInMainHand()));
        if (!this.plugin.getConfigManager().isSculkImmunity(tier)) {
            return;
        }
        Material blockType = event.getBlock().getType();
        if (blockType == Material.SCULK_SHRIEKER || blockType == Material.SCULK_SENSOR || blockType == Material.CALIBRATED_SCULK_SENSOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item.getType() != Material.GOLDEN_APPLE && item.getType() != Material.ENCHANTED_GOLDEN_APPLE) {
            return;
        }
        if (!this.plugin.getGemManager().hasGemType(player, GemType.LIFE)) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        int tier = this.plugin.getGemManager().getGemTier(player);
        int absorptionLevel = this.plugin.getConfigManager().getGoldenAppleAbsorptionLevel(tier);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, absorptionLevel, true, true));
            player.sendMessage("\u00a7a\u00a7oLife Gem enhanced your golden apple!");
        }, 1L);
    }

    @EventHandler
    public void onFireballExplosion(EntityDamageByEntityEvent event) {
        Player shooter;
        Fireball fireball;
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player damaged = (Player)event.getEntity();
        Entity damager = event.getDamager();
        if (damager instanceof Fireball && (fireball = (Fireball)damager).getShooter() instanceof Player && (shooter = (Player)fireball.getShooter()).getUniqueId().equals(damaged.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        boolean hasAstra;
        LivingEntity killed = event.getEntity();
        Player killer = killed.getKiller();
        if (killer == null) {
            return;
        }
        boolean bl = hasAstra = this.plugin.getGemManager().hasGemTypeInOffhand(killer, GemType.ASTRA) || this.isHoldingAstraGem(killer);
        if (!hasAstra) {
            return;
        }
        if (!this.plugin.getEnergyManager().arePassivesActive(killer)) {
            return;
        }
        this.plugin.getSoulManager().absorbSoul(killer, (Entity)killed);
    }

    @EventHandler
    public void onAstraSoulCapture(EntityDamageByEntityEvent event) {
        boolean hasAstra;
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getDamager();
        if (!player.isSneaking()) {
            return;
        }
        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity) || target instanceof Player) {
            return;
        }
        boolean bl = hasAstra = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.ASTRA) || this.isHoldingAstraGem(player);
        if (!hasAstra) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        LivingEntity mob = (LivingEntity)target;
        if (this.plugin.getSoulManager().captureMob(player, mob)) {
            event.setCancelled(true);
        }
    }

    private boolean isHoldingAstraGem(Player player) {
        String oraxenId;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        return mainHand != null && (oraxenId = CustomItemManager.getIdByItem(mainHand)) != null && oraxenId.contains("astra_gem");
    }

    private boolean isHoldingFluxGem(Player player) {
        String oraxenId;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        return mainHand != null && (oraxenId = CustomItemManager.getIdByItem(mainHand)) != null && oraxenId.contains("flux_gem");
    }

    @EventHandler
    public void onChargedCreeperDamage(EntityDamageByEntityEvent event) {
        boolean hasFlux;
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!(event.getDamager() instanceof Creeper)) {
            return;
        }
        Creeper creeper = (Creeper)event.getDamager();
        if (!creeper.isPowered()) {
            return;
        }
        Player player = (Player)event.getEntity();
        boolean bl = hasFlux = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FLUX) || this.isHoldingFluxGem(player);
        if (!hasFlux) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        double reduction = this.plugin.getConfig().getDouble("gems.passives.flux.charged-creeper-reduction", 1.0);
        event.setDamage(event.getDamage() * (1.0 - reduction));
        Particle.DustOptions cyan = new Particle.DustOptions(ParticleUtils.FLUX_CYAN, 1.2f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 50, 0.5, 0.5, 0.5, 0.0, (Object)cyan, true);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.1);
        player.sendMessage("\u00a7b\u26a1 \u00a7oFlux gem absorbed the charged creeper blast!");
    }

    private boolean isHoldingStrengthGem(Player player) {
        String oraxenId;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        return mainHand != null && (oraxenId = CustomItemManager.getIdByItem(mainHand)) != null && oraxenId.contains("strength_gem");
    }

    @EventHandler
    public void onStrengthBloodthorns(EntityDamageByEntityEvent event) {
        double maxHealth;
        double currentHealth;
        double healthRatio;
        double maxBonus;
        double bonusDamage;
        ItemStack mainHand;
        String oraxenId;
        boolean hasStrength;
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getDamager();
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        boolean bl = hasStrength = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.STRENGTH) || this.isHoldingStrengthGem(player);
        if (!hasStrength) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        int tier = this.plugin.getGemManager().getTierFor(player, GemType.STRENGTH);
        if (tier == 0 && (oraxenId = CustomItemManager.getIdByItem(mainHand = player.getInventory().getItemInMainHand())) != null) {
            tier = GemType.getTierFromOraxenId(oraxenId);
        }
        if (tier == 0) {
            tier = 1;
        }
        if ((bonusDamage = (maxBonus = this.plugin.getConfigManager().getBloodthornsMaxBonusDamage(tier)) * (1.0 - (healthRatio = (currentHealth = player.getHealth()) / (maxHealth = player.getMaxHealth())))) > 0.1) {
            event.setDamage(event.getDamage() + bonusDamage);
            LivingEntity target = (LivingEntity)event.getEntity();
            Particle.DustOptions redDust = new Particle.DustOptions(ParticleUtils.STRENGTH_RED, 1.0f);
            int particleCount = Math.min((int)(bonusDamage * 3.0), 30);
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), particleCount, 0.3, 0.3, 0.3, 0.0, (Object)redDust, true);
            target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0.0, 1.0, 0.0), Math.min((int)(bonusDamage * 2.0), 15), 0.3, 0.3, 0.3);
        }
    }

    private boolean isHoldingWealthGem(Player player) {
        String oraxenId;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        return mainHand != null && (oraxenId = CustomItemManager.getIdByItem(mainHand)) != null && oraxenId.contains("wealth_gem");
    }

    @EventHandler
    public void onWealthDurabilityChip(EntityDamageByEntityEvent event) {
        ItemStack mainHand;
        String oraxenId;
        boolean hasWealth;
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getDamager();
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        LivingEntity target = (LivingEntity)event.getEntity();
        boolean bl = hasWealth = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.WEALTH) || this.isHoldingWealthGem(player);
        if (!hasWealth) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        int tier = this.plugin.getGemManager().getTierFor(player, GemType.WEALTH);
        if (tier == 0 && (oraxenId = CustomItemManager.getIdByItem(mainHand = player.getInventory().getItemInMainHand())) != null) {
            tier = GemType.getTierFromOraxenId(oraxenId);
        }
        if (tier == 0) {
            tier = 1;
        }
        String tierKey = tier == 2 ? "tier2" : "tier1";
        int extraDamage = this.plugin.getConfig().getInt("passives.wealth." + tierKey + ".durability-chip-extra-damage", 1);
        if (!(target instanceof Player)) {
            return;
        }
        Player targetPlayer = (Player)target;
        ItemStack[] armor = targetPlayer.getInventory().getArmorContents();
        boolean damaged = false;
        for (int i = 0; i < armor.length; ++i) {
            if (armor[i] == null || armor[i].getType().isAir() || !(armor[i].getItemMeta() instanceof Damageable)) continue;
            Damageable damageable = (Damageable)armor[i].getItemMeta();
            damageable.setDamage(damageable.getDamage() + extraDamage);
            armor[i].setItemMeta((ItemMeta)damageable);
            damaged = true;
        }
        if (damaged) {
            targetPlayer.getInventory().setArmorContents(armor);
            Particle.DustOptions greenDust = new Particle.DustOptions(ParticleUtils.WEALTH_GREEN, 1.0f);
            targetPlayer.getWorld().spawnParticle(Particle.DUST, targetPlayer.getLocation().add(0.0, 1.0, 0.0), 10, 0.3, 0.3, 0.3, 0.0, (Object)greenDust, true);
        }
    }

    @EventHandler
    public void onWealthArmorMend(EntityDamageByEntityEvent event) {
        ItemStack mainHand;
        String oraxenId;
        boolean hasWealth;
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getDamager();
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        if (event.getEntity() == player) {
            return;
        }
        boolean bl = hasWealth = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.WEALTH) || this.isHoldingWealthGem(player);
        if (!hasWealth) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        int tier = this.plugin.getGemManager().getTierFor(player, GemType.WEALTH);
        if (tier == 0 && (oraxenId = CustomItemManager.getIdByItem(mainHand = player.getInventory().getItemInMainHand())) != null) {
            tier = GemType.getTierFromOraxenId(oraxenId);
        }
        if (tier == 0) {
            tier = 1;
        }
        String tierKey = tier == 2 ? "tier2" : "tier1";
        int repairAmount = this.plugin.getConfig().getInt("passives.wealth." + tierKey + ".armor-mend-amount", 2);
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean repaired = false;
        for (int i = 0; i < armor.length; ++i) {
            Damageable damageable;
            int currentDamage;
            if (armor[i] == null || armor[i].getType().isAir() || !(armor[i].getItemMeta() instanceof Damageable) || (currentDamage = (damageable = (Damageable)armor[i].getItemMeta()).getDamage()) <= 0) continue;
            damageable.setDamage(Math.max(0, currentDamage - repairAmount));
            armor[i].setItemMeta((ItemMeta)damageable);
            repaired = true;
        }
        if (repaired) {
            player.getInventory().setArmorContents(armor);
            Particle.DustOptions greenDust = new Particle.DustOptions(ParticleUtils.WEALTH_GREEN, 0.8f);
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 8, 0.3, 0.3, 0.3, 0.0, (Object)greenDust, true);
        }
    }

    @EventHandler
    public void onWealthDoubleDebris(FurnaceExtractEvent event) {
        ItemStack mainHand;
        String oraxenId;
        boolean hasWealth;
        Player player = event.getPlayer();
        boolean bl = hasWealth = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.WEALTH) || this.isHoldingWealthGem(player);
        if (!hasWealth) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        int tier = this.plugin.getGemManager().getTierFor(player, GemType.WEALTH);
        if (tier == 0 && (oraxenId = CustomItemManager.getIdByItem(mainHand = player.getInventory().getItemInMainHand())) != null) {
            tier = GemType.getTierFromOraxenId(oraxenId);
        }
        if (tier == 0) {
            tier = 1;
        }
        String tierKey = tier == 2 ? "tier2" : "tier1";
        boolean enabled = this.plugin.getConfig().getBoolean("passives.wealth." + tierKey + ".double-debris", true);
        if (!enabled) {
            return;
        }
        if (event.getItemType() != Material.NETHERITE_SCRAP) {
            return;
        }
        int extracted = event.getItemAmount();
        if (extracted <= 0) {
            return;
        }
        player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.NETHERITE_SCRAP, extracted));
        Particle.DustOptions greenDust = new Particle.DustOptions(ParticleUtils.WEALTH_GREEN, 1.2f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 20, 0.3, 0.3, 0.3, 0.0, (Object)greenDust, true);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        player.sendMessage("\u00a7a\u00a7oDouble Debris! Extra netherite scrap!");
    }

    @EventHandler
    public void onUnfortunateAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getDamager();
        double attackFailChance = this.plugin.getConfig().getDouble("passives.unfortunate.attack-fail-chance", 0.75);
        if (WealthAbilities.shouldUnfortunateFail(player.getUniqueId(), attackFailChance)) {
            event.setCancelled(true);
            player.sendMessage("\u00a7c\u00a7oYou can't do that while Unfortunate!");
        }
    }

    @EventHandler
    public void onUnfortunateBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        double blockPlaceFailChance = this.plugin.getConfig().getDouble("passives.unfortunate.block-place-fail-chance", 1.0);
        if (WealthAbilities.shouldUnfortunateFail(player.getUniqueId(), blockPlaceFailChance)) {
            event.setCancelled(true);
            player.sendMessage("\u00a7c\u00a7oYou can't do that while Unfortunate!");
        }
    }

    @EventHandler
    public void onUnfortunateEat(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        double eatFailChance = this.plugin.getConfig().getDouble("passives.unfortunate.eat-fail-chance", 1.0);
        if (WealthAbilities.shouldUnfortunateFail(player.getUniqueId(), eatFailChance)) {
            event.setCancelled(true);
            player.sendMessage("\u00a7c\u00a7oYou can't do that while Unfortunate!");
        }
    }

    @EventHandler
    public void onUnfortunateBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        double blockBreakFailChance = this.plugin.getConfig().getDouble("passives.unfortunate.block-break-fail-chance", 0.5);
        if (WealthAbilities.shouldUnfortunateFail(player.getUniqueId(), blockBreakFailChance)) {
            event.setCancelled(true);
            player.sendMessage("\u00a7c\u00a7oYou can't do that while Unfortunate!");
        }
    }

    @EventHandler
    public void onUnfortunateSprint(PlayerToggleSprintEvent event) {
        if (!event.isSprinting()) {
            return;
        }
        Player player = event.getPlayer();
        double sprintFailChance = this.plugin.getConfig().getDouble("passives.unfortunate.sprint-fail-chance", 0.5);
        if (WealthAbilities.shouldUnfortunateFail(player.getUniqueId(), sprintFailChance)) {
            event.setCancelled(true);
            player.sendMessage("\u00a7c\u00a7oYou can't do that while Unfortunate!");
        }
    }

    @EventHandler
    public void onUnfortunateJump(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom().getY() < event.getTo().getY() && player.isOnGround()) {
            double jumpFailChance = this.plugin.getConfig().getDouble("passives.unfortunate.jump-fail-chance", 0.3);
            if (WealthAbilities.shouldUnfortunateFail(player.getUniqueId(), jumpFailChance)) {
                event.setTo(event.getFrom());
                player.sendMessage("\u00a7c\u00a7oYou can't do that while Unfortunate!");
            }
        }
    }

    @EventHandler
    public void onUnfortunateDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        double dropItemFailChance = this.plugin.getConfig().getDouble("passives.unfortunate.drop-item-fail-chance", 0.8);
        if (WealthAbilities.shouldUnfortunateFail(player.getUniqueId(), dropItemFailChance)) {
            event.setCancelled(true);
            player.sendMessage("\u00a7c\u00a7oYou can't do that while Unfortunate!");
        }
    }

    @EventHandler
    public void onUnfortunatePickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getEntity();
        double pickupItemFailChance = this.plugin.getConfig().getDouble("passives.unfortunate.pickup-item-fail-chance", 0.7);
        if (WealthAbilities.shouldUnfortunateFail(player.getUniqueId(), pickupItemFailChance)) {
            event.setCancelled(true);
            player.sendMessage("\u00a7c\u00a7oYou can't do that while Unfortunate!");
        }
    }

    @EventHandler
    public void onUnfortunateInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        double interactFailChance = this.plugin.getConfig().getDouble("passives.unfortunate.interact-fail-chance", 0.6);
        if (WealthAbilities.shouldUnfortunateFail(player.getUniqueId(), interactFailChance)) {
            event.setCancelled(true);
            player.sendMessage("\u00a7c\u00a7oYou can't do that while Unfortunate!");
        }
    }

    @EventHandler
    public void onUnfortunateCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getWhoClicked();
        double craftFailChance = this.plugin.getConfig().getDouble("passives.unfortunate.craft-fail-chance", 0.9);
        if (WealthAbilities.shouldUnfortunateFail(player.getUniqueId(), craftFailChance)) {
            event.setCancelled(true);
            player.sendMessage("\u00a7c\u00a7oYou can't do that while Unfortunate!");
        }
    }

    @EventHandler
    public void onItemLockUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!WealthAbilities.isItemLocked(player.getUniqueId())) {
            return;
        }
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        ItemStack lockedItem = WealthAbilities.getLockedItem(player.getUniqueId());
        if (lockedItem == null || heldItem == null) {
            return;
        }
        if (heldItem.isSimilar(lockedItem)) {
            event.setCancelled(true);
            player.sendMessage("\u00a7c\u00a7oThis item is locked!");
        }
    }

    @EventHandler
    public void onItemLockSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!WealthAbilities.isItemLocked(player.getUniqueId())) {
            return;
        }
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        ItemStack lockedItem = WealthAbilities.getLockedItem(player.getUniqueId());
        if (lockedItem == null || newItem == null) {
            return;
        }
        if (newItem.isSimilar(lockedItem)) {
            player.sendMessage("\u00a7c\u00a7oWarning: This item is locked!");
        }
    }

    @EventHandler
    public void onStrengthChadHit(EntityDamageByEntityEvent event) {
        ItemStack mainHand;
        String oraxenId;
        boolean hasStrength;
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getDamager();
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        boolean hasActiveChad = this.plugin.getStrengthAbilities() != null && this.plugin.getStrengthAbilities().getChadHitsRemaining(player) > 0;
        hasStrength = this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.STRENGTH) || this.isHoldingStrengthGem(player) || hasActiveChad;
        if (!hasStrength) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        int tier = this.plugin.getGemManager().getTierFor(player, GemType.STRENGTH);
        if (tier == 0 && (oraxenId = CustomItemManager.getIdByItem(mainHand = player.getInventory().getItemInMainHand())) != null) {
            tier = GemType.getTierFromOraxenId(oraxenId);
        }
        if (tier < 2 && !hasActiveChad) {
            return;
        }
        double bonusDamage = this.plugin.getStrengthAbilities().consumeChadBonus(player);
        if (bonusDamage <= 0.0) {
            return;
        }
        event.setDamage(event.getDamage() + bonusDamage);
        LivingEntity target = (LivingEntity)event.getEntity();
        Particle.DustOptions redDust = new Particle.DustOptions(ParticleUtils.STRENGTH_RED, 2.0f);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 50, 0.8, 0.8, 0.8, 0.0, (Object)redDust, true);
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.3);
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0.0, 1.0, 0.0), 10, 0.5, 0.5, 0.5);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.7f);
    }

    @EventHandler
    public void onRichRushBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!WealthAbilities.hasRichRush(player.getUniqueId())) {
            return;
        }
        if (event.isCancelled()) {
            return;
        }
        Material type = event.getBlock().getType();
        if (!this.isOreBlock(type)) {
            return;
        }
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool != null && tool.hasItemMeta() && tool.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {
            return;
        }
        if (type == Material.ANCIENT_DEBRIS) {
            return;
        }
        ArrayList<ItemStack> drops = new ArrayList<>(event.getBlock().getDrops(player.getInventory().getItemInMainHand(), (Entity)player));
        for (ItemStack drop : drops) {
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop.clone());
        }
    }

    private boolean isOreBlock(Material type) {
        return type == Material.COAL_ORE || type == Material.DEEPSLATE_COAL_ORE || type == Material.IRON_ORE || type == Material.DEEPSLATE_IRON_ORE || type == Material.GOLD_ORE || type == Material.DEEPSLATE_GOLD_ORE || type == Material.NETHER_GOLD_ORE || type == Material.COPPER_ORE || type == Material.DEEPSLATE_COPPER_ORE || type == Material.DIAMOND_ORE || type == Material.DEEPSLATE_DIAMOND_ORE || type == Material.EMERALD_ORE || type == Material.DEEPSLATE_EMERALD_ORE || type == Material.LAPIS_ORE || type == Material.DEEPSLATE_LAPIS_ORE || type == Material.REDSTONE_ORE || type == Material.DEEPSLATE_REDSTONE_ORE || type == Material.NETHER_QUARTZ_ORE || type == Material.ANCIENT_DEBRIS;
    }

    @EventHandler
    public void onRichRushEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        if (!WealthAbilities.hasRichRush(killer.getUniqueId())) {
            return;
        }
        EntityType entityType = event.getEntity().getType();
        if (entityType == EntityType.DONKEY || entityType == EntityType.MULE || entityType == EntityType.LLAMA || entityType == EntityType.TRADER_LLAMA) {
            return;
        }
        ArrayList<ItemStack> originalDrops = new ArrayList<>(event.getDrops());
        for (ItemStack drop : originalDrops) {
            if (!this.isNaturalMobDrop(drop)) continue;
            event.getDrops().add(drop.clone());
        }
    }

    private boolean isNaturalMobDrop(ItemStack item) {
        ItemMeta meta;
        if (item == null || item.getType().isAir()) {
            return false;
        }
        if (CustomItemManager.isUndroppable(item)) {
            return false;
        }
        String customId = CustomItemManager.getIdByItem(item);
        if (customId != null && this.plugin.getGemManager().isAnyGem(customId)) {
            return false;
        }
        Material type = item.getType();
        if (type.name().contains("HELMET") || type.name().contains("CHESTPLATE") || type.name().contains("LEGGINGS") || type.name().contains("BOOTS") || type.name().contains("SWORD") || type.name().contains("AXE") || type.name().contains("PICKAXE") || type.name().contains("SHOVEL") || type.name().contains("HOE") || type.name().contains("BOW") || type.name().contains("CROSSBOW") || type.name().contains("TRIDENT") || type.name().contains("SHIELD")) {
            return false;
        }
        if (type.name().contains("SHULKER") || type.name().contains("CHEST") || type.name().contains("BARREL") || type.name().contains("BUNDLE") || type == Material.SADDLE || type.name().contains("HORSE_ARMOR")) {
            return false;
        }
        if (item.hasItemMeta() && ((meta = item.getItemMeta()).hasDisplayName() || meta.hasLore())) {
            return false;
        }
        if (type == Material.ROTTEN_FLESH || type == Material.BONE || type == Material.SPIDER_EYE || type == Material.STRING || type == Material.GUNPOWDER || type == Material.ENDER_PEARL || type == Material.BLAZE_ROD || type == Material.GHAST_TEAR || type == Material.MAGMA_CREAM || type == Material.SLIME_BALL || type == Material.PRISMARINE_SHARD || type == Material.PRISMARINE_CRYSTALS || type == Material.RABBIT_HIDE || type == Material.RABBIT_FOOT || type == Material.PHANTOM_MEMBRANE || type == Material.NAUTILUS_SHELL) {
            return true;
        }
        if (type == Material.BEEF || type == Material.PORKCHOP || type == Material.MUTTON || type == Material.CHICKEN || type == Material.RABBIT || type == Material.COD || type == Material.SALMON || type == Material.TROPICAL_FISH || type == Material.PUFFERFISH) {
            return true;
        }
        if (type == Material.COOKED_BEEF || type == Material.COOKED_PORKCHOP || type == Material.COOKED_MUTTON || type == Material.COOKED_CHICKEN || type == Material.COOKED_RABBIT || type == Material.COOKED_COD || type == Material.COOKED_SALMON) {
            return true;
        }
        if (type == Material.LEATHER || type == Material.FEATHER || type == Material.INK_SAC || type == Material.GLOW_INK_SAC) {
            return true;
        }
        if (type == Material.NETHER_STAR || type == Material.WITHER_SKELETON_SKULL || type == Material.SKELETON_SKULL || type == Material.ZOMBIE_HEAD || type == Material.CREEPER_HEAD || type == Material.DRAGON_HEAD) {
            return true;
        }
        if (type == Material.TOTEM_OF_UNDYING || type == Material.ELYTRA || type == Material.DRAGON_BREATH) {
            return true;
        }
        if (type == Material.ARROW || type == Material.EGG || type == Material.SNOWBALL) {
            return true;
        }
        if (type == Material.ECHO_SHARD || type == Material.SCULK_CATALYST) {
            return true;
        }
        if (type == Material.SNIFFER_EGG) {
            return true;
        }
        return type == Material.OCHRE_FROGLIGHT || type == Material.PEARLESCENT_FROGLIGHT || type == Material.VERDANT_FROGLIGHT;
    }

    @EventHandler
    public void onFluxFlowBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FLUX)) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        this.plugin.getFlowStateManager().registerAction(player, FlowStateManager.ActionType.BLOCK_BREAK);
    }

    @EventHandler
    public void onFluxFlowAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getDamager();
        if (!this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FLUX)) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        this.plugin.getFlowStateManager().registerAction(player, FlowStateManager.ActionType.ATTACK);
    }

    @EventHandler
    public void onFluxFlowArrowShoot(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Arrow)) {
            return;
        }
        ProjectileSource shooter = projectile.getShooter();
        if (!(shooter instanceof Player)) {
            return;
        }
        Player player = (Player)shooter;
        if (!this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FLUX)) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        this.plugin.getFlowStateManager().registerAction(player, FlowStateManager.ActionType.ARROW_SHOOT);
    }

    @EventHandler
    public void onFluxFlowSprint(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        if (!event.isSprinting()) {
            return;
        }
        if (!this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FLUX)) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        this.plugin.getFlowStateManager().registerAction(player, FlowStateManager.ActionType.SPRINT);
    }

    @EventHandler
    public void onFluxFlowJump(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getTo().getY() <= event.getFrom().getY()) {
            return;
        }
        if (!player.isOnGround()) {
            return;
        }
        if (!this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FLUX)) {
            return;
        }
        if (!this.canUsePassives(player)) {
            return;
        }
        this.plugin.getFlowStateManager().registerAction(player, FlowStateManager.ActionType.JUMP);
    }

    private boolean canUsePassives(Player player) {
        if (this.plugin.getGemLockManager() != null && this.plugin.getGemLockManager().isLocked(player)) {
            return false;
        }
        if (!this.plugin.getEnergyManager().arePassivesActive(player)) {
            return false;
        }
        return this.plugin.getRegionManager() == null || !this.plugin.getRegionManager().areGemsDisabled(player);
    }
}

