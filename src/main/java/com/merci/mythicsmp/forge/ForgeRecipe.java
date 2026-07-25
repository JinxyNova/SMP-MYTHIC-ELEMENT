package com.merci.mythicsmp.forge;

import com.merci.mythicsmp.items.MythicItem;
import org.bukkit.Material;

import java.util.Map;

/**
 * Une recette : un objet mythique en entrée + des matériaux, qui produit
 * soit un objet de remplacement (ex: lance_fer -> lance_netherite), soit
 * une simple amélioration en place de l'objet d'entrée (resultItem == null,
 * voir ForgeManager — on pose alors le flag "upgraded" comme pour les
 * gemmes, déjà lu par CombatEffectsListener / RangedEffectsListener).
 */
public record ForgeRecipe(String inputId, Map<Material, Integer> materials, MythicItem resultItem) {

    public boolean isUpgradeInPlace() {
        return resultItem == null;
    }
}
