package dev.xoperr.blissgems.api;

/**
 * How often a gem comes up in a random roll. The roll first picks a rarity by its weight
 * ({@code gems.rarity-weights} in config.yml), then a gem of that rarity uniformly.
 */
public enum GemRarity {
    COMMON,
    RARE,
    MYTHIC,
    LEGENDARY;

    public static GemRarity fromString(String name) {
        if (name == null) {
            return null;
        }
        try {
            return GemRarity.valueOf(name.trim().toUpperCase());
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }
}
