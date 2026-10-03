package dev.xoperr.blissgems.pedestal;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/** Protects the pedestal and turns items dropped at it into rituals and energy deposits. */
public final class PedestalListener implements Listener {
    private static final Set<Material> BANNED_NEAR = EnumSet.of(Material.PISTON, Material.STICKY_PISTON, Material.TNT,
        Material.RAIL, Material.POWERED_RAIL, Material.DETECTOR_RAIL, Material.ACTIVATOR_RAIL);
    private static final String REPAIR_KIT = "repair_kit";
    private static final String RESTORATION = "restoration_book";
    private static final String ENERGY = "energy_bottle";

    private final BlissGems plugin;
    private final PedestalManager manager;
    private final PedestalState state;

    public PedestalListener(BlissGems plugin, PedestalManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.state = manager.state();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Location loc = event.getBlock().getLocation();
        Location main = this.state.mainLoc();
        if (main == null) {
            return;
        }
        Material type = event.getBlock().getType();
        if (PedestalState.near(loc, main, 10.0) && BANNED_NEAR.contains(type)) {
            event.setCancelled(true);
            return;
        }
        boolean onCenter = PedestalState.near(loc, main, 1.8);
        boolean structure = this.state.isPedestalBlock(loc) || this.state.isPedestalBlock(loc.clone().add(0, -1, 0));
        if (structure && !(onCenter && type == Material.BEACON)) {
            event.setCancelled(true);
            return;
        }
        if (!onCenter) {
            return;
        }
        if (type != Material.BEACON || this.state.beacon() || !this.state.active()) {
            event.setCancelled(true);
            return;
        }
        event.getPlayer().sendMessage(PedestalManager.color("&f You Have Placed &c The Beacon"));
        this.state.setBeacon(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        boolean beacon = PedestalState.near(loc, this.state.mainLoc(), 0.8);
        boolean debris = PedestalState.near(loc, this.state.ancientDebrisLoc(), 0.8);
        if (!beacon && !debris) {
            if (this.state.nearStructure(loc)) {
                event.setCancelled(true);
                event.getPlayer().damage(0.001);
            }
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || this.state.ritual() == PedestalState.Ritual.NONE) {
            return;
        }
        if (debris) {
            this.terminateRitual("&4Ancient debris tampered with — ritual terminated.");
            return;
        }
        this.state.decrementDurability();
        int durability = this.state.durability();
        if (durability <= 0) {
            this.terminateRitual("&4Pedestal beacon broken " + PedestalState.MAX_DURABILITY + " times — ritual terminated.");
        } else {
            this.manager.broadcastNear(this.state.mainLoc(), 8.0, "&cPedestal break attempt, durability is " + durability + "/" + PedestalState.MAX_DURABILITY);
        }
    }

    private void terminateRitual(String message) {
        this.manager.broadcastNear(this.state.mainLoc(), 8.0, message);
        PedestalManager.invalidatePulses();
        this.manager.revive().shutdownRestore();
        this.state.resetRitual();
        this.state.setActive(false);
        PedestalReviveRitual.restoreWeather();
        this.state.removeBeam();
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            this.state.unbuildStructure();
            this.state.save();
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Location main = this.state.mainLoc();
        if (main == null || !PedestalState.near(player.getLocation(), main, 16.0)) {
            return;
        }
        Item drop = event.getItemDrop();
        String id = CustomItemManager.getIdByItem(drop.getItemStack());
        if (id == null) {
            return;
        }
        switch (id) {
            case REPAIR_KIT, RESTORATION -> this.trackRitualThrow(player, drop, id);
            case ENERGY -> this.trackEnergyThrow(player, drop);
            default -> { }
        }
    }

    /**
     * A Repair Kit or Restoration Book starts its ritual when it lands on the pedestal (or is
     * dropped while standing on it). Anything else gets told why nothing happened.
     */
    private void trackRitualThrow(Player player, Item item, String id) {
        Location main = this.state.mainLoc();
        Location target = main.clone().add(0.5, 0.5, 0.5);
        if (PedestalState.near(player.getLocation(), main, 3.0)) {
            this.startFromItem(player, item, id);
            return;
        }
        new BukkitRunnable() {
            int ticks;

            @Override
            public void run() {
                if (item.isDead() || !item.isValid()) {
                    this.cancel();
                    return;
                }
                Location at = item.getLocation();
                if (at.getWorld().equals(target.getWorld()) && at.distanceSquared(target) <= 6.25) {
                    this.cancel();
                    PedestalListener.this.startFromItem(player, item, id);
                    return;
                }
                if (++this.ticks >= 100) {
                    this.cancel();
                    player.sendMessage(PedestalManager.color("&7Throw it &fonto the pedestal &7(or drop it while standing on it) to start the ritual."));
                }
            }
        }.runTaskTimer(this.plugin, 1L, 1L);
    }

    private void startFromItem(Player player, Item item, String id) {
        if (REPAIR_KIT.equals(id)) {
            this.startRepair(player, item);
        } else {
            this.startRestoration(player, item);
        }
    }

    private void trackEnergyThrow(Player player, Item item) {
        Location main = this.state.mainLoc();
        if (main == null) {
            return;
        }
        Location target = main.clone().add(0.5, 0.5, 0.5);
        new BukkitRunnable() {
            int ticks;

            @Override
            public void run() {
                if (item.isDead() || !item.isValid() || PedestalListener.this.state.mainLoc() == null) {
                    this.cancel();
                    return;
                }
                Location at = item.getLocation();
                if (at.getWorld().equals(target.getWorld()) && at.distanceSquared(target) <= 1.69) {
                    this.cancel();
                    PedestalListener.this.depositEnergy(player, item);
                    return;
                }
                if (++this.ticks >= 100) {
                    this.cancel();
                }
            }
        }.runTaskTimer(this.plugin, 1L, 1L);
    }

    private boolean ready(Player player) {
        if (!this.state.active()) {
            player.sendMessage(PedestalManager.color("&cPedestal is not active."));
            return false;
        }
        if (this.state.ritual() != PedestalState.Ritual.NONE) {
            player.sendMessage(PedestalManager.color("&cAnother ritual is already in progress."));
            return false;
        }
        return true;
    }

    private void startRepair(Player player, Item item) {
        if (!this.ready(player)) {
            return;
        }
        this.state.setRitual(PedestalState.Ritual.REPAIR);
        this.state.resetCount();
        this.state.setDepositCooldown(0);
        this.state.setDurability(PedestalState.MAX_DURABILITY);
        Location main = this.state.mainLoc();
        PedestalStartEffect.play(this.plugin, item, main, org.bukkit.Color.fromRGB(120, 255, 170), false);
        takeOne(item);
        player.sendTitle(PedestalManager.color("&a&lRepair Ritual"), PedestalManager.color("&7Throw &f" + PedestalState.DEPOSITS_REQUIRED + " Energy Bottles &7onto the pedestal"), 10, 70, 20);
        this.plugin.getServer().broadcastMessage(PedestalManager.color("&d" + player.getName() + " has started a repair ritual!"));
    }

    private void startRestoration(Player player, Item item) {
        if (!this.ready(player)) {
            return;
        }
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        if (energy != 0) {
            player.sendMessage(PedestalManager.color("&cRestoration only works while your gem is &lBROKEN&c. You have " + energy + " energy."));
            return;
        }
        this.state.setRitual(PedestalState.Ritual.REVIVE);
        this.state.setRevivingPlayer(player.getUniqueId());
        this.state.resetCount();
        this.state.setDurability(PedestalState.MAX_DURABILITY);
        Location main = this.state.mainLoc();
        PedestalStartEffect.play(this.plugin, item, main, org.bukkit.Color.fromRGB(170, 70, 255), true);
        takeOne(item);
        PedestalReviveRitual.forceStormAndThunder(main.getWorld());
        player.sendTitle(PedestalManager.color("&5&lRestoration Ritual"), PedestalManager.color("&7Throw &f" + PedestalState.DEPOSITS_REQUIRED + " Energy Bottles &7onto the pedestal"), 10, 70, 20);
        this.plugin.getServer().broadcastMessage(PedestalManager.color("&d" + player.getName() + " has started a restoration ritual!"));
    }

    private void depositEnergy(Player player, Item item) {
        Location main = this.state.mainLoc();
        if (this.state.ritual() == PedestalState.Ritual.NONE) {
            player.sendMessage(PedestalManager.color("&cNo ritual active. Drop a Repair Kit (or a Restoration Book while Broken) to start one."));
            return;
        }
        if (this.state.count() >= PedestalState.DEPOSITS_REQUIRED) {
            this.manager.broadcastNear(main, 8.0, "<##FFD773>Energy deposit requirement has been fulfilled!");
            return;
        }
        if (this.state.depositCooldown() != 0) {
            player.sendMessage(PedestalManager.color("&cPedestal cooldown: " + this.state.depositCooldown() + "s"));
            return;
        }
        main.getWorld().playSound(main, Sound.BLOCK_BEACON_AMBIENT, 40.0f, 1.6f);
        takeOne(item);
        main.getWorld().playSound(main, Sound.ITEM_AXE_SCRAPE, 10.0f, 0.5f);
        main.getWorld().playSound(main, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 20.0f, 1.2f);
        this.state.incrementCount();
        if (this.state.ritual() == PedestalState.Ritual.REPAIR) {
            this.manager.progress(player, Achievement.GOOD_AS_NEW, 1);
        }
        this.state.setDepositCooldown(player.isOp() ? 2 : this.plugin.getConfig().getInt("pedestal.deposit-cooldown-seconds", 30));
        if (this.state.count() >= PedestalState.DEPOSITS_REQUIRED) {
            this.manager.broadcastNear(main, 8.0, "<##FFD773>Energy deposit requirement has been fulfilled!");
            this.state.summonBeam();
            if (this.state.ritual() == PedestalState.Ritual.REPAIR) {
                this.manager.completeRepair();
            } else if (this.state.ritual() == PedestalState.Ritual.REVIVE) {
                this.manager.broadcastNear(main, 8.0, "<##96FFD9>Please stand on the pedestal center!");
            }
        } else {
            this.manager.broadcastNear(main, 8.0, "<##96FFD9>" + this.state.count() + "/" + PedestalState.DEPOSITS_REQUIRED + " energy deposited!");
            this.manager.broadcastNear(main, 8.0, "&cPlease wait for pedestal cooldown...");
        }
    }

    private static void takeOne(Item item) {
        ItemStack stack = item.getItemStack();
        if (stack.getAmount() > 1) {
            stack.setAmount(stack.getAmount() - 1);
            item.setItemStack(stack);
        } else {
            item.remove();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(b -> this.state.isPedestalBlock(b.getLocation()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(b -> this.state.isPedestalBlock(b.getLocation()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (this.state.isPedestalBlock(b.getLocation()) || this.state.isPedestalBlock(b.getRelative(event.getDirection()).getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (this.state.isPedestalBlock(b.getLocation()) || this.state.isPedestalBlock(b.getRelative(event.getDirection()).getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (this.state.isPedestalBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
}
