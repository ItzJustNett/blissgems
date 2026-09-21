/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatMessageType
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package dev.xoperr.blissgems.core.managers;

import dev.xoperr.blissgems.core.api.text.InventoryTextProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class TextManager {
    private final Plugin plugin;
    private final List<InventoryTextProvider> textProviders;
    private BukkitTask updateTask;

    public TextManager(Plugin plugin) {
        this.plugin = plugin;
        this.textProviders = new ArrayList<InventoryTextProvider>();
        this.startUpdateTask();
    }

    public void sendActionBar(Player player, String message) {
        if (player == null || message == null) {
            return;
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, (BaseComponent)new TextComponent(message));
    }

    public void clearActionBar(Player player) {
        if (player == null) {
            return;
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, (BaseComponent)new TextComponent(""));
    }

    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null || title == null) {
            return;
        }
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    public void clearTitle(Player player) {
        if (player == null) {
            return;
        }
        player.resetTitle();
    }

    public void registerTextProvider(InventoryTextProvider provider) {
        if (provider == null) {
            return;
        }
        this.textProviders.add(provider);
        this.textProviders.sort(Comparator.comparingInt(InventoryTextProvider::getPriority).reversed());
    }

    public void unregisterTextProvider(InventoryTextProvider provider) {
        this.textProviders.remove(provider);
    }

    private void startUpdateTask() {
        this.updateTask = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, this::updateAllPlayers, 0L, 5L);
    }

    private void updateAllPlayers() {
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            this.updatePlayer(player);
        }
    }

    private void updatePlayer(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        for (InventoryTextProvider provider : this.textProviders) {
            String text;
            if (!provider.shouldUpdate(player, mainHand, offHand) || (text = provider.getActionBarText(player, mainHand, offHand)) == null) continue;
            this.sendActionBar(player, text);
            return;
        }
    }

    public void cleanup() {
        if (this.updateTask != null) {
            this.updateTask.cancel();
        }
        this.textProviders.clear();
    }
}

