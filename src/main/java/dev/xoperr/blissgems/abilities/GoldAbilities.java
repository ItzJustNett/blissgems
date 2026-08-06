package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.api.GemRegistry;
import dev.xoperr.blissgems.managers.GoldGemManager;
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
 *   use          -> the selected soul's primary
 *   shift + use  -> the selected soul's secondary
 *   F            -> Sundering Beam (charges in the open, then fires)
 *   shift + F    -> the soul selection menu
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
        this.channel(player, true);
    }

    @Override
    public void onSecondary(Player player, int tier) {
        this.channel(player, false);
    }

    /** Hand the activation over to the selected soul's own handler. */
    private void channel(Player player, boolean primary) {
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
        if (primary) {
            handler.onPrimary(player, soulTier);
        } else {
            handler.onSecondary(player, soulTier);
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

        for (double distance = 0.0; distance <= reach; distance += 0.4) {
            player.getWorld().spawnParticle(Particle.DUST,
                eye.clone().add(direction.clone().multiply(distance)), 3, 0.05, 0.05, 0.05, 0.0,
                new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.6F));
        }
        player.getWorld().playSound(eye, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.6F, 1.2F);
        this.plugin.getAbilityManager().setCooldown(player, BEAM_COOLDOWN_ID, cooldown);

        if (entityHit != null && entityHit.getHitEntity() instanceof LivingEntity target) {
            target.damage(damage, player);
            target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 3, 0.4, 0.4, 0.4, 0.0);
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
        Map<String, Integer> souls = this.plugin.getGoldGemManager().getHarvested(player.getUniqueId());
        if (souls.isEmpty()) {
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-no-soul"));
            return;
        }
        Inventory menu = Bukkit.createInventory(null, 27, SOUL_MENU_TITLE);
        String selected = this.plugin.getGoldGemManager().getActive(player.getUniqueId());
        int slot = 0;
        for (Map.Entry<String, Integer> soul : souls.entrySet()) {
            ItemStack icon = CustomItemManager.getItemById(soul.getKey() + "_gem_t" + soul.getValue());
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
