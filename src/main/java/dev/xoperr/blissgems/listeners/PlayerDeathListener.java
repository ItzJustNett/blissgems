/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.EnergyState;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

public class PlayerDeathListener
implements Listener {
    private final BlissGems plugin;
    private final Map<UUID, List<ItemStack>> savedGems = new HashMap<UUID, List<ItemStack>>();

    public PlayerDeathListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        EnergyState victimState = this.plugin.getEnergyManager().getEnergyState(victim);
        boolean victimHadEnergy = victimState != EnergyState.BROKEN;
        int energyLoss = this.plugin.getConfigManager().getEnergyLossOnDeath();
        this.plugin.getEnergyManager().removeEnergy(victim, energyLoss);
        int currentEnergy = this.plugin.getEnergyManager().getEnergy(victim);
        if (currentEnergy <= 0 && this.plugin.getConfigManager().isBanOnZeroEnergyEnabled()) {
            String banMessage = this.plugin.getConfigManager().getFormattedMessage("energy-zero-banned", new Object[0]);
            if (banMessage == null || banMessage.isEmpty()) {
                banMessage = "You have been banned for reaching 0 energy!";
            }
            victim.ban(banMessage, (Date)null, (String)null);
        }
        if (killer != null && victimHadEnergy) {
            int energyGain = this.plugin.getConfigManager().getEnergyGainOnKill();
            this.plugin.getEnergyManager().addEnergy(killer, energyGain);
            EnergyState killerState = this.plugin.getEnergyManager().getEnergyState(killer);
            if (killerState.isMaxEnergy() && this.plugin.getConfigManager().isEnergyBottleDropEnabled()) {
                this.dropEnergyBottle(victim.getLocation());
            }
        }
        if (killer != null && this.plugin.getConfig().getBoolean("pvp.drop-head-on-kill", true)) {
            this.dropPlayerHead(victim);
        }
        if (this.plugin.getConfigManager().isUpgraderDropOnTier2DeathEnabled() && this.plugin.getGemManager().hasActiveGem(victim) && this.plugin.getGemManager().getGemTier(victim) == 2) {
            this.dropUpgrader(victim.getLocation());
        }
        this.plugin.getAstraAbilities().cleanup(victim);
        this.plugin.getFireAbilities().cleanup(victim);
        this.plugin.getFluxAbilities().cleanup(victim);
        this.plugin.getLifeAbilities().cleanup(victim);
        if (this.plugin.getSoulManager() != null) {
            this.plugin.getSoulManager().cleanup(victim);
        }
        if (this.plugin.getGoldGemManager() != null) {
            this.plugin.getGoldAbilities().cleanup(victim);
            this.plugin.getGoldGemManager().resetProgress(victim);
        }
        this.handleUpgraderChargeLoss(victim);
        this.plugin.getGemManager().updateActiveGem(victim);
        if (killer != null) {
            this.plugin.getGemManager().updateActiveGem(killer);
        }
    }

    private void dropPlayerHead(Player victim) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setPlayerProfile(victim.getPlayerProfile());
            meta.setDisplayName("§e" + victim.getName() + "'s Head");
            head.setItemMeta(meta);
        }
        victim.getWorld().dropItemNaturally(victim.getLocation(), head);
    }

    private void handleUpgraderChargeLoss(Player player) {
        int maxCharges = this.plugin.getConfig().getInt("upgrader.charges", 3);
        boolean foundUpgrader = false;
        for (int i = 0; i < player.getInventory().getSize(); ++i) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null) continue;
            String id = CustomItemManager.getIdByItem(item);
            if (!"gem_upgrader".equals(id)) continue;
            foundUpgrader = true;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            int currentCharges = maxCharges;
            List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            for (String line : lore) {
                String stripped = ChatColor.stripColor(line);
                if (stripped != null && stripped.startsWith("Charges: ")) {
                    try { currentCharges = Integer.parseInt(stripped.substring(9).split("/")[0].trim()); } catch (Exception ignored) {}
                    break;
                }
            }
            currentCharges--;
            if (currentCharges <= 0) {
                player.getInventory().setItem(i, null);
                String gemId = this.plugin.getGemManager().getGemId(player);
                if (gemId != null && this.plugin.getGemManager().getGemTier(player) == 2) {
                    this.plugin.getGemManager().downgradeGem(player, gemId);
                    player.sendMessage("§c§lYour upgrader broke! Your gem reverted to Tier 1.");
                }
            } else {
                boolean found = false;
                for (int j = 0; j < lore.size(); j++) {
                    String stripped = ChatColor.stripColor(lore.get(j));
                    if (stripped != null && stripped.startsWith("Charges: ")) {
                        lore.set(j, "§7Charges: §e" + currentCharges + "§7/§e" + maxCharges);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    lore.add("§7Charges: §e" + currentCharges + "§7/§e" + maxCharges);
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
        if (!foundUpgrader && this.plugin.getGemManager().getGemTier(player) == 2) {
            ItemStack gem = this.plugin.getGemManager().findGemInInventory(player);
            if (gem != null && gem.hasItemMeta()) {
                ItemMeta meta = gem.getItemMeta();
                List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                int currentCharges = maxCharges;
                boolean hasChargeLore = false;
                for (String line : lore) {
                    String stripped = ChatColor.stripColor(line);
                    if (stripped != null && stripped.startsWith("Charges: ")) {
                        hasChargeLore = true;
                        try { currentCharges = Integer.parseInt(stripped.substring(9).split("/")[0].trim()); } catch (Exception ignored) {}
                        break;
                    }
                }
                if (hasChargeLore) {
                    currentCharges--;
                    if (currentCharges <= 0) {
                        String gemId = this.plugin.getGemManager().getGemId(player);
                        this.plugin.getGemManager().downgradeGem(player, gemId);
                        player.sendMessage("§c§lYour Tier 2 gem lost all charges and reverted to Tier 1!");
                    } else {
                        for (int j = 0; j < lore.size(); j++) {
                            String stripped = ChatColor.stripColor(lore.get(j));
                            if (stripped != null && stripped.startsWith("Charges: ")) {
                                lore.set(j, "§7Charges: §e" + currentCharges + "§7/§e" + maxCharges);
                                break;
                            }
                        }
                        meta.setLore(lore);
                        gem.setItemMeta(meta);
                        player.sendMessage("§eYour Tier 2 gem lost a charge! (" + currentCharges + "/" + maxCharges + ")");
                    }
                }
            }
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerDeathProtectGems(PlayerDeathEvent event) {
        if (this.plugin.getGoldGemManager() != null) {
            this.plugin.getGoldGemManager().harvestOnDeath(event);
        }
        if (!this.plugin.getConfig().getBoolean("gems.prevent-drop", true)) {
            return;
        }
        Player victim = event.getEntity();
        ArrayList<ItemStack> gemsToSave = new ArrayList<ItemStack>();
        List droppableOnDeath = this.plugin.getConfig().contains("gems.droppable-on-death") ? this.plugin.getConfig().getStringList("gems.droppable-on-death") : List.of();
        event.getDrops().removeIf(item -> {
            if (CustomItemManager.isUndroppable(item)) {
                if (this.isDroppableOnDeath((ItemStack)item, droppableOnDeath)) {
                    return false;
                }
                gemsToSave.add(item.clone());
                return true;
            }
            return false;
        });
        if (!gemsToSave.isEmpty()) {
            this.savedGems.put(victim.getUniqueId(), gemsToSave);
            this.saveGemsToDisk(victim.getUniqueId(), gemsToSave);
        }
    }

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
        String gemId = id.substring(0, gemMarker);
        for (String entry : droppableGems) {
            String trimmed;
            if (entry == null || !(trimmed = entry.trim()).equalsIgnoreCase(gemId) && !trimmed.equalsIgnoreCase(id)) continue;
            return true;
        }
        return false;
    }

    public void validateDroppableOnDeathConfig() {
        List<String> configured = this.plugin.getConfig().getStringList("gems.droppable-on-death");
        for (String entry : configured) {
            if (entry == null || entry.trim().isEmpty()) continue;
            String gemId = entry.trim().toLowerCase();
            int gemMarker = gemId.indexOf("_gem_t");
            if (gemMarker > 0) {
                gemId = gemId.substring(0, gemMarker);
            }
            if (this.plugin.getGemRegistry() != null && this.plugin.getGemRegistry().getGem(gemId) != null) continue;
            this.plugin.getLogger().warning("gems.droppable-on-death lists '" + entry + "', which is not a known gem - it will never drop. Use the plain gem id (e.g. auratus, heretic).");
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        List<ItemStack> gems = null;
        if (this.savedGems.containsKey(playerId)) {
            gems = this.savedGems.remove(playerId);
            this.clearGemsFromDisk(playerId);
        } else {
            gems = this.loadGemsFromDisk(playerId);
        }
        if (gems != null && !gems.isEmpty()) {
            List<ItemStack> toRestore;
            boolean singleGemOnly = this.plugin.getConfig().getBoolean("gems.single-gem-only", true);
            List<ItemStack> list = toRestore = singleGemOnly && gems.size() > 1 ? List.of(gems.get(0)) : gems;
            if (singleGemOnly && gems.size() > 1) {
                this.plugin.getLogger().warning("Player " + player.getName() + " had " + gems.size() + " gems saved! Only returning first gem due to single-gem-only config.");
            }
            for (ItemStack gem : toRestore) {
                if (this.playerAlreadyHasGem(player, gem)) continue;
                player.getInventory().addItem(new ItemStack[]{gem});
            }
        }
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (player.isOnline()) {
                this.enforceOneGemOnly(player);
                this.plugin.getGemManager().updateActiveGem(player);
            }
        }, 1L);
    }

    private void dropEnergyBottle(Location location) {
        ItemStack bottle = CustomItemManager.getItemById("energy_bottle");
        if (bottle != null) {
            location.getWorld().dropItemNaturally(location, bottle);
        }
    }

    private void dropUpgrader(Location location) {
        ItemStack upgrader = CustomItemManager.getItemById("gem_upgrader");
        if (upgrader != null) {
            location.getWorld().dropItemNaturally(location, upgrader);
        }
    }

    private void saveGemsToDisk(UUID playerId, List<ItemStack> gems) {
        File file;
        File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        YamlConfiguration data = (file = new File(dataFolder, String.valueOf(playerId) + ".yml")).exists() ? YamlConfiguration.loadConfiguration((File)file) : new YamlConfiguration();
        data.set("saved-gems", gems);
        try {
            data.save(file);
        }
        catch (IOException e) {
            this.plugin.getLogger().severe("Failed to save gems for player " + String.valueOf(playerId) + ": " + e.getMessage());
        }
    }

    private List<ItemStack> loadGemsFromDisk(UUID playerId) {
        File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
        File file = new File(dataFolder, String.valueOf(playerId) + ".yml");
        if (!file.exists()) {
            return null;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration((File)file);
        List gems = (List)data.get("saved-gems");
        if (gems != null) {
            data.set("saved-gems", null);
            try {
                data.save(file);
            }
            catch (IOException e) {
                this.plugin.getLogger().warning("Failed to clear saved gems from disk for " + String.valueOf(playerId));
            }
        }
        return gems;
    }

    private void clearGemsFromDisk(UUID playerId) {
        File dataFolder = new File(this.plugin.getDataFolder(), "playerdata");
        File file = new File(dataFolder, String.valueOf(playerId) + ".yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration((File)file);
        if (data.contains("saved-gems")) {
            data.set("saved-gems", null);
            try {
                data.save(file);
            }
            catch (IOException e) {
                this.plugin.getLogger().warning("Failed to clear saved gems from disk for " + String.valueOf(playerId));
            }
        }
    }

    private boolean playerAlreadyHasGem(Player player, ItemStack gem) {
        String gemId = CustomItemManager.getIdByItem(gem);
        if (gemId == null) {
            return false;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !gemId.equals(CustomItemManager.getIdByItem(item))) continue;
            return true;
        }
        return false;
    }

    private void enforceOneGemOnly(Player player) {
        if (!this.plugin.getConfigManager().isSingleGemOnly()) {
            return;
        }
        boolean foundFirst = false;
        for (int i = 0; i < player.getInventory().getSize(); ++i) {
            String itemId;
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || (itemId = CustomItemManager.getIdByItem(item)) == null || !this.plugin.getGemManager().isAnyGem(itemId)) continue;
            if (!foundFirst) {
                foundFirst = true;
                continue;
            }
            player.getInventory().setItem(i, null);
            this.plugin.getLogger().info("Removed duplicate gem from " + player.getName() + " on respawn.");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.plugin.getGemManager().updateActiveGem(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.plugin.getAstraAbilities().cleanup(player);
        this.plugin.getFireAbilities().cleanup(player);
        this.plugin.getFluxAbilities().cleanup(player);
        this.plugin.getSpeedAbilities().cleanup(player.getUniqueId());
        this.plugin.getWealthAbilities().cleanup(player);
        this.plugin.getLifeAbilities().cleanup(player);
        this.plugin.getSoulManager().cleanup(player);
        this.plugin.getSoulManager().clearSouls(player.getUniqueId());
        this.plugin.getEnergyManager().clearCache(player.getUniqueId());
        this.plugin.getGemManager().clearCache(player.getUniqueId());
        this.plugin.getAbilityManager().clearCache(player.getUniqueId());
    }
}

