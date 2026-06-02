---
name: commit-gps
description: Generates git commit messages for the GPS Anywhere project following its session-based development convention. Use when the user asks to commit changes, write a commit message, or stage and commit work done in a BUILD_PLAN session.
---

# GPS Anywhere — Commit Convention

## Format

```
<type>(<scope>): <short description>

<optional body: what changed and why>
```

## Type Tags

| Tag | When to Use |
|-----|-------------|
| `session-N` | Implementing a full BUILD_PLAN session |
| `feat` | New feature or screen outside a session |
| `fix` | Bug fix |
| `build` | Gradle, dependencies, plugin changes |
| `refactor` | Code restructure, no behavior change |
| `style` | Theme, colors, layout only |
| `docs` | PLANNING.md, BUILD_PLAN.md, SKILL.md changes |
| `chore` | Cleanup, file deletion, config |

## Scope Tags (use the affected area)

`home` · `location` · `route` · `saved` · `service` · `db` · `theme` · `nav` · `manifest` · `gradle` · `onboarding`

## Examples

```
session-1: scaffold, master toggle, Room skeleton, SpoofService

build: switch Room to KSP, add Compose plugin for Kotlin 2.0

fix(service): clear currentLat/Lng on stopSpoofing

feat(route): add OSRM route fetch with distance/time display

style(theme): apply Material 3 palette for light and dark mode

docs: update BUILD_PLAN sessions 2–6 for Compose screens

chore: remove XML fragments and layouts after Compose migration
```

## Rules

- Max 72 chars on the first line
- Body optional — add only if the "why" is non-obvious
- Never mention file names in the subject line — use scope instead
- Do not use past tense ("added") — use imperative ("add")
- For session commits, list the main deliverables in the body if multiple files changed
