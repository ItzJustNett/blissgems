/*
 * Decompiled with CFR 0.152.
 */
package dev.xoperr.blissgems.utils;

public enum GemType {
    ASTRA("astra", "Astra", "Phase through attacks and teleport with astral daggers"),
    FIRE("fire", "Fire", "Burn your enemies with charged fireballs and cozy campfires"),
    FLUX("flux", "Flux", "Stun enemies and unleash electric power"),
    LIFE("life", "Life", "Heal yourself and drain the life from enemies"),
    PUFF("puff", "Puff", "Defy gravity with double jumps and immunity to fall damage"),
    SPEED("speed", "Speed", "Move faster and sedatate your foes"),
    STRENGTH("strength", "Strength", "Empower your attacks and weaken your enemies"),
    WEALTH("wealth", "Wealth", "Find fortune and unlock hidden riches");

    private final String id;
    private final String displayName;
    private final String description;

    private GemType(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getDescription() {
        return this.description;
    }

    public String getColor() {
        return switch (this) {
            case ASTRA -> "\u00a7d";    // Light Purple (Mystical/Ethereal)
            case FIRE -> "\u00a7c";     // Red (Fire/Burning)
            case FLUX -> "\u00a7b";     // Aqua (Electric/Energy)
            case LIFE -> "\u00a7a";     // Green (Nature/Healing)
            case PUFF -> "\u00a7f";     // White (Air/Clouds)
            case SPEED -> "\u00a7e";    // Yellow (Fast/Quick)
            case STRENGTH -> "\u00a74"; // Dark Red (Power/Strength)
            case WEALTH -> "\u00a76";   // Gold (Riches/Fortune)
        };
    }

    public static GemType fromOraxenId(String oraxenId) {
        if (oraxenId == null) {
            return null;
        }
        for (GemType type : GemType.values()) {
            if (!oraxenId.toLowerCase().startsWith(type.id + "_gem")) continue;
            return type;
        }
        return null;
    }

    public static int getTierFromOraxenId(String oraxenId) {
        if (oraxenId == null) {
            return 1;
        }
        if (oraxenId.endsWith("_t2")) {
            return 2;
        }
        return 1;
    }

    public static String buildOraxenId(GemType type, int tier) {
        return type.getId() + "_gem_t" + tier;
    }

    public static boolean isGem(String oraxenId) {
        return oraxenId != null && oraxenId.contains("_gem_t");
    }
}

