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
            // Always act on the exact upgrader that was used (event.getItem() is that stack), never
            // on "whatever is in the hand now": swapping hotbar slots mid-use used to skip the
            // consume and leave an infinite upgrader.
            currentCharges--;
            if (currentCharges <= 0) {
                item.setAmount(item.getAmount() - 1);
                player.sendMessage("\u00a7eUpgrader used all charges and was consumed.");
            } else {
                ItemStack used = item.clone();
                used.setAmount(1);
                ItemMeta um = used.getItemMeta();
                if (um != null) {
                    java.util.List<String> lore = um.getLore() != null ? new java.util.ArrayList<>(um.getLore()) : new java.util.ArrayList<>();
                    boolean found = false;
                    for (int i = 0; i < lore.size(); i++) {
                        if (org.bukkit.ChatColor.stripColor(lore.get(i)).startsWith("Charges: ")) {
                            lore.set(i, "\u00a77Charges: \u00a7e" + currentCharges + "\u00a77/\u00a7e" + charges);
                            found = true;
                            break;
                        }
                    }
                    if (!found) lore.add("\u00a77Charges: \u00a7e" + currentCharges + "\u00a77/\u00a7e" + charges);
                    um.setLore(lore);
                    used.setItemMeta(um);
                }
                if (item.getAmount() <= 1) {
                    // single upgrader: update it in place
                    item.setItemMeta(used.getItemMeta());
                } else {
                    // a stack: take one off it and hand back that one with its new charge count
                    item.setAmount(item.getAmount() - 1);
                    for (ItemStack left : player.getInventory().addItem(used).values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), left);
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

