/*
 * Decompiled with CFR 0.152.
 */
package dev.xoperr.blissgems.utils;

public enum AbilitySlot {
    PRIMARY("primary", "Primary"),
    SECONDARY("secondary", "Secondary"),
    TERTIARY("tertiary", "Tertiary"),
    QUATERNARY("quaternary", "Quaternary"),
    QUINARY("quinary", "Quinary"),
    SENARY("senary", "Senary");

    private final String id;
    private final String displayName;

    private AbilitySlot(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public static AbilitySlot fromId(String id) {
        if (id == null) {
            return null;
        }
        String lower = id.toLowerCase();
        for (AbilitySlot s : AbilitySlot.values()) {
            if (!s.id.equals(lower)) continue;
            return s;
        }
        if (lower.equals("main")) {
            return PRIMARY;
        }
        return null;
    }
}

