# Refactor Safety Checklist

## Goals

- Keep behavior stable.
- Keep changes incremental and reviewable.
- Avoid mixing architecture refactor and functional changes in one PR.

## Mandatory Sequence

1. Split responsibilities first (`route`/`screen`/`components`).
2. Keep existing public contracts stable where possible.
3. Migrate call sites.
4. Delete obsolete code only after compile/tests pass.

## Guardrails

```bash
./gradlew checkKotlinFileSize
./gradlew checkNoNavInComponents
./gradlew detektAll
./gradlew checkAll
```

## Runtime Verification

Use targeted builds/tests while iterating:

```bash
./gradlew :backendApp:build
./gradlew :composeApp:compileKotlinDesktop
./gradlew :composeApp:desktopTest
```

## PR Safety Rules

- One logical refactor per PR.
- No unrelated behavior changes.
- Include before/after notes for moved files.
- Update docs when architectural expectations change.
