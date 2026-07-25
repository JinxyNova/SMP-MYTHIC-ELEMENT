package com.merci.mythicsmp.gui;

import com.merci.mythicsmp.elements.Element;
import com.merci.mythicsmp.spells.Spell;
import com.merci.mythicsmp.spells.SpellTier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Reconnaît "cet inventaire est notre roue des sorts" dans les listeners.
 * Mode VIEW : on parcourt les sorts d'un élément (onglets) et on peut en
 * lancer un s'il est débloqué. Mode UNLOCK : ouvert depuis une Rune, ne
 * montre que les 4 sorts d'un élément+palier donné à débloquer.
 */
public class SpellWheelHolder implements InventoryHolder {

    public enum Mode { VIEW, UNLOCK }

    private final Player player;
    private final Mode mode;
    private final Element unlockElement;
    private final SpellTier unlockTier;

    private Element displayedElement;
    private Inventory inventory;
    private final Map<Integer, Spell> slotSpells = new HashMap<>();
    private final Map<Integer, Element> slotElementTabs = new HashMap<>();

    public static SpellWheelHolder view(Player player, Element displayedElement) {
        return new SpellWheelHolder(player, Mode.VIEW, displayedElement, null, null);
    }

    public static SpellWheelHolder unlock(Player player, Element element, SpellTier tier) {
        return new SpellWheelHolder(player, Mode.UNLOCK, element, element, tier);
    }

    private SpellWheelHolder(Player player, Mode mode, Element displayedElement, Element unlockElement, SpellTier unlockTier) {
        this.player = player;
        this.mode = mode;
        this.displayedElement = displayedElement;
        this.unlockElement = unlockElement;
        this.unlockTier = unlockTier;
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

    public Element getDisplayedElement() {
        return displayedElement;
    }

    public Element getUnlockElement() {
        return unlockElement;
    }

    public SpellTier getUnlockTier() {
        return unlockTier;
    }

    public Map<Integer, Spell> getSlotSpells() {
        return slotSpells;
    }

    public Map<Integer, Element> getSlotElementTabs() {
        return slotElementTabs;
    }
}
