# Sécurité des données (Play Console) - réponses pour Knotlight 0.7.0

Formulaire : Play Console → Règles et programmes → Contenu de l'application → **Sécurité des données**.
Base technique (vérifiée dans le code le 2026-11-10) : AdMob **GMA Next-Gen SDK 1.6.0 + UMP 4.0.0** via le
plugin Android `ThreadboundAds`, et la facturation Google Play (GodotGooglePlayBilling 3.3.0) pour le
produit `remove_ads`. **Pas** de Firebase, **pas** de Crashlytics, **pas** de Play Games, aucun serveur à nous,
aucun compte. La sauvegarde (`user://save.json`) ne quitte jamais l'appareil.
Permissions du manifeste : INTERNET, ACCESS_NETWORK_STATE, VIBRATE, plus AD_ID ajoutée par le SDK AdMob.

À refaire à chaque ajout de SDK. Vérifier les réponses AdMob contre la page officielle
https://developers.google.com/admob/android/privacy/play-data-disclosure avant d'envoyer.

## Vue d'ensemble

| Question | Réponse |
|---|---|
| Votre appli collecte-t-elle ou partage-t-elle des données utilisateur ? | **Oui** (SDK AdMob) |
| Toutes les données sont-elles chiffrées en transit ? | **Oui** (HTTPS, SDK Google) |
| Méthodes de création de compte | **Aucune** (pas de compte dans l'appli) |
| Proposez-vous un moyen de demander la suppression des données ? | **Oui** : les données locales sont effacées par « Reset progress » dans les réglages ou à la désinstallation ; les données traitées par Google via le compte Google (myaccount.google.com) et le lien dans la politique de confidentialité. URL à indiquer : https://innocraftcode.github.io/knotlight/privacy-policy/ |
| Conformité aux Règles relatives aux familles | **Non** (public cible 13+, voir app-content-answers.md) |
| Examen indépendant de la sécurité (MASA) | **Non** |

## Types de données

### Localisation
| Donnée | Collectée | Partagée | Éphémère | Obligatoire | Finalités |
|---|---|---|---|---|---|
| Position approximative (déduite de l'adresse IP par AdMob) | Oui | Oui | Non | Obligatoire | Publicité ou marketing, Analyse |

### Infos et performances de l'appli
| Donnée | Collectée | Partagée | Éphémère | Obligatoire | Finalités |
|---|---|---|---|---|---|
| Diagnostics (SDK AdMob) | Oui | Oui | Non | Obligatoire | Analyse, Publicité ou marketing |
| Journaux de plantage | **Non** (pas de Crashlytics) | | | | |

### Activité dans l'appli
| Donnée | Collectée | Partagée | Éphémère | Obligatoire | Finalités |
|---|---|---|---|---|---|
| Interactions dans l'appli (interactions avec les annonces, SDK AdMob) | Oui | Oui | Non | Obligatoire | Publicité ou marketing, Analyse |

### Identifiants de l'appareil ou autres
| Donnée | Collectée | Partagée | Éphémère | Obligatoire | Finalités |
|---|---|---|---|---|---|
| Identifiants de l'appareil ou autres (identifiant publicitaire, app set ID) | Oui | Oui | Non | Obligatoire | Publicité ou marketing, Analyse |

### Infos financières
| Donnée | Réponse |
|---|---|
| Historique des achats (`remove_ads`) | **Non déclaré** : l'achat est traité par la facturation Google Play, dont les données vont à Google Play et non à nous. Google indique que les données collectées par ses propres services (Play Billing) n'ont pas à être déclarées par le développeur ; à confirmer dans l'aide « Sécurité des données » au moment de remplir. |

Aucune autre catégorie : pas de nom, e-mail, contacts, photos, fichiers, audio, santé, historique web,
pas d'ID utilisateur (aucun compte), pas de contenu généré par l'utilisateur.

## Pourquoi « partagée = Oui »
Le SDK AdMob transmet ces données à Google, un tiers au sens du formulaire. Choix conservateur, identique à
Tidy Tubes. Aucune donnée n'est « éphémère » : Google les conserve selon ses règles.

## Pourquoi « obligatoire »
La diffusion d'annonces est nécessaire au fonctionnement de l'appli gratuite. La **personnalisation** des
annonces, elle, dépend du consentement UMP (EEE, R.-U., États américains) ; l'utilisateur peut la refuser
via « Options de confidentialité » sur l'écran d'accueil (affiché quand UMP l'exige). Après l'achat
`remove_ads`, les interstitiels disparaissent mais le SDK reste présent (vidéos récompensées facultatives).

## Points de cohérence à vérifier ailleurs dans la console
- **Contenu de l'application → Annonces** : « Oui, mon application contient des annonces ».
- **Contenu de l'application → Identifiant publicitaire** (si la question apparaît) : « Oui », finalités Publicité et Analyse.
- **Public cible et contenu** : 13 ans et plus.
- **Politique de confidentialité** : l'URL ci-dessus doit répondre (page `privacy-policy/knotlight/index.html` à pousser sur GitHub Pages) **avant** de la saisir.
- **Produit intégré** : `remove_ads` doit exister dans Monétiser → Produits intégrés, sinon l'appli affiche « Not available in the store yet ».
- Ces réponses correspondent au code : `src/ads/admob_provider.gd` (aucune annonce en release sans `admob_ids.cfg`), `src/billing/purchases.gd`, `src/autoload/game.gd` (sauvegarde locale uniquement).

## Import CSV
Pas de CSV généré pour Knotlight : le script `tools\store\build-data-safety-csv.ps1` contient les réponses de
Tidy Tubes (Firebase, Play Games) qui ne s'appliquent pas ici. Saisir les réponses ci-dessus à la main
(une dizaine de cases), ou dupliquer le script avec ces réponses si l'on veut l'import.
