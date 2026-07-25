package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.items.Ids;
import com.merci.mythicsmp.items.ItemRegistry;
import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.items.mythic.PhoenixHeartItem;
import com.merci.mythicsmp.utils.ItemIdentifier;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * Tous les objets de combat corps-à-corps "simples" (un effet déclenché au
 * coup porté ou au blocage) sont regroupés ici plutôt que dans un listener
 * par objet — le comportement change, mais le point d'accroche
 * (EntityDamageByEntityEvent) est le même à chaque fois.
 */
public class CombatEffectsListener implements Listener {

    private final Plugin plugin;
    private final ItemRegistry registry;
    private final Random random = new Random();

    public CombatEffectsListener(Plugin plugin, ItemRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler
    public void onMeleeHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        String id = ItemIdentifier.getId(plugin, weapon);
        if (id == null) return;

        boolean upgraded = isUpgraded(weapon);

        switch (id) {
            case Ids.MARTEAU_GUERRE -> knockbackNearby(target, upgraded ? 2.0 : 1.4);
            case Ids.HACHE_TONNERRE -> {
                if (random.nextDouble() < (upgraded ? 0.45 : 0.25)) {
                    target.getWorld().strikeLightning(target.getLocation());
                }
            }
            case Ids.DAGUE_POISON -> {
                int currentAmplifier = target.hasPotionEffect(PotionEffectType.POISON)
                        ? target.getPotionEffect(PotionEffectType.POISON).getAmplifier() + 1
                        : 0;
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100,
                        Math.min(currentAmplifier, upgraded ? 6 : 4)));
            }
            default -> { /* rien à faire pour les autres armes ici (Fouet -> clic droit, voir UtilityItemsListener) */ }
        }

        // Bouclier miroir : si la VICTIME du coup (donc pas l'attaquant ci-dessus,
        // mais le joueur qui se prend le coup) bloque avec ce bouclier, on renvoie.
        if (event.getEntity() instanceof Player defender
                && defender.isBlocking()
                && ItemIdentifier.hasId(plugin, defender.getInventory().getItemInMainHand(), Ids.BOUCLIER_MIROIR)
                && event.getDamager() instanceof LivingEntity attacker) {
            boolean shieldUpgraded = isUpgraded(defender.getInventory().getItemInMainHand());
            double reflectRatio = shieldUpgraded ? 0.75 : 0.5;
            double reflected = event.getFinalDamage() * reflectRatio;
            event.setDamage(event.getFinalDamage() * 0.3); // le porteur encaisse moins
            attacker.damage(reflected, defender);
            attacker.getWorld().spawnParticle(Particle.END_ROD, attacker.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.02);
        }
    }

    // Garde-fou : living.damage() plus bas déclenche un NOUVEL EntityDamageByEntityEvent.
    // Sans ce verrou, ce nouvel event repasse par onGauntletPunch tant que le joueur tient
    // toujours le gantelet -> boucle infinie -> StackOverflowError -> crash serveur.
    private boolean handlingGauntletExplosion = false;

    @EventHandler
    public void onGauntletPunch(EntityDamageByEntityEvent event) {
        if (handlingGauntletExplosion) return;
        if (!(event.getDamager() instanceof Player player)) return;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!ItemIdentifier.hasId(plugin, hand, Ids.GANTELET_EXPLOSIF)) return;

        Location loc = event.getEntity().getLocation();
        boolean upgraded = isUpgraded(hand);
        double radius = upgraded ? 3.5 : 2.5;
        double damage = upgraded ? 4.0 : 2.0;
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
        loc.getWorld().spawnParticle(Particle.LAVA, loc, 6, 0.3, 0.3, 0.3, 0);
        loc.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.2f);

        handlingGauntletExplosion = true;
        try {
            for (org.bukkit.entity.Entity nearby : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                if (nearby instanceof LivingEntity living && !nearby.equals(player) && !nearby.equals(event.getEntity())) {
                    living.damage(damage, player);
                    Vector push = living.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.6);
                    living.setVelocity(living.getVelocity().add(push.setY(0.3)));
                }
            }
        } finally {
            handlingGauntletExplosion = false;
        }
    }

    @EventHandler
    public void onScytheKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (!ItemIdentifier.hasId(plugin, weapon, Ids.FAUX_MOISSON)) return;

        event.setDroppedExp(event.getDroppedExp() + 5);
        if (random.nextDouble() < 0.10) {
            MythicItem legendaryOrBetter = randomHighRarityItem();
            if (legendaryOrBetter != null) {
                event.getEntity().getWorld().dropItem(event.getEntity().getLocation(), legendaryOrBetter.build());
                killer.sendMessage("§6La Faux de la Moisson t'offre un objet rare !");
            }
        }
    }

    private void knockbackNearby(LivingEntity center, double strength) {
        for (org.bukkit.entity.Entity nearby : center.getWorld().getNearbyEntities(center.getLocation(), 3, 2, 3)) {
            if (nearby instanceof LivingEntity living) {
                Vector push = living.getLocation().toVector().subtract(center.getLocation().toVector());
                if (push.lengthSquared() == 0) continue;
                push.normalize().multiply(strength).setY(0.35);
                living.setVelocity(living.getVelocity().add(push));
            }
        }
        groundShockwave(center.getLocation());
    }

    /** Anneau de particules qui s'étend au sol depuis l'impact, façon "onde de choc". */
    private void groundShockwave(Location center) {
        center.getWorld().playSound(center, org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.6f);
        center.getWorld().playSound(center, org.bukkit.Sound.BLOCK_ANVIL_LAND, 0.6f, 0.7f);
        new org.bukkit.scheduler.BukkitRunnable() {
            double radius = 0.4;
            @Override
            public void run() {
                if (radius > 3.4) {
                    cancel();
                    return;
                }
                for (double angle = 0; angle < 360; angle += 12) {
                    double rad = Math.toRadians(angle);
                    Location point = center.clone().add(Math.cos(rad) * radius, 0.1, Math.sin(rad) * radius);
                    center.getWorld().spawnParticle(Particle.CRIT, point, 1, 0, 0, 0, 0);
                    center.getWorld().spawnParticle(Particle.CLOUD, point, 1, 0, 0.02, 0, 0.01);
                }
                radius += 0.45;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean isUpgraded(ItemStack weapon) {
        if (weapon == null || weapon.getItemMeta() == null) return false;
        Boolean value = weapon.getItemMeta().getPersistentDataContainer()
                .get(MythicItem.upgradedKey(plugin), PersistentDataType.BOOLEAN);
        return Boolean.TRUE.equals(value);
    }

    private MythicItem randomHighRarityItem() {
        var candidates = registry.all().values().stream()
                .filter(item -> item.getRarity() == com.merci.mythicsmp.items.ItemRarity.RARE
                        || item.getRarity() == com.merci.mythicsmp.items.ItemRarity.EPIQUE)
                .filter(item -> !item.getId().equals(PhoenixHeartItem.ID)) // pas de coeur du phénix en drop aléatoire
                .toList();
        if (candidates.isEmpty()) return null;
        return candidates.get(random.nextInt(candidates.size()));
    }
}
