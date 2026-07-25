package com.merci.mythicsmp.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class AuctionHolder implements InventoryHolder {

    public static final int PAGE_SIZE = 45; // 5 rangées de contenu, la 6e est réservée à la navigation
    public static final int PREVIOUS_SLOT = 45;
    public static final int NEXT_SLOT = 53;

    private Inventory inventory;
    private int page;

    /** Associe chaque slot affiché à l'id de l'annonce qu'il représente, pour cette page précise. */
    private final java.util.Map<Integer, UUID> slotToListingId = new java.util.HashMap<>();

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void mapSlot(int slot, UUID listingId) {
        slotToListingId.put(slot, listingId);
    }

    public UUID getListingIdAt(int slot) {
        return slotToListingId.get(slot);
    }

    public void clearMapping() {
        slotToListingId.clear();
    }
}
