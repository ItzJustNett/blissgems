package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;

import java.util.HashMap;
import java.util.Map;

public class EnchantLimiterManager {
    private final BlissGems plugin;
    private final Map<String, Integer> limits = new HashMap<>();
    private boolean enabled;
    private int enderPearlCooldownSeconds;

    public EnchantLimiterManager(BlissGems plugin) {
        this.plugin = plugin;
        this.reload();
    }

    public void reload() {
        this.limits.clear();
        this.enabled = this.plugin.getConfig().getBoolean("enchant-limiter.enabled", true);
        this.enderPearlCooldownSeconds = this.plugin.getConfig().getInt("combat.ender-pearl-cooldown-seconds", 15);

        ConfigurationSection section = this.plugin.getConfig().getConfigurationSection("enchant-limiter.limits");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                int maxLvl = section.getInt(key, -1);
                if (maxLvl >= 0) {
                    limits.put(key.toLowerCase().replace("minecraft:", ""), maxLvl);
                }
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setLimit(String enchantName, int maxLevel) {
        String key = enchantName.toLowerCase().replace("minecraft:", "");
        limits.put(key, maxLevel);
        this.plugin.getConfig().set("enchant-limiter.limits." + key, maxLevel);
        this.plugin.saveConfig();
    }

    public int getLimit(String enchantName) {
        String key = enchantName.toLowerCase().replace("minecraft:", "");
        return limits.getOrDefault(key, -1);
    }

    public int getLimit(Enchantment enchantment) {
        if (enchantment == null) return -1;
        String key = enchantment.getKey().getKey().toLowerCase();
        return getLimit(key);
    }

    public Map<String, Integer> getAllLimits() {
        return new HashMap<>(limits);
    }

    public int getEnderPearlCooldownSeconds() {
        return enderPearlCooldownSeconds;
    }

    public void setEnderPearlCooldownSeconds(int seconds) {
        this.enderPearlCooldownSeconds = seconds;
        this.plugin.getConfig().set("combat.ender-pearl-cooldown-seconds", seconds);
        this.plugin.saveConfig();
    }
}
