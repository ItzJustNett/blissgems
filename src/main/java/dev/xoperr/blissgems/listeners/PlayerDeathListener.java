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
import dev.xoperr.blissgems.utils.GemType;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class PlayerDeathListener
implements Listener {
    private final BlissGems plugin;
    private final Map<UUID, ItemStack> savedGems = new HashMap<>();

    public PlayerDeathListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
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

        if (killer != null) {
            int energyGain = this.plugin.getConfigManager().getEnergyGainOnKill();
            this.plugin.getEnergyManager().addEnergy(killer, energyGain);
            EnergyState killerState = this.plugin.getEnergyManager().getEnergyState(killer);
            if (killerState.isMaxEnergy() && this.plugin.getConfigManager().isEnergyBottleDropEnabled()) {
                this.dropEnergyBottle(victim.getLocation());
            }
        }

        // Keep gem on death - remove from drops and save for respawn
        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (item == null) continue;
            String oraxenId = CustomItemManager.getIdByItem(item);
            if (oraxenId != null && GemType.isGem(oraxenId)) {
                savedGems.put(victim.getUniqueId(), item.clone());
                iterator.remove();
                break;
            }
        }

        // Clean up any active Fire gem charging
        this.plugin.getFireAbilities().cleanup(victim);
        // Clean up any active Flux gem charging
        this.plugin.getFluxAbilities().cleanup(victim);

        this.plugin.getGemManager().updateActiveGem(victim);
        if (killer != null) {
            this.plugin.getGemManager().updateActiveGem(killer);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Give back the saved gem
        if (savedGems.containsKey(uuid)) {
            ItemStack gem = savedGems.remove(uuid);
            if (gem != null) {
                player.getInventory().addItem(gem);
            }
        }

        // Update active gem after respawn
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            this.plugin.getGemManager().updateActiveGem(player);
        }, 1L);
    }

    private void dropEnergyBottle(Location location) {
        ItemStack bottle = CustomItemManager.getItemById((String)"energy_bottle");
        if (bottle != null) {
            location.getWorld().dropItemNaturally(location, bottle);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.plugin.getGemManager().updateActiveGem(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Clean up Fire gem charging
        this.plugin.getFireAbilities().cleanup(player);
        // Clean up Flux gem charging
        this.plugin.getFluxAbilities().cleanup(player);
        // Clean up Speed gem Adrenaline Rush
        this.plugin.getSpeedAbilities().cleanup(player.getUniqueId());

        this.plugin.getEnergyManager().clearCache(player.getUniqueId());
        this.plugin.getGemManager().clearCache(player.getUniqueId());
        this.plugin.getAbilityManager().clearCache(player.getUniqueId());
    }
}

