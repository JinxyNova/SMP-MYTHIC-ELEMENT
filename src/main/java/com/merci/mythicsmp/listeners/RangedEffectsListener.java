package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.items.Ids;
import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.utils.ItemIdentifier;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.Particle;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * L'Arc du Vent se reconnaît via son NamespacedKey au moment du tir
 * (EntityShootBowEvent). On marque la flèche avec une metadata simple pour
 * la retrouver à l'impact, sans avoir besoin de PersistentDataContainer
 * sur un projectile (plus simple pour un objet éphémère comme une flèche).
 */
public class RangedEffectsListener implements Listener {

    private static final String METADATA_KEY = "mythicsmp_wind_arrow";
    private static final String METADATA_UPGRADED = "mythicsmp_wind_arrow_upgraded";

    private final Plugin plugin;

    public RangedEffectsListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getBow() == null) return;
        if (!ItemIdentifier.hasId(plugin, event.getBow(), Ids.ARC_VENT)) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;

        arrow.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, true));

        Boolean bowUpgraded = event.getBow().getItemMeta().getPersistentDataContainer()
                .get(MythicItem.upgradedKey(plugin), PersistentDataType.BOOLEAN);
        if (Boolean.TRUE.equals(bowUpgraded)) {
            arrow.setMetadata(METADATA_UPGRADED, new FixedMetadataValue(plugin, true));
        }

        // Traînée de vent tant que la flèche est en vol, pour qu'on la voie
        // vraiment "trancher l'air" plutôt que voler comme une flèche normale.
        new BukkitRunnable() {
            @Override
            public void run() {
                if (arrow.isDead() || !arrow.isValid()) {
                    cancel();
                    return;
                }
                arrow.getWorld().spawnParticle(Particle.CLOUD, arrow.getLocation(), 2, 0.05, 0.05, 0.05, 0.001);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!arrow.hasMetadata(METADATA_KEY)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        double strength = arrow.hasMetadata(METADATA_UPGRADED) ? 4.0 : 2.5;
        Vector push = arrow.getVelocity().normalize().multiply(strength);
        target.setVelocity(target.getVelocity().add(push.setY(0.4)));
        target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation(), 25, 0.4, 0.4, 0.4, 0.08);
        target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 1);
        target.getWorld().playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PHANTOM_FLAP, 1.2f, 0.8f);
    }
}
