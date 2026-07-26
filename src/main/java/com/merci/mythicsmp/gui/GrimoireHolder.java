package com.merci.mythicsmp.gui;

import com.merci.mythicsmp.spells.Spell;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Reconnaît "cet inventaire est notre Grimoire" dans les listeners.
 *
 * Mode WHEEL : roue rapide d'accès aux 9 sorts favoris du joueur (clic sur
 * une case = lancer le sort).
 * Mode CONFIG : les 9 mêmes cases, mais un clic gauche ouvre le mode PICK
 * pour choisir quel sort débloqué y assigner (clic droit = vider la case).
 * Mode PICK : liste paginée des sorts débloqués du joueur, pour remplir
 * la case ciblée (targetSlot) du Grimoire.
 */
public class GrimoireHolder implements InventoryHolder {

    public enum Mode { WHEEL, CONFIG, PICK }

    private final Player player;
    private final Mode mode;
    private final int targetSlot; // utilisé seulement en mode PICK
    private final int page; // utilisé seulement en mode PICK

    private Inventory inventory;

    // WHEEL / CONFIG : slot GUI -> index de case (0..8)
    private final Map<Integer, Integer> slotToGrimoireIndex = new HashMap<>();
    // PICK : slot GUI -> sort proposé à cette case
    private final Map<Integer, Spell> slotSpells = new HashMap<>();

    private int navPrevSlot = -1;
    private int navNextSlot = -1;
    private int navBackSlot = -1;
    private int navClearSlot = -1;
    private int navConfigureSlot = -1;

    public static GrimoireHolder wheel(Player player) {
        return new GrimoireHolder(player, Mode.WHEEL, -1, 0);
    }

    public static GrimoireHolder config(Player player) {
        return new GrimoireHolder(player, Mode.CONFIG, -1, 0);
    }

    public static GrimoireHolder pick(Player player, int targetSlot, int page) {
        return new GrimoireHolder(player, Mode.PICK, targetSlot, page);
    }

    private GrimoireHolder(Player player, Mode mode, int targetSlot, int page) {
        this.player = player;
        this.mode = mode;
        this.targetSlot = targetSlot;
        this.page = page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public Mode getMode() {
        return mode;
    }

    public int getTargetSlot() {
        return targetSlot;
    }

    public int getPage() {
        return page;
    }

    public Map<Integer, Integer> getSlotToGrimoireIndex() {
        return slotToGrimoireIndex;
    }

    public Map<Integer, Spell> getSlotSpells() {
        return slotSpells;
    }

    public int getNavPrevSlot() {
        return navPrevSlot;
    }

    public void setNavPrevSlot(int navPrevSlot) {
        this.navPrevSlot = navPrevSlot;
    }

    public int getNavNextSlot() {
        return navNextSlot;
    }

    public void setNavNextSlot(int navNextSlot) {
        this.navNextSlot = navNextSlot;
    }

    public int getNavBackSlot() {
        return navBackSlot;
    }

    public void setNavBackSlot(int navBackSlot) {
        this.navBackSlot = navBackSlot;
    }

    public int getNavClearSlot() {
        return navClearSlot;
    }

    public void setNavClearSlot(int navClearSlot) {
        this.navClearSlot = navClearSlot;
    }

    public int getNavConfigureSlot() {
        return navConfigureSlot;
    }

    public void setNavConfigureSlot(int navConfigureSlot) {
        this.navConfigureSlot = navConfigureSlot;
    }
}
