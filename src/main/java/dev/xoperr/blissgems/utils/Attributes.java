/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Registry
 *  org.bukkit.attribute.Attribute
 */
package dev.xoperr.blissgems.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;

public final class Attributes {
    private Attributes() {
    }

    public static Attribute maxHealth() {
        return Attributes.byKey("max_health", "generic.max_health");
    }

    public static Attribute attackSpeed() {
        return Attributes.byKey("attack_speed", "generic.attack_speed");
    }

    private static Attribute byKey(String ... keys) {
        for (String key : keys) {
            Attribute attribute = (Attribute)Registry.ATTRIBUTE.get(NamespacedKey.minecraft((String)key));
            if (attribute == null) continue;
            return attribute;
        }
        return null;
    }
}

