package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.items.Ids;
import com.merci.mythicsmp.utils.ItemIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Clic droit avec l'Œil du Wither en main : les entités vivantes dans un
 * rayon de 20 blocs reçoivent Glowing (contour visible à travers les murs)
 * pendant quelques secondes. Cooldown géré côté joueur pour éviter le spam.
 */
public class WitherEyeListener implements Listener {

    private static final int RADIUS = 20;
    private static final int GLOW_TICKS = 100; // 5 secondes
    private static final long COOLDOWN_MS = 15_000;

    private final Plugin plugin;
    private final java.util.Map<java.util.UUID, Long> cooldowns = new java.util.HashMap<>();

    public WitherEyeListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        if (!ItemIdentifier.hasId(plugin, player.getInventory().getItemInMainHand(), Ids.OEIL_WITHER)) return;

        long now = System.currentTimeMillis();
        Long ready = cooldowns.get(player.getUniqueId());
        if (ready != null && now < ready) {
            player.sendMessage(Component.text("L'Œil du Wither doit encore se recharger.", NamedTextColor.RED));
            return;
        }
        cooldowns.put(player.getUniqueId(), now + COOLDOWN_MS);

        for (Entity nearby : player.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (nearby instanceof LivingEntity living) {
                living.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, GLOW_TICKS, 0));
            }
        }
        player.getWorld().spawnParticle(org.bukkit.Particle.SOUL, player.getLocation(), 30, 1, 1, 1, 0.05);
        player.sendMessage(Component.text("Tu perçois toutes les créatures proches.", NamedTextColor.LIGHT_PURPLE));
        event.setCancelled(true);
    }
}
