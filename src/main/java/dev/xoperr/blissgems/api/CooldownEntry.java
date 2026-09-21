/*
 * Decompiled with CFR 0.152.
 */
package dev.xoperr.blissgems.api;

public class CooldownEntry {
    private final String abilityKey;
    private final String displayName;

    public CooldownEntry(String abilityKey, String displayName) {
        this.abilityKey = abilityKey;
        this.displayName = displayName;
    }

    public String getAbilityKey() {
        return this.abilityKey;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}

