package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.items.ItemRegistry;
import com.merci.mythicsmp.items.MythicItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Commande admin pour donner un objet mythique (arme ou objet spécial) à
 * un joueur, par son id (voir /mythiclist pour la liste complète des ids).
 */
public class MythicGiveCommand implements CommandExecutor {

    private static final String PERMISSION = "mythicsmp.admin";

    private final ItemRegistry itemRegistry;

    public MythicGiveCommand(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("Tu n'as pas la permission d'utiliser cette commande.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage : /" + label + " <joueur> <itemId> [quantité]", NamedTextColor.RED));
            sender.sendMessage(Component.text("Voir /mythiclist pour la liste des ids disponibles.", NamedTextColor.GRAY));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Joueur introuvable ou hors ligne : " + args[0], NamedTextColor.RED));
            return true;
        }

        MythicItem item = itemRegistry.get(args[1]);
        if (item == null) {
            sender.sendMessage(Component.text("Objet inconnu : " + args[1] + " (voir /mythiclist).", NamedTextColor.RED));
            return true;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Quantité invalide : " + args[2], NamedTextColor.RED));
                return true;
            }
        }

        for (int i = 0; i < amount; i++) {
            ItemStack stack = item.build();
            target.getInventory().addItem(stack);
        }

        sender.sendMessage(Component.text(amount + "x " + item.getId() + " donné(s) à " + target.getName() + ".", NamedTextColor.GREEN));
        target.sendMessage(Component.text("Tu as reçu " + amount + "x " + item.getId() + " d'un admin !", NamedTextColor.LIGHT_PURPLE));
        return true;
    }
}
