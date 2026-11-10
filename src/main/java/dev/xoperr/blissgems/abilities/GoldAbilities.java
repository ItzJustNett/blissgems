/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.util.RayTraceResult
 *  org.bukkit.util.Vector
 */
package dev.xoperr.blissgems.abilities;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.GemAbilityHandler;
import dev.xoperr.blissgems.managers.GemRegistryImpl;
import dev.xoperr.blissgems.managers.GoldGemManager;
import dev.xoperr.blissgems.utils.AbilitySlot;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class GoldAbilities
implements GemAbilityHandler,
Listener {
    public static final String BEAM_COOLDOWN_ID = "gold-beam";
    private static final String SOUL_MENU_TITLE = "\u00a76\u00a7lHarvested Souls";
    /** Moveset I sits here and Moveset II next to it in the soul menu. */
    private static final int MOVESET_SLOT = 18;
    private static final Set<String> PRIMARY_REQUIRES_TIER_2 = Set.of("wealth", "strength");
    private final BlissGems plugin;
    private final Map<UUID, BukkitRunnable> charging = new HashMap<UUID, BukkitRunnable>();

    public GoldAbilities(BlissGems plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPrimary(Player player, int tier) {
        if (this.awakened(player)) {
            this.plugin.getAwakenedGold().primary(player);
            return;
        }
        this.channel(player, AbilitySlot.PRIMARY);
    }

    @Override
    public void onSecondary(Player player, int tier) {
        if (this.awakened(player)) {
            this.plugin.getAwakenedGold().secondary(player);
            return;
        }
        this.channel(player, AbilitySlot.SECONDARY);
    }

    @Override
    public void onQuinary(Player player, int tier) {
        if (this.awakened(player)) {
            this.plugin.getAwakenedGold().quinary(player);
            return;
        }
        this.channel(player, AbilitySlot.TERTIARY);
    }

    @Override
    public void onSenary(Player player, int tier) {
        if (this.awakened(player)) {
            this.chargeBeam(player);
            return;
        }
        this.channel(player, AbilitySlot.QUATERNARY);
    }

    /** A fully awakened gem with a moveset picked in /bliss goldcycle: that kit replaces soul channelling (see AwakenedGold). */
    private boolean awakened(Player player) {
        return this.plugin.getAwakenedGold() != null && this.plugin.getAwakenedGold().moveset(player) > 0;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private void channel(Player player, AbilitySlot slot) {
        boolean primaryNeedsTier2;
        GemAbilityHandler handler;
        GoldGemManager gold = this.plugin.getGoldGemManager();
        String soul = gold.getActive(player.getUniqueId());
        if (soul == null) {
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-no-soul"));
            return;
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        GemAbilityHandler gemAbilityHandler = handler = registry != null ? registry.getAbilityHandler(soul) : null;
        if (handler == null) {
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-no-soul"));
            return;
        }
        int soulTier = gold.getActiveTier(player.getUniqueId());
        if (soulTier < 2 && slot == AbilitySlot.PRIMARY && !"puff".equals(soul)) {
            // a Tier 1 soul holds only its ability, like a Tier 1 gem (Wealth's is Pockets)
            if ("wealth".equals(soul)) {
                this.plugin.getWealthAbilities().pockets(player);
                return;
            }
            slot = AbilitySlot.TERTIARY;
        }
        boolean bl = primaryNeedsTier2 = slot == AbilitySlot.PRIMARY && PRIMARY_REQUIRES_TIER_2.contains(soul);
        if ((slot != AbilitySlot.PRIMARY && !(slot == AbilitySlot.TERTIARY && !"puff".equals(soul)) || primaryNeedsTier2) && soulTier < 2) {
            String message = this.plugin.getConfigManager().getMessage("gold-soul-too-weak");
            if (message.isEmpty()) {
                message = "\u00a7c\u00a7oThe {gem} soul was taken at Tier 1, it has no second power to give.";
            }
            player.sendMessage(message.replace("{gem}", this.plugin.getGemManager().getGemDisplayName(soul)));
            return;
        }
        this.plugin.getGemManager().setChannelTierOverride(player.getUniqueId(), soulTier);
        try {
            switch (slot) {
                case PRIMARY: {
                    handler.onPrimary(player, soulTier);
                    return;
                }
                case SECONDARY: {
                    handler.onSecondary(player, soulTier);
                    return;
                }
                case TERTIARY: {
                    handler.onTertiary(player, soulTier);
                    return;
                }
                default: {
                    handler.onQuaternary(player, soulTier);
                    return;
                }
            }
        }
        finally {
            this.plugin.getGemManager().clearChannelTierOverride(player.getUniqueId());
        }
    }

    @Override
    public void onTertiary(Player player, int tier) {
        if (this.awakened(player)) {
            this.plugin.getAwakenedGold().tertiary(player);
            return;
        }
        this.chargeBeam(player);
    }

    private void chargeBeam(final Player player) {
        final UUID playerId = player.getUniqueId();
        if (this.charging.containsKey(playerId)) {
            this.breakCharge(player, "gold-beam-cancelled");
            return;
        }
        if (!this.plugin.getGoldGemManager().isGoldGemInMainHand(player)) {
            return;
        }
        if (this.plugin.getAbilityManager().isOnCooldown(player, BEAM_COOLDOWN_ID)) {
            int remaining = this.plugin.getAbilityManager().getRemainingCooldown(player, BEAM_COOLDOWN_ID);
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-beam-cooldown").replace("{seconds}", String.valueOf(remaining)));
            return;
        }
        final int chargeTicks = Math.max(1, this.plugin.getConfig().getInt("gold.beam.charge-ticks", 60));
        player.sendMessage(this.plugin.getConfigManager().getMessage("gold-beam-charging"));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.4f, 0.5f);
        BukkitRunnable task = new BukkitRunnable(){
            int ticks = 0;

            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    GoldAbilities.this.charging.remove(playerId);
                    this.cancel();
                    return;
                }
                Location eye = player.getEyeLocation();
                Vector direction = eye.getDirection().normalize();
                for (double distance = 1.0; distance <= 4.0; distance += 0.5) {
                    player.getWorld().spawnParticle(Particle.DUST, eye.clone().add(direction.clone().multiply(distance)), 2, 0.1, 0.1, 0.1, 0.0, (Object)new Particle.DustOptions(Color.fromRGB((int)255, (int)200, (int)40), 1.4f));
                }
                if (this.ticks % 10 == 0) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1.2f, 0.6f + (float)this.ticks / 120.0f);
                }
                if (++this.ticks >= chargeTicks) {
                    GoldAbilities.this.charging.remove(playerId);
                    this.cancel();
                    GoldAbilities.this.fireBeam(player);
                }
            }
        };
        this.charging.put(playerId, task);
        task.runTaskTimer((Plugin)this.plugin, 1L, 1L);
    }

    private void fireBeam(Player player) {
        Entity entity2;
        double range = this.plugin.getConfig().getDouble("gold.beam.range", 60.0);
        double damage = this.plugin.getConfig().getDouble("gold.beam.damage", 24.0);
        int cooldown = this.plugin.getConfig().getInt("gold.beam.cooldown", 300);
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        RayTraceResult blockHit = player.getWorld().rayTraceBlocks(eye, direction, range);
        double reach = blockHit != null && blockHit.getHitPosition() != null ? blockHit.getHitPosition().distance(eye.toVector()) : range;
        RayTraceResult entityHit = player.getWorld().rayTraceEntities(eye, direction, reach, 0.6, entity -> entity instanceof LivingEntity && !entity.equals((Object)player));
        Vector sideways = direction.clone().crossProduct(new Vector(0.0, 1.0, 0.0));
        if (sideways.lengthSquared() < 1.0E-4) {
            sideways = new Vector(1.0, 0.0, 0.0);
        }
        sideways.normalize();
        Vector upwards = direction.clone().crossProduct(sideways).normalize();
        Particle.DustOptions core = new Particle.DustOptions(Color.fromRGB((int)255, (int)235, (int)130), 3.0f);
        Particle.DustOptions corona = new Particle.DustOptions(Color.fromRGB((int)255, (int)170, (int)0), 1.6f);
        for (double distance = 0.0; distance <= reach; distance += 0.2) {
            Location at = eye.clone().add(direction.clone().multiply(distance));
            player.getWorld().spawnParticle(Particle.DUST, at, 6, 0.12, 0.12, 0.12, 0.0, (Object)core);
            player.getWorld().spawnParticle(Particle.END_ROD, at, 1, 0.05, 0.05, 0.05, 0.0);
            double spin = distance * 1.2;
            for (int step = 0; step < 4; ++step) {
                double angle = spin + (double)step * Math.PI / 2.0;
                Location ring = at.clone().add(sideways.clone().multiply(Math.cos(angle) * 0.55)).add(upwards.clone().multiply(Math.sin(angle) * 0.55));
                player.getWorld().spawnParticle(Particle.DUST, ring, 2, 0.05, 0.05, 0.05, 0.0, (Object)corona);
            }
        }
        player.getWorld().playSound(eye, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.6f, 1.2f);
        this.plugin.getAbilityManager().setCooldown(player, BEAM_COOLDOWN_ID, cooldown);
        if (entityHit != null && (entity2 = entityHit.getHitEntity()) instanceof LivingEntity) {
            LivingEntity target = (LivingEntity)entity2;
            target.damage(damage, (Entity)player);
            target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 6, 0.6, 0.6, 0.6, 0.0);
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0.0, 1.0, 0.0), 60, 0.5, 0.7, 0.5, 0.0, (Object)core);
        } else {
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-beam-missed"));
        }
    }

    private void breakCharge(Player player, String messageKey) {
        BukkitRunnable task = this.charging.remove(player.getUniqueId());
        if (task == null) {
            return;
        }
        task.cancel();
        player.sendMessage(this.plugin.getConfigManager().getMessage(messageKey));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.2f, 0.8f);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onChargerDamaged(EntityDamageEvent event) {
        Player player;
        Entity entity = event.getEntity();
        if (entity instanceof Player && this.charging.containsKey((player = (Player)entity).getUniqueId())) {
            this.breakCharge(player, "gold-beam-broken");
        }
    }

    @Override
    public void onQuaternary(Player player, int tier) {
        this.openSoulMenu(player);
    }

    public void openSoulMenu(Player player) {
        Map<String, GoldGemManager.Harvest> souls = this.plugin.getGoldGemManager().getHarvested(player.getUniqueId());
        if (souls.isEmpty()) {
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-no-soul"));
            return;
        }
        Inventory menu = Bukkit.createInventory(null, (int)27, (String)SOUL_MENU_TITLE);
        String selected = this.plugin.getGoldGemManager().getActive(player.getUniqueId());
        int slot = 0;
        for (Map.Entry<String, GoldGemManager.Harvest> soul : souls.entrySet()) {
            ItemStack icon = CustomItemManager.getItemById(soul.getKey() + "_gem_t" + soul.getValue().tier());
            if (icon == null) continue;
            ItemMeta meta = icon.getItemMeta();
            if (meta != null && soul.getKey().equals(selected)) {
                meta.setDisplayName(meta.getDisplayName() + " \u00a7a\u00a7l< CHANNELLED >");
                icon.setItemMeta(meta);
            }
            menu.setItem(slot++, icon);
        }
        // a fully awakened gem also offers its own two movesets (bottom row)
        GoldGemManager gold = this.plugin.getGoldGemManager();
        if (gold.isFullyAwakened(player.getUniqueId())) {
            int set = gold.getMoveset(player.getUniqueId());
            menu.setItem(MOVESET_SLOT, this.movesetIcon("\u00a76\u00a7lMoveset I", "\u00a7eSnap / Golden Drill \u00a78\u2022 \u00a7eGilded Wrath \u00a78\u2022 \u00a7eRayler / Gilded Viola", set == 1));
            menu.setItem(MOVESET_SLOT + 1, this.movesetIcon("\u00a76\u00a7lMoveset II", "\u00a7eProphet's Ladder / Gilded Blitz \u00a78\u2022 \u00a7eGilded Wrath \u00a78\u2022 \u00a7eMark of the Beast", set == 2));
        }
        player.openInventory(menu);
    }

    private ItemStack movesetIcon(String name, String kit, boolean selected) {
        ItemStack icon = CustomItemManager.getItemById(GoldGemManager.GOLD_ITEM_ID);
        if (icon == null) {
            icon = new ItemStack(Material.GOLD_INGOT);
        }
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name + (selected ? " \u00a7a\u00a7l< CHANNELLED >" : ""));
            meta.setLore(List.of(kit));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onSoulMenuClick(InventoryClickEvent event) {
        String gemId;
        Player player;
        block7: {
            block6: {
                if (!SOUL_MENU_TITLE.equals(event.getView().getTitle())) {
                    return;
                }
                event.setCancelled(true);
                HumanEntity humanEntity = event.getWhoClicked();
                if (!(humanEntity instanceof Player)) break block6;
                player = (Player)humanEntity;
                if (event.getCurrentItem() != null) break block7;
            }
            return;
        }
        int raw = event.getRawSlot();
        if (raw == MOVESET_SLOT || raw == MOVESET_SLOT + 1) {
            if (this.plugin.getGoldGemManager().setMoveset(player, raw == MOVESET_SLOT ? 1 : 2)) {
                player.sendMessage(raw == MOVESET_SLOT
                    ? "\u00a76\u00a7lMoveset I \u00a78- \u00a7eSnap / Golden Drill \u00a78\u2022 \u00a7eGilded Wrath \u00a78\u2022 \u00a7eRayler / Gilded Viola"
                    : "\u00a76\u00a7lMoveset II \u00a78- \u00a7eProphet's Ladder / Gilded Blitz \u00a78\u2022 \u00a7eGilded Wrath \u00a78\u2022 \u00a7eMark of the Beast");
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.4f);
            }
            player.closeInventory();
            return;
        }
        String itemId = CustomItemManager.getIdByItem(event.getCurrentItem());
        String string = gemId = itemId != null ? this.plugin.getGemRegistry().gemIdFromItemId(itemId) : null;
        if (gemId == null) {
            return;
        }
        if (this.plugin.getGoldGemManager().setActive(player, gemId)) {
            player.sendMessage(this.plugin.getConfigManager().getMessage("gold-soul-selected").replace("{gem}", this.plugin.getGemManager().getGemDisplayName(gemId)));
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.4f);
        }
        player.closeInventory();
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onHolderAttack(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        if (!this.plugin.getGoldGemManager().holdsGoldGem(player)) {
            return;
        }
        double chance = this.plugin.getConfig().getDouble("gold.damage-boost.chance", 0.15);
        if (Math.random() >= chance) {
            return;
        }
        double multiplier = this.plugin.getConfig().getDouble("gold.damage-boost.multiplier", 1.5);
        event.setDamage(event.getDamage() * multiplier);
        player.getWorld().spawnParticle(Particle.DUST, event.getEntity().getLocation().add(0.0, 1.0, 0.0), 10, 0.3, 0.3, 0.3, 0.0, (Object)new Particle.DustOptions(Color.fromRGB((int)255, (int)215, (int)0), 1.2f));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.6f);
    }

    @Override
    public void cleanup(Player player) {
        BukkitRunnable task = this.charging.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.cleanup(event.getPlayer());
        this.plugin.getGoldGemManager().unload(event.getPlayer().getUniqueId());
    }
}

