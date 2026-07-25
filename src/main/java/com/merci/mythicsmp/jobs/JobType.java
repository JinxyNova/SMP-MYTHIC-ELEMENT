package com.merci.mythicsmp.jobs;

/**
 * Les métiers disponibles. Chaque métier gagne de l'XP via des actions
 * précises (voir JobListener) et monte de niveau selon la même courbe
 * (voir JobManager.xpForLevel).
 */
public enum JobType {

    MINEUR("Mineur"),
    BUCHERON("Bûcheron"),
    CHASSEUR("Chasseur"),
    PECHEUR("Pêcheur"),
    FERMIER("Fermier");

    private final String label;

    JobType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
