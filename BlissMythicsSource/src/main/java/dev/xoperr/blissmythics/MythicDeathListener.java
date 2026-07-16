/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.xoperr.blissgems.api.BlissGemsAPI
 *  dev.xoperr.blissgems.utils.CustomItemManager
 *  org.bukkit.Bukkit
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package dev.xoperr.blissmythics;

import dev.xoperr.blissgems.api.BlissGemsAPI;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissmythics.BlissMythics;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class MythicDeathListener
implements Listener {
    private static final Set<String> MYTHIC_IDS = Set.of("heretic", "auratus");
    private final BlissMythics plugin;
    private final BlissGemsAPI api;
    private final Set<UUID> pendingRemoval = new HashSet<UUID>();
    private final File file;

    public MythicDeathListener(BlissMythics blissMythics, BlissGemsAPI blissGemsAPI) {
        this.plugin = blissMythics;
        this.api = blissGemsAPI;
        this.file = new File(blissMythics.getDataFolder(), "pending-mythic-removals.yml");
        this.load();
    }

    private boolean isMythicGemItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return false;
        }
        String string = CustomItemManager.getIdByItem((ItemStack)itemStack);
        if (string == null) {
            return false;
        }
        String string2 = this.api.getGemRegistry().gemIdFromItemId(string);
        return string2 != null && MYTHIC_IDS.contains(string2);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onDeath(PlayerDeathEvent playerDeathEvent) {
        if (playerDeathEvent.getKeepInventory()) {
            return;
        }
        Player player = playerDeathEvent.getEntity();
        boolean bl = false;
        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (!this.isMythicGemItem(itemStack)) continue;
            playerDeathEvent.getDrops().add(itemStack.clone());
            bl = true;
        }
        if (bl) {
            this.pendingRemoval.add(player.getUniqueId());
            this.save();
            player.sendMessage("\u00a7d\u00a7oYour mythic gem was dropped where you died!");
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent playerRespawnEvent) {
        Player player = playerRespawnEvent.getPlayer();
        UUID uUID = player.getUniqueId();
        if (!this.pendingRemoval.contains(uUID)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            for (int i = 0; i < player.getInventory().getSize(); ++i) {
                ItemStack itemStack = player.getInventory().getItem(i);
                if (!this.isMythicGemItem(itemStack)) continue;
                player.getInventory().setItem(i, null);
            }
            this.pendingRemoval.remove(uUID);
            this.save();
        }, 3L);
    }

    void save() {
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        yamlConfiguration.set("pending", this.pendingRemoval.stream().map(UUID::toString).collect(Collectors.toList()));
        try {
            if (!this.plugin.getDataFolder().exists()) {
                this.plugin.getDataFolder().mkdirs();
            }
            yamlConfiguration.save(this.file);
        }
        catch (IOException iOException) {
            this.plugin.getLogger().warning("Could not save pending mythic removals: " + iOException.getMessage());
        }
    }

    private void load() {
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)this.file);
        for (String string : yamlConfiguration.getStringList("pending")) {
            try {
                this.pendingRemoval.add(UUID.fromString(string));
            }
            catch (IllegalArgumentException illegalArgumentException) {}
        }
    }
}

