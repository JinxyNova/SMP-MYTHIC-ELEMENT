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

public class MythicGiveCommand implements CommandExecutor {

    private final ItemRegistry registry;

    public MythicGiveCommand(ItemRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage : /mythicgive <joueur> <id> [quantité]", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Joueur introuvable : " + args[0], NamedTextColor.RED));
            return true;
        }

        MythicItem item = registry.get(args[1]);
        if (item == null) {
            sender.sendMessage(Component.text("Objet inconnu : " + args[1] + " (voir /mythiclist)", NamedTextColor.RED));
            return true;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException ignored) {
                sender.sendMessage(Component.text("Quantité invalide, 1 utilisé par défaut.", NamedTextColor.YELLOW));
            }
        }

        ItemStack stack = item.build();
        stack.setAmount(amount);
        target.getInventory().addItem(stack);

        sender.sendMessage(Component.text("Objet donné à " + target.getName() + ".", NamedTextColor.GREEN));
        target.sendMessage(Component.text("Tu as reçu un objet mythique !", NamedTextColor.GREEN));
        return true;
    }
}
