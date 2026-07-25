package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.elements.Element;
import com.merci.mythicsmp.elements.ElementManager;
import com.merci.mythicsmp.elements.ElementMenuManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * /mythicelement            -> profil élémentaire du joueur (alias de "info")
 * /mythicelement info       -> pareil, explicite
 * /mythicelement choose     -> ouvre le menu de choix de la classe de départ
 * /mythicelement admin unlock|max <joueur> [élément|all]
 *      -> réservé aux admins (mythicsmp.elements.admin), permet de débloquer
 *         un élément précis (ou les 4 d'un coup avec "all"/sans argument)
 *         pour n'importe quel joueur en ligne, sans passer par la Gemme au
 *         Pouvoir Infini. "max" a le même effet que "unlock" côté éléments
 *         (il n'y a pas de palier par élément, juste un nombre d'éléments
 *         possédés qui détermine le grade), le mot-clé existe surtout pour
 *         rester cohérent avec /mythicspells admin unlock|max.
 */
public class MythicElementCommand implements CommandExecutor {

    private final ElementManager elementManager;
    private final ElementMenuManager menuManager;

    public MythicElementCommand(ElementManager elementManager, ElementMenuManager menuManager) {
        this.elementManager = elementManager;
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            handleAdmin(sender, args);
            return true;
        }

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

        // "info" est un alias explicite de la commande sans argument.
        if (args.length > 0 && !args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(Component.text(
                    "Usage : /mythicelement [info|choose]", NamedTextColor.RED));
            return true;
        }

        showProfile(player);
        return true;
    }

    private void showProfile(Player player) {
        List<Element> owned = elementManager.getElements(player.getUniqueId());
        if (owned.isEmpty()) {
            player.sendMessage(Component.text(
                    "Tu n'as pas encore choisi de classe élémentaire. /mythicelement choose",
                    NamedTextColor.YELLOW));
            return;
        }

        String elementsList = owned.stream().map(Element::getLabel).collect(Collectors.joining(", "));
        String rank = elementManager.getRankLabel(player.getUniqueId());
        player.sendMessage(Component.text(
                "Éléments (" + owned.size() + "/" + ElementManager.MAX_ELEMENTS + ") : " + elementsList,
                NamedTextColor.AQUA));
        player.sendMessage(Component.text("Grade : " + rank, NamedTextColor.LIGHT_PURPLE));
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mythicsmp.elements.admin")) {
            sender.sendMessage(Component.text("Tu n'as pas la permission d'utiliser cette commande.", NamedTextColor.RED));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text(
                    "Usage : /mythicelement admin unlock|max <joueur> [élément|all]", NamedTextColor.RED));
            return;
        }

        String action = args[1].toLowerCase();
        if (!action.equals("unlock") && !action.equals("max")) {
            sender.sendMessage(Component.text(
                    "Usage : /mythicelement admin unlock|max <joueur> [élément|all]", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(Component.text("Joueur introuvable ou hors ligne : " + args[2], NamedTextColor.RED));
            return;
        }

        String elementArg = args.length >= 4 ? args[3] : "all";

        if (elementArg.equalsIgnoreCase("all")) {
            int added = elementManager.adminUnlockAll(target.getUniqueId());
            String rank = elementManager.getRankLabel(target.getUniqueId());
            sender.sendMessage(Component.text(
                    (added > 0 ? added + " élément(s) débloqué(s)" : "Déjà tous débloqués")
                            + " pour " + target.getName() + " (grade : " + rank + ").", NamedTextColor.GREEN));
            target.sendMessage(Component.text(
                    "Un admin a débloqué tous tes éléments ! Grade : " + rank, NamedTextColor.LIGHT_PURPLE));
            return;
        }

        Element element;
        try {
            element = Element.valueOf(elementArg.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text(
                    "Élément inconnu : " + elementArg + " (feu, eau, terre, vent, ou all)", NamedTextColor.RED));
            return;
        }

        boolean added = elementManager.adminUnlock(target.getUniqueId(), element);
        String rank = elementManager.getRankLabel(target.getUniqueId());
        if (added) {
            sender.sendMessage(Component.text(
                    "Élément " + element.getLabel() + " débloqué pour " + target.getName()
                            + " (grade : " + rank + ").", NamedTextColor.GREEN));
            target.sendMessage(Component.text(
                    "Un admin t'a débloqué l'élément " + element.getLabel() + " !", NamedTextColor.LIGHT_PURPLE));
        } else {
            sender.sendMessage(Component.text(
                    target.getName() + " possède déjà l'élément " + element.getLabel() + ".", NamedTextColor.YELLOW));
        }
    }
}
