# Équipe d'agents — configuration réelle (2026-10-26)

Les agents sont des fichiers `.claude/agents/<nom>.md` (frontmatter `name`, `description`, `tools`,
`skills`) reconnus nativement par Claude Code : invocables par `@agent-<nom>`, par délégation
automatique, ou via l'outil Agent. Règle du workspace : **un agent principal par tâche**, spécialistes
seulement pour un besoin distinct, intégration finale centralisée.

## Correspondance avec les rôles demandés

| Rôle demandé | Agent réel | Statut | Périmètre / fichiers de travail | Critère de validation |
|---|---|---|---|---|
| A. Principal Architect / Team Lead | `game-architect` + session principale (Claude) | existant | découpage, priorités, registre des décisions (`config/architecture/ADR-*.md`, ce document), intégration finale | tests verts, builds verts, aucune régression de la cartographie |
| B. Senior Product Designer / UI-UX | **`ui-ux-designer-agent`** | **créé** | `docs/design-system.md`, `Palette`, `SkinFactory`, `Ui`, rendus réels | contraste ≥ 4,5:1, cibles ≥ 64 u, captures revues |
| C. Senior Frontend / Mobile Engineer | `libgdx-game-developer` | existant | `core/.../screens`, `ui`, `save`, `audio` | `:core:test`, `:lwjgl3:run`, captures |
| D. Backend / Integration | — | **non applicable** | l'app n'a ni backend ni API ; aucun contrat d'échange à préserver | — |
| E. Brand Identity / ASO | **`brand-aso-agent`** | **créé** (E+F regroupés : même matière marketing) | `docs/branding-and-aso.md`, `play-store/<jeu>/listing.json` | limites Play (30/80/4000), recherches de disponibilité documentées |
| F. App Icon / Marketing Asset Designer | `brand-aso-agent` + `game-art-technical-agent` | regroupé | `tools/store/render-brand-assets.ps1`, icônes Android | `validate-store-assets.ps1` PASS |
| G. QA Automation / Regression | `qa-testing-agent` | existant | tests JUnit, scénarios émulateur, `docs/final-qa-report.md` | tous les tests verts, APK installé et lancé |
| H. Google Play Compliance Auditor | **`play-compliance-auditor-agent`** | **créé**, lecture seule, indépendant des développeurs | `docs/google-play-compliance-report.md` | chaque contrôle PASS/FAIL/WARNING/NOT VERIFIED/NOT APPLICABLE avec preuve |
| I. AdMob / Monetization Engineer | `ads-monetization-agent` | existant | `ads/`, `AdMobAdProvider`, `docs/admob-integration-current-release.md` | tests RewardGate/AdPacing, logcat SDK |
| J. Release Engineer | `google-play-release-agent` + `senior-android-developer` | existants | `build-android.ps1`, AAB, checklist release | AAB généré, checklist remplie |

Agents conservés sans rôle direct dans cette mission : `game-design-agent`, `performance-agent`,
`security-privacy-agent` (consulté par l'auditeur), `code-review-agent` (revue finale).

## Organisation du travail

- Travail en parallèle limité aux tâches sans conflit de fichiers : captures/vidéo (scripts) pendant
  la rédaction des documents ; revue de code et audit de conformité lancés comme agents
  indépendants en lecture seule sur l'état final.
- Un seul responsable de l'intégration : la session principale. Aucun agent n'a écrit dans les
  fichiers d'un autre.

## Vérifications de fonctionnement

- Les nouveaux agents et skills apparaissent dans la liste de Claude Code (skills listés
  automatiquement après création ; agents chargés au prochain démarrage de session, voir
  `docs/agent-guide.md`).
- Les agents de revue et d'audit ont été exécutés sur l'état final (résultats dans
  `docs/final-qa-report.md` et `docs/google-play-compliance-report.md`).

## Limitations

- Les fichiers d'agents créés pendant une session ne sont chargés qu'au démarrage suivant de
  Claude Code : l'audit de conformité de cette mission a donc été exécuté par
  `google-play-release-agent` en mode auditeur, en suivant à la lettre la définition de
  `play-compliance-auditor-agent.md` (lecture seule, un seul fichier de sortie). Dès la prochaine
  session, `@agent-play-compliance-auditor-agent` est directement invocable.
- Pas de persistance « mémoire » entre agents au-delà des fichiers : les décisions sont dans les docs.
- Pas d'agent backend (sans objet). Pas d'agent « vidéo » : la production vidéo est scriptée
  (`capture-desktop.ps1 -Video`), pas confiée à un agent.
