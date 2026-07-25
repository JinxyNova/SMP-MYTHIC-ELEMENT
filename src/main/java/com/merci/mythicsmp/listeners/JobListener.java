package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.jobs.JobManager;
import com.merci.mythicsmp.jobs.JobType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.Set;

/**
 * Distribue l'XP de métier selon ce que fait le joueur : miner (Mineur),
 * couper du bois (Bûcheron), tuer des monstres (Chasseur), pêcher (Pêcheur)
 * ou récolter des cultures mûres (Fermier). Un joueur ne gagne de l'XP que
 * pour le métier qu'il a choisi via /mythicjob choose.
 */
public class JobListener implements Listener {

    private static final Set<Material> ORES = Set.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.NETHER_QUARTZ_ORE, Material.ANCIENT_DEBRIS
    );

    private static final Set<Material> LOGS = Set.of(
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG,
            Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
            Material.MANGROVE_LOG, Material.CHERRY_LOG, Material.CRIMSON_STEM, Material.WARPED_STEM
    );

    private static final Set<Material> CROPS = Set.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS
    );

    private final JobManager jobManager;

    public JobListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material type = event.getBlock().getType();

        if (ORES.contains(type)) {
            grantXp(player, JobType.MINEUR, 8);
        } else if (LOGS.contains(type)) {
            grantXp(player, JobType.BUCHERON, 5);
        } else if (CROPS.contains(type) && isMatureCrop(event.getBlock())) {
            grantXp(player, JobType.FERMIER, 4);
        }
    }

    private boolean isMatureCrop(Block block) {
        return !(block.getBlockData() instanceof Ageable ageable) || ageable.getAge() >= ageable.getMaximumAge();
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (event.getEntity() instanceof Monster) {
            grantXp(killer, JobType.CHASSEUR, 10);
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        grantXp(player, JobType.PECHEUR, 12);
    }

    private void grantXp(Player player, JobType job, int amount) {
        if (jobManager.getJob(player.getUniqueId()) != job) return;
        boolean leveledUp = jobManager.addXp(player.getUniqueId(), amount);
        if (leveledUp) {
            int level = jobManager.getLevel(player.getUniqueId());
            player.sendMessage(Component.text(
                    "Ton métier de " + job.getLabel() + " passe niveau " + level + " !", NamedTextColor.GOLD));
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    player.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.05);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }
}
