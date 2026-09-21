/*
 * Decompiled with CFR 0.152.
 */
package dev.xoperr.blissgems.utils;

public enum AbilityBinding {
    RIGHT_CLICK("right_click", "Right Click", false),
    SHIFT_RIGHT_CLICK("shift_right_click", "Shift + Right Click", true),
    LEFT_CLICK("left_click", "Left Click", false),
    SHIFT_LEFT_CLICK("shift_left_click", "Shift + Left Click", true),
    SWAP_HAND("swap_hand", "F (Swap Offhand)", false),
    SHIFT_SWAP_HAND("shift_swap_hand", "Shift + F", true);

    private final String id;
    private final String displayName;
    private final boolean requiresSneak;

    private AbilityBinding(String id, String displayName, boolean requiresSneak) {
        this.id = id;
        this.displayName = displayName;
        this.requiresSneak = requiresSneak;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public boolean requiresSneak() {
        return this.requiresSneak;
    }

    public static AbilityBinding fromId(String id) {
        if (id == null) {
            return null;
        }
        String lower = id.toLowerCase().replace('-', '_');
        for (AbilityBinding b : AbilityBinding.values()) {
            if (!b.id.equals(lower)) continue;
            return b;
        }
        return null;
    }

    public static AbilityBinding rightClick(boolean sneaking) {
        return sneaking ? SHIFT_RIGHT_CLICK : RIGHT_CLICK;
    }

    public static AbilityBinding leftClick(boolean sneaking) {
        return sneaking ? SHIFT_LEFT_CLICK : LEFT_CLICK;
    }

    public static AbilityBinding swapHand(boolean sneaking) {
        return sneaking ? SHIFT_SWAP_HAND : SWAP_HAND;
    }
}

