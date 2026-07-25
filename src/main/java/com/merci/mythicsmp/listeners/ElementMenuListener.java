package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.elements.Element;
import com.merci.mythicsmp.elements.ElementManager;
import com.merci.mythicsmp.gui.ElementMenuHolder;
import com.merci.mythicsmp.items.mythic.InfinitePowerGemItem;
import com.merci.mythicsmp.utils.ItemIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

/**
 * Gère les clics dans le menu de choix d'élément (voir ElementMenuManager),
 * dans ses deux modes : choix de la classe de départ (définitif) et
 * déblocage d'un élément supplémentaire (consomme une Gemme au Pouvoir
 * Infini dans l'inventaire du joueur).
 */
public class ElementMenuListener implements Listener {

    private final Plugin plugin;
    private final ElementManager elementManager;

    public ElementMenuListener(Plugin plugin, ElementManager elementManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ElementMenuHolder holder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) return;

        Element element = ElementMenuHolder.SLOT_ELEMENTS.get(event.getRawSlot());
        if (element == null) return;

        if (holder.getMode() == ElementMenuHolder.Mode.STARTER) {
            handleStarterChoice(player, element);
        } else {
            handleUnlock(player, element);
        }
    }

    private void handleStarterChoice(Player player, Element element) {
        boolean chosen = elementManager.chooseStarter(player.getUniqueId(), element);
        if (!chosen) {
            player.sendMessage(Component.text("Tu as déjà choisi ta classe de départ.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }
        player.sendMessage(Component.text(
                "Tu as choisi l'élément " + element.getLabel() + " comme classe de départ !",
                element.getColor()));
        player.sendMessage(Component.text("Ce choix est définitif — bonne chance !", NamedTextColor.GRAY));
        player.closeInventory();
    }

    private void handleUnlock(Player player, Element element) {
        if (elementManager.hasElement(player.getUniqueId(), element)) {
            player.sendMessage(Component.text("Tu maîtrises déjà cet élément.", NamedTextColor.YELLOW));
            return;
        }
        if (!removeOneGem(player)) {
            player.sendMessage(Component.text(
                    "Il te faut une Gemme au Pouvoir Infini pour débloquer un élément.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }
        boolean unlocked = elementManager.unlockElement(player.getUniqueId(), element);
        if (!unlocked) {
            // Ne devrait pas arriver (gemme déjà consommée) mais on rembourse par sécurité.
            player.getInventory().addItem(new InfinitePowerGemItem(plugin).build());
            player.sendMessage(Component.text("Impossible de débloquer cet élément.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }
        player.sendMessage(Component.text(
                "Tu maîtrises désormais l'élément " + element.getLabel() + " !", element.getColor()));
        String rank = elementManager.getRankLabel(player.getUniqueId());
        if (rank != null) {
            player.sendMessage(Component.text("Nouveau grade : " + rank, NamedTextColor.LIGHT_PURPLE));
        }
        player.closeInventory();
    }

    /** Retire un exemplaire de la Gemme au Pouvoir Infini de l'inventaire du joueur. */
    private boolean removeOneGem(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (ItemStack stack : inventory.getContents()) {
            if (ItemIdentifier.hasId(plugin, stack, InfinitePowerGemItem.ID)) {
                stack.setAmount(stack.getAmount() - 1);
                return true;
            }
        }
        return false;
    }
}
