# Design system « Nocturne » — Tidy Tubes

Implémenté dans `games/sortpuzzle/core/src/main/java/com/ben/laama/sortpuzzle/ui/`
(`Palette`, `SkinFactory`, `Sdfs`, `Icons`, `Tex`, `Ui`, `TubeTextures`, `BrandMark`). Les unités
sont des unités monde : largeur 720 (minimum), hauteur 1280 (minimum), viewport étendu jusqu'à
1000×1720 selon l'écran.

## 1. Direction artistique

Catégorie réelle : puzzle casual de tri (ball sort). Les concurrents utilisent soit des fonds
blancs/pastel génériques, soit des interfaces chargées de boutons dorés et de bannières. Tidy Tubes
prend le contre-pied : une **nuit calme** — fond indigo profond avec deux voiles de couleur
lents (« aurora »), verre discret, un seul accent menthe pour l'action, et les boules comme
unique source de couleur vive. Résultat : les boules (l'objet du jeu) sont ce qui attire l'œil,
l'interface se tait. Ton : apaisant, précis, sans décor superflu.

## 2. Couleurs (`Palette.java`)

| Rôle | Nom | Hex | Usage |
|---|---|---|---|
| Fond haut / bas | `BG_TOP` / `BG_BOTTOM` | `#222B5E` / `#0B0E24` | dégradé vertical plein écran |
| Aurora A / B | `AURORA_A` / `AURORA_B` | `#3FB7C9` / `#7B5CE6` | halos radiaux α 0,20, respiration lente |
| Surface / forte | `SURFACE` / `SURFACE_STRONG` | `#FFFFFF14` / `#FFFFFF26` | cartes, tubes, boutons fantômes |
| Bord | `EDGE` | `#FFFFFF52` | liseré des tubes |
| Carte opaque | `CARD` | `#1C2250F7` | dialogues, toasts |
| Voile | `SCRIM` | `#05071599` | derrière les dialogues |
| Ombre | `SHADOW` | `#00000070` | ombre de contact sous les tubes |
| Primaire | `PRIMARY` / `PRIMARY_DOWN` | `#4FE3C1` / `#2FB89A` | action principale, sélection, indicateur d'aide |
| Sur primaire | `ON_PRIMARY` | `#05221B` | texte sur menthe (contraste 11:1) |
| Or | `GOLD` / `GOLD_DOWN` / `ON_GOLD` | `#FFC857` / `#D9A33A` / `#2A1D00` | pièces, boosters, félicitations, suggestion |
| Succès / Danger / Info | `SUCCESS` / `DANGER` / `INFO` | `#5BE49B` / `#FF6F7D` / `#7CC4FF` | niveaux faits, erreurs, information |
| Texte | `TEXT` / `TEXT_DIM` / `TEXT_DISABLED` | `#F4F6FF` / `#A3AACB` / `#5C6388` | 15:1 / 6,8:1 / désactivé |
| Boules (10) | `ball(i)` | coral `#F2504B`, bleu `#3B82F6`, vert `#34C759`, jaune `#FFD23F`, violet `#A855F7`, orange `#FF8A3D`, teal `#14B8A6`, rose `#F472B6`, argent `#CBD5E1`, brun `#9A5B2B` | différentes en luminosité et en teinte (daltonisme) |

Contrastes mesurés sur `BG_BOTTOM` : `TEXT` ≈ 15:1, `TEXT_DIM` ≈ 6,8:1 (> 4,5:1), `PRIMARY` ≈ 10:1.

## 3. Typographie (`SkinFactory.registerFonts`)

Poppins (SIL Open Font License, fichiers dans `android/src/main/assets/fonts/`), rasterisée par
gdx-freetype à la densité de l'écran (`min(largeur/720, hauteur/1280)`, bornée 1–2,5) puis
réduite d'autant : texte net sur tout téléphone.

| Style | Fonte | Taille (u) | Usage |
|---|---|---|---|
| `display` | ExtraBold | 58 | nom de l'app, mots de félicitation |
| `title` | ExtraBold | 40 | titres d'écran et de dialogue |
| `heading` | Bold | 30 | niveau, boutons primaires/secondaires, numéros de tuile |
| `default` (body) | SemiBold | 24 | paragraphes, lignes de réglages |
| `label` | SemiBold | 20 | boutons pill, chips, texte secondaire |
| `small` / `caption` | Medium | 17 | légendes, sous-titres, statuts |

Casse : phrases en minuscules (« Level 24 »), majuscules réservées aux boutons d'action courts.

## 4. Espacements, formes, élévation

- Grille de 4 u ; marges d'écran 26–36 u ; padding carte 22/26 u.
- Rayons : carte 28, carte petite 16, pill 24, pill large 36, bouton icône = cercle.
- Élévation sans ombres portées (fond sombre) : hiérarchie par opacité des surfaces (8 % → 15 %
  → 24 %) ; une seule ombre de contact sous les tubes.
- Toutes les formes sont des champs de distance signée (`Sdfs`) rasterisés une fois (`Tex.fromSdf`)
  — anti-aliasing analytique, zéro asset.

## 5. Composants (`Ui.java`)

| Composant | Spécification | États |
|---|---|---|
| `iconButton` | cercle 72 u, glyphe 34 u, surface 9 % | pressé (+8 % luminosité, échelle 0,92 → ressort), désactivé (α 0,45) |
| `button primary` | pill-lg, menthe, heading, padding 18/44 | pressé plus sombre, désactivé gris |
| `button secondary` | pill-lg, surface forte | idem |
| `button gold` / `ghost` / `danger` | pill, label | idem |
| `twoLineButton` | titre + légende (Play : « Level 37 • 6 colours ») | idem |
| `card` | carte 28, surface 8 % | — |
| `chip` | pill, icône teintée + label (pièces, niveaux faits) | — |
| `Toggle` | 78×44, piste menthe / 16 %, bouton glissant 160 ms | on/off |
| `dialog(title, onResult)` | carte opaque, voile, boutons stylés, retour exactement une fois | — |
| `toast` | pill opaque, monte de 18 u, 1,5 s | — |
| Tuiles de niveau | 112×112, `tile-done` (vert), `tile-current` (menthe, pulsation 1,06), `tile-open`, `tile-locked` (cadenas) | — |
| Tube (`TubeActor`) | corps en U généré à la taille exacte (`TubeTextures`), reflet gauche, liseré, ombre | sélectionné (surface forte + liseré menthe), suggéré (liseré or pulsant), shake |
| Boule (`BallActor`) | sphère ombrée 128 px, teintée | levée (bob), vol en arc, squash à l'atterrissage |

Icônes (`Icons.java`, 96 px, trait 8) : back, next, close, undo, restart/replay, hint (ampoule),
tube (+), settings (engrenage), home, sound, sound-off, music, check, lock, coin, play, grid,
info, help, trash.

## 6. Navigation, états, clavier

- Flux : Splash → Accueil → (Niveaux | Réglages | Plateau). Retour matériel : Accueil quitte,
  les autres reviennent à l'accueil ; dialogues modaux.
- Chargement : « Shuffling the tubes… » avec trois boules qui respirent.
- Vide / verrouillé : tuiles cadenas ; « Hint • 40 » quand une vidéo n'est pas disponible.
- Erreur : toasts (« Not enough coins… », « Video closed early… »), jamais de blocage.
- Confirmations destructives : dialogue « Reset progress? » avec bouton danger.
- Onboarding : carte « How to play » au premier niveau (une fois, `SaveData.tutorialSeen`),
  rappelable depuis Réglages.
- Clavier : aucun champ de saisie dans le jeu (non applicable).
- Permissions / consentement : formulaire UMP géré par le SDK ; carte « Privacy options » dans
  Réglages uniquement si requis.

## 7. Mouvement

Durées 120–300 ms, `swingOut`/`sine`, fondu d'écran 250 ms, jamais bloquant ; `delta` borné à
1/30 s. Aurora : période ≈ 25 s. Boules d'arrière-plan : intensité 1 sur les menus, 0,35 sur le
plateau, 0,6 dans Réglages.

## 8. Thèmes

Un seul thème (sombre) : un jeu possède sa surface et un thème clair diluerait la direction
artistique ; les contrastes sont validés sur le fond sombre. Non pertinent pour cette catégorie.

## 9. Adaptation aux écrans

`ExtendViewport(720, 1280, 1000, 1720)` : 9:16 exact (captures), 20:9 (Pixel 10 : monde
720×1616, vérifié sur émulateur), 4:3 (tablette : jusqu'à 1000 de large). Le plateau se centre
sur la hauteur réelle ; les tables en fill-parent suivent.
