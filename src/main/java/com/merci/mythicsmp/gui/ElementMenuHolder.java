package com.merci.mythicsmp.gui;

import com.merci.mythicsmp.elements.Element;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;

/**
 * Sert à reconnaître "cet inventaire ouvert est bien notre menu de choix
 * d'élément" dans les listeners, et à savoir dans quel mode il a été ouvert
 * (choix de la classe de départ, ou déblocage d'un élément supplémentaire
 * via la Gemme au Pouvoir Infini).
 */
public class ElementMenuHolder implements InventoryHolder {

    public enum Mode { STARTER, UNLOCK }

    /** Emplacement de chaque élément dans le menu (inventaire de taille 27). */
    public static final Map<Integer, Element> SLOT_ELEMENTS = Map.of(
            11, Element.FEU,
            13, Element.EAU,
            15, Element.TERRE,
            17, Element.VENT
    );

    private final Player player;
    private final Mode mode;
    private Inventory inventory;

    public ElementMenuHolder(Player player, Mode mode) {
        this.player = player;
        this.mode = mode;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public Mode getMode() {
        return mode;
    }
}
