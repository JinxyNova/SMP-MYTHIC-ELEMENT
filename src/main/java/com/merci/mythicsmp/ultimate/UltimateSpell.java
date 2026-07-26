package com.merci.mythicsmp.ultimate;

import com.merci.mythicsmp.spells.SpellEffect;
import com.merci.mythicsmp.spells.SpellEffects;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Les 5 sorts réservés à la classe Ultime Mage (voir UltimateMageManager) :
 * 4 sorts de fusion combinant les 4 éléments en même temps (Feu, Eau, Terre,
 * Vent), pensés pour être parmi les plus puissants du jeu — assez pour
 * abattre en un seul coup une cible aussi résistante que l'Elder Guardian
 * (80 PV, sans armure) — plus un sort de soin instantané.
 *
 * Chaque sort est mis en scène en plusieurs actes (charge -> relâche ->
 * rémanence) via UltimateVisuals plutôt qu'un simple nuage de particules
 * ponctuel : ce sont les sorts les plus rares du serveur, ils doivent se
 * voir et s'entendre en conséquence.
 *
 * Ne fait volontairement pas partie du système Element/Spell existant (qui
 * repose sur 4 éléments distincts choisis un par un) : cette classe est une
 * récompense à part, débloquée une fois les 4 éléments déjà maîtrisés (voir
 * UltimateMageManager / UltimateMageGemItem).
 */
public enum UltimateSpell {

    FUSION_CATACLYSME(
            "fusion_cataclysme",
            "Cataclysme des Quatre Éléments",
            "Déchaîne les 4 éléments en une explosion dévastatrice autour de toi.",
            Material.NETHER_STAR,
            90,
            (plugin, caster, power) -> {
                // Acte 1 : la terre gronde, une colonne de particules des 4 éléments monte
                // en tournant autour du lanceur pendant 25 ticks (1.25s) tandis que le son
                // enfle. Acte 2 : tout explose d'un coup, en plusieurs anneaux concentriques.
                caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.6f);
                caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.5f);

                new BukkitRunnable() {
                    int tick = 0;
                    final int duration = 34;

                    @Override
                    public void run() {
                        if (!caster.isOnline()) {
                            cancel();
                            return;
                        }
                        if (tick >= duration) {
                            cancel();
                            unleashCataclysme(plugin, caster);
                            return;
                        }
                        Location base = caster.getLocation();
                        double progress = tick / (double) duration;
                        double height = progress * 3.2;
                        double angle = tick * 0.8;
                        double radius = 2.1 * (1.0 - 0.3 * progress);
                        Particle[] elements = {Particle.FLAME, Particle.SPLASH, Particle.BLOCK_CRUMBLE, Particle.CLOUD};
                        for (int i = 0; i < elements.length; i++) {
                            double a = angle + (2 * Math.PI / elements.length) * i;
                            double x = base.getX() + radius * Math.cos(a);
                            double z = base.getZ() + radius * Math.sin(a);
                            // BLOCK_CRUMBLE a besoin d'une BlockData, sinon le serveur lève une
                            // exception qui annule la tâche entière (et donc l'explosion finale
                            // ne se déclenchait jamais) : c'était le bug qui cassait ce sort.
                            if (elements[i] == Particle.BLOCK_CRUMBLE) {
                                base.getWorld().spawnParticle(elements[i], x, base.getY() + height, z, 1,
                                        org.bukkit.Material.STONE.createBlockData());
                            } else {
                                base.getWorld().spawnParticle(elements[i], x, base.getY() + height, z, 1, 0, 0, 0, 0);
                            }
                        }
                        if (tick % 4 == 0) {
                            base.getWorld().playSound(base, Sound.BLOCK_BEACON_AMBIENT, 0.6f, 0.7f + (float) progress);
                        }
                        tick += 2;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
    ),

    FUSION_JUGEMENT(
            "fusion_jugement",
            "Jugement des Origines",
            "Concentre les 4 éléments en un rayon unique, ravageur, sur la cible visée.",
            Material.END_ROD,
            90,
            (plugin, caster, power) -> {
                // Acte 1 : une petite sphère d'énergie se concentre devant les yeux du
                // lanceur (18 ticks) pendant que le tonnerre monte en aigu. Acte 2 : le
                // rayon part d'un coup, ponctué d'anneaux d'impact tout le long, et
                // explose en un mélange des 4 éléments là où il touche.
                new BukkitRunnable() {
                    int tick = 0;
                    final int duration = 24;

                    @Override
                    public void run() {
                        if (!caster.isOnline()) {
                            cancel();
                            return;
                        }
                        if (tick >= duration) {
                            cancel();
                            unleashJugement(caster);
                            return;
                        }
                        Location focus = SpellEffects.forwardPoint(caster, 1.8);
                        double progress = tick / (double) duration;
                        double radius = 0.7 * (1.0 - progress) + 0.05;
                        for (int i = 0; i < 6; i++) {
                            double angle = tick * 1.1 + (2 * Math.PI / 6) * i;
                            double x = focus.getX() + radius * Math.cos(angle);
                            double y = focus.getY() + radius * Math.sin(angle) * 0.6;
                            double z = focus.getZ() + radius * Math.sin(angle);
                            focus.getWorld().spawnParticle(Particle.END_ROD, x, y, z, 1, 0, 0, 0, 0);
                        }
                        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME,
                                0.5f, 0.5f + (float) progress * 1.5f);
                        tick += 2;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
    ),

    FUSION_TEMPETE(
            "fusion_tempete",
            "Tempête Primordiale",
            "Attire tes ennemis, les brûle, les gèle et les écrase en une seule vague.",
            Material.DRAGON_BREATH,
            90,
            (plugin, caster, power) -> {
                // Acte 1 : un vortex de nuages et de flocons se forme au-dessus du
                // lanceur pendant que le rugissement du dragon enfle (24 ticks). Acte 2 :
                // le vortex attire tout le monde d'un coup, embrase, immobilise puis
                // relâche une onde glacée qui écrase.
                caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 0.5f);

                new BukkitRunnable() {
                    int tick = 0;
                    final int duration = 32;

                    @Override
                    public void run() {
                        if (!caster.isOnline()) {
                            cancel();
                            return;
                        }
                        if (tick >= duration) {
                            cancel();
                            unleashTempete(plugin, caster);
                            return;
                        }
                        Location base = caster.getLocation().add(0, 2.2, 0);
                        double progress = tick / (double) duration;
                        double radius = 3.3 * (1.0 - progress) + 0.3;
                        double angle = tick * 1.3;
                        for (int arm = 0; arm < 4; arm++) {
                            double a = angle + (2 * Math.PI / 4) * arm;
                            double x = base.getX() + radius * Math.cos(a);
                            double z = base.getZ() + radius * Math.sin(a);
                            base.getWorld().spawnParticle(arm % 2 == 0 ? Particle.CLOUD : Particle.SNOWFLAKE,
                                    x, base.getY(), z, 1, 0, 0, 0, 0);
                        }
                        tick += 2;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
    ),

    FUSION_CONVERGENCE(
            "fusion_convergence",
            "Convergence Élémentaire",
            "Frappe tout autour de toi de plein fouet puis t'octroie la puissance des 4 éléments.",
            Material.BEACON,
            90,
            (plugin, caster, power) -> {
                // Acte 1 : 4 anneaux colorés (un par élément) spiralent VERS le lanceur
                // en se rapprochant et en montant (20 ticks) : la puissance converge.
                // Acte 2 : tout part d'un coup vers l'extérieur (choc + buffs), avec des
                // anneaux qui repartent en sens inverse pour bien montrer le "relâché".
                caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 0.7f);

                Particle.DustOptions fire = UltimateVisuals.dust(Color.fromRGB(255, 100, 20), 1.1f);
                Particle.DustOptions water = UltimateVisuals.dust(Color.fromRGB(50, 130, 255), 1.1f);
                Particle.DustOptions earth = UltimateVisuals.dust(Color.fromRGB(110, 80, 40), 1.1f);
                Particle.DustOptions wind = UltimateVisuals.dust(Color.fromRGB(220, 255, 240), 1.1f);
                Particle.DustOptions[] colors = {fire, water, earth, wind};

                new BukkitRunnable() {
                    int tick = 0;
                    final int duration = 27;

                    @Override
                    public void run() {
                        if (!caster.isOnline()) {
                            cancel();
                            return;
                        }
                        if (tick >= duration) {
                            cancel();
                            unleashConvergence(plugin, caster, colors);
                            return;
                        }
                        Location base = caster.getLocation();
                        double progress = tick / (double) duration;
                        double radius = 5.2 * (1.0 - progress) + 0.4;
                        double height = 2.1 * progress;
                        double angle = -tick * 0.9;
                        for (int i = 0; i < colors.length; i++) {
                            double a = angle + (2 * Math.PI / colors.length) * i;
                            double x = base.getX() + radius * Math.cos(a);
                            double z = base.getZ() + radius * Math.sin(a);
                            base.getWorld().spawnParticle(Particle.DUST, x, base.getY() + height, z, 1, 0, 0, 0, 0, colors[i]);
                        }
                        tick += 2;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
    ),

    SOIN(
            "fusion_soin",
            "Bénédiction des Éléments",
            "Te soigne instantanément et intégralement, et purifie les effets négatifs.",
            Material.TOTEM_OF_UNDYING,
            45,
            (plugin, caster, power) -> {
                // Une colonne de lumière douce monte pendant que le carillon enfle
                // (12 ticks), puis un pilier de coeurs/END_ROD éclate vers le haut au
                // moment précis où le soin et la purification s'appliquent.
                Particle.DustOptions gold = UltimateVisuals.dust(Color.fromRGB(255, 230, 140), 1.3f);

                new BukkitRunnable() {
                    int tick = 0;
                    final int duration = 16;

                    @Override
                    public void run() {
                        if (!caster.isOnline()) {
                            cancel();
                            return;
                        }
                        if (tick >= duration) {
                            cancel();
                            unleashSoin(plugin, caster, gold);
                            return;
                        }
                        Location base = caster.getLocation();
                        double progress = tick / (double) duration;
                        double height = progress * 2.4;
                        double radius = 0.9;
                        double angle = tick * 0.9;
                        for (int i = 0; i < 5; i++) {
                            double a = angle + (2 * Math.PI / 5) * i;
                            double x = base.getX() + radius * Math.cos(a);
                            double z = base.getZ() + radius * Math.sin(a);
                            base.getWorld().spawnParticle(Particle.DUST, x, base.getY() + height, z, 1, 0, 0, 0, 0, gold);
                        }
                        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME,
                                0.6f, 0.9f + (float) progress);
                        tick += 1;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
    );

    private final String id;
    private final String displayName;
    private final String description;
    private final Material icon;
    private final int cooldownSeconds;
    private final SpellEffect effect;

    UltimateSpell(String id, String displayName, String description, Material icon, int cooldownSeconds, SpellEffect effect) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.cooldownSeconds = cooldownSeconds;
        this.effect = effect;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public SpellEffect getEffect() {
        return effect;
    }

    public static NamedTextColor color() {
        return NamedTextColor.LIGHT_PURPLE;
    }

    public void cast(Plugin plugin, Player caster) {
        effect.cast(plugin, caster, 1.0);
    }

    // ---------------------------------------------------------------
    // Actes de relâche : ce que déclenche chaque sort une fois sa charge terminée.
    // ---------------------------------------------------------------

    private static void unleashCataclysme(Plugin plugin, Player caster) {
        Location origin = caster.getLocation().add(0, 1, 0);
        caster.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, origin, 4, 1.3, 0.7, 1.3, 0);
        caster.getWorld().spawnParticle(Particle.FLAME, origin, 95, 2.6, 1.3, 2.6, 0.08);
        caster.getWorld().spawnParticle(Particle.SPLASH, origin, 95, 2.6, 1.3, 2.6, 0.08);
        // BLOCK_CRUMBLE a besoin d'une BlockData, sinon l'appel lève une exception qui
        // annule cette méthode (dégâts compris) : c'était le bug qui empêchait le
        // Cataclysme d'infliger ses dégâts et son onde de choc.
        caster.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, origin, 95, 2.6, 1.3, 2.6, 0.08, org.bukkit.Material.STONE.createBlockData());
        caster.getWorld().spawnParticle(Particle.CLOUD, origin, 95, 2.6, 1.3, 2.6, 0.08);
        caster.getWorld().playSound(origin, Sound.ENTITY_WITHER_DEATH, 1.3f, 0.8f);
        caster.getWorld().playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);
        // 4 anneaux qui s'étendent en séquence, plus loin et plus longtemps qu'avant.
        UltimateVisuals.shockwave(plugin, origin, Particle.EXPLOSION, 10.5, 4, 3);
        SpellEffects.damageKnockbackAoe(caster, 10.5, 110.0, 2.3, Particle.EXPLOSION);
    }

    private static void unleashJugement(Player caster) {
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.4f, 1f);
        Location eye = caster.getEyeLocation();
        UltimateVisuals.beamRings(eye, eye.getDirection(), 29, 0.6, Particle.END_ROD);
        boolean hit = SpellEffects.hitscan(caster, 29, 130.0, Particle.END_ROD);
        Location impact = SpellEffects.forwardPoint(caster, hit ? 4 : 26);
        caster.getWorld().spawnParticle(Particle.FLAME, impact, 48, 0.8, 0.8, 0.8, 0.04);
        caster.getWorld().spawnParticle(Particle.SPLASH, impact, 48, 0.8, 0.8, 0.8, 0.04);
        // BLOCK_CRUMBLE a besoin d'une BlockData, sinon l'appel lève une exception.
        caster.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, impact, 32, 0.8, 0.8, 0.8, 0.04, org.bukkit.Material.STONE.createBlockData());
        caster.getWorld().spawnParticle(Particle.CLOUD, impact, 32, 0.8, 0.8, 0.8, 0.04);
        UltimateVisuals.ring(impact, 2.4, 20, Particle.END_ROD, 0);
        caster.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.1f);
    }

    private static void unleashTempete(Plugin plugin, Player caster) {
        Location origin = caster.getLocation().add(0, 1, 0);
        caster.getWorld().playSound(origin, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 1f);
        SpellEffects.pullAoe(caster, 9.8, 0, Particle.CLOUD);
        for (Entity nearby : caster.getNearbyEntities(9.8, 9.8, 9.8)) {
            if (nearby instanceof LivingEntity target && !target.equals(caster)) {
                target.setFireTicks(130);
                SpellEffects.root(target, 80);
            }
        }
        // Écrasement différé d'une demi-seconde, le temps que la traction fasse effet,
        // avec deux ondes de choc alternées feu/glace, puis une vraie vague qui part du
        // joueur et s'étend progressivement (au lieu d'un simple nuage figé sur place).
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!caster.isOnline()) return;
                Location center = caster.getLocation().add(0, 1, 0);
                UltimateVisuals.shockwave(plugin, center, Particle.FLAME, 9.8, 3, 2);
                UltimateVisuals.shockwave(plugin, center, Particle.SNOWFLAKE, 9.8, 3, 2);
                SpellEffects.expandingWave(plugin, caster, center, 9.8, 5, 2, 95.0, 1.6,
                        Particle.SNOWFLAKE, Particle.CLOUD, Sound.ENTITY_GENERIC_EXPLODE);
            }
        }.runTaskLater(plugin, 10L);
    }

    private static void unleashConvergence(Plugin plugin, Player caster, Particle.DustOptions[] colors) {
        Location origin = caster.getLocation().add(0, 1, 0);
        caster.getWorld().spawnParticle(Particle.END_ROD, origin, 110, 2.0, 1.3, 2.0, 0.1);
        caster.getWorld().playSound(origin, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.4f, 1.1f);
        caster.getWorld().playSound(origin, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1f);
        SpellEffects.damageKnockbackAoe(caster, 9.2, 105.0, 1.8, Particle.CLOUD);
        // Les 4 anneaux élémentaires repartent vers l'extérieur, en sens inverse de la charge.
        for (Particle.DustOptions color : colors) {
            UltimateVisuals.shockwaveDust(plugin, origin, color, 7.9, 4, 2);
        }
        SpellEffects.buff(caster, PotionEffectType.STRENGTH, 2, 260);
        SpellEffects.buff(caster, PotionEffectType.SPEED, 1, 260);
        SpellEffects.buff(caster, PotionEffectType.RESISTANCE, 1, 260);
        SpellEffects.buff(caster, PotionEffectType.FIRE_RESISTANCE, 0, 260);
    }

    private static void unleashSoin(Plugin plugin, Player caster, Particle.DustOptions gold) {
        Location origin = caster.getLocation().add(0, 1.2, 0);
        caster.getWorld().spawnParticle(Particle.HEART, origin, 42, 0.8, 0.8, 0.8, 0.06);
        caster.getWorld().spawnParticle(Particle.END_ROD, origin, 55, 0.55, 1.3, 0.55, 0.06);
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1.2f);
        UltimateVisuals.shockwaveDust(plugin, caster.getLocation(), gold, 5.2, 4, 2);
        SpellEffects.heal(caster, 9999.0);
        for (var effect : java.util.List.copyOf(caster.getActivePotionEffects())) {
            if (effect.getType().equals(PotionEffectType.POISON)
                    || effect.getType().equals(PotionEffectType.WITHER)
                    || effect.getType().equals(PotionEffectType.SLOWNESS)
                    || effect.getType().equals(PotionEffectType.WEAKNESS)
                    || effect.getType().equals(PotionEffectType.BLINDNESS)) {
                caster.removePotionEffect(effect.getType());
            }
        }
    }
}
