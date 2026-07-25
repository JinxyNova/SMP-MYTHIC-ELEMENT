package com.merci.mythicsmp.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Sert uniquement à reconnaître "cet inventaire ouvert est bien notre menu
 * de forge" dans les listeners, sans dépendre du titre affiché (qui peut
 * changer de langue) ni d'une liste externe d'inventaires ouverts.
 */
public class ForgeHolder implements InventoryHolder {

    public static final int INPUT_SLOT = 11;
    public static final int MATERIAL_SLOT_1 = 13;
    public static final int MATERIAL_SLOT_2 = 15;
    public static final int CONFIRM_SLOT = 22;

    private final Player player;
    private Inventory inventory;

    public ForgeHolder(Player player) {
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
}
