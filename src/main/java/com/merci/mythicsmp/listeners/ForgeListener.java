package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.forge.ForgeManager;
import com.merci.mythicsmp.gui.ForgeHolder;
import com.merci.mythicsmp.quests.QuestManager;
import com.merci.mythicsmp.quests.QuestType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Le clic droit sur une table de forge ouvre notre menu à la place du menu
 * vanilla. Tout le reste (validation de recette, coûts) est délégué à
 * ForgeManager — ce listener ne fait que le pont entre les events Bukkit
 * et cette classe.
 */
public class ForgeListener implements Listener {

    private final ForgeManager forgeManager;
    private final QuestManager questManager;

    public ForgeListener(ForgeManager forgeManager, QuestManager questManager) {
        this.forgeManager = forgeManager;
        this.questManager = questManager;
    }

    @EventHandler
    public void onInteractSmithingTable(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != org.bukkit.Material.SMITHING_TABLE) return;

        event.setCancelled(true);
        forgeManager.open(event.getPlayer());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ForgeHolder)) return;

        int slot = event.getRawSlot();
        boolean withinForgeMenu = slot >= 0 && slot < event.getInventory().getSize();

        if (withinForgeMenu && slot == ForgeHolder.CONFIRM_SLOT) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                String message = forgeManager.tryUpgrade(event.getInventory(), player);
                player.sendMessage(message);
                if (message.startsWith("§a")) {
                    questManager.addProgress(player, QuestType.AMELIORER_A_LA_FORGE, 1, null);
                }
            }
            return;
        }

        if (withinForgeMenu && slot != ForgeHolder.INPUT_SLOT
                && slot != ForgeHolder.MATERIAL_SLOT_1 && slot != ForgeHolder.MATERIAL_SLOT_2) {
            // Toutes les autres cases du menu (le fond en verre) sont fixes.
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof ForgeHolder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        for (int slot : new int[]{ForgeHolder.INPUT_SLOT, ForgeHolder.MATERIAL_SLOT_1, ForgeHolder.MATERIAL_SLOT_2}) {
            ItemStack stack = event.getInventory().getItem(slot);
            if (stack != null) {
                var leftover = player.getInventory().addItem(stack);
                leftover.values().forEach(item -> player.getWorld().dropItem(player.getLocation(), item));
            }
        }
    }
}
