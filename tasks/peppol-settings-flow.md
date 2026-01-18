# Peppol settings flow — redesign + state machine alignment

This document tracks the work to bring the Peppol **Settings / Enablement** flow in line with the new UX artifact and the product philosophy:

- Silent by default (minimal user effort)
- Never show external provider names to users
- Don’t ask for VAT in the Peppol UI (workspace already has VAT + Recommand integration exists)
- Make the Premium positioning visible in the UI (subtle, non-blocking)
- Mobile-friendly Compose UI

---

## Phase 0 — Audit (done)

- [x] Locate Peppol registration entry points (`SettingsDestination.PeppolRegistration`, workspace creation route).
- [x] Confirm architecture constraints (Route/Screen split, `checkNoNavInComponents`, FlowMVI).
- [x] Confirm existing Peppol infrastructure (Recommand integration, tenant VAT availability).

---

## Phase 1 — Backend: registration state machine completeness (done)

### What changed

- [x] `POST /api/v1/peppol/enable` now enables Peppol using **current tenant context** (no request body needed).
- [x] Added `POST /api/v1/peppol/enable-sending-only` to support the “blocked → sending only” path.
- [x] “Waiting transfer” polling now also supports `SENDING_ONLY` → `ACTIVE` auto-completion when the Peppol ID becomes available.
- [x] Suppress provider name leakage: verification results no longer return `blockedBy` for UI.
- [x] Improve Recommand company sync: ensure company data is updated (address, enterprise number, SMP recipient flag).

### Files (key)

- `foundation/domain/src/commonMain/kotlin/tech/dokus/domain/routes/PeppolRoutes.kt`
- `backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/PeppolRoutes.kt`
- `foundation/peppol/src/main/kotlin/tech/dokus/peppol/service/PeppolRegistrationService.kt`
- `foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/client/RecommandCompaniesClient.kt`
- `foundation/peppol/src/main/kotlin/tech/dokus/peppol/service/PeppolVerificationService.kt`

---

## Phase 2 — Design system: reusable UI pieces (done)

Created generic aura components (not Peppol-specific) mirroring the mock:

- [x] `AnimatedCheck` (success state animation)
- [x] `WaitingIndicator` (subtle pulsing waiting state)
- [x] `PCopyRow` (copy-to-clipboard row for IDs)

Files:

- `foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/common/AnimatedCheck.kt`
- `foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/common/WaitingIndicator.kt`
- `foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/common/PCopyRow.kt`

---

## Phase 3 — Frontend: new flow + modern UI (done)

### UX/UI

- [x] Replace outdated “enter VAT / provider” UX with the new flow screens:
  - Fresh → Activating → Active
  - Blocked → (Wait transfer | Sending only)
  - Waiting transfer (polling)
  - External / Failed
- [x] Add Premium hint in the “Fresh” screen (badge).
- [x] Add “Transfer request” expandable with copyable email template.
- [x] Make it mobile-friendly (centered content width, scroll-safe layout).

### State + side-effects

- [x] Rewrite FlowMVI contract/container to:
  - derive Peppol ID from the current workspace VAT
  - never request VAT input from the user
  - map backend statuses to the new UI states
- [x] Add waiting-state polling loop (30s) in the Route (not in the Screen).

Files (key):

- `features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/peppol/mvi/PeppolRegistrationContract.kt`
- `features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/peppol/mvi/PeppolRegistrationContainer.kt`
- `features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/peppol/screen/PeppolRegistrationScreen.kt`
- `features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/peppol/route/PeppolRegistrationRoute.kt`

---

## Phase 4 — Client data/domain wiring (done)

- [x] Update domain use cases to match the backend:
  - `EnablePeppolUseCase` no longer takes VAT input.
  - Add `EnablePeppolSendingOnlyUseCase`.
- [x] Update cashflow remote datasource:
  - `enablePeppol()` calls `POST /api/v1/peppol/enable` with no body
  - `enablePeppolSendingOnly()` calls `POST /api/v1/peppol/enable-sending-only`
- [x] Update Koin wiring (data + presentation).

Files (key):

- `features/cashflow/domain/src/commonMain/kotlin/tech/dokus/features/cashflow/usecases/PeppolRegistrationUseCases.kt`
- `features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/usecase/PeppolRegistrationUseCaseImpls.kt`
- `features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/datasource/CashflowRemoteDataSource.kt`
- `features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/datasource/CashflowRemoteDataSourceImpl.kt`
- `features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/di/CashflowDataModule.kt`
- `features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/di/CashflowPresentationModule.kt`

---

## Phase 5 — Validation (done)

### Commands run

- [x] `./gradlew :backendApp:compileKotlin`
- [x] `./gradlew :features:cashflow:presentation:compileKotlinDesktop`
- [x] `./gradlew :features:cashflow:presentation:compileKotlinIosSimulatorArm64`
- [x] `./gradlew checkNoNavInComponents`

### Notes

- `./gradlew checkKotlinFileSize` currently fails repo-wide due to pre-existing large files; Peppol UI files added/modified in this task are kept under the 450 LOC limit by splitting shared UI into `PeppolRegistrationSharedUi.kt`.

---

## Follow-ups (optional)

- [ ] Hook the “Premium” badge to real entitlement (if/when subscription state is available).
- [ ] Consider background polling for `SENDING_ONLY` → `ACTIVE` on backend (if desired).
- [ ] Add i18n keys for the Peppol copy once translation extraction is in place.
