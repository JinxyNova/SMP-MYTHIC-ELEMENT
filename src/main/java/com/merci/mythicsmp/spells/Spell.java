package com.merci.mythicsmp.spells;

import com.merci.mythicsmp.elements.Element;

/**
 * Définition statique d'un sort (voir SpellRegistry pour les 64 sorts).
 * L'état par joueur (débloqué ou non, nombre d'utilisations, niveau de
 * maîtrise) est géré séparément par SpellManager/SpellProgress.
 */
public record Spell(String id, Element element, SpellTier tier, String name, String description, SpellEffect effect) {
}
