/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.key.Key
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.api.CooldownEntry;
import dev.xoperr.blissgems.api.GemDefinition;
import dev.xoperr.blissgems.managers.AbilityManager;
import dev.xoperr.blissgems.managers.GemManager;
import dev.xoperr.blissgems.managers.GemRegistryImpl;
import dev.xoperr.blissgems.managers.GoldGemManager;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissgems.utils.GemType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class CooldownDisplayManager {
    private static final String GOLD_GEM_GLYPH = "\ue020";
    private static final String GOLD_BEAM_GLYPH = "\ue021";
    private final BlissGems plugin;
    private BukkitTask displayTask;
    private static final Map<GemType, List<String[]>> GEM_ABILITIES = new HashMap<GemType, List<String[]>>();
    private static final Key HUD_FONT;

    public CooldownDisplayManager(BlissGems plugin) {
        this.plugin = plugin;
        this.startDisplayTask();
    }

    private void startDisplayTask() {
        this.displayTask = this.plugin.getServer().getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            for (Player player : this.plugin.getServer().getOnlinePlayers()) {
                if (!this.plugin.getGemManager().hasActiveGem(player) || !this.isHoldingGem(player) || this.plugin.getFireAbilities().isCharging(player) || this.plugin.getFluxAbilities().isCharging(player) || this.plugin.getStrengthAbilities().isTrackingActive(player)) continue;
                if (this.plugin.getGemLockManager() != null && this.plugin.getGemLockManager().isLocked(player)) {
                    int left = this.plugin.getGemLockManager().getRemainingSeconds(player.getUniqueId());
                    String locked = "\u00a76\ua42c \u00a7c\u00a7lGEM LOCKED \u00a78| \u00a77" + left + "s";
                    this.sendActionBar(player, locked);
                    continue;
                }
                int energy = this.plugin.getEnergyManager().getEnergy(player);
                Object cooldownDisplay = this.buildCooldownDisplay(player);
                if (energy == 0) {
                    String brokenPrefix = "\u00a7c\u00a7lBROKEN \u00a78| ";
                    cooldownDisplay = !((String)cooldownDisplay).isEmpty() ? brokenPrefix + (String)cooldownDisplay : "\u00a7c\u00a7lBROKEN";
                }
                if (((String)cooldownDisplay).isEmpty()) continue;
                this.sendActionBar(player, (String)cooldownDisplay);
            }
        }, 0L, 10L);
    }

    private void sendActionBar(Player player, String legacy) {
        TextComponent out = Component.empty();
        StringBuilder run = new StringBuilder();
        boolean runGlyph = false;
        TextColor color = null;
        boolean bold = false;
        boolean italic = false;
        boolean strike = false;
        boolean under = false;
        boolean obf = false;
        for (int i = 0; i < legacy.length(); ++i) {
            char ch = legacy.charAt(i);
            if (ch == '\u00a7' && i + 1 < legacy.length()) {
                if (run.length() > 0) {
                    out = out.append(this.styleRun(run.toString(), runGlyph, color, bold, italic, strike, under, obf));
                    run.setLength(0);
                }
                char code = Character.toLowerCase(legacy.charAt(++i));
                switch (code) {
                    case '0': 
                    case '1': 
                    case '2': 
                    case '3': 
                    case '4': 
                    case '5': 
                    case '6': 
                    case '7': 
                    case '8': 
                    case '9': 
                    case 'a': 
                    case 'b': 
                    case 'c': 
                    case 'd': 
                    case 'e': 
                    case 'f': {
                        color = CooldownDisplayManager.legacyColor(code);
                        obf = false;
                        under = false;
                        strike = false;
                        italic = false;
                        bold = false;
                        break;
                    }
                    case 'l': {
                        bold = true;
                        break;
                    }
                    case 'o': {
                        italic = true;
                        break;
                    }
                    case 'm': {
                        strike = true;
                        break;
                    }
                    case 'n': {
                        under = true;
                        break;
                    }
                    case 'k': {
                        obf = true;
                        break;
                    }
                    case 'r': {
                        color = null;
                        obf = false;
                        under = false;
                        strike = false;
                        italic = false;
                        bold = false;
                        break;
                    }
                }
                continue;
            }
            boolean glyph = CooldownDisplayManager.isGlyph(ch);
            if (run.length() > 0 && glyph != runGlyph) {
                out = out.append(this.styleRun(run.toString(), runGlyph, color, bold, italic, strike, under, obf));
                run.setLength(0);
            }
            runGlyph = glyph;
            run.append(ch);
        }
        if (run.length() > 0) {
            out = out.append(this.styleRun(run.toString(), runGlyph, color, bold, italic, strike, under, obf));
        }
        player.sendActionBar((Component)out);
    }

    private static boolean isGlyph(char ch) {
        return ch < ' ' || ch > '~';
    }

    private Component styleRun(String text, boolean glyph, TextColor color, boolean bold, boolean italic, boolean strike, boolean under, boolean obf) {
        Component c = Component.text((String)text);
        if (color != null) {
            c = c.color(color);
        }
        if (bold) {
            c = c.decorate(TextDecoration.BOLD);
        }
        if (italic) {
            c = c.decorate(TextDecoration.ITALIC);
        }
        if (strike) {
            c = c.decorate(TextDecoration.STRIKETHROUGH);
        }
        if (under) {
            c = c.decorate(TextDecoration.UNDERLINED);
        }
        if (obf) {
            c = c.decorate(TextDecoration.OBFUSCATED);
        }
        if (!glyph) {
            c = c.font(HUD_FONT);
        }
        return c;
    }

    private static TextColor legacyColor(char code) {
        return switch (code) {
            case '0' -> TextColor.color((int)0);
            case '1' -> TextColor.color((int)170);
            case '2' -> TextColor.color((int)43520);
            case '3' -> TextColor.color((int)43690);
            case '4' -> TextColor.color((int)0xAA0000);
            case '5' -> TextColor.color((int)0xAA00AA);
            case '6' -> TextColor.color((int)0xFFAA00);
            case '7' -> TextColor.color((int)0xAAAAAA);
            case '8' -> TextColor.color((int)0x555555);
            case '9' -> TextColor.color((int)0x5555FF);
            case 'a' -> TextColor.color((int)0x55FF55);
            case 'b' -> TextColor.color((int)0x55FFFF);
            case 'c' -> TextColor.color((int)0xFF5555);
            case 'd' -> TextColor.color((int)0xFF55FF);
            case 'e' -> TextColor.color((int)0xFFFF55);
            default -> TextColor.color((int)0xFFFFFF);
        };
    }

    private boolean isHoldingGem(Player player) {
        return this.getCurrentGemInfo(player) != null;
    }

    private GemInfo getCurrentGemInfo(Player player) {
        String oraxenId;
        ItemStack offHand;
        String oraxenId2;
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && (oraxenId2 = CustomItemManager.getIdByItem(mainHand)) != null) {
            if (GemType.isGem(oraxenId2)) {
                GemType type = GemType.fromOraxenId(oraxenId2);
                int tier = GemType.getTierFromOraxenId(oraxenId2);
                String gemId = type != null ? type.getId() : null;
                return new GemInfo(type, gemId, tier, "\u270b");
            }
            if (registry != null && registry.isRegisteredGem(oraxenId2)) {
                String gemId = registry.gemIdFromItemId(oraxenId2);
                int tier = registry.tierFromItemId(oraxenId2);
                return new GemInfo(null, gemId, tier, "\u270b");
            }
        }
        if ((offHand = player.getInventory().getItemInOffHand()) != null && (oraxenId = CustomItemManager.getIdByItem(offHand)) != null) {
            if (GemType.isGem(oraxenId)) {
                GemType type = GemType.fromOraxenId(oraxenId);
                int tier = GemType.getTierFromOraxenId(oraxenId);
                String gemId = type != null ? type.getId() : null;
                return new GemInfo(type, gemId, tier, "\ud83d\udee1");
            }
            if (registry != null && registry.isRegisteredGem(oraxenId)) {
                String gemId = registry.gemIdFromItemId(oraxenId);
                int tier = registry.tierFromItemId(oraxenId);
                return new GemInfo(null, gemId, tier, "\ud83d\udee1");
            }
        }
        return null;
    }

    private String buildCooldownDisplay(Player player) {
        StringBuilder display = new StringBuilder();
        AbilityManager abilityManager = this.plugin.getAbilityManager();
        GemInfo gemInfo = this.getCurrentGemInfo(player);
        if (gemInfo == null) {
            return "";
        }
        GemType gemType = gemInfo.type;
        int tier = gemInfo.tier;
        if ("gold".equals(gemInfo.gemId)) {
            return this.buildGoldDisplay(player);
        }
        if (gemType == null) {
            return "";
        }
        String gemColor = gemType.getColor();
        List<String[]> abilities = GEM_ABILITIES.get((Object)gemType);
        if (abilities == null) {
            return "";
        }
        if (gemType == GemType.ASTRA) {
            String ability1Icon = this.getAbilityIcon(gemType, 0);
            display.append(ability1Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(0)[0]));
            if (tier == 2) {
                String ability2Icon = this.getAbilityIcon(gemType, 1);
                display.append("  \u00a75(\u2726)  ");
                display.append(ability2Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(1)[0]));
            }
            return display.toString();
        }
        if (gemType == GemType.FLUX) {
            String ability1Icon = this.getAbilityIcon(gemType, 0);
            display.append(ability1Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(0)[0]));
            if (tier == 2) {
                String ability2Icon = this.getAbilityIcon(gemType, 1);
                int remainingFlash = abilityManager.getRemainingCooldown(player, abilities.get(2)[0]);
                int remainingKinetic = abilityManager.getRemainingCooldown(player, abilities.get(3)[0]);
                if (remainingFlash > 0 && remainingKinetic > 0) {
                    display.append(" \u00a7b(").append(remainingFlash).append("s\u00a77|").append(remainingKinetic).append("s) ");
                } else if (remainingFlash > 0) {
                    display.append(" \u00a7b(").append(remainingFlash).append("s) ");
                } else if (remainingKinetic > 0) {
                    display.append(" \u00a7b(").append(remainingKinetic).append("s) ");
                } else {
                    display.append("  \u00a7b(\u26a1)  ");
                }
                display.append(ability2Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(1)[0]));
            }
            return display.toString();
        }
        if (gemType == GemType.LIFE) {
            String ability1Icon = this.getAbilityIcon(gemType, 0);
            display.append(ability1Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(0)[0]));
            if (tier == 2) {
                String ability2Icon = this.getAbilityIcon(gemType, 1);
                String vortexStatus = this.formatCompactStatus(player, abilities.get(2)[0]);
                String lockStatus = this.formatCompactStatus(player, abilities.get(3)[0]);
                if (!"\u2022".equals(vortexStatus) && !"\u2022".equals(lockStatus)) {
                    display.append(" \u00a7d(").append(vortexStatus).append("\u00a77|").append(lockStatus).append(") ");
                } else if (!"\u2022".equals(vortexStatus)) {
                    display.append(" \u00a7d(").append(vortexStatus).append(") ");
                } else if (!"\u2022".equals(lockStatus)) {
                    display.append(" \u00a7d(").append(lockStatus).append(") ");
                } else {
                    display.append("  \u00a7d(\u2764)  ");
                }
                display.append(ability2Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(1)[0]));
            }
            return display.toString();
        }
        if (gemType == GemType.PUFF) {
            String ability1Icon = this.getAbilityIcon(gemType, 0);
            display.append(ability1Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(0)[0]));
            if (tier == 2) {
                String ability2Icon = this.getAbilityIcon(gemType, 1);
                int remainingGroup = abilityManager.getRemainingCooldown(player, abilities.get(2)[0]);
                if (remainingGroup > 0) {
                    display.append(" \u00a7f(").append(remainingGroup).append("s) ");
                } else {
                    display.append("  \u00a7f(\u2728)  ");
                }
                display.append(ability2Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(1)[0]));
            }
            return display.toString();
        }
        if (gemType == GemType.SPEED) {
            String ability1Icon = this.getAbilityIcon(gemType, 0);
            int blurCharges = this.plugin.getSpeedAbilities().getBlurCharges(player.getUniqueId());
            display.append(ability1Icon).append(" ");
            if (blurCharges > 0) {
                int maxCharges = this.plugin.getConfig().getInt("abilities.blur.strikes", 3);
                display.append("\u00a7e(").append(blurCharges).append("/").append(maxCharges).append(")");
            } else {
                display.append(this.readyOrSeconds(player, abilities.get(0)[0]));
            }
            if (tier == 2 && abilities.size() > 1) {
                String ability2Icon = this.getAbilityIcon(gemType, 1);
                String tertiaryStatus = this.formatCompactStatus(player, abilities.get(2)[0]);
                if (!"\u2022".equals(tertiaryStatus)) {
                    display.append(" \u00a7a(").append(tertiaryStatus).append(") ");
                } else {
                    display.append("  \u00a7a(\u26a1)  ");
                }
                display.append(ability2Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(1)[0]));
            }
            return display.toString();
        }
        if (gemType == GemType.WEALTH) {
            String ability1Icon = this.getAbilityIcon(gemType, 0);
            display.append(ability1Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(0)[0]));
            if (tier == 2 && abilities.size() > 1) {
                String ampStatus;
                String ability2Icon = this.getAbilityIcon(gemType, 1);
                String lockStatus = abilities.size() > 2 ? this.formatCompactStatus(player, abilities.get(2)[0]) : "\u2022";
                String string = ampStatus = abilities.size() > 3 ? this.formatCompactStatus(player, abilities.get(3)[0]) : "\u2022";
                if (!"\u2022".equals(lockStatus) && !"\u2022".equals(ampStatus)) {
                    display.append(" \u00a7e(").append(lockStatus).append("\u00a77|").append(ampStatus).append(") ");
                } else if (!"\u2022".equals(lockStatus)) {
                    display.append(" \u00a7e(").append(lockStatus).append(") ");
                } else if (!"\u2022".equals(ampStatus)) {
                    display.append(" \u00a7e(").append(ampStatus).append(") ");
                } else {
                    display.append("  \u00a7e(\ud83d\udcb0)  ");
                }
                display.append(ability2Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(1)[0]));
            }
            return display.toString();
        }
        if (gemType == GemType.STRENGTH) {
            String ability1Icon = this.getAbilityIcon(gemType, 0);
            display.append(ability1Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(0)[0]));
            if (tier == 2 && abilities.size() > 1) {
                String tertiaryStatus;
                String ability2Icon = this.getAbilityIcon(gemType, 1);
                String string = tertiaryStatus = abilities.size() > 2 ? this.formatCompactStatus(player, abilities.get(2)[0]) : "\u2022";
                if (!"\u2022".equals(tertiaryStatus)) {
                    display.append(" \u00a76(").append(tertiaryStatus).append(") ");
                } else {
                    display.append("  \u00a76(\ud83d\udcaa)  ");
                }
                display.append(ability2Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(1)[0]));
            }
            return display.toString();
        }
        if (gemType == GemType.ASTRA) {
            String ability1Icon = this.getAbilityIcon(gemType, 0);
            display.append(ability1Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(0)[0]));
            if (tier == 2 && abilities.size() > 1) {
                String ability2Icon = this.getAbilityIcon(gemType, 1);
                display.append(" ").append(gemColor).append("| ").append(ability2Icon).append(" ").append(this.readyOrSeconds(player, abilities.get(1)[0]));
                if (abilities.size() > 2) {
                    display.append(" ").append(gemColor).append("| ").append(gemColor).append("Drift: ").append(this.readyOrSeconds(player, abilities.get(2)[0]));
                }
                if (abilities.size() > 3) {
                    display.append(" ").append(gemColor).append("| ").append(gemColor).append("Nullify: ").append(this.readyOrSeconds(player, abilities.get(3)[0]));
                }
            }
            return display.toString();
        }
        String[] ability1 = abilities.get(0);
        String ability1Key = ability1[0];
        String ability1Icon = this.getAbilityIcon(gemType, 0);
        display.append(ability1Icon).append(" ").append(this.readyOrSeconds(player, ability1Key));
        if (tier == 2 && abilities.size() > 1) {
            String[] ability2 = abilities.get(1);
            String ability2Key = ability2[0];
            String ability2Icon = this.getAbilityIcon(gemType, 1);
            display.append(" ").append(gemColor).append("| ").append(ability2Icon).append(" ").append(this.readyOrSeconds(player, ability2Key));
            for (int i = 2; i < abilities.size(); ++i) {
                String[] ability = abilities.get(i);
                String abilityKeyStr = ability[0];
                display.append(" ").append(gemColor).append("| ").append(gemColor).append("\u2726 ").append(this.readyOrSeconds(player, abilityKeyStr));
            }
        }
        return display.toString();
    }

    private String buildGoldDisplay(Player player) {
        GoldGemManager gold = this.plugin.getGoldGemManager();
        if (gold == null) {
            return "";
        }
        AbilityManager abilityManager = this.plugin.getAbilityManager();
        Map<String, GoldGemManager.Harvest> souls = gold.getHarvested(player.getUniqueId());
        StringBuilder display = new StringBuilder();
        display.append("\u00a7f").append(GOLD_GEM_GLYPH).append(" \u00a76").append(souls.size()).append("\u00a77/").append(8);
        int beam = abilityManager.getRemainingCooldown(player, "gold-beam");
        display.append(" \u00a76(\u00a7f").append(GOLD_BEAM_GLYPH).append(" ").append(this.readyOrSeconds(beam)).append("\u00a76)");
        if (souls.isEmpty()) {
            return display.append(" \u00a78| \u00a77no soul").toString();
        }
        display.append(" \u00a78|");
        String active = gold.getActive(player.getUniqueId());
        for (Map.Entry<String, GoldGemManager.Harvest> entry : souls.entrySet()) {
            GemType soul = GemManager.builtInType(entry.getKey());
            List<String[]> abilities = soul != null ? GEM_ABILITIES.get((Object)soul) : null;
            if (abilities == null || abilities.isEmpty()) continue;
            boolean channelled = entry.getKey().equals(active);
            if (!channelled) {
                display.append(" \u00a7f").append(this.getAbilityIcon(soul, 0)).append(" \u00a78").append(this.shortCooldown(abilityManager.getRemainingCooldown(player, abilities.get(0)[0])));
                continue;
            }
            int usable = entry.getValue().tier() >= 2 ? Math.min(abilities.size(), 4) : 1;
            display.append(" ").append(this.getGemColor(soul)).append("\u00ab");
            for (int i = 0; i < usable; ++i) {
                display.append("\u00a7f").append(this.getAbilityIcon(soul, i)).append(" ").append(this.readyOrSeconds(abilityManager.getRemainingCooldown(player, abilities.get(i)[0])));
                if (i >= usable - 1) continue;
                display.append(" ");
            }
            display.append(this.getGemColor(soul)).append("\u00bb");
        }
        return display.toString();
    }

    private String shortCooldown(int remaining) {
        return remaining > 0 ? "\u00a7c" + remaining + "s" : "\u00a7a\u2022";
    }

    private String shortCooldown(Player player, String abilityKey) {
        if (this.plugin.getAbilityManager().isAbilityActive(player, abilityKey)) {
            return "\u00a7eAct";
        }
        int remaining = this.plugin.getAbilityManager().getRemainingCooldown(player, abilityKey);
        return remaining > 0 ? "\u00a7c" + remaining + "s" : "\u00a7a\u2022";
    }

    private String formatCompactStatus(Player player, String abilityKey) {
        if (this.plugin.getAbilityManager().isAbilityActive(player, abilityKey)) {
            return "Act";
        }
        int remaining = this.plugin.getAbilityManager().getRemainingCooldown(player, abilityKey);
        if (remaining > 0) {
            return remaining + "s";
        }
        return "\u2022";
    }

    private String readyOrSeconds(int remaining) {
        return remaining > 0 ? "\u00a7c" + remaining + "s" : "\u00a7aReady";
    }

    private String readyOrSeconds(Player player, String abilityKey) {
        if (this.plugin.getAbilityManager().isAbilityActive(player, abilityKey)) {
            return "\u00a7eActive";
        }
        int remaining = this.plugin.getAbilityManager().getRemainingCooldown(player, abilityKey);
        return remaining > 0 ? "\u00a7c" + remaining + "s" : "\u00a7aReady";
    }

    private String getCustomGemIcon(GemType gemType) {
        return switch (gemType) {
            default -> throw new IncompatibleClassChangeError();
            case ASTRA -> "\ue000";
            case FIRE -> "\ue001";
            case FLUX -> "\ue002";
            case LIFE -> "\ue003";
            case PUFF -> "\ue004";
            case SPEED -> "\ue005";
            case STRENGTH -> "\ue006";
            case WEALTH -> "\ue007";
        };
    }

    private String getAbilityIcon(GemType gemType, int abilityIndex) {
        return switch (gemType) {
            default -> throw new IncompatibleClassChangeError();
            case ASTRA -> {
                if (abilityIndex == 0) {
                    yield "\ue010";
                }
                if (abilityIndex == 1) {
                    yield "\ue011";
                }
                if (abilityIndex == 2) {
                    yield "\ue010";
                }
                yield "\ue011";
            }
            case FIRE -> {
                if (abilityIndex == 0) {
                    yield "\ue012";
                }
                yield "\ue013";
            }
            case FLUX -> {
                if (abilityIndex == 0) {
                    yield "\ue014";
                }
                yield "\ue015";
            }
            case LIFE -> {
                if (abilityIndex == 0) {
                    yield "\ue016";
                }
                yield "\ue017";
            }
            case PUFF -> {
                if (abilityIndex == 0) {
                    yield "\ue018";
                }
                yield "\ue019";
            }
            case SPEED -> {
                if (abilityIndex == 0) {
                    yield "\ue01a";
                }
                yield "\ue01b";
            }
            case STRENGTH -> {
                if (abilityIndex == 0) {
                    yield "\ue01c";
                }
                yield "\ue01d";
            }
            case WEALTH -> abilityIndex == 0 ? "\ue01e" : "\ue01f";
        };
    }

    private String getGemIcon(GemType gemType) {
        switch (gemType) {
            case ASTRA: {
                return "\u2726";
            }
            case FIRE: {
                return "\ud83d\udd25";
            }
            case FLUX: {
                return "\u26a1";
            }
            case LIFE: {
                return "\u2764";
            }
            case PUFF: {
                return "\ud83d\udca8";
            }
            case SPEED: {
                return "\u26a1";
            }
            case STRENGTH: {
                return "\u2694";
            }
            case WEALTH: {
                return "\ud83d\udcb0";
            }
        }
        return "\u25c6";
    }

    private String getGemColor(GemType gemType) {
        switch (gemType) {
            case ASTRA: {
                return "\u00a75";
            }
            case FIRE: {
                return "\u00a7c";
            }
            case FLUX: {
                return "\u00a7b";
            }
            case LIFE: {
                return "\u00a7d";
            }
            case PUFF: {
                return "\u00a7f";
            }
            case SPEED: {
                return "\u00a7a";
            }
            case STRENGTH: {
                return "\u00a76";
            }
            case WEALTH: {
                return "\u00a7e";
            }
        }
        return "\u00a77";
    }

    private String getEnergyBar(int energy) {
        String icon;
        String color;
        StringBuilder bar = new StringBuilder();
        if (energy >= 8) {
            color = "\u00a7d\u00a7l";
            icon = "\u25cf";
        } else if (energy >= 5) {
            color = "\u00a7b";
            icon = "\u25cf";
        } else if (energy >= 3) {
            color = "\u00a7e";
            icon = "\u25cf";
        } else if (energy >= 2) {
            color = "\u00a76";
            icon = "\u25cf";
        } else if (energy == 1) {
            color = "\u00a74";
            icon = "\u25cb";
        } else {
            color = "\u00a7c\u00a7l";
            icon = "\u25cb";
        }
        bar.append(color);
        for (int i = 0; i < 10; ++i) {
            if (i < energy) {
                bar.append(icon);
                continue;
            }
            bar.append("\u00a78\u25cb");
        }
        bar.append(" \u00a77(").append(energy).append("/10)");
        return bar.toString();
    }

    private String getCooldownBar(int remaining, int total, String abilityName) {
        double percentage = 1.0 - (double)remaining / (double)total;
        int bars = (int)(percentage * 10.0);
        StringBuilder bar = new StringBuilder();
        String color = remaining <= 3 ? "\u00a7a" : (remaining <= 10 ? "\u00a7e" : (remaining <= 30 ? "\u00a76" : "\u00a7c"));
        bar.append(color).append(abilityName).append(" ");
        bar.append("\u00a77[");
        for (int i = 0; i < 10; ++i) {
            if (i < bars) {
                bar.append("\u00a7a\u25b0");
                continue;
            }
            bar.append("\u00a78\u25b1");
        }
        bar.append("\u00a77] ").append(color).append(remaining).append("s");
        return bar.toString();
    }

    private String buildAddonCooldownDisplay(Player player, GemInfo gemInfo) {
        if (gemInfo.gemId == null) {
            return "";
        }
        GemRegistryImpl registry = this.plugin.getGemRegistry();
        if (registry == null) {
            return "";
        }
        List<CooldownEntry> entries = registry.getCooldownEntries(gemInfo.gemId);
        if (entries == null || entries.isEmpty()) {
            return "";
        }
        GemDefinition def = registry.getGem(gemInfo.gemId);
        String gemColor = def != null ? def.getColor() : "\u00a77";
        AbilityManager abilityManager = this.plugin.getAbilityManager();
        StringBuilder display = new StringBuilder();
        boolean first = true;
        for (CooldownEntry entry : entries) {
            if (!first) {
                display.append(" ").append(gemColor).append("| ");
            }
            first = false;
            int remaining = abilityManager.getRemainingCooldown(player, entry.getAbilityKey());
            display.append(gemColor).append(entry.getDisplayName()).append(" ");
            if (remaining > 0) {
                display.append("\u00a7c").append(remaining).append("s");
                continue;
            }
            display.append("\u00a7aReady");
        }
        return display.toString();
    }

    public void stop() {
        if (this.displayTask != null) {
            this.displayTask.cancel();
        }
    }

    static {
        GEM_ABILITIES.put(GemType.FIRE, Arrays.asList(new String[]{"fire-fireball", "Fireball"}, new String[]{"fire-campfire", "Campfire"}, new String[]{"fire-crisp", "Crisp"}, new String[]{"fire-meteor-shower", "Meteor"}));
        GEM_ABILITIES.put(GemType.ASTRA, Arrays.asList(new String[]{"astra-daggers", "Daggers"}, new String[]{"astra-projection", "Projection"}, new String[]{"astra-drift", "Drift"}, new String[]{"astra-void", "Nullify"}));
        GEM_ABILITIES.put(GemType.LIFE, Arrays.asList(new String[]{"life-drainer", "Drainer"}, new String[]{"life-circle-of-life", "Circle"}, new String[]{"life-vitality-vortex", "Vortex"}, new String[]{"life-heart-lock", "Lock"}));
        GEM_ABILITIES.put(GemType.FLUX, Arrays.asList(new String[]{"flux-beam", "Beam"}, new String[]{"flux-ground", "Ground"}, new String[]{"flux-flashbang", "Flash"}, new String[]{"flux-kinetic-burst", "Kinetic"}));
        GEM_ABILITIES.put(GemType.PUFF, Arrays.asList(new String[]{"puff-dash", "Dash"}, new String[]{"puff-breezy-bash", "Bash"}, new String[]{"puff-group-bash", "Group"}));
        GEM_ABILITIES.put(GemType.SPEED, Arrays.asList(new String[]{"speed-blur", "Blur"}, new String[]{"speed-storm", "Storm"}, new String[]{"speed-terminal", "Terminal"}));
        GEM_ABILITIES.put(GemType.STRENGTH, Arrays.asList(new String[]{"strength-chad", "Chad"}, new String[]{"strength-frailer", "Frailer"}, new String[]{"strength-shadow-stalker", "Stalker"}, new String[]{"strength-nullify", "Nullify"}));
        GEM_ABILITIES.put(GemType.WEALTH, Arrays.asList(new String[]{"wealth-unfortunate", "Unfortunate"}, new String[]{"wealth-rich-rush", "Rush"}, new String[]{"wealth-item-lock", "Lock"}, new String[]{"wealth-amplification", "Amplify"}));
        HUD_FONT = Key.key((String)"bliss", (String)"hud");
    }

    private static class GemInfo {
        final GemType type;
        final String gemId;
        final int tier;
        final String location;

        GemInfo(GemType type, String gemId, int tier, String location) {
            this.type = type;
            this.gemId = gemId;
            this.tier = tier;
            this.location = location;
        }
    }
}

