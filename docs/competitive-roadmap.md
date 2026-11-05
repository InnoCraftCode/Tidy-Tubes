# Tidy Tubes - feuille de route "jeu compétitif" (4 axes)

Date : 2026-11-04. Demande utilisateur : gameplay satisfaisant, carte du monde qui évolue, direction
artistique unifiée (personnages, pièces 2D/3D), boutique complète (pièces, boosters, packs, récompenses
quotidiennes, événements). Ce document dit ce qui est fait, ce qui est planifié, et ce qui demande une
décision.

## État au 2026-11-04 (livré le jour même, version 1.1.0 / versionCode 10)

| Axe | Fait | Reste |
|---|---|---|
| 1. Gameplay feel | Arc de vol, squash à l'atterrissage, **onde de contact** colorée (`ui/Ripple`), rebond + étincelles + flash doré + mot d'encouragement quand un tube est complet, pluie de confettis à la victoire, carte de résultat animée | Traînée lumineuse pendant le vol, micro-vibration haptique à la complétion (déjà `vibrate` en réglage), "pop" sonore gradué par tube |
| 2. World map | `logic/World` : 9 biomes qui se succèdent par chapitre de 10 niveaux (Harbour, Deep Sea, Forest, Lavender Fields, Sakura Garden, Ember Caves, Golden Hour, Midnight, Aurora, puis II, III…), **le décor du niveau et de la carte prend le thème du biome** (sauf si le joueur a équipé un thème), écran Levels = cartes de chapitre avec nom, progression et couleurs du biome, accueil affiche le biome courant | Illustration par biome (voir axe 3), "boss" de fin de chapitre (niveau spécial 10 couleurs avec récompense), transition animée entre deux biomes |
| 3. Art direction | Système Nocturne cohérent (tokens, SDF, Poppins), 29 looks achetables, finitions 3D-like (verre, gemme, chrome, jelly) | **Décision requise** : personnages et pièces illustrées exigent des assets dessinés (pas d'artiste ni d'outil d'illustration ici). Options : (a) mascotte procédurale "Tubo" (un tube avec des yeux, animé en scene2d) ; (b) commander 9 illustrations de biome + 1 mascotte (brief prêt à écrire) ; (c) générer avec un outil IA sous licence compatible Play. Aucune des trois n'est lancée sans ton choix |
| 4. Store | Cosmétiques (29), **Boosters : à l'unité et packs de 5 / 12 avec remise affichée** (`logic/BoosterPacks`), **Récompense quotidienne 7 jours** (`logic/DailyRewards`, carte sur l'accueil), bonus vidéo quotidien, objectifs hebdo, succès | **Événements** : cadre à construire = fenêtre temporelle (UTC), règle spéciale (ex. "semaine Sakura" : niveaux en palette Sakura, cosmétique exclusif à 3 victoires), compteur sur l'accueil. Base existante : `WeeklyGoals` + `ChallengeRules`. Estimation 2 jours |

## Ordre recommandé pour la suite
1. Événements hebdomadaires (axe 4) : plus gros levier de rétention J7 restant, et il réutilise le catalogue.
2. Décision art (axe 3) puis mascotte ou illustrations : c'est ce qui change le plus la perception dans le store.
3. Boss de chapitre + transition de biome (axe 2).
4. Traînée de vol et haptique (axe 1) : finition, à mesurer avec les analytics avant d'en faire plus.

## Garde-fous
- Identité "calme" : pas d'explosions violentes ni de secousse d'écran ; l'intensité vient de la lumière,
  du rebond et du son. Le réglage Vibration reste un opt-in.
- Rien n'est vendu contre de l'argent réel ; si des IAP arrivent un jour, la fiche, Data safety et l'IARC
  sont à refaire (voir `shop-cosmetics-spec.md`).
- Chaque mécanique vit en `logic/` avec tests ; les écrans ne décident de rien.
