/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.comphenix.protocol.PacketType$Play$Server
 *  com.comphenix.protocol.ProtocolLibrary
 *  com.comphenix.protocol.ProtocolManager
 *  com.comphenix.protocol.events.PacketContainer
 *  com.comphenix.protocol.wrappers.EnumWrappers$NativeGameMode
 *  com.comphenix.protocol.wrappers.EnumWrappers$PlayerInfoAction
 *  com.comphenix.protocol.wrappers.PlayerInfoData
 *  com.comphenix.protocol.wrappers.WrappedChatComponent
 *  com.comphenix.protocol.wrappers.WrappedGameProfile
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.entity.ArmorStand
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.profile.PlayerProfile
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.util.EulerAngle
 */
package dev.xoperr.blissgems.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

public final class PlayerCloneNPC {
    private static final boolean PROTOCOL_LIB_PRESENT = PlayerCloneNPC.detectProtocolLib();

    private PlayerCloneNPC() {
    }

    public static boolean isAvailable() {
        return PROTOCOL_LIB_PRESENT;
    }

    private static boolean detectProtocolLib() {
        try {
            Class.forName("com.comphenix.protocol.ProtocolLibrary");
            return Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;
        }
        catch (Throwable t) {
            return false;
        }
    }

    public static void play(Plugin plugin, Player owner, Location loc, long windupTicks, long lifetimeTicks) {
        if (PROTOCOL_LIB_PRESENT) {
            try {
                PlayerCloneNPC.playPacketClone(plugin, owner, loc, windupTicks, lifetimeTicks);
                return;
            }
            catch (Throwable t) {
                plugin.getLogger().warning("[PlayerCloneNPC] Packet clone failed (" + String.valueOf(t) + "), falling back to armor stand clone.");
            }
        }
        PlayerCloneNPC.playArmorStandClone(plugin, owner, loc, windupTicks, lifetimeTicks);
    }

    private static void playPacketClone(Plugin plugin, Player owner, Location loc, long windupTicks, long lifetimeTicks) {
        final ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        final UUID npcId = UUID.randomUUID();
        WrappedGameProfile ownerProfile = WrappedGameProfile.fromPlayer((Player)owner);
        WrappedGameProfile profile = new WrappedGameProfile(npcId, owner.getName());
        profile.getProperties().putAll(ownerProfile.getProperties());
        final int entityId = ThreadLocalRandom.current().nextInt(0x3FFFFFFF, Integer.MAX_VALUE);
        final List<Player> viewers = PlayerCloneNPC.onlinePlayersNear(loc, 48.0);
        if (viewers.isEmpty()) {
            return;
        }
        PacketContainer addInfo = pm.createPacket(PacketType.Play.Server.PLAYER_INFO);
        addInfo.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        PlayerInfoData data = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText((String)owner.getName()));
        addInfo.getPlayerInfoDataLists().write(1, Collections.singletonList(data));
        PacketContainer spawn = pm.createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
        spawn.getIntegers().write(0, entityId);
        spawn.getUUIDs().write(0, npcId);
        spawn.getDoubles().write(0, loc.getX()).write(1, loc.getY()).write(2, loc.getZ());
        spawn.getBytes().write(0, (byte)(loc.getYaw() * 256.0f / 360.0f));
        spawn.getBytes().write(1, (byte)(loc.getPitch() * 256.0f / 360.0f));
        for (Player viewer : viewers) {
            PlayerCloneNPC.sendSafely(pm, viewer, addInfo);
            PlayerCloneNPC.sendSafely(pm, viewer, spawn);
        }
        new BukkitRunnable(){

            public void run() {
                PacketContainer remove = pm.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
                remove.getUUIDLists().write(0, Collections.singletonList(npcId));
                for (Player viewer : viewers) {
                    if (!viewer.isOnline()) continue;
                    PlayerCloneNPC.sendSafely(pm, viewer, remove);
                }
            }
        }.runTaskLater(plugin, 5L);
        new BukkitRunnable(){

            public void run() {
                PacketContainer animation = pm.createPacket(PacketType.Play.Server.ANIMATION);
                animation.getIntegers().write(0, entityId);
                animation.getIntegers().write(1, 0);
                for (Player viewer : viewers) {
                    if (!viewer.isOnline()) continue;
                    PlayerCloneNPC.sendSafely(pm, viewer, animation);
                }
            }
        }.runTaskLater(plugin, windupTicks);
        new BukkitRunnable(){

            public void run() {
                PacketContainer destroy = pm.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                destroy.getIntLists().write(0, Collections.singletonList(entityId));
                for (Player viewer : viewers) {
                    if (!viewer.isOnline()) continue;
                    PlayerCloneNPC.sendSafely(pm, viewer, destroy);
                }
            }
        }.runTaskLater(plugin, lifetimeTicks);
    }

    private static void sendSafely(ProtocolManager pm, Player viewer, PacketContainer packet) {
        try {
            pm.sendServerPacket(viewer, packet);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private static List<Player> onlinePlayersNear(Location loc, double radius) {
        World world = loc.getWorld();
        if (world == null) {
            return Collections.emptyList();
        }
        double radiusSq = radius * radius;
        ArrayList<Player> result = new ArrayList<Player>();
        for (Player p : world.getPlayers()) {
            if (!(p.getLocation().distanceSquared(loc) <= radiusSq)) continue;
            result.add(p);
        }
        return result;
    }

    private static void playArmorStandClone(Plugin plugin, Player owner, Location loc, long windupTicks, long lifetimeTicks) {
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        Location standLoc = loc.clone();
        standLoc.setYaw(owner.getLocation().getYaw());
        standLoc.setPitch(0.0f);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta)head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwnerProfile((PlayerProfile)owner.getPlayerProfile());
            head.setItemMeta((ItemMeta)skullMeta);
        }
        final ArmorStand clone = (ArmorStand)world.spawn(standLoc, ArmorStand.class, stand -> {
            stand.setInvulnerable(true);
            stand.setBasePlate(false);
            stand.setArms(true);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setSilent(true);
            stand.setPersistent(false);
            stand.setCustomName(owner.getName());
            if (stand.getEquipment() != null) {
                stand.getEquipment().setHelmet(head);
                stand.getEquipment().setChestplate(owner.getInventory().getChestplate());
                stand.getEquipment().setLeggings(owner.getInventory().getLeggings());
                stand.getEquipment().setBoots(owner.getInventory().getBoots());
                stand.getEquipment().setItemInMainHand(owner.getInventory().getItemInMainHand());
            }
        });
        new BukkitRunnable(){

            public void run() {
                if (clone.isValid()) {
                    clone.setRightArmPose(new EulerAngle(Math.toRadians(-150.0), 0.0, 0.0));
                }
            }
        }.runTaskLater(plugin, windupTicks);
        new BukkitRunnable(){

            public void run() {
                if (clone.isValid()) {
                    clone.remove();
                }
            }
        }.runTaskLater(plugin, lifetimeTicks);
    }
}

