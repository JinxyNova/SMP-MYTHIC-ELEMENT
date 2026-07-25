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

/**
 * Commande publique pour choisir/consulter ses éléments. Ouvre le bon menu
 * selon la situation du joueur (choix de départ, ou déblocage d'un élément
 * supplémentaire s'il en a déjà mais pas les 4).
 *
 * Sous-commande "admin" réservée aux admins pour forcer le déblocage
 * d'éléments (utile pour mettre un joueur au niveau max, voir aussi
 * /mythicadminspell maxall qui fait élément + sorts en une fois).
 */
public class MythicElementCommand implements CommandExecutor {

    private static final String PERMISSION = "mythicsmp.admin";

    private final ElementManager elementManager;
    private final ElementMenuManager elementMenuManager;

    public MythicElementCommand(ElementManager elementManager, ElementMenuManager elementMenuManager) {
        this.elementManager = elementManager;
        this.elementMenuManager = elementMenuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("admin")) {
            handleAdmin(sender, label, args);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        if (args.length >= 1 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("liste"))) {
            sendInfo(player);
            return true;
        }

        if (!elementManager.hasChosenStarter(player.getUniqueId())) {
            elementMenuManager.openStarterMenu(player);
        } else if (!elementManager.isMaxed(player.getUniqueId())) {
            elementMenuManager.openUnlockMenu(player);
        } else {
            sendInfo(player);
        }
        return true;
    }

    private void sendInfo(Player player) {
        List<Element> owned = elementManager.getElements(player.getUniqueId());
        String rank = elementManager.getRankLabel(player.getUniqueId());
        player.sendMessage(Component.text("=== Ton profil élémentaire ===", NamedTextColor.GOLD));
        if (owned.isEmpty()) {
            player.sendMessage(Component.text("Aucun élément choisi pour l'instant. Utilise /mythicelement pour commencer.", NamedTextColor.GRAY));
            return;
        }
        for (Element element : owned) {
            player.sendMessage(Component.text("• " + element.getLabel(), element.getColor()));
        }
        player.sendMessage(Component.text("Grade : " + rank + " (" + owned.size() + "/" + ElementManager.MAX_ELEMENTS + ")", NamedTextColor.LIGHT_PURPLE));
        if (owned.size() >= ElementManager.MAX_ELEMENTS) {
            player.sendMessage(Component.text("Tu maîtrises tous les éléments : va voir /mythicultimate !", NamedTextColor.AQUA));
        }
    }

    // ----------------------------------------------------------------- admin

    private void handleAdmin(CommandSender sender, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("Tu n'as pas la permission d'utiliser cette commande.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage : /" + label + " admin unlock <joueur> <élément|all>", NamedTextColor.RED));
            sender.sendMessage(Component.text("        /" + label + " admin max <joueur>", NamedTextColor.RED));
            return;
        }

        String sub = args[1].toLowerCase();
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(Component.text("Joueur introuvable ou hors ligne : " + args[2], NamedTextColor.RED));
            return;
        }

        switch (sub) {
            case "unlock" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Usage : /" + label + " admin unlock <joueur> <élément|all>", NamedTextColor.RED));
                    return;
                }
                if (args[3].equalsIgnoreCase("all")) {
                    elementManager.adminUnlockAllElements(target.getUniqueId());
                    sender.sendMessage(Component.text("Les 4 éléments ont été débloqués pour " + target.getName() + ".", NamedTextColor.GREEN));
                    target.sendMessage(Component.text("Un admin t'a débloqué tous les éléments !", NamedTextColor.LIGHT_PURPLE));
                    return;
                }
                Element element = parseElement(args[3]);
                if (element == null) {
                    sender.sendMessage(Component.text("Élément inconnu : " + args[3] + " (feu, eau, terre, vent).", NamedTextColor.RED));
                    return;
                }
                if (!elementManager.hasChosenStarter(target.getUniqueId())) {
                    elementManager.chooseStarter(target.getUniqueId(), element);
                } else {
                    elementManager.unlockElement(target.getUniqueId(), element);
                }
                sender.sendMessage(Component.text("Élément " + element.getLabel() + " débloqué pour " + target.getName() + ".", NamedTextColor.GREEN));
            }
            case "max" -> {
                elementManager.adminUnlockAllElements(target.getUniqueId());
                sender.sendMessage(Component.text(target.getName() + " maîtrise maintenant les 4 éléments.", NamedTextColor.GOLD));
                target.sendMessage(Component.text("Un admin t'a mis au niveau maximum d'éléments !", NamedTextColor.GOLD));
            }
            default -> sender.sendMessage(Component.text("Sous-commande inconnue : " + sub + " (unlock, max).", NamedTextColor.RED));
        }
    }

    private Element parseElement(String text) {
        try {
            return Element.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
