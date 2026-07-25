package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.jobs.JobManager;
import com.merci.mythicsmp.jobs.JobType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MythicJobTopCommand implements CommandExecutor {

    private final JobManager jobManager;

    public MythicJobTopCommand(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<Map.Entry<UUID, Integer>> top = jobManager.top(10);
        if (top.isEmpty()) {
            sender.sendMessage(Component.text("Personne n'a encore choisi de métier.", NamedTextColor.RED));
            return true;
        }
        sender.sendMessage(Component.text("=== Classement des métiers ===", NamedTextColor.GOLD));
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : top) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = "???";
            JobType job = jobManager.getJob(entry.getKey());
            int level = JobManager.levelForXp(entry.getValue());
            sender.sendMessage(Component.text(
                    "#" + rank + " " + name + " - " + (job != null ? job.getLabel() : "?") + " (Niv. " + level + ")",
                    NamedTextColor.YELLOW));
            rank++;
        }
        return true;
    }
}
