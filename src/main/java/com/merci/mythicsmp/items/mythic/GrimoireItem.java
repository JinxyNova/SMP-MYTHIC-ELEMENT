package com.merci.mythicsmp.items.mythic;

import com.merci.mythicsmp.items.Ids;
import com.merci.mythicsmp.items.ItemRarity;
import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.utils.ItemBuilder;
import com.merci.mythicsmp.utils.ItemIdentifier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Livre permettant d'accéder rapidement à 9 sorts favoris (voir
 * GrimoireManager / GrimoireGuiManager dans le package spells) sans
 * repasser par la roue des sorts complète.
 *
 * Clic droit : ouvre la roue rapide des 9 sorts assignés.
 * Clic droit + sneak : ouvre l'écran de configuration des 9 cases.
 * (voir GrimoireListener pour le branchement des clics)
 */
public class GrimoireItem implements MythicItem {

    private final Plugin plugin;

    public GrimoireItem(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return Ids.GRIMOIRE;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIQUE;
    }

    @Override
    public ItemStack build() {
        return new ItemBuilder(plugin, Material.BOOK)
                .name("Grimoire", getRarity())
                .lore("Clic droit : ouvre la roue rapide")
                .lore("de tes 9 sorts favoris.")
                .lore("Clic droit + sneak : configure")
                .lore("quels sorts y assigner.")
                .rarityFooter(getRarity())
                .glow()
                .tag(getId())
                .build();
    }

    @Override
    public boolean matches(ItemStack stack) {
        return ItemIdentifier.hasId(plugin, stack, getId());
    }
}
