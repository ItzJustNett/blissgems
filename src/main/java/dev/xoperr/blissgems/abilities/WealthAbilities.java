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
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
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

    // Amplification: tracks which players are currently amplified (for preventing stacking)
    private final Set<UUID> amplifiedPlayers = new HashSet<>();

    // PDC prefix for amplify original enchant levels
    private static final String AMP_PDC_PREFIX = "amp_orig_";
    // PDC prefix used by auto-enchant system (to skip those enchants)
    private static final String AE_PDC_PREFIX = "ae_orig_";

    // Rich Rush: tracks players with active Rich Rush (increased drops)
    private static final Set<UUID> richRushPlayers = new HashSet<>();

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
        UUID uuid = player.getUniqueId();

        // Apply Haste (faster mining) and Luck
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration * 20, 2, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, duration * 20, 3, false, true));

        // Track active Rich Rush for drop multiplication in listeners
        richRushPlayers.add(uuid);
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            richRushPlayers.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage("\u00a7e\u00a7oRich Rush has worn off.");
            }
        }, duration * 20L);

        // Rich Rush with bright green dust (RGB 0, 166, 44)
        Particle.DustOptions greenDust = new Particle.DustOptions(ParticleUtils.WEALTH_GREEN, 1.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 50, 0.5, 0.5, 0.5, 0.0, greenDust, true);
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0.0, 1.0, 0.0), 40, 0.5, 0.5, 0.5);
        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Rich Rush"));
        player.sendMessage("\u00a76\u00a7lRich Rush! \u00a7eMob and ore drops doubled for " + duration + "s!");
    }

    /**
     * Check if a player has Rich Rush active (for drop multiplication in listeners)
     */
    public static boolean hasRichRush(UUID uuid) {
        return richRushPlayers.contains(uuid);
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

        UUID uuid = player.getUniqueId();

        // Prevent stacking amplify
        if (amplifiedPlayers.contains(uuid)) {
            player.sendMessage("\u00a7c\u00a7oAmplification is already active!");
            return;
        }

        int duration = this.plugin.getConfigManager().getAbilityDuration("wealth-amplification");

        // Collect all equipment items to amplify
        boolean anyAmplified = false;
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (amplifyItem(armor[i])) anyAmplified = true;
        }
        player.getInventory().setArmorContents(armor);

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (amplifyItem(mainHand)) anyAmplified = true;

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (amplifyItem(offHand)) anyAmplified = true;

        amplifiedPlayers.add(uuid);

        // Schedule revert after duration
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            amplifiedPlayers.remove(uuid);

            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) return;

            // Scan ALL items in inventory for amplify PDC markers and revert
            revertAllAmplifiedItems(p);

            p.sendMessage("\u00a7e\u00a7oAmplification has worn off. Enchantments restored.");
        }, duration * 20L);

        // Particles + sound
        Particle.DustOptions greenDust = new Particle.DustOptions(ParticleUtils.WEALTH_GREEN, 1.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);
        player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 100, 0.5, 1.0, 0.5, 0.0, greenDust, true);
        player.spawnParticle(Particle.ENCHANT, player.getLocation().add(0.0, 1.0, 0.0), 80, 0.5, 1.0, 0.5);
        // Achievement: Boundary Break
        if (this.plugin.getAchievementManager() != null && anyAmplified) {
            this.plugin.getAchievementManager().unlock(player, Achievement.BOUNDARY_BREAK);
        }

        this.plugin.getAbilityManager().useAbility(player, abilityKey);
        player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("ability-activated", "ability", "Amplification"));
    }

    /**
     * Amplify a single item: boost all non-auto-enchanted enchants by +1,
     * storing originals in PDC so they can be reverted regardless of slot.
     * Returns true if any enchants were boosted.
     */
    private boolean amplifyItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;

        Map<Enchantment, Integer> enchants = item.getEnchantments();
        if (enchants.isEmpty()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean modified = false;

        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            Enchantment enchant = entry.getKey();
            int currentLevel = entry.getValue();

            // Skip auto-enchanted enchants (they're temporary and managed by auto-enchant system)
            NamespacedKey aeKey = new NamespacedKey(plugin, AE_PDC_PREFIX + enchant.getKey().getKey());
            if (pdc.has(aeKey, PersistentDataType.INTEGER)) {
                continue;
            }

            // Skip if already amplified
            NamespacedKey ampKey = new NamespacedKey(plugin, AMP_PDC_PREFIX + enchant.getKey().getKey());
            if (pdc.has(ampKey, PersistentDataType.INTEGER)) {
                continue;
            }

            // Store original level and boost by +1
            pdc.set(ampKey, PersistentDataType.INTEGER, currentLevel);
            meta.addEnchant(enchant, currentLevel + 1, true);
            modified = true;
        }

        if (modified) {
            item.setItemMeta(meta);
        }
        return modified;
    }

    /**
     * Revert all amplified enchants on all items in a player's inventory.
     * Scans by PDC markers so it works regardless of what slot items ended up in.
     */
    private void revertAllAmplifiedItems(Player p) {
        // Scan all inventory slots
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (item != null && !item.getType().isAir()) {
                stripAmplifyEnchants(item);
            }
        }
        // Armor contents (accessed separately to ensure setArmorContents is called)
        ItemStack[] armor = p.getInventory().getArmorContents();
        boolean armorModified = false;
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && !armor[i].getType().isAir()) {
                if (stripAmplifyEnchants(armor[i])) armorModified = true;
            }
        }
        if (armorModified) {
            p.getInventory().setArmorContents(armor);
        }
        // Off hand
        ItemStack offHand = p.getInventory().getItemInOffHand();
        if (!offHand.getType().isAir()) {
            stripAmplifyEnchants(offHand);
        }
    }

    /**
     * Strip amplify PDC markers from an item, restoring original enchant levels.
     * Can be called on any item (e.g., dropped items).
     * Returns true if the item was modified.
     */
    public static boolean stripAmplifyEnchants(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean modified = false;

        for (Enchantment enchant : Enchantment.values()) {
            NamespacedKey ampKey = getAmpKey(item, enchant);
            if (ampKey != null && pdc.has(ampKey, PersistentDataType.INTEGER)) {
                int originalLevel = pdc.get(ampKey, PersistentDataType.INTEGER);
                if (originalLevel == 0) {
                    meta.removeEnchant(enchant);
                } else {
                    meta.addEnchant(enchant, originalLevel, true);
                }
                pdc.remove(ampKey);
                modified = true;
            }
        }

        if (modified) {
            item.setItemMeta(meta);
        }
        return modified;
    }

    /**
     * Check if an item has any amplify PDC markers.
     */
    public static boolean hasAmplifyEnchants(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        for (Enchantment enchant : Enchantment.values()) {
            NamespacedKey ampKey = getAmpKey(item, enchant);
            if (ampKey != null && pdc.has(ampKey, PersistentDataType.INTEGER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper: get the amplify PDC NamespacedKey for an enchantment.
     * Returns null if the item has no meta (shouldn't happen if checked beforehand).
     */
    private static NamespacedKey getAmpKey(ItemStack item, Enchantment enchant) {
        if (!item.hasItemMeta()) return null;
        // Use the namespace from the item's existing PDC keys plugin context
        // Since this is static, we construct the key manually with "blissgems" namespace
        return NamespacedKey.fromString("blissgems:" + AMP_PDC_PREFIX + enchant.getKey().getKey());
    }

    // Static accessors for PassiveListener
    public static boolean isUnfortunate(UUID uuid) {
        return unfortunatePlayers.contains(uuid) && Math.random() < 0.5;
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
