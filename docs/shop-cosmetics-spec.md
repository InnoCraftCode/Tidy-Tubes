# Tidy Tubes - Boutique de cosmétiques (spec v1)

Date : 2026-11-04. Statut : **implémenté dans la 1.1.0 (versionCode 10)** le jour même ; écarts par
rapport au texte ci-dessous : quatre onglets (Boules, Finition, Tubes, Thèmes) au lieu de trois, le
chip pièces de l'accueil reste informatif et c'est un bouton « SHOP » (doré + « NEW » quand un item
devient abordable) qui ouvre la boutique, la carte « Styles » de l'écran Progression renvoie à la
boutique. Code : `logic/Cosmetic`, `logic/Shop`, `logic/Contrast`, `ui/Cosmetics`, `screens/ShopScreen`,
tests `ShopTest`, `CosmeticCatalogueTest`, `SaveDataCosmeticsTest`.
Idée de départ (utilisateur) : une « marketplace » dans le jeu où le joueur change les boules, les tubes
ou le thème, paie avec ses points et continue à jouer.

## 1. Ce que c'est, ce que ce n'est pas

- **Cosmétique uniquement.** Un item ne change jamais les règles, la difficulté, les indices ni les coups.
  Le joueur qui n'achète rien joue exactement le même jeu.
- **Monnaie : les pièces existantes.** Aucune nouvelle monnaie, aucun achat réel (IAP). La fiche store
  dit « Aucun achat » et le formulaire Sécurité des données reste valable. Si un jour on ajoute des IAP,
  c'est une autre spec (Play Billing, Data safety, fiche, IARC à refaire).
- **On garde la promesse d'honnêteté du jeu** : prix affichés, aperçu réel avant achat, pas de loot box,
  pas d'item à durée limitée dans la v1.

Pourquoi ça aide le jeu à réussir : une boutique transforme les pièces (aujourd'hui dépensées seulement
en indices et coups) en **objectif visible**. Le joueur revient pour « encore 120 pièces et j'ai le thème
Sakura ». C'est le levier de rétention J3-J7 le moins cher à construire, car tout le rendu est déjà
procédural : un item = quelques couleurs ou paramètres, zéro asset à dessiner.

## 2. Économie actuelle (mesurée dans GameConfig / SaveData)

| Source | Pièces |
|---|---|
| Niveau réussi | 25 (+15 si au par ou mieux) |
| Défi du jour (première victoire) | 35 |
| Bonus vidéo quotidien | 10 |
| Objectifs hebdo, succès | variable, ponctuel |
| **Dépenses** : indices pour le niveau | 40 |
| **Dépenses** : 5 coups supplémentaires | 60 |

Revenu typique d'un joueur régulier : **35 à 40 pièces par niveau**, soit 350 à 400 par chapitre de dix.
Les 3 palettes de boules existantes (Classique gratuite, Pastel via le défi du jour, Néon via les étoiles)
sont conservées telles quelles : elles deviennent des items de la boutique avec la mention « débloqué par
progression » au lieu d'un prix.

## 3. Catalogue v1 (14 items, 3 onglets) — étendu le 2026-11-04 soir à 29 items

Demande utilisateur : « que l'utilisateur ne soit jamais à court d'achats ». Ajouts (tous dans
`logic/Cosmetic`, validés par `CosmeticCatalogueTest` contraste/distinction et `ShopTest.catalogueDepth`) :
palettes Sunset 500, Berry 500, Retro 900 ; finitions Jelly 500, Chrome 1500 ; tubes Ink 250, Rose 500,
Silver 500, Crystal 1500 ; thèmes Midnight 250, Deep sea 500, Lavender 500, Ember 900, Golden hour 1500.
Nouveau palier **1500** = objets de prestige. Valeur totale à acheter : 16 250 pièces, soit environ
300 niveaux de jeu régulier (40 pièces/niveau) ; 25 items en vente, au moins 4 par catégorie.

Paliers de prix : **250 / 500 / 900**. Le premier achat doit arriver vite (250 pièces = 6 à 7 niveaux),
le dernier item demande environ 25 niveaux de jeu régulier.

### Onglet Boules
| Id | Nom | Type | Obtention |
|---|---|---|---|
| ball.classic | Classique | palette (existante 0) | gratuit, équipé par défaut |
| ball.pastel | Pastel | palette (existante 1) | défi du jour (règle actuelle) |
| ball.neon | Néon | palette (existante 2) | étoiles (règle actuelle) |
| ball.candy | Bonbon | nouvelle palette (saturée, rose/menthe/citron) | 250 |
| ball.ocean | Océan | nouvelle palette (bleus/verts/sable, contraste vérifié) | 500 |
| ball.matte | Finition mate | même palette équipée, sans reflet | 250 |
| ball.glass | Finition verre | reflet fort + ombre interne | 500 |
| ball.gem | Finition gemme | facettes (SDF polygonal) | 900 |

Palette et finition sont deux choix indépendants (une palette + une finition équipées à la fois).

### Onglet Tubes
| Id | Nom | Rendu (TubeTextures) | Prix |
|---|---|---|---|
| tube.glass | Verre | actuel | gratuit |
| tube.frost | Givré | corps plus opaque, bord doux | 250 |
| tube.neon | Néon | bord lumineux couleur PRIMARY | 500 |
| tube.gold | Or | bord doré, corps ambré | 900 |

### Onglet Thèmes (fond + aurores + teinte des cartes)
| Id | Nom | BG_TOP / BG_BOTTOM / AURORA_A / AURORA_B | Prix |
|---|---|---|---|
| theme.nocturne | Nocturne | actuel (222B5E / 0B0E24 / 3FB7C9 / 7B5CE6) | gratuit |
| theme.aurore | Aurore | 5A2E6E / 1A0B24 / FF8A65 / F472B6 | 500 |
| theme.foret | Forêt | 1E4D3A / 06150F / 5BE49B / 3FB7C9 | 500 |
| theme.sakura | Sakura | 6E2E4A / 24081A / FFB3C7 / FFD23F | 900 |

Règle de contraste : chaque thème doit garder le texte TEXT (F4F6FF) à un ratio ≥ 7:1 sur BG_TOP et les
10 couleurs de la palette équipée distinguables sur le fond (vérifié sur rendu, cf. docs/design-system.md).
Les couleurs ci-dessus sont des points de départ à ajuster à l'écran.

## 4. Parcours joueur

1. **Découverte** : sur l'écran d'accueil, le chip pièces devient tappable et ouvre la boutique
   (icône panier à côté du compteur). Un point coloré apparaît sur le chip quand le joueur peut
   s'offrir un item qu'il ne possède pas (une fois par item, jamais de popup).
2. **Après une victoire** : si le solde vient de dépasser le prix d'un item non possédé, le récapitulatif
   de fin de niveau affiche une ligne discrète « Nouveau dans la boutique : Bonbon » (une seule fois par item).
3. **Boutique** : trois onglets. Chaque item est une carte avec aperçu vivant (un mini tube avec trois boules
   rendues avec l'item), nom, et un état : `Prix` / `Possédé` / `Équipé` / `Débloqué à ... (progression)`.
   En haut, un **aperçu grandeur nature** : un petit plateau de 4 tubes qui applique en direct l'item
   sélectionné, pour juger avant d'acheter.
4. **Achat** : tap sur « Acheter 500 » ouvre un dialogue de confirmation (évite les faux taps) avec le
   solde avant/après. Validation : les pièces volent du compteur vers la carte (réutiliser l'animation
   de WinSummary), son coin.wav, carte passe à « Équipé ». L'item est équipé automatiquement après achat.
5. **Équiper / changer** : un tap sur un item possédé l'équipe immédiatement, sans dialogue.
6. **Réglages** : le sélecteur de palette actuel est remplacé par un bouton « Boutique ».
7. **Reset progress** : efface aussi les items (le dialogue de reset le dit explicitement).

Pas de bouton vendre, pas de remboursement, pas d'item temporaire.

## 5. Architecture (règles du workspace : logique pure testée, écrans sans règles)

### logic/ (Java pur, JUnit)
- `logic/Cosmetic.java` : enum du catalogue. Champs : `id`, `category` (BALL_PALETTE, BALL_FINISH, TUBE,
  THEME), `price` (0 = gratuit), `unlock` (NONE, DAILY_PALETTE, STARS) et pour les palettes/thèmes les
  valeurs de couleurs en hex (String, pas de type libGDX).
- `logic/Shop.java` : règles. `boolean canBuy(SaveData, Cosmetic)`, `PurchaseResult buy(SaveData, Cosmetic)`
  (débit exactement une fois, refus si déjà possédé ou solde insuffisant ou item à déblocage par progression),
  `equip(SaveData, Cosmetic)` (refus si non possédé), `Cosmetic equipped(SaveData, Category)`,
  `List<Cosmetic> newlyAffordable(SaveData)` pour les notifications.
- `logic/ContrastCheck.java` (optionnel) : ratio WCAG entre deux hex, utilisé par un test qui valide
  chaque thème contre TEXT.

### save/SaveData.java
- `Set<String> ownedCosmetics` (persisté en JSON comme liste), `String equippedBallPalette`,
  `String equippedBallFinish`, `String equippedTube`, `String equippedTheme`.
- Migration au chargement : l'ancien `ballPalette` (int) devient `equippedBallPalette` ; les palettes 1 et 2
  restent régies par `isCosmeticUnlocked` (conditions de progression), le reste par `ownedCosmetics`.
- Les items gratuits sont toujours considérés possédés (pas écrits dans le set).

### ui/
- `ui/Theme.java` : instance runtime construite depuis un `Cosmetic` THEME ; `Palette` expose
  `Palette.theme()` et les écrans lisent `Palette.theme().bgTop` etc. au lieu des constantes BG_* / AURORA_*.
  `AnimatedBackground` et `BaseScreen` sont les deux seuls consommateurs à modifier.
- `Palette.setBallPalette(Cosmetic)` : la palette devient une liste de 10 couleurs lue depuis l'enum.
- `ui/Tex` : une texture de sphère par finition (glossy, matte, glass, gem), générée à la demande et
  cachée ; `BallActor` reçoit la finition via le Skin (région nommée `ball-<finish>`).
- `TubeTextures` : paramètre `TubeStyle` (opacité du corps, couleur/épaisseur du bord), cache par style
  et taille, dispose des anciens à l'équipement.
- `ui/CosmeticPreview.java` : acteur « mini tube + 3 boules » utilisé par les cartes et l'aperçu.

### screens/ShopScreen.java
Onglets, grille scrollable (2 colonnes téléphone, 3 tablette via ExtendViewport), aperçu en haut,
dialogue de confirmation via `Ui.dialog`. Aucune règle : tout passe par `Shop`.

### Analytics (déjà branché, interface `Analytics`)
`shop_open(source)`, `shop_purchase(item, price, coins_after, level)`, `shop_equip(item)`,
`shop_view_item(item)`. Métriques à suivre la première semaine : % de joueurs qui ouvrent la boutique,
% qui achètent un premier item, niveau moyen du premier achat (cible : avant le niveau 12), rétention J7
des acheteurs vs non-acheteurs.

### Tests (JUnit, `:core:test --tests '*Shop*'`)
- achat débite exactement le prix, une seule fois ; second achat refusé.
- achat refusé si solde insuffisant (solde inchangé) ; refusé pour un item à progression.
- équiper un item non possédé refusé ; items gratuits équipables sans achat.
- migration d'une sauvegarde 1.0.9 (ballPalette = 2) → `equippedBallPalette = ball.neon`.
- `newlyAffordable` ne renvoie chaque item qu'une fois.
- chaque thème passe le contraste texte ≥ 7:1 ; chaque palette garde 10 couleurs distinctes.

## 6. Risques et garde-fous

- **Lisibilité** : une palette « jolie » mais ambiguë rend le tri injouable. Test de contraste + l'option
  « étiquettes de couleur » existante reste disponible quel que soit l'item.
- **Performance** : textures générées à l'équipement seulement, jamais par frame ; dispose de l'ancien style.
  Budget : < 50 ms de génération à l'équipement, aucune allocation dans `draw`.
- **Économie** : si les joueurs achètent tout avant le niveau 30, les paliers sont trop bas ; si personne
  n'achète avant le niveau 20, trop hauts. Ajuster avec les événements, pas à l'intuition.
- **Pub** : aucune réduction « regarde une vidéo » dans la v1. Possible en v2 (vidéo récompensée = +30
  pièces vers un item, via RewardGate), à décider avec les chiffres.
- **Store** : la fiche peut gagner une ligne « Personnalise tes boules, tubes et thèmes » et une capture
  de la boutique. Rien à changer côté politique.

## 7. Plan de livraison

| Étape | Contenu | Estimation |
|---|---|---|
| 1 | `Cosmetic`, `Shop`, SaveData + migration, tests | 1 jour |
| 2 | `Theme` runtime, `Palette` dynamique, fond et écrans thémés | 1 jour |
| 3 | `ShopScreen` (onglets, cartes, aperçu, dialogue, animation pièces), entrée depuis l'accueil et les réglages | 1,5 jour |
| 4 | Finitions de boules et styles de tubes dans `Tex` / `TubeTextures` | 1 jour |
| 5 | Analytics, captures store, QA sur AVD, bump de version | 0,5 jour |

v2 (après lecture des données) : items saisonniers, réduction par vidéo récompensée, item offert au
premier défi hebdo réussi, aperçu d'un item verrouillé pendant un niveau.
