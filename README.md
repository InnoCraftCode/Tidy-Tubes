# mobile-android

Workspace for building, testing, monetizing (AdMob) and publishing Android games with **libGDX**.

## Quick start (Windows PowerShell)
```powershell
cd "E:\mobile andoird"
powershell -ExecutionPolicy Bypass -File .\tools\environment-check\check-environment.ps1
powershell -ExecutionPolicy Bypass -File .\tools\scripts\new-game.ps1 -Name my-game -PackageId com.ben.laama.mygame -DisplayName "My Game"
powershell -ExecutionPolicy Bypass -File .\tools\android-build\build-android.ps1 -Project my-game -Task Desktop
powershell -ExecutionPolicy Bypass -File .\tools\android-build\build-android.ps1 -Project my-game -Task Debug
```

## What's here
| Folder | Purpose |
|---|---|
| `games/` | One Gradle project per game, first one `games/sortpuzzle` (Ball Sort) - [registry](games/README.md) |
| `templates/libgdx-2d/` | Playable template with AdMob (Next-Gen SDK + UMP) wired in |
| `tools/` | Environment check, workspace validation, build, new-game scripts |
| `config/` | Architecture decisions, coding standard, release checklists |
| `shared/` | Design, monetization, analytics, privacy and testing templates |
| `docs/` | Guides: [architecture](docs/architecture.md), [installation](docs/installation.md), [agents](docs/agent-guide.md), [skills](docs/skills-catalog.md), [monetization](docs/monetization-guide.md), [Google Play](docs/google-play-guide.md), [privacy](docs/privacy-guide.md), [troubleshooting](docs/troubleshooting.md) |
| `reports/` | Audit, dependency inventory, validation report |
| `.claude/` | Claude Code agents, skills, slash commands, permissions |

## Claude Code
Open this folder in VS Code with the Claude Code extension (or run `claude` here). `CLAUDE.md` gives
Claude the rules and routing; type `/` for commands such as `/test Sortpuzzle`,
`/build-android Sortpuzzle Debug`, `/prepare-play-store <game>`.

## Validation
```powershell
powershell -ExecutionPolicy Bypass -File .\tools\validation\validate-workspace.ps1 -RunTests
```
