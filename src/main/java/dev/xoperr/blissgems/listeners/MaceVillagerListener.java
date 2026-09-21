package dev.xoperr.blissgems.listeners;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.managers.MaceVillagerManager;
import dev.xoperr.blissgems.utils.CustomItemManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class MaceVillagerListener implements Listener {
    private final BlissGems plugin;
    private final MaceVillagerManager villagerManager;
    private final Map<UUID, Long> maceCooldowns = new HashMap<>();

    public MaceVillagerListener(BlissGems plugin, MaceVillagerManager villagerManager) {
        this.plugin = plugin;
        this.villagerManager = villagerManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Entity target = event.getRightClicked();
        if (!(target instanceof Villager)) return;
        Villager villager = (Villager) target;
        if (!villagerManager.isCustomVillager(villager)) return;

        Player player = event.getPlayer();
        if (player.isSneaking()) {
            event.setCancelled(true);
            ItemStack soul = villagerManager.convertToSoul(villager);
            if (soul != null) {
                Location loc = villager.getLocation();
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);
                loc.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.2f);
                villager.remove();

                if (player.getInventory().firstEmpty() == -1) {
                    loc.getWorld().dropItemNaturally(loc, soul);
                } else {
                    player.getInventory().addItem(soul);
                }
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&d&l✦ [SOUL FORM] &7Villager successfully bound into &dSoul Form&7! Place down anywhere to restore."));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        String id = CustomItemManager.getIdByItem(item);
        if (!"villager_soul".equals(id) && (item.getItemMeta() == null ||
                !item.getItemMeta().getPersistentDataContainer().has(CustomItemManager.getVillagerSoulKey(), org.bukkit.persistence.PersistentDataType.STRING))) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        event.setCancelled(true);
        Location spawnLoc = clicked.getLocation().add(0.5, 1.0, 0.5);
        Villager restored = villagerManager.restoreFromSoul(spawnLoc, item);
        if (restored != null) {
            item.setAmount(item.getAmount() - 1);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a&l✦ [SOUL FORM] &7Villager released from &dSoul Form&7! All trade history preserved."));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMerchantTrade(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getInventory() instanceof MerchantInventory)) return;

        MerchantInventory merchantInv = (MerchantInventory) event.getInventory();
        Merchant merchant = merchantInv.getMerchant();
        if (!(merchant instanceof Villager)) return;

        Villager villager = (Villager) merchant;
        if (!villagerManager.isCustomVillager(villager)) return;

        // Slot 2 in MerchantInventory is the result slot
        if (event.getSlotType() == InventoryType.SlotType.RESULT || event.getRawSlot() == 2) {
            Player player = (Player) event.getWhoClicked();
            int selectedIndex = merchantInv.getSelectedRecipeIndex();

            if (villagerManager.hasPlayerTraded(villager, selectedIndex, player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&c&l[MERCHANT] &cYou have already purchased this item! Limit is 1 trade per item per player."));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            // If click is taking the item, record the trade!
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                villagerManager.recordPlayerTrade(villager, selectedIndex, player.getUniqueId());
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDoubleDurabilityDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        if (CustomItemManager.isDoubleDurability(item)) {
            // 50% chance to absorb damage, effectively doubling total durability
            if (ThreadLocalRandom.current().nextBoolean()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMaceAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player attacker = (Player) event.getDamager();
        ItemStack held = attacker.getInventory().getItemInMainHand();

        if (CustomItemManager.isLimitedMace(held)) {
            UUID uuid = attacker.getUniqueId();
            long now = System.currentTimeMillis();
            long cdExpires = maceCooldowns.getOrDefault(uuid, 0L);
            if (cdExpires > now) {
                long remainingSeconds = (cdExpires - now) / 1000L + 1;
                event.setDamage(1.0); // Reduce to basic unarmed hit
                attacker.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.RED + "Tempered Mace Cooldown: " + ChatColor.YELLOW + remainingSeconds + "s remaining!"));
                attacker.playSound(attacker.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.8f, 0.8f);
                return;
            }
            maceCooldowns.put(uuid, now + 60_000L);
            attacker.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.GOLD + "⚡ Tempered Mace Strike unleashed! " + ChatColor.GRAY + "(60s cooldown)"));
            attacker.getWorld().playSound(attacker.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1.5f);
        }
    }
}
