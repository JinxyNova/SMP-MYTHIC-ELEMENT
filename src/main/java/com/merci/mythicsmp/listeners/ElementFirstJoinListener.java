package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.elements.ElementManager;
import com.merci.mythicsmp.elements.ElementMenuManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * À la toute première connexion d'un joueur (il n'a encore aucun élément),
 * on lui ouvre le menu de choix de classe élémentaire. Ce choix est
 * définitif : une fois fait, ce listener n'a plus d'effet pour ce joueur.
 */
public class ElementFirstJoinListener implements Listener {

    private final Plugin plugin;
    private final ElementManager elementManager;
    private final ElementMenuManager menuManager;

    public ElementFirstJoinListener(Plugin plugin, ElementManager elementManager, ElementMenuManager menuManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
        this.menuManager = menuManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (elementManager.hasChosenStarter(player.getUniqueId())) return;

        player.sendMessage(Component.text(
                "Bienvenue sur MythicSMP ! Choisis ta classe élémentaire (Feu, Eau, Terre ou Vent).",
                NamedTextColor.GOLD));
        player.sendMessage(Component.text(
                "Attention : ce choix est définitif, tu ne pourras pas en changer ensuite.",
                NamedTextColor.GRAY));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && !elementManager.hasChosenStarter(player.getUniqueId())) {
                    menuManager.openStarterMenu(player);
                }
            }
        }.runTaskLater(plugin, 40L);
    }
}
