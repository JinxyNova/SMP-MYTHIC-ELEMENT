package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.items.mythic.PhoenixHeartItem;
import com.merci.mythicsmp.utils.ItemIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

/**
 * Si des dégâts sur un joueur seraient fatals et qu'il possède un Cœur du
 * Phénix quelque part dans son inventaire, on annule les dégâts, on le
 * soigne et on consomme un exemplaire de l'objet.
 */
public class PhoenixHeartListener implements Listener {

    private final Plugin plugin;

    public PhoenixHeartListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getHealth() - event.getFinalDamage() > 0) return; // pas fatal, rien à faire

        PlayerInventory inventory = player.getInventory();
        int slot = findPhoenixHeartSlot(inventory);
        if (slot == -1) return;

        event.setCancelled(true);

        ItemStack stack = inventory.getItem(slot);
        stack.setAmount(stack.getAmount() - 1);

        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.min(maxHealth, PhoenixHeartItem.HEAL_HEARTS));
        player.setFireTicks(0);

        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.05);
        player.sendMessage(Component.text("Le Cœur du Phénix t'a ramené d'entre les morts !", NamedTextColor.GOLD));
    }

    private int findPhoenixHeartSlot(PlayerInventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && ItemIdentifier.hasId(plugin, item, PhoenixHeartItem.ID)) {
                return i;
            }
        }
        return -1;
    }
}
