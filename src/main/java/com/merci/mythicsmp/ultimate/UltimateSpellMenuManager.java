package com.merci.mythicsmp.ultimate;

import com.merci.mythicsmp.gui.UltimateSpellMenuHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Construit et ouvre le menu (27 cases) listant les 5 sorts de la classe
 * Ultime Mage. Séparé du listener de clic (UltimateSpellMenuListener) pour
 * rester facile à ajuster, même principe que ElementMenuManager/SpellWheelManager.
 */
public class UltimateSpellMenuManager {

    private static final int[] SPELL_SLOTS = {11, 13, 15, 21, 23};

    private final Plugin plugin;
    private final UltimateMageManager ultimateMageManager;

    public UltimateSpellMenuManager(Plugin plugin, UltimateMageManager ultimateMageManager) {
        this.plugin = plugin;
        this.ultimateMageManager = ultimateMageManager;
    }

    public void open(Player player) {
        UltimateSpellMenuHolder holder = new UltimateSpellMenuHolder(player);
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text("✦ Sorts du Mage Ultime ✦", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        holder.setInventory(inventory);

        ItemStack filler = filler();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        UltimateSpell[] spells = UltimateSpell.values();
        for (int i = 0; i < spells.length && i < SPELL_SLOTS.length; i++) {
            inventory.setItem(SPELL_SLOTS[i], spellIcon(player, spells[i]));
            holder.getSlotSpells().put(SPELL_SLOTS[i], spells[i]);
        }

        player.openInventory(inventory);
    }

    private ItemStack spellIcon(Player player, UltimateSpell spell) {
        ItemStack stack = new ItemStack(spell.getIcon());
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("✦ " + spell.getDisplayName(), UltimateSpell.color(), TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        long remaining = ultimateMageManager.getRemainingCooldownSeconds(player.getUniqueId(), spell.getId());
        meta.lore(List.of(
                Component.text(spell.getDescription(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text(remaining > 0 ? "⏳ En recharge : " + remaining + "s" : "▶ Clic : lancer le sort",
                        remaining > 0 ? NamedTextColor.RED : NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
        ));
        meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack filler() {
        ItemStack stack = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(" "));
        stack.setItemMeta(meta);
        return stack;
    }
}
