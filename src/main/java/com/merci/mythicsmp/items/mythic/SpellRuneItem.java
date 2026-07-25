package com.merci.mythicsmp.items.mythic;

import com.merci.mythicsmp.elements.Element;
import com.merci.mythicsmp.items.ItemRarity;
import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.spells.SpellTier;
import com.merci.mythicsmp.utils.ItemBuilder;
import com.merci.mythicsmp.utils.ItemIdentifier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Une rune par (élément, palier) — 16 au total. Clic droit avec une rune en
 * main ouvre le menu de déblocage du palier correspondant (voir
 * SpellWheelManager#openUnlock / SpellRuneListener), à condition que le
 * joueur maîtrise déjà l'élément associé. Le joueur choisit ensuite lequel
 * des 4 sorts de ce palier débloquer ; la rune est consommée au choix.
 */
public class SpellRuneItem implements MythicItem {

    private final Plugin plugin;
    private final Element element;
    private final SpellTier tier;

    public SpellRuneItem(Plugin plugin, Element element, SpellTier tier) {
        this.plugin = plugin;
        this.element = element;
        this.tier = tier;
    }

    public static String idFor(Element element, SpellTier tier) {
        return "rune_" + element.name().toLowerCase() + "_" + tier.name().toLowerCase();
    }

    public Element getElement() {
        return element;
    }

    public SpellTier getTier() {
        return tier;
    }

    @Override
    public String getId() {
        return idFor(element, tier);
    }

    @Override
    public ItemRarity getRarity() {
        return switch (tier) {
            case FAIBLE -> ItemRarity.RARE;
            case MOYEN -> ItemRarity.EPIQUE;
            case FORT, ULTRA_FORT -> ItemRarity.LEGENDAIRE;
        };
    }

    @Override
    public ItemStack build() {
        return new ItemBuilder(plugin, Material.PRISMARINE_SHARD)
                .name("Rune " + element.getLabel() + " (" + tier.getLabel() + ")", getRarity())
                .lore("Clic droit : débloque un sort " + tier.getLabel().toLowerCase())
                .lore("de l'élément " + element.getLabel() + " au choix.")
                .lore("Nécessite de maîtriser cet élément.")
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
