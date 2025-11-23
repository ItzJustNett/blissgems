/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.th0rgal.oraxen.api.OraxenItems
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.ItemStack
 */
package fun.obriy.blissgems.listeners;

import fun.obriy.blissgems.BlissGems;
import fun.obriy.blissgems.utils.GemType;
import io.th0rgal.oraxen.api.OraxenItems;
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
        String oraxenId = OraxenItems.getIdByItem((ItemStack)item);
        if (oraxenId == null || !oraxenId.endsWith("_gem_upgrader")) {
            return;
        }
        event.setCancelled(true);
        GemType upgraderType = null;
        for (GemType type : GemType.values()) {
            if (!oraxenId.equals(type.getId() + "_gem_upgrader")) continue;
            upgraderType = type;
            break;
        }
        if (upgraderType == null) {
            return;
        }
        if (!this.plugin.getGemManager().hasActiveGem(player)) {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("no-gem", new Object[0]));
            return;
        }
        GemType currentGemType = this.plugin.getGemManager().getGemType(player);
        int currentTier = this.plugin.getGemManager().getGemTier(player);
        if (currentGemType != upgraderType) {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("upgrade-wrong-type", new Object[0]));
            return;
        }
        if (currentTier != 1) {
            player.sendMessage(this.plugin.getConfigManager().getFormattedMessage("upgrade-already-tier2", new Object[0]));
            return;
        }
        if (this.plugin.getGemManager().upgradeGem(player, upgraderType)) {
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

