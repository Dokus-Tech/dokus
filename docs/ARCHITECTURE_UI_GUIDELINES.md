# Architecture & UI Guidelines

## Module boundaries
- `foundation/*`: shared building blocks only (design system, navigation, platform glue, domain models, storage).
- `features/<domain>/*`: vertical slices. No cross-feature dependencies in presentation.
- `composeApp/`: app entry, navigation host, platform-specific wiring only.
- `backendApp/`: server-side routes/services only.

## Folder structure conventions
```
features/<domain>/presentation/
  route/         // navigation + container wiring + side-effects
  screen/        // pure UI composition (state + callbacks)
  components/    // feature-specific UI pieces
  models/        // UI-only models + mappers
features/<domain>/data/
features/<domain>/domain/   // only if per-feature domain exists
```

## Naming conventions
- `*Route`: navigation + container subscription + side-effects.
- `*Screen`: stateless UI composition.
- `*Component`: reusable within feature only.
- File names match primary composable/class.

## Aura vs feature components
- Put in `foundation/aura` only when UI is reusable across features or clearly generic.
- Feature-specific UI stays in `features/<domain>/presentation/components`.
- Aura components must be neutral in naming (`PDateField`, `PDropdownField`, `PChoiceChips`).

## Enum localization/iconization
- UI never maps enums with `when` in screens/components.
- Use `.localized` / `.iconized` extensions in `foundation/aura/extensions` or feature UI models.
- Extensions must live in `commonMain` and use Compose resources.

## Side-effects
- Navigation, snackbars, analytics, and system effects live in `*Route`.
- `*Screen` and `*Component` emit intents/callbacks only.
- Store/reducer performs validation and state transformations.

## MVI (FlowMVI)
- UI emits `Intent` only; reducers handle all logic.
- State is immutable and stable; derived fields belong in state or presenter/mapper.
- Actions are side-effects (navigation/snackbar) handled in routes.
