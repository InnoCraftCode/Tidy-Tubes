# Marque et ASO — Tidy Tubes

## 1. Positionnement

Puzzle de tri de boules (ball sort), calme, honnête, sans friction : niveaux infinis toujours
solubles, annulation gratuite, indices d'un vrai solveur. Public : adultes et adolescents (13+)
cherchant un jeu de réflexion apaisant pour de courtes sessions, à une main.

## 2. Noms étudiés (recherches web du 26/10/2026)

| Nom | Mémorisation | Prononciation / orthographe | Originalité apparente | Pertinence | Potentiel de marque | Disponibilité observée (recherche web « <nom> app game ») | Risque de confusion |
|---|---|---|---|---|---|---|---|
| **Tidy Tubes** | forte (allitération) | simple en EN et FR | bonne | décrit l'action (ranger des tubes) | bon (ton calme) | aucune app « Tidy Tubes » trouvée ; des apps « Tidy » existent dans d'autres catégories (nettoyage de photos, ménage) | faible |
| Huevial | moyenne | « hue-vial », ambiguë à l'oral | très bonne | OK (teinte + fiole) | moyen | aucune occurrence | faible, mais nom difficile à épeler |
| Vialflow | moyenne | simple | bonne | moyenne (évoque le liquide, pas les boules) | moyen | aucune occurrence ; « Liquid Vial », « SortFlow » existent (water sort) | moyen |
| Sortalyst | moyenne | « sort-a-lyst », évoque « catalyst » | bonne | moyenne (abstrait) | moyen | aucune occurrence | faible |
| Chromatube | moyenne | simple | moyenne | bonne | moyen | **« Chromatubes » existe sur l'App Store** (jeu de routage de boules) | **élevé** |

**Choix : Tidy Tubes.** Mémorable, descriptif, prononçable en français comme en anglais, aucune
app homonyme trouvée, cohérent avec le ton (« a tidy little puzzle » était déjà le sous-titre
interne). Les recherches sont des observations de résultats web à la date indiquée, pas une
vérification juridique : avant publication, vérifier la fiche Play (recherche « Tidy Tubes »
dans le Play Store), le nom de domaine si un site est souhaité, et éventuellement un registre de
marques.

## 3. Identité

| Élément | Valeur |
|---|---|
| Nom | Tidy Tubes |
| Slogan | « Sort. Breathe. Repeat. » (FR : « Triez. Respirez. Recommencez. ») |
| Proposition de valeur | Des niveaux infinis toujours solubles, une annulation gratuite, des indices honnêtes — un puzzle qui respecte votre temps. |
| Palette de marque | Indigo `#222B5E`→`#0B0E24`, menthe `#4FE3C1`, or `#FFC857`, boules coral/bleu/vert/jaune (voir design-system.md) |
| Style visuel | Nocturne : fond profond, verre discret, couleurs vives réservées aux boules, typographie Poppins |
| Principes de rédaction | phrases courtes, minuscules sauf boutons, aucune promesse chiffrée, pas de « gratuit sans pub » tant que des pubs existent, vouvoiement en FR |
| Icône | tube en verre contenant jaune/bleu, boule coral soulevée, fond indigo (vecteur + PNG, voir `tools/store/render-brand-assets.ps1`) |

## 4. Fiche Google Play (source : `play-store/sortpuzzle/listing.json`)

Limites officielles respectées (vérifiées par `build-listing-doc.ps1`) : titre ≤ 30, description
courte ≤ 80, description complète ≤ 4000, notes de version ≤ 500.

| Champ | en-US | fr-FR |
|---|---|---|
| Titre (≤ 30) | `Tidy Tubes: Ball Sort Puzzle` (28) | `Tidy Tubes : Tri de Couleurs` (28) |
| Description courte (≤ 80) | `Sort the balls by colour. Calm, endless levels, every one solvable. Play offline.` (78) | `Triez les boules par couleur. Des niveaux calmes, infinis, toujours solubles. Hors ligne.` (80) |
| Description complète | voir listing.json (≈ 1 050 caractères) | voir listing.json |
| Catégorie | Jeux › Réflexion | |
| Contient des annonces | **Oui** (AdMob : récompensées facultatives + interstitiels espacés) | |
| Captures | 7 captures réelles 1080×1920 avec légende et texte alternatif (`screenshots[]`) | |

Les textes ne contiennent ni classement, ni chiffre inventé, ni témoignage, ni badge.

## 5. Traductions

en-US et fr-FR livrées (marchés visés : francophone et anglophone). D'autres langues peuvent être
ajoutées dans `listing.json` ; l'interface du jeu est en anglais (textes courts), une
localisation in-app n'est pas incluse dans cette version.

## 6. Compatibilité préservée

Inchangés : applicationId / package `com.ben.laama.sortpuzzle`, nom du dossier projet
`games/sortpuzzle`, nom des préférences, signature. Seuls le nom affiché (`strings.xml`,
`GameConfig.APP_NAME`), les titres de fenêtre et les textes Store changent.
