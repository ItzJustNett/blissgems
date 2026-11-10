package dev.xoperr.blissgems.games;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.Attributes;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * The Dragon Games shop ({@code /bliss shop}; the Energy Games have none, admins can always open it).
 * Points, prices under games.shop.dragon: 2 hearts, an enchanted golden apple, a restock shulker,
 * a totem, 2 Gem Traders, 64 cobwebs / golden apples / experience bottles, a Player Tracker, and
 * Strength I (blaze powder), Speed I (sugar) and Fire Resistance (magma cream) - right-click to take
 * the effect. Reviving someone is {@code /bliss games revive <player>} for that player's points value.
 */
public final class GamesShop implements Listener {
    private final BlissGems plugin;
    private final EventGames games;
    private final NamespacedKey effectKey;

    public GamesShop(BlissGems plugin, EventGames games) {
        this.plugin = plugin;
        this.games = games;
        this.effectKey = new NamespacedKey(plugin, "games-effect-item");
    }

    private static final class Holder implements InventoryHolder {
        Inventory inv;

        @Override
        public Inventory getInventory() {
            return this.inv;
        }
    }

    private record Offer(String key, int slot, ItemStack icon, String name, String what) {
    }

    private List<Offer> dragonOffers() {
        return List.of(
            new Offer("totem", 11, new ItemStack(Material.TOTEM_OF_UNDYING), "§e§lTotem of Undying", "One totem."),
            new Offer("strength", 12, new ItemStack(Material.BLAZE_POWDER), "§c§lStrength I", "Right-click for Strength I."),
            new Offer("webs", 13, new ItemStack(Material.COBWEB), "§f§l64 Cobwebs", "A stack of cobwebs."),
            new Offer("gaps", 14, new ItemStack(Material.GOLDEN_APPLE), "§6§l64 Golden Apples", "A stack of golden apples."),
            new Offer("speed", 15, new ItemStack(Material.SUGAR), "§b§lSpeed I", "Right-click for Speed I."),
            new Offer("hearts", 20, new ItemStack(Material.NETHER_STAR), "§c§l2 Hearts", "+2 max hearts, for good."),
            new Offer("traders", 22, CustomItemManager.getItemById("gem_trader"), "§2§l2 Gem Traders", "Two Gem Traders."),
            new Offer("fire-resistance", 23, new ItemStack(Material.MAGMA_CREAM), "§6§lFire Resistance", "Right-click for Fire Resistance."),
            new Offer("exp", 24, new ItemStack(Material.EXPERIENCE_BOTTLE), "§a§l64 Bottles o' Enchanting", "A stack of experience bottles."),
            new Offer("egap", 29, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE), "§d§lEnchanted Golden Apple", "One enchanted golden apple."),
            new Offer("tracker", 32, new ItemStack(Material.COMPASS), "§a§lPlayer Tracker", "Points at the nearest player for an hour."),
            new Offer("restock-shulker", 38, new ItemStack(Material.PURPLE_SHULKER_BOX), "§5§lRestock Shulker", "Gapples, strength, speed, fire res, webs and xp."));
    }

    private int dragonPrice(String key) {
        return this.plugin.getConfig().getInt("games.shop.dragon." + key, switch (key) {
            case "hearts", "egap", "restock-shulker", "totem" -> 75;
            case "traders", "webs" -> 30;
            case "gaps", "exp", "tracker" -> 15;
            default -> 5;
        });
    }

    /** What a Dragon shop offer actually hands over. */
    private List<ItemStack> deliver(Player p, String key) {
        return switch (key) {
            case "totem" -> List.of(new ItemStack(Material.TOTEM_OF_UNDYING));
            case "strength", "speed", "fire-resistance" -> List.of(this.effectItem(key, this.plugin.getConfig().getInt("games.shop.effect-items-per-buy", 1)));
            case "webs" -> List.of(new ItemStack(Material.COBWEB, 64));
            case "gaps" -> List.of(new ItemStack(Material.GOLDEN_APPLE, 64));
            case "exp" -> List.of(new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
            case "egap" -> List.of(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));
            case "tracker" -> List.of(this.games.tracker());
            case "traders" -> {
                ItemStack trader = CustomItemManager.getItemById("gem_trader");
                yield trader == null ? List.of() : List.of(trader, trader.clone());
            }
            case "restock-shulker" -> List.of(this.restockShulker());
            case "hearts" -> {
                this.addHearts(p);
                yield List.of();
            }
            default -> List.of();
        };
    }

    /** Bliss's effect items: blaze powder = Strength I, sugar = Speed I, magma cream = Fire Resistance. */
    private ItemStack effectItem(String key, int amount) {
        ItemStack item = new ItemStack(switch (key) {
            case "strength" -> Material.BLAZE_POWDER;
            case "speed" -> Material.SUGAR;
            default -> Material.MAGMA_CREAM;
        }, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(switch (key) {
            case "strength" -> "§c§lStrength I";
            case "speed" -> "§b§lSpeed I";
            default -> "§6§lFire Resistance";
        });
        meta.setLore(List.of("§7Right-click to use."));
        meta.getPersistentDataContainer().set(this.effectKey, PersistentDataType.STRING, key);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onEffectItem(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick() || event.getItem() == null || !event.getItem().hasItemMeta()) return;
        String key = event.getItem().getItemMeta().getPersistentDataContainer().get(this.effectKey, PersistentDataType.STRING);
        if (key == null) return;
        event.setCancelled(true);
        PotionEffectType type = switch (key) {
            case "strength" -> PotionEffectType.STRENGTH;
            case "speed" -> PotionEffectType.SPEED;
            default -> PotionEffectType.FIRE_RESISTANCE;
        };
        Player p = event.getPlayer();
        p.addPotionEffect(new PotionEffect(type, this.plugin.getConfig().getInt("games.shop.effect-seconds", 180) * 20, 0));
        event.getItem().setAmount(event.getItem().getAmount() - 1);
        p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.2f);
    }

    /** The restock shulker as in Bliss: a row each of gapples, 2x strength, 2x speed, 2x fire res, webs, xp. */
    private ItemStack restockShulker() {
        ItemStack box = new ItemStack(Material.PURPLE_SHULKER_BOX);
        BlockStateMeta meta = (BlockStateMeta) box.getItemMeta();
        ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
        for (int row = 0; row < 3; row++) {
            ItemStack[] line = {new ItemStack(Material.GOLDEN_APPLE, 64), this.effectItem("strength", 64), this.effectItem("strength", 64),
                this.effectItem("speed", 64), this.effectItem("speed", 64), this.effectItem("fire-resistance", 64), this.effectItem("fire-resistance", 64),
                new ItemStack(Material.COBWEB, 64), new ItemStack(Material.EXPERIENCE_BOTTLE, 64)};
            for (int col = 0; col < 9; col++) shulker.getInventory().setItem(row * 9 + col, line[col]);
        }
        meta.setBlockState(shulker);
        meta.setDisplayName("§5§lRestock Shulker");
        box.setItemMeta(meta);
        return box;
    }

    /** +2 max hearts that stack with every purchase. */
    private void addHearts(Player p) {
        AttributeInstance hp = p.getAttribute(Attributes.maxHealth());
        if (hp == null) return;
        NamespacedKey key = new NamespacedKey(this.plugin, "games-shop-hearts");
        double extra = 0.0;
        for (AttributeModifier m : new java.util.ArrayList<>(hp.getModifiers())) {
            if (key.equals(m.getKey())) {
                extra = m.getAmount();
                hp.removeModifier(m);
            }
        }
        hp.addModifier(new AttributeModifier(key, extra + 4.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
    }

    public void open(Player player) {
        boolean admin = player.isOp() || player.hasPermission("blissgems.admin");
        if (this.games.mode() != EventGames.Mode.DRAGON && !admin) {
            player.sendMessage(this.games.mode() == EventGames.Mode.ENERGY ? "§cThe Energy Games have no shop." : "§cThe shop only opens while the Dragon Games run.");
            return;
        }
        if (!this.games.isPlayer(player) && !admin) {
            player.sendMessage("§cThe shop is only open to players in the games.");
            return;
        }
        this.openDragon(player);
    }

    private void openDragon(Player player) {
        Holder holder = new Holder();
        holder.inv = Bukkit.createInventory(holder, 54, "§6§lShop §8- " + this.games.points(player) + " points");
        for (Offer offer : this.dragonOffers()) {
            if (offer.icon() == null) continue;
            holder.inv.setItem(offer.slot(), this.button(offer.icon().clone(), offer.name(), offer.what(), this.dragonPrice(offer.key())));
        }
        player.openInventory(holder.inv);
    }

    private ItemStack button(ItemStack item, String name, String what, int price) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of("§7" + what, "§7Cost: §f" + price + " points"));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player p) || event.getClickedInventory() != holder.inv) return;
        this.buyDragon(p, event.getRawSlot(), this.games.isOut(p));
    }

    private void buyDragon(Player p, int slot, boolean out) {
        for (Offer offer : this.dragonOffers()) {
            if (offer.slot() != slot) continue;
            if (out) {
                p.sendMessage("§cEliminated players can't buy from the shop.");
                return;
            }
            int price = this.dragonPrice(offer.key());
            if (!this.games.spendPoints(p, price)) {
                p.sendMessage("§cNot enough points (" + price + ").");
                return;
            }
            for (ItemStack item : this.deliver(p, offer.key())) {
                for (ItemStack left : p.getInventory().addItem(item).values()) p.getWorld().dropItemNaturally(p.getLocation(), left);
            }
            p.sendMessage("§aBought " + offer.name() + " §afor " + price + " points.");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
            this.openDragon(p);
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }
}
