/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 */
package dev.xoperr.blissgems.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.xoperr.blissgems.BlissGems;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.bukkit.plugin.Plugin;

public class UpdateChecker {
    private static final String VERSIONS_URL = "https://api.modrinth.com/v2/project/%s/version";
    private final BlissGems plugin;

    public UpdateChecker(BlissGems plugin) {
        this.plugin = plugin;
    }

    public void checkAsync() {
        if (!this.plugin.getConfig().getBoolean("auto-update.enabled", true)) {
            return;
        }
        String slug = this.plugin.getConfig().getString("auto-update.modrinth-project", "").trim();
        if (slug.isEmpty()) {
            this.plugin.getLogger().info("[AutoUpdate] Set auto-update.modrinth-project in config.yml to enable update checks.");
            return;
        }
        boolean notifyOnly = this.plugin.getConfig().getBoolean("auto-update.notify-only", false);
        this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> this.run(slug, notifyOnly));
    }

    private void run(String slug, boolean notifyOnly) {
        try {
            String current;
            JsonArray versions = this.fetchJson(String.format(VERSIONS_URL, slug)).getAsJsonArray();
            JsonObject latest = null;
            for (JsonElement el : versions) {
                JsonObject v = el.getAsJsonObject();
                if (!this.targetsBukkit(v)) continue;
                latest = v;
                break;
            }
            if (latest == null) {
                this.plugin.getLogger().warning("[AutoUpdate] No Paper/Spigot/Bukkit version found on Modrinth for '" + slug + "'.");
                return;
            }
            String remote = latest.get("version_number").getAsString();
            if (this.compareVersions(remote, current = this.plugin.getDescription().getVersion()) <= 0) {
                this.plugin.getLogger().info("[AutoUpdate] Up to date (" + current + ").");
                return;
            }
            this.plugin.getLogger().info("[AutoUpdate] New version available: " + remote + " (current " + current + ").");
            if (notifyOnly) {
                return;
            }
            JsonObject file = this.primaryFile(latest);
            if (file == null) {
                this.plugin.getLogger().warning("[AutoUpdate] Latest version has no downloadable file.");
                return;
            }
            this.downloadToUpdateFolder(file.get("url").getAsString(), remote);
        }
        catch (Exception ex) {
            this.plugin.getLogger().warning("[AutoUpdate] Update check failed: " + ex.getMessage());
        }
    }

    private boolean targetsBukkit(JsonObject version) {
        JsonArray loaders = version.getAsJsonArray("loaders");
        if (loaders == null) {
            return false;
        }
        for (JsonElement l : loaders) {
            switch (l.getAsString().toLowerCase()) {
                case "paper": 
                case "spigot": 
                case "bukkit": 
                case "purpur": 
                case "folia": {
                    return true;
                }
            }
        }
        return false;
    }

    private JsonObject primaryFile(JsonObject version) {
        JsonArray files = version.getAsJsonArray("files");
        if (files == null || files.isEmpty()) {
            return null;
        }
        for (JsonElement fe : files) {
            JsonObject f = fe.getAsJsonObject();
            if (!f.has("primary") || !f.get("primary").getAsBoolean()) continue;
            return f;
        }
        return files.get(0).getAsJsonObject();
    }

    private void downloadToUpdateFolder(String url, String version) throws Exception {
        File runningJar = this.plugin.getPluginFile();
        File updateDir = new File(this.plugin.getDataFolder().getParentFile(), this.plugin.getServer().getUpdateFolder());
        if (!updateDir.exists() && !updateDir.mkdirs()) {
            this.plugin.getLogger().warning("[AutoUpdate] Could not create update folder " + String.valueOf(updateDir));
            return;
        }
        File dest = new File(updateDir, runningJar.getName());
        HttpURLConnection conn = this.open(url);
        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(dest);){
            int read;
            byte[] buffer = new byte[8192];
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        this.plugin.getLogger().info("[AutoUpdate] Downloaded " + version + " to " + this.plugin.getServer().getUpdateFolder() + "/" + dest.getName() + " \u2014 it will be applied on the next server restart.");
    }

    private JsonElement fetchJson(String url) throws Exception {
        HttpURLConnection conn = this.open(url);
        if (conn.getResponseCode() != 200) {
            throw new IllegalStateException("HTTP " + conn.getResponseCode() + " from " + url);
        }
        try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);){
            JsonElement jsonElement = JsonParser.parseReader(reader);
            return jsonElement;
        }
    }

    private HttpURLConnection open(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", "BlissGems/" + this.plugin.getDescription().getVersion() + " (auto-updater)");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(15000);
        conn.setInstanceFollowRedirects(true);
        return conn;
    }

    private int compareVersions(String a, String b) {
        String[] pa = a.replaceAll("[^0-9.]", "").split("\\.");
        String[] pb = b.replaceAll("[^0-9.]", "").split("\\.");
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; ++i) {
            int y;
            int x = i < pa.length && !pa[i].isEmpty() ? Integer.parseInt(pa[i]) : 0;
            int n = y = i < pb.length && !pb[i].isEmpty() ? Integer.parseInt(pb[i]) : 0;
            if (x == y) continue;
            return Integer.compare(x, y);
        }
        return 0;
    }
}

