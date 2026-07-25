package com.merci.mythicsmp.items.mythic;

import com.merci.mythicsmp.items.ItemRarity;
import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.utils.ItemBuilder;
import com.merci.mythicsmp.utils.ItemIdentifier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Tête cosmétique portée dans le slot casque. Tant qu'elle est équipée,
 * WeaponEffectListener applique une régénération passive au porteur
 * (voir la boucle périodique dans MythicSMP#startPassiveEffectsTask).
 */
public class AdminHeadItem implements MythicItem {

    public static final String ID = "tete_admin";
    public static final int REGEN_LEVEL = 1;

    private final Plugin plugin;

    public AdminHeadItem(Plugin plugin) {
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
        return new ItemBuilder(plugin, Material.PLAYER_HEAD)
                .name("Couronne de l'Administrateur", getRarity())
                .lore("Un vestige d'un pouvoir ancien.")
                .lore("Porté : régénération passive.")
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
