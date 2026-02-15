# Architecture and UI Guidelines

## Module Boundaries

- `composeApp/`: app shell, root nav host, platform entry points.
- `backendApp/`: Ktor runtime, route registration, workers.
- `foundation/*`: shared cross-feature modules (`aura`, `domain`, `database`, `platform`, `navigation`, etc.).
- `features/<feature>/{domain,data,presentation}`: client-facing feature slices.
- `features/<feature>/backend`: backend-only feature libraries.

## Feature Structure (Presentation)

Preferred layout:

```text
features/<feature>/presentation/
  navigation/
  di/
  mvi/
  presentation/
    <area>/
      route/
      screen/
      components/
      model/
```

## Route vs Screen vs Components

`route/`
- Owns navigation, one-off side effects, and external interactions.
- Subscribes to stores/containers and translates actions.

`screen/`
- Stateless UI composition.
- Receives state + callbacks only.

`components/`
- Reusable UI building blocks scoped to feature or shared aura.
- No navigation and no global store access.

## Navigation Guardrail

Navigation APIs are forbidden in `presentation/components` and `presentation/screen`.

Enforced by:

```bash
./gradlew checkNoNavInComponents
```

## File Size Guardrail

Kotlin file max is 450 LOC unless allowlisted.

Enforced by:

```bash
./gradlew checkKotlinFileSize
```

## Shared UI Rules

Put components in `foundation/aura` only when they are cross-feature and domain-agnostic.

Keep domain-specific components in feature modules.

## Side Effects

Allowed in routes only:
- Navigation
- Snackbar/dialog one-offs
- Analytics/logging events
- Platform integrations

Screens/components emit intents or callbacks and stay side-effect free.

## Strings and Localization

- Avoid hardcoded UI strings.
- Use resource-based strings.
- Use existing localization/extension patterns for enum and exception display.

## Validation Commands

Use these before merge:

```bash
./gradlew checkAll
./gradlew :composeApp:desktopTest
./gradlew :backendApp:test
```
