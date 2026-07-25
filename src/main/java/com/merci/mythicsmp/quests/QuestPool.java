package com.merci.mythicsmp.quests;

import org.bukkit.Material;

import java.util.List;

/**
 * Toutes les quêtes possibles. QuestManager en tire 3 au sort chaque jour
 * (avec une graine basée sur la date, donc les mêmes 3 pour tout le monde
 * ce jour-là). Pour ajouter une quête au pool : une ligne de plus ici.
 */
public final class QuestPool {

    private QuestPool() {}

    public static final List<QuestDefinition> ALL = List.of(
            new QuestDefinition("tuer_15_mobs", QuestType.TUER_MOBS,
                    "Éliminer 15 monstres", 15, 50.0, null),
            new QuestDefinition("miner_20_fer", QuestType.MINER_BLOC,
                    "Miner 20 minerais de fer", 20, 40.0, Material.IRON_ORE),
            new QuestDefinition("miner_10_diamant", QuestType.MINER_BLOC,
                    "Miner 10 minerais de diamant", 10, 80.0, Material.DIAMOND_ORE),
            new QuestDefinition("degats_arme_mythique", QuestType.DEGATS_ARME_MYTHIQUE,
                    "Infliger 100 points de dégâts avec une arme mythique", 100, 60.0, null),
            new QuestDefinition("vendre_2_marche", QuestType.VENDRE_AU_MARCHE,
                    "Mettre 2 objets en vente au Marché Mythique", 2, 30.0, null),
            new QuestDefinition("ameliorer_1_forge", QuestType.AMELIORER_A_LA_FORGE,
                    "Réussir une amélioration à la Forge Mythique", 1, 70.0, null)
    );
}
