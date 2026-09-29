package dev.xoperr.blissgems.pedestal;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.GemType;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Spawn restoration pedestal: an admin-placed structure where players run
 * a REPAIR ritual (repair kit + 5 energy bottles, +1 energy to everyone near)
 * or a REVIVE ritual (restoration book while Broken + 5 energy bottles, then
 * stand on the beacon to be lifted, spun through the gems and restored).
 */
public final class PedestalManager {
    private static final Pattern HEX = Pattern.compile("<##([0-9A-Fa-f]{6})>");
    private static final AtomicInteger GEN = new AtomicInteger();

    private final BlissGems plugin;
    private final PedestalState state;
    private final PedestalReviveRitual revive;
    private final Set<UUID> glowing = new HashSet<>();
    private BukkitTask secondTask;
    private BukkitTask fastTask;

    public PedestalManager(BlissGems plugin) {
        this.plugin = plugin;
        this.state = new PedestalState(plugin);
        this.state.load();
        this.revive = new PedestalReviveRitual(plugin, this);
        this.secondTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickSecond, 20L, 20L);
        this.fastTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickFast, 20L, 10L);
    }

    public PedestalState state() {
        return this.state;
    }

    public PedestalReviveRitual revive() {
        return this.revive;
    }

    public boolean isNearPedestal(Location loc, double radius) {
        return PedestalState.near(loc, this.state.mainLoc(), radius);
    }

    // ---- admin ----

    public void setPedestal(Location beaconCenter) {
        if (this.state.active()) {
            this.deactivate();
        }
        this.state.configureFromBeaconCenter(beaconCenter);
        this.state.save();
    }

    public boolean activate() {
        if (this.state.mainLoc() == null) {
            return false;
        }
        this.state.setActive(true);
        this.state.setBeacon(false);
        this.state.resetRitual();
        this.state.buildStructure();
        PedestalReviveRitual.restoreWeather();
        this.state.save();
        return true;
    }

    public void deactivate() {
        this.revive.shutdownRestore();
        this.state.setActive(false);
        this.state.resetRitual();
        this.state.unbuildStructure();
        PedestalReviveRitual.restoreWeather();
        this.state.save();
    }

    // ---- repair ritual completion: 5 pulses 10s apart, +1 energy to everyone within 8 blocks ----

    public void completeRepair() {
        Location center = this.state.subLoc();
        if (center == null) {
            return;
        }
        int gen = GEN.incrementAndGet();
        World world = center.getWorld();
        world.playSound(center, Sound.ITEM_AXE_SCRAPE, 10.0f, 0.5f);
        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 20.0f, 1.2f);
        int max = this.plugin.getConfigManager().getMaxEnergy();
        for (int pulse = 1; pulse <= 5; pulse++) {
            final int p = pulse;
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                if (gen != GEN.get() || this.state.ritual() != PedestalState.Ritual.REPAIR) {
                    return;
                }
                world.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 40.0f, 1.6f);
                drawHappyRings(center);
                for (Player player : world.getPlayers()) {
                    if (player.getLocation().distanceSquared(center) > 64.0) continue;
                    int energy = this.plugin.getEnergyManager().getEnergy(player);
                    if (energy <= 0 || energy >= max) continue;
                    this.plugin.getEnergyManager().setEnergy(player, energy + 1);
                }
                if (p == 5) {
                    this.finishRepair();
                }
            }, pulse * 200L);
        }
    }

    public static void invalidatePulses() {
        GEN.incrementAndGet();
    }

    private void finishRepair() {
        Location main = this.state.mainLoc();
        if (main != null) {
            main.getWorld().playSound(main, Sound.BLOCK_BEACON_DEACTIVATE, 20.0f, 2.0f);
            this.broadcastNear(main, 8.0, "&aThe pedestal ritual is complete.");
        }
        this.state.removeBeam();
        this.state.setActive(false);
        this.state.resetRitual();
        this.state.unbuildStructure();
        this.state.save();
    }

    private static void drawHappyRings(Location c) {
        double[] radii = {0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0, 3.5, 4.0};
        for (double r : radii) {
            int points = (int) Math.max(12.0, r * 12.0);
            for (int i = 0; i < points; i++) {
                double a = Math.toRadians((double) i / points * 360.0);
                c.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, c.getX() + r * Math.cos(a), c.getY() + 0.1, c.getZ() + r * Math.sin(a), 1, 0, 0, 0, 0);
            }
        }
    }

    // ---- ticking ----

    private void tickSecond() {
        int cd = this.state.depositCooldown();
        if (cd > 1) {
            this.state.setDepositCooldown(cd - 1);
        } else if (cd == 1) {
            this.state.setDepositCooldown(0);
            Location main = this.state.mainLoc();
            if (main != null && this.state.ritual() != PedestalState.Ritual.NONE && this.state.count() < PedestalState.DEPOSITS_REQUIRED) {
                this.broadcastNear(main, 8.0, "&6Pedestal cooldown is over. <##96FFD9>Please deposit 1 &lᴇɴᴇʀɢʏ!");
            }
        }
        this.tickGlow();
    }

    private void tickFast() {
        if (this.state.ritual() != PedestalState.Ritual.REVIVE || this.state.revivingPlayer() == null) {
            return;
        }
        Player player = Bukkit.getPlayer(this.state.revivingPlayer());
        if (player != null && !this.state.revivalLocked(player.getUniqueId())) {
            this.revive.tryStart(player);
        }
    }

    /** Players standing at an open pedestal glow in their gem's colour. */
    private void tickGlow() {
        Location main = this.state.mainLoc();
        Set<UUID> now = new HashSet<>();
        if (main != null && this.state.active()) {
            for (Player player : main.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(main) > 64.0) continue;
                GemType type = this.plugin.getGemManager().getGemType(player);
                if (type == null) continue;
                now.add(player.getUniqueId());
                applyGemGlow(player, type, 140);
            }
        }
        for (Iterator<UUID> it = this.glowing.iterator(); it.hasNext(); ) {
            UUID id = it.next();
            if (now.contains(id)) continue;
            it.remove();
            Player player = Bukkit.getPlayer(id);
            if (player != null && !this.state.revivalLocked(id)) clearGemGlow(player);
        }
        this.glowing.addAll(now);
    }

    public static void applyGemGlow(Player player, GemType type, int ticks) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (GemType t : GemType.values()) {
            Team team = board.getTeam(teamName(t));
            if (team != null && t != type) team.removeEntry(player.getName());
        }
        Team team = board.getTeam(teamName(type));
        if (team == null) {
            team = board.registerNewTeam(teamName(type));
            ChatColor color = ChatColor.getByChar(type.getColor().charAt(1));
            team.setColor(color == null ? ChatColor.WHITE : color);
        }
        if (!team.hasEntry(player.getName())) team.addEntry(player.getName());
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, ticks, 0, false, false, false), true);
    }

    public static void clearGemGlow(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (GemType t : GemType.values()) {
            Team team = board.getTeam(teamName(t));
            if (team != null) team.removeEntry(player.getName());
        }
        player.removePotionEffect(PotionEffectType.GLOWING);
    }

    private static String teamName(GemType type) {
        return "bliss_ped_" + type.getId();
    }

    // ---- text ----

    public static String color(String text) {
        Matcher m = HEX.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(net.md_5.bungee.api.ChatColor.of("#" + m.group(1)).toString()));
        }
        m.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public void broadcastNear(Location center, double radius, String message) {
        if (center == null || center.getWorld() == null) {
            return;
        }
        String colored = color(message);
        for (Player player : center.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= radius * radius) player.sendMessage(colored);
        }
    }

    public void unlock(Player player, Achievement achievement) {
        if (this.plugin.getAchievementManager() != null) this.plugin.getAchievementManager().unlock(player, achievement);
    }

    public void progress(Player player, Achievement achievement, int amount) {
        if (this.plugin.getAchievementManager() != null) this.plugin.getAchievementManager().addProgress(player, achievement, amount);
    }

    public void shutdown() {
        if (this.secondTask != null) this.secondTask.cancel();
        if (this.fastTask != null) this.fastTask.cancel();
        this.revive.shutdownRestore();
        this.state.removeBeam();
        PedestalReviveRitual.restoreWeather();
        for (UUID id : this.glowing) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) clearGemGlow(player);
        }
        this.glowing.clear();
        this.state.save();
    }
}
