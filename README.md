# MythicSMP

Plugin Paper pour serveur SMP : 29 armes et objets uniques.

## Ce qui est dedans

**Armes de base**
- 3 lances lançables : `lance_fer`, `lance_netherite`, `lance_celeste` (dégâts et cooldown croissants)
- `epee_feu` — Épée du Brasier : enflamme la cible + anneau de feu au sol
- `epee_glace` — Épée du Frimas : ralentit fortement la cible + gèle l'eau autour

**Objets mythiques de base**
- `gemme_feu` / `gemme_glace` — à fusionner avec l'épée correspondante pour renforcer ses effets
- `tete_admin` — casque cosmétique, régénération passive tant qu'il est porté
- `coeur_phenix` — ressuscite une fois avant une mort fatale, se consume après usage

**Combat corps-à-corps** (voir `CombatEffectsListener`)
- `marteau_guerre` — repousse les ennemis proches à chaque coup
- `faux_moisson` — bonus d'XP + chance de loot rare/épique à chaque kill
- `hache_tonnerre` — chance d'invoquer la foudre sur la cible
- `dague_poison` — empoisonne, le poison s'accumule à chaque coup
- `bouclier_miroir` — renvoie une partie des dégâts en bloquant
- `fouet` — attire la cible visée vers toi
- `gantelet_explosif` — chaque coup à mains nues crée une mini-explosion sans casser de blocs

**Distance**
- `arc_vent` — les flèches tirées repoussent violemment leur cible (voir `RangedEffectsListener`)

**Objets à lancer** (voir `ThrowableItemsListener`)
- `bombe_gel` — gèle l'eau et ralentit à l'impact
- `grenade_fumigene` — nuage de fumée qui aveugle
- `totem_soin` — zone de soin temporaire pour toi et tes alliés

**Utilitaires actifs** (voir `UtilityItemsListener`)
- `ancre_teleportation` — sneak+clic = pose un point de retour, clic normal = y retourner
- `boussole_tresor` — pointe vers la structure connue la plus proche
- `grappin` — te propulse vers le bloc visé
- `baton_teleportation_ennemi` — téléporte un joueur touché à un endroit aléatoire proche
- `fragment_etoile` — se consume pour donner un objet légendaire aléatoire
- `sac_rangement` — ouvre un coffre personnel de 36 cases (voir limite plus bas)
- `oeil_wither` — rend les entités proches visibles à travers les murs quelques secondes (voir `WitherEyeListener`)

**Passifs / portés** (voir `PassiveEquipmentTask`)
- `amulette_temps` — ralentit les mobs proches tant qu'elle est dans l'inventaire
- `couronne_roi` — booste la chance/XP des alliés proches tant qu'elle est portée
- `lanterne_ame` — repousse les mobs hostiles à proximité
- `elytres_vent` — boost de vitesse régulier en vol plané

## Ce qui n'est PAS dans ce lot

Tout ce qui était sur ta liste de départ est fait ! Les 30 idées d'origine sont soit un objet, soit intégrées dans un des 3 systèmes (forge, boss, marché) ou dans les quêtes ci-dessous.

## La classe Ultime Mage (sorts de fusion des 4 éléments)

Une fois les 4 éléments déjà maîtrisés (voir plus haut), tuer Le Gardien Écarlate avec un niveau d'XP (barre d'XP au-dessus de la barre d'inventaire) ≥ **445** fait apparaître une **Gemme du Mage Ultime** (`gemme_mage_ultime`), en plus de la Gemme au Pouvoir Infini qui apparaît elle dès 300.

- Clic droit sur la gemme : débloque directement la classe **Ultime Mage** et ses **5 sorts** d'un coup (pas de choix, contrairement aux éléments) — voir `UltimateMageGemListener` / `UltimateMageManager`.
- `/mythicultimate` (permission `mythicsmp.ultimate`, accessible à tous mais inutile sans la classe) ouvre le menu des 5 sorts :
  - **4 sorts de fusion** combinant les 4 éléments à la fois (Cataclysme des Quatre Éléments, Jugement des Origines, Tempête Primordiale, Convergence Élémentaire) — extrêmement puissants (dégâts ≥ 90-130, de quoi one-shot même l'Elder Guardian, 80 PV).
  - **1 sort de soin instantané** (Bénédiction des Éléments) : soin complet immédiat + purge des effets négatifs.
  - Chaque sort a son propre cooldown (voir `UltimateSpell.java` pour ajuster dégâts/cooldowns).
- Tout est géré dans le package `ultimate/` (`UltimateMageManager`, `UltimateSpell`, `UltimateSpellMenuManager`) + `gui/UltimateSpellMenuHolder` et `listeners/UltimateMageGemListener` / `UltimateSpellMenuListener`.

### Sort admin : Destruction Absolue

`/mythicadminspell` (permission `mythicsmp.adminspell`, OP par défaut) est un sort à part, réservé aux administrateurs : il n'existe ni objet ni classe pour l'obtenir, la commande **est** le sort — tape-la et tout ce qui est vivant dans un rayon de 40 blocs autour de toi (sauf toi) est instantanément anéanti (`setHealth(0)`, ignore résistances et Totem of Undying). Voir `MythicAdminSpellCommand.java`.

## Les Quêtes Journalières

`/mythicquests` (permission `mythicsmp.quests`, accessible à tous) affiche les 3 quêtes du jour et ta progression.

- 3 quêtes tirées au hasard chaque jour parmi 6 possibles (`quests/QuestPool.java`), les mêmes pour tout le serveur ce jour-là
- Progression suivie automatiquement : tuer des monstres, miner du fer/diamant, infliger des dégâts avec une arme mythique, vendre au Marché Mythique, réussir une amélioration à la Forge
- Récompense en pièces (le même système que le Marché Mythique) versée automatiquement dès qu'une quête est terminée
- Progression et quêtes du jour sauvegardées dans `quests.yml`, réinitialisées automatiquement au changement de jour (date du serveur)

Pour ajouter une quête au pool : une `QuestDefinition` de plus dans `QuestPool.java`. Si elle utilise un type déjà branché (tuer, miner, dégâts, vente, forge), elle fonctionne immédiatement sans toucher au reste du code — sinon il faut ajouter le déclenchement correspondant dans `QuestProgressListener` (ou à l'endroit de l'action, comme fait pour la vente et la forge).

## Le Marché Mythique (économie entre joueurs)

Trois commandes, toutes en `mythicsmp.auction` (accessible à tous par défaut) :

- `/mythicsell <prix>` — met en vente l'objet tenu en main (toute la pile), le retire de ton inventaire
- `/mythicauction` — ouvre le marché en GUI, paginé automatiquement au-delà de 45 annonces. Clic sur une annonce d'un autre joueur = achat immédiat si tu as les fonds. Clic sur ta propre annonce = récupération gratuite (annulation)
- `/mythicbalance` — affiche ton solde

**La monnaie** est interne au plugin (pas de dépendance à Vault ou à un autre plugin d'économie) : tout le monde commence à 100 pièces, gagnées/dépensées uniquement via le marché pour l'instant. Fichiers de sauvegarde dans le dossier de données du plugin : `balances.yml` (soldes) et `auctions.yml` (annonces actives, y compris l'objet complet — donc rien n'est perdu si le serveur redémarre pendant qu'une annonce est en ligne).

Si tu veux relier ces pièces à d'autres sources (récompense de quête, vente de récolte, etc.), le point d'entrée est `EconomyManager.deposit(uuid, montant)` — appelable depuis n'importe quel autre listener.

## Le boss : Le Gardien Écarlate

`/mythicboss` (permission `mythicsmp.boss`, OP par défaut) l'invoque à ta position.

- Un seul exemplaire peut être en vie à la fois sur tout le serveur (évite le spam)
- 200 PV, dégâts renforcés, épée de netherite en main
- Barre de vie visible pour tous les joueurs à moins de 40 blocs
- Toutes les 3 secondes environ : 25% de chance de foudroyer un joueur proche au hasard, 15% de chance d'invoquer 2 zombified piglins en renfort
- À sa mort : loot garanti d'un objet Légendaire ou Mythique du registre, message annoncé à tout le serveur, 50 XP

Pour changer le boss plus tard (santé, apparence, capacités) : tout est dans `boss/BossManager.java` (stats de spawn) et `boss/BossAbilityTask.java` (les capacités pendant le combat). Pour ajouter un deuxième boss différent, dupliquer ces deux classes avec un autre `NamespacedKey` et une autre commande.

## La Forge Mythique

Clic droit sur une **table de forge (Smithing Table)** → ouvre notre menu à la place du menu vanilla.

- Case du milieu (rangée 2) : l'objet à améliorer
- Deux cases à côté : les matériaux requis
- L'enclume à droite : valide la recette

Deux types de recettes (voir `ForgeRecipeRegistry.java`) :
- **Progression de tier** : `lance_fer` → `lance_netherite` (2 débris de netherite + 4 lingots d'or), puis `lance_netherite` → `lance_celeste` (1 étoile du Nether + 4 diamants)
- **Amélioration en place** (3 diamants + 1 lingot de netherite) pour les 8 armes de combat restantes : marteau, faux, hache tonnerre, dague, bouclier miroir, fouet, gantelet, arc du vent — chacune devient strictement plus forte (knockback, chance de foudre, poison, reflet de dégâts, etc., voir le code de `CombatEffectsListener`/`RangedEffectsListener` pour le détail des valeurs)

Les épées élémentaires ne passent pas par la forge : elles gardent leur propre système de fusion de gemmes vu plus haut, pour ne pas avoir deux façons différentes de faire la même chose sur le même objet.

Pour ajouter une recette de tier plus tard : un `ForgeRecipe` de plus dans `ForgeRecipeRegistry`. Pour rendre un nouvel objet améliorable en place : ajouter son id dans `GENERIC_UPGRADE_IDS` du même fichier, puis lire `MythicItem.upgradedKey(plugin)` dans le listener concerné pour scaler l'effet.

## Commandes

- `/mythicgive <joueur> <id> [quantité]` — donne un objet (permission `mythicsmp.give`, OP par défaut)
- `/mythiclist` — liste tous les ids disponibles (accessible à tous)
- `/mythicboss` — invoque Le Gardien Écarlate (permission `mythicsmp.boss`, OP par défaut)
- `/mythicsell <prix>`, `/mythicauction`, `/mythicbalance` — voir la section Marché Mythique plus bas
- `/mythicquests` — voir la section Quêtes Journalières plus bas

## Comment fusionner une gemme avec une épée

1. Tiens la gemme en main principale
2. Mets l'épée élémentaire correspondante en main secondaire (offhand, touche F par défaut)
3. Sneak (accroupi) + clic droit

## Compiler le plugin (sans rien installer chez toi)

Comme pour ton projet précédent, tout passe par GitHub Actions :

1. Crée un repo GitHub et mets-y tout ce dossier (`pom.xml`, `.github/`, `src/`)
2. Push sur la branche `main`
3. Onglet **Actions** du repo → l'action "Build MythicSMP" tourne automatiquement
4. Une fois terminée (icône verte), ouvre le run → section **Artifacts** en bas de page → télécharge `MythicSMP-plugin`, ça contient `MythicSMP.jar`
5. Mets ce `.jar` dans le dossier `plugins/` de ton serveur Paper, relance le serveur

## Limite connue : le Sac de Rangement

Son contenu est gardé en mémoire tant que le serveur tourne, mais **n'est pas sauvegardé sur disque** — un redémarrage du serveur le vide. Le brancher sur une vraie sauvegarde (fichier ou base de données) serait une bonne prochaine étape si tu comptes l'utiliser sérieusement.

## Où ajouter un nouvel objet plus tard

1. Crée sa classe dans `items/weapons/` ou `items/mythic/` (copie une classe existante comme modèle, ex: `FireSwordItem.java`)
2. Ajoute une ligne `register(new TonNouvelItem(plugin));` dans `ItemRegistry.java`
3. Si l'objet a un comportement actif (au clic, à l'impact, etc.), ajoute la logique dans un listener existant ou crée-en un nouveau dans `listeners/` et enregistre-le dans `MythicSMP.onEnable()`

Rien d'autre à toucher — le registre et les commandes s'adaptent automatiquement.

## Notes techniques

- Ciblé pour Paper API `1.21.4` (compatible 26.2, l'API plugin n'a pas changé depuis la 1.21 selon Mojang)
- Tout l'état des objets (id, amélioration de gemme) est stocké en `PersistentDataContainer`, jamais dans le nom affiché — donc renommer un item en enclume ne casse rien
- Si le premier build GitHub Actions échoue, regarde le log de l'étape "Build with Maven" : ce sera probablement un nom de particule ou d'enchantement à ajuster selon la version exacte de Paper que tu utilises (ces noms bougent parfois d'une version à l'autre)
