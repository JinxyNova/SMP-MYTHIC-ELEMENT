package com.merci.mythicsmp.scoreboard;

import com.merci.mythicsmp.economy.EconomyManager;
import com.merci.mythicsmp.elements.ElementManager;
import com.merci.mythicsmp.jobs.JobManager;
import com.merci.mythicsmp.jobs.JobType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Panneau latéral affiché en permanence à chaque joueur : heure en jeu,
 * IP du serveur, solde, métier et niveau. Se met à jour chaque seconde.
 * Chaque joueur reçoit son propre objet Scoreboard (créé au premier passage)
 * pour ne jamais toucher au scoreboard "principal" du serveur.
 */
public class ScoreboardManager extends BukkitRunnable {

    private static final String OBJECTIVE_NAME = "mythicsmp_side";
    private static final String SERVER_IP = "mythicsmp.hosterfy.eu";

    private final Plugin plugin;
    private final EconomyManager economyManager;
    private final JobManager jobManager;
    private final ElementManager elementManager;

    public ScoreboardManager(Plugin plugin, EconomyManager economyManager, JobManager jobManager, ElementManager elementManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.jobManager = jobManager;
        this.elementManager = elementManager;
    }

    public void start() {
        runTaskTimer(plugin, 0L, 20L);
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            update(player);
        }
    }

    private void update(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board == null || board == plugin.getServer().getScoreboardManager().getMainScoreboard()) {
            board = plugin.getServer().getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }

        Objective objective = board.getObjective(OBJECTIVE_NAME);
        if (objective == null) {
            objective = board.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY,
                    Component.text("MythicSMP", NamedTextColor.GOLD, TextDecoration.BOLD));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        for (String entry : new HashSet<>(board.getEntries())) {
            board.resetScores(entry);
        }

        List<String> lines = buildLines(player);
        int score = lines.size();
        for (String line : lines) {
            objective.getScore(line).setScore(score--);
        }
    }

    private List<String> buildLines(Player player) {
        List<String> lines = new ArrayList<>();
        String clock = formatWorldTime(player.getWorld().getTime());
        double balance = economyManager.getBalance(player.getUniqueId());
        JobType job = jobManager.getJob(player.getUniqueId());

        lines.add("§7§m――――――――――――――");
        lines.add("§fIP: §b" + SERVER_IP);
        lines.add("§fHeure: §e" + clock);
        lines.add("§fArgent: §a" + String.format("%.0f", balance) + " pièces");
        if (job != null) {
            int level = jobManager.getLevel(player.getUniqueId());
            lines.add("§fMétier: §d" + job.getLabel() + " §7(Niv. " + level + ")");
        } else {
            lines.add("§fMétier: §7Aucun (/mythicjob)");
        }
        int elementCount = elementManager.getElements(player.getUniqueId()).size();
        if (elementCount > 0) {
            lines.add("§fÉléments: §d" + elementCount + "/" + ElementManager.MAX_ELEMENTS
                    + " §7(" + elementManager.getRankLabel(player.getUniqueId()) + ")");
        }
        lines.add("§7§m――――――――――――――§r ");
        return dedupe(lines);
    }

    /** Les entrées de scoreboard doivent être uniques : si deux lignes sont
     * identiques (les deux séparateurs), on en distingue une avec un espace invisible. */
    private List<String> dedupe(List<String> lines) {
        Set<String> seen = new HashSet<>();
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            String candidate = line;
            while (!seen.add(candidate)) {
                candidate = candidate + "§r";
            }
            result.add(candidate);
        }
        return result;
    }

    /** Le temps Minecraft démarre à 6h (tick 0) ; on convertit en horloge lisible. */
    private String formatWorldTime(long time) {
        long adjusted = (time + 6000) % 24000;
        long hours = adjusted / 1000;
        long minutes = (adjusted % 1000) * 60 / 1000;
        return String.format("%02d:%02d", hours, minutes);
    }
}
