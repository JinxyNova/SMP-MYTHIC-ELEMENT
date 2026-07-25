package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.items.ItemRarity;
import com.merci.mythicsmp.items.ItemRegistry;
import com.merci.mythicsmp.items.MythicItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Commande publique listant tous les objets mythiques (armes et objets
 * spéciaux) du serveur, groupés par rareté, avec leur id et leur lore
 * (qui contient les infos de puissance : dégâts, effets, etc.). Ouverte à
 * tout le monde pour servir de wiki des objets.
 */
public class MythicListCommand implements CommandExecutor {

    private final ItemRegistry itemRegistry;

    public MythicListCommand(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ItemRarity filter = null;
        if (args.length >= 1) {
            filter = parseRarity(args[0]);
            if (filter == null) {
                sender.sendMessage(Component.text("Rareté inconnue : " + args[0]
                        + " (commun, rare, epique, legendaire, mythique).", NamedTextColor.RED));
                return true;
            }
        }

        sender.sendMessage(Component.text("=== Objets & Armes de MythicSMP ===", NamedTextColor.GOLD));

        List<MythicItem> items = new ArrayList<>(itemRegistry.all().values());
        items.sort(Comparator.comparing((MythicItem item) -> item.getRarity().ordinal())
                .thenComparing(MythicItem::getId));

        ItemRarity currentSection = null;
        for (MythicItem item : items) {
            if (filter != null && item.getRarity() != filter) continue;
            if (item.getRarity() != currentSection) {
                currentSection = item.getRarity();
                sender.sendMessage(Component.text(" "));
                sender.sendMessage(Component.text("★ " + currentSection.getLabel(), currentSection.getColor()));
            }
            sender.sendMessage(Component.text("  • " + displayName(item) + "  (id: " + item.getId() + ")", NamedTextColor.GRAY));
            for (String loreLine : loreLines(item)) {
                sender.sendMessage(Component.text("      " + loreLine, NamedTextColor.DARK_GRAY));
            }
        }
        return true;
    }

    private String displayName(MythicItem item) {
        ItemStack stack = item.build();
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && meta.displayName() != null) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(meta.displayName());
        }
        return item.getId();
    }

    private List<String> loreLines(MythicItem item) {
        List<String> lines = new ArrayList<>();
        ItemStack stack = item.build();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || meta.lore() == null) return lines;
        var serializer = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText();
        for (Component component : meta.lore()) {
            String text = serializer.serialize(component).trim();
            if (!text.isEmpty() && !text.startsWith("★")) {
                lines.add(text);
            }
        }
        return lines;
    }

    private ItemRarity parseRarity(String text) {
        try {
            return ItemRarity.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            for (ItemRarity rarity : ItemRarity.values()) {
                if (rarity.getLabel().equalsIgnoreCase(text)) return rarity;
            }
            return null;
        }
    }
}
