package dev.xoperr.blissgems.goldevent;

import org.bukkit.entity.Player;

/**
 * Talks to the resource pack's core shaders through the client's clock: a player time on a
 * far-future "signature" day (80000+) with a chosen time of day is read by the shader as a
 * code (dream tint, eye-close, memory grade...). Without the pack it's just an odd sky.
 */
public final class SignatureClock {
    public static final long POV_DAY = 80000L;
    public static final long POV_CODE = 16001L;
    public static final long EYE_DAY = 80019L;
    public static final long DREAM_DAY = 80036L;
    public static final long DREAM_TOD = 6016L;
    public static final long MEMORY_DAY = 80037L;
    public static final long STORM_DAY = 80038L;
    public static final long COVER_DAY = 80020L;
    public static final long POV_COVER_CODE = 16121L;

    private SignatureClock() {
    }

    public static void send(Player p, long day, long timeOfDay) {
        if (p == null || p.getWorld() == null) {
            return;
        }
        p.setPlayerTime((day - p.getWorld().getFullTime() / 24000L) * 24000L + timeOfDay, false);
    }

    /** Sends a signature day while keeping the world's real time of day. */
    public static void sendKeepingHour(Player p, long day) {
        if (p == null || p.getWorld() == null) {
            return;
        }
        send(p, day, p.getWorld().getFullTime() % 24000L);
    }

    /** The "eyelids" cover: k 15..1 darkens the screen with the pack, 0 = open. */
    public static void cover(Player p, int k) {
        sendKeepingHour(p, COVER_DAY + k);
    }
}
