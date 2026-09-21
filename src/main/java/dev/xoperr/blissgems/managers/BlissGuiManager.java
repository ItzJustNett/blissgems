/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.EnergyState;
import dev.xoperr.blissgems.utils.GemType;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BlissGuiManager
implements Listener {
    private final BlissGems plugin;
    private static final String GUI_TITLE = "\u00a75\u00a7lBlissGems Menu";

    public BlissGuiManager(BlissGems plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, (int)27, (String)GUI_TITLE);
        gui.setItem(11, this.createGemInfoItem(player));
        gui.setItem(13, this.createEnergyInfoItem(player));
        gui.setItem(15, this.createSettingsItem(player));
        ItemStack border = this.createBorderItem();
        for (int i = 0; i < 27; ++i) {
            if (gui.getItem(i) != null) continue;
            gui.setItem(i, border);
        }
        player.openInventory(gui);
    }

    private ItemStack createGemInfoItem(Player player) {
        ItemStack item;
        GemType gemType = this.plugin.getGemManager().getGemType(player);
        int tier = this.plugin.getGemManager().getGemTier(player);
        ArrayList<String> lore = new ArrayList<>();
        if (gemType != null) {
            String[] descLines;
            item = new ItemStack(Material.ECHO_SHARD);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("\u00a7d\u00a7lYour Gem");
            lore.add("\u00a77Type: \u00a7f" + gemType.getDisplayName());
            lore.add("\u00a77Tier: \u00a7f" + tier);
            lore.add("");
            lore.add("\u00a77Description:");
            for (String line : descLines = gemType.getDescription().split("\n")) {
                lore.add("\u00a78" + line);
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        } else {
            item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("\u00a7c\u00a7lNo Gem");
            lore.add("\u00a77You don't have a gem equipped!");
            lore.add("\u00a77Get one from an admin or find one.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createEnergyInfoItem(Player player) {
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        EnergyState state = this.plugin.getEnergyManager().getEnergyState(player);
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7b\u00a7lEnergy Status");
        ArrayList<String> lore = new ArrayList<>();
        lore.add("\u00a77Energy: \u00a7f" + energy + "\u00a78/\u00a7f10");
        lore.add("\u00a77State: \u00a7f" + state.getDisplayName());
        lore.add("");
        StringBuilder energyBar = new StringBuilder("\u00a78[");
        for (int i = 0; i < 10; ++i) {
            if (i < energy) {
                energyBar.append("\u00a7a\u25a0");
                continue;
            }
            energyBar.append("\u00a77\u25a0");
        }
        energyBar.append("\u00a78]");
        lore.add(energyBar.toString());
        lore.add("");
        if (energy == 0) {
            lore.add("\u00a7c\u2718 Abilities disabled");
            lore.add("\u00a7c\u2718 Passives disabled");
        } else if (energy == 1) {
            lore.add("\u00a7a\u2714 Abilities enabled");
            lore.add("\u00a7c\u2718 Passives disabled");
        } else {
            lore.add("\u00a7a\u2714 Abilities enabled");
            lore.add("\u00a7a\u2714 Passives enabled");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSettingsItem(Player player) {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a7e\u00a7lSettings");
        ArrayList<String> lore = new ArrayList<>();
        lore.add("\u00a77Current Settings:");
        lore.add("");
        boolean clickEnabled = this.plugin.getClickActivationManager().isClickActivationEnabled(player);
        lore.add("\u00a77Click Activation: " + (clickEnabled ? "\u00a7aEnabled" : "\u00a7cDisabled"));
        lore.add("\u00a78Use /bliss toggle_click to change");
        lore.add("");
        int trustedCount = this.plugin.getTrustedPlayersManager().getTrustedPlayers(player).size();
        lore.add("\u00a77Trusted Players: \u00a7f" + trustedCount);
        lore.add("\u00a78Use /bliss trust <player>");
        lore.add("");
        if (player.hasPermission("blissgems.admin")) {
            boolean banEnabled = this.plugin.getConfigManager().isBanOnZeroEnergyEnabled();
            lore.add("\u00a7c\u00a7lAdmin Setting:");
            lore.add("\u00a77Ban on 0 Energy: " + (banEnabled ? "\u00a7aEnabled" : "\u00a7cDisabled"));
            lore.add("\u00a78Use /bliss bannable <true/false>");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(GUI_TITLE)) {
            event.setCancelled(true);
        }
    }
}

