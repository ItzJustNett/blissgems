/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.ItemStack
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Achievement;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class UpgraderListener
implements Listener {
    private final BlissGems plugin;
    private final Map<UUID, Long> lastUpgrade = new HashMap<UUID, Long>();
    private static final long UPGRADE_DEBOUNCE_MS = 400L;

    public UpgraderListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND && event.getHand() != EquipmentSlot.OFF_HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        String oraxenId = CustomItemManager.getIdByItem(item);
        if (oraxenId == null || !oraxenId.equals("gem_upgrader")) {
            return;
        }
        event.setCancelled(true);
        long now = System.currentTimeMillis();
        Long last = this.lastUpgrade.get(player.getUniqueId());
        if (last != null && now - last < 400L) {
            return;
        }
        this.lastUpgrade.put(player.getUniqueId(), now);
        if (!this.plugin.getGemManager().hasActiveGem(player)) {
            this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "no-gem", new Object[0]);
            return;
        }
        String currentGemId = this.plugin.getGemManager().getGemId(player);
        int currentTier = this.plugin.getGemManager().getGemTier(player);
        if (currentTier != 1) {
            this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "upgrade-already-tier2", new Object[0]);
            return;
        }
        int charges = this.plugin.getConfig().getInt("upgrader.charges", 3);
        ItemMeta meta = item.getItemMeta();
        int currentCharges = charges;
        if (meta != null && meta.hasLore() && meta.getLore() != null) {
            for (String line : meta.getLore()) {
                String stripped = org.bukkit.ChatColor.stripColor(line);
                if (stripped.startsWith("Charges: ")) {
                    try { currentCharges = Integer.parseInt(stripped.substring(9).trim()); } catch (Exception ignored) {}
                    break;
                }
            }
        }
        if (currentCharges <= 0) {
            player.sendMessage("§cThis upgrader has no charges left!");
            return;
        }
        if (this.plugin.getGemManager().upgradeGem(player, currentGemId)) {
            boolean upgraderInOffHand;
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            boolean upgraderInMainHand = mainHand != null && "gem_upgrader".equals(CustomItemManager.getIdByItem(mainHand));
            boolean bl = upgraderInOffHand = offHand != null && "gem_upgrader".equals(CustomItemManager.getIdByItem(offHand));
            currentCharges--;
            if (currentCharges <= 0) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else if (upgraderInMainHand) {
                    player.getInventory().setItemInMainHand(null);
                } else if (upgraderInOffHand) {
                    player.getInventory().setItemInOffHand(null);
                }
                player.sendMessage("§eUpgrader used all charges and was consumed.");
            } else {
                if (meta != null) {
                    java.util.List<String> lore = meta.getLore() != null ? new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<>();
                    boolean found = false;
                    for (int i = 0; i < lore.size(); i++) {
                        String stripped = org.bukkit.ChatColor.stripColor(lore.get(i));
                        if (stripped.startsWith("Charges: ")) {
                            lore.set(i, "§7Charges: §e" + currentCharges + "§7/§e" + charges);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        lore.add("§7Charges: §e" + currentCharges + "§7/§e" + charges);
                    }
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                    ItemStack updated = item.clone();
                    updated.setAmount(1);
                    if (updated.getItemMeta() != null) {
                        ItemMeta um = updated.getItemMeta();
                        java.util.List<String> ul = um.getLore() != null ? new java.util.ArrayList<>(um.getLore()) : new java.util.ArrayList<>();
                        boolean uf = false;
                        for (int i = 0; i < ul.size(); i++) {
                            if (org.bukkit.ChatColor.stripColor(ul.get(i)).startsWith("Charges: ")) {
                                ul.set(i, "§7Charges: §e" + currentCharges + "§7/§e" + charges);
                                uf = true;
                                break;
                            }
                        }
                        if (!uf) ul.add("§7Charges: §e" + currentCharges + "§7/§e" + charges);
                        um.setLore(ul);
                        updated.setItemMeta(um);
                    }
                    if (upgraderInMainHand) {
                        player.getInventory().setItemInMainHand(updated);
                    } else if (upgraderInOffHand) {
                        player.getInventory().setItemInOffHand(updated);
                    }
                }
            }
            if (this.plugin.getConfigManager().shouldPlayUpgradeEffects()) {
                try {
                    String soundName = this.plugin.getConfigManager().getUpgradeSound();
                    Sound sound = Sound.valueOf((String)soundName);
                    player.playSound(player.getLocation(), sound, 1.0f, 1.5f);
                    String particleName = this.plugin.getConfigManager().getUpgradeParticle();
                    Particle particle = Particle.valueOf((String)particleName);
                    int count = this.plugin.getConfigManager().getUpgradeParticleCount();
                    player.spawnParticle(particle, player.getLocation().add(0.0, 1.0, 0.0), count, 0.5, 0.5, 0.5);
                }
                catch (IllegalArgumentException e) {
                    this.plugin.getLogger().warning("Invalid particle or sound in config: " + e.getMessage());
                }
            }
            this.plugin.getConfigManager().sendFormattedMessage((CommandSender)player, "upgrade-success", new Object[0]);
            if (this.plugin.getAchievementManager() != null) {
                this.plugin.getAchievementManager().unlock(player, Achievement.THE_NEXT_LEVEL);
            }
        } else {
            player.sendMessage("\u00a7cFailed to upgrade gem!");
        }
    }
}

