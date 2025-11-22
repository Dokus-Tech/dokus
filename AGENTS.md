# Repository Guidelines

## Project Structure & Module Organization
- `composeApp/`: Kotlin Multiplatform UI for desktop, web (WASM), Android, and iOS.
- `foundation/*`: Shared design system, navigation, platform glue, domain models, storage.
- `features/<domain>/{backend,data,presentation,domain}`: Vertical slices (auth, cashflow, payment, reporting, audit, banking).
- `build-logic/`: Gradle convention plugins; prefer adding settings here instead of per-module hacks.
- `deployment/`, `k8s/`, `rabbitmq/`, `docker-compose.local.yml`: Ops manifests and local orchestration; `docs/` holds architecture, setup, and security references.

## Build, Test, and Development Commands
- Local stack (databases, Redis, helpers): `./dev.sh` uses compose profiles in `docker-compose.local.yml`.
- Full build: `./gradlew build` (use `-PENV=local` to point clients at localhost).
- Desktop: `./gradlew :composeApp:run`; Web (live reload): `./gradlew :composeApp:wasmJsBrowserRun`.
- Android: `./gradlew :composeApp:assembleDebug` (APK in `composeApp/build/outputs/apk/debug`).
- iOS: open in Xcode via Android Studio and run generated `ios*` targets.

## Coding Style & Naming Conventions
- Kotlin official style, 4-space indents, IDE import order; keep composables small and declarative.
- Modules stay lower_snake; Kotlin packages lowerCamel; classes/files UpperCamel; tests mirror source packages.
- Reuse `foundation/design-system` components and Koin DI; keep Gradle in KTS and lean on `build-logic`.

## Testing Guidelines
- Default: `./gradlew check`; scope when iterating (e.g., `./gradlew :features:auth:backend:test`).
- Tests end with `*Test.kt`; fixtures live beside code under `src/<platform>Test`.
- Cover financial math, PEPPOL rules, and multi-tenant boundaries; add Ktor integration tests when touching endpoints; document platform coverage for UI work.

## Commit & Pull Request Guidelines
- Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `test:`); imperative, scoped messages.
- PRs need intent, linked issues, and test commands; include before/after screenshots for UI.
- One logical change per PR; update `CHANGELOG.md` for user-facing changes; keep branches rebased on `main`.

## Security & Configuration Tips
- No secrets in VCS; use env vars or compose overrides. Check `docs/SECURITY.md` and `docs/SETUP.md` before altering auth/networking.
- Enforce tenant IDs at repositories/RPC handlers and keep audit logging intact.
- When adding services/ports, align with `docs/INFRASTRUCTURE_PORTS.md` and mirror updates in `deployment/` and `k8s/`.
