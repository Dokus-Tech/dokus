# Contributing to Dokus

## Before You Start

- Read [`docs/00-READ_FIRST.md`](docs/00-READ_FIRST.md) for product boundaries.
- Read [`docs/ARCHITECTURE_UI_GUIDELINES.md`](docs/ARCHITECTURE_UI_GUIDELINES.md) for Route/Screen/Components rules.
- Read [`docs/REFACTOR_SAFETY.md`](docs/REFACTOR_SAFETY.md) before structural refactors.

## Development Setup

### Prerequisites

- JDK 21+
- Docker + Docker Compose
- Xcode (for iOS builds)
- Android Studio (for Android workflows)

### Local services

```bash
./dev.sh start
```

### Build and run

```bash
./gradlew build
./gradlew :backendApp:run
./gradlew :composeApp:desktopRun
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

## Required Checks

Run these before opening a PR:

```bash
./gradlew checkAll
./gradlew :backendApp:test
./gradlew :composeApp:desktopTest
```

When touching UI rendering/components, also run screenshot tasks:

```bash
./gradlew verifyScreenshots
```

## Coding Rules

- Use Kotlin official style.
- Keep Kotlin files under 450 LOC unless explicitly allowlisted.
- Keep navigation and side effects in `route/` only.
- Prefer `foundation/aura` reusable components for cross-feature UI.
- Use version catalog dependencies (`libs.*`) instead of hardcoded versions.

## Commit and Branch Rules

- Use branch names with `feature/` prefix.
- Use Conventional Commits:
- `feat:`
- `fix:`
- `chore:`
- `docs:`
- `test:`

Examples:
- `feat(cashflow): add document list filter for ingestion status`
- `fix(auth): refresh token invalidation on logout`

## Pull Requests

Use the project PR template in [`.github/PULL_REQUEST_TEMPLATE.md`](.github/PULL_REQUEST_TEMPLATE.md).

PRs should include:
- Purpose and scope.
- Linked issue(s), if applicable.
- Test commands executed.
- Screenshots for UI changes.
- Changelog update for user-facing changes.

## Documentation Changes

If your change affects behavior, update docs in the same PR:
- Public behavior or workflows: `README.md`, `docs/IMPLEMENTED_CAPABILITIES.md`
- Architecture or coding rules: `docs/ARCHITECTURE_UI_GUIDELINES.md`
- Deployment behavior: `deployment/README.md`

## Security Reporting

Do not open a public issue for sensitive vulnerabilities.

Report security issues to: `artem@invoid.vision`.
