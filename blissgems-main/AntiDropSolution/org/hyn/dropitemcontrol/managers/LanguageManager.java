package org.hyn.dropitemcontrol.managers;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.hyn.dropitemcontrol.Dropitemcontrol;

public class LanguageManager {
   private final Dropitemcontrol plugin;
   private final String defaultLanguage;
   private final boolean allowPlayerLanguage;
   private final Map<String, FileConfiguration> languageFiles;
   private final Map<UUID, String> playerLanguages;

   public LanguageManager(Dropitemcontrol plugin) {
      this.plugin = plugin;
      this.languageFiles = new HashMap();
      this.playerLanguages = new HashMap();
      FileConfiguration config = plugin.getConfig();
      this.defaultLanguage = config.getString("language.default", "vi");
      this.allowPlayerLanguage = config.getBoolean("language.allow-player-language", true);
      this.loadLanguageFiles();
   }

   private void loadLanguageFiles() {
      File langDir = new File(this.plugin.getDataFolder(), "lang");
      if (!langDir.exists()) {
         langDir.mkdirs();
         this.plugin.saveResource("lang/vi.yml", false);
         this.plugin.saveResource("lang/en.yml", false);
      }

      File[] langFiles = langDir.listFiles((dir, name) -> {
         return name.endsWith(".yml");
      });
      if (langFiles != null) {
         File[] var3 = langFiles;
         int var4 = langFiles.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            File file = var3[var5];
            String langCode = file.getName().replace(".yml", "");
            FileConfiguration langConfig = YamlConfiguration.loadConfiguration(file);
            this.languageFiles.put(langCode, langConfig);
            this.plugin.getLogger().info("Loaded language file: " + langCode);
         }
      }

      if (!this.languageFiles.containsKey(this.defaultLanguage)) {
         this.plugin.getLogger().warning("Default language file not found: " + this.defaultLanguage);
         if (!this.languageFiles.isEmpty()) {
            String firstLang = (String)this.languageFiles.keySet().iterator().next();
            this.plugin.getLogger().warning("Using " + firstLang + " as default language instead");
         }
      }

   }

   public FileConfiguration getLanguageConfig(Player player) {
      if (player != null && this.allowPlayerLanguage) {
         String playerLang = (String)this.playerLanguages.getOrDefault(player.getUniqueId(), this.defaultLanguage);
         if (this.languageFiles.containsKey(playerLang)) {
            return (FileConfiguration)this.languageFiles.get(playerLang);
         }
      }

      return (FileConfiguration)this.languageFiles.getOrDefault(this.defaultLanguage, this.plugin.getConfig());
   }

   public FileConfiguration getDefaultLanguageConfig() {
      return (FileConfiguration)this.languageFiles.getOrDefault(this.defaultLanguage, this.plugin.getConfig());
   }

   public boolean setPlayerLanguage(Player player, String langCode) {
      if (this.languageFiles.containsKey(langCode)) {
         this.playerLanguages.put(player.getUniqueId(), langCode);
         return true;
      } else {
         return false;
      }
   }

   public String getPlayerLanguage(Player player) {
      return (String)this.playerLanguages.getOrDefault(player.getUniqueId(), this.defaultLanguage);
   }

   public Map<String, String> getAvailableLanguages() {
      Map<String, String> languages = new HashMap();

      String langCode;
      String langName;
      for(Iterator var2 = this.languageFiles.keySet().iterator(); var2.hasNext(); languages.put(langCode, langName)) {
         langCode = (String)var2.next();
         byte var6 = -1;
         switch(langCode.hashCode()) {
         case 3241:
            if (langCode.equals("en")) {
               var6 = 1;
            }
            break;
         case 3763:
            if (langCode.equals("vi")) {
               var6 = 0;
            }
         }

         switch(var6) {
         case 0:
            langName = "Tiếng Việt";
            break;
         case 1:
            langName = "English";
            break;
         default:
            langName = langCode;
         }
      }

      return languages;
   }
}
