package org.hyn.dropitemcontrol.managers;

import org.hyn.dropitemcontrol.Dropitemcontrol;

public class PluginStateManager {
   private final Dropitemcontrol plugin;
   private boolean enabled;

   public PluginStateManager(Dropitemcontrol plugin) {
      this.plugin = plugin;
      this.enabled = true;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void enablePlugin() {
      this.enabled = true;
      this.plugin.getLogger().info("DropItemControl đã được BẬT!");
   }

   public void disablePlugin() {
      this.enabled = false;
      this.plugin.getLogger().info("DropItemControl đã được TẮT!");
   }

   public boolean togglePlugin() {
      this.enabled = !this.enabled;
      if (this.enabled) {
         this.plugin.getLogger().info("DropItemControl đã được BẬT!");
      } else {
         this.plugin.getLogger().info("DropItemControl đã được TẮT!");
      }

      return this.enabled;
   }
}
