package dev.xoperr.blissgems;

import dev.xoperr.blissgems.abilities.AstraAbilities;
import dev.xoperr.blissgems.abilities.FireAbilities;
import dev.xoperr.blissgems.abilities.FluxAbilities;
import dev.xoperr.blissgems.abilities.GoldAbilities;
import dev.xoperr.blissgems.abilities.LifeAbilities;
import dev.xoperr.blissgems.abilities.PuffAbilities;
import dev.xoperr.blissgems.abilities.SpeedAbilities;
import dev.xoperr.blissgems.abilities.StrengthAbilities;
import dev.xoperr.blissgems.abilities.WealthAbilities;
import dev.xoperr.blissgems.api.BlissGemsAPI;
import dev.xoperr.blissgems.api.CooldownEntry;
import dev.xoperr.blissgems.api.GemDefinition;
import dev.xoperr.blissgems.commands.BlissCommand;
import dev.xoperr.blissgems.commands.FixHeartsCommand;
import dev.xoperr.blissgems.commands.FixedHeartsCommand;
import dev.xoperr.blissgems.commands.FixGemsCommand;
import dev.xoperr.blissgems.listeners.AutoEnchantListener;
import dev.xoperr.blissgems.listeners.BrokenGemDamageListener;
import dev.xoperr.blissgems.listeners.GaleCloudListener;
import dev.xoperr.blissgems.listeners.RestorationBookListener;
import dev.xoperr.blissgems.listeners.PrismaticEdgeListener;
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
import dev.xoperr.blissgems.managers.GemLockManager;
import dev.xoperr.blissgems.managers.GoldGemManager;
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
    private GoldGemManager goldGemManager;
    private GoldAbilities goldAbilities;
    private FlowStateManager flowStateManager;
    private GemLockManager gemLockManager;
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

        initStep("CustomItemManager", () -> CustomItemManager.initialize(this));

        initStep("ProtectionManager", () -> this.protectionManager = new ProtectionManager(this));
        initStep("ParticleManager", () -> this.particleManager = new ParticleManager(this));
        initStep("TextManager", () -> this.textManager = new TextManager(this));
        initStep("AutoEnchantManager", () -> this.autoEnchantManager = new AutoEnchantManager(this));
        initStep("RegionManager", () -> this.regionManager = new RegionManager(this));

        initStep("Core APIs", () -> {
            GemProtectionAPI.initialize(protectionManager);
            ParticleAPI.initialize(particleManager);
            InventoryTextAPI.initialize(textManager);
            AutoEnchantAPI.initialize(autoEnchantManager);
            RegionAPI.initialize(regionManager);
        });

        initStep("ConfigManager", () -> this.configManager = new ConfigManager(this));
        initStep("EnergyManager", () -> this.energyManager = new EnergyManager(this));
        initStep("GemManager", () -> this.gemManager = new GemManager(this));
        initStep("AbilityManager", () -> this.abilityManager = new AbilityManager(this));
        initStep("AbilityBindingManager", () -> this.abilityBindingManager = new AbilityBindingManager(this));
        initStep("PassiveManager", () -> this.passiveManager = new PassiveManager(this));
        initStep("ClickActivationManager", () -> this.clickActivationManager = new ClickActivationManager(this));
        initStep("TrustedPlayersManager", () -> this.trustedPlayersManager = new TrustedPlayersManager(this));
        initStep("RepairKitManager", () -> this.repairKitManager = new RepairKitManager(this));
        initStep("ReviveBeaconManager", () -> this.reviveBeaconManager = new ReviveBeaconManager(this));
        initStep("SoulManager", () -> this.soulManager = new SoulManager(this));
        try {
            this.flowStateManager = new FlowStateManager(this);
            this.gemLockManager = new GemLockManager(this);
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: FlowStateManager ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
        initStep("CriticalHitManager", () -> this.criticalHitManager = new CriticalHitManager(this));
        initStep("PluginMessagingManager", () -> this.pluginMessagingManager = new PluginMessagingManager(this));
        initStep("AstraAbilities", () -> this.astraAbilities = new AstraAbilities(this));
        initStep("FireAbilities", () -> this.fireAbilities = new FireAbilities(this));
        initStep("FluxAbilities", () -> this.fluxAbilities = new FluxAbilities(this));
        initStep("LifeAbilities", () -> this.lifeAbilities = new LifeAbilities(this));
        initStep("PuffAbilities", () -> this.puffAbilities = new PuffAbilities(this));
        initStep("SpeedAbilities", () -> this.speedAbilities = new SpeedAbilities(this));
        initStep("StrengthAbilities", () -> this.strengthAbilities = new StrengthAbilities(this));
        initStep("WealthAbilities", () -> this.wealthAbilities = new WealthAbilities(this));
        initStep("GoldGem", () -> {
            this.goldGemManager = new GoldGemManager(this);
            this.goldAbilities = new GoldAbilities(this);
        });
        initStep("GemRegistry/API", () -> {
            this.gemRegistry = new GemRegistryImpl(this);
            this.registerBuiltInGems();
            this.getServer().getServicesManager().register(
                BlissGemsAPI.class, this, this, ServicePriority.Normal);
            this.getLogger().info("BlissGems Addon API registered via ServicesManager");
        });

        initStep("CooldownDisplayManager", () -> this.cooldownDisplayManager = new CooldownDisplayManager(this));
        initStep("StatsManager", () -> this.statsManager = new StatsManager(this));
        initStep("AchievementManager", () -> this.achievementManager = new AchievementManager(this));
        initStep("EnhancedGuiManager", () -> this.enhancedGuiManager = new EnhancedGuiManager(this));
        initStep("RecipeManager/GemRitualManager", () -> {
            this.recipeManager = new RecipeManager(this);
            this.gemRitualManager = new GemRitualManager(this);
        });
        initStep("Recipe Registration", () -> {
            if (this.recipeManager != null) {
                this.recipeManager.registerRecipes();
            }
        });
        initStep("SMP Auto-Start Check", this::checkAutoStartSmp);

        initStep("Listener Registration", this::registerListeners);

        // Command registration — MUST always run so /bliss doesn't show bare usage message
        initStep("Command Registration", this::registerCommands);

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

    /**
     * Runs a single start-up step, logging and swallowing any failure so one broken
     * subsystem cannot abort the rest of onEnable().
     */
    private void initStep(String name, Runnable step) {
        try {
            step.run();
        } catch (Exception e) {
            this.getLogger().severe("=== BLISSGEMS FAILED TO INITIALIZE: " + name + " ===");
            this.getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
    }

    public void onDisable() {
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

        // Registration order below is load-bearing — keep it as-is.
        registerEvents(new PlayerDeathListener(this));
        this.getServer().getPluginManager().registerEvents((Listener)new ComprehensiveGemProtectionListener(this), (Plugin)this);
        registerEvents(
            new GemInteractListener(this),
            new UpgraderListener(this),
            new PassiveListener(this),
            new PlayerJoinListener(this),
            new AutoEnchantListener(this),
            new StunListener(this),
            new RepairKitListener(this),
            new ReviveBeaconListener(this),
            new KillTrackingListener(this),
            new TeleportListener(this),
            new VillagerTradeListener(this),
            new SwapHandAbilityListener(this),
            new dev.xoperr.blissgems.listeners.RitualCleanupListener(this),
            new BrokenGemDamageListener(this),
            new GaleCloudListener(this),
            new RestorationBookListener(this),
            new PrismaticEdgeListener(this));
        if (this.goldAbilities != null) {
            registerEvents(this.goldAbilities);
        }
        // Anti-dupe: break the "drop-and-swap" ghost dupe (drop + same-tick hotbar swap).
        dev.xoperr.blissgems.listeners.DropSwapGuard dropSwapGuard = new dev.xoperr.blissgems.listeners.DropSwapGuard(this);
        this.getServer().getPluginManager().registerEvents((Listener)dropSwapGuard, (Plugin)this);
        dropSwapGuard.start();
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

    /** Registers the given listeners with this plugin, in argument order. */
    private void registerEvents(Listener... listeners) {
        for (Listener listener : listeners) {
            this.getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    private void registerCommands() {
        this.blissCommand = new BlissCommand(this);
        this.getCommand("bliss").setExecutor((CommandExecutor)this.blissCommand);
        this.getCommand("bliss").setTabCompleter((TabCompleter)this.blissCommand);

        bindCommand("fixhearts", new FixHeartsCommand(this));
        bindCommand("fixedhearts", new FixedHeartsCommand(this));
        bindCommand("fixgems", new FixGemsCommand(this));
    }

    /** Wires an executor + tab completer onto a command, skipping it if plugin.yml omits it. */
    private <T extends CommandExecutor & TabCompleter> void bindCommand(String name, T handler) {
        org.bukkit.command.PluginCommand command = this.getCommand(name);
        if (command == null) {
            return;
        }
        command.setExecutor(handler);
        command.setTabCompleter(handler);
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

    public GoldGemManager getGoldGemManager() {
        return this.goldGemManager;
    }

    public GoldAbilities getGoldAbilities() {
        return this.goldAbilities;
    }

    public SoulManager getSoulManager() {
        return this.soulManager;
    }

    public GemLockManager getGemLockManager() {
        return this.gemLockManager;
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

    @Override
    public GemRegistryImpl getGemRegistry() {
        return this.gemRegistry;
    }

    @Override
    public boolean playerHasGem(org.bukkit.entity.Player player, String gemId) {
        GemManager.ActiveGem gem = this.gemManager.getActiveGem(player);
        return gem != null && gemId.equals(gem.getGemId());
    }

    /**
     * Registers all 8 built-in gems, their ability handlers, passive handlers,
     * and cooldown display entries with the gem registry.
     */
    private void registerBuiltInGems() {
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

        // The Gold Gem is registered straight into the registry rather than added to the
        // GemType enum: everything that iterates GemType.values() (recipes, villager trades,
        // upgraders, GUIs) would otherwise treat it as an ordinary craftable gem.
        this.gemRegistry.registerGem(new GemDefinition.Builder("gold")
            .displayName("Gold")
            .description("Watch the lines of reality fray as eight souls become one")
            .color("§6")
            .plugin("BlissGems")
            .maxTier(1)
            .material(org.bukkit.Material.PRISMARINE_CRYSTALS)
            .t1CustomModelData(1009)
            .build());

        // Register ability handlers (each ability class now implements GemAbilityHandler)
        if (this.astraAbilities != null) this.gemRegistry.registerAbilities("astra", this.astraAbilities);
        if (this.fireAbilities != null) this.gemRegistry.registerAbilities("fire", this.fireAbilities);
        if (this.fluxAbilities != null) this.gemRegistry.registerAbilities("flux", this.fluxAbilities);
        if (this.lifeAbilities != null) this.gemRegistry.registerAbilities("life", this.lifeAbilities);
        if (this.puffAbilities != null) this.gemRegistry.registerAbilities("puff", this.puffAbilities);
        if (this.speedAbilities != null) this.gemRegistry.registerAbilities("speed", this.speedAbilities);
        if (this.strengthAbilities != null) this.gemRegistry.registerAbilities("strength", this.strengthAbilities);
        if (this.wealthAbilities != null) this.gemRegistry.registerAbilities("wealth", this.wealthAbilities);
        if (this.goldAbilities != null) this.gemRegistry.registerAbilities("gold", this.goldAbilities);

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
        this.gemRegistry.registerCooldowns("gold", List.of(
            new CooldownEntry("gold-beam", "Beam")
        ));
    }
}

