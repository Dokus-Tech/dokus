# Refactor Safety

## Incremental strategy
- Change one feature slice at a time (route → screen → components).
- Keep each commit/build green before moving to the next batch.
- Avoid multi-module rewrites; prefer small, targeted moves.

## Safe refactor checklist
- Preserve public APIs or update all call sites in the same change.
- Keep business logic out of composables; emit intents only.
- Keep navigation and side-effects in `*Route`.
- Add/extend UI-only mappers for derived data.
- Update string resources for all user-visible text.

## Guardrails
- Detekt rules enforce file/function length and basic complexity.
- `checkKotlinFileSize` fails if any Kotlin source > 450 lines.
- `checkNoNavInComponents` fails if navigation APIs appear under `presentation/components`.

## When touching large files
- Split by responsibility (route/screen/components/models).
- Remove dead code or duplicate UI after extraction.
- Prefer Aura components for repeated UI patterns.
