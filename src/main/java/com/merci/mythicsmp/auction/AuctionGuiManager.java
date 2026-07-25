package com.merci.mythicsmp.auction;

import com.merci.mythicsmp.gui.AuctionHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Sépare "construire ce qu'il y a à l'écran" (cette classe) de "réagir à un
 * clic" (AuctionGuiListener) — plus facile à faire évoluer si un jour on
 * veut trier/filtrer les annonces différemment.
 */
public class AuctionGuiManager {

    private final Plugin plugin;
    private final AuctionManager auctionManager;

    public AuctionGuiManager(Plugin plugin, AuctionManager auctionManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
    }

    public void open(Player player, int page) {
        AuctionHolder holder = new AuctionHolder();
        Inventory inventory = plugin.getServer().createInventory(holder, 54,
                Component.text("Marché Mythique", NamedTextColor.DARK_GREEN));
        holder.setInventory(inventory);
        holder.setPage(page);
        render(holder);
        player.openInventory(inventory);
    }

    /** Redessine le contenu du menu déjà ouvert (utilisé après achat/annulation pour rester à jour). */
    public void render(AuctionHolder holder) {
        Inventory inventory = holder.getInventory();
        inventory.clear();
        holder.clearMapping();

        List<AuctionListing> all = auctionManager.getListings();
        int page = holder.getPage();
        int start = page * AuctionHolder.PAGE_SIZE;

        for (int i = 0; i < AuctionHolder.PAGE_SIZE; i++) {
            int index = start + i;
            if (index >= all.size()) break;

            AuctionListing listing = all.get(index);
            inventory.setItem(i, buildDisplayItem(listing));
            holder.mapSlot(i, listing.id());
        }

        boolean hasPrevious = page > 0;
        boolean hasNext = start + AuctionHolder.PAGE_SIZE < all.size();

        if (hasPrevious) {
            inventory.setItem(AuctionHolder.PREVIOUS_SLOT, namedItem(Material.ARROW, "§ePage précédente"));
        }
        if (hasNext) {
            inventory.setItem(AuctionHolder.NEXT_SLOT, namedItem(Material.ARROW, "§ePage suivante"));
        }
        inventory.setItem(49, namedItem(Material.PAPER, "§7Page " + (page + 1)));
    }

    private ItemStack buildDisplayItem(AuctionListing listing) {
        ItemStack display = listing.item().clone();
        ItemMeta meta = display.getItemMeta();

        List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        lore.add(Component.empty());
        lore.add(Component.text("Vendu par " + listing.sellerName(), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Prix : " + formatPrice(listing.price()), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Clic : acheter (ou récupérer si c'est le tien)", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack namedItem(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(name));
        stack.setItemMeta(meta);
        return stack;
    }

    public static String formatPrice(double price) {
        return String.format("%.2f pièces", price);
    }
}
