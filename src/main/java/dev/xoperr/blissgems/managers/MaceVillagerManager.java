package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class MaceVillagerManager {
    private final BlissGems plugin;
    private final NamespacedKey villagerTypeKey;
    private final NamespacedKey tradeHistoryKey;

    public static final String TYPE_MACE_1 = "mace_1";
    public static final String TYPE_MACE_2 = "mace_2";
    public static final String TYPE_MACE_3 = "mace_3";
    public static final String TYPE_ENERGY = "energy_games";

    public MaceVillagerManager(BlissGems plugin) {
        this.plugin = plugin;
        this.villagerTypeKey = new NamespacedKey((Plugin) plugin, "custom_villager_type");
        this.tradeHistoryKey = new NamespacedKey((Plugin) plugin, "custom_trade_history");
    }

    public NamespacedKey getVillagerTypeKey() {
        return villagerTypeKey;
    }

    public NamespacedKey getTradeHistoryKey() {
        return tradeHistoryKey;
    }

    public boolean isCustomVillager(Villager villager) {
        if (villager == null) return false;
        return villager.getPersistentDataContainer().has(villagerTypeKey, PersistentDataType.STRING);
    }

    public String getVillagerType(Villager villager) {
        if (!isCustomVillager(villager)) return null;
        return villager.getPersistentDataContainer().get(villagerTypeKey, PersistentDataType.STRING);
    }

    public Villager spawnCustomVillager(Location loc, String type) {
        Villager villager = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        setupVillager(villager, type, null);
        return villager;
    }

    public void setupVillager(Villager villager, String type, String tradeHistory) {
        villager.setAI(true);
        villager.setBreed(false);
        villager.setAdult();
        villager.setRemoveWhenFarAway(false);
        villager.setPersistent(true);

        PersistentDataContainer pdc = villager.getPersistentDataContainer();
        pdc.set(villagerTypeKey, PersistentDataType.STRING, type);
        if (tradeHistory != null && !tradeHistory.isEmpty()) {
            pdc.set(tradeHistoryKey, PersistentDataType.STRING, tradeHistory);
        } else if (!pdc.has(tradeHistoryKey, PersistentDataType.STRING)) {
            pdc.set(tradeHistoryKey, PersistentDataType.STRING, "");
        }

        List<MerchantRecipe> recipes = new ArrayList<>();
        switch (type) {
            case TYPE_MACE_1: {
                villager.setCustomName(ChatColor.translateAlternateColorCodes('&', "&e&lMace Artisan &7(Triad I)"));
                villager.setCustomNameVisible(true);
                villager.setProfession(Villager.Profession.WEAPONSMITH);
                villager.setVillagerType(Villager.Type.PLAINS);

                // Trade 0: Mace 1 (Standard Enchantable Mace)
                ItemStack mace1 = CustomItemManager.getItemById("mace_1");
                MerchantRecipe recipe0 = new MerchantRecipe(mace1, 999999);
                recipe0.addIngredient(new ItemStack(Material.EMERALD, 64));
                recipe0.addIngredient(new ItemStack(Material.DIAMOND, 32));
                recipes.add(recipe0);

                // Trade 1: Mythic Netherite Helmet
                ItemStack helm = CustomItemManager.getItemById("netherite_helmet_mythic");
                MerchantRecipe recipe1 = new MerchantRecipe(helm, 999999);
                recipe1.addIngredient(new ItemStack(Material.EMERALD, 64));
                recipe1.addIngredient(new ItemStack(Material.NETHERITE_SCRAP, 32));
                recipes.add(recipe1);
                break;
            }
            case TYPE_MACE_2: {
                villager.setCustomName(ChatColor.translateAlternateColorCodes('&', "&c&lMace Warden &7(Triad II)"));
                villager.setCustomNameVisible(true);
                villager.setProfession(Villager.Profession.ARMORER);
                villager.setVillagerType(Villager.Type.TAIGA);

                // Trade 0: Mace 2 (Tempered / Limited Mace)
                ItemStack mace2 = CustomItemManager.getItemById("mace_limited");
                MerchantRecipe recipe0 = new MerchantRecipe(mace2, 999999);
                recipe0.addIngredient(new ItemStack(Material.EMERALD, 64));
                recipe0.addIngredient(new ItemStack(Material.DIAMOND, 32));
                recipes.add(recipe0);

                // Trade 1: Mythic Netherite Chestplate
                ItemStack chest = CustomItemManager.getItemById("netherite_chestplate_mythic");
                MerchantRecipe recipe1 = new MerchantRecipe(chest, 999999);
                recipe1.addIngredient(new ItemStack(Material.EMERALD, 64));
                recipe1.addIngredient(new ItemStack(Material.NETHERITE_SCRAP, 48));
                recipes.add(recipe1);

                // Trade 2: Mythic Netherite Leggings
                ItemStack legs = CustomItemManager.getItemById("netherite_leggings_mythic");
                MerchantRecipe recipe2 = new MerchantRecipe(legs, 999999);
                recipe2.addIngredient(new ItemStack(Material.EMERALD, 64));
                recipe2.addIngredient(new ItemStack(Material.NETHERITE_SCRAP, 40));
                recipes.add(recipe2);
                break;
            }
            case TYPE_MACE_3: {
                villager.setCustomName(ChatColor.translateAlternateColorCodes('&', "&b&lMace Champion &7(Triad III)"));
                villager.setCustomNameVisible(true);
                villager.setProfession(Villager.Profession.TOOLSMITH);
                villager.setVillagerType(Villager.Type.DESERT);

                // Trade 0: Mace 3 (Overcharged Mace)
                ItemStack mace3 = CustomItemManager.getItemById("mace_3");
                MerchantRecipe recipe0 = new MerchantRecipe(mace3, 999999);
                recipe0.addIngredient(new ItemStack(Material.EMERALD, 64));
                recipe0.addIngredient(new ItemStack(Material.DIAMOND, 32));
                recipes.add(recipe0);

                // Trade 1: Mythic Netherite Boots
                ItemStack boots = CustomItemManager.getItemById("netherite_boots_mythic");
                MerchantRecipe recipe1 = new MerchantRecipe(boots, 999999);
                recipe1.addIngredient(new ItemStack(Material.EMERALD, 64));
                recipe1.addIngredient(new ItemStack(Material.NETHERITE_SCRAP, 32));
                recipes.add(recipe1);
                break;
            }
            case TYPE_ENERGY: {
                villager.setCustomName(ChatColor.translateAlternateColorCodes('&', "&a&lEnergy Games Master"));
                villager.setCustomNameVisible(true);
                villager.setProfession(Villager.Profession.CLERIC);
                villager.setVillagerType(Villager.Type.SAVANNA);

                // Trade 0: Player Tracker -> Energy Bottle
                ItemStack tracker = CustomItemManager.getItemById("player_tracker");
                ItemStack energyBottle = CustomItemManager.getItemById("energy_bottle");
                if (energyBottle == null) {
                    energyBottle = new ItemStack(Material.EXPERIENCE_BOTTLE, 10);
                }
                MerchantRecipe recipe0 = new MerchantRecipe(energyBottle, 999999);
                recipe0.addIngredient(tracker);
                recipes.add(recipe0);
                break;
            }
        }
        villager.setRecipes(recipes);
    }

    public boolean hasPlayerTraded(Villager villager, int tradeIndex, UUID playerUuid) {
        if (villager == null || playerUuid == null) return false;
        PersistentDataContainer pdc = villager.getPersistentDataContainer();
        String raw = pdc.get(tradeHistoryKey, PersistentDataType.STRING);
        if (raw == null || raw.isEmpty()) return false;
        String token = tradeIndex + ":" + playerUuid.toString();
        return raw.contains(token);
    }

    public void recordPlayerTrade(Villager villager, int tradeIndex, UUID playerUuid) {
        if (villager == null || playerUuid == null) return;
        PersistentDataContainer pdc = villager.getPersistentDataContainer();
        String raw = pdc.get(tradeHistoryKey, PersistentDataType.STRING);
        if (raw == null) raw = "";
        String token = tradeIndex + ":" + playerUuid.toString();
        if (!raw.contains(token)) {
            raw = raw.isEmpty() ? token : raw + ";" + token;
            pdc.set(tradeHistoryKey, PersistentDataType.STRING, raw);
        }
    }

    public ItemStack convertToSoul(Villager villager) {
        String type = getVillagerType(villager);
        if (type == null) return null;
        PersistentDataContainer pdc = villager.getPersistentDataContainer();
        String tradeHistory = pdc.get(tradeHistoryKey, PersistentDataType.STRING);
        if (tradeHistory == null) tradeHistory = "";

        ItemStack soul = CustomItemManager.getItemById("villager_soul");
        if (soul == null) {
            soul = new ItemStack(Material.NETHER_STAR);
        }
        ItemMeta meta = soul.getItemMeta();
        if (meta != null) {
            String title = villager.getCustomName() != null ? villager.getCustomName() : type;
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&d&lVillager Soul &8(" + title + "&8)"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Captured soul of a specialized merchant.");
            lore.add(ChatColor.YELLOW + "Shift + Right-Click block to release.");
            lore.add(ChatColor.DARK_GRAY + "Retains all trade history and limits.");
            meta.setLore(lore);

            PersistentDataContainer itemPdc = meta.getPersistentDataContainer();
            itemPdc.set(CustomItemManager.getVillagerSoulKey(), PersistentDataType.STRING, type);
            itemPdc.set(CustomItemManager.getVillagerDataKey(), PersistentDataType.STRING, tradeHistory);
            soul.setItemMeta(meta);
        }
        return soul;
    }

    public Villager restoreFromSoul(Location loc, ItemStack soulItem) {
        if (soulItem == null || !soulItem.hasItemMeta()) return null;
        PersistentDataContainer itemPdc = soulItem.getItemMeta().getPersistentDataContainer();
        if (!itemPdc.has(CustomItemManager.getVillagerSoulKey(), PersistentDataType.STRING)) {
            return null;
        }
        String type = itemPdc.get(CustomItemManager.getVillagerSoulKey(), PersistentDataType.STRING);
        String tradeHistory = itemPdc.get(CustomItemManager.getVillagerDataKey(), PersistentDataType.STRING);

        Villager villager = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        setupVillager(villager, type, tradeHistory);

        loc.getWorld().spawnParticle(Particle.SOUL, loc.clone().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 1.4f);
        return villager;
    }
}
