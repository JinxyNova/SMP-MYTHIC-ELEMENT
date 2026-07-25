package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.boss.BossManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BossSpawnCommand implements CommandExecutor {

    private final BossManager bossManager;

    public BossSpawnCommand(BossManager bossManager) {
        this.bossManager = bossManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande doit être utilisée en jeu.");
            return true;
        }
        sender.sendMessage(bossManager.spawn(player.getLocation()));
        return true;
    }
}
