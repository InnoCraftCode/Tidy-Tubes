# Refonte UI — avant / après

Captures « avant » (version Ball Sort du 21/10/2026) :
`play-store/sortpuzzle/screenshots/before-redesign/`. Captures « après » (rendu réel 1080×1920 de
l'application refondue) : `play-store/sortpuzzle/screenshots/phone/` et `store-assets/screenshots/phone/`.
Vérification sur appareil : émulateur Pixel_10 (1080×2424, Android 17), voir `docs/final-qa-report.md`.

## Écran par écran

| Écran | Avant | Après | Fichiers |
|---|---|---|---|
| Splash | 4 boules + « BALL SORT » en Arial bitmap | brand mark (3 tubes animés) qui surgit, « Tidy Tubes » en Poppins, tagline | `SplashScreen`, `BrandMark` |
| Accueil | titre, Play, Levels, Sound, boutique bas de page, bourse | barre haute (mute, réglages), brand mark, Play à deux lignes (« Level 37 • 6 colours »), Levels, chips pièces / niveaux faits, carte boutique | `HomeScreen` |
| Sélection de niveaux | grille plate, « < » texte, légendes « locked » | chapitres de 10 avec couleurs et bande de difficulté, tuiles vertes avec meilleur score, tuile courante menthe pulsante, cadenas | `LevelSelectScreen` |
| Plateau | HUD texte (« LEVEL 24 STEADY », boutons texte UNDO/RESET/+TUBE/HINT AD), tubes = panneaux rectangulaires | HUD : bouton retour rond, badge niveau + bande + coups, chip pièces, statut en pill ; 4 verbes ronds avec légende (Undo, Reset, Tube ×3, Hint ×1) ; vrais tubes en U avec reflet, liseré et ombre ; indicateur de chargement | `GameScreen`, `HudTable`, `TubeActor`, `TubeTextures` |
| Indice | idem | liseré or pulsant + boule fantôme (conservé), légende « Hint ▶ » (vidéo) ou « Hint • 40 » (pièces) selon disponibilité | `GameScreen.hintCaption` |
| Plus de coups | dialogue texte, bouton « +5 MOVES » (récompense fictive) | carte « Out of moves » : Home / Retry / « +5 ▶ » (vidéo) ou « +5 • 60 » (pièces) ou explication honnête | `GameScreen.showOutOfMovesDialog` |
| Victoire | « LEVEL 4 DONE », « +40 coins » | « Perfect! » ou « Level 4 cleared », détail, chip pièces, Home (icône) / Replay / Next primaire ; interstitiel espacé possible à ce moment | `GameScreen.showResult` |
| Réglages | **inexistant** (mute sur l'accueil) | carte Audio (effets, musique), carte Jeu (How to play, Reset avec confirmation), carte Privacy options (si UMP l'exige), carte À propos (version, « Free with ads • No account • Plays offline », AdMob/identifiant publicitaire, politique de confidentialité, licence de la police) | `SettingsScreen` |
| Onboarding | aucun | carte « How to play » en 3 étapes au premier niveau, rappel dans Réglages | `GameScreen.showHowToPlay` |
| Toasts / erreurs | labels rouges flottants | pill opaque centrée, messages explicites | `Ui.toast` |

## Exigences UX vérifiées

- Lisibilité : Poppins rasterisée à la densité (plus d'agrandissement de bitmap) ; corps 24 u ≈ 36 px sur 1080p.
- Contrastes : texte 15:1, texte secondaire 6,8:1, texte sur menthe 11:1.
- Cibles tactiles : boutons ronds 72 u (≈ 108 px), tuiles 112 u, tubes ≥ 80 u de large.
- Navigation prévisible : retour matériel géré partout, dialogues modaux, un seul geste par action.
- Retours immédiats : squash des boutons, shake des tubes, sons, toasts.
- Animations sobres : 120–300 ms, jamais bloquantes.
- Tailles d'écran : viewport étendu, vérifié 9:16 (captures) et 20:9 (émulateur).
- Mémoire : textures générées une fois, cache de tubes par taille, aucune allocation par frame dans
  `draw()` ; pas de réseau hors SDK publicitaire.

## Défauts corrigés après revue des rendus

1. Feature graphic : le brand mark chevauchait le titre → composition réduite et décalée.
2. Icône : motif trop petit dans le cadre → échelle augmentée (512 px et lanceur).
3. (Code) `Dialog.result` protégé : remplacement d'un hook fragile par `Ui.dialog(title, onResult)`.
4. (Revue de code) Avec `ExtendViewport`, le plateau placé à la main ne suivait pas un
   redimensionnement (fenêtre desktop, pliable/tablette sous Android 16) → `GameScreen.onResized`
   relance `relayout()`. Légende du bouton Hint rafraîchie quand une vidéo devient disponible.
   Bascule Musique initialisée depuis le drapeau réel. Message « vidéo fermée trop tôt » distinct de
   « pas de vidéo ».
5. (Test release sur émulateur) Le build R8 plantait au premier `Actions.moveBy` (« missing
   no-arg constructor ») : règle `-keepclassmembers` pour les constructeurs sans argument des
   classes libGDX mises en pool. Défaut préexistant, invisible en debug.

## Non fait / hors périmètre

- Thème clair (non pertinent, voir design-system §8).
- Tablette : captures 7″ (1200×1920) et 10″ (1600×2560) rendues par le vrai jeu et revues
  (`store-assets/screenshots/tablet-7`, `tablet-10`) ; pas de vérification sur tablette physique.
