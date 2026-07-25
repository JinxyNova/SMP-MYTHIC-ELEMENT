package com.merci.mythicsmp.quests;

/**
 * Chaque type correspond à un événement Bukkit précis suivi par
 * QuestProgressListener. Pour ajouter un type : une valeur ici + son
 * branchement dans QuestProgressListener + au moins une QuestDefinition
 * dans QuestPool qui l'utilise.
 */
public enum QuestType {
    TUER_MOBS,
    MINER_BLOC,
    DEGATS_ARME_MYTHIQUE,
    VENDRE_AU_MARCHE,
    AMELIORER_A_LA_FORGE
}
