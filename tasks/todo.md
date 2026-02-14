# Review & Fix: `fix/document-processing-hung` branch

## Scope Review
All changes are **in scope** - document processing reliability, Peppol hardening, auto-confirm policy. No scope creep.

## Findings & Fixes

### HIGH Priority
- [x] **1. DI bypass in DocumentRepository** — Removed `DocumentIngestionRunRepository()` manual instantiation; reverted to expression body.
- [x] **2. Remove stale recovery from read paths** — Removed `recoverStaleProcessingRunsInternal()` calls from `listByDocument`, `getLatestForDocument`, `findActiveRun`, and `listWithDraftsAndIngestion`. Deleted `recoverStaleProcessingRunsForTenant()`, `recoverStaleProcessingRuns()`, `recoverStaleProcessingRunsInternal()` methods. Deleted `DocumentIngestionRunRepositoryStaleRecoveryTest.kt`. Worker's `processBatch` already recovers stale runs every poll cycle.

### MEDIUM Priority
- [x] **3. `ingestionTimeoutErrorMessage()` → `INGESTION_TIMEOUT_ERROR_MESSAGE` val** — Changed from `fun` to `val` in `DocumentProcessingConstants`. Updated all 3 callers.
- [x] **4. `runCatching` → `runSuspendCatching` in PeppolPollingWorker** — Fixed at line 288; added import. Prevents swallowing `CancellationException` in suspend lambda.
- [x] **5. Simplify `recoverStaleProcessingRunsInternal` condition** — Resolved by removing the method entirely (fix #2). The remaining `ProcessorIngestionRepository.recoverStaleRuns()` has no duplication (no nullable tenantId).

### LOW Priority
- [x] **6. `InvoiceDraftData` required fields check** — Added `invoiceNumber` and `totalAmount` checks for invoice auto-confirm (was unconditionally `true`).
- [x] **7. `Pair` return type → `DocumentListPage<T>`** — Created `DocumentListPage<T>(items, totalCount)` data class. Updated `listByTenant`, `listWithDraftsAndIngestion`, and `DocumentListingQuery`. All 7 destructuring call sites work unchanged.
- [x] **8. `startedAt` null edge case** — Added `isNull() or (lessEq cutoff)` in `ProcessorIngestionRepository.recoverStaleRuns()` so runs with null `startedAt` are also recovered.
- `processBatchForTest` internal test helper — Acceptable pattern, no change needed.

## Review

### Changes Made (all fixes)
| File | Change |
|------|--------|
| `DocumentProcessingConstants.kt` | `fun ingestionTimeoutErrorMessage()` → `val INGESTION_TIMEOUT_ERROR_MESSAGE` |
| `DocumentProcessingWorker.kt` | Updated caller to use `INGESTION_TIMEOUT_ERROR_MESSAGE` |
| `ProcessorIngestionRepository.kt` | Updated caller to `INGESTION_TIMEOUT_ERROR_MESSAGE`; added `startedAt.isNull()` fallback; added `isNull`/`or` imports |
| `DocumentIngestionRunRepository.kt` | Removed stale recovery from 3 read methods; removed 3 recovery methods; removed unused `lessEq` import |
| `DocumentRepository.kt` | Removed DI bypass; added `DocumentListPage<T>` data class; updated `listByTenant` and `listWithDraftsAndIngestion` return types |
| `DocumentListingQuery.kt` | Updated return type to `DocumentListPage`; changed 2 return expressions |
| `PeppolPollingWorker.kt` | `runCatching` → `runSuspendCatching`; added import |
| `AutoConfirmPolicy.kt` | Added `invoiceNumber`/`totalAmount` checks for `InvoiceDraftData` |
| `baseline_foundation_database.xml` | Updated detekt baseline for new return type |
| `DocumentIngestionRunRepositoryStaleRecoveryTest.kt` | Deleted (tested removed behavior) |

### Build Verification
- `./gradlew :foundation:database:compileKotlin` — BUILD SUCCESSFUL
- `./gradlew :backendApp:compileKotlin` — BUILD SUCCESSFUL
- `./gradlew :foundation:database:compileTestKotlin` — BUILD SUCCESSFUL
- `./gradlew :backendApp:compileTestKotlin` — BUILD SUCCESSFUL
