/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.util.RayTraceResult
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.ParticleUtils;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
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

public class WealthAbilities
implements GemAbilityHandler {
    private final BlissGems plugin;
    private final Map<UUID, Inventory> pocketsInventories;
    private final Map<UUID, Boolean> autoSmeltEnabled;
    private final File pocketsDataFolder;
    private static final Set<UUID> unfortunatePlayers = new HashSet<UUID>();
    private static final Map<UUID, ItemStack> itemLockedPlayers = new HashMap<UUID, ItemStack>();
    private final Set<UUID> amplifiedPlayers = new HashSet<UUID>();
    private static final String AMP_PDC_PREFIX = "amp_orig_";
    private static final String AE_PDC_PREFIX = "ae_orig_";
    private static final Set<UUID> richRushPlayers = new HashSet<UUID>();

    public WealthAbilities(BlissGems plugin) {
        this.plugin = plugin;
        this.pocketsInventories = new HashMap<UUID, Inventory>();
        this.autoSmeltEnabled = new HashMap<UUID, Boolean>();
        this.pocketsDataFolder = new File(plugin.getDataFolder(), "pockets");
        if (!this.pocketsDataFolder.exists()) {
            this.pocketsDataFolder.mkdirs();
        }
    }

    public void onRightClick(Player player, int tier) {
        if (tier == 2 && player.isSneaking()) {
            this.richRush(player);
        } else {
            this.unfortunate(player);
        }
    }

    @Override
    public void onPrimary(Player player, int tier) {
        this.unfortunate(player);
    }

    @Override
    public void onSecondary(Player player, int tier) {
        this.richRush(player);
    }

    @Override
    public void onTertiary(Player player, int tier) {
        this.itemLock(player);
    }

    @Override
    public void onQuaternary(Player player, int tier) {
        this.amplification(player);
    }

    private boolean requireTier2(Player player) {
        if (this.plugin.getGemManager().getGemTier(player) >= 2) {
            return true;
        }
        player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
        return false;
    }

    public void pockets(Player player) {
        if (!this.requireTier2(player)) {
            return;
        }
        Inventory pockets = this.pocketsInventories.computeIfAbsent(player.getUniqueId(), uuid -> {
            Inventory inv = Bukkit.createInventory(null, (int)9, (String)"\u00a76\u00a7lPockets");
            this.loadPocketsInventory(player.getUniqueId(), inv);
            return inv;
        });
        player.openInventory(pockets);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    public void unfortunate(Player player) {
        Entity entity2;
        if (!this.requireTier2(player)) {
            return;
        }
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
        UUID targetUUID = targetPlayer.getUniqueId();
        unfortunatePlayers.add(targetUUID);
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            unfortunatePlayers.remove(targetUUID);
            if (targetPlayer.isOnline()) {
                targetPlayer.sendMessage("\u00a7a\u00a7oUnfortunate has worn off.");
            }
            this.plugin.getAbilityManager().endAbilityDuration(player, abilityKey);
        }, (long)duration * 20L);
        Particle.DustOptions greenDust = new Particle.DustOptions(ParticleUtils.WEALTH_GREEN, 1.5f);
        targetPlayer.getWorld().spawnParticle(Particle.DUST, targetPlayer.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.0, (Object)greenDust, true);
        targetPlayer.getWorld().spawnParticle(Particle.SMOKE, targetPlayer.getLocation().add(0.0, 1.0, 0.0), 20, 0.5, 0.5, 0.5);
        player.playSound(player.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 1.0f, 0.8f);
        targetPlayer.sendMessage("\u00a7c\u00a7oYou've been afflicted with Unfortunate! Actions disabled for " + duration + "s!");
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, duration);
        this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "ability-activated", "ability", "Unfortunate");
    }

    public void itemLock(Player player) {
        Entity entity2;
        if (!this.requireTier2(player)) {
            return;
        }
        String abilityKey = "wealth-item-lock";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        RayTraceResult target = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 15.0, entity -> entity instanceof Player && entity != player);
        if (target == null || !((entity2 = target.getHitEntity()) instanceof Player)) {
            player.sendMessage("\u00a7cNo player target found!");
            return;
        }
        Player targetPlayer = (Player)entity2;
        ItemStack targetItem = targetPlayer.getInventory().getItemInMainHand();
        if (targetItem == null || targetItem.getType().isAir()) {
            player.sendMessage("\u00a7cTarget isn't holding an item!");
            return;
        }
        UUID targetUUID = targetPlayer.getUniqueId();
        itemLockedPlayers.put(targetUUID, targetItem.clone());
        int duration = this.plugin.getConfig().getInt("abilities.durations.wealth-item-lock", 10);
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            itemLockedPlayers.remove(targetUUID);
            if (targetPlayer.isOnline()) {
                targetPlayer.sendMessage("\u00a7a\u00a7oItem Lock has worn off.");
            }
            this.plugin.getAbilityManager().endAbilityDuration(player, abilityKey);
        }, (long)duration * 20L);
        Particle.DustOptions greenDust = new Particle.DustOptions(ParticleUtils.WEALTH_GREEN, 1.5f);
        targetPlayer.getWorld().spawnParticle(Particle.DUST, targetPlayer.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.0, (Object)greenDust, true);
        targetPlayer.playSound(targetPlayer.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);
        String itemName = targetItem.getType().name().toLowerCase().replace('_', ' ');
        if (targetItem.hasItemMeta() && targetItem.getItemMeta().hasDisplayName()) {
            itemName = targetItem.getItemMeta().getDisplayName();
        }
        targetPlayer.sendMessage("\u00a7c\u00a7oYour " + itemName + " has been locked for " + duration + "s!");
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, duration);
        this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "ability-activated", "ability", "Item Lock");
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
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, duration * 20, 2, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, duration * 20, 3, false, true));
        richRushPlayers.add(uuid);
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            richRushPlayers.remove(uuid);
            this.plugin.getAbilityManager().endAbilityDuration(player, abilityKey);
            Player p = Bukkit.getPlayer((UUID)uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage("\u00a7e\u00a7oRich Rush has worn off.");
            }
        }, (long)duration * 20L);
        Particle.DustOptions greenDust = new Particle.DustOptions(ParticleUtils.WEALTH_GREEN, 1.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 50, 0.5, 0.5, 0.5, 0.0, (Object)greenDust, true);
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0.0, 1.0, 0.0), 40, 0.5, 0.5, 0.5);
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, duration);
        this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "ability-activated", "ability", "Rich Rush");
        player.sendMessage("\u00a76\u00a7lRich Rush! \u00a7eMob and ore drops doubled for " + duration + "s!");
    }

    public static boolean hasRichRush(UUID uuid) {
        return richRushPlayers.contains(uuid);
    }

    public void amplification(Player player) {
        ItemStack offHand;
        if (this.plugin.getGemManager().getGemTier(player) < 2) {
            player.sendMessage("\u00a7c\u00a7oThis ability requires Tier 2!");
            return;
        }
        String abilityKey = "wealth-amplification";
        if (!this.plugin.getAbilityManager().canUseAbility(player, abilityKey)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (this.amplifiedPlayers.contains(uuid)) {
            player.sendMessage("\u00a7c\u00a7oAmplification is already active!");
            return;
        }
        int duration = this.plugin.getConfigManager().getAbilityDuration("wealth-amplification");
        boolean anyAmplified = false;
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; ++i) {
            if (!this.amplifyItem(armor[i])) continue;
            anyAmplified = true;
        }
        player.getInventory().setArmorContents(armor);
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (this.amplifyItem(mainHand)) {
            anyAmplified = true;
        }
        if (this.amplifyItem(offHand = player.getInventory().getItemInOffHand())) {
            anyAmplified = true;
        }
        this.amplifiedPlayers.add(uuid);
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            this.amplifiedPlayers.remove(uuid);
            this.plugin.getAbilityManager().endAbilityDuration(player, abilityKey);
            Player p = Bukkit.getPlayer((UUID)uuid);
            if (p == null || !p.isOnline()) {
                return;
            }
            this.revertAllAmplifiedItems(p);
            p.sendMessage("\u00a7e\u00a7oAmplification has worn off. Enchantments restored.");
        }, (long)duration * 20L);
        Particle.DustOptions greenDust = new Particle.DustOptions(ParticleUtils.WEALTH_GREEN, 1.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);
        player.spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 100, 0.5, 1.0, 0.5, 0.0, (Object)greenDust, true);
        player.spawnParticle(Particle.ENCHANT, player.getLocation().add(0.0, 1.0, 0.0), 80, 0.5, 1.0, 0.5);
        if (this.plugin.getAchievementManager() != null && anyAmplified) {
            this.plugin.getAchievementManager().unlock(player, Achievement.BOUNDARY_BREAK);
        }
        this.plugin.getAbilityManager().useAbilityWithDuration(player, abilityKey, duration);
        this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "ability-activated", "ability", "Amplification");
    }

    private boolean amplifyItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        Map<Enchantment, Integer> enchants = item.getEnchantments();
        if (enchants.isEmpty()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean modified = false;
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            NamespacedKey ampKey;
            Enchantment enchant = entry.getKey();
            int currentLevel = entry.getValue();
            NamespacedKey aeKey = new NamespacedKey((Plugin)this.plugin, AE_PDC_PREFIX + enchant.getKey().getKey());
            if (pdc.has(aeKey, PersistentDataType.INTEGER) || pdc.has(ampKey = new NamespacedKey((Plugin)this.plugin, AMP_PDC_PREFIX + enchant.getKey().getKey()), PersistentDataType.INTEGER)) continue;
            pdc.set(ampKey, PersistentDataType.INTEGER, currentLevel);
            meta.addEnchant(enchant, currentLevel + 1, true);
            modified = true;
        }
        if (modified) {
            item.setItemMeta(meta);
        }
        return modified;
    }

    private void revertAllAmplifiedItems(Player p) {
        ItemStack offHand;
        for (int i = 0; i < p.getInventory().getSize(); ++i) {
            ItemStack item = p.getInventory().getItem(i);
            if (item == null || item.getType().isAir()) continue;
            WealthAbilities.stripAmplifyEnchants(item);
        }
        ItemStack[] armor = p.getInventory().getArmorContents();
        boolean armorModified = false;
        for (int i = 0; i < armor.length; ++i) {
            if (armor[i] == null || armor[i].getType().isAir() || !WealthAbilities.stripAmplifyEnchants(armor[i])) continue;
            armorModified = true;
        }
        if (armorModified) {
            p.getInventory().setArmorContents(armor);
        }
        if (!(offHand = p.getInventory().getItemInOffHand()).getType().isAir()) {
            WealthAbilities.stripAmplifyEnchants(offHand);
        }
    }

    public static boolean stripAmplifyEnchants(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean modified = false;
        for (Enchantment enchant : Enchantment.values()) {
            NamespacedKey ampKey = WealthAbilities.getAmpKey(item, enchant);
            if (ampKey == null || !pdc.has(ampKey, PersistentDataType.INTEGER)) continue;
            int originalLevel = (Integer)pdc.get(ampKey, PersistentDataType.INTEGER);
            if (originalLevel == 0) {
                meta.removeEnchant(enchant);
            } else {
                meta.addEnchant(enchant, originalLevel, true);
            }
            pdc.remove(ampKey);
            modified = true;
        }
        if (modified) {
            item.setItemMeta(meta);
        }
        return modified;
    }

    public static boolean hasAmplifyEnchants(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        for (Enchantment enchant : Enchantment.values()) {
            NamespacedKey ampKey = WealthAbilities.getAmpKey(item, enchant);
            if (ampKey == null || !pdc.has(ampKey, PersistentDataType.INTEGER)) continue;
            return true;
        }
        return false;
    }

    private static NamespacedKey getAmpKey(ItemStack item, Enchantment enchant) {
        if (!item.hasItemMeta()) {
            return null;
        }
        return NamespacedKey.fromString((String)("blissgems:amp_orig_" + enchant.getKey().getKey()));
    }

    public static boolean isUnfortunate(UUID uuid) {
        return unfortunatePlayers.contains(uuid);
    }

    public static boolean shouldUnfortunateFail(UUID uuid, double failChance) {
        return unfortunatePlayers.contains(uuid) && Math.random() < failChance;
    }

    public static boolean isItemLocked(UUID uuid) {
        return itemLockedPlayers.containsKey(uuid);
    }

    public static ItemStack getLockedItem(UUID uuid) {
        return itemLockedPlayers.get(uuid);
    }

    @Override
    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        if (this.amplifiedPlayers.remove(uuid)) {
            this.revertAllAmplifiedItems(player);
            this.plugin.getAbilityManager().endAbilityDuration(player, "wealth-amplification");
        }
        unfortunatePlayers.remove(uuid);
        itemLockedPlayers.remove(uuid);
        if (richRushPlayers.remove(uuid)) {
            this.plugin.getAbilityManager().endAbilityDuration(player, "wealth-rich-rush");
        }
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

    public void savePocketsInventory(UUID uuid) {
        Inventory inv = this.pocketsInventories.get(uuid);
        if (inv == null) {
            return;
        }
        File file = new File(this.pocketsDataFolder, uuid.toString() + ".yml");
        YamlConfiguration data = new YamlConfiguration();
        for (int i = 0; i < inv.getSize(); ++i) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;
            data.set("items." + i, (Object)item);
        }
        try {
            data.save(file);
        }
        catch (IOException e) {
            this.plugin.getLogger().severe("Failed to save pockets inventory for " + String.valueOf(uuid) + ": " + e.getMessage());
        }
    }

    private void loadPocketsInventory(UUID uuid, Inventory inv) {
        File file = new File(this.pocketsDataFolder, uuid.toString() + ".yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration((File)file);
        if (data.contains("items")) {
            for (String key : data.getConfigurationSection("items").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ItemStack item = data.getItemStack("items." + key);
                    if (item == null || slot < 0 || slot >= inv.getSize()) continue;
                    inv.setItem(slot, item);
                }
                catch (NumberFormatException e) {
                    this.plugin.getLogger().warning("Invalid slot key in pockets data: " + key);
                }
            }
        }
    }

    public void saveAllPockets() {
        for (UUID uuid : this.pocketsInventories.keySet()) {
            this.savePocketsInventory(uuid);
        }
    }
}

