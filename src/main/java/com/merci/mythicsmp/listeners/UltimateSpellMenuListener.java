package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.gui.UltimateSpellMenuHolder;
import com.merci.mythicsmp.ultimate.UltimateMageManager;
import com.merci.mythicsmp.ultimate.UltimateSpell;
import com.merci.mythicsmp.ultimate.UltimateSpellMenuManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;

/**
 * Gère les clics dans le menu des sorts du Mage Ultime (voir
 * UltimateSpellMenuManager) : vérifie la classe, le cooldown, puis lance
 * l'effet et rafraîchit le menu.
 */
public class UltimateSpellMenuListener implements Listener {

    private final Plugin plugin;
    private final UltimateMageManager ultimateMageManager;
    private final UltimateSpellMenuManager menuManager;

    public UltimateSpellMenuListener(Plugin plugin, UltimateMageManager ultimateMageManager, UltimateSpellMenuManager menuManager) {
        this.plugin = plugin;
        this.ultimateMageManager = ultimateMageManager;
        this.menuManager = menuManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof UltimateSpellMenuHolder holder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        UltimateSpell spell = holder.getSlotSpells().get(event.getRawSlot());
        if (spell == null) return;

        if (!ultimateMageManager.hasClass(player.getUniqueId())) {
            player.sendMessage(Component.text("Tu n'es pas Mage Ultime.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        long remaining = ultimateMageManager.getRemainingCooldownSeconds(player.getUniqueId(), spell.getId());
        if (remaining > 0) {
            player.sendMessage(Component.text("Ce sort est encore en recharge : " + remaining + "s.", NamedTextColor.RED));
            return;
        }

        spell.cast(plugin, player);
        ultimateMageManager.startCooldown(player.getUniqueId(), spell.getId(), spell.getCooldownSeconds());
        player.sendMessage(Component.text("Tu lances " + spell.getDisplayName() + " !", NamedTextColor.LIGHT_PURPLE));

        menuManager.open(player);
    }
}
