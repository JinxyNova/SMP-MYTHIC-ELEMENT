package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.boss.BossManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

public class BossDeathListener implements Listener {

    private final BossManager bossManager;

    public BossDeathListener(BossManager bossManager) {
        this.bossManager = bossManager;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        boolean isBoss = Boolean.TRUE.equals(event.getEntity().getPersistentDataContainer()
                .get(bossManager.getBossKey(), PersistentDataType.BOOLEAN));
        if (!isBoss) return;

        event.getDrops().clear();
        event.setDroppedExp(50);
        bossManager.handleDeath(event.getEntity(), event.getEntity().getKiller());
    }
}
