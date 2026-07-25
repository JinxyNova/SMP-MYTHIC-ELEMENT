package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.elements.ElementManager;
import com.merci.mythicsmp.items.mythic.UltimateMageGemItem;
import com.merci.mythicsmp.ultimate.UltimateMageManager;
import com.merci.mythicsmp.utils.ItemIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Clic droit sur la Gemme du Mage Ultime : débloque directement la classe
 * Ultime Mage et ses 5 sorts (pas de menu de choix, contrairement à la
 * Gemme au Pouvoir Infini — voir UltimateMageManager), à condition que le
 * joueur maîtrise déjà les 4 éléments.
 */
public class UltimateMageGemListener implements Listener {

    private final Plugin plugin;
    private final ElementManager elementManager;
    private final UltimateMageManager ultimateMageManager;

    public UltimateMageGemListener(Plugin plugin, ElementManager elementManager, UltimateMageManager ultimateMageManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
        this.ultimateMageManager = ultimateMageManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!ItemIdentifier.hasId(plugin, hand, UltimateMageGemItem.ID)) return;

        event.setCancelled(true);

        if (ultimateMageManager.hasClass(player.getUniqueId())) {
            player.sendMessage(Component.text("Tu es déjà Mage Ultime !", NamedTextColor.LIGHT_PURPLE));
            return;
        }
        if (!elementManager.isMaxed(player.getUniqueId())) {
            player.sendMessage(Component.text(
                    "Tu dois d'abord maîtriser les 4 éléments avant d'utiliser cette gemme.", NamedTextColor.RED));
            return;
        }

        hand.setAmount(hand.getAmount() - 1);
        ultimateMageManager.unlock(player.getUniqueId());

        player.sendMessage(Component.text(
                "Les 4 éléments fusionnent en toi... tu es devenu MAGE ULTIME !", NamedTextColor.LIGHT_PURPLE));
        player.sendMessage(Component.text(
                "Tape /mythicultimate pour accéder à tes 5 nouveaux sorts.", NamedTextColor.GRAY));
        player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, player.getLocation().add(0, 1, 0), 60, 0.6, 1, 0.6, 0.05);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.4f);

        plugin.getServer().broadcast(Component.text(
                player.getName() + " est devenu MAGE ULTIME, maître des 4 éléments !", NamedTextColor.LIGHT_PURPLE));
    }
}
