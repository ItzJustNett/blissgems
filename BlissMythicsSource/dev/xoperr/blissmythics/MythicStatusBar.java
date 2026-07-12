/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.xoperr.blissgems.api.BlissGemsAPI
 *  dev.xoperr.blissgems.api.CooldownEntry
 *  dev.xoperr.blissgems.utils.CustomItemManager
 *  net.md_5.bungee.api.ChatMessageType
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package dev.xoperr.blissmythics;

import dev.xoperr.blissgems.api.BlissGemsAPI;
import dev.xoperr.blissgems.api.CooldownEntry;
import dev.xoperr.blissgems.utils.CustomItemManager;
import dev.xoperr.blissmythics.AuratusGem;
import dev.xoperr.blissmythics.BlissMythics;
import dev.xoperr.blissmythics.HereticGem;
import java.util.List;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class MythicStatusBar {
    private final BlissMythics plugin;
    private final BlissGemsAPI api;
    private final HereticGem heretic;
    private final AuratusGem auratus;

    public MythicStatusBar(BlissMythics blissMythics, BlissGemsAPI blissGemsAPI, HereticGem hereticGem, AuratusGem auratusGem) {
        this.plugin = blissMythics;
        this.api = blissGemsAPI;
        this.heretic = hereticGem;
        this.auratus = auratusGem;
        Bukkit.getScheduler().runTaskTimer((Plugin)blissMythics, this::tick, 20L, 10L);
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String string;
            String string2 = this.heldMythicGemId(player);
            if (string2 == null || (string = this.build(player, string2)).isEmpty()) continue;
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, (BaseComponent)new TextComponent(string));
        }
    }

    private String heldMythicGemId(Player player) {
        String string = this.gemIdOf(player.getInventory().getItemInMainHand());
        if (string == null) {
            string = this.gemIdOf(player.getInventory().getItemInOffHand());
        }
        return string;
    }

    private String gemIdOf(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return null;
        }
        String string = CustomItemManager.getIdByItem((ItemStack)itemStack);
        if (string == null) {
            return null;
        }
        String string2 = this.api.getGemRegistry().gemIdFromItemId(string);
        return string2 != null && ("heretic".equals(string2) || "auratus".equals(string2)) ? string2 : null;
    }

    private String build(Player player, String string) {
        List list = this.api.getGemRegistry().getCooldownEntries(string);
        if (list == null || list.isEmpty()) {
            return "";
        }
        String string2 = "heretic".equals(string) ? "\u00a74" : "\u00a76";
        StringBuilder stringBuilder = new StringBuilder();
        boolean bl = true;
        for (CooldownEntry cooldownEntry : list) {
            boolean bl2;
            if (!bl) {
                stringBuilder.append(" ").append(string2).append("| ");
            }
            bl = false;
            int n = this.api.getAbilityManager().getRemainingCooldown(player, cooldownEntry.getAbilityKey());
            boolean bl3 = bl2 = cooldownEntry.getAbilityKey().equals("auratus-perforators") || cooldownEntry.getAbilityKey().equals("heretic-bloodsaws");
            if (bl2) {
                String string3 = cooldownEntry.getDisplayName();
                if (cooldownEntry.getAbilityKey().equals("auratus-perforators")) {
                    int n2 = this.auratus.chainCharges(player);
                    stringBuilder.append("\u00a7f").append(string3).append(" ");
                    if (n2 >= 2) {
                        stringBuilder.append("\u00a7a2");
                        continue;
                    }
                    if (n2 == 1) {
                        stringBuilder.append("\u00a7e1");
                        continue;
                    }
                    stringBuilder.append("\u00a7c").append(this.auratus.nextChainChargeIn(player)).append("s");
                    continue;
                }
                stringBuilder.append("\u00a7f").append(string3).append(" ");
                if (n > 0) {
                    stringBuilder.append("\u00a7c").append(n).append("s");
                    continue;
                }
                if (this.heretic.chargeWindowActive(player)) {
                    stringBuilder.append("\u00a7e1");
                    continue;
                }
                stringBuilder.append("\u00a7a2");
                continue;
            }
            stringBuilder.append(string2).append(cooldownEntry.getDisplayName()).append(" ");
            if (n > 0) {
                stringBuilder.append("\u00a7c").append(n).append("s");
                continue;
            }
            stringBuilder.append("\u00a7aReady");
        }
        return stringBuilder.toString();
    }
}

