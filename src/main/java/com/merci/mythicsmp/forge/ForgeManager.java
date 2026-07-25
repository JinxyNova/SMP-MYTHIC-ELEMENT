package com.merci.mythicsmp.forge;

import com.merci.mythicsmp.gui.ForgeHolder;
import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.utils.ItemIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class ForgeManager {

    private final Plugin plugin;
    private final ForgeRecipeRegistry recipes;

    public ForgeManager(Plugin plugin, ForgeRecipeRegistry recipes) {
        this.plugin = plugin;
        this.recipes = recipes;
    }

    /** Ouvre le menu de la forge pour un joueur. */
    public void open(Player player) {
        ForgeHolder holder = new ForgeHolder(player);
        Inventory inventory = plugin.getServer().createInventory(holder, 27,
                Component.text("Forge Mythique", NamedTextColor.GOLD));
        holder.setInventory(inventory);

        ItemStack pane = namedPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < 27; slot++) {
            inventory.setItem(slot, pane);
        }
        inventory.setItem(ForgeHolder.INPUT_SLOT, null);
        inventory.setItem(ForgeHolder.MATERIAL_SLOT_1, null);
        inventory.setItem(ForgeHolder.MATERIAL_SLOT_2, null);
        inventory.setItem(ForgeHolder.CONFIRM_SLOT, namedPane(Material.ANVIL, "Valider l'amélioration"));

        player.openInventory(inventory);
    }

    private ItemStack namedPane(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW));
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Tente d'appliquer une recette à partir du contenu actuel du menu.
     * Renvoie un message à afficher au joueur (succès ou raison de l'échec).
     */
    public String tryUpgrade(Inventory forgeInventory, Player player) {
        ItemStack input = forgeInventory.getItem(ForgeHolder.INPUT_SLOT);
        String inputId = ItemIdentifier.getId(plugin, input);
        if (inputId == null) {
            return "§cPose un objet mythique dans la case d'entrée.";
        }

        ForgeRecipe recipe = recipes.findFor(inputId);
        if (recipe == null) {
            return "§cCet objet ne peut pas être amélioré à la forge.";
        }

        if (isAlreadyUpgraded(input)) {
            return "§cCet objet est déjà amélioré au maximum.";
        }

        Map<Material, Integer> provided = new HashMap<>();
        addCount(provided, forgeInventory.getItem(ForgeHolder.MATERIAL_SLOT_1));
        addCount(provided, forgeInventory.getItem(ForgeHolder.MATERIAL_SLOT_2));

        for (Map.Entry<Material, Integer> required : recipe.materials().entrySet()) {
            int have = provided.getOrDefault(required.getKey(), 0);
            if (have < required.getValue()) {
                return "§cIl manque : " + required.getValue() + "x " + required.getKey().name() + ".";
            }
        }

        // Consommation exacte des matériaux (les deux slots peuvent contenir le même matériau)
        consumeMaterials(forgeInventory, recipe.materials());
        forgeInventory.setItem(ForgeHolder.INPUT_SLOT, null);

        if (recipe.isUpgradeInPlace()) {
            ItemStack upgraded = markUpgraded(input);
            player.getInventory().addItem(upgraded);
            return "§aObjet amélioré avec succès !";
        } else {
            MythicItem result = recipe.resultItem();
            player.getInventory().addItem(result.build());
            return "§aTon objet a évolué en " + result.getId() + " !";
        }
    }

    private boolean isAlreadyUpgraded(ItemStack stack) {
        if (stack.getItemMeta() == null) return false;
        Boolean value = stack.getItemMeta().getPersistentDataContainer()
                .get(MythicItem.upgradedKey(plugin), PersistentDataType.BOOLEAN);
        return Boolean.TRUE.equals(value);
    }

    private ItemStack markUpgraded(ItemStack original) {
        ItemStack copy = original.clone();
        copy.setAmount(1);
        ItemMeta meta = copy.getItemMeta();
        meta.getPersistentDataContainer().set(MythicItem.upgradedKey(plugin), PersistentDataType.BOOLEAN, true);
        meta.lore(appendLoreLine(meta, "✦ Amélioré par la Forge"));
        copy.setItemMeta(meta);
        return copy;
    }

    private java.util.List<Component> appendLoreLine(ItemMeta meta, String line) {
        java.util.List<Component> lore = new java.util.ArrayList<>(meta.lore() == null ? java.util.List.of() : meta.lore());
        lore.add(Component.text(line, NamedTextColor.AQUA));
        return lore;
    }

    private void addCount(Map<Material, Integer> map, ItemStack stack) {
        if (stack == null) return;
        map.merge(stack.getType(), stack.getAmount(), Integer::sum);
    }

    private void consumeMaterials(Inventory forgeInventory, Map<Material, Integer> required) {
        Map<Material, Integer> remaining = new HashMap<>(required);
        for (int slot : new int[]{ForgeHolder.MATERIAL_SLOT_1, ForgeHolder.MATERIAL_SLOT_2}) {
            ItemStack stack = forgeInventory.getItem(slot);
            if (stack == null) continue;
            Integer need = remaining.get(stack.getType());
            if (need == null || need <= 0) continue;
            int take = Math.min(need, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) forgeInventory.setItem(slot, null);
            remaining.put(stack.getType(), need - take);
        }
    }
}
