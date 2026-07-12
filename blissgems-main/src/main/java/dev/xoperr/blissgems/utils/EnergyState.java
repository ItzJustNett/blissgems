/*
 * Decompiled with CFR 0.152.
 */
package dev.xoperr.blissgems.utils;

public enum EnergyState {
    BROKEN(0, 0, "Broken", "\u00a7c\u00a7lBROKEN"),
    RUINED(1, 1, "Ruined", "\u00a74Ruined"),
    SHATTERED(2, 2, "Shattered", "\u00a76Shattered"),
    CRACKED(3, 3, "Cracked", "\u00a7e\u03a9Cracked"),
    SCRATCHED(4, 4, "Scratched", "\u00a7aS\u03baratched"),
    PRISTINE(5, 5, "Pristine", "\u00a7bPristine"),
    PRISTINE_PLUS_1(6, 6, "Pristine +1", "\u00a7d\u00a7lPristine \u00a75+1"),
    PRISTINE_PLUS_2(7, 7, "Pristine +2", "\u00a7d\u00a7lPristine \u00a75+2"),
    PRISTINE_PLUS_3(8, 8, "Pristine +3", "\u00a7d\u00a7lPristine \u00a75+3"),
    PRISTINE_PLUS_4(9, 9, "Pristine +4", "\u00a7d\u00a7lPristine \u00a75+4"),
    PRISTINE_PLUS_5(10, 10, "Pristine +5", "\u00a7d\u00a7lPristine \u00a75+5");

    private final int minEnergy;
    private final int maxEnergy;
    private final String name;
    private final String displayName;

    private EnergyState(int minEnergy, int maxEnergy, String name, String displayName) {
        this.minEnergy = minEnergy;
        this.maxEnergy = maxEnergy;
        this.name = name;
        this.displayName = displayName;
    }

    public int getMinEnergy() {
        return this.minEnergy;
    }

    public int getMaxEnergy() {
        return this.maxEnergy;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public static EnergyState fromEnergy(int energy) {
        energy = Math.max(0, Math.min(10, energy));
        for (EnergyState state : EnergyState.values()) {
            if (energy < state.minEnergy || energy > state.maxEnergy) continue;
            return state;
        }
        return BROKEN;
    }

    public boolean passivesActive() {
        return this.minEnergy > 1;
    }

    public boolean abilitiesUsable() {
        return this.minEnergy >= 2; // Changed: Now requires 2+ energy (was 1+)
    }

    public boolean isEnhanced() {
        return this.minEnergy > 5;
    }

    public boolean isMaxEnergy() {
        return this == PRISTINE_PLUS_5;
    }
}

