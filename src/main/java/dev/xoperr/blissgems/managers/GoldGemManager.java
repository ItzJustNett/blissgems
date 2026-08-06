package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemPassiveHandler;
import dev.xoperr.blissgems.api.GemRegistry;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.EnergyState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks the Gold Gem's awakening: which gems its holder has harvested from their
 * victims, and which of those souls is currently selected.
 *
 * The Gold Gem itself is soulbound - it survives death like any other gem - but the
 * awakening does not. Dying wipes every harvested soul, so the holder has to hunt the
 * gems down again.
 */
public class GoldGemManager {

    /** The item id the Gold Gem is registered under. */
    public static final String GOLD_ITEM_ID = "gold_gem_t1";

    /** Eight souls become one - the number of harvests that fully awakens the gem. */
    public static final int SOULS_TO_AWAKEN = 8;

    private final BlissGems plugin;

    // Harvested souls per holder: gem id -> the tier that gem was at when it was taken.
    private final Map<UUID, Map<String, Integer>> harvested = new HashMap<>();
    // The soul whose abilities the Gold Gem currently channels.
    private final Map<UUID, String> active = new HashMap<>();

    public GoldGemManager(BlissGems plugin) {
        this.plugin = plugin;
        this.startPassiveTask();
    }

    // ========================================================================
    // Holder state
    // ========================================================================

    /**
     * True if this player is carrying the Gold Gem anywhere in their inventory.
     * Unlike normal gems the Gold Gem does not have to be held in main or offhand -
     * its harvested passives run as long as it is on the player at all.
     */
    public boolean holdsGoldGem(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && GOLD_ITEM_ID.equals(CustomItemManager.getIdByItem(item))) {
                return true;
            }
        }
        return false;
    }

    /** Gem ids this player has harvested, in the order they were taken. */
    public Map<String, Integer> getHarvested(UUID playerId) {
        return this.harvested.getOrDefault(playerId, Map.of());
    }

    /** The gem id whose abilities the Gold Gem currently casts, or null if none is selected. */
    public String getActive(UUID playerId) {
        return this.active.get(playerId);
    }

    /** The tier of the selected soul, or 1 if nothing is selected. */
    public int getActiveTier(UUID playerId) {
        String gemId = this.active.get(playerId);
        if (gemId == null) {
            return 1;
        }
        return this.getHarvested(playerId).getOrDefault(gemId, 1);
    }

    /**
     * Select a harvested soul. Returns false if the player never harvested that gem.
     */
    public boolean setActive(Player player, String gemId) {
        if (!this.getHarvested(player.getUniqueId()).containsKey(gemId)) {
            return false;
        }
        this.active.put(player.getUniqueId(), gemId);
        this.save(player.getUniqueId());
        return true;
    }

    // ========================================================================
    // Harvesting
    // ========================================================================

    /**
     * Called from the death handler before gem drop-protection runs. If the killer carries
     * the Gold Gem and the victim died holding a normal gem, that gem is torn out of the
     * victim's inventory and drops, shattered into fragments, and its soul is added to the
     * killer's awakening.
     *
     * Runs before drop-protection on purpose: a harvested gem must not end up in the
     * victim's saved-gems list, or it would be handed straight back to them on respawn.
     */
    public void harvestOnDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim) || !this.holdsGoldGem(killer)) {
            return;
        }
        // A broken gem holds nothing worth taking.
        if (this.plugin.getEnergyManager().getEnergyState(victim) == EnergyState.BROKEN) {
            return;
        }

        String harvestedId = null;
        int harvestedTier = 1;

        // Pull the gem out of the drops first, then sweep the inventory in case a
        // keep-inventory setup meant it never reached the drop list.
        for (ItemStack item : new ArrayList<>(event.getDrops())) {
            String gemId = this.harvestableGemId(item);
            if (gemId != null) {
                harvestedId = gemId;
                harvestedTier = this.plugin.getGemRegistry().tierFromItemId(CustomItemManager.getIdByItem(item));
                event.getDrops().remove(item);
                break;
            }
        }
        for (ItemStack item : victim.getInventory().getContents()) {
            String gemId = this.harvestableGemId(item);
            if (gemId != null) {
                if (harvestedId == null) {
                    harvestedId = gemId;
                    harvestedTier = this.plugin.getGemRegistry().tierFromItemId(CustomItemManager.getIdByItem(item));
                }
                victim.getInventory().remove(item);
            }
        }

        if (harvestedId == null) {
            return;
        }

        this.harvested.computeIfAbsent(killer.getUniqueId(), id -> new LinkedHashMap<>())
            .put(harvestedId, harvestedTier);
        // The first soul taken becomes the active one so the gem is usable straight away.
        this.active.putIfAbsent(killer.getUniqueId(), harvestedId);
        this.save(killer.getUniqueId());

        this.shatter(victim.getLocation());
        this.refreshGoldItem(killer);

        String message = this.plugin.getConfigManager().getMessage("gold-soul-repurposed");
        if (message != null && !message.isEmpty()) {
            Bukkit.broadcastMessage(message.replace("{player}", victim.getName()));
        }
        killer.playSound(killer.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 0.7F);
        int soulCount = this.getHarvested(killer.getUniqueId()).size();
        killer.sendMessage(this.plugin.getConfigManager().getMessage("gold-soul-harvested")
            .replace("{gem}", this.plugin.getGemManager().getGemDisplayName(harvestedId))
            .replace("{count}", String.valueOf(soulCount)));

        // Eight souls become one - the gem is fully awake and everyone should know.
        if (soulCount >= SOULS_TO_AWAKEN) {
            String awakened = this.plugin.getConfigManager().getMessage("gold-awakened");
            if (awakened != null && !awakened.isEmpty()) {
                Bukkit.broadcastMessage(awakened.replace("{player}", killer.getName()));
            }
        }
    }

    /**
     * Rewrite the Gold Gem's lore in the holder's inventory so the item itself shows how far
     * the awakening has come, and which souls it is carrying.
     */
    private void refreshGoldItem(Player player) {
        Map<String, Integer> souls = this.getHarvested(player.getUniqueId());
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !GOLD_ITEM_ID.equals(CustomItemManager.getIdByItem(item))) {
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                continue;
            }
            List<String> lore = new ArrayList<>();
            lore.add("§f§lWATCH THE LINES OF REALITY FRAY AS EIGHT SOULS BECOME ONE");
            lore.add(this.stateLine(souls.size()));
            lore.add("");
            lore.add("§6🌟 §6§lHARVESTED SOULS");
            if (souls.isEmpty()) {
                lore.add("§8- the gem is silent -");
            } else {
                for (Map.Entry<String, Integer> soul : souls.entrySet()) {
                    lore.add("§7- " + this.plugin.getGemManager().getGemDisplayName(soul.getKey())
                        + " §8(T" + soul.getValue() + ")");
                }
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    private String stateLine(int soulCount) {
        if (soulCount <= 0) {
            return "§6(Dormant)";
        }
        if (soulCount >= SOULS_TO_AWAKEN) {
            return "§6§l(Awakened)";
        }
        return "§6(Awakening — " + soulCount + "/" + SOULS_TO_AWAKEN + ")";
    }

    /**
     * The gem id of a harvestable gem item, or null if this stack is not one. The Gold Gem
     * cannot harvest itself, and mythics are left alone - only the eight normal gems feed it.
     */
    private String harvestableGemId(ItemStack item) {
        if (item == null) {
            return null;
        }
        String itemId = CustomItemManager.getIdByItem(item);
        if (itemId == null || GOLD_ITEM_ID.equals(itemId)) {
            return null;
        }
        String gemId = this.plugin.getGemRegistry().gemIdFromItemId(itemId);
        if (gemId == null || GemManager.builtInType(gemId) == null) {
            return null;
        }
        return gemId;
    }

    /** Scatter the shattered gem's fragments at the victim's feet. */
    private void shatter(Location location) {
        int amount = this.plugin.getConfig().getInt("gold.shards-per-harvest", 3);
        if (amount <= 0 || location.getWorld() == null) {
            return;
        }
        ItemStack fragment = CustomItemManager.getItemById("gem_fragment");
        if (fragment == null) {
            return;
        }
        fragment.setAmount(amount);
        location.getWorld().dropItemNaturally(location, fragment);
        location.getWorld().playSound(location, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.2F, 0.6F);
    }

    /**
     * Wipe a holder's awakening. The Gold Gem is kept on death like every other gem, but
     * everything it had absorbed is lost and has to be re-harvested.
     */
    public void resetProgress(Player player) {
        UUID playerId = player.getUniqueId();
        boolean hadSouls = !this.getHarvested(playerId).isEmpty();
        this.harvested.remove(playerId);
        this.active.remove(playerId);
        this.save(playerId);
        this.refreshGoldItem(player);
        if (hadSouls) {
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-progress-reset"));
        }
    }

    // ========================================================================
    // Harvested passives
    // ========================================================================

    /**
     * Harvested passives run on their own timer rather than through PassiveManager, because
     * PassiveManager only ticks players holding a gem in their offhand and the Gold Gem
     * grants its stolen passives from anywhere in the inventory.
     */
    private void startPassiveTask() {
        int interval = this.plugin.getConfigManager().getPassiveUpdateInterval();
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : GoldGemManager.this.plugin.getServer().getOnlinePlayers()) {
                    if (player.isDead()) {
                        continue;
                    }
                    if (GoldGemManager.this.getHarvested(player.getUniqueId()).isEmpty()) {
                        continue;
                    }
                    if (!GoldGemManager.this.holdsGoldGem(player)) {
                        continue;
                    }
                    GoldGemManager.this.applyHarvestedPassives(player);
                }
            }
        }.runTaskTimer((Plugin) this.plugin, interval, interval);
    }

    /** Run every harvested gem's passive handler on the holder. */
    private void applyHarvestedPassives(Player player) {
        if (this.plugin.getGemLockManager() != null && this.plugin.getGemLockManager().isLocked(player)) {
            return;
        }
        if (this.plugin.getRegionManager() != null && this.plugin.getRegionManager().areGemsDisabled(player)) {
            return;
        }
        GemRegistry registry = this.plugin.getGemRegistry();
        if (registry == null) {
            return;
        }
        for (Map.Entry<String, Integer> soul : this.getHarvested(player.getUniqueId()).entrySet()) {
            GemPassiveHandler handler = registry.getPassiveHandler(soul.getKey());
            if (handler != null) {
                handler.applyPassives(player, soul.getValue());
            }
        }
    }

    // ========================================================================
    // Persistence
    // ========================================================================

    private File getPlayerFile(UUID playerId) {
        File folder = new File(this.plugin.getDataFolder(), "playerdata");
        folder.mkdirs();
        return new File(folder, playerId + ".yml");
    }

    public void load(UUID playerId) {
        File file = this.getPlayerFile(playerId);
        if (!file.exists()) {
            return;
        }
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        List<String> souls = data.getStringList("gold.harvested");
        if (!souls.isEmpty()) {
            Map<String, Integer> parsed = new LinkedHashMap<>();
            for (String soul : souls) {
                // Stored as "<gemId>:<tier>" so a harvested Tier 2 keeps its stronger passives.
                int split = soul.lastIndexOf(':');
                if (split <= 0) {
                    parsed.put(soul, 1);
                } else {
                    try {
                        parsed.put(soul.substring(0, split), Integer.parseInt(soul.substring(split + 1)));
                    } catch (NumberFormatException ignored) {
                        parsed.put(soul.substring(0, split), 1);
                    }
                }
            }
            this.harvested.put(playerId, parsed);
        }
        String selected = data.getString("gold.active");
        if (selected != null) {
            this.active.put(playerId, selected);
        }
    }

    public void save(UUID playerId) {
        File file = this.getPlayerFile(playerId);
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        Map<String, Integer> souls = this.getHarvested(playerId);
        if (souls.isEmpty()) {
            data.set("gold.harvested", null);
            data.set("gold.active", null);
        } else {
            List<String> serialised = new ArrayList<>();
            for (Map.Entry<String, Integer> soul : souls.entrySet()) {
                serialised.add(soul.getKey() + ":" + soul.getValue());
            }
            data.set("gold.harvested", serialised);
            data.set("gold.active", this.active.get(playerId));
        }
        try {
            data.save(file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save Gold Gem data for " + playerId + ": " + e.getMessage());
        }
    }

    /** Drop the in-memory state for a player who logged off. */
    public void unload(UUID playerId) {
        this.harvested.remove(playerId);
        this.active.remove(playerId);
    }
}
