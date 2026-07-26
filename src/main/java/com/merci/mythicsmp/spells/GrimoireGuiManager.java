package com.merci.mythicsmp.spells;

import com.merci.mythicsmp.gui.GrimoireHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit les 3 vues du Grimoire (voir GrimoireHolder) : la roue rapide
 * (WHEEL), l'écran de configuration des 9 cases (CONFIG) et la liste des
 * sorts débloqués pour remplir une case (PICK). Toute la mise en page vit
 * ici, séparée du listener de clic (GrimoireListener).
 *
 * Les 9 cases sont disposées en losange dans une grille de 5 lignes pour
 * évoquer une roue plutôt qu'un simple carré, dans le même esprit visuel
 * que la roue des sorts (SpellWheelManager).
 */
public class GrimoireGuiManager {

    private static final int[] WHEEL_SLOTS = {11, 13, 15, 19, 22, 25, 29, 31, 33};
    private static final int HUB_SLOT = 4;
    private static final int BOTTOM_BUTTON_SLOT = 40;
    private static final int PICK_PAGE_SIZE = 45;

    private final Plugin plugin;
    private final GrimoireManager grimoireManager;
    private final SpellManager spellManager;

    public GrimoireGuiManager(Plugin plugin, GrimoireManager grimoireManager, SpellManager spellManager) {
        this.plugin = plugin;
        this.grimoireManager = grimoireManager;
        this.spellManager = spellManager;
    }

    // ------------------------------------------------------------- WHEEL

    public void openWheel(Player player) {
        GrimoireHolder holder = GrimoireHolder.wheel(player);
        Inventory inventory = Bukkit.createInventory(holder, 45,
                Component.text("✦ Grimoire — Accès Rapide ✦", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        holder.setInventory(inventory);

        fillBackground(inventory, Material.PURPLE_STAINED_GLASS_PANE);

        String[] slots = grimoireManager.getSlots(player.getUniqueId());
        for (int i = 0; i < WHEEL_SLOTS.length; i++) {
            int guiSlot = WHEEL_SLOTS[i];
            holder.getSlotToGrimoireIndex().put(guiSlot, i);
            inventory.setItem(guiSlot, spellSlotIcon(player, slots[i], i));
        }

        inventory.setItem(HUB_SLOT, hubIcon());
        inventory.setItem(BOTTOM_BUTTON_SLOT, configureButtonIcon());
        holder.setNavConfigureSlot(BOTTOM_BUTTON_SLOT);

        player.openInventory(inventory);
    }

    // ------------------------------------------------------------- CONFIG

    public void openConfig(Player player) {
        GrimoireHolder holder = GrimoireHolder.config(player);
        Inventory inventory = Bukkit.createInventory(holder, 45,
                Component.text("✦ Grimoire — Configuration ✦", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        holder.setInventory(inventory);

        fillBackground(inventory, Material.MAGENTA_STAINED_GLASS_PANE);

        String[] slots = grimoireManager.getSlots(player.getUniqueId());
        for (int i = 0; i < WHEEL_SLOTS.length; i++) {
            int guiSlot = WHEEL_SLOTS[i];
            holder.getSlotToGrimoireIndex().put(guiSlot, i);
            inventory.setItem(guiSlot, configSlotIcon(slots[i], i));
        }

        inventory.setItem(HUB_SLOT, configHubIcon());
        inventory.setItem(BOTTOM_BUTTON_SLOT, backToWheelIcon());
        holder.setNavBackSlot(BOTTOM_BUTTON_SLOT);

        player.openInventory(inventory);
    }

    // ------------------------------------------------------------- PICK

    public void openPick(Player player, int targetSlot, int page) {
        List<Spell> unlocked = unlockedSpells(player);
        int maxPage = Math.max(0, (unlocked.size() - 1) / PICK_PAGE_SIZE);
        int clampedPage = Math.max(0, Math.min(page, maxPage));

        GrimoireHolder holder = GrimoireHolder.pick(player, targetSlot, clampedPage);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                Component.text("✦ Grimoire — Choisis un sort (case " + (targetSlot + 1) + ") ✦",
                        NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        holder.setInventory(inventory);

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, pane(Material.GRAY_STAINED_GLASS_PANE));
        }

        int start = clampedPage * PICK_PAGE_SIZE;
        for (int i = 0; i < PICK_PAGE_SIZE && (start + i) < unlocked.size(); i++) {
            Spell spell = unlocked.get(start + i);
            inventory.setItem(i, pickSpellIcon(player, spell));
            holder.getSlotSpells().put(i, spell);
        }

        if (clampedPage > 0) {
            inventory.setItem(45, navIcon("◀ Page précédente"));
            holder.setNavPrevSlot(45);
        }
        if (clampedPage < maxPage) {
            inventory.setItem(53, navIcon("Page suivante ▶"));
            holder.setNavNextSlot(53);
        }
        inventory.setItem(49, clearSlotIcon());
        holder.setNavClearSlot(49);

        player.openInventory(inventory);
    }

    private List<Spell> unlockedSpells(Player player) {
        List<Spell> result = new ArrayList<>();
        for (Spell spell : spellManager.getRegistry().all()) {
            if (spellManager.isUnlocked(player.getUniqueId(), spell.id())) {
                result.add(spell);
            }
        }
        return result;
    }

    // ------------------------------------------------------------- Décor

    private void fillBackground(Inventory inventory, Material accent) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, pane(accent));
            }
        }
    }

    private ItemStack pane(Material material) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(" "));
        stack.setItemMeta(meta);
        return stack;
    }

    private void glow(ItemMeta meta) {
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    private ItemStack hubIcon() {
        ItemStack stack = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("✦ Grimoire ✦", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Clique un sort pour le lancer.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Bouton en bas : configurer tes 9 cases.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack configHubIcon() {
        ItemStack stack = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("Configuration du Grimoire", NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Clic gauche sur une case : choisir un sort.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Clic droit sur une case : la vider.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack configureButtonIcon() {
        ItemStack stack = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("▶ Configurer le Grimoire", NamedTextColor.YELLOW, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Choisis les 9 sorts favoris.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack backToWheelIcon() {
        ItemStack stack = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("◀ Retour à la roue rapide", NamedTextColor.AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack navIcon(String text) {
        ItemStack stack = new ItemStack(Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(text, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack clearSlotIcon() {
        ItemStack stack = new ItemStack(Material.BARRIER);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("Vider cette case", NamedTextColor.RED, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack spellSlotIcon(Player player, String spellId, int index) {
        Spell spell = spellId != null ? spellManager.getRegistry().get(spellId) : null;
        if (spell == null) {
            ItemStack stack = new ItemStack(Material.GRAY_DYE);
            ItemMeta meta = stack.getItemMeta();
            meta.displayName(Component.text("Case " + (index + 1) + " — vide", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Configure ton Grimoire (sneak +", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("clic droit) pour y assigner un sort.", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)));
            stack.setItemMeta(meta);
            return stack;
        }

        ItemStack stack = new ItemStack(spell.element().getIcon());
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("✦ " + spell.name(), spell.element().getColor(), TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(spell.tier().getLabel() + " • " + spell.element().getLabel(), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(spell.description(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));

        long remaining = spellManager.getRemainingCooldownSeconds(player.getUniqueId(), spell.id());
        lore.add(Component.text(" "));
        lore.add(Component.text(remaining > 0 ? "⏳ En recharge : " + remaining + "s" : "▶ Clic : lancer le sort",
                remaining > 0 ? NamedTextColor.RED : NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));

        if (spell.tier() == SpellTier.ULTRA_FORT) glow(meta);
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack configSlotIcon(String spellId, int index) {
        Spell spell = spellId != null ? spellManager.getRegistry().get(spellId) : null;
        ItemStack stack = new ItemStack(spell != null ? spell.element().getIcon() : Material.BOOK);
        ItemMeta meta = stack.getItemMeta();

        if (spell != null) {
            meta.displayName(Component.text("Case " + (index + 1) + " : " + spell.name(), spell.element().getColor(), TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text(spell.tier().getLabel() + " • " + spell.element().getLabel(), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text(" "),
                    Component.text("Clic gauche : changer le sort", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Clic droit : vider la case", NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false)));
        } else {
            meta.displayName(Component.text("Case " + (index + 1) + " — vide", NamedTextColor.DARK_GRAY, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Clic gauche : assigner un sort", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false)));
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack pickSpellIcon(Player player, Spell spell) {
        ItemStack stack = new ItemStack(spell.element().getIcon());
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(spell.name(), spell.element().getColor(), TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(spell.tier().getLabel() + " • " + spell.element().getLabel(), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(spell.description(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(" "));
        if (grimoireManager.contains(player.getUniqueId(), spell.id())) {
            lore.add(Component.text("Déjà présent dans une autre case.", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("▶ Clic : assigner à cette case", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));

        if (spell.tier() == SpellTier.ULTRA_FORT) glow(meta);
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }
}
