package com.merci.mythicsmp.utils;

import com.merci.mythicsmp.items.MythicItem;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Lit l'id mythique stocké dans le PersistentDataContainer d'un item.
 * Utilisé par les listeners pour savoir "c'est quel objet ?" sans
 * dépendre du nom affiché.
 */
public final class ItemIdentifier {

    private ItemIdentifier() {}

    public static String getId(Plugin plugin, ItemStack stack) {
        if (stack == null || stack.getItemMeta() == null) return null;
        return stack.getItemMeta().getPersistentDataContainer()
                .get(MythicItem.idKey(plugin), PersistentDataType.STRING);
    }

    public static boolean hasId(Plugin plugin, ItemStack stack, String id) {
        return id.equals(getId(plugin, stack));
    }
}
