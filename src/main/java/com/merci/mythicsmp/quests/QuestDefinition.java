package com.merci.mythicsmp.quests;

import org.bukkit.Material;

/**
 * @param material uniquement utilisé par MINER_BLOC (le bloc précis à miner) ; null sinon
 */
public record QuestDefinition(String id, QuestType type, String description, int targetAmount,
                               double rewardCoins, Material material) {

    /** Pour MINER_BLOC : accepte aussi la variante "deepslate" du même minerai. */
    public boolean matchesMinedBlock(Material broken) {
        if (material == null) return false;
        return broken == material || broken.name().equals("DEEPSLATE_" + material.name());
    }
}
