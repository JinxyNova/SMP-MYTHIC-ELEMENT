package com.merci.mythicsmp.items;

/**
 * Tous les ids des objets "génériques" (voir GenericMythicItem) regroupés ici.
 * Les listeners branchent leur comportement sur ces constantes plutôt que
 * sur des chaînes en dur éparpillées dans le code.
 */
public final class Ids {

    private Ids() {}

    // Combat corps-à-corps
    public static final String MARTEAU_GUERRE = "marteau_guerre";
    public static final String FAUX_MOISSON = "faux_moisson";
    public static final String HACHE_TONNERRE = "hache_tonnerre";
    public static final String DAGUE_POISON = "dague_poison";
    public static final String BOUCLIER_MIROIR = "bouclier_miroir";
    public static final String FOUET = "fouet";
    public static final String GANTELET_EXPLOSIF = "gantelet_explosif";

    // Distance
    public static final String ARC_VENT = "arc_vent";

    // Lancers (projectiles à effet de zone)
    public static final String BOMBE_GEL = "bombe_gel";
    public static final String GRENADE_FUMIGENE = "grenade_fumigene";
    public static final String TOTEM_SOIN = "totem_soin";

    // Utilitaires actifs (clic droit)
    public static final String ANCRE_TELEPORTATION = "ancre_teleportation";
    public static final String BOUSSOLE_TRESOR = "boussole_tresor";
    public static final String GRAPPIN = "grappin";
    public static final String BATON_TELEPORTATION_ENNEMI = "baton_teleportation_ennemi";
    public static final String FRAGMENT_ETOILE = "fragment_etoile";
    public static final String SAC_RANGEMENT = "sac_rangement";

    // Passifs / portés
    public static final String AMULETTE_TEMPS = "amulette_temps";
    public static final String OEIL_WITHER = "oeil_wither";
    public static final String COURONNE_ROI = "couronne_roi";
    public static final String LANTERNE_AME = "lanterne_ame";
    public static final String ELYTRES_VENT = "elytres_vent";
}
