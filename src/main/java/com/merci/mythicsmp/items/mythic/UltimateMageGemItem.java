package com.merci.mythicsmp.items.mythic;

import com.merci.mythicsmp.items.ItemRarity;
import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.utils.ItemBuilder;
import com.merci.mythicsmp.utils.ItemIdentifier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Obtenue en tuant le boss une fois le niveau d'XP (barre d'XP) à 445
 * atteint ET les 4 éléments déjà maîtrisés (voir BossManager). Clic droit
 * pour débloquer la classe Ultime Mage et ses 5 sorts (voir
 * UltimateMageManager / UltimateMageGemListener). Se consomme à l'utilisation
 * réussie.
 */
public class UltimateMageGemItem implements MythicItem {

    public static final String ID = "gemme_mage_ultime";

    private final Plugin plugin;

    public UltimateMageGemItem(Plugin plugin) {
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
        return new ItemBuilder(plugin, Material.END_CRYSTAL)
                .name("Gemme du Mage Ultime", getRarity())
                .lore("Clic droit : débloque la classe Ultime Mage")
                .lore("et ses 5 sorts (nécessite de maîtriser")
                .lore("déjà les 4 éléments).")
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
