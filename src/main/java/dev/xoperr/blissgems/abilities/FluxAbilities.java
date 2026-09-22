/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatMessageType
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.Color
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.Damageable
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.util.Vector
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.managers.FluxEnergyManager;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.ParticleUtils;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class FluxAbilities
implements GemAbilityHandler {
    private final BlissGems plugin;
    private static final Set<UUID> stunnedPlayers = new HashSet<UUID>();

    public FluxAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    public static boolean isPlayerStunned(UUID playerId) {
        return stunnedPlayers.contains(playerId);
    }

    public boolean isCharging(Player player) {
        return this.plugin.getFluxEnergyManager() != null && this.plugin.getFluxEnergyManager().isCharging(player);
    }

    public int getCharge(Player player) {
        return this.plugin.getFluxEnergyManager() != null ? (int)this.plugin.getFluxEnergyManager().getBeamCharge(player.getUniqueId()) : 0;
    }

    public void onRightClick(Player player, int tier) {
        if (tier == 2 && player.isSneaking()) {
            this.ground(player);
        } else {
            this.fluxBeam(player);
        }
    }

    @Override
    public void onPrimary(Player player, int tier) {
        this.fluxBeam(player);
    }

    @Override
    public void onSecondary(Player player, int tier) {
        this.ground(player);
    }

    @Override
    public void onTertiary(Player player, int tier) {
        this.flashbang(player);
    }

    @Override
    public void onQuaternary(Player player, int tier) {
        this.kineticBurst(player);
    }

    public void fluxBeam(Player player) {
        String abilityKey = "flux-beam";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        FluxEnergyManager manager = this.plugin.getFluxEnergyManager();
        if (manager == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        double charge = manager.getBeamCharge(uuid);
        boolean charging = manager.isCharging(player);
        if (charge >= 10.0) {
            this.fireChargedBeam(player);
            return;
        }
        if (charging) {
            player.sendMessage("\u00a7b\u26a1 \u00a7oCharging Flux Beam in progress... (" + String.format("%.1f%%", charge) + " / 10% min to fire)");
            return;
        }
        double watts = manager.getWatts(uuid);
        if (watts <= 0.0) {
            player.sendMessage("\u00a7c\u00a7lYour Flux Gem has 0 Watts! \u00a77Use \u00a7f/bliss charge \u00a77to open the charging station and insert conductive fuels.");
            return;
        }
        manager.setCharging(player, true);
        player.sendMessage("\u00a7b\u26a1 \u00a7oCharging Flux Beam from battery... Right-click again once charged (min 10%) to fire!");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.7f, 1.5f);
    }

    private void fireChargedBeam(Player player) {
        Player targetPlayer;
        Player tp;
        FluxEnergyManager manager = this.plugin.getFluxEnergyManager();
        if (manager == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        double charge = manager.getBeamCharge(uuid);
        if (charge < 10.0) {
            player.sendMessage("\u00a7c\u00a7oNot enough charge! (Minimum 10% needed to fire)");
            return;
        }
        LivingEntity target = this.getTargetEntity(player, 30);
        if (target == null) {
            player.sendMessage("\u00a7cNo target found!");
            return;
        }
        manager.setBeamCharge(uuid, 0.0);
        manager.setCharging(player, false);
        if (charge >= 100.0 && this.plugin.getAchievementManager() != null) {
            this.plugin.getAchievementManager().unlock(player, Achievement.OVERCHARGED);
        }
        if (charge >= 200.0) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.5f, 1.0f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.8f);
        }
        if (target instanceof Player) {
            Player targetPlayer2 = (Player)target;
            if (this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer2)) {
                int maxArmorDamage = this.plugin.getConfig().getInt("abilities.damage.flux-beam-max-armor-damage", 350);
                int armorRestore = (int)(charge / 100.0 * (double)maxArmorDamage);
                this.restoreArmorDurability(targetPlayer2, armorRestore);
                player.sendMessage("\u00a7a\u00a7lRestored " + targetPlayer2.getName() + "'s armor! (+" + armorRestore + " durability)");
                this.drawBeamParticles(player.getEyeLocation(), targetPlayer2.getEyeLocation().add(0.0, 1.0, 0.0));
                return;
            }
        }
        double baseDamage = this.plugin.getConfig().getDouble("abilities.damage.flux-beam-base", 12.5);
        double damageMultiplier = 1.0 + charge / 50.0;
        double finalDamage = baseDamage * damageMultiplier;
        int maxArmorDamage = this.plugin.getConfig().getInt("abilities.damage.flux-beam-max-armor-damage", 350);
        int armorDamage = (int)(charge / 100.0 * (double)maxArmorDamage);
        if (target instanceof Player && ((tp = (Player)target).getGameMode() == GameMode.CREATIVE || tp.getGameMode() == GameMode.SPECTATOR)) {
            player.sendMessage("\u00a7cCannot hit players in creative/spectator mode!");
            return;
        }
        double currentHealth = target.getHealth();
        boolean wouldDie = currentHealth <= finalDamage;
        boolean hasTotem = false;
        if (wouldDie && target instanceof Player) {
            targetPlayer = (Player)target;
            ItemStack mainHand = targetPlayer.getInventory().getItemInMainHand();
            ItemStack offHand = targetPlayer.getInventory().getItemInOffHand();
            if (mainHand.getType() == Material.TOTEM_OF_UNDYING) {
                hasTotem = true;
                targetPlayer.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            } else if (offHand.getType() == Material.TOTEM_OF_UNDYING) {
                hasTotem = true;
                targetPlayer.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            }
            if (hasTotem) {
                targetPlayer.setHealth(1.0);
                targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 800, 1));
                targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
                targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0));
                targetPlayer.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, targetPlayer.getLocation().add(0.0, 1.0, 0.0), 100, 0.5, 0.5, 0.5, 0.5);
                targetPlayer.playSound(targetPlayer.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                player.sendMessage("\u00a7e\u26a1 Totem of Undying activated!");
            }
        }
        if (!hasTotem) {
            double newHealth = Math.max(0.0, currentHealth - finalDamage);
            target.setHealth(newHealth);
        }
        if (target instanceof Player) {
            targetPlayer = (Player)target;
            this.damageArmorPieces(targetPlayer, armorDamage);
            targetPlayer.sendTitle("", "\u00a7c\u00a7l-" + String.format("%.1f", finalDamage) + " HP", 5, 15, 10);
            targetPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, (BaseComponent)new TextComponent("\u00a7c\u00a7l\u26a1 FLUX BEAM HIT! -" + String.format("%.1f", finalDamage) + " HP \u00a77(Armor: -" + armorDamage + ")"));
        }
        this.drawBeamParticles(player.getEyeLocation(), target.getEyeLocation().add(0.0, 1.0, 0.0));
        Location hitLoc = target.getLocation().add(0.0, 1.0, 0.0);
        Particle.DustOptions redDamage = new Particle.DustOptions(Color.fromRGB((int)255, (int)50, (int)50), 1.5f);
        Particle.DustOptions cyanHit = new Particle.DustOptions(ParticleUtils.FLUX_CYAN, 1.3f);
        target.getWorld().spawnParticle(Particle.DUST, hitLoc, 150, 1.2, 1.5, 1.2, 0.0, (Object)cyanHit, true);
        target.getWorld().spawnParticle(Particle.DUST, hitLoc, 100, 0.9, 1.2, 0.9, 0.0, (Object)redDamage, true);
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 200, 1.0, 1.3, 1.0, 0.15);
        target.getWorld().spawnParticle(Particle.CRIT, hitLoc, 80, 0.8, 1.0, 0.8, 0.3);
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, hitLoc, 100, 0.9, 1.2, 0.9, 0.2);
        target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, hitLoc, 60, 0.7, 1.0, 0.7, 0.08);
        target.getWorld().spawnParticle(Particle.EXPLOSION, hitLoc, 5, 0.5, 0.5, 0.5, 0.0);
        for (int i = 0; i < 32; ++i) {
            double angle = (double)i / 32.0 * 2.0 * Math.PI;
            double radius = 1.5;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(x, 0.5, z), 5, 0.1, 0.3, 0.1, 0.0, (Object)redDamage, true);
            target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(x, 0.5, z), 3, 0.1, 0.2, 0.1, 0.05);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.8f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, 1.5f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.8f);
        this.plugin.getAbilityManager().useAbility(player, "flux-beam");
        String targetName = target instanceof Player ? ((Player)target).getName() : target.getType().name();
        player.sendMessage("\u00a7b\u26a1\u00a7l FLUX BEAM HIT! \u00a7r\u00a7o" + targetName + " \u00a77(" + charge + "% charge)");
        player.sendMessage("\u00a7c  \u2764 Damage: " + String.format("%.1f", finalDamage) + " HP \u00a78| \u00a77Armor: -" + armorDamage);
    }

    private void drawBeamParticles(Location from, Location to) {
        double distance = from.distance(to);
        int points = (int)(distance * 8.0);
        Particle.DustOptions cyanCore = new Particle.DustOptions(ParticleUtils.FLUX_CYAN, 1.2f);
        Particle.DustOptions cyanOuter = new Particle.DustOptions(ParticleUtils.FLUX_CYAN, 0.8f);
        for (int i = 0; i <= points; ++i) {
            double ratio = (double)i / (double)points;
            double x = from.getX() + (to.getX() - from.getX()) * ratio;
            double y = from.getY() + (to.getY() - from.getY()) * ratio;
            double z = from.getZ() + (to.getZ() - from.getZ()) * ratio;
            Location point = new Location(from.getWorld(), x, y, z);
            from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, x, y, z, 8, 0.08, 0.08, 0.08, 0.03);
            from.getWorld().spawnParticle(Particle.DUST, x, y, z, 6, 0.1, 0.1, 0.1, 0.0, (Object)cyanCore, true);
            from.getWorld().spawnParticle(Particle.DUST, x, y, z, 4, 0.15, 0.15, 0.15, 0.0, (Object)cyanOuter, true);
            from.getWorld().spawnParticle(Particle.END_ROD, x, y, z, 3, 0.05, 0.05, 0.05, 0.01);
            from.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, x, y, z, 2, 0.08, 0.08, 0.08, 0.01);
            if (i % 3 != 0) continue;
            double angle = (double)i * 0.5;
            double arcRadius = 0.25;
            for (int arc = 0; arc < 3; ++arc) {
                double arcAngle = angle + (double)arc * Math.PI * 2.0 / 3.0;
                double offsetX = Math.cos(arcAngle) * arcRadius;
                double offsetZ = Math.sin(arcAngle) * arcRadius;
                from.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, x + offsetX, y, z + offsetZ, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    private void damageArmorPieces(Player player, int durabilityDamage) {
        if (durabilityDamage <= 0) {
            return;
        }
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            short maxDurability;
            ItemMeta meta;
            if (armor == null || armor.getType() == Material.AIR || !((meta = armor.getItemMeta()) instanceof Damageable)) continue;
            Damageable damageable = (Damageable)meta;
            int newDamage = damageable.getDamage() + durabilityDamage;
            if (newDamage >= (maxDurability = armor.getType().getMaxDurability())) {
                armor.setAmount(0);
                continue;
            }
            damageable.setDamage(newDamage);
            armor.setItemMeta(meta);
        }
    }

    private void restoreArmorDurability(Player player, int durabilityRestore) {
        if (durabilityRestore <= 0) {
            return;
        }
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            ItemMeta meta;
            if (armor == null || armor.getType() == Material.AIR || !((meta = armor.getItemMeta()) instanceof Damageable)) continue;
            Damageable damageable = (Damageable)meta;
            int newDamage = Math.max(0, damageable.getDamage() - durabilityRestore);
            damageable.setDamage(newDamage);
            armor.setItemMeta(meta);
        }
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0.0, 1.0, 0.0), 50, 0.5, 0.5, 0.5);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 2.0f);
    }

    public void cancelCharging(Player player) {
        if (this.plugin.getFluxEnergyManager() != null) {
            this.plugin.getFluxEnergyManager().setCharging(player, false);
        }
    }

    @Override
    public void cleanup(Player player) {
        this.cancelCharging(player);
    }

    public void ground(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "flux-ground";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        LivingEntity target = this.getTargetEntity(player, 20);
        if (target == null) {
            player.sendMessage("\u00a7cNo target found!");
            return;
        }
        if (target instanceof Player) {
            Player targetPlayer = (Player)target;
            if (this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) {
                player.sendMessage("\u00a7c\u00a7lYou cannot stun " + targetPlayer.getName() + " because they are trusted!");
                return;
            }
        }
        int stunDurationSeconds = this.plugin.getConfig().getInt("abilities.durations.flux-ground-freeze", 5);
        int stunDuration = stunDurationSeconds * 20;
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, stunDuration, 3, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, stunDuration, 1, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, stunDuration, 0, false, true));
        target.setNoDamageTicks(0);
        if (target instanceof Player) {
            Player targetPlayer = (Player)target;
            stunnedPlayers.add(targetPlayer.getUniqueId());
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> stunnedPlayers.remove(targetPlayer.getUniqueId()), (long)stunDuration);
            new org.bukkit.scheduler.BukkitRunnable() {
                int count = 0;
                @Override
                public void run() {
                    if (!targetPlayer.isOnline() || targetPlayer.isDead() || ++count > (stunDurationSeconds * 2)) {
                        cancel();
                        return;
                    }
                    int slot1 = (int)(Math.random() * 9);
                    int slot2 = (int)(Math.random() * 9);
                    if (slot1 != slot2) {
                        ItemStack i1 = targetPlayer.getInventory().getItem(slot1);
                        ItemStack i2 = targetPlayer.getInventory().getItem(slot2);
                        targetPlayer.getInventory().setItem(slot1, i2);
                        targetPlayer.getInventory().setItem(slot2, i1);
                    }
                }
            }.runTaskTimer((Plugin)this.plugin, 10L, 10L);
        }
        Particle.DustOptions darkCyanDust = new Particle.DustOptions(ParticleUtils.FLUX_DARK_CYAN, 1.5f);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 200, 1.0, 1.8, 1.0, 0.0, (Object)darkCyanDust, true);
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0.0, 1.0, 0.0), 100, 0.7, 1.2, 0.7);
        target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, target.getLocation(), 60, 0.7, 0.2, 0.7);
        target.getWorld().spawnParticle(Particle.SMOKE, target.getLocation().add(0.0, 0.5, 0.0), 40, 0.5, 0.5, 0.5);
        for (int i = 0; i < 24; ++i) {
            double angle = (double)i / 24.0 * 2.0 * Math.PI;
            double x = Math.cos(angle) * 1.8;
            double z = Math.sin(angle) * 1.8;
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(x, 0.1, z), 8, 0.15, 0.15, 0.15, 0.0, (Object)darkCyanDust, true);
            target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(x, 0.5, z), 3, 0.1, 0.1, 0.1, 0.0);
        }
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.2f);
        player.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 1.0, 0.0), 50, 1.2, 1.2, 1.2);
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, stunDurationSeconds);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Ground");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        if (target instanceof Player) {
            Player targetPlayer = (Player)target;
            targetPlayer.sendMessage("\u00a7c\u00a7lYou have been grounded for " + stunDurationSeconds + " seconds! You cannot move or jump!");
        }
        String targetName = target instanceof Player ? ((Player)target).getName() : target.getType().name();
        player.sendMessage("\u00a7b\u26a1 \u00a7oGrounded " + targetName + " for " + stunDurationSeconds + " seconds!");
    }

    public void flashbang(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "flux-flashbang";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        double radius = this.plugin.getConfig().getDouble("abilities.flux-flashbang.radius", 8.0);
        int duration = this.plugin.getConfigManager().getAbilityDuration("flux-flashbang") * 20;
        Location center = player.getLocation();
        int affectedCount = 0;
        for (Entity entity : player.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity) || entity.equals((Object)player)) continue;
            LivingEntity target = (LivingEntity)entity;
            if (target instanceof Player) {
                Player targetPlayer = (Player)target;
                if (this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) continue;
            }
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 0, false, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, duration, 1, false, true));
            Particle.DustOptions white = new Particle.DustOptions(Color.WHITE, 1.5f);
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 80, 0.5, 0.5, 0.5, 0.0, (Object)white, true);
            ++affectedCount;
            if (!(target instanceof Player)) continue;
            ((Player)target).sendMessage("\u00a7f\u00a7lFLASHBANGED! \u00a77You are blinded!");
        }
        Particle.DustOptions brightWhite = new Particle.DustOptions(Color.WHITE, 2.0f);
        center.getWorld().spawnParticle(Particle.DUST, center.clone().add(0.0, 1.0, 0.0), 200, radius / 2.0, radius / 2.0, radius / 2.0, 0.0, (Object)brightWhite, true);
        center.getWorld().spawnParticle(Particle.FIREWORK, center.clone().add(0.0, 1.0, 0.0), 50, radius / 3.0, radius / 3.0, radius / 3.0, 0.2);
        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0.0, 1.0, 0.0), 80, radius / 2.0, radius / 2.0, radius / 2.0, 0.1);
        player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.8f);
        player.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 2.0f);
        int flashDurationSeconds = this.plugin.getConfigManager().getAbilityDuration("flux-flashbang");
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, flashDurationSeconds);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Flashbang");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        player.sendMessage("\u00a7b\u26a1 \u00a7oFlashbang! Blinded " + affectedCount + " enemies!");
    }

    public void kineticBurst(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "flux-kinetic-burst";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        double radius = this.plugin.getConfig().getDouble("abilities.flux-kinetic-burst.radius", 6.0);
        double knockbackPower = this.plugin.getConfig().getDouble("abilities.flux-kinetic-burst.knockback", 2.5);
        Location center = player.getLocation();
        int affectedCount = 0;
        for (Entity entity : player.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            Vector direction;
            if (entity.equals((Object)player)) continue;
            if (entity instanceof Player) {
                Player targetPlayer = (Player)entity;
                if (this.plugin.getTrustedPlayersManager().isTrusted(player, targetPlayer)) continue;
            }
            if ((direction = entity.getLocation().toVector().subtract(center.toVector())).lengthSquared() > 0.0) {
                direction.normalize();
            }
            direction.setY(0.5);
            direction.multiply(knockbackPower);
            entity.setVelocity(direction);
            ++affectedCount;
        }
        player.getWorld().spawnParticle(Particle.SONIC_BOOM, center.clone().add(0.0, 1.0, 0.0), 10, 0.5, 0.5, 0.5, 0.0);
        Particle.DustOptions cyan = new Particle.DustOptions(ParticleUtils.FLUX_CYAN, 1.5f);
        player.getWorld().spawnParticle(Particle.DUST, center.clone().add(0.0, 1.0, 0.0), 150, radius / 2.0, radius / 2.0, radius / 2.0, 0.0, (Object)cyan, true);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0.0, 1.0, 0.0), 100, radius / 2.0, radius / 2.0, radius / 2.0, 0.2);
        ParticleUtils.drawRing(center, ParticleUtils.FLUX_CYAN, 1.2f, radius * 0.33, 0.1, 32);
        ParticleUtils.drawRing(center, ParticleUtils.FLUX_CYAN, 1.2f, radius * 0.66, 0.1, 32);
        ParticleUtils.drawRing(center, ParticleUtils.FLUX_CYAN, 1.2f, radius, 0.1, 32);
        player.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.8f);
        player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);
        int kineticDurationSeconds = this.plugin.getConfigManager().getAbilityDuration("flux-kinetic-burst");
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, kineticDurationSeconds);
        String msg = this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Kinetic Burst");
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
        player.sendMessage("\u00a7b\u26a1 \u00a7oKinetic Burst! Knocked back " + affectedCount + " entities!");
    }

    public void conduction(Player player) {
        String abilityKey = "flux-conduction";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        int range = this.plugin.getConfig().getInt("abilities.flux-conduction.range", 10);
        Location playerLoc = player.getLocation();
        Block closest = null;
        double closestDist = Double.MAX_VALUE;
        for (int x = -range; x <= range; ++x) {
            for (int y = -range; y <= range; ++y) {
                for (int z = -range; z <= range; ++z) {
                    double dist;
                    Block block = playerLoc.getBlock().getRelative(x, y, z);
                    if (!FluxAbilities.isCopperBlock(block.getType()) || !((dist = block.getLocation().distanceSquared(playerLoc)) < closestDist)) continue;
                    closestDist = dist;
                    closest = block;
                }
            }
        }
        if (closest == null) {
            player.sendMessage("\u00a7c\u26a1 \u00a7oNo copper blocks within " + range + " blocks!");
            return;
        }
        Block feet = closest.getRelative(0, 1, 0);
        Block head = closest.getRelative(0, 2, 0);
        if (!feet.isPassable() || !head.isPassable()) {
            player.sendMessage("\u00a7c\u26a1 \u00a7oThere is no safe space above that copper block!");
            return;
        }
        Location departure = player.getLocation().clone();
        Location tpLoc = closest.getLocation().add(0.5, 1.0, 0.5);
        tpLoc.setYaw(departure.getYaw());
        tpLoc.setPitch(departure.getPitch());
        if (this.plugin.getAchievementManager() != null) {
            int distance = (int)departure.distance(tpLoc);
            this.plugin.getAchievementManager().addProgress(player, Achievement.ZIP_AWAY, distance);
        }
        player.teleport(tpLoc);
        Particle.DustOptions cyan = new Particle.DustOptions(ParticleUtils.FLUX_CYAN, 1.2f);
        departure.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, departure.clone().add(0.0, 1.0, 0.0), 50, 0.5, 0.5, 0.5, 0.1);
        departure.getWorld().spawnParticle(Particle.DUST, departure.clone().add(0.0, 1.0, 0.0), 40, 0.5, 0.5, 0.5, 0.0, (Object)cyan, true);
        tpLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, tpLoc.clone().add(0.0, 0.5, 0.0), 50, 0.5, 0.5, 0.5, 0.1);
        tpLoc.getWorld().spawnParticle(Particle.DUST, tpLoc.clone().add(0.0, 0.5, 0.0), 40, 0.5, 0.5, 0.5, 0.0, (Object)cyan, true);
        player.playSound(departure, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
        player.playSound(tpLoc, Sound.BLOCK_COPPER_BULB_TURN_ON, 1.0f, 1.2f);
        player.sendMessage("\u00a7b\u26a1 \u00a7oConducted to copper!");
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
    }

    private static boolean isCopperBlock(Material material) {
        String name = material.name();
        return name.contains("COPPER") && !name.endsWith("_ORE") && !name.startsWith("RAW_");
    }

    private LivingEntity getTargetEntity(Player player, int range) {
        return player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), (double)range, entity -> entity instanceof LivingEntity && entity != player) != null ? (LivingEntity)player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), (double)range, entity -> entity instanceof LivingEntity && entity != player).getHitEntity() : null;
    }
}

