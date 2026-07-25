package com.merci.mythicsmp.items.mythic;

import com.merci.mythicsmp.items.ItemRarity;
import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.utils.ItemBuilder;
import com.merci.mythicsmp.utils.ItemIdentifier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Gemme à utiliser (clic droit en tenant l'épée correspondante, voir
 * GemFusionListener) pour rendre les effets de l'épée élémentaire plus forts.
 * Se consomme lors de la fusion.
 */
public class ElementalGemItem implements MythicItem {

    public static final String ID_FEU = "gemme_feu";
    public static final String ID_GLACE = "gemme_glace";

    private final Plugin plugin;
    private final String id;
    private final String displayName;
    private final Material material;

    private ElementalGemItem(Plugin plugin, String id, String displayName, Material material) {
        this.plugin = plugin;
        this.id = id;
        this.displayName = displayName;
        this.material = material;
    }

    public static ElementalGemItem fire(Plugin plugin) {
        return new ElementalGemItem(plugin, ID_FEU, "Gemme du Brasier", Material.FIRE_CHARGE);
    }

    public static ElementalGemItem ice(Plugin plugin) {
        return new ElementalGemItem(plugin, ID_GLACE, "Gemme du Frimas", Material.PRISMARINE_CRYSTALS);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public ItemStack build() {
        return new ItemBuilder(plugin, material)
                .name(displayName, getRarity())
                .lore("Clic droit en tenant l'épée élémentaire")
                .lore("correspondante pour l'améliorer.")
                .rarityFooter(getRarity())
                .glow()
                .tag(id)
                .build();
    }

    @Override
    public boolean matches(ItemStack stack) {
        return ItemIdentifier.hasId(plugin, stack, id);
    }
}
