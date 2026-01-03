# Refactor Safety

## Goals
- Keep changes incremental and compile-safe.
- Preserve runtime behavior and FlowMVI state transitions.
- Avoid large rewrites that mix UI, state, and navigation.

## Checklist
1. Split Route/Screen/Components first, then move packages.
2. Keep public APIs stable; update all call sites when needed.
3. Move side effects to routes only (navigation, snackbars, analytics).
4. Use `.localized` / `.iconized` extensions for enums.
5. Prefer small files; split when exceeding 250-400 LOC.
6. Run `./gradlew checkAll` before merging.

## Incremental Strategy
- Refactor one destination at a time.
- Keep screens pure UI with state + callbacks.
- Introduce new routes next to old screens, then switch navigation.
- Delete unused old code only after compilation passes.

## Guardrails
- `./gradlew checkKotlinFileSize` enforces max 450 LOC.
- `./gradlew checkNoNavInComponents` blocks navigation imports in `presentation/components` and `presentation/screen`.
- `./gradlew detektAll` enforces complexity and formatting rules.

## Testing
- Run `./gradlew :composeApp:assembleDebug` for Android.
- Run `./gradlew :backendApp:build` for backend.
- Use targeted module tests when iterating.
