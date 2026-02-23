# Repository Guidelines

## Project Structure & Module Organization
- `composeApp/`: Compose Multiplatform client (Android, iOS, Desktop/JVM, Web/WASM).
- `backendApp/`: Backend entry point (single Ktor server / modular monolith).
- `foundation/*`: Shared modules (`aura` design system, `app-common`, `platform`, `navigation`, `domain`, `backend-common`, `database`, `peppol`, `sstorage`).
- `features/<feature>/{domain,data,presentation}`: Client feature slices (currently `auth`, `cashflow`, `contacts`). Backend-only feature libs may live under `features/<feature>/backend` (e.g. `features/ai/backend`).
- `build-logic/`: Gradle convention plugins + versioning; prefer changes here over per-module build hacks.
- `deployment/`: Docker Compose profiles + `dokus.sh` (self-host); local dev stack is driven by `./dev.sh`.
- `docs/`: Product/architecture references (see `docs/ARCHITECTURE_UI_GUIDELINES.md` and `docs/REFACTOR_SAFETY.md`).
- `tasks/`: Engineering notes and implementation write-ups (good place for phased work logs).

## Build, Test, and Development Commands
- Local infra stack (Postgres/Redis/MinIO/Traefik): `./dev.sh start` (or `./dev.sh` for the interactive console); uses `deployment/docker-compose.pro.yml` + `deployment/docker-compose.local.yml`.
- Full build: `./gradlew build`.
- Backend: `./gradlew :backendApp:run` (tests: `./gradlew :backendApp:test`).
- Desktop client: `./gradlew :composeApp:run` (explicit: `./gradlew :composeApp:desktopRun`).
- Web (WASM): `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` (production: `./gradlew :composeApp:wasmJsBrowserProductionRun`).
- Android: `./gradlew :composeApp:assembleDebug` (APK in `composeApp/build/outputs/apk/debug`).
- iOS: open `iosApp/iosApp.xcodeproj` in Xcode and run.
- Quality guardrails: `./gradlew checkAll` (detekt + custom checks); screenshot tests: `./gradlew verifyScreenshots` / `./gradlew recordScreenshots`.

## Coding Style & Naming Conventions
- Kotlin official style, 4-space indents, IDE import order; keep composables small and declarative.
- Gradle: prefer `.kts`, version catalog (`libs.*`), and type-safe project accessors; avoid hardcoding versions.
- Follow Route/Screen/Components split; navigation + side effects live in `route/` only (enforced by `./gradlew checkNoNavInComponents`). See `docs/ARCHITECTURE_UI_GUIDELINES.md`.
- Keep Kotlin files small; `./gradlew checkKotlinFileSize` enforces a 450 LOC max.
- Reuse `foundation/aura` components for cross-feature UI; use Koin for DI.
- PEPPOL UX: don’t ask for VAT in UI if it exists in workspace context, and never show external provider names to users.

## Testing Guidelines
- Fast local: `./gradlew checkAll` + targeted module tests (e.g. `./gradlew :features:cashflow:presentation:testDebugUnitTest`).
- Cross-target: `./gradlew allTests`.
- Tests end with `*Test.kt`; keep fixtures beside code under `src/<platform>Test` where possible.
- UI screenshots (Roborazzi): `./gradlew verifyScreenshots` (update baselines via `./gradlew recordScreenshots`, clear with `./gradlew clearScreenshots`).

## Commit & Pull Request Guidelines
- Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `test:`); imperative, scoped messages.
- PRs need intent, linked issues, and test commands; include before/after screenshots for UI.
- One logical change per PR; update `CHANGELOG.md` for user-facing changes; keep branches rebased on `main`.

## Security & Configuration Tips
- No secrets in VCS; use `deployment/.env.example` / `deployment/.env` (and env vars in CI).
- Preserve multi-tenant boundaries and auditability; see `docs/10-DATA-PRIVACY-AND-TRUST.md` and `docs/11-PEPPOL-AND-COMPLIANCE.md`.
- When adding services/ports, update `deployment/docker-compose.*.yml` and `dev.sh`, and document the change in `deployment/README.md` when relevant.
