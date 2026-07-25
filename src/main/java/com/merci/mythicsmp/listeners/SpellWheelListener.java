package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.elements.Element;
import com.merci.mythicsmp.gui.SpellWheelHolder;
import com.merci.mythicsmp.items.mythic.SpellRuneItem;
import com.merci.mythicsmp.spells.Spell;
import com.merci.mythicsmp.spells.SpellManager;
import com.merci.mythicsmp.spells.SpellWheelManager;
import com.merci.mythicsmp.utils.ItemIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

/**
 * Gère les clics dans la roue des sorts, dans ses deux modes (voir
 * SpellWheelHolder) : changer d'onglet élément / lancer un sort débloqué
 * (mode VIEW), ou débloquer un sort en consommant une Rune (mode UNLOCK).
 */
public class SpellWheelListener implements Listener {

    private final Plugin plugin;
    private final SpellManager spellManager;
    private final SpellWheelManager wheelManager;

    public SpellWheelListener(Plugin plugin, SpellManager spellManager, SpellWheelManager wheelManager) {
        this.plugin = plugin;
        this.spellManager = spellManager;
        this.wheelManager = wheelManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SpellWheelHolder holder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        if (holder.getMode() == SpellWheelHolder.Mode.VIEW) {
            Element tab = holder.getSlotElementTabs().get(slot);
            if (tab != null) {
                wheelManager.openView(player, tab);
                return;
            }
            Spell spell = holder.getSlotSpells().get(slot);
            if (spell != null) castFromWheel(player, spell);
        } else {
            Spell spell = holder.getSlotSpells().get(slot);
            if (spell != null) unlockFromRune(player, spell);
        }
    }

    private void castFromWheel(Player player, Spell spell) {
        if (!spellManager.isUnlocked(player.getUniqueId(), spell.id())) {
            player.sendMessage(Component.text("Ce sort n'est pas encore débloqué.", NamedTextColor.RED));
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

    private void unlockFromRune(Player player, Spell spell) {
        if (spellManager.isUnlocked(player.getUniqueId(), spell.id())) {
            player.sendMessage(Component.text("Tu maîtrises déjà ce sort.", NamedTextColor.YELLOW));
            return;
        }
        if (!removeOneRune(player, spell)) {
            player.sendMessage(Component.text("Il te manque la rune adaptée.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }
        spellManager.unlock(player.getUniqueId(), spell.id());
        player.sendMessage(Component.text("Tu as débloqué le sort " + spell.name() + " !", spell.element().getColor()));
        player.closeInventory();
    }

    private boolean removeOneRune(Player player, Spell spell) {
        String runeId = SpellRuneItem.idFor(spell.element(), spell.tier());
        PlayerInventory inventory = player.getInventory();
        for (ItemStack stack : inventory.getContents()) {
            if (ItemIdentifier.hasId(plugin, stack, runeId)) {
                stack.setAmount(stack.getAmount() - 1);
                return true;
            }
        }
        return false;
    }
}
