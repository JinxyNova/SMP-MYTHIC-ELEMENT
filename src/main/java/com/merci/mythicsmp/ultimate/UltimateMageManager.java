package com.merci.mythicsmp.ultimate;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Suit quels joueurs ont débloqué la classe Ultime Mage (persisté dans
 * ultimate_mage.yml, même principe que ElementManager/JobManager), ainsi
 * que les temps de recharge de ses 5 sorts (en mémoire seulement, comme
 * SpellManager — pas besoin de survivre à un redémarrage).
 *
 * Contrairement aux 4 éléments, il n'y a qu'une seule façon d'obtenir cette
 * classe (voir UltimateMageGemItem) et elle débloque directement les 5
 * sorts d'un coup : pas de choix, pas de progression par palier.
 */
public class UltimateMageManager {

    /** Seuil de niveau d'XP (barre d'XP) à atteindre, en plus de maîtriser
     * déjà les 4 éléments, pour qu'un kill du boss donne la Gemme du Mage
     * Ultime (voir BossManager). Volontairement plus haut que le seuil de
     * la Gemme au Pouvoir Infini (300) puisque c'est une récompense de fin
     * de progression. */
    public static final int GEM_XP_LEVEL_THRESHOLD = 445;

    private final Plugin plugin;
    private final File file;
    private final Set<UUID> unlocked = new HashSet<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public UltimateMageManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ultimate_mage.yml");
        load();
    }

    public boolean hasClass(UUID player) {
        return unlocked.contains(player);
    }

    /** @return true si la classe vient d'être débloquée (faux si déjà possédée). */
    public boolean unlock(UUID player) {
        if (unlocked.contains(player)) return false;
        unlocked.add(player);
        save();
        return true;
    }

    /** Secondes restantes avant de pouvoir relancer ce sort (0 si prêt). */
    public long getRemainingCooldownSeconds(UUID player, String spellId) {
        Long readyAt = cooldowns.getOrDefault(player, Map.of()).get(spellId);
        if (readyAt == null) return 0;
        long remainingMillis = readyAt - System.currentTimeMillis();
        return remainingMillis <= 0 ? 0 : (remainingMillis / 1000) + 1;
    }

    public void startCooldown(UUID player, String spellId, int seconds) {
        cooldowns.computeIfAbsent(player, k -> new HashMap<>())
                .put(spellId, System.currentTimeMillis() + seconds * 1000L);
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getStringList("unlocked")) {
            try {
                unlocked.add(UUID.fromString(key));
            } catch (IllegalArgumentException ignored) {
                // clé invalide, on l'ignore plutôt que de planter le chargement
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("unlocked", unlocked.stream().map(UUID::toString).toList());
        try {
            plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Impossible de sauvegarder ultimate_mage.yml", e);
        }
    }
}
