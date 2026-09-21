/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package dev.xoperr.blissgems.api;

import org.bukkit.entity.Player;

public interface GemAbilityHandler {
    public void onPrimary(Player var1, int var2);

    default public void onSecondary(Player player, int tier) {
    }

    default public void onTertiary(Player player, int tier) {
    }

    default public void onQuaternary(Player player, int tier) {
    }

    default public void onQuinary(Player player, int tier) {
    }

    default public void onSenary(Player player, int tier) {
    }

    default public void cleanup(Player player) {
    }
}

