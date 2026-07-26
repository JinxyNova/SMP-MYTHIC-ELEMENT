package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.gui.GrimoireHolder;
import com.merci.mythicsmp.items.Ids;
import com.merci.mythicsmp.spells.GrimoireGuiManager;
import com.merci.mythicsmp.spells.GrimoireManager;
import com.merci.mythicsmp.spells.Spell;
import com.merci.mythicsmp.spells.SpellManager;
import com.merci.mythicsmp.utils.ItemIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Gère l'ouverture du Grimoire (clic droit du livre : roue rapide ; clic
 * droit + sneak : configuration) et tous les clics dans ses 3 vues (voir
 * GrimoireHolder / GrimoireGuiManager).
 */
public class GrimoireListener implements Listener {

    private final Plugin plugin;
    private final GrimoireManager grimoireManager;
    private final SpellManager spellManager;
    private final GrimoireGuiManager guiManager;

    public GrimoireListener(Plugin plugin, GrimoireManager grimoireManager, SpellManager spellManager, GrimoireGuiManager guiManager) {
        this.plugin = plugin;
        this.grimoireManager = grimoireManager;
        this.spellManager = spellManager;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!ItemIdentifier.hasId(plugin, hand, Ids.GRIMOIRE)) return;

        event.setCancelled(true);
        if (player.isSneaking()) {
            guiManager.openConfig(player);
        } else {
            guiManager.openWheel(player);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GrimoireHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        switch (holder.getMode()) {
            case WHEEL -> handleWheelClick(player, holder, slot);
            case CONFIG -> handleConfigClick(player, holder, slot, event.getClick());
            case PICK -> handlePickClick(player, holder, slot);
        }
    }

    private void handleWheelClick(Player player, GrimoireHolder holder, int slot) {
        if (slot == holder.getNavConfigureSlot()) {
            guiManager.openConfig(player);
            return;
        }
        Integer index = holder.getSlotToGrimoireIndex().get(slot);
        if (index == null) return;

        String spellId = grimoireManager.getSlot(player.getUniqueId(), index);
        if (spellId == null) {
            player.sendMessage(Component.text("Case vide — configure ton Grimoire (sneak + clic droit).", NamedTextColor.YELLOW));
            return;
        }

        Spell spell = spellManager.getRegistry().get(spellId);
        if (spell == null) return;
        castFromGrimoire(player, spell);
    }

    private void handleConfigClick(Player player, GrimoireHolder holder, int slot, ClickType click) {
        if (slot == holder.getNavBackSlot()) {
            guiManager.openWheel(player);
            return;
        }
        Integer index = holder.getSlotToGrimoireIndex().get(slot);
        if (index == null) return;

        if (click.isRightClick()) {
            grimoireManager.clearSlot(player.getUniqueId(), index);
            guiManager.openConfig(player);
            return;
        }
        guiManager.openPick(player, index, 0);
    }

    private void handlePickClick(Player player, GrimoireHolder holder, int slot) {
        if (slot == holder.getNavPrevSlot()) {
            guiManager.openPick(player, holder.getTargetSlot(), holder.getPage() - 1);
            return;
        }
        if (slot == holder.getNavNextSlot()) {
            guiManager.openPick(player, holder.getTargetSlot(), holder.getPage() + 1);
            return;
        }
        if (slot == holder.getNavClearSlot()) {
            grimoireManager.clearSlot(player.getUniqueId(), holder.getTargetSlot());
            guiManager.openConfig(player);
            return;
        }

        Spell spell = holder.getSlotSpells().get(slot);
        if (spell == null) return;

        grimoireManager.setSlot(player.getUniqueId(), holder.getTargetSlot(), spell.id());
        player.sendMessage(Component.text(
                spell.name() + " assigné à la case " + (holder.getTargetSlot() + 1) + " du Grimoire.",
                spell.element().getColor()));
        guiManager.openConfig(player);
    }

    private void castFromGrimoire(Player player, Spell spell) {
        if (!spellManager.isUnlocked(player.getUniqueId(), spell.id())) {
            player.sendMessage(Component.text("Ce sort n'est plus débloqué — reconfigure ton Grimoire.", NamedTextColor.RED));
            return;
        }
        SpellManager.CastResult result = spellManager.cast(player, spell.id());
        switch (result) {
            case ON_COOLDOWN -> player.sendMessage(Component.text(
                    "Ce sort est encore en recharge (" + spellManager.getRemainingCooldownSeconds(player.getUniqueId(), spell.id()) + "s).",
                    NamedTextColor.RED));
            case SUCCESS -> player.sendMessage(Component.text(spell.name() + " !", spell.element().getColor()));
            case SUCCESS_LEVEL_UP -> {
                player.sendMessage(Component.text(spell.name() + " !", spell.element().getColor()));
                player.sendMessage(Component.text(
                        "Ton sort " + spell.name() + " passe niveau " + spellManager.getProgress(player.getUniqueId(), spell.id()).getLevel() + " !",
                        NamedTextColor.LIGHT_PURPLE));
            }
            default -> {
            }
        }
        player.closeInventory();
    }
}
