package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.quests.QuestManager;
import com.merci.mythicsmp.quests.QuestType;
import com.merci.mythicsmp.utils.ItemIdentifier;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

public class QuestProgressListener implements Listener {

    private final Plugin plugin;
    private final QuestManager questManager;

    public QuestProgressListener(Plugin plugin, QuestManager questManager) {
        this.plugin = plugin;
        this.questManager = questManager;
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!(event.getEntity() instanceof Monster)) return;

        questManager.addProgress(killer, QuestType.TUER_MOBS, 1, null);
    }

    @EventHandler
    public void onMine(BlockBreakEvent event) {
        questManager.addProgress(event.getPlayer(), QuestType.MINER_BLOC, 1, event.getBlock().getType());
    }

    @EventHandler
    public void onWeaponDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        String weaponId = ItemIdentifier.getId(plugin, player.getInventory().getItemInMainHand());
        if (weaponId == null) return;

        int damage = (int) Math.round(event.getFinalDamage());
        if (damage <= 0) return;
        questManager.addProgress(player, QuestType.DEGATS_ARME_MYTHIQUE, damage, null);
    }
}
