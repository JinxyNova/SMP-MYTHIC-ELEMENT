package com.merci.mythicsmp.spells;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Stocke, pour chaque joueur, les 9 sorts assignés à son Grimoire (voir
 * GrimoireGuiManager) : un raccourci vers 9 sorts déjà débloqués, au choix
 * du joueur, pour y accéder plus vite qu'en fouillant la roue des sorts
 * complète. Persisté dans grimoire.yml, même principe que spells.yml
 * (voir SpellManager).
 */
public class GrimoireManager {

    public static final int SLOT_COUNT = 9;

    private final Plugin plugin;
    private final File file;
    private final Map<UUID, String[]> loadouts = new HashMap<>();

    public GrimoireManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "grimoire.yml");
        load();
    }

    /** Les 9 cases du joueur (null = case vide). Tableau interne, à ne pas modifier directement. */
    public String[] getSlots(UUID player) {
        return loadouts.computeIfAbsent(player, k -> new String[SLOT_COUNT]);
    }

    public String getSlot(UUID player, int index) {
        if (index < 0 || index >= SLOT_COUNT) return null;
        return getSlots(player)[index];
    }

    public void setSlot(UUID player, int index, String spellId) {
        if (index < 0 || index >= SLOT_COUNT) return;
        getSlots(player)[index] = spellId;
        save();
    }

    public void clearSlot(UUID player, int index) {
        setSlot(player, index, null);
    }

    /** true si ce sort occupe déjà une des 9 cases du joueur. */
    public boolean contains(UUID player, String spellId) {
        for (String s : getSlots(player)) {
            if (spellId.equals(s)) return true;
        }
        return false;
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                List<String> stored = config.getStringList(key + ".slots");
                String[] slots = new String[SLOT_COUNT];
                for (int i = 0; i < SLOT_COUNT && i < stored.size(); i++) {
                    String value = stored.get(i);
                    slots[i] = (value == null || value.isEmpty() || value.equals("-")) ? null : value;
                }
                loadouts.put(uuid, slots);
            } catch (IllegalArgumentException ignored) {
                // clé invalide, on l'ignore plutôt que de planter le chargement
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, String[]> entry : loadouts.entrySet()) {
            List<String> values = new ArrayList<>();
            for (String s : entry.getValue()) {
                values.add(s == null ? "-" : s);
            }
            config.set(entry.getKey().toString() + ".slots", values);
        }
        try {
            plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Impossible de sauvegarder grimoire.yml", e);
        }
    }
}
