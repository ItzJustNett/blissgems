package dev.xoperr.blissgems.villagerevent;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.pedestal.PedestalManager;
import dev.xoperr.blissgems.utils.CustomItemManager;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

/** Villager-event items (energy tokens, the three villager souls, the wanderer's disc), names, colours and trades. */
public final class VillagerEventItems {
    public static final String EVENT_TAG = "BlissEventVillager";
    public static final int CMD_ENERGY_TOKEN = 350;
    public static final int CMD_SOUL_BASE = 359;
    private static final int[] SOUL_STOPS = {0xFFAA00, 0xFFFF55};
    private static final int[] OWNER_STOPS = {0xAA66FF, 0xFF75DD};

    private final BlissGems plugin;
    private final NamespacedKey discKey;
    private final NamespacedKey tokenKey;

    public VillagerEventItems(BlissGems plugin) {
        this.plugin = plugin;
        this.discKey = new NamespacedKey(plugin, "village_disc");
        this.tokenKey = new NamespacedKey(plugin, "energy_token");
    }

    // ---- text ----

    public static Component text(String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(PedestalManager.color(legacy)).decoration(TextDecoration.ITALIC, false);
    }

    /** A left-to-right gradient that cycles through the given colours, shifted by phase. */
    public static Component flowGradient(String s, double phase, int[] stops) {
        Component out = Component.empty();
        int n = s.length();
        for (int i = 0; i < n; i++) {
            double t = phase + (n <= 1 ? 0.0 : (double) i / n);
            out = out.append(Component.text(String.valueOf(s.charAt(i)), TextColor.color(sample(stops, t))));
        }
        return out.decoration(TextDecoration.ITALIC, false);
    }

    private static int sample(int[] stops, double t) {
        double x = (t - Math.floor(t)) * stops.length;
        int i = (int) Math.floor(x) % stops.length;
        int a = stops[i];
        int b = stops[(i + 1) % stops.length];
        double f = x - Math.floor(x);
        int r = (int) Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * f);
        int g = (int) Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * f);
        int bl = (int) Math.round((a & 255) + ((b & 255) - (a & 255)) * f);
        return (r << 16) | (g << 8) | bl;
    }

    // ---- cosmetics per village (1..3) ----

    public static String ownerName(int id) {
        return switch (id) {
            case 1 -> "Duke Dennis";
            case 2 -> "KaiCenat";
            default -> "Darren Watkins";
        };
    }

    public static Component villagerName(int id) {
        return text("&f" + ownerName(id));
    }

    public static Color soulColor(int id) {
        return switch (id) {
            case 1 -> Color.fromRGB(255, 105, 180);
            case 2 -> Color.fromRGB(192, 192, 192);
            default -> Color.fromRGB(218, 112, 214);
        };
    }

    public static Particle.DustOptions auraDust(int id, float size) {
        return new Particle.DustOptions(soulColor(id), size);
    }

    public static Particle.DustOptions auraDustEdge(int id, float size) {
        Color c = soulColor(id);
        return new Particle.DustOptions(Color.fromRGB((c.getRed() + 255) / 2, (c.getGreen() + 255) / 2, (c.getBlue() + 255) / 2), size);
    }

    public static Particle.DustOptions beamDust(int id) {
        return switch (id) {
            case 1 -> new Particle.DustOptions(Color.fromRGB(255, 105, 180), 1.0f);
            case 2 -> new Particle.DustOptions(Color.fromRGB(180, 180, 180), 1.0f);
            default -> new Particle.DustOptions(Color.fromRGB(200, 80, 200), 1.0f);
        };
    }

    // ---- items ----

    public ItemStack energyToken() {
        return energyToken(1);
    }

    public ItemStack energyToken(int amount) {
        ItemStack item = new ItemStack(Material.NAUTILUS_SHELL, amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(text("<#90EE90>&lᴇɴᴇʀɢʏ &r<#90EE90>ᴛᴏᴋᴇɴ"));
        meta.lore(List.of(text("<#90EE90>&oConverted from bottled energy."), text("&7Cannot be reversed or restore gem energy.")));
        meta.setCustomModelData(CMD_ENERGY_TOKEN);
        meta.getPersistentDataContainer().set(this.tokenKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isEnergyToken(ItemStack item) {
        if (item == null || item.getType() != Material.NAUTILUS_SHELL || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(this.tokenKey, PersistentDataType.BYTE)
            || (meta.hasCustomModelData() && meta.getCustomModelData() == CMD_ENERGY_TOKEN);
    }

    public boolean isBottledEnergy(ItemStack item) {
        return item != null && "energy_bottle".equals(CustomItemManager.getIdByItem(item));
    }

    public ItemStack soul(int id) {
        if (id < 1 || id > 3) throw new IllegalArgumentException("soul id must be 1..3");
        ItemStack item = new ItemStack(Material.PITCHER_POD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(flowGradient("Villager Soul", 0.0, SOUL_STOPS));
        meta.lore(List.of(flowGradient("Soul of", 0.0, OWNER_STOPS), text("&f" + ownerName(id))));
        meta.setCustomModelData(CMD_SOUL_BASE + id);
        meta.setUnbreakable(true);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    /** 1..3 for a villager soul, -1 otherwise. */
    public int soulIdOf(ItemStack item) {
        if (item == null || item.getType() != Material.PITCHER_POD || !item.hasItemMeta()) return -1;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasCustomModelData()) return -1;
        int id = meta.getCustomModelData() - CMD_SOUL_BASE;
        return id >= 1 && id <= 3 ? id : -1;
    }

    public ItemStack villageDisc() {
        ItemStack item = new ItemStack(Material.MUSIC_DISC_5);
        ItemMeta meta = item.getItemMeta();
        meta.lore(List.of(text("<#DA70D6>&oPlay it in a jukebox..."), text("&7The melody whispers where the"), text("&7lost village can be found.")));
        meta.getPersistentDataContainer().set(this.discKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isVillageDisc(ItemStack item) {
        return item != null && item.getType() == Material.MUSIC_DISC_5 && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer().has(this.discKey, PersistentDataType.BYTE);
    }

    // ---- villagers and trades ----

    public Villager spawnVillager(int id, Location at, VillagerEventState state) {
        Villager v = at.getWorld().spawn(at, Villager.class, spawned -> this.dressVillager(spawned, id));
        state.setVillager(id, v);
        state.setVillagerLoc(id, at);
        state.setVillagerAlive(id, true);
        state.unlockSoulDrop(v.getUniqueId());
        state.save();
        return v;
    }

    public void dressVillager(Villager v, int id) {
        v.addScoreboardTag(EVENT_TAG);
        v.customName(villagerName(id));
        v.setCustomNameVisible(true);
        v.setSilent(true);
        v.setPersistent(true);
        v.setRemoveWhenFarAway(false);
        v.setVillagerType(Villager.Type.PLAINS);
        v.setProfession(Villager.Profession.TOOLSMITH);
        v.setVillagerLevel(5);
        v.setRecipes(this.buildTrades(id));
    }

    public void applyTrades(int id, Villager v) {
        v.setRecipes(this.buildTrades(id));
    }

    private List<MerchantRecipe> buildTrades(int id) {
        List<MerchantRecipe> list = new ArrayList<>();
        list.add(recipe(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE), new ItemStack(Material.NETHERITE_INGOT), 9999999));
        list.add(recipe(splash(PotionType.STRONG_SWIFTNESS), new ItemStack(Material.EMERALD, 8), 9999999));
        list.add(recipe(splash(PotionType.STRONG_STRENGTH), new ItemStack(Material.EMERALD, 8), 9999999));
        list.add(recipe(new ItemStack(Material.COBWEB, 64), new ItemStack(Material.EMERALD, 4), 9999999));
        list.add(recipe(new ItemStack(Material.BREEZE_ROD), new ItemStack(Material.EMERALD, 4), 9999999));
        list.add(recipe(new ItemStack(Material.EXPERIENCE_BOTTLE, 64), new ItemStack(Material.EMERALD, 20), 9999999));
        list.add(recipe(new ItemStack(Material.MACE), this.energyToken(TokenTrade.MACE.cost), 1));
        for (TokenTrade t : TokenTrade.exclusivesFor(id)) {
            list.add(recipe(t.build(), this.energyToken(t.cost), 1));
        }
        return list;
    }

    private static MerchantRecipe recipe(ItemStack result, ItemStack cost, int maxUses) {
        MerchantRecipe r = new MerchantRecipe(result, 0, maxUses, false, 0, 0.0f);
        r.addIngredient(cost);
        return r;
    }

    private static ItemStack splash(PotionType type) {
        ItemStack item = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionType(type);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack namedNetherite(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(text("&f&l" + name));
        meta.lore(List.of(text("&7One of one on the server.")));
        item.setItemMeta(meta);
        return item;
    }

    /** The one-per-village trades paid for in energy tokens. */
    public enum TokenTrade {
        MACE("mace", 8, "Mace", Material.MACE, null),
        HELM("helm", 4, "Netherite Helm", Material.NETHERITE_HELMET, "ɴᴇᴛʜᴇʀɪᴛᴇ ʜᴇʟᴍ"),
        LEGS("legs", 4, "Netherite Leggings", Material.NETHERITE_LEGGINGS, "ɴᴇᴛʜᴇʀɪᴛᴇ ʟᴇɢɢɪɴɢꜱ"),
        CHEST("chest", 4, "Netherite Chestplate", Material.NETHERITE_CHESTPLATE, "ɴᴇᴛʜᴇʀɪᴛᴇ ᴄʜᴇꜱᴛᴘʟᴀᴛᴇ"),
        BOOTS("boots", 4, "Netherite Boots", Material.NETHERITE_BOOTS, "ɴᴇᴛʜᴇʀɪᴛᴇ ʙᴏᴏᴛꜱ"),
        SWORD("sword", 4, "Netherite Sword", Material.NETHERITE_SWORD, "ɴᴇᴛʜᴇʀɪᴛᴇ ꜱᴡᴏʀᴅ"),
        AXE("axe", 4, "Netherite Axe", Material.NETHERITE_AXE, "ɴᴇᴛʜᴇʀɪᴛᴇ ᴀxᴇ");

        public final String key;
        public final int cost;
        public final String displayName;
        private final Material material;
        private final String smallCaps;

        TokenTrade(String key, int cost, String displayName, Material material, String smallCaps) {
            this.key = key;
            this.cost = cost;
            this.displayName = displayName;
            this.material = material;
            this.smallCaps = smallCaps;
        }

        public ItemStack build() {
            return this.smallCaps == null ? new ItemStack(this.material) : namedNetherite(this.material, this.smallCaps);
        }

        public static TokenTrade match(ItemStack item) {
            if (item == null) return null;
            for (TokenTrade t : values()) if (t.material == item.getType()) return t;
            return null;
        }

        static TokenTrade[] exclusivesFor(int id) {
            return switch (id) {
                case 1 -> new TokenTrade[]{HELM, LEGS};
                case 2 -> new TokenTrade[]{CHEST, BOOTS};
                default -> new TokenTrade[]{SWORD, AXE};
            };
        }
    }
}
