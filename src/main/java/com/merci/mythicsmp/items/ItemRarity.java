package com.merci.mythicsmp.items;

import net.kyori.adventure.text.format.TextColor;

/**
 * Les paliers de rareté utilisés par tous les objets du plugin.
 * Chaque rareté a sa couleur, utilisée dans le nom et le lore de l'item.
 */
public enum ItemRarity {

    COMMUN("Commun", TextColor.color(0xAAAAAA)),
    RARE("Rare", TextColor.color(0x55AAFF)),
    EPIQUE("Épique", TextColor.color(0xAA55FF)),
    LEGENDAIRE("Légendaire", TextColor.color(0xFFAA00)),
    MYTHIQUE("Mythique", TextColor.color(0xFF5555));

    private final String label;
    private final TextColor color;

    ItemRarity(String label, TextColor color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public TextColor getColor() {
        return color;
    }
}
