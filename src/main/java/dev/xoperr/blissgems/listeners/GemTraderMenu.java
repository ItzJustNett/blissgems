package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Left-clicking with a Gem Trader opens this picker. Every gem but your own starts in the pool;
 * click gems to take them out, then roll. Each gem taken out costs trader.pick-energy-per-gem
 * energy, so leaving a single gem in buys exactly that gem at the highest price.
 */
public final class GemTraderMenu implements Listener {
    private static final int MAX_GEMS = 53;
    /** Taken-out gems show their Tier 1 look with a red cross (resource pack models 6001-6008). */
    private static final int DISABLED_MODEL_DATA = 6001;
    private final BlissGems plugin;
    private final GemInteractListener trades;

    public GemTraderMenu(BlissGems plugin, GemInteractListener trades) {
        this.plugin = plugin;
        this.trades = trades;
    }

    private static final class Holder implements InventoryHolder {
        final List<String> options = new ArrayList<>();
        final Set<String> disabled = new HashSet<>();
        Inventory inv;
        int rollSlot;

        @Override
        public Inventory getInventory() {
            return this.inv;
        }
    }

    public void open(Player player) {
        if (!this.plugin.getConfig().getBoolean("trader.pick-menu", true)) return;
        if (!this.plugin.getGemManager().hasActiveGem(player)) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("no-gem");
            if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
            return;
        }
        Holder holder = new Holder();
        String current = this.plugin.getGemManager().getGemId(player);
        for (String id : this.plugin.getGemManager().getAvailableGemIds()) {
            if (!id.equals(current) && holder.options.size() < MAX_GEMS) holder.options.add(id);
        }
        if (holder.options.isEmpty()) {
            player.sendMessage("§cNo other gem types available!");
            return;
        }
        // Bliss: one row like Pockets, the gems then the trade button in the last slot
        int size = Math.min(54, (holder.options.size() / 9 + 1) * 9);
        holder.rollSlot = size - 1;
        holder.inv = Bukkit.createInventory(holder, size, "§2§lGem Trader §8- pick your pool");
        this.render(player, holder);
        player.openInventory(holder.inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.8f, 1.3f);
    }

    private int cost(Holder holder) {
        return holder.disabled.size() * this.plugin.getConfig().getInt("trader.pick-energy-per-gem", 1);
    }

    private void render(Player player, Holder holder) {
        holder.inv.clear();
        for (int i = 0; i < holder.options.size(); i++) {
            String id = holder.options.get(i);
            holder.inv.setItem(i, this.icon(id, !holder.disabled.contains(id)));
        }
        int cost = this.cost(holder);
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        int left = holder.options.size() - holder.disabled.size();
        ItemStack roll = new ItemStack(cost < energy ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
        ItemMeta meta = roll.getItemMeta();
        meta.setDisplayName("§a§lTrade");
        meta.setLore(List.of(
            "§7Rolls one of the §f" + left + "§7 gem(s) still in the pool.",
            "§7Cost: §b" + cost + " energy §8(you have " + energy + ")",
            "",
            "§8Click gems to take them out of the pool."));
        roll.setItemMeta(meta);
        holder.inv.setItem(holder.rollSlot, roll);
    }

    /** A look-alike of the gem with no item id, so nothing treats it as a real gem. */
    private ItemStack icon(String gemId, boolean inPool) {
        ItemStack gem = CustomItemManager.getItemById(gemId + "_gem_t1", 5);
        GemType type = GemType.fromOraxenId(gemId + "_gem_t1");
        boolean crossed = !inPool && type != null && gem != null;
        ItemStack icon = new ItemStack((inPool || crossed) && gem != null ? gem.getType() : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = icon.getItemMeta();
        ItemMeta source = gem != null ? gem.getItemMeta() : null;
        if (inPool && source != null && source.hasCustomModelData()) meta.setCustomModelData(source.getCustomModelData());
        if (crossed) meta.setCustomModelData(DISABLED_MODEL_DATA + type.ordinal());
        String name = this.plugin.getGemManager().getGemColorCode(gemId) + "§l" + this.plugin.getGemManager().getGemDisplayName(gemId);
        meta.setDisplayName(inPool ? name : "§8§m" + this.plugin.getGemManager().getGemDisplayName(gemId));
        meta.setLore(List.of(inPool ? "§aIn the pool §8- click to take out" : "§cTaken out §8- click to put back"));
        icon.setItemMeta(meta);
        return icon;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != holder.inv) return;
        int slot = event.getRawSlot();
        if (slot < holder.options.size()) {
            String id = holder.options.get(slot);
            if (!holder.disabled.remove(id)) {
                if (holder.options.size() - holder.disabled.size() <= 1) {
                    player.sendMessage("§cAt least one gem has to stay in the pool.");
                    return;
                }
                holder.disabled.add(id);
            }
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
            this.render(player, holder);
            return;
        }
        if (slot == holder.rollSlot) this.roll(player, holder);
    }

    private void roll(Player player, Holder holder) {
        ItemStack trader = player.getInventory().getItemInMainHand();
        if (!"gem_trader".equals(CustomItemManager.getIdByItem(trader))) {
            player.sendMessage("§cHold the Gem Trader in your main hand.");
            return;
        }
        int wait = this.trades.traderSecondsLeft(player);
        if (wait > 0) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("trade-cooldown", "seconds", wait);
            if (msg != null && !msg.isEmpty()) player.sendMessage(msg);
            return;
        }
        int cost = this.cost(holder);
        if (this.plugin.getEnergyManager().getEnergy(player) - cost < 1) {
            player.sendMessage("§cNot enough energy: this pool costs " + cost + " and you can't go below 1.");
            return;
        }
        List<String> pool = new ArrayList<>(holder.options);
        pool.removeAll(holder.disabled);
        String pick = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        player.closeInventory();
        if (this.trades.tradeGem(player, trader, pick) && cost > 0) {
            this.plugin.getEnergyManager().removeEnergy(player, cost);
            player.sendMessage("§7The trade cost you §b" + cost + " energy§7.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }
}
