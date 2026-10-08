package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.goldevent.RitualItems;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * /bliss - the gem catalogue, a normal 3-row chest with the BLISS banner (resource pack font
 * bliss:menu) above it:
 * <pre>
 *  T1 T1 T1 T1 [GOLD] T2 T2 T2 T2
 *  T1 T1 T1 T1 [glas] T2 T2 T2 T2
 *  .  .  .  [AURATUS][MORE][HERETIC] .  .  .
 * </pre>
 * Every gem is drawn at its top Pristine look. The Gold Gem opens the Gold Gem page (the dormant gem,
 * Wire Fragments 1-7 and the Fragment Core). Auratus and Heretic come from the BlissMythics addon;
 * without it, clicking them links its download. The glowing diamond block between them links the
 * BlissGems Expansion (more gems). Everything here is a display copy: nothing can be
 * taken out, and the copies carry no item id, so they are never usable items.
 */
public final class GemMenu implements Listener {
    public static final String MYTHICS_URL = "https://modrinth.com/plugin/auratus-hertic-addon";
    public static final String EXPANSION_URL = "https://modrinth.com/plugin/blissgems-expansion";
    /** Banner glyph, after a -7 px space so it lines up with the chest's left edge. */
    private static final Component BANNER = Component.text("\ue201\ue200")
        .font(Key.key("bliss", "menu")).color(NamedTextColor.WHITE);
    private static final String[] GEMS = {"astra", "fire", "flux", "life", "puff", "speed", "strength", "wealth"};
    /** The full-colour look; lower energy draws the paler, cracked textures. */
    private static final int SHOW_ENERGY = 10;
    private static final int[] LEFT = {0, 1, 2, 3, 9, 10, 11, 12};
    private static final int[] RIGHT = {5, 6, 7, 8, 14, 15, 16, 17};
    private static final int GOLD = 4;
    private static final int[] DIVIDER = {13};
    private static final int MORE_GEMS = 22;
    private static final int AURATUS = 21;
    private static final int HERETIC = 23;
    private static final int BACK = 18;
    private final BlissGems plugin;

    public GemMenu(BlissGems plugin) {
        this.plugin = plugin;
    }

    private enum Page { GEMS, GOLD }

    private static final class Holder implements InventoryHolder {
        final Page page;
        Inventory inv;

        Holder(Page page) {
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return this.inv;
        }
    }

    public void open(Player player) {
        Holder holder = new Holder(Page.GEMS);
        holder.inv = Bukkit.createInventory(holder, 27, BANNER);
        for (int i = 0; i < GEMS.length; i++) {
            if (!this.plugin.getConfig().getBoolean("gems.enabled." + GEMS[i], true)) continue;
            holder.inv.setItem(LEFT[i], showcase(CustomItemManager.getItemById(GEMS[i] + "_gem_t1", SHOW_ENERGY)));
            holder.inv.setItem(RIGHT[i], showcase(CustomItemManager.getItemById(GEMS[i] + "_gem_t2", SHOW_ENERGY)));
        }
        ItemStack gold = showcase(CustomItemManager.getItemById("gold_gem_t1"));
        if (gold != null) {
            ItemMeta meta = gold.getItemMeta();
            List<Component> lore = meta.lore() == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(meta.lore());
            lore.add(Component.empty());
            lore.add(mm("<!italic><gradient:#F7971E:#FFD200>Click to see the Gold Gem and its fragments</gradient>"));
            meta.lore(lore);
            gold.setItemMeta(meta);
        }
        holder.inv.setItem(GOLD, gold);
        for (int slot : DIVIDER) holder.inv.setItem(slot, divider());
        boolean mythics = this.mythicsInstalled();
        holder.inv.setItem(AURATUS, mythic(5003, "<gradient:#FFD86F:#FC6262><bold>Auratus Gem</bold></gradient>", mythics));
        holder.inv.setItem(HERETIC, mythic(5001, "<gradient:#B06AB3:#4568DC><bold>Heretic Gem</bold></gradient>", mythics));
        holder.inv.setItem(MORE_GEMS, moreGems());
        player.openInventory(holder.inv);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.2f);
    }

    /** The Gold Gem page: the dormant gem on top, the seven Wire Fragments, the Fragment Core. */
    public void openGold(Player player) {
        Holder holder = new Holder(Page.GOLD);
        holder.inv = Bukkit.createInventory(holder, 27, BANNER);
        holder.inv.setItem(4, showcase(CustomItemManager.getItemById("gold_gem_t1")));
        for (int n = 1; n <= 7; n++) holder.inv.setItem(9 + n, showcase(RitualItems.wireFragment(n)));
        holder.inv.setItem(22, showcase(RitualItems.fragmentCore()));
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.displayName(mm("<!italic><gradient:#C77DFF:#4CC9F0>Back</gradient>"));
        back.setItemMeta(meta);
        holder.inv.setItem(BACK, back);
        player.openInventory(holder.inv);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.6f, 1.4f);
    }

    private boolean mythicsInstalled() {
        return Bukkit.getPluginManager().isPluginEnabled("BlissMythics");
    }

    /** A display copy: same look and text as the real item, but without the plugin's ids. */
    private static ItemStack showcase(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().getKeys().forEach(k -> meta.getPersistentDataContainer().remove(k));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack divider() {
        ItemStack pane = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setHideTooltip(true);
        pane.setItemMeta(meta);
        return pane;
    }

    /** Auratus / Heretic: the gem's texture (from the pack), and where to get them. */
    private static ItemStack mythic(int model, String name, boolean installed) {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(model);
        meta.displayName(mm("<!italic>" + name));
        meta.lore(installed
            ? List.of(mm("<!italic><gray>A mythic gem from the BlissMythics addon.</gray>"))
            : List.of(mm("<!italic><gray>A mythic gem from the <white>BlissMythics</white> addon.</gray>"),
                      mm("<!italic><red>Not installed on this server.</red>"),
                      Component.empty(),
                      mm("<!italic><gradient:#4CC9F0:#C77DFF>Click for the download link</gradient>")));
        item.setItemMeta(meta);
        return item;
    }

    /** Hard to miss: a glowing diamond block pointing at the BlissGems Expansion. */
    private static ItemStack moreGems() {
        ItemStack item = new ItemStack(Material.DIAMOND_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        meta.displayName(mm("<!italic><gradient:#4CC9F0:#C77DFF:#FF85C0><bold>Want more gems?</bold></gradient>"));
        meta.lore(List.of(
            mm("<!italic><gray>New gems, powers and items come with</gray>"),
            mm("<!italic><white>BlissGems Expansion</white><gray>.</gray>"),
            Component.empty(),
            mm("<!italic><gradient:#4CC9F0:#C77DFF>Click for the download link</gradient>")));
        item.setItemMeta(meta);
        return item;
    }

    private void sendLink(Player player, String intro, String url) {
        player.closeInventory();
        player.sendMessage(mm("<gradient:#C77DFF:#4CC9F0><bold>BlissGems</bold></gradient> <dark_gray>»</dark_gray> " + intro));
        player.sendMessage(mm("<gradient:#4CC9F0:#C77DFF><underlined>" + url + "</underlined></gradient>")
                .clickEvent(ClickEvent.openUrl(url)).decoration(TextDecoration.ITALIC, false));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.3f);
    }

    private static Component mm(String text) {
        return MiniMessage.miniMessage().deserialize(text);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() >= 27) return;
        int slot = event.getRawSlot();
        if (holder.page == Page.GEMS) {
            if (slot == GOLD) {
                this.openGold(player);
            } else if ((slot == AURATUS || slot == HERETIC) && !this.mythicsInstalled()) {
                this.sendLink(player, "<gray>Auratus and Heretic come with the </gray><white>BlissMythics</white><gray> addon:</gray>", MYTHICS_URL);
            } else if (slot == MORE_GEMS) {
                this.sendLink(player, "<gray>More gems: </gray><white>BlissGems Expansion</white><gray>:</gray>", EXPANSION_URL);
            }
        } else if (slot == BACK) {
            this.open(player);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }
}
