package dev.xoperr.blissgems.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

/** Fired before a donor gem is imbued into a Tier 3 gem. Cancelling keeps both items unchanged. */
public class GemImbueEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ItemStack gemItem;
    private final String gemId;
    private final String imbuedGemId;
    private final String previousImbuedGemId;
    private boolean cancelled;

    public GemImbueEvent(Player player, ItemStack gemItem, String gemId, String imbuedGemId, String previousImbuedGemId) {
        super(player);
        this.gemItem = gemItem;
        this.gemId = gemId;
        this.imbuedGemId = imbuedGemId;
        this.previousImbuedGemId = previousImbuedGemId;
    }

    public ItemStack getGemItem() {
        return this.gemItem;
    }

    public String getGemId() {
        return this.gemId;
    }

    public String getImbuedGemId() {
        return this.imbuedGemId;
    }

    /** The imbue this one replaces, or null. */
    public String getPreviousImbuedGemId() {
        return this.previousImbuedGemId;
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
}
