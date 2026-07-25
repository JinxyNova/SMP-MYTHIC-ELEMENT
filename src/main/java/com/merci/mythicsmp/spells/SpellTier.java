package com.merci.mythicsmp.spells;

/**
 * Les 4 paliers de puissance de sorts, communs aux 4 éléments (4 sorts par
 * palier et par élément = 16 sorts par élément = 64 sorts au total).
 * powerScale sert de multiplicateur de base pour la puissance des effets
 * (dégâts, durée, rayon...), baseCooldownSeconds est le temps de recharge
 * de base avant réduction par la maîtrise du sort (voir SpellProgress).
 */
public enum SpellTier {

    FAIBLE("Faible", 8, 1.0),
    MOYEN("Moyen", 16, 1.7),
    FORT("Fort", 26, 2.6),
    ULTRA_FORT("Ultra-Fort", 45, 4.0);

    private final String label;
    private final int baseCooldownSeconds;
    private final double powerScale;

    SpellTier(String label, int baseCooldownSeconds, double powerScale) {
        this.label = label;
        this.baseCooldownSeconds = baseCooldownSeconds;
        this.powerScale = powerScale;
    }

    public String getLabel() {
        return label;
    }

    public int getBaseCooldownSeconds() {
        return baseCooldownSeconds;
    }

    public double getPowerScale() {
        return powerScale;
    }
}
