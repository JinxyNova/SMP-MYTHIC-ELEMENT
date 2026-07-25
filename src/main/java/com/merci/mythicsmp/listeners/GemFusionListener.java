package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.items.mythic.ElementalGemItem;
import com.merci.mythicsmp.items.weapons.FireSwordItem;
import com.merci.mythicsmp.items.weapons.IceSwordItem;
import com.merci.mythicsmp.utils.ItemIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Fusion : tenir la gemme en main principale, l'épée correspondante en
 * main secondaire (offhand), puis clic droit + sneak. La gemme est
 * consommée et l'épée devient "upgraded" (voir WeaponEffectListener).
 */
public class GemFusionListener implements Listener {

    private final Plugin plugin;

    public GemFusionListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getPlayer().isSneaking()) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        String gemId = ItemIdentifier.getId(plugin, mainHand);
        String swordId = ItemIdentifier.getId(plugin, offHand);
        if (gemId == null || swordId == null) return;

        boolean matches = (ElementalGemItem.ID_FEU.equals(gemId) && FireSwordItem.ID.equals(swordId))
                || (ElementalGemItem.ID_GLACE.equals(gemId) && IceSwordItem.ID.equals(swordId));
        if (!matches) return;

        ItemMeta meta = offHand.getItemMeta();
        Boolean already = meta.getPersistentDataContainer()
                .get(MythicItem.upgradedKey(plugin), PersistentDataType.BOOLEAN);
        if (Boolean.TRUE.equals(already)) {
            player.sendMessage(Component.text("Cette épée est déjà améliorée.", NamedTextColor.RED));
            return;
        }

        meta.getPersistentDataContainer().set(MythicItem.upgradedKey(plugin), PersistentDataType.BOOLEAN, true);
        meta.lore(java.util.stream.Stream.concat(
                meta.lore() == null ? java.util.stream.Stream.of() : meta.lore().stream(),
                java.util.stream.Stream.of(Component.text("✦ Améliorée par une gemme", NamedTextColor.AQUA))
        ).toList());
        offHand.setItemMeta(meta);

        mainHand.setAmount(mainHand.getAmount() - 1);

        player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, player.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.02);
        player.sendMessage(Component.text("Ton épée a été améliorée !", NamedTextColor.GREEN));
        event.setCancelled(true);
    }
}
