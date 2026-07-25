package com.merci.mythicsmp.listeners;

import com.merci.mythicsmp.items.Ids;
import com.merci.mythicsmp.items.ItemRarity;
import com.merci.mythicsmp.items.ItemRegistry;
import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.utils.ItemIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Regroupe les objets à usage "clic droit -> effet ponctuel" qui n'ont pas
 * besoin d'un listener dédié à eux seuls. Le Sac de Rangement est stocké en
 * mémoire (voir note dans README : ça ne survit pas à un redémarrage tant
 * qu'on n'a pas branché une vraie sauvegarde).
 */
public class UtilityItemsListener implements Listener {

    private final Plugin plugin;
    private final ItemRegistry registry;
    private final Random random = new Random();

    private final Map<UUID, Location> teleportAnchors = new HashMap<>();
    private final Map<UUID, Inventory> personalStorage = new HashMap<>();

    public UtilityItemsListener(Plugin plugin, ItemRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        String id = ItemIdentifier.getId(plugin, hand);
        if (id == null) return;

        switch (id) {
            case Ids.ANCRE_TELEPORTATION -> handleAnchor(player);
            case Ids.BOUSSOLE_TRESOR -> handleTreasureCompass(player);
            case Ids.GRAPPIN -> handleGrapple(player);
            case Ids.FRAGMENT_ETOILE -> handleStarFragment(player, hand);
            case Ids.SAC_RANGEMENT -> handleStorageBag(player);
            case Ids.FOUET -> handleWhip(player, hand);
            default -> { return; }
        }
        event.setCancelled(true);
    }

    /**
     * Bâton de Bannissement : clic droit directement sur un joueur (et non plus
     * "au corps-à-corps") pour coller au lore de l'objet ("Clic droit sur un
     * joueur : le téléporte"). L'ancienne version accrochée à EntityDamageByEntityEvent
     * ne correspondait pas à cet usage annoncé, d'où l'impression que ça ne marchait pas.
     */
    @EventHandler
    public void onBanishStaffUse(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!ItemIdentifier.hasId(plugin, player.getInventory().getItemInMainHand(), Ids.BATON_TELEPORTATION_ENNEMI)) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        event.setCancelled(true);

        Location base = target.getLocation();
        Location destination = base.clone().add(
                random.nextInt(21) - 10, 0, random.nextInt(21) - 10);
        destination.setY(target.getWorld().getHighestBlockYAt(destination) + 1);
        target.teleport(destination);
        target.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, base, 40, 0.5, 1, 0.5, 0.1);
        target.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, destination, 40, 0.5, 1, 0.5, 0.1);
        target.getWorld().playSound(base, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
        player.sendMessage(Component.text("Tu bannis " + target.getName() + " au loin !", NamedTextColor.LIGHT_PURPLE));
        target.sendMessage(Component.text(player.getName() + " t'a banni au loin !", NamedTextColor.RED));
    }

    /**
     * Fouet des Âmes : maintenant activé en clic droit (comme annoncé dans le lore),
     * et non plus seulement quand on frappait déjà la cible au corps-à-corps.
     * Vise la première entité vivante dans la ligne de mire, jusqu'à 20 blocs.
     */
    private void handleWhip(Player player, ItemStack whip) {
        Entity targeted = player.getTargetEntity(20);
        if (!(targeted instanceof LivingEntity target) || target.equals(player)) {
            player.sendMessage(Component.text("Aucune cible visée par le fouet.", NamedTextColor.RED));
            return;
        }
        boolean upgraded = isUpgraded(whip);
        org.bukkit.util.Vector pull = player.getLocation().toVector().subtract(target.getLocation().toVector())
                .normalize().multiply(upgraded ? 1.8 : 1.2);
        target.setVelocity(pull.setY(0.3));
        target.getWorld().spawnParticle(org.bukkit.Particle.CRIT, target.getLocation(), 15, 0.2, 0.3, 0.2, 0.05);
        target.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.7f);
    }

    private boolean isUpgraded(ItemStack stack) {
        if (stack == null || stack.getItemMeta() == null) return false;
        Boolean value = stack.getItemMeta().getPersistentDataContainer()
                .get(MythicItem.upgradedKey(plugin), PersistentDataType.BOOLEAN);
        return Boolean.TRUE.equals(value);
    }

    private void handleAnchor(Player player) {
        if (player.isSneaking()) {
            teleportAnchors.put(player.getUniqueId(), player.getLocation());
            player.sendMessage(Component.text("Point de retour posé ici.", NamedTextColor.AQUA));
            return;
        }
        Location anchor = teleportAnchors.get(player.getUniqueId());
        if (anchor == null) {
            player.sendMessage(Component.text("Aucun point de retour posé (sneak + clic droit pour en poser un).", NamedTextColor.RED));
            return;
        }
        player.teleport(anchor);
        player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, anchor, 30, 0.4, 0.8, 0.4, 0.1);
    }

    private void handleTreasureCompass(Player player) {
        // Cherche la structure la plus proche parmi quelques types courants.
        org.bukkit.generator.structure.Structure[] toCheck = {
                org.bukkit.generator.structure.Structure.VILLAGE_PLAINS,
                org.bukkit.generator.structure.Structure.STRONGHOLD,
                org.bukkit.generator.structure.Structure.MONUMENT,
                org.bukkit.generator.structure.Structure.SHIPWRECK
        };
        player.sendMessage(Component.text("La boussole scrute les environs...", NamedTextColor.GRAY));
        Location closest = null;
        // Rayon élargi (1500 blocs) et findUnexplored=true : sans ça, la recherche se
        // limitait aux chunks déjà générés dans un rayon minuscule et ne trouvait
        // presque jamais rien, ce qui donnait l'impression que l'objet ne marchait pas.
        for (org.bukkit.generator.structure.Structure structure : toCheck) {
            var result = player.getWorld().locateNearestStructure(player.getLocation(), structure, 1500, true);
            if (result != null) {
                Location loc = result.getLocation();
                if (closest == null || loc.distanceSquared(player.getLocation()) < closest.distanceSquared(player.getLocation())) {
                    closest = loc;
                }
            }
        }
        if (closest == null) {
            player.sendMessage(Component.text("Aucune structure connue à proximité.", NamedTextColor.RED));
            return;
        }
        player.setCompassTarget(closest);
        player.sendMessage(Component.text("La boussole pointe vers la structure la plus proche.", NamedTextColor.GREEN));
    }

    private void handleGrapple(Player player) {
        var target = player.getTargetBlockExact(30);
        Location destination = target != null ? target.getLocation().add(0.5, 1, 0.5) : null;
        if (destination == null) {
            player.sendMessage(Component.text("Rien à accrocher dans cette direction.", NamedTextColor.RED));
            return;
        }
        org.bukkit.util.Vector direction = destination.toVector().subtract(player.getLocation().toVector());
        double distance = direction.length();
        if (distance < 1.5) {
            player.sendMessage(Component.text("Trop proche pour t'accrocher.", NamedTextColor.RED));
            return;
        }
        // Traction proportionnelle à la distance, plafonnée à 1.6 : évite l'effet
        // "téléportation" qu'on avait à courte portée avec l'ancienne valeur fixe (3.0).
        double thrust = Math.min(distance, 8.0) / 8.0 * 1.6;
        direction.normalize().multiply(thrust);
        player.setVelocity(direction.setY(Math.max(direction.getY(), 0.35)));
        player.getWorld().spawnParticle(org.bukkit.Particle.CRIT, player.getLocation(), 10, 0.2, 0.2, 0.2, 0.05);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ARROW_SHOOT, 1f, 1.3f);
    }

    private void handleStarFragment(Player player, ItemStack fragment) {
        var legendaries = registry.all().values().stream()
                .filter(item -> item.getRarity() == ItemRarity.LEGENDAIRE)
                .toList();
        if (legendaries.isEmpty()) {
            player.sendMessage(Component.text("Aucun objet légendaire enregistré pour l'instant.", NamedTextColor.RED));
            return;
        }
        MythicItem reward = legendaries.get(random.nextInt(legendaries.size()));
        player.getInventory().addItem(reward.build());
        fragment.setAmount(fragment.getAmount() - 1);
        player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, player.getLocation().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.05);
        player.sendMessage(Component.text("Le Fragment d'Étoile s'illumine et t'offre un objet légendaire !", NamedTextColor.GOLD));
    }

    private void handleStorageBag(Player player) {
        Inventory storage = personalStorage.computeIfAbsent(player.getUniqueId(),
                uuid -> plugin.getServer().createInventory(null, 36, Component.text("Sac de Rangement")));
        player.openInventory(storage);
    }
}
