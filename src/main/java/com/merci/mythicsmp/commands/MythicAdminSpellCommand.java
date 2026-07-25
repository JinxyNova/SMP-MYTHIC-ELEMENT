package com.merci.mythicsmp.commands;

import com.merci.mythicsmp.ultimate.UltimateVisuals;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Sort réservé aux administrateurs (permission mythicsmp.adminspell, OP par
 * défaut) : contrairement aux autres sorts, il n'existe ni objet ni classe
 * pour l'obtenir — la commande elle-même EST le sort, on ne peut donc y
 * accéder que via cette commande admin.
 *
 * "Destruction Absolue" tue instantanément (setHealth(0), pas de simple
 * dégât qui pourrait être absorbé par une résistance ou une Totem of
 * Undying) toutes les entités vivantes dans un large rayon autour du
 * lanceur, lui excepté.
 *
 * Mise en scène en 3 actes, à la hauteur du sort le plus destructeur du
 * plugin : une charge sombre et oppressante (colonne d'âmes, grondement du
 * Wither qui enfle), une onde de choc qui balaie tout le rayon, puis un
 * faisceau vertical qui s'abat sur chaque cible condamnée avant
 * l'implosion finale.
 */
public class MythicAdminSpellCommand implements CommandExecutor {

    private static final double RADIUS = 40.0;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin("MythicSMP");
        if (plugin == null) {
            sender.sendMessage("Erreur interne : plugin introuvable.");
            return true;
        }

        // On fige les cibles dès l'instant du cast (pas au moment de l'implosion,
        // plus tard) pour ne pas punir quelqu'un qui vient d'entrer dans le rayon
        // pendant la charge du sort.
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity nearby : player.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (nearby instanceof LivingEntity target && !target.equals(player)) {
                targets.add(target);
            }
        }

        castDestructionAbsolue(plugin, player, targets);
        return true;
    }

    private void castDestructionAbsolue(Plugin plugin, Player player, List<LivingEntity> targets) {
        Location origin = player.getLocation();
        Particle.DustOptions voidColor = UltimateVisuals.dust(Color.fromRGB(20, 0, 25), 1.6f);
        Particle.DustOptions bloodColor = UltimateVisuals.dust(Color.fromRGB(120, 0, 10), 1.3f);

        // Acte 1 : charge oppressante — colonne d'âmes qui monte et tourne pendant 30
        // ticks (1.5s), grondement du Wither qui enfle en volume et en aigu.
        player.getWorld().playSound(origin, Sound.ENTITY_WITHER_AMBIENT, 1f, 0.4f);
        player.getWorld().playSound(origin, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.3f);

        new BukkitRunnable() {
            int tick = 0;
            final int duration = 30;

            @Override
            public void run() {
                if (tick >= duration) {
                    cancel();
                    unleash(plugin, player, origin, targets, voidColor, bloodColor);
                    return;
                }
                Location base = player.getLocation();
                double progress = tick / (double) duration;
                double height = progress * 3.0;
                double radius = 1.4 * (1.0 - 0.4 * progress);
                double angle = tick * 0.9;
                for (int i = 0; i < 6; i++) {
                    double a = angle + (2 * Math.PI / 6) * i;
                    double x = base.getX() + radius * Math.cos(a);
                    double z = base.getZ() + radius * Math.sin(a);
                    base.getWorld().spawnParticle(Particle.SOUL, x, base.getY() + height, z, 1, 0, 0, 0, 0);
                    base.getWorld().spawnParticle(Particle.DUST, x, base.getY() + height, z, 1, 0, 0, 0, 0, voidColor);
                }
                if (tick % 4 == 0) {
                    base.getWorld().playSound(base, Sound.ENTITY_WITHER_HURT, 0.4f, 0.3f + (float) progress);
                }
                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void unleash(Plugin plugin, Player player, Location origin, List<LivingEntity> targets,
                          Particle.DustOptions voidColor, Particle.DustOptions bloodColor) {
        // Acte 2 : rugissement + onde de choc sombre qui balaie tout le rayon du sort,
        // visuellement, avant que les cibles ne tombent.
        player.getWorld().playSound(origin, Sound.ENTITY_ENDER_DRAGON_GROWL, 2f, 0.4f);
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, origin, 6, 1.5, 1, 1.5, 0);
        UltimateVisuals.shockwaveDust(plugin, origin, voidColor, RADIUS, 6, 2);

        // Acte 3, à retardement (12 ticks, le temps que l'onde de choc "arrive") : un
        // faisceau vertical de particules s'abat sur chaque cible condamnée juste avant
        // qu'elle ne tombe, façon jugement divin inversé.
        new BukkitRunnable() {
            @Override
            public void run() {
                int killed = 0;
                for (LivingEntity target : targets) {
                    if (target.isDead()) continue;
                    Location loc = target.getLocation();
                    for (double y = 0; y < 6; y += 0.4) {
                        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, y, 0), 1, 0, 0, 0, 0);
                    }
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 20, 0.4, 0.8, 0.4, 0, bloodColor);
                    loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_HURT, 1f, 0.6f);
                    target.setHealth(0.0);
                    killed++;
                }

                // Acte final : implosion silencieuse et brutale sur le lanceur, la marque
                // visuelle du pouvoir qu'il vient d'exercer.
                player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, origin, 14, RADIUS / 5, 2, RADIUS / 5, 0);
                player.getWorld().spawnParticle(Particle.SOUL, origin, 60, 3, 2, 3, 0.05);
                player.getWorld().playSound(origin, Sound.ENTITY_WITHER_DEATH, 2f, 0.5f);
                player.sendMessage(Component.text(
                        "Destruction Absolue : " + killed + " entité(s) anéantie(s).", NamedTextColor.DARK_RED));
            }
        }.runTaskLater(plugin, 12L);
    }
}
