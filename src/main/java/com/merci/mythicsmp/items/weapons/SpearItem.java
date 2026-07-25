package com.merci.mythicsmp.items.weapons;

import com.merci.mythicsmp.items.ItemRarity;
import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.utils.ItemBuilder;
import com.merci.mythicsmp.utils.ItemIdentifier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Une lance qui se lance comme un trident (le comportement de vol est géré
 * par SpearListener). Trois tiers existent, tous construits depuis cette
 * même classe pour éviter la duplication : voir SpearItem.tiers(plugin).
 */
public class SpearItem implements MythicItem {

    private final Plugin plugin;
    private final String id;
    private final String displayName;
    private final ItemRarity rarity;
    private final double throwDamage;
    private final int cooldownSeconds;

    public SpearItem(Plugin plugin, String id, String displayName, ItemRarity rarity,
                      double throwDamage, int cooldownSeconds) {
        this.plugin = plugin;
        this.id = id;
        this.displayName = displayName;
        this.rarity = rarity;
        this.throwDamage = throwDamage;
        this.cooldownSeconds = cooldownSeconds;
    }

    public double getThrowDamage() {
        return throwDamage;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
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
        return new ItemBuilder(plugin, Material.TRIDENT)
                .name(displayName, rarity)
                .lore("Clic droit : lance la lance devant toi.")
                .lore("Dégâts au lancer : " + throwDamage)
                .lore("Recharge : " + cooldownSeconds + "s")
                .rarityFooter(rarity)
                .glow()
                .unbreakable()
                .tag(id)
                .build();
    }

    @Override
    public boolean matches(ItemStack stack) {
        return ItemIdentifier.hasId(plugin, stack, id);
    }

    /** Fabrique les 3 tiers de lance d'un coup, prêts à enregistrer dans le registre. */
    public static SpearItem[] tiers(Plugin plugin) {
        return new SpearItem[] {
                new SpearItem(plugin, "lance_fer", "Lance de Fer", ItemRarity.COMMUN, 6.0, 3),
                new SpearItem(plugin, "lance_netherite", "Lance de Netherite", ItemRarity.EPIQUE, 10.0, 2),
                new SpearItem(plugin, "lance_celeste", "Lance Céleste", ItemRarity.LEGENDAIRE, 14.0, 1)
        };
    }
}
