package dev.xoperr.blissgems.pedestal;

import dev.xoperr.blissgems.BlissGems;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;

/**
 * Where the spawn pedestal is and what it is doing right now.
 * Only the centre and the open/closed flag survive a restart; a ritual in progress does not.
 */
public final class PedestalState {
    public enum Ritual { NONE, REPAIR, REVIVE }

    public static final String CRYSTAL_TAG = "BlissPedestalCrystal";
    public static final int DEPOSITS_REQUIRED = 5;
    public static final int MAX_DURABILITY = 3;

    // fallback structure used when pedestal_layout.yml has no "open" section
    private static final int[][] GILDED = {{0, 0, -1}, {0, 0, 1}, {1, 0, 0}, {-1, 0, 0}};
    private static final int[][] GOLD = {{1, 0, -1}, {1, 0, 1}, {-1, 0, -1}, {-1, 0, 1}};
    private static final int[][] RAW_GOLD = {{-1, 0, -2}, {1, 0, -2}, {-2, 0, 1}, {-2, 0, -1}, {1, 0, 2}, {-1, 0, 2}, {2, 0, -1}, {2, 0, 1}};
    private static final int[][] CHERRY = {{2, 0, 0}, {-2, 0, 0}, {0, 0, -2}, {0, 0, 2}};

    private final BlissGems plugin;
    private final File file;
    private PedestalLayout layout;
    private final Set<Long> layoutOffsets = new HashSet<>();

    private Location mainLoc;
    private boolean active;
    private boolean beacon;
    private Ritual ritual = Ritual.NONE;
    private int count;
    private int durability = MAX_DURABILITY;
    private int depositCooldown;
    private UUID revivingPlayer;
    private boolean busy;
    private final Set<UUID> revivalLocked = new HashSet<>();

    public PedestalState(BlissGems plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pedestal.yml");
        this.reloadLayout();
    }

    public void reloadLayout() {
        this.layout = PedestalLayout.load(this.plugin);
        this.layoutOffsets.clear();
        for (PedestalLayout.Block b : this.layout.open()) this.layoutOffsets.add(key(b.dx(), b.dy(), b.dz()));
        for (PedestalLayout.Block b : this.layout.closed()) this.layoutOffsets.add(key(b.dx(), b.dy(), b.dz()));
    }

    private static long key(int dx, int dy, int dz) {
        return ((long) (dx & 0xFFFFF) << 40) | ((long) (dy & 0xFFFFF) << 20) | (dz & 0xFFFFF);
    }

    // ---- locations ----

    public Location mainLoc() {
        return this.mainLoc == null ? null : this.mainLoc.clone();
    }

    /** Block above the beacon: where energy and the repair rings are centred. */
    public Location subLoc() {
        return this.mainLoc == null ? null : this.mainLoc.clone().add(0, 1, 0);
    }

    public Location ancientDebrisLoc() {
        return this.mainLoc == null ? null : this.mainLoc.clone().add(0, -1, 0);
    }

    /** Centre of the block a restoring player must stand on (top of the beacon). */
    public Location standLoc() {
        return this.mainLoc == null ? null : this.mainLoc.clone().add(0.5, 1, 0.5);
    }

    public void configureFromBeaconCenter(Location center) {
        this.mainLoc = new Location(center.getWorld(), center.getBlockX(), center.getBlockY(), center.getBlockZ());
    }

    /** True for any block that belongs to the pedestal structure (layout, beacon, debris or fallback pillars). */
    public boolean isPedestalBlock(Location loc) {
        if (loc == null || this.mainLoc == null || loc.getWorld() == null || !loc.getWorld().equals(this.mainLoc.getWorld())) {
            return false;
        }
        int dx = loc.getBlockX() - this.mainLoc.getBlockX();
        int dy = loc.getBlockY() - this.mainLoc.getBlockY();
        int dz = loc.getBlockZ() - this.mainLoc.getBlockZ();
        if (dx == 0 && dz == 0 && (dy == 0 || dy == -1)) {
            return true;
        }
        if (this.layoutOffsets.contains(key(dx, dy, dz))) {
            return true;
        }
        if (dy != 0) {
            return false;
        }
        for (int[][] set : new int[][][]{GILDED, GOLD, RAW_GOLD, CHERRY}) {
            for (int[] o : set) if (o[0] == dx && o[2] == dz) return true;
        }
        return false;
    }

    /** Loose check used for "you can't break near the pillars". */
    public boolean nearStructure(Location loc) {
        if (this.isPedestalBlock(loc)) {
            return true;
        }
        Location sub = this.subLoc();
        return sub != null && near(loc, sub, 2.0);
    }

    public static boolean near(Location a, Location b, double radius) {
        return a != null && b != null && a.getWorld() != null && a.getWorld().equals(b.getWorld()) && a.distanceSquared(b) <= radius * radius;
    }

    // ---- flags ----

    public boolean active() { return this.active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean beacon() { return this.beacon; }
    public void setBeacon(boolean beacon) { this.beacon = beacon; }
    public Ritual ritual() { return this.ritual; }
    public void setRitual(Ritual ritual) { this.ritual = ritual; }
    public int count() { return this.count; }
    public void incrementCount() { this.count++; }
    public void resetCount() { this.count = 0; }
    public int durability() { return this.durability; }
    public void setDurability(int durability) { this.durability = durability; }
    public void decrementDurability() { this.durability--; }
    public int depositCooldown() { return this.depositCooldown; }
    public void setDepositCooldown(int seconds) { this.depositCooldown = seconds; }
    public UUID revivingPlayer() { return this.revivingPlayer; }
    public void setRevivingPlayer(UUID uuid) { this.revivingPlayer = uuid; }
    public boolean pedestalBusy() { return this.busy; }
    public void setPedestalBusy(boolean busy) { this.busy = busy; }
    public boolean revivalLocked(UUID uuid) { return this.revivalLocked.contains(uuid); }

    public void setRevivalLocked(UUID uuid, boolean locked) {
        if (locked) this.revivalLocked.add(uuid); else this.revivalLocked.remove(uuid);
    }

    public void resetRitual() {
        this.ritual = Ritual.NONE;
        this.count = 0;
        this.durability = MAX_DURABILITY;
        this.depositCooldown = 0;
        this.revivingPlayer = null;
        this.busy = false;
    }

    // ---- persistence ----

    public void load() {
        if (!this.file.exists()) {
            this.applyFixedConfig(this.plugin.getConfig());
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(this.file);
        World world = yml.getString("world") == null ? null : Bukkit.getWorld(yml.getString("world"));
        if (world != null && yml.contains("x") && yml.contains("y") && yml.contains("z")) {
            this.mainLoc = new Location(world, yml.getInt("x"), yml.getInt("y"), yml.getInt("z"));
        }
        this.active = yml.getBoolean("active", false);
        if (this.mainLoc == null) {
            this.applyFixedConfig(this.plugin.getConfig());
        }
    }

    /** Builds the pedestal from config "pedestal.fixed" when no location has been set in-game. */
    public boolean applyFixedConfig(FileConfiguration config) {
        if (!config.getBoolean("pedestal.fixed.enabled", false)) {
            return false;
        }
        World world = Bukkit.getWorld(config.getString("pedestal.fixed.world", "world"));
        if (world == null || !config.contains("pedestal.fixed.x") || !config.contains("pedestal.fixed.y") || !config.contains("pedestal.fixed.z")) {
            this.plugin.getLogger().warning("pedestal.fixed is enabled but its world/x/y/z are incomplete; not building it.");
            return false;
        }
        this.configureFromBeaconCenter(new Location(world, config.getInt("pedestal.fixed.x"), config.getInt("pedestal.fixed.y"), config.getInt("pedestal.fixed.z")));
        this.unbuildStructure();
        this.save();
        return true;
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        if (this.mainLoc != null && this.mainLoc.getWorld() != null) {
            yml.set("world", this.mainLoc.getWorld().getName());
            yml.set("x", this.mainLoc.getBlockX());
            yml.set("y", this.mainLoc.getBlockY());
            yml.set("z", this.mainLoc.getBlockZ());
        }
        yml.set("active", this.active);
        try {
            yml.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Could not save pedestal.yml: " + e.getMessage());
        }
    }

    // ---- structure ----

    public void buildStructure() {
        if (this.mainLoc == null) {
            return;
        }
        if (this.layout.hasOpen()) {
            for (PedestalLayout.Block b : this.layout.open()) {
                this.mainLoc.clone().add(b.dx(), b.dy(), b.dz()).getBlock().setBlockData(b.data());
            }
        } else {
            place(GILDED, Material.GILDED_BLACKSTONE);
            place(GOLD, Material.GOLD_BLOCK);
            place(CHERRY, Material.CHERRY_LOG);
            place(RAW_GOLD, Material.RAW_GOLD_BLOCK);
        }
        this.mainLoc.getBlock().setType(Material.BEACON);
        this.ancientDebrisLoc().getBlock().setType(Material.ANCIENT_DEBRIS);
        this.beacon = false;
        this.mainLoc.getWorld().playSound(this.mainLoc, Sound.BLOCK_BEACON_AMBIENT, 40.0f, 1.6f);
    }

    public void unbuildStructure() {
        if (this.mainLoc == null) {
            return;
        }
        if (this.layout.hasOpen() || this.layout.hasClosed()) {
            for (PedestalLayout.Block b : this.layout.open()) {
                this.mainLoc.clone().add(b.dx(), b.dy(), b.dz()).getBlock().setType(Material.AIR);
            }
            for (PedestalLayout.Block b : this.layout.closed()) {
                this.mainLoc.clone().add(b.dx(), b.dy(), b.dz()).getBlock().setBlockData(b.data());
            }
        } else {
            place(GILDED, Material.BLACKSTONE);
            place(GOLD, Material.BLACKSTONE);
            place(CHERRY, Material.BLACKSTONE);
            place(RAW_GOLD, Material.BLACKSTONE);
        }
        this.mainLoc.getBlock().setType(Material.BEDROCK);
        this.ancientDebrisLoc().getBlock().setType(Material.BEDROCK);
        this.beacon = false;
        this.removeBeam();
        this.mainLoc.getWorld().playSound(this.mainLoc, Sound.BLOCK_BEACON_DEACTIVATE, 20.0f, 2.0f);
    }

    private void place(int[][] offsets, Material material) {
        for (int[] o : offsets) this.mainLoc.clone().add(o[0], o[1], o[2]).getBlock().setType(material);
    }

    /** Sky-high end crystal firing its beam down into the pedestal. */
    public void summonBeam() {
        if (this.mainLoc == null || this.mainLoc.getWorld() == null) {
            return;
        }
        this.removeBeam();
        World world = this.mainLoc.getWorld();
        double x = this.mainLoc.getBlockX() + 0.5;
        double z = this.mainLoc.getBlockZ() + 0.5;
        Location target = new Location(world, x, 50.0, z);
        world.spawn(new Location(world, x, world.getMaxHeight() - 1, z), EnderCrystal.class, crystal -> {
            crystal.setShowingBottom(false);
            crystal.setBeamTarget(target);
            crystal.setPersistent(false);
            crystal.setInvulnerable(true);
            crystal.addScoreboardTag(CRYSTAL_TAG);
        });
    }

    public void removeBeam() {
        if (this.mainLoc == null || this.mainLoc.getWorld() == null) {
            return;
        }
        List<Entity> found = new ArrayList<>(this.mainLoc.getWorld().getNearbyEntities(this.mainLoc, 16.0, 512.0, 16.0));
        for (Entity e : found) {
            if (e instanceof EnderCrystal && e.getScoreboardTags().contains(CRYSTAL_TAG)) e.remove();
        }
    }
}
