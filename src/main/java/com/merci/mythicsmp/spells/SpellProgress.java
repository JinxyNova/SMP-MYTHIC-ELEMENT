package com.merci.mythicsmp.spells;

/**
 * Suivi de la maîtrise d'un sort déjà débloqué par un joueur. Plus le sort
 * est utilisé, plus son niveau de maîtrise augmente (jusqu'à MAX_LEVEL),
 * ce qui améliore légèrement sa puissance et réduit sa recharge : c'est le
 * système "plus on joue un sort, plus on gagne d'options".
 */
public class SpellProgress {

    public static final int MAX_LEVEL = 3;
    private static final int[] USES_FOR_LEVEL = {0, 30, 100}; // seuil pour atteindre le niveau 1, 2, 3

    private int uses;

    public SpellProgress() {
        this(0);
    }

    public SpellProgress(int uses) {
        this.uses = uses;
    }

    public int getUses() {
        return uses;
    }

    /** @return true si cette utilisation a fait passer le sort à un niveau supérieur. */
    public boolean registerUse() {
        int before = getLevel();
        uses++;
        return getLevel() > before;
    }

    public int getLevel() {
        int level = 1;
        for (int i = USES_FOR_LEVEL.length - 1; i >= 0; i--) {
            if (uses >= USES_FOR_LEVEL[i]) {
                level = i + 1;
                break;
            }
        }
        return level;
    }

    public int getUsesForNextLevel() {
        int level = getLevel();
        if (level >= MAX_LEVEL) return -1;
        return USES_FOR_LEVEL[level];
    }

    /** +10% de puissance par niveau de maîtrise au-delà du niveau 1. */
    public double getPowerMultiplier() {
        return 1.0 + (getLevel() - 1) * 0.10;
    }

    /** -8% de recharge par niveau de maîtrise au-delà du niveau 1. */
    public double getCooldownMultiplier() {
        return 1.0 - (getLevel() - 1) * 0.08;
    }

    /** Admin : passe directement ce sort à son niveau de maîtrise maximum. */
    public void maxOut() {
        this.uses = USES_FOR_LEVEL[USES_FOR_LEVEL.length - 1];
    }
}
