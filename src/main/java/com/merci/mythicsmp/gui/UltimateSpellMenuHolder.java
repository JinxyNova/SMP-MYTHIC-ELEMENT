package com.merci.mythicsmp.gui;

import com.merci.mythicsmp.ultimate.UltimateSpell;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Sert à reconnaître "cet inventaire ouvert est bien le menu des sorts de
 * la classe Ultime Mage" dans UltimateSpellMenuListener.
 */
public class UltimateSpellMenuHolder implements InventoryHolder {

    private final Player player;
    private Inventory inventory;
    private final Map<Integer, UltimateSpell> slotSpells = new HashMap<>();

    public UltimateSpellMenuHolder(Player player) {
        this.player = player;
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

    public Map<Integer, UltimateSpell> getSlotSpells() {
        return slotSpells;
    }
}
