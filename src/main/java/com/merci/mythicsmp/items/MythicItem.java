package com.merci.mythicsmp.items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

/**
 * Contrat que tout objet unique du plugin doit respecter.
 * Chaque objet sait :
 *  - construire son ItemStack (nom, lore, enchant glow, PersistentDataContainer)
 *  - se reconnaître à partir d'un ItemStack (via sa clé NBT)
 *
 * Les listeners (SpearListener, WeaponEffectListener...) se basent
 * uniquement sur getId() pour savoir quel comportement appliquer,
 * jamais sur le nom affiché (qui peut changer côté joueur).
 */
public interface MythicItem {

    /** Identifiant unique et stable de l'objet, ex: "lance_fer". */
    String getId();

    /** Rareté affichée dans le lore. */
    ItemRarity getRarity();

    /** Construit un exemplaire neuf de l'objet, prêt à être donné. */
    ItemStack build();

    /** Vrai si l'ItemStack donné est bien une instance de cet objet mythique. */
    boolean matches(ItemStack stack);

    /** Clé NBT partagée pour stocker l'id de l'objet dans le PersistentDataContainer. */
    static NamespacedKey idKey(org.bukkit.plugin.Plugin plugin) {
        return new NamespacedKey(plugin, "mythic_id");
    }

    /** Clé NBT utilisée pour marquer une épée élémentaire comme améliorée par une gemme. */
    static NamespacedKey upgradedKey(org.bukkit.plugin.Plugin plugin) {
        return new NamespacedKey(plugin, "mythic_upgraded");
    }
}
