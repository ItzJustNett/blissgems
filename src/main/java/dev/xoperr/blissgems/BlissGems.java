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
import dev.xoperr.blissgems.core.api.protection.GemProtectionAPI;
import dev.xoperr.blissgems.core.api.particle.ParticleAPI;
import dev.xoperr.blissgems.core.api.text.InventoryTextAPI;
import dev.xoperr.blissgems.core.api.enchant.AutoEnchantAPI;
import dev.xoperr.blissgems.core.listeners.ItemDropListener;
import dev.xoperr.blissgems.core.listeners.InventoryInteractListener;
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

    public void onEnable() {
        this.saveDefaultConfig();
        CustomItemManager.initialize(this);

        // Initialize internal XoperrCore managers
        this.protectionManager = new ProtectionManager(this);
        this.particleManager = new ParticleManager(this);
        this.textManager = new TextManager(this);
        this.autoEnchantManager = new AutoEnchantManager(this);

        // Initialize XoperrCore APIs
        GemProtectionAPI.initialize(protectionManager);
        ParticleAPI.initialize(particleManager);
        InventoryTextAPI.initialize(textManager);
        AutoEnchantAPI.initialize(autoEnchantManager);

        this.configManager = new ConfigManager(this);
        this.energyManager = new EnergyManager(this);
        this.gemManager = new GemManager(this);
        this.abilityManager = new AbilityManager(this);
        this.passiveManager = new PassiveManager(this);
        this.clickActivationManager = new ClickActivationManager(this);
        this.trustedPlayersManager = new TrustedPlayersManager(this);
        this.repairKitManager = new RepairKitManager(this);
        this.reviveBeaconManager = new ReviveBeaconManager(this);
        this.soulManager = new SoulManager(this);
        this.flowStateManager = new FlowStateManager(this);
        this.criticalHitManager = new CriticalHitManager(this);
        this.pluginMessagingManager = new PluginMessagingManager(this);
        this.astraAbilities = new AstraAbilities(this);
        this.fireAbilities = new FireAbilities(this);
        this.fluxAbilities = new FluxAbilities(this);
        this.lifeAbilities = new LifeAbilities(this);
        this.puffAbilities = new PuffAbilities(this);
        this.speedAbilities = new SpeedAbilities(this);
        this.strengthAbilities = new StrengthAbilities(this);
        this.wealthAbilities = new WealthAbilities(this);
        this.cooldownDisplayManager = new CooldownDisplayManager(this);
        this.statsManager = new StatsManager(this);
        this.enhancedGuiManager = new EnhancedGuiManager(this);
        this.recipeManager = new RecipeManager(this);
        this.recipeManager.registerRecipes();
        this.registerListeners();
        this.registerCommands();
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

        // Clean up all Fire and Flux gem charging tasks
        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (this.fireAbilities != null) {
                this.fireAbilities.cleanup(player);
            }
            if (this.fluxAbilities != null) {
                this.fluxAbilities.cleanup(player);
            }
        }

        this.energyManager.saveAll();
        if (this.abilityManager != null) {
            this.abilityManager.saveAllCooldowns();
        }
        this.getLogger().info("BlissGems has been disabled!");
    }

    private void registerListeners() {
        // Register XoperrCore listeners
        this.getServer().getPluginManager().registerEvents(new ItemDropListener(protectionManager), this);
        this.getServer().getPluginManager().registerEvents(new InventoryInteractListener(protectionManager), this);

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

