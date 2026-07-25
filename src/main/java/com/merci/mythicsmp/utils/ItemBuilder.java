package com.merci.mythicsmp.utils;

import com.merci.mythicsmp.items.ItemRarity;
import com.merci.mythicsmp.items.MythicItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Petit builder fluide pour fabriquer les items du plugin sans dupliquer
 * la logique de nom/lore/rareté/NBT dans chaque classe d'objet.
 */
public class ItemBuilder {

    private final Plugin plugin;
    private final ItemStack stack;
    private final ItemMeta meta;
    private final List<Component> lore = new ArrayList<>();

    public ItemBuilder(Plugin plugin, Material material) {
        this.plugin = plugin;
        this.stack = new ItemStack(material);
        this.meta = stack.getItemMeta();
    }

    public ItemBuilder name(String name, ItemRarity rarity) {
        meta.displayName(Component.text(name, rarity.getColor())
                .decoration(TextDecoration.ITALIC, false));
        return this;
    }

    public ItemBuilder lore(String line) {
        lore.add(Component.text(line, net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        return this;
    }

    public ItemBuilder rarityFooter(ItemRarity rarity) {
        lore.add(Component.empty());
        lore.add(Component.text("★ " + rarity.getLabel(), rarity.getColor())
                .decoration(TextDecoration.ITALIC, false));
        return this;
    }

    public ItemBuilder glow() {
        meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemBuilder unbreakable() {
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        return this;
    }

    public ItemBuilder tag(String id) {
        NamespacedKey key = MythicItem.idKey(plugin);
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
        return this;
    }

    public ItemStack build() {
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }
}
