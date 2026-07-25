package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.auction.AuctionGuiManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AuctionOpenCommand implements CommandExecutor {

    private final AuctionGuiManager guiManager;

    public AuctionOpenCommand(AuctionGuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande doit être utilisée en jeu.");
            return true;
        }
        guiManager.open(player, 0);
        return true;
    }
}
