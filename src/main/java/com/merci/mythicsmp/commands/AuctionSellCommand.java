package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.auction.AuctionGuiManager;
import com.merci.mythicsmp.auction.AuctionManager;
import com.merci.mythicsmp.quests.QuestManager;
import com.merci.mythicsmp.quests.QuestType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AuctionSellCommand implements CommandExecutor {

    private final AuctionManager auctionManager;
    private final QuestManager questManager;

    public AuctionSellCommand(AuctionManager auctionManager, QuestManager questManager) {
        this.auctionManager = auctionManager;
        this.questManager = questManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande doit être utilisée en jeu.");
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage : /mythicsell <prix>", NamedTextColor.RED));
            return true;
        }

        double price;
        try {
            price = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Prix invalide.", NamedTextColor.RED));
            return true;
        }
        if (price <= 0) {
            player.sendMessage(Component.text("Le prix doit être supérieur à 0.", NamedTextColor.RED));
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            player.sendMessage(Component.text("Tiens l'objet à vendre en main avant de faire /mythicsell.", NamedTextColor.RED));
            return true;
        }

        ItemStack toSell = hand.clone();
        player.getInventory().setItemInMainHand(null);

        auctionManager.createListing(player.getUniqueId(), player.getName(), toSell, price);
        questManager.addProgress(player, QuestType.VENDRE_AU_MARCHE, 1, null);
        player.sendMessage(Component.text("Objet mis en vente pour " + AuctionGuiManager.formatPrice(price) + ".", NamedTextColor.GREEN));
        return true;
    }
}
