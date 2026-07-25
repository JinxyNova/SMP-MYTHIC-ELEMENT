package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.jobs.JobManager;
import com.merci.mythicsmp.jobs.JobType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MythicJobCommand implements CommandExecutor {

    private final JobManager jobManager;

    public MythicJobCommand(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            JobType job = jobManager.getJob(player.getUniqueId());
            if (job == null) {
                player.sendMessage(Component.text(
                        "Tu n'as pas encore de métier. /mythicjob choose <mineur|bucheron|chasseur|pecheur|fermier>",
                        NamedTextColor.YELLOW));
                return true;
            }
            int level = jobManager.getLevel(player.getUniqueId());
            int xp = jobManager.getXp(player.getUniqueId());
            int needed = JobManager.xpForLevel(level);
            player.sendMessage(Component.text(
                    "Métier : " + job.getLabel() + " | Niveau " + level + " | " + xp + " XP (prochain niveau : " + needed + " XP)",
                    NamedTextColor.GOLD));
            return true;
        }

        if (args[0].equalsIgnoreCase("choose")) {
            if (args.length < 2) {
                player.sendMessage(Component.text(
                        "Utilisation : /mythicjob choose <mineur|bucheron|chasseur|pecheur|fermier>", NamedTextColor.RED));
                return true;
            }
            JobType job;
            try {
                job = JobType.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text(
                        "Métier inconnu. Choix possibles : mineur, bucheron, chasseur, pecheur, fermier", NamedTextColor.RED));
                return true;
            }
            jobManager.setJob(player.getUniqueId(), job);
            player.sendMessage(Component.text("Tu es maintenant " + job.getLabel() + " !", NamedTextColor.GREEN));
            return true;
        }

        player.sendMessage(Component.text("Utilisation : /mythicjob info | /mythicjob choose <métier>", NamedTextColor.RED));
        return true;
    }
}
