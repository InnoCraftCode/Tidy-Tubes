# Audit initial — Tidy Tubes (ex « Ball Sort »), `games/sortpuzzle`

Date : 2026-10-26. Périmètre : l'application existante avant toute modification de cette mission.
Point de restauration : `backups/2026-10-26-pre-redesign/sortpuzzle-sources-and-store-kit.zip`
(sources du projet sans `build/`, `.gradle/`, `.idea/`, `libs/` ; kit Play Store complet).

## 1. Analyse technique

| Élément | Constat |
|---|---|
| Stack | **libGDX 1.13.1** (Java), Gradle 8.14.3 (Kotlin DSL), AGP 8.11.1, JDK 21 (toolchain), bytecode 17 pour `core`/`android` (D8), 21 pour `lwjgl3`. Pas de Kotlin, pas de Flutter/RN. |
| Modules | `core` (règles, écrans, UI, sauvegarde, audio, ads), `lwjgl3` (desktop + outil de capture), `android` (cible de publication). |
| Package / applicationId | `com.ben.laama.sortpuzzle` (conservé : jamais encore publié, mais inchangé par principe). |
| Versions | versionCode 1, versionName 1.0.0. |
| SDK Android (avant) | compileSdk 35, **targetSdk 35** (Google Play exige 36 depuis le 31/08/2026), minSdk 24. Plateforme 36 absente de la machine, plateforme **37 (Android 17)** présente. |
| Signature | `keystore.properties` git-ignoré, lu par `android/build.gradle.kts` ; absent sur cette machine → builds release non signés. Keystore jamais lu ni affiché. |
| Dépendances | gdx, gdx-backend-android, gdx-backend-lwjgl3, gdx-platform natives (4 ABI), gdx-lwjgl3-angle, JUnit 5.11.4. **Aucun SDK publicitaire, analytics ou paiement.** |
| Publicité (avant) | Interface `AdProvider` + `NoOpAdProvider` qui **accorde les récompenses sans vidéo** (stub de développement) ; utilisé aussi dans `AndroidLauncher`. Les libellés « HINT AD » / « +5 MOVES » promettaient une vidéo qui n'existait pas. |
| Réseau / permissions | Aucune permission déclarée dans le manifeste (avant), aucune connexion réseau dans le code. |
| Données | Progression dans libGDX `Preferences` (JSON), local uniquement. Aucune donnée personnelle. |
| Ressources graphiques | Aucun asset bitmap : tout est généré par `Tex.java` (Pixmap). Police : Arial 15 px embarquée dans gdx.jar, agrandie 3,6× (floue aux grandes tailles). Icône de lanceur = icône par défaut d'Android Studio (robot vert). |
| Audio | Effets et musique synthétisés (`tools/audio/SfxSynth.java`), fichiers `.wav` originaux, sans licence tierce. |
| Build / scripts | `tools/android-build/build-android.ps1`, `tools/validation/validate-workspace.ps1`, kit Store (`tools/store/*`), capture desktop scriptée (`StoreCapture.java`). FFmpeg 9.0.2 présent. |
| Tests | 70 tests JUnit 5 (`model`, `logic`), **tous verts** à l'audit (`gradlew :core:test`, exit 0). Aucun test UI. |
| Outillage machine | JDK 21 Temurin, Android SDK (platforms 35 et 37, build-tools 35/36, emulator, AVD **Pixel_10** API 37 16 Ko), FFmpeg. **Git absent** (pas de dépôt ; sauvegarde zip à la place). Python absent (stub Store), ImageMagick absent. |
| Problèmes connus | targetSdk < 36 ; wrapper Gradle réécrit en 9.x une fois par l'extension VS Code (protégé par `.vscode/settings.json`) ; `.gradle/9.7.1` résiduel dans le projet. |

## 2. Cartographie fonctionnelle (référence anti-régression)

| Fonctionnalité | Écran(s) | Actions utilisateur | Traitement | États / cas limites | Dépendance externe |
|---|---|---|---|---|---|
| Splash | `SplashScreen` | tap pour passer | aucun | 1,6 s puis accueil | — |
| Accueil | `HomeScreen` | Play (reprend `currentLevel`), Levels, Son on/off, Acheter un tube (120 pièces) | `SaveData` | bouton d'achat désactivé si pièces insuffisantes (shake) | — |
| Sélection de niveaux | `LevelSelectScreen` | tap sur une tuile débloquée | `SaveData.isUnlocked/isCompleted`, `Difficulty.colorsFor` | grille infinie (débloqués + 30), scroll sur le niveau courant | — |
| Plateau | `GameScreen` | tap tube source / tube cible ; Undo ; Reset ; +Tube ; Hint ; retour | `LevelGenerator` (thread), `MoveValidator`, `BoardState.apply`, `WinChecker`, `Solver` (par, thread) | chargement (« shuffling... »), coup illégal (shake), tube complété (fête), victoire, **plus de coups** (limite = par + marge) | — |
| Génération de niveaux | logique pure | — | marche arrière depuis le plateau résolu, seed déterministe par index | niveau n toujours identique, toujours soluble | — |
| Indices | `GameScreen` → `HintEngine` (thread) | bouton Hint | 1 gratuit/niveau ; ensuite « récompense » débloquant les indices du niveau | MOVE / UNDO / UNKNOWN / ALREADY_WON ; compteur « moves left » | `AdProvider.showRewarded` |
| Coups supplémentaires | dialogue « Out of moves » | Home / Reset / +5 moves | +5 à la limite | si « vidéo » indisponible → message | `AdProvider.showRewarded` |
| Boosters (tube supplémentaire) | `GameScreen` | bouton +Tube | 3 gratuits/niveau, puis stock permanent, puis achat auto 120 pièces | max 14 tubes ; pas assez de pièces → message | — |
| Économie | `SaveData.recordWin` | — | 25 pièces/victoire, +15 si ≤ par, rejouer = 1/3 | — | — |
| Sauvegarde | `SaveData` | — | JSON dans Preferences, sanitisation, règle R8 `-keep` | blob corrompu → profil neuf | — |
| Audio | `SoundManager` | mute global | `.ogg` puis `.wav`, musique en boucle | fichiers manquants → silence | — |
| Captures Store | `StoreCapture` (lwjgl3) | — | rendu 1080×1920 off-screen, scénario scripté, solveur | — | — |

## 3. Risques identifiés et priorités

| # | Risque | Gravité | Décision |
|---|---|---|---|
| 1 | targetSdk 35 → refus Google Play | Bloquant | compileSdk 37 (installé) + targetSdk 36 ; vérifié par build. |
| 2 | Stub `NoOpAdProvider` en production : récompenses « vidéo » accordées sans vidéo, libellés trompeurs | Élevé | Fournisseur inerte pour les builds sans pub + `RewardOffer` (pièces) ; puis intégration AdMob réelle à la demande de l'utilisateur. |
| 3 | Icône de lanceur par défaut Android Studio | Élevé (fiche) | Nouvelle identité et icônes générées (vecteur + PNG). |
| 4 | Typographie bitmap floue, UI générique | Moyen (perception premium) | gdx-freetype + Poppins (OFL) rasterisée à la densité de l'écran ; design system « Nocturne ». |
| 5 | Nom « Ball Sort » générique, non distinctif, des dizaines d'homonymes sur Play | Moyen | Renommage « Tidy Tubes » (voir `branding-and-aso.md`). |
| 6 | Letterboxing sur les écrans 20:9 (`FitViewport` 9:16) | Moyen | `ExtendViewport` 720×1280 → 1000×1720. |
| 7 | Pas de Git | Moyen (process) | Sauvegarde zip ; installation de Git laissée à l'utilisateur (règle : demander avant d'installer). |
| 8 | Politique de confidentialité absente (obligatoire dans Play Console) | Bloquant (action utilisateur) | URL à fournir ; bouton prévu dans Réglages dès que `GameConfig.PRIVACY_POLICY_URL` est renseignée. |
| 9 | Aucun écran Réglages / aide / à-propos | Faible | Ajoutés. |

## 4. Ce qui n'a pas été modifié (protection de compatibilité)

applicationId / package `com.ben.laama.sortpuzzle`, nom de dossier `games/sortpuzzle`, préférences
`com.ben.laama.sortpuzzle.save` (progression conservée), règles du jeu (`model/`, `logic/`),
économie (prix, pièces), génération de niveaux, configuration de signature, 70 tests existants.
