package dev.xoperr.blissgems.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Spawns a short-lived visual clone of a player standing at a location.
 *
 * When ProtocolLib is installed, the clone is a packet-only fake player carrying the real
 * player's skin (full body, not just the head) - nothing but cosmetic packets, no server-side
 * entity, so it needs no cleanup beyond its own destroy packet. Without ProtocolLib, it falls
 * back to an ArmorStand wearing the player's head as a skull.
 */
public final class PlayerCloneNPC {

    private static final boolean PROTOCOL_LIB_PRESENT = detectProtocolLib();

    private PlayerCloneNPC() {
    }

    public static boolean isAvailable() {
        return PROTOCOL_LIB_PRESENT;
    }

    private static boolean detectProtocolLib() {
        try {
            Class.forName("com.comphenix.protocol.ProtocolLibrary");
            return Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Spawns a clone of {@code owner} standing at {@code loc}. It raises its arm in a swing
     * at {@code windupTicks} and disappears at {@code lifetimeTicks}.
     */
    public static void play(Plugin plugin, Player owner, Location loc, long windupTicks, long lifetimeTicks) {
        if (PROTOCOL_LIB_PRESENT) {
            try {
                playPacketClone(plugin, owner, loc, windupTicks, lifetimeTicks);
                return;
            } catch (Throwable t) {
                plugin.getLogger().warning("[PlayerCloneNPC] Packet clone failed (" + t
                    + "), falling back to armor stand clone.");
            }
        }
        playArmorStandClone(plugin, owner, loc, windupTicks, lifetimeTicks);
    }

    // ================= Packet-based full body clone (requires ProtocolLib) =================

    private static void playPacketClone(Plugin plugin, Player owner, Location loc, long windupTicks, long lifetimeTicks) {
        ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        UUID npcId = UUID.randomUUID();
        WrappedGameProfile ownerProfile = WrappedGameProfile.fromPlayer(owner);
        WrappedGameProfile profile = new WrappedGameProfile(npcId, owner.getName());
        profile.getProperties().putAll(ownerProfile.getProperties());

        int entityId = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE / 2, Integer.MAX_VALUE);
        List<Player> viewers = onlinePlayersNear(loc, 48);
        if (viewers.isEmpty()) {
            return;
        }

        // The client only resolves a skin for entities that have (or recently had) a tab-list
        // entry, so the fake profile needs to be added just long enough for that to happen.
        PacketContainer addInfo = pm.createPacket(PacketType.Play.Server.PLAYER_INFO);
        addInfo.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        PlayerInfoData data = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL,
            WrappedChatComponent.fromText(owner.getName()));
        addInfo.getPlayerInfoDataLists().write(1, Collections.singletonList(data));

        PacketContainer spawn = pm.createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
        spawn.getIntegers().write(0, entityId);
        spawn.getUUIDs().write(0, npcId);
        spawn.getDoubles().write(0, loc.getX()).write(1, loc.getY()).write(2, loc.getZ());
        spawn.getBytes().write(0, (byte) (loc.getYaw() * 256.0F / 360.0F));
        spawn.getBytes().write(1, (byte) (loc.getPitch() * 256.0F / 360.0F));

        for (Player viewer : viewers) {
            sendSafely(pm, viewer, addInfo);
            sendSafely(pm, viewer, spawn);
        }

        // Tab list entry is only needed for the initial skin resolve - drop it a moment later.
        new BukkitRunnable() {
            @Override
            public void run() {
                PacketContainer remove = pm.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
                remove.getUUIDLists().write(0, Collections.singletonList(npcId));
                for (Player viewer : viewers) {
                    if (viewer.isOnline()) {
                        sendSafely(pm, viewer, remove);
                    }
                }
            }
        }.runTaskLater(plugin, 5L);

        new BukkitRunnable() {
            @Override
            public void run() {
                PacketContainer animation = pm.createPacket(PacketType.Play.Server.ANIMATION);
                animation.getIntegers().write(0, entityId);
                animation.getIntegers().write(1, 0); // 0 = swing main arm
                for (Player viewer : viewers) {
                    if (viewer.isOnline()) {
                        sendSafely(pm, viewer, animation);
                    }
                }
            }
        }.runTaskLater(plugin, windupTicks);

        new BukkitRunnable() {
            @Override
            public void run() {
                PacketContainer destroy = pm.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                destroy.getIntLists().write(0, Collections.singletonList(entityId));
                for (Player viewer : viewers) {
                    if (viewer.isOnline()) {
                        sendSafely(pm, viewer, destroy);
                    }
                }
            }
        }.runTaskLater(plugin, lifetimeTicks);
    }

    private static void sendSafely(ProtocolManager pm, Player viewer, PacketContainer packet) {
        try {
            pm.sendServerPacket(viewer, packet);
        } catch (Throwable ignored) {
            // A single viewer failing to receive a cosmetic packet shouldn't break the clone
            // for everyone else.
        }
    }

    private static List<Player> onlinePlayersNear(Location loc, double radius) {
        World world = loc.getWorld();
        if (world == null) return Collections.emptyList();
        double radiusSq = radius * radius;
        List<Player> result = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= radiusSq) {
                result.add(p);
            }
        }
        return result;
    }

    // ================= ArmorStand fallback (no ProtocolLib) =================

    private static void playArmorStandClone(Plugin plugin, Player owner, Location loc, long windupTicks, long lifetimeTicks) {
        World world = loc.getWorld();
        if (world == null) return;

        Location standLoc = loc.clone();
        standLoc.setYaw(owner.getLocation().getYaw());
        standLoc.setPitch(0.0f);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            // The full PlayerProfile carries the skin texture property directly, unlike
            // setOwningPlayer(player), which can silently fail to resolve a texture for a
            // profile the client hasn't already cached.
            skullMeta.setOwnerProfile(owner.getPlayerProfile());
            head.setItemMeta(skullMeta);
        }

        ArmorStand clone = world.spawn(standLoc, ArmorStand.class, stand -> {
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

        new BukkitRunnable() {
            @Override
            public void run() {
                if (clone.isValid()) {
                    clone.setRightArmPose(new EulerAngle(Math.toRadians(-150.0), 0.0, 0.0));
                }
            }
        }.runTaskLater(plugin, windupTicks);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (clone.isValid()) {
                    clone.remove();
                }
            }
        }.runTaskLater(plugin, lifetimeTicks);
    }
}
