# Dokus

Dokus is a Kotlin Multiplatform financial operations platform for Belgian businesses.

It combines:
- A single Ktor backend (`backendApp`) deployed as a modular monolith.
- A Compose Multiplatform client (`composeApp`) for Android, iOS, Desktop, and Web (WASM).
- Shared foundation and feature modules for domain logic, storage, UI, and integrations.

## Current Status

- Active development.
- Tagged releases available (`v0.1.1` through `v0.1.17`).
- Primary deployment target: self-hosted Docker stack with optional cloud profile.

## Repository Structure

- `composeApp/`: multiplatform client app.
- `backendApp/`: backend server entry point and runtime wiring.
- `foundation/*`: shared modules (`aura`, `domain`, `database`, `platform`, `navigation`, etc.).
- `features/<feature>/{domain,data,presentation}`: client feature slices.
- `features/<feature>/backend`: backend-only feature libraries (for example AI).
- `deployment/`: Docker Compose deployment configs and `dokus.sh` management script.
- `docs/`: product, architecture, and engineering documentation.

## Quick Start

### Development stack (Postgres/Redis/MinIO/Traefik)

```bash
./dev.sh start
```

### Run backend

```bash
./gradlew :backendApp:run
```

### Run clients

```bash
./gradlew :composeApp:desktopRun
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
./gradlew :composeApp:assembleDebug
# iOS: open iosApp/iosApp.xcodeproj in Xcode
```

### Quality checks

```bash
./gradlew checkAll
./gradlew :backendApp:test
./gradlew :composeApp:desktopTest
```

## Deployment

For managed setup commands and profiles, see:
- [`deployment/README.md`](deployment/README.md)

Quick command:

```bash
cd deployment
./dokus.sh setup
```

## Documentation

Start here:
- [`docs/README.md`](docs/README.md)

Key references:
- Product guardrails: [`docs/00-READ_FIRST.md`](docs/00-READ_FIRST.md)
- Architecture/UI rules: [`docs/ARCHITECTURE_UI_GUIDELINES.md`](docs/ARCHITECTURE_UI_GUIDELINES.md)
- Implemented capabilities snapshot: [`docs/IMPLEMENTED_CAPABILITIES.md`](docs/IMPLEMENTED_CAPABILITIES.md)
- Refactor checklist: [`docs/REFACTOR_SAFETY.md`](docs/REFACTOR_SAFETY.md)

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for setup, workflow, and PR requirements.

## License

AGPL v3. See [`LICENSE`](LICENSE).
