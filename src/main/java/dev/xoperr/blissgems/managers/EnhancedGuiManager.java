/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.EnergyState;
import dev.xoperr.blissgems.utils.GemType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EnhancedGuiManager
implements Listener {
    private final BlissGems plugin;
    private final Map<UUID, Integer> playerPage = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> playerSubPage = new HashMap<UUID, Integer>();
    private static final String PLAYER_GUI_TITLE = "\u00a75\u00a7lBlissGems Menu";
    private static final String ADMIN_GUI_TITLE = "\u00a7c\u00a7l[ADMIN] BlissGems Control";
    private static final String ADMIN_GEMS_TITLE = "\u00a79\u00a7l[ADMIN] Enabled Gems";
    private static final String ADMIN_SETTINGS_TITLE = "\u00a76\u00a7l[ADMIN] Settings";
    private static final String ADMIN_GEMOPS_TITLE = "\u00a7d\u00a7l[ADMIN] Gem Operations";
    private static final String ADMIN_ENERGY_TITLE = "\u00a7c\u00a7l[ADMIN] Energy Control";
    private static final String ADMIN_PLAYERS_TITLE = "\u00a7a\u00a7l[ADMIN] Player Management";
    private static final String ADMIN_CONFIG_EDITOR_TITLE = "\u00a7e\u00a7l[ADMIN] Config Editor";

    public EnhancedGuiManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        if (player.hasPermission("blissgems.admin")) {
            this.openAdminDashboard(player, 0);
        } else {
            this.openPlayerDashboard(player);
        }
    }

    private void openPlayerDashboard(Player player) {
        Inventory gui = Bukkit.createInventory(null, (int)54, (String)PLAYER_GUI_TITLE);
        gui.setItem(11, this.createGemInfoItem(player));
        gui.setItem(13, this.createEnergyInfoItem(player));
        if (player.hasPermission("blissgems.admin")) {
            gui.setItem(15, this.createRerollItem(player));
        }
        gui.setItem(20, this.createAbilitiesItem(player));
        gui.setItem(22, this.createPassivesItem(player));
        gui.setItem(24, this.createCooldownsItem(player));
        gui.setItem(29, this.createTrustedItem(player));
        gui.setItem(31, this.createStatsItem(player));
        gui.setItem(33, this.createSettingsItem(player));
        player.openInventory(gui);
        this.playerPage.put(player.getUniqueId(), 0);
    }

    private void openAdminDashboard(Player player, int page) {
        Inventory gui = Bukkit.createInventory(null, (int)54, (String)ADMIN_GUI_TITLE);
        gui.setItem(10, this.createPlayersControlItem());
        gui.setItem(12, this.createEnabledGemsItem());
        gui.setItem(14, this.createSettingsControlItem());
        gui.setItem(16, this.createGemOpsItem());
        gui.setItem(19, this.createEnergyControlItem());
        gui.setItem(21, this.createReloadConfigItem());
        gui.setItem(23, this.createConfigEditorItem());
        player.openInventory(gui);
    }

    private ItemStack createGemInfoItem(Player player) {
        ItemStack item;
        GemType gemType = this.plugin.getGemManager().getGemType(player);
        int tier = this.plugin.getGemManager().getGemTier(player);
        ArrayList<String> lore = new ArrayList<String>();
        if (gemType != null) {
            String[] descLines;
            item = new ItemStack(Material.ECHO_SHARD);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("\u00a7d\u00a7lYour Gem");
            lore.add("\u00a77Type: \u00a7f" + gemType.getDisplayName());
            lore.add("\u00a77Tier: \u00a7f" + tier);
            lore.add("");
            lore.add("\u00a77Description:");
            for (String line : descLines = gemType.getDescription().split("\n")) {
                lore.add("\u00a78" + line);
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        } else {
            item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("\u00a7c\u00a7lNo Gem");
            lore.add("\u00a77You don't have a gem equipped!");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createEnergyInfoItem(Player player) {
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        EnergyState state = this.plugin.getEnergyManager().getEnergyState(player);
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7b\u00a7lEnergy Status");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("\u00a77Energy: \u00a7f" + energy + "\u00a78/\u00a7f10");
        lore.add("\u00a77State: \u00a7f" + state.getDisplayName());
        lore.add("");
        StringBuilder energyBar = new StringBuilder("\u00a78[");
        for (int i = 0; i < 10; ++i) {
            if (i < energy) {
                energyBar.append("\u00a7a\u25a0");
                continue;
            }
            energyBar.append("\u00a77\u25a0");
        }
        energyBar.append("\u00a78]");
        lore.add(energyBar.toString());
        lore.add("");
        if (energy == 0) {
            lore.add("\u00a7c\u2718 Abilities disabled");
            lore.add("\u00a7c\u2718 Passives disabled");
        } else if (energy == 1) {
            lore.add("\u00a7a\u2714 Abilities enabled");
            lore.add("\u00a7c\u2718 Passives disabled");
        } else {
            lore.add("\u00a7a\u2714 Abilities enabled");
            lore.add("\u00a7a\u2714 Passives enabled");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRerollItem(Player player) {
        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7e\u00a7l\ud83c\udfb0 SPIN FOR NEW GEM");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("\u00a77Click to reroll your gem!");
        lore.add("\u00a77Cost: 2 Energy");
        lore.add("\u00a77You get random gem (no repeats)");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAbilitiesItem(Player player) {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7c\u00a7lAbilities");
        ArrayList<String> lore = new ArrayList<String>();
        GemType gemType = this.plugin.getGemManager().getGemType(player);
        if (gemType != null) {
            lore.add("\u00a77T1: \u00a7f" + gemType.getDisplayName());
            lore.add("\u00a77T2: \u00a7fAvailable");
        } else {
            lore.add("\u00a77No gem equipped");
        }
        lore.add("\u00a78Click for details");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPassivesItem(Player player) {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a76\u00a7lPassive Effects");
        ArrayList<String> lore = new ArrayList<String>();
        GemType gemType = this.plugin.getGemManager().getGemType(player);
        if (gemType != null) {
            lore.add("\u00a77Gem: \u00a7f" + gemType.getDisplayName());
            lore.add("\u00a77Status: \u00a7aActive");
        } else {
            lore.add("\u00a77No passives");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCooldownsItem(Player player) {
        ItemStack item = new ItemStack(Material.REPEATER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a73\u00a7lCooldowns");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("\u00a77Track your ability");
        lore.add("\u00a77cooldown timers");
        lore.add("\u00a78Click for details");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTrustedItem(Player player) {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a72\u00a7lTrusted Players");
        int trustedCount = this.plugin.getTrustedPlayersManager().getTrustedPlayers(player).size();
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("\u00a77Count: \u00a7f" + trustedCount);
        lore.add("\u00a78Use /bliss trust <player>");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatsItem(Player player) {
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a76\u00a7lYour Stats");
        int kills = this.plugin.getStatsManager().getKills(player);
        ArrayList<String> lore = new ArrayList<>();
        lore.add("\u00a77Kills: \u00a7f" + kills);
        lore.add("\u00a78Type /bliss stats");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSettingsItem(Player player) {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7e\u00a7lSettings");
        ArrayList<String> lore = new ArrayList<>();
        boolean clickEnabled = this.plugin.getClickActivationManager().isClickActivationEnabled(player);
        lore.add("\u00a77Click Activation: " + (clickEnabled ? "\u00a7aEnabled" : "\u00a7cDisabled"));
        lore.add("\u00a78Use /bliss toggle_click");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlayersControlItem() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7a\u00a7l[Admin] Players");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("\u00a77Manage player gems & energy");
        lore.add("\u00a77(Click for player list)");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEnabledGemsItem() {
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a79\u00a7l[Admin] Enabled Gems");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("\u00a77Toggle gems on/off");
        lore.add("\u00a77(Click to see status)");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSettingsControlItem() {
        ItemStack item = new ItemStack(Material.REPEATING_COMMAND_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a76\u00a7l[Admin] Settings");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("\u00a77Max Energy: 10");
        lore.add("\u00a77Starting Energy: 5");
        lore.add("\u00a77(Click to adjust)");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGemOpsItem() {
        ItemStack item = new ItemStack(Material.SHULKER_BOX);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7d\u00a7l[Admin] Gem Ops");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("\u00a77Give items:");
        lore.add("\u00a77- Energy Bottles");
        lore.add("\u00a77- Reroll Tokens");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEnergyControlItem() {
        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7c\u00a7l[Admin] Energy Control");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("\u00a77Global energy settings");
        lore.add("\u00a77Scale all players by 1.5x");
        lore.add("\u00a77Or set all to X energy");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createReloadConfigItem() {
        ItemStack item = new ItemStack(Material.LEVER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7b\u00a7l[Admin] Reload Config");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("\u00a77Reload config.yml");
        lore.add("\u00a77All changes apply");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createConfigEditorItem() {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7e\u00a7l[Admin] Config Editor");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("\u00a77Edit config values");
        lore.add("\u00a77in-game!");
        lore.add("\u00a7aClick to open editor");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void openEnabledGemsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, (int)54, (String)ADMIN_GEMS_TITLE);
        gui.setItem(10, this.createGemToggleItem(GemType.FIRE));
        gui.setItem(11, this.createGemToggleItem(GemType.SPEED));
        gui.setItem(12, this.createGemToggleItem(GemType.WEALTH));
        gui.setItem(13, this.createGemToggleItem(GemType.ASTRA));
        gui.setItem(14, this.createGemToggleItem(GemType.PUFF));
        gui.setItem(15, this.createGemToggleItem(GemType.FLUX));
        gui.setItem(16, this.createGemToggleItem(GemType.LIFE));
        gui.setItem(19, this.createGemToggleItem(GemType.STRENGTH));
        gui.setItem(49, this.createBackButton());
        player.openInventory(gui);
    }

    private void openSettingsControl(Player player) {
        Inventory gui = Bukkit.createInventory(null, (int)54, (String)ADMIN_SETTINGS_TITLE);
        int maxEnergy = this.plugin.getConfig().getInt("energy.max-energy", 10);
        int startEnergy = this.plugin.getConfig().getInt("energy.starting-energy", 10);
        int gainOnKill = this.plugin.getConfig().getInt("energy.gain-on-kill", 1);
        int lossOnDeath = this.plugin.getConfig().getInt("energy.loss-on-death", 1);
        gui.setItem(11, this.createSettingItem("Max Energy", maxEnergy, "Maximum energy capacity"));
        gui.setItem(13, this.createSettingItem("Starting Energy", startEnergy, "Energy for new gems"));
        gui.setItem(15, this.createSettingItem("Gain on Kill", gainOnKill, "Energy gained per kill"));
        gui.setItem(20, this.createSettingItem("Loss on Death", lossOnDeath, "Energy lost on death"));
        gui.setItem(49, this.createBackButton());
        player.openInventory(gui);
    }

    private void openGemOpsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, (int)54, (String)ADMIN_GEMOPS_TITLE);
        gui.setItem(11, this.createGiveItemButton("Energy Bottle", Material.HONEY_BOTTLE, "Give 1 energy bottle"));
        gui.setItem(13, this.createGiveItemButton("Repair Kit", Material.RECOVERY_COMPASS, "Give 1 repair kit"));
        gui.setItem(15, this.createGiveItemButton("Upgrader", Material.NETHER_STAR, "Give 1 upgrader"));
        gui.setItem(20, this.createGiveItemButton("Trader", Material.EMERALD, "Give 1 trader"));
        gui.setItem(22, this.createGiveItemButton("Gem Fragment", Material.PRISMARINE_SHARD, "Give 1 gem fragment"));
        gui.setItem(49, this.createBackButton());
        player.openInventory(gui);
    }

    private void openEnergyControl(Player player) {
        Inventory gui = Bukkit.createInventory(null, (int)54, (String)ADMIN_ENERGY_TITLE);
        gui.setItem(11, this.createEnergyOpButton("Set All to 10", "\u00a7aSet everyone to max energy"));
        gui.setItem(13, this.createEnergyOpButton("Set All to 5", "\u00a7eSet everyone to half energy"));
        gui.setItem(15, this.createEnergyOpButton("Add 1 to All", "\u00a7bGive everyone +1 energy"));
        gui.setItem(20, this.createEnergyOpButton("Remove 1 from All", "\u00a7cTake -1 energy from everyone"));
        gui.setItem(49, this.createBackButton());
        player.openInventory(gui);
    }

    private void openPlayersControl(Player player) {
        Inventory gui = Bukkit.createInventory(null, (int)54, (String)ADMIN_PLAYERS_TITLE);
        ArrayList onlinePlayers = new ArrayList(Bukkit.getOnlinePlayers());
        int slot = 0;
        for (int i = 0; i < Math.min(45, onlinePlayers.size()); ++i) {
            Player target = (Player)onlinePlayers.get(i);
            gui.setItem(slot++, this.createPlayerManageItem(target));
        }
        gui.setItem(49, this.createBackButton());
        player.openInventory(gui);
    }

    private void openConfigEditor(Player player) {
        Inventory gui = Bukkit.createInventory(null, (int)54, (String)ADMIN_CONFIG_EDITOR_TITLE);
        gui.setItem(10, this.createEditableConfigItem("Max Energy", "energy.max-energy", 10));
        gui.setItem(11, this.createEditableConfigItem("Starting Energy", "energy.starting-energy", 10));
        gui.setItem(12, this.createEditableConfigItem("Gain on Kill", "energy.gain-on-kill", 1));
        gui.setItem(13, this.createEditableConfigItem("Loss on Death", "energy.loss-on-death", 1));
        gui.setItem(19, this.createEditableConfigItem("Astra Daggers", "abilities.cooldowns.astra-daggers", 15));
        gui.setItem(20, this.createEditableConfigItem("Fire Fireball", "abilities.cooldowns.fire-fireball", 10));
        gui.setItem(21, this.createEditableConfigItem("Flux Ground", "abilities.cooldowns.flux-ground", 45));
        gui.setItem(22, this.createEditableConfigItem("Life Drainer", "abilities.cooldowns.life-heart-drainer", 60));
        gui.setItem(23, this.createEditableConfigItem("Puff Dash", "abilities.cooldowns.puff-dash", 5));
        gui.setItem(24, this.createEditableConfigItem("Speed Sedative", "abilities.cooldowns.speed-sedative", 35));
        gui.setItem(25, this.createEditableConfigItem("Strength Thorns", "abilities.cooldowns.strength-bloodthorns", 20));
        gui.setItem(28, this.createEditableConfigItem("Astral Projection", "abilities.cooldowns.astra-projection", 120));
        gui.setItem(29, this.createEditableConfigItem("Fire Campfire", "abilities.cooldowns.fire-campfire", 60));
        gui.setItem(30, this.createEditableConfigItem("Flux Beam", "abilities.cooldowns.flux-beam", 240));
        gui.setItem(31, this.createEditableConfigItem("Circle of Life", "abilities.cooldowns.life-circle-of-life", 60));
        gui.setItem(32, this.createEditableConfigItem("Breezy Bash", "abilities.cooldowns.puff-breezy-bash", 10));
        gui.setItem(33, this.createEditableConfigItem("Adrenaline Rush", "abilities.cooldowns.adrenaline-rush", 90));
        gui.setItem(34, this.createEditableConfigItem("Speed Storm", "abilities.cooldowns.speed-storm", 45));
        gui.setItem(37, this.createEditableConfigItem("Frailer", "abilities.cooldowns.strength-frailer", 25));
        gui.setItem(38, this.createEditableConfigItem("Chad Strength", "abilities.cooldowns.strength-chad", 30));
        gui.setItem(39, this.createEditableConfigItem("Durability Chip", "abilities.cooldowns.wealth-durability-chip", 30));
        gui.setItem(40, this.createEditableConfigItem("Unfortunate", "abilities.cooldowns.wealth-unfortunate", 90));
        gui.setItem(41, this.createEditableConfigItem("Rich Rush", "abilities.cooldowns.wealth-rich-rush", 540));
        gui.setItem(42, this.createEditableConfigItem("Amplification", "abilities.cooldowns.wealth-amplification", 180));
        gui.setItem(49, this.createBackButton());
        player.openInventory(gui);
    }

    private ItemStack createGemToggleItem(GemType gemType) {
        boolean enabled = this.plugin.getConfig().getBoolean("gems.enabled." + gemType.name().toLowerCase(), true);
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((enabled ? "\u00a7a" : "\u00a77") + gemType.getDisplayName());
        ArrayList<String> lore = new ArrayList<>();
        lore.add("\u00a77Status: " + (enabled ? "\u00a7aEnabled" : "\u00a7cDisabled"));
        lore.add("");
        lore.add("\u00a7eClick to toggle!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSettingItem(String name, int value, String desc) {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a76" + name);
        ArrayList<String> lore = new ArrayList<>();
        lore.add("\u00a77Current: \u00a7f" + value);
        lore.add("\u00a77" + desc);
        lore.add("");
        lore.add("\u00a78(Read-only - use Config Editor)");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEditableConfigItem(String name, String configPath, int defaultValue) {
        int value = this.plugin.getConfig().getInt(configPath, defaultValue);
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7e" + name);
        ArrayList<String> lore = new ArrayList<>();
        lore.add("\u00a77Current: \u00a7f" + value);
        lore.add("\u00a77Path: \u00a78" + configPath);
        lore.add("");
        lore.add("\u00a7aLeft-Click: \u00a77+1");
        lore.add("\u00a7aShift Left-Click: \u00a77+10");
        lore.add("\u00a7cRight-Click: \u00a77-1");
        lore.add("\u00a7cShift Right-Click: \u00a77-10");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGiveItemButton(String name, Material material, String desc) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7d" + name);
        ArrayList<String> lore = new ArrayList<>();
        lore.add("\u00a77" + desc);
        lore.add("");
        lore.add("\u00a7eLeft-click: Give to yourself");
        lore.add("\u00a7eRight-click: Give to all online");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEnergyOpButton(String name, String desc) {
        ItemStack item = new ItemStack(Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7c" + name);
        ArrayList<String> lore = new ArrayList<String>();
        lore.add(desc);
        lore.add("");
        lore.add("\u00a7eClick to execute!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlayerManageItem(Player target) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7e" + target.getName());
        GemType gemType = this.plugin.getGemManager().getGemType(target);
        int energy = this.plugin.getEnergyManager().getEnergy(target);
        ArrayList<String> lore = new ArrayList<>();
        lore.add("\u00a77Gem: \u00a7f" + (gemType != null ? gemType.getDisplayName() : "None"));
        lore.add("\u00a77Energy: \u00a7f" + energy + "\u00a78/\u00a7f10");
        lore.add("");
        lore.add("\u00a7eLeft-click: Manage player");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7c\u00ab Back");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("\u00a77Return to main menu");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBorderItem(Player player) {
        GemType gemType = this.plugin.getGemManager().getGemType(player);
        Material glassMaterial = Material.GRAY_STAINED_GLASS_PANE;
        if (gemType != null) {
            glassMaterial = switch (gemType) {
                case FIRE -> Material.RED_STAINED_GLASS_PANE;
                case SPEED -> Material.YELLOW_STAINED_GLASS_PANE;
                case WEALTH -> Material.CYAN_STAINED_GLASS_PANE;
                case ASTRA -> Material.MAGENTA_STAINED_GLASS_PANE;
                case PUFF -> Material.LIME_STAINED_GLASS_PANE;
                case FLUX -> Material.BLUE_STAINED_GLASS_PANE;
                case LIFE -> Material.PINK_STAINED_GLASS_PANE;
                case STRENGTH -> Material.ORANGE_STAINED_GLASS_PANE;
                default -> Material.GRAY_STAINED_GLASS_PANE;
            };
        }
        ItemStack item = new ItemStack(glassMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAdminBorderItem() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player)event.getWhoClicked();
        if (title.equals(PLAYER_GUI_TITLE) || title.equals(ADMIN_GUI_TITLE) || title.equals(ADMIN_GEMS_TITLE) || title.equals(ADMIN_SETTINGS_TITLE) || title.equals(ADMIN_GEMOPS_TITLE) || title.equals(ADMIN_ENERGY_TITLE) || title.equals(ADMIN_PLAYERS_TITLE) || title.equals(ADMIN_CONFIG_EDITOR_TITLE)) {
            event.setCancelled(true);
            int slot = event.getSlot();
            boolean isRightClick = event.getClick().isRightClick();
            boolean isShiftClick = event.getClick().isShiftClick();
            if (title.equals(ADMIN_GUI_TITLE)) {
                this.handleAdminDashboardClick(player, slot);
            } else if (title.equals(PLAYER_GUI_TITLE)) {
                this.handlePlayerGuiClick(player, slot);
            } else if (title.equals(ADMIN_GEMS_TITLE)) {
                this.handleEnabledGemsClick(player, slot);
            } else if (title.equals(ADMIN_SETTINGS_TITLE)) {
                this.handleSettingsClick(player, slot);
            } else if (title.equals(ADMIN_GEMOPS_TITLE)) {
                this.handleGemOpsClick(player, slot, isRightClick);
            } else if (title.equals(ADMIN_ENERGY_TITLE)) {
                this.handleEnergyControlClick(player, slot);
            } else if (title.equals(ADMIN_PLAYERS_TITLE)) {
                this.handlePlayersClick(player, slot);
            } else if (title.equals(ADMIN_CONFIG_EDITOR_TITLE)) {
                this.handleConfigEditorClick(player, slot, isRightClick, isShiftClick);
            }
        }
    }

    private void handlePlayerGuiClick(Player player, int slot) {
        switch (slot) {
            case 15: {
                this.handleRerollGem(player);
                break;
            }
            case 20: {
                player.closeInventory();
                player.sendMessage("\u00a7c\u00a7l\u2694 ABILITIES \u00a7r\u00a77- Check /bliss for ability commands!");
                player.sendMessage("\u00a77Your gem abilities can be activated by right-clicking your gem.");
                break;
            }
            case 24: {
                player.closeInventory();
                player.sendMessage("\u00a73\u00a7l\u23f1 COOLDOWNS \u00a7r\u00a77- Hold your gem to see cooldowns in action bar!");
                break;
            }
            case 29: {
                player.closeInventory();
                player.performCommand("bliss trusted");
                break;
            }
            case 31: {
                player.closeInventory();
                player.performCommand("bliss stats me");
                break;
            }
            case 33: {
                this.handleSettingsToggle(player);
            }
        }
    }

    private void handleRerollGem(Player player) {
        GemType newGem;
        if (!player.hasPermission("blissgems.admin")) {
            player.sendMessage("\u00a7c\u00a7lNo Permission! \u00a77Only admins can reroll gems.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        int currentEnergy = this.plugin.getEnergyManager().getEnergy(player);
        if (currentEnergy < 2) {
            player.sendMessage("\u00a7c\u00a7lInsufficient Energy! \u00a77You need \u00a7c2 energy \u00a77to reroll.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        GemType currentGem = this.plugin.getGemManager().getGemType(player);
        if (currentGem == null) {
            player.sendMessage("\u00a7c\u00a7lNo gem found! \u00a77You need a gem to reroll.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        GemType[] allGems = GemType.values();
        while ((newGem = allGems[new Random().nextInt(allGems.length)]) == currentGem) {
        }
        boolean success = this.plugin.getGemManager().replaceGemType(player, newGem);
        if (!success) {
            player.sendMessage("\u00a7c\u00a7lReroll failed! \u00a77Could not replace gem.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        this.plugin.getEnergyManager().setEnergy(player, currentEnergy - 2);
        player.closeInventory();
        player.sendMessage("\u00a7a\u00a7l\u2714 GEM REROLLED!");
        player.sendMessage("\u00a77New Gem: \u00a7f" + newGem.getDisplayName());
        player.sendMessage("\u00a77Energy: \u00a7c" + (currentEnergy - 2) + " \u00a78(-2)");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
        player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0.0, 1.0, 0.0), 50, 0.5, 0.5, 0.5, 0.1);
        player.spawnParticle(Particle.ENCHANT, player.getLocation().add(0.0, 1.0, 0.0), 100, 0.8, 1.0, 0.8);
    }

    private void handleSettingsToggle(Player player) {
        boolean currentState = this.plugin.getClickActivationManager().isClickActivationEnabled(player);
        this.plugin.getClickActivationManager().setClickActivation(player, !currentState);
        player.sendMessage("\u00a7e\u00a7l\u2699 SETTINGS");
        player.sendMessage("\u00a77Click Activation: " + (!currentState ? "\u00a7aEnabled" : "\u00a7cDisabled"));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        this.openPlayerDashboard(player);
    }

    private void handleAdminDashboardClick(Player player, int slot) {
        switch (slot) {
            case 10: {
                this.openPlayersControl(player);
                break;
            }
            case 12: {
                this.openEnabledGemsMenu(player);
                break;
            }
            case 14: {
                this.openSettingsControl(player);
                break;
            }
            case 16: {
                this.openGemOpsMenu(player);
                break;
            }
            case 19: {
                this.openEnergyControl(player);
                break;
            }
            case 21: {
                player.closeInventory();
                this.plugin.reloadConfig();
                this.plugin.getConfigManager().reload();
                player.sendMessage("\u00a7a\u00a7l\u2714 Config reloaded successfully!");
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
                break;
            }
            case 23: {
                this.openConfigEditor(player);
            }
        }
    }

    private void handleEnabledGemsClick(Player player, int slot) {
        if (slot == 49) {
            this.openMainMenu(player);
            return;
        }
        GemType gemType = null;
        switch (slot) {
            case 10: {
                gemType = GemType.FIRE;
                break;
            }
            case 11: {
                gemType = GemType.SPEED;
                break;
            }
            case 12: {
                gemType = GemType.WEALTH;
                break;
            }
            case 13: {
                gemType = GemType.ASTRA;
                break;
            }
            case 14: {
                gemType = GemType.PUFF;
                break;
            }
            case 15: {
                gemType = GemType.FLUX;
                break;
            }
            case 16: {
                gemType = GemType.LIFE;
                break;
            }
            case 19: {
                gemType = GemType.STRENGTH;
            }
        }
        if (gemType != null) {
            String configPath = "gems.enabled." + gemType.name().toLowerCase();
            boolean currentState = this.plugin.getConfig().getBoolean(configPath, true);
            this.plugin.getConfig().set(configPath, (Object)(!currentState ? 1 : 0));
            this.plugin.saveConfig();
            player.sendMessage("\u00a7e" + gemType.getDisplayName() + " \u00a77is now " + (!currentState ? "\u00a7aEnabled" : "\u00a7cDisabled"));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, !currentState ? 1.5f : 0.8f);
            this.openEnabledGemsMenu(player);
        }
    }

    private void handleSettingsClick(Player player, int slot) {
        if (slot == 49) {
            this.openMainMenu(player);
            return;
        }
        player.sendMessage("\u00a77Settings are read-only. Use Config Editor to change values.");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
    }

    private void handleGemOpsClick(Player player, int slot, boolean isRightClick) {
        if (slot == 49) {
            this.openMainMenu(player);
            return;
        }
        String itemId = null;
        String itemName = null;
        switch (slot) {
            case 11: {
                itemId = "energy_bottle";
                itemName = "Energy Bottle";
                break;
            }
            case 13: {
                itemId = "repair_kit";
                itemName = "Repair Kit";
                break;
            }
            case 15: {
                itemId = "gem_upgrader";
                itemName = "Upgrader";
                break;
            }
            case 20: {
                itemId = "gem_trader";
                itemName = "Trader";
                break;
            }
            case 22: {
                itemId = "gem_fragment";
                itemName = "Gem Fragment";
            }
        }
        if (itemId != null) {
            if (isRightClick) {
                int count = 0;
                for (Player target : Bukkit.getOnlinePlayers()) {
                    ItemStack item = CustomItemManager.getItemById(itemId);
                    if (item == null) continue;
                    target.getInventory().addItem(new ItemStack[]{item});
                    target.sendMessage("\u00a7a\u00a7l\u2714 Received " + itemName + " from admin!");
                    ++count;
                }
                player.sendMessage("\u00a7a\u00a7l\u2714 Gave " + itemName + " to " + count + " players!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            } else {
                ItemStack item = CustomItemManager.getItemById(itemId);
                if (item != null) {
                    player.getInventory().addItem(new ItemStack[]{item});
                    player.sendMessage("\u00a7a\u00a7l\u2714 Received " + itemName + "!");
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.5f);
                }
            }
        }
    }

    private void handleEnergyControlClick(Player player, int slot) {
        if (slot == 49) {
            this.openMainMenu(player);
            return;
        }
        int count = 0;
        String action = "";
        switch (slot) {
            case 11: {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    this.plugin.getEnergyManager().setEnergy(target, 10);
                    ++count;
                }
                action = "Set all players to 10 energy";
                break;
            }
            case 13: {
                for (Player target : Bukkit.getOnlinePlayers()) {
                    this.plugin.getEnergyManager().setEnergy(target, 5);
                    ++count;
                }
                action = "Set all players to 5 energy";
                break;
            }
            case 15: {
                int current;
                for (Player target : Bukkit.getOnlinePlayers()) {
                    current = this.plugin.getEnergyManager().getEnergy(target);
                    this.plugin.getEnergyManager().setEnergy(target, Math.min(10, current + 1));
                    ++count;
                }
                action = "Added +1 energy to all players";
                break;
            }
            case 20: {
                int current;
                for (Player target : Bukkit.getOnlinePlayers()) {
                    current = this.plugin.getEnergyManager().getEnergy(target);
                    this.plugin.getEnergyManager().setEnergy(target, Math.max(0, current - 1));
                    ++count;
                }
                action = "Removed -1 energy from all players";
            }
        }
        if (!action.isEmpty()) {
            player.sendMessage("\u00a7c\u00a7l\u26a1 ENERGY CONTROL");
            player.sendMessage("\u00a77" + action + " \u00a78(\u00a7f" + count + " \u00a77players)");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);
            for (Player target : Bukkit.getOnlinePlayers()) {
                target.sendMessage("\u00a7c\u00a7l\u26a1 Energy updated by admin!");
            }
        }
    }

    private void handlePlayersClick(Player player, int slot) {
        ArrayList onlinePlayers;
        if (slot == 49) {
            this.openMainMenu(player);
            return;
        }
        if (slot < 45 && slot < (onlinePlayers = new ArrayList(Bukkit.getOnlinePlayers())).size()) {
            Player target = (Player)onlinePlayers.get(slot);
            player.closeInventory();
            player.sendMessage("\u00a7a\u00a7l\u00bb Managing: \u00a7f" + target.getName());
            player.sendMessage("\u00a77Use commands:");
            player.sendMessage("\u00a7e/bliss give " + target.getName() + " <gem> [tier]");
            player.sendMessage("\u00a7e/bliss energy " + target.getName() + " set <amount>");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }

    private void handleConfigEditorClick(Player player, int slot, boolean isRightClick, boolean isShiftClick) {
        if (slot == 49) {
            this.openMainMenu(player);
            return;
        }
        String configPath = null;
        int defaultValue = 0;
        String displayName = "";
        switch (slot) {
            case 10: {
                configPath = "energy.max-energy";
                defaultValue = 10;
                displayName = "Max Energy";
                break;
            }
            case 11: {
                configPath = "energy.starting-energy";
                defaultValue = 10;
                displayName = "Starting Energy";
                break;
            }
            case 12: {
                configPath = "energy.gain-on-kill";
                defaultValue = 1;
                displayName = "Gain on Kill";
                break;
            }
            case 13: {
                configPath = "energy.loss-on-death";
                defaultValue = 1;
                displayName = "Loss on Death";
                break;
            }
            case 19: {
                configPath = "abilities.cooldowns.astra-daggers";
                defaultValue = 15;
                displayName = "Astra Daggers";
                break;
            }
            case 20: {
                configPath = "abilities.cooldowns.fire-fireball";
                defaultValue = 10;
                displayName = "Fire Fireball";
                break;
            }
            case 21: {
                configPath = "abilities.cooldowns.flux-ground";
                defaultValue = 45;
                displayName = "Flux Ground";
                break;
            }
            case 22: {
                configPath = "abilities.cooldowns.life-heart-drainer";
                defaultValue = 60;
                displayName = "Life Drainer";
                break;
            }
            case 23: {
                configPath = "abilities.cooldowns.puff-dash";
                defaultValue = 5;
                displayName = "Puff Dash";
                break;
            }
            case 24: {
                configPath = "abilities.cooldowns.speed-sedative";
                defaultValue = 35;
                displayName = "Speed Sedative";
                break;
            }
            case 25: {
                configPath = "abilities.cooldowns.strength-bloodthorns";
                defaultValue = 20;
                displayName = "Strength Thorns";
                break;
            }
            case 28: {
                configPath = "abilities.cooldowns.astra-projection";
                defaultValue = 120;
                displayName = "Astral Projection";
                break;
            }
            case 29: {
                configPath = "abilities.cooldowns.fire-campfire";
                defaultValue = 60;
                displayName = "Fire Campfire";
                break;
            }
            case 30: {
                configPath = "abilities.cooldowns.flux-beam";
                defaultValue = 240;
                displayName = "Flux Beam";
                break;
            }
            case 31: {
                configPath = "abilities.cooldowns.life-circle-of-life";
                defaultValue = 60;
                displayName = "Circle of Life";
                break;
            }
            case 32: {
                configPath = "abilities.cooldowns.puff-breezy-bash";
                defaultValue = 10;
                displayName = "Breezy Bash";
                break;
            }
            case 33: {
                configPath = "abilities.cooldowns.adrenaline-rush";
                defaultValue = 90;
                displayName = "Adrenaline Rush";
                break;
            }
            case 34: {
                configPath = "abilities.cooldowns.speed-storm";
                defaultValue = 45;
                displayName = "Speed Storm";
                break;
            }
            case 37: {
                configPath = "abilities.cooldowns.strength-frailer";
                defaultValue = 25;
                displayName = "Frailer";
                break;
            }
            case 38: {
                configPath = "abilities.cooldowns.strength-chad";
                defaultValue = 30;
                displayName = "Chad Strength";
                break;
            }
            case 39: {
                configPath = "abilities.cooldowns.wealth-durability-chip";
                defaultValue = 30;
                displayName = "Durability Chip";
                break;
            }
            case 40: {
                configPath = "abilities.cooldowns.wealth-unfortunate";
                defaultValue = 90;
                displayName = "Unfortunate";
                break;
            }
            case 41: {
                configPath = "abilities.cooldowns.wealth-rich-rush";
                defaultValue = 540;
                displayName = "Rich Rush";
                break;
            }
            case 42: {
                configPath = "abilities.cooldowns.wealth-amplification";
                defaultValue = 180;
                displayName = "Amplification";
            }
        }
        if (configPath != null) {
            int currentValue = this.plugin.getConfig().getInt(configPath, defaultValue);
            int change = 0;
            change = isRightClick ? (isShiftClick ? -10 : -1) : (isShiftClick ? 10 : 1);
            int newValue = Math.max(0, currentValue + change);
            this.plugin.getConfig().set(configPath, (Object)newValue);
            this.plugin.saveConfig();
            player.sendMessage("\u00a7e\u00a7l\u2699 CONFIG EDITOR");
            player.sendMessage("\u00a77" + displayName + ": \u00a7f" + currentValue + " \u00a77\u2192 \u00a7f" + newValue + " \u00a78(" + (change >= 0 ? "+" : "") + change + ")");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, change >= 0 ? 1.2f : 0.8f);
            this.openConfigEditor(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player)event.getPlayer();
        this.playerPage.remove(player.getUniqueId());
        this.playerSubPage.remove(player.getUniqueId());
    }
}

