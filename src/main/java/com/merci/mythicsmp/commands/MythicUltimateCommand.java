package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.ultimate.UltimateMageManager;
import com.merci.mythicsmp.ultimate.UltimateSpellMenuManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MythicUltimateCommand implements CommandExecutor {

    private final UltimateMageManager ultimateMageManager;
    private final UltimateSpellMenuManager menuManager;

    public MythicUltimateCommand(UltimateMageManager ultimateMageManager, UltimateSpellMenuManager menuManager) {
        this.ultimateMageManager = ultimateMageManager;
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        if (!ultimateMageManager.hasClass(player.getUniqueId())) {
            player.sendMessage(Component.text(
                    "Tu n'es pas encore Mage Ultime. Maîtrise les 4 éléments puis utilise", NamedTextColor.YELLOW));
            player.sendMessage(Component.text(
                    "une Gemme du Mage Ultime (obtenue en tuant le boss).", NamedTextColor.YELLOW));
            return true;
        }

        menuManager.open(player);
        return true;
    }
}
