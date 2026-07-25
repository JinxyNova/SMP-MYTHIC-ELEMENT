package com.merci.mythicsmp.elements;

import com.merci.mythicsmp.gui.ElementMenuHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Construit le menu (27 cases) affichant les 4 éléments et l'ouvre pour un
 * joueur, que ce soit pour le choix initial et définitif de la classe de
 * départ, ou pour débloquer un élément supplémentaire avec la Gemme au
 * Pouvoir Infini. Le remplissage/décoration est séparé du listener de clic
 * (ElementMenuListener) pour rester facile à ajuster.
 */
public class ElementMenuManager {

    private final Plugin plugin;
    private final ElementManager elementManager;

    public ElementMenuManager(Plugin plugin, ElementManager elementManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
    }

    public void openStarterMenu(Player player) {
        open(player, ElementMenuHolder.Mode.STARTER, "Choisis ta classe élémentaire");
    }

    public void openUnlockMenu(Player player) {
        open(player, ElementMenuHolder.Mode.UNLOCK, "Débloque un nouvel élément");
    }

    private void open(Player player, ElementMenuHolder.Mode mode, String title) {
        ElementMenuHolder holder = new ElementMenuHolder(player, mode);
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text(title, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        holder.setInventory(inventory);

        ItemStack filler = filler();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        List<Element> owned = elementManager.getElements(player.getUniqueId());
        for (var entry : ElementMenuHolder.SLOT_ELEMENTS.entrySet()) {
            inventory.setItem(entry.getKey(), elementIcon(entry.getValue(), owned.contains(entry.getValue()), mode));
        }

        player.openInventory(inventory);
    }

    private ItemStack elementIcon(Element element, boolean owned, ElementMenuHolder.Mode mode) {
        ItemStack stack = new ItemStack(element.getIcon());
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(element.getLabel(), element.getColor(), TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        if (mode == ElementMenuHolder.Mode.UNLOCK && owned) {
            meta.lore(List.of(Component.text("Tu maîtrises déjà cet élément.", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
        } else if (mode == ElementMenuHolder.Mode.STARTER) {
            meta.lore(List.of(Component.text("Clic : choisis cet élément comme", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("classe de départ (définitif !).", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)));
        } else {
            meta.lore(List.of(Component.text("Clic : débloque cet élément.", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack filler() {
        ItemStack stack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(" "));
        stack.setItemMeta(meta);
        return stack;
    }
}
