/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Registry
 *  org.bukkit.Sound
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ArmorMeta
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.trim.ArmorTrim
 *  org.bukkit.inventory.meta.trim.TrimMaterial
 *  org.bukkit.inventory.meta.trim.TrimPattern
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemPassiveHandler;
import dev.xoperr.blissgems.managers.GemManager;
import dev.xoperr.blissgems.managers.GemRegistryImpl;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.EnergyState;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.CallSite;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
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

public class GoldGemManager {
    public static final String GOLD_ITEM_ID = "gold_gem_t1";
    public static final int SOULS_TO_AWAKEN = 8;
    public static final int BASE_MODEL_DATA = 1009;
    private final BlissGems plugin;
    private final Map<UUID, Map<String, Harvest>> harvested = new HashMap<UUID, Map<String, Harvest>>();
    private final Map<UUID, String> active = new HashMap<UUID, String>();
    private final Map<UUID, UUID> trackedInstance = new HashMap<UUID, UUID>();
    private final Set<UUID> trimsDisabled = new HashSet<UUID>();

    public GoldGemManager(BlissGems plugin) {
        this.plugin = plugin;
        this.startPassiveTask();
    }

    public boolean holdsGoldGem(Player player) {
        ItemStack gem = this.findGoldGem(player);
        if (gem == null) {
            return false;
        }
        this.syncInstance(player, gem);
        return true;
    }

    private ItemStack findGoldGem(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (this.isGoldGem(offhand)) {
            return offhand;
        }
        ItemStack mainhand = player.getInventory().getItemInMainHand();
        if (this.isGoldGem(mainhand)) {
            return mainhand;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (!this.isGoldGem(item)) continue;
            return item;
        }
        return null;
    }

    private void syncInstance(Player player, ItemStack gem) {
        UUID onItem = CustomItemManager.ensureGoldInstanceId(gem);
        if (onItem == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        UUID tracked = this.trackedInstance.get(playerId);
        if (tracked == null) {
            this.trackedInstance.put(playerId, onItem);
            this.save(playerId);
            return;
        }
        if (!tracked.equals(onItem)) {
            this.harvested.remove(playerId);
            this.active.remove(playerId);
            this.trackedInstance.put(playerId, onItem);
            this.save(playerId);
        }
    }

    public boolean isGoldGemInMainHand(Player player) {
        return this.isGoldGem(player.getInventory().getItemInMainHand());
    }

    public boolean isGoldGem(ItemStack item) {
        return item != null && GOLD_ITEM_ID.equals(CustomItemManager.getIdByItem(item));
    }

    public Map<String, Harvest> getHarvested(UUID playerId) {
        this.syncInstanceIfOnline(playerId);
        return this.harvested.getOrDefault(playerId, Map.of());
    }

    public String getActive(UUID playerId) {
        this.syncInstanceIfOnline(playerId);
        return this.active.get(playerId);
    }

    public int getActiveTier(UUID playerId) {
        String gemId = this.getActive(playerId);
        Harvest soul = gemId != null ? this.getHarvested(playerId).get(gemId) : null;
        return soul != null ? soul.tier() : 1;
    }

    private void syncInstanceIfOnline(UUID playerId) {
        Player player = Bukkit.getPlayer((UUID)playerId);
        if (player == null) {
            return;
        }
        ItemStack gem = this.findGoldGem(player);
        if (gem != null) {
            this.syncInstance(player, gem);
        }
    }

    public boolean setActive(Player player, String gemId) {
        if (!this.getHarvested(player.getUniqueId()).containsKey(gemId)) {
            return false;
        }
        this.active.put(player.getUniqueId(), gemId);
        this.save(player.getUniqueId());
        return true;
    }

    public String cycleActive(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, Harvest> souls = this.getHarvested(playerId);
        if (souls.isEmpty()) {
            return null;
        }
        ArrayList<String> order = new ArrayList<String>(souls.keySet());
        String current = this.active.get(playerId);
        int index = current != null ? order.indexOf(current) : -1;
        String next = (String)order.get((index + 1) % order.size());
        this.active.put(playerId, next);
        this.save(playerId);
        return next;
    }

    public boolean isTrimEnabled(UUID playerId) {
        return !this.trimsDisabled.contains(playerId);
    }

    public boolean toggleTrims(Player player) {
        UUID playerId = player.getUniqueId();
        if (this.trimsDisabled.remove(playerId)) {
            this.applyTrims(player);
            this.save(playerId);
            return true;
        }
        this.trimsDisabled.add(playerId);
        this.clearTrims(player);
        this.save(playerId);
        return false;
    }

    public void fillSoul(Player player, String gemId, int tier) {
        UUID playerId = player.getUniqueId();
        this.harvested.computeIfAbsent(playerId, id -> new LinkedHashMap()).put(gemId, new Harvest(gemId, tier, null));
        this.active.putIfAbsent(playerId, gemId);
        this.save(playerId);
        this.refreshGoldItem(player);
    }

    public boolean removeSoul(Player player, String gemId) {
        UUID playerId = player.getUniqueId();
        this.getHarvested(playerId);
        Map<String, Harvest> souls = this.harvested.get(playerId);
        if (souls == null || souls.remove(gemId) == null) {
            return false;
        }
        if (gemId.equals(this.active.get(playerId))) {
            this.active.remove(playerId);
            if (!souls.isEmpty()) {
                this.active.put(playerId, souls.keySet().iterator().next());
            }
        }
        if (souls.isEmpty()) {
            this.harvested.remove(playerId);
        }
        this.save(playerId);
        this.refreshGoldItem(player);
        return true;
    }

    public int clearSouls(Player player) {
        UUID playerId = player.getUniqueId();
        int removed = this.getHarvested(playerId).size();
        this.harvested.remove(playerId);
        this.active.remove(playerId);
        this.save(playerId);
        this.refreshGoldItem(player);
        return removed;
    }

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

    public boolean canHarvest(Player killer, Player victim) {
        if (killer == null || victim == null || killer.equals((Object)victim) || !this.holdsGoldGem(killer)) {
            return false;
        }
        return this.plugin.getEnergyManager().getEnergyState(victim) != EnergyState.BROKEN;
    }

    public Harvest peekGem(Player victim) {
        UUID owner = victim.getUniqueId();
        for (int slot = 0; slot < victim.getInventory().getSize(); ++slot) {
            Harvest found = this.describe(victim.getInventory().getItem(slot), owner);
            if (found == null) continue;
            return found;
        }
        return this.describe(victim.getInventory().getItemInOffHand(), owner);
    }

    public Harvest takeGem(Player victim, List<ItemStack> drops) {
        Harvest taken = null;
        UUID owner = victim.getUniqueId();
        if (drops != null) {
            for (ItemStack item : new ArrayList<ItemStack>(drops)) {
                Harvest found = this.describe(item, owner);
                if (found == null) continue;
                taken = found;
                drops.remove(item);
                break;
            }
        }
        for (int slot = 0; slot < victim.getInventory().getSize(); ++slot) {
            Harvest found = this.describe(victim.getInventory().getItem(slot), owner);
            if (found == null) continue;
            if (taken == null) {
                taken = found;
            }
            victim.getInventory().setItem(slot, null);
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

    public void absorb(Player killer, Player victim, Harvest taken) {
        String awakened;
        this.harvested.computeIfAbsent(killer.getUniqueId(), id -> new LinkedHashMap()).put(taken.gemId(), taken);
        this.active.putIfAbsent(killer.getUniqueId(), taken.gemId());
        this.save(killer.getUniqueId());
        this.shatter(victim.getLocation(), taken.gemId());
        this.refreshGoldItem(killer);
        String message = this.plugin.getConfigManager().getMessage("gold-soul-repurposed");
        if (message != null && !message.isEmpty()) {
            Bukkit.broadcastMessage((String)message.replace("{player}", victim.getName()));
        }
        killer.playSound(killer.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.7f);
        int soulCount = this.getHarvested(killer.getUniqueId()).size();
        killer.sendMessage(this.plugin.getConfigManager().getMessage("gold-soul-harvested").replace("{gem}", this.plugin.getGemManager().getGemDisplayName(taken.gemId())).replace("{count}", String.valueOf(soulCount)));
        if (soulCount >= 8 && (awakened = this.plugin.getConfigManager().getMessage("gold-awakened")) != null && !awakened.isEmpty()) {
            Bukkit.broadcastMessage((String)awakened.replace("{player}", killer.getName()));
        }
    }

    private Harvest describe(ItemStack item, UUID owner) {
        String gemId = this.harvestableGemId(item);
        if (gemId == null) {
            return null;
        }
        int tier = this.plugin.getGemRegistry().tierFromItemId(CustomItemManager.getIdByItem(item));
        return new Harvest(gemId, tier, owner);
    }

    private void refreshGoldItem(Player player) {
        Map<String, Harvest> souls = this.getHarvested(player.getUniqueId());
        for (int slot = 0; slot < player.getInventory().getSize(); ++slot) {
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
        ArrayList<String> lore = new ArrayList<>();
        lore.add("\u00a7f\u00a7lWATCH THE LINES OF REALITY FRAY AS EIGHT SOULS BECOME ONE");
        lore.add(this.stateLine(souls.size()));
        lore.add("");
        lore.add("\u00a76\ud83c\udf1f \u00a76\u00a7lHARVESTED SOULS");
        if (souls.isEmpty()) {
            lore.add("\u00a78- the gem is silent -");
        } else {
            lore.add(this.colourBar(souls.keySet()));
            for (Map.Entry<String, Harvest> soul : souls.entrySet()) {
                lore.add(this.plugin.getGemManager().getGemColorCode(soul.getKey()) + "- " + this.plugin.getGemManager().getGemDisplayName(soul.getKey()) + " \u00a78(T" + soul.getValue().tier() + ")");
            }
        }
        meta.setLore(lore);
        meta.setCustomModelData(Integer.valueOf(1009 + Math.min(souls.size(), 8)));
        item.setItemMeta(meta);
    }

    private String colourBar(Iterable<String> gemIds) {
        StringBuilder bar = new StringBuilder();
        for (String gemId : gemIds) {
            bar.append(this.plugin.getGemManager().getGemColorCode(gemId)).append("\u2756");
        }
        return bar.toString();
    }

    private String stateLine(int soulCount) {
        if (soulCount <= 0) {
            return "\u00a76(Dormant)";
        }
        if (soulCount >= 8) {
            return "\u00a76\u00a7l(Awakened)";
        }
        return "\u00a76(Awakening \u2014 " + soulCount + "/8)";
    }

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

    private void shatter(Location location, String harvestedId) {
        if (location.getWorld() == null) {
            return;
        }
        location.getWorld().spawnParticle(Particle.DUST, location.clone().add(0.0, 1.0, 0.0), 12, 0.4, 0.6, 0.4, 0.0, (Object)new Particle.DustOptions(this.soulColour(harvestedId), 1.8f));
        location.getWorld().playSound(location, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.2f, 0.6f);
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

    public Color soulColour(String gemId) {
        if (this.plugin.getGemRitualManager() == null) {
            return Color.fromRGB((int)255, (int)215, (int)0);
        }
        return this.plugin.getGemRitualManager().getGemColor(gemId);
    }

    public void resetProgress(Player player) {
        boolean hadSouls;
        UUID playerId = player.getUniqueId();
        Map<String, Harvest> souls = this.getHarvested(playerId);
        boolean bl = hadSouls = !souls.isEmpty();
        if (hadSouls && this.plugin.getConfig().getBoolean("gold.death.return-souls", true)) {
            for (Harvest soul : new ArrayList<Harvest>(souls.values())) {
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
        if (this.plugin.getConfig().getBoolean("gold.death.break-gem", true) && this.holdsGoldGem(player)) {
            this.plugin.getEnergyManager().setEnergy(player, 0);
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-gem-broken"));
        }
    }

    private void returnSoul(Player holder, Harvest soul) {
        if (soul.owner() == null || soul.owner().equals(holder.getUniqueId())) {
            return;
        }
        String itemId = soul.gemId() + "_gem_t" + soul.tier();
        Player owner = this.plugin.getServer().getPlayer(soul.owner());
        if (owner != null && owner.isOnline()) {
            this.plugin.getGemManager().giveGemToOffhand(owner, soul.gemId(), soul.tier());
            owner.sendMessage(this.plugin.getConfigManager().getMessage("gold-soul-returned").replace("{player}", holder.getName()).replace("{gem}", this.plugin.getGemManager().getGemDisplayName(soul.gemId())));
            return;
        }
        this.queuePendingGem(soul.owner(), itemId);
    }

    private void queuePendingGem(UUID ownerId, String itemId) {
        File file = this.getPlayerFile(ownerId);
        YamlConfiguration data = YamlConfiguration.loadConfiguration((File)file);
        List pending = data.getStringList("gold.pending-return");
        pending.add(itemId);
        data.set("gold.pending-return", (Object)pending);
        try {
            data.save(file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to queue a returned gem for " + String.valueOf(ownerId) + ": " + e.getMessage());
        }
    }

    public void deliverPendingGems(Player player) {
        File file = this.getPlayerFile(player.getUniqueId());
        if (!file.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration((File)file);
        List<String> pending = data.getStringList("gold.pending-return");
        if (pending.isEmpty()) {
            return;
        }
        for (String itemId : pending) {
            int marker = itemId.indexOf("_gem_t");
            if (marker <= 0) continue;
            String gemId = itemId.substring(0, marker);
            int tier = "2".equals(itemId.substring(marker + 6)) ? 2 : 1;
            this.plugin.getGemManager().giveGemToOffhand(player, gemId, tier);
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-soul-returned").replace("{player}", "a fallen Gold Gem").replace("{gem}", this.plugin.getGemManager().getGemDisplayName(gemId)));
        }
        data.set("gold.pending-return", null);
        try {
            data.save(file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to clear returned gems for " + player.getName() + ": " + e.getMessage());
        }
    }

    private void startPassiveTask() {
        int interval = this.plugin.getConfigManager().getPassiveUpdateInterval();
        new BukkitRunnable(){

            public void run() {
                for (Player player : GoldGemManager.this.plugin.getServer().getOnlinePlayers()) {
                    if (player.isDead() || !GoldGemManager.this.holdsGoldGem(player)) continue;
                    GoldGemManager.this.applyTrims(player);
                    if (GoldGemManager.this.getHarvested(player.getUniqueId()).isEmpty()) continue;
                    GoldGemManager.this.applyHarvestedPassives(player);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, (long)interval, (long)interval);
    }

    private void applyHarvestedPassives(Player player) {
        if (this.plugin.getGemLockManager() != null && this.plugin.getGemLockManager().isLocked(player)) {
            return;
        }
        if (this.plugin.getRegionManager() != null && this.plugin.getRegionManager().areGemsDisabled(player)) {
            return;
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        if (registry == null) {
            return;
        }
        for (Map.Entry<String, Harvest> soul : this.getHarvested(player.getUniqueId()).entrySet()) {
            GemPassiveHandler handler = registry.getPassiveHandler(soul.getKey());
            if (handler == null) continue;
            handler.applyPassives(player, soul.getValue().tier());
        }
        this.drawSoulAura(player);
    }

    private void drawSoulAura(Player player) {
        ArrayList<String> souls = new ArrayList<String>(this.getHarvested(player.getUniqueId()).keySet());
        if (souls.isEmpty() || player.getWorld() == null) {
            return;
        }
        Location centre = player.getLocation().add(0.0, 1.0, 0.0);
        int points = souls.size() * 3;
        for (int i = 0; i < points; ++i) {
            double angle = Math.PI * 2 * (double)i / (double)points;
            Location at = centre.clone().add(Math.cos(angle) * 0.8, 0.0, Math.sin(angle) * 0.8);
            player.getWorld().spawnParticle(Particle.DUST, at, 1, 0.0, 0.15, 0.0, 0.0, (Object)new Particle.DustOptions(this.soulColour((String)souls.get(i % souls.size())), 0.9f));
        }
    }

    private void applyTrims(Player player) {
        if (!this.plugin.getConfig().getBoolean("gold.armour-trim.enabled", true)) {
            return;
        }
        if (this.trimsDisabled.contains(player.getUniqueId())) {
            return;
        }
        TrimPattern pattern = this.trimPattern();
        ItemStack[] armour = player.getInventory().getArmorContents();
        boolean changed = false;
        for (ItemStack piece : armour) {
            ArmorMeta meta;
            ItemMeta itemMeta;
            if (piece == null || !((itemMeta = piece.getItemMeta()) instanceof ArmorMeta) || (meta = (ArmorMeta)itemMeta).hasTrim()) continue;
            meta.setTrim(new ArmorTrim(TrimMaterial.GOLD, pattern));
            meta.getPersistentDataContainer().set(this.trimKey(), PersistentDataType.BYTE, (byte)1);
            piece.setItemMeta((ItemMeta)meta);
            changed = true;
        }
        if (changed) {
            player.getInventory().setArmorContents(armour);
        }
    }

    private void clearTrims(Player player) {
        ItemStack[] armour = player.getInventory().getArmorContents();
        boolean changed = false;
        for (ItemStack piece : armour) {
            ArmorMeta meta;
            ItemMeta itemMeta;
            if (piece == null || !((itemMeta = piece.getItemMeta()) instanceof ArmorMeta) || !(meta = (ArmorMeta)itemMeta).getPersistentDataContainer().has(this.trimKey(), PersistentDataType.BYTE)) continue;
            meta.setTrim(null);
            meta.getPersistentDataContainer().remove(this.trimKey());
            piece.setItemMeta((ItemMeta)meta);
            changed = true;
        }
        if (changed) {
            player.getInventory().setArmorContents(armour);
        }
    }

    private NamespacedKey trimKey() {
        return new NamespacedKey((Plugin)this.plugin, "gold_trim");
    }

    private TrimPattern trimPattern() {
        String name = this.plugin.getConfig().getString("gold.armour-trim.pattern", "FLOW");
        TrimPattern pattern = (TrimPattern)Registry.TRIM_PATTERN.get(NamespacedKey.minecraft((String)name.toLowerCase()));
        return pattern != null ? pattern : TrimPattern.FLOW;
    }

    private File getSummonFile() {
        return new File(this.plugin.getDataFolder(), "gold.yml");
    }

    public boolean isSummoned() {
        return YamlConfiguration.loadConfiguration((File)this.getSummonFile()).getBoolean("summoned", false);
    }

    public void markSummoned(Player summoner) {
        File file = this.getSummonFile();
        YamlConfiguration data = YamlConfiguration.loadConfiguration((File)file);
        data.set("summoned", (Object)true);
        data.set("summoned-by", (Object)summoner.getUniqueId().toString());
        data.set("summoned-by-name", (Object)summoner.getName());
        data.set("summoned-at", (Object)System.currentTimeMillis());
        try {
            data.save(file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to record the Gold Gem summon: " + e.getMessage());
        }
    }

    private Harvest deserialise(String stored) {
        String[] parts = stored.split(":");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return null;
        }
        int tier = 1;
        if (parts.length > 1) {
            try {
                tier = Integer.parseInt(parts[1]);
            }
            catch (NumberFormatException ignored) {
                tier = 1;
            }
        }
        UUID owner = null;
        if (parts.length > 2) {
            try {
                owner = UUID.fromString(parts[2]);
            }
            catch (IllegalArgumentException ignored) {
                owner = null;
            }
        }
        return new Harvest(parts[0], tier, owner);
    }

    private File getPlayerFile(UUID playerId) {
        File folder = new File(this.plugin.getDataFolder(), "playerdata");
        folder.mkdirs();
        return new File(folder, String.valueOf(playerId) + ".yml");
    }

    public void load(UUID playerId) {
        String instance;
        String selected;
        File file = this.getPlayerFile(playerId);
        if (!file.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration((File)file);
        List<String> souls = data.getStringList("gold.harvested");
        if (!souls.isEmpty()) {
            LinkedHashMap<String, Harvest> parsed = new LinkedHashMap<String, Harvest>();
            for (String soul : souls) {
                Harvest entry = this.deserialise(soul);
                if (entry == null) continue;
                parsed.put(entry.gemId(), entry);
            }
            this.harvested.put(playerId, parsed);
        }
        if ((selected = data.getString("gold.active")) != null) {
            this.active.put(playerId, selected);
        }
        if ((instance = data.getString("gold.instance")) != null) {
            try {
                this.trackedInstance.put(playerId, UUID.fromString(instance));
            }
            catch (IllegalArgumentException illegalArgumentException) {
                // empty catch block
            }
        }
        if (data.getBoolean("gold.trims-disabled", false)) {
            this.trimsDisabled.add(playerId);
        }
    }

    public void save(UUID playerId) {
        File file = this.getPlayerFile(playerId);
        YamlConfiguration data = YamlConfiguration.loadConfiguration((File)file);
        Map<String, Harvest> souls = this.getHarvested(playerId);
        if (souls.isEmpty()) {
            data.set("gold.harvested", null);
            data.set("gold.active", null);
        } else {
            ArrayList<CallSite> serialised = new ArrayList<CallSite>();
            for (Harvest soul : souls.values()) {
                serialised.add((CallSite)((Object)(soul.gemId() + ":" + soul.tier() + (String)(soul.owner() != null ? ":" + String.valueOf(soul.owner()) : ""))));
            }
            data.set("gold.harvested", serialised);
            data.set("gold.active", (Object)this.active.get(playerId));
        }
        UUID instance = this.trackedInstance.get(playerId);
        data.set("gold.instance", (Object)(instance != null ? instance.toString() : null));
        data.set("gold.trims-disabled", (Object)(this.trimsDisabled.contains(playerId) ? Boolean.valueOf(true) : null));
        try {
            data.save(file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save Gold Gem data for " + String.valueOf(playerId) + ": " + e.getMessage());
        }
    }

    public void unload(UUID playerId) {
        this.harvested.remove(playerId);
        this.active.remove(playerId);
        this.trackedInstance.remove(playerId);
        this.trimsDisabled.remove(playerId);
    }

    public record Harvest(String gemId, int tier, UUID owner) {
    }
}

