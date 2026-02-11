# Fix: BILL misclassified as INVOICE — wrong counterparty

## Plan

### 1. Add `associatedPersonNames` to `InputWithTenantContext` interface
- [x] Add `val associatedPersonNames: List<String> get() = emptyList()` to `InputWithTenantContext`
- [x] Update `tenantContextInjectorNode` prompt to include person names + fuzzy matching guidance

### 2. Add `associatedPersonNames` field to `AcceptDocumentInput`
- [x] Add `override val associatedPersonNames: List<String> = emptyList()` to `AcceptDocumentInput`

### 3. Fetch tenant member names in `DocumentProcessingWorker`
- [x] Inject `UserRepository` dependency
- [x] Fetch active member names and pass to `AcceptDocumentInput`

### 4. Strengthen classification prompt in `ClassifyDocumentSubGraph`
- [x] Remove `@Suppress("UnusedReceiverParameter")`, use `tenant` in prompt
- [x] Add fuzzy name matching guidance
- [x] Make INVOICE vs BILL distinction explicit with tenant name

### 5. Verify
- [x] Compile: `./gradlew :features:ai:backend:compileKotlin` — BUILD SUCCESSFUL
- [x] Compile: `./gradlew :backendApp:compileKotlin` — BUILD SUCCESSFUL

---

# Feature: "Something's wrong" → Re-analyze with user feedback

## Plan

### Layer 1: Domain model
- [x] Add `val userFeedback: String? = null` to `ReprocessRequest` in `DocumentRecordDto.kt`

### Layer 2: Database
- [x] Add `user_feedback` column to `DocumentIngestionRunsTable`
- [x] Add `userFeedback` field to `IngestionItemEntity`
- [x] Update `createRun()` to accept and store `userFeedback`
- [x] Update `findPendingForProcessing()` to read `userFeedback` (both repositories)

### Layer 3: Backend route + worker
- [x] Pass `request.userFeedback` through route to `createRun()`
- [x] Read `ingestion.userFeedback` in worker, pass to `AcceptDocumentInput`

### Layer 4: AI pipeline
- [x] Add `val userFeedback: String? = null` to `AcceptDocumentInput`
- [x] Add `val userFeedback: String?` to `InputWithTenantContext` interface
- [x] Inject user feedback into LLM session in `tenantContextInjectorNode()` via `buildUserFeedbackPrompt()`

### Layer 5: Frontend
- [x] Add `FeedbackDialogState` to `DocumentReviewState.kt`
- [x] Add intents: `ShowFeedbackDialog`, `DismissFeedbackDialog`, `UpdateFeedbackText`, `SubmitFeedback`
- [x] Add action handlers in `DocumentReviewActions.kt`
- [x] Wire intents in `DocumentReviewReducer.kt` + `DocumentReviewContainer.kt`
- [x] Create `FeedbackDialog.kt` component
- [x] Update `DocumentReviewRoute.kt` — render feedback dialog, "reject instead" opens reject dialog
- [x] Change "Something's wrong" in `DocumentReviewScreen.kt`, `ReviewContent.kt` to open feedback dialog
- [x] Add string resources in `cashflow.xml`

### Layer 6: DB Migration
- [x] Add Flyway migration `V2__add_user_feedback_to_ingestion_runs.sql`

### Layer 7: Verify
- [x] Compile: `./gradlew :features:ai:backend:compileKotlin` — BUILD SUCCESSFUL
- [x] Compile: `./gradlew :backendApp:compileKotlin` — BUILD SUCCESSFUL
- [x] Compile: `./gradlew :features:cashflow:presentation:compileKotlinWasmJs` — BUILD SUCCESSFUL

---

## Review

### Changes Summary

**13 files modified, 2 files created:**

| File | Change |
|------|--------|
| `DocumentRecordDto.kt` | Added `userFeedback: String? = null` to `ReprocessRequest` |
| `DocumentIngestionRunsTable.kt` | Added `user_feedback` nullable text column |
| `IngestionItemEntity.kt` | Added `userFeedback: String? = null` field |
| `DocumentIngestionRunRepository.kt` | Added `userFeedback` param to `createRun()`, read it in `findPendingForProcessing()` |
| `ProcessorIngestionRepository.kt` | Read `userFeedback` in `findPendingForProcessing()` |
| `DocumentRecordRoutes.kt` | Pass `request.userFeedback` to `createRun()` |
| `DocumentProcessingWorker.kt` | Pass `ingestion.userFeedback` to `AcceptDocumentInput` |
| `AcceptDocumentStrategy.kt` | Added `userFeedback: String? = null` to `AcceptDocumentInput` |
| `TenantContextInjectorNode.kt` | Added `userFeedback` to interface + `buildUserFeedbackPrompt()` + inject as user message |
| `DocumentReviewState.kt` | Added `FeedbackDialogState` data class + `feedbackDialogState` to `Content` |
| `DocumentReviewIntent.kt` | Added 4 new intents for feedback dialog |
| `DocumentReviewActions.kt` | Added 4 feedback dialog handlers (show, dismiss, update text, submit) |
| `DocumentReviewReducer.kt` | Wired 4 new feedback handlers |
| `DocumentReviewContainer.kt` | Wired 4 new intents to reducer |
| `DocumentReviewScreen.kt` | Changed top bar "Something's wrong" → `ShowFeedbackDialog` |
| `ReviewContent.kt` | Changed desktop + mobile footer "Something's wrong" → `ShowFeedbackDialog` |
| `DocumentReviewRoute.kt` | Added `FeedbackDialog` rendering with "reject instead" secondary link |
| **New:** `FeedbackDialog.kt` | Correction-first dialog with text field, "Re-analyze" button, "Reject document instead" link |
| **New:** `V2__add_user_feedback_to_ingestion_runs.sql` | Flyway migration for `user_feedback` column |
| `cashflow.xml` | Added 4 string resources |

### Data flow

1. User clicks "Something's wrong" → `FeedbackDialog` opens
2. User types correction → clicks "Re-analyze"
3. `SubmitFeedback` intent → calls `reprocessDocument(id, ReprocessRequest(force=true, userFeedback="..."))`
4. Route creates ingestion run with `userFeedback` stored in DB
5. Worker picks up run → reads `userFeedback` → passes to `AcceptDocumentInput`
6. `tenantContextInjectorNode` injects feedback as `## USER CORRECTION` user message before classification/extraction
7. LLM sees the correction and adjusts its output accordingly

### What stays unchanged

- `RejectDocumentDialog.kt` — untouched, accessible via "Reject document instead" link in feedback dialog
- Existing reject flow — identical behavior
- `AnalysisFailedBanner` retry flow — still works (calls reprocess without feedback)
- Auto-polling logic — already exists, reused (UI transitions when re-processing completes)
