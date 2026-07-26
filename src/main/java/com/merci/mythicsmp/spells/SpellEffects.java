package com.merci.mythicsmp.spells;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    /**
     * BlockData par défaut pour les particules qui en réclament une (ex :
     * BLOCK_CRUMBLE). Sans ça, le serveur refuse la particule et lève une
     * exception qui annule tout le reste de la méthode en cours (dégâts
     * compris) : c'était le bug qui empêchait plusieurs sorts de Terre (et
     * une partie du sort de fusion Cataclysme) de se déclencher correctement.
     */
    private static final BlockData ROCK_DEBRIS = Material.STONE.createBlockData();

    /**
     * Remplace un appel direct à World#spawnParticle : fournit automatiquement
     * la donnée requise pour les particules qui en ont besoin (BLOCK_CRUMBLE),
     * et se comporte normalement pour toutes les autres. À utiliser partout où
     * la particule vient d'un paramètre (donc potentiellement BLOCK_CRUMBLE).
     */
    private static void spawn(World world, Particle particle, Location loc, int count,
                               double offsetX, double offsetY, double offsetZ, double extra) {
        if (particle == Particle.BLOCK_CRUMBLE) {
            world.spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ, extra, ROCK_DEBRIS);
        } else {
            world.spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ, extra);
        }
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
        spawn(origin.getWorld(), particle, origin, 85, radius / 1.6, 0.55, radius / 1.6, 0.035);
        origin.getWorld().playSound(origin, sound, 1.3f, 1f);
        for (Entity nearby : origin.getWorld().getNearbyEntities(origin, radius, radius, radius)) {
            if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                target.damage(damage, caster);
            }
        }
    }

    public static void damageAoe(Player caster, double radius, double damage, Particle particle, Sound sound) {
        Location origin = caster.getLocation().add(0, 1, 0);
        spawn(caster.getWorld(), particle, origin, 70, radius / 1.6, 0.8, radius / 1.6, 0.025);
        caster.getWorld().playSound(origin, sound, 1.2f, 1f);
        for (Entity nearby : caster.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                target.damage(damage, caster);
            }
        }
    }

    public static void damageKnockbackAoe(Player caster, double radius, double damage, double knockback, Particle particle) {
        Location origin = caster.getLocation().add(0, 1, 0);
        spawn(caster.getWorld(), particle, origin, 85, radius / 1.6, 0.8, radius / 1.6, 0.035);
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
        spawn(caster.getWorld(), particle, origin.clone().add(0, 1, 0), 70, radius / 1.6, 0.8, radius / 1.6, 0.025);
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
                spawn(center.getWorld(), particle, center.clone().add(0, 0.3, 0), 35, radius / 1.7, 0.35, radius / 1.7, 0.025);
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
                spawn(point.getWorld(), coreParticle, point, 4, 0.08, 0.08, 0.08, 0.012);
                double swirl = tick * 0.9;
                spawn(point.getWorld(), trailParticle, point.clone().add(Math.cos(swirl) * 0.18, Math.sin(swirl) * 0.18, 0), 1, 0, 0, 0, 0);
                spawn(point.getWorld(), trailParticle, point.clone().add(-Math.cos(swirl) * 0.18, -Math.sin(swirl) * 0.18, 0), 1, 0, 0, 0, 0);

                point.add(direction.clone().multiply(speed));
                travelled += speed;
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Impact soigné d'un projectile : gerbe de particules en 2 couches, son, dégâts directs + éclaboussure optionnelle. */
    private static void impactBurst(Location point, Particle core, Particle trail, Sound sound,
                                     double damage, double explosionRadius, Player caster, LivingEntity directHit) {
        spawn(point.getWorld(), core, point, 45, 0.4, 0.4, 0.4, 0.07);
        spawn(point.getWorld(), trail, point, 28, 0.55, 0.55, 0.55, 0.025);
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
            spawn(center.getWorld(), particle, center.clone().add(x, 0.1, z), 1, 0, 0, 0, 0);
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
            spawn(origin.getWorld(), particle, origin.clone().add(x, y, z), 1, 0, 0, 0, 0);
        }
    }

    /**
     * Aura de particules qui SUIT le joueur en temps réel pendant toute la
     * durée de l'effet (buffs, avatars, dashs...) : deux anneaux tournants
     * à hauteur différente, recalculés sur la position live du joueur.
     */
    public static void followingAura(Plugin plugin, Player target, Particle particle, int durationTicks,
                                      double radius, int pointsPerRing) {
        double biggerRadius = radius * 1.15;
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
                    double x = Math.cos(a) * biggerRadius;
                    double z = Math.sin(a) * biggerRadius;
                    spawn(lowRing.getWorld(), particle, lowRing.clone().add(x, 0, z), 1, 0, 0, 0, 0);
                    spawn(highRing.getWorld(), particle, highRing.clone().add(-x, 0, -z), 1, 0, 0, 0, 0);
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
                spawn(caster.getWorld(), particle, caster.getLocation().add(0, 1, 0), 9, 0.3, 0.4, 0.3, 0.025);
                elapsed++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Version sonorisée + en couches de damageKnockbackAoe, avec anneau au sol pour bien lire la zone touchée. */
    public static void damageKnockbackAoe(Player caster, double radius, double damage, double knockback,
                                           Particle particle, Particle accent, Sound sound) {
        Location origin = caster.getLocation().add(0, 1, 0);
        spawn(caster.getWorld(), particle, origin, 95, radius / 1.6, 0.8, radius / 1.6, 0.04);
        spawn(caster.getWorld(), accent, origin, 45, radius / 1.6, 1.0, radius / 1.6, 0.015);
        groundRing(caster.getLocation(), accent, radius, 34);
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
        spawn(caster.getWorld(), particle, origin.clone().add(0, 1, 0), 80, radius / 1.6, 0.8, radius / 1.6, 0.025);
        groundRing(origin, particle, radius, 30);
        caster.getWorld().playSound(origin, sound, 1.2f, 1f);
        for (Entity nearby : caster.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                Vector pull = origin.toVector().subtract(target.getLocation().toVector()).normalize().multiply(0.6).setY(0.2);
                target.setVelocity(pull);
                if (damage > 0) target.damage(damage, caster);
            }
        }
    }

    // ============================================================
    //  MÉTÉORES — un vrai objet qui tombe visuellement du ciel jusqu'à
    //  son point d'impact, au lieu d'une simple explosion posée au sol.
    // ============================================================

    /**
     * Un météore qui tombe réellement du ciel jusqu'à `impactPoint` : part en
     * hauteur au-dessus du point visé, descend à vitesse constante avec une
     * traînée de particules bien visible, puis explose en zone à l'impact.
     * C'est la pièce qui manquait aux sorts de météore(s) : avant, seule
     * l'explosion au sol se déclenchait, sans rien qui ne tombe du ciel.
     */
    public static void meteorFall(Plugin plugin, Player caster, Location impactPoint, double startHeight,
                                   double fallSpeed, double impactRadius, double damage,
                                   Particle coreParticle, Particle trailParticle, Sound impactSound) {
        Location groundPoint = impactPoint.clone();
        groundPoint.getWorld().playSound(groundPoint, Sound.ENTITY_GHAST_SHOOT, 1.4f, 0.6f);
        groundRing(groundPoint, coreParticle, Math.max(2.0, impactRadius * 0.7), 26);

        new BukkitRunnable() {
            final Location point = groundPoint.clone().add(0, startHeight, 0);

            @Override
            public void run() {
                if (!caster.isOnline()) {
                    cancel();
                    return;
                }
                if (point.getY() <= groundPoint.getY() + 0.4) {
                    cancel();
                    point.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, groundPoint.clone().add(0, 0.2, 0), 2, 0.3, 0, 0.3, 0);
                    explodeAt(caster, groundPoint, impactRadius, damage, coreParticle, impactSound);
                    return;
                }
                spawn(point.getWorld(), coreParticle, point, 8, 0.22, 0.22, 0.22, 0.012);
                spawn(point.getWorld(), trailParticle, point, 4, 0.15, 0.15, 0.15, 0.01);
                point.subtract(0, fallSpeed, 0);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Plusieurs météores (voir meteorFall) qui s'abattent à des instants et
     * des positions différentes dans une zone, pour un vrai effet de pluie
     * plutôt qu'une simple zone de dégâts continue sans rien qui ne tombe.
     */
    public static void meteorShower(Plugin plugin, Player caster, Location center, int meteorCount,
                                     double spreadRadius, double startHeight, double fallSpeed,
                                     double impactRadius, double damagePerMeteor,
                                     Particle coreParticle, Particle trailParticle, Sound impactSound) {
        groundRing(center, coreParticle, spreadRadius, 40);
        for (int i = 0; i < meteorCount; i++) {
            int index = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!caster.isOnline()) return;
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * spreadRadius;
                    Location impact = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                    meteorFall(plugin, caster, impact, startHeight, fallSpeed, impactRadius, damagePerMeteor,
                            coreParticle, trailParticle, impactSound);
                }
            }.runTaskLater(plugin, (long) (index * 8));
        }
    }

    // ============================================================
    //  VAGUES — un vrai front d'onde qui s'étend tick après tick au lieu
    //  d'un simple nuage de particules figé sur place. Utilisé par les
    //  gros sorts d'eau et par la Tempête Primordiale.
    // ============================================================

    /**
     * Vague qui s'étend depuis un centre : plusieurs anneaux qui grandissent
     * progressivement, infligeant des dégâts et un repoussement au moment
     * précis où le front d'onde atteint chaque cible (une seule fois par
     * cible, pour ne pas la frapper à chaque tick).
     */
    public static void expandingWave(Plugin plugin, Player caster, Location center, double maxRadius,
                                      int steps, int periodTicks, double damagePerHit, double knockback,
                                      Particle crestParticle, Particle foamParticle, Sound sound) {
        center.getWorld().playSound(center, sound, 1.3f, 1f);
        new BukkitRunnable() {
            int step = 0;
            final Set<UUID> alreadyHit = new HashSet<>();

            @Override
            public void run() {
                if (step >= steps || !caster.isOnline()) {
                    cancel();
                    return;
                }
                double radius = maxRadius * (step + 1) / (double) steps;
                double previousRadius = maxRadius * step / (double) steps;
                int points = 26 + step * 8;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI / points) * i;
                    double x = center.getX() + radius * Math.cos(angle);
                    double z = center.getZ() + radius * Math.sin(angle);
                    Location crest = new Location(center.getWorld(), x, center.getY() + 0.2, z);
                    center.getWorld().spawnParticle(crestParticle, crest, 1, 0, 0.25, 0, 0.04);
                    if (i % 2 == 0) {
                        center.getWorld().spawnParticle(foamParticle, crest.clone().add(0, 0.4, 0), 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
                for (Entity nearby : center.getWorld().getNearbyEntities(center, radius, radius + 1, radius)) {
                    if (nearby instanceof LivingEntity target && !target.equals(caster) && !alreadyHit.contains(target.getUniqueId())) {
                        double dist = target.getLocation().distance(center);
                        if (dist <= radius + 0.5 && dist >= previousRadius - 1.0) {
                            alreadyHit.add(target.getUniqueId());
                            target.damage(damagePerHit, caster);
                            Vector push = target.getLocation().toVector().subtract(center.toVector())
                                    .normalize().multiply(knockback).setY(0.35);
                            target.setVelocity(push);
                        }
                    }
                }
                step++;
            }
        }.runTaskTimer(plugin, 0L, periodTicks);
    }

    // ============================================================
    //  TORNADE — la vraie mécanique demandée pour "Œil du Cyclone" :
    //  un entonnoir qui voyage réellement dans la direction figée au
    //  moment du cast, plutôt qu'un simple effet posé sur place.
    // ============================================================

    /**
     * Blocs "meubles" qu'une tornade peut arracher du sol (terre, sable,
     * gravier, neige, végétation, cultures). Volontairement limité à ça :
     * pas de pierre/bois, pour ne pas raser une construction du joueur au
     * passage — seul le terrain naturel meuble est affecté, et l'arrachage
     * est définitif (le bloc ne repousse pas).
     */
    private static boolean isTornadoLiftable(Material type) {
        return switch (type) {
            case DIRT, GRASS_BLOCK, COARSE_DIRT, ROOTED_DIRT, PODZOL, MYCELIUM,
                    SAND, RED_SAND, GRAVEL, SNOW, SNOW_BLOCK,
                    SHORT_GRASS, TALL_GRASS, FERN, LARGE_FERN, DEAD_BUSH,
                    OAK_LEAVES, BIRCH_LEAVES, SPRUCE_LEAVES, JUNGLE_LEAVES, ACACIA_LEAVES,
                    DARK_OAK_LEAVES, MANGROVE_LEAVES, CHERRY_LEAVES, AZALEA_LEAVES, FLOWERING_AZALEA_LEAVES,
                    WHEAT, CARROTS, POTATOES, BEETROOTS,
                    POPPY, DANDELION, CORNFLOWER, ALLIUM, AZURE_BLUET, ORANGE_TULIP,
                    RED_TULIP, PINK_TULIP, WHITE_TULIP, OXEYE_DAISY, LILY_OF_THE_VALLEY -> true;
            default -> false;
        };
    }

    /** Point au sol (sommet du plus haut bloc solide) sous une position donnée, pour suivre le relief. */
    private static Location highestSolidGround(Location near) {
        Location loc = near.clone();
        int y = loc.getWorld().getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ());
        loc.setY(y + 1);
        return loc;
    }

    /**
     * LA vraie tornade : part de la position du joueur et avance en ligne
     * droite dans la direction où il regardait au moment du cast (direction
     * figée une fois pour toutes, elle ne suit pas la caméra ensuite). Elle
     * suit le relief du terrain, tourne sur elle-même en avançant (entonnoir
     * à plusieurs anneaux, plus large en haut), aspire et endommage les
     * ennemis qu'elle traverse, ET arrache réellement les blocs meubles
     * autour d'elle (voir isTornadoLiftable) en les transformant en débris
     * qui tourbillonnent puis disparaissent — les blocs arrachés ne
     * repoussent pas, un vrai passage de tornade laisse des marques dans
     * le terrain.
     */
    public static void movingTornado(Plugin plugin, Player caster, double travelDistance, double speed,
                                      double funnelRadius, double funnelHeight, double damagePerTick) {
        Vector direction = caster.getLocation().getDirection().setY(0);
        if (direction.lengthSquared() < 0.0001) direction = new Vector(0, 0, 1);
        direction.normalize();
        final Vector dir = direction;

        Location start = caster.getLocation();
        start.getWorld().playSound(start, Sound.ENTITY_PHANTOM_FLAP, 2f, 0.6f);
        start.getWorld().playSound(start, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.2f, 1.6f);

        new BukkitRunnable() {
            final Location center = start.clone();
            final Map<UUID, Integer> lastHitTick = new HashMap<>();
            double travelled = 0;
            int tick = 0;

            @Override
            public void run() {
                if (!caster.isOnline() || travelled >= travelDistance) {
                    Location end = highestSolidGround(center);
                    spawn(end.getWorld(), Particle.CLOUD, end.clone().add(0, funnelHeight / 2, 0), 130,
                            funnelRadius, funnelHeight / 2, funnelRadius, 0.07);
                    end.getWorld().playSound(end, Sound.ENTITY_PHANTOM_FLAP, 1.6f, 1.5f);
                    cancel();
                    return;
                }

                center.add(dir.clone().multiply(speed));
                travelled += speed;
                Location groundBase = highestSolidGround(center);

                // --- Entonnoir : anneaux qui s'élargissent avec la hauteur et tournent en avançant ---
                int rings = 7;
                for (int r = 0; r < rings; r++) {
                    double t = r / (double) (rings - 1);
                    double y = t * funnelHeight;
                    double ringRadius = funnelRadius * (0.35 + 0.65 * t);
                    double angle = tick * 0.55 + r * 0.9;
                    int points = 10 + r * 2;
                    for (int i = 0; i < points; i++) {
                        double a = angle + (2 * Math.PI / points) * i;
                        double x = Math.cos(a) * ringRadius;
                        double z = Math.sin(a) * ringRadius;
                        Location p = groundBase.clone().add(x, y, z);
                        spawn(p.getWorld(), Particle.CLOUD, p, 1, 0, 0, 0, 0);
                        if (i % 3 == 0) {
                            spawn(p.getWorld(), Particle.BLOCK_CRUMBLE, p, 1, 0, 0, 0, 0);
                        }
                    }
                }

                if (tick % 8 == 0) {
                    groundBase.getWorld().playSound(groundBase, Sound.ENTITY_PHANTOM_FLAP, 1.4f, 0.8f);
                }

                // --- Arrache des blocs meubles autour de la base et les fait tourbillonner ---
                if (tick % 2 == 0) {
                    int lifted = 0;
                    for (int i = 0; i < 6 && lifted < 3; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = Math.random() * funnelRadius;
                        int bx = (int) Math.floor(groundBase.getX() + Math.cos(angle) * dist);
                        int bz = (int) Math.floor(groundBase.getZ() + Math.sin(angle) * dist);
                        int topY = groundBase.getWorld().getHighestBlockYAt(bx, bz);
                        for (int dy = 0; dy >= -2; dy--) {
                            var block = groundBase.getWorld().getBlockAt(bx, topY + dy, bz);
                            if (isTornadoLiftable(block.getType())) {
                                var data = block.getBlockData();
                                block.setType(Material.AIR);
                                FallingBlock falling = block.getWorld().spawnFallingBlock(
                                        block.getLocation().add(0.5, 0.3, 0.5), data);
                                falling.setDropItem(false);
                                falling.setHurtEntities(false);
                                Vector spin = new Vector(Math.cos(angle) * 0.3,
                                        0.55 + Math.random() * 0.3, Math.sin(angle) * 0.3);
                                falling.setVelocity(spin);
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        if (!falling.isDead()) falling.remove();
                                    }
                                }.runTaskLater(plugin, 30L);
                                lifted++;
                                break;
                            }
                        }
                    }
                }

                // --- Aspire, soulève et endommage les entités prises dans l'entonnoir ---
                for (Entity nearby : groundBase.getWorld().getNearbyEntities(
                        groundBase.clone().add(0, funnelHeight / 2, 0), funnelRadius + 1.5, funnelHeight, funnelRadius + 1.5)) {
                    if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                        Vector toCenter = groundBase.toVector().subtract(target.getLocation().toVector());
                        toCenter.setY(0);
                        if (toCenter.lengthSquared() > 0.01) toCenter.normalize();
                        target.setVelocity(toCenter.multiply(0.45).setY(0.32));

                        Integer last = lastHitTick.get(target.getUniqueId());
                        if (last == null || tick - last >= 10) {
                            target.damage(damagePerTick, caster);
                            lastHitTick.put(target.getUniqueId(), tick);
                        }
                    }
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Colonne tournante immobile centrée sur le joueur (contrairement à
     * movingTornado, elle ne se déplace pas) : anneaux à hauteurs
     * croissantes qui tournent en continu pendant `durationTicks`. Purement
     * visuel, à combiner avec pullAoe/areaOverTime pour les mécaniques —
     * utilisé pour donner une vraie silhouette de tornade aux sorts de
     * cyclone stationnaires (Tornade Miniature, Cyclone...).
     */
    public static void spinningFunnelAroundCaster(Plugin plugin, Player caster, double radius, double height, int durationTicks) {
        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= durationTicks || !caster.isOnline()) {
                    cancel();
                    return;
                }
                Location origin = caster.getLocation();
                int rings = 5;
                for (int r = 0; r < rings; r++) {
                    double t = r / (double) (rings - 1);
                    double y = t * height;
                    double ringRadius = radius * (0.4 + 0.6 * t);
                    double angle = elapsed * 0.45 + r * 1.1;
                    int points = 8 + r * 2;
                    for (int i = 0; i < points; i++) {
                        double a = angle + (2 * Math.PI / points) * i;
                        double x = Math.cos(a) * ringRadius;
                        double z = Math.sin(a) * ringRadius;
                        Location p = origin.clone().add(x, y, z);
                        spawn(p.getWorld(), Particle.CLOUD, p, 1, 0, 0, 0, 0);
                    }
                }
                elapsed += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /**
     * Éclats de "foudre" purement visuels (colonne de particules de vent +
     * son de grondement à intervalles aléatoires) pour donner de la
     * spectacle à un sort de tempête sans les complications d'une vraie
     * foudre vanilla (feu, dégâts en plus). N'inflige aucun dégât.
     */
    public static void lightningFlair(Plugin plugin, Location center, double radius, int durationTicks, int periodTicks) {
        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= durationTicks) {
                    cancel();
                    return;
                }
                double angle = Math.random() * 2 * Math.PI;
                double dist = Math.random() * radius;
                Location strike = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                Location ground = highestSolidGround(strike);
                for (double y = 0; y < 6; y += 0.4) {
                    spawn(ground.getWorld(), Particle.CLOUD, ground.clone().add(0, y, 0), 2, 0.05, 0, 0.05, 0.01);
                }
                ground.getWorld().playSound(ground, Sound.ENTITY_PHANTOM_FLAP, 1.6f, 0.5f);
                elapsed += periodTicks;
            }
        }.runTaskTimer(plugin, 0L, periodTicks);
    }

    /** Petit cône de particules jeté devant le joueur au moment du cast, pour ponctuer un lancer de projectile. */
    public static void windBurstCone(Player caster, Particle particle, Sound sound) {
        Location eye = caster.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        caster.getWorld().playSound(eye, sound, 1.1f, 1.3f);
        for (int i = 0; i < 18; i++) {
            double spread = (Math.random() - 0.5) * 0.9;
            Vector v = dir.clone().add(new Vector(spread, spread * 0.5, spread)).normalize().multiply(0.6 + Math.random() * 0.5);
            Location p = eye.clone().add(v);
            spawn(p.getWorld(), particle, p, 2, 0.05, 0.05, 0.05, 0.02);
        }
    }
}
