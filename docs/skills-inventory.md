# Inventaire des skills — 2026-10-26

Skills = dossiers `.claude/skills/<nom>/SKILL.md` chargés à la demande par Claude Code (ou `/<nom>`).
Aucun skill tiers n'a été installé : les besoins étaient couverts par les skills du workspace,
deux skills ont été créés, et les outils externes nécessaires (GDI+, FFmpeg, émulateur) sont déjà
sur la machine. Aucun mécanisme de sécurité n'a été contourné.

## Skills du workspace (déjà présents, réutilisés)

| Skill | Besoin de la mission | Utilisé pour |
|---|---|---|
| `libgdx-game-development` | Android / stack réelle, refactoring | refonte des écrans, nouveaux composants |
| `android-build-and-export` | build, signature, SDK | compileSdk 37 / targetSdk 36, APK/AAB, natives |
| `admob-integration` | AdMob | port de `AdMobAdProvider`, config Gradle, manifeste, UMP |
| `rewarded-ads-design` | placements | récompensé (indices, coups), interstitiel espacé |
| `privacy-and-consent` | confidentialité, Data safety | consentement UMP, déclarations, politique |
| `google-play-publishing` | release engineering, fiche | checklist, limites de textes, AAB |
| `mobile-game-testing` | tests automatisés / régression | tests JUnit, scénarios émulateur |
| `game-architecture-review` | qualité du code | revue finale |
| `mobile-game-performance` | performance | contrôle des allocations/textures (pas de problème mesuré) |
| `token-efficient-development` | méthode de travail | lectures ciblées, tests étroits |
| `game-project-bootstrap` | — | non utilisé (pas de nouveau jeu) |

## Skills créés pour cette mission (vérifiés : listés par Claude Code après création)

| Skill | Couvre | Fichier |
|---|---|---|
| `premium-ui-design` | UI/UX mobile, design system, accessibilité, vérification sur rendus réels | `.claude/skills/premium-ui-design/SKILL.md` |
| `store-asset-production` | création/traitement d'images (icône, feature graphic), captures automatisées, vidéo, validation JSON, export | `.claude/skills/store-asset-production/SKILL.md` |

## Besoins et solutions retenues

| Besoin | Solution | Limite |
|---|---|---|
| Génération d'images (icône, feature graphic) | script GDI+ (`tools/store/render-brand-assets.ps1`), police Poppins chargée en privé | pas de génération IA ; rendu vectoriel/procédural, reproductible |
| Captures d'écran réelles | `StoreCapture.java` (rendu 1080×1920 du vrai jeu) + émulateur Pixel_10 (adb screencap) | — |
| Vidéo | FFmpeg 9.0.2 (`tools/store/make-promo-video.ps1`) | — |
| Validation technique des assets | `tools/store/validate-store-assets.ps1` → JSON | les contrôles de contenu restent humains |
| Historique / restauration | **Git absent** ; zip dans `backups/` | installation de Git à faire par l'utilisateur (`winget install --id Git.Git -e`) |
| Figma MCP | disponible mais non utilisé : le design system vit dans le code (`Palette`, `SkinFactory`, `Ui`) et les rendus réels font foi | export Figma possible plus tard via `/figma-generate-design` |
