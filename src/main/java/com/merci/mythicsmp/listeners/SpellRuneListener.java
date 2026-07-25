package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.elements.Element;
import com.merci.mythicsmp.elements.ElementManager;
import com.merci.mythicsmp.items.ItemRegistry;
import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.items.mythic.SpellRuneItem;
import com.merci.mythicsmp.spells.SpellTier;
import com.merci.mythicsmp.spells.SpellWheelManager;
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
 * Clic droit sur une Rune (voir SpellRuneItem) : ouvre le menu de
 * déblocage du palier correspondant, si le joueur maîtrise déjà
 * l'élément de cette rune.
 */
public class SpellRuneListener implements Listener {

    private final Plugin plugin;
    private final ItemRegistry itemRegistry;
    private final ElementManager elementManager;
    private final SpellWheelManager wheelManager;

    public SpellRuneListener(Plugin plugin, ItemRegistry itemRegistry, ElementManager elementManager, SpellWheelManager wheelManager) {
        this.plugin = plugin;
        this.itemRegistry = itemRegistry;
        this.elementManager = elementManager;
        this.wheelManager = wheelManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        for (Element element : Element.values()) {
            for (SpellTier tier : SpellTier.values()) {
                String runeId = SpellRuneItem.idFor(element, tier);
                MythicItem item = itemRegistry.get(runeId);
                if (item == null || !item.matches(hand)) continue;

                event.setCancelled(true);
                if (!elementManager.hasElement(player.getUniqueId(), element)) {
                    player.sendMessage(Component.text(
                            "Tu dois d'abord maîtriser l'élément " + element.getLabel() + " pour utiliser cette rune.",
                            NamedTextColor.RED));
                    return;
                }
                wheelManager.openUnlock(player, element, tier);
                return;
            }
        }
    }
}
