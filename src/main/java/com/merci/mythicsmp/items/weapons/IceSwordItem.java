package com.merci.mythicsmp.items.weapons;

import com.merci.mythicsmp.items.ItemRarity;
import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.utils.ItemBuilder;
import com.merci.mythicsmp.utils.ItemIdentifier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class IceSwordItem implements MythicItem {

    public static final String ID = "epee_glace";
    public static final int SLOW_TICKS = 100;   // 5 secondes de ralentissement
    public static final int SLOW_LEVEL = 2;      // Lenteur III
    public static final int FREEZE_RADIUS = 3;   // rayon de gel de l'eau autour du coup

    private final Plugin plugin;

    public IceSwordItem(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIQUE;
    }

    @Override
    public ItemStack build() {
        return new ItemBuilder(plugin, Material.DIAMOND_SWORD)
                .name("Épée du Frimas", getRarity())
                .lore("Chaque coup ralentit fortement la cible.")
                .lore("Gèle l'eau à proximité de l'impact.")
                .rarityFooter(getRarity())
                .glow()
                .unbreakable()
                .tag(ID)
                .build();
    }

    @Override
    public boolean matches(ItemStack stack) {
        return ItemIdentifier.hasId(plugin, stack, ID);
    }
}
