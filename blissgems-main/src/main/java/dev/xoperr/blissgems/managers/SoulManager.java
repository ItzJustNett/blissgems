package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.*;

/**
 * Manages the Astra Gem soul system:
 * - Soul Absorption: Temporarily add extra max hearts on kill
 * - Soul Capture: Capture up to 2 mobs in the gem and release them
 */
public class SoulManager {
    private final BlissGems plugin;
    private final NamespacedKey capturedMobKey;
    private final Map<UUID, List<CapturedMob>> playerSouls = new HashMap<>();
    private static final int MAX_CAPTURED_SOULS = 2;
    private static final String SOUL_MODIFIER_PREFIX = "soul_absorb_";

    // Track active soul absorption modifiers per player (for stacking/cleanup)
    private int soulModifierCounter = 0;
    private final Map<UUID, List<UUID>> activeSoulModifiers = new HashMap<>();

    public SoulManager(BlissGems plugin) {
        this.plugin = plugin;
        this.capturedMobKey = new NamespacedKey(plugin, "captured_mob");
    }

    /**
     * Called when a player with Astra gem kills an entity
     * Temporarily adds extra max hearts based on what was killed
     */
    public void absorbSoul(Player player, Entity killedEntity) {
        boolean isPlayer = killedEntity instanceof Player;
        double bonusHealth;

        if (isPlayer) {
            bonusHealth = plugin.getConfig().getDouble("abilities.soul-absorption.player-kill-hearts", 10.0); // 5 hearts
        } else {
            bonusHealth = plugin.getConfig().getDouble("abilities.soul-absorption.mob-kill-hearts", 4.0); // 2 hearts
        }

        int durationSeconds = plugin.getConfig().getInt("abilities.soul-absorption.duration", 60);

        // Add temporary max health via attribute modifier
        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr == null) return;

        UUID modifierUuid = UUID.randomUUID();
        NamespacedKey modifierKey = new NamespacedKey(plugin, "soul-absorb-" + modifierUuid.toString().replace("-", ""));
        soulModifierCounter++;
        AttributeModifier modifier = new AttributeModifier(modifierUuid, modifierKey.toString(), bonusHealth, AttributeModifier.Operation.ADD_NUMBER);
        maxHealthAttr.addModifier(modifier);
        activeSoulModifiers.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(modifierUuid);
        persistModifierUuid(player.getUniqueId(), modifierUuid);

        // Schedule removal after duration
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                maxHealthAttr.removeModifier(modifier);
                double newMax = maxHealthAttr.getValue();
                if (player.getHealth() > newMax) {
                    player.setHealth(newMax);
                }
            }
            List<UUID> tracked = activeSoulModifiers.get(player.getUniqueId());
            if (tracked != null) {
                tracked.remove(modifierUuid);
                if (tracked.isEmpty()) activeSoulModifiers.remove(player.getUniqueId());
            }
            unpersistModifierUuid(player.getUniqueId(), modifierUuid);
        }, durationSeconds * 20L);

        // Visual and sound effects
        player.getWorld().spawnParticle(Particle.SOUL,
            player.getLocation().add(0, 1, 0),
            15, 0.5, 0.5, 0.5, 0.05);
        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
            player.getLocation().add(0, 1, 0),
            10, 0.5, 0.5, 0.5, 0);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_SOUL_SAND_BREAK, 1.0f, 1.5f);

        String entityName = isPlayer ? ((Player) killedEntity).getName() :
            killedEntity.getType().toString().toLowerCase().replace("_", " ");
        player.sendMessage("§d§oAbsorbed soul from " + entityName + "! +" + (bonusHealth / 2) + " hearts for " + durationSeconds + "s");
    }

    /**
     * Attempts to capture a mob into the player's gem
     * Returns true if successful
     */
    public boolean captureMob(Player player, LivingEntity mob) {
        // Check if mob is a player (can't capture players)
        if (mob instanceof Player) {
            player.sendMessage("§c§oCannot capture players!");
            return false;
        }

        // Get current captured souls
        List<CapturedMob> souls = playerSouls.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());

        // Check if at max capacity
        if (souls.size() >= MAX_CAPTURED_SOULS) {
            player.sendMessage("§c§oYour gem is full! (Max " + MAX_CAPTURED_SOULS + " souls)");
            return false;
        }

        // Check if mob is capturable (some entities shouldn't be captured)
        if (!isCapturable(mob)) {
            player.sendMessage("§c§oThis entity cannot be captured!");
            return false;
        }

        // Capture the mob
        CapturedMob capturedMob = new CapturedMob(mob.getType(), mob.getCustomName());
        souls.add(capturedMob);

        // Remove the mob from world
        mob.remove();

        // Effects
        Location mobLoc = mob.getLocation();
        mobLoc.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
            mobLoc.add(0, 1, 0),
            50, 0.5, 0.5, 0.5, 0.1);
        mobLoc.getWorld().spawnParticle(Particle.WITCH,
            mobLoc,
            30, 0.5, 0.5, 0.5, 0);
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1.0f, 2.0f);

        String mobName = capturedMob.getDisplayName();
        player.sendMessage("§d§lSoul Captured! §d§oCaptured " + mobName + " (" + souls.size() + "/" + MAX_CAPTURED_SOULS + ")");

        return true;
    }

    /**
     * Releases all captured mobs in reverse order (LIFO - Last In First Out)
     */
    public void releaseAllSouls(Player player) {
        List<CapturedMob> souls = playerSouls.get(player.getUniqueId());

        if (souls == null || souls.isEmpty()) {
            player.sendMessage("§c§oNo souls captured!");
            return;
        }

        Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(2));
        spawnLoc.setY(player.getLocation().getY()); // Same Y level as player

        // Release in reverse order (LIFO)
        Collections.reverse(souls);

        int released = 0;
        for (CapturedMob capturedMob : souls) {
            Entity spawned = player.getWorld().spawnEntity(spawnLoc, capturedMob.getType());

            // Restore custom name if it had one
            if (capturedMob.getCustomName() != null && spawned instanceof LivingEntity) {
                spawned.setCustomName(capturedMob.getCustomName());
                spawned.setCustomNameVisible(true);
            }

            // Effects at spawn location
            spawnLoc.getWorld().spawnParticle(Particle.PORTAL,
                spawnLoc.add(0, 1, 0),
                30, 0.5, 0.5, 0.5, 0.1);

            released++;
        }

        souls.clear();

        // Sound and message
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1.0f, 0.8f);
        player.sendMessage("§d§lReleased " + released + " captured soul(s)!");
    }

    /**
     * Gets the list of captured souls for a player
     */
    public List<CapturedMob> getCapturedSouls(Player player) {
        return playerSouls.getOrDefault(player.getUniqueId(), new ArrayList<>());
    }

    /**
     * Gets the number of captured souls for a player
     */
    public int getCapturedCount(Player player) {
        return getCapturedSouls(player).size();
    }

    /**
     * Checks if a mob can be captured
     */
    private boolean isCapturable(LivingEntity mob) {
        EntityType type = mob.getType();

        // Blacklist of uncapturable entities
        return type != EntityType.ENDER_DRAGON &&
               type != EntityType.WITHER &&
               type != EntityType.WARDEN &&
               type != EntityType.PLAYER &&
               type != EntityType.ARMOR_STAND &&
               !(mob instanceof NPC);
    }

    /**
     * Clears captured souls for a player (when they log out)
     */
    public void clearSouls(UUID playerId) {
        playerSouls.remove(playerId);
    }

    /**
     * Removes any active soul-absorption max-health modifiers from the player.
     * Uses multiple strategies because Paper 1.21+ may serialize legacy String-named
     * modifiers with a namespace prefix (e.g. "minecraft:soul_absorb_0"), so a single
     * filter alone is unreliable across relogs:
     *   1. Tracked UUIDs persisted to disk (bulletproof, survives restarts)
     *   2. NamespacedKey check (modern API)
     *   3. Broad name pattern (legacy)
     */
    public void cleanup(org.bukkit.entity.Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;

        Set<UUID> persistedUuids = loadPersistedModifierUuids(player.getUniqueId());

        List<AttributeModifier> toRemove = new ArrayList<>();
        for (AttributeModifier m : attr.getModifiers()) {
            if (m.getUniqueId() != null && persistedUuids.contains(m.getUniqueId())) {
                toRemove.add(m);
                continue;
            }
            if (isSoulModifier(m)) {
                toRemove.add(m);
            }
        }
        for (AttributeModifier m : toRemove) {
            attr.removeModifier(m);
        }

        // Clamp current health to (possibly reduced) max — Paper doesn't always do this
        // automatically on modifier removal, which leaves a player with over-max hearts.
        double max = attr.getValue();
        if (player.getHealth() > max) {
            player.setHealth(max);
        }

        activeSoulModifiers.remove(player.getUniqueId());
        clearPersistedModifierUuids(player.getUniqueId());

        if (!toRemove.isEmpty()) {
            plugin.getLogger().info("[SoulManager] Removed " + toRemove.size()
                + " stale soul-absorb modifier(s) from " + player.getName());
        }
    }

    private boolean isSoulModifier(AttributeModifier m) {
        String name = m.getName();
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.contains("soul_absorb") || lower.contains("soul-absorb")) return true;
            if (lower.startsWith("blissgems:")) return true;
        }
        try {
            org.bukkit.NamespacedKey key = m.getKey();
            if (key != null) {
                if ("blissgems".equals(key.getNamespace())) return true;
                String value = key.getKey();
                if (value != null && (value.contains("soul_absorb") || value.contains("soul-absorb"))) return true;
            }
        } catch (Throwable ignored) {
            // older API doesn't expose getKey() — fall through
        }
        return false;
    }

    // ---- disk persistence so we can identify our modifiers across restarts ----

    private java.io.File getPlayerFile(UUID playerId) {
        java.io.File folder = new java.io.File(plugin.getDataFolder(), "playerdata");
        if (!folder.exists()) folder.mkdirs();
        return new java.io.File(folder, playerId + ".yml");
    }

    private void persistModifierUuid(UUID playerId, UUID modifierUuid) {
        java.io.File f = getPlayerFile(playerId);
        org.bukkit.configuration.file.FileConfiguration data =
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
        List<String> list = data.getStringList("soul-modifier-uuids");
        list.add(modifierUuid.toString());
        data.set("soul-modifier-uuids", list);
        try { data.save(f); } catch (java.io.IOException ignored) {}
    }

    private void unpersistModifierUuid(UUID playerId, UUID modifierUuid) {
        java.io.File f = getPlayerFile(playerId);
        if (!f.exists()) return;
        org.bukkit.configuration.file.FileConfiguration data =
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
        List<String> list = data.getStringList("soul-modifier-uuids");
        list.remove(modifierUuid.toString());
        data.set("soul-modifier-uuids", list.isEmpty() ? null : list);
        try { data.save(f); } catch (java.io.IOException ignored) {}
    }

    private Set<UUID> loadPersistedModifierUuids(UUID playerId) {
        java.io.File f = getPlayerFile(playerId);
        if (!f.exists()) return Collections.emptySet();
        org.bukkit.configuration.file.FileConfiguration data =
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
        List<String> list = data.getStringList("soul-modifier-uuids");
        Set<UUID> out = new HashSet<>();
        for (String s : list) {
            try { out.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        return out;
    }

    private void clearPersistedModifierUuids(UUID playerId) {
        java.io.File f = getPlayerFile(playerId);
        if (!f.exists()) return;
        org.bukkit.configuration.file.FileConfiguration data =
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
        if (data.contains("soul-modifier-uuids")) {
            data.set("soul-modifier-uuids", null);
            try { data.save(f); } catch (java.io.IOException ignored) {}
        }
    }

    /**
     * Data class for captured mob information
     */
    public static class CapturedMob {
        private final EntityType type;
        private final String customName;

        public CapturedMob(EntityType type, String customName) {
            this.type = type;
            this.customName = customName;
        }

        public EntityType getType() {
            return type;
        }

        public String getCustomName() {
            return customName;
        }

        public String getDisplayName() {
            if (customName != null && !customName.isEmpty()) {
                return customName;
            }
            return type.toString().toLowerCase().replace("_", " ");
        }
    }
}
