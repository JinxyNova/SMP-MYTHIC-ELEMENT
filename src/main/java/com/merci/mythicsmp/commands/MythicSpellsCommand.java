package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.elements.Element;
import com.merci.mythicsmp.elements.ElementManager;
import com.merci.mythicsmp.spells.Spell;
import com.merci.mythicsmp.spells.SpellManager;
import com.merci.mythicsmp.spells.SpellProgress;
import com.merci.mythicsmp.spells.SpellRegistry;
import com.merci.mythicsmp.spells.SpellWheelManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /mythicspells                          -> ouvre la roue des sorts (comme avant)
 * /mythicspells liste [élément]          -> le grimoire complet (64 sorts, ou filtré par élément) :
 *                                           tier, puissance (x1.0 à x4.0), recharge de base, description,
 *                                           rappel de la Rune nécessaire, et ton statut si tu es joueur.
 * /mythicspells admin unlock|max <joueur> [sort|all]
 *                                        -> réservé aux admins (mythicsmp.spells.admin) : débloque ou
 *                                           maximise la maîtrise d'un sort précis (ou de tous les sorts,
 *                                           ou de tous les sorts d'un élément) pour n'importe quel joueur en ligne.
 */
public class MythicSpellsCommand implements CommandExecutor {

    private final ElementManager elementManager;
    private final SpellWheelManager wheelManager;
    private final SpellManager spellManager;

    public MythicSpellsCommand(ElementManager elementManager, SpellWheelManager wheelManager, SpellManager spellManager) {
        this.elementManager = elementManager;
        this.wheelManager = wheelManager;
        this.spellManager = spellManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            handleAdmin(sender, args);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("liste")) {
            showGrimoire(sender, args.length >= 2 ? args[1] : null);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs (utilise /mythicspells liste depuis la console).");
            return true;
        }

        List<Element> owned = elementManager.getElements(player.getUniqueId());
        if (owned.isEmpty()) {
            player.sendMessage(Component.text(
                    "Choisis d'abord ta classe élémentaire (/mythicelement choose).", NamedTextColor.YELLOW));
            return true;
        }
        wheelManager.openView(player, owned.get(0));
        return true;
    }

    // ------------------------------------------------------------- Grimoire

    private void showGrimoire(CommandSender sender, String elementArg) {
        Element filter = null;
        if (elementArg != null) {
            try {
                filter = Element.valueOf(elementArg.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                sender.sendMessage(Component.text(
                        "Élément inconnu : " + elementArg + " (feu, eau, terre, vent)", NamedTextColor.RED));
                return;
            }
        }

        SpellRegistry registry = spellManager.getRegistry();
        List<Spell> spells = filter != null ? registry.forElement(filter) : registry.all();
        Player player = sender instanceof Player p ? p : null;

        sender.sendMessage(Component.text(
                "=== Grimoire des Sorts" + (filter != null ? " — " + filter.getLabel() : " (64 sorts)") + " ===",
                NamedTextColor.GOLD));

        for (Spell spell : spells) {
            NamedTextColor elementColor = spell.element().getColor();
            String power = String.format(Locale.ROOT, "x%.1f", spell.tier().getPowerScale());

            Component line = Component.text("• " + spell.name() + " ", elementColor)
                    .append(Component.text("[" + spell.element().getLabel() + " / " + spell.tier().getLabel() + "]", NamedTextColor.GRAY))
                    .append(Component.text("  Puissance " + power, NamedTextColor.YELLOW))
                    .append(Component.text("  Recharge " + spell.tier().getBaseCooldownSeconds() + "s", NamedTextColor.AQUA));
            sender.sendMessage(line);
            sender.sendMessage(Component.text("   " + spell.description(), NamedTextColor.DARK_GRAY));
            sender.sendMessage(Component.text(
                    "   Débloqué avec : Rune " + spell.element().getLabel() + " (" + spell.tier().getLabel() + ")",
                    NamedTextColor.DARK_AQUA));

            if (player != null) {
                if (spellManager.isUnlocked(player.getUniqueId(), spell.id())) {
                    SpellProgress progress = spellManager.getProgress(player.getUniqueId(), spell.id());
                    sender.sendMessage(Component.text(
                            "   Statut : Débloqué — maîtrise niveau " + progress.getLevel() + "/" + SpellProgress.MAX_LEVEL,
                            NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("   Statut : Verrouillé", NamedTextColor.RED));
                }
            }
        }
    }

    // ------------------------------------------------------------- Admin

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mythicsmp.spells.admin")) {
            sender.sendMessage(Component.text("Tu n'as pas la permission d'utiliser cette commande.", NamedTextColor.RED));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text(
                    "Usage : /mythicspells admin unlock|max <joueur> [sort|all]", NamedTextColor.RED));
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        if (!action.equals("unlock") && !action.equals("max")) {
            sender.sendMessage(Component.text(
                    "Usage : /mythicspells admin unlock|max <joueur> [sort|all]", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(Component.text("Joueur introuvable ou hors ligne : " + args[2], NamedTextColor.RED));
            return;
        }

        String spellArg = args.length >= 4 ? args[3] : "all";
        boolean isMax = action.equals("max");

        if (spellArg.equalsIgnoreCase("all")) {
            int count = isMax ? spellManager.adminMaxAll(target.getUniqueId()) : spellManager.adminUnlockAll(target.getUniqueId());
            sender.sendMessage(Component.text(
                    (isMax ? "Maîtrise maximale appliquée sur " : count + " sort(s) débloqué(s) pour ")
                            + (isMax ? "les 64 sorts de " : "") + target.getName() + ".", NamedTextColor.GREEN));
            target.sendMessage(Component.text(
                    isMax ? "Un admin a maximisé tous tes sorts !" : "Un admin a débloqué tous les sorts pour toi !",
                    NamedTextColor.LIGHT_PURPLE));
            return;
        }

        // Un élément entier plutôt qu'un sort précis (pratique pour "tout le feu", etc.)
        Element asElement = null;
        try {
            asElement = Element.valueOf(spellArg.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            // Ce n'est pas un nom d'élément, on tente alors un id de sort précis ci-dessous.
        }

        if (asElement != null) {
            int count = isMax ? spellManager.adminMaxElement(target.getUniqueId(), asElement)
                    : spellManager.adminUnlockElement(target.getUniqueId(), asElement);
            sender.sendMessage(Component.text(
                    (isMax ? "Maîtrise maximale appliquée sur " : count + " sort(s) débloqué(s) sur ")
                            + "l'élément " + asElement.getLabel() + " pour " + target.getName() + ".", NamedTextColor.GREEN));
            target.sendMessage(Component.text(
                    "Un admin a " + (isMax ? "maximisé" : "débloqué") + " tes sorts " + asElement.getLabel() + " !",
                    NamedTextColor.LIGHT_PURPLE));
            return;
        }

        Spell spell = spellManager.getRegistry().get(spellArg);
        if (spell == null) {
            sender.sendMessage(Component.text(
                    "Sort inconnu : " + spellArg + " (voir /mythicspells liste)", NamedTextColor.RED));
            return;
        }

        boolean ok = isMax ? spellManager.adminMax(target.getUniqueId(), spell.id())
                : spellManager.unlock(target.getUniqueId(), spell.id());
        if (ok) {
            sender.sendMessage(Component.text(
                    (isMax ? "Sort maximisé" : "Sort débloqué") + " : " + spell.name() + " pour " + target.getName() + ".",
                    NamedTextColor.GREEN));
            target.sendMessage(Component.text(
                    "Un admin t'a " + (isMax ? "maximisé" : "débloqué") + " le sort " + spell.name() + " !",
                    NamedTextColor.LIGHT_PURPLE));
        } else {
            sender.sendMessage(Component.text(
                    target.getName() + " maîtrise déjà pleinement " + spell.name() + ".", NamedTextColor.YELLOW));
        }
    }
}
