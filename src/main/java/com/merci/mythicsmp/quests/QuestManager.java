package com.merci.mythicsmp.quests;

import com.merci.mythicsmp.economy.EconomyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Les 3 quêtes du jour sont tirées avec une graine dérivée de la date
 * (LocalDate.now().toEpochDay()) : tout le monde a les mêmes ce jour-là,
 * sans avoir besoin de les stocker explicitement tant que la date n'a pas
 * changé. On ne les fige dans le fichier qu'au moment où on les calcule,
 * pour pouvoir comparer "la date sauvegardée" à "aujourd'hui" et savoir
 * quand réinitialiser la progression de tout le monde.
 */
public class QuestManager {

    private final Plugin plugin;
    private final EconomyManager economyManager;
    private final File file;

    private LocalDate currentDay;
    private List<QuestDefinition> todaysQuests;

    // playerId -> (questId -> progression)
    private final Map<UUID, Map<String, Integer>> progress = new HashMap<>();
    // playerId -> ids de quêtes déjà réclamées aujourd'hui
    private final Map<UUID, java.util.Set<String>> completed = new HashMap<>();

    public QuestManager(Plugin plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.file = new File(plugin.getDataFolder(), "quests.yml");
        load();
        rotateIfNewDay();
    }

    public List<QuestDefinition> getTodaysQuests() {
        rotateIfNewDay();
        return todaysQuests;
    }

    public int getProgress(UUID player, String questId) {
        return progress.getOrDefault(player, Map.of()).getOrDefault(questId, 0);
    }

    public boolean isCompleted(UUID player, String questId) {
        return completed.getOrDefault(player, java.util.Set.of()).contains(questId);
    }

    /** Point d'entrée appelé par QuestProgressListener (et les commandes marché/forge) à chaque action pertinente. */
    public void addProgress(Player player, QuestType type, int amount, Material relatedBlock) {
        rotateIfNewDay();
        UUID uuid = player.getUniqueId();

        for (QuestDefinition quest : todaysQuests) {
            if (quest.type() != type) continue;
            if (type == QuestType.MINER_BLOC && !quest.matchesMinedBlock(relatedBlock)) continue;
            if (isCompleted(uuid, quest.id())) continue;

            int newProgress = progress.computeIfAbsent(uuid, k -> new HashMap<>())
                    .merge(quest.id(), amount, Integer::sum);

            if (newProgress >= quest.targetAmount()) {
                completeQuest(player, quest);
            }
        }
        save();
    }

    private void completeQuest(Player player, QuestDefinition quest) {
        completed.computeIfAbsent(player.getUniqueId(), k -> new java.util.HashSet<>()).add(quest.id());
        economyManager.deposit(player.getUniqueId(), quest.rewardCoins());
        player.sendMessage(Component.text("Quête terminée : " + quest.description()
                + " (+" + (int) quest.rewardCoins() + " pièces)", NamedTextColor.GOLD));
    }

    private void rotateIfNewDay() {
        LocalDate today = LocalDate.now();
        if (todaysQuests != null && today.equals(currentDay)) return;

        currentDay = today;
        Random seeded = new Random(today.toEpochDay());
        List<QuestDefinition> shuffled = new ArrayList<>(QuestPool.ALL);
        java.util.Collections.shuffle(shuffled, seeded);
        this.todaysQuests = shuffled.subList(0, Math.min(3, shuffled.size()));

        progress.clear();
        completed.clear();
        save();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String savedDay = config.getString("day");
        if (savedDay != null) {
            try {
                currentDay = LocalDate.parse(savedDay);
            } catch (Exception ignored) {
                currentDay = null;
            }
        }

        if (currentDay != null && currentDay.equals(LocalDate.now())) {
            Random seeded = new Random(currentDay.toEpochDay());
            List<QuestDefinition> shuffled = new ArrayList<>(QuestPool.ALL);
            java.util.Collections.shuffle(shuffled, seeded);
            this.todaysQuests = shuffled.subList(0, Math.min(3, shuffled.size()));

            if (config.isConfigurationSection("players")) {
                for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
                    UUID uuid = UUID.fromString(uuidStr);
                    String base = "players." + uuidStr + ".";
                    Map<String, Integer> playerProgress = new HashMap<>();
                    if (config.isConfigurationSection(base + "progress")) {
                        for (String questId : config.getConfigurationSection(base + "progress").getKeys(false)) {
                            playerProgress.put(questId, config.getInt(base + "progress." + questId));
                        }
                    }
                    progress.put(uuid, playerProgress);
                    completed.put(uuid, new java.util.HashSet<>(config.getStringList(base + "completed")));
                }
            }
        }
        // Si la date sauvegardée n'est pas aujourd'hui, rotateIfNewDay() (appelé juste après dans le
        // constructeur) régénérera todaysQuests et repartira sur une progression vide.
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("day", currentDay != null ? currentDay.toString() : LocalDate.now().toString());

        for (Map.Entry<UUID, Map<String, Integer>> entry : progress.entrySet()) {
            String base = "players." + entry.getKey() + ".progress.";
            for (Map.Entry<String, Integer> questProgress : entry.getValue().entrySet()) {
                config.set(base + questProgress.getKey(), questProgress.getValue());
            }
        }
        for (Map.Entry<UUID, java.util.Set<String>> entry : completed.entrySet()) {
            config.set("players." + entry.getKey() + ".completed", new ArrayList<>(entry.getValue()));
        }

        try {
            plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Impossible de sauvegarder quests.yml", e);
        }
    }
}
