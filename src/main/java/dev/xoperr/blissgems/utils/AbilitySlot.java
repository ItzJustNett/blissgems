package dev.xoperr.blissgems.utils;

/**
 * The six ability slots a gem can expose. Slot is gem-defined (Primary, Secondary,
 * Tertiary, Quaternary, Quinary, Senary); the input that triggers each slot is
 * player-configurable via AbilityBinding.
 *
 * Normal gems only define four. The last two exist for the Gold Gem, which needs six
 * inputs to reach a harvested soul's full set of four abilities alongside its own two.
 */
public enum AbilitySlot {
    PRIMARY("primary", "Primary"),
    SECONDARY("secondary", "Secondary"),
    TERTIARY("tertiary", "Tertiary"),
    QUATERNARY("quaternary", "Quaternary"),
    QUINARY("quinary", "Quinary"),
    SENARY("senary", "Senary");

    private final String id;
    private final String displayName;

    AbilitySlot(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AbilitySlot fromId(String id) {
        if (id == null) return null;
        String lower = id.toLowerCase();
        for (AbilitySlot s : values()) {
            if (s.id.equals(lower)) return s;
        }
        if (lower.equals("main")) return PRIMARY; // friendly alias
        return null;
    }
}
