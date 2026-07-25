package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.elements.Element;
import com.merci.mythicsmp.elements.ElementManager;
import com.merci.mythicsmp.elements.ElementMenuManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class MythicElementCommand implements CommandExecutor {

    private final ElementManager elementManager;
    private final ElementMenuManager menuManager;

    public MythicElementCommand(ElementManager elementManager, ElementMenuManager menuManager) {
        this.elementManager = elementManager;
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("choose")) {
            if (elementManager.hasChosenStarter(player.getUniqueId())) {
                player.sendMessage(Component.text(
                        "Tu as déjà choisi ta classe de départ, impossible d'en changer.", NamedTextColor.RED));
                return true;
            }
            menuManager.openStarterMenu(player);
            return true;
        }

        List<Element> owned = elementManager.getElements(player.getUniqueId());
        if (owned.isEmpty()) {
            player.sendMessage(Component.text(
                    "Tu n'as pas encore choisi de classe élémentaire. /mythicelement choose",
                    NamedTextColor.YELLOW));
            return true;
        }

        String elementsList = owned.stream().map(Element::getLabel).collect(Collectors.joining(", "));
        String rank = elementManager.getRankLabel(player.getUniqueId());
        player.sendMessage(Component.text(
                "Éléments (" + owned.size() + "/" + ElementManager.MAX_ELEMENTS + ") : " + elementsList,
                NamedTextColor.AQUA));
        player.sendMessage(Component.text("Grade : " + rank, NamedTextColor.LIGHT_PURPLE));
        return true;
    }
}
