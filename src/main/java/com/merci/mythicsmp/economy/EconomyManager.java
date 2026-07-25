package com.merci.mythicsmp.economy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Monnaie interne simple et indépendante de tout plugin d'économie externe
 * (pas de dépendance Vault). Les soldes sont chargés une fois au démarrage
 * et réécrits sur disque après chaque transaction — un peu plus d'IO que
 * l'idéal, mais ça garantit qu'on ne perd jamais d'argent sur un crash,
 * ce qui compte plus qu'un gain de performance ici.
 */
public class EconomyManager {

    private static final double DEFAULT_BALANCE = 100.0;

    private final Plugin plugin;
    private final File file;
    private final Map<UUID, Double> balances = new HashMap<>();

    public EconomyManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "balances.yml");
        load();
    }

    public double getBalance(UUID player) {
        return balances.getOrDefault(player, DEFAULT_BALANCE);
    }

    public boolean withdraw(UUID player, double amount) {
        double current = getBalance(player);
        if (current < amount) return false;
        balances.put(player, current - amount);
        save();
        return true;
    }

    public void deposit(UUID player, double amount) {
        balances.put(player, getBalance(player) + amount);
        save();
    }

    /** Classement des plus grosses fortunes, du plus riche au moins riche. */
    public java.util.List<Map.Entry<UUID, Double>> topBalances(int limit) {
        return balances.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .toList();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                balances.put(UUID.fromString(key), config.getDouble(key));
            } catch (IllegalArgumentException ignored) {
                // clé invalide dans le fichier, on l'ignore plutôt que de planter le chargement
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Impossible de sauvegarder balances.yml", e);
        }
    }
}
