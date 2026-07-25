package com.merci.mythicsmp.elements;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

/**
 * Les 4 éléments du système de classes élémentaires. Un joueur choisit un
 * premier élément à sa toute première connexion (définitif) puis peut en
 * débloquer d'autres grâce à la Gemme au Pouvoir Infini, jusqu'à en
 * maîtriser 4 au maximum.
 *
 * Les sorts propres à chaque élément (16 par élément : 4 faibles, 4 moyens,
 * 4 forts, 4 ultra-forts) ne sont pas encore implémentés ici — cette classe
 * ne pose que le squelette du système de classes en attendant.
 */
public enum Element {

    FEU("Feu", NamedTextColor.RED, Material.BLAZE_POWDER),
    EAU("Eau", NamedTextColor.AQUA, Material.HEART_OF_THE_SEA),
    TERRE("Terre", NamedTextColor.GREEN, Material.EMERALD),
    VENT("Vent", NamedTextColor.WHITE, Material.FEATHER);

    private final String label;
    private final NamedTextColor color;
    private final Material icon;

    Element(String label, NamedTextColor color, Material icon) {
        this.label = label;
        this.color = color;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public Material getIcon() {
        return icon;
    }
}
