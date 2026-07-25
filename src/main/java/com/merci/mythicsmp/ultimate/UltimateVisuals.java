package com.merci.mythicsmp.ultimate;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Petite bibliothèque de figures de particules (anneaux, spirales montantes,
 * ondes de choc qui s'étendent, faisceaux ponctués d'anneaux d'impact)
 * réservée aux sorts du Mage Ultime et au sort admin : ce sont les sorts
 * les plus rares et les plus forts du serveur, ils méritent une vraie mise
 * en scène en plusieurs temps (charge -> relâche -> rémanence) plutôt
 * qu'un simple nuage de particules ponctuel comme les sorts élémentaires
 * de base.
 *
 * Tout repose sur de la trigonométrie basique (cercle paramétrique) étalée
 * sur plusieurs ticks via BukkitRunnable, dans le même esprit que
 * SpellEffects#areaOverTime.
 */
public final class UltimateVisuals {

    private UltimateVisuals() {
    }

    public static Particle.DustOptions dust(Color color, float size) {
        return new Particle.DustOptions(color, size);
    }

    /** Anneau de particules "simples" (pas de couleur) à une hauteur donnée autour d'un centre. */
    public static void ring(Location center, double radius, int points, Particle particle, double yOffset) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            world.spawnParticle(particle, x, center.getY() + yOffset, z, 1, 0, 0, 0, 0);
        }
    }

    /** Anneau de particules colorées (poussière) à une hauteur donnée autour d'un centre. */
    public static void ringDust(Location center, double radius, int points, double yOffset, Particle.DustOptions dust) {
        World world = center.getWorld();
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            world.spawnParticle(Particle.DUST, x, center.getY() + yOffset, z, 1, 0, 0, 0, 0, dust);
        }
    }

    /**
     * Empile plusieurs anneaux colorés à des hauteurs croissantes autour d'un centre, chacun
     * tourné un peu plus que le précédent : donne un effet de "colonne en vrille" (double hélice
     * si on l'appelle deux fois avec deux jeux de couleurs et un décalage d'angle différent).
     */
    public static void spiralColumn(Location center, double radius, double spinPerRing, int rings,
                                     double ringSpacing, int pointsPerRing, Particle.DustOptions dust) {
        for (int r = 0; r < rings; r++) {
            World world = center.getWorld();
            double spin = r * spinPerRing;
            for (int i = 0; i < pointsPerRing; i++) {
                double angle = spin + (2 * Math.PI / pointsPerRing) * i;
                double x = center.getX() + radius * Math.cos(angle);
                double z = center.getZ() + radius * Math.sin(angle);
                world.spawnParticle(Particle.DUST, x, center.getY() + r * ringSpacing, z, 1, 0, 0, 0, 0, dust);
            }
        }
    }

    /** Onde de choc : une série d'anneaux qui grandissent progressivement, tick après tick. */
    public static void shockwave(Plugin plugin, Location center, Particle particle, double maxRadius, int steps, int periodTicks) {
        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (step >= steps) {
                    cancel();
                    return;
                }
                double radius = maxRadius * (step + 1) / (double) steps;
                ring(center, radius, 20 + step * 5, particle, 0.15);
                step++;
            }
        }.runTaskTimer(plugin, 0L, periodTicks);
    }

    /** Variante colorée de shockwave, pour un "anneau de poussière" qui s'étend (ex : onde d'éveil, halo). */
    public static void shockwaveDust(Plugin plugin, Location center, Particle.DustOptions dust, double maxRadius, int steps, int periodTicks) {
        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (step >= steps) {
                    cancel();
                    return;
                }
                double radius = maxRadius * (step + 1) / (double) steps;
                ringDust(center, radius, 20 + step * 5, 0.15, dust);
                step++;
            }
        }.runTaskTimer(plugin, 0L, periodTicks);
    }

    /**
     * Spirale montante autour du joueur pendant la "charge" du sort (avant que l'effet ne parte) :
     * plusieurs bras tournants qui montent progressivement, façon invocation rituelle.
     * `onComplete` est lancé une fois la charge terminée (là où l'effet réel doit partir).
     */
    public static void chargeUp(Plugin plugin, Player caster, int durationTicks, double radius,
                                 int arms, double maxHeight, Particle particle, Runnable onComplete) {
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!caster.isOnline()) {
                    cancel();
                    return;
                }
                if (tick >= durationTicks) {
                    cancel();
                    if (onComplete != null) onComplete.run();
                    return;
                }
                Location base = caster.getLocation();
                double progress = tick / (double) durationTicks;
                double angle = tick * 0.7;
                double height = progress * maxHeight;
                double shrinkingRadius = radius * (1.0 - 0.35 * progress);
                for (int arm = 0; arm < arms; arm++) {
                    double a = angle + (2 * Math.PI / arms) * arm;
                    double x = base.getX() + shrinkingRadius * Math.cos(a);
                    double z = base.getZ() + shrinkingRadius * Math.sin(a);
                    base.getWorld().spawnParticle(particle, x, base.getY() + height, z, 1, 0, 0, 0, 0);
                }
                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Colorée : même principe que chargeUp mais avec des particules DUST (couleur au choix). */
    public static void chargeUpDust(Plugin plugin, Player caster, int durationTicks, double radius,
                                     int arms, double maxHeight, Particle.DustOptions dust, Runnable onComplete) {
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!caster.isOnline()) {
                    cancel();
                    return;
                }
                if (tick >= durationTicks) {
                    cancel();
                    if (onComplete != null) onComplete.run();
                    return;
                }
                Location base = caster.getLocation();
                double progress = tick / (double) durationTicks;
                double angle = tick * 0.7;
                double height = progress * maxHeight;
                double shrinkingRadius = radius * (1.0 - 0.35 * progress);
                for (int arm = 0; arm < arms; arm++) {
                    double a = angle + (2 * Math.PI / arms) * arm;
                    double x = base.getX() + shrinkingRadius * Math.cos(a);
                    double z = base.getZ() + shrinkingRadius * Math.sin(a);
                    base.getWorld().spawnParticle(Particle.DUST, x, base.getY() + height, z, 1, 0, 0, 0, 0, dust);
                }
                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Anneaux d'impact espacés le long d'un faisceau tiré devant le joueur (donne du "poids" au rayon). */
    public static void beamRings(Location origin, Vector direction, double length, double step, Particle particle) {
        World world = origin.getWorld();
        Vector dir = direction.clone().normalize();
        for (double d = 0; d < length; d += step) {
            Location point = origin.clone().add(dir.clone().multiply(d));
            ring(point, 0.3 + (d / length) * 0.4, 8, particle, 0);
        }
    }
}
