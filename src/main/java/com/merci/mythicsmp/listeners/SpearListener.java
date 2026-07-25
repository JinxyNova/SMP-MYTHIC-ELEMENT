package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.items.ItemRegistry;
import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.items.weapons.SpearItem;
import com.merci.mythicsmp.utils.ItemIdentifier;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Les lances utilisent le trident vanilla comme support technique
 * (mécanique de vol déjà gérée par le jeu), mais on personnalise :
 *  - les dégâts au lancer selon le tier de la lance
 *  - une traînée de particules pendant le vol
 *  - un cooldown propre par joueur et par tier
 */
public class SpearListener implements Listener {

    private final Plugin plugin;
    private final ItemRegistry registry;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public SpearListener(Plugin plugin, ItemRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof Player player)) return;

        SpearItem spear = findSpear(trident);
        if (spear == null) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long ready = cooldowns.get(uuid);
        if (ready != null && now < ready) {
            trident.remove();
            long secondsLeft = (ready - now) / 1000 + 1;
            player.sendMessage("§cLance en recharge encore " + secondsLeft + "s.");
            return;
        }
        cooldowns.put(uuid, now + spear.getCooldownSeconds() * 1000L);

        Particle trailParticle = trailParticleFor(spear);

        // Traînée de particules tant que la lance vole, différente selon le tier
        new BukkitRunnable() {
            @Override
            public void run() {
                if (trident.isDead() || !trident.isValid()) {
                    cancel();
                    return;
                }
                trident.getWorld().spawnParticle(trailParticle, trident.getLocation(), 3, 0.05, 0.05, 0.05, 0.01);
                if (trailParticle != Particle.CRIT) {
                    trident.getWorld().spawnParticle(Particle.CRIT, trident.getLocation(), 1, 0.02, 0.02, 0.02, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        trident.getWorld().playSound(trident.getLocation(), org.bukkit.Sound.ITEM_TRIDENT_THROW, 1f, 1f);
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        SpearItem spear = findSpear(trident);
        if (spear == null) return;
        Particle impactParticle = trailParticleFor(spear);
        trident.getWorld().spawnParticle(impactParticle, trident.getLocation(), 25, 0.3, 0.3, 0.3, 0.08);
        trident.getWorld().spawnParticle(Particle.CRIT, trident.getLocation(), 15, 0.2, 0.2, 0.2, 0.1);
        trident.getWorld().playSound(trident.getLocation(), org.bukkit.Sound.ITEM_TRIDENT_HIT_GROUND, 1f, 1f);
    }

    /** Une identité visuelle par tier plutôt que la même traînée pour les trois lances. */
    private Particle trailParticleFor(SpearItem spear) {
        return switch (spear.getId()) {
            case "lance_netherite" -> Particle.SOUL;
            case "lance_celeste" -> Particle.END_ROD;
            default -> Particle.CRIT; // lance_fer
        };
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Trident trident)) return;
        SpearItem spear = findSpear(trident);
        if (spear == null) return;
        event.setDamage(spear.getThrowDamage());
    }

    private SpearItem findSpear(Trident trident) {
        String id = ItemIdentifier.getId(plugin, trident.getItem());
        if (id == null) return null;
        MythicItem item = registry.get(id);
        return (item instanceof SpearItem spear) ? spear : null;
    }
}
