package com.merci.mythicsmp.items;

import com.merci.mythicsmp.utils.ItemBuilder;
import com.merci.mythicsmp.utils.ItemIdentifier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Sert à tous les objets dont le comportement se résume à "un id + un
 * branchement dans un listener" (pas de champ spécifique comme les tiers
 * de lance). Le comportement réel vit dans les listeners (CombatEffects,
 * ThrowableItems, UtilityItems, PassiveEquipment) qui branchent sur l'id
 * via les constantes de la classe Ids.
 *
 * Pour un objet qui a besoin de données propres (ex: 3 tiers de dégâts
 * différents), on garde une vraie classe dédiée comme SpearItem.
 */
public class GenericMythicItem implements MythicItem {

    private final Plugin plugin;
    private final String id;
    private final String displayName;
    private final ItemRarity rarity;
    private final Material material;
    private final List<String> lore;
    private final boolean glow;

    public GenericMythicItem(Plugin plugin, String id, String displayName, ItemRarity rarity,
                              Material material, List<String> lore, boolean glow) {
        this.plugin = plugin;
        this.id = id;
        this.displayName = displayName;
        this.rarity = rarity;
        this.material = material;
        this.lore = lore;
        this.glow = glow;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ItemRarity getRarity() {
        return rarity;
    }

    @Override
    public ItemStack build() {
        ItemBuilder builder = new ItemBuilder(plugin, material)
                .name(displayName, rarity);
        for (String line : lore) {
            builder.lore(line);
        }
        builder.rarityFooter(rarity);
        if (glow) builder.glow();
        return builder.tag(id).build();
    }

    @Override
    public boolean matches(ItemStack stack) {
        return ItemIdentifier.hasId(plugin, stack, id);
    }
}
