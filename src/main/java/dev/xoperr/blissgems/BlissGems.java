/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package dev.xoperr.blissgems;

import dev.xoperr.blissgems.abilities.AstraAbilities;
import dev.xoperr.blissgems.abilities.FireAbilities;
import dev.xoperr.blissgems.abilities.FluxAbilities;
import dev.xoperr.blissgems.abilities.LifeAbilities;
import dev.xoperr.blissgems.abilities.PuffAbilities;
import dev.xoperr.blissgems.abilities.SpeedAbilities;
import dev.xoperr.blissgems.abilities.StrengthAbilities;
import dev.xoperr.blissgems.abilities.WealthAbilities;
import dev.xoperr.blissgems.commands.BlissCommand;
import dev.xoperr.blissgems.listeners.AutoEnchantListener;
import dev.xoperr.blissgems.listeners.GemDropListener;
import dev.xoperr.blissgems.listeners.GemInteractListener;
import dev.xoperr.blissgems.listeners.KillTrackingListener;
import dev.xoperr.blissgems.listeners.PassiveListener;
import dev.xoperr.blissgems.listeners.PlayerDeathListener;
import dev.xoperr.blissgems.listeners.PlayerJoinListener;
import dev.xoperr.blissgems.listeners.RepairKitListener;
import dev.xoperr.blissgems.listeners.ReviveBeaconListener;
import dev.xoperr.blissgems.listeners.StunListener;
import dev.xoperr.blissgems.listeners.UpgraderListener;
import dev.xoperr.blissgems.managers.AbilityManager;
import dev.xoperr.blissgems.managers.EnhancedGuiManager;
import dev.xoperr.blissgems.managers.ClickActivationManager;
import dev.xoperr.blissgems.managers.CooldownDisplayManager;
import dev.xoperr.blissgems.managers.CriticalHitManager;
import dev.xoperr.blissgems.managers.EnergyManager;
import dev.xoperr.blissgems.managers.FlowStateManager;
import dev.xoperr.blissgems.managers.GemManager;
import dev.xoperr.blissgems.managers.GemRitualManager;
import dev.xoperr.blissgems.managers.AchievementManager;
import dev.xoperr.blissgems.managers.StatsManager;
import dev.xoperr.blissgems.managers.PassiveManager;
import dev.xoperr.blissgems.managers.PluginMessagingManager;
import dev.xoperr.blissgems.managers.RecipeManager;
import dev.xoperr.blissgems.managers.RepairKitManager;
import dev.xoperr.blissgems.managers.ReviveBeaconManager;
import dev.xoperr.blissgems.managers.ItemOwnershipManager;
import dev.xoperr.blissgems.managers.SoulManager;
import dev.xoperr.blissgems.managers.TrustedPlayersManager;
import dev.xoperr.blissgems.utils.ConfigManager;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.core.managers.ProtectionManager;
import dev.xoperr.blissgems.core.managers.ParticleManager;
import dev.xoperr.blissgems.core.managers.TextManager;
import dev.xoperr.blissgems.core.managers.AutoEnchantManager;
import dev.xoperr.blissgems.core.api.protection.GemProtectionAPI;
import dev.xoperr.blissgems.core.api.particle.ParticleAPI;
import dev.xoperr.blissgems.core.api.text.InventoryTextAPI;
import dev.xoperr.blissgems.core.api.enchant.AutoEnchantAPI;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class BlissGems
extends JavaPlugin {
    private ConfigManager configManager;
    private EnergyManager energyManager;
    private GemManager gemManager;
    private AbilityManager abilityManager;
    private PassiveManager passiveManager;
    private ClickActivationManager clickActivationManager;
    private TrustedPlayersManager trustedPlayersManager;
    private CooldownDisplayManager cooldownDisplayManager;
    private EnhancedGuiManager enhancedGuiManager;
    private StatsManager statsManager;
    private RecipeManager recipeManager;
    private RepairKitManager repairKitManager;
    private ReviveBeaconManager reviveBeaconManager;
    private SoulManager soulManager;
    private FlowStateManager flowStateManager;
    private CriticalHitManager criticalHitManager;
    private ItemOwnershipManager itemOwnershipManager;
    private PluginMessagingManager pluginMessagingManager;
    private AstraAbilities astraAbilities;
    private FireAbilities fireAbilities;
    private FluxAbilities fluxAbilities;
    private LifeAbilities lifeAbilities;
    private PuffAbilities puffAbilities;
    private SpeedAbilities speedAbilities;
    private StrengthAbilities strengthAbilities;
    private WealthAbilities wealthAbilities;
    private ProtectionManager protectionManager;
    private ParticleManager particleManager;
    private TextManager textManager;
    private AutoEnchantManager autoEnchantManager;
    private AchievementManager achievementManager;
    private GemRitualManager gemRitualManager;

    public void onEnable() {
        this.saveDefaultConfig();

        try {
            CustomItemManager.initialize(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: CustomItemManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }

        // Initialize internal XoperrCore managers
        try {
            this.protectionManager = new ProtectionManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: ProtectionManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.particleManager = new ParticleManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: ParticleManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.textManager = new TextManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: TextManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.autoEnchantManager = new AutoEnchantManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: AutoEnchantManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }

        // Initialize XoperrCore APIs
        try {
            GemProtectionAPI.initialize(protectionManager);
            ParticleAPI.initialize(particleManager);
            InventoryTextAPI.initialize(textManager);
            AutoEnchantAPI.initialize(autoEnchantManager);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: Core APIs ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }

        try {
            this.configManager = new ConfigManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: ConfigManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.energyManager = new EnergyManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: EnergyManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.gemManager = new GemManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: GemManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.abilityManager = new AbilityManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: AbilityManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.passiveManager = new PassiveManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: PassiveManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.clickActivationManager = new ClickActivationManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: ClickActivationManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.trustedPlayersManager = new TrustedPlayersManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: TrustedPlayersManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.repairKitManager = new RepairKitManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: RepairKitManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.reviveBeaconManager = new ReviveBeaconManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: ReviveBeaconManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.soulManager = new SoulManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: SoulManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.flowStateManager = new FlowStateManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: FlowStateManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.criticalHitManager = new CriticalHitManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: CriticalHitManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.itemOwnershipManager = new ItemOwnershipManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: ItemOwnershipManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.pluginMessagingManager = new PluginMessagingManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: PluginMessagingManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.astraAbilities = new AstraAbilities(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: AstraAbilities ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.fireAbilities = new FireAbilities(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: FireAbilities ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.fluxAbilities = new FluxAbilities(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: FluxAbilities ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.lifeAbilities = new LifeAbilities(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: LifeAbilities ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.puffAbilities = new PuffAbilities(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: PuffAbilities ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.speedAbilities = new SpeedAbilities(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: SpeedAbilities ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.strengthAbilities = new StrengthAbilities(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: StrengthAbilities ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.wealthAbilities = new WealthAbilities(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: WealthAbilities ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.cooldownDisplayManager = new CooldownDisplayManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: CooldownDisplayManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.statsManager = new StatsManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: StatsManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.achievementManager = new AchievementManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: AchievementManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.enhancedGuiManager = new EnhancedGuiManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: EnhancedGuiManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            this.recipeManager = new RecipeManager(this);
            this.gemRitualManager = new GemRitualManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: RecipeManager/GemRitualManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        try {
            if (this.recipeManager != null) {
                this.recipeManager.registerRecipes();
            }
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: Recipe Registration ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        // Auto-detect SMP start based on existing playerdata
        try {
            this.checkAutoStartSmp();
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: SMP Auto-Start Check ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }

        try {
            this.registerListeners();
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: Listener Registration ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }

        // Command registration — MUST always run so /bliss doesn't show bare usage message
        try {
            this.registerCommands();
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: Command Registration ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }

        this.getLogger().info("BlissGems has been enabled!");
        this.getLogger().info("Version: " + this.getDescription().getVersion());
        this.getLogger().info("Using custom item system with vanilla Minecraft items");
    }

    public void onDisable() {
        // Cleanup XoperrCore managers
        if (this.particleManager != null) {
            this.particleManager.cleanup();
        }
        if (this.textManager != null) {
            this.textManager.cleanup();
        }
        if (this.autoEnchantManager != null) {
            this.autoEnchantManager.cleanup();
        }

        if (this.cooldownDisplayManager != null) {
            this.cooldownDisplayManager.stop();
        }
        if (this.repairKitManager != null) {
            this.repairKitManager.cleanup();
        }
        if (this.reviveBeaconManager != null) {
            this.reviveBeaconManager.cleanup();
        }
        if (this.pluginMessagingManager != null) {
            this.pluginMessagingManager.shutdown();
        }
        if (this.recipeManager != null) {
            this.recipeManager.unregisterRecipes();
        }

        // Clean up all gem ability tasks
        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (this.astraAbilities != null) {
                this.astraAbilities.cleanup(player);
            }
            if (this.fireAbilities != null) {
                this.fireAbilities.cleanup(player);
            }
            if (this.fluxAbilities != null) {
                this.fluxAbilities.cleanup(player);
            }
            if (this.strengthAbilities != null) {
                this.strengthAbilities.cleanup(player);
            }
        }

        this.energyManager.saveAll();
        if (this.abilityManager != null) {
            this.abilityManager.saveAllCooldowns();
        }
        if (this.achievementManager != null) {
            this.achievementManager.saveAll();
        }
        this.getLogger().info("BlissGems has been disabled!");
    }

    private void checkAutoStartSmp() {
        if (this.configManager == null || this.configManager.isSmpStarted()) {
            return;
        }

        java.io.File playerDataFolder = new java.io.File(getDataFolder(), "playerdata");
        if (!playerDataFolder.exists() || !playerDataFolder.isDirectory()) {
            return;
        }

        int threshold = this.configManager.getSmpAutoStartThreshold();
        int count = 0;

        java.io.File[] files = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (java.io.File file : files) {
            org.bukkit.configuration.file.FileConfiguration data =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            if (data.getBoolean("received-first-gem", false)) {
                count++;
                if (count >= threshold) {
                    this.configManager.setSmpStarted(true);
                    this.getLogger().info("SMP auto-started! Found " + count + " players with gems (threshold: " + threshold + ").");
                    return;
                }
            }
        }
    }

    private void registerListeners() {
        // Register XoperrCore listeners
        // ItemDropListener removed - gems can now be dropped
        // InventoryInteractListener removed - gems can now be moved to containers

        // Register BlissGems listeners
        this.getServer().getPluginManager().registerEvents((Listener)new PlayerDeathListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new GemDropListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new GemInteractListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new UpgraderListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new PassiveListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new PlayerJoinListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new AutoEnchantListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new StunListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new RepairKitListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new ReviveBeaconListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new KillTrackingListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)this.enhancedGuiManager, (Plugin)this);
        if (this.itemOwnershipManager != null) {
            this.getServer().getPluginManager().registerEvents((Listener)this.itemOwnershipManager, (Plugin)this);
        }
    }

    private void registerCommands() {
        BlissCommand blissCommand = new BlissCommand(this);
        this.getCommand("bliss").setExecutor((CommandExecutor)blissCommand);
        this.getCommand("bliss").setTabCompleter((TabCompleter)blissCommand);
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public EnergyManager getEnergyManager() {
        return this.energyManager;
    }

    public GemManager getGemManager() {
        return this.gemManager;
    }

    public AbilityManager getAbilityManager() {
        return this.abilityManager;
    }

    public PassiveManager getPassiveManager() {
        return this.passiveManager;
    }

    public RecipeManager getRecipeManager() {
        return this.recipeManager;
    }

    public GemRitualManager getGemRitualManager() {
        return this.gemRitualManager;
    }

    public AstraAbilities getAstraAbilities() {
        return this.astraAbilities;
    }

    public FireAbilities getFireAbilities() {
        return this.fireAbilities;
    }

    public FluxAbilities getFluxAbilities() {
        return this.fluxAbilities;
    }

    public LifeAbilities getLifeAbilities() {
        return this.lifeAbilities;
    }

    public PuffAbilities getPuffAbilities() {
        return this.puffAbilities;
    }

    public SpeedAbilities getSpeedAbilities() {
        return this.speedAbilities;
    }

    public StrengthAbilities getStrengthAbilities() {
        return this.strengthAbilities;
    }

    public WealthAbilities getWealthAbilities() {
        return this.wealthAbilities;
    }

    public RepairKitManager getRepairKitManager() {
        return this.repairKitManager;
    }

    public ReviveBeaconManager getReviveBeaconManager() {
        return this.reviveBeaconManager;
    }

    public SoulManager getSoulManager() {
        return this.soulManager;
    }

    public FlowStateManager getFlowStateManager() {
        return this.flowStateManager;
    }

    public CriticalHitManager getCriticalHitManager() {
        return this.criticalHitManager;
    }

    public ItemOwnershipManager getItemOwnershipManager() {
        return this.itemOwnershipManager;
    }

    public ClickActivationManager getClickActivationManager() {
        return this.clickActivationManager;
    }

    public TrustedPlayersManager getTrustedPlayersManager() {
        return this.trustedPlayersManager;
    }

    public EnhancedGuiManager getEnhancedGuiManager() {
        return this.enhancedGuiManager;
    }

    public StatsManager getStatsManager() {
        return this.statsManager;
    }

    public AchievementManager getAchievementManager() {
        return this.achievementManager;
    }

    public PluginMessagingManager getPluginMessagingManager() {
        return this.pluginMessagingManager;
    }

    // XoperrCore getters
    public ProtectionManager getProtectionManager() {
        return this.protectionManager;
    }

    public ParticleManager getParticleManager() {
        return this.particleManager;
    }

    public TextManager getTextManager() {
        return this.textManager;
    }

    public AutoEnchantManager getAutoEnchantManager() {
        return this.autoEnchantManager;
    }
}

