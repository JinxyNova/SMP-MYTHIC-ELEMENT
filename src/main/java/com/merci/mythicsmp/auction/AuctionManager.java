package com.merci.mythicsmp.auction;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Les annonces vivent dans un LinkedHashMap (ordre d'ajout stable pour un
 * affichage cohérent en pages) et sont réécrites en entier sur disque après
 * chaque changement — même logique de sécurité que EconomyManager : on
 * préfère un peu plus d'IO à la possibilité de perdre l'objet d'un joueur.
 *
 * ItemStack implémente ConfigurationSerializable, donc YamlConfiguration
 * sait le sauvegarder/recharger tout seul (pas besoin de sérialiser à la main).
 */
public class AuctionManager {

    private final Plugin plugin;
    private final File file;
    private final Map<UUID, AuctionListing> listings = new LinkedHashMap<>();

    public AuctionManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "auctions.yml");
        load();
    }

    public List<AuctionListing> getListings() {
        return new ArrayList<>(listings.values());
    }

    public AuctionListing get(UUID id) {
        return listings.get(id);
    }

    public AuctionListing createListing(UUID sellerId, String sellerName, ItemStack item, double price) {
        UUID id = UUID.randomUUID();
        AuctionListing listing = new AuctionListing(id, sellerId, sellerName, item, price, System.currentTimeMillis());
        listings.put(id, listing);
        save();
        return listing;
    }

    /** Retire une annonce (achat conclu ou annulation) et la renvoie pour que l'appelant gère l'objet/l'argent. */
    public AuctionListing remove(UUID id) {
        AuctionListing removed = listings.remove(id);
        if (removed != null) save();
        return removed;
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String path = key + ".";
                UUID sellerId = UUID.fromString(config.getString(path + "sellerId"));
                String sellerName = config.getString(path + "sellerName");
                ItemStack item = config.getItemStack(path + "item");
                double price = config.getDouble(path + "price");
                long createdAt = config.getLong(path + "createdAt");
                if (item != null && sellerName != null) {
                    listings.put(id, new AuctionListing(id, sellerId, sellerName, item, price, createdAt));
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Annonce ignorée (donnée invalide) dans auctions.yml : " + key);
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (AuctionListing listing : listings.values()) {
            String path = listing.id() + ".";
            config.set(path + "sellerId", listing.sellerId().toString());
            config.set(path + "sellerName", listing.sellerName());
            config.set(path + "item", listing.item());
            config.set(path + "price", listing.price());
            config.set(path + "createdAt", listing.createdAt());
        }
        try {
            plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Impossible de sauvegarder auctions.yml", e);
        }
    }
}
