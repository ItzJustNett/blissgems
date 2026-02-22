package me.hexedhero.np.metrics;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;
import javax.net.ssl.HttpsURLConnection;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Metrics {
   public static final int B_STATS_VERSION = 1;
   private static final String URL = "https://bStats.org/submitData/bukkit";
   private final boolean enabled;
   private static boolean logFailedRequests;
   private static boolean logSentData;
   private static boolean logResponseStatusText;
   private static String serverUUID;
   private final Plugin plugin;
   private final List<Metrics.CustomChart> charts = new ArrayList();

   public Metrics(Plugin plugin) {
      if (plugin == null) {
         throw new IllegalArgumentException("Plugin cannot be null!");
      } else {
         this.plugin = plugin;
         File bStatsFolder = new File(plugin.getDataFolder().getParentFile(), "bStats");
         File configFile = new File(bStatsFolder, "config.yml");
         YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
         if (!config.isSet("serverUuid")) {
            config.addDefault("enabled", true);
            config.addDefault("serverUuid", UUID.randomUUID().toString());
            config.addDefault("logFailedRequests", false);
            config.addDefault("logSentData", false);
            config.addDefault("logResponseStatusText", false);
            config.options().header("bStats collects some data for plugin authors like how many servers are using their plugins.\nTo honor their work, you should not disable it.\nThis has nearly no effect on the server performance!\nCheck out https://bStats.org/ to learn more :)").copyDefaults(true);

            try {
               config.save(configFile);
            } catch (IOException var9) {
            }
         }

         this.enabled = config.getBoolean("enabled", true);
         serverUUID = config.getString("serverUuid");
         logFailedRequests = config.getBoolean("logFailedRequests", false);
         logSentData = config.getBoolean("logSentData", false);
         logResponseStatusText = config.getBoolean("logResponseStatusText", false);
         if (this.enabled) {
            boolean found = false;
            Iterator var6 = Bukkit.getServicesManager().getKnownServices().iterator();

            while(var6.hasNext()) {
               Class service = (Class)var6.next();

               try {
                  service.getField("B_STATS_VERSION");
                  found = true;
                  break;
               } catch (NoSuchFieldException var10) {
               }
            }

            Bukkit.getServicesManager().register(Metrics.class, this, plugin, ServicePriority.Normal);
            if (!found) {
               this.startSubmitting();
            }
         }

      }
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void addCustomChart(Metrics.CustomChart chart) {
      if (chart == null) {
         throw new IllegalArgumentException("Chart cannot be null!");
      } else {
         this.charts.add(chart);
      }
   }

   private void startSubmitting() {
      final Timer timer = new Timer(true);
      timer.scheduleAtFixedRate(new TimerTask() {
         public void run() {
            if (!Metrics.this.plugin.isEnabled()) {
               timer.cancel();
            } else {
               Bukkit.getScheduler().runTask(Metrics.this.plugin, () -> {
                  Metrics.this.submitData();
               });
            }
         }
      }, 300000L, 1800000L);
   }

   public JSONObject getPluginData() {
      JSONObject data = new JSONObject();
      String pluginName = this.plugin.getDescription().getName();
      String pluginVersion = this.plugin.getDescription().getVersion();
      data.put("pluginName", pluginName);
      data.put("pluginVersion", pluginVersion);
      JSONArray customCharts = new JSONArray();
      Iterator var5 = this.charts.iterator();

      while(var5.hasNext()) {
         Metrics.CustomChart customChart = (Metrics.CustomChart)var5.next();
         JSONObject chart = customChart.getRequestJsonObject();
         if (chart != null) {
            customCharts.add(chart);
         }
      }

      data.put("customCharts", customCharts);
      return data;
   }

   private JSONObject getServerData() {
      int playerAmount;
      try {
         Method onlinePlayersMethod = Class.forName("org.bukkit.Server").getMethod("getOnlinePlayers");
         playerAmount = onlinePlayersMethod.getReturnType().equals(Collection.class) ? ((Collection)onlinePlayersMethod.invoke(Bukkit.getServer())).size() : ((Player[])((Player[])onlinePlayersMethod.invoke(Bukkit.getServer()))).length;
      } catch (Exception var10) {
         playerAmount = Bukkit.getOnlinePlayers().size();
      }

      int onlineMode = Bukkit.getOnlineMode() ? 1 : 0;
      String bukkitVersion = Bukkit.getVersion();
      String javaVersion = System.getProperty("java.version");
      String osName = System.getProperty("os.name");
      String osArch = System.getProperty("os.arch");
      String osVersion = System.getProperty("os.version");
      int coreCount = Runtime.getRuntime().availableProcessors();
      JSONObject data = new JSONObject();
      data.put("serverUUID", serverUUID);
      data.put("playerAmount", playerAmount);
      data.put("onlineMode", onlineMode);
      data.put("bukkitVersion", bukkitVersion);
      data.put("javaVersion", javaVersion);
      data.put("osName", osName);
      data.put("osArch", osArch);
      data.put("osVersion", osVersion);
      data.put("coreCount", coreCount);
      return data;
   }

   private void submitData() {
      JSONObject data = this.getServerData();
      JSONArray pluginData = new JSONArray();
      Iterator var3 = Bukkit.getServicesManager().getKnownServices().iterator();

      while(var3.hasNext()) {
         Class service = (Class)var3.next();

         try {
            service.getField("B_STATS_VERSION");
            Iterator var5 = Bukkit.getServicesManager().getRegistrations(service).iterator();

            while(var5.hasNext()) {
               RegisteredServiceProvider provider = (RegisteredServiceProvider)var5.next();

               try {
                  pluginData.add(provider.getService().getMethod("getPluginData").invoke(provider.getProvider()));
               } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException var8) {
               }
            }
         } catch (NoSuchFieldException var9) {
         }
      }

      data.put("plugins", pluginData);
      (new Thread(() -> {
         try {
            sendData(this.plugin, data);
         } catch (Exception var3) {
            if (logFailedRequests) {
               this.plugin.getLogger().log(Level.WARNING, "Could not submit plugin stats of " + this.plugin.getName(), var3);
            }
         }

      })).start();
   }

   private static void sendData(Plugin plugin, JSONObject data) throws Exception {
      if (data == null) {
         throw new IllegalArgumentException("Data cannot be null!");
      } else if (Bukkit.isPrimaryThread()) {
         throw new IllegalAccessException("This method must not be called from the main thread!");
      } else {
         if (logSentData) {
            plugin.getLogger().info("Sending data to bStats: " + data.toString());
         }

         HttpsURLConnection connection = (HttpsURLConnection)(new URL("https://bStats.org/submitData/bukkit")).openConnection();
         byte[] compressedData = compress(data.toString());
         connection.setRequestMethod("POST");
         connection.addRequestProperty("Accept", "application/json");
         connection.addRequestProperty("Connection", "close");
         connection.addRequestProperty("Content-Encoding", "gzip");
         connection.addRequestProperty("Content-Length", String.valueOf(compressedData.length));
         connection.setRequestProperty("Content-Type", "application/json");
         connection.setRequestProperty("User-Agent", "MC-Server/1");
         connection.setDoOutput(true);
         DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
         outputStream.write(compressedData);
         outputStream.flush();
         outputStream.close();
         InputStream inputStream = connection.getInputStream();
         BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
         StringBuilder builder = new StringBuilder();

         String line;
         while((line = bufferedReader.readLine()) != null) {
            builder.append(line);
         }

         bufferedReader.close();
         if (logResponseStatusText) {
            plugin.getLogger().info("Sent data to bStats and received response: " + builder.toString());
         }

      }
   }

   private static byte[] compress(String str) throws IOException {
      if (str == null) {
         return null;
      } else {
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         GZIPOutputStream gzip = new GZIPOutputStream(outputStream);
         gzip.write(str.getBytes(StandardCharsets.UTF_8));
         gzip.close();
         return outputStream.toByteArray();
      }
   }

   static {
      if (System.getProperty("bstats.relocatecheck") == null || !System.getProperty("bstats.relocatecheck").equals("false")) {
         String defaultPackage = new String(new byte[]{111, 114, 103, 46, 98, 115, 116, 97, 116, 115, 46, 98, 117, 107, 107, 105, 116});
         String examplePackage = new String(new byte[]{121, 111, 117, 114, 46, 112, 97, 99, 107, 97, 103, 101});
         if (Metrics.class.getPackage().getName().equals(defaultPackage) || Metrics.class.getPackage().getName().equals(examplePackage)) {
            throw new IllegalStateException("bStats Metrics class has not been relocated correctly!");
         }
      }

   }

   public static class AdvancedBarChart extends Metrics.CustomChart {
      private final Callable<Map<String, int[]>> callable;

      public AdvancedBarChart(String chartId, Callable<Map<String, int[]>> callable) {
         super(chartId);
         this.callable = callable;
      }

      protected JSONObject getChartData() throws Exception {
         JSONObject data = new JSONObject();
         JSONObject values = new JSONObject();
         Map<String, int[]> map = (Map)this.callable.call();
         if (map != null && !map.isEmpty()) {
            boolean allSkipped = true;
            Iterator var5 = map.entrySet().iterator();

            while(true) {
               Entry entry;
               do {
                  if (!var5.hasNext()) {
                     if (allSkipped) {
                        return null;
                     }

                     data.put("values", values);
                     return data;
                  }

                  entry = (Entry)var5.next();
               } while(((int[])entry.getValue()).length == 0);

               allSkipped = false;
               JSONArray categoryValues = new JSONArray();
               int[] var8 = (int[])entry.getValue();
               int var9 = var8.length;

               for(int var10 = 0; var10 < var9; ++var10) {
                  int categoryValue = var8[var10];
                  categoryValues.add(categoryValue);
               }

               values.put(entry.getKey(), categoryValues);
            }
         } else {
            return null;
         }
      }
   }

   public static class SimpleBarChart extends Metrics.CustomChart {
      private final Callable<Map<String, Integer>> callable;

      public SimpleBarChart(String chartId, Callable<Map<String, Integer>> callable) {
         super(chartId);
         this.callable = callable;
      }

      protected JSONObject getChartData() throws Exception {
         JSONObject data = new JSONObject();
         JSONObject values = new JSONObject();
         Map<String, Integer> map = (Map)this.callable.call();
         if (map != null && !map.isEmpty()) {
            Iterator var4 = map.entrySet().iterator();

            while(var4.hasNext()) {
               Entry<String, Integer> entry = (Entry)var4.next();
               JSONArray categoryValues = new JSONArray();
               categoryValues.add(entry.getValue());
               values.put(entry.getKey(), categoryValues);
            }

            data.put("values", values);
            return data;
         } else {
            return null;
         }
      }
   }

   public static class MultiLineChart extends Metrics.CustomChart {
      private final Callable<Map<String, Integer>> callable;

      public MultiLineChart(String chartId, Callable<Map<String, Integer>> callable) {
         super(chartId);
         this.callable = callable;
      }

      protected JSONObject getChartData() throws Exception {
         JSONObject data = new JSONObject();
         JSONObject values = new JSONObject();
         Map<String, Integer> map = (Map)this.callable.call();
         if (map != null && !map.isEmpty()) {
            boolean allSkipped = true;
            Iterator var5 = map.entrySet().iterator();

            while(var5.hasNext()) {
               Entry<String, Integer> entry = (Entry)var5.next();
               if ((Integer)entry.getValue() != 0) {
                  allSkipped = false;
                  values.put(entry.getKey(), entry.getValue());
               }
            }

            if (allSkipped) {
               return null;
            } else {
               data.put("values", values);
               return data;
            }
         } else {
            return null;
         }
      }
   }

   public static class SingleLineChart extends Metrics.CustomChart {
      private final Callable<Integer> callable;

      public SingleLineChart(String chartId, Callable<Integer> callable) {
         super(chartId);
         this.callable = callable;
      }

      protected JSONObject getChartData() throws Exception {
         JSONObject data = new JSONObject();
         int value = (Integer)this.callable.call();
         if (value == 0) {
            return null;
         } else {
            data.put("value", value);
            return data;
         }
      }
   }

   public static class DrilldownPie extends Metrics.CustomChart {
      private final Callable<Map<String, Map<String, Integer>>> callable;

      public DrilldownPie(String chartId, Callable<Map<String, Map<String, Integer>>> callable) {
         super(chartId);
         this.callable = callable;
      }

      public JSONObject getChartData() throws Exception {
         JSONObject data = new JSONObject();
         JSONObject values = new JSONObject();
         Map<String, Map<String, Integer>> map = (Map)this.callable.call();
         if (map != null && !map.isEmpty()) {
            boolean reallyAllSkipped = true;
            Iterator var5 = map.entrySet().iterator();

            while(var5.hasNext()) {
               Entry<String, Map<String, Integer>> entryValues = (Entry)var5.next();
               JSONObject value = new JSONObject();
               boolean allSkipped = true;

               for(Iterator var9 = ((Map)map.get(entryValues.getKey())).entrySet().iterator(); var9.hasNext(); allSkipped = false) {
                  Entry<String, Integer> valueEntry = (Entry)var9.next();
                  value.put(valueEntry.getKey(), valueEntry.getValue());
               }

               if (!allSkipped) {
                  reallyAllSkipped = false;
                  values.put(entryValues.getKey(), value);
               }
            }

            if (reallyAllSkipped) {
               return null;
            } else {
               data.put("values", values);
               return data;
            }
         } else {
            return null;
         }
      }
   }

   public static class AdvancedPie extends Metrics.CustomChart {
      private final Callable<Map<String, Integer>> callable;

      public AdvancedPie(String chartId, Callable<Map<String, Integer>> callable) {
         super(chartId);
         this.callable = callable;
      }

      protected JSONObject getChartData() throws Exception {
         JSONObject data = new JSONObject();
         JSONObject values = new JSONObject();
         Map<String, Integer> map = (Map)this.callable.call();
         if (map != null && !map.isEmpty()) {
            boolean allSkipped = true;
            Iterator var5 = map.entrySet().iterator();

            while(var5.hasNext()) {
               Entry<String, Integer> entry = (Entry)var5.next();
               if ((Integer)entry.getValue() != 0) {
                  allSkipped = false;
                  values.put(entry.getKey(), entry.getValue());
               }
            }

            if (allSkipped) {
               return null;
            } else {
               data.put("values", values);
               return data;
            }
         } else {
            return null;
         }
      }
   }

   public static class SimplePie extends Metrics.CustomChart {
      private final Callable<String> callable;

      public SimplePie(String chartId, Callable<String> callable) {
         super(chartId);
         this.callable = callable;
      }

      protected JSONObject getChartData() throws Exception {
         JSONObject data = new JSONObject();
         String value = (String)this.callable.call();
         if (value != null && !value.isEmpty()) {
            data.put("value", value);
            return data;
         } else {
            return null;
         }
      }
   }

   public abstract static class CustomChart {
      final String chartId;

      CustomChart(String chartId) {
         if (chartId != null && !chartId.isEmpty()) {
            this.chartId = chartId;
         } else {
            throw new IllegalArgumentException("ChartId cannot be null or empty!");
         }
      }

      private JSONObject getRequestJsonObject() {
         JSONObject chart = new JSONObject();
         chart.put("chartId", this.chartId);

         try {
            JSONObject data = this.getChartData();
            if (data == null) {
               return null;
            } else {
               chart.put("data", data);
               return chart;
            }
         } catch (Throwable var3) {
            if (Metrics.logFailedRequests) {
               Bukkit.getLogger().log(Level.WARNING, "Failed to get data for custom chart with id " + this.chartId, var3);
            }

            return null;
         }
      }

      protected abstract JSONObject getChartData() throws Exception;
   }
}
