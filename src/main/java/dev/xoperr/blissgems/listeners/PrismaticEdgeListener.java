package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.abilities.SpeedAbilities;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Prismatic Edge — a legendary sword with two mechanics:
 *
 * <ul>
 *   <li><b>Prismatic Beam</b> (right click): a long-range beam cycling through every gem
 *       colour that damages and freezes whatever it passes through, and heals the wielder.</li>
 *   <li><b>Combo crits</b>: five consecutive hits put the wielder into a crit state where
 *       every following hit crits, until the target lands a hit back.</li>
 * </ul>
 */
public class PrismaticEdgeListener implements Listener {
    private static final String ITEM_ID = "prismatic_edge";

    /** Beam colours, one per built-in gem, cycled along the beam's length. */
    private static final Color[] PRISM_COLORS = {
        Color.fromRGB(106, 11, 184),   // Astra
        Color.fromRGB(255, 85, 85),    // Fire
        Color.fromRGB(85, 255, 255),   // Flux
        Color.fromRGB(85, 255, 85),    // Life
        Color.fromRGB(255, 255, 255),  // Puff
        Color.fromRGB(255, 255, 85),   // Speed
        Color.fromRGB(170, 0, 0),      // Strength
        Color.fromRGB(255, 170, 0)     // Wealth
    };

    private final BlissGems plugin;

    /** Consecutive hits landed without being hit back. */
    private final Map<UUID, Integer> comboHits = new HashMap<>();
    /** Wielders whose combo has matured into guaranteed crits. */
    private final Set<UUID> critState = new HashSet<>();
    /** Beam cooldown expiry, in millis since epoch. */
    private final Map<UUID, Long> beamCooldowns = new HashMap<>();

    public PrismaticEdgeListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    private boolean isPrismaticEdge(ItemStack item) {
        return item != null && ITEM_ID.equals(CustomItemManager.getIdByItem(item));
    }

    // ========================================================================
    // Prismatic Beam
    // ========================================================================

    @EventHandler
    public void onBeamCast(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!isPrismaticEdge(event.getItem())) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);

        int cooldownSeconds = this.plugin.getConfig().getInt("prismatic-edge.beam.cooldown-seconds", 120);
        long now = System.currentTimeMillis();
        long readyAt = beamCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now < readyAt) {
            long remaining = (readyAt - now + 999) / 1000;
            player.sendMessage("§c§oPrismatic Beam is recharging - §f" + remaining + "s§c§o left.");
            return;
        }
        beamCooldowns.put(player.getUniqueId(), now + cooldownSeconds * 1000L);

        fireBeam(player);
    }

    private void fireBeam(Player player) {
        double range = this.plugin.getConfig().getDouble("prismatic-edge.beam.range", 24.0);
        double damage = this.plugin.getConfig().getDouble("prismatic-edge.beam.damage", 10.0);
        double beamRadius = this.plugin.getConfig().getDouble("prismatic-edge.beam.hit-radius", 1.6);
        int freezeSeconds = this.plugin.getConfig().getInt("prismatic-edge.beam.freeze-seconds", 3);
        int regenSeconds = this.plugin.getConfig().getInt("prismatic-edge.beam.regen-seconds", 8);
        int regenLevel = this.plugin.getConfig().getInt("prismatic-edge.beam.regen-level", 2);

        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Set<UUID> alreadyHit = new HashSet<>();

        player.getWorld().playSound(eye, Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.2f, 1.4f);
        player.getWorld().playSound(eye, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.8f);

        // Walk the beam forward, painting it and catching anything close to the line
        for (double travelled = 0; travelled < range; travelled += 0.4) {
            Location point = eye.clone().add(direction.clone().multiply(travelled));

            if (point.getBlock().getType().isSolid()) {
                break;
            }

            Color color = PRISM_COLORS[(int) (travelled * 2) % PRISM_COLORS.length];
            player.getWorld().spawnParticle(Particle.DUST, point, 4, 0.08, 0.08, 0.08, 0.0,
                new Particle.DustOptions(color, 1.4f), true);
            player.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.03, 0.03, 0.03, 0.0);

            for (Entity entity : player.getWorld().getNearbyEntities(point, beamRadius, beamRadius, beamRadius)) {
                if (!(entity instanceof LivingEntity living) || living.equals(player)) continue;
                if (!alreadyHit.add(living.getUniqueId())) continue;

                living.damage(damage, player);
                applyFreeze(living, freezeSeconds);

                living.getWorld().spawnParticle(Particle.FLASH, living.getLocation().add(0, 1, 0), 2, 0.2, 0.2, 0.2, 0.0);
                living.getWorld().playSound(living.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
            }
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,
            regenSeconds * 20, Math.max(0, regenLevel - 1), true, true));
        player.sendMessage("§b§l✦ §fPrismatic Beam released!");
    }

    /** Frost overlay plus the plugin's own freeze, so the target genuinely cannot move. */
    private void applyFreeze(LivingEntity target, int seconds) {
        target.setFreezeTicks(Math.max(target.getFreezeTicks(), seconds * 20 + 140));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, seconds * 20, 6, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, seconds * 20, 2, true, true));

        if (target instanceof Player frozen) {
            UUID id = frozen.getUniqueId();
            SpeedAbilities.freezePlayer(id);
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin,
                () -> SpeedAbilities.unfreezePlayer(id), seconds * 20L);
            frozen.sendMessage("§b§oYou are frozen solid!");
        }
    }

    // ========================================================================
    // Combo crits
    // ========================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onComboHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        // Being hit breaks the victim's own combo, whatever the attacker is holding
        if (event.getEntity() instanceof Player victim) {
            resetCombo(victim);
        }

        if (!isPrismaticEdge(attacker.getInventory().getItemInMainHand())) {
            return;
        }

        int required = this.plugin.getConfig().getInt("prismatic-edge.combo.hits-required", 5);
        double critMultiplier = this.plugin.getConfig().getDouble("prismatic-edge.combo.crit-multiplier", 1.5);

        UUID id = attacker.getUniqueId();

        // Already in crit state — every hit lands as a crit until someone hits back
        if (critState.contains(id)) {
            event.setDamage(event.getDamage() * critMultiplier);
            spawnCritEffect(event.getEntity());
            return;
        }

        int hits = comboHits.merge(id, 1, Integer::sum);
        if (hits >= required) {
            critState.add(id);
            comboHits.remove(id);
            attacker.playSound(attacker.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.6f);
            attacker.sendMessage("§b§l✦ §fCombo complete - §eyour hits now crit§f!");
        } else {
            attacker.playSound(attacker.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.0f + hits * 0.15f);
        }
    }

    private void spawnCritEffect(Entity target) {
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 25, 0.4, 0.4, 0.4, 0.4);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
    }

    private void resetCombo(Player player) {
        UUID id = player.getUniqueId();
        boolean hadCrit = critState.remove(id);
        comboHits.remove(id);
        if (hadCrit) {
            player.sendMessage("§7§oYour crit streak was broken.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        comboHits.remove(id);
        critState.remove(id);
        beamCooldowns.remove(id);
    }
}
