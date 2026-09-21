/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.io.ByteArrayDataInput
 *  com.google.common.io.ByteArrayDataOutput
 *  com.google.common.io.ByteStreams
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.messaging.PluginMessageListener
 */
package dev.xoperr.blissgems.managers;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.xoperr.blissgems.BlissGems;
import dev.xoperr.blissgems.utils.GemType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class PluginMessagingManager
implements PluginMessageListener {
    private final BlissGems plugin;
    public static final String ABILITY_CHANNEL = "blissgems:ability";
    public static final String GEM_DATA_CHANNEL = "blissgems:gemdata";

    public PluginMessagingManager(BlissGems plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerIncomingPluginChannel((Plugin)plugin, ABILITY_CHANNEL, (PluginMessageListener)this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel((Plugin)plugin, GEM_DATA_CHANNEL);
    }

    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(ABILITY_CHANNEL)) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput((byte[])message);
        String abilityType = in.readUTF();
        if (!this.plugin.getGemManager().hasActiveGem(player)) {
            player.sendMessage("\u00a7c\u00a7lYou don't have a gem!");
            return;
        }
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        if (energy <= 0) {
            String msg = this.plugin.getConfigManager().getFormattedMessage("no-energy", new Object[0]);
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
            return;
        }
        GemType gemType = this.plugin.getGemManager().getGemType(player);
        int tier = this.plugin.getGemManager().getGemTier(player);
        if (gemType == null) {
            return;
        }
        switch (abilityType) {
            case "main": {
                this.handleMainAbility(player, gemType, tier);
                break;
            }
            case "secondary": {
                if (tier < 2) {
                    player.sendMessage("\u00a7c\u00a7lSecondary abilities require Tier 2 gem!");
                    return;
                }
                this.handleSecondaryAbility(player, gemType, tier);
                break;
            }
            case "tertiary": {
                if (tier < 2) {
                    player.sendMessage("\u00a7c\u00a7lTertiary abilities require Tier 2 gem!");
                    return;
                }
                this.handleTertiaryAbility(player, gemType, tier);
            }
        }
    }

    private void handleMainAbility(Player player, GemType gemType, int tier) {
        switch (gemType) {
            case ASTRA: {
                this.plugin.getAstraAbilities().onRightClick(player, tier);
                break;
            }
            case FIRE: {
                this.plugin.getFireAbilities().onRightClick(player, tier);
                break;
            }
            case FLUX: {
                this.plugin.getFluxAbilities().onRightClick(player, tier);
                break;
            }
            case LIFE: {
                this.plugin.getLifeAbilities().onRightClick(player, tier);
                break;
            }
            case PUFF: {
                this.plugin.getPuffAbilities().onRightClick(player, tier);
                break;
            }
            case SPEED: {
                this.plugin.getSpeedAbilities().onRightClick(player, tier);
                break;
            }
            case STRENGTH: {
                this.plugin.getStrengthAbilities().onRightClick(player, tier);
                break;
            }
            case WEALTH: {
                this.plugin.getWealthAbilities().onRightClick(player, tier);
            }
        }
    }

    private void handleSecondaryAbility(Player player, GemType gemType, int tier) {
        switch (gemType) {
            case ASTRA: {
                this.plugin.getAstraAbilities().onRightClick(player, tier);
                break;
            }
            case FIRE: {
                this.plugin.getFireAbilities().onRightClick(player, tier);
                break;
            }
            case FLUX: {
                this.plugin.getFluxAbilities().onRightClick(player, tier);
                break;
            }
            case LIFE: {
                this.plugin.getLifeAbilities().onRightClick(player, tier);
                break;
            }
            case PUFF: {
                this.plugin.getPuffAbilities().onRightClick(player, tier);
                break;
            }
            case SPEED: {
                this.plugin.getSpeedAbilities().onRightClick(player, tier);
                break;
            }
            case STRENGTH: {
                this.plugin.getStrengthAbilities().onRightClick(player, tier);
                break;
            }
            case WEALTH: {
                this.plugin.getWealthAbilities().onRightClick(player, tier);
            }
        }
    }

    private void handleTertiaryAbility(Player player, GemType gemType, int tier) {
        switch (gemType) {
            case WEALTH: {
                this.plugin.getWealthAbilities().onRightClick(player, tier);
                break;
            }
            default: {
                player.sendMessage("\u00a7c\u00a7lThis gem doesn't have a tertiary ability!");
            }
        }
    }

    public void sendGemData(Player player) {
        if (!player.isOnline()) {
            return;
        }
        GemType gemType = this.plugin.getGemManager().getGemType(player);
        int tier = this.plugin.getGemManager().getGemTier(player);
        int energy = this.plugin.getEnergyManager().getEnergy(player);
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(gemType != null ? gemType.name() : "NONE");
        out.writeInt(tier);
        out.writeInt(energy);
        player.sendPluginMessage((Plugin)this.plugin, GEM_DATA_CHANNEL, out.toByteArray());
    }

    public void shutdown() {
        this.plugin.getServer().getMessenger().unregisterIncomingPluginChannel((Plugin)this.plugin, ABILITY_CHANNEL);
        this.plugin.getServer().getMessenger().unregisterOutgoingPluginChannel((Plugin)this.plugin, GEM_DATA_CHANNEL);
    }
}

