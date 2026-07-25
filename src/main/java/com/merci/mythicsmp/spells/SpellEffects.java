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

    /** Version sonorisée de areaOverTime (joue un son de déclenchement une fois au lancement). */
    public static void areaOverTime(Plugin plugin, Player caster, Location center, double radius,
                                     double damagePerTick, int durationTicks, int periodTicks, Particle particle, Sound sound) {
        center.getWorld().playSound(center, sound, 1.3f, 1f);
        areaOverTime(plugin, caster, center, radius, damagePerTick, durationTicks, periodTicks, particle);
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

    // ============================================================
    //  NOUVEAU KIT VISUEL/SONORE "PROPRE" — projectiles qui voyagent
    //  réellement dans les airs, particules qui suivent le joueur,
    //  impacts sonorisés et en couches. Utilisé par feu/eau/terre/vent.
    //  (Les méthodes ci-dessus restent inchangées pour ne rien casser
    //  ailleurs, ex : UltimateSpell.)
    // ============================================================

    /**
     * Un vrai projectile visuel qui avance tick par tick devant le joueur,
     * avec une particule "coeur" + une particule de traînée, s'arrête sur
     * un bloc solide ou sur la première entité touchée, joue un son de
     * lancer puis un son d'impact, et peut infliger des dégâts en zone
     * autour du point d'impact (explosionRadius = 0 pour un impact simple).
     */
    public static void launchProjectile(Plugin plugin, Player caster, double range, double speed,
                                         double hitRadius, double damage, double explosionRadius,
                                         Particle coreParticle, Particle trailParticle,
                                         Sound launchSound, Sound impactSound) {
        Location eye = caster.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        caster.getWorld().playSound(eye, launchSound, 1.3f, 1.15f);

        new BukkitRunnable() {
            final Location point = eye.clone().add(direction.clone().multiply(0.8));
            double travelled = 0.8;
            int tick = 0;

            @Override
            public void run() {
                if (!caster.isOnline() || travelled >= range) {
                    cancel();
                    return;
                }

                if (point.getBlock().getType().isSolid()) {
                    impactBurst(point, coreParticle, trailParticle, impactSound, damage, explosionRadius, caster, null);
                    cancel();
                    return;
                }

                for (Entity nearby : point.getWorld().getNearbyEntities(point, hitRadius, hitRadius, hitRadius)) {
                    if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                        impactBurst(point, coreParticle, trailParticle, impactSound, damage, explosionRadius, caster, target);
                        cancel();
                        return;
                    }
                }

                // Coeur du projectile + halo qui tourne légèrement autour pour un rendu "boule" plutôt qu'un point plat
                point.getWorld().spawnParticle(coreParticle, point, 3, 0.06, 0.06, 0.06, 0.01);
                double swirl = tick * 0.9;
                point.getWorld().spawnParticle(trailParticle, point.clone().add(Math.cos(swirl) * 0.18, Math.sin(swirl) * 0.18, 0), 1, 0, 0, 0, 0);
                point.getWorld().spawnParticle(trailParticle, point.clone().add(-Math.cos(swirl) * 0.18, -Math.sin(swirl) * 0.18, 0), 1, 0, 0, 0, 0);

                point.add(direction.clone().multiply(speed));
                travelled += speed;
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Impact soigné d'un projectile : gerbe de particules en 2 couches, son, dégâts directs + éclaboussure optionnelle. */
    private static void impactBurst(Location point, Particle core, Particle trail, Sound sound,
                                     double damage, double explosionRadius, Player caster, LivingEntity directHit) {
        point.getWorld().spawnParticle(core, point, 35, 0.35, 0.35, 0.35, 0.06);
        point.getWorld().spawnParticle(trail, point, 20, 0.5, 0.5, 0.5, 0.02);
        point.getWorld().playSound(point, sound, 1.3f, 1f);

        if (directHit != null) {
            directHit.damage(damage, caster);
        }
        if (explosionRadius > 0) {
            for (Entity nearby : point.getWorld().getNearbyEntities(point, explosionRadius, explosionRadius, explosionRadius)) {
                if (nearby instanceof LivingEntity target && !target.equals(caster) && !target.equals(directHit)) {
                    target.damage(damage * 0.6, caster);
                }
            }
        }
    }

    /** Anneau de particules posé au sol, utile pour annoncer une zone d'effet avant/pendant un sort. */
    public static void groundRing(Location center, Particle particle, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            center.getWorld().spawnParticle(particle, center.clone().add(x, 0.1, z), 1, 0, 0, 0, 0);
        }
    }

    /** Spirale montante de particules autour d'un point, pour un cast/transformation qui en jette. */
    public static void risingSpiral(Location origin, Particle particle, double height, double radius, int turns) {
        int steps = Math.max(10, turns * 24);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double angle = t * turns * 2 * Math.PI;
            double y = t * height;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            origin.getWorld().spawnParticle(particle, origin.clone().add(x, y, z), 1, 0, 0, 0, 0);
        }
    }

    /**
     * Aura de particules qui SUIT le joueur en temps réel pendant toute la
     * durée de l'effet (buffs, avatars, dashs...) : deux anneaux tournants
     * à hauteur différente, recalculés sur la position live du joueur.
     */
    public static void followingAura(Plugin plugin, Player target, Particle particle, int durationTicks,
                                      double radius, int pointsPerRing) {
        new BukkitRunnable() {
            int elapsed = 0;
            double angle = 0;

            @Override
            public void run() {
                if (elapsed >= durationTicks || !target.isOnline()) {
                    cancel();
                    return;
                }
                Location base = target.getLocation();
                Location lowRing = base.clone().add(0, 0.15, 0);
                Location highRing = base.clone().add(0, 1.9, 0);

                for (int i = 0; i < pointsPerRing; i++) {
                    double a = angle + (2 * Math.PI / pointsPerRing) * i;
                    double x = Math.cos(a) * radius;
                    double z = Math.sin(a) * radius;
                    lowRing.getWorld().spawnParticle(particle, lowRing.clone().add(x, 0, z), 1, 0, 0, 0, 0);
                    highRing.getWorld().spawnParticle(particle, highRing.clone().add(-x, 0, -z), 1, 0, 0, 0, 0);
                }
                angle += 0.4;
                elapsed += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /** Traînée qui suit le joueur pendant un court sprint/dash. */
    public static void dashTrail(Plugin plugin, Player caster, Particle particle, int durationTicks) {
        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= durationTicks || !caster.isOnline()) {
                    cancel();
                    return;
                }
                caster.getWorld().spawnParticle(particle, caster.getLocation().add(0, 1, 0), 6, 0.25, 0.35, 0.25, 0.02);
                elapsed++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Version sonorisée + en couches de damageKnockbackAoe, avec anneau au sol pour bien lire la zone touchée. */
    public static void damageKnockbackAoe(Player caster, double radius, double damage, double knockback,
                                           Particle particle, Particle accent, Sound sound) {
        Location origin = caster.getLocation().add(0, 1, 0);
        caster.getWorld().spawnParticle(particle, origin, 70, radius / 2, 0.6, radius / 2, 0.03);
        caster.getWorld().spawnParticle(accent, origin, 30, radius / 2, 0.8, radius / 2, 0.01);
        groundRing(caster.getLocation(), accent, radius, 28);
        caster.getWorld().playSound(origin, sound, 1.3f, 1f);
        for (Entity nearby : caster.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                target.damage(damage, caster);
                Vector push = target.getLocation().toVector().subtract(caster.getLocation().toVector())
                        .normalize().multiply(knockback).setY(0.35);
                target.setVelocity(push);
            }
        }
    }

    /** Version sonorisée de pullAoe, avec anneau au sol. */
    public static void pullAoe(Player caster, double radius, double damage, Particle particle, Sound sound) {
        Location origin = caster.getLocation();
        caster.getWorld().spawnParticle(particle, origin.clone().add(0, 1, 0), 60, radius / 2, 0.6, radius / 2, 0.02);
        groundRing(origin, particle, radius, 24);
        caster.getWorld().playSound(origin, sound, 1.2f, 1f);
        for (Entity nearby : caster.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                Vector pull = origin.toVector().subtract(target.getLocation().toVector()).normalize().multiply(0.6).setY(0.2);
                target.setVelocity(pull);
                if (damage > 0) target.damage(damage, caster);
            }
        }
    }
}
