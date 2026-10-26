# Rapport d'audit de conformité Google Play — Tidy Tubes 1.0.0

| | |
|---|---|
| Application | **Tidy Tubes** (`games/sortpuzzle`), `applicationId` `com.ben.laama.sortpuzzle`, versionCode 1, versionName 1.0.0 |
| Pile | libGDX 1.13.1, Java 17 (D8), AGP 8.11.1, Gradle 8.14.3, compileSdk 37, targetSdk 36, minSdk 24 |
| Publicité | GMA Next-Gen SDK 1.5.0 + UMP 4.0.0, derrière `AdProvider` |
| Auditeur | `play-compliance-auditor-agent` (indépendant, lecture seule ; n'a écrit aucun code) |
| Date de l'audit | 2026-10-26 |
| Artefacts audités | `E:\mobile andoird\games\sortpuzzle\android\build\outputs\apk\release\android-release-unsigned.apk` (6 838 389 octets, 20:44) et `E:\mobile andoird\games\sortpuzzle\android\build\outputs\bundle\release\android-release.aab` (5 143 059 octets, 20:44) |

> **Avertissement.** Ce rapport constate l'état du projet à la date indiquée et le compare aux pages officielles lues ce jour. Il ne garantit en rien l'acceptation par Google : la revue Play est humaine et automatique, ses critères évoluent, et plusieurs contrôles dépendent d'actions que seul l'utilisateur peut faire dans Play Console et AdMob.

## 0. Verdict

**Statut global : BLOQUÉ** (blocking items ci-dessous), puis **PRÊT SOUS CONDITIONS** dès que les trois bloquants sont levés.

> *Mise à jour de l'intégrateur (26/10/2026 21:25)* — état des trois bloquants après l'audit :
> 1. **Politique de confidentialité** : page rédigée (`privacy-policy/index.html`, EN + FR, AdMob et identifiant publicitaire mentionnés), URL câblée dans `GameConfig.PRIVACY_POLICY_URL` et `listing.json`, bouton visible dans Réglages (capture 07 refaite). **Reste** : pousser le dépôt et activer GitHub Pages (main, racine) pour que `https://innocraftcode.github.io/Tidy-Tubes/privacy-policy/` réponde.
> 2. **Signature** : clé d'upload créée à la demande de l'utilisateur, `keystore.properties` en place, AAB 8,6 Mo signé (`jarsigner` : jar verified). **Levé.**
> 3. **IDs AdMob** : `admob.properties` rempli (3 IDs de production, même éditeur), build 21:21 avec `ADS_ENABLED=true` et le bon ID d'application dans le manifeste. **Levé.** Nouveau constat sur l'AVD : UMP répond « no form(s) configured for the input app ID » → **les messages de consentement RGPD / États-Unis doivent être créés et publiés dans AdMob avant publication** (sinon aucun consentement recueilli en EEE/UK/États concernés).
> Statut effectif : **PRÊT SOUS CONDITIONS** (Pages à activer, messages UMP à publier, déclarations Play Console à saisir).

Bloquants (aucun ne peut être levé par un agent) :

1. **Aucune politique de confidentialité** : `GameConfig.PRIVACY_POLICY_URL` est vide, le bouton de l'écran Réglages est masqué, `listing.json` contient un espace réservé. Obligatoire pour toutes les applications (politique Données utilisateur, lue 2026-10-26).
2. **AAB non signé** : pas de `keystore.properties` sur cette machine ; l'AAB ne contient aucune signature (`META-INF/*.RSA|.SF` absents). Play refuse un bundle non signé.
3. **Le build actuel n'est pas celui qu'il faut téléverser** : sans `admob.properties`, `BuildConfig.ADS_ENABLED=false` **et** le manifeste release embarque l'ID d'application AdMob de démonstration de Google (`ca-app-pub-3940256099942544~3347511713`, vérifié par `aapt2 dump xmltree`). Un build « avec AdMob » exige `admob.properties` avec des IDs de production, puis une reconstruction. Téléverser l'artefact actuel consommerait le versionCode 1 pour une version sans publicité avec un ID de démo dans le manifeste.

Deux états sont distingués dans tout le rapport :

| État | Définition | Déclarations Play cohérentes |
|---|---|---|
| **Build actuel (pubs off)** | AAB/APK du 2026-10-26 20:44, construit sans `admob.properties` : `DisabledAdProvider`, aucun appel SDK, aucune requête réseau, mais SDK et permissions fusionnées présents | Contient des annonces : incohérent quel que soit le choix (SDK présent mais inactif) — ne pas téléverser |
| **Release configurée (pubs on)** | Même code, reconstruit avec `admob.properties` (IDs de production) et `keystore.properties` | Contient des annonces = Oui ; ID publicitaire = Oui ; Data safety selon §8 |

## 1. Sources officielles consultées (toutes lues le 2026-10-26)

| Page | Ce qui a été retenu |
|---|---|
| https://developer.android.com/google/play/requirements/target-sdk | « New apps and app updates must target Android 16 (API level 36) or higher » ; « Starting August 31 2026 » ; extension possible jusqu'au 2026-11-01. Page datée « Last updated 2026-10-16 UTC ». |
| https://developer.android.com/guide/practices/page-sizes | « all apps targeting Android 15 (API level 35) and higher must support 16 KB memory page sizes on 64-bit devices on Google Play. Starting February 1, 2027, if your app updates don't support 16 KB memory page sizes, you won't be able to release these updates. » Vérifications : `zipalign -c -P 16 -v 4`, segments ELF LOAD alignés 2**14, AGP ≥ 8.5.1. |
| https://support.google.com/googleplay/android-developer/answer/9866151 | Icône 512×512 PNG 32 bits ≤ 1024 Ko ; bannière 1024×500 JPEG/PNG 24 bits sans alpha ; captures : min 2, 320–3840 px, grand côté ≤ 2× petit côté, max 8 par type ; vidéo = URL YouTube unique, publique ou non répertoriée, sans publicité, non restreinte par l'âge, intégrable ; pas de « Best », « #1 », « Free », « Download now » dans les visuels ; texte alternatif ≤ 140 caractères. |
| https://support.google.com/googleplay/android-developer/answer/10787469 | Politique de confidentialité obligatoire pour **toutes** les applications ; les données collectées par les SDK tiers doivent être déclarées ; l'identifiant publicitaire se déclare sous « Device or other IDs » ; déclaration inexacte → « blocked updates or removal ». |
| https://support.google.com/googleplay/android-developer/answer/14151465 | Comptes personnels créés après le 2023-11-13 : « a minimum of 12 testers who have been opted in continuously for at least 14 days » en test fermé avant de demander l'accès production ; revue ≈ 7 jours. |
| https://support.google.com/googleplay/android-developer/answer/9857753 (Annonces) | Interdits : interstitiels inattendus, « at the beginning of a level », avant l'écran de chargement, non fermables après 15 s, à la sortie de l'app ; autorisé : après un écran de score ; « rewarded ads which are explicitly opted-in by users » exemptées ; AAID uniquement pour publicité/analytics, respecter l'opt-out. |
| https://support.google.com/googleplay/android-developer/answer/9893335 (Familles) | S'applique si les enfants sont **un** des publics cibles ; alors SDK publicitaires auto-certifiés Familles, pas de ciblage par centres d'intérêt, écran d'âge neutre pour public mixte ; Google « reserves the right to conduct its own review » du public déclaré. |
| https://support.google.com/googleplay/android-developer/answer/6048248 (ID publicitaire) | Utiliser l'AAID « in place of any other device identifiers » pour la publicité ; permission `com.google.android.gms.permission.AD_ID` requise à partir d'Android 13 (peut être fusionnée par le SDK). |
| https://support.google.com/googleplay/android-developer/answer/10144311 (Données utilisateur) | « All apps must post a privacy policy link in the designated field within Play Console » **et** « a privacy policy link or text within the app itself » ; Data safety doit être cohérente avec la politique ; disclosure proéminente si usage inattendu. |
| https://support.google.com/googleplay/android-developer/answer/9898842 (Métadonnées) | Titre ≤ 30 caractères ; pas d'emoji ni de caractères spéciaux répétés ; pas de mots-clés répétitifs, pas de MAJUSCULES hors marque, pas de « #1 / Best of / free for limited time », pas de témoignages anonymes. |
| https://support.google.com/googleplay/android-developer/answer/9859655 (Classification) | Questionnaire IARC obligatoire pour toute nouvelle app ; à refaire si le contenu change ; « Misrepresentation … may result in its removal or suspension ». |
| https://developers.google.com/admob/android/privacy/play-data-disclosure | Divulgation **du SDK legacy 25.5.0** (la page ne couvre pas le Next-Gen) : adresse IP, interactions produit, diagnostics, identifiants (AAID, app set ID) — collectés **et** partagés, TLS. « Last updated 2026-10-26 UTC ». |
| https://developers.google.com/admob/android/next-gen/rel-notes | Next-Gen **1.6.0 publié le 2026-10-22** ; 1.5.0 date du 2026-09-24. |
| https://developers.google.com/admob/android/privacy/release-notes | UMP 4.0.0 (2025-10-31) est la version la plus récente ; minSdk 23. |

Pages **non consultables** (HTTP 404 aujourd'hui) : `answer/9878052` (déclaration « Contient des annonces ») et `admob/android/early-access/release-notes`. `answer/9859152` ne contient pas les déclarations App content. Les contrôles qui en dépendent sont marqués NOT VERIFIED sur le point précis.

## 2. Preuves collectées par l'auditeur

| Preuve | Commande / fichier | Résultat |
|---|---|---|
| Badging APK release | `aapt2 dump badging android-release-unsigned.apk` | package `com.ben.laama.sortpuzzle` versionCode 1 versionName 1.0.0 ; minSdk 24 ; targetSdk 36 ; compileSdk 37 ; label « Tidy Tubes » ; ABIs arm64-v8a, armeabi-v7a, x86, x86_64 ; 7 `uses-permission` (liste §5) |
| ID AdMob dans le manifeste release | `aapt2 dump xmltree --file AndroidManifest.xml` | `com.google.android.gms.ads.APPLICATION_ID = ca-app-pub-3940256099942544~3347511713` (**ID de démo Google**) |
| Provenance des permissions | `android/build/outputs/logs/manifest-merger-release-report.txt` | INTERNET : manifeste app ; ACCESS_NETWORK_STATE, READ_BASIC_PHONE_STATE, AD_ID : `ads-mobile-sdk:1.5.0` ; WAKE_LOCK, FOREGROUND_SERVICE : `androidx.work:work-runtime:2.7.0` (tiré par le SDK) ; `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` : `androidx.core:1.13.1` ; RECEIVE_BOOT_COMPLETED **retiré** par le SDK (`tools:node="remove"`), absent du manifeste fusionné |
| zipalign 16 Ko | `zipalign -c -P 16 4 <apk>` | exit 0 (Verification successful) |
| Alignement ELF | lecture des en-têtes ELF de chaque `.so` de l'APK **et** de l'AAB (8 bibliothèques × 2) | tous les segments PT_LOAD `p_align = 16384` |
| Signature AAB/APK | listage des entrées ZIP | aucun `META-INF/MANIFEST.MF`, `*.SF`, `*.RSA` ni bloc v2/v3 → **non signé** |
| Dépendances embarquées | `BUNDLE-METADATA/com.android.tools.build.libraries/dependencies.pb` extrait de l'AAB | `ads-mobile-sdk`, `user-messaging-platform`, `play-services-ads-identifier`, `play-services-appset`, `play-services-base/basement/tasks/cronet`, `cronet-*`, `okhttp`, `tink-android`, `work-runtime`, `room-runtime`, `webkit`, `browser`, `gdx*`. **Aucun `play-services-ads` legacy.** |
| Secrets / SDK | `powershell -ExecutionPolicy Bypass -File .\tools\validation\validate-workspace.ps1` | 0 échec, 1 avertissement (template `templates/libgdx-2d` en targetSdk 35 — hors périmètre de cette release) ; `games/sortpuzzle` : targetSdk 36, aucun motif de secret dans 215 fichiers |
| Fichiers sensibles | listage `games/sortpuzzle/` | `keystore.properties`, `admob.properties`, `*.jks` **absents** ; `.gitignore` les exclut ; le dossier n'est pas un dépôt git (pas de tag de release possible) |
| Visuels | `tools\store\validate-store-assets.ps1 -Game sortpuzzle -ReportPath <scratchpad>` (rapport écrit hors projet) + mesure System.Drawing | overall PASS ; icône 512×512 PNG 32 bits 180 Ko ; bannière 1024×500 PNG 24 bits ; 7 captures 1080×1920 PNG 24 bits ; icônes launcher OK ; 11 points `humanReview` jamais auto-validés |
| Textes | mesure de `listing.json` | en-US : titre 28, courte 76, longue 1063, notes 102 ; fr-FR : titre 28, courte 77, longue 1297, notes 118 (limites 30 / 80 / 4000 / 500) |
| Journaux | grep `Log\.|System.out|Gdx.app.(log|error)` sur `src/main` | 11 lignes dans `AdMobAdProvider` (messages d'erreur SDK, états de chargement), 2 dans `SoundManager`, 1 dans `SaveData`, 1 dans `NoOpAdProvider` (desktop). Aucun identifiant, aucune donnée joueur |
| Réseau | grep `HttpRequest|Gdx.net|java.net|Socket|okhttp|WebView` | une seule occurrence : `Gdx.net.openURI(PRIVACY_POLICY_URL)` (ouvre le navigateur) |
| Identifiants / capteurs | grep `ANDROID_ID|Settings.Secure|AdvertisingId|getDeviceId|TelephonyManager|Location|Camera|Firebase|Analytics` | 0 occurrence dans le code du jeu |
| Stockage | grep `Preferences|Gdx.files` | `SaveData` → `Gdx.app.getPreferences("com.ben.laama.sortpuzzle.save")` ; polices et sons en `internal` (assets) ; aucun fichier externe |

**Écart avec le brief** : le brief cite `play-store/sortpuzzle/store-assets/validation-report/asset-validation.json` (PASS). Ce dossier `store-assets/` **n'existe pas** sur le disque ; le kit exporté n'a pas été produit. La validation a été refaite sur le kit source (`play-store/sortpuzzle/`) : PASS.
> *Note de l'intégrateur (26/10/2026 21:00)* : l'auditeur a cherché le kit sous `play-store/sortpuzzle/`. Le kit exporté se trouve à la **racine du workspace** (`E:\mobile andoird\store-assets\`, 38 fichiers, 10,3 Mo), régénéré après l'audit par `tools\store\export-store-assets.ps1` avec `validation-report\asset-validation.json` = PASS. Le contrôle 56 est donc satisfait.

Non refait par l'auditeur (interdit par le brief : pas de build Gradle, pas d'émulateur relancé) : `:core:test`, `:android:printAdConfig`, exécution sur l'AVD Pixel_10. Ces résultats sont repris de `docs/admob-integration-current-release.md` et marqués NOT VERIFIED là où ils comptent.

## 3. Applicabilité des familles de politiques

| Famille | Applicable ? | Justification |
|---|---|---|
| Annonces | **Oui** | GMA Next-Gen + UMP embarqués ; interstitiels et vidéos récompensées |
| ID publicitaire | **Oui** | permission `AD_ID` fusionnée par le SDK ; `play-services-ads-identifier` présent |
| Données utilisateur / Data safety | **Oui** | le SDK collecte identifiants, IP, interactions, diagnostics (release configurée) |
| Politique de confidentialité | **Oui** | obligatoire pour toute app, a fortiori avec SDK publicitaire |
| Familles | **Non, à condition** de déclarer un public cible ≥ 13 ans et de ne pas cibler les enfants ; devient applicable si un groupe < 13 ans est coché |
| Contenu généré par les utilisateurs | Non applicable | aucune saisie, aucun partage |
| Paiements / achats intégrés | Non applicable | pas de facturation ; les « pièces » sont uniquement gagnées en jeu (`SaveData.recordWin`) |
| Formulaire de déclaration des permissions | Non applicable | aucune permission sensible (SMS, appels, localisation, stockage, accessibilité, etc.) |
| Santé, finance, gouvernement, VPN, jeux d'argent | Non applicable | — |
| Abus d'appareil et de réseau | Non applicable | pas de téléchargement de code, pas de service en arrière-plan propre ; `androidx.work` n'est utilisé que par le SDK |

## 4. Tableau des contrôles

Légende : **PASS** conforme ; **FAIL** bloquant ou non conforme ; **WARNING** risque résiduel ou point à corriger avant publication ; **NOT VERIFIED** impossible à vérifier par l'auditeur ; **NEEDS USER** action que seul l'utilisateur peut faire ; **N/A** sans objet.

### 4.1 Cible API, 16 Ko, bundle, signature, identité

| # | Contrôle | Statut | Preuve | Corrigé par / quand | Risque résiduel |
|---|---|---|---|---|---|
| 1 | targetSdk ≥ 36 (nouvelles apps depuis 2026-08-31) | **PASS** | badging : targetSdk 36, compileSdk 37 ; `android/build.gradle.kts` l. 56-61 ; `validate-workspace` OK | senior-android-developer (passage 35 → 36 documenté dans le commentaire du build) | AGP 8.11.1 testé jusqu'à compileSdk 36 ; `android.suppressUnsupportedCompileSdk=37` masque l'avertissement. Re-vérifier à chaque mise à jour d'AGP. |
| 2 | minSdk 24, 64 bits inclus | **PASS** | badging : minSdk 24 ; `native-code: arm64-v8a …` | — | UMP 4.0.0 exige minSdk 23 : OK. |
| 3 | Pages 16 Ko (alignement ELF + zip) | **PASS** | `p_align=16384` sur 8/8 `.so` (APK et AAB) ; `zipalign -P 16` exit 0 ; AGP 8.11.1 ≥ 8.5.1 ; test sur AVD 16 Ko rapporté par l'équipe (NOT VERIFIED par l'auditeur) | libGDX 1.13.1 fournit des natifs alignés ; rien à faire côté projet | `bundletool dump config` non exécuté (outil absent) : l'indicateur `PAGE_ALIGNMENT_16K` du bundle est NOT VERIFIED. Le build-tools 36 l'applique normalement. |
| 4 | Format App Bundle | **PASS** | `android-release.aab` 5 143 059 octets ; `BundleConfig.pb`, `base/native.pb`, `proguard.map` embarqués | — | — |
| 5 | AAB signé avec clé d'upload | **FAIL** | aucune entrée de signature dans l'AAB ; `keystore.properties` absent ; README §« Android release » décrit la procédure | **personne** : à faire par l'utilisateur (générer un keystore d'upload, le sauvegarder hors dépôt, créer `keystore.properties`, reconstruire avec `build-android.ps1 -Project sortpuzzle -Task Bundle`, vérifier `jarsigner -verify`) | Perte du keystore d'upload = procédure de réinitialisation Google ; sauvegarder dans un gestionnaire de mots de passe + copie hors ligne. |
| 6 | Play App Signing | **NEEDS USER** | s'active lors de la création de l'app dans Play Console (page 9859152 : « Accept the Play App Signing Terms of Service ») | — | — |
| 7 | applicationId définitif, pas `com.example.*` | **PASS** | `com.ben.laama.sortpuzzle` (build.gradle.kts l. 59, badging, listing.json) | — | Permanent après le premier upload ; `namespace` identique. |
| 8 | versionCode / versionName cohérents | **PASS** avec réserve | versionCode 1, versionName 1.0.0 dans badging, `build.gradle.kts`, racine `version = "1.0.0"`, `GameConfig.VERSION_NAME`, `listing.json` | — | `GameConfig.VERSION_NAME` est synchronisé **à la main** (commentaire l. 18) : risque de divergence aux prochaines versions. Si l'artefact actuel (pubs off) est téléversé, le build pubs-on devra passer en versionCode 2. |
| 9 | Nom d'app ≤ 30 caractères | **PASS** | `strings.xml` « Tidy Tubes » (10) ; titres listing 28/28 | — | — |
| 10 | R8 / shrink activés, règles de conservation | **PASS** | `isMinifyEnabled=true`, `isShrinkResources=true`, `proguard-rules.pro` conserve `SaveData` et les constructeurs scene2d (bug trouvé le 2026-10-26 sur le test release) | libgdx-game-developer | La sauvegarde a été testée sur une installation fraîche seulement ; la mise à jour par-dessus une version précédente n'existe pas encore (première release). |

### 4.2 Permissions et dépendances

| # | Contrôle | Statut | Preuve | Corrigé par | Risque résiduel |
|---|---|---|---|---|---|
| 11 | `INTERNET` | **PASS** | déclarée par l'app (manifeste l. 6) ; justification : chargement des annonces et ouverture de la politique de confidentialité | — | Nécessaire même en build « pubs off » tant que le SDK est présent ; sinon à retirer (doc §3.2). |
| 12 | `ACCESS_NETWORK_STATE` | **PASS** | fusionnée par `ads-mobile-sdk:1.5.0` ; permission normale, usage SDK (état réseau avant requête) | — | — |
| 13 | `READ_BASIC_PHONE_STATE` | **PASS** | fusionnée par `ads-mobile-sdk:1.5.0` ; permission **normale** (API 33+), sans formulaire Play | — | Peut surprendre un relecteur ; l'origine SDK est documentée dans le manifeste merger report. |
| 14 | `com.google.android.gms.permission.AD_ID` | **PASS** | fusionnée par le SDK ; exigée par la politique ID publicitaire pour API 33+ | — | Implique la réponse « Oui » à la déclaration ID publicitaire (§4.6). |
| 15 | `WAKE_LOCK`, `FOREGROUND_SERVICE` | **PASS** | fusionnées par `androidx.work:work-runtime:2.7.0`, dépendance du SDK ; aucun service propre à l'app ; aucun `FOREGROUND_SERVICE_*` typé (pas de déclaration de type de service en Console) | — | `work-runtime 2.7.0` est ancien mais imposé par le SDK. |
| 16 | `…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | **PASS** | permission de signature privée d'`androidx.core` ; standard | — | — |
| 17 | `RECEIVE_BOOT_COMPLETED` | **PASS** | demandée par `work-runtime`, **retirée** par le manifeste du SDK (`tools:node="remove"`) ; absente du badging | Google (SDK) | — |
| 18 | Aucune permission dangereuse / sensible | **PASS** | badging : aucune permission runtime | — | — |
| 19 | Versions SDK publicitaires | **PASS** (corrigé par l'intégrateur) | audit : GMA 1.5.0 alors que 1.6.0 est paru le 2026-10-22. Mis à jour le 26/10 21:07 : `gmaNextGen = "1.6.0"`, 90 tests verts, AAB 5,2 Mo reconstruit, template recompilé, SDK initialisé et annonces chargées sur l'AVD ; UMP 4.0.0 = dernière | intégrateur | Aucun. |
| 20 | Absence du SDK legacy `play-services-ads` | **PASS** | `dependencies.pb` : seuls `play-services-ads-identifier`, `-appset`, `-base`, `-basement`, `-tasks`, `-cronet` (dépendances du Next-Gen) | — | — |
| 21 | Secrets hors dépôt | **PASS** | `validate-workspace` 0 échec ; `.gitignore` couvre `keystore.properties`, `admob.properties`, `*.jks` ; `admob.properties.example` ne contient que des zéros ; IDs de démo seulement dans le build type `debug` et comme repli de manifeste | — | Le dossier n'est pas versionné (pas de `.git`) : aucune protection par l'historique, mais aucun risque de fuite par commit non plus. |

### 4.3 Journaux, stockage, réseau, données personnelles

| # | Contrôle | Statut | Preuve | Risque résiduel |
|---|---|---|---|---|
| 22 | Journaux sans donnée sensible | **PASS** | `AdMobAdProvider` : `Log.i/w` avec messages d'erreur SDK uniquement ; `SaveData.load` logue le message d'exception d'une sauvegarde corrompue (pas le contenu) ; `SoundManager` logue des chemins d'assets | Le SDK lui-même imprime l'ID d'appareil haché en debug (comportement Google, non contrôlable). |
| 23 | Stockage local | **PASS** | `SaveData` : un blob JSON dans `Preferences` (`com.ben.laama.sortpuzzle.save`) ; champs : niveaux, pièces, boosters, réglages audio, tutoriel vu ; aucun identifiant | `allowBackup="true"` + `fullBackupContent="true"` : la progression peut être copiée par la sauvegarde Android. Aucune donnée personnelle ; impact Data safety jugé nul mais NOT VERIFIED contre la FAQ Data safety. `dataExtractionRules` absent (avertissement lint, pas une politique). |
| 24 | Réseau | **PASS** | aucune requête du jeu ; seul `Gdx.net.openURI` (navigateur) ; tout le trafic réseau provient du SDK (cronet/okhttp, TLS) | — |
| 25 | Données personnelles collectées par l'app | **PASS** (aucune) | aucun compte, aucune saisie, aucun identifiant lu (grep §2) | — |
| 26 | Capteurs | **PASS** | `AndroidLauncher` désactive accéléromètre, boussole, gyroscope | — |

### 4.4 SDK publicitaire et consentement

| # | Contrôle | Statut | Preuve | Corrigé par | Risque résiduel |
|---|---|---|---|---|---|
| 27 | SDK confiné derrière `AdProvider` | **PASS** | seul `android/.../AdMobAdProvider.java` importe `com.google.android.libraries.ads` / `com.google.android.ump` ; `AdProvidersTest` (5 tests, NOT VERIFIED par l'auditeur) | — | — |
| 28 | UMP sollicité à chaque lancement | **PASS** | `Main.create` → `ads.initialize()` → `gatherConsent()` : `requestConsentInfoUpdate` puis `loadAndShowConsentFormIfRequired` | — | — |
| 29 | Requêtes seulement si `canRequestAds()` | **PASS** | `startSdkIfAllowed()` l. 123-139 : `consent.canRequestAds()` + `AtomicBoolean` ; `MobileAds.initialize` sur un thread dédié | — | Appel immédiat de `startSdkIfAllowed()` après la demande (l. 120) : s'appuie sur le consentement de la session précédente, conforme à la doc UMP. |
| 30 | Point d'entrée « Options de confidentialité » | **PASS** avec réserve | `SettingsScreen` l. 65-71 : carte affichée si `getPrivacyOptionsRequirementStatus()==REQUIRED` | — | La carte est construite à l'ouverture de l'écran : si l'utilisateur ouvre Réglages avant la réponse UMP (hors ligne au lancement), l'entrée manque jusqu'à la prochaine visite. Mineur. |
| 31 | Messages GDPR / États américains **publiés** dans AdMob | **NEEDS USER** | `docs/admob-integration-current-release.md` §4 ; UMP ≥ 3.0.0 : `canRequestAds` renvoie `true` **sans message configuré** (notes de version UMP lues ce jour) | — | **Risque élevé** : sans message publié, les annonces en EEE/UK sont servies sans consentement → violation RGPD / politique Google. À faire avant tout test fermé avec IDs de production. |
| 32 | Récompense accordée une seule fois, après confirmation SDK | **PASS** | `RewardGate` enveloppe `onUserEarnedReward` + `onAdDismissedFullScreenContent` ; `RewardGateTest` 4 tests (NOT VERIFIED par l'auditeur) ; objets d'annonce à usage unique | — | — |
| 33 | Placement des interstitiels conforme à la politique Annonces | **PASS** | `GameScreen.afterBreak` l. 1073-1080 : uniquement après la carte « niveau réussi », sur appui NEXT/REPLAY ; `AdPacing` : jamais au 1er break, tous les 3 niveaux, ≥ 90 s ; jamais au début d'un niveau, jamais à la sortie, jamais avant le chargement ; la navigation a toujours lieu (`then` passé au SDK) | — | Les durées « fermable en 15 s » dépendent de l'annonce servie (Google). |
| 34 | Vidéos récompensées opt-in, repli honnête | **PASS** | `RewardOffer.decide` : vidéo si prête, sinon pièces, sinon message ; `onFailed` → toast « Video closed early — no hint this time » ; boutons jamais morts (`showOutOfMovesDialog`) | — | — |
| 35 | IDs de démo seulement en debug | **PASS** / **WARNING build actuel** | `build.gradle.kts` l. 94-122 : debug → IDs de démo ; release → `admob.properties` ; **mais** le placeholder manifeste release retombe sur `testAppId` quand le fichier manque (l. 116), ce que confirme l'APK | — | Le build actuel contient l'ID d'application de démo dans son manifeste. Inerte (SDK jamais initialisé), mais **ne pas téléverser**. |
| 36 | Build « pubs off » n'émet aucune requête | **PASS** (code) | `AdProviders.forAndroid(null)` → `DisabledAdProvider` ; `createNetwork()` renvoie `null` si `!BuildConfig.ADS_ENABLED` | — | Les SDK restent sur le classpath : aucun `ContentProvider`/`Initializer` publicitaire n'a été vérifié au démarrage (le manifeste fusionné en déclare — `androidx.startup`, `profileinstaller`, services du SDK). Un trafic de télémétrie passif n'est pas exclu : NOT VERIFIED sans capture réseau. |
| 37 | Pas d'ID d'appareil de test en release | **PASS** | `TEST_DEVICE_IDS` = "" en release ; `ConsentDebugSettings` seulement si `config.debug` | — | — |

### 4.5 Data safety, politique de confidentialité, déclarations App content

| # | Contrôle | Statut | Preuve | Corrigé par | Risque résiduel |
|---|---|---|---|---|---|
| 38 | Politique de confidentialité hébergée, liée en Console et dans l'app | **FAIL** | `GameConfig.PRIVACY_POLICY_URL = ""` ; bouton masqué (`SettingsScreen` l. 82) ; `listing.json` : « à compléter » ; modèle disponible `shared/privacy/privacy-policy-template.md` (mentionne AdMob, AAID, IP, UMP) | **personne** | Bloquant : la politique Données utilisateur l'exige pour toute app. Le texte « see the privacy policy » du card À propos renvoie aujourd'hui vers rien (voir #52). |
| 39 | Data safety — release configurée (pubs on) | **NEEDS USER** (mapping prêt) | `shared/privacy/data-safety-worksheet.md` + divulgation Google (legacy 25.5.0, lue ce jour) : Localisation approximative (IP) — collectée, partagée ; Activité dans l'app : interactions — collectées, partagées ; Infos et performances : plantages, diagnostics — collectés, partagés ; Appareil ou autres ID (AAID, app set ID) — collectés, partagés ; finalités : publicité, analyse, prévention de la fraude ; chiffré en transit ; suppression : désinstallation (aucune donnée côté développeur) ; **données du jeu : non collectées** | — | La page de divulgation Google ne couvre **que le SDK legacy** ; aucune page équivalente pour le Next-Gen n'a pu être lue (NOT VERIFIED). Déclarer comme le legacy est l'hypothèse la plus prudente. |
| 40 | Data safety — build actuel (pubs off) | **WARNING** | aucun appel SDK, donc aucune collecte ; mais SDK et permissions présents | — | Déclarer « aucune collecte » puis activer les pubs dans l'update suivante impose de refaire Data safety, Annonces, ID publicitaire et la politique. Choisir **un** état avant le premier upload. |
| 41 | « Contient des annonces » | **NEEDS USER** | release configurée : **Oui** ; `listing.json.containsAds` prêt ; page de référence 9878052 injoignable (NOT VERIFIED sur le libellé exact) | — | « Non » avec un SDK publicitaire embarqué = déclaration inexacte. |
| 42 | Déclaration ID publicitaire | **NEEDS USER** | permission `AD_ID` dans le manifeste ; `play-services-ads-identifier` présent → répondre **Oui**, usages « Publicité ou marketing » et « Analyse » | — | Une réponse « Non » avec `AD_ID` présent est détectée automatiquement par Play. |
| 43 | Classification du contenu (IARC) | **NEEDS USER** | aucune violence, sexualité, achat ; contient des annonces ; attendu « Tout public / PEGI 3 » (`listing.json.contentRating`), à confirmer par le questionnaire | — | Refaire le questionnaire si les annonces ou le contenu changent. |
| 44 | Public cible et contenu | **NEEDS USER** — recommandation **13 ans et plus** | `listing.json.targetAudience` ; visuels colorés mais pas de personnage, de vocabulaire ni d'imagerie enfantine ; pas de cartoon violence | — | **WARNING** : un jeu de tri de couleurs « peut attirer » les enfants ; Google se réserve le droit de requalifier. Si un groupe < 13 ans est coché : politique Familles (SDK auto-certifié, pas de ciblage par intérêts, écran d'âge neutre) — non prévu. |
| 45 | Accès à l'application | **NEEDS USER** | aucune connexion, aucun code : « Toutes les fonctionnalités sont accessibles sans accès spécial » | — | — |
| 46 | Formulaire de déclaration des permissions | **N/A** | aucune permission sensible | — | — |
| 47 | Cohérence politique ↔ Data safety ↔ texte in-app | **WARNING** | le card À propos annonce « Ads are served by Google AdMob, which may use your device's advertising ID » (cohérent) ; la politique n'existe pas encore pour être comparée | — | À re-vérifier une fois la politique rédigée (elle doit citer AdMob, l'AAID, l'IP/localisation approximative, les diagnostics, et le mécanisme de consentement UMP). |

### 4.6 Fiche Play Store et visuels

| # | Contrôle | Statut | Preuve | Risque résiduel |
|---|---|---|---|---|
| 48 | Titre ≤ 30, sans emoji ni caractères spéciaux répétés | **PASS** | « Tidy Tubes: Ball Sort Puzzle » (28) ; « Tidy Tubes : Tri de Couleurs » (28) | Le sous-titre « Ball Sort Puzzle » est un terme générique de genre, pas un mot-clé de marque tierce. |
| 49 | Descriptions : limites, pas de bourrage, pas de fausses promesses | **PASS** | courte 76/77 ≤ 80 ; longue 1063/1297 ≤ 4000 ; mise en forme en puces, pas de répétition de mots-clés, pas de « #1 / Best of / Download now » ; « best possible move count » est descriptif (par) ; « Contains ads … No purchases » est **exact** pour la release configurée ; « every one of them has a solution » est étayé par la construction (`LevelGeneratorTest`) | Pour un build **pubs off**, « Contains ads » deviendrait inexact : encore une raison de ne téléverser que la release configurée. « Offline » est vrai pour le jeu, pas pour les annonces : acceptable mais surveiller les retours. |
| 50 | Notes de version ≤ 500 | **PASS** | 102 / 118 caractères | — |
| 51 | Aucune mention « sans publicité » dans l'app | **PASS** avec réserve | grep : aucune chaîne utilisateur « no ads » ; le card À propos dit « Free with ads » ; seule la **Javadoc** de `SettingsScreen` (l. 19) parle encore de « no ads » (commentaire obsolète, invisible) | Nettoyer le commentaire pour éviter une régression de texte. |
| 52 | Texte À propos renvoyant à une politique inexistante | **WARNING** | `SettingsScreen` l. 78-81 : « … see the privacy policy. » alors qu'aucun bouton n'est affiché tant que l'URL est vide | Se corrige de lui-même en renseignant `PRIVACY_POLICY_URL` (#38). |
| 53 | Icône 512×512 PNG 32 bits ≤ 1 Mo | **PASS** | 512×512, `Format32bppArgb`, 180 Ko (mesuré + validateur) | Revue humaine : lisible à 48 px, pas de badge. |
| 54 | Bannière 1024×500 sans alpha | **PASS** | 1024×500, `Format24bppRgb`, 321 Ko | Revue humaine : pas de texte promotionnel ; éléments clés au centre. |
| 55 | Captures téléphone | **PASS** | 7 × 1080×1920 (9:16), PNG 24 bits, 250–589 Ko ; ≥ 3 portrait 1080×1920 conseillé pour les jeux : satisfait ; alt text présent, ≤ 140 caractères (le plus long ≈ 139) | Revue humaine : vraie UI, pas d'overlay. La capture 07 montre le card À propos **sans** bouton de politique : à refaire après #38 pour refléter l'app réelle. |
| 56 | Kit exporté `store-assets/` | **PASS** (corrigé par l'intégrateur) | l'auditeur a cherché sous `play-store/sortpuzzle/` ; le kit est à la racine du workspace `store-assets/` (38 fichiers), `validation-report/asset-validation.json` overall PASS, régénéré le 26/10 21:00 | Aucun. |
| 57 | Vidéo promotionnelle | **NEEDS USER** | fichiers locaux `video/promo-portrait-1080x1920.mp4` (3,3 Mo) et `promo-landscape-1920x1080.mp4` (2,4 Mo) ; Play exige une **URL YouTube** unique, publique ou non répertoriée, sans monétisation, non restreinte par l'âge, intégrable ; gameplay réel dès les 10 premières secondes | Facultatif ; si publiée, vérifier que YouTube n'insère pas de pubs (droits musicaux : la musique est synthétisée maison, pas de revendication attendue). |
| 58 | Catégorie, e-mail de contact, site | **NEEDS USER** | catégorie « Jeux > Réflexion » ; e-mail et site : « à compléter » dans `listing.json` | Le site est requis par AdMob pour `app-ads.txt`, pas par Play. |
| 59 | Document Word de la fiche | **PASS** | régénéré 20:46 après `listing.json` ; un fichier de verrou `~$dy Tubes - Fiche Google Play.docx` indique que Word a le document ouvert | Fermer Word avant une prochaine régénération. |
| 60 | Licences tierces | **PASS** | Poppins sous SIL OFL (`assets/fonts/OFL.txt`), crédité dans l'app ; sons synthétisés maison | — |

### 4.7 Qualité, tests, pistes de test

| # | Contrôle | Statut | Preuve | Risque résiduel |
|---|---|---|---|---|
| 61 | Tests unitaires `:core:test` | **NOT VERIFIED** | 13 classes de test présentes (dont `AdProvidersTest`, `RewardGateTest`, `AdPacingTest`, `RewardOfferTest`) ; « all PASSED » selon `docs/admob-integration-current-release.md` ; non relancés (interdiction de Gradle) | — |
| 62 | Exécution sur appareil (debug, AVD Pixel_10 API 37, 16 Ko) | **NOT VERIFIED** | rapporté par l'équipe : UMP OK, SDK initialisé, rewarded + interstitial chargés (IDs démo), pas de crash ; APK release signé en debug lancé | Aucun test sur appareil physique, ni bas de gamme, ni formulaire de consentement EEE (nécessite un ID d'appareil de test ou une géolocalisation EEE). |
| 63 | Vitals / rapport de pré-lancement | **NEEDS USER** | disponibles après le premier upload en test interne | — |
| 64 | Piste de test interne | **NEEDS USER** | — | Premier upload = celui qui fixe l'applicationId et consomme le versionCode. |
| 65 | Exigence test fermé (compte personnel créé après 2023-11-13) | **NEEDS USER** / **NOT VERIFIED** | type et date de création du compte développeur inconnus ; si concerné : 12 testeurs opt-in **14 jours consécutifs**, puis « Apply for production », revue ≈ 7 jours | Planifier ≥ 3 semaines entre le premier test fermé et la production. |
| 66 | Déploiement progressif | **N/A** (première version) | — | Pour les mises à jour : 20 % puis 100 % après contrôle des vitals. |

## 5. Récapitulatif des permissions du manifeste fusionné (release)

| Permission | Origine | Type | Justification | Déclaration Console |
|---|---|---|---|---|
| `android.permission.INTERNET` | app (`AndroidManifest.xml` l. 6) | normale | annonces ; ouverture de l'URL de politique | aucune |
| `android.permission.ACCESS_NETWORK_STATE` | `ads-mobile-sdk:1.5.0` | normale | état réseau avant requête d'annonce | aucune |
| `android.permission.READ_BASIC_PHONE_STATE` | `ads-mobile-sdk:1.5.0` | normale (API 33+) | signaux réseau du SDK | aucune |
| `com.google.android.gms.permission.AD_ID` | `ads-mobile-sdk:1.5.0` | normale | lecture de l'identifiant publicitaire | **ID publicitaire = Oui** |
| `android.permission.WAKE_LOCK` | `androidx.work:2.7.0` (via SDK) | normale | tâches WorkManager du SDK | aucune |
| `android.permission.FOREGROUND_SERVICE` | `androidx.work:2.7.0` (via SDK) | normale | WorkManager (aucun service de premier plan typé) | aucune |
| `com.ben.laama.sortpuzzle.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | `androidx.core:1.13.1` | signature | protection des receivers dynamiques | aucune |

## 6. Actions manuelles réservées à l'utilisateur

Rien de ce qui suit ne doit être exécuté par un agent (règle 5 du workspace). Ordre recommandé.

**Avant la reconstruction**
1. Rédiger et héberger la politique de confidentialité (base : `shared/privacy/privacy-policy-template.md`, citer AdMob, AAID, IP/localisation approximative, diagnostics, UMP, « Privacy options », désinstallation = suppression). Renseigner l'URL dans `GameConfig.PRIVACY_POLICY_URL` et `listing.json.privacyPolicyUrl` (demander la modification à un agent développeur ; l'auditeur ne modifie rien).
2. Créer le compte AdMob (profil de paiement), l'application et deux blocs (rewarded, interstitial) ; copier les IDs dans `games/sortpuzzle/admob.properties` (jamais dans le dépôt ni dans le chat).
3. Dans AdMob > Confidentialité et messages : créer **et publier** le message RGPD (EEE/UK) et le message États américains. Sans cela, l'app sert des annonces sans consentement.
4. Générer un keystore d'upload, le sauvegarder (gestionnaire de mots de passe + copie hors ligne), créer `games/sortpuzzle/keystore.properties`.
5. Facultatif mais conseillé : passer GMA Next-Gen en 1.6.0 (`gradle/libs.versions.toml`) et relancer les tests.
6. Reconstruire : `.\tools\android-build\build-android.ps1 -Project sortpuzzle -Task Bundle`, puis `gradlew :android:printAdConfig` doit afficher `release: ADS_ENABLED=true`, `jarsigner -verify` OK, et refaire la capture 07 (card À propos avec le bouton Politique).
7. Enregistrer son téléphone comme appareil de test (`admob.testDeviceIds`, debug uniquement) et vérifier le formulaire UMP avec `DEBUG_GEOGRAPHY_EEA`. Ne jamais cliquer sur une annonce réelle.

**Play Console (création de l'app)**
8. Créer l'app, accepter les déclarations (Programme développeur, lois d'exportation US) et les conditions **Play App Signing**.
9. Fiche : titre, descriptions et notes depuis `listing.json` ; icône, bannière, captures depuis `play-store/sortpuzzle/` ; e-mail de contact ; site web (nécessaire pour `app-ads.txt`) ; vidéo YouTube si souhaitée ; catégorie Puzzle ; gratuit ; pays.
10. App content : URL de politique de confidentialité ; **Annonces = Oui** ; **ID publicitaire = Oui** (publicité, analyse) ; Data safety selon §4.5 #39 ; **Accès à l'application** : aucun accès spécial ; **Classification** : questionnaire IARC ; **Public cible** : 13 ans et plus, « non conçu pour les enfants » ; Applications gouvernementales / financières / santé : Non.
11. Téléverser l'AAB **signé, pubs on** en test interne ; installer, jouer, vérifier Vitals et le rapport de pré-lancement.
12. Si le compte est personnel et créé après le 2023-11-13 : test fermé avec ≥ 12 testeurs pendant 14 jours consécutifs, puis « Apply for production ».
13. Après publication : lier l'app AdMob à la fiche Play ; publier `app-ads.txt` à la racine du site déclaré.

## 7. Risques résiduels après levée des bloquants

| Risque | Probabilité | Impact | Atténuation |
|---|---|---|---|
| Google requalifie le public cible comme incluant des enfants (jeu de couleurs) | moyenne | rejet / passage sous Familles | garder un ton neutre, aucune imagerie enfantine dans les visuels ; répondre « non conçu pour les enfants » ; si demandé, basculer vers SDK auto-certifié + écran d'âge |
| Divulgation Data safety du SDK Next-Gen différente du legacy | faible | demande de correction Data safety | déclarer comme le legacy (plus large) ; re-vérifier la page de divulgation Google après chaque mise à jour du SDK |
| Annonces servies sans consentement en EEE (messages AdMob non publiés) | élevée tant que #31 non fait | violation RGPD, suspension AdMob | action 3 ci-dessus ; test avec `DEBUG_GEOGRAPHY_EEA` |
| Divergence versionName entre `GameConfig` et Gradle aux prochaines versions | moyenne | incohérence visible dans l'app | lire la version depuis `BuildConfig` ou un fichier généré (hors périmètre de cet audit) |
| Pas d'historique git | — | aucune traçabilité des releases | initialiser un dépôt avant le premier upload ; taguer `v1.0.0` |

## 8. Mapping Data safety résumé (release configurée)

| Catégorie Play | Type | Collecté | Partagé | Facultatif | Finalités | Source |
|---|---|---|---|---|---|---|
| Localisation | Approximative (dérivée de l'IP) | Oui | Oui | Non | Publicité, Analyse, Prévention de la fraude | GMA SDK |
| Activité dans l'app | Interactions dans l'app | Oui | Oui | Non | Publicité, Analyse | GMA SDK |
| Infos et performances | Journaux de plantage, Diagnostics | Oui | Oui | Non | Analyse, Prévention de la fraude | GMA SDK |
| Appareil ou autres ID | AAID, App set ID | Oui | Oui | Non (révocable via Android > Confidentialité > Annonces) | Publicité, Analyse, Prévention de la fraude | GMA SDK, `AD_ID` |
| Infos personnelles, Finances, Santé, Messages, Photos, Audio, Fichiers, Calendrier, Contacts, Historique | — | Non | Non | — | — | aucun accès |
| Progression du jeu | — | Non (reste sur l'appareil ; `SaveData`) | Non | — | — | app |

Chiffrement en transit : Oui (TLS, selon Google). Suppression : désinstallation ; aucune donnée détenue par le développeur. Données du jeu couvertes par la sauvegarde Android (si activée par l'utilisateur) : non personnelles.

---
Fin du rapport. Fichier produit : `E:\mobile andoird\docs\google-play-compliance-report.md`. Aucun autre fichier du projet n'a été modifié ; le rapport du validateur de visuels a été écrit dans le scratchpad de session, pas dans le projet.
