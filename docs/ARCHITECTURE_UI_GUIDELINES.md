# Architecture & UI Guidelines

## Module Boundaries
- `composeApp/`: App entry points, navigation host, platform shells.
- `foundation/*`: Shared design system (aura), navigation, platform glue, storage, and cross-feature utilities.
- `features/<feature>/*`: Vertical slices containing feature-specific domain, data, and presentation.
- `backendApp/`: Backend service entry point and HTTP routes.

Rules:
- Domain models and business rules live in `foundation/domain` or `features/<feature>/domain`.
- Data sources (API, DB, storage) live in `features/<feature>/data` or shared `foundation` modules.
- Presentation modules are UI + MVI only, no business logic.

## Feature Folder Structure
Preferred structure inside each feature:

```
features/<feature>/
  data/
  domain/               # if feature-specific domain exists
  presentation/
    navigation/
    di/
    mvi/
    presentation/
      <group>/
        route/
        screen/
        components/
        model/
```

Notes:
- `route/` owns navigation, store subscription, and side effects.
- `screen/` is pure UI (state in, callbacks out).
- `components/` are feature-specific reusable UI blocks.
- `model/` is UI-only models (view state, display models).

## Naming Conventions
- Route: `<Feature><Flow><Thing>Route` (e.g., `ProfileSettingsRoute`).
- Screen: `<Feature><Flow><Thing>Screen` (e.g., `ProfileSettingsScreen`).
- Component: `<Thing>Card`, `<Thing>Row`, `<Thing>Section`.

Packages:
- `tech.dokus.features.<feature>.*`
- `tech.dokus.foundation.*`

## Route vs Screen vs Component
Routes:
- Subscribe to FlowMVI store.
- Handle navigation, snackbars, dialogs, analytics, and side effects.
- Map actions to navigation or one-off events.

Screens:
- Stateless UI and layout decisions.
- Receives state and callbacks only.
- No direct navigation or store references.

Components:
- Small UI building blocks.
- No navigation or store subscription.

## Aura vs Feature Components
Put components in `foundation/aura` only if:
- They are reused across multiple features.
- They are visually generic and not tied to a domain concept.

Keep in feature `presentation/components` if:
- They are domain-specific (e.g., contact, invoice, peppol).
- They are only used within a single feature.

## Enums: Localization and Iconization
- Enums must provide `.localized` and `.iconized` extensions.
- Do not map enums with `when` inside screens.
- Extensions live in `foundation/aura/extensions` or a feature `ui` package if scoped.
- Extensions must be `commonMain` compatible.

## Side Effects
Allowed only in Route:
- Navigation (LocalNavController, `navigateTo`, `savedStateHandle`).
- Snackbars, dialogs, and one-off UI events.
- Analytics, logging, and system integrations.

Screens and components emit intents/callbacks only.

## Resources & Strings
- No hardcoded strings in UI.
- Use `stringResource` in UI-level composables.
- Enum labels must come from `.localized` extensions.
