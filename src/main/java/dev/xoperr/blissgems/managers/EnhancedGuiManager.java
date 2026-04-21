package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.EnergyState;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EnhancedGuiManager implements Listener {
    private final BlissGems plugin;
    private final Map<UUID, Integer> playerPage = new HashMap<>(); // Track which page player is on
    private final Map<UUID, Integer> playerSubPage = new HashMap<>(); // For admin sub-menus
    private static final String PLAYER_GUI_TITLE = "§5§lBlissGems Menu";
    private static final String ADMIN_GUI_TITLE = "§c§l[ADMIN] BlissGems Control";
    private static final String ADMIN_GEMS_TITLE = "§9§l[ADMIN] Enabled Gems";
    private static final String ADMIN_SETTINGS_TITLE = "§6§l[ADMIN] Settings";
    private static final String ADMIN_GEMOPS_TITLE = "§d§l[ADMIN] Gem Operations";
    private static final String ADMIN_ENERGY_TITLE = "§c§l[ADMIN] Energy Control";
    private static final String ADMIN_PLAYERS_TITLE = "§a§l[ADMIN] Player Management";
    private static final String ADMIN_CONFIG_EDITOR_TITLE = "§e§l[ADMIN] Config Editor";

    public EnhancedGuiManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        if (player.hasPermission("blissgems.admin")) {
            openAdminDashboard(player, 0); // Page parameter kept for compatibility but not used
        } else {
            openPlayerDashboard(player);
        }
    }

    // ==================== PLAYER DASHBOARD ====================
    private void openPlayerDashboard(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, PLAYER_GUI_TITLE);

        // Row 2: Gem Info, Energy, Reroll (centered)
        gui.setItem(11, createGemInfoItem(player));
        gui.setItem(13, createEnergyInfoItem(player));
        if (player.hasPermission("blissgems.admin")) {
            gui.setItem(15, createRerollItem(player));
        }

        // Row 3: Abilities, Passives, Cooldowns (centered)
        gui.setItem(20, createAbilitiesItem(player));
        gui.setItem(22, createPassivesItem(player));
        gui.setItem(24, createCooldownsItem(player));

        // Row 4: Trusted, Stats, Settings (centered)
        gui.setItem(29, createTrustedItem(player));
        gui.setItem(31, createStatsItem(player));
        gui.setItem(33, createSettingsItem(player));

        // No decorative borders - cleaner look

        player.openInventory(gui);
        playerPage.put(player.getUniqueId(), 0);
    }

    // ==================== ADMIN DASHBOARD ====================
    private void openAdminDashboard(Player player, int page) {
        Inventory gui = Bukkit.createInventory(null, 54, ADMIN_GUI_TITLE);

        // Admin Controls (centered layout)
        gui.setItem(10, createPlayersControlItem());
        gui.setItem(12, createEnabledGemsItem());
        gui.setItem(14, createSettingsControlItem());
        gui.setItem(16, createGemOpsItem());

        gui.setItem(19, createEnergyControlItem());
        gui.setItem(21, createReloadConfigItem());
        gui.setItem(23, createConfigEditorItem());

        // No borders for cleaner look

        player.openInventory(gui);
    }

    // ==================== PLAYER ITEMS ====================

    private ItemStack createGemInfoItem(Player player) {
        GemType gemType = plugin.getGemManager().getGemType(player);
        int tier = plugin.getGemManager().getGemTier(player);

        ItemStack item;
        List<String> lore = new ArrayList<>();

        if (gemType != null) {
            item = new ItemStack(Material.ECHO_SHARD);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§d§lYour Gem");

            lore.add("§7Type: §f" + gemType.getDisplayName());
            lore.add("§7Tier: §f" + tier);
            lore.add("");
            lore.add("§7Description:");
            String[] descLines = gemType.getDescription().split("\n");
            for (String line : descLines) {
                lore.add("§8" + line);
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        } else {
            item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§c§lNo Gem");
            lore.add("§7You don't have a gem equipped!");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createEnergyInfoItem(Player player) {
        int energy = plugin.getEnergyManager().getEnergy(player);
        EnergyState state = plugin.getEnergyManager().getEnergyState(player);

        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§lEnergy Status");

        List<String> lore = new ArrayList<>();
        lore.add("§7Energy: §f" + energy + "§8/§f10");
        lore.add("§7State: §f" + state.getDisplayName());
        lore.add("");

        // Energy bar
        StringBuilder energyBar = new StringBuilder("§8[");
        for (int i = 0; i < 10; i++) {
            if (i < energy) {
                energyBar.append("§a■");
            } else {
                energyBar.append("§7■");
            }
        }
        energyBar.append("§8]");
        lore.add(energyBar.toString());
        lore.add("");

        if (energy == 0) {
            lore.add("§c✘ Abilities disabled");
            lore.add("§c✘ Passives disabled");
        } else if (energy == 1) {
            lore.add("§a✔ Abilities enabled");
            lore.add("§c✘ Passives disabled");
        } else {
            lore.add("§a✔ Abilities enabled");
            lore.add("§a✔ Passives enabled");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRerollItem(Player player) {
        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§l🎰 SPIN FOR NEW GEM");

        List<String> lore = new ArrayList<>();
        lore.add("§7Click to reroll your gem!");
        lore.add("§7Cost: 2 Energy");
        lore.add("§7You get random gem (no repeats)");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAbilitiesItem(Player player) {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§lAbilities");

        List<String> lore = new ArrayList<>();
        GemType gemType = plugin.getGemManager().getGemType(player);
        if (gemType != null) {
            lore.add("§7T1: §f" + gemType.getDisplayName());
            lore.add("§7T2: §fAvailable");
        } else {
            lore.add("§7No gem equipped");
        }
        lore.add("§8Click for details");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPassivesItem(Player player) {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§lPassive Effects");

        List<String> lore = new ArrayList<>();
        GemType gemType = plugin.getGemManager().getGemType(player);
        if (gemType != null) {
            lore.add("§7Gem: §f" + gemType.getDisplayName());
            lore.add("§7Status: §aActive");
        } else {
            lore.add("§7No passives");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCooldownsItem(Player player) {
        ItemStack item = new ItemStack(Material.REPEATER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§3§lCooldowns");

        List<String> lore = new ArrayList<>();
        lore.add("§7Track your ability");
        lore.add("§7cooldown timers");
        lore.add("§8Click for details");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTrustedItem(Player player) {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§2§lTrusted Players");

        int trustedCount = plugin.getTrustedPlayersManager().getTrustedPlayers(player).size();
        List<String> lore = new ArrayList<>();
        lore.add("§7Count: §f" + trustedCount);
        lore.add("§8Use /bliss trust <player>");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatsItem(Player player) {
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§lYour Stats");

        int kills = plugin.getStatsManager().getKills(player);
        List<String> lore = new ArrayList<>();
        lore.add("§7Kills: §f" + kills);
        lore.add("§8Type /bliss stats");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSettingsItem(Player player) {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§lSettings");

        List<String> lore = new ArrayList<>();
        boolean clickEnabled = plugin.getClickActivationManager().isClickActivationEnabled(player);
        lore.add("§7Click Activation: " + (clickEnabled ? "§aEnabled" : "§cDisabled"));
        lore.add("§8Use /bliss toggle_click");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ==================== ADMIN ITEMS ====================

    private ItemStack createPlayersControlItem() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§l[Admin] Players");

        List<String> lore = new ArrayList<>();
        lore.add("§7Manage player gems & energy");
        lore.add("§7(Click for player list)");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEnabledGemsItem() {
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§9§l[Admin] Enabled Gems");

        List<String> lore = new ArrayList<>();
        lore.add("§7Toggle gems on/off");
        lore.add("§7(Click to see status)");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSettingsControlItem() {
        ItemStack item = new ItemStack(Material.REPEATING_COMMAND_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§l[Admin] Settings");

        List<String> lore = new ArrayList<>();
        lore.add("§7Max Energy: 10");
        lore.add("§7Starting Energy: 5");
        lore.add("§7(Click to adjust)");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGemOpsItem() {
        ItemStack item = new ItemStack(Material.SHULKER_BOX);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§l[Admin] Gem Ops");

        List<String> lore = new ArrayList<>();
        lore.add("§7Give items:");
        lore.add("§7- Energy Bottles");
        lore.add("§7- Reroll Tokens");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEnergyControlItem() {
        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§l[Admin] Energy Control");

        List<String> lore = new ArrayList<>();
        lore.add("§7Global energy settings");
        lore.add("§7Scale all players by 1.5x");
        lore.add("§7Or set all to X energy");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createReloadConfigItem() {
        ItemStack item = new ItemStack(Material.LEVER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§l[Admin] Reload Config");

        List<String> lore = new ArrayList<>();
        lore.add("§7Reload config.yml");
        lore.add("§7All changes apply");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createConfigEditorItem() {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§l[Admin] Config Editor");

        List<String> lore = new ArrayList<>();
        lore.add("§7Edit config values");
        lore.add("§7in-game!");
        lore.add("§aClick to open editor");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }


    // ==================== ADMIN SUB-MENUS ====================

    private void openEnabledGemsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ADMIN_GEMS_TITLE);

        // Centered layout
        gui.setItem(10, createGemToggleItem(GemType.FIRE));
        gui.setItem(11, createGemToggleItem(GemType.SPEED));
        gui.setItem(12, createGemToggleItem(GemType.WEALTH));
        gui.setItem(13, createGemToggleItem(GemType.ASTRA));
        gui.setItem(14, createGemToggleItem(GemType.PUFF));
        gui.setItem(15, createGemToggleItem(GemType.FLUX));
        gui.setItem(16, createGemToggleItem(GemType.LIFE));
        gui.setItem(19, createGemToggleItem(GemType.STRENGTH));

        // Back button
        gui.setItem(49, createBackButton());

        // No borders for cleaner look

        player.openInventory(gui);
    }

    private void openSettingsControl(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ADMIN_SETTINGS_TITLE);

        int maxEnergy = plugin.getConfig().getInt("energy.max-energy", 10);
        int startEnergy = plugin.getConfig().getInt("energy.starting-energy", 10);
        int gainOnKill = plugin.getConfig().getInt("energy.gain-on-kill", 1);
        int lossOnDeath = plugin.getConfig().getInt("energy.loss-on-death", 1);

        gui.setItem(11, createSettingItem("Max Energy", maxEnergy, "Maximum energy capacity"));
        gui.setItem(13, createSettingItem("Starting Energy", startEnergy, "Energy for new gems"));
        gui.setItem(15, createSettingItem("Gain on Kill", gainOnKill, "Energy gained per kill"));
        gui.setItem(20, createSettingItem("Loss on Death", lossOnDeath, "Energy lost on death"));

        gui.setItem(49, createBackButton());

        // No borders for cleaner look

        player.openInventory(gui);
    }

    private void openGemOpsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ADMIN_GEMOPS_TITLE);

        gui.setItem(11, createGiveItemButton("Energy Bottle", Material.HONEY_BOTTLE, "Give 1 energy bottle"));
        gui.setItem(13, createGiveItemButton("Repair Kit", Material.RECOVERY_COMPASS, "Give 1 repair kit"));
        gui.setItem(15, createGiveItemButton("Upgrader", Material.NETHER_STAR, "Give 1 upgrader"));
        gui.setItem(20, createGiveItemButton("Trader", Material.EMERALD, "Give 1 trader"));
        gui.setItem(22, createGiveItemButton("Gem Fragment", Material.PRISMARINE_SHARD, "Give 1 gem fragment"));

        gui.setItem(49, createBackButton());

        // No borders for cleaner look

        player.openInventory(gui);
    }

    private void openEnergyControl(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ADMIN_ENERGY_TITLE);

        gui.setItem(11, createEnergyOpButton("Set All to 10", "§aSet everyone to max energy"));
        gui.setItem(13, createEnergyOpButton("Set All to 5", "§eSet everyone to half energy"));
        gui.setItem(15, createEnergyOpButton("Add 1 to All", "§bGive everyone +1 energy"));
        gui.setItem(20, createEnergyOpButton("Remove 1 from All", "§cTake -1 energy from everyone"));

        gui.setItem(49, createBackButton());

        // No borders for cleaner look

        player.openInventory(gui);
    }

    private void openPlayersControl(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ADMIN_PLAYERS_TITLE);

        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        int slot = 0;
        for (int i = 0; i < Math.min(45, onlinePlayers.size()); i++) {
            Player target = onlinePlayers.get(i);
            gui.setItem(slot++, createPlayerManageItem(target));
        }

        gui.setItem(49, createBackButton());

        // No borders for cleaner look

        player.openInventory(gui);
    }

    private void openConfigEditor(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ADMIN_CONFIG_EDITOR_TITLE);

        // Energy settings (row 2)
        gui.setItem(10, createEditableConfigItem("Max Energy", "energy.max-energy", 10));
        gui.setItem(11, createEditableConfigItem("Starting Energy", "energy.starting-energy", 10));
        gui.setItem(12, createEditableConfigItem("Gain on Kill", "energy.gain-on-kill", 1));
        gui.setItem(13, createEditableConfigItem("Loss on Death", "energy.loss-on-death", 1));

        // Tier 1 ability cooldowns (row 3)
        gui.setItem(19, createEditableConfigItem("Astra Daggers", "abilities.cooldowns.astra-daggers", 15));
        gui.setItem(20, createEditableConfigItem("Fire Fireball", "abilities.cooldowns.fire-fireball", 10));
        gui.setItem(21, createEditableConfigItem("Flux Ground", "abilities.cooldowns.flux-ground", 45));
        gui.setItem(22, createEditableConfigItem("Life Drainer", "abilities.cooldowns.life-heart-drainer", 60));
        gui.setItem(23, createEditableConfigItem("Puff Dash", "abilities.cooldowns.puff-dash", 5));
        gui.setItem(24, createEditableConfigItem("Speed Sedative", "abilities.cooldowns.speed-sedative", 35));
        gui.setItem(25, createEditableConfigItem("Strength Thorns", "abilities.cooldowns.strength-bloodthorns", 20));

        // Tier 2 ability cooldowns (row 4)
        gui.setItem(28, createEditableConfigItem("Astral Projection", "abilities.cooldowns.astra-projection", 120));
        gui.setItem(29, createEditableConfigItem("Fire Campfire", "abilities.cooldowns.fire-campfire", 60));
        gui.setItem(30, createEditableConfigItem("Flux Beam", "abilities.cooldowns.flux-beam", 240));
        gui.setItem(31, createEditableConfigItem("Circle of Life", "abilities.cooldowns.life-circle-of-life", 60));
        gui.setItem(32, createEditableConfigItem("Breezy Bash", "abilities.cooldowns.puff-breezy-bash", 10));
        gui.setItem(33, createEditableConfigItem("Adrenaline Rush", "abilities.cooldowns.adrenaline-rush", 90));
        gui.setItem(34, createEditableConfigItem("Speed Storm", "abilities.cooldowns.speed-storm", 45));

        // More cooldowns (row 5)
        gui.setItem(37, createEditableConfigItem("Frailer", "abilities.cooldowns.strength-frailer", 25));
        gui.setItem(38, createEditableConfigItem("Chad Strength", "abilities.cooldowns.strength-chad", 30));
        gui.setItem(39, createEditableConfigItem("Durability Chip", "abilities.cooldowns.wealth-durability-chip", 30));
        gui.setItem(40, createEditableConfigItem("Unfortunate", "abilities.cooldowns.wealth-unfortunate", 90));
        gui.setItem(41, createEditableConfigItem("Rich Rush", "abilities.cooldowns.wealth-rich-rush", 540));
        gui.setItem(42, createEditableConfigItem("Amplification", "abilities.cooldowns.wealth-amplification", 180));

        gui.setItem(49, createBackButton());

        // No borders for cleaner look

        player.openInventory(gui);
    }

    // ==================== ADMIN SUB-MENU ITEMS ====================

    private ItemStack createGemToggleItem(GemType gemType) {
        boolean enabled = plugin.getConfig().getBoolean("gems.enabled." + gemType.name().toLowerCase(), true);

        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((enabled ? "§a" : "§7") + gemType.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("§7Status: " + (enabled ? "§aEnabled" : "§cDisabled"));
        lore.add("");
        lore.add("§eClick to toggle!");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSettingItem(String name, int value, String desc) {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6" + name);

        List<String> lore = new ArrayList<>();
        lore.add("§7Current: §f" + value);
        lore.add("§7" + desc);
        lore.add("");
        lore.add("§8(Read-only - use Config Editor)");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEditableConfigItem(String name, String configPath, int defaultValue) {
        int value = plugin.getConfig().getInt(configPath, defaultValue);

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e" + name);

        List<String> lore = new ArrayList<>();
        lore.add("§7Current: §f" + value);
        lore.add("§7Path: §8" + configPath);
        lore.add("");
        lore.add("§aLeft-Click: §7+1");
        lore.add("§aShift Left-Click: §7+10");
        lore.add("§cRight-Click: §7-1");
        lore.add("§cShift Right-Click: §7-10");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGiveItemButton(String name, Material material, String desc) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d" + name);

        List<String> lore = new ArrayList<>();
        lore.add("§7" + desc);
        lore.add("");
        lore.add("§eLeft-click: Give to yourself");
        lore.add("§eRight-click: Give to all online");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEnergyOpButton(String name, String desc) {
        ItemStack item = new ItemStack(Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c" + name);

        List<String> lore = new ArrayList<>();
        lore.add(desc);
        lore.add("");
        lore.add("§eClick to execute!");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlayerManageItem(Player target) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e" + target.getName());

        GemType gemType = plugin.getGemManager().getGemType(target);
        int energy = plugin.getEnergyManager().getEnergy(target);

        List<String> lore = new ArrayList<>();
        lore.add("§7Gem: §f" + (gemType != null ? gemType.getDisplayName() : "None"));
        lore.add("§7Energy: §f" + energy + "§8/§f10");
        lore.add("");
        lore.add("§eLeft-click: Manage player");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c« Back");

        List<String> lore = new ArrayList<>();
        lore.add("§7Return to main menu");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ==================== UTILITY ITEMS ====================

    private ItemStack createBorderItem(Player player) {
        GemType gemType = plugin.getGemManager().getGemType(player);
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

    // ==================== CLICK HANDLER ====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();

        // Check if it's any of our GUIs
        if (title.equals(PLAYER_GUI_TITLE) || title.equals(ADMIN_GUI_TITLE) ||
            title.equals(ADMIN_GEMS_TITLE) || title.equals(ADMIN_SETTINGS_TITLE) ||
            title.equals(ADMIN_GEMOPS_TITLE) || title.equals(ADMIN_ENERGY_TITLE) ||
            title.equals(ADMIN_PLAYERS_TITLE) || title.equals(ADMIN_CONFIG_EDITOR_TITLE)) {

            event.setCancelled(true);

            int slot = event.getSlot();
            boolean isRightClick = event.getClick().isRightClick();
            boolean isShiftClick = event.getClick().isShiftClick();

            if (title.equals(ADMIN_GUI_TITLE)) {
                handleAdminDashboardClick(player, slot);
            } else if (title.equals(PLAYER_GUI_TITLE)) {
                handlePlayerGuiClick(player, slot);
            } else if (title.equals(ADMIN_GEMS_TITLE)) {
                handleEnabledGemsClick(player, slot);
            } else if (title.equals(ADMIN_SETTINGS_TITLE)) {
                handleSettingsClick(player, slot);
            } else if (title.equals(ADMIN_GEMOPS_TITLE)) {
                handleGemOpsClick(player, slot, isRightClick);
            } else if (title.equals(ADMIN_ENERGY_TITLE)) {
                handleEnergyControlClick(player, slot);
            } else if (title.equals(ADMIN_PLAYERS_TITLE)) {
                handlePlayersClick(player, slot);
            } else if (title.equals(ADMIN_CONFIG_EDITOR_TITLE)) {
                handleConfigEditorClick(player, slot, isRightClick, isShiftClick);
            }
        }
    }

    private void handlePlayerGuiClick(Player player, int slot) {
        switch (slot) {
            case 15: // Reroll Gem
                handleRerollGem(player);
                break;
            case 20: // Abilities Info
                player.closeInventory();
                player.sendMessage("§c§l⚔ ABILITIES §r§7- Check /bliss for ability commands!");
                player.sendMessage("§7Your gem abilities can be activated by right-clicking your gem.");
                break;
            case 24: // Cooldowns
                player.closeInventory();
                player.sendMessage("§3§l⏱ COOLDOWNS §r§7- Hold your gem to see cooldowns in action bar!");
                break;
            case 29: // Trusted Players
                player.closeInventory();
                player.performCommand("bliss trusted");
                break;
            case 31: // Stats
                player.closeInventory();
                player.performCommand("bliss stats me");
                break;
            case 33: // Settings
                handleSettingsToggle(player);
                break;
        }
    }

    private void handleRerollGem(Player player) {
        if (!player.hasPermission("blissgems.admin")) {
            player.sendMessage("§c§lNo Permission! §7Only admins can reroll gems.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        int currentEnergy = plugin.getEnergyManager().getEnergy(player);

        if (currentEnergy < 2) {
            player.sendMessage("§c§lInsufficient Energy! §7You need §c2 energy §7to reroll.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        GemType currentGem = plugin.getGemManager().getGemType(player);

        if (currentGem == null) {
            player.sendMessage("§c§lNo gem found! §7You need a gem to reroll.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Get random gem (different from current)
        GemType[] allGems = GemType.values();
        GemType newGem;
        do {
            newGem = allGems[new java.util.Random().nextInt(allGems.length)];
        } while (newGem == currentGem);

        // Replace gem type (keeps tier and updates textures)
        boolean success = plugin.getGemManager().replaceGemType(player, newGem);

        if (!success) {
            player.sendMessage("§c§lReroll failed! §7Could not replace gem.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Deduct energy AFTER successful replacement
        plugin.getEnergyManager().setEnergy(player, currentEnergy - 2);

        player.closeInventory();
        player.sendMessage("§a§l✔ GEM REROLLED!");
        player.sendMessage("§7New Gem: §f" + newGem.getDisplayName());
        player.sendMessage("§7Energy: §c" + (currentEnergy - 2) + " §8(-2)");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);

        // Show particle effects
        player.spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
        player.spawnParticle(org.bukkit.Particle.ENCHANT, player.getLocation().add(0, 1, 0), 100, 0.8, 1.0, 0.8);
    }

    private void handleSettingsToggle(Player player) {
        boolean currentState = plugin.getClickActivationManager().isClickActivationEnabled(player);
        plugin.getClickActivationManager().setClickActivation(player, !currentState);

        player.sendMessage("§e§l⚙ SETTINGS");
        player.sendMessage("§7Click Activation: " + (!currentState ? "§aEnabled" : "§cDisabled"));
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

        // Refresh GUI
        openPlayerDashboard(player);
    }

    private void handleAdminDashboardClick(Player player, int slot) {
        switch (slot) {
            case 10: // Players Control
                openPlayersControl(player);
                break;
            case 12: // Enabled Gems
                openEnabledGemsMenu(player);
                break;
            case 14: // Settings Control
                openSettingsControl(player);
                break;
            case 16: // Gem Ops
                openGemOpsMenu(player);
                break;
            case 19: // Energy Control
                openEnergyControl(player);
                break;
            case 21: // Reload Config
                player.closeInventory();
                plugin.reloadConfig();
                plugin.getConfigManager().reload();
                player.sendMessage("§a§l✔ Config reloaded successfully!");
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
                break;
            case 23: // Config Editor
                openConfigEditor(player);
                break;
        }
    }

    // ==================== SUB-MENU CLICK HANDLERS ====================

    private void handleEnabledGemsClick(Player player, int slot) {
        if (slot == 49) { // Back button
            openMainMenu(player);
            return;
        }

        // Map slots to gem types
        GemType gemType = null;
        switch (slot) {
            case 10: gemType = GemType.FIRE; break;
            case 11: gemType = GemType.SPEED; break;
            case 12: gemType = GemType.WEALTH; break;
            case 13: gemType = GemType.ASTRA; break;
            case 14: gemType = GemType.PUFF; break;
            case 15: gemType = GemType.FLUX; break;
            case 16: gemType = GemType.LIFE; break;
            case 19: gemType = GemType.STRENGTH; break;
        }

        if (gemType != null) {
            String configPath = "gems.enabled." + gemType.name().toLowerCase();
            boolean currentState = plugin.getConfig().getBoolean(configPath, true);

            plugin.getConfig().set(configPath, !currentState);
            plugin.saveConfig();

            player.sendMessage("§e" + gemType.getDisplayName() + " §7is now " + (!currentState ? "§aEnabled" : "§cDisabled"));
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, !currentState ? 1.5f : 0.8f);

            // Refresh menu
            openEnabledGemsMenu(player);
        }
    }

    private void handleSettingsClick(Player player, int slot) {
        if (slot == 49) { // Back button
            openMainMenu(player);
            return;
        }

        player.sendMessage("§7Settings are read-only. Use Config Editor to change values.");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
    }

    private void handleGemOpsClick(Player player, int slot, boolean isRightClick) {
        if (slot == 49) { // Back button
            openMainMenu(player);
            return;
        }

        String itemId = null;
        String itemName = null;

        switch (slot) {
            case 11: // Energy Bottle
                itemId = "energy_bottle";
                itemName = "Energy Bottle";
                break;
            case 13: // Repair Kit
                itemId = "repair_kit";
                itemName = "Repair Kit";
                break;
            case 15: // Upgrader
                itemId = "gem_upgrader";
                itemName = "Upgrader";
                break;
            case 20: // Trader
                itemId = "gem_trader";
                itemName = "Trader";
                break;
            case 22: // Gem Fragment
                itemId = "gem_fragment";
                itemName = "Gem Fragment";
                break;
        }

        if (itemId != null) {
            if (isRightClick) {
                // Give to all online players
                int count = 0;
                for (Player target : Bukkit.getOnlinePlayers()) {
                    ItemStack item = dev.xoperr.blissgems.utils.CustomItemManager.getItemById(itemId);
                    if (item != null) {
                        target.getInventory().addItem(item);
                        target.sendMessage("§a§l✔ Received " + itemName + " from admin!");
                        count++;
                    }
                }
                player.sendMessage("§a§l✔ Gave " + itemName + " to " + count + " players!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            } else {
                // Give to self
                ItemStack item = dev.xoperr.blissgems.utils.CustomItemManager.getItemById(itemId);
                if (item != null) {
                    player.getInventory().addItem(item);
                    player.sendMessage("§a§l✔ Received " + itemName + "!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.5f);
                }
            }
        }
    }

    private void handleEnergyControlClick(Player player, int slot) {
        if (slot == 49) { // Back button
            openMainMenu(player);
            return;
        }

        int count = 0;
        String action = "";

        switch (slot) {
            case 11: // Set All to 10
                for (Player target : Bukkit.getOnlinePlayers()) {
                    plugin.getEnergyManager().setEnergy(target, 10);
                    count++;
                }
                action = "Set all players to 10 energy";
                break;
            case 13: // Set All to 5
                for (Player target : Bukkit.getOnlinePlayers()) {
                    plugin.getEnergyManager().setEnergy(target, 5);
                    count++;
                }
                action = "Set all players to 5 energy";
                break;
            case 15: // Add 1 to All
                for (Player target : Bukkit.getOnlinePlayers()) {
                    int current = plugin.getEnergyManager().getEnergy(target);
                    plugin.getEnergyManager().setEnergy(target, Math.min(10, current + 1));
                    count++;
                }
                action = "Added +1 energy to all players";
                break;
            case 20: // Remove 1 from All
                for (Player target : Bukkit.getOnlinePlayers()) {
                    int current = plugin.getEnergyManager().getEnergy(target);
                    plugin.getEnergyManager().setEnergy(target, Math.max(0, current - 1));
                    count++;
                }
                action = "Removed -1 energy from all players";
                break;
        }

        if (!action.isEmpty()) {
            player.sendMessage("§c§l⚡ ENERGY CONTROL");
            player.sendMessage("§7" + action + " §8(§f" + count + " §7players)");
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);

            // Notify all players
            for (Player target : Bukkit.getOnlinePlayers()) {
                target.sendMessage("§c§l⚡ Energy updated by admin!");
            }
        }
    }

    private void handlePlayersClick(Player player, int slot) {
        if (slot == 49) { // Back button
            openMainMenu(player);
            return;
        }

        if (slot < 45) {
            List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
            if (slot < onlinePlayers.size()) {
                Player target = onlinePlayers.get(slot);
                player.closeInventory();
                player.sendMessage("§a§l» Managing: §f" + target.getName());
                player.sendMessage("§7Use commands:");
                player.sendMessage("§e/bliss give " + target.getName() + " <gem> [tier]");
                player.sendMessage("§e/bliss energy " + target.getName() + " set <amount>");
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
        }
    }

    private void handleConfigEditorClick(Player player, int slot, boolean isRightClick, boolean isShiftClick) {
        if (slot == 49) { // Back button
            openMainMenu(player);
            return;
        }

        // Map slots to config paths
        String configPath = null;
        int defaultValue = 0;
        String displayName = "";

        switch (slot) {
            // Energy settings (row 2)
            case 10: configPath = "energy.max-energy"; defaultValue = 10; displayName = "Max Energy"; break;
            case 11: configPath = "energy.starting-energy"; defaultValue = 10; displayName = "Starting Energy"; break;
            case 12: configPath = "energy.gain-on-kill"; defaultValue = 1; displayName = "Gain on Kill"; break;
            case 13: configPath = "energy.loss-on-death"; defaultValue = 1; displayName = "Loss on Death"; break;

            // Tier 1 ability cooldowns (row 3)
            case 19: configPath = "abilities.cooldowns.astra-daggers"; defaultValue = 15; displayName = "Astra Daggers"; break;
            case 20: configPath = "abilities.cooldowns.fire-fireball"; defaultValue = 10; displayName = "Fire Fireball"; break;
            case 21: configPath = "abilities.cooldowns.flux-ground"; defaultValue = 45; displayName = "Flux Ground"; break;
            case 22: configPath = "abilities.cooldowns.life-heart-drainer"; defaultValue = 60; displayName = "Life Drainer"; break;
            case 23: configPath = "abilities.cooldowns.puff-dash"; defaultValue = 5; displayName = "Puff Dash"; break;
            case 24: configPath = "abilities.cooldowns.speed-sedative"; defaultValue = 35; displayName = "Speed Sedative"; break;
            case 25: configPath = "abilities.cooldowns.strength-bloodthorns"; defaultValue = 20; displayName = "Strength Thorns"; break;

            // Tier 2 ability cooldowns (row 4)
            case 28: configPath = "abilities.cooldowns.astra-projection"; defaultValue = 120; displayName = "Astral Projection"; break;
            case 29: configPath = "abilities.cooldowns.fire-campfire"; defaultValue = 60; displayName = "Fire Campfire"; break;
            case 30: configPath = "abilities.cooldowns.flux-beam"; defaultValue = 240; displayName = "Flux Beam"; break;
            case 31: configPath = "abilities.cooldowns.life-circle-of-life"; defaultValue = 60; displayName = "Circle of Life"; break;
            case 32: configPath = "abilities.cooldowns.puff-breezy-bash"; defaultValue = 10; displayName = "Breezy Bash"; break;
            case 33: configPath = "abilities.cooldowns.adrenaline-rush"; defaultValue = 90; displayName = "Adrenaline Rush"; break;
            case 34: configPath = "abilities.cooldowns.speed-storm"; defaultValue = 45; displayName = "Speed Storm"; break;

            // More cooldowns (row 5)
            case 37: configPath = "abilities.cooldowns.strength-frailer"; defaultValue = 25; displayName = "Frailer"; break;
            case 38: configPath = "abilities.cooldowns.strength-chad"; defaultValue = 30; displayName = "Chad Strength"; break;
            case 39: configPath = "abilities.cooldowns.wealth-durability-chip"; defaultValue = 30; displayName = "Durability Chip"; break;
            case 40: configPath = "abilities.cooldowns.wealth-unfortunate"; defaultValue = 90; displayName = "Unfortunate"; break;
            case 41: configPath = "abilities.cooldowns.wealth-rich-rush"; defaultValue = 540; displayName = "Rich Rush"; break;
            case 42: configPath = "abilities.cooldowns.wealth-amplification"; defaultValue = 180; displayName = "Amplification"; break;
        }

        if (configPath != null) {
            int currentValue = plugin.getConfig().getInt(configPath, defaultValue);
            int change = 0;

            if (isRightClick) {
                // Right-click: decrease
                change = isShiftClick ? -10 : -1;
            } else {
                // Left-click: increase
                change = isShiftClick ? 10 : 1;
            }

            int newValue = Math.max(0, currentValue + change); // Prevent negative values

            plugin.getConfig().set(configPath, newValue);
            plugin.saveConfig();

            player.sendMessage("§e§l⚙ CONFIG EDITOR");
            player.sendMessage("§7" + displayName + ": §f" + currentValue + " §7→ §f" + newValue + " §8(" + (change >= 0 ? "+" : "") + change + ")");
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, change >= 0 ? 1.2f : 0.8f);

            // Refresh the GUI to show updated values
            openConfigEditor(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        playerPage.remove(player.getUniqueId());
        playerSubPage.remove(player.getUniqueId());
    }
}
