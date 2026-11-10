package dev.xoperr.blissgems.progression;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Attributes;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Bliss Season 3's Progressions (overworld -> nether -> end -> villagers -> netherite). The server
 * is in one stage at a time; each stage opens part of the game and sets kit limits:
 * <ol>
 *   <li>Nether and End closed, plain diamond armour, Protection I, no potions. Eight Enchanted
 *       Obsidian in a portal frame open the Nether and start Progression 2.</li>
 *   <li>Nether open; Protection III / Sharpness III, Tier 1 potions. The first visit to the Nether
 *       wakes a custom Wither that drops 4 Upgraders, 2 Restoration Books and 8 Traders.</li>
 *   <li>End open; Upgraders craftable, Sharpness V, chorus fruit capped at 16. The Dragon Egg's
 *       holder gets 20 hearts and halved gem cooldowns.</li>
 *   <li>Villager trading, chorus limit gone.</li>
 *   <li>Season 3 kit: Protection III, Sharpness V, Knockback I, no Thorns, Strength/Speed I, no
 *       Turtle Master or enchanted golden apples; Meteor Shower, Item Lock and Flux charging off.</li>
 * </ol>
 * Everything is off unless progression.enabled is true; limits live in config.yml per stage.
 */
public final class Progression implements Listener {
    private final BlissGems plugin;
    private final File file;
    private final NamespacedKey eggKey;
    private final NamespacedKey bossKey;
    private int stage = 1;
    private boolean witherSpawned;
    private final Set<String> obsidian = new HashSet<>();
    private final Set<java.util.UUID> yellowTab = new HashSet<>();

    public Progression(BlissGems plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "progression.yml");
        this.eggKey = new NamespacedKey(plugin, "dragon-egg-hearts");
        this.bossKey = new NamespacedKey(plugin, "progression-wither");
        YamlConfiguration y = YamlConfiguration.loadConfiguration(this.file);
        this.stage = Math.max(1, Math.min(5, y.getInt("stage", 1)));
        this.witherSpawned = y.getBoolean("wither-spawned");
        this.obsidian.addAll(y.getStringList("enchanted-obsidian"));
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 40L);
    }

    public boolean enabled() {
        return this.plugin.getConfig().getBoolean("progression.enabled", false);
    }

    public int stage() {
        return this.stage;
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("stage", this.stage);
        y.set("wither-spawned", this.witherSpawned);
        y.set("enchanted-obsidian", new ArrayList<>(this.obsidian));
        try {
            y.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("progression.yml: " + e.getMessage());
        }
    }

    private ConfigurationSection limits() {
        return this.stageSection(this.stage);
    }

    private ConfigurationSection stageSection(int stage) {
        ConfigurationSection stages = this.plugin.getConfig().getConfigurationSection("progression.stages");
        return stages == null ? null : stages.getConfigurationSection(String.valueOf(stage));
    }

    // ---- admin ----

    public void command(org.bukkit.command.CommandSender sender, String[] args) {
        String sub = args.length >= 2 ? args[1].toLowerCase() : "status";
        // any phase, any direction: set <n>, a bare number, next, prev
        Integer target = switch (sub) {
            case "set" -> args.length >= 3 ? parsePhase(args[2]) : null;
            case "next" -> this.stage + 1;
            case "prev", "back" -> this.stage - 1;
            default -> parsePhase(sub);
        };
        if (target != null) {
            if (target < 1 || target > 5) {
                sender.sendMessage("§cThere are phases 1 to 5 (now in " + this.stage + ").");
                return;
            }
            if (target == this.stage) {
                sender.sendMessage("§7Already in phase " + this.stage + ".");
                return;
            }
            this.setStage(target);
            sender.sendMessage("§aProgression set to phase " + this.stage + " §f(" + this.phaseName(this.stage) + ")"
                + (this.enabled() ? "" : " §c- progression.enabled is false, so nothing changes in game until it is on"));
            return;
        }
        if (sub.equals("set")) {
            sender.sendMessage("§c/bliss progression set <1-5>");
            return;
        }
        if (sender instanceof Player p) this.showPhase(p);
        sender.sendMessage("§6Phase §e" + this.stage + "§6/5 §f" + this.phaseName(this.stage) + " " + (this.enabled() ? "§a(on)" : "§c(off - progression.enabled)"));
        sender.sendMessage("§7Enchanted Obsidian placed: §f" + this.obsidian.size() + "§7, Wither " + (this.witherSpawned ? "woken" : "asleep"));
        sender.sendMessage("§7/bliss progression <status|set <1-5>|1-5|next|prev>");
    }

    private static Integer parsePhase(String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void setStage(int next) {
        next = Math.max(1, Math.min(5, next));
        if (next == this.stage) return;
        this.stage = next;
        // going back before the Nether lets its Wither wake again
        if (next < 2) this.witherSpawned = false;
        this.save();
        if (!this.enabled()) return;
        ConfigurationSection section = this.stageSection(next);
        double border = section == null ? 0.0 : section.getDouble("border", 0.0);
        if (border > 0) {
            for (World w : Bukkit.getWorlds()) w.getWorldBorder().setSize(w.getEnvironment() == World.Environment.NETHER ? border / 8.0 : border);
        }
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&6&l✦ PHASE " + next + " &7- &f" + this.phaseName(next) + " &7has begun."));
        for (Player p : Bukkit.getOnlinePlayers()) {
            this.showPhase(p);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.8f);
        }
    }

    /** "Phase N" with the phase's name under it (progression.stages.N.name). */
    public String phaseName(int stage) {
        ConfigurationSection section = this.stageSection(stage);
        String name = section == null ? null : section.getString("name");
        return name != null ? name : "Phase " + stage;
    }

    private void showPhase(Player p) {
        p.sendTitle("§aPhase " + this.stage, "§f" + this.phaseName(this.stage), 10, 70, 20);
    }

    // ---- dimensions ----

    private boolean dimensionOpen(World.Environment env) {
        if (env == World.Environment.NETHER) return this.stage >= 2;
        if (env == World.Environment.THE_END) return this.stage >= 3;
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        if (!this.enabled() || event.getTo() == null || event.getTo().getWorld() == null) return;
        World.Environment env = event.getTo().getWorld().getEnvironment();
        if (!this.dimensionOpen(env) && !event.getPlayer().hasPermission("blissgems.admin")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cThe " + (env == World.Environment.NETHER ? "Nether" : "End") + " is sealed until a later Progression.");
        }
    }

    /** In Progression 1 a nether portal only lights inside a frame holding enough Enchanted Obsidian, and that opens the Nether. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        if (!this.enabled() || this.stage >= 2 || event.getReason() != PortalCreateEvent.CreateReason.FIRE) return;
        Set<String> frame = new HashSet<>();
        for (org.bukkit.block.BlockState state : event.getBlocks()) {
            if (state.getType() != Material.NETHER_PORTAL) continue;
            for (BlockFace face : new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                Block b = state.getBlock().getRelative(face);
                if (this.obsidian.contains(key(b))) frame.add(key(b));
            }
        }
        int needed = this.plugin.getConfig().getInt("progression.enchanted-obsidian-needed", 8);
        if (frame.size() < needed) {
            event.setCancelled(true);
            if (event.getEntity() instanceof Player p) p.sendMessage("§5The portal stays dark. §7It needs " + needed + " Enchanted Obsidian in its frame (" + frame.size() + " found).");
            return;
        }
        Bukkit.getScheduler().runTask(this.plugin, () -> this.setStage(2));
    }

    // ---- Enchanted Obsidian ----

    private static String key(Block b) {
        return b.getWorld().getName() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
    }

    private static boolean isEnchantedObsidian(ItemStack item) {
        return "enchanted_obsidian".equals(CustomItemManager.getIdByItem(item));
    }

    private static final String GLOW_TAG = "bliss_enchanted_obsidian";

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceObsidian(BlockPlaceEvent event) {
        if (isEnchantedObsidian(event.getItemInHand())) {
            this.obsidian.add(key(event.getBlockPlaced()));
            this.save();
            // a glowing outline on the placed block (a block display, saved with the chunk)
            Block b = event.getBlockPlaced();
            b.getWorld().spawn(b.getLocation(), BlockDisplay.class, d -> {
                d.setBlock(Material.CRYING_OBSIDIAN.createBlockData());
                d.setGlowing(true);
                d.setGlowColorOverride(Color.PURPLE);
                d.setTransformation(new Transformation(new Vector3f(-0.005f, -0.005f, -0.005f), new Quaternionf(), new Vector3f(1.01f, 1.01f, 1.01f), new Quaternionf()));
                d.addScoreboardTag(GLOW_TAG);
            });
        }
    }

    /** A dropped Enchanted Obsidian glows. */
    @EventHandler(ignoreCancelled = true)
    public void onObsidianDrop(ItemSpawnEvent event) {
        if (isEnchantedObsidian(event.getEntity().getItemStack())) event.getEntity().setGlowing(true);
    }

    /** Breaking a placed Enchanted Obsidian gives the item back, never a plain block. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreakObsidian(BlockBreakEvent event) {
        if (!this.obsidian.remove(key(event.getBlock()))) return;
        this.save();
        for (org.bukkit.entity.Entity e : event.getBlock().getWorld().getNearbyEntities(event.getBlock().getLocation().add(0.5, 0.5, 0.5), 0.6, 0.6, 0.6)) {
            if (e instanceof BlockDisplay && e.getScoreboardTags().contains(GLOW_TAG)) e.remove();
        }
        event.setDropItems(false);
        ItemStack item = CustomItemManager.getItemById("enchanted_obsidian");
        if (item != null) event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().add(0.5, 0.5, 0.5), item);
    }

    // ---- the Wither ----

    @EventHandler
    public void onEnterNether(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        if (!this.enabled() || this.stage != 2 || this.witherSpawned || p.getWorld().getEnvironment() != World.Environment.NETHER) return;
        if (!this.plugin.getConfig().getBoolean("progression.wither.enabled", true)) return;
        this.witherSpawned = true;
        this.save();
        Location at = p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(16)).add(0, 6, 0);
        Wither wither = p.getWorld().spawn(at, Wither.class, w -> {
            w.setCustomName("§5§lThe Nether's Warden");
            w.setCustomNameVisible(true);
            w.getPersistentDataContainer().set(this.bossKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            AttributeInstance hp = w.getAttribute(Attributes.maxHealth());
            if (hp != null) {
                hp.setBaseValue(this.plugin.getConfig().getDouble("progression.wither.health", 900.0));
                w.setHealth(hp.getBaseValue());
            }
        });
        wither.setRemoveWhenFarAway(false);
        Bukkit.broadcastMessage("§5§l" + p.getName() + " §dstepped into the Nether, and something woke up.");
    }

    @EventHandler
    public void onWitherDeath(EntityDeathEvent event) {
        if (!event.getEntity().getPersistentDataContainer().has(this.bossKey, org.bukkit.persistence.PersistentDataType.BYTE)) return;
        event.getDrops().clear();
        Location at = event.getEntity().getLocation();
        this.drop(at, "gem_upgrader", this.plugin.getConfig().getInt("progression.wither.upgraders", 4));
        this.drop(at, "restoration_book", this.plugin.getConfig().getInt("progression.wither.revive-books", 2));
        this.drop(at, "gem_trader", this.plugin.getConfig().getInt("progression.wither.traders", 8));
        Bukkit.broadcastMessage("§5§lThe Nether's Warden has fallen.");
    }

    private void drop(Location at, String id, int amount) {
        for (int i = 0; i < amount; i++) {
            ItemStack item = CustomItemManager.getItemById(id);
            if (item != null) at.getWorld().dropItemNaturally(at, item);
        }
    }

    // ---- crafting, trading, smithing ----

    @EventHandler(priority = EventPriority.HIGH)
    public void onCraft(PrepareItemCraftEvent event) {
        if (!this.enabled() || this.stage >= 3 || event.getInventory().getResult() == null) return;
        if ("gem_upgrader".equals(CustomItemManager.getIdByItem(event.getInventory().getResult()))) event.getInventory().setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSmithing(PrepareSmithingEvent event) {
        if (!this.enabled() || !this.plugin.getConfig().getBoolean("progression.block-netherite-smithing", true)) return;
        ItemStack result = event.getResult();
        if (result != null && result.getType().name().startsWith("NETHERITE_")) event.setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTrade(PlayerInteractEntityEvent event) {
        if (!this.enabled() || this.stage >= 4 || !(event.getRightClicked() instanceof Villager)) return;
        if (event.getPlayer().hasPermission("blissgems.admin")) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage("§cVillagers don't trade until Progression 4.");
    }

    // ---- kit limits ----

    private int cap(Enchantment ench) {
        ConfigurationSection l = this.limits();
        if (l == null) return -1;
        String k = ench.getKey().getKey();
        return l.isSet("enchants." + k) ? l.getInt("enchants." + k) : -1;
    }

    /** Lowers enchantments above the stage's caps (0 removes them). True when something changed. */
    private boolean clamp(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getEnchantments().isEmpty() || CustomItemManager.isMegaMace(item)) return false;
        boolean changed = false;
        for (Map.Entry<Enchantment, Integer> e : new HashMap<>(item.getEnchantments()).entrySet()) {
            int cap = this.cap(e.getKey());
            if (cap < 0 || e.getValue() <= cap) continue;
            item.removeEnchantment(e.getKey());
            if (cap > 0) item.addUnsafeEnchantment(e.getKey(), cap);
            changed = true;
        }
        return changed;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnvil(PrepareAnvilEvent event) {
        if (!this.enabled()) return;
        ItemStack result = event.getResult();
        if (this.clamp(result)) event.setResult(result);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (!this.enabled()) return;
        for (Map.Entry<Enchantment, Integer> e : new HashMap<>(event.getEnchantsToAdd()).entrySet()) {
            int cap = this.cap(e.getKey());
            if (cap == 0) event.getEnchantsToAdd().remove(e.getKey());
            else if (cap > 0 && e.getValue() > cap) event.getEnchantsToAdd().put(e.getKey(), cap);
        }
    }

    /** Potions: banned outright, or only up to a level, or specific types banned (stage config). */
    private String potionProblem(List<PotionEffect> effects) {
        ConfigurationSection l = this.limits();
        if (l == null) return null;
        if (!l.getBoolean("potions", true)) return "Potions are banned in this Progression.";
        int maxLevel = l.getInt("max-potion-level", 0);
        List<String> banned = l.getStringList("banned-potions");
        for (PotionEffect e : effects) {
            String name = e.getType().getKey().getKey();
            if (banned.contains(name)) return "That potion is banned in this Progression.";
            if (maxLevel > 0 && e.getAmplifier() + 1 > maxLevel) return "Potions above level " + maxLevel + " are banned in this Progression.";
        }
        return null;
    }

    private static List<PotionEffect> effectsOf(ItemStack item) {
        List<PotionEffect> out = new ArrayList<>();
        if (item != null && item.getItemMeta() instanceof PotionMeta meta) {
            if (meta.getBasePotionType() != null) out.addAll(meta.getBasePotionType().getPotionEffects());
            out.addAll(meta.getCustomEffects());
        }
        return out;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!this.enabled()) return;
        ConfigurationSection l = this.limits();
        ItemStack item = event.getItem();
        if (l != null && l.getStringList("banned-items").contains(item.getType().name())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cThat item is banned in this Progression.");
            return;
        }
        String problem = this.potionProblem(effectsOf(item));
        if (problem != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c" + problem);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSplash(PotionSplashEvent event) {
        if (!this.enabled()) return;
        ThrownPotion potion = event.getPotion();
        String problem = this.potionProblem(new ArrayList<>(potion.getEffects()));
        if (problem != null) {
            event.setCancelled(true);
            if (potion.getShooter() instanceof Player p) p.sendMessage("§c" + problem);
        }
    }

    /** Item caps (chorus fruit, totems): you can't pick up past the stage's limit. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!this.enabled() || !(event.getEntity() instanceof Player p)) return;
        ConfigurationSection l = this.limits();
        if (l == null) return;
        Material type = event.getItem().getItemStack().getType();
        if (!l.isSet("max-items." + type.name())) return;
        int max = l.getInt("max-items." + type.name());
        int have = 0;
        for (ItemStack it : p.getInventory().getContents()) if (it != null && it.getType() == type) have += it.getAmount();
        if (have + event.getItem().getItemStack().getAmount() > max) event.setCancelled(true);
    }

    /** Gem abilities switched off in this stage (abilities.cooldowns keys, plus flux-charging). */
    public boolean abilityBanned(String abilityKey) {
        if (!this.enabled()) return false;
        ConfigurationSection l = this.limits();
        return l != null && l.getStringList("banned-abilities").contains(abilityKey);
    }

    /** The Dragon Egg halves gem cooldowns from Progression 3 on. */
    public int adjustCooldown(Player player, int seconds) {
        if (!this.enabled() || this.stage < 3 || !player.getInventory().contains(Material.DRAGON_EGG)) return seconds;
        return Math.max(1, seconds / 2);
    }

    // ---- periodic: worn gear, obsidian holders, the egg ----

    private void tick() {
        boolean on = this.enabled();
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean egg = on && this.stage >= 3 && p.getInventory().contains(Material.DRAGON_EGG);
            this.eggHearts(p, egg);
            // the obsidian's holder glows whether or not Progressions are on (the item says so)
            boolean holder = false;
            for (ItemStack it : p.getInventory().getContents()) if (isEnchantedObsidian(it)) holder = true;
            if (holder) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false, false));
                p.setPlayerListName("§e" + p.getName());
            } else if (p.getPlayerListName().equals("§e" + p.getName()) && !p.isGlowing()) {
                p.setPlayerListName(null);
            }
            if (!on) continue;
            for (ItemStack armour : p.getInventory().getArmorContents()) this.clamp(armour);
            this.clamp(p.getInventory().getItemInMainHand());
            this.clamp(p.getInventory().getItemInOffHand());
            if (this.stage == 1 && !p.hasPermission("blissgems.admin")) this.diamondOnly(p);
        }
    }

    /** Progression 1: any armour piece that isn't diamond goes back into the inventory. */
    private void diamondOnly(Player p) {
        ItemStack[] armour = p.getInventory().getArmorContents();
        boolean moved = false;
        for (int i = 0; i < armour.length; i++) {
            ItemStack a = armour[i];
            if (a == null || a.getType().isAir() || a.getType().name().startsWith("DIAMOND_") || a.getType() == Material.ELYTRA
                || a.getType().name().endsWith("_HEAD") || a.getType() == Material.CARVED_PUMPKIN) continue;
            armour[i] = null;
            for (ItemStack left : p.getInventory().addItem(a).values()) p.getWorld().dropItemNaturally(p.getLocation(), left);
            moved = true;
        }
        if (moved) {
            p.getInventory().setArmorContents(armour);
            p.sendMessage("§cOnly plain diamond armour is allowed in Progression 1.");
        }
    }

    private void eggHearts(Player p, boolean on) {
        AttributeInstance attr = p.getAttribute(Attributes.maxHealth());
        if (attr == null) return;
        AttributeModifier current = null;
        for (AttributeModifier m : attr.getModifiers()) if (this.eggKey.equals(m.getKey())) current = m;
        if (on && current == null) {
            attr.addModifier(new AttributeModifier(this.eggKey, 20.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        } else if (!on && current != null) {
            attr.removeModifier(current);
            if (p.getHealth() > attr.getValue()) p.setHealth(attr.getValue());
        }
    }
}
