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
package fun.obriy.blissgems;

import fun.obriy.blissgems.abilities.AstraAbilities;
import fun.obriy.blissgems.abilities.FireAbilities;
import fun.obriy.blissgems.abilities.FluxAbilities;
import fun.obriy.blissgems.abilities.LifeAbilities;
import fun.obriy.blissgems.abilities.PuffAbilities;
import fun.obriy.blissgems.abilities.SpeedAbilities;
import fun.obriy.blissgems.abilities.StrengthAbilities;
import fun.obriy.blissgems.abilities.WealthAbilities;
import fun.obriy.blissgems.commands.BlissCommand;
import fun.obriy.blissgems.listeners.AutoEnchantListener;
import fun.obriy.blissgems.listeners.GemInteractListener;
import fun.obriy.blissgems.listeners.PassiveListener;
import fun.obriy.blissgems.listeners.PlayerDeathListener;
import fun.obriy.blissgems.listeners.PlayerJoinListener;
import fun.obriy.blissgems.listeners.StunListener;
import fun.obriy.blissgems.listeners.UpgraderListener;
import fun.obriy.blissgems.managers.AbilityManager;
import fun.obriy.blissgems.managers.CooldownDisplayManager;
import fun.obriy.blissgems.managers.EnergyManager;
import fun.obriy.blissgems.managers.GemManager;
import fun.obriy.blissgems.managers.PassiveManager;
import fun.obriy.blissgems.utils.ConfigManager;
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
    private CooldownDisplayManager cooldownDisplayManager;
    private AstraAbilities astraAbilities;
    private FireAbilities fireAbilities;
    private FluxAbilities fluxAbilities;
    private LifeAbilities lifeAbilities;
    private PuffAbilities puffAbilities;
    private SpeedAbilities speedAbilities;
    private StrengthAbilities strengthAbilities;
    private WealthAbilities wealthAbilities;

    public void onEnable() {
        this.saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.energyManager = new EnergyManager(this);
        this.gemManager = new GemManager(this);
        this.abilityManager = new AbilityManager(this);
        this.passiveManager = new PassiveManager(this);
        this.astraAbilities = new AstraAbilities(this);
        this.fireAbilities = new FireAbilities(this);
        this.fluxAbilities = new FluxAbilities(this);
        this.lifeAbilities = new LifeAbilities(this);
        this.puffAbilities = new PuffAbilities(this);
        this.speedAbilities = new SpeedAbilities(this);
        this.strengthAbilities = new StrengthAbilities(this);
        this.wealthAbilities = new WealthAbilities(this);
        this.cooldownDisplayManager = new CooldownDisplayManager(this);
        this.registerListeners();
        this.registerCommands();
        this.getLogger().info("BlissGems has been enabled!");
        this.getLogger().info("Version: " + this.getDescription().getVersion());
        this.getLogger().info("Integrated with Oraxen for custom items");
    }

    public void onDisable() {
        if (this.cooldownDisplayManager != null) {
            this.cooldownDisplayManager.stop();
        }
        this.energyManager.saveAll();
        this.getLogger().info("BlissGems has been disabled!");
    }

    private void registerListeners() {
        this.getServer().getPluginManager().registerEvents((Listener)new PlayerDeathListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new GemInteractListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new UpgraderListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new PassiveListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new PlayerJoinListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new AutoEnchantListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new StunListener(this), (Plugin)this);
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
}

