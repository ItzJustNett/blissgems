package dev.xoperr.blissgems.lore;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemDefinition;
import dev.xoperr.blissgems.managers.GemRegistryImpl;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SizedFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Quakitus's Binding Vow: his Fire Gem keeps only Fire Resistance and a Charged Fireball that burns
 * for 5-6 hearts, blows up bigger in purple and black, and marks whoever it hits. The gem can't be
 * traded and survives death.
 * The EMP is a Binding Vow of its own: a 10s sphere, every mythic disabled for 30s, then 5 hearts
 * gone for 30s and the holder's gem off for 60s.
 */
public final class FireVow implements Listener {
    private static final String EMP_KEY = "fire-vow-emp";

    private final BlissGems plugin;
    private final NamespacedKey fireballKey;
    private final NamespacedKey heartCapKey;
    private final Map<UUID, Long> burned = new HashMap<>();
    private final Map<UUID, Set<UUID>> fireballHits = new HashMap<>();
    private final Set<UUID> recharging = new HashSet<>();
    private final Map<UUID, Long> gemDisabledUntil = new HashMap<>();
    private long empUntil;

    public FireVow(BlissGems plugin) {
        this.plugin = plugin;
        this.fireballKey = new NamespacedKey(plugin, "fire_vow_fireball");
        this.heartCapKey = new NamespacedKey(plugin, "fire_vow_emp_hearts");
    }

    /** The player's gem is Quakitus's Binding Vow Fire Gem. */
    public boolean hasVowGem(Player player) {
        return this.plugin.getGemManager().getGemType(player) == GemType.FIRE
                && CustomItemManager.isBindingVow(this.plugin.getGemManager().findGemInInventory(player));
    }

    public boolean isEmpActive() {
        return System.currentTimeMillis() < this.empUntil;
    }

    /** True (and tells the player) when an EMP is down and this gem is a mythic. */
    public boolean jamsGem(Player player, String gemId) {
        if (!this.isEmpActive() || gemId == null) return false;
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        GemDefinition def = registry != null ? registry.getGem(gemId) : null;
        if (def == null || def.getMinTier() < 2) return false;
        this.jammedMessage(player);
        return true;
    }

    /** True (and tells the player) when an EMP is down: for mythic items. */
    public boolean jamsMythicItem(Player player) {
        if (!this.isEmpActive()) return false;
        this.jammedMessage(player);
        return true;
    }

    private void jammedMessage(Player player) {
        int left = (int)Math.ceil((this.empUntil - System.currentTimeMillis()) / 1000.0);
        player.sendMessage("§5§oYour mythic is disabled by an EMP! §d(" + left + "s)");
    }

    public void markFireball(Fireball fireball) {
        fireball.getPersistentDataContainer().set(this.fireballKey, PersistentDataType.BYTE, (byte)1);
        fireball.setYield(fireball.getYield() * (float)this.plugin.getConfig().getDouble("abilities.fire-vow.explosion-multiplier", 1.6));
        if (fireball instanceof SizedFireball sized) {
            sized.setDisplayItem(FireVow.purpleCharge());
        }
    }

    /** The purple fire charge model (assets/minecraft/items/fire_charge.json). */
    public static ItemStack purpleCharge() {
        ItemStack look = new ItemStack(Material.FIRE_CHARGE);
        ItemMeta meta = look.getItemMeta();
        meta.setCustomModelData(100);
        look.setItemMeta(meta);
        return look;
    }

    /** Purple and black smoke where the vow fireball goes off. */
    @EventHandler
    public void onFireballExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Fireball fireball) || !fireball.getPersistentDataContainer().has(this.fireballKey, PersistentDataType.BYTE)) {
            return;
        }
        Location at = event.getLocation();
        Particle.DustOptions pink = new Particle.DustOptions(Color.fromRGB(230, 60, 170), 3.0f);
        Particle.DustOptions black = new Particle.DustOptions(Color.fromRGB(10, 5, 15), 3.0f);
        new BukkitRunnable() {
            int ticks = 0;

            public void run() {
                if ((this.ticks += 2) > 30) {
                    this.cancel();
                    return;
                }
                double spread = 1.0 + this.ticks * 0.12;
                at.getWorld().spawnParticle(Particle.DUST, at, 60, spread, spread, spread, 0.0, pink, true);
                at.getWorld().spawnParticle(Particle.DUST, at, 60, spread, spread, spread, 0.0, black, true);
            }
        }.runTaskTimer(this.plugin, 0L, 2L);
        at.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, at, 2, 0.5, 0.5, 0.5, 0.0, null, true);
    }

    /** The vow gem is switched off for a minute after each EMP. */
    public boolean isGemDisabled(Player player) {
        Long until = this.gemDisabledUntil.get(player.getUniqueId());
        return until != null && System.currentTimeMillis() < until;
    }

    /** Tells the player their vow gem is still switched off; true if it is. */
    public boolean blockedByDisabledGem(Player player) {
        if (!this.isGemDisabled(player)) return false;
        int left = (int)Math.ceil((this.gemDisabledUntil.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000.0);
        player.sendMessage("\u00a75\u00a7oYour gem is disabled after the EMP. \u00a7d(" + left + "s)");
        return true;
    }

    /** Pink sparks above the head while the (purple) fireball charges. */
    public void chargeTick(Player player) {
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 2.3, 0.0), 4, 0.2, 0.15, 0.2, 0.0, new Particle.DustOptions(Color.fromRGB(255, 95, 150), 1.1f));
    }

    /** Sneak + right-click with an empty hand sets off the EMP vow. */
    @EventHandler(priority = EventPriority.LOW)
    public void onEmpClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND || !player.isSneaking() || !event.getAction().isRightClick()
                || !player.getInventory().getItemInMainHand().getType().isAir() || !this.plugin.getBindingVows().hasEmpVow(player)) {
            return;
        }
        event.setCancelled(true);
        this.emp(player);
    }

    /** Sneak + F with both hands empty: the client never reports an empty-hand right click on air. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEmpSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack main = event.getMainHandItem();
        ItemStack off = event.getOffHandItem();
        if (!player.isSneaking() || (main != null && !main.getType().isAir()) || (off != null && !off.getType().isAir())
                || !this.plugin.getBindingVows().hasEmpVow(player)) {
            return;
        }
        event.setCancelled(true);
        this.emp(player);
    }

    public void emp(Player player) {
        if (!this.plugin.getBindingVows().hasEmpVow(player)) {
            player.sendMessage("\u00a7cOnly the EMP Binding Vow holds an EMP.");
            return;
        }
        if (!this.plugin.getAbilityManager().canUseAbility(player, EMP_KEY)) {
            return;
        }
        int effect = this.plugin.getConfig().getInt("abilities.fire-vow.emp-effect-seconds", 30);
        int heartCost = this.plugin.getConfig().getInt("abilities.fire-vow.heart-cost-seconds", 30);
        int gemOff = this.plugin.getConfig().getInt("abilities.fire-vow.gem-disabled-seconds", 60);
        this.plugin.getAbilityManager().setCooldown(player, EMP_KEY, effect + gemOff);
        this.empUntil = Math.max(this.empUntil, System.currentTimeMillis() + effect * 1000L);
        player.sendMessage("\u00a7dDeploying EMP!");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.6f);
        this.sphere(player.getLocation().add(0.0, 1.0, 0.0));
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != player) {
                online.sendMessage("\u00a75\u00a7l\u26a1 \u00a7dAn EMP went off. Every mythic is disabled for " + effect + "s.");
            }
        }
        final UUID uuid = player.getUniqueId();
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            this.recharging.add(uuid);
            this.gemDisabledUntil.put(uuid, System.currentTimeMillis() + gemOff * 1000L);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                this.capHearts(p);
                p.sendMessage("\u00a7fThe effects of your EMP have ended.");
                p.sendMessage("\u00a7eNow recharging at the cost of \u00a7c5 Hearts");
            }
        }, effect * 20L);
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            this.recharging.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) this.uncapHearts(p);
        }, (effect + heartCost) * 20L);
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage("\u00a7dYour gem is back and your EMP has recharged.");
                p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.4f);
            }
        }, (effect + gemOff) * 20L);
    }

    /** A pink shell of light around the EMP for its first seconds. */
    private void sphere(Location center) {
        int seconds = this.plugin.getConfig().getInt("abilities.fire-vow.sphere-seconds", 10);
        double radius = this.plugin.getConfig().getDouble("abilities.fire-vow.sphere-radius", 8.0);
        Particle.DustOptions light = new Particle.DustOptions(Color.fromRGB(255, 120, 200), 1.6f);
        Particle.DustOptions dark = new Particle.DustOptions(Color.fromRGB(150, 40, 130), 1.6f);
        new BukkitRunnable() {
            int ticks = 0;

            public void run() {
                if ((this.ticks += 5) > seconds * 20) {
                    this.cancel();
                    return;
                }
                int points = 220;
                double golden = Math.PI * (3.0 - Math.sqrt(5.0));
                for (int i = 0; i < points; ++i) {
                    double y = 1.0 - 2.0 * (i + 0.5) / points;
                    double r = Math.sqrt(1.0 - y * y);
                    double a = golden * i + this.ticks * 0.02;
                    Location at = center.clone().add(Math.cos(a) * r * radius, y * radius, Math.sin(a) * r * radius);
                    center.getWorld().spawnParticle(Particle.DUST, at, 1, 0.0, 0.0, 0.0, 0.0, i % 3 == 0 ? dark : light);
                }
            }
        }.runTaskTimer(this.plugin, 0L, 5L);
    }

    @SuppressWarnings("deprecation")
    private void capHearts(Player player) {
        AttributeInstance hp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (hp == null) return;
        this.uncapHearts(player);
        // Takes 5 hearts, never below one.
        double cut = Math.min(10.0, hp.getValue() - 2.0);
        if (cut > 0.0) {
            hp.addModifier(new AttributeModifier(this.heartCapKey, -cut, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        }
    }

    @SuppressWarnings("deprecation")
    private void uncapHearts(Player player) {
        AttributeInstance hp = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (hp != null) hp.removeModifier(this.heartCapKey);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // A restart mid-recharge must not leave the 5-heart cap on for good.
        if (!this.recharging.contains(event.getPlayer().getUniqueId())) {
            this.uncapHearts(event.getPlayer());
        } else {
            this.capHearts(event.getPlayer());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (this.recharging.contains(player.getUniqueId())) {
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.capHearts(player));
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFireballHit(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof Fireball fireball) || !fireball.getPersistentDataContainer().has(this.fireballKey, PersistentDataType.BYTE)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim) || victim.equals(fireball.getShooter())) {
            return;
        }
        // The impact and the blast both hurt: only the first one counts.
        Set<UUID> hit = this.fireballHits.computeIfAbsent(fireball.getUniqueId(), id -> {
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> this.fireballHits.remove(id), 100L);
            return new HashSet<>();
        });
        if (!hit.add(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        double min = this.plugin.getConfig().getDouble("abilities.fire-vow.fireball-min-damage", 10.0);
        double max = this.plugin.getConfig().getDouble("abilities.fire-vow.fireball-max-damage", 12.0);
        event.setDamage(min + ThreadLocalRandom.current().nextDouble() * Math.max(0.0, max - min));
        for (EntityDamageEvent.DamageModifier mod : new EntityDamageEvent.DamageModifier[]{EntityDamageEvent.DamageModifier.ARMOR, EntityDamageEvent.DamageModifier.MAGIC}) {
            if (event.isApplicable(mod)) event.setDamage(mod, 0.0);
        }
        this.mark(victim);
    }

    /** The hit glows for as long as the sun's particles cling to it. */
    private void mark(LivingEntity victim) {
        int seconds = this.plugin.getConfig().getInt("abilities.fire-vow.mark-seconds", 6);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, seconds * 20, 0, false, false));
        this.burned.put(victim.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
        new BukkitRunnable() {
            int ticks = 0;

            public void run() {
                if (!victim.isValid() || (this.ticks += 4) > seconds * 20) {
                    this.cancel();
                    return;
                }
                victim.getWorld().spawnParticle(Particle.FLAME, victim.getLocation().add(0.0, 1.0, 0.0), 6, 0.3, 0.5, 0.3, 0.02);
                victim.getWorld().spawnParticle(Particle.DRAGON_BREATH, victim.getLocation().add(0.0, 1.0, 0.0), 3, 0.3, 0.5, 0.3, 0.01, Particle.DRAGON_BREATH.getDataType() == Float.class ? (Object) 1.0f : null);
            }
        }.runTaskTimer(this.plugin, 0L, 4L);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Long until = this.burned.remove(victim.getUniqueId());
        if (until != null && System.currentTimeMillis() < until) {
            event.setDeathMessage(victim.getName() + " flew too close to the sun");
        }
    }
}
