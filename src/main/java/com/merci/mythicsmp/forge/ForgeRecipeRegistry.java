package com.merci.mythicsmp.forge;

import com.merci.mythicsmp.items.Ids;
import com.merci.mythicsmp.items.ItemRegistry;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Construit toutes les recettes de la forge au démarrage. Deux familles :
 *  - les recettes de tier explicites pour les lances (un objet précis en sort)
 *  - une recette générique "améliore en place" appliquée à toutes les armes
 *    de combat qui n'ont pas déjà un chemin de progression dédié (les
 *    épées élémentaires utilisent la fusion de gemmes, pas la forge).
 *
 * Pour ajouter une recette de tier plus tard : un ForgeRecipe de plus dans
 * la liste. Pour rendre un nouvel objet améliorable en place : ajouter son
 * id dans GENERIC_UPGRADE_IDS.
 */
public class ForgeRecipeRegistry {

    private static final String[] GENERIC_UPGRADE_IDS = {
            Ids.MARTEAU_GUERRE, Ids.FAUX_MOISSON, Ids.HACHE_TONNERRE, Ids.DAGUE_POISON,
            Ids.BOUCLIER_MIROIR, Ids.FOUET, Ids.GANTELET_EXPLOSIF, Ids.ARC_VENT
    };

    private final List<ForgeRecipe> recipes = new ArrayList<>();

    public ForgeRecipeRegistry(ItemRegistry itemRegistry) {
        // Tiers de lance : coûts progressifs
        recipes.add(new ForgeRecipe(
                "lance_fer",
                Map.of(Material.NETHERITE_SCRAP, 2, Material.GOLD_INGOT, 4),
                itemRegistry.get("lance_netherite")
        ));
        recipes.add(new ForgeRecipe(
                "lance_netherite",
                Map.of(Material.NETHER_STAR, 1, Material.DIAMOND, 4),
                itemRegistry.get("lance_celeste")
        ));

        // Amélioration générique en place pour les armes de combat restantes
        for (String id : GENERIC_UPGRADE_IDS) {
            recipes.add(new ForgeRecipe(
                    id,
                    Map.of(Material.DIAMOND, 3, Material.NETHERITE_INGOT, 1),
                    null
            ));
        }
    }

    public ForgeRecipe findFor(String inputId) {
        return recipes.stream().filter(r -> r.inputId().equals(inputId)).findFirst().orElse(null);
    }
}
