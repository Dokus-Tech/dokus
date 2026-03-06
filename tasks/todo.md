# Bank Statement Import Code Review Fixes

## Critical Security Fixes
- [x] C1-C5: Add tenant_id filters to all missing queries

## Backend Correctness
- [x] H1: Replace runCatching with runSuspendCatching
- [x] H2: Keep AUTO_PAY_ENABLED as const val (reverted env var approach per user request)
- [x] H3: Add paymentMethod to CashflowPaymentRequest
- [x] H4: Fix DI to use singleOf pattern
- [x] H5: Replace UUID.fromString(x.toString()) with x.value

## Domain Model Fixes
- [x] H7: Change counterpartyIban from String? to Iban?
  - [x] H7.1: Update `ImportedBankTransactionDto.counterpartyIban` type in Cashflow.kt
  - [x] H7.2: Wrap DB string in `Iban.from()` in ImportedBankTransactionRepository.toDto()
  - [x] H7.3: Update `normalizedIban(tx.counterpartyIban)` calls in BankStatementMatchingService.kt
  - [x] H7.4: Update `normalizeIban(tx.counterpartyIban)` call in InvoiceBankAutomationService.kt
  - [x] H7.5: Wrap string literal in `Iban(...)` in ReviewPreviewData.kt
- [x] H8: Unify AutoMatchCreatedBy -> PaymentCreatedBy
  - [x] H8.1: Remove `AutoMatchCreatedBy` enum from BankStatementEnums.kt
  - [x] H8.2: Update InvoiceBankMatchLinksTable.kt
  - [x] H8.3: Update InvoiceBankMatchLinkRepository.kt
  - [x] H8.4: Update AutoPaymentService.kt
- [x] M10: Rename ignoreSuggestedTransaction -> dismissSuggestedMatch

## AI Extraction Fixes
- [x] H9: Add IBAN validation in extraction
- [x] H10: Sanitize language prompt injection
- [x] M12: Add bank statement validation in auditor
- [x] M13: Comment dead Peppol branch

## Route/Worker Fixes
- [x] H11: Add error handling to getPaymentCandidates
- [x] M5: Default bank statements to NeedsReview
- [x] M31: Fix dispatcher error message

## Database Fixes
- [x] H12: Fix broken unique constraint (V9 migration)
- [x] M6: Fix upsertAutoMatched race condition
- [x] M7: Push remainingAmount filter to SQL
- [x] L5: Add missing audit events index
- [x] L6: Add created_at to payment candidates

## Frontend Fixes
- [x] H13: Replace date text field with PDatePickerDialog
- [x] H14: Map raw reasons to user-friendly labels
- [x] M9: Replace hardcoded English strings
- [x] M15: Fix transaction picker overflow
- [x] L8: Add suggested match indicator
- [x] M33: Remove unnecessary launch wrapper

## Remaining Service Fixes
- [x] M1: Add audit log for manual payments
- [x] M2: Fix suggestion clearing scope
- [x] M3: Replace parseJsonArray with Json.decodeFromString
- [x] M11: Add audit info to AutoPaymentStatusDto
- [x] L9-service: Replace silent overpayment clamp with error

## Review

### Summary of All Changes (33 fixes across ~30 files)

**Critical Security (C1-C5):** Added tenant_id WHERE clauses to all DB queries missing them — `InvoiceBankMatchLinkRepository` (markAutoPaid, markReversed, upsertAutoMatched UPDATE), `PaymentRepository` (listByInvoice, getTotalPaid, createPayment re-select), `AutoPaymentService` (undo PaymentsTable.update).

**Backend Correctness:**
- H1: Replaced `runCatching` with `runSuspendCatching` in all new suspend functions (AutoPaymentService, CashflowPaymentService, CashflowEntriesRepository, PaymentRepository). Fixed stale `return@runCatching` label.
- H2: Kept `AUTO_PAY_ENABLED` as `const val = false` (user decision).
- H3: Added `paymentMethod` field to `CashflowPaymentRequest`, threaded through `CashflowPaymentService`.
- H4: Replaced 4 manual `single { ... }` with `singleOf(::ClassName)` in DI config.
- H5: Replaced ~60 `UUID.fromString(x.toString())` with `x.value.toJavaUuid()` across 10 files.

**Domain Model:**
- H7: Changed `ImportedBankTransactionDto.counterpartyIban` from `String?` to `Iban?`, updated all 5 usage sites.
- H8: Removed duplicate `AutoMatchCreatedBy` enum, unified to `PaymentCreatedBy` in 4 files.
- M10: Renamed `ignoreSuggestedTransaction` to `dismissSuggestedMatch`.

**AI Extraction:**
- H9: Added `.takeIf { it.isValid }` after `Iban.from()` in extraction.
- H10: Sanitized language interpolation to prevent prompt injection.
- M12: Added per-row validation in `FinancialExtractionAuditor` (date, amount, confidence range).
- M13: Added defensive comment on unreachable Peppol bank statement branch.

**Route/Worker:**
- H11: Wrapped `getPaymentCandidates` in `runSuspendCatching` with error handling.
- M5: Bank statements now default to `NeedsReview` status.
- M31: Fixed dispatcher error message to "Bank statement documents cannot be manually confirmed".

**Database:**
- H12: Created V9 migration with partial unique index for manual payment dedup, dropped broken uniqueIndex.
- M6: Replaced read-then-write in `upsertAutoMatched` with Exposed `upsert()`.
- M7: Pushed `remainingAmount > 0` filter from Kotlin to SQL.
- L5: Added `index(tenantId, cashflowEntryId)` to AutoPaymentAuditEventsTable.
- L6: Added `created_at` column to CashflowPaymentCandidatesTable.

**Frontend:**
- H13: Replaced raw text field with `PDatePickerDialog` in RecordPaymentDialog.
- H14: Added `formatMatchReason()` mapping for user-friendly reason labels.
- M9: Extracted ~39 hardcoded strings to `cashflow.xml` string resources.
- M15: Wrapped transaction list in `LazyColumn` with `heightIn(max = 200.dp)`.
- L8: Added "Suggested match" indicator when selected tx matches suggestion.
- M33: Removed unnecessary `launch { }` wrapper around intent call.

**Services:**
- M1: Added audit log entry (ManualPayment/ManualPaid) to `CashflowPaymentService.recordPayment`.
- M2: Narrowed suggestion clearing to only entries receiving new scores.
- M3: Replaced hand-rolled `parseJsonArray` with `Json.decodeFromString`.
- M11: Added `scoreMargin` and `matchSignals` fields to `AutoPaymentStatusDto`.
- L9: Replaced silent overpayment clamp with explicit `DokusException.BadRequest`.

### Deferred Items (out of scope)
- M4: Scoring logic dedup (intentionally different thresholds)
- M8: Bank statement title fallback (low impact)
- M14: DirectionResolutionSource.Unknown confidence (cosmetic)
- L1: Double for score/margin (not money, acceptable)
- L3: BankStatement.supported flag (correct as-is)
- L4: Table package move (high churn, no-op)
- L7: Two-phase draft write (architectural, intentional)
- L9-domain: EUR currency fallback (correct for Belgian market)

### Verification
- [ ] `./gradlew allTests` — all tests pass
- [ ] `./gradlew check` — full build succeeds
