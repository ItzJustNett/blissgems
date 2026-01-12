package dev.xoperr.blissgems.managers;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.*;

/**
 * Manages the Astra Gem soul system:
 * - Soul Absorption: Heal when killing mobs/players
 * - Soul Capture: Capture up to 2 mobs in the gem and release them
 */
public class SoulManager {
    private final BlissGems plugin;
    private final NamespacedKey capturedMobKey;
    private final Map<UUID, List<CapturedMob>> playerSouls = new HashMap<>();
    private static final int MAX_CAPTURED_SOULS = 2;

    public SoulManager(BlissGems plugin) {
        this.plugin = plugin;
        this.capturedMobKey = new NamespacedKey(plugin, "captured_mob");
    }

    /**
     * Called when a player with Astra gem kills an entity
     * Heals the player based on what was killed
     */
    public void absorbSoul(Player player, Entity killedEntity) {
        boolean isPlayer = killedEntity instanceof Player;
        double healAmount;

        if (isPlayer) {
            healAmount = 10.0; // 5 hearts for player kills
        } else {
            healAmount = 5.0; // 2.5 hearts for mob kills
        }

        // Heal the player
        double currentHealth = player.getHealth();
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double newHealth = Math.min(currentHealth + healAmount, maxHealth);
        player.setHealth(newHealth);

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
        player.sendMessage("§d§oAbsorbed soul from " + entityName + "! +" + (healAmount / 2) + " hearts");
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
