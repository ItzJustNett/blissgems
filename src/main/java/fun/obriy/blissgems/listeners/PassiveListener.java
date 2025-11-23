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
package fun.obriy.blissgems.listeners;

import fun.obriy.blissgems.BlissGems;
import fun.obriy.blissgems.utils.GemType;
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
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;
import org.bukkit.plugin.Plugin;

public class PassiveListener
implements Listener {
    private final BlissGems plugin;
    private final Map<UUID, Boolean> doubleJumpReady;
    private final Map<UUID, Long> lastJumpTime;

    public PassiveListener(BlissGems plugin) {
        this.plugin = plugin;
        this.doubleJumpReady = new HashMap<UUID, Boolean>();
        this.lastJumpTime = new HashMap<UUID, Long>();
    }

    private boolean isHoldingPuffGem(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null) {
            String oraxenId = io.th0rgal.oraxen.api.OraxenItems.getIdByItem(mainHand);
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
        double phaseChance = this.plugin.getConfigManager().getPhaseChance();
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
            double multiplier = this.plugin.getConfigManager().getUndeadDamageMultiplier();
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
        String oraxenId = io.th0rgal.oraxen.api.OraxenItems.getIdByItem(mainHand);
        if (oraxenId == null || !oraxenId.contains("puff_gem")) {
            return;
        }
        if (!this.plugin.getEnergyManager().arePassivesActive(player)) {
            return;
        }

        // Launch target player up (velocity for ~35 blocks height)
        Vector launchVelocity = new Vector(0, 4.5, 0);
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
        if (!this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.PUFF) && !isHoldingPuffGem(player)) {
            return;
        }
        if (!this.plugin.getEnergyManager().arePassivesActive(player)) {
            return;
        }
        if (player.isOnGround()) {
            this.doubleJumpReady.put(player.getUniqueId(), true);
        }
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking()) {
            return;
        }
        if (!this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.PUFF) && !isHoldingPuffGem(player)) {
            return;
        }
        if (!this.plugin.getEnergyManager().arePassivesActive(player)) {
            return;
        }
        if (!player.isOnGround() && this.doubleJumpReady.getOrDefault(player.getUniqueId(), false).booleanValue()) {
            long now = System.currentTimeMillis();
            Long lastJump = this.lastJumpTime.get(player.getUniqueId());
            if (lastJump == null || now - lastJump > 500L) {
                Vector velocity = player.getVelocity();
                velocity.setY(0.8);
                player.setVelocity(velocity);
                this.doubleJumpReady.put(player.getUniqueId(), false);
                this.lastJumpTime.put(player.getUniqueId(), now);
                player.sendMessage("\u00a7b\u00a7oDouble jump!");
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!this.plugin.getGemManager().hasGemTypeInOffhand(player, GemType.FIRE)) {
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
            float saturationGain = (float)gain * (float)this.plugin.getConfigManager().getSaturationMultiplier();
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

        LivingEntity target = (LivingEntity) hitEntity;

        // Deal electric damage
        double shockDamage = this.plugin.getConfig().getDouble("abilities.damage.flux-shocking", 3.0);
        target.damage(shockDamage, player);

        // Spawn electric particles
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.5f);
    }

    @EventHandler
    public void onSculkShriekerActivate(BlockReceiveGameEvent event) {
        // Puff Gem - Sculk Shrieker immunity
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

        // Check if this is a sculk shrieker or sensor
        Material blockType = event.getBlock().getType();
        if (blockType == Material.SCULK_SHRIEKER || blockType == Material.SCULK_SENSOR || blockType == Material.CALIBRATED_SCULK_SENSOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        // Life Gem - Golden Apple Absorption Bonus
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item.getType() != Material.GOLDEN_APPLE && item.getType() != Material.ENCHANTED_GOLDEN_APPLE) {
            return;
        }

        if (!this.plugin.getGemManager().hasGemType(player, GemType.LIFE)) {
            return;
        }

        // Give Absorption 2 (4 hearts = amplifier 1, since amplifier 0 = 2 hearts)
        // Schedule for next tick to apply after the apple's default effects
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1, true, true));
            player.sendMessage("\u00a7a\u00a7oLife Gem enhanced your golden apple!");
        }, 1L);
    }
}

