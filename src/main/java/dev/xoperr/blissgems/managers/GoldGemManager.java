package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemPassiveHandler;
import dev.xoperr.blissgems.api.GemRegistry;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.EnergyState;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;
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
 * awakening does not. Dying hands every stolen gem back to the player it was taken from and
 * leaves the holder with a broken gem, so the whole hunt has to be done again from scratch.
 */
public class GoldGemManager {

    /** The item id the Gold Gem is registered under. */
    public static final String GOLD_ITEM_ID = "gold_gem_t1";

    /** Eight souls become one - the number of harvests that fully awakens the gem. */
    public static final int SOULS_TO_AWAKEN = 8;

    /**
     * CustomModelData of the dormant gem. Each harvested soul adds one, so the pack can show
     * the gem taking on the colours of what it has eaten (1009 dormant .. 1017 awakened).
     */
    public static final int BASE_MODEL_DATA = 1009;

    private final BlissGems plugin;

    // Harvested souls per holder: gem id -> what was taken, and from whom.
    private final Map<UUID, Map<String, Harvest>> harvested = new HashMap<>();
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
        // The hands are checked by themselves first: gems normally sit in the offhand, and
        // getContents() has not been reliable about including it across API versions.
        if (this.isGoldGem(player.getInventory().getItemInOffHand())
            || this.isGoldGem(player.getInventory().getItemInMainHand())) {
            return true;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (this.isGoldGem(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean isGoldGem(ItemStack item) {
        return item != null && GOLD_ITEM_ID.equals(CustomItemManager.getIdByItem(item));
    }

    /**
     * A gem torn out of a victim: which gem it was, the tier it was at, and who it was taken
     * from. The owner is remembered because the souls do not stay stolen forever - when the
     * holder dies, every gem goes back to the player it was taken from.
     */
    public record Harvest(String gemId, int tier, UUID owner) {}

    /** Souls this player has harvested, keyed by gem id, in the order they were taken. */
    public Map<String, Harvest> getHarvested(UUID playerId) {
        return this.harvested.getOrDefault(playerId, Map.of());
    }

    /** The gem id whose abilities the Gold Gem currently casts, or null if none is selected. */
    public String getActive(UUID playerId) {
        return this.active.get(playerId);
    }

    /** The tier of the selected soul, or 1 if nothing is selected. */
    public int getActiveTier(UUID playerId) {
        String gemId = this.active.get(playerId);
        Harvest soul = gemId != null ? this.getHarvested(playerId).get(gemId) : null;
        return soul != null ? soul.tier() : 1;
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
        if (!this.canHarvest(killer, victim)) {
            return;
        }
        Harvest taken = this.takeGem(victim, event.getDrops());
        if (taken == null) {
            return;
        }
        this.absorb(killer, victim, taken);
    }

    /**
     * True if this killer could tear a gem out of this victim. Checked by the harvest
     * ceremony before it intercepts a fatal blow, and again by the death handler for kills
     * the ceremony never saw (fall damage finishing a fight, /kill, a disabled ceremony).
     */
    public boolean canHarvest(Player killer, Player victim) {
        if (killer == null || victim == null || killer.equals(victim) || !this.holdsGoldGem(killer)) {
            return false;
        }
        // A broken gem holds nothing worth taking.
        return this.plugin.getEnergyManager().getEnergyState(victim) != EnergyState.BROKEN;
    }

    /**
     * The gem this victim would give up, without taking it. The ceremony needs to know what
     * it is showing before the animation starts, but must not strip the victim until the
     * animation has actually finished.
     */
    public Harvest peekGem(Player victim) {
        UUID owner = victim.getUniqueId();
        for (int slot = 0; slot < victim.getInventory().getSize(); slot++) {
            Harvest found = this.describe(victim.getInventory().getItem(slot), owner);
            if (found != null) {
                return found;
            }
        }
        return this.describe(victim.getInventory().getItemInOffHand(), owner);
    }

    /**
     * Tear the victim's gem out of their inventory (and, on death, out of their drops).
     * Returns what was taken, or null if they had nothing harvestable.
     *
     * @param drops the death drop list, or null when the gem is taken from a living player
     */
    public Harvest takeGem(Player victim, List<ItemStack> drops) {
        Harvest taken = null;
        UUID owner = victim.getUniqueId();

        // Pull the gem out of the drops first, then sweep the inventory in case a
        // keep-inventory setup meant it never reached the drop list.
        if (drops != null) {
            for (ItemStack item : new ArrayList<>(drops)) {
                Harvest found = this.describe(item, owner);
                if (found != null) {
                    taken = found;
                    drops.remove(item);
                    break;
                }
            }
        }
        // Slot-indexed rather than Inventory#remove: the offhand is where gems normally sit
        // and both getContents() and remove() have been unreliable about reaching it, which
        // left the victim holding the very gem that was supposed to have been torn out.
        for (int slot = 0; slot < victim.getInventory().getSize(); slot++) {
            Harvest found = this.describe(victim.getInventory().getItem(slot), owner);
            if (found != null) {
                if (taken == null) {
                    taken = found;
                }
                victim.getInventory().setItem(slot, null);
            }
        }
        Harvest offhand = this.describe(victim.getInventory().getItemInOffHand(), owner);
        if (offhand != null) {
            if (taken == null) {
                taken = offhand;
            }
            victim.getInventory().setItemInOffHand(null);
        }

        if (taken != null) {
            this.plugin.getGemManager().updateActiveGem(victim);
        }
        return taken;
    }

    /**
     * Add a taken gem to the killer's awakening: bookkeeping, the shattering burst at the
     * victim's feet, the reskinned Gold Gem and the announcements.
     */
    public void absorb(Player killer, Player victim, Harvest taken) {
        this.harvested.computeIfAbsent(killer.getUniqueId(), id -> new LinkedHashMap<>())
            .put(taken.gemId(), taken);
        // The first soul taken becomes the active one so the gem is usable straight away.
        this.active.putIfAbsent(killer.getUniqueId(), taken.gemId());
        this.save(killer.getUniqueId());

        this.shatter(victim.getLocation(), taken.gemId());
        this.refreshGoldItem(killer);

        String message = this.plugin.getConfigManager().getMessage("gold-soul-repurposed");
        if (message != null && !message.isEmpty()) {
            Bukkit.broadcastMessage(message.replace("{player}", victim.getName()));
        }
        killer.playSound(killer.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 0.7F);
        int soulCount = this.getHarvested(killer.getUniqueId()).size();
        killer.sendMessage(this.plugin.getConfigManager().getMessage("gold-soul-harvested")
            .replace("{gem}", this.plugin.getGemManager().getGemDisplayName(taken.gemId()))
            .replace("{count}", String.valueOf(soulCount)));

        // Eight souls become one - the gem is fully awake and everyone should know.
        if (soulCount >= SOULS_TO_AWAKEN) {
            String awakened = this.plugin.getConfigManager().getMessage("gold-awakened");
            if (awakened != null && !awakened.isEmpty()) {
                Bukkit.broadcastMessage(awakened.replace("{player}", killer.getName()));
            }
        }
    }

    /** Describe a stack as a harvestable gem, or null if it is not one. */
    private Harvest describe(ItemStack item, UUID owner) {
        String gemId = this.harvestableGemId(item);
        if (gemId == null) {
            return null;
        }
        int tier = this.plugin.getGemRegistry().tierFromItemId(CustomItemManager.getIdByItem(item));
        return new Harvest(gemId, tier, owner);
    }

    /**
     * Rewrite the Gold Gem in the holder's inventory so the item itself shows how far the
     * awakening has come: the souls it carries, each in its own colour, and a model that
     * steps up with every soul taken.
     */
    private void refreshGoldItem(Player player) {
        Map<String, Harvest> souls = this.getHarvested(player.getUniqueId());
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            this.refreshGoldStack(player.getInventory().getItem(slot), souls);
        }
        this.refreshGoldStack(player.getInventory().getItemInOffHand(), souls);
    }

    private void refreshGoldStack(ItemStack item, Map<String, Harvest> souls) {
        if (!this.isGoldGem(item)) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        List<String> lore = new ArrayList<>();
        lore.add("§f§lWATCH THE LINES OF REALITY FRAY AS EIGHT SOULS BECOME ONE");
        lore.add(this.stateLine(souls.size()));
        lore.add("");
        lore.add("§6🌟 §6§lHARVESTED SOULS");
        if (souls.isEmpty()) {
            lore.add("§8- the gem is silent -");
        } else {
            lore.add(this.colourBar(souls.keySet()));
            for (Map.Entry<String, Harvest> soul : souls.entrySet()) {
                lore.add(this.plugin.getGemManager().getGemColorCode(soul.getKey()) + "- "
                    + this.plugin.getGemManager().getGemDisplayName(soul.getKey())
                    + " §8(T" + soul.getValue().tier() + ")");
            }
        }
        meta.setLore(lore);
        // Every soul steps the model up one, so the pack can colour the gem's centre with
        // what it has eaten. Falls back to the dormant model if the pack has no variant.
        meta.setCustomModelData(BASE_MODEL_DATA + Math.min(souls.size(), SOULS_TO_AWAKEN));
        item.setItemMeta(meta);
    }

    /** One pip per harvested soul, each in that gem's colour. */
    private String colourBar(Iterable<String> gemIds) {
        StringBuilder bar = new StringBuilder();
        for (String gemId : gemIds) {
            bar.append(this.plugin.getGemManager().getGemColorCode(gemId)).append("❖");
        }
        return bar.toString();
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

    /** Scatter the shattered gem's fragments at the victim's feet, in that gem's own colour. */
    private void shatter(Location location, String harvestedId) {
        if (location.getWorld() == null) {
            return;
        }
        // The burst is drawn in the taken gem's colour, so a kill reads as "that colour just
        // went into the Gold Gem".
        location.getWorld().spawnParticle(Particle.DUST, location.clone().add(0.0, 1.0, 0.0),
            40, 0.4, 0.6, 0.4, 0.0,
            new Particle.DustOptions(this.soulColour(harvestedId), 1.8F));
        location.getWorld().playSound(location, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.2F, 0.6F);

        int amount = this.plugin.getConfig().getInt("gold.shards-per-harvest", 3);
        if (amount <= 0) {
            return;
        }
        ItemStack fragment = CustomItemManager.getItemById("gem_fragment");
        if (fragment == null) {
            return;
        }
        fragment.setAmount(amount);
        location.getWorld().dropItemNaturally(location, fragment);
    }

    /** Particle colour of a harvested gem, shared with the ritual so the colours match. */
    public Color soulColour(String gemId) {
        if (this.plugin.getGemRitualManager() == null) {
            return Color.fromRGB(255, 215, 0);
        }
        return this.plugin.getGemRitualManager().getGemColor(gemId);
    }

    /**
     * Wipe a holder's awakening. The Gold Gem itself is soulbound and stays with them, but
     * dying costs them everything it had absorbed: every stolen gem goes back to the player
     * it was taken from, and the gem they are left holding is broken - no passives, no
     * abilities, until a Restoration Book brings it back.
     */
    public void resetProgress(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, Harvest> souls = this.getHarvested(playerId);
        boolean hadSouls = !souls.isEmpty();
        if (hadSouls && this.plugin.getConfig().getBoolean("gold.death.return-souls", true)) {
            for (Harvest soul : new ArrayList<>(souls.values())) {
                this.returnSoul(player, soul);
            }
        }
        this.harvested.remove(playerId);
        this.active.remove(playerId);
        this.save(playerId);
        this.refreshGoldItem(player);
        this.clearTrims(player);
        if (hadSouls) {
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-progress-reset"));
        }
        // Breaking the gem is what makes a death actually cost something: the holder keeps
        // the item but none of its power until they restore it.
        if (this.plugin.getConfig().getBoolean("gold.death.break-gem", true) && this.holdsGoldGem(player)) {
            this.plugin.getEnergyManager().setEnergy(player, 0);
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-gem-broken"));
        }
    }

    /**
     * Hand a stolen gem back to the player it was taken from. An owner who is offline gets it
     * queued to their playerdata and handed over the next time they join - a gem must never
     * be lost just because its owner happened not to be watching.
     */
    private void returnSoul(Player holder, Harvest soul) {
        if (soul.owner() == null || soul.owner().equals(holder.getUniqueId())) {
            return;
        }
        String itemId = soul.gemId() + "_gem_t" + soul.tier();
        Player owner = this.plugin.getServer().getPlayer(soul.owner());
        if (owner != null && owner.isOnline()) {
            // Straight into the offhand where gem resolution looks first, so the returned gem
            // starts working the moment it lands.
            this.plugin.getGemManager().giveGemToOffhand(owner, soul.gemId(), soul.tier());
            owner.sendMessage(this.plugin.getConfigManager().getMessage("gold-soul-returned")
                .replace("{player}", holder.getName())
                .replace("{gem}", this.plugin.getGemManager().getGemDisplayName(soul.gemId())));
            return;
        }
        this.queuePendingGem(soul.owner(), itemId);
    }

    private void queuePendingGem(UUID ownerId, String itemId) {
        File file = this.getPlayerFile(ownerId);
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        List<String> pending = data.getStringList("gold.pending-return");
        pending.add(itemId);
        data.set("gold.pending-return", pending);
        try {
            data.save(file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Failed to queue a returned gem for " + ownerId + ": " + e.getMessage());
        }
    }

    /**
     * Hand over any gems that were returned to this player while they were offline. Called
     * from the join handler, right after their Gold Gem state is loaded.
     */
    public void deliverPendingGems(Player player) {
        File file = this.getPlayerFile(player.getUniqueId());
        if (!file.exists()) {
            return;
        }
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        List<String> pending = data.getStringList("gold.pending-return");
        if (pending.isEmpty()) {
            return;
        }
        for (String itemId : pending) {
            int marker = itemId.indexOf("_gem_t");
            if (marker <= 0) {
                continue;
            }
            String gemId = itemId.substring(0, marker);
            int tier = "2".equals(itemId.substring(marker + 6)) ? 2 : 1;
            this.plugin.getGemManager().giveGemToOffhand(player, gemId, tier);
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-soul-returned")
                .replace("{player}", "a fallen Gold Gem")
                .replace("{gem}", this.plugin.getGemManager().getGemDisplayName(gemId)));
        }
        data.set("gold.pending-return", null);
        try {
            data.save(file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Failed to clear returned gems for " + player.getName() + ": " + e.getMessage());
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
                    if (player.isDead() || !GoldGemManager.this.holdsGoldGem(player)) {
                        continue;
                    }
                    // The gilding marks the holder whether or not the gem has woken up yet.
                    GoldGemManager.this.applyTrims(player);
                    if (!GoldGemManager.this.getHarvested(player.getUniqueId()).isEmpty()) {
                        GoldGemManager.this.applyHarvestedPassives(player);
                    }
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
        for (Map.Entry<String, Harvest> soul : this.getHarvested(player.getUniqueId()).entrySet()) {
            GemPassiveHandler handler = registry.getPassiveHandler(soul.getKey());
            if (handler != null) {
                handler.applyPassives(player, soul.getValue().tier());
            }
        }
        this.drawSoulAura(player);
    }

    /**
     * A ring of the collected gems' colours around the holder: the fuller the awakening, the
     * more colours orbit them, so other players can read how far along a Gold Gem is.
     */
    private void drawSoulAura(Player player) {
        List<String> souls = new ArrayList<>(this.getHarvested(player.getUniqueId()).keySet());
        if (souls.isEmpty() || player.getWorld() == null) {
            return;
        }
        Location centre = player.getLocation().add(0.0, 1.0, 0.0);
        int points = souls.size() * 3;
        for (int i = 0; i < points; i++) {
            double angle = 2.0 * Math.PI * i / points;
            Location at = centre.clone().add(Math.cos(angle) * 0.8, 0.0, Math.sin(angle) * 0.8);
            player.getWorld().spawnParticle(Particle.DUST, at, 1, 0.0, 0.15, 0.0, 0.0,
                new Particle.DustOptions(this.soulColour(souls.get(i % souls.size())), 0.9F));
        }
    }

    // ========================================================================
    // Gold trim
    // ========================================================================

    /**
     * Gild the holder's armour. A Gold Gem should be visible on the player carrying it, so
     * every worn piece takes a gold trim for as long as they have the gem.
     *
     * Only untrimmed pieces are touched, and the ones this does trim are marked, so removing
     * the gilding later can never strip a trim the player applied themselves at a smithing
     * table.
     */
    private void applyTrims(Player player) {
        if (!this.plugin.getConfig().getBoolean("gold.armour-trim.enabled", true)) {
            return;
        }
        TrimPattern pattern = this.trimPattern();
        ItemStack[] armour = player.getInventory().getArmorContents();
        boolean changed = false;
        for (ItemStack piece : armour) {
            if (piece == null || !(piece.getItemMeta() instanceof ArmorMeta meta) || meta.hasTrim()) {
                continue;
            }
            meta.setTrim(new ArmorTrim(TrimMaterial.GOLD, pattern));
            meta.getPersistentDataContainer().set(this.trimKey(), PersistentDataType.BYTE, (byte) 1);
            piece.setItemMeta(meta);
            changed = true;
        }
        if (changed) {
            player.getInventory().setArmorContents(armour);
        }
    }

    /** Strip the gilding this plugin applied, leaving any trim the player chose themselves. */
    private void clearTrims(Player player) {
        ItemStack[] armour = player.getInventory().getArmorContents();
        boolean changed = false;
        for (ItemStack piece : armour) {
            if (piece == null || !(piece.getItemMeta() instanceof ArmorMeta meta)) {
                continue;
            }
            if (!meta.getPersistentDataContainer().has(this.trimKey(), PersistentDataType.BYTE)) {
                continue;
            }
            meta.setTrim(null);
            meta.getPersistentDataContainer().remove(this.trimKey());
            piece.setItemMeta(meta);
            changed = true;
        }
        if (changed) {
            player.getInventory().setArmorContents(armour);
        }
    }

    private NamespacedKey trimKey() {
        return new NamespacedKey(this.plugin, "gold_trim");
    }

    /** The trim pattern from config, falling back to Sentry if the name is not a real pattern. */
    private TrimPattern trimPattern() {
        String name = this.plugin.getConfig().getString("gold.armour-trim.pattern", "SENTRY");
        TrimPattern pattern = Registry.TRIM_PATTERN.get(NamespacedKey.minecraft(name.toLowerCase()));
        return pattern != null ? pattern : TrimPattern.SENTRY;
    }

    // ========================================================================
    // Summoning
    // ========================================================================

    private File getSummonFile() {
        return new File(this.plugin.getDataFolder(), "gold.yml");
    }

    /**
     * True if a Gold Gem has already been summoned on this server. With
     * {@code gold.summon.once-per-server} on, the craft is a one-time event: the gem that
     * exists is the only one there will ever be, and losing track of it is permanent.
     */
    public boolean isSummoned() {
        return YamlConfiguration.loadConfiguration(this.getSummonFile()).getBoolean("summoned", false);
    }

    /** Record that the one Gold Gem has been crafted. */
    public void markSummoned(Player summoner) {
        File file = this.getSummonFile();
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        data.set("summoned", true);
        data.set("summoned-by", summoner.getUniqueId().toString());
        data.set("summoned-by-name", summoner.getName());
        data.set("summoned-at", System.currentTimeMillis());
        try {
            data.save(file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Failed to record the Gold Gem summon: " + e.getMessage());
        }
    }

    // ========================================================================
    // Persistence
    // ========================================================================

    /**
     * Parse one stored soul. Written as "<gemId>:<tier>:<ownerUuid>"; entries saved before
     * owners were tracked have no third field and come back with a null owner, which simply
     * means that gem has nobody to be returned to.
     */
    private Harvest deserialise(String stored) {
        String[] parts = stored.split(":");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return null;
        }
        int tier = 1;
        if (parts.length > 1) {
            try {
                tier = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
                tier = 1;
            }
        }
        UUID owner = null;
        if (parts.length > 2) {
            try {
                owner = UUID.fromString(parts[2]);
            } catch (IllegalArgumentException ignored) {
                owner = null;
            }
        }
        return new Harvest(parts[0], tier, owner);
    }

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
            Map<String, Harvest> parsed = new LinkedHashMap<>();
            for (String soul : souls) {
                Harvest entry = this.deserialise(soul);
                if (entry != null) {
                    parsed.put(entry.gemId(), entry);
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
        Map<String, Harvest> souls = this.getHarvested(playerId);
        if (souls.isEmpty()) {
            data.set("gold.harvested", null);
            data.set("gold.active", null);
        } else {
            List<String> serialised = new ArrayList<>();
            for (Harvest soul : souls.values()) {
                serialised.add(soul.gemId() + ":" + soul.tier()
                    + (soul.owner() != null ? ":" + soul.owner() : ""));
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
