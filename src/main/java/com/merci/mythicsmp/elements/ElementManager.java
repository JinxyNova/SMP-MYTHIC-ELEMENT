package com.merci.mythicsmp.elements;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Suit les éléments débloqués par chaque joueur, dans l'ordre où ils ont
 * été choisis (le premier élément, choisi à la première connexion, ne peut
 * jamais être changé ni retiré). Même principe que EconomyManager/JobManager
 * (yml simple, sauvegarde à chaque changement — les changements sont rares
 * ici, donc pas de souci d'IO).
 */
public class ElementManager {

    public static final int MAX_ELEMENTS = Element.values().length;

    /** Seuil de niveau d'XP (barre d'XP) à atteindre pour qu'un kill du
     * boss donne une Gemme au Pouvoir Infini. */
    public static final int GEM_XP_LEVEL_THRESHOLD = 300;

    private static final String[] RANK_LABELS = {
            "Disciple des Éléments",
            "Sage des Éléments",
            "Seigneur des Éléments",
            "MYTHIC ELEMENTS"
    };

    private final Plugin plugin;
    private final File file;
    private final Map<UUID, LinkedHashSet<Element>> elements = new LinkedHashMap<>();

    public ElementManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "elements.yml");
        load();
    }

    /** Faux tant que le joueur n'a jamais choisi son premier élément. */
    public boolean hasChosenStarter(UUID player) {
        return !getElements(player).isEmpty();
    }

    public List<Element> getElements(UUID player) {
        return new ArrayList<>(elements.getOrDefault(player, new LinkedHashSet<>()));
    }

    public boolean hasElement(UUID player, Element element) {
        return elements.getOrDefault(player, new LinkedHashSet<>()).contains(element);
    }

    public boolean isMaxed(UUID player) {
        return getElements(player).size() >= MAX_ELEMENTS;
    }

    /** @return true si le choix a bien été enregistré (faux si un élément était déjà choisi). */
    public boolean chooseStarter(UUID player, Element element) {
        if (hasChosenStarter(player)) return false;
        elements.computeIfAbsent(player, k -> new LinkedHashSet<>()).add(element);
        save();
        return true;
    }

    /** @return true si l'élément a bien été débloqué (faux si déjà possédé, pas encore de
     * classe de départ, ou déjà au maximum des 4 éléments). */
    public boolean unlockElement(UUID player, Element element) {
        if (!hasChosenStarter(player)) return false;
        if (isMaxed(player)) return false;
        LinkedHashSet<Element> owned = elements.computeIfAbsent(player, k -> new LinkedHashSet<>());
        if (owned.contains(element)) return false;
        owned.add(element);
        save();
        return true;
    }

    /**
     * Admin : force le déblocage d'un élément précis, en ignorant les règles
     * normales (pas besoin d'avoir déjà choisi de classe de départ, pas de
     * limite au nombre maximum d'éléments). Utilisé par
     * /mythicelement admin unlock|max.
     *
     * @return true si l'élément vient d'être ajouté (faux s'il était déjà possédé).
     */
    public boolean adminUnlock(UUID player, Element element) {
        LinkedHashSet<Element> owned = elements.computeIfAbsent(player, k -> new LinkedHashSet<>());
        boolean added = owned.add(element);
        if (added) save();
        return added;
    }

    /**
     * Admin : débloque directement les 4 éléments pour ce joueur (équivalent
     * du "niveau max" côté éléments, puisqu'il n'y a pas de palier par
     * élément — juste le nombre d'éléments possédés).
     *
     * @return le nombre d'éléments nouvellement ajoutés.
     */
    public int adminUnlockAll(UUID player) {
        LinkedHashSet<Element> owned = elements.computeIfAbsent(player, k -> new LinkedHashSet<>());
        int added = 0;
        for (Element element : Element.values()) {
            if (owned.add(element)) added++;
        }
        if (added > 0) save();
        return added;
    }

    /** Titre/grade correspondant au nombre d'éléments maîtrisés (null si aucun élément). */
    public String getRankLabel(UUID player) {
        int count = getElements(player).size();
        if (count == 0) return null;
        return RANK_LABELS[Math.min(count, RANK_LABELS.length) - 1];
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                List<String> names = config.getStringList(key + ".elements");
                LinkedHashSet<Element> owned = new LinkedHashSet<>();
                for (String name : names) {
                    try {
                        owned.add(Element.valueOf(name));
                    } catch (IllegalArgumentException ignored) {
                        // élément inconnu dans le fichier, on l'ignore plutôt que de planter le chargement
                    }
                }
                if (!owned.isEmpty()) {
                    elements.put(uuid, owned);
                }
            } catch (IllegalArgumentException ignored) {
                // clé invalide, on l'ignore plutôt que de planter le chargement
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, LinkedHashSet<Element>> entry : elements.entrySet()) {
            List<String> names = entry.getValue().stream().map(Enum::name).toList();
            config.set(entry.getKey() + ".elements", names);
        }
        try {
            plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Impossible de sauvegarder elements.yml", e);
        }
    }
}
