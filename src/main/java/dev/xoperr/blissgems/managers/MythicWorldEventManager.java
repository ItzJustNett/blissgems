package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MythicWorldEventManager {
    private final BlissGems plugin;
    private final File dataFile;
    private YamlConfiguration config;
    private final Map<String, BrokenMythicEntry> entries = new LinkedHashMap<>();
    private BukkitTask tickerTask;

    public static class BrokenMythicEntry {
        public String id;
        public String itemId;
        public String displayName;
        public long brokenTimestamp;
        public long respawnTimestamp;
        public String status; // PENDING, RESPAWNED
        public int respawnX;
        public int respawnY;
        public int respawnZ;

        public boolean isPending() {
            return "PENDING".equalsIgnoreCase(status);
        }

        public long getRemainingMillis() {
            return Math.max(0, respawnTimestamp - System.currentTimeMillis());
        }

        public String getRemainingFormatted() {
            long millis = getRemainingMillis();
            if (millis <= 0) return "Ready to respawn";
            long seconds = millis / 1000;
            long days = seconds / 86400;
            seconds %= 86400;
            long hours = seconds / 3600;
            seconds %= 3600;
            long minutes = seconds / 60;
            if (days > 0) {
                return String.format("%dd %dh %dm", days, hours, minutes);
            } else if (hours > 0) {
                return String.format("%dh %dm", hours, minutes);
            } else {
                return String.format("%dm %ds", minutes, seconds % 60);
            }
        }
    }

    public MythicWorldEventManager(BlissGems plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "broken_mythics.yml");
        this.loadData();
        this.startTicker();
    }

    public void loadData() {
        entries.clear();
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create broken_mythics.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection sec = config.getConfigurationSection("broken_items");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                BrokenMythicEntry entry = new BrokenMythicEntry();
                entry.id = key;
                entry.itemId = sec.getString(key + ".itemId");
                entry.displayName = sec.getString(key + ".displayName");
                entry.brokenTimestamp = sec.getLong(key + ".brokenTimestamp");
                entry.respawnTimestamp = sec.getLong(key + ".respawnTimestamp");
                entry.status = sec.getString(key + ".status", "PENDING");
                entry.respawnX = sec.getInt(key + ".respawnX", 0);
                entry.respawnY = sec.getInt(key + ".respawnY", 0);
                entry.respawnZ = sec.getInt(key + ".respawnZ", 0);
                entries.put(key, entry);
            }
        }
    }

    public void saveData() {
        if (config == null) return;
        config.set("broken_items", null);
        for (BrokenMythicEntry entry : entries.values()) {
            String path = "broken_items." + entry.id;
            config.set(path + ".itemId", entry.itemId);
            config.set(path + ".displayName", entry.displayName);
            config.set(path + ".brokenTimestamp", entry.brokenTimestamp);
            config.set(path + ".respawnTimestamp", entry.respawnTimestamp);
            config.set(path + ".status", entry.status);
            config.set(path + ".respawnX", entry.respawnX);
            config.set(path + ".respawnY", entry.respawnY);
            config.set(path + ".respawnZ", entry.respawnZ);
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save broken_mythics.yml: " + e.getMessage());
        }
    }

    public void handleMythicBreak(ItemStack item, Player player, String reason) {
        String itemId = CustomItemManager.getIdByItem(item);
        if (itemId == null) {
            itemId = CustomItemManager.getMythicId(item);
        }
        String name = (item.getItemMeta() != null && item.getItemMeta().hasDisplayName())
                ? item.getItemMeta().getDisplayName()
                : (itemId != null ? itemId : item.getType().name());

        BrokenMythicEntry entry = new BrokenMythicEntry();
        entry.id = UUID.randomUUID().toString().substring(0, 8);
        entry.itemId = itemId != null ? itemId : item.getType().name().toLowerCase();
        entry.displayName = name;
        entry.brokenTimestamp = System.currentTimeMillis();

        long timerDays = plugin.getConfig().getLong("mythic-respawn.timer-days", 7);
        entry.respawnTimestamp = entry.brokenTimestamp + (timerDays * 24L * 3600L * 1000L);
        entry.status = "PENDING";

        entries.put(entry.id, entry);
        saveData();

        // Broadcast to all online players
        String alert = ChatColor.translateAlternateColorCodes('&',
                "&4&l[MYTHIC SHATTERED] &c" + name + " &7has been destroyed! It will reform somewhere in the 10k x 10k world border in " + timerDays + " days.");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(alert);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.6f);
        }
    }

    private void startTicker() {
        if (tickerTask != null) tickerTask.cancel();
        tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkRespawns, 200L, 600L); // Check every 30s
    }

    private void checkRespawns() {
        long now = System.currentTimeMillis();
        boolean changed = false;

        for (BrokenMythicEntry entry : entries.values()) {
            if (!entry.isPending()) continue;
            if (now >= entry.respawnTimestamp) {
                // Respawn item in overworld 10k x 10k
                World world = Bukkit.getWorlds().get(0);
                if (world == null) continue;

                int x = ThreadLocalRandom.current().nextInt(-5000, 5001);
                int z = ThreadLocalRandom.current().nextInt(-5000, 5001);
                int y = world.getHighestBlockYAt(x, z);

                Block block = world.getBlockAt(x, y + 1, z);
                block.setType(Material.CHEST);
                if (block.getState() instanceof Chest) {
                    Chest chest = (Chest) block.getState();
                    ItemStack reward = CustomItemManager.getItemById(entry.itemId);
                    if (reward == null) {
                        reward = new ItemStack(Material.NETHERITE_CHESTPLATE);
                    }
                    chest.getBlockInventory().setItem(13, reward);
                }

                entry.status = "RESPAWNED";
                entry.respawnX = x;
                entry.respawnY = y + 1;
                entry.respawnZ = z;
                changed = true;

                // Broadcast
                String alert = ChatColor.translateAlternateColorCodes('&',
                        "&6&l[MYTHIC REBORN] &eA shattered mythic (&f" + entry.displayName + "&e) has reformed at &bX: " + x + ", Y: " + (y + 1) + ", Z: " + z + "&e! Coordinates revealed!");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(alert);
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }
            }
        }

        if (changed) {
            saveData();
        }
    }

    public List<BrokenMythicEntry> getAllEntries() {
        return new ArrayList<>(entries.values());
    }

    public List<BrokenMythicEntry> getActiveBrokenMythics() {
        List<BrokenMythicEntry> list = new ArrayList<>();
        for (BrokenMythicEntry e : entries.values()) {
            if (e.isPending()) list.add(e);
        }
        return list;
    }

    public void cleanup() {
        if (tickerTask != null) tickerTask.cancel();
    }
}
