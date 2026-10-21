package dev.xoperr.blissgems.goldevent;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.pedestal.PedestalManager;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.util.Vector;

/**
 * "Memories" are staff players (or NPC bots) who live inside the dream's memory world under
 * names from a pool. Dreamers who reach the memory are intruders; an intruder killing a memory
 * purges it (the memory is sent home, its disguise dropped).
 */
public final class MemoryRoster implements Listener {
    private static final List<String> SEED = List.of("ItsRyanStuff", "EchoOfNoah", "PaleWanderer", "GreyHollow", "FadedMilo",
        "DustBoundLeo", "AshenKai", "HollowFinn", "MutedTheo", "SilentEli", "WornOutSam", "GhostlyOwen", "BleakToby", "FrayedZane", "QuietedMax");

    private final BlissGems plugin;
    private final GoldenDream dream;
    private final File file;
    private final YamlConfiguration cfg;
    private final Set<UUID> returning = new HashSet<>();
    private final Set<UUID> moveOnRespawn = new HashSet<>();
    private final Set<UUID> wakeOnRespawn = new HashSet<>();
    private final Set<String> reserved = new HashSet<>();

    public MemoryRoster(BlissGems plugin, GoldenDream dream) {
        this.plugin = plugin;
        this.dream = dream;
        this.file = new File(plugin.getDataFolder(), "memories.yml");
        this.cfg = YamlConfiguration.loadConfiguration(this.file);
        if (!this.cfg.contains("names") || this.cfg.getStringList("names").size() < 10) {
            this.cfg.set("names", new ArrayList<>(SEED));
            this.save();
        }
    }

    public List<String> names() {
        return this.cfg.getStringList("names");
    }

    private Set<String> taken() {
        Set<String> out = new HashSet<>();
        ConfigurationSection active = this.cfg.getConfigurationSection("active");
        if (active != null) for (String k : active.getKeys(false)) {
            String n = active.getString(k);
            if (n != null) out.add(n.toLowerCase());
        }
        for (String r : this.reserved) out.add(r.toLowerCase());
        return out;
    }

    private List<String> freeNames() {
        Set<String> taken = this.taken();
        List<String> out = new ArrayList<>();
        for (String n : this.names()) if (!taken.contains(n.toLowerCase())) out.add(n);
        return out;
    }

    public String claimFreeName() {
        List<String> free = this.freeNames();
        if (free.isEmpty()) return null;
        String n = free.get(ThreadLocalRandom.current().nextInt(free.size()));
        this.reserved.add(n);
        return n;
    }

    public void claimSpecificName(String n) {
        if (n != null) this.reserved.add(n);
    }

    public void releaseName(String n) {
        if (n != null) this.reserved.remove(n);
    }

    public boolean isNameFree(String n) {
        return n != null && !n.isBlank() && !this.taken().contains(n.toLowerCase());
    }

    public boolean isMemory(UUID id) {
        return this.cfg.contains("active." + id);
    }

    public String nameOf(UUID id) {
        return this.cfg.getString("active." + id);
    }

    public Set<UUID> activeIds() {
        Set<UUID> out = new HashSet<>();
        ConfigurationSection active = this.cfg.getConfigurationSection("active");
        if (active != null) for (String k : active.getKeys(false)) {
            try {
                out.add(UUID.fromString(k));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return out;
    }

    /** Makes a player a memory with the given pool name (or a random free one). Null when no name is free. */
    public String assign(Player p, String name) {
        if (name == null) {
            List<String> free = this.freeNames();
            if (free.isEmpty()) return null;
            name = free.get(ThreadLocalRandom.current().nextInt(free.size()));
        } else if (!this.freeNames().contains(name)) {
            return null;
        }
        this.cfg.set("active." + p.getUniqueId(), name);
        this.save();
        this.dream.nicks().setName(p, name);
        GoldenDreamWorld world = this.dream.world();
        if (world.inDream(p.getUniqueId())) {
            if (p.isDead()) {
                this.wakeOnRespawn.add(p.getUniqueId());
            } else {
                world.wake(p);
                this.moveToMemory(p);
            }
        } else if (!GoldenDreamWorld.isDreamWorld(p.getWorld())) {
            if (p.isDead()) this.moveOnRespawn.add(p.getUniqueId()); else this.moveToMemory(p);
        }
        return name;
    }

    private void moveToMemory(Player p) {
        Location at = this.dream.world().memoryArrival();
        if (at != null) {
            p.setVelocity(new Vector());
            p.teleport(at);
            p.setFallDistance(0);
        }
    }

    public boolean rename(Player p, String name) {
        if (!this.isMemory(p.getUniqueId()) || !this.isNameFree(name)) return false;
        this.cfg.set("active." + p.getUniqueId(), name);
        this.save();
        this.dream.nicks().setName(p, name);
        return true;
    }

    public void remove(Player p) {
        UUID id = p.getUniqueId();
        this.moveOnRespawn.remove(id);
        this.wakeOnRespawn.remove(id);
        this.returning.remove(id);
        this.cfg.set("active." + id, null);
        this.save();
        this.dream.nicks().reset(p);
        if (GoldenDreamWorld.isDreamWorld(p.getWorld()) && !this.dream.world().inDream(id)) {
            if (p.isDead()) {
                this.returning.add(id);
            } else {
                World main = Bukkit.getWorlds().get(0);
                p.setVelocity(new Vector());
                p.teleport(main.getSpawnLocation());
                p.setFallDistance(0);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        UUID id = p.getUniqueId();
        if (!this.isMemory(id)) return;
        if (!this.killedByIntruder(p)) {
            event.deathMessage(null);
            return;
        }
        event.deathMessage(Component.text(PedestalManager.color("&e" + this.nameOf(id) + " has been purged.")));
        p.getWorld().strikeLightningEffect(p.getLocation());
        this.cfg.set("active." + id, null);
        this.save();
        this.returning.add(id);
        this.dream.nicks().forget(id);
    }

    private boolean killedByIntruder(Player p) {
        Player killer = p.getKiller();
        if (killer == null) {
            EntityDamageEvent last = p.getLastDamageCause();
            if (last instanceof EntityDamageByEntityEvent by) {
                if (by.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player shooter) killer = shooter;
                else if (by.getDamager() instanceof Player direct) killer = direct;
            }
        }
        return killer != null && !this.isMemory(killer.getUniqueId()) && this.dream.world().isIntruder(killer.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();
        UUID id = p.getUniqueId();
        if (this.wakeOnRespawn.remove(id)) {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (p.isOnline() && !p.isDead() && this.isMemory(id)) {
                    this.dream.world().wake(p);
                    this.moveToMemory(p);
                }
            });
        }
        if (this.moveOnRespawn.remove(id) && this.isMemory(id)) {
            Location at = this.dream.world().memoryArrival();
            if (at != null) event.setRespawnLocation(at);
        }
        boolean dreamRespawn = event.getRespawnLocation().getWorld() != null && GoldenDreamWorld.isDreamWorld(event.getRespawnLocation().getWorld());
        if (this.returning.remove(id)) {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (p.isOnline()) this.dream.nicks().apply(p);
            });
            if ((event.isBedSpawn() || event.isAnchorSpawn()) && !dreamRespawn) return;
            event.setRespawnLocation(Bukkit.getWorlds().get(0).getSpawnLocation());
            return;
        }
        // anyone without business in the dream who would respawn there goes home instead
        if (dreamRespawn && !this.isMemory(id) && !this.dream.world().inDream(id) && !this.dream.world().isVisiting(id)) {
            event.setRespawnLocation(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
    }

    private void save() {
        try {
            this.cfg.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("memories.yml: " + e.getMessage());
        }
    }
}
