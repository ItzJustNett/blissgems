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
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import dev.xoperr.blissgems.utils.ParticleUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class FluxEnergyManager {
    public static final String GUI_TITLE = "\u00a7b\ud83d\udd2e Flux Charging Station";
    public static final int DEFAULT_MAX_WATTS = 2000000;
    public static final double DEFAULT_CHARGE_PER_TICK = 0.667;
    public static final double DEFAULT_BEAM_MAX_CHARGE = 200.0;
    private static final int[] FUEL_SLOTS = new int[]{10, 11, 12, 14, 15, 16};
    private final BlissGems plugin;
    private final Map<UUID, Double> playerWatts = new ConcurrentHashMap<UUID, Double>();
    private final Map<UUID, Double> beamCharge = new ConcurrentHashMap<UUID, Double>();
    private final Map<UUID, Boolean> isCharging = new ConcurrentHashMap<UUID, Boolean>();
    private final Map<UUID, Inventory> openStations = new ConcurrentHashMap<UUID, Inventory>();
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
                player.sendMessage(String.valueOf(ChatColor.RED) + "You don't have enough energy to charge!");
                this.isCharging.put(uuid, false);
                return;
            }
            this.isCharging.put(uuid, true);
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Charging started!");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.8f);
        } else {
            this.isCharging.put(uuid, false);
            player.sendMessage(String.valueOf(ChatColor.RED) + "Charging paused!");
        }
        Inventory inv = this.openStations.get(uuid);
        if (inv != null) {
            this.updateStatusItem(player, inv);
            this.updateLeverItem(player, inv);
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
            default -> 0;
        };
    }

    public void openChargingStation(Player player) {
        Inventory inv = Bukkit.createInventory(null, (int)27, (String)GUI_TITLE);
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = book.getItemMeta();
        if (bookMeta != null) {
            bookMeta.setDisplayName("\u00a7b\u00a7lHow to Charge");
            ArrayList<String> lore = new ArrayList<>();
            lore.add("\u00a77Place charging items in the slots");
            lore.add("\u00a77to add energy to your Flux Gem");
            lore.add("");
            lore.add("\u00a7eCharging Values:");
            lore.add("\u00a7fCopper Ingot: \u00a7a" + String.format("%,d", this.getFuelWattValue(Material.COPPER_INGOT)) + " watts");
            lore.add("\u00a7fCopper Block: \u00a7a" + String.format("%,d", this.getFuelWattValue(Material.COPPER_BLOCK)) + " watts");
            lore.add("\u00a7fIron Ingot: \u00a7a" + String.format("%,d", this.getFuelWattValue(Material.IRON_INGOT)) + " watts");
            lore.add("\u00a7fIron Block: \u00a7a" + String.format("%,d", this.getFuelWattValue(Material.IRON_BLOCK)) + " watts");
            lore.add("\u00a7fDiamond: \u00a7a" + String.format("%,d", this.getFuelWattValue(Material.DIAMOND)) + " watts");
            lore.add("\u00a7fDiamond Block: \u00a7a" + String.format("%,d", this.getFuelWattValue(Material.DIAMOND_BLOCK)) + " watts");
            lore.add("\u00a7fNetherite Ingot: \u00a7a" + String.format("%,d", this.getFuelWattValue(Material.NETHERITE_INGOT)) + " watts");
            lore.add("\u00a7fNetherite Block: \u00a7a" + String.format("%,d", this.getFuelWattValue(Material.NETHERITE_BLOCK)) + " watts");
            lore.add("\u00a7fWither Skull: \u00a7a" + String.format("%,d", this.getFuelWattValue(Material.WITHER_SKELETON_SKULL)) + " watts");
            lore.add("");
            lore.add("\u00a7cMax Capacity: \u00a76" + String.format("%,d", this.getMaxWatts()) + " watts");
            bookMeta.setLore(lore);
            book.setItemMeta(bookMeta);
        }
        inv.setItem(26, book);
        this.updateStatusItem(player, inv);
        this.updateLeverItem(player, inv);
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < 27; ++i) {
            if (this.isFuelSlot(i) || i == 4 || i == 26 || i == 22) continue;
            inv.setItem(i, glass);
        }
        this.openStations.put(player.getUniqueId(), inv);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.5f);
    }

    private boolean isFuelSlot(int slot) {
        for (int s : FUEL_SLOTS) {
            if (s != slot) continue;
            return true;
        }
        return false;
    }

    public void updateStatusItem(Player player, Inventory inv) {
        UUID uuid = player.getUniqueId();
        double currentWatts = this.getWatts(uuid);
        int maxWatts = this.getMaxWatts();
        double batteryPercent = currentWatts / (double)maxWatts * 100.0;
        double currentBeam = this.getBeamCharge(uuid);
        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = star.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("\u00a76\u00a7l\u26a1 Current Status");
            ArrayList<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("\u00a7fTotal Watts: \u00a7b" + String.format("%,.0f", currentWatts) + " \u00a77/ \u00a7b" + String.format("%,d", maxWatts));
            lore.add("\u00a7fBattery Charge: \u00a7a" + String.format("%.2f%%", batteryPercent));
            lore.add("\u00a7fBeam Ready: \u00a7e" + String.format("%.2f%%", currentBeam));
            lore.add("");
            lore.add(this.isCharging(player) ? "\u00a7aCurrently Charging" : "\u00a7cNot Charging");
            meta.setLore(lore);
            star.setItemMeta(meta);
        }
        inv.setItem(4, star);
    }

    public void updateLeverItem(Player player, Inventory inv) {
        boolean active = this.isCharging(player);
        ItemStack lever = new ItemStack(Material.LEVER);
        ItemMeta meta = lever.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(active ? "\u00a7c\u00a7lStop Charging" : "\u00a7a\u00a7lStart Charging");
            lever.setItemMeta(meta);
        }
        inv.setItem(22, lever);
    }

    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }
        int rawSlot = event.getRawSlot();
        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        if (currentItem != null && CustomItemManager.getIdByItem(currentItem) != null && GemType.isGem(CustomItemManager.getIdByItem(currentItem)) || cursorItem != null && CustomItemManager.getIdByItem(cursorItem) != null && GemType.isGem(CustomItemManager.getIdByItem(cursorItem))) {
            event.setCancelled(true);
            return;
        }
        if (rawSlot >= 27 && event.isShiftClick() && (currentItem == null || this.getFuelWattValue(currentItem.getType()) <= 0)) {
            event.setCancelled(true);
            return;
        }
        if (rawSlot >= 0 && rawSlot < 27) {
            if (rawSlot == 22) {
                event.setCancelled(true);
                this.setCharging(player, !this.isCharging(player));
                return;
            }
            if (rawSlot == 4 || rawSlot == 13 || !this.isFuelSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }
            if (cursorItem != null && !cursorItem.getType().isAir() && this.getFuelWattValue(cursorItem.getType()) <= 0) {
                event.setCancelled(true);
                player.sendMessage("\u00a7cThis item is not a conductive fuel!");
                return;
            }
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (player.isOnline() && player.getOpenInventory().getTitle().equals(GUI_TITLE)) {
                this.consumeFuels(player, player.getOpenInventory().getTopInventory());
            }
        }, 1L);
    }

    private void consumeFuels(Player player, Inventory inv) {
        UUID uuid = player.getUniqueId();
        double currentWatts = this.getWatts(uuid);
        int maxWatts = this.getMaxWatts();
        boolean changed = false;
        for (int slot : FUEL_SLOTS) {
            int fuelVal;
            ItemStack stack = inv.getItem(slot);
            if (stack == null || stack.getType().isAir() || (fuelVal = this.getFuelWattValue(stack.getType())) <= 0) continue;
            int amount = stack.getAmount();
            double totalProvided = (double)fuelVal * (double)amount;
            if (currentWatts + totalProvided > (double)maxWatts) {
                double needed = (double)maxWatts - currentWatts;
                if (needed <= 0.0) continue;
                int itemsNeeded = (int)Math.ceil(needed / (double)fuelVal);
                itemsNeeded = Math.min(itemsNeeded, amount);
                double gained = (double)fuelVal * (double)itemsNeeded;
                currentWatts = Math.min((double)maxWatts, currentWatts + gained);
                int remainingAmount = amount - itemsNeeded;
                if (remainingAmount > 0) {
                    stack.setAmount(remainingAmount);
                    inv.setItem(slot, stack);
                } else {
                    inv.setItem(slot, null);
                }
                changed = true;
                continue;
            }
            currentWatts += totalProvided;
            inv.setItem(slot, null);
            changed = true;
        }
        if (changed) {
            this.setWatts(uuid, currentWatts);
            double percent = currentWatts / (double)maxWatts * 100.0;
            player.sendMessage("\u00a7b\ud83d\udd2e \u00a7bYou now have \u00a7f" + String.format("%,.0f", currentWatts) + " \u00a7bwatts, up to \u00a7a" + String.format("%.2f%%", percent) + " \u00a7bcharge.");
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
            this.updateStatusItem(player, inv);
        }
    }

    public void onInventoryClose(InventoryCloseEvent event) {
        HumanEntity humanEntity = event.getPlayer();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }
        Inventory inv = event.getInventory();
        for (int slot : FUEL_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType().isAir()) continue;
            inv.setItem(slot, null);
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack[]{item});
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
        this.openStations.remove(player.getUniqueId());
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
                    boolean holdsFlux = FluxEnergyManager.this.isHoldingFluxGem(player);
                    if (!holdsFlux) {
                        FluxEnergyManager.this.setCharging(player, false);
                        player.sendMessage("\u00a7cCharging paused! (Must hold Flux Gem)");
                        continue;
                    }
                    double watts = FluxEnergyManager.this.getWatts(uuid);
                    double beam = FluxEnergyManager.this.getBeamCharge(uuid);
                    int maxWatts = FluxEnergyManager.this.getMaxWatts();
                    double chargeRate = FluxEnergyManager.this.getChargePerTick();
                    double maxBeam = FluxEnergyManager.this.getBeamMaxCharge();
                    if (watts <= 0.0) {
                        FluxEnergyManager.this.setCharging(player, false);
                        player.sendMessage("\u00a7b\ud83d\udd2e \u00a7bYou ran out of watts, your gem is charged at \u00a7e" + String.format("%.2f%%", beam));
                        continue;
                    }
                    if (beam >= maxBeam) {
                        FluxEnergyManager.this.setCharging(player, false);
                        player.sendMessage("\u00a7b\u26a1\u00a7l Maximum beam charge reached! \u00a7e(" + (int)beam + "%)");
                        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.4f);
                        continue;
                    }
                    if (beam >= 100.0) {
                        if (maxBeam <= 100.0) {
                            FluxEnergyManager.this.setBeamCharge(uuid, 100.0);
                            FluxEnergyManager.this.setCharging(player, false);
                            player.sendMessage("\u00a7b\u26a1\u00a7l Fully charged! \u00a7eRight-click to fire!");
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
                        if (beam >= 100.0 && beam < 100.0 + chargeRate) {
                            player.sendMessage("\u00a7c\u00a7l\u26a1 WARNING: DO NOT RELEASE BEAM UNTIL 200% (Inflicts progressive self-damage)!");
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
                    FluxEnergyManager.this.showChargeActionBar(player, beam + transferPercent, watts - wattsCost, maxWatts);
                    Inventory openInv = FluxEnergyManager.this.openStations.get(uuid);
                    if (openInv == null) continue;
                    FluxEnergyManager.this.updateStatusItem(player, openInv);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 1L, 1L);
    }

    private void showChargeActionBar(Player player, double beam, double watts, int maxWatts) {
        int bars = (int)Math.min(20.0, Math.max(0.0, beam / 100.0 * 20.0));
        StringBuilder bar = new StringBuilder("\u00a7b\u26a1 Flux Beam: \u00a7e");
        for (int i = 0; i < 20; ++i) {
            if (i < bars) {
                bar.append("\u2588");
                continue;
            }
            bar.append("\u00a78\u2588");
        }
        bar.append(" \u00a7b").append(String.format("%.1f%%", beam));
        bar.append(" \u00a77| \u00a7f").append(String.format("%,.0f", watts)).append("W");
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, (BaseComponent)new TextComponent(bar.toString()));
    }

    public boolean isHoldingFluxGem(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return this.isFluxGem(main) || this.isFluxGem(off);
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
        Inventory inv = this.openStations.remove(uuid);
        if (inv != null) {
            for (int slot : FUEL_SLOTS) {
                ItemStack item = inv.getItem(slot);
                if (item == null || item.getType().isAir()) continue;
                inv.setItem(slot, null);
                player.getInventory().addItem(new ItemStack[]{item});
            }
        }
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
        this.saveData();
    }
}

