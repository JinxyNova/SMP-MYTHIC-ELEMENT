package com.merci.mythicsmp.boss;

import com.merci.mythicsmp.economy.EconomyManager;
import com.merci.mythicsmp.elements.ElementManager;
import com.merci.mythicsmp.items.ItemRarity;
import com.merci.mythicsmp.items.ItemRegistry;
import com.merci.mythicsmp.items.MythicItem;
import com.merci.mythicsmp.items.mythic.InfinitePowerGemItem;
import com.merci.mythicsmp.items.mythic.UltimateMageGemItem;
import com.merci.mythicsmp.ultimate.UltimateMageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Random;
import java.util.UUID;

/**
 * Un seul "Gardien Écarlate" peut être en vie à la fois (par instance de
 * plugin, tous mondes confondus) pour éviter qu'on en spam plusieurs et que
 * ça devienne ingérable. Le suivi (barre de vie, tâche de capacités) est
 * gardé ici plutôt que sur l'entité elle-même pour rester facile à nettoyer
 * à la mort du boss.
 */
public class BossManager {

    public static final double MAX_HEALTH = 450.0;
    public static final double ATTACK_DAMAGE = 18.0;

    private static final int GUARANTEED_LOOT_COUNT = 3;
    private static final double MONEY_REWARD = 250.0;

    private final Plugin plugin;
    private final ItemRegistry itemRegistry;
    private final EconomyManager economyManager;
    private final ElementManager elementManager;
    private final UltimateMageManager ultimateMageManager;
    private final NamespacedKey bossKey;
    private final Random random = new Random();

    private UUID activeBossId;
    private BossBar activeBossBar;
    private BossAbilityTask activeTask;

    public BossManager(Plugin plugin, ItemRegistry itemRegistry, EconomyManager economyManager,
                        ElementManager elementManager, UltimateMageManager ultimateMageManager) {
        this.plugin = plugin;
        this.itemRegistry = itemRegistry;
        this.economyManager = economyManager;
        this.elementManager = elementManager;
        this.ultimateMageManager = ultimateMageManager;
        this.bossKey = new NamespacedKey(plugin, "mythicsmp_boss");
    }

    public NamespacedKey getBossKey() {
        return bossKey;
    }

    public boolean hasActiveBoss() {
        return activeBossId != null;
    }

    public String spawn(Location location) {
        if (hasActiveBoss()) {
            return "§cLe Gardien Écarlate est déjà en vie quelque part sur le serveur.";
        }

        World world = location.getWorld();
        WitherSkeleton boss = (WitherSkeleton) world.spawnEntity(location, EntityType.WITHER_SKELETON);
        boss.customName(Component.text("Le Gardien Écarlate", NamedTextColor.RED));
        boss.setCustomNameVisible(true);
        boss.getAttribute(Attribute.MAX_HEALTH).setBaseValue(MAX_HEALTH);
        boss.setHealth(MAX_HEALTH);
        boss.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(ATTACK_DAMAGE);
        if (boss.getAttribute(Attribute.KNOCKBACK_RESISTANCE) != null) {
            boss.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(0.6);
        }
        if (boss.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            boss.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.28);
        }
        boss.getPersistentDataContainer().set(bossKey, PersistentDataType.BOOLEAN, true);
        boss.setShouldBurnInDay(false);
        boss.getEquipment().setItemInMainHand(new ItemStack(org.bukkit.Material.NETHERITE_SWORD));
        boss.getEquipment().setHelmet(new ItemStack(org.bukkit.Material.NETHERITE_HELMET));
        boss.getEquipment().setChestplate(new ItemStack(org.bukkit.Material.NETHERITE_CHESTPLATE));
        boss.getEquipment().setLeggings(new ItemStack(org.bukkit.Material.NETHERITE_LEGGINGS));
        boss.getEquipment().setBoots(new ItemStack(org.bukkit.Material.NETHERITE_BOOTS));
        boss.getEquipment().setItemInMainHandDropChance(0f);
        boss.getEquipment().setHelmetDropChance(0f);
        boss.getEquipment().setChestplateDropChance(0f);
        boss.getEquipment().setLeggingsDropChance(0f);
        boss.getEquipment().setBootsDropChance(0f);

        BossBar bar = plugin.getServer().createBossBar("Le Gardien Écarlate", BarColor.RED, BarStyle.SEGMENTED_10);
        bar.setProgress(1.0);

        activeBossId = boss.getUniqueId();
        activeBossBar = bar;
        activeTask = new BossAbilityTask(plugin, boss, bar);
        activeTask.start();

        plugin.getServer().broadcast(Component.text("Le Gardien Écarlate est apparu ! Prépare-toi.", NamedTextColor.DARK_RED));
        return "§aLe Gardien Écarlate a été invoqué.";
    }

    /** Appelé par BossDeathListener quand l'entité meurt. */
    public void handleDeath(LivingEntity entity, Player killer) {
        if (!entity.getUniqueId().equals(activeBossId)) return;

        if (activeTask != null) activeTask.cancel();
        if (activeBossBar != null) activeBossBar.removeAll();

        for (int i = 0; i < GUARANTEED_LOOT_COUNT; i++) {
            MythicItem loot = randomLoot();
            if (loot != null) {
                entity.getWorld().dropItem(entity.getLocation(), loot.build());
            }
        }
        entity.getWorld().dropItem(entity.getLocation(), new ItemStack(org.bukkit.Material.NETHERITE_SWORD));

        if (killer != null) {
            economyManager.deposit(killer.getUniqueId(), MONEY_REWARD);
            killer.sendMessage(Component.text("Tu reçois " + (int) MONEY_REWARD + " pièces pour avoir vaincu le boss !", NamedTextColor.GOLD));

            if (killer.getLevel() >= ElementManager.GEM_XP_LEVEL_THRESHOLD
                    && !elementManager.isMaxed(killer.getUniqueId())) {
                MythicItem gem = itemRegistry.get(InfinitePowerGemItem.ID);
                if (gem != null) {
                    entity.getWorld().dropItem(entity.getLocation(), gem.build());
                    killer.sendMessage(Component.text(
                            "Un pouvoir infini t'envahit... une Gemme au Pouvoir Infini est apparue !",
                            NamedTextColor.LIGHT_PURPLE));
                }
            }

            if (killer.getLevel() >= UltimateMageManager.GEM_XP_LEVEL_THRESHOLD
                    && elementManager.isMaxed(killer.getUniqueId())
                    && !ultimateMageManager.hasClass(killer.getUniqueId())) {
                MythicItem ultimateGem = itemRegistry.get(UltimateMageGemItem.ID);
                if (ultimateGem != null) {
                    entity.getWorld().dropItem(entity.getLocation(), ultimateGem.build());
                    killer.sendMessage(Component.text(
                            "Les 4 éléments résonnent en toi... une Gemme du Mage Ultime est apparue !",
                            NamedTextColor.LIGHT_PURPLE));
                }
            }
        }

        String killerName = killer != null ? killer.getName() : "quelqu'un";
        plugin.getServer().broadcast(Component.text(
                "Le Gardien Écarlate a été vaincu par " + killerName + " !", NamedTextColor.GOLD));

        activeBossId = null;
        activeBossBar = null;
        activeTask = null;
    }

    private MythicItem randomLoot() {
        var candidates = itemRegistry.all().values().stream()
                .filter(item -> item.getRarity() == ItemRarity.LEGENDAIRE || item.getRarity() == ItemRarity.MYTHIQUE)
                .toList();
        if (candidates.isEmpty()) return null;
        return candidates.get(random.nextInt(candidates.size()));
    }

    /** Utile pour BossDeathListener afin de vérifier rapidement si une entité est LE boss actif. */
    public boolean isTrackedBoss(UUID entityId) {
        return entityId.equals(activeBossId);
    }
}
