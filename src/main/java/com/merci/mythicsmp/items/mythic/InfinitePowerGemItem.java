package com.merci.mythicsmp.items.mythic;

import com.merci.mythicsmp.items.ItemRarity;
import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.utils.ItemBuilder;
import com.merci.mythicsmp.utils.ItemIdentifier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Obtenue en tuant le boss une fois le niveau d'XP (barre d'XP) à 300
 * atteint (voir BossManager). Clic droit pour ouvrir le menu de
 * déblocage d'un nouvel élément (voir ElementMenuManager / InfinitePowerGemListener).
 * Se consomme à l'utilisation réussie.
 */
public class InfinitePowerGemItem implements MythicItem {

    public static final String ID = "gemme_pouvoir_infini";

    private final Plugin plugin;

    public InfinitePowerGemItem(Plugin plugin) {
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
        return new ItemBuilder(plugin, Material.NETHER_STAR)
                .name("Gemme au Pouvoir Infini", getRarity())
                .lore("Clic droit : débloque un nouvel élément")
                .lore("à maîtriser (jusqu'à 4 au total).")
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
