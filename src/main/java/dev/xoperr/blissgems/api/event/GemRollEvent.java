package dev.xoperr.blissgems.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Fired whenever a random gem is rolled for a player (first gem, reroll, trader, restoration,
 * Tier 3 bonus roll). Change the result with {@link #setGemId(String)}; cancelling means no gem
 * is rolled and the caller treats it as "nothing available".
 */
public class GemRollEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Reason reason;
    private final int tier;
    private final boolean mutated;
    private String gemId;
    private boolean cancelled;

    public GemRollEvent(Player player, Reason reason, String gemId, int tier, boolean mutated) {
        super(player);
        this.reason = reason;
        this.gemId = gemId;
        this.tier = tier;
        this.mutated = mutated;
    }

    public Reason getReason() {
        return this.reason;
    }

    public String getGemId() {
        return this.gemId;
    }

    public void setGemId(String gemId) {
        this.gemId = gemId;
    }

    public int getTier() {
        return this.tier;
    }

    /** Whether the roll mutated into a mutation gem (as rolled, before listeners change the id). */
    public boolean isMutated() {
        return this.mutated;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public enum Reason {
        FIRST_GEM,
        REROLL,
        TRADER,
        RESTORATION,
        TIER3_BONUS,
        OTHER
    }
}
