/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.th0rgal.oraxen.api.OraxenItems
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.inventory.ItemStack
 */
package fun.obriy.blissgems.listeners;

import fun.obriy.blissgems.BlissGems;
import fun.obriy.blissgems.utils.EnergyState;
import fun.obriy.blissgems.utils.GemType;
import io.th0rgal.oraxen.api.OraxenItems;
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
            String oraxenId = OraxenItems.getIdByItem(item);
            if (oraxenId != null && GemType.isGem(oraxenId)) {
                savedGems.put(victim.getUniqueId(), item.clone());
                iterator.remove();
                break;
            }
        }

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
        ItemStack bottle = OraxenItems.getItemById((String)"energy_bottle").build();
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
        this.plugin.getEnergyManager().clearCache(player.getUniqueId());
        this.plugin.getGemManager().clearCache(player.getUniqueId());
        this.plugin.getAbilityManager().clearCache(player.getUniqueId());
    }
}

