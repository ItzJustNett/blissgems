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
import dev.xoperr.blissgems.api.BlissGemsAPI;
import dev.xoperr.blissgems.api.CooldownEntry;
import dev.xoperr.blissgems.api.GemDefinition;
import dev.xoperr.blissgems.api.GemRegistry;
import dev.xoperr.blissgems.commands.BlissCommand;
import dev.xoperr.blissgems.commands.FixHeartsCommand;
import dev.xoperr.blissgems.commands.FixedHeartsCommand;
import dev.xoperr.blissgems.commands.FixGemsCommand;
import dev.xoperr.blissgems.listeners.AutoEnchantListener;
import dev.xoperr.blissgems.listeners.ComprehensiveGemProtectionListener;
import dev.xoperr.blissgems.listeners.GemInteractListener;
import dev.xoperr.blissgems.listeners.KillTrackingListener;
import dev.xoperr.blissgems.listeners.PassiveListener;
import dev.xoperr.blissgems.listeners.PlayerDeathListener;
import dev.xoperr.blissgems.listeners.PlayerJoinListener;
import dev.xoperr.blissgems.listeners.RepairKitListener;
import dev.xoperr.blissgems.listeners.ReviveBeaconListener;
import dev.xoperr.blissgems.listeners.StunListener;
import dev.xoperr.blissgems.listeners.SwapHandAbilityListener;
import dev.xoperr.blissgems.listeners.TeleportListener;
import dev.xoperr.blissgems.listeners.UpgraderListener;
import dev.xoperr.blissgems.listeners.VillagerTradeListener;
import dev.xoperr.blissgems.managers.AbilityBindingManager;
import dev.xoperr.blissgems.managers.AbilityManager;
import dev.xoperr.blissgems.managers.GemRegistryImpl;
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
import dev.xoperr.blissgems.managers.SoulManager;
import dev.xoperr.blissgems.managers.TrustedPlayersManager;
import dev.xoperr.blissgems.utils.ConfigManager;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.core.managers.ProtectionManager;
import dev.xoperr.blissgems.core.managers.ParticleManager;
import dev.xoperr.blissgems.core.managers.TextManager;
import dev.xoperr.blissgems.core.managers.AutoEnchantManager;
import dev.xoperr.blissgems.core.managers.RegionManager;
import dev.xoperr.blissgems.core.api.protection.GemProtectionAPI;
import dev.xoperr.blissgems.core.api.particle.ParticleAPI;
import dev.xoperr.blissgems.core.api.text.InventoryTextAPI;
import dev.xoperr.blissgems.core.api.enchant.AutoEnchantAPI;
import dev.xoperr.blissgems.core.api.region.RegionAPI;
import dev.faststats.bukkit.BukkitMetrics;
import dev.faststats.core.Metrics;
import dev.faststats.core.data.Metric;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public class BlissGems
extends JavaPlugin
implements BlissGemsAPI {
    private ConfigManager configManager;
    private BlissCommand blissCommand;
    private GemRegistryImpl gemRegistry;
    private EnergyManager energyManager;
    private GemManager gemManager;
    private AbilityManager abilityManager;
    private AbilityBindingManager abilityBindingManager;
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
    private PluginMessagingManager pluginMessagingManager;
    private AstraAbilities astraAbilities;
    private FireAbilities fireAbilities;
    private FluxAbilities fluxAbilities;
    private LifeAbilities lifeAbilities;
    private PuffAbilities puffAbilities;
    private SpeedAbilities speedAbilities;
    private StrengthAbilities strengthAbilities;
    private WealthAbilities wealthAbilities;
    private dev.xoperr.blissgems.listeners.ItemOwnershipListener itemOwnershipListener;
    private ProtectionManager protectionManager;
    private ParticleManager particleManager;
    private TextManager textManager;
    private AutoEnchantManager autoEnchantManager;
    private RegionManager regionManager;
    private AchievementManager achievementManager;
    private GemRitualManager gemRitualManager;
    private Metrics metrics;

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
        try {
            this.regionManager = new RegionManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: RegionManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }

        // Initialize XoperrCore APIs
        try {
            GemProtectionAPI.initialize(protectionManager);
            ParticleAPI.initialize(particleManager);
            InventoryTextAPI.initialize(textManager);
            AutoEnchantAPI.initialize(autoEnchantManager);
            RegionAPI.initialize(regionManager);
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
            this.abilityBindingManager = new AbilityBindingManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: AbilityBindingManager ===");
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
        // Initialize Gem Registry and register built-in gems + API
        try {
            this.gemRegistry = new GemRegistryImpl(this);
            this.registerBuiltInGems();
            this.getServer().getServicesManager().register(
                BlissGemsAPI.class, this, this, ServicePriority.Normal);
            this.getLogger().info("BlissGems Addon API registered via ServicesManager");
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: GemRegistry/API ===");
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

        // Initialize FastStats metrics (if enabled in config)
        if (this.getConfig().getBoolean("send-anonymous-metrics", true)) {
            try {
                this.metrics = BukkitMetrics.factory()
                    .token("33b82f6ed2f61ee4be22345da22fbf24")
                    .addMetric(Metric.number("active_gem_players", () -> {
                        int count = 0;
                        for (org.bukkit.entity.Player p : getServer().getOnlinePlayers()) {
                            if (gemManager != null && gemManager.hasGemInOffhand(p)) count++;
                        }
                        return count;
                    }))
                    .create(this);
                this.metrics.ready();
            } catch (Exception e) {
                this.getLogger().warning("FastStats metrics failed to initialize: " + e.getMessage());
            }
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
        if (this.itemOwnershipListener != null) {
            this.itemOwnershipListener.stop();
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
            if (this.lifeAbilities != null) {
                this.lifeAbilities.cleanup(player);
            }
        }

        if (this.metrics != null) {
            this.metrics.shutdown();
        }
        this.energyManager.saveAll();
        if (this.wealthAbilities != null) {
            this.wealthAbilities.saveAllPockets();
        }
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
        this.getServer().getPluginManager().registerEvents((Listener)new ComprehensiveGemProtectionListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new GemInteractListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new UpgraderListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new PassiveListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new PlayerJoinListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new AutoEnchantListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new StunListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new RepairKitListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new ReviveBeaconListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new KillTrackingListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new TeleportListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new VillagerTradeListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new SwapHandAbilityListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new dev.xoperr.blissgems.listeners.RitualCleanupListener(this), (Plugin)this);
        // Sweep any ritual display entities orphaned before this start-up (loaded worlds only;
        // unloaded-chunk leftovers are caught by RitualCleanupListener as their chunks load).
        int sweptRitualGems = dev.xoperr.blissgems.managers.GemRitualManager.sweepAll(this.getServer());
        if (sweptRitualGems > 0) {
            this.getLogger().info("Swept " + sweptRitualGems + " orphaned ritual gem display(s) on enable.");
        }
        this.itemOwnershipListener = new dev.xoperr.blissgems.listeners.ItemOwnershipListener(this);
        this.getServer().getPluginManager().registerEvents((Listener)this.itemOwnershipListener, (Plugin)this);
        this.itemOwnershipListener.start();
        this.getServer().getPluginManager().registerEvents((Listener)this.enhancedGuiManager, (Plugin)this);
    }

    private void registerCommands() {
        this.blissCommand = new BlissCommand(this);
        this.getCommand("bliss").setExecutor((CommandExecutor)this.blissCommand);
        this.getCommand("bliss").setTabCompleter((TabCompleter)this.blissCommand);

        FixHeartsCommand fixHearts = new FixHeartsCommand(this);
        if (this.getCommand("fixhearts") != null) {
            this.getCommand("fixhearts").setExecutor((CommandExecutor)fixHearts);
            this.getCommand("fixhearts").setTabCompleter((TabCompleter)fixHearts);
        }

        FixedHeartsCommand fixedHearts = new FixedHeartsCommand(this);
        if (this.getCommand("fixedhearts") != null) {
            this.getCommand("fixedhearts").setExecutor((CommandExecutor)fixedHearts);
            this.getCommand("fixedhearts").setTabCompleter((TabCompleter)fixedHearts);
        }

        FixGemsCommand fixGems = new FixGemsCommand(this);
        if (this.getCommand("fixgems") != null) {
            this.getCommand("fixgems").setExecutor((CommandExecutor)fixGems);
            this.getCommand("fixgems").setTabCompleter((TabCompleter)fixGems);
        }
    }

    public BlissCommand getBlissCommand() {
        return this.blissCommand;
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

    public AbilityBindingManager getAbilityBindingManager() {
        return this.abilityBindingManager;
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

    public RegionManager getRegionManager() {
        return this.regionManager;
    }

    // ========================================================================
    // Gem Registry & Addon API
    // ========================================================================

    @Override
    public GemRegistryImpl getGemRegistry() {
        return this.gemRegistry;
    }

    @Override
    public boolean playerHasGem(org.bukkit.entity.Player player, String gemId) {
        GemManager.ActiveGem gem = this.gemManager.getActiveGem(player);
        if (gem == null) return false;
        return gemId.equals(gem.getGemId());
    }

    /**
     * Registers all 8 built-in gems, their ability handlers, passive handlers,
     * and cooldown display entries with the gem registry.
     */
    private void registerBuiltInGems() {
        // Register gem definitions
        for (dev.xoperr.blissgems.utils.GemType type : dev.xoperr.blissgems.utils.GemType.values()) {
            GemDefinition def = new GemDefinition.Builder(type.getId())
                .displayName(type.getDisplayName())
                .description(type.getDescription())
                .color(type.getColor())
                .plugin("BlissGems")
                .maxTier(2)
                .build();
            this.gemRegistry.registerGem(def);
        }

        // Register ability handlers (each ability class now implements GemAbilityHandler)
        if (this.astraAbilities != null) this.gemRegistry.registerAbilities("astra", this.astraAbilities);
        if (this.fireAbilities != null) this.gemRegistry.registerAbilities("fire", this.fireAbilities);
        if (this.fluxAbilities != null) this.gemRegistry.registerAbilities("flux", this.fluxAbilities);
        if (this.lifeAbilities != null) this.gemRegistry.registerAbilities("life", this.lifeAbilities);
        if (this.puffAbilities != null) this.gemRegistry.registerAbilities("puff", this.puffAbilities);
        if (this.speedAbilities != null) this.gemRegistry.registerAbilities("speed", this.speedAbilities);
        if (this.strengthAbilities != null) this.gemRegistry.registerAbilities("strength", this.strengthAbilities);
        if (this.wealthAbilities != null) this.gemRegistry.registerAbilities("wealth", this.wealthAbilities);

        // Register passive handlers
        if (this.passiveManager != null) {
            this.passiveManager.registerBuiltInHandlers(this.gemRegistry);
        }

        // Register cooldown display entries
        this.gemRegistry.registerCooldowns("astra", List.of(
            new CooldownEntry("astra-daggers", "Daggers"),
            new CooldownEntry("astra-projection", "Projection")
        ));
        this.gemRegistry.registerCooldowns("fire", List.of(
            new CooldownEntry("fire-fireball", "Fireball"),
            new CooldownEntry("fire-campfire", "Campfire"),
            new CooldownEntry("fire-crisp", "Crisp"),
            new CooldownEntry("fire-meteor-shower", "Meteor")
        ));
        this.gemRegistry.registerCooldowns("flux", List.of(
            new CooldownEntry("flux-beam", "Beam"),
            new CooldownEntry("flux-ground", "Ground"),
            new CooldownEntry("flux-flashbang", "Flash"),
            new CooldownEntry("flux-kinetic-burst", "Kinetic")
        ));
        this.gemRegistry.registerCooldowns("life", List.of(
            new CooldownEntry("life-drainer", "Drainer"),
            new CooldownEntry("life-circle-of-life", "Circle"),
            new CooldownEntry("life-vitality-vortex", "Vortex"),
            new CooldownEntry("life-heart-lock", "Lock")
        ));
        this.gemRegistry.registerCooldowns("puff", List.of(
            new CooldownEntry("puff-dash", "Dash"),
            new CooldownEntry("puff-breezy-bash", "Bash"),
            new CooldownEntry("puff-group-bash", "Group")
        ));
        this.gemRegistry.registerCooldowns("speed", List.of(
            new CooldownEntry("speed-blur", "Blur"),
            new CooldownEntry("speed-storm", "Storm"),
            new CooldownEntry("speed-terminal", "Terminal")
        ));
        this.gemRegistry.registerCooldowns("strength", List.of(
            new CooldownEntry("strength-nullify", "Nullify"),
            new CooldownEntry("strength-frailer", "Frailer"),
            new CooldownEntry("strength-shadow-stalker", "Stalker")
        ));
        this.gemRegistry.registerCooldowns("wealth", List.of(
            new CooldownEntry("wealth-unfortunate", "Unfortunate"),
            new CooldownEntry("wealth-rich-rush", "Rush"),
            new CooldownEntry("wealth-item-lock", "Lock"),
            new CooldownEntry("wealth-amplification", "Amplify")
        ));
    }
}

