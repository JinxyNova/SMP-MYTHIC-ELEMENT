package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.items.weapons.FireSwordItem;
import com.merci.mythicsmp.items.weapons.IceSwordItem;
import com.merci.mythicsmp.utils.ItemIdentifier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Applique les effets spéciaux des épées élémentaires quand un joueur
 * touche une cible en combat au corps-à-corps. Si l'épée a été améliorée
 * par une gemme (voir GemFusionListener), les effets sont renforcés.
 */
public class WeaponEffectListener implements Listener {

    private final Plugin plugin;

    public WeaponEffectListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMeleeHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        String id = ItemIdentifier.getId(plugin, weapon);
        if (id == null) return;

        boolean upgraded = isUpgraded(weapon);

        if (FireSwordItem.ID.equals(id)) {
            applyFire(target, upgraded);
        } else if (IceSwordItem.ID.equals(id)) {
            applyIce(target, upgraded);
        }
    }

    private void applyFire(LivingEntity target, boolean upgraded) {
        int ticks = upgraded ? FireSwordItem.FIRE_TICKS * 2 : FireSwordItem.FIRE_TICKS;
        target.setFireTicks(ticks);

        double radius = upgraded ? FireSwordItem.RING_RADIUS + 1 : FireSwordItem.RING_RADIUS;
        Location center = target.getLocation();
        target.getWorld().spawnParticle(org.bukkit.Particle.FLAME, center, 30, radius / 2, 0.3, radius / 2, 0.02);

        // Anneau de feu temporaire (ne casse aucun bloc, juste des flammes posées puis retirées)
        for (double angle = 0; angle < 360; angle += 30) {
            double rad = Math.toRadians(angle);
            Block block = center.clone()
                    .add(Math.cos(rad) * radius, 0, Math.sin(rad) * radius)
                    .getBlock();
            Block below = block.getRelative(0, -1, 0);
            if (below.getType().isSolid() && block.getType() == Material.AIR) {
                Material previous = block.getType();
                block.setType(Material.FIRE);
                plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> block.setType(previous), 60L);
            }
        }
    }

    private void applyIce(LivingEntity target, boolean upgraded) {
        int amplifier = upgraded ? IceSwordItem.SLOW_LEVEL + 1 : IceSwordItem.SLOW_LEVEL;
        int ticks = upgraded ? IceSwordItem.SLOW_TICKS * 2 : IceSwordItem.SLOW_TICKS;
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, amplifier));

        int radius = upgraded ? IceSwordItem.FREEZE_RADIUS + 1 : IceSwordItem.FREEZE_RADIUS;
        Location center = target.getLocation();
        target.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, center, 25, radius / 2.0, 0.3, radius / 2.0, 0.02);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block block = center.clone().add(x, 0, z).getBlock();
                if (block.getType() == Material.WATER) {
                    block.setType(Material.ICE);
                }
            }
        }
    }

    private boolean isUpgraded(ItemStack weapon) {
        if (weapon.getItemMeta() == null) return false;
        Boolean value = weapon.getItemMeta().getPersistentDataContainer()
                .get(MythicItem.upgradedKey(plugin), PersistentDataType.BOOLEAN);
        return Boolean.TRUE.equals(value);
    }
}
