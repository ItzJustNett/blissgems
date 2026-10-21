/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatMessageType
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scheduler.BukkitTask
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.pedestal.PedestalManager;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import dev.xoperr.blissgems.utils.ParticleUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class FluxEnergyManager {
    public static final int DEFAULT_MAX_WATTS = 2000000;
    public static final double DEFAULT_CHARGE_PER_TICK = 0.667;
    public static final double DEFAULT_BEAM_MAX_CHARGE = 200.0;
    private final BlissGems plugin;
    private final Map<UUID, Double> playerWatts = new ConcurrentHashMap<UUID, Double>();
    private final Map<UUID, Double> beamCharge = new ConcurrentHashMap<UUID, Double>();
    private final Map<UUID, Boolean> isCharging = new ConcurrentHashMap<UUID, Boolean>();
    private final Map<UUID, Deque<ItemStack>> queues = new HashMap<>();
    private final Map<UUID, BukkitTask> burners = new HashMap<>();
    private final Set<UUID> autoStart = new HashSet<>();
    private BukkitTask chargeTask;
    private final File dataFile;

    public FluxEnergyManager(BlissGems plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "flux_energy.yml");
        this.loadData();
        this.startChargingLoop();
    }

    public int getMaxWatts() {
        return this.plugin.getConfig().getInt("abilities.flux-charging.max-watts", 2000000);
    }

    public double getChargePerTick() {
        return this.plugin.getConfig().getDouble("abilities.flux-charging.charge-per-tick", 0.667);
    }

    public double getBeamMaxCharge() {
        return this.plugin.getConfig().getDouble("abilities.flux-charging.beam-max-charge", 200.0);
    }

    public double getWatts(UUID uuid) {
        return this.playerWatts.getOrDefault(uuid, 0.0);
    }

    public void setWatts(UUID uuid, double watts) {
        double clamped = Math.max(0.0, Math.min(watts, (double)this.getMaxWatts()));
        this.playerWatts.put(uuid, clamped);
    }

    public void addWatts(UUID uuid, double amount) {
        this.setWatts(uuid, this.getWatts(uuid) + amount);
    }

    public double getBeamCharge(UUID uuid) {
        return this.beamCharge.getOrDefault(uuid, 0.0);
    }

    public void setBeamCharge(UUID uuid, double charge) {
        double maxCharge = this.getBeamMaxCharge();
        double clamped = Math.max(0.0, Math.min(charge, maxCharge));
        this.beamCharge.put(uuid, clamped);
    }

    public boolean isCharging(Player player) {
        return this.isCharging.getOrDefault(player.getUniqueId(), false);
    }

    public void setCharging(Player player, boolean charging) {
        UUID uuid = player.getUniqueId();
        if (charging) {
            if (this.getWatts(uuid) <= 0.0) {
                player.sendMessage(PedestalManager.color("&cYou don't have enough energy to charge!"));
                this.isCharging.put(uuid, false);
                return;
            }
            this.isCharging.put(uuid, true);
            player.sendMessage(PedestalManager.color("&aCharging started!"));
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.8f);
        } else {
            this.isCharging.put(uuid, false);
            player.sendMessage(PedestalManager.color("&cCharging Stopped!"));
        }
    }

    public int getFuelWattValue(Material material) {
        if (material == null) {
            return 0;
        }
        String pathUnder = "abilities.flux-charging.fuel-values." + material.name();
        if (this.plugin.getConfig().contains(pathUnder)) {
            return this.plugin.getConfig().getInt(pathUnder);
        }
        String pathHyphen = "abilities.flux-charging.fuels." + material.name().toLowerCase().replace('_', '-');
        if (this.plugin.getConfig().contains(pathHyphen)) {
            return this.plugin.getConfig().getInt(pathHyphen);
        }
        return switch (material) {
            case COPPER_INGOT -> 1928;
            case COPPER_BLOCK -> 17352;
            case IRON_INGOT -> 106;
            case IRON_BLOCK -> 1067;
            case DIAMOND -> 5202;
            case DIAMOND_BLOCK -> 46819;
            case NETHERITE_INGOT -> 46819;
            case NETHERITE_BLOCK -> 421378;
            case WITHER_SKELETON_SKULL -> 112047;
            case NETHER_STAR -> 336141;
            default -> 0;
        };
    }

    /** Watts one item is worth; custom items (anything with model data or a BlissGems id) never burn. */
    private int wattsFor(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0;
        if (CustomItemManager.getIdByItem(item) != null) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasCustomModelData()) return 0;
        return this.getFuelWattValue(item.getType());
    }

    // ---- Watt Deposit: a plain chest; what you put in burns into watts one item a second ----

    public static final class DepositHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return this.inventory;
        }
    }

    public static boolean isDeposit(Inventory inv) {
        return inv != null && inv.getHolder() instanceof DepositHolder;
    }

    public void openChargingStation(Player player) {
        DepositHolder holder = new DepositHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, PedestalManager.color("&9Watt Deposit (" + String.format("%,.0f", this.getWatts(player.getUniqueId())) + " watts)"));
        holder.inventory = inv;
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 0.7f, 1.4f);
    }

    /** Gems can't go in; everything else may (what won't burn comes back on close). */
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (isGem(current) || isGem(cursor)) {
            event.setCancelled(true);
            return;
        }
        if (event.getClick() == ClickType.NUMBER_KEY && event.getRawSlot() < 27) {
            ItemStack hotbar = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            if (isGem(hotbar)) event.setCancelled(true);
        }
    }

    private static boolean isGem(ItemStack item) {
        String id = item == null ? null : CustomItemManager.getIdByItem(item);
        return id != null && GemType.isGem(id);
    }

    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !isDeposit(event.getInventory())) {
            return;
        }
        Inventory inv = event.getInventory();
        Deque<ItemStack> queue = this.queues.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>());
        int returned = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;
            inv.setItem(i, null);
            if (this.wattsFor(item) > 0) {
                queue.addLast(item);
            } else {
                this.giveBack(player, item);
                returned += item.getAmount();
            }
        }
        if (returned > 0) {
            player.sendMessage(PedestalManager.color("&7" + returned + " item" + (returned == 1 ? "" : "s") + " that won't burn came back to you."));
        }
        if (queue.isEmpty()) {
            this.queues.remove(player.getUniqueId());
        } else {
            this.startBurner(player.getUniqueId());
        }
        this.saveData();
        this.beginCharging(player);
    }

    private void beginCharging(Player player) {
        UUID id = player.getUniqueId();
        if (this.getWatts(id) <= 0.0) {
            this.autoStart.add(id);
            return;
        }
        this.autoStart.remove(id);
        if (!this.isCharging(player)) this.setCharging(player, true);
    }

    private void giveBack(Player player, ItemStack item) {
        for (ItemStack left : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }
    }

    private void startBurner(UUID id) {
        if (this.burners.containsKey(id)) return;
        this.burners.put(id, Bukkit.getScheduler().runTaskTimer(this.plugin, () -> this.burn(id), 20L, 20L));
    }

    private void stopBurner(UUID id) {
        BukkitTask t = this.burners.remove(id);
        if (t != null) t.cancel();
        this.autoStart.remove(id);
    }

    /** Burns the next deposited item into watts. */
    private void burn(UUID id) {
        Player player = Bukkit.getPlayer(id);
        Deque<ItemStack> queue = this.queues.get(id);
        if (queue == null || queue.isEmpty() || player == null) {
            if (queue != null && queue.isEmpty()) this.queues.remove(id);
            this.stopBurner(id);
            return;
        }
        if (player.isDead()) return;
        double watts = this.getWatts(id);
        int max = this.getMaxWatts();
        if (watts >= max) {
            this.stopBurner(id);
            this.saveData();
            player.sendMessage(PedestalManager.color("🔮 &bThe gem is full — the rest of your deposit is held until it has room."));
            return;
        }
        ItemStack next = queue.peekFirst();
        int value = this.wattsFor(next);
        if (value <= 0) {
            queue.pollFirst();
            this.giveBack(player, next);
            this.saveData();
            return;
        }
        if (next.getAmount() <= 1) queue.pollFirst(); else next.setAmount(next.getAmount() - 1);
        double now = Math.min(max, watts + value);
        this.setWatts(id, now);
        player.sendMessage(PedestalManager.color("&f🔮 You now have " + String.format("%,.0f", now) + " watts, up to " + String.format("%.1f", now / max * 100.0) + "% charge."));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.6f);
        if (this.autoStart.contains(id)) this.beginCharging(player);
        if (queue.isEmpty()) {
            this.queues.remove(id);
            this.stopBurner(id);
        }
        this.saveData();
    }

    /** Re-arms a saved deposit queue when its owner comes back. */
    public void onJoin(Player player) {
        Deque<ItemStack> queue = this.queues.get(player.getUniqueId());
        if (queue != null && !queue.isEmpty()) this.startBurner(player.getUniqueId());
    }

    private void startChargingLoop() {
        this.chargeTask = new BukkitRunnable(){
            private int tickCount = 0;

            public void run() {
                ++this.tickCount;
                for (UUID uuid : FluxEnergyManager.this.isCharging.keySet()) {
                    if (!FluxEnergyManager.this.isCharging.getOrDefault(uuid, false).booleanValue()) continue;
                    Player player = Bukkit.getPlayer((UUID)uuid);
                    if (player == null || !player.isOnline() || player.isDead()) {
                        FluxEnergyManager.this.isCharging.put(uuid, false);
                        continue;
                    }
                    if (!FluxEnergyManager.this.isHoldingFluxGem(player)) {
                        FluxEnergyManager.this.isCharging.put(uuid, false);
                        player.sendMessage(PedestalManager.color("&cCharging paused!"));
                        continue;
                    }
                    double watts = FluxEnergyManager.this.getWatts(uuid);
                    double beam = FluxEnergyManager.this.getBeamCharge(uuid);
                    int maxWatts = FluxEnergyManager.this.getMaxWatts();
                    double chargeRate = FluxEnergyManager.this.getChargePerTick();
                    double maxBeam = FluxEnergyManager.this.getBeamMaxCharge();
                    if (watts <= 0.0) {
                        FluxEnergyManager.this.isCharging.put(uuid, false);
                        player.sendMessage(PedestalManager.color("&f🔮 You ran out of watts, your gem is charged at " + String.format("%.1f", beam) + "%"));
                        continue;
                    }
                    if (beam >= maxBeam) {
                        FluxEnergyManager.this.isCharging.put(uuid, false);
                        player.sendMessage(PedestalManager.color("&f🔮 &eYour gem is fully overcharged at " + (int)beam + "%"));
                        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.4f);
                        continue;
                    }
                    if (beam >= 100.0) {
                        if (maxBeam <= 100.0) {
                            FluxEnergyManager.this.setBeamCharge(uuid, 100.0);
                            FluxEnergyManager.this.isCharging.put(uuid, false);
                            player.sendMessage(PedestalManager.color("&f🔮 Your gem is fully charged at 100%"));
                            continue;
                        }
                        if (this.tickCount % 20 == 0) {
                            if (beam > 181.0) {
                                player.damage(10.0);
                            } else if (beam > 161.0) {
                                player.damage(8.0);
                            } else if (beam > 141.0) {
                                player.damage(6.0);
                            } else if (beam > 121.0) {
                                player.damage(4.0);
                            } else if (beam > 101.0) {
                                player.damage(2.0);
                            }
                        }
                        if (beam < 100.0 + chargeRate) {
                            player.sendMessage(PedestalManager.color("&f🔮 &6Overcharging! &7(past 100% — it hurts until 200%)"));
                            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 2.0f);
                        }
                    }
                    double batteryPercent = watts / (double)maxWatts * 100.0;
                    double transferPercent = Math.min(chargeRate, batteryPercent);
                    transferPercent = Math.min(transferPercent, maxBeam - beam);
                    double wattsCost = transferPercent / 100.0 * (double)maxWatts;
                    FluxEnergyManager.this.setWatts(uuid, Math.max(0.0, watts - wattsCost));
                    FluxEnergyManager.this.setBeamCharge(uuid, beam + transferPercent);
                    player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0.0, 1.0, 0.0), 10, 0.4, 0.4, 0.4, 0.0, (Object)new Particle.DustOptions(ParticleUtils.FLUX_CYAN, 1.0f));
                    player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0.0, 0.8, 0.0), 4, 0.2, 0.2, 0.2, 0.02);
                    if (this.tickCount % 5 == 0) FluxEnergyManager.this.showChargeActionBar(player, beam + transferPercent, watts - wattsCost, maxWatts);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 1L, 1L);
    }

    /** Blood-style HUD: stored watts as a percentage, then the beam charge. */
    private void showChargeActionBar(Player player, double beam, double watts, int maxWatts) {
        String bar = "<##009ac9>🔺 &b" + String.format("%.2f", watts / maxWatts * 100.0)
            + "  <##5ED7FF>🔮 &b" + String.format("%.2f", beam) + "%";
        player.sendActionBar(PedestalManager.color(bar));
    }

    public boolean isHoldingFluxGem(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        if (this.isFluxGem(main) || this.isFluxGem(off)) {
            return true;
        }
        if (this.plugin.getGemManager() != null && this.plugin.getGemManager().hasGemType(player, GemType.FLUX)) {
            return true;
        }
        return false;
    }

    public boolean isFluxGem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        String id = CustomItemManager.getIdByItem(item);
        return "flux_gem_t1".equals(id) || "flux_gem_t2".equals(id);
    }

    public void cleanupPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        this.isCharging.remove(uuid);
        BukkitTask t = this.burners.remove(uuid);
        if (t != null) t.cancel();
        this.autoStart.remove(uuid);
        this.saveData();
    }

    public void loadData() {
        if (!this.dataFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration((File)this.dataFile);
        if (config.contains("watts")) {
            for (String key : config.getConfigurationSection("watts").getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    this.playerWatts.put(id, config.getDouble("watts." + key, 0.0));
                }
                catch (Exception ignored) {}
            }
        }
        if (config.isConfigurationSection("deposit")) {
            for (String key : config.getConfigurationSection("deposit").getKeys(false)) {
                try {
                    Deque<ItemStack> q = new ArrayDeque<>();
                    for (Object o : config.getList("deposit." + key, List.of())) if (o instanceof ItemStack item) q.addLast(item);
                    if (!q.isEmpty()) this.queues.put(UUID.fromString(key), q);
                }
                catch (Exception ignored) {}
            }
        }
        if (config.contains("beam_charge")) {
            for (String key : config.getConfigurationSection("beam_charge").getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    this.beamCharge.put(id, config.getDouble("beam_charge." + key, 0.0));
                }
                catch (Exception exception) {}
            }
        }
    }

    public void saveData() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Double> entry : this.playerWatts.entrySet()) {
            config.set("watts." + entry.getKey().toString(), (Object)entry.getValue());
        }
        for (Map.Entry<UUID, Double> entry : this.beamCharge.entrySet()) {
            config.set("beam_charge." + entry.getKey().toString(), (Object)entry.getValue());
        }
        for (Map.Entry<UUID, Deque<ItemStack>> entry : this.queues.entrySet()) {
            if (!entry.getValue().isEmpty()) config.set("deposit." + entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        try {
            config.save(this.dataFile);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save flux_energy.yml: " + e.getMessage());
        }
    }

    public void stop() {
        if (this.chargeTask != null) {
            this.chargeTask.cancel();
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isDeposit(p.getOpenInventory().getTopInventory())) p.closeInventory();
        }
        for (BukkitTask t : this.burners.values()) t.cancel();
        this.burners.clear();
        this.saveData();
    }
}

