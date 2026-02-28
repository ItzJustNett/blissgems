package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.ParticleUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;

public class WealthAbilities {
    private final BlissGems plugin;
    private final Map<UUID, Inventory> pocketsInventories;
    private final Map<UUID, Boolean> autoSmeltEnabled;

    // Unfortunate: tracks players whose actions are disabled
    private static final Set<UUID> unfortunatePlayers = new HashSet<>();

    // Item Lock: tracks players with a locked item
    private static final Map<UUID, ItemStack> itemLockedPlayers = new HashMap<>();

    // Amplification: stores original enchantment levels for reverting
    private final Map<UUID, Map<Integer, Map<Enchantment, Integer>>> amplifiedPlayers = new HashMap<>();

    public WealthAbilities(BlissGems plugin) {
        this.plugin = plugin;
        this.pocketsInventories = new HashMap<UUID, Inventory>();
        this.autoSmeltEnabled = new HashMap<UUID, Boolean>();
    }

    public void onRightClick(Player player, int tier) {
        if (tier < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        if (player.isSneaking()) {
            this.itemLock(player);
        } else {
            this.unfortunate(player);
        }
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
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
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

        // Add target to unfortunate set (disables actions)
        UUID targetUUID = targetPlayer.getUniqueId();
        unfortunatePlayers.add(targetUUID);

        // Schedule removal after duration
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            unfortunatePlayers.remove(targetUUID);
            if (targetPlayer.isOnline()) {
                targetPlayer.sendMessage("\u00a7a\u00a7oUnfortunate has worn off.");
            }
        }, duration * 20L);

        // Particles + sound
        Particle.DustOptions greenDust = new Particle.DustOptions(ParticleUtils.WEALTH_GREEN, 1.5f);
        targetPlayer.getWorld().spawnParticle(Particle.DUST, targetPlayer.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.0, greenDust, true);
        targetPlayer.getWorld().spawnParticle(Particle.SMOKE, targetPlayer.getLocation().add(0.0, 1.0, 0.0), 20, 0.5, 0.5, 0.5);
        player.playSound(player.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 1.0f, 0.8f);

        // Notify target
        targetPlayer.sendMessage("\u00a7c\u00a7oYou've been afflicted with Unfortunate! Actions disabled for " + duration + "s!");

        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Unfortunate"));
    }

    public void itemLock(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "wealth-item-lock";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        RayTraceResult target = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 15.0, entity -> entity instanceof Player && entity != player);
        if (target == null || !(target.getHitEntity() instanceof Player)) {
            player.sendMessage("\u00a7cNo player target found!");
            return;
        }
        Player targetPlayer = (Player)target.getHitEntity();
        ItemStack targetItem = targetPlayer.getInventory().getItemInMainHand();

        if (targetItem == null || targetItem.getType().isAir()) {
            player.sendMessage("\u00a7cTarget isn't holding an item!");
            return;
        }

        // Lock the target's held item
        UUID targetUUID = targetPlayer.getUniqueId();
        itemLockedPlayers.put(targetUUID, targetItem.clone());

        int duration = this.plugin.getConfig().getInt("abilities.durations.wealth-item-lock", 10);

        // Schedule removal after duration
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            itemLockedPlayers.remove(targetUUID);
            if (targetPlayer.isOnline()) {
                targetPlayer.sendMessage("\u00a7a\u00a7oItem Lock has worn off.");
            }
        }, duration * 20L);

        // Particles + sound
        Particle.DustOptions greenDust = new Particle.DustOptions(ParticleUtils.WEALTH_GREEN, 1.5f);
        targetPlayer.getWorld().spawnParticle(Particle.DUST, targetPlayer.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.0, greenDust, true);
        targetPlayer.playSound(targetPlayer.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);

        // Get item name for message
        String itemName = targetItem.getType().name().toLowerCase().replace('_', ' ');
        if (targetItem.hasItemMeta() && targetItem.getItemMeta().hasDisplayName()) {
            itemName = targetItem.getItemMeta().getDisplayName();
        }

        targetPlayer.sendMessage("\u00a7c\u00a7oYour " + itemName + " has been locked for " + duration + "s!");

        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Item Lock"));
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

        // Rich Rush with bright green dust (RGB 0, 166, 44)
        Particle.DustOptions greenDust = new Particle.DustOptions(ParticleUtils.WEALTH_GREEN, 1.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 50, 0.5, 0.5, 0.5, 0.0, greenDust, true);
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0.0, 1.0, 0.0), 40, 0.5, 0.5, 0.5);
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

        UUID uuid = player.getUniqueId();

        // Store original enchantment levels and boost all enchantments by +1
        Map<Integer, Map<Enchantment, Integer>> originalEnchants = new HashMap<>();

        // Process all 6 equipment slots: 4 armor + main hand + off hand
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        // Armor slots (indices 0-3 for boots, leggings, chestplate, helmet)
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && !armor[i].getType().isAir() && !armor[i].getEnchantments().isEmpty()) {
                Map<Enchantment, Integer> origLevels = new HashMap<>(armor[i].getEnchantments());
                originalEnchants.put(i, origLevels);
                for (Map.Entry<Enchantment, Integer> entry : origLevels.entrySet()) {
                    armor[i].addUnsafeEnchantment(entry.getKey(), entry.getValue() + 1);
                }
            }
        }
        player.getInventory().setArmorContents(armor);

        // Main hand (slot index 100)
        if (mainHand != null && !mainHand.getType().isAir() && !mainHand.getEnchantments().isEmpty()) {
            Map<Enchantment, Integer> origLevels = new HashMap<>(mainHand.getEnchantments());
            originalEnchants.put(100, origLevels);
            for (Map.Entry<Enchantment, Integer> entry : origLevels.entrySet()) {
                mainHand.addUnsafeEnchantment(entry.getKey(), entry.getValue() + 1);
            }
        }

        // Off hand (slot index 101)
        if (offHand != null && !offHand.getType().isAir() && !offHand.getEnchantments().isEmpty()) {
            Map<Enchantment, Integer> origLevels = new HashMap<>(offHand.getEnchantments());
            originalEnchants.put(101, origLevels);
            for (Map.Entry<Enchantment, Integer> entry : origLevels.entrySet()) {
                offHand.addUnsafeEnchantment(entry.getKey(), entry.getValue() + 1);
            }
        }

        amplifiedPlayers.put(uuid, originalEnchants);

        // Schedule revert after duration
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            Map<Integer, Map<Enchantment, Integer>> stored = amplifiedPlayers.remove(uuid);
            if (stored == null) return;

            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) return;

            // Restore armor enchantments
            ItemStack[] currentArmor = p.getInventory().getArmorContents();
            for (int i = 0; i < currentArmor.length; i++) {
                Map<Enchantment, Integer> origLevels = stored.get(i);
                if (origLevels != null && currentArmor[i] != null && !currentArmor[i].getType().isAir()) {
                    // Remove all enchantments first, then re-add originals
                    for (Enchantment ench : new java.util.ArrayList<>(currentArmor[i].getEnchantments().keySet())) {
                        currentArmor[i].removeEnchantment(ench);
                    }
                    for (Map.Entry<Enchantment, Integer> entry : origLevels.entrySet()) {
                        currentArmor[i].addUnsafeEnchantment(entry.getKey(), entry.getValue());
                    }
                }
            }
            p.getInventory().setArmorContents(currentArmor);

            // Restore main hand
            Map<Enchantment, Integer> mainOrigLevels = stored.get(100);
            if (mainOrigLevels != null) {
                ItemStack mh = p.getInventory().getItemInMainHand();
                if (mh != null && !mh.getType().isAir()) {
                    for (Enchantment ench : new java.util.ArrayList<>(mh.getEnchantments().keySet())) {
                        mh.removeEnchantment(ench);
                    }
                    for (Map.Entry<Enchantment, Integer> entry : mainOrigLevels.entrySet()) {
                        mh.addUnsafeEnchantment(entry.getKey(), entry.getValue());
                    }
                }
            }

            // Restore off hand
            Map<Enchantment, Integer> offOrigLevels = stored.get(101);
            if (offOrigLevels != null) {
                ItemStack oh = p.getInventory().getItemInOffHand();
                if (oh != null && !oh.getType().isAir()) {
                    for (Enchantment ench : new java.util.ArrayList<>(oh.getEnchantments().keySet())) {
                        oh.removeEnchantment(ench);
                    }
                    for (Map.Entry<Enchantment, Integer> entry : offOrigLevels.entrySet()) {
                        oh.addUnsafeEnchantment(entry.getKey(), entry.getValue());
                    }
                }
            }

            p.sendMessage("\u00a7e\u00a7oAmplification has worn off. Enchantments restored.");
        }, duration * 20L);

        // Particles + sound
        Particle.DustOptions greenDust = new Particle.DustOptions(ParticleUtils.WEALTH_GREEN, 1.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);
        player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 100, 0.5, 1.0, 0.5, 0.0, greenDust, true);
        player.spawnParticle(Particle.ENCHANT, player.getLocation().add(0.0, 1.0, 0.0), 80, 0.5, 1.0, 0.5);
        // Achievement: Boundary Break
        if (this.plugin.getAchievementManager() != null && !originalEnchants.isEmpty()) {
            this.plugin.getAchievementManager().unlock(player, Achievement.BOUNDARY_BREAK);
        }

        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Amplification"));
    }

    // Static accessors for PassiveListener
    public static boolean isUnfortunate(UUID uuid) {
        return unfortunatePlayers.contains(uuid);
    }

    public static boolean isItemLocked(UUID uuid) {
        return itemLockedPlayers.containsKey(uuid);
    }

    public static ItemStack getLockedItem(UUID uuid) {
        return itemLockedPlayers.get(uuid);
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
