---
description: Review changed files or a game against the workspace architecture, ad and release rules
argument-hint: "<project or files>"
---

Review: $ARGUMENTS

Delegate to the code-review-agent with the exact files (there is no Git yet, so ask which files changed
if not given). It applies the game-architecture-review skill and returns ranked findings with
file:line and failure scenarios. Don't modify code during the review; offer fixes afterwards.
