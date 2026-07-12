package dev.xoperr.blissgems.api;

/**
 * Describes a single cooldown that should be tracked and displayed
 * in the action bar for a gem type.
 */
public class CooldownEntry {
    private final String abilityKey;
    private final String displayName;

    /**
     * @param abilityKey The key used with AbilityManager (e.g. "ice-freeze")
     * @param displayName Short name shown in cooldown display (e.g. "Freeze")
     */
    public CooldownEntry(String abilityKey, String displayName) {
        this.abilityKey = abilityKey;
        this.displayName = displayName;
    }

    public String getAbilityKey() {
        return abilityKey;
    }

    public String getDisplayName() {
        return displayName;
    }
}
