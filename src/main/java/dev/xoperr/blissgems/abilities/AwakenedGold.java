package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Attributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.AbstractWindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * The awakened Gold Gem (all eight souls harvested, then woken by the Golden Dream). Its passives -
 * Ichor, Midas Touch, Prophecy - always work; its two movesets sit in /bliss goldcycle after the souls:
 * <ul>
 *   <li>Moveset I: primary - Snap (gem in main hand) / Golden Drill (off hand), shared cooldown;
 *       secondary - Rayler (main hand) / Gilded Viola, the triple beam (off hand), on Rayler's 3 charges</li>
 *   <li>Moveset II: primary - Prophet's Ladder (main hand) / Gilded Blitz (off hand), shared cooldown;
 *       secondary - Mark of the Beast (2 charges)</li>
 *   <li>both: tertiary - Gilded Wrath; senary - the Sundering Beam</li>
 * </ul>
 * Numbers live under gold.awakened in config.yml.
 */
public class AwakenedGold implements Listener {
    public static final String SNAP_KEY = "gold-snap";
    public static final String WRATH_KEY = "gold-wrath";
    public static final String LADDER_KEY = "gold-ladder";
    private static final String TRIPLE_KEY = "gold-rayler-triple";
    private static final Color GOLD = Color.fromRGB(255, 200, 40);
    private static final Color PALE_GOLD = Color.fromRGB(255, 235, 130);
    private static final Set<Material> ICHOR_ITEMS = Set.of(Material.COBWEB, Material.EXPERIENCE_BOTTLE, Material.ENDER_PEARL,
        Material.WIND_CHARGE, Material.GOLDEN_APPLE, Material.TOTEM_OF_UNDYING, Material.FIREWORK_ROCKET, Material.WATER_BUCKET);

    private final BlissGems plugin;
    private final NamespacedKey prophecyKey;
    private final NamespacedKey heartLockKey;
    /** Rayler charges in use: when each spent charge comes back (epoch ms). */
    private final Map<UUID, List<Long>> raylerRefills = new HashMap<>();
    /** Mark of the Beast charges in use: when each spent charge comes back (epoch ms). */
    private final Map<UUID, List<Long>> markRefills = new HashMap<>();
    private final Map<UUID, Long> lastMark = new HashMap<>();
    private final Set<UUID> drilling = new HashSet<>();
    private int passiveRuns;

    public AwakenedGold(BlissGems plugin) {
        this.plugin = plugin;
        this.prophecyKey = new NamespacedKey(plugin, "gold-prophecy");
        this.heartLockKey = new NamespacedKey(plugin, "heart-lock");
        Bukkit.getScheduler().runTaskTimer(plugin, this::passiveTick, 20L, 2L);
    }

    public boolean isEnabled() {
        return this.plugin.getConfig().getBoolean("gold.awakened.enabled", true);
    }

    /** True when the player holds a fully awakened Gold Gem (its passives work). */
    public boolean isAwakened(Player player) {
        // the soul count is a map lookup; only then scan the inventory for the gem
        return this.isEnabled() && this.plugin.getGoldGemManager() != null
            && this.plugin.getGoldGemManager().cachedSoulCount(player.getUniqueId()) >= 8
            && this.plugin.getGoldGemManager().isFullyAwakened(player.getUniqueId())
            && this.plugin.getGoldGemManager().holdsGoldGem(player);
    }

    /** The awakened moveset in use (1 or 2), or 0 when a soul is channelled instead. */
    public int moveset(Player player) {
        return this.isAwakened(player) ? this.plugin.getGoldGemManager().getMoveset(player.getUniqueId()) : 0;
    }

    // ---- inputs ----

    public void primary(Player player) {
        boolean main = this.plugin.getGoldGemManager().isGoldGemInMainHand(player);
        if (this.moveset(player) == 2) {
            if (main) this.prophetsLadder(player);
            else this.gildedBlitz(player);
        } else if (main) {
            this.snap(player);
        } else {
            this.goldenDrill(player);
        }
    }

    public void secondary(Player player) {
        if (this.moveset(player) == 2) this.markOfTheBeast(player);
        else if (this.plugin.getGoldGemManager().isGoldGemInMainHand(player)) this.rayler(player);
        else this.gildedViola(player);
    }

    public void tertiary(Player player) {
        this.gildedWrath(player);
    }

    public void quinary(Player player) {
        if (this.moveset(player) == 2) this.markOfTheBeast(player);
        else this.gildedViola(player);
    }

    // ---- Snap / Golden Drill ----

    private boolean snapReady(Player player) {
        int left = this.plugin.getAbilityManager().getRemainingCooldown(player, SNAP_KEY);
        if (left > 0) {
            player.sendMessage("§6Snap §7and §6Golden Drill §7recharge in §c" + left + "s");
            return false;
        }
        return true;
    }

    /** Disables three random gems and hurts everyone on the server (double for broken gems). */
    private void snap(Player player) {
        if (!this.snapReady(player)) return;
        this.plugin.getAbilityManager().setCooldown(player, SNAP_KEY, this.plugin.getConfig().getInt("gold.awakened.snap.cooldown", 120));
        double damage = this.plugin.getConfig().getDouble("gold.awakened.snap.damage", 7.0);
        int lockSeconds = this.plugin.getConfig().getInt("gold.awakened.snap.lock-seconds", 60);
        List<Player> victims = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(player) || this.isTrusted(player, p) || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;
            victims.add(p);
            boolean broken = this.plugin.getEnergyManager().getEnergy(p) <= 0;
            p.damage(broken ? damage * 2.0 : damage, player);
            p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 0.5f);
        }
        Collections.shuffle(victims);
        List<String> locked = new ArrayList<>();
        for (Player p : victims) {
            if (locked.size() >= 3) break;
            if (!this.plugin.getGemManager().hasActiveGem(p) || this.plugin.getGemLockManager() == null || this.snapProof(p)) continue;
            this.plugin.getGemLockManager().lock(p, lockSeconds);
            locked.add(p.getName());
            p.sendMessage("§6§lSNAP! §7Your gem has been disabled for " + lockSeconds + "s.");
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 1.6f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1.5, 0), 80, 1.2, 1.2, 1.2, 0.0, new Particle.DustOptions(GOLD, 2.0f));
        Bukkit.broadcastMessage("§6§l" + player.getName() + " snapped." + (locked.isEmpty() ? "" : " §7Disabled: §e" + String.join(", ", locked)));
    }

    /** Mythic gems and Binding Vow gems can't be snapped off. */
    private boolean snapProof(Player p) {
        if (this.plugin.getBindingVows() != null && this.plugin.getBindingVows().vowItemId(p) != null) return true;
        String gemId = this.plugin.getGemManager().getGemId(p);
        dev.xoperr.blissgems.api.GemDefinition def = gemId != null && this.plugin.getGemRegistry() != null ? this.plugin.getGemRegistry().getGem(gemId) : null;
        return def != null && def.getMinTier() >= 2;
    }

    /** A controllable charge forward: drags enemies along, grinds them and bores through blocks. */
    private void goldenDrill(Player player) {
        if (!this.snapReady(player)) return;
        this.plugin.getAbilityManager().setCooldown(player, SNAP_KEY, this.plugin.getConfig().getInt("gold.awakened.snap.cooldown", 120));
        int ticks = this.plugin.getConfig().getInt("gold.awakened.drill.ticks", 160);
        double speed = this.plugin.getConfig().getDouble("gold.awakened.drill.speed", 1.1);
        double damage = this.plugin.getConfig().getDouble("gold.awakened.drill.damage", 1.5);
        boolean breakBlocks = this.plugin.getConfig().getBoolean("gold.awakened.drill.break-blocks", true);
        UUID id = player.getUniqueId();
        this.drilling.add(id);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.4f, 0.7f);
        new BukkitRunnable() {
            int t;

            public void run() {
                if (!player.isOnline() || player.isDead() || this.t++ >= ticks) {
                    Bukkit.getScheduler().runTaskLater(AwakenedGold.this.plugin, () -> AwakenedGold.this.drilling.remove(id), 60L);
                    this.cancel();
                    return;
                }
                Vector dir = player.getEyeLocation().getDirection().normalize();
                player.setVelocity(dir.clone().multiply(speed));
                player.setFallDistance(0.0f);
                Location at = player.getLocation().add(0, 1, 0);
                player.getWorld().spawnParticle(Particle.DUST, at, 12, 0.6, 0.6, 0.6, 0.0, new Particle.DustOptions(GOLD, 1.6f));
                if (this.t % 4 == 0) player.getWorld().playSound(at, Sound.BLOCK_GRINDSTONE_USE, 0.8f, 0.6f);
                if (breakBlocks) AwakenedGold.this.breakAround(player, at.clone().add(dir.clone().multiply(1.2)), 1.5);
                for (LivingEntity e : at.getNearbyLivingEntities(5.0)) {
                    if (!AwakenedGold.this.isEnemy(player, e)) continue;
                    e.setVelocity(at.toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.45).add(dir.clone().multiply(speed * 0.8)));
                    // every 10 ticks: hits land past the victim's damage immunity
                    if (this.t % 10 == 0 && e.getLocation().distanceSquared(at) <= 6.25) e.damage(damage, player);
                }
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
    }

    // ---- Rayler / Gilded Viola ----

    private int raylerCharges(Player player) {
        int max = 3;
        List<Long> refills = this.raylerRefills.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        long now = System.currentTimeMillis();
        refills.removeIf(at -> at <= now);
        return Math.max(0, max - refills.size());
    }

    private boolean spendRaylerCharge(Player player) {
        if (this.raylerCharges(player) <= 0) {
            long next = Collections.min(this.raylerRefills.get(player.getUniqueId()));
            player.sendMessage("§6Rayler §7has no charge. Next in §c" + (int) Math.ceil((next - System.currentTimeMillis()) / 1000.0) + "s");
            return false;
        }
        long refill = this.plugin.getConfig().getInt("gold.awakened.rayler.charge-seconds", 95) * 1000L;
        this.raylerRefills.get(player.getUniqueId()).add(System.currentTimeMillis() + refill);
        return true;
    }

    /** For the action bar: charges ready right now. */
    public int getRaylerCharges(Player player) {
        return this.raylerCharges(player);
    }

    /** The smaller beam: blinks behind the enemy in sight, then fires through them. */
    private void rayler(Player player) {
        if (!this.spendRaylerCharge(player)) return;
        double damage = this.plugin.getConfig().getDouble("gold.awakened.rayler.damage", 5.0);
        int pierce = this.plugin.getConfig().getInt("gold.awakened.rayler.pierce-blocks", 6);
        double reach = this.plugin.getConfig().getDouble("gold.awakened.rayler.blink-range", 30.0);
        RayTraceResult trace = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), reach, 0.8,
            e -> e instanceof LivingEntity && this.isEnemy(player, (LivingEntity) e));
        if (trace != null && trace.getHitEntity() instanceof LivingEntity target) {
            Vector away = target.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0);
            if (away.lengthSquared() < 1.0E-4) away = player.getLocation().getDirection().setY(0);
            Location behind = target.getLocation().add(away.normalize().multiply(2.5));
            Block feet = behind.getBlock();
            if (feet.isPassable() && feet.getRelative(0, 1, 0).isPassable()) {
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.0, new Particle.DustOptions(GOLD, 1.6f));
                behind.setDirection(target.getEyeLocation().toVector().subtract(behind.clone().add(0, player.getEyeHeight(), 0).toVector()));
                player.teleport(behind);
                player.setFallDistance(0.0f);
            }
        }
        this.fireRay(player, player.getEyeLocation(), damage, pierce, 1.0f);
        player.getWorld().playSound(player.getEyeLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.4f, 1.5f);
        player.sendMessage("§6Rayler §7- §e" + this.raylerCharges(player) + "§7/3 charges left");
    }

    /** Gilded Viola: all three Rayler charges in three parallel beams. Needs a full Rayler and has its own long cooldown. */
    private void gildedViola(Player player) {
        int left = this.plugin.getAbilityManager().getRemainingCooldown(player, TRIPLE_KEY);
        if (left > 0) {
            player.sendMessage("§6Gilded Viola §7recharges in §c" + this.formatLong(left));
            return;
        }
        if (this.raylerCharges(player) < 3) {
            player.sendMessage("§6Gilded Viola §7needs all 3 Rayler charges.");
            return;
        }
        for (int i = 0; i < 3; i++) this.spendRaylerCharge(player);
        this.plugin.getAbilityManager().setCooldown(player, TRIPLE_KEY, this.plugin.getConfig().getInt("gold.awakened.rayler.triple-cooldown", 10800));
        double damage = this.plugin.getConfig().getDouble("gold.awakened.rayler.triple-damage", 22.0);
        int pierce = this.plugin.getConfig().getInt("gold.awakened.rayler.triple-pierce-blocks", 12);
        Location eye = player.getEyeLocation();
        Vector side = eye.getDirection().crossProduct(new Vector(0, 1, 0));
        if (side.lengthSquared() < 1.0E-4) side = new Vector(1, 0, 0);
        side.normalize();
        for (int i = -1; i <= 1; i++) this.fireRay(player, eye.clone().add(side.clone().multiply(i * 1.2)), damage, pierce, 2.0f);
        player.getWorld().playSound(eye, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.7f);
        player.getWorld().playSound(eye, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);
    }

    private void fireRay(Player player, Location from, double damage, int pierceBlocks, float width) {
        double range = this.plugin.getConfig().getDouble("gold.awakened.rayler.range", 60.0);
        Vector dir = player.getEyeLocation().getDirection().normalize();
        Set<UUID> hit = new HashSet<>();
        int broken = 0;
        Particle.DustOptions core = new Particle.DustOptions(PALE_GOLD, 1.5f + width);
        for (double d = 0.5; d <= range; d += 0.4) {
            Location at = from.clone().add(dir.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.DUST, at, 3, 0.08, 0.08, 0.08, 0.0, core);
            Block block = at.getBlock();
            if (!block.isPassable()) {
                if (broken >= pierceBlocks || !this.tryBreak(player, block)) break;
                broken++;
            }
            for (LivingEntity e : at.getNearbyLivingEntities(0.6 + width * 0.3)) {
                if (!this.isEnemy(player, e) || !hit.add(e.getUniqueId())) continue;
                e.damage(damage, player);
                e.getWorld().spawnParticle(Particle.EXPLOSION, e.getLocation().add(0, 1, 0), 2, 0.3, 0.3, 0.3, 0.0);
            }
        }
    }

    // ---- Moveset II: Prophet's Ladder / Gilded Blitz, Mark of the Beast ----

    private boolean ladderReady(Player player) {
        int left = this.plugin.getAbilityManager().getRemainingCooldown(player, LADDER_KEY);
        if (left > 0) {
            player.sendMessage("§6Prophet's Ladder §7and §6Gilded Blitz §7recharge in §c" + this.formatLong(left));
            return false;
        }
        this.plugin.getAbilityManager().setCooldown(player, LADDER_KEY, this.plugin.getConfig().getInt("gold.awakened.ladder.cooldown", 180));
        return true;
    }

    /** A golden beam that drags every enemy along it into its middle, then strikes them with Wealth's Unfortunate. */
    private void prophetsLadder(Player player) {
        if (!this.ladderReady(player)) return;
        double length = this.plugin.getConfig().getDouble("gold.awakened.ladder.length", 20.0);
        double damage = this.plugin.getConfig().getDouble("gold.awakened.ladder.damage", 9.0);
        int unfortunate = this.plugin.getConfig().getInt("gold.awakened.ladder.unfortunate-seconds", 10);
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Location middle = eye.clone().add(dir.clone().multiply(length / 2.0));
        Set<LivingEntity> caught = new HashSet<>();
        for (double d = 0.5; d <= length; d += 0.5) {
            Location at = eye.clone().add(dir.clone().multiply(d));
            if (((int) (d * 2)) % 2 == 0) {
                player.getWorld().spawnParticle(Particle.DUST, at, 2, 0.15, 0.15, 0.15, 0.0, new Particle.DustOptions(PALE_GOLD, 2.0f));
            }
            for (LivingEntity e : at.getNearbyLivingEntities(1.6)) {
                if (this.isEnemy(player, e)) caught.add(e);
            }
        }
        player.getWorld().playSound(eye, Sound.BLOCK_BEACON_ACTIVATE, 1.6f, 1.4f);
        new BukkitRunnable() {
            int t;

            public void run() {
                if (this.t++ >= 10) {
                    for (LivingEntity e : caught) {
                        if (!e.isValid() || e.isDead()) continue;
                        e.damage(damage, player);
                        if (e instanceof Player victim) WealthAbilities.afflictUnfortunate(AwakenedGold.this.plugin, victim, unfortunate);
                        e.getWorld().spawnParticle(Particle.DUST, e.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.0, new Particle.DustOptions(GOLD, 1.8f));
                    }
                    middle.getWorld().playSound(middle, Sound.BLOCK_ANVIL_LAND, 1.2f, 1.4f);
                    this.cancel();
                    return;
                }
                for (LivingEntity e : caught) {
                    if (!e.isValid() || e.isDead()) continue;
                    Vector pull = middle.toVector().subtract(e.getLocation().toVector());
                    if (pull.lengthSquared() > 1.0) e.setVelocity(pull.normalize().multiply(0.7));
                }
                middle.getWorld().spawnParticle(Particle.DUST, middle, 12, 0.5, 0.5, 0.5, 0.0, new Particle.DustOptions(GOLD, 1.6f));
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
    }

    /** Yanks the enemy in sight to the user, hurts them and strips Regeneration, Resistance and Absorption. */
    private void gildedBlitz(Player player) {
        double reach = this.plugin.getConfig().getDouble("gold.awakened.blitz.range", 16.0);
        RayTraceResult trace = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), reach, 0.8,
            e -> e instanceof LivingEntity && this.isEnemy(player, (LivingEntity) e));
        if (trace == null || !(trace.getHitEntity() instanceof LivingEntity target)) {
            player.sendMessage("§6Gilded Blitz §7needs an enemy in sight.");
            return;
        }
        if (!this.ladderReady(player)) return;
        double damage = this.plugin.getConfig().getDouble("gold.awakened.blitz.damage", 7.0);
        Vector pull = player.getLocation().toVector().subtract(target.getLocation().toVector());
        target.setVelocity(pull.multiply(0.25).setY(0.4));
        for (PotionEffectType type : List.of(PotionEffectType.REGENERATION, PotionEffectType.RESISTANCE, PotionEffectType.ABSORPTION)) {
            target.removePotionEffect(type);
        }
        target.setAbsorptionAmount(0.0);
        target.damage(damage, player);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.0, new Particle.DustOptions(GOLD, 1.8f));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1.2f, 1.6f);
    }

    private int markCharges(Player player) {
        int max = this.plugin.getConfig().getInt("gold.awakened.mark.charges", 2);
        List<Long> refills = this.markRefills.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
        long now = System.currentTimeMillis();
        refills.removeIf(at -> at <= now);
        return Math.max(0, max - refills.size());
    }

    /** For the action bar: Mark of the Beast charges ready right now. */
    public int getMarkCharges(Player player) {
        return this.markCharges(player);
    }

    /** A short, brutal beam on two charges, with a pause between shots. */
    private void markOfTheBeast(Player player) {
        long now = System.currentTimeMillis();
        long gap = this.plugin.getConfig().getInt("gold.awakened.mark.delay-seconds", 5) * 1000L;
        Long last = this.lastMark.get(player.getUniqueId());
        if (last != null && now - last < gap) {
            player.sendMessage("§6Mark of the Beast §7is ready again in §c" + (int) Math.ceil((gap - (now - last)) / 1000.0) + "s");
            return;
        }
        if (this.markCharges(player) <= 0) {
            long next = Collections.min(this.markRefills.get(player.getUniqueId()));
            player.sendMessage("§6Mark of the Beast §7has no charge. Next in §c" + (int) Math.ceil((next - now) / 1000.0) + "s");
            return;
        }
        this.markRefills.get(player.getUniqueId()).add(now + this.plugin.getConfig().getInt("gold.awakened.mark.charge-seconds", 90) * 1000L);
        this.lastMark.put(player.getUniqueId(), now);
        double range = this.plugin.getConfig().getDouble("gold.awakened.mark.range", 7.0);
        double damage = this.plugin.getConfig().getDouble("gold.awakened.mark.damage", 13.0);
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Set<UUID> hit = new HashSet<>();
        for (double d = 0.5; d <= range; d += 0.3) {
            Location at = eye.clone().add(dir.clone().multiply(d));
            if (!at.getBlock().isPassable()) break;
            player.getWorld().spawnParticle(Particle.DUST, at, 4, 0.12, 0.12, 0.12, 0.0, new Particle.DustOptions(Color.fromRGB(150, 20, 20), 2.2f));
            player.getWorld().spawnParticle(Particle.DUST, at, 2, 0.05, 0.05, 0.05, 0.0, new Particle.DustOptions(GOLD, 1.2f));
            for (LivingEntity e : at.getNearbyLivingEntities(0.9)) {
                if (!this.isEnemy(player, e) || !hit.add(e.getUniqueId())) continue;
                e.damage(damage, player);
            }
        }
        player.getWorld().playSound(eye, Sound.ENTITY_WITHER_SHOOT, 1.4f, 0.6f);
        player.sendMessage("§6Mark of the Beast §7- §e" + this.markCharges(player) + "§7/" + this.plugin.getConfig().getInt("gold.awakened.mark.charges", 2) + " charges left");
    }

    // ---- Gilded Wrath ----

    private void gildedWrath(Player player) {
        int left = this.plugin.getAbilityManager().getRemainingCooldown(player, WRATH_KEY);
        if (left > 0) {
            player.sendMessage("§6Gilded Wrath §7recharges in §c" + left + "s");
            return;
        }
        this.plugin.getAbilityManager().setCooldown(player, WRATH_KEY, this.plugin.getConfig().getInt("gold.awakened.wrath.cooldown", 50));
        double radius = this.plugin.getConfig().getDouble("gold.awakened.wrath.radius", 8.0);
        double maxDamage = this.plugin.getConfig().getDouble("gold.awakened.wrath.max-damage", 30.0);
        int lockSeconds = this.plugin.getConfig().getInt("gold.awakened.wrath.gem-lock-seconds", 20);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.6f, 0.5f);
        new BukkitRunnable() {
            int t;

            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    this.cancel();
                    return;
                }
                Location centre = player.getLocation().add(0, 1, 0);
                if (this.t < 20) {
                    for (LivingEntity e : centre.getNearbyLivingEntities(radius)) {
                        if (AwakenedGold.this.isEnemy(player, e)) e.setVelocity(centre.toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.4));
                    }
                    double r = radius * (1.0 - this.t / 20.0);
                    for (int i = 0; i < 16; i++) {
                        double a = Math.PI * 2 * i / 16.0 + this.t * 0.3;
                        player.getWorld().spawnParticle(Particle.DUST, centre.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r), 1, 0, 0, 0, 0.0, new Particle.DustOptions(GOLD, 1.6f));
                    }
                    this.t++;
                    return;
                }
                player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, centre, 1);
                player.getWorld().playSound(centre, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
                for (LivingEntity e : centre.getNearbyLivingEntities(radius)) {
                    if (!AwakenedGold.this.isEnemy(player, e)) continue;
                    double dist = e.getLocation().distance(centre);
                    e.damage(maxDamage * Math.max(0.25, 1.0 - dist / radius), player);
                    e.setVelocity(e.getLocation().toVector().subtract(centre.toVector()).normalize().multiply(1.6).setY(0.6));
                    if (e instanceof Player victim) {
                        AwakenedGold.this.randomDebuffs(player, victim);
                        if (AwakenedGold.this.plugin.getGemLockManager() != null) AwakenedGold.this.plugin.getGemLockManager().lock(victim, lockSeconds);
                    }
                }
                this.cancel();
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
    }

    /** One or two of the gems' debuffs: Frailer, Nullify, Item Lock, Unfortunate, Heart Drainer, Heart Lock. */
    private void randomDebuffs(Player caster, Player victim) {
        int seconds = this.plugin.getConfig().getInt("gold.awakened.wrath.debuff-seconds", 10);
        List<Integer> pool = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
        Collections.shuffle(pool);
        int count = 1 + ThreadLocalRandom.current().nextInt(2);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            switch (pool.get(i)) {
                case 0 -> {
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, seconds * 20, 0));
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, seconds * 20, 0));
                    names.add("Frailer");
                }
                case 1 -> {
                    for (PotionEffect effect : new ArrayList<>(victim.getActivePotionEffects())) victim.removePotionEffect(effect.getType());
                    names.add("Nullify");
                }
                case 2 -> {
                    if (WealthAbilities.lockHeldItem(this.plugin, victim, seconds)) names.add("Item Lock");
                }
                case 3 -> {
                    WealthAbilities.afflictUnfortunate(this.plugin, victim, seconds);
                    names.add("Unfortunate");
                }
                case 4 -> {
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, seconds * 20, 1));
                    names.add("Heart Drainer");
                }
                default -> {
                    this.heartLock(victim, seconds);
                    names.add("Heart Lock");
                }
            }
        }
        if (!names.isEmpty()) victim.sendMessage("§6§lGilded Wrath §7- " + String.join(", ", names) + " §7(" + seconds + "s)");
    }

    /** Caps max health at current health for a while (the same modifier Life's Heart Lock uses). */
    private void heartLock(Player victim, int seconds) {
        AttributeInstance attr = victim.getAttribute(Attributes.maxHealth());
        if (attr == null) return;
        for (AttributeModifier m : new ArrayList<>(attr.getModifiers())) {
            if (this.heartLockKey.equals(m.getKey())) attr.removeModifier(m);
        }
        double reduction = victim.getHealth() - attr.getValue();
        if (reduction >= 0) return;
        attr.addModifier(new AttributeModifier(this.heartLockKey, reduction, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            AttributeInstance a = victim.getAttribute(Attributes.maxHealth());
            if (a == null) return;
            for (AttributeModifier m : new ArrayList<>(a.getModifiers())) {
                if (this.heartLockKey.equals(m.getKey())) a.removeModifier(m);
            }
        }, seconds * 20L);
    }

    // ---- passives ----

    private void passiveTick() {
        // runs every 2 ticks; the server tick's parity depends on when the task started, so count runs
        boolean prophecyTick = this.passiveRuns++ % 10 == 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean awake = this.isAwakened(player) && this.plugin.getEnergyManager().getEnergy(player) > 1;
            if (prophecyTick) this.prophecy(player, awake);
            if (awake) this.midasTouch(player);
        }
    }

    /** Prophecy: ten extra hearts while the awakened gem is held. */
    private void prophecy(Player player, boolean awake) {
        AttributeInstance attr = player.getAttribute(Attributes.maxHealth());
        if (attr == null) return;
        AttributeModifier current = null;
        for (AttributeModifier m : attr.getModifiers()) {
            if (this.prophecyKey.equals(m.getKey())) current = m;
        }
        double bonus = this.plugin.getConfig().getDouble("gold.awakened.prophecy-health", 20.0);
        if (awake && current == null) {
            attr.addModifier(new AttributeModifier(this.prophecyKey, bonus, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        } else if (!awake && current != null) {
            attr.removeModifier(current);
            if (player.getHealth() > attr.getValue()) player.setHealth(attr.getValue());
        }
    }

    /** Midas Touch: enemy pearls and wind charges that come close hang frozen in the air, then fall apart. */
    private void midasTouch(Player player) {
        for (Entity e : player.getNearbyEntities(5.0, 5.0, 5.0)) {
            if (!(e instanceof EnderPearl) && !(e instanceof AbstractWindCharge)) continue;
            Projectile projectile = (Projectile) e;
            if (player.equals(projectile.getShooter())) continue;
            if (projectile.getShooter() instanceof Player shooter && this.isTrusted(player, shooter)) continue;
            if (projectile.hasGravity() || projectile.getVelocity().lengthSquared() > 1.0E-6) {
                projectile.setVelocity(new Vector());
                projectile.setGravity(false);
                if (projectile instanceof AbstractWindCharge charge) charge.setAcceleration(new Vector());
                projectile.getWorld().spawnParticle(Particle.DUST, projectile.getLocation(), 8, 0.15, 0.15, 0.15, 0.0, new Particle.DustOptions(GOLD, 1.2f));
                projectile.getWorld().playSound(projectile.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.8f);
                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    if (projectile.isValid()) projectile.remove();
                }, 60L);
            }
        }
    }

    /** Ichor: a hit can shake a utility item out of the victim's inventory onto the floor. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIchor(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player victim)) return;
        if (!this.isAwakened(attacker) || this.plugin.getEnergyManager().getEnergy(attacker) <= 1 || this.isTrusted(attacker, victim)) return;
        if (ThreadLocalRandom.current().nextDouble() >= this.plugin.getConfig().getDouble("gold.awakened.ichor-chance", 0.08)) return;
        ItemStack[] contents = victim.getInventory().getStorageContents();
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && ICHOR_ITEMS.contains(contents[i].getType())) candidates.add(i);
        }
        if (candidates.isEmpty()) return;
        int slot = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        ItemStack dropped = contents[slot];
        victim.getInventory().setItem(slot, null);
        victim.getWorld().dropItemNaturally(victim.getLocation(), dropped);
        victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 15, 0.3, 0.4, 0.3, 0.0, new Particle.DustOptions(GOLD, 1.2f));
        victim.sendMessage("§6§oIchor shook loose your " + dropped.getType().name().toLowerCase().replace('_', ' ') + "!");
    }

    /** Drilling users take no fall damage. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        UUID id = event.getEntity().getUniqueId();
        if (this.drilling.contains(id)) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.prophecy(event.getPlayer(), false);
        this.drilling.remove(event.getPlayer().getUniqueId());
    }

    // ---- helpers ----

    private boolean isTrusted(Player owner, Player other) {
        return this.plugin.getTrustedPlayersManager() != null && this.plugin.getTrustedPlayersManager().isTrusted(owner, other);
    }

    private boolean isEnemy(Player owner, LivingEntity e) {
        if (e.equals(owner) || e.isDead()) return false;
        if (e instanceof Player p) {
            return !this.isTrusted(owner, p) && p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR;
        }
        return true;
    }

    private void breakAround(Player player, Location centre, double radius) {
        int r = (int) Math.ceil(radius);
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z > radius * radius) continue;
                    Block b = centre.clone().add(x, y, z).getBlock();
                    if (!b.isPassable()) this.tryBreak(player, b);
                }
            }
        }
    }

    /** Breaks a block as the player would, so claims and region protection still apply. */
    private boolean tryBreak(Player player, Block block) {
        if (block.getType().getHardness() < 0 || block.getType() == Material.BEDROCK) return false;
        BlockBreakEvent check = new BlockBreakEvent(block, player);
        Bukkit.getPluginManager().callEvent(check);
        if (check.isCancelled()) return false;
        block.breakNaturally();
        return true;
    }

    private String formatLong(int seconds) {
        if (seconds >= 3600) return (seconds / 3600) + "h " + (seconds % 3600 / 60) + "m";
        if (seconds >= 60) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }
}
