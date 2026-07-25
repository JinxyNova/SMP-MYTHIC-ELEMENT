package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.items.Ids;
import com.merci.mythicsmp.items.mythic.AdminHeadItem;
import com.merci.mythicsmp.utils.ItemIdentifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Tourne toutes les 2 secondes (40 ticks) et vérifie, pour chaque joueur en
 * ligne, s'il porte ou tient un objet à effet passif. Un seul scan périodique
 * pour tous ces objets plutôt qu'une tâche par objet — moins de charge et
 * plus simple à étendre : ajouter un `case` dans `applyPassives`.
 */
public class PassiveEquipmentTask extends BukkitRunnable {

    private final Plugin plugin;

    public PassiveEquipmentTask(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        runTaskTimer(plugin, 0L, 10L);
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            applyHelmetEffects(player);
            applyHeldEffects(player);
            applyElytraBoost(player);
        }
    }

    private void applyHelmetEffects(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null) return;

        if (ItemIdentifier.hasId(plugin, helmet, AdminHeadItem.ID)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, AdminHeadItem.REGEN_LEVEL, true, false));
        }
        if (ItemIdentifier.hasId(plugin, helmet, Ids.COURONNE_ROI)) {
            for (Entity nearby : player.getNearbyEntities(10, 10, 10)) {
                if (nearby instanceof Player ally) {
                    ally.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 60, 0, true, false));
                }
            }
        }
    }

    private void applyHeldEffects(Player player) {
        // "Portée dans l'inventaire" pour l'amulette : on vérifie main + offhand,
        // simple et suffisant sans avoir à scanner tout l'inventaire chaque tick.
        boolean hasTimeAmulet = ItemIdentifier.hasId(plugin, player.getInventory().getItemInMainHand(), Ids.AMULETTE_TEMPS)
                || ItemIdentifier.hasId(plugin, player.getInventory().getItemInOffHand(), Ids.AMULETTE_TEMPS)
                || containsInInventory(player, Ids.AMULETTE_TEMPS);
        if (hasTimeAmulet) {
            for (Entity nearby : player.getNearbyEntities(6, 6, 6)) {
                if (nearby instanceof LivingEntity living && !(nearby instanceof Player)) {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, false));
                }
            }
        }

        boolean hasSoulLantern = ItemIdentifier.hasId(plugin, player.getInventory().getItemInMainHand(), Ids.LANTERNE_AME)
                || ItemIdentifier.hasId(plugin, player.getInventory().getHelmet(), Ids.LANTERNE_AME)
                || containsInInventory(player, Ids.LANTERNE_AME);
        if (hasSoulLantern) {
            for (Entity nearby : player.getNearbyEntities(8, 8, 8)) {
                if (nearby instanceof org.bukkit.entity.Monster monster) {
                    org.bukkit.util.Vector push = monster.getLocation().toVector()
                            .subtract(player.getLocation().toVector());
                    if (push.lengthSquared() == 0) continue;
                    push.normalize().multiply(0.75);
                    monster.setVelocity(monster.getVelocity().add(push.setY(0.25)));
                    monster.getWorld().spawnParticle(org.bukkit.Particle.SOUL, monster.getLocation().add(0, 0.5, 0), 2, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }
    }

    private void applyElytraBoost(Player player) {
        if (!player.isGliding()) return;
        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null || !ItemIdentifier.hasId(plugin, chest, Ids.ELYTRES_VENT)) return;

        org.bukkit.util.Vector boost = player.getLocation().getDirection().normalize().multiply(0.6);
        player.setVelocity(player.getVelocity().add(boost));
        player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, player.getLocation(), 10, 0.2, 0.2, 0.2, 0.02);
    }

    private boolean containsInInventory(Player player, String id) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && ItemIdentifier.hasId(plugin, stack, id)) return true;
        }
        return false;
    }
}
