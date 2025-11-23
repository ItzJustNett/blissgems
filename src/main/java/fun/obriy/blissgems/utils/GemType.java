/*
 * Decompiled with CFR 0.152.
 */
package fun.obriy.blissgems.utils;

public enum GemType {
    ASTRA("astra", "Astra"),
    FIRE("fire", "Fire"),
    FLUX("flux", "Flux"),
    LIFE("life", "Life"),
    PUFF("puff", "Puff"),
    SPEED("speed", "Speed"),
    STRENGTH("strength", "Strength"),
    WEALTH("wealth", "Wealth");

    private final String id;
    private final String displayName;

    private GemType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
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

