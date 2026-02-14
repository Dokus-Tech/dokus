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

### LOW Priority (won't fix - acceptable as-is)
- `InvoiceDraftData` returning `true` unconditionally in `hasRequiredFieldsForAutoConfirm` — Intentional: invoice validation is handled by other checks (`isAmountPositive`, contact linkage).
- `Pair` return type in `listWithDraftsAndIngestion` — Pre-existing, not introduced by this branch.
- `processBatchForTest` internal test helper — Acceptable pattern for testing concurrent worker behavior.
- `startedAt` could be null edge case — Very unlikely in practice since `markAsProcessing` always sets it.

## Review

### Changes Made
| File | Change |
|------|--------|
| `DocumentProcessingConstants.kt` | `fun ingestionTimeoutErrorMessage()` → `val INGESTION_TIMEOUT_ERROR_MESSAGE` |
| `DocumentProcessingWorker.kt` | Updated caller to use `INGESTION_TIMEOUT_ERROR_MESSAGE` |
| `ProcessorIngestionRepository.kt` | Updated caller to use `INGESTION_TIMEOUT_ERROR_MESSAGE` |
| `DocumentIngestionRunRepository.kt` | Removed stale recovery from 3 read methods; removed 3 public/private recovery methods; removed unused `lessEq` import |
| `DocumentRepository.kt` | Removed DI bypass (`DocumentIngestionRunRepository()` instantiation); reverted to expression body |
| `PeppolPollingWorker.kt` | `runCatching` → `runSuspendCatching`; added import |
| `DocumentIngestionRunRepositoryStaleRecoveryTest.kt` | Deleted (tested removed behavior) |

### Build Verification
- `./gradlew :foundation:domain:compileKotlinJvm` — BUILD SUCCESSFUL
- `./gradlew :foundation:database:compileKotlin` — BUILD SUCCESSFUL
- `./gradlew :backendApp:compileKotlin` — BUILD SUCCESSFUL
- `./gradlew :foundation:database:compileTestKotlin` — BUILD SUCCESSFUL
- `./gradlew :backendApp:compileTestKotlin` — BUILD SUCCESSFUL
