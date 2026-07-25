package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.elements.ElementManager;
import com.merci.mythicsmp.elements.ElementMenuManager;
import com.merci.mythicsmp.items.mythic.InfinitePowerGemItem;
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
 * Clic droit sur la Gemme au Pouvoir Infini : ouvre le menu de déblocage
 * d'un nouvel élément (la gemme n'est consommée qu'en cas de choix réussi,
 * voir ElementMenuListener).
 */
public class InfinitePowerGemListener implements Listener {

    private final Plugin plugin;
    private final ElementManager elementManager;
    private final ElementMenuManager menuManager;

    public InfinitePowerGemListener(Plugin plugin, ElementManager elementManager, ElementMenuManager menuManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
        this.menuManager = menuManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!ItemIdentifier.hasId(plugin, hand, InfinitePowerGemItem.ID)) return;

        event.setCancelled(true);

        if (!elementManager.hasChosenStarter(player.getUniqueId())) {
            player.sendMessage(Component.text(
                    "Choisis d'abord ta classe de départ avant d'utiliser cette gemme.", NamedTextColor.RED));
            return;
        }
        if (elementManager.isMaxed(player.getUniqueId())) {
            player.sendMessage(Component.text(
                    "Tu maîtrises déjà les 4 éléments !", NamedTextColor.LIGHT_PURPLE));
            return;
        }
        menuManager.openUnlockMenu(player);
    }
}
