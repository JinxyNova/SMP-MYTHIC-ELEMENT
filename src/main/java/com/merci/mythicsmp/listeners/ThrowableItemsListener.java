package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.items.Ids;
import com.merci.mythicsmp.utils.ItemIdentifier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

/**
 * La Bombe de Gel, la Grenade Fumigène et le Totem de Soin sont lancés à la
 * main (clic droit) : on annule le comportement vanilla de l'item tenu,
 * on fait voler une snowball technique à sa place, taguée en metadata pour
 * savoir quel effet appliquer à l'impact.
 */
public class ThrowableItemsListener implements Listener {

    private static final String METADATA_KEY = "mythicsmp_throwable_id";

    private final Plugin plugin;
    private final Random random = new Random();

    public ThrowableItemsListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onThrow(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        String id = ItemIdentifier.getId(plugin, hand);
        if (id == null) return;
        if (!id.equals(Ids.BOMBE_GEL) && !id.equals(Ids.GRENADE_FUMIGENE) && !id.equals(Ids.TOTEM_SOIN)) return;

        event.setCancelled(true);

        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, id));

        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        if (!snowball.hasMetadata(METADATA_KEY)) return;

        String id = snowball.getMetadata(METADATA_KEY).get(0).asString();
        Location impact = snowball.getLocation();

        switch (id) {
            case Ids.BOMBE_GEL -> freezeZone(impact);
            case Ids.GRENADE_FUMIGENE -> smokeZone(impact);
            case Ids.TOTEM_SOIN -> healZone(impact);
            default -> { }
        }
    }

    private void freezeZone(Location impact) {
        impact.getWorld().spawnParticle(Particle.SNOWFLAKE, impact, 40, 2, 1, 2, 0.05);
        for (LivingEntity entity : nearbyLiving(impact, 3)) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));
        }
        int radius = 3;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block block = impact.clone().add(x, 0, z).getBlock();
                if (block.getType() == Material.WATER) {
                    block.setType(Material.ICE);
                }
            }
        }
    }

    private void smokeZone(Location impact) {
        impact.getWorld().playSound(impact, org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 0.6f, 0.4f);

        // Nuage dense et concentré qui reste en place quelques secondes, au lieu
        // d'un unique burst à large dispersion qui donnait l'impression que les
        // particules partaient n'importe où plutôt que de former une vraie fumée.
        new org.bukkit.scheduler.BukkitRunnable() {
            int elapsed = 0;
            @Override
            public void run() {
                if (elapsed >= 80) { // ~4 secondes de nuage
                    cancel();
                    return;
                }
                for (int i = 0; i < 4; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double radius = random.nextDouble() * 1.8;
                    Location point = impact.clone().add(
                            Math.cos(angle) * radius,
                            random.nextDouble() * 1.8,
                            Math.sin(angle) * radius);
                    impact.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, point, 2, 0.1, 0.1, 0.1, 0.005);
                }
                elapsed += 4;
            }
        }.runTaskTimer(plugin, 0L, 2L);

        for (LivingEntity entity : nearbyLiving(impact, 3.5)) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0));
        }
    }

    private void healZone(Location impact) {
        impact.getWorld().spawnParticle(Particle.HEART, impact, 20, 1.5, 1, 1.5, 0.05);
        new org.bukkit.scheduler.BukkitRunnable() {
            int elapsed = 0;
            @Override
            public void run() {
                if (elapsed >= 300) { // 15 secondes de soin (au lieu de 5)
                    cancel();
                    return;
                }
                impact.getWorld().spawnParticle(Particle.HEART, impact.clone().add(0, 0.3, 0), 4, 1.3, 0.4, 1.3, 0.0);
                for (Player player : impact.getWorld().getPlayers()) {
                    if (player.getLocation().distanceSquared(impact) <= 20.25) { // rayon 4.5
                        double max = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                        player.setHealth(Math.min(max, player.getHealth() + 1));
                    }
                }
                elapsed += 20;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private Iterable<LivingEntity> nearbyLiving(Location center, double radius) {
        return center.getWorld().getNearbyEntities(center, radius, radius, radius).stream()
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .toList();
    }
}
