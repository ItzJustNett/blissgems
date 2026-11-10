package dev.xoperr.blissgems.games;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Attributes;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Bliss's one-life events, run by staff with {@code /bliss games ...}:
 * <ul>
 *   <li><b>Energy Games</b> - die and you're out (red name; blue while in). A kill quota (one kill
 *       every few days or you're out), the energy shop (/bliss shop: 1-hour trackers, rule
 *       requests, revives during a revive round), double-energy and glowing events, gem bans,
 *       hidden gem stages (every gem shows Pristine) and the buffed netherite pieces.</li>
 *   <li><b>Dragon Games</b> - the same one life (green name, red once out), every player has a
 *       points value (random tier at the start or set by staff) that their killer earns, spent in
 *       the shop; Flux/Speed/Astra/Strength and off-hand shields banned, a shrinking border and
 *       the Blood Moon finale (a timed red night: charged creepers, maxed zombies, enchanted
 *       skeleton bows, strong spiders, no sleeping). The last one standing gets the Dragon Egg.</li>
 * </ul>
 */
public final class EventGames implements Listener {
    public enum Mode { ENERGY, DRAGON }

    private final BlissGems plugin;
    private final File file;
    private final NamespacedKey pieceKey;
    private final NamespacedKey trackerKey;
    private Mode mode;
    private final Set<UUID> players = new HashSet<>();
    private final Set<UUID> out = new HashSet<>();
    private final Map<UUID, Long> lastKill = new HashMap<>();
    private final Map<UUID, Integer> points = new HashMap<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> worth = new HashMap<>();
    private final Set<String> bannedGems = new HashSet<>();
    private final Map<UUID, Long> doubleUntil = new HashMap<>();
    private final Map<UUID, Long> glowDeadline = new HashMap<>();
    private boolean bloodMoon;
    private long bloodMoonStart;
    private long bloodMoonUntil;
    private BossBar bloodBar;
    private long startedAt;
    /** Shaderpack signal day for the Blood Moon's red sky (see DreamSky for how signals work). */
    private static final long BLOOD_DAY = 80040L;

    public EventGames(BlissGems plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "games.yml");
        this.pieceKey = new NamespacedKey(plugin, "games-piece");
        this.trackerKey = new NamespacedKey(plugin, "tracker-expires");
        this.load();
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 40L);
    }

    public boolean running() {
        return this.mode != null;
    }

    public Mode mode() {
        return this.mode;
    }

    public boolean isOut(Player p) {
        return this.out.contains(p.getUniqueId());
    }

    /** Energy Games hide gem stages: every gem looks Pristine. */
    public boolean hidesGemStages() {
        return this.mode == Mode.ENERGY && this.plugin.getConfig().getBoolean("games.energy.hide-gem-stages", true);
    }

    /** Gem abilities of a banned gem don't work while the games run (ability keys are "<gem>-..."). */
    public boolean abilityBanned(String abilityKey) {
        if (this.mode == null || abilityKey == null) return false;
        for (String gem : this.bannedGems) if (abilityKey.startsWith(gem + "-")) return true;
        return false;
    }

    // ---- commands ----

    public void command(CommandSender sender, String[] args) {
        String sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "status";
        switch (sub) {
            case "energy", "dragon" -> {
                if (args.length >= 3 && args[2].equalsIgnoreCase("start")) this.start(sender, sub.equals("energy") ? Mode.ENERGY : Mode.DRAGON);
                else sender.sendMessage("§7/bliss games " + sub + " start");
            }
            case "stop" -> this.stop(sender);
            case "eliminate" -> this.withPlayer(sender, args, p -> {
                if (this.mode == null) sender.sendMessage("§cNo games are running.");
                else if (this.out.contains(p.getUniqueId())) sender.sendMessage("§7" + p.getName() + " is already out.");
                else {
                    this.players.add(p.getUniqueId());
                    this.eliminate(p, "removed by staff");
                    sender.sendMessage("§a" + p.getName() + " is eliminated.");
                }
            });
            case "revive" -> this.reviveCommand(sender, args);
            case "worth" -> this.worthCommand(sender, args);
            case "ban" -> this.gemBan(sender, args, true);
            case "unban" -> this.gemBan(sender, args, false);
            case "points" -> this.pointsCommand(sender, args);
            case "double" -> this.doubleEnergy(sender, args);
            case "glow" -> this.glowEvent(sender, args);
            case "border" -> this.border(sender, args);
            case "bloodmoon" -> {
                if (args.length >= 3 && args[2].equalsIgnoreCase("off")) this.endBloodMoon();
                else this.startBloodMoon(args.length >= 4 ? parse(args[3], 0) : args.length >= 3 ? parse(args[2], 0) : 0);
            }
            case "piece" -> this.givePiece(sender, args);
            default -> this.status(sender);
        }
    }

    private void status(CommandSender sender) {
        if (this.mode == null) {
            sender.sendMessage("§7No games running. /bliss games <energy|dragon> start");
            return;
        }
        sender.sendMessage("§6" + this.title() + " §7- " + (this.players.size() - this.out.size()) + " left of " + this.players.size()
            + ", banned gems: " + (this.bannedGems.isEmpty() ? "none" : String.join(", ", this.bannedGems)));
        sender.sendMessage("§7/bliss games <stop|eliminate|revive|worth|points|ban|unban|double|glow|border|bloodmoon [minutes|off]|piece>");
    }

    private interface PlayerAction {
        void run(Player p);
    }

    private void withPlayer(CommandSender sender, String[] args, PlayerAction action) {
        Player p = args.length >= 3 ? Bukkit.getPlayer(args[2]) : null;
        if (p == null) {
            sender.sendMessage("§cPlayer not found.");
            return;
        }
        action.run(p);
    }

    private String title() {
        return this.mode == Mode.DRAGON ? "Dragon Games" : "Energy Games";
    }

    private void start(CommandSender sender, Mode mode) {
        if (this.mode != null) {
            sender.sendMessage("§cThe " + this.title() + " are already running. /bliss games stop first.");
            return;
        }
        this.mode = mode;
        this.players.clear();
        this.out.clear();
        this.lastKill.clear();
        this.points.clear();
        this.kills.clear();
        this.worth.clear();
        this.doubleUntil.clear();
        this.glowDeadline.clear();
        this.bannedGems.clear();
        this.bloodMoon = false;
        this.startedAt = System.currentTimeMillis();
        if (mode == Mode.DRAGON) this.bannedGems.addAll(this.plugin.getConfig().getStringList("games.dragon.banned-gems"));
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR || p.hasPermission("blissgems.admin") && !this.plugin.getConfig().getBoolean("games.include-staff", false)) continue;
            this.players.add(p.getUniqueId());
            if (mode == Mode.DRAGON) this.worth.put(p.getUniqueId(), this.randomWorth());
        }
        this.save();
        this.refreshNames();
        for (Player p : Bukkit.getOnlinePlayers()) this.plugin.getGemManager().updateGemTextures(p);
        this.announce("&6&l" + this.title().toUpperCase() + " &7have begun. &f" + this.players.size() + "&7 players, one life each.");
        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1.0f, 1.0f);
    }

    private void stop(CommandSender sender) {
        if (this.mode == null) {
            sender.sendMessage("§7Nothing is running.");
            return;
        }
        this.announce("&6&l" + this.title().toUpperCase() + " &7are over.");
        if (this.bloodMoon) this.endBloodMoon();
        this.mode = null;
        this.save();
        this.refreshNames();
        for (Player p : Bukkit.getOnlinePlayers()) this.plugin.getGemManager().updateGemTextures(p); // real gem stages again
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (this.out.contains(p.getUniqueId()) && p.getGameMode() == GameMode.SPECTATOR) p.setGameMode(GameMode.SURVIVAL);
        }
    }

    private void gemBan(CommandSender sender, String[] args, boolean ban) {
        if (args.length < 3) {
            sender.sendMessage("§7/bliss games " + (ban ? "ban" : "unban") + " <gem>");
            return;
        }
        String gem = args[2].toLowerCase(Locale.ROOT);
        if (ban) this.bannedGems.add(gem);
        else this.bannedGems.remove(gem);
        this.save();
        this.announce("&7The &f" + gem + " &7gem is " + (ban ? "&cbanned" : "&aallowed again") + "&7.");
    }

    // ---- eliminations ----

    private void eliminate(Player p, String why) {
        if (this.mode == null || !this.players.contains(p.getUniqueId()) || !this.out.add(p.getUniqueId())) return;
        this.save();
        this.refreshNames();
        this.announce("&c&l✖ " + p.getName() + " &7is eliminated &8(" + why + ")&7. &f" + (this.players.size() - this.out.size()) + "&7 left.");
        if (this.plugin.getConfig().getBoolean("games.eliminated-spectate", true)) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                if (p.isOnline() && !p.isDead()) p.setGameMode(GameMode.SPECTATOR);
            }, 1L);
        }
        this.checkWinner();
    }

    private void revive(Player p) {
        if (!this.out.remove(p.getUniqueId())) return;
        this.lastKill.put(p.getUniqueId(), System.currentTimeMillis());
        this.save();
        this.refreshNames();
        if (p.getGameMode() == GameMode.SPECTATOR) {
            p.setGameMode(GameMode.SURVIVAL);
            p.teleport(p.getWorld().getSpawnLocation());
        }
        this.announce("&a&l✦ " + p.getName() + " &7is back in the games.");
    }

    private void checkWinner() {
        List<UUID> alive = new ArrayList<>(this.players);
        alive.removeAll(this.out);
        if (alive.size() != 1) return;
        Player winner = Bukkit.getPlayer(alive.get(0));
        String name = winner != null ? winner.getName() : Bukkit.getOfflinePlayer(alive.get(0)).getName();
        this.announce("&6&l✦ " + name + " WINS THE " + this.title().toUpperCase() + "!");
        if (this.mode == Mode.DRAGON && winner != null && this.plugin.getConfig().getBoolean("games.dragon.egg-to-winner", true)) {
            for (ItemStack left : winner.getInventory().addItem(new ItemStack(Material.DRAGON_EGG)).values()) winner.getWorld().dropItemNaturally(winner.getLocation(), left);
        }
        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (this.mode == null) return;
        Player victim = event.getEntity();
        if (!this.players.contains(victim.getUniqueId()) || this.out.contains(victim.getUniqueId())) return;
        Player killer = victim.getKiller();
        if (killer != null && this.players.contains(killer.getUniqueId()) && !this.out.contains(killer.getUniqueId())) {
            this.lastKill.put(killer.getUniqueId(), System.currentTimeMillis());
            this.glowDeadline.remove(killer.getUniqueId());
            int victimKills = this.kills.getOrDefault(victim.getUniqueId(), 0);
            this.kills.merge(killer.getUniqueId(), 1, Integer::sum);
            if (this.mode == Mode.DRAGON) {
                // the killer earns the victim's points value (stronger players are worth more)
                int worth = this.worth(victim.getUniqueId()) + victimKills * this.plugin.getConfig().getInt("games.dragon.points-per-victim-kill", 5);
                this.points.merge(killer.getUniqueId(), worth, Integer::sum);
                killer.sendMessage("§6+" + worth + " points §7(" + this.points(killer) + " total)");
            } else if (this.doubleUntil.getOrDefault(killer.getUniqueId(), 0L) > System.currentTimeMillis()) {
                this.plugin.getEnergyManager().addEnergy(killer, this.plugin.getConfigManager().getEnergyGainOnKill());
                killer.sendMessage("§b§lDOUBLE ENERGY §7- the kill paid twice.");
            }
        }
        this.eliminate(victim, killer != null ? "killed by " + killer.getName() : "died");
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (this.mode != null && this.out.contains(event.getPlayer().getUniqueId()) && this.plugin.getConfig().getBoolean("games.eliminated-spectate", true)) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> event.getPlayer().setGameMode(GameMode.SPECTATOR), 1L);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (this.mode != null) this.refreshNames();
    }

    // ---- energy events ----

    private List<Player> randomHalf() {
        List<Player> alive = new ArrayList<>();
        for (UUID id : this.players) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && !this.out.contains(id)) alive.add(p);
        }
        Collections.shuffle(alive);
        return alive.subList(0, (alive.size() + 1) / 2);
    }

    private void doubleEnergy(CommandSender sender, String[] args) {
        int minutes = args.length >= 3 ? parse(args[2], 120) : 120;
        long until = System.currentTimeMillis() + minutes * 60_000L;
        List<String> names = new ArrayList<>();
        for (Player p : this.randomHalf()) {
            this.doubleUntil.put(p.getUniqueId(), until);
            names.add(p.getName());
            p.sendMessage("§b§lDOUBLE ENERGY §7- your kills pay twice for " + minutes + " minutes.");
        }
        this.save();
        this.announce("&b&lDOUBLE ENERGY &7for " + minutes + " minutes: &f" + String.join(", ", names));
    }

    private void glowEvent(CommandSender sender, String[] args) {
        int hours = args.length >= 3 ? parse(args[2], 5) : 5;
        long deadline = System.currentTimeMillis() + hours * 3_600_000L;
        List<String> names = new ArrayList<>();
        for (Player p : this.randomHalf()) {
            this.glowDeadline.put(p.getUniqueId(), deadline);
            names.add(p.getName());
            p.sendMessage("§e§lGLOWING §7- get a kill within " + hours + " hours or you're out.");
        }
        this.save();
        this.announce("&e&lGLOWING EVENT &7(" + hours + "h to get a kill): &f" + String.join(", ", names));
    }

    private void border(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§7/bliss games border <size> <minutes>");
            return;
        }
        int size = parse(args[2], 0);
        int minutes = parse(args[3], 0);
        if (size <= 0) return;
        World w = sender instanceof Player p ? p.getWorld() : Bukkit.getWorlds().get(0);
        w.getWorldBorder().setSize(size, Math.max(0, minutes) * 60L);
        this.announce("&c&lTHE BORDER CLOSES &7to &f" + size + "&7 blocks over " + minutes + " minutes.");
    }

    private static int parse(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ---- Blood Moon ----

    private void startBloodMoon(int minutes) {
        if (minutes <= 0) minutes = this.plugin.getConfig().getInt("games.blood-moon-minutes", 90);
        this.bloodMoon = true;
        this.bloodMoonStart = System.currentTimeMillis();
        this.bloodMoonUntil = this.bloodMoonStart + minutes * 60_000L;
        this.save();
        this.showBloodMoon();
        this.announce("&4&lTHE BLOOD MOON RISES &7- " + minutes + " minutes. No alliances, and the night is hungry.");
        for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);
    }

    private void endBloodMoon() {
        if (!this.bloodMoon) return;
        this.bloodMoon = false;
        this.save();
        if (this.bloodBar != null) {
            this.bloodBar.removeAll();
            this.bloodBar = null;
        }
        for (Player p : Bukkit.getOnlinePlayers()) p.resetPlayerTime();
        this.announce("&7The Blood Moon sets.");
    }

    /** The red bar ("The Blood Moon", draining over its run) and a red night sky for everyone in the overworld. */
    private void showBloodMoon() {
        if (this.bloodBar == null) {
            this.bloodBar = Bukkit.createBossBar("§4§l- The Blood Moon -", BarColor.RED, BarStyle.SOLID, BarFlag.DARKEN_SKY, BarFlag.CREATE_FOG);
        }
        long now = System.currentTimeMillis();
        double left = (this.bloodMoonUntil - now) / (double) Math.max(1L, this.bloodMoonUntil - this.bloodMoonStart);
        this.bloodBar.setProgress(Math.max(0.0, Math.min(1.0, left)));
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean overworld = p.getWorld().getEnvironment() == World.Environment.NORMAL;
            if (overworld) {
                this.bloodBar.addPlayer(p);
                // frozen at midnight on the signal day: the shaderpack paints the sky and moon red
                long full = p.getWorld().getFullTime();
                p.setPlayerTime(BLOOD_DAY * 24000L + 18000L - (full - Math.floorMod(full, 24000L)), false);
            } else {
                this.bloodBar.removePlayer(p);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBed(PlayerBedEnterEvent event) {
        if (!this.bloodMoon) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage("§4You can't sleep through the Blood Moon.");
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (!this.bloodMoon || event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        if (event.getEntity() instanceof Creeper c) {
            c.setPowered(true);
        } else if (event.getEntity() instanceof Zombie z && z.getEquipment() != null) {
            // max gear: full Protection IV netherite and a Sharpness V sword
            z.getEquipment().setHelmet(enchanted(Material.NETHERITE_HELMET, Enchantment.PROTECTION, 4));
            z.getEquipment().setChestplate(enchanted(Material.NETHERITE_CHESTPLATE, Enchantment.PROTECTION, 4));
            z.getEquipment().setLeggings(enchanted(Material.NETHERITE_LEGGINGS, Enchantment.PROTECTION, 4));
            z.getEquipment().setBoots(enchanted(Material.NETHERITE_BOOTS, Enchantment.PROTECTION, 4));
            z.getEquipment().setItemInMainHand(enchanted(Material.NETHERITE_SWORD, Enchantment.SHARPNESS, 5));
            noDrops(z);
        } else if (event.getEntity() instanceof Skeleton sk && sk.getEquipment() != null) {
            ItemStack bow = enchanted(Material.BOW, Enchantment.POWER, 5);
            bow.addUnsafeEnchantment(Enchantment.PUNCH, 2);
            bow.addUnsafeEnchantment(Enchantment.FLAME, 1);
            sk.getEquipment().setItemInMainHand(bow);
            noDrops(sk);
        } else if (event.getEntity() instanceof Spider s) {
            s.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 1));
        }
    }

    private static ItemStack enchanted(Material material, Enchantment enchantment, int level) {
        ItemStack item = new ItemStack(material);
        item.addUnsafeEnchantment(enchantment, level);
        return item;
    }

    private static void noDrops(org.bukkit.entity.LivingEntity mob) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            try {
                mob.getEquipment().setDropChance(slot, 0.0f);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    // ---- the buffed netherite pieces ----

    private void givePiece(CommandSender sender, String[] args) {
        Player p = args.length >= 4 ? Bukkit.getPlayer(args[2]) : null;
        if (p == null) {
            sender.sendMessage("§7/bliss games piece <player> <sword|axe|helmet|chestplate|leggings|boots>");
            return;
        }
        ItemStack item;
        String lore;
        switch (args[3].toLowerCase(Locale.ROOT)) {
            case "sword" -> {
                item = new ItemStack(Material.NETHERITE_SWORD);
                item.addUnsafeEnchantment(Enchantment.SHARPNESS, 6);
                lore = "Sharpness VI";
            }
            case "axe" -> {
                item = new ItemStack(Material.NETHERITE_AXE);
                item.addUnsafeEnchantment(Enchantment.SHARPNESS, 6);
                lore = "Sharpness VI";
            }
            case "helmet" -> {
                item = new ItemStack(Material.NETHERITE_HELMET);
                lore = "+2 hearts";
            }
            case "chestplate" -> {
                item = new ItemStack(Material.NETHERITE_CHESTPLATE);
                lore = "Strength I while worn";
            }
            case "leggings" -> {
                item = new ItemStack(Material.NETHERITE_LEGGINGS);
                lore = "Weaving while worn";
            }
            case "boots" -> {
                item = new ItemStack(Material.NETHERITE_BOOTS);
                lore = "Speed I while worn";
            }
            default -> {
                sender.sendMessage("§cUnknown piece.");
                return;
            }
        }
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§lMythic " + capitalise(args[3]));
        meta.setLore(List.of("§e" + lore, "§8Energy Games"));
        meta.getPersistentDataContainer().set(this.pieceKey, PersistentDataType.STRING, args[3].toLowerCase(Locale.ROOT));
        item.setItemMeta(meta);
        for (ItemStack left : p.getInventory().addItem(item).values()) p.getWorld().dropItemNaturally(p.getLocation(), left);
        sender.sendMessage("§aGave " + p.getName() + " the Mythic " + capitalise(args[3]) + ".");
    }

    private static String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    private String piece(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(this.pieceKey, PersistentDataType.STRING);
    }

    // ---- revives and points values ----

    /**
     * Staff revive for free; anyone else pays the revived player's points value (Dragon Games) or
     * the shop's revive price in energy (Energy Games).
     */
    public void reviveCommand(CommandSender sender, String[] args) {
        Player target = args.length >= 3 ? Bukkit.getPlayer(args[2]) : null;
        if (target == null) {
            sender.sendMessage("§7/bliss games revive <player>");
            return;
        }
        if (this.mode == null) {
            sender.sendMessage("§cNo games are running.");
            return;
        }
        if (!this.out.contains(target.getUniqueId())) {
            sender.sendMessage("§7" + target.getName() + " is still in the games.");
            return;
        }
        if (!sender.hasPermission("blissgems.admin")) {
            if (!(sender instanceof Player payer) || !this.players.contains(payer.getUniqueId()) || this.out.contains(payer.getUniqueId())) {
                sender.sendMessage("§cOnly players still in the games can buy a revive.");
                return;
            }
            int cost = this.reviveCost(target);
            boolean paid;
            if (this.mode == Mode.DRAGON) {
                paid = this.spendPoints(payer, cost);
            } else {
                // energy can't be spent below 1 (that would break the gem)
                paid = this.plugin.getEnergyManager().getEnergy(payer) - cost >= 1;
                if (paid) this.plugin.getEnergyManager().removeEnergy(payer, cost);
            }
            if (!paid) {
                sender.sendMessage("§cReviving " + target.getName() + " costs " + cost + (this.mode == Mode.DRAGON ? " points." : " energy."));
                return;
            }
        }
        this.revive(target);
        sender.sendMessage("§a" + target.getName() + " is back in the games.");
    }

    public int reviveCost(Player target) {
        return this.mode == Mode.DRAGON ? this.worth(target.getUniqueId()) : this.plugin.getConfig().getInt("games.energy.revive-cost", 1);
    }

    private void worthCommand(CommandSender sender, String[] args) {
        Player p = args.length >= 3 ? Bukkit.getPlayer(args[2]) : null;
        if (p == null) {
            sender.sendMessage("§7/bliss games worth <player> [points]");
            return;
        }
        if (args.length >= 4) {
            this.worth.put(p.getUniqueId(), Math.max(0, parse(args[3], 0)));
            this.save();
        }
        sender.sendMessage("§6" + p.getName() + " §7is worth §f" + this.worth(p.getUniqueId()) + " §7points.");
    }

    /** /bliss games points <player> [amount|+amount|-amount]: shows, sets or changes a player's Dragon Games points. */
    private void pointsCommand(CommandSender sender, String[] args) {
        Player p = args.length >= 3 ? Bukkit.getPlayer(args[2]) : null;
        if (p == null) {
            sender.sendMessage("§7/bliss games points <player> [amount|+amount|-amount]");
            return;
        }
        if (args.length >= 4) {
            String arg = args[3];
            boolean relative = arg.startsWith("+") || arg.startsWith("-");
            int amount = parse(relative && arg.startsWith("+") ? arg.substring(1) : arg, 0);
            this.points.put(p.getUniqueId(), Math.max(0, relative ? this.points(p) + amount : amount));
            this.save();
        }
        sender.sendMessage("§6" + p.getName() + " §7has §f" + this.points(p) + " §7points.");
    }

    public int worth(UUID id) {
        return this.worth.getOrDefault(id, this.plugin.getConfig().getInt("games.dragon.default-worth", 15));
    }

    /** A random tier from games.dragon.worth-tiers (repeat a value to make it likelier). */
    private int randomWorth() {
        List<Integer> tiers = this.plugin.getConfig().getIntegerList("games.dragon.worth-tiers");
        if (tiers.isEmpty()) return this.plugin.getConfig().getInt("games.dragon.default-worth", 15);
        return tiers.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(tiers.size()));
    }

    // ---- shop hooks ----

    public int points(Player p) {
        return this.points.getOrDefault(p.getUniqueId(), 0);
    }

    public boolean spendPoints(Player p, int cost) {
        int have = this.points(p);
        if (have < cost) return false;
        this.points.put(p.getUniqueId(), have - cost);
        this.save();
        return true;
    }

    public boolean isPlayer(Player p) {
        return this.players.contains(p.getUniqueId());
    }

    /** A compass that points at the nearest other player still in the games, for an hour. */
    public ItemStack tracker() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.setDisplayName("§a§lPlayer Tracker");
        meta.setLore(List.of("§7Points at the nearest player still in the games.", "§8Crumbles after one hour."));
        meta.getPersistentDataContainer().set(this.trackerKey, PersistentDataType.LONG, System.currentTimeMillis()
            + this.plugin.getConfig().getInt("games.tracker-minutes", 60) * 60_000L);
        compass.setItemMeta(meta);
        return compass;
    }

    public void announce(String message) {
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    // ---- periodic ----

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            this.updateTrackers(p, now);
            this.pieceEffects(p);
        }
        if (this.bloodMoon) {
            if (now >= this.bloodMoonUntil) this.endBloodMoon();
            else this.showBloodMoon();
        }
        if (this.mode == null) return;
        if (this.mode == Mode.DRAGON && this.plugin.getConfig().getBoolean("games.dragon.ban-offhand-shields", true)) {
            for (UUID id : this.players) {
                Player p = Bukkit.getPlayer(id);
                if (p == null || p.getInventory().getItemInOffHand().getType() != Material.SHIELD) continue;
                ItemStack shield = p.getInventory().getItemInOffHand();
                p.getInventory().setItemInOffHand(null);
                for (ItemStack left : p.getInventory().addItem(shield).values()) p.getWorld().dropItemNaturally(p.getLocation(), left);
                p.sendMessage("§cOff-hand shields are banned in the Dragon Games.");
            }
        }
        long quota = this.mode == Mode.ENERGY ? this.plugin.getConfig().getLong("games.energy.kill-quota-days", 4L) * 86_400_000L : 0L;
        for (UUID id : new ArrayList<>(this.players)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || this.out.contains(id)) continue;
            if (quota > 0 && now - this.lastKill.getOrDefault(id, this.startedAt) > quota) this.eliminate(p, "missed the kill quota");
            Long glow = this.glowDeadline.get(id);
            if (glow != null) {
                if (now > glow) {
                    this.glowDeadline.remove(id);
                    this.eliminate(p, "no kill while glowing");
                } else {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false, false));
                }
            }
        }
    }

    private void updateTrackers(Player p, long now) {
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType() != Material.COMPASS || !it.hasItemMeta()) continue;
            Long expires = it.getItemMeta().getPersistentDataContainer().get(this.trackerKey, PersistentDataType.LONG);
            if (expires == null) continue;
            if (now > expires) {
                p.getInventory().setItem(i, null);
                p.sendMessage("§7Your Player Tracker crumbled.");
                continue;
            }
            Player nearest = null;
            double best = Double.MAX_VALUE;
            for (Player other : p.getWorld().getPlayers()) {
                if (other.equals(p) || other.getGameMode() == GameMode.SPECTATOR || this.mode != null && (!this.players.contains(other.getUniqueId()) || this.out.contains(other.getUniqueId()))) continue;
                double d = other.getLocation().distanceSquared(p.getLocation());
                if (d < best) {
                    best = d;
                    nearest = other;
                }
            }
            if (nearest == null) continue;
            CompassMeta meta = (CompassMeta) it.getItemMeta();
            meta.setLodestone(nearest.getLocation());
            meta.setLodestoneTracked(false);
            it.setItemMeta(meta);
        }
    }

    private void pieceEffects(Player p) {
        // +2 hearts while the helmet is on (an item attribute would drop netherite's own armour values)
        org.bukkit.attribute.AttributeInstance hp = p.getAttribute(Attributes.maxHealth());
        if (hp != null) {
            NamespacedKey key = new NamespacedKey(this.plugin, "games-helmet");
            AttributeModifier current = null;
            for (AttributeModifier m : hp.getModifiers()) if (key.equals(m.getKey())) current = m;
            boolean wearing = "helmet".equals(this.piece(p.getInventory().getHelmet()));
            if (wearing && current == null) {
                hp.addModifier(new AttributeModifier(key, 4.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
            } else if (!wearing && current != null) {
                hp.removeModifier(current);
                if (p.getHealth() > hp.getValue()) p.setHealth(hp.getValue());
            }
        }
        ItemStack chest = p.getInventory().getChestplate();
        ItemStack legs = p.getInventory().getLeggings();
        ItemStack boots = p.getInventory().getBoots();
        if ("chestplate".equals(this.piece(chest))) p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0, false, false, true));
        if ("boots".equals(this.piece(boots))) p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false, true));
        if ("leggings".equals(this.piece(legs))) {
            PotionEffectType weaving = org.bukkit.Registry.EFFECT.get(NamespacedKey.minecraft("weaving"));
            if (weaving != null) p.addPotionEffect(new PotionEffect(weaving, 60, 0, false, false, true));
        }
    }

    // ---- name colours ----

    private void refreshNames() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team in = team(board, "bliss_games_in", this.mode == Mode.DRAGON ? ChatColor.GREEN : ChatColor.BLUE);
        Team gone = team(board, "bliss_games_out", ChatColor.RED);
        for (Player p : Bukkit.getOnlinePlayers()) {
            String entry = p.getName();
            if (this.mode == null || !this.players.contains(p.getUniqueId())) {
                in.removeEntry(entry);
                gone.removeEntry(entry);
            } else if (this.out.contains(p.getUniqueId())) {
                gone.addEntry(entry);
            } else {
                in.addEntry(entry);
            }
        }
    }

    private static Team team(Scoreboard board, String name, ChatColor color) {
        Team t = board.getTeam(name);
        if (t == null) t = board.registerNewTeam(name);
        t.setColor(color);
        return t;
    }

    // ---- persistence ----

    private void load() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(this.file);
        String m = y.getString("mode");
        this.mode = m == null ? null : Mode.valueOf(m);
        for (String s : y.getStringList("participants")) this.players.add(UUID.fromString(s));
        for (String s : y.getStringList("out")) this.out.add(UUID.fromString(s));
        this.bannedGems.addAll(y.getStringList("banned-gems"));
        this.bloodMoon = y.getBoolean("blood-moon");
        this.bloodMoonStart = y.getLong("blood-moon-start");
        this.bloodMoonUntil = y.getLong("blood-moon-until");
        this.startedAt = y.getLong("started-at", System.currentTimeMillis());
        if (y.isConfigurationSection("last-kill")) for (String k : y.getConfigurationSection("last-kill").getKeys(false)) this.lastKill.put(UUID.fromString(k), y.getLong("last-kill." + k));
        if (y.isConfigurationSection("points")) for (String k : y.getConfigurationSection("points").getKeys(false)) this.points.put(UUID.fromString(k), y.getInt("points." + k));
        if (y.isConfigurationSection("worth")) for (String k : y.getConfigurationSection("worth").getKeys(false)) this.worth.put(UUID.fromString(k), y.getInt("worth." + k));
        if (y.isConfigurationSection("kills")) for (String k : y.getConfigurationSection("kills").getKeys(false)) this.kills.put(UUID.fromString(k), y.getInt("kills." + k));
        if (y.isConfigurationSection("double-until")) for (String k : y.getConfigurationSection("double-until").getKeys(false)) this.doubleUntil.put(UUID.fromString(k), y.getLong("double-until." + k));
        if (y.isConfigurationSection("glow-deadline")) for (String k : y.getConfigurationSection("glow-deadline").getKeys(false)) this.glowDeadline.put(UUID.fromString(k), y.getLong("glow-deadline." + k));
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("mode", this.mode == null ? null : this.mode.name());
        y.set("participants", this.players.stream().map(UUID::toString).toList());
        y.set("out", this.out.stream().map(UUID::toString).toList());
        y.set("banned-gems", new ArrayList<>(this.bannedGems));
        y.set("blood-moon", this.bloodMoon);
        y.set("blood-moon-start", this.bloodMoonStart);
        y.set("blood-moon-until", this.bloodMoonUntil);
        y.set("started-at", this.startedAt);
        this.lastKill.forEach((k, v) -> y.set("last-kill." + k, v));
        this.points.forEach((k, v) -> y.set("points." + k, v));
        this.kills.forEach((k, v) -> y.set("kills." + k, v));
        this.worth.forEach((k, v) -> y.set("worth." + k, v));
        this.doubleUntil.forEach((k, v) -> y.set("double-until." + k, v));
        this.glowDeadline.forEach((k, v) -> y.set("glow-deadline." + k, v));
        try {
            y.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("games.yml: " + e.getMessage());
        }
    }
}
