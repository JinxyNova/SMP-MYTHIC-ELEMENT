package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.items.ItemRarity;
import com.merci.mythicsmp.items.ItemRegistry;
import com.merci.mythicsmp.items.MythicItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;

/**
 * /mythiclist [rareté] — liste tous les objets/armes mythiques, groupés par
 * rareté (Commun -> Mythique), avec id + description/stats (extraits du
 * lore réel de l'objet, donc toujours à jour même si un item change son
 * lore). Un argument de rareté optionnel filtre l'affichage à une seule
 * rareté (ex : /mythiclist légendaire).
 */
public class MythicListCommand implements CommandExecutor {

    private final ItemRegistry registry;

    public MythicListCommand(ItemRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ItemRarity filter = null;
        if (args.length >= 1) {
            filter = parseRarity(args[0]);
            if (filter == null) {
                sender.sendMessage(Component.text(
                        "Rareté inconnue : " + args[0] + " (commun, rare, épique, légendaire, mythique)",
                        NamedTextColor.RED));
                return true;
            }
        }

        sender.sendMessage(Component.text(
                "=== Objets mythiques" + (filter != null ? " — " + filter.getLabel() : "") + " ===",
                NamedTextColor.GOLD));

        for (ItemRarity rarity : ItemRarity.values()) {
            if (filter != null && rarity != filter) continue;

            List<MythicItem> items = registry.all().values().stream()
                    .filter(item -> item.getRarity() == rarity)
                    .toList();
            if (items.isEmpty()) continue;

            sender.sendMessage(Component.text("--- " + rarity.getLabel() + " ---", rarity.getColor()));
            for (MythicItem item : items) {
                ItemStack stack = item.build();
                ItemMeta meta = stack.getItemMeta();

                String displayName = meta != null && meta.displayName() != null
                        ? PlainTextComponentSerializer.plainText().serialize(meta.displayName())
                        : item.getId();

                sender.sendMessage(Component.text("- " + item.getId() + " ", NamedTextColor.GRAY)
                        .append(Component.text(displayName, rarity.getColor())));

                if (meta != null && meta.hasLore() && meta.lore() != null) {
                    for (Component loreLine : meta.lore()) {
                        String plain = PlainTextComponentSerializer.plainText().serialize(loreLine);
                        // On saute les lignes vides et le footer "★ Rareté" (déjà affiché par le groupe).
                        if (plain.isBlank() || plain.startsWith("★")) continue;
                        sender.sendMessage(Component.text("    " + plain, NamedTextColor.DARK_GRAY));
                    }
                }
            }
        }
        return true;
    }

    private ItemRarity parseRarity(String input) {
        String normalized = stripAccents(input.toLowerCase(Locale.ROOT));
        for (ItemRarity rarity : ItemRarity.values()) {
            if (rarity.name().toLowerCase(Locale.ROOT).equals(normalized)) return rarity;
            if (stripAccents(rarity.getLabel().toLowerCase(Locale.ROOT)).equals(normalized)) return rarity;
        }
        return null;
    }

    private String stripAccents(String input) {
        return input
                .replace("é", "e").replace("è", "e").replace("ê", "e")
                .replace("à", "a")
                .replace("ï", "i");
    }
}
