# Rapport final QA et build — Tidy Tubes 1.0.0 (`games/sortpuzzle`)

Date : 2026-10-26. Machine : Windows 11, JDK 21, Android SDK (platforms 35/37, build-tools 36.0.0),
AVD Pixel_10 (Android 17 / API 37, 1080×2424, pages mémoire 16 Ko), FFmpeg 9.0.2. Git absent.

## 1. Commandes exécutées et résultats

| # | Commande (depuis `games\sortpuzzle` sauf mention) | Résultat | Erreurs / corrections |
|---|---|---|---|
| 1 | `gradlew :core:test` (référence avant modification) | 70 tests, 0 échec | — |
| 2 | `gradlew :core:test :lwjgl3:compileJava` (après refonte UI) | 76 tests, 0 échec ; compilation OK | — |
| 3 | `gradlew :core:compileJava :lwjgl3:compileJava :android:compileDebugJavaWithJavac` | BUILD SUCCESSFUL | avertissement AGP « compileSdk 37 » (attendu, supprimé ensuite via `android.suppressUnsupportedCompileSdk`) |
| 4 | `gradlew :lwjgl3:storeCapture` (×3) | 7 captures 1080×1920 rendues depuis le vrai jeu | — |
| 5 | `gradlew :core:test :android:assembleDebug` (après AdMob) | **90 tests, 0 échec** ; `android-debug.apk` 15,4 Mo | — |
| 6 | `adb install -r` debug + `am start` + logcat (AVD Pixel_10) | lancement OK, UMP « consent info updated », « GMA SDK initialised », « Interstitial loaded », « Rewarded loaded » (IDs de démo), aucun crash ; ancienne sauvegarde (niveau 2, 40 pièces) reprise sans perte | — |
| 7 | `gradlew :android:bundleRelease :android:assembleRelease :android:printAdConfig` | AAB 4,9 Mo, APK release 6,5 Mo ; `release: ADS_ENABLED=false` (pas d'`admob.properties`) | — |
| 8 | `aapt2 dump badging`, `zipalign -c -P 16 4`, analyse ELF `p_align` | targetSdk 36, compileSdk 37, minSdk 24 ; alignement 16 Ko « Verification successful » ; `libgdx.so` et `libgdx-freetype.so` p_align 16384 | — |
| 9 | APK release signé avec la clé de debug (copie de test), installé, lancé sur l'AVD | **CRASH** au premier `Actions.moveBy` : « Class cannot be created (missing no-arg constructor) » (`ReflectionPool`), puis idem sur `GlyphLayout` | Règle R8 ajoutée (`-keepclassmembers class com.badlogic.gdx.** { public <init>(); }` + Action/Event). Défaut **préexistant** (jamais testé en release avant). |
| 10 | Rebuild release + re-test AVD (×2) | processus vivant après lancement, ouverture d'un niveau et coups joués ; aucune exception fatale | — |
| 11 | Revue de code indépendante (`code-review-agent`) | 0 critique, 1 majeur, 9 mineurs | majeur corrigé : `GameScreen.onResized` → `relayout()` ; mineurs corrigés : `consent` volatile, `Json.setIgnoreUnknownFields(true)`, message « vidéo fermée trop tôt », rafraîchissement de la légende Hint, bascule Musique, javadocs, `inputs.files(natives)` ; non traité : code mort mineur (icônes/méthodes inutilisées, conservés comme bibliothèque) |
| 12 | `gradlew :core:test :lwjgl3:compileJava :android:assembleDebug :android:assembleRelease :android:bundleRelease` (final) | **90 tests, 0 échec** ; debug 15,4 Mo, release APK 6,6 Mo, **AAB 5,1 Mo (non signé)** | — |
| 13 | Test final AVD du build release (signé debug) : lancement, niveau, deux coups | processus vivant, aucune exception | — |
| 14 | `tools\validation\validate-workspace.ps1` (racine) | 0 échec, 1 avertissement (template à targetSdk 35, hors périmètre) | — |
| 15 | `tools\store\validate-store-assets.ps1` / `export-store-assets.ps1` | overall **PASS** (icône, feature graphic, 7 captures, icônes launcher) | — |
| 16 | `tools\store\build-listing-doc.ps1` | descriptions courtes dépassaient 80 caractères (81, 89) → raccourcies ; document Word régénéré | corrigé |
| 17 | `tools\store\capture-desktop.ps1 -Video` + FFmpeg | 704 images, deux MP4 de 23,5 s | — |
| 18 | Audit de conformité indépendant (auditeur, lecture seule) | 66 contrôles ; verdict **BLOQUÉ** par 3 actions utilisateur, puis PRÊT SOUS CONDITIONS | voir `docs/google-play-compliance-report.md` |

## 2. Couverture de tests

| Type | État |
|---|---|
| Unitaires (règles, génération, solveur, indices, difficulté) | 70 tests existants, verts |
| Unitaires ajoutés | `AdProvidersTest` 5, `RewardGateTest` 4, `AdPacingTest` 5, `RewardOfferTest` 6 (= 20) |
| Intégration / UI | pas de framework UI ; vérification par rendus réels (StoreCapture) et par scénarios sur émulateur (lancement, accueil, niveau, coups, sauvegarde reprise, build release R8) |
| Parcours principaux | accueil → niveau → coups ✔ (AVD) ; indice gratuit ✔ (capture 03) ; victoire ✔ (capture 04) ; niveaux ✔ ; réglages ✔ ; tutoriel première fois ✔ (AVD) |
| Navigation / bouton retour | code revu ; non automatisé |
| Réseau | aucun réseau applicatif ; SDK pub : chargements observés dans logcat |
| Permissions | aucune demande à l'exécution (aucune permission dangereuse) |
| États d'erreur | pas de pièces / pas de vidéo / vidéo fermée : chemins revus et testés unitairement (`RewardOffer`, `RewardGate`) |
| AdMob | init + chargement observés ; **vidéo récompensée non visionnée de bout en bout sur l'AVD** (la carte « How to play » a intercepté le test automatisé) ; interstitiel non déclenché (nécessite 3 niveaux réussis) |

## 3. Build

| Élément | Valeur |
|---|---|
| applicationId / versionCode / versionName | `com.ben.laama.sortpuzzle` / 1 / 1.0.0 |
| SDK | compileSdk 37, targetSdk 36, minSdk 24 |
| Signature | **AAB non signé** : pas de `keystore.properties` sur cette machine (jamais lu, jamais généré ; aucun keystore créé). L'APK release n'a été signé qu'avec la clé de debug, dans le dossier temporaire, pour le test. |
| Pub en release | `ADS_ENABLED=false` tant qu'`admob.properties` est absent (par conception) : **ne pas téléverser ce build** si la fiche déclare des annonces ; reconstruire avec les IDs de production. |
| ABI | arm64-v8a, armeabi-v7a, x86, x86_64, natives alignées 16 Ko |

## 4. Régressions trouvées et corrigées

1. Crash du build release (R8) sur les objets mis en pool — préexistant, corrigé (règle keep).
2. Plateau non recalculé après redimensionnement avec `ExtendViewport` — introduit par la refonte, corrigé.
3. Descriptions courtes trop longues — corrigé.
4. Composition de la feature graphic (chevauchement) et icône trop petite — corrigé avant export.

## 5. Non exécuté / limites

- Pas de test sur téléphone physique (émulateur uniquement, x86_64 ; les natives ARM sont vérifiées
  par alignement, pas exécutées).
- Pas de test en EEE du formulaire de consentement UMP (nécessite un appareil de test ou une
  géolocalisation EEE), ni avec des IDs de production.
- Tablette : viewport adaptatif non vérifié sur tablette réelle ; aucune capture tablette livrée.
- Git absent : aucun historique ; sauvegarde zip `backups/2026-10-26-pre-redesign/`.
- Les builds release contiennent l'ID d'application AdMob de démo dans le manifeste quand
  `admob.properties` manque (repli documenté) ; l'auditeur demande de ne pas publier ce build.
