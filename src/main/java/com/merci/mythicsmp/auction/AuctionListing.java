package com.merci.mythicsmp.auction;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public record AuctionListing(UUID id, UUID sellerId, String sellerName, ItemStack item, double price, long createdAt) {
}
