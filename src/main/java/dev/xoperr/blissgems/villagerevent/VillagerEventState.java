package dev.xoperr.blissgems.villagerevent;

import dev.xoperr.blissgems.BlissGems;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

/**
 * Everything the Villager event remembers: which phase it is in, where the compass is,
 * the three event villagers, who carries each soul, one-time trades already claimed and
 * the raid's progress. Persisted to villager-event.yml so a restart mid-event resumes it.
 */
public final class VillagerEventState {
    private final BlissGems plugin;
    private final File file;

    private boolean active;          // soul hunt running (phase 2)
    private boolean finalActive;     // the last raid (phase 3)
    private int villagersReturned;
    private final Set<Integer> returnedVillagers = new HashSet<>();
    private double ringRot;
    private double auraRot;
    private int raidWave;
    private boolean raidBetween;
    private final Map<UUID, Double> raidDamage = new HashMap<>();
    private Location compassLoc;
    private final Map<Integer, UUID> villagerUuids = new HashMap<>();
    private final Map<Integer, Location> villagerLocs = new HashMap<>();
    private final Map<Integer, Boolean> villagerAlive = new HashMap<>();
    private final Set<Integer> villagerEverKilled = new HashSet<>();
    private final Map<Integer, String> soulHolder = new HashMap<>();
    private final Map<Integer, Location> soulLastPos = new HashMap<>();
    private final Set<UUID> soulDroppedLock = new HashSet<>();
    private final Map<Integer, Set<String>> tradeBought = new HashMap<>();
    private final Map<UUID, Integer> tradingWith = new HashMap<>();

    public VillagerEventState(BlissGems plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "villager-event.yml");
    }

    // ---- phases ----
    public boolean isActive() { return this.active; }
    public void setActive(boolean v) { this.active = v; }
    public boolean isFinalActive() { return this.finalActive; }
    public void setFinalActive(boolean v) { this.finalActive = v; }
    public boolean isEventRunning() { return this.active || this.finalActive; }

    // ---- returned villagers ----
    public int villagersReturned() { return this.villagersReturned; }
    public void setVillagersReturned(int v) { this.villagersReturned = v; }
    public void incrementVillagersReturned() { this.villagersReturned++; }
    public boolean isVillagerReturned(int id) { return this.returnedVillagers.contains(id); }
    public void markVillagerReturned(int id) { this.returnedVillagers.add(id); this.save(); }
    public void clearReturnedVillagers() { this.returnedVillagers.clear(); this.save(); }

    // ---- raid ----
    public int raidWave() { return this.raidWave; }
    public void setRaidWave(int v) { this.raidWave = v; }
    public boolean raidBetween() { return this.raidBetween; }
    public void setRaidBetween(boolean v) { this.raidBetween = v; }

    public void addRaidDamage(UUID id, double amount) {
        if (id != null && amount > 0) this.raidDamage.merge(id, amount, Double::sum);
    }

    public UUID topRaidDamager() {
        UUID best = null;
        double most = 0;
        for (Map.Entry<UUID, Double> e : this.raidDamage.entrySet()) {
            if (e.getValue() > most) { most = e.getValue(); best = e.getKey(); }
        }
        return best;
    }

    // ---- cosmetics ----
    public double ringRot() { return this.ringRot; }
    public void addRingRot(double d) { this.ringRot = (this.ringRot + d) % 360.0; }
    public double auraRot() { return this.auraRot; }
    public void addAuraRot(double d) { this.auraRot = (this.auraRot + d) % 360.0; }

    // ---- compass ----
    public Location compassLoc() { return this.compassLoc; }
    public void setCompassLoc(Location loc) { this.compassLoc = loc == null ? null : loc.clone(); }

    // ---- villagers ----
    public UUID villagerUuid(int id) { return this.villagerUuids.get(id); }

    public Villager getVillager(int id) {
        UUID uuid = this.villagerUuids.get(id);
        if (uuid == null) return null;
        Entity e = Bukkit.getEntity(uuid);
        return e instanceof Villager v ? v : null;
    }

    public void setVillager(int id, Villager v) {
        this.villagerUuids.put(id, v.getUniqueId());
        this.villagerLocs.put(id, v.getLocation().clone());
        this.villagerAlive.put(id, true);
    }

    public void clearVillager(int id) { this.villagerUuids.remove(id); }
    public Location villagerLoc(int id) { return this.villagerLocs.get(id); }
    public void setVillagerLoc(int id, Location loc) { this.villagerLocs.put(id, loc.clone()); }
    public boolean isVillagerAlive(int id) { return this.villagerAlive.getOrDefault(id, false); }
    public void setVillagerAlive(int id, boolean alive) { this.villagerAlive.put(id, alive); }
    public void markVillagerKilled(int id) { this.villagerEverKilled.add(id); }
    public boolean wasVillagerEverKilled(int id) { return this.villagerEverKilled.contains(id); }

    // ---- souls ----
    public String soulHolder(int id) { return this.soulHolder.get(id); }
    public void setSoulHolder(int id, String name) { this.soulHolder.put(id, name); }
    public void clearSoulHolder(int id) { this.soulHolder.remove(id); }
    public Location soulLastPos(int id) { return this.soulLastPos.get(id); }
    public void setSoulLastPos(int id, Location loc) { this.soulLastPos.put(id, loc.clone()); }
    public void clearSoulLastPos(int id) { this.soulLastPos.remove(id); }
    public boolean isSoulDropped(UUID villager) { return this.soulDroppedLock.contains(villager); }
    public void lockSoulDrop(UUID villager) { this.soulDroppedLock.add(villager); }
    public void unlockSoulDrop(UUID villager) { this.soulDroppedLock.remove(villager); }

    // ---- one-time trades ----
    public boolean isTradeBought(int id, String key) {
        Set<String> s = this.tradeBought.get(id);
        return s != null && s.contains(key);
    }
    public boolean isTradeBoughtAny(int id) {
        Set<String> s = this.tradeBought.get(id);
        return s != null && !s.isEmpty();
    }
    public void setTradeBought(int id, String key) { this.tradeBought.computeIfAbsent(id, k -> new HashSet<>()).add(key); }
    public void clearTradeBought(int id) { this.tradeBought.remove(id); }
    public Integer tradingWith(UUID player) { return this.tradingWith.get(player); }
    public void setTradingWith(UUID player, int id) { this.tradingWith.put(player, id); }
    public void clearTradingWith(UUID player) { this.tradingWith.remove(player); }

    /** Wipes the event back to "not started" and removes any living event villagers. */
    public void endEvent() {
        this.active = false;
        this.finalActive = false;
        this.villagersReturned = 0;
        this.returnedVillagers.clear();
        this.ringRot = 0;
        this.auraRot = 0;
        this.raidWave = 0;
        this.raidBetween = false;
        this.raidDamage.clear();
        this.soulHolder.clear();
        this.soulLastPos.clear();
        this.soulDroppedLock.clear();
        this.tradingWith.clear();
        for (int id = 1; id <= 3; id++) {
            Villager v = this.getVillager(id);
            if (v != null && this.isVillagerAlive(id)) v.remove();
            this.villagerUuids.remove(id);
            this.villagerLocs.remove(id);
            this.villagerAlive.remove(id);
            this.villagerEverKilled.remove(id);
            this.tradeBought.remove(id);
        }
        this.save();
    }

    // ---- persistence ----

    public void load() {
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(this.file);
        this.active = y.getBoolean("active");
        this.finalActive = y.getBoolean("final-active");
        this.villagersReturned = y.getInt("villagers-returned");
        this.returnedVillagers.clear();
        this.returnedVillagers.addAll(y.getIntegerList("returned"));
        this.raidWave = y.getInt("raid.wave");
        this.raidBetween = y.getBoolean("raid.between");
        ConfigurationSection dmg = y.getConfigurationSection("raid.damage");
        if (dmg != null) for (String k : dmg.getKeys(false)) this.raidDamage.put(UUID.fromString(k), dmg.getDouble(k));
        this.compassLoc = y.getLocation("compass");
        for (int id = 1; id <= 3; id++) {
            String p = "villagers." + id + ".";
            String uuid = y.getString(p + "uuid");
            if (uuid != null) this.villagerUuids.put(id, UUID.fromString(uuid));
            Location loc = y.getLocation(p + "loc");
            if (loc != null) this.villagerLocs.put(id, loc);
            if (y.contains(p + "alive")) this.villagerAlive.put(id, y.getBoolean(p + "alive"));
            if (y.getBoolean(p + "ever-killed")) this.villagerEverKilled.add(id);
            String holder = y.getString(p + "soul-holder");
            if (holder != null) this.soulHolder.put(id, holder);
            Location last = y.getLocation(p + "soul-last");
            if (last != null) this.soulLastPos.put(id, last);
            if (y.isList(p + "trades-bought")) this.tradeBought.put(id, new HashSet<>(y.getStringList(p + "trades-bought")));
        }
        for (String s : y.getStringList("soul-dropped")) this.soulDroppedLock.add(UUID.fromString(s));
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("active", this.active);
        y.set("final-active", this.finalActive);
        y.set("villagers-returned", this.villagersReturned);
        y.set("returned", this.returnedVillagers.stream().toList());
        y.set("raid.wave", this.raidWave);
        y.set("raid.between", this.raidBetween);
        for (Map.Entry<UUID, Double> e : this.raidDamage.entrySet()) y.set("raid.damage." + e.getKey(), e.getValue());
        y.set("compass", this.compassLoc);
        for (int id = 1; id <= 3; id++) {
            String p = "villagers." + id + ".";
            UUID uuid = this.villagerUuids.get(id);
            y.set(p + "uuid", uuid == null ? null : uuid.toString());
            y.set(p + "loc", this.villagerLocs.get(id));
            if (this.villagerAlive.containsKey(id)) y.set(p + "alive", this.villagerAlive.get(id));
            y.set(p + "ever-killed", this.villagerEverKilled.contains(id));
            y.set(p + "soul-holder", this.soulHolder.get(id));
            y.set(p + "soul-last", this.soulLastPos.get(id));
            Set<String> bought = this.tradeBought.get(id);
            y.set(p + "trades-bought", bought == null ? null : bought.stream().toList());
        }
        y.set("soul-dropped", this.soulDroppedLock.stream().map(UUID::toString).toList());
        try {
            y.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Could not save villager-event.yml: " + e.getMessage());
        }
    }
}
