package com.merci.mythicsmp.spells;

import com.merci.mythicsmp.elements.Element;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Les 64 sorts du jeu (16 par élément : 4 faibles, 4 moyens, 4 forts,
 * 4 ultra-forts — voir SpellTier). Les effets combinent les briques de
 * SpellEffects avec des chiffres mis à l'échelle par `power` (déjà égal à
 * tier.powerScale * maîtrise du joueur, voir SpellManager).
 *
 * Chaque élément a sa propre identité visuelle/sonore construite à partir
 * du kit de SpellEffects :
 *  - FEU   : FLAME/LAVA, sons de flammes et d'explosion.
 *  - EAU   : SPLASH/DRIPPING_WATER, sons d'eau et de vagues.
 *  - TERRE : BLOCK_CRUMBLE/CRIT, sons de pierre qui se brise.
 *  - VENT  : CLOUD/SWEEP_ATTACK, sons de rafales et de vol.
 * Les sorts d'attaque à distance envoient un vrai projectile qui voyage
 * dans les airs (SpellEffects#launchProjectile) plutôt qu'un rayon
 * instantané, et les buffs/dashs/avatars s'accompagnent de particules qui
 * suivent le joueur en continu (followingAura / dashTrail).
 */
public final class SpellRegistry {

    private final Map<String, Spell> byId = new LinkedHashMap<>();

    public SpellRegistry() {
        registerFeu();
        registerEau();
        registerTerre();
        registerVent();
    }

    private void add(String id, Element element, SpellTier tier, String name, String description, SpellEffect effect) {
        byId.put(id, new Spell(id, element, tier, name, description, effect));
    }

    public Spell get(String id) {
        return byId.get(id);
    }

    public List<Spell> all() {
        return List.copyOf(byId.values());
    }

    public List<Spell> forElement(Element element) {
        return byId.values().stream().filter(s -> s.element() == element).toList();
    }

    public List<Spell> forElementAndTier(Element element, SpellTier tier) {
        return byId.values().stream().filter(s -> s.element() == element && s.tier() == tier).toList();
    }

    private static int amp(double power) {
        return (int) Math.max(0, Math.min(4, Math.round(power) - 1));
    }

    // ---------------------------------------------------------------- FEU

    private void registerFeu() {
        add("feu_faible_1", Element.FEU, SpellTier.FAIBLE, "Étincelle",
                "Un projectile de flamme qui file droit devant toi et explose au contact.",
                (plugin, caster, power) -> SpellEffects.launchProjectile(plugin, caster, 20, 1.1, 0.6,
                        4.0 * power, 0, Particle.FLAME, Particle.LAVA,
                        Sound.ITEM_FIRECHARGE_USE, Sound.ENTITY_BLAZE_SHOOT));

        add("feu_faible_2", Element.FEU, SpellTier.FAIBLE, "Griffe Ardente",
                "Enflamme brièvement tes poings : tes prochaines attaques frappent plus fort.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 0.8f);
                    SpellEffects.buff(caster, PotionEffectType.STRENGTH, amp(power), 100);
                    SpellEffects.followingAura(plugin, caster, Particle.FLAME, 100, 0.6, 8);
                });

        add("feu_faible_3", Element.FEU, SpellTier.FAIBLE, "Pas Brûlant",
                "Un sursaut de vitesse laissant une traînée de flammes derrière toi.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.3f);
                    SpellEffects.buff(caster, PotionEffectType.SPEED, amp(power), 100);
                    SpellEffects.dashTrail(plugin, caster, Particle.FLAME, 25);
                });

        add("feu_faible_4", Element.FEU, SpellTier.FAIBLE, "Regard Ardent",
                "Un anneau de flammes révèle et fait briller les ennemis proches.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.3f, 1f);
                    SpellEffects.groundRing(caster.getLocation(), Particle.FLAME, 6.0, 32);
                    for (Entity nearby : caster.getNearbyEntities(15, 15, 15)) {
                        if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                            SpellEffects.debuff(target, PotionEffectType.GLOWING, 0, (int) (100 * power));
                        }
                    }
                });

        add("feu_moyen_1", Element.FEU, SpellTier.MOYEN, "Boule de Feu",
                "Projette une véritable boule de feu qui explose et éclabousse les alentours à l'impact.",
                (plugin, caster, power) -> SpellEffects.launchProjectile(plugin, caster, 26, 1.2, 0.8,
                        6.0 * power, 3.0, Particle.FLAME, Particle.LAVA,
                        Sound.ITEM_FIRECHARGE_USE, Sound.ENTITY_GENERIC_EXPLODE));

        add("feu_moyen_2", Element.FEU, SpellTier.MOYEN, "Anneau de Braises",
                "Une onde de flammes autour de toi endommage et repousse les ennemis proches.",
                (plugin, caster, power) -> SpellEffects.damageKnockbackAoe(caster, 4.0, 5.0 * power, 1.1,
                        Particle.FLAME, Particle.LAVA, Sound.ENTITY_GENERIC_EXPLODE));

        add("feu_moyen_3", Element.FEU, SpellTier.MOYEN, "Armure Ignée",
                "T'entoure de flammes protectrices qui t'accompagnent pendant un moment.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.4f, 0.9f);
                    SpellEffects.buff(caster, PotionEffectType.FIRE_RESISTANCE, 0, (int) (200 * power));
                    SpellEffects.shield(caster, amp(power), (int) (100 * power));
                    SpellEffects.followingAura(plugin, caster, Particle.FLAME, (int) (100 * power), 0.7, 10);
                });

        add("feu_moyen_4", Element.FEU, SpellTier.MOYEN, "Ruée Infernale",
                "Fonce en avant en embrasant tout sur ton passage.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.2f, 1.1f);
                    SpellEffects.dash(caster, 1.4 * power, 0.3);
                    SpellEffects.dashTrail(plugin, caster, Particle.FLAME, 20);
                    SpellEffects.damageAoe(caster, 3.0, 4.0 * power, Particle.FLAME, Sound.ENTITY_BLAZE_SHOOT);
                });

        add("feu_fort_1", Element.FEU, SpellTier.FORT, "Météore",
                "Un anneau de feu marque le point visé, puis un météore explosif s'y écrase.",
                (plugin, caster, power) -> {
                    var target = SpellEffects.forwardPoint(caster, 10);
                    SpellEffects.groundRing(target, Particle.FLAME, 4.0, 28);
                    SpellEffects.delayedExplodeAt(plugin, caster, target,
                            30, 4.0, 10.0 * power, Particle.FLAME, Sound.ENTITY_GENERIC_EXPLODE);
                });

        add("feu_fort_2", Element.FEU, SpellTier.FORT, "Vague Ardente",
                "Une vague de feu qui brûle et repousse tout devant toi.",
                (plugin, caster, power) -> SpellEffects.damageKnockbackAoe(caster, 5.5, 9.0 * power, 1.6,
                        Particle.FLAME, Particle.LAVA, Sound.ENTITY_GENERIC_EXPLODE));

        add("feu_fort_3", Element.FEU, SpellTier.FORT, "Renaissance des Cendres",
                "Une spirale de flammes monte autour de toi tandis que tu te soignes fortement.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TOTEM_USE, 1.2f, 1f);
                    SpellEffects.heal(caster, 6.0 * power);
                    SpellEffects.buff(caster, PotionEffectType.FIRE_RESISTANCE, 0, (int) (100 * power));
                    SpellEffects.risingSpiral(caster.getLocation(), Particle.FLAME, 2.5, 0.8, 2);
                });

        add("feu_fort_4", Element.FEU, SpellTier.FORT, "Tornade de Flammes",
                "Attire les ennemis proches vers toi puis les embrase.",
                (plugin, caster, power) -> SpellEffects.pullAoe(caster, 6.0, 7.0 * power, Particle.FLAME, Sound.ENTITY_BLAZE_SHOOT));

        add("feu_ultra_1", Element.FEU, SpellTier.ULTRA_FORT, "Apocalypse Écarlate",
                "Une explosion dévastatrice autour de toi. Sort ultime, longue recharge.",
                (plugin, caster, power) -> {
                    SpellEffects.risingSpiral(caster.getLocation(), Particle.FLAME, 3.0, 1.2, 3);
                    SpellEffects.damageKnockbackAoe(caster, 7.0, 16.0 * power, 1.8,
                            Particle.EXPLOSION, Particle.FLAME, Sound.ENTITY_GENERIC_EXPLODE);
                });

        add("feu_ultra_2", Element.FEU, SpellTier.ULTRA_FORT, "Avatar de Feu",
                "Te transforme temporairement en incarnation du feu : force, vitesse et immunité au feu.",
                (plugin, caster, power) -> {
                    int duration = (int) (140 * power);
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.4f);
                    SpellEffects.risingSpiral(caster.getLocation(), Particle.FLAME, 2.5, 1.0, 2);
                    SpellEffects.buff(caster, PotionEffectType.STRENGTH, amp(power), duration);
                    SpellEffects.buff(caster, PotionEffectType.SPEED, amp(power), duration);
                    SpellEffects.buff(caster, PotionEffectType.FIRE_RESISTANCE, 0, duration);
                    SpellEffects.followingAura(plugin, caster, Particle.FLAME, duration, 0.8, 12);
                });

        add("feu_ultra_3", Element.FEU, SpellTier.ULTRA_FORT, "Pluie de Météores",
                "Plusieurs météores s'abattent autour de toi sur quelques secondes.",
                (plugin, caster, power) -> {
                    var center = SpellEffects.forwardPoint(caster, 6);
                    SpellEffects.groundRing(center, Particle.FLAME, 6.0, 36);
                    SpellEffects.areaOverTime(plugin, caster, center, 6.0, 3.0 * power, 100, 20,
                            Particle.FLAME, Sound.ENTITY_GENERIC_EXPLODE);
                });

        add("feu_ultra_4", Element.FEU, SpellTier.ULTRA_FORT, "Cœur du Volcan",
                "Concentre toute ta puissance dans un coup dévastateur porté au corps à corps.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.3f, 0.7f);
                    boolean hit = SpellEffects.hitscan(caster, 5, 20.0 * power, Particle.LAVA);
                    if (hit) caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.8f);
                });
    }

    // ---------------------------------------------------------------- EAU

    private void registerEau() {
        add("eau_faible_1", Element.EAU, SpellTier.FAIBLE, "Jet d'Eau",
                "Un jet d'eau qui voyage devant toi et repousse la cible touchée.",
                (plugin, caster, power) -> SpellEffects.launchProjectile(plugin, caster, 18, 1.3, 0.6,
                        3.0 * power, 0, Particle.SPLASH, Particle.DRIPPING_WATER,
                        Sound.ITEM_BUCKET_EMPTY, Sound.ENTITY_PLAYER_SPLASH));

        add("eau_faible_2", Element.EAU, SpellTier.FAIBLE, "Brume Légère",
                "T'enveloppe de brume, gênant la visée des ennemis proches.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1f, 1.4f);
                    for (Entity nearby : caster.getNearbyEntities(8, 8, 8)) {
                        if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                            SpellEffects.debuff(target, PotionEffectType.BLINDNESS, 0, (int) (30 * power));
                        }
                    }
                    caster.getWorld().spawnParticle(Particle.CLOUD, caster.getLocation(), 40, 1.5, 1, 1.5, 0.01);
                    SpellEffects.followingAura(plugin, caster, Particle.SPLASH, 30, 1.0, 6);
                });

        add("eau_faible_3", Element.EAU, SpellTier.FAIBLE, "Pas Aquatique",
                "Un regain de vitesse et de mobilité, accompagné d'un sillage d'eau.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_DOLPHIN_JUMP, 1f, 1.2f);
                    SpellEffects.buff(caster, PotionEffectType.SPEED, amp(power), 120);
                    SpellEffects.buff(caster, PotionEffectType.DOLPHINS_GRACE, 0, 120);
                    SpellEffects.dashTrail(plugin, caster, Particle.DRIPPING_WATER, 25);
                });

        add("eau_faible_4", Element.EAU, SpellTier.FAIBLE, "Regard des Profondeurs",
                "T'offre une vision nocturne et sous-marine temporaire.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.AMBIENT_UNDERWATER_ENTER, 1f, 1f);
                    SpellEffects.buff(caster, PotionEffectType.NIGHT_VISION, 0, (int) (400 * power));
                    SpellEffects.buff(caster, PotionEffectType.WATER_BREATHING, 0, (int) (400 * power));
                });

        add("eau_moyen_1", Element.EAU, SpellTier.MOYEN, "Trombe d'Eau",
                "Une trombe d'eau voyageuse qui frappe et repousse la première cible touchée.",
                (plugin, caster, power) -> SpellEffects.launchProjectile(plugin, caster, 22, 1.4, 0.7,
                        5.0 * power, 0, Particle.SPLASH, Particle.DRIPPING_WATER,
                        Sound.ITEM_BUCKET_EMPTY, Sound.ENTITY_DOLPHIN_SPLASH));

        add("eau_moyen_2", Element.EAU, SpellTier.MOYEN, "Vague Assourdissante",
                "Une vague repousse et ralentit tous les ennemis autour de toi.",
                (plugin, caster, power) -> {
                    SpellEffects.damageKnockbackAoe(caster, 5.0, 3.0 * power, 1.3,
                            Particle.SPLASH, Particle.DRIPPING_WATER, Sound.ENTITY_DOLPHIN_SPLASH);
                    for (Entity nearby : caster.getNearbyEntities(5, 5, 5)) {
                        if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                            SpellEffects.debuff(target, PotionEffectType.SLOWNESS, amp(power), 60);
                        }
                    }
                });

        add("eau_moyen_3", Element.EAU, SpellTier.MOYEN, "Bulle Protectrice",
                "T'entoure d'une bulle qui absorbe les prochains dégâts.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.1f, 1.3f);
                    SpellEffects.shield(caster, amp(power) + 1, (int) (150 * power));
                    SpellEffects.followingAura(plugin, caster, Particle.BUBBLE_POP, (int) (150 * power), 0.7, 10);
                });

        add("eau_moyen_4", Element.EAU, SpellTier.MOYEN, "Cage de Glace",
                "Immobilise la cible visée dans un bloc de glace.",
                (plugin, caster, power) -> {
                    var eye = caster.getEyeLocation();
                    var result = caster.getWorld().rayTraceEntities(eye, eye.getDirection(), 10, 0.5,
                            e -> e instanceof LivingEntity && !e.equals(caster));
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.6f, 0.6f);
                    if (result != null && result.getHitEntity() instanceof LivingEntity target) {
                        SpellEffects.root(target, (int) (60 * power));
                        target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.02);
                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_PLACE, 1f, 0.8f);
                    }
                });

        add("eau_fort_1", Element.EAU, SpellTier.FORT, "Marée Destructrice",
                "Une vague massive repousse et endommage tout ce qui t'entoure.",
                (plugin, caster, power) -> SpellEffects.damageKnockbackAoe(caster, 6.5, 8.0 * power, 1.8,
                        Particle.SPLASH, Particle.DRIPPING_WATER, Sound.ENTITY_DOLPHIN_SPLASH));

        add("eau_fort_2", Element.EAU, SpellTier.FORT, "Prison de Glace",
                "Gèle totalement la cible visée, l'empêchant d'agir un moment.",
                (plugin, caster, power) -> {
                    var eye = caster.getEyeLocation();
                    var result = caster.getWorld().rayTraceEntities(eye, eye.getDirection(), 12, 0.5,
                            e -> e instanceof LivingEntity && !e.equals(caster));
                    if (result != null && result.getHitEntity() instanceof LivingEntity target) {
                        SpellEffects.root(target, (int) (100 * power));
                        SpellEffects.debuff(target, PotionEffectType.WEAKNESS, amp(power), (int) (100 * power));
                        target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 50, 0.5, 0.8, 0.5, 0.03);
                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.2f, 0.7f);
                    }
                });

        add("eau_fort_3", Element.EAU, SpellTier.FORT, "Renouveau Océanique",
                "Un puissant soin qui purifie aussi les effets négatifs.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1.4f);
                    SpellEffects.heal(caster, 10.0 * power);
                    SpellEffects.risingSpiral(caster.getLocation(), Particle.SPLASH, 2.2, 0.7, 2);
                    for (var effect : List.copyOf(caster.getActivePotionEffects())) {
                        if (effect.getType().equals(PotionEffectType.POISON)
                                || effect.getType().equals(PotionEffectType.WITHER)
                                || effect.getType().equals(PotionEffectType.SLOWNESS)
                                || effect.getType().equals(PotionEffectType.WEAKNESS)) {
                            caster.removePotionEffect(effect.getType());
                        }
                    }
                });

        add("eau_fort_4", Element.EAU, SpellTier.FORT, "Danse des Courants",
                "Un déplacement rapide et fluide, difficile à toucher pendant un instant.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_DOLPHIN_JUMP, 1.2f, 1.1f);
                    SpellEffects.dash(caster, 1.6 * power, 0.2);
                    SpellEffects.dashTrail(plugin, caster, Particle.SPLASH, 20);
                    SpellEffects.buff(caster, PotionEffectType.SPEED, amp(power) + 1, 60);
                });

        add("eau_ultra_1", Element.EAU, SpellTier.ULTRA_FORT, "Déluge",
                "Une zone d'eau déchaînée inflige des dégâts continus pendant plusieurs secondes.",
                (plugin, caster, power) -> {
                    var center = SpellEffects.forwardPoint(caster, 5);
                    SpellEffects.groundRing(center, Particle.SPLASH, 5.5, 32);
                    SpellEffects.areaOverTime(plugin, caster, center, 5.5, 2.5 * power, 100, 20,
                            Particle.SPLASH, Sound.ENTITY_DOLPHIN_SPLASH);
                });

        add("eau_ultra_2", Element.EAU, SpellTier.ULTRA_FORT, "Léviathan des Abysses",
                "Invoque temporairement un allié aquatique pour combattre à tes côtés.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_DROWNED_AMBIENT, 1.3f, 0.8f);
                    SpellEffects.groundRing(caster.getLocation(), Particle.SPLASH, 2.0, 20);
                    SpellEffects.summonAlly(plugin, caster, EntityType.DROWNED, (int) (300 * power));
                });

        add("eau_ultra_3", Element.EAU, SpellTier.ULTRA_FORT, "Glaciation Totale",
                "Gèle tous les ennemis dans une large zone autour de toi.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.3f, 0.6f);
                    for (Entity nearby : caster.getNearbyEntities(8, 8, 8)) {
                        if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                            SpellEffects.root(target, (int) (100 * power));
                        }
                    }
                    caster.getWorld().spawnParticle(Particle.SNOWFLAKE, caster.getLocation(), 100, 4, 2, 4, 0.05);
                    SpellEffects.groundRing(caster.getLocation(), Particle.SNOWFLAKE, 8.0, 40);
                });

        add("eau_ultra_4", Element.EAU, SpellTier.ULTRA_FORT, "Cœur de l'Océan",
                "T'entoure d'un puissant bouclier et te soigne progressivement.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TOTEM_USE, 1.2f, 1.2f);
                    SpellEffects.shield(caster, amp(power) + 2, (int) (200 * power));
                    SpellEffects.buff(caster, PotionEffectType.REGENERATION, amp(power), (int) (200 * power));
                    SpellEffects.followingAura(plugin, caster, Particle.SPLASH, (int) (200 * power), 0.9, 12);
                });
    }

    // ------------------------------------------------------------- TERRE

    private void registerTerre() {
        add("terre_faible_1", Element.TERRE, SpellTier.FAIBLE, "Éclat de Pierre",
                "Projette un éclat de pierre tranchant qui voyage droit sur la cible.",
                (plugin, caster, power) -> SpellEffects.launchProjectile(plugin, caster, 18, 1.2, 0.55,
                        4.5 * power, 0, Particle.BLOCK_CRUMBLE, Particle.CRIT,
                        Sound.BLOCK_STONE_BREAK, Sound.BLOCK_STONE_HIT));

        add("terre_faible_2", Element.TERRE, SpellTier.FAIBLE, "Racines",
                "Fait surgir des racines qui immobilisent brièvement la cible visée.",
                (plugin, caster, power) -> {
                    var eye = caster.getEyeLocation();
                    var result = caster.getWorld().rayTraceEntities(eye, eye.getDirection(), 8, 0.5,
                            e -> e instanceof LivingEntity && !e.equals(caster));
                    if (result != null && result.getHitEntity() instanceof LivingEntity target) {
                        SpellEffects.root(target, (int) (30 * power));
                        target.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, target.getLocation(), 30, 0.3, 0.5, 0.3, 0.02);
                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GRASS_BREAK, 1f, 1f);
                    }
                });

        add("terre_faible_3", Element.TERRE, SpellTier.FAIBLE, "Peau de Roche",
                "Ta peau durcit temporairement, réduisant les dégâts reçus.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_STONE_PLACE, 1.1f, 0.9f);
                    SpellEffects.buff(caster, PotionEffectType.RESISTANCE, amp(power), 100);
                    SpellEffects.followingAura(plugin, caster, Particle.BLOCK_CRUMBLE, 100, 0.6, 8);
                });

        add("terre_faible_4", Element.TERRE, SpellTier.FAIBLE, "Odorat de la Terre",
                "Révèle brièvement les créatures proches à travers les obstacles.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_STONE_STEP, 1f, 0.7f);
                    for (Entity nearby : caster.getNearbyEntities(20, 20, 20)) {
                        if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                            SpellEffects.debuff(target, PotionEffectType.GLOWING, 0, (int) (100 * power));
                        }
                    }
                });

        add("terre_moyen_1", Element.TERRE, SpellTier.MOYEN, "Séisme Local",
                "Un choc sismique fait vaciller et endommage les ennemis proches.",
                (plugin, caster, power) -> SpellEffects.damageKnockbackAoe(caster, 4.5, 5.0 * power, 0.8,
                        Particle.BLOCK_CRUMBLE, Particle.CRIT, Sound.BLOCK_STONE_BREAK));

        add("terre_moyen_2", Element.TERRE, SpellTier.MOYEN, "Mur de Pierre",
                "Un mur de pierre s'érige devant toi, bloquant projectiles et ennemis un instant.",
                (plugin, caster, power) -> {
                    SpellEffects.shield(caster, amp(power) + 1, (int) (80 * power));
                    caster.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, SpellEffects.forwardPoint(caster, 2), 70, 1, 1.5, 0.3, 0);
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_STONE_PLACE, 1.2f, 0.8f);
                });

        add("terre_moyen_3", Element.TERRE, SpellTier.MOYEN, "Carapace",
                "Une carapace de pierre t'offre une solide protection temporaire.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_STONE_PLACE, 1.3f, 0.7f);
                    SpellEffects.buff(caster, PotionEffectType.RESISTANCE, amp(power) + 1, (int) (100 * power));
                    SpellEffects.buff(caster, PotionEffectType.SLOWNESS, 0, (int) (100 * power));
                    SpellEffects.followingAura(plugin, caster, Particle.BLOCK_CRUMBLE, (int) (100 * power), 0.7, 10);
                });

        add("terre_moyen_4", Element.TERRE, SpellTier.MOYEN, "Charge du Golem",
                "Fonce en avant et percute violemment les ennemis sur ton passage.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.1f, 0.9f);
                    SpellEffects.dash(caster, 1.2 * power, 0.2);
                    SpellEffects.dashTrail(plugin, caster, Particle.BLOCK_CRUMBLE, 20);
                    SpellEffects.damageKnockbackAoe(caster, 3.0, 6.0 * power, 1.4, Particle.BLOCK_CRUMBLE, Particle.CRIT, Sound.BLOCK_STONE_BREAK);
                });

        add("terre_fort_1", Element.TERRE, SpellTier.FORT, "Tremblement de Terre",
                "Une secousse puissante endommage et déséquilibre tous les ennemis proches.",
                (plugin, caster, power) -> SpellEffects.damageKnockbackAoe(caster, 6.0, 9.0 * power, 1.0,
                        Particle.EXPLOSION, Particle.BLOCK_CRUMBLE, Sound.ENTITY_IRON_GOLEM_ATTACK));

        add("terre_fort_2", Element.TERRE, SpellTier.FORT, "Prison Rocheuse",
                "Emprisonne la cible visée dans la roche, l'immobilisant longuement.",
                (plugin, caster, power) -> {
                    var eye = caster.getEyeLocation();
                    var result = caster.getWorld().rayTraceEntities(eye, eye.getDirection(), 12, 0.5,
                            e -> e instanceof LivingEntity && !e.equals(caster));
                    if (result != null && result.getHitEntity() instanceof LivingEntity target) {
                        SpellEffects.root(target, (int) (100 * power));
                        target.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, target.getLocation().add(0, 1, 0), 50, 0.4, 0.7, 0.4, 0.03);
                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_STONE_PLACE, 1.3f, 0.6f);
                    }
                });

        add("terre_fort_3", Element.TERRE, SpellTier.FORT, "Régénération Tellurique",
                "Puise dans la force de la terre pour te soigner fortement dans la durée.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
                    SpellEffects.heal(caster, 6.0 * power);
                    SpellEffects.buff(caster, PotionEffectType.REGENERATION, amp(power), (int) (100 * power));
                    SpellEffects.risingSpiral(caster.getLocation(), Particle.BLOCK_CRUMBLE, 2.2, 0.7, 2);
                });

        add("terre_fort_4", Element.TERRE, SpellTier.FORT, "Invocation de Golem",
                "Invoque un golem de fer temporaire pour combattre à tes côtés.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.3f, 0.7f);
                    SpellEffects.groundRing(caster.getLocation(), Particle.BLOCK_CRUMBLE, 2.0, 20);
                    SpellEffects.summonAlly(plugin, caster, EntityType.IRON_GOLEM, (int) (400 * power));
                });

        add("terre_ultra_1", Element.TERRE, SpellTier.ULTRA_FORT, "Cataclysme",
                "Une explosion tellurique dévastatrice autour de toi. Sort ultime, longue recharge.",
                (plugin, caster, power) -> {
                    SpellEffects.risingSpiral(caster.getLocation(), Particle.BLOCK_CRUMBLE, 3.0, 1.2, 3);
                    SpellEffects.damageKnockbackAoe(caster, 7.5, 17.0 * power, 1.5,
                            Particle.EXPLOSION_EMITTER, Particle.BLOCK_CRUMBLE, Sound.ENTITY_GENERIC_EXPLODE);
                });

        add("terre_ultra_2", Element.TERRE, SpellTier.ULTRA_FORT, "Avatar de Pierre",
                "Te transforme temporairement en colosse de pierre : résistance et force énormes.",
                (plugin, caster, power) -> {
                    int duration = (int) (140 * power);
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_STONE_PLACE, 1.4f, 0.6f);
                    SpellEffects.buff(caster, PotionEffectType.RESISTANCE, amp(power) + 1, duration);
                    SpellEffects.buff(caster, PotionEffectType.STRENGTH, amp(power), duration);
                    SpellEffects.followingAura(plugin, caster, Particle.BLOCK_CRUMBLE, duration, 0.8, 12);
                });

        add("terre_ultra_3", Element.TERRE, SpellTier.ULTRA_FORT, "Effondrement",
                "Le sol s'effondre après un court délai, assommant et blessant lourdement les ennemis proches.",
                (plugin, caster, power) -> {
                    SpellEffects.groundRing(caster.getLocation(), Particle.BLOCK_CRUMBLE, 6.5, 36);
                    SpellEffects.delayedExplodeAt(plugin, caster, caster.getLocation(),
                            30, 6.5, 14.0 * power, Particle.BLOCK_CRUMBLE, Sound.ENTITY_GENERIC_EXPLODE);
                });

        add("terre_ultra_4", Element.TERRE, SpellTier.ULTRA_FORT, "Cœur de la Montagne",
                "T'entoure d'un bouclier massif et réduit fortement les dégâts reçus un long moment.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_STONE_PLACE, 1.3f, 0.5f);
                    SpellEffects.shield(caster, amp(power) + 2, (int) (200 * power));
                    SpellEffects.buff(caster, PotionEffectType.RESISTANCE, amp(power) + 1, (int) (200 * power));
                    SpellEffects.followingAura(plugin, caster, Particle.BLOCK_CRUMBLE, (int) (200 * power), 0.9, 12);
                });
    }

    // -------------------------------------------------------------- VENT

    private void registerVent() {
        add("vent_faible_1", Element.VENT, SpellTier.FAIBLE, "Rafale",
                "Une rafale de vent qui voyage devant toi et repousse la cible touchée.",
                (plugin, caster, power) -> SpellEffects.launchProjectile(plugin, caster, 20, 1.6, 0.6,
                        3.0 * power, 0, Particle.CLOUD, Particle.SWEEP_ATTACK,
                        Sound.ENTITY_PHANTOM_FLAP, Sound.ENTITY_PLAYER_ATTACK_SWEEP));

        add("vent_faible_2", Element.VENT, SpellTier.FAIBLE, "Bond",
                "Un puissant saut propulsé par le vent.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.1f, 1.3f);
                    SpellEffects.dash(caster, 0.4, 1.0 * power);
                    SpellEffects.dashTrail(plugin, caster, Particle.CLOUD, 15);
                });

        add("vent_faible_3", Element.VENT, SpellTier.FAIBLE, "Pas Léger",
                "Un souffle de vent qui accélère tes mouvements.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_ELYTRA_FLYING, 0.8f, 1.4f);
                    SpellEffects.buff(caster, PotionEffectType.SPEED, amp(power), 120);
                    SpellEffects.dashTrail(plugin, caster, Particle.CLOUD, 25);
                });

        add("vent_faible_4", Element.VENT, SpellTier.FAIBLE, "Œil du Vent",
                "Le vent te renseigne brièvement sur la position des ennemis proches.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1.5f);
                    for (Entity nearby : caster.getNearbyEntities(15, 15, 15)) {
                        if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                            SpellEffects.debuff(target, PotionEffectType.GLOWING, 0, (int) (80 * power));
                        }
                    }
                });

        add("vent_moyen_1", Element.VENT, SpellTier.MOYEN, "Tornade Miniature",
                "Une petite tornade attire et endommage les ennemis proches.",
                (plugin, caster, power) -> SpellEffects.pullAoe(caster, 5.0, 4.0 * power, Particle.CLOUD, Sound.ENTITY_PHANTOM_FLAP));

        add("vent_moyen_2", Element.VENT, SpellTier.MOYEN, "Bourrasque",
                "Une bourrasque puissante repousse violemment tout ce qui t'entoure.",
                (plugin, caster, power) -> SpellEffects.damageKnockbackAoe(caster, 5.0, 3.0 * power, 2.2,
                        Particle.CLOUD, Particle.SWEEP_ATTACK, Sound.ENTITY_PLAYER_ATTACK_SWEEP));

        add("vent_moyen_3", Element.VENT, SpellTier.MOYEN, "Ailes du Vent",
                "Ralentit ta chute et te rend plus rapide un instant, comme si tu planais.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1f, 1.2f);
                    SpellEffects.buff(caster, PotionEffectType.SLOW_FALLING, 0, (int) (100 * power));
                    SpellEffects.buff(caster, PotionEffectType.SPEED, amp(power), 100);
                    SpellEffects.followingAura(plugin, caster, Particle.CLOUD, (int) (100 * power), 0.7, 10);
                });

        add("vent_moyen_4", Element.VENT, SpellTier.MOYEN, "Lame de Vent",
                "Une lame d'air tranchante voyage et frappe la première cible touchée.",
                (plugin, caster, power) -> SpellEffects.launchProjectile(plugin, caster, 24, 1.7, 0.6,
                        6.0 * power, 0, Particle.SWEEP_ATTACK, Particle.CLOUD,
                        Sound.ENTITY_PLAYER_ATTACK_SWEEP, Sound.ENTITY_PLAYER_ATTACK_SWEEP));

        add("vent_fort_1", Element.VENT, SpellTier.FORT, "Cyclone",
                "Un cyclone attire puis endommage continuellement les ennemis proches.",
                (plugin, caster, power) -> {
                    SpellEffects.pullAoe(caster, 6.5, 0, Particle.CLOUD, Sound.ENTITY_PHANTOM_FLAP);
                    SpellEffects.areaOverTime(plugin, caster, caster.getLocation(), 5.0, 2.0 * power, 60, 20,
                            Particle.CLOUD, Sound.ENTITY_PHANTOM_FLAP);
                });

        add("vent_fort_2", Element.VENT, SpellTier.FORT, "Éclair Fulgurant",
                "Un déplacement fulgurant qui endommage tout sur ton passage.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.3f, 1.2f);
                    SpellEffects.dash(caster, 2.2 * power, 0.15);
                    SpellEffects.dashTrail(plugin, caster, Particle.CLOUD, 15);
                    SpellEffects.damageAoe(caster, 3.0, 6.0 * power, Particle.CLOUD, Sound.ENTITY_PHANTOM_FLAP);
                });

        add("vent_fort_3", Element.VENT, SpellTier.FORT, "Bouclier Aérien",
                "Un mur d'air te protège des projectiles et des chocs pendant un moment.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1.1f, 1f);
                    SpellEffects.shield(caster, amp(power) + 1, (int) (150 * power));
                    SpellEffects.followingAura(plugin, caster, Particle.CLOUD, (int) (150 * power), 0.8, 10);
                });

        add("vent_fort_4", Element.VENT, SpellTier.FORT, "Chant des Tempêtes",
                "Affaiblit et ralentit tous les ennemis dans une large zone.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.3f, 0.7f);
                    SpellEffects.groundRing(caster.getLocation(), Particle.CLOUD, 7.0, 32);
                    for (Entity nearby : caster.getNearbyEntities(7, 7, 7)) {
                        if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                            SpellEffects.debuff(target, PotionEffectType.SLOWNESS, amp(power), (int) (100 * power));
                            SpellEffects.debuff(target, PotionEffectType.WEAKNESS, amp(power), (int) (100 * power));
                        }
                    }
                });

        add("vent_ultra_1", Element.VENT, SpellTier.ULTRA_FORT, "Tempête Dévastatrice",
                "Une tempête déchaînée inflige des dégâts continus et repousse tout autour de toi.",
                (plugin, caster, power) -> {
                    SpellEffects.damageKnockbackAoe(caster, 6.0, 8.0 * power, 1.6,
                            Particle.CLOUD, Particle.SWEEP_ATTACK, Sound.ENTITY_PLAYER_ATTACK_SWEEP);
                    SpellEffects.areaOverTime(plugin, caster, caster.getLocation(), 6.0, 2.5 * power, 100, 20,
                            Particle.CLOUD, Sound.ENTITY_PHANTOM_FLAP);
                });

        add("vent_ultra_2", Element.VENT, SpellTier.ULTRA_FORT, "Avatar du Vent",
                "Te transforme temporairement en tourbillon : vitesse et saut extrêmes.",
                (plugin, caster, power) -> {
                    int duration = (int) (140 * power);
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1.3f, 1.3f);
                    SpellEffects.buff(caster, PotionEffectType.SPEED, amp(power) + 1, duration);
                    SpellEffects.buff(caster, PotionEffectType.JUMP_BOOST, amp(power) + 1, duration);
                    SpellEffects.buff(caster, PotionEffectType.SLOW_FALLING, 0, duration);
                    SpellEffects.followingAura(plugin, caster, Particle.CLOUD, duration, 0.8, 12);
                });

        add("vent_ultra_3", Element.VENT, SpellTier.ULTRA_FORT, "Œil du Cyclone",
                "Aspire violemment tous les ennemis proches vers toi avant de les frapper.",
                (plugin, caster, power) -> {
                    SpellEffects.pullAoe(caster, 8.0, 0, Particle.CLOUD, Sound.ENTITY_PHANTOM_FLAP);
                    SpellEffects.delayedExplodeAt(plugin, caster, caster.getLocation(), 15, 4.0, 12.0 * power,
                            Particle.EXPLOSION, Sound.ENTITY_GENERIC_EXPLODE);
                });

        add("vent_ultra_4", Element.VENT, SpellTier.ULTRA_FORT, "Souffle des Cieux",
                "T'octroie une mobilité aérienne exceptionnelle pendant une longue durée.",
                (plugin, caster, power) -> {
                    int duration = (int) (200 * power);
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1.3f, 1f);
                    SpellEffects.buff(caster, PotionEffectType.SLOW_FALLING, 0, duration);
                    SpellEffects.buff(caster, PotionEffectType.SPEED, amp(power) + 1, duration);
                    SpellEffects.buff(caster, PotionEffectType.JUMP_BOOST, amp(power) + 1, duration);
                    SpellEffects.dash(caster, 0.5, 1.4 * power);
                    SpellEffects.followingAura(plugin, caster, Particle.CLOUD, duration, 0.9, 12);
                });
    }
}
