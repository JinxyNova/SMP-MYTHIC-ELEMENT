package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.economy.EconomyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MythicBalTopCommand implements CommandExecutor {

    private final EconomyManager economyManager;

    public MythicBalTopCommand(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<Map.Entry<UUID, Double>> top = economyManager.topBalances(10);
        if (top.isEmpty()) {
            sender.sendMessage(Component.text("Aucun solde enregistré pour l'instant.", NamedTextColor.RED));
            return true;
        }
        sender.sendMessage(Component.text("=== Classement des plus riches ===", NamedTextColor.GOLD));
        int rank = 1;
        for (Map.Entry<UUID, Double> entry : top) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = "???";
            sender.sendMessage(Component.text(
                    "#" + rank + " " + name + " - " + String.format("%.0f", entry.getValue()) + " pièces",
                    NamedTextColor.YELLOW));
            rank++;
        }
        return true;
    }
}
