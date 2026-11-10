# Contenu de l'application (Play Console) - réponses pour Knotlight 0.7.0

État constaté dans la console le 2026-11-10 : toutes les sections ci-dessous sont **vides**. Ordre conseillé :
politique de confidentialité → annonces → informations de connexion → classification du contenu →
cible et contenu → sécurité des données → statut de professionnel → catégorie et coordonnées → fiche Play Store.
Rien ne doit être envoyé pour examen sans l'accord explicite du propriétaire du compte.

## 1. Règles de confidentialité
URL : `https://innocraftcode.github.io/knotlight/privacy-policy/`
La page est hébergée par le dépôt GitHub InnoCraftCode/knotlight (source : `privacy-policy/knotlight/index.html`, EN + FR). Vérifier que
l'URL répond en navigation privée **avant** de la saisir.

## 2. Annonces
**Oui, mon application contient des annonces.** (AdMob : vidéos récompensées facultatives + interstitiels après une victoire.)

## 3. Informations de connexion
- L'appli nécessite-t-elle une connexion pour accéder à tout ou partie du contenu ? **Non** (aucun compte, aucune connexion).
- Aucun identifiant de test à fournir.

## 4. Classification du contenu (questionnaire IARC)
Catégorie : **Jeu**. E-mail de contact : benlamschille@gmail.com.

| Question IARC | Réponse |
|---|---|
| Violence (réaliste, cartoon, contre des humains/animaux/fantastiques) | **Non** à tout. Aucun combat, aucune blessure. |
| Peur, horreur, contenu effrayant | **Non** |
| Sexualité, nudité, contenu suggestif | **Non** |
| Langage grossier, humour cru | **Non** |
| Drogues, alcool, tabac | **Non** |
| Jeux d'argent (réels ou simulés), loteries | **Non** (le calendrier et la roue n'existent pas ; les récompenses sont fixes) |
| Discrimination, contenu haineux | **Non** |
| Contenu généré par les utilisateurs, chat, partage | **Non** |
| Les utilisateurs peuvent-ils interagir entre eux ? | **Non** |
| Partage de la position de l'utilisateur avec d'autres | **Non** |
| Achats de biens numériques | **Oui** (produit « Sans publicité ») |
| Contient des publicités | **Oui** |
| Accès à Internet / navigateur web intégré | Internet : **Oui** (annonces) ; navigateur intégré : **Non** |
| Contenu non filtré / web ouvert | **Non** |

Résultat attendu : ESRB **Everyone**, PEGI **3**, USK **0**, ClassInd **L** (à confirmer : le questionnaire change).

## 5. Cible et contenu
| Question | Réponse |
|---|---|
| Tranches d'âge cibles | **13-15, 16-17, 18 ans et plus** (ne pas cocher les tranches enfants) |
| L'appli est-elle conçue pour les enfants ? | **Non** |
| Détails de l'appli : pourrait-elle attirer involontairement les enfants ? | Répondre **Non** si le compte assume que la cible est 13+ (style cartoon et mascotte renard, mais règles de planification, économie, publicités et achat intégré). Google peut requalifier : dans ce cas la politique Familles s'applique (SDK certifiés, consentement, formats), non prévue dans cette version. |
| Vos annonces respectent-elles les règles Familles ? | Non concerné si « Non » ci-dessus |
| La fiche Play Store doit-elle être signalée « pour les enfants » ? | **Non** |

## 6. Sécurité des données
Voir `data-safety-answers.md` (AdMob uniquement, aucune donnée à nous).

## 7. Applis gouvernementales
**Non**, l'appli n'est pas développée pour ou au nom d'un gouvernement.

## 8. Fonctionnalités financières
**Mon appli ne propose aucune fonctionnalité financière** (l'achat intégré « Sans publicité » n'en est pas une).

## 9. Santé
**Mon appli ne propose aucune fonctionnalité liée à la santé.**

## 10. Statut de professionnel (au niveau de l'appli)
Le compte est déjà déclaré **professionnel** (constaté dans la console). Pour l'appli :
- **Oui, mon statut de professionnel s'applique à cette appli** (l'appli génère des revenus : publicités + achat).
- Conditions d'utilisation : `https://innocraftcode.github.io/knotlight/terms/` (dépôt InnoCraftCode/knotlight, GitHub Pages).

## 11. Catégorie et coordonnées (Paramètres de la fiche Play Store)
| Champ | Valeur |
|---|---|
| Application ou jeu | **Jeu** |
| Catégorie | **Réflexion (Puzzle)** |
| Tags (jusqu'à 5) | Réflexion, Casse-tête, Relaxant, Occasionnel, Hors ligne (choisir parmi ceux proposés par la console) |
| Adresse e-mail (visible) | benlamschille@gmail.com |
| Téléphone | facultatif (laisser vide) |
| Site web | https://innocraftcode.github.io/knotlight/ (app-ads.txt à la racine du dépôt knotlight) |
| Marketing externe | laisser activé |

## 12. Fiche Play Store (par défaut en-US, plus fr-FR)
Textes dans `listing.json` ; Word généré par `.\tools\store\build-listing-doc.ps1 -Game threadbound-festival`.
Éléments graphiques (rendus le 2026-11-10 par `games\threadbound-festival\tests\capture_store.gd`, validés
par `validate-store-assets.ps1` : tout PASS) :
- `graphics\icon-512.png` (512x512, 32 bits) et `graphics\feature-graphic-1024x500.png` (24 bits).
- `screenshots\phone\01..08-*.png` : 8 captures 1080x1920 en anglais (fiche en-US) ; les mêmes en français
  dans `screenshots\phone-fr\` pour la fiche fr-FR. Légendes et textes alternatifs dans `listing.json`.
- Icônes launcher Android (`games\threadbound-festival\assets\icons\launcher_*.png`, 432 adaptative
  foreground/background/monochrome + 192 legacy) déclarées dans `export_presets.cfg` : **le prochain export
  AAB (versionCode 8) les embarque** ; l'AAB 0.7.0 actuel porte encore l'icône par défaut de Godot.
Regénérer tout : `Godot_v4.7.2-stable_win64_console.exe --path . --script res://tests/capture_store.gd -- "E:/mobile andoird/play-store/threadbound-festival" en`
(deuxième argument = langue ; sans `--headless`, le rendu est hors écran dans un SubViewport).

## 13. Avant le premier upload (rappel, hors console)
- Créer l'appli AdMob + 1 unité récompensée + 1 interstitiel + message UMP, puis `games\threadbound-festival\admob_ids.cfg` et rebuild de l'AAB (sinon release = aucune annonce).
- Créer le produit intégré `remove_ads` (Monétiser → Produits → Produits intégrés), prix suggéré 3,99 USD.
- Test interne (vous + quelques comptes) puis test fermé : 12 testeurs inscrits pendant 14 jours avant la demande de production.
