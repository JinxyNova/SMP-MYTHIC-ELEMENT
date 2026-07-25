package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.quests.QuestDefinition;
import com.merci.mythicsmp.quests.QuestManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuestListCommand implements CommandExecutor {

    private final QuestManager questManager;

    public QuestListCommand(QuestManager questManager) {
        this.questManager = questManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande doit être utilisée en jeu.");
            return true;
        }

        player.sendMessage(Component.text("=== Quêtes du jour ===", NamedTextColor.GOLD));
        for (QuestDefinition quest : questManager.getTodaysQuests()) {
            boolean done = questManager.isCompleted(player.getUniqueId(), quest.id());
            int current = Math.min(questManager.getProgress(player.getUniqueId(), quest.id()), quest.targetAmount());

            NamedTextColor color = done ? NamedTextColor.GREEN : NamedTextColor.GRAY;
            String status = done ? " ✔" : " (" + current + "/" + quest.targetAmount() + ")";
            player.sendMessage(Component.text("- " + quest.description() + status
                    + "  [+" + (int) quest.rewardCoins() + " pièces]", color));
        }
        return true;
    }
}
