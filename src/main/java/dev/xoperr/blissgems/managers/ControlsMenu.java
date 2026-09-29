package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.CooldownEntry;
import dev.xoperr.blissgems.pedestal.PedestalManager;
import dev.xoperr.blissgems.utils.AbilityBinding;
import dev.xoperr.blissgems.utils.AbilitySlot;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * /bliss ability - the controls menu. One item per ability: left-click it to cycle which key fires
 * it (right click, shift + right click, hit, shift + hit, F, shift + F), right-click it to unbind.
 * A key can only fire one ability, so taking a key from another ability unbinds that one.
 */
public final class ControlsMenu implements Listener {
    private static final int[] SLOT_POS = {10, 11, 12, 13, 15, 16};
    private static final Material[] SLOT_ICON = {Material.LIME_DYE, Material.YELLOW_DYE, Material.ORANGE_DYE, Material.RED_DYE, Material.GOLD_NUGGET, Material.GOLD_INGOT};
    private static final AbilityBinding[] ORDER = {AbilityBinding.RIGHT_CLICK, AbilityBinding.SHIFT_RIGHT_CLICK, AbilityBinding.LEFT_CLICK,
        AbilityBinding.SHIFT_LEFT_CLICK, AbilityBinding.SWAP_HAND, AbilityBinding.SHIFT_SWAP_HAND};
    private static final int RESET_POS = 22;
    private final BlissGems plugin;

    public ControlsMenu(BlissGems plugin) {
        this.plugin = plugin;
    }

    private static final class Holder implements InventoryHolder {
        Inventory inv;

        @Override
        public Inventory getInventory() {
            return this.inv;
        }
    }

    public void open(Player p) {
        Holder h = new Holder();
        h.inv = Bukkit.createInventory(h, 27, PedestalManager.color("&d&l\u26a1 &5Ability Controls"));
        this.fill(p, h.inv);
        p.openInventory(h.inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
    }

    private void fill(Player p, Inventory inv) {
        ItemStack pane = item(Material.PURPLE_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
        inv.setItem(4, item(Material.BOOK, "&d&lYour Controls", List.of(
            "&7Hold your gem and use these keys.",
            "&7Hitting something counts as a left click.",
            "",
            "&eLeft-click &7an ability: change its key",
            "&eRight-click &7an ability: unbind it",
            "",
            "&8F only does gem things if you bind it here.")));
        EnumMap<AbilityBinding, AbilitySlot> map = this.plugin.getAbilityBindingManager().getAll(p);
        List<String> names = this.abilityNames(p);
        AbilitySlot[] slots = AbilitySlot.values();
        for (int i = 0; i < slots.length && i < SLOT_POS.length; i++) {
            AbilitySlot slot = slots[i];
            AbilityBinding key = keyFor(map, slot);
            String ability = i < names.size() ? names.get(i) : (i >= 4 ? "Gold extra " + (i - 3) : slot.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add("&7Key: " + (key != null ? "&f" + key.getDisplayName() : "&8unbound"));
            if (i >= 4) lore.add("&8Gold Gem only (its harvested soul's powers)");
            lore.add("");
            lore.add("&eLeft-click &7to change key");
            lore.add("&eRight-click &7to unbind");
            inv.setItem(SLOT_POS[i], item(SLOT_ICON[i], "&d" + ability + " &8(" + slot.getDisplayName() + ")", lore));
        }
        inv.setItem(RESET_POS, item(Material.BARRIER, "&cReset to defaults", List.of(
            "&7Right click, Shift + Right click,", "&7Hit, Shift + Hit")));
    }

    /** Names of the held gem's abilities, in slot order, when the gem registered them. */
    private List<String> abilityNames(Player p) {
        List<String> out = new ArrayList<>();
        String gem = this.plugin.getGemManager().getGemId(p);
        if (gem == null || this.plugin.getGemRegistry() == null) return out;
        String key = gem.toLowerCase(Locale.ROOT).replace("_gem_t1", "").replace("_gem_t2", "");
        List<CooldownEntry> entries = this.plugin.getGemRegistry().getCooldownEntries(key);
        if (entries != null) for (CooldownEntry e : entries) out.add(e.getDisplayName());
        return out;
    }

    private static AbilityBinding keyFor(Map<AbilityBinding, AbilitySlot> map, AbilitySlot slot) {
        for (Map.Entry<AbilityBinding, AbilitySlot> e : map.entrySet()) if (e.getValue() == slot) return e.getKey();
        return null;
    }

    private static ItemStack item(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(PedestalManager.color(name));
        List<String> l = new ArrayList<>();
        for (String s : lore) l.add(PedestalManager.color(s));
        meta.setLore(l);
        it.setItemMeta(meta);
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player p) || event.getRawSlot() >= 27) return;
        AbilityBindingManager mgr = this.plugin.getAbilityBindingManager();
        if (event.getRawSlot() == RESET_POS) {
            mgr.resetToDefaults(p);
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 0.4f, 1.6f);
            this.fill(p, event.getView().getTopInventory());
            return;
        }
        int idx = -1;
        for (int i = 0; i < SLOT_POS.length; i++) if (SLOT_POS[i] == event.getRawSlot()) idx = i;
        if (idx < 0 || idx >= AbilitySlot.values().length) return;
        AbilitySlot slot = AbilitySlot.values()[idx];
        EnumMap<AbilityBinding, AbilitySlot> map = mgr.getAll(p);
        AbilityBinding current = keyFor(map, slot);
        if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
            if (current != null) mgr.unbind(p, current);
        } else {
            int at = -1;
            for (int i = 0; i < ORDER.length; i++) if (ORDER[i] == current) at = i;
            AbilityBinding next = ORDER[(at + 1) % ORDER.length];
            if (current != null) mgr.unbind(p, current);
            AbilitySlot taken = map.get(next);
            if (taken != null && taken != slot) mgr.unbind(p, next);
            mgr.setBinding(p, next, slot);
        }
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.8f);
        this.fill(p, event.getView().getTopInventory());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }
}
