package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.auction.AuctionGuiManager;
import com.merci.mythicsmp.economy.EconomyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand implements CommandExecutor {

    private final EconomyManager economyManager;

    public BalanceCommand(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande doit être utilisée en jeu.");
            return true;
        }
        double balance = economyManager.getBalance(player.getUniqueId());
        player.sendMessage(Component.text("Ton solde : " + AuctionGuiManager.formatPrice(balance), NamedTextColor.GOLD));
        return true;
    }
}
