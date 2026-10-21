package dev.xoperr.blissgems.pedestal;

import dev.xoperr.blissgems.BlissGems;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The moment a Repair Kit or Restoration Book is accepted: the item rises over the pedestal
 * spinning inside a spiral of light, then bursts into a pillar with thunder (purple for
 * restoration, green for repair). About 2 seconds.
 */
final class PedestalStartEffect {
    private PedestalStartEffect() {
    }

    static void play(BlissGems plugin, Item item, Location main, Color color, boolean restoration) {
        World w = main.getWorld();
        if (w == null) return;
        Location centre = main.clone().add(0.5, 1.2, 0.5);
        ItemStack shown = item.getItemStack().clone();
        shown.setAmount(1);
        ItemDisplay display = w.spawn(item.getLocation(), ItemDisplay.class, d -> {
            d.setItemStack(shown);
            d.setPersistent(false);
            d.setBillboard(Display.Billboard.FIXED);
            d.setGlowing(true);
            d.setGlowColorOverride(color);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setTeleportDuration(2);
        });
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.3f);
        w.playSound(centre, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 3.0f, restoration ? 0.7f : 1.2f);
        w.playSound(centre, restoration ? Sound.ENTITY_EVOKER_PREPARE_SUMMON : Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.MASTER, 2.0f, 1.0f);
        Location from = item.getLocation().clone();
        new BukkitRunnable() {
            int t;

            @Override
            public void run() {
                if (!display.isValid()) {
                    this.cancel();
                    return;
                }
                double f = Math.min(1.0, this.t / 36.0);
                double ease = f * f * (3.0 - 2.0 * f);
                Location at = from.clone().add(centre.clone().add(0, 1.6 * ease, 0).toVector().subtract(from.toVector()).multiply(ease));
                display.teleport(at);
                float spin = (float) (this.t * 0.35);
                float scale = (float) (0.6 + 0.6 * ease);
                display.setTransformation(new Transformation(new Vector3f(), new Quaternionf().rotateY(spin), new Vector3f(scale, scale, scale), new Quaternionf()));
                // two strands of light spiralling up around the pedestal
                for (int strand = 0; strand < 2; strand++) {
                    double a = this.t * 0.45 + strand * Math.PI;
                    double r = 1.6 * (1.0 - 0.6 * ease);
                    w.spawnParticle(Particle.DUST, centre.getX() + Math.cos(a) * r, centre.getY() - 0.8 + 2.6 * ease, centre.getZ() + Math.sin(a) * r, 2, 0.02, 0.02, 0.02, 0, dust, true);
                }
                w.spawnParticle(Particle.ENCHANT, at, 6, 0.3, 0.3, 0.3, 0.6, null, true);
                if (this.t % 6 == 0) w.playSound(at, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.MASTER, 1.2f, 0.6f + (float) ease);
                if (this.t >= 40) {
                    this.cancel();
                    display.remove();
                    burst(w, at, centre, dust, restoration);
                    return;
                }
                this.t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static void burst(World w, Location at, Location centre, Particle.DustOptions dust, boolean restoration) {
        w.strikeLightningEffect(centre.clone().add(0, -1.2, 0));
        if (Particle.FLASH.getDataType() == Color.class) w.spawnParticle(Particle.FLASH, at, 2, 0, 0, 0, 0, Color.WHITE, true);
        else w.spawnParticle(Particle.FLASH, at, 2, 0, 0, 0, 0, null, true);
        w.spawnParticle(Particle.END_ROD, at, 60, 0.2, 0.2, 0.2, 0.25, null, true);
        w.spawnParticle(restoration ? Particle.REVERSE_PORTAL : Particle.HAPPY_VILLAGER, at, 80, 0.6, 0.6, 0.6, 0.2, null, true);
        for (double y = 0; y < 12; y += 0.25) {
            w.spawnParticle(Particle.DUST, centre.getX(), centre.getY() - 1 + y, centre.getZ(), 2, 0.12, 0.05, 0.12, 0, dust, true);
        }
        for (int i = 0; i < 48; i++) {
            double a = Math.PI * 2 * i / 48;
            w.spawnParticle(Particle.DUST, centre.getX() + Math.cos(a) * 2.5, centre.getY() - 1.0, centre.getZ() + Math.sin(a) * 2.5, 1, 0, 0, 0, 0, dust, true);
        }
        w.playSound(centre, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 2.0f, restoration ? 0.6f : 1.0f);
        w.playSound(centre, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.MASTER, 2.0f, 0.8f);
    }
}
