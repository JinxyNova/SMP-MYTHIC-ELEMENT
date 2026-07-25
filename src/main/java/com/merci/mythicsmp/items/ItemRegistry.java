package com.merci.mythicsmp.items;

import com.merci.mythicsmp.elements.Element;
import com.merci.mythicsmp.items.mythic.AdminHeadItem;
import com.merci.mythicsmp.items.mythic.ElementalGemItem;
import com.merci.mythicsmp.items.mythic.InfinitePowerGemItem;
import com.merci.mythicsmp.items.mythic.PhoenixHeartItem;
import com.merci.mythicsmp.items.mythic.SpellRuneItem;
import com.merci.mythicsmp.items.mythic.UltimateMageGemItem;
import com.merci.mythicsmp.items.weapons.FireSwordItem;
import com.merci.mythicsmp.items.weapons.IceSwordItem;
import com.merci.mythicsmp.items.weapons.SpearItem;
import com.merci.mythicsmp.spells.SpellTier;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Point d'entrée unique pour retrouver un objet mythique par son id.
 *
 * Deux façons d'ajouter un objet :
 *  - un comportement simple, réutilisable via un listener existant :
 *    register(generic(...)) directement ici, aucune nouvelle classe requise
 *  - un comportement avec ses propres données (ex: 3 tiers) : créer une
 *    vraie classe (voir items/weapons/SpearItem.java) et l'enregistrer
 */
public class ItemRegistry {

    private final Plugin plugin;
    private final Map<String, MythicItem> items = new LinkedHashMap<>();

    public ItemRegistry(Plugin plugin) {
        this.plugin = plugin;

        // --- Objets avec classe dédiée (comportement ou données spécifiques) ---
        for (SpearItem spear : SpearItem.tiers(plugin)) {
            register(spear);
        }
        register(new FireSwordItem(plugin));
        register(new IceSwordItem(plugin));
        register(ElementalGemItem.fire(plugin));
        register(ElementalGemItem.ice(plugin));
        register(new AdminHeadItem(plugin));
        register(new PhoenixHeartItem(plugin));
        register(new InfinitePowerGemItem(plugin));
        register(new UltimateMageGemItem(plugin));
        for (Element element : Element.values()) {
            for (SpellTier tier : SpellTier.values()) {
                register(new SpellRuneItem(plugin, element, tier));
            }
        }

        // --- Combat corps-à-corps (voir CombatEffectsListener) ---
        register(generic(Ids.MARTEAU_GUERRE, "Marteau de Guerre", ItemRarity.RARE, Material.MACE,
                List.of("Chaque coup repousse les ennemis", "proches dans un petit rayon.")));
        register(generic(Ids.FAUX_MOISSON, "Faux de la Moisson", ItemRarity.RARE, Material.DIAMOND_HOE,
                List.of("Un kill donne un bonus d'XP", "et une chance de loot rare.")));
        register(generic(Ids.HACHE_TONNERRE, "Hache Tonnerre", ItemRarity.EPIQUE, Material.NETHERITE_AXE,
                List.of("Chance d'invoquer la foudre", "sur la cible touchée.")));
        register(generic(Ids.DAGUE_POISON, "Dague du Poison", ItemRarity.RARE, Material.IRON_SWORD,
                List.of("Dégâts faibles mais empoisonne", "la cible, le poison s'accumule.")));
        register(generic(Ids.BOUCLIER_MIROIR, "Bouclier Miroir", ItemRarity.EPIQUE, Material.SHIELD,
                List.of("En bloquant, renvoie une partie", "des dégâts à l'attaquant.")));
        register(generic(Ids.FOUET, "Fouet des Âmes", ItemRarity.RARE, Material.LEAD,
                List.of("Clic droit : attire la cible visée", "violemment vers toi.")));
        register(generic(Ids.GANTELET_EXPLOSIF, "Gantelet Explosif", ItemRarity.EPIQUE, Material.NETHERITE_HOE,
                List.of("Chaque coup à mains nues crée", "une petite explosion sans dégât de bloc.")));

        // --- Distance ---
        register(generic(Ids.ARC_VENT, "Arc du Vent", ItemRarity.EPIQUE, Material.BOW,
                List.of("Les flèches tirées repoussent", "violemment leur cible en arrière.")));

        // --- Lancers à effet de zone (voir ThrowableItemsListener) ---
        register(generic(Ids.BOMBE_GEL, "Bombe de Gel", ItemRarity.RARE, Material.SNOWBALL,
                List.of("À l'impact : gèle l'eau proche", "et ralentit tout le monde autour.")));
        register(generic(Ids.GRENADE_FUMIGENE, "Grenade Fumigène", ItemRarity.RARE, Material.GRAY_DYE,
                List.of("À l'impact : nuage de fumée", "qui aveugle brièvement la zone.")));
        register(generic(Ids.TOTEM_SOIN, "Totem de Soin", ItemRarity.EPIQUE, Material.GLOWSTONE_DUST,
                List.of("À l'impact : zone de soin temporaire", "pour toi et tes alliés proches.")));

        // --- Utilitaires actifs, clic droit (voir UtilityItemsListener) ---
        register(generic(Ids.ANCRE_TELEPORTATION, "Ancre de Téléportation", ItemRarity.RARE, Material.LODESTONE,
                List.of("Clic droit (sneak) : pose un point de retour.", "Clic droit (debout) : reviens à ce point.")));
        register(generic(Ids.BOUSSOLE_TRESOR, "Boussole du Trésor", ItemRarity.RARE, Material.COMPASS,
                List.of("Clic droit : pointe vers la structure", "la plus proche.")));
        register(generic(Ids.GRAPPIN, "Grappin", ItemRarity.RARE, Material.FISHING_ROD,
                List.of("Clic droit : te propulse vers le bloc", "ou l'ennemi visé.")));
        register(generic(Ids.BATON_TELEPORTATION_ENNEMI, "Bâton de Bannissement", ItemRarity.EPIQUE, Material.BLAZE_ROD,
                List.of("Clic droit sur un joueur : le téléporte", "à un endroit aléatoire proche.")));
        register(generic(Ids.FRAGMENT_ETOILE, "Fragment d'Étoile", ItemRarity.MYTHIQUE, Material.NETHER_STAR,
                List.of("Clic droit : se consume pour t'offrir", "un objet légendaire aléatoire.")));
        register(generic(Ids.SAC_RANGEMENT, "Sac de Rangement", ItemRarity.RARE, Material.BUNDLE,
                List.of("Clic droit : ouvre ton coffre", "de rangement personnel (36 cases).")));

        // --- Passifs / portés (voir PassiveEquipmentTask) ---
        register(generic(Ids.AMULETTE_TEMPS, "Amulette du Temps", ItemRarity.EPIQUE, Material.CLOCK,
                List.of("Portée dans l'inventaire : ralentit", "légèrement les ennemis proches.")));
        register(generic(Ids.OEIL_WITHER, "Œil du Wither", ItemRarity.EPIQUE, Material.WITHER_SKELETON_SKULL,
                List.of("Clic droit : les entités proches", "deviennent visibles à travers les murs.")));
        register(generic(Ids.COURONNE_ROI, "Couronne du Roi", ItemRarity.MYTHIQUE, Material.GOLDEN_HELMET,
                List.of("Portée : booste l'XP gagné", "par toi et tes alliés proches.")));
        register(generic(Ids.LANTERNE_AME, "Lanterne d'Âme Améliorée", ItemRarity.RARE, Material.SOUL_LANTERN,
                List.of("Portée ou en main : repousse les", "mobs hostiles à proximité.")));
        register(generic(Ids.ELYTRES_VENT, "Élytres du Vent", ItemRarity.LEGENDAIRE, Material.ELYTRA,
                List.of("En vol : un boost de vitesse", "s'active régulièrement.")));
    }

    private GenericMythicItem generic(String id, String name, ItemRarity rarity, Material material, List<String> lore) {
        return new GenericMythicItem(plugin, id, name, rarity, material, lore, true);
    }

    private void register(MythicItem item) {
        items.put(item.getId(), item);
    }

    public MythicItem get(String id) {
        return items.get(id);
    }

    public Map<String, MythicItem> all() {
        return items;
    }
}
