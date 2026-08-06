package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.EnergyState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Restoration Book — the only way back from a Broken gem. Consuming one starts a
 * storm ritual that reforges the gem at random and returns it at Pristine.
 */
public class RestorationBookListener implements Listener {
    private static final String ITEM_ID = "restoration_book";

    private final BlissGems plugin;
    /** Players mid-ritual, so a second book cannot be burned on top of the first. */
    private final Set<UUID> activeRituals = new HashSet<>();

    public RestorationBookListener(BlissGems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRestorationBookUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return; // Fires for both hands; only act once
        }

        ItemStack item = event.getItem();
        if (item == null || !ITEM_ID.equals(CustomItemManager.getIdByItem(item))) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (activeRituals.contains(player.getUniqueId())) {
            player.sendMessage("§c§oYour Restoration Ritual is already under way!");
            return;
        }

        // Only a Broken gem can be restored this way
        if (this.plugin.getEnergyManager().getEnergyState(player) != EnergyState.BROKEN) {
            player.sendMessage("§c§oThe Restoration Book only works while your gem is §c§lBROKEN§c§o.");
            return;
        }

        List<String> available = this.plugin.getGemManager().getAvailableGemIds();
        if (available.isEmpty()) {
            player.sendMessage("§c§oNo gems are available to restore to!");
            return;
        }
        String newGem = available.get((int) (Math.random() * available.size()));
        int tier = Math.max(1, this.plugin.getGemManager().getGemTier(player));

        // Consume the book
        item.setAmount(item.getAmount() - 1);

        activeRituals.add(player.getUniqueId());
        clearExistingGems(player);
        broadcastRitualStart(player);

        long grantDelay = this.plugin.getGemRitualManager().performRestorationRitual(player, newGem, tier);

        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            activeRituals.remove(player.getUniqueId());
            if (!player.isOnline()) {
                return;
            }

            // Restored gems come back at Pristine
            this.plugin.getEnergyManager().setEnergy(player, EnergyState.PRISTINE.getMinEnergy());

            if (this.plugin.getGemManager().giveGemToOffhand(player, newGem, tier)) {
                String gemName = this.plugin.getGemManager().getGemDisplayName(newGem);
                String gemColor = this.plugin.getGemManager().getGemColorCode(newGem);
                player.sendMessage("§5§l» §fYour gem has been reforged: " + gemColor + "§l" + gemName + " §f(Tier " + tier + ")§5§l «");
                broadcastRitualComplete(player, gemColor + gemName);
            } else {
                player.sendMessage("§c§oThe ritual failed to reforge your gem, contact staff.");
            }
        }, grantDelay);
    }

    /** Strip the old broken gem so the reforged one is the only gem in play. */
    private void clearExistingGems(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot == null) continue;
            if (this.plugin.getGemManager().isAnyGem(CustomItemManager.getIdByItem(slot))) {
                player.getInventory().setItem(i, null);
            }
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (this.plugin.getGemManager().isAnyGem(CustomItemManager.getIdByItem(offhand))) {
            player.getInventory().setItemInOffHand(null);
        }
    }

    private void broadcastRitualStart(Player player) {
        if (!this.plugin.getConfig().getBoolean("restoration.broadcast", true)) {
            return;
        }
        this.plugin.getServer().broadcastMessage("§5§l✦ §d" + player.getName()
            + " §7has begun a §5§lRestoration Ritual§7! The sky darkens...");
    }

    private void broadcastRitualComplete(Player player, String gemDisplay) {
        if (!this.plugin.getConfig().getBoolean("restoration.broadcast", true)) {
            return;
        }
        this.plugin.getServer().broadcastMessage("§5§l✦ §d" + player.getName()
            + "§7's gem was reforged as " + gemDisplay + " §7at §bPristine§7.");
    }
}
