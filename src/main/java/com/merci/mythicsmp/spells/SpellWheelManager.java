package com.merci.mythicsmp.spells;

import com.merci.mythicsmp.elements.Element;
import com.merci.mythicsmp.elements.ElementManager;
import com.merci.mythicsmp.gui.SpellWheelHolder;
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
 * Construit les deux variantes de la roue des sorts (voir SpellWheelHolder)
 * et les ouvre pour un joueur. Toute la mise en page/décoration vit ici,
 * séparée du listener de clic (SpellWheelListener).
 *
 * Design : un dégradé de couleur par palier (blanc → jaune → orange →
 * rouge, du plus faible au plus fort, un peu comme les paliers de rareté
 * qu'on voit sur des serveurs comme Paladium) et un cadre teinté par
 * l'élément affiché, pour que le menu reste lisible d'un coup d'œil tout
 * en ayant un peu de personnalité.
 */
public class SpellWheelManager {

    // Colonnes utilisées pour aligner les 4 sorts d'un palier dans une ligne de 9 cases.
    private static final int[] TIER_COLUMNS = {1, 3, 5, 7};
    private static final int[] TAB_SLOTS = {1, 3, 5, 7};

    // Un fond par ligne : dégradé du palier le plus faible (blanc) au plus fort (rouge).
    private static final Material[] ROW_GLASS = {
            Material.PURPLE_STAINED_GLASS_PANE,  // en-tête
            Material.WHITE_STAINED_GLASS_PANE,   // Faible
            Material.YELLOW_STAINED_GLASS_PANE,  // Moyen
            Material.ORANGE_STAINED_GLASS_PANE,  // Fort
            Material.RED_STAINED_GLASS_PANE,     // Ultra-Fort
            Material.PURPLE_STAINED_GLASS_PANE,  // pied de page
    };

    private final Plugin plugin;
    private final ElementManager elementManager;
    private final SpellManager spellManager;

    public SpellWheelManager(Plugin plugin, ElementManager elementManager, SpellManager spellManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
        this.spellManager = spellManager;
    }

    public void openView(Player player, Element displayedElement) {
        SpellWheelHolder holder = SpellWheelHolder.view(player, displayedElement);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                Component.text("✦ Roue des Sorts — " + displayedElement.getLabel() + " ✦",
                        NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        holder.setInventory(inventory);

        fillGradientBackground(inventory, displayedElement);

        List<Element> owned = elementManager.getElements(player.getUniqueId());
        for (int i = 0; i < owned.size() && i < TAB_SLOTS.length; i++) {
            Element element = owned.get(i);
            inventory.setItem(TAB_SLOTS[i], elementTabIcon(element, element == displayedElement));
            holder.getSlotElementTabs().put(TAB_SLOTS[i], element);
        }

        SpellTier[] tiers = SpellTier.values();
        for (int row = 0; row < tiers.length; row++) {
            SpellTier tier = tiers[row];
            int rowStart = 9 * (row + 1);
            List<Spell> spells = spellManager.getRegistry().forElementAndTier(displayedElement, tier);
            for (int i = 0; i < spells.size() && i < TIER_COLUMNS.length; i++) {
                int slot = rowStart + TIER_COLUMNS[i];
                Spell spell = spells.get(i);
                inventory.setItem(slot, spellIcon(player, spell));
                holder.getSlotSpells().put(slot, spell);
            }
        }

        inventory.setItem(4, hubIcon(displayedElement));
        inventory.setItem(49, profileIcon(player));
        player.openInventory(inventory);
    }

    public void openUnlock(Player player, Element element, SpellTier tier) {
        SpellWheelHolder holder = SpellWheelHolder.unlock(player, element, tier);
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text("✦ Débloquer — " + element.getLabel() + " " + tier.getLabel() + " ✦",
                        NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        holder.setInventory(inventory);

        Material tierGlass = tierGlass(tier);
        Material accent = elementGlass(element);
        for (int i = 0; i < 27; i++) {
            boolean corner = i == 0 || i == 8 || i == 18 || i == 26;
            inventory.setItem(i, pane(corner ? accent : tierGlass));
        }

        List<Spell> spells = spellManager.getRegistry().forElementAndTier(element, tier);
        int[] slots = {10, 12, 14, 16};
        for (int i = 0; i < spells.size() && i < slots.length; i++) {
            Spell spell = spells.get(i);
            inventory.setItem(slots[i], spellIcon(player, spell));
            holder.getSlotSpells().put(slots[i], spell);
        }

        player.openInventory(inventory);
    }

    private void fillGradientBackground(Inventory inventory, Element displayedElement) {
        Material accent = elementGlass(displayedElement);
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = row * 9 + col;
                boolean edgeRow = row == 0 || row == 5;
                boolean corner = edgeRow && (col == 0 || col == 8);
                inventory.setItem(slot, pane(corner ? accent : ROW_GLASS[row]));
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

    private Material elementGlass(Element element) {
        return switch (element) {
            case FEU -> Material.RED_STAINED_GLASS_PANE;
            case EAU -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case TERRE -> Material.GREEN_STAINED_GLASS_PANE;
            case VENT -> Material.WHITE_STAINED_GLASS_PANE;
        };
    }

    private Material tierGlass(SpellTier tier) {
        return switch (tier) {
            case FAIBLE -> Material.WHITE_STAINED_GLASS_PANE;
            case MOYEN -> Material.YELLOW_STAINED_GLASS_PANE;
            case FORT -> Material.ORANGE_STAINED_GLASS_PANE;
            case ULTRA_FORT -> Material.RED_STAINED_GLASS_PANE;
        };
    }

    private void glow(ItemMeta meta) {
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    private ItemStack hubIcon(Element displayedElement) {
        ItemStack stack = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("✦ Roue des Sorts ✦", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Élément affiché : " + displayedElement.getLabel(), displayedElement.getColor())
                        .decoration(TextDecoration.ITALIC, false),
                Component.text(" ", NamedTextColor.GRAY),
                Component.text("Blanc → Jaune → Orange → Rouge", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("= Faible → Moyen → Fort → Ultra-Fort", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        glow(meta);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack elementTabIcon(Element element, boolean selected) {
        ItemStack stack = new ItemStack(element.getIcon());
        ItemMeta meta = stack.getItemMeta();
        Component name = Component.text(element.getLabel(), element.getColor(), TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(selected ? name.decorate(TextDecoration.UNDERLINED) : name);
        meta.lore(List.of(Component.text(selected ? "▶ Onglet actif" : "Clic : afficher cet élément", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        if (selected) glow(meta);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack spellIcon(Player player, Spell spell) {
        boolean unlocked = spellManager.isUnlocked(player.getUniqueId(), spell.id());
        ItemStack stack = new ItemStack(unlocked ? spell.element().getIcon() : Material.GRAY_DYE);
        ItemMeta meta = stack.getItemMeta();

        NamedTextColor tierColor = tierColor(spell.tier());
        meta.displayName(Component.text((unlocked ? "✦ " : "🔒 ") + spell.name(), unlocked ? tierColor : NamedTextColor.DARK_GRAY, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(spell.tier().getLabel() + " • " + spell.element().getLabel(), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(spell.description(), NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));

        if (unlocked) {
            SpellProgress progress = spellManager.getProgress(player.getUniqueId(), spell.id());
            lore.add(Component.text(" "));
            lore.add(Component.text("Maîtrise : niveau " + progress.getLevel() + "/" + SpellProgress.MAX_LEVEL,
                    NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
            int nextLevel = progress.getUsesForNextLevel();
            if (nextLevel >= 0) {
                lore.add(Component.text("Prochain niveau : " + progress.getUses() + "/" + nextLevel + " utilisations",
                        NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }
            long remaining = spellManager.getRemainingCooldownSeconds(player.getUniqueId(), spell.id());
            lore.add(Component.text(remaining > 0 ? "⏳ En recharge : " + remaining + "s" : "▶ Clic : lancer le sort",
                    remaining > 0 ? NamedTextColor.RED : NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            if (spell.tier() == SpellTier.ULTRA_FORT) glow(meta);
        } else {
            lore.add(Component.text(" "));
            lore.add(Component.text("Verrouillé — débloque-le avec une", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Rune " + spell.element().getLabel() + " " + spell.tier().getLabel(), NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack profileIcon(Player player) {
        ItemStack stack = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("Ton profil élémentaire", NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Element> owned = elementManager.getElements(player.getUniqueId());
        String elementsList = owned.isEmpty() ? "Aucun" :
                owned.stream().map(Element::getLabel).reduce((a, b) -> a + ", " + b).orElse("Aucun");
        String rank = elementManager.getRankLabel(player.getUniqueId());
        meta.lore(List.of(
                Component.text("Éléments : " + elementsList, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false),
                Component.text("Grade : " + (rank != null ? rank : "Aucun"), NamedTextColor.LIGHT_PURPLE)
                        .decoration(TextDecoration.ITALIC, false)));
        stack.setItemMeta(meta);
        return stack;
    }

    private NamedTextColor tierColor(SpellTier tier) {
        return switch (tier) {
            case FAIBLE -> NamedTextColor.GRAY;
            case MOYEN -> NamedTextColor.YELLOW;
            case FORT -> NamedTextColor.GOLD;
            case ULTRA_FORT -> NamedTextColor.RED;
        };
    }
}
