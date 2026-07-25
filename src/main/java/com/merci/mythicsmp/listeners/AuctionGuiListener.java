package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.auction.AuctionGuiManager;
import com.merci.mythicsmp.auction.AuctionListing;
import com.merci.mythicsmp.auction.AuctionManager;
import com.merci.mythicsmp.economy.EconomyManager;
import com.merci.mythicsmp.gui.AuctionHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AuctionGuiListener implements Listener {

    private final AuctionManager auctionManager;
    private final AuctionGuiManager guiManager;
    private final EconomyManager economyManager;

    public AuctionGuiListener(AuctionManager auctionManager, AuctionGuiManager guiManager, EconomyManager economyManager) {
        this.auctionManager = auctionManager;
        this.guiManager = guiManager;
        this.economyManager = economyManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AuctionHolder holder)) return;
        event.setCancelled(true); // le menu est en lecture seule, aucun objet ne doit pouvoir en sortir par drag/shift-click

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();

        if (slot == AuctionHolder.PREVIOUS_SLOT && holder.getPage() > 0) {
            holder.setPage(holder.getPage() - 1);
            guiManager.render(holder);
            return;
        }
        if (slot == AuctionHolder.NEXT_SLOT) {
            holder.setPage(holder.getPage() + 1);
            guiManager.render(holder);
            return;
        }

        UUID listingId = holder.getListingIdAt(slot);
        if (listingId == null) return;

        AuctionListing listing = auctionManager.get(listingId);
        if (listing == null) {
            player.sendMessage(Component.text("Cette annonce vient d'être vendue.", NamedTextColor.RED));
            guiManager.render(holder);
            return;
        }

        if (listing.sellerId().equals(player.getUniqueId())) {
            reclaim(player, holder, listing);
        } else {
            purchase(player, holder, listing);
        }
    }

    private void purchase(Player buyer, AuctionHolder holder, AuctionListing listing) {
        if (!economyManager.withdraw(buyer.getUniqueId(), listing.price())) {
            buyer.sendMessage(Component.text("Tu n'as pas assez de pièces (besoin de "
                    + AuctionGuiManager.formatPrice(listing.price()) + ").", NamedTextColor.RED));
            return;
        }

        // On retire l'annonce en premier pour éviter que deux joueurs l'achètent au même instant.
        AuctionListing removed = auctionManager.remove(listing.id());
        if (removed == null) {
            economyManager.deposit(buyer.getUniqueId(), listing.price()); // remboursement, annonce déjà partie
            buyer.sendMessage(Component.text("Trop tard, quelqu'un d'autre vient de l'acheter.", NamedTextColor.RED));
            guiManager.render(holder);
            return;
        }

        economyManager.deposit(removed.sellerId(), removed.price());
        giveItemOrDrop(buyer, removed.item());
        buyer.sendMessage(Component.text("Achat effectué pour " + AuctionGuiManager.formatPrice(removed.price()) + ".", NamedTextColor.GREEN));
        guiManager.render(holder);
    }

    private void reclaim(Player seller, AuctionHolder holder, AuctionListing listing) {
        AuctionListing removed = auctionManager.remove(listing.id());
        if (removed == null) return;
        giveItemOrDrop(seller, removed.item());
        seller.sendMessage(Component.text("Annonce retirée, objet récupéré.", NamedTextColor.YELLOW));
        guiManager.render(holder);
    }

    private void giveItemOrDrop(Player player, ItemStack item) {
        var leftover = player.getInventory().addItem(item);
        leftover.values().forEach(stack -> player.getWorld().dropItem(player.getLocation(), stack));
    }
}
