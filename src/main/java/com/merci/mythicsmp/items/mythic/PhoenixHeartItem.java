package com.merci.mythicsmp.items.mythic;

import com.merci.mythicsmp.items.ItemRarity;
import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.utils.ItemBuilder;
import com.merci.mythicsmp.utils.ItemIdentifier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Doit être gardé dans l'inventaire (pas forcément en main). Si le joueur
 * subit un coup qui l'aurait tué, PhoenixHeartListener annule les dégâts,
 * le soigne et consomme un exemplaire de l'objet.
 */
public class PhoenixHeartItem implements MythicItem {

    public static final String ID = "coeur_phenix";
    public static final double HEAL_HEARTS = 10.0; // points de vie rendus (10 = 5 cœurs)

    private final Plugin plugin;

    public PhoenixHeartItem(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.MYTHIQUE;
    }

    @Override
    public ItemStack build() {
        return new ItemBuilder(plugin, Material.TOTEM_OF_UNDYING)
                .name("Cœur du Phénix", getRarity())
                .lore("Gardé dans l'inventaire, te ramène")
                .lore("d'entre les morts une seule fois.")
                .lore("Se consume après usage.")
                .rarityFooter(getRarity())
                .glow()
                .tag(ID)
                .build();
    }

    @Override
    public boolean matches(ItemStack stack) {
        return ItemIdentifier.hasId(plugin, stack, ID);
    }
}
