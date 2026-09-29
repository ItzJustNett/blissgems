/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 */
package dev.xoperr.blissgems.api;

import java.util.List;
import org.bukkit.Material;

public class GemDefinition {
    private final String id;
    private final String displayName;
    private final String description;
    private final String color;
    private final String pluginName;
    private final int maxTier;
    private final Material material;
    private final int t1CustomModelData;
    private final int t2CustomModelData;
    private final List<String> t1Lore;
    private final List<String> t2Lore;
    private final String t1DisplayName;
    private final String t2DisplayName;
    private final int t3CustomModelData;
    private final List<String> t3Lore;
    private final String t3DisplayName;
    private final GemRarity rarity;
    private final String mutationOf;

    private GemDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.color = builder.color;
        this.pluginName = builder.pluginName;
        this.maxTier = builder.maxTier;
        this.material = builder.material;
        this.t1CustomModelData = builder.t1CustomModelData;
        this.t2CustomModelData = builder.t2CustomModelData;
        this.t1Lore = builder.t1Lore;
        this.t2Lore = builder.t2Lore;
        this.t1DisplayName = builder.t1DisplayName;
        this.t2DisplayName = builder.t2DisplayName;
        this.t3CustomModelData = builder.t3CustomModelData;
        this.t3Lore = builder.t3Lore;
        this.t3DisplayName = builder.t3DisplayName;
        this.rarity = builder.rarity;
        this.mutationOf = builder.mutationOf;
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
        return this.color;
    }

    public String getPluginName() {
        return this.pluginName;
    }

    public int getMaxTier() {
        return this.maxTier;
    }

    public Material getMaterial() {
        return this.material;
    }

    public int getT1CustomModelData() {
        return this.t1CustomModelData;
    }

    public int getT2CustomModelData() {
        return this.t2CustomModelData;
    }

    public List<String> getT1Lore() {
        return this.t1Lore;
    }

    public List<String> getT2Lore() {
        return this.t2Lore;
    }

    public String getT1DisplayName() {
        return this.t1DisplayName;
    }

    public String getT2DisplayName() {
        return this.t2DisplayName;
    }

    /** Tier 3 model data; falls back to the Tier 2 value when unset. */
    public int getT3CustomModelData() {
        return this.t3CustomModelData > 0 ? this.t3CustomModelData : this.t2CustomModelData;
    }

    /** Tier 3 lore; falls back to the Tier 2 lore when unset. */
    public List<String> getT3Lore() {
        return this.t3Lore != null ? this.t3Lore : this.t2Lore;
    }

    /** Tier 3 display name; falls back to the Tier 2 name when unset. */
    public String getT3DisplayName() {
        return this.t3DisplayName != null ? this.t3DisplayName : this.t2DisplayName;
    }

    public GemRarity getRarity() {
        return this.rarity;
    }

    /** The base gem id this gem is a mutation of, or null for a normal gem. */
    public String getMutationOf() {
        return this.mutationOf;
    }

    public boolean isMutation() {
        return this.mutationOf != null;
    }

    public String buildItemId(int tier) {
        return this.id + "_gem_t" + tier;
    }

    public static class Builder {
        private final String id;
        private String displayName;
        private String description = "";
        private String color = "\u00a77";
        private String pluginName = "BlissGems";
        private int maxTier = 2;
        private Material material = Material.ECHO_SHARD;
        private int t1CustomModelData = -1;
        private int t2CustomModelData = -1;
        private List<String> t1Lore;
        private List<String> t2Lore;
        private String t1DisplayName;
        private String t2DisplayName;
        private int t3CustomModelData = -1;
        private List<String> t3Lore;
        private String t3DisplayName;
        private GemRarity rarity = GemRarity.COMMON;
        private String mutationOf;

        public Builder(String id) {
            this.id = id;
            this.displayName = id.substring(0, 1).toUpperCase() + id.substring(1);
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        public Builder plugin(String pluginName) {
            this.pluginName = pluginName;
            return this;
        }

        public Builder maxTier(int maxTier) {
            this.maxTier = maxTier;
            return this;
        }

        public Builder material(Material material) {
            this.material = material;
            return this;
        }

        public Builder t1CustomModelData(int cmd) {
            this.t1CustomModelData = cmd;
            return this;
        }

        public Builder t2CustomModelData(int cmd) {
            this.t2CustomModelData = cmd;
            return this;
        }

        public Builder t1Lore(List<String> lore) {
            this.t1Lore = lore;
            return this;
        }

        public Builder t2Lore(List<String> lore) {
            this.t2Lore = lore;
            return this;
        }

        public Builder t1DisplayName(String name) {
            this.t1DisplayName = name;
            return this;
        }

        public Builder t2DisplayName(String name) {
            this.t2DisplayName = name;
            return this;
        }

        public Builder t3CustomModelData(int cmd) {
            this.t3CustomModelData = cmd;
            return this;
        }

        public Builder t3Lore(List<String> lore) {
            this.t3Lore = lore;
            return this;
        }

        public Builder t3DisplayName(String name) {
            this.t3DisplayName = name;
            return this;
        }

        public Builder rarity(GemRarity rarity) {
            this.rarity = rarity != null ? rarity : GemRarity.COMMON;
            return this;
        }

        /**
         * Marks this gem as a mutation of another gem. Mutations never come up in a random roll
         * directly; a roll that lands on the base gem may mutate into one instead.
         */
        public Builder mutationOf(String baseGemId) {
            this.mutationOf = baseGemId;
            return this;
        }

        public GemDefinition build() {
            if (this.id == null || this.id.isEmpty()) {
                throw new IllegalArgumentException("Gem ID must not be empty");
            }
            return new GemDefinition(this);
        }
    }
}

