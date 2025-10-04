# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ThePredict is a Kotlin Multiplatform (KMP) financial prediction application targeting Android, iOS, Desktop (JVM), and Web (WASM). The codebase uses Compose Multiplatform for UI and follows a feature-modular architecture.

## Build Commands

### Running the Application
```bash
# Web development server with hot reload
./gradlew wasmJsBrowserRun -t

# Android debug build
./gradlew :composeApp:assembleDebug

# Desktop application (macOS/Windows/Linux)
./gradlew :composeApp:packageReleaseDmg  # macOS
./gradlew :composeApp:packageReleaseMsi  # Windows
./gradlew :composeApp:packageReleaseDeb  # Linux
```

### Testing
```bash
# Run all tests across platforms
./gradlew allTests

# Run specific platform tests
./gradlew testDebugUnitTest     # Android
./gradlew desktopTest           # Desktop/JVM
./gradlew iosSimulatorArm64Test # iOS Simulator (ARM)

# Full verification (build + test)
./gradlew check
```

### Development
```bash
# Clean build artifacts
./gradlew clean

# Build all modules
./gradlew build
```

## Architecture

### Module Structure
The project follows a feature-based modular architecture:

- **`/composeApp`**: Main application entry point with platform-specific configurations
- **`/application`**: Feature modules and core infrastructure
  - Feature modules: `onboarding`, `home`, `dashboard`, `contacts`, `cashflow`, `simulation`, `inventory`, `banking`, `profile`
  - Core modules: `core`, `platform`, `repository`, `navigation`, `ui`
- **`/shared`**: Domain models and configuration shared across all modules
- **`/server`**: Backend microservices (currently disabled in settings.gradle.kts)

### Key Architectural Patterns
- **Dependency Injection**: Kodein DI throughout the application
- **Navigation**: Voyager for type-safe navigation
- **Platform-specific code**: `expect`/`actual` declarations in the `platform` module
- **Source sets**: Each module has `commonMain`, `androidMain`, `iosMain`, `desktopMain`, and `wasmJsMain`

### Package Naming
All packages follow: `ai.thepredict.{module}.{feature}`

## Technology Stack

- **Kotlin**: 2.2.10
- **Compose Multiplatform**: 1.9.0
- **Ktor**: 3.3.0 (HTTP client/server)
- **Kodein**: 7.28.0 (DI)
- **Voyager**: 1.1.0-beta03 (Navigation)
- **kotlinx.serialization**: 1.9.0

## Key Files & Entry Points

- Main application: `/composeApp/src/commonMain/kotlin/ai/thepredict/app/App.kt`
- Version catalog: `/gradle/libs.versions.toml`
- Module configuration: `/settings.gradle.kts`
- Custom build plugins: `/build-logic/convention/`
- Server endpoints: `/shared/configuration/src/commonMain/kotlin/ai/thepredict/configuration/ServerEndpoint.kt`

## Development Guidelines

1. **Dependencies**: Always use the version catalog (`libs.*` references) instead of hardcoding versions
2. **Module references**: Use type-safe project accessors (e.g., `projects.application.core`)
3. **Platform code**: Place platform-specific implementations in appropriate source sets
4. **Testing**: Limited test coverage exists; server tests are in `/server/{module}/src/test/kotlin/`
5. **Server modules**: Currently commented out in settings.gradle.kts; uncomment if server work is needed

## Common Workflows

### Adding a new feature module
1. Create module in `/application/{feature-name}`
2. Add to `settings.gradle.kts`
3. Follow existing module structure with platform source sets
4. Register in Kodein DI modules

### Working with platform-specific code
1. Define interface with `expect` in `commonMain`
2. Provide `actual` implementations in platform source sets
3. Use the `platform` module for shared platform abstractions

### Debugging build issues
1. Check module inclusion in `settings.gradle.kts`
2. Verify dependencies in module's `build.gradle.kts`
3. Ensure version catalog entries exist for new dependencies
4. Run `./gradlew clean` before rebuilding