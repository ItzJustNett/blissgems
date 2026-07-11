/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.inventory.ItemStack
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.EnergyState;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerDeathListener
implements Listener {
    private final BlissGems plugin;
    private final Map<UUID, List<ItemStack>> savedGems = new HashMap<>();

    public PlayerDeathListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Check victim's energy state BEFORE death to determine if killer can gain energy
        EnergyState victimState = this.plugin.getEnergyManager().getEnergyState(victim);
        boolean victimHadEnergy = victimState != EnergyState.BROKEN;

        int energyLoss = this.plugin.getConfigManager().getEnergyLossOnDeath();
        this.plugin.getEnergyManager().removeEnergy(victim, energyLoss);

        // Check if player reached 0 energy and ban is enabled
        int currentEnergy = this.plugin.getEnergyManager().getEnergy(victim);
        if (currentEnergy <= 0 && this.plugin.getConfigManager().isBanOnZeroEnergyEnabled()) {
            String banMessage = this.plugin.getConfigManager().getFormattedMessage("energy-zero-banned");
            if (banMessage == null || banMessage.isEmpty()) {
                banMessage = "You have been banned for reaching 0 energy!";
            }
            // Ban the player
            victim.ban(banMessage, (java.util.Date)null, (String)null);
        }

        if (killer != null && victimHadEnergy) {
            // Only give killer energy if victim had energy to drop (not BROKEN)
            int energyGain = this.plugin.getConfigManager().getEnergyGainOnKill();
            this.plugin.getEnergyManager().addEnergy(killer, energyGain);
            EnergyState killerState = this.plugin.getEnergyManager().getEnergyState(killer);
            if (killerState.isMaxEnergy() && this.plugin.getConfigManager().isEnergyBottleDropEnabled()) {
                this.dropEnergyBottle(victim.getLocation());
            }
        }

        // NOTE: Gem drop-protection runs in onPlayerDeathProtectGems() at LOWEST priority
        // (below) so it strips gems out of event.getDrops() BEFORE gravestone / SMP-core
        // plugins snapshot or replace the drop list. Doing it here at HIGHEST ran too late.

        // Clean up any active Astra gem abilities (projection, drift, void)
        this.plugin.getAstraAbilities().cleanup(victim);
        // Clean up any active Fire gem charging
        this.plugin.getFireAbilities().cleanup(victim);
        // Clean up any active Flux gem charging
        this.plugin.getFluxAbilities().cleanup(victim);
        // Clean up Life gem heart modifiers (Circle of Life, Heart Lock)
        this.plugin.getLifeAbilities().cleanup(victim);
        // Clean up Astra Soul Absorption max-health modifiers
        if (this.plugin.getSoulManager() != null) {
            this.plugin.getSoulManager().cleanup(victim);
        }

        this.plugin.getGemManager().updateActiveGem(victim);
        if (killer != null) {
            this.plugin.getGemManager().updateActiveGem(killer);
        }
    }

    /**
     * Strip undroppable gems from the death drops at the EARLIEST possible point.
     *
     * Other death-handling plugins (gravestones, "SMP core" lives/hardcore systems, some
     * lag plugins) frequently read or replace event.getDrops() at NORMAL/HIGH priority. If
     * we stripped gems at HIGHEST we'd run AFTER them — they'd already have copied the gems
     * into a grave / their own drop handling, so the gem effectively becomes droppable.
     * Running at LOWEST pulls gems out of the list before anyone else sees it. Gems are
     * restored on respawn, with enforceOneGemOnly() guarding against any duplication (e.g.
     * a plugin-driven keepInventory that also retains the gem).
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeathProtectGems(PlayerDeathEvent event) {
        if (!this.plugin.getConfig().getBoolean("gems.prevent-drop", true)) {
            return;
        }
        Player victim = event.getEntity();
        List<ItemStack> gemsToSave = new ArrayList<>();
        List<String> droppableOnDeath = this.plugin.getConfig().contains("gems.droppable-on-death")
                ? this.plugin.getConfig().getStringList("gems.droppable-on-death")
                : java.util.List.of("heretic", "auratus");
        event.getDrops().removeIf(item -> {
            if (CustomItemManager.isUndroppable(item)) {
                if (isDroppableOnDeath(item, droppableOnDeath)) {
                    return false; // configured to drop on death — leave it in the drops
                }
                gemsToSave.add(item.clone());
                return true; // remove from drops
            }
            return false;
        });
        if (!gemsToSave.isEmpty()) {
            savedGems.put(victim.getUniqueId(), gemsToSave);
            saveGemsToDisk(victim.getUniqueId(), gemsToSave);
        }
    }

    /**
     * True if the item is a gem whose type is configured to drop on death (e.g. heretic/auratus),
     * so it is left in the death drops instead of being kept and re-given on respawn.
     */
    private boolean isDroppableOnDeath(ItemStack item, List<String> droppableGems) {
        if (droppableGems.isEmpty()) {
            return false;
        }
        String id = CustomItemManager.getIdByItem(item);
        if (id == null) {
            return false;
        }
        int gemMarker = id.indexOf("_gem_t");
        if (gemMarker <= 0) {
            return false;
        }
        return droppableGems.contains(id.substring(0, gemMarker));
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Give saved gems back to player
        UUID playerId = player.getUniqueId();
        List<ItemStack> gems = null;

        // Check memory first (for immediate respawns)
        if (savedGems.containsKey(playerId)) {
            gems = savedGems.remove(playerId);
            // Also clear disk copy to prevent duplication on future restarts
            clearGemsFromDisk(playerId);
        }
        // If not in memory, check disk (for respawns after disconnect/restart)
        else {
            gems = loadGemsFromDisk(playerId);
        }

        if (gems != null && !gems.isEmpty()) {
            // Safety check: If single-gem-only is enabled, only return the first gem
            // This prevents duplication bugs
            boolean singleGemOnly = this.plugin.getConfig().getBoolean("gems.single-gem-only", true);
            if (singleGemOnly && gems.size() > 1) {
                this.plugin.getLogger().warning("Player " + player.getName() + " had " + gems.size() +
                    " gems saved! Only returning first gem due to single-gem-only config.");
                player.getInventory().addItem(gems.get(0));
            } else {
                for (ItemStack gem : gems) {
                    player.getInventory().addItem(gem);
                }
            }
        }

        // Enforce single-gem-only after restoring gems (safety net against duplication)
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            if (player.isOnline()) {
                enforceOneGemOnly(player);
                this.plugin.getGemManager().updateActiveGem(player);
            }
        }, 1L);
    }

    private void dropEnergyBottle(Location location) {
        ItemStack bottle = CustomItemManager.getItemById((String)"energy_bottle");
        if (bottle != null) {
            location.getWorld().dropItemNaturally(location, bottle);
        }
    }

    /**
     * Save gems to disk so they persist across server restarts/disconnects
     */
    private void saveGemsToDisk(UUID playerId, List<ItemStack> gems) {
        File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File file = new File(dataFolder, playerId + ".yml");
        FileConfiguration data;

        if (file.exists()) {
            data = YamlConfiguration.loadConfiguration(file);
        } else {
            data = new YamlConfiguration();
        }

        // Save gem items as serialized data
        data.set("saved-gems", gems);

        try {
            data.save(file);
        } catch (IOException e) {
            this.plugin.getLogger().severe("Failed to save gems for player " + playerId + ": " + e.getMessage());
        }
    }

    /**
     * Load saved gems from disk
     */
    private List<ItemStack> loadGemsFromDisk(UUID playerId) {
        File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
        File file = new File(dataFolder, playerId + ".yml");

        if (!file.exists()) {
            return null;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        @SuppressWarnings("unchecked")
        List<ItemStack> gems = (List<ItemStack>) data.get("saved-gems");

        // Clear the saved gems from disk after loading
        if (gems != null) {
            data.set("saved-gems", null);
            try {
                data.save(file);
            } catch (IOException e) {
                this.plugin.getLogger().warning("Failed to clear saved gems from disk for " + playerId);
            }
        }

        return gems;
    }

    /**
     * Clear saved gems from disk without loading them
     */
    private void clearGemsFromDisk(UUID playerId) {
        File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
        File file = new File(dataFolder, playerId + ".yml");

        if (!file.exists()) {
            return;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        if (data.contains("saved-gems")) {
            data.set("saved-gems", null);
            try {
                data.save(file);
            } catch (IOException e) {
                this.plugin.getLogger().warning("Failed to clear saved gems from disk for " + playerId);
            }
        }
    }

    /**
     * Remove duplicate gems from inventory, keeping only the first one found.
     */
    private void enforceOneGemOnly(Player player) {
        boolean foundFirst = false;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null) continue;
            String itemId = CustomItemManager.getIdByItem(item);
            if (itemId == null) continue;
            if (!this.plugin.getGemManager().isAnyGem(itemId)) continue;

            if (!foundFirst) {
                foundFirst = true;
            } else {
                player.getInventory().setItem(i, null);
                this.plugin.getLogger().info("Removed duplicate gem from " + player.getName() + " on respawn.");
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.plugin.getGemManager().updateActiveGem(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Clean up Astra gem abilities (projection, drift, void)
        this.plugin.getAstraAbilities().cleanup(player);
        // Clean up Fire gem charging
        this.plugin.getFireAbilities().cleanup(player);
        // Clean up Flux gem charging
        this.plugin.getFluxAbilities().cleanup(player);
        // Clean up Speed gem Adrenaline Rush
        this.plugin.getSpeedAbilities().cleanup(player.getUniqueId());
        // Clean up Wealth gem amplification/effects
        this.plugin.getWealthAbilities().cleanup(player);
        // Clean up Life gem heart modifiers (Circle of Life, Heart Lock)
        this.plugin.getLifeAbilities().cleanup(player);
        // Clean up Astra Soul Absorption max-health modifiers
        this.plugin.getSoulManager().cleanup(player);

        // Clear captured souls
        this.plugin.getSoulManager().clearSouls(player.getUniqueId());

        // No need to clear saved gems - they're persisted to disk now!
        // If player quits before respawning, gems will be loaded from disk on next login

        this.plugin.getEnergyManager().clearCache(player.getUniqueId());
        this.plugin.getGemManager().clearCache(player.getUniqueId());
        this.plugin.getAbilityManager().clearCache(player.getUniqueId());
    }
}

