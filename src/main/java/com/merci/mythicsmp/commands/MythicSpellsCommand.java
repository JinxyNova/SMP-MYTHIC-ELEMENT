package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.elements.Element;
import com.merci.mythicsmp.elements.ElementManager;
import com.merci.mythicsmp.spells.SpellWheelManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class MythicSpellsCommand implements CommandExecutor {

    private final ElementManager elementManager;
    private final SpellWheelManager wheelManager;

    public MythicSpellsCommand(ElementManager elementManager, SpellWheelManager wheelManager) {
        this.elementManager = elementManager;
        this.wheelManager = wheelManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
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
}
