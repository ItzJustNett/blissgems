package me.hexedhero.np;

import me.hexedhero.np.listeners.DropListener;
import me.hexedhero.np.metrics.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public class NoDrop extends JavaPlugin {
   public void onEnable() {
      new Metrics(this);
      this.saveDefaultConfig();
      this.getServer().getPluginManager().registerEvents(new DropListener(), this);
      this.getLogger().info("NoDrop v" + this.getDescription().getVersion() + " Enabled!");
   }

   public void onDisable() {
      this.getLogger().info("NoDrop v" + this.getDescription().getVersion() + " Disabled!");
   }
}
