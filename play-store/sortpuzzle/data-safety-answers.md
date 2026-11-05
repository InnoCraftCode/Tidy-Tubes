# Sécurité des données (Play Console) - réponses pour Tidy Tubes à partir de la version 1.0.8

Formulaire : Play Console → Règles et programmes → Contenu de l'application → **Sécurité des données** → Gérer.
À refaire à chaque ajout de SDK. Base : AdMob (GMA Next-Gen + UMP), Google Play Games Services v2
(connexion facultative + classements), Firebase Analytics et Crashlytics (collecte activée seulement après
le consentement UMP, jamais en debug). Aucune donnée n'est collectée par nos propres serveurs : nous n'en avons pas.

## Vue d'ensemble

| Question | Réponse |
|---|---|
| Votre appli collecte-t-elle ou partage-t-elle des données utilisateur ? | **Oui** |
| Toutes les données sont-elles chiffrées en transit ? | **Oui** (HTTPS, SDK Google) |
| Proposez-vous un moyen de demander la suppression des données ? | **Oui** : « Reset progress » efface les données locales ; données Google via le compte Google (myaccount.google.com) et lien dans la politique de confidentialité. Indiquer l'URL de la politique : https://innocraftcode.github.io/Tidy-Tubes/privacy-policy/ |
| Conformité aux Règles relatives aux familles | Non (le jeu n'est pas destiné principalement aux enfants, filtrage publicitaire déjà déclaré dans « Public cible ») |
| Examen indépendant de la sécurité | Non |

## Types de données

### Localisation
| Donnée | Collectée | Partagée | Obligatoire | Finalités |
|---|---|---|---|---|
| Position approximative (déduite de l'adresse IP par AdMob) | Oui | Oui | Obligatoire (publicité) | Publicité ou marketing, Analyse |

### Infos et performances de l'appli
| Donnée | Collectée | Partagée | Obligatoire | Finalités |
|---|---|---|---|---|
| Journaux de plantage (Crashlytics) | Oui | Oui | Obligatoire | Analyse |
| Diagnostics (Crashlytics, Analytics, AdMob) | Oui | Oui | Obligatoire | Analyse, Publicité ou marketing |

### Activité dans l'appli
| Donnée | Collectée | Partagée | Obligatoire | Finalités |
|---|---|---|---|---|
| Interactions dans l'appli (événements de niveau, indices, pubs récompensées : Firebase Analytics) | Oui | Oui | Obligatoire | Analyse |
| Autres contenus générés par l'utilisateur (scores envoyés aux classements Play Games) | Oui | Oui | **Facultatif** (uniquement si connexion Play Games) | Fonctionnalités de l'appli |

### Identifiants de l'appareil ou autres
| Donnée | Collectée | Partagée | Obligatoire | Finalités |
|---|---|---|---|---|
| Identifiants de l'appareil ou autres (identifiant publicitaire pour AdMob ; ID d'instance d'application Firebase) | Oui | Oui | Obligatoire | Publicité ou marketing, Analyse, Fonctionnalités de l'appli |

### Infos personnelles
| Donnée | Collectée | Partagée | Obligatoire | Finalités |
|---|---|---|---|---|
| ID utilisateur (ID joueur et pseudo Google Play Games) | Oui | Oui | **Facultatif** (connexion Play Games) | Fonctionnalités de l'appli |

Aucune autre catégorie : pas de nom, e-mail, contacts, photos, fichiers, audio, santé, finances, historique web.

## Pourquoi « partagée = Oui »
Les SDK Google (AdMob, Firebase, Play Games) transmettent ces données à Google, un tiers au sens du formulaire.
AdMob et Analytics sont en « collecte éphémère : non » (Google les conserve selon ses règles).

## Points de cohérence à vérifier ailleurs dans la console
- **Contenu de l'application → Annonces** : « Oui, contient des annonces » (inchangé).
- **Contenu de l'application → Identifiant publicitaire** : « Oui » (AdMob), inchangé.
- **Public cible et contenu** : inchangé (13+ recommandé).
- **Politique de confidentialité** : URL ci-dessus ; la page mentionne désormais Play Games, Analytics et Crashlytics.
- Ces réponses correspondent au manifeste : `firebase_analytics_collection_enabled=false` et
  `firebase_crashlytics_collection_enabled=false` au démarrage, activés par `AndroidLauncher` après `canRequestAds()`.

## Import CSV (généré le 2026-11-04)
- `data-safety-import.csv` : fichier à importer tel quel (Sécurité des données → « Importer depuis un fichier CSV »).
  L'import **écrase** toutes les réponses déjà saisies ; relire chaque section après l'import.
- `data-safety-import-with-notes.csv` : même contenu avec les recommandations en tête (lecture seule, ne pas importer).
- `data-safety-template.csv` : export vierge du formulaire (777 lignes, export réel de la console du 2026 avec les questions de création de compte). Si Google change l'en-tête,
  exporter le formulaire depuis la console et remplacer ce fichier.
- Regénérer après tout changement de SDK : `.\tools\store\build-data-safety-csv.ps1` (les réponses sont dans le script).
- Hors CSV, à saisir à la main : URL de la politique de confidentialité, Familles = Non, examen indépendant = Non.
