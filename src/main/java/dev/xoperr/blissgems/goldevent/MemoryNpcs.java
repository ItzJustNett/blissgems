package dev.xoperr.blissgems.goldevent;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.pedestal.PedestalManager;
import dev.xoperr.blissgems.utils.Attributes;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * Bot "memories": Citizens player-NPCs wearing a random, fully enchanted kit from memory_npcs.yml
 * that roam around where they were spawned and fight players who come within aggro-range, wearing
 * a pool name and an optional skin. They carry supplies (gapples, potions, a totem), block with a
 * shield, strafe, and use a gem's power. Killing one purges it and drops what it carried plus its
 * head; laying heads-needed heads (15) down at the memory world's spawn (the pedestal) brings the
 * great thunder and wakes the dreamers.
 * Citizens is an optional dependency and is only ever touched through reflection, so this
 * class works (as "unavailable") on servers without it.
 */
public final class MemoryNpcs implements Listener {
    /** The kits shipped before memories were diamond-only: replaced when memory_npcs.yml still has them. */
    private static final List<List<String>> OLD_KITS = List.of(
        List.of("HELMET:DIAMOND_HELMET", "CHESTPLATE:DIAMOND_CHESTPLATE", "LEGGINGS:DIAMOND_LEGGINGS", "BOOTS:DIAMOND_BOOTS", "HAND:DIAMOND_SWORD", "OFF_HAND:SHIELD"),
        List.of("HELMET:NETHERITE_HELMET", "CHESTPLATE:NETHERITE_CHESTPLATE", "LEGGINGS:NETHERITE_LEGGINGS", "BOOTS:NETHERITE_BOOTS", "HAND:NETHERITE_AXE"),
        List.of("HELMET:IRON_HELMET", "CHESTPLATE:DIAMOND_CHESTPLATE", "LEGGINGS:IRON_LEGGINGS", "BOOTS:DIAMOND_BOOTS", "HAND:MACE"),
        List.of("HELMET:GOLDEN_HELMET", "CHESTPLATE:NETHERITE_CHESTPLATE", "LEGGINGS:DIAMOND_LEGGINGS", "BOOTS:GOLDEN_BOOTS", "HAND:NETHERITE_SWORD", "OFF_HAND:TOTEM_OF_UNDYING"));
    private final BlissGems plugin;
    private final GoldenDream dream;
    private final MemoryRoster roster;
    private final NamespacedKey headKey;
    private final File file;
    private final YamlConfiguration cfg;
    private final boolean citizens;
    private final Map<String, Mem> live = new HashMap<>();
    private final Map<UUID, Mem> byEntity = new HashMap<>();
    private final List<ItemStack> loot = new ArrayList<>();
    private final List<Map<String, Material>> kits = new ArrayList<>();
    private final List<ItemStack> supplies = new ArrayList<>();
    private static final List<String> BOT_GEMS = List.of("fire", "puff", "speed", "strength", "life");
    private static final List<String> KIT_SLOTS = List.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "HAND", "OFF_HAND");
    private BukkitTask task;

    public MemoryNpcs(BlissGems plugin, GoldenDream dream, MemoryRoster roster) {
        this.plugin = plugin;
        this.dream = dream;
        this.roster = roster;
        this.headKey = new NamespacedKey(plugin, "memory_head");
        this.file = new File(plugin.getDataFolder(), "memory_npcs.yml");
        this.cfg = YamlConfiguration.loadConfiguration(this.file);
        this.citizens = Bukkit.getPluginManager().getPlugin("Citizens") != null && classExists("net.citizensnpcs.api.CitizensAPI");
        this.loadLoot();
        this.loadKits();
        this.loadSupplies();
        ConfigurationSection spawned = this.cfg.getConfigurationSection("spawned");
        if (spawned != null) for (String k : spawned.getKeys(false)) roster.claimSpecificName(spawned.getString(k + ".name"));
    }

    public boolean available() {
        return this.citizens;
    }

    /** Spawns one bot memory at the location; returns its name, or null when no name is free / it failed. */
    public String spawn(Location at, String skin) {
        if (!this.citizens || at == null || at.getWorld() == null) return null;
        String name = this.roster.claimFreeName();
        if (name == null) return null;
        Mem m = new Mem(UUID.randomUUID().toString().substring(0, 8), name, skin, at.clone());
        if (!this.spawnEntity(m)) {
            this.roster.releaseName(name);
            return null;
        }
        this.live.put(m.id, m);
        this.persist(m);
        this.cfg.set("goal.total", this.cfg.getInt("goal.total") + 1);
        this.save();
        this.start();
        return name;
    }

    /** Re-creates the bots saved in memory_npcs.yml (after a restart). */
    public void reload() {
        if (!this.citizens) return;
        ConfigurationSection spawned = this.cfg.getConfigurationSection("spawned");
        if (spawned == null) return;
        for (String id : new ArrayList<>(spawned.getKeys(false))) {
            String p = "spawned." + id + ".";
            World w = this.cfg.getString(p + "world") == null ? null : Bukkit.getWorld(this.cfg.getString(p + "world"));
            String name = this.cfg.getString(p + "name");
            if (w == null || name == null) continue;
            this.roster.claimSpecificName(name);
            Mem m = new Mem(id, name, this.cfg.getString(p + "skin"), new Location(w, this.cfg.getDouble(p + "x"), this.cfg.getDouble(p + "y"),
                this.cfg.getDouble(p + "z"), (float) this.cfg.getDouble(p + "yaw"), (float) this.cfg.getDouble(p + "pitch")));
            if (this.spawnEntity(m)) this.live.put(id, m);
        }
        if (!this.live.isEmpty()) this.start();
    }

    public int clearAll() {
        int n = 0;
        for (Mem m : new ArrayList<>(this.live.values())) {
            this.destroy(m);
            n++;
        }
        this.live.clear();
        this.cfg.set("spawned", null);
        this.cfg.set("goal", null);
        this.save();
        return n;
    }

    public int count() {
        return this.live.size();
    }

    public List<String> liveNames() {
        List<String> out = new ArrayList<>();
        for (Mem m : this.live.values()) out.add(m.name);
        return out;
    }

    public List<Location> liveLocations() {
        List<Location> out = new ArrayList<>();
        for (Mem m : this.live.values()) {
            Entity e = this.entity(m);
            if (e != null) out.add(e.getLocation());
        }
        return out;
    }

    private Mem byName(String name) {
        for (Mem m : this.live.values()) if (m.name.equalsIgnoreCase(name)) return m;
        return null;
    }

    public boolean rename(String oldName, String newName) {
        Mem m = this.byName(oldName);
        if (m == null || !this.roster.isNameFree(newName)) return false;
        this.roster.releaseName(m.name);
        this.roster.claimSpecificName(newName);
        m.name = newName;
        try {
            call(m.npc, "setName", newName);
            this.respawnInPlace(m);
        } catch (Throwable t) {
            this.plugin.getLogger().warning("NPC memory rename: " + t.getMessage());
        }
        this.persist(m);
        return true;
    }

    public boolean reskin(String name, String skin) {
        Mem m = this.byName(name);
        if (m == null) return false;
        m.skin = skin;
        try {
            Object trait = call(m.npc, "getOrAddTrait", Class.forName("net.citizensnpcs.trait.SkinTrait"));
            if (skin == null || skin.isBlank()) {
                call(trait, "clearTexture");
                this.respawnInPlace(m);
            } else {
                call(trait, "setSkinName", skin);
            }
        } catch (Throwable t) {
            this.plugin.getLogger().warning("NPC memory skin: " + t.getMessage());
        }
        this.persist(m);
        return true;
    }

    // ---- Citizens (reflection) ----

    private boolean spawnEntity(Mem m) {
        try {
            Object registry = Class.forName("net.citizensnpcs.api.CitizensAPI").getMethod("getNPCRegistry").invoke(null);
            Object npc = call(registry, "createNPC", EntityType.PLAYER, m.name);
            if (m.skin != null && !m.skin.isBlank()) {
                call(call(npc, "getOrAddTrait", Class.forName("net.citizensnpcs.trait.SkinTrait")), "setSkinName", m.skin);
            }
            Object data = call(npc, "data");
            Class<?> meta = Class.forName("net.citizensnpcs.api.npc.NPC$Metadata");
            call(data, "setPersistent", enumConst(meta, "SHOULD_SAVE"), false);
            call(data, "setPersistent", enumConst(meta, "DEFAULT_PROTECTED"), false);
            call(data, "setPersistent", enumConst(meta, "RESPAWN_DELAY"), -1);
            m.npc = npc;
            call(npc, "spawn", m.home);
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.gear(m), 5L);
            return true;
        } catch (Throwable t) {
            this.plugin.getLogger().warning("NPC memory spawn failed: " + t);
            return false;
        }
    }

    private void gear(Mem m) {
        try {
            Entity e = this.entity(m);
            if (e == null) return;
            this.byEntity.put(e.getUniqueId(), m);
            Object equipment = call(m.npc, "getOrAddTrait", Class.forName("net.citizensnpcs.api.trait.trait.Equipment"));
            Class<?> slot = Class.forName("net.citizensnpcs.api.trait.trait.Equipment$EquipmentSlot");
            if (m.kit < 0 || m.kit >= this.kits.size()) m.kit = ThreadLocalRandom.current().nextInt(this.kits.size());
            m.worn.clear();
            for (Map.Entry<String, Material> piece : this.kits.get(m.kit).entrySet()) {
                ItemStack item = this.enchanted(piece.getValue());
                m.worn.add(item);
                call(equipment, "set", enumConst(slot, piece.getKey()), item);
            }
            m.bag.clear();
            for (ItemStack supply : this.supplies) m.bag.add(supply.clone());
            if (m.gem == null) m.gem = BOT_GEMS.get(ThreadLocalRandom.current().nextInt(BOT_GEMS.size()));
            m.potted = false;
            m.geared = true;
        } catch (Throwable t) {
            this.plugin.getLogger().warning("NPC memory gear failed: " + t);
        }
    }

    private Entity entity(Mem m) {
        if (m.npc == null) return null;
        try {
            if (!(Boolean) call(m.npc, "isSpawned")) return null;
            return (Entity) call(m.npc, "getEntity");
        } catch (Throwable t) {
            return null;
        }
    }

    private void respawnInPlace(Mem m) throws Exception {
        Entity e = this.entity(m);
        if (e == null) return;
        Location at = e.getLocation();
        call(m.npc, "despawn");
        call(m.npc, "spawn", at);
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.gear(m), 5L);
    }

    private void destroy(Mem m) {
        m.destroyedByUs = true;
        Entity e = this.entity(m);
        if (e != null) this.byEntity.remove(e.getUniqueId());
        try {
            if (m.npc != null) call(m.npc, "destroy");
        } catch (Throwable ignored) {
        }
        this.roster.releaseName(m.name);
    }

    /** Kit pieces come fully enchanted (max protection / sharpness, unbreaking, mending). */
    private ItemStack enchanted(Material material) {
        ItemStack item = new ItemStack(material);
        String n = material.name();
        if (n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS")) {
            item.addUnsafeEnchantment(Enchantment.PROTECTION, 4);
        } else if (n.endsWith("_SWORD") || n.endsWith("_AXE")) {
            item.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
        } else if (material == Material.MACE) {
            item.addUnsafeEnchantment(Enchantment.DENSITY, 5);
        } else {
            return item;
        }
        item.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
        item.addUnsafeEnchantment(Enchantment.MENDING, 1);
        return item;
    }

    /** Sprinting bots wander 6-14 blocks around their home every few seconds. */
    private void start() {
        if (this.task == null) this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tickAll, 10L, 10L);
    }

    private void tickAll() {
        long now = System.currentTimeMillis();
        this.tickPedestal();
        for (Mem m : new ArrayList<>(this.live.values())) {
            try {
                this.tick(m, now);
            } catch (Throwable t) {
                if (!m.warned) {
                    m.warned = true;
                    this.plugin.getLogger().warning("NPC memory " + m.name + " tick failed: " + t);
                }
            }
        }
    }

    private void tick(Mem m, long now) throws Exception {
        Entity e = this.entity(m);
        if (e == null || !m.geared) return;
        if (e instanceof Player bot) bot.setSprinting(true);
        Object nav = call(m.npc, "getNavigator");
        Player prey = this.nearestPrey(e);
        if (prey != null) {
            // An aggressive Citizens target makes the bot chase and melee with what it holds
            if (!prey.getUniqueId().equals(m.prey) || !(Boolean) call(nav, "isNavigating")) {
                call(nav, "setTarget", prey, true);
                m.prey = prey.getUniqueId();
            }
            m.nextWander = 0L;
            if (e instanceof Player bot) this.fight(m, bot, prey, now);
            return;
        }
        m.prey = null;
        if (now < m.nextWander && (Boolean) call(nav, "isNavigating")) return;
        m.nextWander = now + 3000L + ThreadLocalRandom.current().nextInt(3000);
        World w = m.home.getWorld();
        if (w == null) return;
        double a = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
        double d = 6.0 + ThreadLocalRandom.current().nextDouble() * 8.0;
        Location target = m.home.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
        if (!w.isChunkLoaded(target.getBlockX() >> 4, target.getBlockZ() >> 4)) target = m.home.clone();
        target.setY(w.getHighestBlockYAt(target) + 1);
        call(nav, "setTarget", target);
        call(call(nav, "getLocalParameters"), "speedModifier", 1.3f);
    }

    // ---- fighting like a player ----

    private void fight(Mem m, Player bot, Player prey, long now) {
        double dist = bot.getLocation().distance(prey.getLocation());
        // pot up once per fight
        if (!m.potted) {
            m.potted = true;
            if (this.take(m, "POTION_STRENGTH")) this.drink(bot, PotionEffectType.STRENGTH, 0, 1800);
            if (this.take(m, "POTION_SPEED")) this.drink(bot, PotionEffectType.SPEED, 0, 1800);
        }
        // heal when low: splash healing first, then a gapple
        double max = bot.getAttribute(Attributes.maxHealth()) != null ? bot.getAttribute(Attributes.maxHealth()).getValue() : 20.0;
        if (bot.getHealth() < max * 0.45 && now >= m.nextHeal) {
            m.nextHeal = now + 3500L;
            if (this.take(m, "SPLASH_HEALING")) {
                bot.setHealth(Math.min(max, bot.getHealth() + 8.0));
                bot.getWorld().spawnParticle(Particle.EFFECT, bot.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.0);
                bot.getWorld().playSound(bot.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 1.0f, 1.0f);
            } else if (this.take(m, "ENCHANTED_GOLDEN_APPLE") || this.take(m, "GOLDEN_APPLE")) {
                bot.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 1));
                bot.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                bot.getWorld().playSound(bot.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
            }
        }
        // the memory's gem power
        if (dist <= 14.0 && now >= m.nextGem) {
            m.nextGem = now + 15000L + ThreadLocalRandom.current().nextInt(6000);
            this.useGem(m, bot, prey);
        }
        // strafe and crit-jump in close range instead of walking straight in
        if (dist < 3.5 && bot.isOnGround()) {
            Vector to = prey.getLocation().toVector().subtract(bot.getLocation().toVector()).setY(0);
            if (to.lengthSquared() > 0.01) {
                Vector side = new Vector(-to.getZ(), 0, to.getX()).normalize().multiply(ThreadLocalRandom.current().nextBoolean() ? 0.35 : -0.35);
                if (ThreadLocalRandom.current().nextInt(3) == 0) side.setY(0.42);
                bot.setVelocity(bot.getVelocity().add(side));
            }
        }
    }

    private void drink(Player bot, PotionEffectType type, int amp, int ticks) {
        bot.addPotionEffect(new PotionEffect(type, ticks, amp));
        bot.getWorld().playSound(bot.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
    }

    private void useGem(Mem m, Player bot, Player prey) {
        World w = bot.getWorld();
        switch (m.gem) {
            case "fire" -> {
                Vector dir = prey.getEyeLocation().toVector().subtract(bot.getEyeLocation().toVector()).normalize();
                Fireball ball = w.spawn(bot.getEyeLocation().add(dir.clone().multiply(1.5)), Fireball.class);
                ball.setShooter(bot);
                ball.setDirection(dir);
                ball.setVelocity(dir.multiply(1.6));
                ball.setYield(1.5f);
                w.playSound(bot.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.6f);
            }
            case "puff" -> {
                prey.setVelocity(prey.getVelocity().setY(1.6));
                w.spawnParticle(Particle.CLOUD, prey.getLocation(), 30, 0.5, 0.3, 0.5, 0.1);
                w.playSound(prey.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 0.8f);
            }
            case "speed" -> {
                bot.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 2));
                w.spawnParticle(Particle.ELECTRIC_SPARK, bot.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.1);
                w.playSound(bot.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.6f);
            }
            case "strength" -> {
                bot.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 160, 1));
                w.spawnParticle(Particle.DUST, bot.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.0, new Particle.DustOptions(Color.RED, 1.4f));
                w.playSound(bot.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.8f, 1.2f);
            }
            default -> {
                bot.setHealth(Math.min(bot.getHealth() + 6.0, bot.getAttribute(Attributes.maxHealth()) != null ? bot.getAttribute(Attributes.maxHealth()).getValue() : 20.0));
                w.spawnParticle(Particle.HEART, bot.getLocation().add(0, 2, 0), 6, 0.4, 0.3, 0.4, 0.0);
                w.playSound(bot.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
            }
        }
    }

    /** Removes one supply of that kind from the bag; false when the bot has none left. */
    private boolean take(Mem m, String kind) {
        for (ItemStack item : m.bag) {
            if (!kind.equals(supplyKind(item))) continue;
            item.setAmount(item.getAmount() - 1);
            if (item.getAmount() <= 0) m.bag.remove(item);
            return true;
        }
        return false;
    }

    private static String supplyKind(ItemStack item) {
        if (item.getItemMeta() instanceof PotionMeta pm && pm.getBasePotionType() != null) {
            PotionType t = pm.getBasePotionType();
            if (item.getType() == Material.SPLASH_POTION && (t == PotionType.HEALING || t == PotionType.STRONG_HEALING)) return "SPLASH_HEALING";
            if (t == PotionType.STRENGTH || t == PotionType.LONG_STRENGTH || t == PotionType.STRONG_STRENGTH) return "POTION_STRENGTH";
            if (t == PotionType.SWIFTNESS || t == PotionType.LONG_SWIFTNESS || t == PotionType.STRONG_SWIFTNESS) return "POTION_SPEED";
        }
        return item.getType().name();
    }

    /** A bot with a shield blocks some hits from the front; an axe knocks the shield down for 5s. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBotHit(EntityDamageByEntityEvent event) {
        Mem m = this.byEntity.get(event.getEntity().getUniqueId());
        if (m == null || !(event.getEntity() instanceof Player bot) || bot.getInventory().getItemInOffHand().getType() != Material.SHIELD) return;
        Entity attacker = event.getDamager() instanceof org.bukkit.entity.Projectile pr && pr.getShooter() instanceof Entity sh ? sh : event.getDamager();
        long now = System.currentTimeMillis();
        if (now < m.shieldDownUntil) return;
        if (attacker instanceof Player p && p.getInventory().getItemInMainHand().getType().name().endsWith("_AXE")) {
            m.shieldDownUntil = now + 5000L;
            bot.getWorld().playSound(bot.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 1.0f);
            return;
        }
        Vector facing = bot.getLocation().getDirection().setY(0);
        Vector toAttacker = attacker.getLocation().toVector().subtract(bot.getLocation().toVector()).setY(0);
        if (facing.lengthSquared() < 1.0E-4 || toAttacker.lengthSquared() < 1.0E-4 || facing.normalize().dot(toAttacker.normalize()) < 0.2) return;
        if (ThreadLocalRandom.current().nextDouble() >= this.cfg.getDouble("shield-chance", 0.35)) return;
        event.setCancelled(true);
        bot.getWorld().playSound(bot.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
    }

    /** A totem in the bag saves the bot once, like a player's would. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBotLethal(EntityDamageEvent event) {
        Mem m = this.byEntity.get(event.getEntity().getUniqueId());
        if (m == null || !(event.getEntity() instanceof Player bot) || event.getFinalDamage() < bot.getHealth()) return;
        if (!this.take(m, "TOTEM_OF_UNDYING")) return;
        event.setCancelled(true);
        bot.setHealth(1.0);
        bot.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1));
        bot.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
        bot.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0));
        bot.playEffect(EntityEffect.TOTEM_RESURRECT);
    }

    // ---- the heads goal ----

    private ItemStack head(Mem m, Player bot) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setPlayerProfile(bot.getPlayerProfile());
        meta.setDisplayName(PedestalManager.color("&e" + m.name + "'s Memory"));
        meta.setLore(List.of(PedestalManager.color("&7Lay it down at the memory's spawn.")));
        meta.getPersistentDataContainer().set(this.headKey, PersistentDataType.STRING, m.name);
        head.setItemMeta(meta);
        return head;
    }

    private boolean isHead(ItemStack item) {
        return item != null && item.getType() == Material.PLAYER_HEAD && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer().has(this.headKey, PersistentDataType.STRING);
    }

    private Location pedestal() {
        return this.dream.world() != null ? this.dream.world().memoryArrival() : null;
    }

    /** Heads dropped (Q) at the pedestal are laid to rest; all of them brings the end of the dream. */
    @EventHandler(ignoreCancelled = true)
    public void onHeadDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (!this.isHead(item)) return;
        Location at = this.pedestal();
        Player p = event.getPlayer();
        if (at == null || p.getWorld() != at.getWorld() || p.getLocation().distanceSquared(at) > 36.0) return;
        event.getItemDrop().remove();
        int delivered = this.cfg.getInt("goal.delivered") + item.getAmount();
        int total = this.headsNeeded();
        this.cfg.set("goal.delivered", delivered);
        this.save();
        at.getWorld().playSound(at, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.5f, 0.8f);
        at.getWorld().spawnParticle(Particle.END_ROD, at.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.05);
        String msg = PedestalManager.color("&e" + p.getName() + " &7laid a memory to rest &8(&6" + Math.min(delivered, total) + "&8/&6" + total + "&8)");
        for (Player o : at.getWorld().getPlayers()) o.sendMessage(msg);
        if (total > 0 && delivered >= total) this.complete(at);
    }

    /** Heads to lay down: memory_npcs.yml heads-needed (15), or every memory spawned when that is 0. */
    private int headsNeeded() {
        int needed = this.cfg.getInt("heads-needed", 15);
        return needed > 0 ? needed : this.cfg.getInt("goal.total");
    }

    private void complete(Location at) {
        this.cfg.set("goal", null);
        this.save();
        String msg = PedestalManager.color("&6&lEvery memory is laid to rest. &eThe dream is ending...");
        for (Player o : Bukkit.getOnlinePlayers()) if (GoldenDreamWorld.isDreamWorld(o.getWorld())) o.sendMessage(msg);
        PurgeThunder.strike(this.plugin, this.dream, at);
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> WakeFloat.start(this.plugin, this.dream), 440L);
    }

    /** A ring of gold over the pedestal while heads are still owed. */
    private void tickPedestal() {
        int total = this.cfg.getInt("goal.total") > 0 ? this.headsNeeded() : 0;
        if (total <= 0 || this.cfg.getInt("goal.delivered") >= total) return;
        Location at = this.pedestal();
        if (at == null || at.getWorld().getPlayers().isEmpty()) return;
        Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(255, 200, 40), 1.3f);
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * i / 24.0;
            at.getWorld().spawnParticle(Particle.DUST, at.clone().add(Math.cos(a) * 1.5, 0.2, Math.sin(a) * 1.5), 1, 0, 0, 0, 0, gold, true);
        }
    }

    /** The closest real player in survival/adventure within aggro-range of the bot, or null. */
    private Player nearestPrey(Entity bot) {
        double range = this.cfg.getDouble("aggro-range", 16.0);
        if (range <= 0) return null;
        Player best = null;
        double bestDist = range * range;
        for (Player p : bot.getWorld().getPlayers()) {
            if (this.byEntity.containsKey(p.getUniqueId()) || p.hasMetadata("NPC") || p.isDead()) continue;
            if (p.getGameMode() == org.bukkit.GameMode.CREATIVE || p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            double d = p.getLocation().distanceSquared(bot.getLocation());
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBotDeath(PlayerDeathEvent event) {
        Mem m = this.byEntity.remove(event.getEntity().getUniqueId());
        if (m == null || m.destroyedByUs) return;
        event.deathMessage(null);
        event.getDrops().clear();
        Location at = event.getEntity().getLocation();
        if (at.getY() < at.getWorld().getMinHeight()) at = m.home;
        String msg = PedestalManager.color("&e" + m.name + " has been purged.");
        for (Player p : Bukkit.getOnlinePlayers()) if (GoldenDreamWorld.isDreamWorld(p.getWorld()) == GoldenDreamWorld.isDreamWorld(at.getWorld())) p.sendMessage(msg);
        at.getWorld().strikeLightningEffect(at);
        at.getWorld().playSound(at, Sound.ENTITY_WITHER_DEATH, SoundCategory.MASTER, 3.0f, 0.5f);
        // what the memory carried is the loot (memory_npcs.yml loot-mode: list = the old fixed loot)
        if ("list".equalsIgnoreCase(this.cfg.getString("loot-mode", "inventory"))) {
            for (ItemStack item : this.loot) at.getWorld().dropItemNaturally(at, item.clone());
        } else {
            for (ItemStack item : m.worn) at.getWorld().dropItemNaturally(at, item.clone());
            for (ItemStack item : m.bag) at.getWorld().dropItemNaturally(at, item.clone());
        }
        at.getWorld().dropItemNaturally(at, this.head(m, event.getEntity()));
        m.destroyedByUs = true;
        this.live.remove(m.id);
        this.roster.releaseName(m.name);
        this.cfg.set("spawned." + m.id, null);
        this.save();
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            try {
                call(m.npc, "destroy");
            } catch (Throwable ignored) {
            }
        }, 1L);
    }

    public void shutdown() {
        for (Mem m : new ArrayList<>(this.live.values())) {
            m.destroyedByUs = true;
            try {
                if (m.npc != null) call(m.npc, "destroy");
            } catch (Throwable ignored) {
            }
        }
        this.live.clear();
        this.byEntity.clear();
        if (this.task != null) this.task.cancel();
        this.task = null;
    }

    private void persist(Mem m) {
        String p = "spawned." + m.id + ".";
        this.cfg.set(p + "name", m.name);
        this.cfg.set(p + "skin", m.skin);
        this.cfg.set(p + "world", m.home.getWorld().getName());
        this.cfg.set(p + "x", m.home.getX());
        this.cfg.set(p + "y", m.home.getY());
        this.cfg.set(p + "z", m.home.getZ());
        this.cfg.set(p + "yaw", m.home.getYaw());
        this.cfg.set(p + "pitch", m.home.getPitch());
        this.save();
    }

    private void loadLoot() {
        if (!this.cfg.contains("loot")) {
            this.cfg.set("loot", List.of("DIAMOND:1", "EMERALD:2", "GOLDEN_APPLE:1", "IRON_INGOT:3"));
            this.save();
        }
        for (String s : this.cfg.getStringList("loot")) {
            String[] parts = s.split(":");
            Material mat = Material.matchMaterial(parts[0].trim());
            if (mat == null) {
                this.plugin.getLogger().warning("memory_npcs.yml loot: unknown material '" + parts[0] + "'");
                continue;
            }
            int amount = 1;
            try {
                if (parts.length > 1) amount = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
            }
            this.loot.add(new ItemStack(mat, Math.max(1, Math.min(mat.getMaxStackSize(), amount))));
        }
    }

    /** Supplies every bot carries: MATERIAL:amount, or SPLASH_HEALING / POTION_STRENGTH / POTION_SPEED:amount. */
    private void loadSupplies() {
        if (!this.cfg.contains("supplies")) {
            this.cfg.set("shield-chance", 0.35);
            this.cfg.set("loot-mode", "inventory");
            this.cfg.set("supplies", List.of("GOLDEN_APPLE:8", "ENCHANTED_GOLDEN_APPLE:1", "SPLASH_HEALING:3", "POTION_STRENGTH:1", "POTION_SPEED:1", "TOTEM_OF_UNDYING:1"));
            this.save();
        }
        for (String s : this.cfg.getStringList("supplies")) {
            String[] parts = s.split(":");
            String kind = parts[0].trim().toUpperCase();
            int amount = 1;
            try {
                if (parts.length > 1) amount = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
            }
            ItemStack item = switch (kind) {
                case "SPLASH_HEALING" -> potion(Material.SPLASH_POTION, PotionType.STRONG_HEALING);
                case "POTION_STRENGTH" -> potion(Material.POTION, PotionType.STRENGTH);
                case "POTION_SPEED" -> potion(Material.POTION, PotionType.SWIFTNESS);
                default -> {
                    Material mat = Material.matchMaterial(kind);
                    yield mat == null ? null : new ItemStack(mat);
                }
            };
            if (item == null) {
                this.plugin.getLogger().warning("memory_npcs.yml supplies: unknown '" + parts[0] + "'");
                continue;
            }
            item.setAmount(Math.max(1, Math.min(item.getMaxStackSize(), amount)));
            this.supplies.add(item);
        }
    }

    private static ItemStack potion(Material type, PotionType potion) {
        ItemStack item = new ItemStack(type);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionType(potion);
        item.setItemMeta(meta);
        return item;
    }

    /** Kits are lists of SLOT:MATERIAL (HELMET, CHESTPLATE, LEGGINGS, BOOTS, HAND, OFF_HAND); each bot gets one at random. */
    private void loadKits() {
        if (!this.cfg.contains("kits") || OLD_KITS.equals(this.cfg.getList("kits"))) {
            // Bliss: memories wear maxed-out diamond, never gold, iron or netherite
            this.cfg.set("aggro-range", this.cfg.getDouble("aggro-range", 16.0));
            this.cfg.set("kits", List.of(
                List.of("HELMET:DIAMOND_HELMET", "CHESTPLATE:DIAMOND_CHESTPLATE", "LEGGINGS:DIAMOND_LEGGINGS", "BOOTS:DIAMOND_BOOTS", "HAND:DIAMOND_SWORD", "OFF_HAND:SHIELD"),
                List.of("HELMET:DIAMOND_HELMET", "CHESTPLATE:DIAMOND_CHESTPLATE", "LEGGINGS:DIAMOND_LEGGINGS", "BOOTS:DIAMOND_BOOTS", "HAND:DIAMOND_AXE"),
                List.of("HELMET:DIAMOND_HELMET", "CHESTPLATE:DIAMOND_CHESTPLATE", "LEGGINGS:DIAMOND_LEGGINGS", "BOOTS:DIAMOND_BOOTS", "HAND:MACE"),
                List.of("HELMET:DIAMOND_HELMET", "CHESTPLATE:DIAMOND_CHESTPLATE", "LEGGINGS:DIAMOND_LEGGINGS", "BOOTS:DIAMOND_BOOTS", "HAND:DIAMOND_SWORD", "OFF_HAND:TOTEM_OF_UNDYING")));
            this.save();
        }
        if (!this.cfg.contains("heads-needed")) {
            this.cfg.set("heads-needed", 15);
            this.save();
        }
        for (Object raw : this.cfg.getList("kits", List.of())) {
            if (!(raw instanceof List<?> pieces)) continue;
            Map<String, Material> kit = new HashMap<>();
            for (Object piece : pieces) {
                String[] parts = String.valueOf(piece).split(":");
                Material mat = parts.length == 2 ? Material.matchMaterial(parts[1].trim()) : null;
                if (mat == null || !KIT_SLOTS.contains(parts[0].trim().toUpperCase())) {
                    this.plugin.getLogger().warning("memory_npcs.yml kits: bad entry '" + piece + "'");
                    continue;
                }
                kit.put(parts[0].trim().toUpperCase(), mat);
            }
            if (!kit.isEmpty()) this.kits.add(kit);
        }
        if (this.kits.isEmpty()) {
            this.kits.add(Map.of("HELMET", Material.DIAMOND_HELMET, "CHESTPLATE", Material.DIAMOND_CHESTPLATE, "LEGGINGS", Material.DIAMOND_LEGGINGS,
                "BOOTS", Material.DIAMOND_BOOTS, "HAND", Material.DIAMOND_SWORD));
        }
    }

    private void save() {
        try {
            this.cfg.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("memory_npcs.yml: " + e.getMessage());
        }
    }

    // ---- reflection helpers ----

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumConst(Class<?> type, String name) {
        return Enum.valueOf((Class) type, name);
    }

    /** Calls the public method with that name whose parameters accept the given arguments. */
    static Object call(Object target, String name, Object... args) throws Exception {
        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
            Class<?>[] types = m.getParameterTypes();
            boolean ok = true;
            for (int i = 0; i < types.length && ok; i++) ok = args[i] == null || box(types[i]).isInstance(args[i]);
            if (!ok) continue;
            m.setAccessible(true);
            return m.invoke(target, args);
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + name + "/" + args.length);
    }

    private static Class<?> box(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == boolean.class) return Boolean.class;
        if (c == float.class) return Float.class;
        if (c == double.class) return Double.class;
        if (c == long.class) return Long.class;
        return c;
    }

    private static final class Mem {
        final String id;
        String name;
        String skin;
        final Location home;
        Object npc;
        boolean geared;
        boolean warned;
        boolean destroyedByUs;
        long nextWander;
        int kit = -1;
        UUID prey;
        final List<ItemStack> worn = new ArrayList<>();
        final List<ItemStack> bag = new ArrayList<>();
        String gem;
        boolean potted;
        long nextHeal;
        long nextGem;
        long shieldDownUntil;

        Mem(String id, String name, String skin, Location home) {
            this.id = id;
            this.name = name;
            this.skin = skin;
            this.home = home;
        }
    }
}
