# Company Settings Screen Implementation

**Status:** Complete
**Date:** 2026-01-18

---

## Summary

Implemented a redesigned `WorkspaceSettingsScreen` following the Dokus design philosophy of "Infrastructure, not SaaS". The screen has been transformed from a form-based settings page into a status ledger showing legal identity and PEPPOL compliance status.

---

## Changes Made

### Phase 1: New Aura Components
- [x] `StatusDot` - Circular status indicator with Confirmed/Warning/Error/Neutral/Empty types
- [x] `LockIcon` - Lock icon for indicating immutable fields
- [x] `DataRow` - Label-value pair component with status and lock indicators, responsive layout
- [x] `SettingsSection` - Collapsible section with edit mode support

### Phase 2: Domain Model
- [x] `PeppolActivityDto` - Model for PEPPOL activity timestamps

### Phase 3: Use Cases
- [x] `GetPeppolActivityUseCase` - Fetches latest PEPPOL transmission timestamps

### Phase 4: Contract Updates
- [x] Added `peppolRegistration` and `peppolActivity` to Content state
- [x] Added `editingSection` for section-level edit mode
- [x] Added `isLegalIdentityLocked` computed property
- [x] Added new intents: `EnterEditMode`, `CancelEditMode`, `SaveSection`

### Phase 5: Container Updates
- [x] Added PEPPOL use cases to container
- [x] Fetches PEPPOL data in `handleLoad()`
- [x] Handles section edit mode intents

### Phase 6: Screen Redesign
- [x] PEPPOL Connection section (always first, primary visual weight)
- [x] Legal Identity section (collapsed if verified, fields locked after PEPPOL)
- [x] Banking Details section (collapsible)
- [x] Invoice Format section (collapsible)
- [x] Payment Terms section (collapsible)
- [x] Section-level edit mode with Save/Cancel actions

---

## Files Changed

| File | Change |
|------|--------|
| `foundation/aura/.../components/status/StatusDot.kt` | NEW |
| `foundation/aura/.../components/icons/LockIcon.kt` | NEW |
| `foundation/aura/.../components/settings/DataRow.kt` | NEW |
| `foundation/aura/.../components/settings/SettingsSection.kt` | NEW |
| `foundation/aura/.../composeResources/values/common.xml` | Added `action_edit` string |
| `foundation/domain/.../model/PeppolActivity.kt` | NEW |
| `features/cashflow/domain/.../usecases/PeppolRegistrationUseCases.kt` | Added interface |
| `features/cashflow/data/.../usecase/PeppolRegistrationUseCaseImpls.kt` | Added implementation |
| `features/cashflow/data/.../di/CashflowDataModule.kt` | Added DI binding |
| `composeApp/.../viewmodel/WorkspaceSettingsContract.kt` | Added PEPPOL state |
| `composeApp/.../viewmodel/WorkspaceSettingsContainer.kt` | Added PEPPOL handling |
| `composeApp/.../screens/settings/WorkspaceSettingsScreen.kt` | Rewritten |
| `composeApp/.../DiModule.kt` | Added PEPPOL use cases |

---

## Verification

- Desktop tests pass (`./gradlew desktopTest`)
- WASM JS build succeeds (`./gradlew :composeApp:compileKotlinWasmJs`)

Note: Pre-existing test failure in `ValidateOgmUseCaseTest.kt` (unrelated to these changes)

---

## Key Design Decisions

1. **Section-level edit mode** - Each section can be edited independently
2. **Field locking** - Legal Name and VAT Number locked after PEPPOL Active status
3. **PEPPOL section at top** - Primary visual weight with elevated background
4. **DataRow for view mode** - Clean label-value display with status indicators
5. **Responsive layout** - Desktop (horizontal) vs Mobile (stacked) layouts
