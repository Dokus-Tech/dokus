# Code Review Fixes — `feature/incoming-document-flow`

## HIGH Priority
- [x] 1. Replace `runCatching` → `runSuspendCatching` in `DocumentPurposeSimilarityService` (4 calls) and `DocumentConfirmationDispatcher` (1 call)
- [x] 2. Wrap purpose enrichment in `runSuspendCatching` in `DocumentProcessingWorker` (best-effort, shouldn't kill ingestion)
- [x] 3. Fix `retryClicked` latch in `PeppolRegistrationScreen.SetupErrorContent` — reset on new error state
- [x] 4. Fix non-atomic upserts in `DocumentPurposeSimilarityRepository` and `DocumentPurposeTemplateRepository` — use Exposed `upsert`
- [x] 5. Fix double navigation-to-login on forced logout in `DokusNavHost` — add `hasForceLoggedOut` guard

## MEDIUM Priority
- [x] 6. Add audit log for `applyUserPurposeEdit` — SKIPPED (no audit infrastructure exists yet)
- [x] 7. Add error boundary for Peppol structured snapshot deserialization in `AcceptDocumentStrategy`
- [x] 8. Add HNSW index comment + Flyway migration for `document_purpose_examples.embedding`
- [x] 9. Guard `updatePurposeContext` against null key overwrites in `DocumentDraftRepository`
- [x] 10. Fix desktop empty state — show `documents_empty_title` on all screen sizes when filter=All
- [x] 11. Fix `knownRemoteDocumentIds` ghost row flicker in `DocumentsRoute`

## LOW Priority
- [x] 12. Fix `forceNavigateToLogin` stale closure — addressed by `hasForceLoggedOut` guard from task 5
- [x] 13. Fix fallback `purposeSource` — use `null` instead of `AiRag` when neither template nor RAG matched
- [x] 14. Fix `UUID.fromString(x.toString())` anti-pattern — use `tenantId.value.toJavaUuid()` (done in task 4)
- [x] 15. Fix `InlineRowAction` touch target — raise `defaultMinSize` to 44dp
- [x] 16. Remove unused imports + replace `runBlocking` → `runTest` in 3 test files

## Review Summary

### Changes made (17 files modified, 1 file created)

**Backend services:**
- `DocumentPurposeSimilarityService.kt` — 4x `runCatching` → `runSuspendCatching`
- `DocumentConfirmationDispatcher.kt` — 1x `runCatching` → `runSuspendCatching`
- `DocumentProcessingWorker.kt` — wrapped `enrichAfterContactResolution` in `runSuspendCatching`
- `DocumentPurposeService.kt` — no changes needed (audit infra missing)

**Database/repositories:**
- `DocumentPurposeSimilarityRepository.kt` — replaced manual SELECT+INSERT/UPDATE with Exposed `upsert`, fixed UUID pattern
- `DocumentPurposeTemplateRepository.kt` — replaced manual SELECT+INSERT/UPDATE with Exposed `upsert`, fixed UUID pattern
- `DocumentDraftRepository.kt` — guarded null overwrites of counterpartyKey/merchantToken
- `DocumentPurposeExamplesTable.kt` — added HNSW index comment
- Created `V6__purpose_examples_embedding_index.sql` Flyway migration

**AI/processing:**
- `AcceptDocumentStrategy.kt` — catch `SerializationException` on Peppol snapshot deserialization
- `PurposeEnrichmentStrategy.kt` — fallback source → `null`, removed unused `LocalDate` import

**Frontend/UI:**
- `DokusNavHost.kt` — added `hasForceLoggedOut` guard preventing double navigation, reset on LoginSuccess
- `PeppolRegistrationScreen.kt` — `remember(state)` for retryClicked to reset on new error state
- `DocumentsScreenContent.kt` — desktop empty state now shows correct `documents_empty_title`
- `DocumentsRoute.kt` — fixed `knownRemoteDocumentIds` reset logic
- `DocumentsLocalUploadRow.kt` — raised InlineRowAction touch target to 44dp

**Tests:**
- `PeppolVerificationServiceErrorMappingTest.kt` — removed unused `assertFalse` import
- `DocumentPurposeServiceTest.kt`, `DocumentPurposeSimilarityServiceTest.kt`, `DocumentConfirmationDispatcherTest.kt` — `runBlocking` → `runTest`
