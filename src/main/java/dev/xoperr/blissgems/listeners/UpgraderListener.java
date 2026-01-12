/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.ItemStack
 */
package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.GemType;
import dev.xoperr.blissgems.utils.CustomItemManager;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class UpgraderListener
implements Listener {
    private final BlissGems plugin;

    public UpgraderListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        String oraxenId = CustomItemManager.getIdByItem((ItemStack)item);
        // Check if it's a universal gem upgrader (not type-specific anymore)
        if (oraxenId == null || !oraxenId.equals("gem_upgrader")) {
            return;
        }
        event.setCancelled(true);

        // Check if player has a gem
        if (!this.plugin.getGemManager().hasActiveGem(player)) {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-gem", new Object[0]));
            return;
        }

        // Get current gem info
        GemType currentGemType = this.plugin.getGemManager().getGemType(player);
        int currentTier = this.plugin.getGemManager().getGemTier(player);

        // Check if already tier 2
        if (currentTier != 1) {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("upgrade-already-tier2", new Object[0]));
            return;
        }

        // Upgrade the gem (universal upgrader works for any gem type)
        if (this.plugin.getGemManager().upgradeGem(player, currentGemType)) {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
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
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("upgrade-success", new Object[0]));
        } else {
            player.sendMessage("\u00a7cFailed to upgrade gem!");
        }
    }
}

