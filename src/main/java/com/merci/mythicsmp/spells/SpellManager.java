package com.merci.mythicsmp.spells;

import com.merci.mythicsmp.elements.Element;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Suit, pour chaque joueur, quels sorts sont débloqués et leur niveau de
 * maîtrise (persisté dans spells.yml, même principe que ElementManager),
 * ainsi que les temps de recharge en cours (en mémoire seulement — pas
 * besoin de survivre à un redémarrage).
 */
public class SpellManager {

    private final Plugin plugin;
    private final SpellRegistry registry;
    private final File file;

    private final Map<UUID, Map<String, SpellProgress>> unlocked = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public SpellManager(Plugin plugin, SpellRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.file = new File(plugin.getDataFolder(), "spells.yml");
        load();
    }

    public SpellRegistry getRegistry() {
        return registry;
    }

    public boolean isUnlocked(UUID player, String spellId) {
        return unlocked.getOrDefault(player, Map.of()).containsKey(spellId);
    }

    public SpellProgress getProgress(UUID player, String spellId) {
        return unlocked.getOrDefault(player, Map.of()).get(spellId);
    }

    /** @return true si le sort vient d'être débloqué (faux s'il l'était déjà). */
    public boolean unlock(UUID player, String spellId) {
        Map<String, SpellProgress> playerSpells = unlocked.computeIfAbsent(player, k -> new LinkedHashMap<>());
        if (playerSpells.containsKey(spellId)) return false;
        playerSpells.put(spellId, new SpellProgress());
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

    /**
     * Tente de lancer le sort pour ce joueur : vérifie qu'il est débloqué et
     * disponible (recharge écoulée), applique son effet, incrémente son
     * usage et démarre la recharge.
     *
     * @return CastResult indiquant le résultat (voir enum).
     */
    public CastResult cast(Player player, String spellId) {
        Spell spell = registry.get(spellId);
        if (spell == null) return CastResult.UNKNOWN;

        UUID uuid = player.getUniqueId();
        SpellProgress progress = getProgress(uuid, spellId);
        if (progress == null) return CastResult.NOT_UNLOCKED;

        long remaining = getRemainingCooldownSeconds(uuid, spellId);
        if (remaining > 0) return CastResult.ON_COOLDOWN;

        double power = spell.tier().getPowerScale() * progress.getPowerMultiplier();
        spell.effect().cast(plugin, player, power);

        boolean leveledUp = progress.registerUse();
        save();

        long cooldownMillis = (long) (spell.tier().getBaseCooldownSeconds() * 1000 * progress.getCooldownMultiplier());
        cooldowns.computeIfAbsent(uuid, k -> new HashMap<>())
                .put(spellId, System.currentTimeMillis() + cooldownMillis);

        return leveledUp ? CastResult.SUCCESS_LEVEL_UP : CastResult.SUCCESS;
    }

    public enum CastResult {
        SUCCESS, SUCCESS_LEVEL_UP, NOT_UNLOCKED, ON_COOLDOWN, UNKNOWN
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                var section = config.getConfigurationSection(key + ".spells");
                if (section == null) continue;
                Map<String, SpellProgress> playerSpells = new LinkedHashMap<>();
                for (String spellId : section.getKeys(false)) {
                    int uses = section.getInt(spellId + ".uses", 0);
                    playerSpells.put(spellId, new SpellProgress(uses));
                }
                if (!playerSpells.isEmpty()) unlocked.put(uuid, playerSpells);
            } catch (IllegalArgumentException ignored) {
                // clé invalide, on l'ignore plutôt que de planter le chargement
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (var playerEntry : unlocked.entrySet()) {
            for (var spellEntry : playerEntry.getValue().entrySet()) {
                config.set(playerEntry.getKey() + ".spells." + spellEntry.getKey() + ".uses",
                        spellEntry.getValue().getUses());
            }
        }
        try {
            plugin.getDataFolder().mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Impossible de sauvegarder spells.yml", e);
        }
    }
}
