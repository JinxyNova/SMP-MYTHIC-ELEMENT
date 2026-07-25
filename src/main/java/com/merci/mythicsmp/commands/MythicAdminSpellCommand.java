package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.elements.Element;
import com.merci.mythicsmp.elements.ElementManager;
import com.merci.mythicsmp.spells.Spell;
import com.merci.mythicsmp.spells.SpellManager;
import com.merci.mythicsmp.spells.SpellProgress;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commande admin pour tout ce qui touche au déblocage/à la progression des
 * sorts et des éléments : débloquer un sort précis, forcer un niveau de
 * maîtrise (skip de niveaux) ou tout mettre au niveau maximum d'un coup.
 *
 * Réservée aux admins : le sender doit être OP ou avoir la permission
 * "mythicsmp.admin" (un plugin de permissions peut donner ce noeud à un
 * groupe modérateur sans avoir à donner le OP complet).
 */
public class MythicAdminSpellCommand implements CommandExecutor {

    private static final String PERMISSION = "mythicsmp.admin";

    private final SpellManager spellManager;
    private final ElementManager elementManager;

    public MythicAdminSpellCommand(SpellManager spellManager, ElementManager elementManager) {
        this.spellManager = spellManager;
        this.elementManager = elementManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("Tu n'as pas la permission d'utiliser cette commande.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "unlock" -> handleUnlock(sender, label, args);
            case "setlevel" -> handleSetLevel(sender, label, args);
            case "max" -> handleMax(sender, label, args);
            case "maxall" -> handleMaxAll(sender, label, args);
            case "list" -> handleList(sender, args);
            default -> sendHelp(sender, label);
        }
        return true;
    }

    // ---------------------------------------------------------------- unlock

    private void handleUnlock(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage : /" + label + " unlock <joueur> <sortId|all|élément>", NamedTextColor.RED));
            return;
        }
        Player target = resolvePlayer(sender, args[1]);
        if (target == null) return;

        String selector = args[2];
        if (selector.equalsIgnoreCase("all")) {
            int count = spellManager.unlockAll(target.getUniqueId(), null);
            sender.sendMessage(Component.text(count + " sort(s) débloqué(s) pour " + target.getName() + " (tous éléments).", NamedTextColor.GREEN));
            target.sendMessage(Component.text("Un admin a débloqué tous tes sorts !", NamedTextColor.LIGHT_PURPLE));
            return;
        }

        Element element = parseElement(selector);
        if (element != null) {
            int count = spellManager.unlockAll(target.getUniqueId(), element);
            sender.sendMessage(Component.text(count + " sort(s) " + element.getLabel() + " débloqué(s) pour " + target.getName() + ".", NamedTextColor.GREEN));
            target.sendMessage(Component.text("Un admin a débloqué tes sorts " + element.getLabel() + " !", NamedTextColor.LIGHT_PURPLE));
            return;
        }

        Spell spell = spellManager.getRegistry().get(selector);
        if (spell == null) {
            sender.sendMessage(Component.text("Sort, élément ou 'all' inconnu : " + selector
                    + " (utilise /" + label + " list pour voir les ids).", NamedTextColor.RED));
            return;
        }
        boolean unlocked = spellManager.unlock(target.getUniqueId(), spell.id());
        if (unlocked) {
            sender.sendMessage(Component.text("Sort '" + spell.name() + "' débloqué pour " + target.getName() + ".", NamedTextColor.GREEN));
            target.sendMessage(Component.text("Un admin t'a débloqué le sort : " + spell.name(), NamedTextColor.LIGHT_PURPLE));
        } else {
            sender.sendMessage(Component.text(target.getName() + " avait déjà ce sort débloqué.", NamedTextColor.YELLOW));
        }
    }

    // ------------------------------------------------------------- setlevel

    private void handleSetLevel(CommandSender sender, String label, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage : /" + label + " setlevel <joueur> <sortId|all|élément> <niveau 1-"
                    + SpellProgress.MAX_LEVEL + ">", NamedTextColor.RED));
            return;
        }
        Player target = resolvePlayer(sender, args[1]);
        if (target == null) return;

        int level;
        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Niveau invalide : " + args[3], NamedTextColor.RED));
            return;
        }

        String selector = args[2];
        if (selector.equalsIgnoreCase("all")) {
            for (Spell spell : spellManager.getRegistry().all()) {
                spellManager.setLevel(target.getUniqueId(), spell.id(), level);
            }
            sender.sendMessage(Component.text("Tous les sorts de " + target.getName() + " sont maintenant niveau " + level + ".", NamedTextColor.GREEN));
            return;
        }

        Element element = parseElement(selector);
        if (element != null) {
            for (Spell spell : spellManager.getRegistry().forElement(element)) {
                spellManager.setLevel(target.getUniqueId(), spell.id(), level);
            }
            sender.sendMessage(Component.text("Sorts " + element.getLabel() + " de " + target.getName() + " au niveau " + level + ".", NamedTextColor.GREEN));
            return;
        }

        boolean ok = spellManager.setLevel(target.getUniqueId(), selector, level);
        if (!ok) {
            sender.sendMessage(Component.text("Sort, élément ou 'all' inconnu : " + selector, NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Niveau de '" + selector + "' fixé à " + level + " pour " + target.getName() + ".", NamedTextColor.GREEN));
    }

    // ------------------------------------------------------------------ max

    private void handleMax(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage : /" + label + " max <joueur> <sortId|all|élément>", NamedTextColor.RED));
            return;
        }
        Player target = resolvePlayer(sender, args[1]);
        if (target == null) return;

        String selector = args[2];
        if (selector.equalsIgnoreCase("all")) {
            spellManager.maxAll(target.getUniqueId(), null);
            sender.sendMessage(Component.text("Tous les sorts de " + target.getName() + " sont maintenant au niveau max.", NamedTextColor.GREEN));
            return;
        }
        Element element = parseElement(selector);
        if (element != null) {
            spellManager.maxAll(target.getUniqueId(), element);
            sender.sendMessage(Component.text("Sorts " + element.getLabel() + " de " + target.getName() + " au niveau max.", NamedTextColor.GREEN));
            return;
        }
        boolean ok = spellManager.setLevel(target.getUniqueId(), selector, SpellProgress.MAX_LEVEL);
        if (!ok) {
            sender.sendMessage(Component.text("Sort, élément ou 'all' inconnu : " + selector, NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("'" + selector + "' au niveau max pour " + target.getName() + ".", NamedTextColor.GREEN));
    }

    // --------------------------------------------------------------- maxall

    private void handleMaxAll(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage : /" + label + " maxall <joueur>", NamedTextColor.RED));
            return;
        }
        Player target = resolvePlayer(sender, args[1]);
        if (target == null) return;

        elementManager.adminUnlockAllElements(target.getUniqueId());
        spellManager.maxAll(target.getUniqueId(), null);

        sender.sendMessage(Component.text(target.getName()
                + " est maintenant au niveau maximum : 4 éléments + les 64 sorts au niveau "
                + SpellProgress.MAX_LEVEL + ".", NamedTextColor.GOLD));
        target.sendMessage(Component.text("Un admin t'a mis au niveau maximum : tous les éléments et tous les sorts, niveau max !",
                NamedTextColor.GOLD));
    }

    // ----------------------------------------------------------------- list

    private void handleList(CommandSender sender, String[] args) {
        Element filterElement = args.length >= 2 ? parseElement(args[1]) : null;
        sender.sendMessage(Component.text("=== Sorts disponibles ===", NamedTextColor.GOLD));
        for (Element element : Element.values()) {
            if (filterElement != null && element != filterElement) continue;
            sender.sendMessage(Component.text("• " + element.getLabel(), element.getColor()));
            for (Spell spell : spellManager.getRegistry().forElement(element)) {
                sender.sendMessage(Component.text("   - " + spell.id() + "  (" + spell.tier().getLabel() + ") " + spell.name(),
                        NamedTextColor.GRAY));
            }
        }
    }

    // --------------------------------------------------------------- utils

    private Player resolvePlayer(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sender.sendMessage(Component.text("Joueur introuvable ou hors ligne : " + name, NamedTextColor.RED));
        }
        return target;
    }

    private Element parseElement(String text) {
        try {
            return Element.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("=== Commandes admin sorts (" + label + ") ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/" + label + " unlock <joueur> <sortId|all|élément>", NamedTextColor.YELLOW)
                .append(Component.text("  — débloque un/des sort(s)", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " setlevel <joueur> <sortId|all|élément> <niveau 1-"
                + SpellProgress.MAX_LEVEL + ">", NamedTextColor.YELLOW)
                .append(Component.text("  — force un niveau (skip)", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " max <joueur> <sortId|all|élément>", NamedTextColor.YELLOW)
                .append(Component.text("  — met au niveau maximum", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " maxall <joueur>", NamedTextColor.YELLOW)
                .append(Component.text("  — 4 éléments + tous les sorts au niveau max", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " list [élément]", NamedTextColor.YELLOW)
                .append(Component.text("  — liste les ids de sorts", NamedTextColor.GRAY)));
    }
}
