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
 *
 * Rayons, portées et durées ont été augmentés par rapport à la première
 * version (barème approximatif : x1.25 pour les sorts Faible, x1.35 pour
 * Moyen, x1.45 pour Fort, x1.6 pour Ultra-Fort) — les dégâts bruts restent
 * inchangés, seuls la taille des zones, la portée et la durée des effets
 * grandissent, y compris pour les sorts déjà les plus puissants.
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
                (plugin, caster, power) -> SpellEffects.launchProjectile(plugin, caster, 25, 1.1, 0.75,
                        4.0 * power, 0, Particle.FLAME, Particle.LAVA,
                        Sound.ITEM_FIRECHARGE_USE, Sound.ENTITY_BLAZE_SHOOT));

        add("feu_faible_2", Element.FEU, SpellTier.FAIBLE, "Griffe Ardente",
                "Enflamme brièvement tes poings : tes prochaines attaques frappent plus fort.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 0.8f);
                    SpellEffects.buff(caster, PotionEffectType.STRENGTH, amp(power), 125);
                    SpellEffects.followingAura(plugin, caster, Particle.FLAME, 125, 0.75, 8);
                });

        add("feu_faible_3", Element.FEU, SpellTier.FAIBLE, "Pas Brûlant",
                "Un sursaut de vitesse laissant une traînée de flammes derrière toi.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.3f);
                    SpellEffects.buff(caster, PotionEffectType.SPEED, amp(power), 125);
                    SpellEffects.dashTrail(plugin, caster, Particle.FLAME, 31);
                });

        add("feu_faible_4", Element.FEU, SpellTier.FAIBLE, "Regard Ardent",
                "Un anneau de flammes révèle et fait briller les ennemis proches.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.3f, 1f);
                    SpellEffects.groundRing(caster.getLocation(), Particle.FLAME, 7.5, 36);
                    for (Entity nearby : caster.getNearbyEntities(19, 19, 19)) {
                        if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                            SpellEffects.debuff(target, PotionEffectType.GLOWING, 0, (int) (125 * power));
                        }
                    }
                });

        add("feu_moyen_1", Element.FEU, SpellTier.MOYEN, "Boule de Feu",
                "Projette une véritable boule de feu qui explose et éclabousse les alentours à l'impact.",
                (plugin, caster, power) -> SpellEffects.launchProjectile(plugin, caster, 35, 1.2, 1.05,
                        6.0 * power, 4.0, Particle.FLAME, Particle.LAVA,
                        Sound.ITEM_FIRECHARGE_USE, Sound.ENTITY_GENERIC_EXPLODE));

        add("feu_moyen_2", Element.FEU, SpellTier.MOYEN, "Anneau de Braises",
                "Une onde de flammes autour de toi endommage et repousse les ennemis proches.",
                (plugin, caster, power) -> SpellEffects.damageKnockbackAoe(caster, 5.4, 5.0 * power, 1.2,
                        Particle.FLAME, Particle.LAVA, Sound.ENTITY_GENERIC_EXPLODE));

        add("feu_moyen_3", Element.FEU, SpellTier.MOYEN, "Armure Ignée",
                "T'entoure de flammes protectrices qui t'accompagnent pendant un moment.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.4f, 0.9f);
                    SpellEffects.buff(caster, PotionEffectType.FIRE_RESISTANCE, 0, (int) (270 * power));
                    SpellEffects.shield(caster, amp(power), (int) (135 * power));
                    SpellEffects.followingAura(plugin, caster, Particle.FLAME, (int) (135 * power), 0.95, 10);
                });

        add("feu_moyen_4", Element.FEU, SpellTier.MOYEN, "Ruée Infernale",
                "Fonce en avant en embrasant tout sur ton passage.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.2f, 1.1f);
                    SpellEffects.dash(caster, 1.7 * power, 0.3);
                    SpellEffects.dashTrail(plugin, caster, Particle.FLAME, 27);
                    SpellEffects.damageAoe(caster, 4.1, 4.0 * power, Particle.FLAME, Sound.ENTITY_BLAZE_SHOOT);
                });

        add("feu_fort_1", Element.FEU, SpellTier.FORT, "Météore",
                "Un véritable météore s'écrase depuis le ciel sur le point visé.",
                (plugin, caster, power) -> {
                    var target = SpellEffects.forwardPoint(caster, 14);
                    SpellEffects.meteorFall(plugin, caster, target, 18, 0.9, 5.8, 10.0 * power,
                            Particle.FLAME, Particle.LAVA, Sound.ENTITY_GENERIC_EXPLODE);
                });

        add("feu_fort_2", Element.FEU, SpellTier.FORT, "Vague Ardente",
                "Une vague de feu qui brûle et repousse tout devant toi.",
                (plugin, caster, power) -> SpellEffects.damageKnockbackAoe(caster, 8.0, 9.0 * power, 1.8,
                        Particle.FLAME, Particle.LAVA, Sound.ENTITY_GENERIC_EXPLODE));

        add("feu_fort_3", Element.FEU, SpellTier.FORT, "Renaissance des Cendres",
                "Une spirale de flammes monte autour de toi tandis que tu te soignes fortement.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TOTEM_USE, 1.2f, 1f);
                    SpellEffects.heal(caster, 6.0 * power);
                    SpellEffects.buff(caster, PotionEffectType.FIRE_RESISTANCE, 0, (int) (145 * power));
                    SpellEffects.risingSpiral(caster.getLocation(), Particle.FLAME, 3.6, 1.15, 2);
                });

        add("feu_fort_4", Element.FEU, SpellTier.FORT, "Tornade de Flammes",
                "Attire les ennemis proches vers toi puis les embrase.",
                (plugin, caster, power) -> SpellEffects.pullAoe(caster, 8.7, 7.0 * power, Particle.FLAME, Sound.ENTITY_BLAZE_SHOOT));

        add("feu_ultra_1", Element.FEU, SpellTier.ULTRA_FORT, "Apocalypse Écarlate",
                "Une explosion dévastatrice autour de toi. Sort ultime, longue recharge.",
                (plugin, caster, power) -> {
                    SpellEffects.risingSpiral(caster.getLocation(), Particle.FLAME, 4.8, 1.9, 3);
                    SpellEffects.damageKnockbackAoe(caster, 11.2, 16.0 * power, 2.1,
                            Particle.EXPLOSION, Particle.FLAME, Sound.ENTITY_GENERIC_EXPLODE);
                });

        add("feu_ultra_2", Element.FEU, SpellTier.ULTRA_FORT, "Avatar de Feu",
                "Te transforme temporairement en incarnation du feu : force, vitesse et immunité au feu.",
                (plugin, caster, power) -> {
                    int duration = (int) (224 * power);
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.4f);
                    SpellEffects.risingSpiral(caster.getLocation(), Particle.FLAME, 4.0, 1.6, 2);
                    SpellEffects.buff(caster, PotionEffectType.STRENGTH, amp(power), duration);
                    SpellEffects.buff(caster, PotionEffectType.SPEED, amp(power), duration);
                    SpellEffects.buff(caster, PotionEffectType.FIRE_RESISTANCE, 0, duration);
                    SpellEffects.followingAura(plugin, caster, Particle.FLAME, duration, 1.28, 12);
                });

        add("feu_ultra_3", Element.FEU, SpellTier.ULTRA_FORT, "Pluie de Météores",
                "Plusieurs météores s'abattent réellement du ciel dans la zone visée.",
                (plugin, caster, power) -> {
                    var center = SpellEffects.forwardPoint(caster, 9);
                    SpellEffects.meteorShower(plugin, caster, center, 5, 9.6, 20, 1.0, 3.2,
                            4.0 * power, Particle.FLAME, Particle.LAVA, Sound.ENTITY_GENERIC_EXPLODE);
                });

        add("feu_ultra_4", Element.FEU, SpellTier.ULTRA_FORT, "Cœur du Volcan",
                "Concentre toute ta puissance dans un coup dévastateur porté au corps à corps.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.3f, 0.7f);
                    boolean hit = SpellEffects.hitscan(caster, 7, 20.0 * power, Particle.LAVA);
                    if (hit) caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.8f);
                });
    }

    // ---------------------------------------------------------------- EAU

    private void registerEau() {
        add("eau_faible_1", Element.EAU, SpellTier.FAIBLE, "Jet d'Eau",
                "Un jet d'eau qui voyage devant toi et repousse la cible touchée.",
                (plugin, caster, power) -> SpellEffects.launchProjectile(plugin, caster, 22, 1.3, 0.75,
                        3.0 * power, 0, Particle.SPLASH, Particle.DRIPPING_WATER,
                        Sound.ITEM_BUCKET_EMPTY, Sound.ENTITY_PLAYER_SPLASH));

        add("eau_faible_2", Element.EAU, SpellTier.FAIBLE, "Brume Légère",
                "T'enveloppe de brume, gênant la visée des ennemis proches.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1f, 1.4f);
                    for (Entity nearby : caster.getNearbyEntities(10, 10, 10)) {
                        if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                            SpellEffects.debuff(target, PotionEffectType.BLINDNESS, 0, (int) (38 * power));
                        }
                    }
                    caster.getWorld().spawnParticle(Particle.CLOUD, caster.getLocation(), 55, 1.9, 1.3, 1.9, 0.015);
                    SpellEffects.followingAura(plugin, caster, Particle.SPLASH, 38, 1.25, 6);
                });

        add("eau_faible_3", Element.EAU, SpellTier.FAIBLE, "Pas Aquatique",
                "Un regain de vitesse et de mobilité, accompagné d'un sillage d'eau.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_DOLPHIN_JUMP, 1f, 1.2f);
                    SpellEffects.buff(caster, PotionEffectType.SPEED, amp(power), 150);
                    SpellEffects.buff(caster, PotionEffectType.DOLPHINS_GRACE, 0, 150);
                    SpellEffects.dashTrail(plugin, caster, Particle.DRIPPING_WATER, 31);
                });

        add("eau_faible_4", Element.EAU, SpellTier.FAIBLE, "Regard des Profondeurs",
                "T'offre une vision nocturne et sous-marine temporaire.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.AMBIENT_UNDERWATER_ENTER, 1f, 1f);
                    SpellEffects.buff(caster, PotionEffectType.NIGHT_VISION, 0, (int) (500 * power));
                    SpellEffects.buff(caster, PotionEffectType.WATER_BREATHING, 0, (int) (500 * power));
                });

        add("eau_moyen_1", Element.EAU, SpellTier.MOYEN, "Trombe d'Eau",
                "Une trombe d'eau voyageuse qui frappe et repousse la première cible touchée.",
                (plugin, caster, power) -> SpellEffects.launchProjectile(plugin, caster, 30, 1.4, 0.95,
                        5.0 * power, 0, Particle.SPLASH, Particle.DRIPPING_WATER,
                        Sound.ITEM_BUCKET_EMPTY, Sound.ENTITY_DOLPHIN_SPLASH));

        add("eau_moyen_2", Element.EAU, SpellTier.MOYEN, "Vague Assourdissante",
                "Une vraie vague part de toi, s'étend et repousse et ralentit tout ce qu'elle traverse.",
                (plugin, caster, power) -> {
                    SpellEffects.expandingWave(plugin, caster, caster.getLocation().add(0, 1, 0), 6.75, 4, 3,
                            3.0 * power, 1.3, Particle.SPLASH, Particle.BUBBLE_POP, Sound.ENTITY_DOLPHIN_SPLASH);
                    for (Entity nearby : caster.getNearbyEntities(6.75, 6.75, 6.75)) {
                        if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                            SpellEffects.debuff(target, PotionEffectType.SLOWNESS, amp(power), 75);
                        }
                    }
                });

        add("eau_moyen_3", Element.EAU, SpellTier.MOYEN, "Bulle Protectrice",
                "T'entoure d'une bulle qui absorbe les prochains dégâts.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.1f, 1.3f);
                    SpellEffects.shield(caster, amp(power) + 1, (int) (200 * power));
                    SpellEffects.followingAura(plugin, caster, Particle.BUBBLE_POP, (int) (200 * power), 0.95, 10);
                });

        add("eau_moyen_4", Element.EAU, SpellTier.MOYEN, "Cage de Glace",
                "Immobilise la cible visée dans un bloc de glace.",
                (plugin, caster, power) -> {
                    var eye = caster.getEyeLocation();
                    var result = caster.getWorld().rayTraceEntities(eye, eye.getDirection(), 13.5, 0.5,
                            e -> e instanceof LivingEntity && !e.equals(caster));
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.6f, 0.6f);
                    if (result != null && result.getHitEntity() instanceof LivingEntity target) {
                        SpellEffects.root(target, (int) (80 * power));
                        target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 40, 0.5, 0.75, 0.5, 0.025);
                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_PLACE, 1f, 0.8f);
                    }
                });

        add("eau_fort_1", Element.EAU, SpellTier.FORT, "Marée Destructrice",
                "Une vraie vague massive part de toi, grandit, repousse et endommage tout ce qu'elle traverse.",
                (plugin, caster, power) -> SpellEffects.expandingWave(plugin, caster, caster.getLocation().add(0, 1, 0), 9.4, 5, 3,
                        8.0 * power, 1.9, Particle.SPLASH, Particle.DRIPPING_WATER, Sound.ENTITY_DOLPHIN_SPLASH));

        add("eau_fort_2", Element.EAU, SpellTier.FORT, "Prison de Glace",
                "Gèle totalement la cible visée, l'empêchant d'agir un moment.",
                (plugin, caster, power) -> {
                    var eye = caster.getEyeLocation();
                    var result = caster.getWorld().rayTraceEntities(eye, eye.getDirection(), 17.4, 0.5,
                            e -> e instanceof LivingEntity && !e.equals(caster));
                    if (result != null && result.getHitEntity() instanceof LivingEntity target) {
                        SpellEffects.root(target, (int) (145 * power));
                        SpellEffects.debuff(target, PotionEffectType.WEAKNESS, amp(power), (int) (145 * power));
                        target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 70, 0.6, 0.95, 0.6, 0.035);
                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.2f, 0.7f);
                    }
                });

        add("eau_fort_3", Element.EAU, SpellTier.FORT, "Renouveau Océanique",
                "Un puissant soin qui purifie aussi les effets négatifs.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1.4f);
                    SpellEffects.heal(caster, 10.0 * power);
                    SpellEffects.risingSpiral(caster.getLocation(), Particle.SPLASH, 3.2, 1.0, 2);
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
                    SpellEffects.dash(caster, 1.9 * power, 0.2);
                    SpellEffects.dashTrail(plugin, caster, Particle.SPLASH, 29);
                    SpellEffects.buff(caster, PotionEffectType.SPEED, amp(power) + 1, 87);
                });

        add("eau_ultra_1", Element.EAU, SpellTier.ULTRA_FORT, "Déluge",
                "Une première vague massive s'abat puis une zone d'eau déchaînée inflige des dégâts continus pendant plusieurs secondes.",
                (plugin, caster, power) -> {
                    var center = SpellEffects.forwardPoint(caster, 8);
                    SpellEffects.groundRing(center, Particle.SPLASH, 8.8, 40);
                    SpellEffects.expandingWave(plugin, caster, center, 8.8, 5, 2, 4.0 * power, 1.6,
                            Particle.SPLASH, Particle.BUBBLE_POP, Sound.ENTITY_DOLPHIN_SPLASH);
                    SpellEffects.areaOverTime(plugin, caster, center, 8.8, 2.5 * power, 160, 20,
                            Particle.SPLASH, Sound.ENTITY_DOLPHIN_SPLASH);
                });

        add("eau_ultra_2", Element.EAU, SpellTier.ULTRA_FORT, "Léviathan des Abysses",
                "Invoque temporairement un allié aquatique pour combattre à tes côtés.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_DROWNED_AMBIENT, 1.3f, 0.8f);
                    SpellEffects.groundRing(caster.getLocation(), Particle.SPLASH, 3.2, 24);
                    SpellEffects.summonAlly(plugin, caster, EntityType.DROWNED, (int) (480 * power));
                });

        add("eau_ultra_3", Element.EAU, SpellTier.ULTRA_FORT, "Glaciation Totale",
                "Gèle tous les ennemis dans une large zone autour de toi.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.3f, 0.6f);
                    for (Entity nearby : caster.getNearbyEntities(12.8, 12.8, 12.8)) {
                        if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                            SpellEffects.root(target, (int) (160 * power));
                        }
                    }
                    caster.getWorld().spawnParticle(Particle.SNOWFLAKE, caster.getLocation(), 160, 6.4, 3.2, 6.4, 0.06);
                    SpellEffects.groundRing(caster.getLocation(), Particle.SNOWFLAKE, 12.8, 48);
                });

        add("eau_ultra_4", Element.EAU, SpellTier.ULTRA_FORT, "Cœur de l'Océan",
                "T'entoure d'un puissant bouclier et te soigne progressivement.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TOTEM_USE, 1.2f, 1.2f);
                    SpellEffects.shield(caster, amp(power) + 2, (int) (320 * power));
                    SpellEffects.buff(caster, PotionEffectType.REGENERATION, amp(power), (int) (320 * power));
                    SpellEffects.followingAura(plugin, caster, Particle.SPLASH, (int) (320 * power), 1.44, 12);
                });
    }

    // ------------------------------------------------------------- TERRE

    private void registerTerre() {
        add("terre_faible_1", Element.TERRE, SpellTier.FAIBLE, "Éclat de Pierre",
                "Projette un éclat de pierre tranchant qui voyage droit sur la cible.",
                (plugin, caster, power) -> SpellEffects.launchProjectile(plugin, caster, 22, 1.2, 0.7,
                        4.5 * power, 0, Particle.BLOCK_CRUMBLE, Particle.CRIT,
                        Sound.BLOCK_STONE_BREAK, Sound.BLOCK_STONE_HIT));

        add("terre_faible_2", Element.TERRE, SpellTier.FAIBLE, "Racines",
                "Fait surgir des racines qui immobilisent brièvement la cible visée.",
                (plugin, caster, power) -> {
                    var eye = caster.getEyeLocation();
                    var result = caster.getWorld().rayTraceEntities(eye, eye.getDirection(), 10, 0.5,
                            e -> e instanceof LivingEntity && !e.equals(caster));
                    if (result != null && result.getHitEntity() instanceof LivingEntity target) {
                        SpellEffects.root(target, (int) (38 * power));
                        target.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, target.getLocation(), 38, 0.35, 0.6, 0.35, 0.025,
                                org.bukkit.Material.DIRT.createBlockData());
                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GRASS_BREAK, 1f, 1f);
                    }
                });

        add("terre_faible_3", Element.TERRE, SpellTier.FAIBLE, "Peau de Roche",
                "Ta peau durcit temporairement, réduisant les dégâts reçus.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_STONE_PLACE, 1.1f, 0.9f);
                    SpellEffects.buff(caster, PotionEffectType.RESISTANCE, amp(power), 125);
                    SpellEffects.followingAura(plugin, caster, Particle.BLOCK_CRUMBLE, 125, 0.75, 8);
                });

        add("terre_faible_4", Element.TERRE, SpellTier.FAIBLE, "Odorat de la Terre",
                "Révèle brièvement les créatures proches à travers les obstacles.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_STONE_STEP, 1f, 0.7f);
                    for (Entity nearby : caster.getNearbyEntities(25, 25, 25)) {
                        if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                            SpellEffects.debuff(target, PotionEffectType.GLOWING, 0, (int) (125 * power));
                        }
                    }
                });

        add("terre_moyen_1", Element.TERRE, SpellTier.MOYEN, "Séisme Local",
                "Un choc sismique fait vaciller et endommage les ennemis proches.",
                (plugin, caster, power) -> SpellEffects.damageKnockbackAoe(caster, 6.1, 5.0 * power, 0.95,
                        Particle.BLOCK_CRUMBLE, Particle.CRIT, Sound.BLOCK_STONE_BREAK));

        add("terre_moyen_2", Element.TERRE, SpellTier.MOYEN, "Mur de Pierre",
                "Un mur de pierre s'érige devant toi, bloquant projectiles et ennemis un instant.",
                (plugin, caster, power) -> {
                    SpellEffects.shield(caster, amp(power) + 1, (int) (108 * power));
                    caster.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, SpellEffects.forwardPoint(caster, 2.7), 95, 1.3, 2.0, 0.4, 0,
                            org.bukkit.Material.STONE.createBlockData());
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_STONE_PLACE, 1.2f, 0.8f);
                });

        add("terre_moyen_3", Element.TERRE, SpellTier.MOYEN, "Carapace",
                "Une carapace de pierre t'offre une solide protection temporaire.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_STONE_PLACE, 1.3f, 0.7f);
                    SpellEffects.buff(caster, PotionEffectType.RESISTANCE, amp(power) + 1, (int) (135 * power));
                    SpellEffects.buff(caster, PotionEffectType.SLOWNESS, 0, (int) (135 * power));
                    SpellEffects.followingAura(plugin, caster, Particle.BLOCK_CRUMBLE, (int) (135 * power), 0.95, 10);
                });

        add("terre_moyen_4", Element.TERRE, SpellTier.MOYEN, "Charge du Golem",
                "Fonce en avant et percute violemment les ennemis sur ton passage.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.1f, 0.9f);
                    SpellEffects.dash(caster, 1.44 * power, 0.2);
                    SpellEffects.dashTrail(plugin, caster, Particle.BLOCK_CRUMBLE, 27);
                    SpellEffects.damageKnockbackAoe(caster, 4.1, 6.0 * power, 1.6, Particle.BLOCK_CRUMBLE, Particle.CRIT, Sound.BLOCK_STONE_BREAK);
                });

        add("terre_fort_1", Element.TERRE, SpellTier.FORT, "Tremblement de Terre",
                "Une secousse puissante endommage et déséquilibre tous les ennemis proches.",
                (plugin, caster, power) -> SpellEffects.damageKnockbackAoe(caster, 8.7, 9.0 * power, 1.15,
                        Particle.EXPLOSION, Particle.BLOCK_CRUMBLE, Sound.ENTITY_IRON_GOLEM_ATTACK));

        add("terre_fort_2", Element.TERRE, SpellTier.FORT, "Prison Rocheuse",
                "Emprisonne la cible visée dans la roche, l'immobilisant longuement.",
                (plugin, caster, power) -> {
                    var eye = caster.getEyeLocation();
                    var result = caster.getWorld().rayTraceEntities(eye, eye.getDirection(), 17.4, 0.5,
                            e -> e instanceof LivingEntity && !e.equals(caster));
                    if (result != null && result.getHitEntity() instanceof LivingEntity target) {
                        SpellEffects.root(target, (int) (145 * power));
                        target.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, target.getLocation().add(0, 1, 0), 70, 0.55, 0.95, 0.55, 0.045,
                                org.bukkit.Material.STONE.createBlockData());
                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_STONE_PLACE, 1.3f, 0.6f);
                    }
                });

        add("terre_fort_3", Element.TERRE, SpellTier.FORT, "Régénération Tellurique",
                "Puise dans la force de la terre pour te soigner fortement dans la durée.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
                    SpellEffects.heal(caster, 6.0 * power);
                    SpellEffects.buff(caster, PotionEffectType.REGENERATION, amp(power), (int) (145 * power));
                    SpellEffects.risingSpiral(caster.getLocation(), Particle.BLOCK_CRUMBLE, 3.2, 1.0, 2);
                });

        add("terre_fort_4", Element.TERRE, SpellTier.FORT, "Invocation de Golem",
                "Invoque un golem de fer temporaire pour combattre à tes côtés.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.3f, 0.7f);
                    SpellEffects.groundRing(caster.getLocation(), Particle.BLOCK_CRUMBLE, 2.9, 24);
                    SpellEffects.summonAlly(plugin, caster, EntityType.IRON_GOLEM, (int) (580 * power));
                });

        add("terre_ultra_1", Element.TERRE, SpellTier.ULTRA_FORT, "Cataclysme",
                "Une explosion tellurique dévastatrice autour de toi. Sort ultime, longue recharge.",
                (plugin, caster, power) -> {
                    SpellEffects.risingSpiral(caster.getLocation(), Particle.BLOCK_CRUMBLE, 4.8, 1.9, 3);
                    SpellEffects.damageKnockbackAoe(caster, 12.0, 17.0 * power, 1.75,
                            Particle.EXPLOSION_EMITTER, Particle.BLOCK_CRUMBLE, Sound.ENTITY_GENERIC_EXPLODE);
                });

        add("terre_ultra_2", Element.TERRE, SpellTier.ULTRA_FORT, "Avatar de Pierre",
                "Te transforme temporairement en colosse de pierre : résistance et force énormes.",
                (plugin, caster, power) -> {
                    int duration = (int) (224 * power);
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_STONE_PLACE, 1.4f, 0.6f);
                    SpellEffects.buff(caster, PotionEffectType.RESISTANCE, amp(power) + 1, duration);
                    SpellEffects.buff(caster, PotionEffectType.STRENGTH, amp(power), duration);
                    SpellEffects.followingAura(plugin, caster, Particle.BLOCK_CRUMBLE, duration, 1.28, 12);
                });

        add("terre_ultra_3", Element.TERRE, SpellTier.ULTRA_FORT, "Effondrement",
                "Le sol s'effondre après un court délai, assommant et blessant lourdement les ennemis proches.",
                (plugin, caster, power) -> {
                    SpellEffects.groundRing(caster.getLocation(), Particle.BLOCK_CRUMBLE, 10.4, 44);
                    SpellEffects.delayedExplodeAt(plugin, caster, caster.getLocation(),
                            30, 10.4, 14.0 * power, Particle.BLOCK_CRUMBLE, Sound.ENTITY_GENERIC_EXPLODE);
                });

        add("terre_ultra_4", Element.TERRE, SpellTier.ULTRA_FORT, "Cœur de la Montagne",
                "T'entoure d'un bouclier massif et réduit fortement les dégâts reçus un long moment.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_STONE_PLACE, 1.3f, 0.5f);
                    SpellEffects.shield(caster, amp(power) + 2, (int) (320 * power));
                    SpellEffects.buff(caster, PotionEffectType.RESISTANCE, amp(power) + 1, (int) (320 * power));
                    SpellEffects.followingAura(plugin, caster, Particle.BLOCK_CRUMBLE, (int) (320 * power), 1.44, 12);
                });
    }

    // -------------------------------------------------------------- VENT

    private void registerVent() {
        add("vent_faible_1", Element.VENT, SpellTier.FAIBLE, "Rafale",
                "Une rafale de vent qui voyage devant toi et repousse la cible touchée.",
                (plugin, caster, power) -> SpellEffects.launchProjectile(plugin, caster, 25, 1.6, 0.75,
                        3.0 * power, 0, Particle.CLOUD, Particle.SWEEP_ATTACK,
                        Sound.ENTITY_PHANTOM_FLAP, Sound.ENTITY_PLAYER_ATTACK_SWEEP));

        add("vent_faible_2", Element.VENT, SpellTier.FAIBLE, "Bond",
                "Un puissant saut propulsé par le vent.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.1f, 1.3f);
                    SpellEffects.dash(caster, 0.4, 1.2 * power);
                    SpellEffects.dashTrail(plugin, caster, Particle.CLOUD, 19);
                });

        add("vent_faible_3", Element.VENT, SpellTier.FAIBLE, "Pas Léger",
                "Un souffle de vent qui accélère tes mouvements.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_ELYTRA_FLYING, 0.8f, 1.4f);
                    SpellEffects.buff(caster, PotionEffectType.SPEED, amp(power), 150);
                    SpellEffects.dashTrail(plugin, caster, Particle.CLOUD, 31);
                });

        add("vent_faible_4", Element.VENT, SpellTier.FAIBLE, "Œil du Vent",
                "Le vent te renseigne brièvement sur la position des ennemis proches.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1.5f);
                    for (Entity nearby : caster.getNearbyEntities(19, 19, 19)) {
                        if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                            SpellEffects.debuff(target, PotionEffectType.GLOWING, 0, (int) (100 * power));
                        }
                    }
                });

        add("vent_moyen_1", Element.VENT, SpellTier.MOYEN, "Tornade Miniature",
                "Une petite tornade attire et endommage les ennemis proches.",
                (plugin, caster, power) -> SpellEffects.pullAoe(caster, 6.75, 4.0 * power, Particle.CLOUD, Sound.ENTITY_PHANTOM_FLAP));

        add("vent_moyen_2", Element.VENT, SpellTier.MOYEN, "Bourrasque",
                "Une bourrasque puissante repousse violemment tout ce qui t'entoure.",
                (plugin, caster, power) -> SpellEffects.damageKnockbackAoe(caster, 6.75, 3.0 * power, 2.5,
                        Particle.CLOUD, Particle.SWEEP_ATTACK, Sound.ENTITY_PLAYER_ATTACK_SWEEP));

        add("vent_moyen_3", Element.VENT, SpellTier.MOYEN, "Ailes du Vent",
                "Ralentit ta chute et te rend plus rapide un instant, comme si tu planais.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1f, 1.2f);
                    SpellEffects.buff(caster, PotionEffectType.SLOW_FALLING, 0, (int) (135 * power));
                    SpellEffects.buff(caster, PotionEffectType.SPEED, amp(power), 135);
                    SpellEffects.followingAura(plugin, caster, Particle.CLOUD, (int) (135 * power), 0.95, 10);
                });

        add("vent_moyen_4", Element.VENT, SpellTier.MOYEN, "Lame de Vent",
                "Une lame d'air tranchante voyage et frappe la première cible touchée.",
                (plugin, caster, power) -> SpellEffects.launchProjectile(plugin, caster, 32, 1.7, 0.8,
                        6.0 * power, 0, Particle.SWEEP_ATTACK, Particle.CLOUD,
                        Sound.ENTITY_PLAYER_ATTACK_SWEEP, Sound.ENTITY_PLAYER_ATTACK_SWEEP));

        add("vent_fort_1", Element.VENT, SpellTier.FORT, "Cyclone",
                "Un cyclone attire puis endommage continuellement les ennemis proches.",
                (plugin, caster, power) -> {
                    SpellEffects.pullAoe(caster, 9.4, 0, Particle.CLOUD, Sound.ENTITY_PHANTOM_FLAP);
                    SpellEffects.areaOverTime(plugin, caster, caster.getLocation(), 7.25, 2.0 * power, 87, 20,
                            Particle.CLOUD, Sound.ENTITY_PHANTOM_FLAP);
                });

        add("vent_fort_2", Element.VENT, SpellTier.FORT, "Éclair Fulgurant",
                "Un déplacement fulgurant qui endommage tout sur ton passage.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.3f, 1.2f);
                    SpellEffects.dash(caster, 2.6 * power, 0.15);
                    SpellEffects.dashTrail(plugin, caster, Particle.CLOUD, 22);
                    SpellEffects.damageAoe(caster, 4.35, 6.0 * power, Particle.CLOUD, Sound.ENTITY_PHANTOM_FLAP);
                });

        add("vent_fort_3", Element.VENT, SpellTier.FORT, "Bouclier Aérien",
                "Un mur d'air te protège des projectiles et des chocs pendant un moment.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1.1f, 1f);
                    SpellEffects.shield(caster, amp(power) + 1, (int) (215 * power));
                    SpellEffects.followingAura(plugin, caster, Particle.CLOUD, (int) (215 * power), 1.16, 10);
                });

        add("vent_fort_4", Element.VENT, SpellTier.FORT, "Chant des Tempêtes",
                "Affaiblit et ralentit tous les ennemis dans une large zone.",
                (plugin, caster, power) -> {
                    caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.3f, 0.7f);
                    SpellEffects.groundRing(caster.getLocation(), Particle.CLOUD, 10.15, 40);
                    for (Entity nearby : caster.getNearbyEntities(10.15, 10.15, 10.15)) {
                        if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                            SpellEffects.debuff(target, PotionEffectType.SLOWNESS, amp(power), (int) (145 * power));
                            SpellEffects.debuff(target, PotionEffectType.WEAKNESS, amp(power), (int) (145 * power));
                        }
                    }
                });

        add("vent_ultra_1", Element.VENT, SpellTier.ULTRA_FORT, "Tempête Dévastatrice",
                "Une tempête déchaînée inflige des dégâts continus et repousse tout autour de toi.",
                (plugin, caster, power) -> {
                    SpellEffects.damageKnockbackAoe(caster, 9.6, 8.0 * power, 1.9,
                            Particle.CLOUD, Particle.SWEEP_ATTACK, Sound.ENTITY_PLAYER_ATTACK_SWEEP);
                    SpellEffects.areaOverTime(plugin, caster, caster.getLocation(), 9.6, 2.5 * power, 160, 20,
                            Particle.CLOUD, Sound.ENTITY_PHANTOM_FLAP);
                });

        add("vent_ultra_2", Element.VENT, SpellTier.ULTRA_FORT, "Avatar du Vent",
                "Te transforme temporairement en tourbillon : vitesse et saut extrêmes.",
                (plugin, caster, power) -> {
                    int duration = (int) (224 * power);
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1.3f, 1.3f);
                    SpellEffects.buff(caster, PotionEffectType.SPEED, amp(power) + 1, duration);
                    SpellEffects.buff(caster, PotionEffectType.JUMP_BOOST, amp(power) + 1, duration);
                    SpellEffects.buff(caster, PotionEffectType.SLOW_FALLING, 0, duration);
                    SpellEffects.followingAura(plugin, caster, Particle.CLOUD, duration, 1.28, 12);
                });

        add("vent_ultra_3", Element.VENT, SpellTier.ULTRA_FORT, "Œil du Cyclone",
                "Aspire violemment tous les ennemis proches vers toi avant de les frapper.",
                (plugin, caster, power) -> {
                    SpellEffects.pullAoe(caster, 12.8, 0, Particle.CLOUD, Sound.ENTITY_PHANTOM_FLAP);
                    SpellEffects.delayedExplodeAt(plugin, caster, caster.getLocation(), 15, 6.4, 12.0 * power,
                            Particle.EXPLOSION, Sound.ENTITY_GENERIC_EXPLODE);
                });

        add("vent_ultra_4", Element.VENT, SpellTier.ULTRA_FORT, "Souffle des Cieux",
                "T'octroie une mobilité aérienne exceptionnelle pendant une longue durée.",
                (plugin, caster, power) -> {
                    int duration = (int) (320 * power);
                    caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_ELYTRA_FLYING, 1.3f, 1f);
                    SpellEffects.buff(caster, PotionEffectType.SLOW_FALLING, 0, duration);
                    SpellEffects.buff(caster, PotionEffectType.SPEED, amp(power) + 1, duration);
                    SpellEffects.buff(caster, PotionEffectType.JUMP_BOOST, amp(power) + 1, duration);
                    SpellEffects.dash(caster, 0.5, 1.6 * power);
                    SpellEffects.followingAura(plugin, caster, Particle.CLOUD, duration, 1.44, 12);
                });
    }
}
