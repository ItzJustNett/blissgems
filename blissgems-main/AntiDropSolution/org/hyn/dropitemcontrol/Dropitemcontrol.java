package org.hyn.dropitemcontrol;

import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.hyn.dropitemcontrol.commands.DICCommand;
import org.hyn.dropitemcontrol.commands.LanguageCommand;
import org.hyn.dropitemcontrol.commands.LockItemCommand;
import org.hyn.dropitemcontrol.commands.LockedItemsCommand;
import org.hyn.dropitemcontrol.commands.ToggleDICCommand;
import org.hyn.dropitemcontrol.commands.UnlockItemCommand;
import org.hyn.dropitemcontrol.data.PlayerDataManager;
import org.hyn.dropitemcontrol.listeners.InventoryClickListener;
import org.hyn.dropitemcontrol.listeners.ItemDropListener;
import org.hyn.dropitemcontrol.listeners.PlayerConnectionListener;
import org.hyn.dropitemcontrol.managers.ItemLockManager;
import org.hyn.dropitemcontrol.managers.LanguageManager;
import org.hyn.dropitemcontrol.managers.PluginStateManager;
import org.hyn.dropitemcontrol.tabcompleters.DICTabCompleter;
import org.hyn.dropitemcontrol.tabcompleters.LanguageTabCompleter;
import org.hyn.dropitemcontrol.tabcompleters.LockItemTabCompleter;
import org.hyn.dropitemcontrol.tabcompleters.LockedItemsTabCompleter;
import org.hyn.dropitemcontrol.tabcompleters.ToggleDICTabCompleter;
import org.hyn.dropitemcontrol.tabcompleters.UnlockItemTabCompleter;
import org.hyn.dropitemcontrol.utils.MessageUtils;

public final class Dropitemcontrol extends JavaPlugin {
   private ItemLockManager itemLockManager;
   private MessageUtils messageUtils;
   private PluginStateManager pluginStateManager;
   private PlayerDataManager playerDataManager;
   private LanguageManager languageManager;

   public void onEnable() {
      this.saveDefaultConfig();
      this.languageManager = new LanguageManager(this);
      this.messageUtils = new MessageUtils(this, this.languageManager);
      this.playerDataManager = new PlayerDataManager(this);
      this.pluginStateManager = new PluginStateManager(this);
      this.itemLockManager = new ItemLockManager(this, this.playerDataManager);
      this.registerCommands();
      this.getServer().getPluginManager().registerEvents(new ItemDropListener(this, this.itemLockManager, this.messageUtils, this.pluginStateManager), this);
      this.getServer().getPluginManager().registerEvents(new InventoryClickListener(this, this.itemLockManager, this.messageUtils, this.pluginStateManager), this);
      this.getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this, this.playerDataManager), this);
      Logger var10000 = this.getLogger();
      String var10001 = String.valueOf(ChatColor.GREEN);
      var10000.info(var10001 + "DropItemControl" + String.valueOf(ChatColor.RESET) + " đã được kích hoạt!");
      var10000 = this.getLogger();
      var10001 = String.valueOf(ChatColor.YELLOW);
      var10000.info(var10001 + "Discord support server: " + String.valueOf(ChatColor.AQUA) + "https://discord.gg/4PaYftZwu3");
   }

   private void registerCommands() {
      LockItemCommand lockItemCommand = new LockItemCommand(this, this.itemLockManager, this.messageUtils);
      UnlockItemCommand unlockItemCommand = new UnlockItemCommand(this, this.itemLockManager, this.messageUtils);
      LockedItemsCommand lockedItemsCommand = new LockedItemsCommand(this, this.itemLockManager, this.messageUtils);
      ToggleDICCommand toggleDICCommand = new ToggleDICCommand(this, this.pluginStateManager, this.messageUtils);
      LanguageCommand languageCommand = new LanguageCommand(this, this.languageManager, this.messageUtils);
      DICCommand dicCommand = new DICCommand(this, this.itemLockManager, this.messageUtils, this.pluginStateManager, lockItemCommand, unlockItemCommand, lockedItemsCommand, toggleDICCommand);
      this.getCommand("dic").setExecutor(dicCommand);
      this.getCommand("diclockitem").setExecutor(lockItemCommand);
      this.getCommand("dicunlockitem").setExecutor(unlockItemCommand);
      this.getCommand("diclist").setExecutor(lockedItemsCommand);
      this.getCommand("dictoggle").setExecutor(toggleDICCommand);
      this.getCommand("diclanguage").setExecutor(languageCommand);
      LockItemTabCompleter lockItemTabCompleter = new LockItemTabCompleter(this);
      UnlockItemTabCompleter unlockItemTabCompleter = new UnlockItemTabCompleter(this);
      LockedItemsTabCompleter lockedItemsTabCompleter = new LockedItemsTabCompleter();
      ToggleDICTabCompleter toggleDICTabCompleter = new ToggleDICTabCompleter();
      LanguageTabCompleter languageTabCompleter = new LanguageTabCompleter(this.languageManager);
      DICTabCompleter dicTabCompleter = new DICTabCompleter(this, lockItemTabCompleter, unlockItemTabCompleter);
      this.getCommand("dic").setTabCompleter(dicTabCompleter);
      this.getCommand("diclockitem").setTabCompleter(lockItemTabCompleter);
      this.getCommand("dicunlockitem").setTabCompleter(unlockItemTabCompleter);
      this.getCommand("diclist").setTabCompleter(lockedItemsTabCompleter);
      this.getCommand("dictoggle").setTabCompleter(toggleDICTabCompleter);
      this.getCommand("diclanguage").setTabCompleter(languageTabCompleter);
   }

   public void onDisable() {
      Logger var10000 = this.getLogger();
      String var10001 = String.valueOf(ChatColor.RED);
      var10000.info(var10001 + "DropItemControl" + String.valueOf(ChatColor.RESET) + " đã được tắt!");
   }

   public ItemLockManager getItemLockManager() {
      return this.itemLockManager;
   }

   public MessageUtils getMessageUtils() {
      return this.messageUtils;
   }

   public PluginStateManager getPluginStateManager() {
      return this.pluginStateManager;
   }

   public PlayerDataManager getPlayerDataManager() {
      return this.playerDataManager;
   }

   public LanguageManager getLanguageManager() {
      return this.languageManager;
   }
}
