package com.merci.mythicsmp.spells;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * L'effet concret d'un sort. `power` combine déjà l'échelle du palier
 * (SpellTier#getPowerScale) et le bonus de maîtrise du joueur pour ce sort
 * (SpellProgress#getPowerMultiplier) — l'effet n'a qu'à s'en servir comme
 * multiplicateur pour ses dégâts/rayon/durée.
 */
@FunctionalInterface
public interface SpellEffect {
    void cast(Plugin plugin, Player caster, double power);
}
