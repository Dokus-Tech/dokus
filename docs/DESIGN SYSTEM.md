# Dokus Design System

This document captures current UI direction and implementation constraints for Dokus.

## Design Intent

- Calm, operational UI for financial workflows.
- Structure and readability over decorative effects.
- Ledger-first surfaces for documents and cashflow data.

## Core Principles

1. Structure over decoration.
2. Data readability over visual novelty.
3. Semantic color only for state.
4. Quiet defaults; emphasis only when action is required.

## Aura as the Shared UI Layer

Cross-feature reusable primitives belong in:
- `foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/*`

Feature-specific UI belongs in feature presentation modules.

## Shape and Spacing

- Favor small radii and consistent spacing.
- Avoid exaggerated rounding and heavy shadow stacks.
- Prefer borders/contrast/spacing to separate content.

## Color Usage

- Use neutral surfaces for primary layout.
- Reserve semantic colors for statuses (success/warning/error/processing).
- Do not use semantic colors as decorative background noise.

## Typography

- Keep hierarchy tight and legible.
- Avoid oversized hero typography in operational screens.
- Favor dense but readable table/list text styles.

## Interaction Patterns

- Mobile-first capture and quick actions.
- Desktop-first review, filtering, and detail panes.
- States should always be explicit: loading, empty, success, error.

## Accessibility Baseline

- Adequate touch targets.
- Color contrast for status and text.
- Error messages readable without technical jargon.

## Validation

When changing shared styles/components, run:

```bash
./gradlew verifyScreenshots
./gradlew checkAll
```
