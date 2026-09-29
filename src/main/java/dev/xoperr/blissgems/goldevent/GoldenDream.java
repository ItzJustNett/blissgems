package dev.xoperr.blissgems.goldevent;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

/**
 * The Golden Dream event, wired together. Everything works without other plugins; only NPC
 * "memories" (bots standing in for staff) need Citizens, and ask for it when used.
 */
public final class GoldenDream {
    private final BlissGems plugin;
    private final DreamTabList tabList;
    private final NickManager nicks;
    private final MemoryRoster roster;
    private final MemoryNpcs npcs;
    private final GoldenDreamWorld world;
    private final GoldenDreamRitual ritual;

    public GoldenDream(BlissGems plugin, FragmentCoreRitual fx) {
        this.plugin = plugin;
        this.tabList = new DreamTabList(plugin);
        this.nicks = new NickManager(plugin, this);
        this.roster = new MemoryRoster(plugin, this);
        this.npcs = new MemoryNpcs(plugin, this.roster);
        this.world = new GoldenDreamWorld(plugin, this);
        this.ritual = new GoldenDreamRitual(plugin, this, fx);
        PluginManager pm = Bukkit.getPluginManager();
        for (Listener l : new Listener[]{this.tabList, this.nicks, this.roster, this.npcs, this.world, this.ritual, new DreamChannels()}) {
            pm.registerEvents(l, plugin);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            this.npcs.reload();
            this.tabList.refreshAll();
        }, 40L);
    }

    public BlissGems plugin() {
        return this.plugin;
    }

    public DreamTabList tabList() {
        return this.tabList;
    }

    public NickManager nicks() {
        return this.nicks;
    }

    public MemoryRoster roster() {
        return this.roster;
    }

    public MemoryNpcs npcs() {
        return this.npcs;
    }

    public GoldenDreamWorld world() {
        return this.world;
    }

    public GoldenDreamRitual ritual() {
        return this.ritual;
    }

    public void shutdown() {
        this.ritual.abortAll();
        WakeFloat.shutdown();
        this.npcs.shutdown();
        this.world.shutdown();
        this.tabList.listAll();
    }
}
