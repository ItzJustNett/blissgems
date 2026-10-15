package dev.xoperr.blissgems.utils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public final class TrueDamage {
    private TrueDamage() {
    }

    /**
     * Armor-ignoring damage. A blow that would kill goes through the normal damage pipeline
     * instead, so totems, death events and armor still apply to the killing hit.
     */
    public static void apply(LivingEntity target, double amount, Entity source) {
        double health = target.getHealth();
        if (health - amount > 0.0) {
            target.setHealth(health - amount);
            return;
        }
        target.setNoDamageTicks(0);
        target.damage(amount, source);
    }
}
