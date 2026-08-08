package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.api.GemRegistry;
import dev.xoperr.blissgems.managers.GoldGemManager;
import dev.xoperr.blissgems.utils.AbilitySlot;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gold Gem abilities.
 *
 * The gem has one power of its own - a charged beam - and otherwise channels whichever
 * harvested soul the holder has selected:
 *
 *   use               -> the selected soul's primary
 *   shift + use       -> the selected soul's secondary
 *   left-click        -> the selected soul's tertiary
 *   shift+left-click  -> the selected soul's quaternary
 *   F                 -> Sundering Beam (charges in the open, then fires)
 *   shift + F         -> the soul selection menu
 */
public class GoldAbilities implements GemAbilityHandler, Listener {

    /** Cooldown key for the Sundering Beam - also read by the action bar display. */
    public static final String BEAM_COOLDOWN_ID = "gold-beam";
    private static final String SOUL_MENU_TITLE = "§6§lHarvested Souls";

    private final BlissGems plugin;
    // Players currently charging the beam, mapped to the task winding it up.
    private final Map<UUID, BukkitRunnable> charging = new HashMap<>();

    public GoldAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    // ========================================================================
    // Channelled soul abilities
    // ========================================================================

    @Override
    public void onPrimary(Player player, int tier) {
        this.channel(player, AbilitySlot.PRIMARY);
    }

    @Override
    public void onSecondary(Player player, int tier) {
        this.channel(player, AbilitySlot.SECONDARY);
    }

    /** Left-click: the selected soul's tertiary, the first of the two extra inputs. */
    @Override
    public void onQuinary(Player player, int tier) {
        this.channel(player, AbilitySlot.TERTIARY);
    }

    /** Shift + left-click: the selected soul's quaternary. */
    @Override
    public void onSenary(Player player, int tier) {
        this.channel(player, AbilitySlot.QUATERNARY);
    }

    /**
     * Hand the activation over to the selected soul's own handler. The Gold Gem reaches all
     * four of a soul's slots: its own two powers sit on F and shift+F, the soul's on the
     * right-click and left-click pairs.
     */
    private void channel(Player player, AbilitySlot slot) {
        GoldGemManager gold = this.plugin.getGoldGemManager();
        String soul = gold.getActive(player.getUniqueId());
        if (soul == null) {
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-no-soul"));
            return;
        }
        GemRegistry registry = this.plugin.getGemRegistry();
        GemAbilityHandler handler = registry != null ? registry.getAbilityHandler(soul) : null;
        if (handler == null) {
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-no-soul"));
            return;
        }
        int soulTier = gold.getActiveTier(player.getUniqueId());
        // A soul only gives up what it had: a gem harvested at Tier 1 is refused by its own
        // handler's Tier 2 gate, so say that in the Gold Gem's own words rather than letting
        // it read as "your gem is Tier 1" - the Gold Gem has no Tier 2 to reach.
        if (slot != AbilitySlot.PRIMARY && soulTier < 2) {
            // Live configs predate this key, so fall back rather than sending an empty line.
            String message = this.plugin.getConfigManager().getMessage("gold-soul-too-weak");
            if (message.isEmpty()) {
                message = "§c§oThe {gem} soul was taken at Tier 1, it has no second power to give.";
            }
            player.sendMessage(message.replace("{gem}", this.plugin.getGemManager().getGemDisplayName(soul)));
            return;
        }
        // Make the soul's own Tier-2 gate see the tier it was harvested at, not the Gold
        // Gem's (which has no Tier 2) — otherwise Tier-2 soul abilities wrongly report
        // "requires Tier 2".
        this.plugin.getGemManager().setChannelTierOverride(player.getUniqueId(), soulTier);
        try {
            switch (slot) {
                case PRIMARY -> handler.onPrimary(player, soulTier);
                case SECONDARY -> handler.onSecondary(player, soulTier);
                case TERTIARY -> handler.onTertiary(player, soulTier);
                default -> handler.onQuaternary(player, soulTier);
            }
        } finally {
            this.plugin.getGemManager().clearChannelTierOverride(player.getUniqueId());
        }
    }

    // ========================================================================
    // Sundering Beam
    // ========================================================================

    @Override
    public void onTertiary(Player player, int tier) {
        UUID playerId = player.getUniqueId();
        if (this.charging.containsKey(playerId)) {
            this.breakCharge(player, "gold-beam-cancelled");
            return;
        }
        // The beam only winds up from the MAIN hand — left-clicking with the gem in the
        // offhand must not charge it.
        if (!this.plugin.getGoldGemManager().isGoldGemInMainHand(player)) {
            return;
        }
        if (this.plugin.getAbilityManager().isOnCooldown(player, BEAM_COOLDOWN_ID)) {
            int remaining = this.plugin.getAbilityManager().getRemainingCooldown(player, BEAM_COOLDOWN_ID);
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-beam-cooldown")
                .replace("{seconds}", String.valueOf(remaining)));
            return;
        }

        int chargeTicks = Math.max(1, this.plugin.getConfig().getInt("gold.beam.charge-ticks", 60));
        player.sendMessage(this.plugin.getConfigManager().getMessage("gold-beam-charging"));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.4F, 0.5F);

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    GoldAbilities.this.charging.remove(playerId);
                    this.cancel();
                    return;
                }
                // The wind-up is deliberately loud: the target gets a window to break line
                // of sight or punish the caster for standing still.
                Location eye = player.getEyeLocation();
                Vector direction = eye.getDirection().normalize();
                for (double distance = 1.0; distance <= 4.0; distance += 0.5) {
                    player.getWorld().spawnParticle(Particle.DUST,
                        eye.clone().add(direction.clone().multiply(distance)), 2, 0.1, 0.1, 0.1, 0.0,
                        new Particle.DustOptions(Color.fromRGB(255, 200, 40), 1.4F));
                }
                if (this.ticks % 10 == 0) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.2F, 0.6F + this.ticks / 120.0F);
                }
                if (++this.ticks >= chargeTicks) {
                    GoldAbilities.this.charging.remove(playerId);
                    this.cancel();
                    GoldAbilities.this.fireBeam(player);
                }
            }
        };
        this.charging.put(playerId, task);
        task.runTaskTimer((Plugin) this.plugin, 1L, 1L);
    }

    /** Fire the beam along the caster's line of sight and burn the first thing it touches. */
    private void fireBeam(Player player) {
        double range = this.plugin.getConfig().getDouble("gold.beam.range", 60.0);
        double damage = this.plugin.getConfig().getDouble("gold.beam.damage", 1000.0);
        int cooldown = this.plugin.getConfig().getInt("gold.beam.cooldown", 300);

        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        // Stop at the first block so the beam cannot shoot through walls.
        RayTraceResult blockHit = player.getWorld().rayTraceBlocks(eye, direction, range);
        double reach = blockHit != null && blockHit.getHitPosition() != null
            ? blockHit.getHitPosition().distance(eye.toVector())
            : range;

        RayTraceResult entityHit = player.getWorld().rayTraceEntities(eye, direction, reach, 0.6,
            entity -> entity instanceof LivingEntity && !entity.equals(player));

        // A thick core with a wider corona around it: at this cooldown the beam should look
        // like the thing that just cost you five minutes, not a line of dust.
        Vector sideways = direction.clone().crossProduct(new Vector(0.0, 1.0, 0.0));
        if (sideways.lengthSquared() < 1.0E-4) {
            sideways = new Vector(1.0, 0.0, 0.0);
        }
        sideways.normalize();
        Vector upwards = direction.clone().crossProduct(sideways).normalize();
        Particle.DustOptions core = new Particle.DustOptions(Color.fromRGB(255, 235, 130), 3.0F);
        Particle.DustOptions corona = new Particle.DustOptions(Color.fromRGB(255, 170, 0), 1.6F);

        for (double distance = 0.0; distance <= reach; distance += 0.2) {
            Location at = eye.clone().add(direction.clone().multiply(distance));
            player.getWorld().spawnParticle(Particle.DUST, at, 6, 0.12, 0.12, 0.12, 0.0, core);
            player.getWorld().spawnParticle(Particle.END_ROD, at, 1, 0.05, 0.05, 0.05, 0.0);
            // Four points on a ring around the core, rotating along the beam so the corona
            // reads as a spiral rather than a tube.
            double spin = distance * 1.2;
            for (int step = 0; step < 4; step++) {
                double angle = spin + step * Math.PI / 2.0;
                Location ring = at.clone()
                    .add(sideways.clone().multiply(Math.cos(angle) * 0.55))
                    .add(upwards.clone().multiply(Math.sin(angle) * 0.55));
                player.getWorld().spawnParticle(Particle.DUST, ring, 2, 0.05, 0.05, 0.05, 0.0, corona);
            }
        }
        player.getWorld().playSound(eye, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.6F, 1.2F);
        this.plugin.getAbilityManager().setCooldown(player, BEAM_COOLDOWN_ID, cooldown);

        if (entityHit != null && entityHit.getHitEntity() instanceof LivingEntity target) {
            target.damage(damage, player);
            target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 6, 0.6, 0.6, 0.6, 0.0);
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0),
                60, 0.5, 0.7, 0.5, 0.0, core);
        } else {
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-beam-missed"));
        }
    }

    /** Cut a charge short, either by the caster's own input or by being hit. */
    private void breakCharge(Player player, String messageKey) {
        BukkitRunnable task = this.charging.remove(player.getUniqueId());
        if (task == null) {
            return;
        }
        task.cancel();
        player.sendMessage(this.plugin.getConfigManager().getMessage(messageKey));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.2F, 0.8F);
    }

    /** A charging caster who takes a hit loses the beam - the wind-up is punishable. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChargerDamaged(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && this.charging.containsKey(player.getUniqueId())) {
            this.breakCharge(player, "gold-beam-broken");
        }
    }

    // ========================================================================
    // Soul menu
    // ========================================================================

    @Override
    public void onQuaternary(Player player, int tier) {
        this.openSoulMenu(player);
    }

    /** Show every harvested soul; clicking one makes it the channelled gem. */
    public void openSoulMenu(Player player) {
        Map<String, GoldGemManager.Harvest> souls = this.plugin.getGoldGemManager().getHarvested(player.getUniqueId());
        if (souls.isEmpty()) {
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-no-soul"));
            return;
        }
        Inventory menu = Bukkit.createInventory(null, 27, SOUL_MENU_TITLE);
        String selected = this.plugin.getGoldGemManager().getActive(player.getUniqueId());
        int slot = 0;
        for (Map.Entry<String, GoldGemManager.Harvest> soul : souls.entrySet()) {
            ItemStack icon = CustomItemManager.getItemById(soul.getKey() + "_gem_t" + soul.getValue().tier());
            if (icon == null) {
                continue;
            }
            ItemMeta meta = icon.getItemMeta();
            if (meta != null && soul.getKey().equals(selected)) {
                meta.setDisplayName(meta.getDisplayName() + " §a§l< CHANNELLED >");
                icon.setItemMeta(meta);
            }
            menu.setItem(slot++, icon);
        }
        player.openInventory(menu);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSoulMenuClick(InventoryClickEvent event) {
        if (!SOUL_MENU_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getCurrentItem() == null) {
            return;
        }
        String itemId = CustomItemManager.getIdByItem(event.getCurrentItem());
        String gemId = itemId != null ? this.plugin.getGemRegistry().gemIdFromItemId(itemId) : null;
        if (gemId == null) {
            return;
        }
        if (this.plugin.getGoldGemManager().setActive(player, gemId)) {
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-soul-selected")
                .replace("{gem}", this.plugin.getGemManager().getGemDisplayName(gemId)));
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 1.4F);
        }
        player.closeInventory();
    }

    // ========================================================================
    // Passive: unstable power
    // ========================================================================

    /** Every hit the holder lands has a chance to come through amplified. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHolderAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!this.plugin.getGoldGemManager().holdsGoldGem(player)) {
            return;
        }
        double chance = this.plugin.getConfig().getDouble("gold.damage-boost.chance", 0.15);
        if (Math.random() >= chance) {
            return;
        }
        double multiplier = this.plugin.getConfig().getDouble("gold.damage-boost.multiplier", 1.5);
        event.setDamage(event.getDamage() * multiplier);
        player.getWorld().spawnParticle(Particle.DUST, event.getEntity().getLocation().add(0.0, 1.0, 0.0),
            10, 0.3, 0.3, 0.3, 0.0, new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.2F));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8F, 1.6F);
    }

    @Override
    public void cleanup(Player player) {
        BukkitRunnable task = this.charging.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    /** Harvested souls are written on every change, so leaving only drops the cached copy. */
    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        this.cleanup(event.getPlayer());
        this.plugin.getGoldGemManager().unload(event.getPlayer().getUniqueId());
    }
}
