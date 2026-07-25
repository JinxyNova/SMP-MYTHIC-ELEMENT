package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.elements.Element;
import com.merci.mythicsmp.elements.ElementManager;
import com.merci.mythicsmp.spells.Spell;
import com.merci.mythicsmp.spells.SpellManager;
import com.merci.mythicsmp.spells.SpellProgress;
import com.merci.mythicsmp.spells.SpellTier;
import com.merci.mythicsmp.spells.SpellWheelManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Commande publique (ouverte à tous les joueurs) qui sert de "wiki" pour
 * les sorts : liste des 64 sorts par élément/palier avec leur puissance et
 * leur recharge de base, et rappel de comment les débloquer (runes).
 *
 * Sans argument, ouvre la roue des sorts du joueur (comportement normal).
 * Avec "liste"/"list" [élément], affiche le wiki en texte : utile pour
 * tout consulter d'un coup d'œil sans ouvrir de menu, et pour connaître
 * l'id exact d'un sort (utile aux admins pour /mythicadminspell).
 */
public class MythicSpellsCommand implements CommandExecutor {

    private final ElementManager elementManager;
    private final SpellManager spellManager;
    private final SpellWheelManager spellWheelManager;

    public MythicSpellsCommand(ElementManager elementManager, SpellManager spellManager, SpellWheelManager spellWheelManager) {
        this.elementManager = elementManager;
        this.spellManager = spellManager;
        this.spellWheelManager = spellWheelManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Commande réservée aux joueurs (utilise /" + label + " liste depuis la console).");
                return true;
            }
            openWheelForPlayer(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("liste") || args[0].equalsIgnoreCase("list")) {
            Element filter = args.length >= 2 ? parseElement(args[1]) : null;
            sendWiki(sender, filter);
            return true;
        }

        sender.sendMessage(Component.text("Usage : /" + label + " (ouvre la roue) | /" + label + " liste [élément]", NamedTextColor.RED));
        return true;
    }

    private void openWheelForPlayer(Player player) {
        if (!elementManager.hasChosenStarter(player.getUniqueId())) {
            player.sendMessage(Component.text("Choisis d'abord ton élément de départ avec /mythicelement.", NamedTextColor.YELLOW));
            return;
        }
        Element displayed = elementManager.getElements(player.getUniqueId()).get(0);
        spellWheelManager.openView(player, displayed);
    }

    private void sendWiki(CommandSender sender, Element filter) {
        sender.sendMessage(Component.text("=== Grimoire des sorts ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Débloque un sort avec une Rune de l'élément et du palier correspondant", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("(clic droit avec la rune en main), une fois l'élément maîtrisé.", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Chaque sort utilisé gagne de la maîtrise (niveau 1 à " + SpellProgress.MAX_LEVEL
                + ") : +10% de puissance et -8% de recharge par niveau au-delà du niveau 1.", NamedTextColor.GRAY));

        boolean isPlayer = sender instanceof Player;
        UUID uuid = isPlayer ? ((Player) sender).getUniqueId() : null;

        for (Element element : Element.values()) {
            if (filter != null && element != filter) continue;
            sender.sendMessage(Component.text(" "));
            sender.sendMessage(Component.text("── " + element.getLabel() + " ──", element.getColor()));
            for (SpellTier tier : SpellTier.values()) {
                sender.sendMessage(Component.text(" " + tier.getLabel() + " (puissance x" + tier.getPowerScale()
                        + ", recharge base " + tier.getBaseCooldownSeconds() + "s) :", NamedTextColor.WHITE));
                for (Spell spell : spellManager.getRegistry().forElementAndTier(element, tier)) {
                    Component line = Component.text("   • " + spell.name() + " (id: " + spell.id() + ") — " + spell.description(),
                            NamedTextColor.GRAY);
                    if (uuid != null) {
                        if (spellManager.isUnlocked(uuid, spell.id())) {
                            SpellProgress progress = spellManager.getProgress(uuid, spell.id());
                            line = line.append(Component.text(" [débloqué, maîtrise " + progress.getLevel() + "/"
                                    + SpellProgress.MAX_LEVEL + "]", NamedTextColor.GREEN));
                        } else {
                            line = line.append(Component.text(" [verrouillé]", NamedTextColor.DARK_GRAY));
                        }
                    }
                    sender.sendMessage(line);
                }
            }
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
