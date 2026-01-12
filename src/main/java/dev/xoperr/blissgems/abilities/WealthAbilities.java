/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.util.RayTraceResult
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;

public class WealthAbilities {
    private final BlissGems plugin;
    private final Map<UUID, Inventory> pocketsInventories;
    private final Map<UUID, Boolean> autoSmeltEnabled;

    public WealthAbilities(BlissGems plugin) {
        this.plugin = plugin;
        this.pocketsInventories = new HashMap<UUID, Inventory>();
        this.autoSmeltEnabled = new HashMap<UUID, Boolean>();
    }

    public void onRightClick(Player player, int tier) {
        if (tier == 2 && player.isSneaking()) {
            this.richRush(player);
        } else {
            // Try to target an entity for Unfortunate, otherwise no action
            RayTraceResult target = player.getWorld().rayTraceEntities(player.getEyeLocation(),
                player.getEyeLocation().getDirection(), 15.0,
                entity -> entity instanceof Player && entity != player);

            if (target != null && target.getHitEntity() instanceof Player) {
                this.unfortunate(player);
            } else {
                player.sendMessage("§e§oNo target found! §7(Aim at a player to use Unfortunate)");
            }
        }
    }

    public void durabilityChip(Player player) {
        String abilityKey = "wealth-durability-chip";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        player.sendMessage("\u00a76\u00a7lDurability Chip \u00a77(Toggles 2x durability damage on tools)");
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
    }

    public void pockets(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        Inventory pockets = this.pocketsInventories.computeIfAbsent(player.getUniqueId(), uuid -> Bukkit.createInventory(null, (int)9, (String)"\u00a76\u00a7lPockets"));
        player.openInventory(pockets);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    public void unfortunate(Player player) {
        Entity entity2;
        String abilityKey = "wealth-unfortunate";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        RayTraceResult target = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 15.0, entity -> entity instanceof Player && entity != player);
        if (target == null || !((entity2 = target.getHitEntity()) instanceof Player)) {
            player.sendMessage("\u00a7cNo player target found!");
            return;
        }
        Player targetPlayer = (Player)entity2;
        int duration = this.plugin.getConfigManager().getAbilityDuration("wealth-unfortunate");
        targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, duration * 20, 2, false, true));
        targetPlayer.getWorld().spawnParticle(Particle.SMOKE, targetPlayer.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5);
        player.playSound(player.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 1.0f, 0.8f);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Unfortunate"));
    }

    public void richRush(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "wealth-rich-rush";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        int duration = this.plugin.getConfigManager().getAbilityDuration("wealth-rich-rush");
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration * 20, 2, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, duration * 20, 3, false, true));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0.0, 1.0, 0.0), 50, 0.5, 0.5, 0.5);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Rich Rush"));
    }

    public void amplification(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "wealth-amplification";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        int duration = this.plugin.getConfigManager().getAbilityDuration("wealth-amplification");
        for (PotionEffect effect : player.getActivePotionEffects()) {
            PotionEffect amplified = new PotionEffect(effect.getType(), effect.getDuration(), Math.min(effect.getAmplifier() + 1, 5), effect.isAmbient(), effect.hasParticles());
            player.addPotionEffect(amplified);
        }
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);
        player.spawnParticle(Particle.ENCHANT, player.getLocation().add(0.0, 1.0, 0.0), 100, 0.5, 1.0, 0.5);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Amplification"));
    }

    public Inventory getPocketsInventory(UUID uuid) {
        return this.pocketsInventories.get(uuid);
    }

    public boolean isAutoSmeltEnabled(Player player) {
        return this.autoSmeltEnabled.getOrDefault(player.getUniqueId(), false);
    }

    public void setAutoSmelt(Player player, boolean enabled) {
        this.autoSmeltEnabled.put(player.getUniqueId(), enabled);
    }
}

