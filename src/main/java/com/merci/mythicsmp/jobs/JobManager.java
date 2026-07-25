package com.merci.mythicsmp.jobs;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Suit le métier choisi par chaque joueur et son XP. Même principe que
 * EconomyManager (yml simple, pas de base de données), mais on ne sauvegarde
 * sur disque qu'au level up ou à l'arrêt du serveur plutôt qu'à chaque gain
 * d'XP : un joueur qui mine gagne de l'XP à chaque bloc, sauvegarder à
 * chaque fois ferait beaucoup trop d'IO.
 */
public class JobManager {

    private final Plugin plugin;
    private final File file;
    private final Map<UUID, JobType> jobs = new HashMap<>();
    private final Map<UUID, Integer> xp = new HashMap<>();

    public JobManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "jobs.yml");
        load();
    }

    public JobType getJob(UUID player) {
        return jobs.get(player);
    }

    public void setJob(UUID player, JobType job) {
        jobs.put(player, job);
        xp.putIfAbsent(player, 0);
        save();
    }

    public int getXp(UUID player) {
        return xp.getOrDefault(player, 0);
    }

    public int getLevel(UUID player) {
        return levelForXp(getXp(player));
    }

    /** Courbe simple : chaque niveau demande un peu plus d'XP que le précédent. */
    public static int xpForLevel(int level) {
        return 100 + (level - 1) * 50;
    }

    public static int levelForXp(int totalXp) {
        int level = 1;
        int remaining = totalXp;
        int needed = xpForLevel(level);
        while (remaining >= needed) {
            remaining -= needed;
            level++;
            needed = xpForLevel(level);
        }
        return level;
    }

    /** @return true si le joueur vient de monter de niveau grâce à ce gain */
    public boolean addXp(UUID player, int amount) {
        if (!jobs.containsKey(player)) return false;
        int before = getLevel(player);
        xp.merge(player, amount, Integer::sum);
        boolean leveledUp = getLevel(player) > before;
        if (leveledUp) save();
        return leveledUp;
    }

    /** Classement par XP total, du plus expérimenté au moins expérimenté. */
    public List<Map.Entry<UUID, Integer>> top(int limit) {
        return xp.entrySet().stream()
                .filter(entry -> jobs.containsKey(entry.getKey()))
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(limit)
                .toList();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String jobName = config.getString(key + ".job");
                int playerXp = config.getInt(key + ".xp", 0);
                if (jobName != null) {
                    jobs.put(uuid, JobType.valueOf(jobName));
                    xp.put(uuid, playerXp);
                }
            } catch (IllegalArgumentException ignored) {
                // clé ou métier invalide, on l'ignore plutôt que de planter le chargement
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (UUID uuid : jobs.keySet()) {
            config.set(uuid + ".job", jobs.get(uuid).name());
            config.set(uuid + ".xp", xp.getOrDefault(uuid, 0));
        }
        try {
            plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Impossible de sauvegarder jobs.yml", e);
        }
    }
}
