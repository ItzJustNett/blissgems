package dev.xoperr.blissgems.managers;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Handles plugin messaging for Fabric client mod communication
 */
public class PluginMessagingManager implements PluginMessageListener {
    private final BlissGems plugin;

    // Channel identifiers (must match client-side)
    public static final String ABILITY_CHANNEL = "blissgems:ability";
    public static final String GEM_DATA_CHANNEL = "blissgems:gemdata";

    public PluginMessagingManager(BlissGems plugin) {
        this.plugin = plugin;

        // Register incoming message channel
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, ABILITY_CHANNEL, this);

        // Register outgoing message channel
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, GEM_DATA_CHANNEL);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(ABILITY_CHANNEL)) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String abilityType = in.readUTF(); // "main", "secondary", or "tertiary"

        // Check if player has a gem
        if (!plugin.getGemManager().hasActiveGem(player)) {
            player.sendMessage("§c§lYou don't have a gem!");
            return;
        }

        // Check energy
        int energy = plugin.getEnergyManager().getEnergy(player);
        if (energy <= 0) {
            String msg = plugin.getConfigManager().getFormattedMessage("no-energy");
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }

        GemType gemType = plugin.getGemManager().getGemType(player);
        int tier = plugin.getGemManager().getGemTier(player);

        if (gemType == null) {
            return;
        }

        // Route to appropriate ability handler based on ability type
        switch (abilityType) {
            case "main":
                handleMainAbility(player, gemType, tier);
                break;
            case "secondary":
                if (tier < 2) {
                    player.sendMessage("§c§lSecondary abilities require Tier 2 gem!");
                    return;
                }
                handleSecondaryAbility(player, gemType, tier);
                break;
            case "tertiary":
                if (tier < 2) {
                    player.sendMessage("§c§lTertiary abilities require Tier 2 gem!");
                    return;
                }
                handleTertiaryAbility(player, gemType, tier);
                break;
        }
    }

    /**
     * Handle main ability activation
     */
    private void handleMainAbility(Player player, GemType gemType, int tier) {
        switch (gemType) {
            case ASTRA:
                plugin.getAstraAbilities().onRightClick(player, tier);
                break;
            case FIRE:
                plugin.getFireAbilities().onRightClick(player, tier);
                break;
            case FLUX:
                plugin.getFluxAbilities().onRightClick(player, tier);
                break;
            case LIFE:
                plugin.getLifeAbilities().onRightClick(player, tier);
                break;
            case PUFF:
                plugin.getPuffAbilities().onRightClick(player, tier);
                break;
            case SPEED:
                plugin.getSpeedAbilities().onRightClick(player, tier);
                break;
            case STRENGTH:
                plugin.getStrengthAbilities().onRightClick(player, tier);
                break;
            case WEALTH:
                plugin.getWealthAbilities().onRightClick(player, tier);
                break;
        }
    }

    /**
     * Handle secondary ability activation (shift + right-click)
     */
    private void handleSecondaryAbility(Player player, GemType gemType, int tier) {
        // Most gems use shift detection in onRightClick, so we call the same method
        // The ability classes check player.isSneaking() internally
        // For keybind support, we need to temporarily set sneaking or refactor ability methods

        // For now, tell player to shift-click or use the right-click method
        // TODO: Refactor ability classes to accept explicit ability index
        switch (gemType) {
            case ASTRA:
                plugin.getAstraAbilities().onRightClick(player, tier);
                break;
            case FIRE:
                plugin.getFireAbilities().onRightClick(player, tier);
                break;
            case FLUX:
                plugin.getFluxAbilities().onRightClick(player, tier);
                break;
            case LIFE:
                plugin.getLifeAbilities().onRightClick(player, tier);
                break;
            case PUFF:
                plugin.getPuffAbilities().onRightClick(player, tier);
                break;
            case SPEED:
                plugin.getSpeedAbilities().onRightClick(player, tier);
                break;
            case STRENGTH:
                plugin.getStrengthAbilities().onRightClick(player, tier);
                break;
            case WEALTH:
                plugin.getWealthAbilities().onRightClick(player, tier);
                break;
        }
    }

    /**
     * Handle tertiary ability activation (for gems with multiple T2 abilities)
     */
    private void handleTertiaryAbility(Player player, GemType gemType, int tier) {
        // This is for gems that have 3+ abilities at T2
        // Currently most gems have 2 abilities (main + secondary)
        // Wealth gem has multiple abilities - this could route to specific ones

        switch (gemType) {
            case WEALTH:
                // Wealth has multiple abilities: Pockets, Unfortunate, Rich Rush, etc.
                // For now, use the same handler
                plugin.getWealthAbilities().onRightClick(player, tier);
                break;
            default:
                player.sendMessage("§c§lThis gem doesn't have a tertiary ability!");
                break;
        }
    }

    /**
     * Send gem data to client (call when player joins, gem changes, or energy changes)
     */
    public void sendGemData(Player player) {
        if (!player.isOnline()) {
            return;
        }

        GemType gemType = plugin.getGemManager().getGemType(player);
        int tier = plugin.getGemManager().getGemTier(player);
        int energy = plugin.getEnergyManager().getEnergy(player);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(gemType != null ? gemType.name() : "NONE");
        out.writeInt(tier);
        out.writeInt(energy);

        player.sendPluginMessage(plugin, GEM_DATA_CHANNEL, out.toByteArray());
    }

    /**
     * Cleanup on disable
     */
    public void shutdown() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, ABILITY_CHANNEL);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, GEM_DATA_CHANNEL);
    }
}
