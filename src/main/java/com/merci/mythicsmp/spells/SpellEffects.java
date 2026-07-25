package com.merci.mythicsmp.spells;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Blocs de construction communs à tous les sorts, pour éviter de réécrire
 * 64 fois la même mécanique. Chaque sort dans SpellRegistry combine un ou
 * plusieurs de ces effets avec ses propres chiffres (calculés à partir du
 * paramètre `power`, déjà mis à l'échelle par le palier + la maîtrise du
 * joueur — voir SpellEffect).
 *
 * NB : Minecraft n'a pas de vrai "stun" ni de vraie "racine" — on les
 * approxime avec un fort amplificateur de Lenteur/Faiblesse. C'est une
 * approximation volontaire, à affiner plus tard si besoin.
 */
public final class SpellEffects {

    private SpellEffects() {
    }

    public static void delayedExplodeAt(Plugin plugin, Player caster, Location origin, int delayTicks,
                                         double radius, double damage, Particle particle, Sound sound) {
        new BukkitRunnable() {
            @Override
            public void run() {
                explodeAt(caster, origin, radius, damage, particle, sound);
            }
        }.runTaskLater(plugin, delayTicks);
    }

    public static Location forwardPoint(Player caster, double distance) {
        return caster.getEyeLocation().add(caster.getEyeLocation().getDirection().normalize().multiply(distance));
    }

    /** Comme damageAoe mais centré sur un point arbitraire (ex : devant le joueur) plutôt que sur lui. */
    public static void explodeAt(Player caster, Location origin, double radius, double damage, Particle particle, Sound sound) {
        origin.getWorld().spawnParticle(particle, origin, 60, radius / 2, 0.4, radius / 2, 0.03);
        origin.getWorld().playSound(origin, sound, 1.3f, 1f);
        for (Entity nearby : origin.getWorld().getNearbyEntities(origin, radius, radius, radius)) {
            if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                target.damage(damage, caster);
            }
        }
    }

    public static void damageAoe(Player caster, double radius, double damage, Particle particle, Sound sound) {
        Location origin = caster.getLocation().add(0, 1, 0);
        caster.getWorld().spawnParticle(particle, origin, 50, radius / 2, 0.6, radius / 2, 0.02);
        caster.getWorld().playSound(origin, sound, 1.2f, 1f);
        for (Entity nearby : caster.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                target.damage(damage, caster);
            }
        }
    }

    public static void damageKnockbackAoe(Player caster, double radius, double damage, double knockback, Particle particle) {
        Location origin = caster.getLocation().add(0, 1, 0);
        caster.getWorld().spawnParticle(particle, origin, 60, radius / 2, 0.6, radius / 2, 0.03);
        for (Entity nearby : caster.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                target.damage(damage, caster);
                Vector push = target.getLocation().toVector().subtract(caster.getLocation().toVector())
                        .normalize().multiply(knockback).setY(0.35);
                target.setVelocity(push);
            }
        }
    }

    public static void pullAoe(Player caster, double radius, double damage, Particle particle) {
        Location origin = caster.getLocation();
        caster.getWorld().spawnParticle(particle, origin.clone().add(0, 1, 0), 50, radius / 2, 0.6, radius / 2, 0.02);
        for (Entity nearby : caster.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                Vector pull = origin.toVector().subtract(target.getLocation().toVector()).normalize().multiply(0.6).setY(0.2);
                target.setVelocity(pull);
                if (damage > 0) target.damage(damage, caster);
            }
        }
    }

    /** Sort "à distance" instantané : tire un rayon devant le joueur et touche la première cible. */
    public static boolean hitscan(Player caster, double range, double damage, Particle trail) {
        Location eye = caster.getEyeLocation();
        RayTraceResult result = caster.getWorld().rayTraceEntities(eye, eye.getDirection(), range, 0.5,
                e -> e instanceof LivingEntity && !e.equals(caster));

        double travelled = result != null ? eye.toVector().distance(result.getHitPosition()) : range;
        Vector step = eye.getDirection().normalize().multiply(0.5);
        Location point = eye.clone();
        for (double d = 0; d < travelled; d += 0.5) {
            caster.getWorld().spawnParticle(trail, point, 1, 0, 0, 0, 0);
            point.add(step);
        }

        if (result != null && result.getHitEntity() instanceof LivingEntity target) {
            target.damage(damage, caster);
            return true;
        }
        return false;
    }

    /** Approximation d'une "racine"/immobilisation. */
    public static void root(LivingEntity target, int durationTicks) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 250, false, true, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, durationTicks, 5, false, true, true));
    }

    public static void debuff(LivingEntity target, PotionEffectType type, int amplifier, int durationTicks) {
        target.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, false, true, true));
    }

    public static void buff(LivingEntity target, PotionEffectType type, int amplifier, int durationTicks) {
        target.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, false, true, true));
    }

    public static void heal(LivingEntity target, double amount) {
        var maxHealthAttr = target.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealthAttr != null ? maxHealthAttr.getValue() : target.getHealth();
        target.setHealth(Math.min(max, target.getHealth() + amount));
    }

    public static void dash(Player caster, double horizontalPower, double verticalPower) {
        Vector dir = caster.getLocation().getDirection().normalize().multiply(horizontalPower);
        dir.setY(verticalPower);
        caster.setVelocity(dir);
    }

    public static void shield(LivingEntity target, int amplifier, int durationTicks) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, durationTicks, amplifier, false, true, true));
    }

    /** Zone de dégâts continus (ex : pluie de météores, tempête...). */
    public static void areaOverTime(Plugin plugin, Player caster, Location center, double radius,
                                     double damagePerTick, int durationTicks, int periodTicks, Particle particle) {
        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= durationTicks || !caster.isOnline()) {
                    cancel();
                    return;
                }
                center.getWorld().spawnParticle(particle, center.clone().add(0, 0.3, 0), 25, radius / 2, 0.3, radius / 2, 0.02);
                for (Entity nearby : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                    if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                        target.damage(damagePerTick, caster);
                    }
                }
                elapsed += periodTicks;
            }
        }.runTaskTimer(plugin, 0L, periodTicks);
    }

    /** Invoque un allié temporaire pour le joueur (sorts ultra-forts d'invocation). */
    public static void summonAlly(Plugin plugin, Player caster, EntityType type, int durationTicks) {
        Entity entity = caster.getWorld().spawnEntity(caster.getLocation(), type);
        if (entity instanceof LivingEntity ally) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!ally.isDead()) ally.remove();
                }
            }.runTaskLater(plugin, durationTicks);
        }
    }
}
