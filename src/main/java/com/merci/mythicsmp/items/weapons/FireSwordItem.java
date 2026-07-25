package com.merci.mythicsmp.items.weapons;

import com.merci.mythicsmp.items.ItemRarity;
import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.utils.ItemBuilder;
import com.merci.mythicsmp.utils.ItemIdentifier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class FireSwordItem implements MythicItem {

    public static final String ID = "epee_feu";
    public static final int FIRE_TICKS = 100;        // 5 secondes de feu sur la cible
    public static final double RING_RADIUS = 2.0;     // rayon de la zone de feu au sol

    private final Plugin plugin;

    public FireSwordItem(Plugin plugin) {
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
        return new ItemBuilder(plugin, Material.NETHERITE_SWORD)
                .name("Épée du Brasier", getRarity())
                .lore("Chaque coup enflamme la cible.")
                .lore("Fait apparaître un anneau de feu au sol.")
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
