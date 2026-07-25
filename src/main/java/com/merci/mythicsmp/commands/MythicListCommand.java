package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.items.ItemRegistry;
import com.merci.mythicsmp.items.MythicItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MythicListCommand implements CommandExecutor {

    private final ItemRegistry registry;

    public MythicListCommand(ItemRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(Component.text("=== Objets mythiques disponibles ===", NamedTextColor.GOLD));
        for (MythicItem item : registry.all().values()) {
            sender.sendMessage(Component.text("- " + item.getId(), NamedTextColor.GRAY)
                    .append(Component.text("  [" + item.getRarity().getLabel() + "]", item.getRarity().getColor())));
        }
        return true;
    }
}
