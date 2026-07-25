package com.merci.mythicsmp.boss;

import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

/**
 * Tourne toutes les 40 ticks (2s) tant que le boss est vivant : met à jour
 * la barre de vie pour les joueurs proches, et déclenche une capacité
 * spéciale régulièrement pour que le combat ne soit pas juste "taper
 * jusqu'à ce que ça meure". Le Gardien a 4 capacités différentes qui
 * s'enchaînent, et devient plus agressif sous 30% de vie.
 */
public class BossAbilityTask extends BukkitRunnable {

    private static final double TRACK_RADIUS = 40;
    private static final double LIGHTNING_CHANCE = 0.22;
    private static final double SUMMON_CHANCE = 0.18;
    private static final double CHARGE_CHANCE = 0.20;
    private static final double HEAL_CHANCE = 0.10;

    private final Plugin plugin;
    private final LivingEntity boss;
    private final BossBar bar;
    private final Random random = new Random();

    public BossAbilityTask(Plugin plugin, LivingEntity boss, BossBar bar) {
        this.plugin = plugin;
        this.boss = boss;
        this.bar = bar;
    }

    public void start() {
        runTaskTimer(plugin, 0L, 40L);
    }

    @Override
    public void run() {
        if (boss.isDead() || !boss.isValid()) {
            bar.removeAll();
            cancel();
            return;
        }

        updateBossBar();
        maybeUseAbility();
    }

    private void updateBossBar() {
        double max = boss.getAttribute(Attribute.MAX_HEALTH).getValue();
        bar.setProgress(Math.max(0.0, Math.min(1.0, boss.getHealth() / max)));

        List<Player> nearby = boss.getNearbyEntities(TRACK_RADIUS, TRACK_RADIUS, TRACK_RADIUS).stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .toList();

        for (Player player : bar.getPlayers()) {
            if (!nearby.contains(player)) bar.removePlayer(player);
        }
        for (Player player : nearby) {
            bar.addPlayer(player);
        }
    }

    private void maybeUseAbility() {
        List<Player> targets = bar.getPlayers();
        if (targets.isEmpty()) return;

        double healthRatio = boss.getHealth() / boss.getAttribute(Attribute.MAX_HEALTH).getValue();
        boolean enraged = healthRatio < 0.3;

        double roll = random.nextDouble();
        double lightning = LIGHTNING_CHANCE;
        double summon = lightning + SUMMON_CHANCE;
        double charge = summon + CHARGE_CHANCE;
        double heal = charge + (healthRatio < 0.5 ? HEAL_CHANCE : 0);

        if (roll < lightning) {
            Player target = targets.get(random.nextInt(targets.size()));
            target.getWorld().strikeLightning(target.getLocation());
            boss.getWorld().playSound(boss.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SHOOT, 1f, 0.6f);
        } else if (roll < summon) {
            summonMinions(enraged ? 3 : 2);
        } else if (roll < charge) {
            chargeAttack(targets.get(random.nextInt(targets.size())));
        } else if (roll < heal) {
            healSelfAndMinions();
        }
    }

    private void summonMinions(int count) {
        Location center = boss.getLocation();
        for (int i = 0; i < count; i++) {
            Location spawnAt = center.clone().add(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            boss.getWorld().spawnEntity(spawnAt, EntityType.ZOMBIFIED_PIGLIN);
        }
        boss.playEffect(EntityEffect.HURT);
        boss.getWorld().spawnParticle(Particle.SOUL, center, 20, 1, 0.5, 1, 0.05);
    }

    /** Fonce brutalement sur une cible et l'envoie en l'air à l'impact. */
    private void chargeAttack(Player target) {
        Location bossLoc = boss.getLocation();
        org.bukkit.util.Vector direction = target.getLocation().toVector().subtract(bossLoc.toVector());
        if (direction.lengthSquared() == 0) return;
        direction.normalize().multiply(1.8);
        boss.setVelocity(direction.setY(0.4));
        boss.getWorld().playSound(bossLoc, org.bukkit.Sound.ENTITY_RAVAGER_ROAR, 1f, 0.8f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (boss.isDead() || !boss.isValid() || ticks >= 15) {
                    cancel();
                    return;
                }
                boss.getWorld().spawnParticle(Particle.CRIT, boss.getLocation(), 3, 0.3, 0.1, 0.3, 0.02);
                if (boss.getLocation().distanceSquared(target.getLocation()) < 4) {
                    target.setVelocity(target.getVelocity().add(new org.bukkit.util.Vector(0, 0.6, 0)));
                    target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 1);
                    cancel();
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Sous 50% de vie, le Gardien peut se soigner et soigner ses sbires proches. */
    private void healSelfAndMinions() {
        double max = boss.getAttribute(Attribute.MAX_HEALTH).getValue();
        boss.setHealth(Math.min(max, boss.getHealth() + max * 0.08));
        boss.getWorld().spawnParticle(Particle.HEART, boss.getLocation().add(0, 1, 0), 15, 0.6, 0.6, 0.6, 0.05);
        boss.getWorld().playSound(boss.getLocation(), org.bukkit.Sound.ENTITY_WITHER_AMBIENT, 1f, 1.4f);

        for (org.bukkit.entity.Entity nearby : boss.getNearbyEntities(6, 4, 6)) {
            if (nearby instanceof LivingEntity living && !(nearby instanceof Player)) {
                living.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1));
            }
        }
    }
}
